// ObjectDetector.kt - 商品检测器
package com.example.productscanner.detection

import android.content.Context
import com.example.productscanner.model.ScanRecord

class ObjectDetector(private val context: Context) {
    
    companion object {
        // 预定义的商品列表
        val PRODUCTS = listOf(
            "东鹏特饮250ml",
            "可口可乐500ml",
            "康师傅冰红茶500ml",
            "康师傅红烧牛肉面大食桶115g",
            "泉阳泉野生蓝莓果汁饮料",
            "娟珊有机纯牛奶"
        )
    }
    
    fun getProductNames(): List<String> = PRODUCTS
}
