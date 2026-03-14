// ScannerScreen.kt - 重新设计版
package com.example.productscanner.ui

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.productscanner.detection.ObjectDetector
import com.example.productscanner.model.ScanRecord
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

@Composable
fun ScannerScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // 识别记录列表
    var scanRecords by remember { mutableStateOf(listOf<ScanRecord>()) }
    
    // 对话框状态
    var showProductDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    
    val objectDetector = remember { ObjectDetector(context) }
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 相机预览（仅预览，不自动识别）
        CameraPreview(modifier = Modifier.fillMaxSize())
        
        // 顶部标题 + 最新识别结果
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
        ) {
            // 标题
            Text(
                text = "📦 商品扫描",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), MaterialTheme.shapes.medium)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // 显示最新一次识别结果
            if (scanRecords.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val latest = scanRecords.last()
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.9f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "最新识别: ${latest.productName}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "数量: ${latest.quantity} 箱 | 时间: ${latest.time}",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        
        // 底部按钮
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 识别按钮
            Button(
                onClick = { showProductDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("📷 识别", fontSize = 16.sp)
            }
            
            // 历史记录按钮
            Button(
                onClick = { showHistoryDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("📋 记录 (${scanRecords.size})", fontSize = 16.sp)
            }
            
            // 统计按钮
            Button(
                onClick = { showHistoryDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("📊 统计", fontSize = 16.sp)
            }
        }
    }
    
    // 商品选择对话框
    if (showProductDialog) {
        ProductSelectionDialog(
            productNames = objectDetector.getProductNames(),
            onDismiss = { showProductDialog = false },
            onConfirm = { productName, quantity ->
                val record = ScanRecord(
                    productName = productName,
                    quantity = quantity,
                    time = dateFormat.format(Date())
                )
                scanRecords = scanRecords + record
                showProductDialog = false
            }
        )
    }
    
    // 历史记录/统计对话框
    if (showHistoryDialog) {
        HistoryDialog(
            records = scanRecords,
            onDismiss = { showHistoryDialog = false },
            onClear = { scanRecords = emptyList() }
        )
    }
}

@Composable
fun CameraPreview(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            
            previewView
        }
    )
}

@Composable
fun ProductSelectionDialog(
    productNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var selectedProduct by remember { mutableStateOf(productNames.firstOrNull() ?: "") }
    var quantity by remember { mutableStateOf("1") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "📦 识别商品",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 商品选择
                Text("选择商品类型:", fontSize = 14.sp, color = Color.Gray)
                
                // 简化的商品列表
                LazyColumn(
                    modifier = Modifier.height(200.dp)
                ) {
                    items(productNames) { name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedProduct == name,
                                onClick = { selectedProduct = name }
                            )
                            Text(
                                text = name,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                
                // 数量输入
                Text("输入数量（箱）:", fontSize = 14.sp, color = Color.Gray)
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { 
                        if (it.isEmpty() || it.toIntOrNull() != null) {
                            quantity = it
                        }
                    },
                    label = { Text("数量") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantity.toIntOrNull() ?: 1
                    if (selectedProduct.isNotEmpty() && qty > 0) {
                        onConfirm(selectedProduct, qty)
                    }
                }
            ) {
                Text("确认识别")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun HistoryDialog(
    records: List<ScanRecord>,
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
    // 统计每种商品的总数
    val statistics = remember(records) {
        records.groupBy { it.productName }
            .mapValues { entry -> entry.value.sumOf { it.quantity } }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "📋 识别记录 & 统计",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                // 统计摘要
                if (statistics.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE3F2FD)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "📊 统计摘要",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1976D2)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            statistics.entries.forEach { (name, count) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(name, fontSize = 14.sp)
                                    Text(
                                        "$count 箱",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                            }
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                "总计: ${statistics.size} 种商品, ${statistics.values.sum()} 箱",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // 详细记录
                Text(
                    "详细记录 (${records.size} 次识别):",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                if (records.isEmpty()) {
                    Text("暂无识别记录", color = Color.Gray)
                } else {
                    LazyColumn(
                        modifier = Modifier.height(200.dp)
                    ) {
                        items(records.reversed()) { record ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF5F5F5)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            record.productName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "时间: ${record.time}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Text(
                                        "${record.quantity} 箱",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        dismissButton = {
            if (records.isNotEmpty()) {
                TextButton(
                    onClick = onClear,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Text("清空记录")
                }
            }
        }
    )
}
