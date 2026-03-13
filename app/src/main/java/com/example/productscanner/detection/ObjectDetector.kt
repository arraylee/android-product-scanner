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
        private val LABELS = listOf(
            "东鹏特饮250ml",
            "可口可乐500ml",
            "康师傅冰红茶500ml",
            "康师傅红烧牛肉面大食桶115g",
            "泉阳泉野生蓝莓果汁饮料"
        )
    }
    
    private var frameCount = 0
    
    fun detect(imageProxy: ImageProxy): List<DetectionResult> {
        frameCount++
        
        // 每30帧模拟一次检测结果（演示用）
        return if (frameCount % 30 == 0) {
            listOf(
                DetectionResult(
                    label = LABELS.random(),
                    confidence = 0.5f + Random.nextFloat() * 0.4f,
                    boundingBox = Rect(100, 100, 300, 400)
                )
            )
        } else {
            emptyList()
        }
    }
    
    fun detect(bitmap: Bitmap): List<DetectionResult> {
        return listOf(
            DetectionResult(
                label = LABELS.random(),
                confidence = 0.7f + Random.nextFloat() * 0.25f,
                boundingBox = Rect(50, 50, 200, 300)
            )
        )
    }
    
    fun close() {
        // 清理资源
    }
}