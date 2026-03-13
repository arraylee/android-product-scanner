// ObjectDetector.kt - 简化版本（模拟检测）
package com.example.productscanner.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.camera.core.ImageProxy
import com.example.productscanner.model.DetectionResult
import kotlin.random.Random

class ObjectDetector(private val context: Context) {
    
    companion object {
        // 商品类别标签
        val LABELS = listOf(
            "东鹏特饮250ml",
            "可口可乐500ml",
            "康师傅冰红茶500ml",
            "康师傅红烧牛肉面大食桶115g",
            "泉阳泉野生蓝莓果汁饮料",
            "娟珊有机纯牛奶"
        )
    }
    
    private var frameCount = 0
    private val detectionHistory = mutableListOf<DetectionResult>()
    
    fun detect(imageProxy: ImageProxy): List<DetectionResult> {
        frameCount++
        
        // 每20帧模拟一次检测结果（演示用）
        if (frameCount % 20 == 0) {
            val result = DetectionResult(
                label = LABELS.random(),
                confidence = 0.6f + Random.nextFloat() * 0.35f,
                boundingBox = Rect(100, 100, 300, 400)
            )
            detectionHistory.add(result)
            return listOf(result)
        }
        return emptyList()
    }
    
    fun detect(bitmap: Bitmap): List<DetectionResult> {
        val result = DetectionResult(
            label = LABELS.random(),
            confidence = 0.7f + Random.nextFloat() * 0.25f,
            boundingBox = Rect(50, 50, 200, 300)
        )
        detectionHistory.add(result)
        return listOf(result)
    }
    
    // 获取统计结果
    fun getStatistics(): Map<String, Int> {
        return detectionHistory.groupingBy { it.label }.eachCount()
    }
    
    // 清空历史记录
    fun clearHistory() {
        detectionHistory.clear()
    }
    
    fun close() {
        // 清理资源
    }
}