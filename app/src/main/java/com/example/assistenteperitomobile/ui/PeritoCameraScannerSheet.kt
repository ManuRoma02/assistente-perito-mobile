package com.example.assistenteperitomobile.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.assistenteperitomobile.theme.LocalPeritoSemanticColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val ScannerDarkSurface = Color(0xFF141110)
private val ScannerDarkControl = Color(0xFF2B2625)
private val ScannerDarkPill = Color(0xFF2B2423)
private val ScannerGoldAccent = Color(0xFFF5D0A9)
private val ScannerLaserGreen = Color(0xFF34C759)

@Composable
fun InAppCameraScannerSheet(
    onCaptureRealBitmap: (Bitmap, Int) -> Unit,
    onCaptureBurstFiles: ((List<File>) -> Unit)? = null,
    onOpenSystemCamera: () -> Unit,
    onPickFromGallery: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val semanticColors = LocalPeritoSemanticColors.current
    val animationsEnabled = remember(context) { PeritoMotionTokens.areAnimationsEnabled(context) }

    var selectedScanMode by rememberSaveable { mutableIntStateOf(1) }
    var isBurstMode by rememberSaveable { mutableStateOf(false) }
    val burstFileList = remember { mutableStateListOf<File>() }
    var flashEnabled by rememberSaveable { mutableStateOf(true) }
    var shutterFlashAlpha by remember { mutableFloatStateOf(0f) }
    var isCapturing by remember { mutableStateOf(false) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var isScannerDisposed by remember { mutableStateOf(false) }
    var burstShotCounter by remember { mutableIntStateOf(0) }
    val activeBurstSaveJobs = remember { mutableListOf<Job>() }
    val scope = rememberCoroutineScope()
    var isLowLight by remember { mutableStateOf<Boolean?>(null) }

    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }

    DisposableEffect(Unit) {
        var lastAnalysisUpdateMs = 0L
        imageAnalysis.setAnalyzer(Dispatchers.Default.asExecutor()) { imageProxy ->
            try {
                if (isScannerDisposed) return@setAnalyzer
                val now = System.currentTimeMillis()
                if (now - lastAnalysisUpdateMs >= 400L) {
                    lastAnalysisUpdateMs = now
                    val yPlane = imageProxy.planes.firstOrNull()
                    if (yPlane != null) {
                        val buffer = yPlane.buffer
                        val remaining = buffer.remaining()
                        if (remaining > 0) {
                            var sum = 0L
                            var samples = 0
                            val step = 16
                            var idx = 0
                            while (idx < remaining) {
                                sum += (buffer.get(idx).toInt() and 0xFF)
                                samples++
                                idx += step
                            }
                            if (samples > 0) {
                                val avgLuma = sum.toFloat() / samples.toFloat()
                                val lowLight = avgLuma < 65f
                                scope.launch(Dispatchers.Main) {
                                    if (!isScannerDisposed) {
                                        isLowLight = lowLight
                                    }
                                }
                            }
                        }
                    }
                }
            } finally {
                runCatching { imageProxy.close() }
            }
        }
        onDispose {
            isScannerDisposed = true
            runCatching { imageAnalysis.clearAnalyzer() }
            runCatching { cameraProviderRef?.unbindAll() }
            cameraProviderRef = null
            burstFileList.forEach { runCatching { it.delete() } }
            burstFileList.clear()
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val camPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            try {
                camPermLauncher.launch(Manifest.permission.CAMERA)
            } catch (_: Throwable) {}
        }
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    LaunchedEffect(flashEnabled) {
        imageCapture.flashMode = if (flashEnabled) {
            ImageCapture.FLASH_MODE_AUTO
        } else {
            ImageCapture.FLASH_MODE_OFF
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser_anim")
    val laserProgress = infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScannerDarkSurface)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics { testTag = "in_app_camera_scanner_sheet" },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Scanner Peritale & OCR",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Fotocamera Live • ML Kit OCR On-Device",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        flashEnabled = !flashEnabled
                    },
                    shape = RoundedCornerShape(50),
                    color = if (flashEnabled) ScannerGoldAccent else ScannerDarkControl,
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Flash",
                            tint = if (flashEnabled) ScannerDarkSurface else Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (flashEnabled) "Auto" else "Off",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (flashEnabled) ScannerDarkSurface else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onClose()
                    },
                    shape = CircleShape,
                    color = ScannerDarkControl,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Mirino Live CameraX Reale + Guida Ottica CAD Leggera + ImageAnalysis Luminance Readiness
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF121010))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(26.dp))
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                if (isScannerDisposed) {
                                    cameraProvider.unbindAll()
                                    return@addListener
                                }
                                cameraProviderRef = cameraProvider
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                cameraProvider.unbindAll()
                                try {
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageCapture,
                                        imageAnalysis
                                    )
                                } catch (_: Throwable) {
                                    try {
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview,
                                            imageCapture
                                        )
                                    } catch (_: Throwable) {
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_FRONT_CAMERA,
                                            preview,
                                            imageCapture
                                        )
                                    }
                                }
                            } catch (_: Throwable) {}
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = ScannerGoldAccent,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Consenti l'accesso alla Fotocamera per inquadrare documenti e rilievi peritali dal vivo",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { camPermLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Attiva Fotocamera Live", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val padX = w * 0.10f
                val padY = h * 0.14f
                val cornerLen = 28.dp.toPx()
                val strokeW = 3.dp.toPx()
                val bracketColor = ScannerGoldAccent

                drawLine(bracketColor, Offset(padX, padY), Offset(padX + cornerLen, padY), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(padX, padY), Offset(padX, padY + cornerLen), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(w - padX, padY), Offset(w - padX - cornerLen, padY), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(w - padX, padY), Offset(w - padX, padY + cornerLen), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(padX, h - padY), Offset(padX + cornerLen, h - padY), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(padX, h - padY), Offset(padX, h - padY - cornerLen), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(w - padX, h - padY), Offset(w - padX - cornerLen, h - padY), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(w - padX, h - padY), Offset(w - padX, h - padY - cornerLen), strokeW, StrokeCap.Round)

                val laserY = h * (if (animationsEnabled) laserProgress.value else 0.5f)
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            ScannerLaserGreen.copy(alpha = 0.85f),
                            ScannerGoldAccent,
                            ScannerLaserGreen.copy(alpha = 0.85f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(padX, laserY),
                    end = Offset(w - padX, laserY),
                    strokeWidth = 2.2.dp.toPx()
                )
            }

            // Badge Superiore: Modalità Attiva
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xCC191716),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp, start = 12.dp, end = 12.dp)
            ) {
                Text(
                    text = if (isBurstMode) {
                        "⚡ Raffica Rilievi: scatta a ripetizione, poi premi 'Salva Raffica'"
                    } else when (selectedScanMode) {
                        1 -> "📐 Inquadra Atto / Verbale CTU (OCR Automatico)"
                        2 -> "🔍 Inquadra Danno / Infiltrazione / Rilievo"
                        else -> "📐 Inquadra Planimetria / Computo Metrico"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = ScannerGoldAccent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }

            // Badge Inferiore Readiness Luminanza (ImageAnalysis)
            isLowLight?.let { lowLight ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xCC191716),
                    border = BorderStroke(
                        0.8.dp,
                        if (lowLight) semanticColors.warning.copy(alpha = 0.75f) else semanticColors.success.copy(alpha = 0.75f)
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .semantics { testTag = "scanner_readiness_badge" }
                ) {
                    Text(
                        text = if (lowLight) "🌙 Luce bassa" else "☀️ Inquadratura nitida",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (lowLight) ScannerGoldAccent else Color(0xFF9DF0B6),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            if (shutterFlashAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = shutterFlashAlpha))
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ScannerModePill(
                label = "⚖️ Verbale",
                selected = !isBurstMode && selectedScanMode == 1,
                modifier = Modifier.weight(1f),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    isBurstMode = false
                    selectedScanMode = 1
                }
            )
            ScannerModePill(
                label = "🧱 Danni",
                selected = !isBurstMode && selectedScanMode == 2,
                modifier = Modifier.weight(1f),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    isBurstMode = false
                    selectedScanMode = 2
                }
            )
            ScannerModePill(
                label = "📐 Misure",
                selected = !isBurstMode && selectedScanMode == 3,
                modifier = Modifier.weight(1f),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    isBurstMode = false
                    selectedScanMode = 3
                }
            )
            ScannerModePill(
                label = if (burstFileList.isEmpty()) "⚡ Raffica" else "⚡ (${burstFileList.size})",
                selected = isBurstMode,
                modifier = Modifier.weight(1.15f),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    isBurstMode = !isBurstMode
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onPickFromGallery()
                },
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                modifier = Modifier.heightIn(min = 44.dp)
            ) {
                Text("🖼️ Galleria", color = Color.White, fontSize = 12.sp)
            }

            val shutterInteraction = remember { MutableInteractionSource() }
            Surface(
                onClick = {
                    if (isCapturing) return@Surface
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    scope.launch {
                        shutterFlashAlpha = 0.85f
                        delay(110L)
                        shutterFlashAlpha = 0f
                    }
                    if (hasCameraPermission) {
                        isCapturing = true
                        imageCapture.takePicture(
                            Dispatchers.IO.asExecutor(),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    try {
                                        val finalBitmap = try {
                                            val rawBitmap = image.toBitmap()
                                            val rotation = image.imageInfo.rotationDegrees
                                            if (rotation != 0) {
                                                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                                                val rotated = Bitmap.createBitmap(
                                                    rawBitmap,
                                                    0,
                                                    0,
                                                    rawBitmap.width,
                                                    rawBitmap.height,
                                                    matrix,
                                                    true
                                                )
                                                rawBitmap.recycle()
                                                rotated
                                            } else {
                                                rawBitmap
                                            }
                                        } finally {
                                            runCatching { image.close() }
                                        }
                                        scope.launch(Dispatchers.Main) {
                                            isCapturing = false
                                            if (isScannerDisposed) {
                                                runCatching { finalBitmap.recycle() }
                                                return@launch
                                            }
                                            if (isBurstMode) {
                                                val shotIndex = ++burstShotCounter
                                                val saveJob = scope.launch(Dispatchers.IO) {
                                                    try {
                                                        val burstDir = File(context.cacheDir, "camera_burst").apply { mkdirs() }
                                                        val burstFile = File(burstDir, "burst_${System.currentTimeMillis()}_$shotIndex.jpg")
                                                        burstFile.outputStream().use { out ->
                                                            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                                                        }
                                                        finalBitmap.recycle()
                                                        withContext(Dispatchers.Main) {
                                                            if (isScannerDisposed) {
                                                                runCatching { burstFile.delete() }
                                                            } else {
                                                                burstFileList.add(burstFile)
                                                                Toast.makeText(context, "📸 Foto #${burstFileList.size} acquisita!", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    } catch (_: Throwable) {
                                                        runCatching { finalBitmap.recycle() }
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(context, "Errore salvataggio foto raffica", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                                activeBurstSaveJobs.add(saveJob)
                                                saveJob.invokeOnCompletion {
                                                    scope.launch(Dispatchers.Main) {
                                                        activeBurstSaveJobs.remove(saveJob)
                                                    }
                                                }
                                            } else {
                                                onCaptureRealBitmap(finalBitmap, selectedScanMode)
                                            }
                                        }
                                    } catch (_: Throwable) {
                                        runCatching { image.close() }
                                        scope.launch(Dispatchers.Main) {
                                            isCapturing = false
                                            Toast.makeText(context, "Errore scatto fotocamera", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    scope.launch(Dispatchers.Main) {
                                        isCapturing = false
                                        Toast.makeText(context, "Impossibile acquisire la foto", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    } else {
                        camPermLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                interactionSource = shutterInteraction,
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(4.dp, Color.White),
                modifier = Modifier
                    .size(74.dp)
                    .emilPressScale(shutterInteraction, pressedScale = 0.90f)
                    .semantics {
                        contentDescription = "Scatta scansione rilievo o documento"
                        testTag = "in_app_camera_shutter_button"
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onOpenSystemCamera()
                },
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                modifier = Modifier.heightIn(min = 44.dp)
            ) {
                Text("📷 Sistema", color = Color.White, fontSize = 12.sp)
            }
        }

        if (isBurstMode && burstFileList.isNotEmpty()) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    scope.launch {
                        activeBurstSaveJobs.toList().joinAll()
                        val batch = burstFileList.toList()
                        burstFileList.clear()
                        if (batch.isNotEmpty()) {
                            onCaptureBurstFiles?.invoke(batch)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(
                    text = "📁 Salva Raffica (${burstFileList.size} foto) nel Fascicolo",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 13.5.sp
                )
            }
        }
    }
}

@Composable
private fun ScannerModePill(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else ScannerDarkPill,
        border = BorderStroke(1.dp, if (selected) ScannerGoldAccent else Color.White.copy(alpha = 0.12f)),
        modifier = modifier.heightIn(min = 44.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
