// ScanRecord.kt - 扫描记录数据类
package com.example.productscanner.model

data class ScanRecord(
    val productName: String,  // 商品名称
    val quantity: Int,        // 数量（箱）
    val time: String          // 识别时间 (HH:mm:ss)
)
