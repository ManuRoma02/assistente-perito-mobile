package com.example.assistenteperitomobile.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.assistenteperitomobile.theme.LocalPeritoSemanticColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

private fun savePcmAsWavFile(
    context: android.content.Context,
    pcmBytes: ByteArray,
    sampleRate: Int = 16000
): String? {
    if (pcmBytes.isEmpty()) return null
    return try {
        val dir = File(context.filesDir, "voice_notes").apply { mkdirs() }
        val wavFile = File(dir, "voice_${System.currentTimeMillis()}.wav")
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val blockAlign = channels * (bitsPerSample / 8)
        val dataSize = pcmBytes.size
        val chunkSize = 36 + dataSize

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(chunkSize)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1.toShort())
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign.toShort())
            putShort(bitsPerSample.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
        }.array()

        FileOutputStream(wavFile).use { fos ->
            fos.write(header)
            fos.write(pcmBytes)
            fos.flush()
        }
        wavFile.absolutePath
    } catch (_: Throwable) {
        null
    }
}

@Composable
fun InAppVoiceRecordingBar(
    onCancel: () -> Unit,
    onSendVoiceNote: (String, Int, String?) -> Unit,
    @Suppress("UNUSED_PARAMETER") onOpenSystemDictation: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val semanticColors = LocalPeritoSemanticColors.current
    val animationsEnabled = remember(context) { PeritoMotionTokens.areAnimationsEnabled(context) }
    val coroutineScope = rememberCoroutineScope()
    var elapsedSeconds by remember { mutableIntStateOf(1) }
    var liveTranscript by remember { mutableStateOf("") }
    var partialSegment by remember { mutableStateOf("") }
    var statusHint by remember { mutableStateOf<String?>(null) }
    var isFlushingSend by remember { mutableStateOf(false) }
    var recognizerInstance by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var rmsDb by remember { mutableFloatStateOf(4.5f) }
    val pcmBuffer = remember { ByteArrayOutputStream(16000 * 2 * 15) }
    var stopAudioRecordFn by remember { mutableStateOf<(() -> Unit)?>(null) }
    var resumeVoiceCaptureFn by remember { mutableStateOf<(() -> Unit)?>(null) }
    var hasAudioPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            elapsedSeconds += 1
        }
    }

    val audioPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPerm = granted
    }

    LaunchedEffect(Unit) {
        if (!hasAudioPerm) {
            try {
                audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } catch (_: Throwable) {}
        }
    }

    DisposableEffect(hasAudioPerm) {
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        var isDisposed = false
        var isRecordingAudio = true
        var usePipeAudioSource = android.os.Build.VERSION.SDK_INT >= 33
        var currentPipeSink: ParcelFileDescriptor.AutoCloseOutputStream? = null
        var currentPipeReadFd: ParcelFileDescriptor? = null
        var audioRecord: AudioRecord? = null
        var recordThread: Thread? = null

        fun closeCurrentPipe() {
            try { currentPipeSink?.close() } catch (_: Throwable) {}
            try { currentPipeReadFd?.close() } catch (_: Throwable) {}
            currentPipeSink = null
            currentPipeReadFd = null
        }

        fun startAudioRecordCapture() {
            if (!hasAudioPerm || isDisposed || audioRecord != null) return
            isRecordingAudio = true
            try {
                val sampleRate = 16000
                val minBuf = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(3200)
                @Suppress("MissingPermission")
                val ar = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf * 2
                )
                if (ar.state == AudioRecord.STATE_INITIALIZED) {
                    ar.startRecording()
                    audioRecord = ar
                    recordThread = Thread {
                        val readBuf = ByteArray(1600)
                        var lastDbUpdateMs = 0L
                        while (isRecordingAudio && !isDisposed) {
                            val readBytes = try {
                                ar.read(readBuf, 0, readBuf.size)
                            } catch (_: Throwable) {
                                -1
                            }
                            if (readBytes > 0) {
                                synchronized(pcmBuffer) {
                                    pcmBuffer.write(readBuf, 0, readBytes)
                                }
                                val sink = currentPipeSink
                                if (sink != null) {
                                    try {
                                        sink.write(readBuf, 0, readBytes)
                                        sink.flush()
                                    } catch (_: Throwable) {}
                                }
                                var sumSq = 0.0
                                val samples = readBytes / 2
                                if (samples > 0) {
                                    var idx = 0
                                    while (idx + 1 < readBytes) {
                                        val lo = readBuf[idx].toInt() and 0xFF
                                        val hi = readBuf[idx + 1].toInt()
                                        val sample = ((hi shl 8) or lo).toShort().toDouble()
                                        sumSq += sample * sample
                                        idx += 2
                                    }
                                    val now = System.currentTimeMillis()
                                    if (now - lastDbUpdateMs >= 60L) {
                                        lastDbUpdateMs = now
                                        val rms = sqrt(sumSq / samples)
                                        val mappedDb = ((rms / 1800.0) * 10.0 + 2.0).toFloat().coerceIn(1.5f, 12f)
                                        mainHandler.post { rmsDb = mappedDb }
                                    }
                                }
                            } else {
                                try { Thread.sleep(15L) } catch (_: Throwable) {}
                            }
                        }
                    }.apply {
                        name = "PeritoRealVoiceRecorder"
                        isDaemon = true
                        start()
                    }
                } else {
                    ar.release()
                    usePipeAudioSource = false
                }
            } catch (_: Throwable) {
                runCatching { audioRecord?.stop() }
                runCatching { audioRecord?.release() }
                audioRecord = null
                usePipeAudioSource = false
            }
        }

        startAudioRecordCapture()

        stopAudioRecordFn = {
            isRecordingAudio = false
            closeCurrentPipe()
            try { audioRecord?.stop() } catch (_: Throwable) {}
            try { recordThread?.join(250L) } catch (_: Throwable) {}
            try { audioRecord?.release() } catch (_: Throwable) {}
            audioRecord = null
            recordThread = null
        }

        fun buildSpeechIntent(): Intent {
            closeCurrentPipe()
            return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "it-IT")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000L)
                if (usePipeAudioSource && android.os.Build.VERSION.SDK_INT >= 33 && audioRecord != null) {
                    try {
                        val pipe = ParcelFileDescriptor.createPipe()
                        currentPipeReadFd = pipe[0]
                        currentPipeSink = ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])
                        putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, pipe[0])
                        putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
                        putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                        putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, 16000)
                    } catch (_: Throwable) {
                        usePipeAudioSource = false
                    }
                }
            }
        }

        var consecutiveErrorRetries = 0
        val maxSpeechRetries = 6

        val recognizer = try {
            if (hasAudioPerm) {
                val sr = try {
                    SpeechRecognizer.createSpeechRecognizer(context)
                } catch (_: Throwable) {
                    if (android.os.Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                        SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                    } else null
                }
                sr?.apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            consecutiveErrorRetries = 0
                            statusHint = null
                        }

                        override fun onBeginningOfSpeech() {
                            consecutiveErrorRetries = 0
                            statusHint = null
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            if (audioRecord == null) {
                                rmsDb = rmsdB.coerceIn(1f, 12f)
                            }
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {}

                        override fun onError(error: Int) {
                            if (isDisposed) return
                            closeCurrentPipe()
                            val wasPipeEnabled = usePipeAudioSource
                            if (wasPipeEnabled && (error == SpeechRecognizer.ERROR_AUDIO || error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY)) {
                                usePipeAudioSource = false
                            }
                            if (partialSegment.isNotBlank()) {
                                val seg = partialSegment.trim()
                                liveTranscript = if (liveTranscript.isBlank()) seg else "$liveTranscript $seg"
                                partialSegment = ""
                            }
                            val isFatalError = error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
                                (!wasPipeEnabled && (
                                    error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                                        error == SpeechRecognizer.ERROR_AUDIO ||
                                        error == SpeechRecognizer.ERROR_CLIENT
                                    ))
                            if (isFatalError) {
                                statusHint = if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                                    "🎙️ Permesso microfono necessario per la dettatura"
                                } else {
                                    "🎙️ Microfono occupato o non disponibile"
                                }
                                return
                            }
                            if (!isFlushingSend && consecutiveErrorRetries < maxSpeechRetries) {
                                consecutiveErrorRetries += 1
                                mainHandler.postDelayed({
                                    if (!isDisposed && !isFlushingSend) {
                                        try {
                                            startListening(buildSpeechIntent())
                                        } catch (_: Throwable) {}
                                    }
                                }, 180L)
                            } else if (!isFlushingSend) {
                                statusHint = "🎙️ Riconoscimento vocale in pausa: premi Invia o riprova"
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            if (isDisposed) return
                            consecutiveErrorRetries = 0
                            closeCurrentPipe()
                            val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val phrase = list?.firstOrNull()?.trim().orEmpty()
                            if (phrase.isNotEmpty()) {
                                liveTranscript = if (liveTranscript.isBlank()) phrase else "$liveTranscript $phrase"
                            } else if (partialSegment.isNotBlank()) {
                                val seg = partialSegment.trim()
                                liveTranscript = if (liveTranscript.isBlank()) seg else "$liveTranscript $seg"
                            }
                            partialSegment = ""
                            if (!isFlushingSend) {
                                mainHandler.postDelayed({
                                    if (!isDisposed && !isFlushingSend) {
                                        try {
                                            startListening(buildSpeechIntent())
                                        } catch (_: Throwable) {}
                                    }
                                }, 140L)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            if (isDisposed) return
                            val stableList = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val unstableList = partialResults?.getStringArrayList("android.speech.extra.UNSTABLE_TEXT")
                            val stablePart = stableList?.firstOrNull()?.trim().orEmpty()
                            val unstablePart = unstableList?.firstOrNull()?.trim().orEmpty()
                            val combinedPart = when {
                                stablePart.isNotEmpty() && unstablePart.isNotEmpty() -> "$stablePart $unstablePart"
                                stablePart.isNotEmpty() -> stablePart
                                else -> unstablePart
                            }.trim()
                            if (combinedPart.isNotEmpty()) {
                                partialSegment = combinedPart
                                statusHint = null
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                    startListening(buildSpeechIntent())
                }
            } else null
        } catch (_: Throwable) {
            null
        }
        recognizerInstance = recognizer
        resumeVoiceCaptureFn = {
            if (!isDisposed && hasAudioPerm) {
                consecutiveErrorRetries = 0
                startAudioRecordCapture()
                try {
                    recognizer?.startListening(buildSpeechIntent())
                } catch (_: Throwable) {}
            }
        }

        onDispose {
            isDisposed = true
            isRecordingAudio = false
            mainHandler.removeCallbacksAndMessages(null)
            recognizerInstance = null
            stopAudioRecordFn = null
            resumeVoiceCaptureFn = null
            closeCurrentPipe()
            try { audioRecord?.stop() } catch (_: Throwable) {}
            try { recordThread?.join(250L) } catch (_: Throwable) {}
            try { audioRecord?.release() } catch (_: Throwable) {}
            audioRecord = null
            recordThread = null
            try {
                recognizer?.stopListening()
                recognizer?.cancel()
                recognizer?.destroy()
            } catch (_: Throwable) {}
        }
    }

    val combinedTranscript = remember(liveTranscript, partialSegment) {
        when {
            liveTranscript.isNotBlank() && partialSegment.isNotBlank() -> "$liveTranscript $partialSegment"
            partialSegment.isNotBlank() -> partialSegment
            else -> liveTranscript
        }.trim()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "voice_wave_transition")
    val wavePhase = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "voice_wave_phase"
    )
    val redDotAlpha = infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "red_dot_pulse"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .semantics { testTag = "in_app_voice_recording_bar" },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = "🎙️ Registrazione Vocale • Trascrizione in Diretta",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            if (combinedTranscript.isNotBlank()) {
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        liveTranscript = ""
                        partialSegment = ""
                    },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = "↺ Pulisci testo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when {
                    combinedTranscript.isNotBlank() -> "“$combinedTranscript”"
                    statusHint != null -> statusHint!!
                    hasAudioPerm -> "🎙️ Parla ora... registriamo la tua voce reale e trascriviamo in diretta"
                    else -> "🎙️ Consenti il permesso microfono per registrare e trascrivere in-app"
                },
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    combinedTranscript.isNotBlank() -> MaterialTheme.colorScheme.onSurface
                    statusHint != null -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = if (combinedTranscript.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val cancelInteraction = remember { MutableInteractionSource() }
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Reject)
                    stopAudioRecordFn?.invoke()
                    onCancel()
                },
                interactionSource = cancelInteraction,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .size(44.dp)
                    .emilPressScale(cancelInteraction)
                    .semantics {
                        contentDescription = "Annulla registrazione vocale"
                        testTag = "cancel_voice_note_button"
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Annulla",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = semanticColors.brandSoft,
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f)),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val dotAlpha = if (animationsEnabled) redDotAlpha.value else 1f
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = dotAlpha))
                    )
                    val mins = elapsedSeconds / 60
                    val secs = elapsedSeconds % 60
                    Text(
                        text = String.format(Locale.ITALIAN, "%d:%02d", mins, secs),
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                        color = primaryColor,
                        fontWeight = FontWeight.Bold
                    )
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                    ) {
                        val barCount = 24
                        val spacing = size.width / barCount
                        val barWidth = (spacing * 0.56f).coerceAtLeast(3f)
                        val phase = if (animationsEnabled) wavePhase.value else 0f
                        for (i in 0 until barCount) {
                            val norm = sin(phase + i * 0.45f) * 0.5f + 0.5f
                            val boost = (rmsDb / 8f).coerceIn(0.4f, 1.3f)
                            val barH = (size.height * (0.22f + 0.72f * norm * boost)).coerceIn(4.dp.toPx(), size.height)
                            val x = i * spacing + (spacing - barWidth) / 2f
                            val y = (size.height - barH) / 2f
                            drawRoundRect(
                                color = if (i % 3 == 0) primaryColor else secondaryColor.copy(alpha = 0.75f),
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barH),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )
                        }
                    }
                }
            }

            val sendInteraction = remember { MutableInteractionSource() }
            Surface(
                onClick = {
                    if (isFlushingSend) return@Surface
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    val immediateText = combinedTranscript.trim()
                    if (immediateText.isNotBlank()) {
                        isFlushingSend = true
                        val durationSnapshot = elapsedSeconds
                        stopAudioRecordFn?.invoke()
                        try { recognizerInstance?.stopListening() } catch (_: Throwable) {}
                        val wavBytes = synchronized(pcmBuffer) { pcmBuffer.toByteArray() }
                        coroutineScope.launch {
                            val savedWavPath = withContext(Dispatchers.IO) {
                                savePcmAsWavFile(context, wavBytes, 16000)
                            }
                            onSendVoiceNote(immediateText, durationSnapshot, savedWavPath)
                        }
                    } else {
                        isFlushingSend = true
                        statusHint = "⏳ Elaborazione ultime parole..."
                        stopAudioRecordFn?.invoke()
                        try { recognizerInstance?.stopListening() } catch (_: Throwable) {}
                        coroutineScope.launch {
                            var waited = 0
                            while (waited < 650 && liveTranscript.isBlank() && partialSegment.isBlank()) {
                                delay(65L)
                                waited += 65
                            }
                            val flushed = when {
                                liveTranscript.isNotBlank() && partialSegment.isNotBlank() -> "$liveTranscript $partialSegment"
                                partialSegment.isNotBlank() -> partialSegment
                                else -> liveTranscript
                            }.trim()
                            if (flushed.isNotBlank()) {
                                val durationSnapshot = elapsedSeconds
                                val wavBytes = synchronized(pcmBuffer) { pcmBuffer.toByteArray() }
                                val savedWavPath = withContext(Dispatchers.IO) {
                                    savePcmAsWavFile(context, wavBytes, 16000)
                                }
                                onSendVoiceNote(flushed, durationSnapshot, savedWavPath)
                            } else {
                                isFlushingSend = false
                                statusHint = "🎙️ Nessuna voce rilevata: parla nel microfono e premi Invia"
                                resumeVoiceCaptureFn?.invoke()
                            }
                        }
                    }
                },
                interactionSource = sendInteraction,
                shape = CircleShape,
                color = primaryColor,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .size(44.dp)
                    .emilPressScale(sendInteraction)
                    .semantics {
                        contentDescription = "Invia nota vocale"
                        testTag = "send_voice_note_button"
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Invia Nota Vocale",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
