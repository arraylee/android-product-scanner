// ScannerScreen.kt - AI 自动识别版（完整版）
package com.example.productscanner.ui

import androidx.camera.core.*
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
import kotlinx.coroutines.*
import java.util.concurrent.Executors

@Composable
fun ScannerScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    // 状态
    var latestRecords by remember { mutableStateOf(listOf<ScanRecord>()) }
    var allRecords by remember { mutableStateOf(listOf<ScanRecord>()) }
    var isDetecting by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("点击 📷 识别 按钮开始扫描") }
    
    // ObjectDetector
    val objectDetector = remember { ObjectDetector(context) }
    
    // 用于触发识别的状态
    var triggerDetection by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 相机预览 + 分析器
        CameraPreviewWithAnalysis(
            modifier = Modifier.fillMaxSize(),
            triggerDetection = triggerDetection,
            onDetectionComplete = { records ->
                latestRecords = records
                allRecords = objectDetector.getAllRecords()
                isDetecting = false
                statusText = if (records.isEmpty()) "未识别到商品，请调整角度重试" else "✅ 识别完成！"
                triggerDetection = false
            },
            objectDetector = objectDetector
        )
        
        // 顶部：标题和最新识别结果
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
        ) {
            // 标题
            Text(
                text = "📦 AI 商品扫描",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), MaterialTheme.shapes.medium)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 状态文字
            Text(
                text = statusText,
                fontSize = 14.sp,
                color = if (statusText.startsWith("✅")) Color(0xFF4CAF50) else Color.Gray,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), MaterialTheme.shapes.small)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 最新识别结果
            if (latestRecords.isNotEmpty()) {
                latestRecords.forEach { record ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.9f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = record.productName,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "数量: ${record.quantity} 箱 | 时间: ${record.time}",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                        }
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
                onClick = {
                    if (!isDetecting) {
                        isDetecting = true
                        statusText = "🔍 正在识别..."
                        latestRecords = emptyList()
                        triggerDetection = true
                    }
                },
                enabled = !isDetecting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDetecting) Color.Gray else Color(0xFF2196F3)
                ),
                modifier = Modifier.weight(1f)
            ) {
                if (isDetecting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Text("识别中...", fontSize = 16.sp)
                    }
                } else {
                    Text("📷 识别", fontSize = 16.sp)
                }
            }
            
            // 记录按钮
            Button(
                onClick = { showHistory = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("📋 记录 (${allRecords.size})", fontSize = 16.sp)
            }
        }
        
        // 历史记录对话框
        if (showHistory) {
            HistoryDialog(
                records = allRecords,
                onDismiss = { showHistory = false },
                onClear = { 
                    objectDetector.clearRecords()
                    allRecords = emptyList()
                }
            )
        }
    }
}

@Composable
fun CameraPreviewWithAnalysis(
    modifier: Modifier = Modifier,
    triggerDetection: Boolean,
    onDetectionComplete: (List<ScanRecord>) -> Unit,
    objectDetector: ObjectDetector
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    // 用于暂存检测触发状态
    var shouldDetect by remember { mutableStateOf(false) }
    
    // 监听触发信号
    LaunchedEffect(triggerDetection) {
        if (triggerDetection) {
            shouldDetect = true
        }
    }
    
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                // 预览
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                
                // 图像分析（用于识别）
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(
                            Executors.newSingleThreadExecutor()
                        ) { imageProxy ->
                            // 检查是否需要检测
                            if (shouldDetect) {
                                shouldDetect = false  // 重置标志
                                
                                // 执行检测
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val records = objectDetector.detectFromImage(imageProxy)
                                        
                                        withContext(Dispatchers.Main) {
                                            onDetectionComplete(records)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        imageProxy.close()
                                        withContext(Dispatchers.Main) {
                                            onDetectionComplete(emptyList())
                                        }
                                    }
                                }
                            } else {
                                // 不检测时也要关闭 imageProxy
                                imageProxy.close()
                            }
                        }
                    }
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
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
fun HistoryDialog(
    records: List<ScanRecord>,
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
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
