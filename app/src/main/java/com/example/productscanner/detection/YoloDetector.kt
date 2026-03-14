// YoloDetector.kt - 真正的 YOLO 目标检测器
package com.example.productscanner.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import ai.onnxruntime.*
import java.nio.FloatBuffer
import java.util.*
import kotlin.math.max
import kotlin.math.min

data class Detection(
    val label: String,
    val confidence: Float,
    val bbox: RectF
)

class YoloDetector(private val context: Context) {
    
    companion object {
        // 模型输入尺寸
        private const val INPUT_SIZE = 640
        private const val CONFIDENCE_THRESHOLD = 0.5f
        private const val NMS_THRESHOLD = 0.45f
        
        // 商品类别名称（必须与训练时一致）
        val CLASS_NAMES = listOf(
            "东鹏特饮250ml",
            "可口可乐500ml",
            "康师傅冰红茶500ml",
            "康师傅红烧牛肉面大食桶115g",
            "泉阳泉野生蓝莓果汁饮料"
        )
    }
    
    private var ortSession: OrtSession? = null
    private val ortEnvironment = OrtEnvironment.getEnvironment()
    
    init {
        loadModel()
    }
    
    private fun loadModel() {
        try {
            val modelPath = "model/product_detector.onnx"
            context.assets.open(modelPath).use { inputStream ->
                val modelBytes = inputStream.readBytes()
                ortSession = ortEnvironment.createSession(modelBytes)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 检测图像中的商品
     * @param bitmap 输入图像
     * @return 检测到的商品列表
     */
    fun detect(bitmap: Bitmap): List<Detection> {
        if (ortSession == null) {
            return emptyList()
        }
        
        try {
            // 1. 预处理图像
            val inputTensor = preprocess(bitmap)
            
            // 2. 运行推理
            val results = ortSession?.run(mapOf("images" to inputTensor))
            
            // 3. 解析结果
            val detections = parseResults(results, bitmap.width, bitmap.height)
            
            // 4. 非极大值抑制（NMS）
            return applyNMS(detections)
            
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }
    
    /**
     * 图像预处理：调整大小并归一化
     */
    private fun preprocess(bitmap: Bitmap): OnnxTensor {
        // 调整图像大小为 640x640
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        
        // 转换为 FloatBuffer [1, 3, 640, 640]
        val floatBuffer = FloatBuffer.allocate(1 * 3 * INPUT_SIZE * INPUT_SIZE)
        
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaledBitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        
        // 归一化并转换为 CHW 格式
        for (c in 0 until 3) {
            for (h in 0 until INPUT_SIZE) {
                for (w in 0 until INPUT_SIZE) {
                    val pixel = pixels[h * INPUT_SIZE + w]
                    val value = when (c) {
                        0 -> ((pixel shr 16) and 0xFF) / 255.0f  // R
                        1 -> ((pixel shr 8) and 0xFF) / 255.0f   // G
                        else -> (pixel and 0xFF) / 255.0f        // B
                    }
                    floatBuffer.put(value)
                }
            }
        }
        
        floatBuffer.rewind()
        
        return OnnxTensor.createTensor(
            ortEnvironment,
            floatBuffer,
            longArrayOf(1, 3, INPUT_SIZE.toLong(), INPUT_SIZE.toLong())
        )
    }
    
    /**
     * 解析 YOLO 输出结果
     */
    private fun parseResults(results: OrtSession.Result?, imgWidth: Int, imgHeight: Int): List<Detection> {
        if (results == null) return emptyList()
        
        val detections = mutableListOf<Detection>()
        
        try {
            // 获取输出张量
            val outputTensor = results.get(0) as? OnnxTensor ?: return emptyList()
            val outputArray = outputTensor.floatBuffer
            
            // YOLOv8 输出格式: [num_predictions, 4 + num_classes]
            // 通常是 [8400, 9] 对于 5 个类别
            val numPredictions = outputArray.capacity() / (4 + CLASS_NAMES.size)
            
            for (i in 0 until numPredictions) {
                val baseIdx = i * (4 + CLASS_NAMES.size)
                
                // 边界框坐标 (x_center, y_center, width, height)
                val cx = outputArray.get(baseIdx)
                val cy = outputArray.get(baseIdx + 1)
                val w = outputArray.get(baseIdx + 2)
                val h = outputArray.get(baseIdx + 3)
                
                // 找到置信度最高的类别
                var maxConf = 0f
                var maxClassIdx = 0
                
                for (c in CLASS_NAMES.indices) {
                    val conf = outputArray.get(baseIdx + 4 + c)
                    if (conf > maxConf) {
                        maxConf = conf
                        maxClassIdx = c
                    }
                }
                
                // 过滤低置信度
                if (maxConf > CONFIDENCE_THRESHOLD) {
                    // 转换为原始图像坐标
                    val x1 = (cx - w / 2) * imgWidth / INPUT_SIZE
                    val y1 = (cy - h / 2) * imgHeight / INPUT_SIZE
                    val x2 = (cx + w / 2) * imgWidth / INPUT_SIZE
                    val y2 = (cy + h / 2) * imgHeight / INPUT_SIZE
                    
                    detections.add(Detection(
                        label = CLASS_NAMES[maxClassIdx],
                        confidence = maxConf,
                        bbox = RectF(x1, y1, x2, y2)
                    ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return detections
    }
    
    /**
     * 非极大值抑制（NMS）
     */
    private fun applyNMS(detections: List<Detection>): List<Detection> {
        if (detections.isEmpty()) return emptyList()
        
        // 按类别分组进行 NMS
        val grouped = detections.groupBy { it.label }
        val result = mutableListOf<Detection>()
        
        grouped.forEach { (_, classDetections) ->
            val sorted = classDetections.sortedByDescending { it.confidence }
            val suppressed = BooleanArray(sorted.size)
            
            for (i in sorted.indices) {
                if (suppressed[i]) continue
                
                result.add(sorted[i])
                
                for (j in i + 1 until sorted.size) {
                    if (suppressed[j]) continue
                    
                    val iou = calculateIoU(sorted[i].bbox, sorted[j].bbox)
                    if (iou > NMS_THRESHOLD) {
                        suppressed[j] = true
                    }
                }
            }
        }
        
        return result
    }
    
    /**
     * 计算两个边界框的 IoU
     */
    private fun calculateIoU(a: RectF, b: RectF): Float {
        val x1 = max(a.left, b.left)
        val y1 = max(a.top, b.top)
        val x2 = min(a.right, b.right)
        val y2 = min(a.bottom, b.bottom)
        
        val intersectionArea = max(0f, x2 - x1) * max(0f, y2 - y1)
        val unionArea = (a.right - a.left) * (a.bottom - a.top) +
                       (b.right - b.left) * (b.bottom - b.top) -
                       intersectionArea
        
        return if (unionArea > 0) intersectionArea / unionArea else 0f
    }
    
    /**
     * 释放资源
     */
    fun close() {
        ortSession?.close()
    }
}
