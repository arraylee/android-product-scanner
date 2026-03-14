// ObjectDetector.kt - 商品检测管理器
package com.example.productscanner.detection

import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.media.ImageReader
import androidx.camera.core.ImageProxy
import com.example.productscanner.model.ScanRecord
import java.text.SimpleDateFormat
import java.util.*

class ObjectDetector(private val context: Context) {
    
    // 使用真正的 YOLO 检测器
    private val yoloDetector by lazy { YoloDetector(context) }
    
    // 识别记录
    private val scanRecords = mutableListOf<ScanRecord>()
    
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    /**
     * 从 ImageProxy 检测商品（点击识别按钮时使用）
     * 分析图像并自动识别商品种类和数量
     */
    fun detectFromImage(imageProxy: ImageProxy): List<ScanRecord> {
        val bitmap = imageProxy.toBitmap() ?: return emptyList()
        imageProxy.close()
        
        return detectFromBitmap(bitmap)
    }
    
    /**
     * 从 Bitmap 检测商品
     */
    fun detectFromBitmap(bitmap: Bitmap): List<ScanRecord> {
        // 使用 YOLO 模型检测
        val detections = yoloDetector.detect(bitmap)
        
        // 统计每种商品的数量
        val grouped = detections.groupBy { it.label }
        val records = mutableListOf<ScanRecord>()
        
        grouped.forEach { (productName, items) ->
            val record = ScanRecord(
                productName = productName,
                quantity = items.size,  // 检测到几个就是几箱
                time = dateFormat.format(Date())
            )
            records.add(record)
            scanRecords.add(record)
        }
        
        return records
    }
    
    /**
     * 获取所有识别记录
     */
    fun getAllRecords(): List<ScanRecord> = scanRecords.toList()
    
    /**
     * 获取统计信息
     */
    fun getStatistics(): Map<String, Int> {
        return scanRecords.groupBy { it.productName }
            .mapValues { it.value.sumOf { record -> record.quantity } }
    }
    
    /**
     * 清空记录
     */
    fun clearRecords() {
        scanRecords.clear()
    }
    
    /**
     * 获取商品类别列表
     */
    fun getProductNames(): List<String> = YoloDetector.CLASS_NAMES
    
    /**
     * 将 ImageProxy 转换为 Bitmap
     */
    private fun ImageProxy.toBitmap(): Bitmap? {
        val image = this.image ?: return null
        
        return try {
            // YUV_420_888 转 Bitmap
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer
            
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            
            val nv21 = ByteArray(ySize + uSize + vSize)
            
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)
            
            val yuvImage = android.graphics.YuvImage(
                nv21, android.graphics.ImageFormat.NV21,
                this.width, this.height, null
            )
            
            val out = java.io.ByteArrayOutputStream()
            yuvImage.compressToJpeg(
                android.graphics.Rect(0, 0, this.width, this.height),
                100, out
            )
            
            val imageBytes = out.toByteArray()
            android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun close() {
        yoloDetector.close()
    }
}
