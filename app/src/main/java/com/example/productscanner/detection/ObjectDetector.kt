// ObjectDetector.kt
package com.example.productscanner.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import com.example.productscanner.model.DetectionResult
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class ObjectDetector(private val context: Context) {
    
    companion object {
        private const val MODEL_FILE = "product_model.tflite"
        private const val INPUT_SIZE = 640
        private const val CONFIDENCE_THRESHOLD = 0.5f
        
        // 商品类别标签
        private val LABELS = listOf(
            "东鹏特饮250ml",
            "可口可乐500ml",
            "康师傅冰红茶500ml",
            "康师傅红烧牛肉面大食桶115g",
            "泉阳泉野生蓝莓果汁饮料"
        )
    }
    
    private var interpreter: Interpreter? = null
    
    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
        .add(NormalizeOp(0f, 255f))
        .build()
    
    init {
        try {
            val model = FileUtil.loadMappedFile(context, MODEL_FILE)
            interpreter = Interpreter(model)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun detect(imageProxy: ImageProxy): List<DetectionResult> {
        val bitmap = imageProxy.toBitmap() ?: return emptyList()
        
        return detect(bitmap)
    }
    
    fun detect(bitmap: Bitmap): List<DetectionResult> {
        val interpreter = this.interpreter ?: return emptyList()
        
        // 预处理图像
        var tensorImage = TensorImage.fromBitmap(bitmap)
        tensorImage = imageProcessor.process(tensorImage)
        
        // 运行推理
        val output = Array(1) { Array(84) { FloatArray(8400) } }
        interpreter.run(tensorImage.buffer, output)
        
        // 解析结果
        return parseDetections(output[0])
    }
    
    private fun parseDetections(output: Array<FloatArray>): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()
        
        // YOLOv8 输出格式: [x, y, w, h, confidence, class0, class1, ...]
        for (i in 0 until 8400) {
            val confidence = output[4][i]
            
            if (confidence > CONFIDENCE_THRESHOLD) {
                // 找到最高置信度的类别
                var maxClassScore = 0f
                var maxClassIndex = 0
                
                for (c in 0 until 5) {
                    val score = output[5 + c][i]
                    if (score > maxClassScore) {
                        maxClassScore = score
                        maxClassIndex = c
                    }
                }
                
                val finalConfidence = confidence * maxClassScore
                
                if (finalConfidence > CONFIDENCE_THRESHOLD) {
                    results.add(
                        DetectionResult(
                            label = LABELS[maxClassIndex],
                            confidence = finalConfidence,
                            boundingBox = Rect(
                                (output[0][i] - output[2][i] / 2).toInt(),
                                (output[1][i] - output[3][i] / 2).toInt(),
                                (output[0][i] + output[2][i] / 2).toInt(),
                                (output[1][i] + output[3][i] / 2).toInt()
                            )
                        )
                    )
                }
            }
        }
        
        // NMS (非极大值抑制)
        return applyNMS(results)
    }
    
    private fun applyNMS(
        detections: List<DetectionResult>,
        iouThreshold: Float = 0.5f
    ): List<DetectionResult> {
        if (detections.isEmpty()) return emptyList()
        
        val sorted = detections.sortedByDescending { it.confidence }
        val selected = mutableListOf<DetectionResult>()
        val suppressed = BooleanArray(detections.size)
        
        for (i in sorted.indices) {
            if (suppressed[i]) continue
            
            selected.add(sorted[i])
            
            for (j in i + 1 until sorted.size) {
                if (suppressed[j]) continue
                
                if (calculateIoU(sorted[i].boundingBox, sorted[j].boundingBox) > iouThreshold) {
                    suppressed[j] = true
                }
            }
        }
        
        return selected
    }
    
    private fun calculateIoU(rect1: Rect, rect2: Rect): Float {
        val intersectionLeft = maxOf(rect1.left, rect2.left)
        val intersectionTop = maxOf(rect1.top, rect2.top)
        val intersectionRight = minOf(rect1.right, rect2.right)
        val intersectionBottom = minOf(rect1.bottom, rect2.bottom)
        
        if (intersectionLeft >= intersectionRight || intersectionTop >= intersectionBottom) {
            return 0f
        }
        
        val intersectionArea = (intersectionRight - intersectionLeft) * 
                              (intersectionBottom - intersectionTop)
        val rect1Area = rect1.width() * rect1.height()
        val rect2Area = rect2.width() * rect2.height()
        val unionArea = rect1Area + rect2Area - intersectionArea
        
        return intersectionArea.toFloat() / unionArea
    }
    
    private fun ImageProxy.toBitmap(): Bitmap? {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer
        
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        
        val nv21 = ByteArray(ySize + uSize + vSize)
        
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
        val imageBytes = out.toByteArray()
        
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
    
    fun close() {
        interpreter?.close()
    }
}
