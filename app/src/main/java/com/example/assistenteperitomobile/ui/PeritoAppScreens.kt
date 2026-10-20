package com.example.assistenteperitomobile.ui

import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.filled.Email
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.assistenteperitomobile.domain.MainAppTab
import com.example.assistenteperitomobile.domain.PeritoEventCategory
import com.example.assistenteperitomobile.domain.StudioProfile
import com.example.assistenteperitomobile.domain.SystemCalendarInfo
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.assistenteperitomobile.R
import com.example.assistenteperitomobile.data.DownloadableAiModel
import com.example.assistenteperitomobile.data.ota.OtaUpdateManager
import com.example.assistenteperitomobile.data.ota.OtaUpdateManifest
import com.example.assistenteperitomobile.domain.ArchivedDocument
import com.example.assistenteperitomobile.domain.CalendarEventItem
import com.example.assistenteperitomobile.domain.ChatMessage
import com.example.assistenteperitomobile.domain.PeritoCategory
import com.example.assistenteperitomobile.theme.AppleBlue
import com.example.assistenteperitomobile.theme.AppleBlueDark
import com.example.assistenteperitomobile.theme.AppleBlueSoft
import com.example.assistenteperitomobile.theme.AppleGreen
import com.example.assistenteperitomobile.theme.AppleGreenSoft
import com.example.assistenteperitomobile.theme.AppleGroupedBg
import com.example.assistenteperitomobile.theme.AppleLabelPrimary
import com.example.assistenteperitomobile.theme.AppleLabelSecondary
import com.example.assistenteperitomobile.theme.AppleLabelTertiary
import com.example.assistenteperitomobile.theme.AppleOrange
import com.example.assistenteperitomobile.theme.AppleOrangeSoft
import com.example.assistenteperitomobile.theme.AppleRed
import com.example.assistenteperitomobile.theme.AppleRedSoft
import com.example.assistenteperitomobile.theme.AppleSecondaryGroupedBg
import com.example.assistenteperitomobile.theme.AppleWhite
import com.example.assistenteperitomobile.theme.DavezAlabaster
import com.example.assistenteperitomobile.theme.DavezBordeaux
import com.example.assistenteperitomobile.theme.DavezBordeauxHover
import com.example.assistenteperitomobile.theme.DavezBordeauxSoft
import com.example.assistenteperitomobile.theme.DavezFotografiaSlate
import com.example.assistenteperitomobile.theme.DavezStoneBorder
import com.example.assistenteperitomobile.theme.DavezTravertine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

// =============================================================================
// 1. EMIL KOWALSKI MICRO-INTERACTIONS (/emilkowalski-animation)
// GPU-accelerated Draw-phase scale feedback + Reduced Motion compliance
// =============================================================================
fun Modifier.emilPressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.96f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = if (animationsEnabled) {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        } else {
            snap()
        },
        label = "emil_press_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * 12 Disney Principles + Emil Kowalski Physics: Cartoon Squash-and-Stretch (/emilkowalski-animation).
 * Squashes vertically (scaleY < 1f) and expands horizontally (scaleX > 1f) on press,
 * then overshoots with an elastic cartoon bounce on release on the GPU graphicsLayer.
 */
fun Modifier.cartoonPressSquash(
    interactionSource: InteractionSource,
    pressedScaleY: Float = 0.92f,
    squashScaleX: Float = 1.05f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val scaleY by animateFloatAsState(
        targetValue = if (isPressed) pressedScaleY else 1f,
        animationSpec = if (animationsEnabled) {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        } else snap(),
        label = "cartoon_squash_y"
    )
    val scaleX by animateFloatAsState(
        targetValue = if (isPressed) squashScaleX else 1f,
        animationSpec = if (animationsEnabled) {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        } else snap(),
        label = "cartoon_squash_x"
    )
    this.graphicsLayer {
        this.scaleX = scaleX
        this.scaleY = scaleY
    }
}

// =============================================================================
// 2. 3D PERSPECTIVE NEURAL ORB & ARCHITECTURAL PRISM (/threejs + davez Palette)
// Real-time 60fps 3D scene graph with Fibonacci sphere nodes, isometric house
// prism wireframe, z-depth sorting, radial bloom & touch OrbitControls in the
// exact Mario Rossi ("davez") Bordeaux (#4F0B18) & Alabaster (#F8F4F1) style!
// =============================================================================
private data class Vec3(val x: Float, val y: Float, val z: Float)

private val ArchitecturalHouseVertices = listOf(
    Vec3(-0.48f, 0.42f, -0.48f), // 0: base FL
    Vec3(0.48f, 0.42f, -0.48f),  // 1: base FR
    Vec3(0.48f, 0.42f, 0.48f),   // 2: base BR
    Vec3(-0.48f, 0.42f, 0.48f),  // 3: base BL
    Vec3(-0.48f, -0.12f, -0.48f),// 4: wall top FL
    Vec3(0.48f, -0.12f, -0.48f), // 5: wall top FR
    Vec3(0.48f, -0.12f, 0.48f),  // 6: wall top BR
    Vec3(-0.48f, -0.12f, 0.48f), // 7: wall top BL
    Vec3(0f, -0.68f, 0f)         // 8: roof apex (caliper crown)
)

private val ArchitecturalHouseEdges = listOf(
    0 to 1, 1 to 2, 2 to 3, 3 to 0, // base square
    4 to 5, 5 to 6, 6 to 7, 7 to 4, // eaves square
    0 to 4, 1 to 5, 2 to 6, 3 to 7, // pillars
    4 to 8, 5 to 8, 6 to 8, 7 to 8  // roof pyramid
)

private val FibonacciSphereNodes: List<Vec3> = buildList {
    val count = 26
    val goldenAngle = PI.toFloat() * (3f - sqrt(5f))
    for (i in 0 until count) {
        val y = 1f - (i.toFloat() / (count - 1).toFloat()) * 2f
        val radius = sqrt((1f - y * y).coerceAtLeast(0f))
        val theta = goldenAngle * i
        add(Vec3(cos(theta) * radius, y, sin(theta) * radius))
    }
}

@Composable
fun Perito3DThinkingOrb(
    isThinking: Boolean,
    size: Dp = 54.dp,
    modifier: Modifier = Modifier
) {
    val phase: Float
    val pulse: Float
    if (isThinking) {
        val infiniteTransition = rememberInfiniteTransition(label = "orb_3d_rotation")
        val animatedPhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2400,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "orb_phase"
        )
        val animatedPulse by infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 550),
                repeatMode = RepeatMode.Reverse
            ),
            label = "orb_pulse"
        )
        phase = animatedPhase
        pulse = animatedPulse
    } else {
        phase = 0.35f
        pulse = 1.0f
    }

    // Interactive Touch OrbitControls (/threejs)
    var userYaw by remember { mutableFloatStateOf(0.35f) }
    var userPitch by remember { mutableFloatStateOf(-0.28f) }

    Canvas(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    userYaw += dragAmount.x * 0.015f
                    userPitch = (userPitch + dragAmount.y * 0.015f).coerceIn(-1.2f, 1.2f)
                }
            }
            .semantics {
                contentDescription = if (isThinking) "Nucleo 3D PeritoAI in elaborazione" else "Nucleo 3D PeritoAI pronto"
                testTag = "perito_3d_thinking_orb"
            }
    ) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = (this.size.minDimension / 2f) * 0.84f * pulse

        // 1. Ambient Volumetric Bloom Glow (davez Bordeaux & Warm Champagne)
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (isThinking) {
                    listOf(
                        DavezBordeauxHover.copy(alpha = 0.45f),
                        DavezBordeauxSoft.copy(alpha = 0.24f),
                        Color(0xFFF5D0A9).copy(alpha = 0.12f),
                        Color.Transparent
                    )
                } else {
                    listOf(
                        DavezBordeaux.copy(alpha = 0.26f),
                        DavezBordeauxSoft.copy(alpha = 0.12f),
                        Color.Transparent
                    )
                },
                center = Offset(cx, cy),
                radius = r * 1.22f
            ),
            radius = r * 1.22f,
            center = Offset(cx, cy)
        )

        // 2. Deep Imperial Bordeaux 3D Sphere Core (--brand-color #4F0B18 -> #6A1628)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF8C2D40),
                    DavezBordeauxHover,
                    DavezBordeaux,
                    Color(0xFF2B050C)
                ),
                center = Offset(cx - r * 0.25f, cy - r * 0.25f),
                radius = r * 1.05f
            ),
            radius = r * 0.88f,
            center = Offset(cx, cy)
        )

        val rotY = phase + userYaw
        val rotX = userPitch + sin(phase) * 0.18f
        val cosY = cos(rotY)
        val sinY = sin(rotY)
        val cosX = cos(rotX)
        val sinX = sin(rotX)
        val cameraDist = 2.6f

        // 3. Fibonacci Sphere Orbiting Neural Nodes in Alabaster Chalk & Champagne Gold (zero-allocation projection)
        val nodeScale = r * 0.72f
        FibonacciSphereNodes.forEachIndexed { idx, node ->
            val x1 = node.x * cosY - node.z * sinY
            val z1 = node.x * sinY + node.z * cosY
            val y2 = node.y * cosX - z1 * sinX
            val z2 = node.y * sinX + z1 * cosX
            val perspective = cameraDist / (cameraDist - z2).coerceAtLeast(0.8f)
            val px = cx + x1 * nodeScale * perspective
            val py = cy + y2 * nodeScale * perspective

            val depthAlpha = ((z2 + 1.1f) / 2.2f).coerceIn(0.18f, 0.95f)
            val nodeColor = if (isThinking && idx % 3 == 0) {
                Color(0xFFF5D0A9).copy(alpha = depthAlpha)
            } else {
                DavezAlabaster.copy(alpha = depthAlpha * 0.82f)
            }
            val dotRadius = if (z2 > 0f) 2.2.dp.toPx() else 1.3.dp.toPx()
            drawCircle(
                color = nodeColor,
                radius = dotRadius,
                center = Offset(px, py)
            )
        }

        // 4. 3D Isometric Architectural Blueprint House & Caliper Wireframe (primitive array projection, zero GC)
        val houseScale = r * 0.56f
        val numVertices = ArchitecturalHouseVertices.size
        val projX = FloatArray(numVertices)
        val projY = FloatArray(numVertices)
        val projZ = FloatArray(numVertices)
        for (i in 0 until numVertices) {
            val v = ArchitecturalHouseVertices[i]
            val x1 = v.x * cosY - v.z * sinY
            val z1 = v.x * sinY + v.z * cosY
            val y2 = v.y * cosX - z1 * sinX
            val z2 = v.y * sinX + z1 * cosX
            val perspective = cameraDist / (cameraDist - z2).coerceAtLeast(0.8f)
            projX[i] = cx + x1 * houseScale * perspective
            projY[i] = cy + y2 * houseScale * perspective
            projZ[i] = z2
        }

        ArchitecturalHouseEdges.forEach { (i, j) ->
            val avgZ = (projZ[i] + projZ[j]) * 0.5f
            val alpha = ((avgZ + 1.0f) / 1.8f).coerceIn(0.38f, 1.0f)
            val isRoofEdge = i == 8 || j == 8
            drawLine(
                color = if (isRoofEdge) DavezAlabaster.copy(alpha = alpha) else DavezTravertine.copy(alpha = alpha),
                start = Offset(projX[i], projY[i]),
                end = Offset(projX[j], projY[j]),
                strokeWidth = if (isRoofEdge) 1.8.dp.toPx() else 1.3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 5. Champagne Gold AI Spark at the Roof Apex
        drawCircle(
            color = Color(0xFFF5D0A9),
            radius = if (isThinking) 3.6.dp.toPx() else 2.6.dp.toPx(),
            center = Offset(projX[8], projY[8])
        )
        drawCircle(
            color = Color.White,
            radius = 1.4.dp.toPx(),
            center = Offset(projX[8], projY[8])
        )
    }
}

@Composable
fun PeritoAiBrandEmblem(
    size: Dp = 40.dp,
    showOnlineBadge: Boolean = true
) {
    Box(modifier = Modifier.size(size + 2.dp)) {
        Image(
            painter = painterResource(id = R.drawable.rd_logo_colore),
            contentDescription = "Logo PeritoAI RD",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(size)
        )
        if (showOnlineBadge) {
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(1.5.dp)
                    .clip(CircleShape)
                    .background(AppleGreen)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistentePeritoApp(viewModel: PeritoViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showArchiveSheet by rememberSaveable { mutableStateOf(false) }
    var showAiSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showInAppCameraSheet by rememberSaveable { mutableStateOf(false) }

    // Permessi Calendario + Notifiche
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.onPermissionsUpdated()
    }

    val requestCalendarAndNotificationPermissions = {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )
    }

    val handleCalendarConnect = {
        if (!uiState.hasNativeCalendarPermission) {
            requestCalendarAndNotificationPermissions()
        } else {
            viewModel.connectCalendar()
        }
    }

    // Fotocamera di sistema (fallback opzionale oltre allo Scanner Peritale In-App)
    val systemCameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.processCameraBitmap(bitmap)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                systemCameraLauncher.launch(null)
            } catch (_: Throwable) {
                Toast.makeText(context, "Fotocamera di sistema non disponibile", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(
                context,
                "Permesso fotocamera richiesto per scattare foto",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val safeLaunchSystemCamera = {
        val hasCamPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasCamPerm) {
            try {
                systemCameraLauncher.launch(null)
            } catch (_: Throwable) {
                Toast.makeText(context, "Fotocamera di sistema non disponibile", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Selettore File (PDF, .p7m, Immagini)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.processDocumentUri(uri)
        }
    }

    // Riconoscimento Vocale di Sistema (fallback opzionale)
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.sendVoiceNoteMessage(spokenText, durationSeconds = 4)
            }
        }
    }

    val triggerSystemVoiceInput = {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Detta l'appuntamento o il documento da cercare...")
            }
            speechLauncher.launch(intent)
        } catch (_: Throwable) {
            Toast.makeText(
                context,
                "Usa la barra di registrazione vocale in-app",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var inputFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var showAttachSheet by rememberSaveable { mutableStateOf(false) }
    var isRecordingVoice by rememberSaveable { mutableStateOf(false) }
    val inputFocusRequester = remember { FocusRequester() }

    // Sincronizzazione swipe orizzontale Pager -> ViewModel Tab
    LaunchedEffect(pagerState.currentPage) {
        val tab = if (pagerState.currentPage == 0) MainAppTab.CHAT else MainAppTab.AGENDA
        if (uiState.selectedTab != tab) {
            viewModel.selectTab(tab)
        }
    }

    // Sincronizzazione ViewModel Tab -> Pager
    LaunchedEffect(uiState.selectedTab) {
        val targetPage = if (uiState.selectedTab == MainAppTab.CHAT) 0 else 1
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    val prefillCommandInChat: (String) -> Unit = { prefix ->
        isRecordingVoice = false
        inputFieldValue = TextFieldValue(
            text = prefix,
            selection = TextRange(prefix.length)
        )
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
            delay(60L)
            try {
                inputFocusRequester.requestFocus()
                keyboardController?.show()
            } catch (_: Throwable) {}
        }
    }

    Scaffold(
        containerColor = DavezAlabaster,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            PeritoStudioTopBar(
                archivedCount = uiState.archivedDocuments.size,
                hasCalendarPermission = uiState.hasCalendarPermission,
                isProcessing = uiState.isProcessing,
                studioTitle = uiState.aiConfig.studioProfile.studioName,
                onOpenArchive = { showArchiveSheet = true },
                onOpenAiModels = { showAiSettingsSheet = true },
                onRequestCalendarPerm = handleCalendarConnect,
                onClearChat = if (uiState.selectedTab == MainAppTab.CHAT) {
                    { viewModel.clearChatHistory() }
                } else null
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DavezAlabaster)
                .padding(innerPadding)
                .imePadding()
        ) {
            // Toast/Banner di conferma stile iOS + davez Bordeaux
            AnimatedVisibility(
                visible = uiState.statusBanner != null,
                enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.96f),
                exit = fadeOut(tween(150))
            ) {
                uiState.statusBanner?.let { banner ->
                    Surface(
                        color = AppleBlueSoft,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = banner,
                                style = MaterialTheme.typography.labelMedium,
                                color = AppleBlueDark,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearStatusBanner() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Chiudi avviso",
                                    tint = AppleBlueDark,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Barra compatta non bloccante quando un modello IA si scarica in background
            AnimatedVisibility(
                visible = uiState.downloadProgress.isDownloading && !uiState.showFirstRunModelDialog && !showAiSettingsSheet,
                enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.98f),
                exit = fadeOut(tween(150))
            ) {
                val prog = uiState.downloadProgress
                val dlMb = String.format(Locale.ITALIAN, "%.1f", prog.downloadedBytes / (1024.0 * 1024.0))
                val totMb = if (prog.totalBytes > 0) {
                    String.format(Locale.ITALIAN, "%.1f MB", prog.totalBytes / (1024.0 * 1024.0))
                } else "..."
                val queueBadge = if (prog.totalQueueCount > 1) " (${prog.currentQueueIndex}/${prog.totalQueueCount})" else ""
                Surface(
                    onClick = { showAiSettingsSheet = true },
                    color = DavezAlabaster,
                    border = BorderStroke(0.5.dp, DavezStoneBorder.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⬇️ Download in background$queueBadge: ${prog.modelTitle}",
                                style = MaterialTheme.typography.labelMedium,
                                color = DavezBordeaux,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${prog.progressPercent}% ($dlMb/$totMb)",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppleLabelSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.width(6.dp))
                            TextButton(
                                onClick = { viewModel.cancelModelDownload() },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("Pausa", fontSize = 11.sp, color = AppleRed, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { prog.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = DavezBordeaux,
                            trackColor = AppleBlueSoft
                        )
                    }
                }
            }

            // CONTENUTO A SCORRIMENTO ORIZZONTALE (HORIZONTAL PAGER)
            // A sinistra (page 0): Assistente Chat
            // Scroll verso destra (page 1): Agenda Sopralluoghi
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> {
                        PeritoStudioChatScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onPrefillCommand = prefillCommandInChat,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    1 -> {
                        AgendaVisivaScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onRequestCalendarPerm = handleCalendarConnect,
                            onDirectVoiceClick = triggerSystemVoiceInput,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // BARRA DI SCRITTURA CON IL [+] DENTRO IL CONTENITORE DEI TASTI SEZIONE
            // Unificato in tonalità DavezAlabaster identica allo sfondo della chat e dell'app
            UnifiedStudioBottomBar(
                selectedTab = uiState.selectedTab,
                onSelectTab = { tab ->
                    viewModel.selectTab(tab)
                    val page = if (tab == MainAppTab.CHAT) 0 else 1
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(page)
                    }
                },
                agendaCount = uiState.todayEvents.size,
                inputFieldValue = inputFieldValue,
                onInputFieldValueChange = { inputFieldValue = it },
                inputFocusRequester = inputFocusRequester,
                isRecordingVoice = isRecordingVoice,
                onRecordingVoiceChange = { isRecordingVoice = it },
                onOpenAttachSheet = { showAttachSheet = true },
                onSendMessage = { text ->
                    viewModel.sendUserMessage(text)
                },
                onSendVoiceNote = { transcript, dur, path ->
                    viewModel.sendVoiceNoteMessage(transcript, dur, path)
                },
                onSystemVoiceClick = triggerSystemVoiceInput,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Foglio Modale Allegati Moderno Squircle (aperto dal tasto [+] della barra unificata)
        if (showAttachSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAttachSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                dragHandle = { AppleSheetDragHandle() },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                containerColor = DavezAlabaster
            ) {
                ModernAttachmentSheet(
                    onOpenScanner = {
                        showAttachSheet = false
                        showInAppCameraSheet = true
                    },
                    onPickSystemFile = {
                        showAttachSheet = false
                        try {
                            documentPickerLauncher.launch(arrayOf("*/*"))
                        } catch (_: Throwable) {
                            Toast.makeText(context, "Selettore file non disponibile", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onStartVoiceNote = {
                        showAttachSheet = false
                        isRecordingVoice = true
                    },
                    onOpenFolders = {
                        showAttachSheet = false
                        showArchiveSheet = true
                    },
                    onClose = { showAttachSheet = false }
                )
            }
        }

        // Foglio Modale In-App Camera & Scanner Documentale & Rilievi con vera anteprima CameraX (come WhatsApp / iOS)
        if (showInAppCameraSheet) {
            ModalBottomSheet(
                onDismissRequest = { showInAppCameraSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                sheetGesturesEnabled = false,
                dragHandle = { AppleSheetDragHandle(tint = Color.White.copy(alpha = 0.4f)) },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                containerColor = Color(0xFF141110)
            ) {
                InAppCameraScannerSheet(
                    onCaptureRealBitmap = { bitmap, preset ->
                        showInAppCameraSheet = false
                        viewModel.processCameraBitmap(bitmap, preset)
                    },
                    onCaptureBurstBitmaps = { burstBitmaps ->
                        showInAppCameraSheet = false
                        viewModel.processCameraBitmaps(burstBitmaps)
                    },
                    onOpenSystemCamera = {
                        showInAppCameraSheet = false
                        safeLaunchSystemCamera()
                    },
                    onPickFromGallery = {
                        showInAppCameraSheet = false
                        try {
                            documentPickerLauncher.launch(arrayOf("image/*", "application/pdf"))
                        } catch (_: Throwable) {
                            Toast.makeText(context, "Galleria non disponibile", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onClose = { showInAppCameraSheet = false }
                )
            }
        }

        // Foglio Modale iOS per esplorare le 6 Cartelle Archivio
        if (showArchiveSheet) {
            ModalBottomSheet(
                onDismissRequest = { showArchiveSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                dragHandle = { AppleSheetDragHandle() },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                containerColor = AppleWhite
            ) {
                ArchiveFoldersSheetContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    onCameraClick = {
                        showArchiveSheet = false
                        showInAppCameraSheet = true
                    },
                    onPickDocumentClick = {
                        showArchiveSheet = false
                        try {
                            documentPickerLauncher.launch(arrayOf("*/*"))
                        } catch (_: Throwable) {
                            Toast.makeText(context, "Selettore file non disponibile", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onClose = { showArchiveSheet = false }
                )
            }
        }

        // Foglio Modale iOS per Impostazioni & Modelli IA (3 Fissi + 1 Reasoning selezionabile)
        if (showAiSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAiSettingsSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                sheetGesturesEnabled = false,
                dragHandle = { AppleSheetDragHandle() },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                containerColor = AppleWhite
            ) {
                AiEngineSettingsSheetContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    onRequestCalendarPerm = handleCalendarConnect,
                    onClose = { showAiSettingsSheet = false }
                )
            }
        }

        // Dialogo di Benvenuto / Scelta Modello Reasoning alla Prima Apertura
        if (uiState.showFirstRunModelDialog) {
            FirstRunModelDownloadDialog(
                uiState = uiState,
                viewModel = viewModel
            )
        }

        // Dialogo di Aggiornamento In-App OTA
        if (uiState.showUpdateDialog && uiState.availableUpdate != null) {
            AppUpdateDialog(
                manifest = uiState.availableUpdate!!,
                downloadProgress = uiState.updateDownloadProgress,
                statusMessage = uiState.updateStatusMessage,
                onUpdateNow = { viewModel.startUpdateDownloadAndInstall(context) },
                onContinueWithOldVersion = { viewModel.dismissUpdateDialog() }
            )
        }
    }
}

// =============================================================================
// APPLE SHEET DRAG HANDLE (APPLE HIG SPEC)
// Standard iOS Modal grabber pill (36x5dp, continuous rounded corners)
// =============================================================================
@Composable
fun AppleSheetDragHandle(tint: Color = AppleLabelTertiary.copy(alpha = 0.35f)) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(5.dp)
                .background(
                    color = tint,
                    shape = RoundedCornerShape(2.5.dp)
                )
        )
    }
}

// =============================================================================
// APPLE LIQUID GLASS FLOATING DOCK TAB BAR (APPLE HIG 2026 SPEC)
// =============================================================================
// UNIFIED STUDIO BOTTOM BAR (APPLE HIG 2026 SPEC & STUDIO DAVEZ ARCHITETTURA)
// Contenitore unificato in tonalità DavezAlabaster identica allo sfondo dell'app:
// ospita la barra di scrittura con il [+] e i tasti per cambiare sezione (Assistente / Agenda)
// =============================================================================
@Composable
fun UnifiedStudioBottomBar(
    selectedTab: MainAppTab,
    onSelectTab: (MainAppTab) -> Unit,
    agendaCount: Int,
    inputFieldValue: TextFieldValue,
    onInputFieldValueChange: (TextFieldValue) -> Unit,
    inputFocusRequester: FocusRequester,
    isRecordingVoice: Boolean,
    onRecordingVoiceChange: (Boolean) -> Unit,
    onOpenAttachSheet: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendVoiceNote: (transcript: String, durationSec: Int, audioPath: String?) -> Unit,
    onSystemVoiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val isSlashMode = inputFieldValue.text.startsWith("/")

    Surface(
        color = Color(0xF7FAF7F4),
        border = BorderStroke(
            0.8.dp,
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.95f),
                    Color.White.copy(alpha = 0.25f),
                    DavezStoneBorder.copy(alpha = 0.45f)
                )
            )
        ),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        shadowElevation = 10.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. POPUP SUGGERIMENTI COMANDI SLASH (/)
            AnimatedVisibility(
                visible = isSlashMode && !isRecordingVoice,
                enter = fadeIn(tween(160)) + expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)),
                exit = fadeOut(tween(120)) + shrinkVertically(animationSpec = tween(120))
            ) {
                val typedQuery = inputFieldValue.text.removePrefix("/").trim().lowercase(Locale.ITALIAN)
                val filtered = remember(typedQuery) {
                    if (typedQuery.isEmpty()) {
                        PeritoSlashCommandsList
                    } else {
                        PeritoSlashCommandsList.filter {
                            it.command.removePrefix("/").contains(typedQuery) ||
                            it.title.lowercase(Locale.ITALIAN).contains(typedQuery) ||
                            it.syntax.lowercase(Locale.ITALIAN).contains(typedQuery) ||
                            it.description.lowercase(Locale.ITALIAN).contains(typedQuery)
                        }
                    }
                }

                SlashCommandsPopupCard(
                    commands = filtered,
                    typedQuery = typedQuery,
                    onSelectCommand = { cmd ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onInputFieldValueChange(
                            TextFieldValue(
                                text = cmd.insertText,
                                selection = TextRange(cmd.insertText.length)
                            )
                        )
                    },
                    onExecuteDirectCommand = { cmd ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onInputFieldValueChange(TextFieldValue(""))
                        onSendMessage(cmd.command)
                    },
                    onDismiss = {
                        onInputFieldValueChange(TextFieldValue(""))
                    }
                )
            }

            // 2. BARRA DI SCRITTURA LIQUID GLASS CON [+] E MIC/SEND MORPH (iOS 17 SPEC)
            AnimatedContent(
                targetState = isRecordingVoice,
                transitionSpec = {
                    (fadeIn(tween(180)) + scaleIn(
                        initialScale = 0.96f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )).togetherWith(fadeOut(tween(140)))
                },
                label = "unified_voice_bar_morph"
            ) { recordingActive ->
                if (recordingActive) {
                    InAppVoiceRecordingBar(
                        onCancel = { onRecordingVoiceChange(false) },
                        onSendVoiceNote = { transcript, durationSec, audioPath ->
                            onRecordingVoiceChange(false)
                            onSendVoiceNote(transcript, durationSec, audioPath)
                        },
                        onOpenSystemDictation = {
                            onRecordingVoiceChange(false)
                            onSystemVoiceClick()
                        }
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // [+] Pulsante Azioni & Allegati Stile Apple HIG Frosted Glass
                        val attachInteraction = remember { MutableInteractionSource() }
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOpenAttachSheet()
                            },
                            interactionSource = attachInteraction,
                            shape = CircleShape,
                            color = AppleWhite,
                            border = BorderStroke(
                                0.8.dp,
                                Brush.verticalGradient(
                                    listOf(Color.White, DavezStoneBorder.copy(alpha = 0.65f))
                                )
                            ),
                            shadowElevation = 1.5.dp,
                            modifier = Modifier
                                .size(44.dp)
                                .cartoonPressSquash(attachInteraction, pressedScaleY = 0.90f, squashScaleX = 1.08f)
                                .semantics {
                                    contentDescription = "Azioni e allegati"
                                    testTag = "chat_attach_button"
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Azioni e allegati",
                                    tint = DavezBordeaux,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        // Campo di testo arrotondato Capsule Apple White su Davez Alabaster
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp, max = 110.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.White, Color(0xFFFAF8F5))
                                    )
                                )
                                .border(
                                    0.8.dp,
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.95f),
                                            DavezStoneBorder.copy(alpha = 0.50f)
                                        )
                                    ),
                                    RoundedCornerShape(22.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (inputFieldValue.text.isEmpty()) {
                                Text(
                                    text = "Messaggio o digita / per comandi...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 15.sp,
                                        letterSpacing = (-0.2).sp
                                    ),
                                    color = AppleLabelTertiary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = inputFieldValue,
                                onValueChange = onInputFieldValueChange,
                                textStyle = TextStyle(
                                    color = AppleLabelPrimary,
                                    fontSize = 15.5.sp,
                                    lineHeight = 21.sp,
                                    letterSpacing = (-0.2).sp
                                ),
                                cursorBrush = SolidColor(DavezBordeaux),
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(inputFocusRequester)
                                    .semantics { contentDescription = "Campo messaggio chat" }
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        // Pulsante Azione Dinamico: Invia (se c'è testo) / Microfono (se vuoto)
                        val hasText = inputFieldValue.text.isNotBlank()
                        val sendInteraction = remember { MutableInteractionSource() }
                        Surface(
                            onClick = {
                                if (hasText) {
                                    val toSend = inputFieldValue.text.trim()
                                    if (toSend.isNotEmpty()) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onInputFieldValueChange(TextFieldValue(""))
                                        onSendMessage(toSend)
                                    }
                                } else {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onRecordingVoiceChange(true)
                                }
                            },
                            interactionSource = sendInteraction,
                            shape = CircleShape,
                            color = DavezBordeaux,
                            shadowElevation = 2.5.dp,
                            modifier = Modifier
                                .size(44.dp)
                                .cartoonPressSquash(sendInteraction, pressedScaleY = 0.88f, squashScaleX = 1.08f)
                                .semantics {
                                    contentDescription = if (hasText) "Invia messaggio" else "Detta nota vocale"
                                    testTag = if (hasText) "chat_send_button" else "chat_mic_button"
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                AnimatedContent(
                                    targetState = hasText,
                                    transitionSpec = {
                                        (scaleIn(
                                            initialScale = 0.88f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        ) + fadeIn(tween(150))).togetherWith(
                                            scaleOut(targetScale = 0.88f, animationSpec = tween(120)) + fadeOut(tween(120))
                                        )
                                    },
                                    label = "mic_send_morph"
                                ) { showSend ->
                                    if (showSend) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Invia",
                                            tint = AppleWhite,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "Registra Nota Vocale In-App",
                                            tint = AppleWhite,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. SEGMENTED SWITCHER (iOS 17 FLUID DOCK)
            // Quando la tastiera è aperta (imeBottomPx > 0), nascondiamo la barra dock all'istante senza animare l'altezza layout (Emil Kowalski: 100+ times/day = instant state change) per evitare jitter o incertezze con l'IME
            val isKeyboardOpen = imeBottomPx > 0
            if (!isKeyboardOpen) {
                LiquidFluidTabDock(
                    selectedTab = selectedTab,
                    onSelectTab = onSelectTab,
                    agendaCount = agendaCount
                )
            }
        }
    }
}

// =============================================================================
// LIQUID FLUID TAB DOCK (iOS 17 ELEGANT SPRING SWITCHER)
// Fisica a molla organica (/emilkowalski-animation): transizione fluida su GPU
// senza distorsioni, onde anomale o deformazioni di scala.
// =============================================================================
@Composable
fun LiquidFluidTabDock(
    selectedTab: MainAppTab,
    onSelectTab: (MainAppTab) -> Unit,
    agendaCount: Int,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val targetIndex = if (selectedTab == MainAppTab.CHAT) 0 else 1
    val pillProgress = remember { Animatable(targetIndex.toFloat()) }
    var prevIndex by remember { mutableIntStateOf(targetIndex) }

    LaunchedEffect(targetIndex) {
        if (targetIndex != prevIndex) {
            prevIndex = targetIndex
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            pillProgress.animateTo(
                targetValue = targetIndex.toFloat(),
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    // Binario incassato con finitura frosted glass tenue
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFEBE6E1),
        border = BorderStroke(
            0.6.dp,
            Brush.verticalGradient(
                listOf(
                    DavezStoneBorder.copy(alpha = 0.50f),
                    Color.White.copy(alpha = 0.60f)
                )
            )
        ),
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
        ) {
            val availableWidth = maxWidth
            val tabWidth = availableWidth / 2
            val currentProgress = pillProgress.value

            // Pillola Bianca Apple morbida con ombra soffusa e bordo satinato su GPU RenderThread
            Box(
                modifier = Modifier
                    .width(tabWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        translationX = (currentProgress * tabWidth.toPx())
                    }
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(13.dp),
                        ambientColor = Color(0x25000000),
                        spotColor = Color(0x18000000)
                    )
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                Color(0xFFFAF7F5)
                            )
                        )
                    )
                    .border(
                        width = 0.6.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.95f),
                                Color.White.copy(alpha = 0.40f)
                            )
                        ),
                        shape = RoundedCornerShape(13.dp)
                    )
            )

            // C. Livello etichette ed icone con transizione cromatica fluida continua
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sezione Assistente
                val chatFraction = (1f - currentProgress).coerceIn(0f, 1f)
                val chatColor = lerp(AppleLabelSecondary, DavezBordeaux, chatFraction)
                val chatInteraction = remember { MutableInteractionSource() }
                val isChatSelected = selectedTab == MainAppTab.CHAT
                val chatIconBounce by animateFloatAsState(
                    targetValue = if (isChatSelected) 1.16f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "chat_tab_bounce"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(13.dp))
                        .clickable(
                            interactionSource = chatInteraction,
                            indication = null
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectTab(MainAppTab.CHAT)
                        }
                        .cartoonPressSquash(chatInteraction, pressedScaleY = 0.93f, squashScaleX = 1.03f),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selectedTab == MainAppTab.CHAT) Icons.Default.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Assistente",
                            tint = chatColor,
                            modifier = Modifier
                                .size(16.dp)
                                .graphicsLayer {
                                    scaleX = chatIconBounce
                                    scaleY = chatIconBounce
                                }
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Assistente",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (chatFraction > 0.5f) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.5.sp,
                                letterSpacing = (-0.2).sp
                            ),
                            color = chatColor
                        )
                    }
                }

                // Sezione Agenda
                val agendaFraction = currentProgress.coerceIn(0f, 1f)
                val agendaColor = lerp(AppleLabelSecondary, DavezBordeaux, agendaFraction)
                val agendaInteraction = remember { MutableInteractionSource() }
                val isAgendaSelected = selectedTab == MainAppTab.AGENDA
                val agendaIconBounce by animateFloatAsState(
                    targetValue = if (isAgendaSelected) 1.16f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "agenda_tab_bounce"
                )
                val badgeScale by animateFloatAsState(
                    targetValue = if (agendaCount > 0) 1.0f else 0f,
                    animationSpec = spring(
                        dampingRatio = 0.52f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "agenda_badge_scale"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(13.dp))
                        .clickable(
                            interactionSource = agendaInteraction,
                            indication = null
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectTab(MainAppTab.AGENDA)
                        }
                        .cartoonPressSquash(agendaInteraction, pressedScaleY = 0.93f, squashScaleX = 1.03f),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selectedTab == MainAppTab.AGENDA) Icons.Default.CalendarMonth else Icons.Outlined.CalendarMonth,
                            contentDescription = "Agenda",
                            tint = agendaColor,
                            modifier = Modifier
                                .size(16.dp)
                                .graphicsLayer {
                                    scaleX = agendaIconBounce
                                    scaleY = agendaIconBounce
                                }
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Agenda",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (agendaFraction > 0.5f) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.5.sp,
                                letterSpacing = (-0.2).sp
                            ),
                            color = agendaColor
                        )
                        if (agendaCount > 0) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = lerp(DavezBordeaux.copy(alpha = 0.16f), DavezBordeaux, agendaFraction),
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer {
                                        scaleX = badgeScale
                                        scaleY = badgeScale
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (agendaCount > 99) "99+" else agendaCount.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (agendaFraction > 0.5f) AppleWhite else DavezBordeaux
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// TOP BAR STUDIO DAVEZ ARCHITETTURA + APPLE HIG CON MONOGRAMMA RD E SQUIRCLE PILLS
// =============================================================================
@Composable
fun PeritoStudioTopBar(
    archivedCount: Int,
    hasCalendarPermission: Boolean,
    isProcessing: Boolean,
    studioTitle: String = "Studio Tecnico Peritale",
    onOpenArchive: () -> Unit,
    onOpenAiModels: () -> Unit,
    onRequestCalendarPerm: () -> Unit,
    onClearChat: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var showConfirmClear by remember { mutableStateOf(false) }

    if (showConfirmClear && onClearChat != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showConfirmClear = false },
            title = {
                Text(
                    text = "Cancella cronologia chat",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DavezBordeaux
                )
            },
            text = {
                Text(
                    text = "Vuoi svuotare tutti i messaggi della conversazione corrente? I documenti archiviati e i fascicoli rimarranno intatti.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppleLabelPrimary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmClear = false
                        onClearChat()
                    }
                ) {
                    Text("Cancella", color = AppleRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClear = false }) {
                    Text("Annulla", color = AppleLabelSecondary)
                }
            },
            containerColor = AppleWhite,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DavezAlabaster)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Logo 3D PeritoAI RD con pallino verde Online
            PeritoAiBrandEmblem(size = 42.dp, showOnlineBadge = true)

            Spacer(Modifier.width(10.dp))

            // Nome App ("PeritoAI") + Monogramma Studio Tecnico
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "PeritoAI",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        ),
                        color = AppleLabelPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = AppleBlueSoft,
                        border = BorderStroke(0.5.dp, DavezBordeaux.copy(alpha = 0.22f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.rd_logo_colore),
                                contentDescription = "Monogramma Studio",
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = studioTitle.uppercase().take(14),
                                style = MaterialTheme.typography.labelSmall,
                                color = DavezBordeaux,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp
                            )
                        }
                    }
                }
                Text(
                    text = if (isProcessing) "✨ Elaborazione IA in corso..." else "$studioTitle • IA Locale",
                    style = MaterialTheme.typography.bodySmall,
                    color = DavezBordeauxSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Pulsante sincronizzazione Calendario (se non ancora concessa)
            if (!hasCalendarPermission) {
                val calInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRequestCalendarPerm()
                    },
                    interactionSource = calInteraction,
                    shape = CircleShape,
                    color = AppleOrangeSoft,
                    modifier = Modifier
                        .size(40.dp)
                        .emilPressScale(calInteraction)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Collega Calendario",
                            tint = AppleOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
            }

            // Pulsante Squircle Impostazioni & Modelli IA (Touch target 44x44dp Apple HIG)
            val modInteraction = remember { MutableInteractionSource() }
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onOpenAiModels()
                },
                interactionSource = modInteraction,
                shape = RoundedCornerShape(14.dp),
                color = AppleBlueSoft,
                border = BorderStroke(0.5.dp, DavezBordeaux.copy(alpha = 0.18f)),
                modifier = Modifier
                    .size(44.dp)
                    .emilPressScale(modInteraction)
                    .semantics {
                        contentDescription = "Parametri e Modelli IA"
                        testTag = "top_bar_models_button"
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Parametri e Modelli IA",
                        tint = DavezBordeaux,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Pulsante Svuota Chat (se fornito)
            if (onClearChat != null) {
                Spacer(Modifier.width(6.dp))
                val clearInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showConfirmClear = true
                    },
                    interactionSource = clearInteraction,
                    shape = RoundedCornerShape(14.dp),
                    color = AppleBlueSoft,
                    border = BorderStroke(0.5.dp, DavezBordeaux.copy(alpha = 0.18f)),
                    modifier = Modifier
                        .size(44.dp)
                        .emilPressScale(clearInteraction)
                        .semantics {
                            contentDescription = "Svuota Chat"
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Svuota Chat",
                            tint = DavezBordeaux,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Hairline Divider iOS (0.5.dp) in tonalità pietra davez
        HorizontalDivider(thickness = 0.5.dp, color = DavezStoneBorder)
    }
}

// =============================================================================
// =============================================================================
// HERO CARD ARCHITETTURA DAVEZ CON NUCLEO 3D + 3 TASTI FUNZIONALITA CHAT
// =============================================================================
@Composable
fun PeritoInteractiveHeroHeader(
    isProcessing: Boolean,
    isReasoningReady: Boolean,
    onSendOggi: () -> Unit,
    onPrefillSearch: () -> Unit,
    onPrefillDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = AppleWhite,
        shadowElevation = 2.dp,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    DavezBordeaux.copy(alpha = 0.28f),
                    DavezStoneBorder.copy(alpha = 0.55f)
                )
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { testTag = "perito_welcome_hero_card" }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Nucleo 3D Interattivo in Bordeaux Imperiale & Oro Champagne (Trascinabile!)
                Perito3DThinkingOrb(
                    isThinking = isProcessing,
                    size = 54.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Studio Peritale & Rilievi",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold
                            ),
                            color = AppleLabelPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isReasoningReady) AppleGreenSoft else AppleOrangeSoft
                        ) {
                            Text(
                                text = if (isReasoningReady) "Qwen 3.5 Attivo" else "Modello Richiesto",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isReasoningReady) AppleGreen else AppleOrange,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Il rigore delle misure e l'IA locale: registra note vocali, scansiona atti o smista nelle 6 cartelle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleLabelSecondary
                    )
                }
            }

            // 3 Quick Action Bento Cards (/design-taste-frontend-v1 & /agency-ui-designer)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroActionBentoCard(
                    icon = Icons.Default.CalendarMonth,
                    title = "/oggi",
                    subtitle = "Impegni",
                    modifier = Modifier.weight(1f),
                    onClick = onSendOggi
                )
                HeroActionBentoCard(
                    icon = Icons.Default.Search,
                    title = "/cerca",
                    subtitle = "Fascicoli",
                    modifier = Modifier.weight(1f),
                    onClick = onPrefillSearch
                )
                HeroActionBentoCard(
                    icon = Icons.Default.DeleteOutline,
                    title = "/elimina",
                    subtitle = "Rimuovi",
                    modifier = Modifier.weight(1f),
                    onClick = onPrefillDelete
                )
            }
        }
    }
}

@Composable
private fun HeroActionBentoCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    // Cartoonish icon bounce when pressed
    val iconBounce by animateFloatAsState(
        targetValue = if (isPressed) 0.84f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "bento_icon_bounce"
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(14.dp),
        color = DavezAlabaster,
        border = BorderStroke(0.6.dp, DavezStoneBorder),
        modifier = modifier.cartoonPressSquash(interactionSource, pressedScaleY = 0.92f, squashScaleX = 1.05f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppleWhite)
                    .border(0.5.dp, DavezStoneBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .graphicsLayer {
                        scaleX = iconBounce
                        scaleY = iconBounce
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DavezBordeaux,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = (-0.2).sp),
                fontWeight = FontWeight.Bold,
                color = DavezBordeaux,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = AppleLabelSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// =============================================================================
// BOLLA SEMANTICA DI RAGIONAMENTO IA IN CORSO (/agentic-craft-ui Pillar 1 & 4)
// =============================================================================
@Composable
fun AgenticThinkingBubble(label: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking_orb_breathe")
    val breathePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "breathe_phase"
    )
    val breatheScaleX = 1f + sin(breathePhase) * 0.025f
    val breatheScaleY = 1f - sin(breathePhase) * 0.025f

    Surface(
        shape = RoundedCornerShape(
            topStart = 22.dp,
            topEnd = 22.dp,
            bottomStart = 6.dp,
            bottomEnd = 22.dp
        ),
        color = AppleBlueSoft,
        border = BorderStroke(1.dp, DavezBordeaux.copy(alpha = 0.25f)),
        modifier = Modifier
            .widthIn(min = 180.dp, max = 330.dp)
            .graphicsLayer {
                scaleX = breatheScaleX
                scaleY = breatheScaleY
            }
            .semantics { testTag = "agentic_thinking_bubble" }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Perito3DThinkingOrb(isThinking = true, size = 32.dp)
            Text(
                text = label.ifBlank { "🧠 Qwen 3.5 2B (Q4_K_M) • Analisi in corso..." },
                style = MaterialTheme.typography.labelMedium,
                color = DavezBordeaux,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// =============================================================================
// SCHERMATA UNICA TIMELINE CON REGISTRATORE VOCALE IN-APP (STUDIO RESTelli),
// FOTOCAMERA SCANNER 3D E TRAY ALLEGATI MODERNO SQUIRCLE
// =============================================================================
// =============================================================================
// SCHERMATA ASSISTENTE CHAT PERITALE (SFONDO CONTINUO DAVEZ ALABASTER)
// =============================================================================
@Composable
fun PeritoStudioChatScreen(
    uiState: PeritoUiState,
    viewModel: PeritoViewModel,
    onPrefillCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)

    val showScrollToBottom by remember {
        derivedStateOf { listState.canScrollForward }
    }

    // Scroll fluido solo su arrivo di nuovi messaggi o inizio elaborazione
    LaunchedEffect(uiState.messages.size, uiState.isProcessing) {
        val totalItems = uiState.messages.size + 1 + (if (uiState.isProcessing) 1 else 0)
        if (uiState.messages.size > 1 || uiState.isProcessing) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    // Scroll istantaneo su apertura tastiera (IME): niente ritardi, delay o doppie animazioni concorrenti
    LaunchedEffect(imeBottomPx > 0) {
        if (imeBottomPx > 0) {
            val totalItems = uiState.messages.size + 1 + (if (uiState.isProcessing) 1 else 0)
            if (totalItems > 1) {
                listState.scrollToItem(totalItems - 1)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DavezAlabaster)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(DavezAlabaster),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Interattivo con Nucleo 3D e i 3 comandi slash (/oggi, /cerca, /elimina)
            item(key = "hero_header_3d") {
                PeritoInteractiveHeroHeader(
                    isProcessing = uiState.isProcessing,
                    isReasoningReady = uiState.isReasoningReady,
                    onSendOggi = { viewModel.sendUserMessage("/oggi") },
                    onPrefillSearch = { onPrefillCommand("/cerca ") },
                    onPrefillDelete = { onPrefillCommand("/elimina ") }
                )
            }

            items(uiState.messages, key = { it.id }) { msg ->
                Box(
                    modifier = Modifier.animateItem(
                        fadeInSpec = spring(
                            dampingRatio = 0.62f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        placementSpec = spring(
                            dampingRatio = 0.72f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        fadeOutSpec = spring(
                            dampingRatio = 0.65f,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                ) {
                    PeritoStudioMessageBubble(
                        message = msg,
                        isClearingHistory = uiState.isClearingHistory,
                        viewModel = viewModel,
                        onOpenDocument = { doc ->
                            viewModel.archiveRepo.createShareOrOpenIntent(doc, asShare = false)?.let {
                                try {
                                    context.startActivity(it)
                                } catch (_: Throwable) {
                                    Toast.makeText(context, "Usa Condividi per aprire il PDF", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onShareDocument = { doc ->
                            viewModel.archiveRepo.createShareOrOpenIntent(doc, asShare = true)?.let {
                                context.startActivity(Intent.createChooser(it, "Condividi ${doc.fileName}"))
                            }
                        },
                        onOpenMaps = { location ->
                            viewModel.calendarRepo.launchGoogleMaps(context, location)
                        }
                    )
                }
            }

            // Bolla Semantica Thinking Orb durante l'analisi IA (/agentic-craft-ui)
            if (uiState.isProcessing) {
                item(key = "thinking_orb_bubble") {
                    AgenticThinkingBubble(label = uiState.processingLabel)
                }
            }
        }

        // Floating Pill "Ultimi messaggi" quando si scorre verso l'alto
        androidx.compose.animation.AnimatedVisibility(
            visible = showScrollToBottom,
            enter = fadeIn(tween(160)) + scaleIn(initialScale = 0.92f),
            exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.92f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 12.dp)
        ) {
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    coroutineScope.launch {
                        val totalItems = uiState.messages.size + 1
                        listState.animateScrollToItem(totalItems - 1, scrollOffset = 10_000)
                    }
                },
                shape = CircleShape,
                color = AppleWhite,
                shadowElevation = 4.dp,
                border = BorderStroke(0.8.dp, DavezStoneBorder),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Scorri in fondo",
                        tint = DavezBordeaux
                    )
                }
            }
        }
    }
}

// =============================================================================
// REGISTRATORE VOCALE IN-APP STILE WHATSAPP / IMESSAGE CON ONDA SONORA 60FPS,
// REGISTRAZIONE VOCE REALE UTENTE (.WAV) E GOOGLE SPEECHRECOGNIZER HEADLESS
// =============================================================================
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
            putInt(16) // Subchunk1Size (PCM)
            putShort(1.toShort()) // AudioFormat (1 = PCM)
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

// =============================================================================
// COMANDI RAPIDI SLASH (/) & IMPOSTAZIONI RAPIDE
// =============================================================================
data class PeritoSlashCommand(
    val command: String,
    val syntax: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val insertText: String,
    val isDirectAction: Boolean = false
)

val PeritoSlashCommandsList = listOf(
    PeritoSlashCommand(
        command = "/orario",
        syntax = "/orario [orario]",
        title = "Modifica Orario",
        description = "Sposta o correggi l'orario (es: /orario 17:00, /orario +1h, /orario domani)",
        iconEmoji = "🕒",
        insertText = "/orario "
    ),
    PeritoSlashCommand(
        command = "/sposta",
        syntax = "/sposta [orario/giorno]",
        title = "Sposta Appuntamento",
        description = "Sposta a domani o ad altra data (es: /sposta a domani alle 11, /sposta -30m)",
        iconEmoji = "🗓️",
        insertText = "/sposta "
    ),
    PeritoSlashCommand(
        command = "/fissa",
        syntax = "/fissa [dettagli]",
        title = "Nuovo Appuntamento",
        description = "Crea sopralluogo, incontro o udienza (es: /fissa domani alle 15 perizia a Como)",
        iconEmoji = "📅",
        insertText = "/fissa "
    ),
    PeritoSlashCommand(
        command = "/oggi",
        syntax = "/oggi",
        title = "Briefing di Oggi",
        description = "Riepilogo dei sopralluoghi e impegni in programma per oggi",
        iconEmoji = "☀️",
        insertText = "/oggi",
        isDirectAction = true
    ),
    PeritoSlashCommand(
        command = "/cerca",
        syntax = "/cerca [testo]",
        title = "Cerca nell'Archivio",
        description = "Cerca perizie, verbali e computi nelle 6 cartelle (es: /cerca infiltrazioni)",
        iconEmoji = "🔍",
        insertText = "/cerca "
    ),
    PeritoSlashCommand(
        command = "/elimina",
        syntax = "/elimina [appuntamento/file]",
        title = "Elimina Appuntamento o File",
        description = "Rimuove l'ultimo appuntamento dall'agenda o un file archiviato",
        iconEmoji = "🗑️",
        insertText = "/elimina "
    ),
    PeritoSlashCommand(
        command = "/cartelle",
        syntax = "/cartelle",
        title = "Archivio 6 Cartelle",
        description = "Conteggio e riepilogo documenti nelle 6 cartelle peritali",
        iconEmoji = "📂",
        insertText = "/cartelle",
        isDirectAction = true
    ),
    PeritoSlashCommand(
        command = "/calendario",
        syntax = "/calendario",
        title = "Sincronizzazione Calendario",
        description = "Verifica stato sincronizzazione Google Calendar / Agenda Locale",
        iconEmoji = "🔄",
        insertText = "/calendario",
        isDirectAction = true
    ),
    PeritoSlashCommand(
        command = "/impostazioni",
        syntax = "/impostazioni",
        title = "Impostazioni & Modelli IA",
        description = "Configurazione motore locale Qwen 3.5 2B e parametri studio",
        iconEmoji = "⚙️",
        insertText = "/impostazioni",
        isDirectAction = true
    ),
    PeritoSlashCommand(
        command = "/aggiorna",
        syntax = "/aggiorna",
        title = "Aggiorna App (OTA)",
        description = "Verifica la disponibilità di nuove versioni APK e aggiorna subito",
        iconEmoji = "🔄",
        insertText = "/aggiorna",
        isDirectAction = true
    ),
    PeritoSlashCommand(
        command = "/aiuto",
        syntax = "/aiuto",
        title = "Guida Comandi Rapidi",
        description = "Elenco completo delle istruzioni slash supportate dal modello",
        iconEmoji = "💡",
        insertText = "/aiuto",
        isDirectAction = true
    )
)

@Composable
fun SlashCommandsPopupCard(
    commands: List<PeritoSlashCommand>,
    typedQuery: String,
    onSelectCommand: (PeritoSlashCommand) -> Unit,
    onExecuteDirectCommand: (PeritoSlashCommand) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = AppleWhite,
        shadowElevation = 6.dp,
        border = BorderStroke(0.8.dp, DavezStoneBorder),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header con titolo, query filtrata e pulsante chiudi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text("⚡", fontSize = 14.sp)
                    Text(
                        text = "COMANDI RAPIDI & IMPOSTAZIONI",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = DavezBordeaux,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (typedQuery.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = DavezBordeaux.copy(alpha = 0.10f)
                        ) {
                            Text(
                                text = "/$typedQuery",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = DavezBordeaux,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Chiudi suggerimenti",
                        tint = AppleLabelSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = DavezStoneBorder.copy(alpha = 0.6f))

            if (commands.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nessun comando trovato per \"/$typedQuery\".\nProva /orario, /sposta, /oggi, /fissa, /cerca...",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleLabelSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(commands, key = { it.command }) { cmd ->
                        Surface(
                            onClick = { onSelectCommand(cmd) },
                            shape = RoundedCornerShape(12.dp),
                            color = AppleWhite,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Badge icona
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(DavezBordeaux.copy(alpha = 0.08f))
                                            .border(0.5.dp, DavezBordeaux.copy(alpha = 0.18f), RoundedCornerShape(9.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(cmd.iconEmoji, fontSize = 17.sp)
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = cmd.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = AppleLabelPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = AppleBlueSoft
                                            ) {
                                                Text(
                                                    text = cmd.syntax,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 10.sp
                                                    ),
                                                    color = DavezBordeaux,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(1.dp))
                                        Text(
                                            text = cmd.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AppleLabelSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (cmd.isDirectAction) {
                                    Surface(
                                        onClick = { onExecuteDirectCommand(cmd) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = DavezBordeaux,
                                        modifier = Modifier.padding(start = 6.dp)
                                    ) {
                                        Text(
                                            text = "Esegui ⚡",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AppleWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Usa ↗",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DavezBordeaux,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(start = 6.dp, end = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
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
    val coroutineScope = rememberCoroutineScope()
    var elapsedSeconds by remember { mutableIntStateOf(1) }
    var liveTranscript by remember { mutableStateOf("") }
    var partialSegment by remember { mutableStateOf("") }
    var statusHint by remember { mutableStateOf<String?>(null) }
    var isFlushingSend by remember { mutableStateOf(false) }
    var recognizerInstance by remember { mutableStateOf<SpeechRecognizer?>(null) }
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    var rmsDb by remember { mutableFloatStateOf(4.5f) }
    val pcmBuffer = remember { ByteArrayOutputStream(16000 * 2 * 15) }
    var stopAudioRecordFn by remember { mutableStateOf<(() -> Unit)?>(null) }
    var hasAudioPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Timer in tempo reale (0:01, 0:02...)
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            elapsedSeconds += 1
        }
    }

    // Richiesta reattiva permesso RECORD_AUDIO
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

        // 1. Avvia AudioRecord per registrare la VERA voce dell'utente in PCM 16kHz 16-bit Mono (.wav)
        if (hasAudioPerm) {
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
                    audioRecord = ar
                    ar.startRecording()
                    recordThread = Thread {
                        val readBuf = ByteArray(1600) // 50ms a 16kHz 16-bit mono
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
                                // Invia i campioni anche alla pipe di SpeechRecognizer (se attiva su Android 13+)
                                val sink = currentPipeSink
                                if (sink != null) {
                                    try {
                                        sink.write(readBuf, 0, readBytes)
                                        sink.flush()
                                    } catch (_: Throwable) {
                                        // SpeechRecognizer ha chiuso il lato lettura a fine frase
                                    }
                                }
                                // Calcola il volume reale RMS della voce per l'onda sonora con throttling a 60ms e dispatch su MainThread
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
                usePipeAudioSource = false
            }
        }

        stopAudioRecordFn = {
            isRecordingAudio = false
            closeCurrentPipe()
            try {
                audioRecord?.stop()
            } catch (_: Throwable) {}
            try {
                recordThread?.join(250L)
            } catch (_: Throwable) {}
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
                            statusHint = null
                        }

                        override fun onBeginningOfSpeech() {
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
                            // Se l'OEM non supporta EXTRA_AUDIO_SOURCE, passa automaticamente al mic condiviso
                            if (usePipeAudioSource && (error == SpeechRecognizer.ERROR_AUDIO || error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY)) {
                                usePipeAudioSource = false
                            }
                            if (partialSegment.isNotBlank()) {
                                val seg = partialSegment.trim()
                                liveTranscript = if (liveTranscript.isBlank()) seg else "$liveTranscript $seg"
                                partialSegment = ""
                            }
                            if (!isFlushingSend) {
                                mainHandler.postDelayed({
                                    if (!isDisposed && !isFlushingSend) {
                                        try {
                                            startListening(buildSpeechIntent())
                                        } catch (_: Throwable) {}
                                    }
                                }, 180L)
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            if (isDisposed) return
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

        onDispose {
            isDisposed = true
            isRecordingAudio = false
            mainHandler.removeCallbacksAndMessages(null)
            recognizerInstance = null
            stopAudioRecordFn = null
            closeCurrentPipe()
            try {
                audioRecord?.stop()
            } catch (_: Throwable) {}
            try {
                audioRecord?.release()
            } catch (_: Throwable) {}
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
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "voice_wave_phase"
    )
    val redDotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "red_dot_pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppleWhite)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .semantics { testTag = "in_app_voice_recording_bar" },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Trascrizione Live Google SpeechRecognizer Headless In-App + Registrazione Voce Reale Utente
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = AppleRedSoft
            ) {
                Text(
                    text = "🎙️ Registrazione Vocale • Trascrizione in Diretta",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppleRed,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            if (combinedTranscript.isNotBlank()) {
                Surface(
                    onClick = {
                        liveTranscript = ""
                        partialSegment = ""
                    },
                    shape = RoundedCornerShape(50),
                    color = DavezAlabaster,
                    border = BorderStroke(0.5.dp, DavezStoneBorder)
                ) {
                    Text(
                        text = "↺ Pulisci testo",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppleLabelSecondary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Anteprima testo trascritto in tempo reale direttamente nella schermata attuale
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = DavezAlabaster,
            border = BorderStroke(0.5.dp, DavezStoneBorder),
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
                    combinedTranscript.isNotBlank() -> AppleLabelPrimary
                    statusHint != null -> AppleRed
                    else -> AppleLabelSecondary
                },
                fontWeight = if (combinedTranscript.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Barra Principale stile WhatsApp: [🗑️ Annulla] + [🔴 0:03 + Onda Sonora 24 Barre] + [⬆️ Invia Vocale]
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val cancelInteraction = remember { MutableInteractionSource() }
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    stopAudioRecordFn?.invoke()
                    onCancel()
                },
                interactionSource = cancelInteraction,
                shape = CircleShape,
                color = AppleRedSoft,
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
                        tint = AppleRed,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Pillola Centrale con Pallino Rosso Pulsante + Timer + Onda Sonora 60fps
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = AppleBlueSoft,
                border = BorderStroke(1.dp, DavezBordeaux.copy(alpha = 0.25f)),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(AppleRed.copy(alpha = redDotAlpha))
                    )
                    val mins = elapsedSeconds / 60
                    val secs = elapsedSeconds % 60
                    Text(
                        text = String.format(Locale.ITALIAN, "%d:%02d", mins, secs),
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                        color = DavezBordeaux,
                        fontWeight = FontWeight.Bold
                    )
                    // Onda Sonora 24 Barre Dinamica
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                    ) {
                        val barCount = 24
                        val spacing = size.width / barCount
                        val barWidth = (spacing * 0.56f).coerceAtLeast(3f)
                        for (i in 0 until barCount) {
                            val norm = sin(wavePhase + i * 0.45f) * 0.5f + 0.5f
                            val boost = (rmsDb / 8f).coerceIn(0.4f, 1.3f)
                            val barH = (size.height * (0.22f + 0.72f * norm * boost)).coerceIn(4.dp.toPx(), size.height)
                            val x = i * spacing + (spacing - barWidth) / 2f
                            val y = (size.height - barH) / 2f
                            drawRoundRect(
                                color = if (i % 3 == 0) DavezBordeaux else DavezBordeauxSoft,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barH),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )
                        }
                    }
                }
            }

            // Pulsante Invia Nota Vocale (⬆️) — Salva il .WAV della vera voce dell'utente e invia!
            val sendInteraction = remember { MutableInteractionSource() }
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val immediateText = combinedTranscript.trim()
                    if (immediateText.isNotBlank()) {
                        isFlushingSend = true
                        stopAudioRecordFn?.invoke()
                        try {
                            recognizerInstance?.stopListening()
                        } catch (_: Throwable) {}
                        val wavBytes = synchronized(pcmBuffer) { pcmBuffer.toByteArray() }
                        val savedWavPath = savePcmAsWavFile(context, wavBytes, 16000)
                        onSendVoiceNote(immediateText, elapsedSeconds, savedWavPath)
                    } else if (!isFlushingSend) {
                        // Forza il flush finale del motore Google SpeechRecognizer senza aprire pop-up
                        isFlushingSend = true
                        statusHint = "⏳ Elaborazione ultime parole..."
                        stopAudioRecordFn?.invoke()
                        try {
                            recognizerInstance?.stopListening()
                        } catch (_: Throwable) {}
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
                                val wavBytes = synchronized(pcmBuffer) { pcmBuffer.toByteArray() }
                                val savedWavPath = savePcmAsWavFile(context, wavBytes, 16000)
                                onSendVoiceNote(flushed, elapsedSeconds, savedWavPath)
                            } else {
                                isFlushingSend = false
                                statusHint = "🎙️ Nessuna voce rilevata: parla nel microfono e premi Invia"
                            }
                        }
                    }
                },
                interactionSource = sendInteraction,
                shape = CircleShape,
                color = DavezBordeaux,
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
                        tint = AppleWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// =============================================================================
// FOGLIO MODALE ALLEGATI SQUIRCLE STILE WHATSAPP / IOS (APPLE HIG & RESPONSIVE)
// Risolve il problema del pulsante Chiudi che va a capo e garantisce perfetta responsività
// su schermi compatti, standard, foldable e tablet senza menu quadrati!
// =============================================================================
@Composable
fun ModernAttachmentSheet(
    onOpenScanner: () -> Unit,
    onPickSystemFile: () -> Unit,
    onStartVoiceNote: () -> Unit,
    onOpenFolders: () -> Unit,
    onClose: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .background(DavezAlabaster)
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .semantics { testTag = "modern_attachment_sheet" },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header con titolo e pulsante circolare Chiudi stile Apple HIG (non va MAI a capo!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = "Allega o Scansiona",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = (-0.3).sp
                        ),
                        color = AppleLabelPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Smistamento OCR automatico nei 6 Faldoni peritali",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.sp,
                            letterSpacing = (-0.1).sp
                        ),
                        color = AppleLabelSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Pulsante Chiudi Circolare Stile Apple HIG: NON va MAI a capo!
                val closeInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClose()
                    },
                    interactionSource = closeInteraction,
                    shape = CircleShape,
                    color = Color(0x14000000),
                    modifier = Modifier
                        .size(32.dp)
                        .emilPressScale(closeInteraction, pressedScale = 0.90f)
                        .semantics { contentDescription = "Chiudi finestra allegati" }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = AppleLabelPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Griglia 2x2 Bento Squircle Responsive stile Apple HIG & Studio Tecnico
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isCompact = maxWidth < 340.dp
                val tileSpacing = if (isCompact) 8.dp else 12.dp

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(tileSpacing)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(tileSpacing)
                    ) {
                        SquircleAttachmentTile(
                            emoji = "📸",
                            title = "Scanner 3D",
                            subtitle = "Foto & OCR Live",
                            tintColor = DavezBordeaux,
                            bgColor = AppleBlueSoft,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenScanner
                        )
                        SquircleAttachmentTile(
                            emoji = "📄",
                            title = "File & .p7m",
                            subtitle = "Importa da Telefono",
                            tintColor = DavezFotografiaSlate,
                            bgColor = Color(0xFFEDF2F5),
                            modifier = Modifier.weight(1f),
                            onClick = onPickSystemFile
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(tileSpacing)
                    ) {
                        SquircleAttachmentTile(
                            emoji = "🎙️",
                            title = "Nota Vocale",
                            subtitle = "Voce & Trascrizione",
                            tintColor = AppleGreen,
                            bgColor = AppleGreenSoft,
                            modifier = Modifier.weight(1f),
                            onClick = onStartVoiceNote
                        )
                        SquircleAttachmentTile(
                            emoji = "📂",
                            title = "6 Cartelle",
                            subtitle = "Archivio & Perizie",
                            tintColor = AppleOrange,
                            bgColor = AppleOrangeSoft,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenFolders
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SquircleAttachmentTile(
    emoji: String,
    title: String,
    subtitle: String,
    tintColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        interactionSource = interaction,
        shape = RoundedCornerShape(18.dp),
        color = bgColor,
        border = BorderStroke(0.8.dp, tintColor.copy(alpha = 0.20f)),
        shadowElevation = 0.5.dp,
        modifier = modifier.emilPressScale(interaction, pressedScale = 0.95f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppleWhite)
                    .border(0.5.dp, Color.Black.copy(alpha = 0.04f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 20.sp)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        letterSpacing = (-0.2).sp
                    ),
                    color = tintColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        letterSpacing = (-0.1).sp
                    ),
                    color = AppleLabelSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// =============================================================================
// FOTOCAMERA & SCANNER DOCUMENTALE & RILIEVI INTEGRATO IN-APP (LIVE CAMERAX + /apple-design)
// =============================================================================
@Composable
fun InAppCameraScannerSheet(
    onCaptureRealBitmap: (Bitmap, Int) -> Unit,
    onCaptureBurstBitmaps: ((List<Bitmap>) -> Unit)? = null,
    onOpenSystemCamera: () -> Unit,
    onPickFromGallery: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    var selectedScanMode by rememberSaveable { mutableIntStateOf(1) }
    var isBurstMode by rememberSaveable { mutableStateOf(false) }
    val burstList = remember { mutableStateListOf<Bitmap>() }
    var flashEnabled by rememberSaveable { mutableStateOf(true) }
    var shutterFlashAlpha by remember { mutableFloatStateOf(0f) }
    var isCapturing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
    val laserProgress by infiniteTransition.animateFloat(
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics { testTag = "in_app_camera_scanner_sheet" },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Bar dello Scanner In-App (con weight(1f) per non tagliare mai la X di chiusura!)
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
                    color = DavezBordeaux
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = AppleWhite,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Scanner Peritale & OCR",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppleWhite,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Fotocamera Live • ML Kit OCR On-Device",
                        style = MaterialTheme.typography.labelSmall,
                        color = DavezTravertine,
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
                    onClick = { flashEnabled = !flashEnabled },
                    shape = RoundedCornerShape(50),
                    color = if (flashEnabled) Color(0xFFF5D0A9) else Color(0xFF2B2625)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Flash",
                            tint = if (flashEnabled) Color(0xFF191716) else AppleWhite,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (flashEnabled) "Auto" else "Off",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (flashEnabled) Color(0xFF191716) else AppleWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Surface(
                    onClick = onClose,
                    shape = CircleShape,
                    color = Color(0xFF2B2625),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = AppleWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Mirino Live CameraX Reale + Guida Ottica CAD Leggera
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF121010))
                .border(1.dp, DavezBordeauxSoft.copy(alpha = 0.6f), RoundedCornerShape(26.dp))
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
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                cameraProvider.unbindAll()
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
                            } catch (_: Throwable) {}
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    }
                )
            } else {
                // Stato in cui l'utente non ha ancora concesso il permesso fotocamera
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
                        tint = Color(0xFFF5D0A9),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Consenti l'accesso alla Fotocamera per inquadrare documenti e rilievi peritali dal vivo",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleWhite,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { camPermLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = DavezBordeauxHover),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Attiva Fotocamera Live", color = AppleWhite)
                    }
                }
            }

            // Mirino Ottico Leggero: Angoli CAD di inquadratura + Raggio Laser (senza finto testo!)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val padX = w * 0.10f
                val padY = h * 0.14f
                val cornerLen = 28.dp.toPx()
                val strokeW = 3.dp.toPx()
                val bracketColor = Color(0xFFF5D0A9)

                // Angolo Alto-Sinistra
                drawLine(bracketColor, Offset(padX, padY), Offset(padX + cornerLen, padY), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(padX, padY), Offset(padX, padY + cornerLen), strokeW, StrokeCap.Round)
                // Angolo Alto-Destra
                drawLine(bracketColor, Offset(w - padX, padY), Offset(w - padX - cornerLen, padY), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(w - padX, padY), Offset(w - padX, padY + cornerLen), strokeW, StrokeCap.Round)
                // Angolo Basso-Sinistra
                drawLine(bracketColor, Offset(padX, h - padY), Offset(padX + cornerLen, h - padY), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(padX, h - padY), Offset(padX, h - padY - cornerLen), strokeW, StrokeCap.Round)
                // Angolo Basso-Destra
                drawLine(bracketColor, Offset(w - padX, h - padY), Offset(w - padX - cornerLen, h - padY), strokeW, StrokeCap.Round)
                drawLine(bracketColor, Offset(w - padX, h - padY), Offset(w - padX, h - padY - cornerLen), strokeW, StrokeCap.Round)

                // Raggio Laser di Scansione Orizzontale
                val laserY = h * laserProgress
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF34C759).copy(alpha = 0.85f),
                            Color(0xFFF5D0A9),
                            Color(0xFF34C759).copy(alpha = 0.85f),
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
                    .padding(top = 12.dp)
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
                    color = Color(0xFFF5D0A9),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }

            // Flash Bianco allo scatto
            if (shutterFlashAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = shutterFlashAlpha))
                )
            }
        }

        // Selettore Pillole Modalità Scanner + Raffica Sopralluogo Peritale
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ScannerModePill(
                label = "⚖️ Verbale",
                selected = !isBurstMode && selectedScanMode == 1,
                modifier = Modifier.weight(1f),
                onClick = {
                    isBurstMode = false
                    selectedScanMode = 1
                }
            )
            ScannerModePill(
                label = "🧱 Danni",
                selected = !isBurstMode && selectedScanMode == 2,
                modifier = Modifier.weight(1f),
                onClick = {
                    isBurstMode = false
                    selectedScanMode = 2
                }
            )
            ScannerModePill(
                label = "📐 Misure",
                selected = !isBurstMode && selectedScanMode == 3,
                modifier = Modifier.weight(1f),
                onClick = {
                    isBurstMode = false
                    selectedScanMode = 3
                }
            )
            ScannerModePill(
                label = if (burstList.isEmpty()) "⚡ Raffica" else "⚡ (${burstList.size})",
                selected = isBurstMode,
                modifier = Modifier.weight(1.15f),
                onClick = {
                    isBurstMode = !isBurstMode
                }
            )
        }

        // Barra Inferiore Otturatore Stile iOS Camera: [🖼️ Galleria] + [🔘 Scatta Foto Reale] + [📷 App Foto]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onPickFromGallery,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
            ) {
                Text("🖼️ Galleria", color = AppleWhite, fontSize = 12.sp)
            }

            // Pulsante Otturatore Tattile a Doppio Anello iOS (Scatta FOTO VERA con CameraX!)
            val shutterInteraction = remember { MutableInteractionSource() }
            Surface(
                onClick = {
                    if (isCapturing) return@Surface
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        shutterFlashAlpha = 0.85f
                        delay(110L)
                        shutterFlashAlpha = 0f
                    }
                    if (hasCameraPermission) {
                        isCapturing = true
                        imageCapture.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    isCapturing = false
                                    try {
                                        val rawBitmap = image.toBitmap()
                                        val rotation = image.imageInfo.rotationDegrees
                                        val finalBitmap = if (rotation != 0) {
                                            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                                            Bitmap.createBitmap(
                                                rawBitmap,
                                                0,
                                                0,
                                                rawBitmap.width,
                                                rawBitmap.height,
                                                matrix,
                                                true
                                            )
                                        } else {
                                            rawBitmap
                                        }
                                        image.close()
                                        if (isBurstMode) {
                                            burstList.add(finalBitmap)
                                            Toast.makeText(context, "📸 Foto #${burstList.size} acquisita!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            onCaptureRealBitmap(finalBitmap, selectedScanMode)
                                        }
                                    } catch (_: Throwable) {
                                        try { image.close() } catch (_: Throwable) {}
                                        Toast.makeText(context, "Errore scatto fotocamera", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    isCapturing = false
                                    Toast.makeText(context, "Impossibile acquisire la foto", Toast.LENGTH_SHORT).show()
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
                border = BorderStroke(4.dp, AppleWhite),
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
                        .background(DavezBordeauxHover),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = AppleWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            OutlinedButton(
                onClick = onOpenSystemCamera,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
            ) {
                Text("📷 Sistema", color = AppleWhite, fontSize = 12.sp)
            }
        }

        // Se in modalità raffica con foto già scattate, mostra il pulsante di conferma e salvataggio batch
        if (isBurstMode && burstList.isNotEmpty()) {
            Button(
                onClick = {
                    val batch = burstList.toList()
                    burstList.clear()
                    onCaptureBurstBitmaps?.invoke(batch)
                },
                colors = ButtonDefaults.buttonColors(containerColor = DavezBordeaux),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "📁 Salva Raffica (${burstList.size} foto) nel Fascicolo",
                    fontWeight = FontWeight.Bold,
                    color = AppleWhite,
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
        color = if (selected) DavezBordeauxHover else Color(0xFF2B2423),
        border = BorderStroke(1.dp, if (selected) Color(0xFFF5D0A9) else Color.White.copy(alpha = 0.12f)),
        modifier = modifier.height(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) AppleWhite else DavezTravertine,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PeritoQuickPill(
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(50),
        color = AppleBlueSoft,
        border = BorderStroke(0.8.dp, DavezBordeaux.copy(alpha = 0.22f)),
        modifier = Modifier
            .height(36.dp)
            .emilPressScale(interactionSource, pressedScale = 0.95f)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = DavezBordeaux,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// =============================================================================
// BOLLA MESSAGGIO STILE WHATSAPP / IMESSAGE CON NOTA VOCALE RIPRODUCIBILE,
// CARD ALLEGATI SQUIRCLE E TASTIERA INLINE ARROTONDATA
// =============================================================================
@Composable
fun PeritoStudioMessageBubble(
    message: ChatMessage,
    isClearingHistory: Boolean = false,
    viewModel: PeritoViewModel,
    onOpenDocument: (ArchivedDocument) -> Unit,
    onShareDocument: (ArchivedDocument) -> Unit,
    onOpenMaps: (String) -> Unit
) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleBg = if (isUser) DavezBordeaux else AppleWhite
    val textColor = if (isUser) AppleWhite else AppleLabelPrimary
    val timeColor = if (isUser) AppleWhite.copy(alpha = 0.78f) else AppleLabelTertiary

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }
    var showRescheduleDialog by remember { mutableStateOf(false) }

    val textSelectionColors = remember(isUser) {
        TextSelectionColors(
            handleColor = if (isUser) AppleWhite else DavezBordeaux,
            backgroundColor = if (isUser) AppleWhite.copy(alpha = 0.35f) else DavezBordeaux.copy(alpha = 0.22f)
        )
    }

    // Cartoonish Dissolve / Vaporize transition on Clear Chat History
    val dissolveProgress by animateFloatAsState(
        targetValue = if (isClearingHistory) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "dissolve_vaporize"
    )

    // Emil Kowalski Cartoon Physics: Bouncy spring entrance from 0.82f on GPU layer
    val entranceProgress = remember(message.id) { Animatable(0.82f) }
    LaunchedEffect(message.id) {
        if (ValueAnimator.areAnimatorsEnabled()) {
            entranceProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.58f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        } else {
            entranceProgress.snapTo(1f)
        }
    }

    val isRecentAssistantMsg = remember(message.id) {
        !isUser && (System.currentTimeMillis() - message.timestampMillis) < 2500L
    }
    val formattedFullText = remember(message.text, isUser) {
        formatPeritoMessageText(message.text, isUser)
    }
    var visibleChars by remember(message.id) {
        mutableIntStateOf(if (isRecentAssistantMsg) 24.coerceAtMost(formattedFullText.length) else formattedFullText.length)
    }
    LaunchedEffect(message.id, formattedFullText.length) {
        if (isRecentAssistantMsg && ValueAnimator.areAnimatorsEnabled()) {
            val total = formattedFullText.length
            val step = (total / 10).coerceAtLeast(18)
            while (visibleChars < total) {
                delay(22L)
                visibleChars = (visibleChars + step).coerceAtMost(total)
            }
        } else {
            visibleChars = formattedFullText.length
        }
    }

    val displayedText = if (visibleChars >= formattedFullText.length) {
        formattedFullText
    } else {
        formattedFullText.subSequence(0, visibleChars)
    }

    val timeFormatted = remember(message.timestampMillis) {
        SimpleDateFormat("HH:mm", Locale.ITALIAN).format(Date(message.timestampMillis))
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val maxBubbleWidth = remember(screenWidthDp) {
        (screenWidthDp * 0.86f).coerceIn(260.dp, 560.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                if (dissolveProgress > 0f) {
                    val p = dissolveProgress
                    // Cartoon Vaporize: Puff up slightly, float upward, tilt and fade out
                    val puffScale = if (p < 0.25f) {
                        1f + (p / 0.25f) * 0.08f
                    } else {
                        1.08f - ((p - 0.25f) / 0.75f) * 0.70f
                    }
                    scaleX = puffScale
                    scaleY = puffScale
                    alpha = (1f - p * 1.25f).coerceIn(0f, 1f)
                    translationY = -p * 140f
                    rotationZ = if (isUser) p * 7f else -p * 7f
                } else {
                    scaleX = entranceProgress.value
                    scaleY = entranceProgress.value
                    alpha = ((entranceProgress.value - 0.80f) / 0.20f).coerceIn(0f, 1f)
                    translationY = (1f - entranceProgress.value) * 45f
                }
            },
        horizontalAlignment = alignment
    ) {
        // Bolla principale del messaggio con angoli Squircle morbidi (22.dp)
        Surface(
            color = bubbleBg,
            shadowElevation = if (isUser) 2.dp else 1.dp,
            border = if (isUser) null else BorderStroke(0.8.dp, DavezStoneBorder),
            shape = RoundedCornerShape(
                topStart = 22.dp,
                topEnd = 22.dp,
                bottomStart = if (isUser) 22.dp else 6.dp,
                bottomEnd = if (isUser) 6.dp else 22.dp
            ),
            modifier = Modifier.widthIn(min = 90.dp, max = maxBubbleWidth)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                // 1. Se è una Nota Vocale registrata in-app -> Player Onda Sonora stile WhatsApp con Voce Reale Utente!
                if (message.voiceDurationSeconds != null || message.attachmentType == "VOICE") {
                    WhatsAppVoiceNoteCard(
                        durationSeconds = message.voiceDurationSeconds ?: 4,
                        isUser = isUser,
                        voiceAudioPath = message.voiceAudioPath,
                        spokenText = message.text
                    )
                    Spacer(Modifier.height(6.dp))
                } else if (message.attachmentName != null) {
                    // 2. Se è un Allegato (PDF, .p7m, Scansione Peritale) -> Card Allegato Squircle Moderna!
                    ModernAttachmentBubbleCard(
                        fileName = message.attachmentName,
                        attachmentType = message.attachmentType ?: "PDF",
                        isUser = isUser
                    )
                    Spacer(Modifier.height(6.dp))
                }

                // Testo del messaggio (o trascrizione vocale) con Selezione Testo stile WhatsApp (SelectionContainer)
                if (message.attachmentName == null || message.voiceDurationSeconds != null) {
                    CompositionLocalProvider(LocalTextSelectionColors provides textSelectionColors) {
                        SelectionContainer {
                            Text(
                                text = displayedText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor,
                                lineHeight = 21.sp,
                                modifier = Modifier.semantics {
                                    contentDescription = formattedFullText.text
                                }
                            )
                        }
                    }
                }

                // Badge Visivo di Verifica Appuntamento (/agentic-craft-ui Pillar 3)
                message.createdEvent?.let { ev ->
                    Spacer(Modifier.height(8.dp))
                    AppointmentVerificationSubCard(event = ev)
                }

                Spacer(Modifier.height(5.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Azioni rapide stile WhatsApp / Claude: Copia intero testo & Condividi risultati agente
                    if (message.text.isNotBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Tasto Rapido Copia Tutto
                            Surface(
                                shape = RoundedCornerShape(7.dp),
                                color = if (isUser) AppleWhite.copy(alpha = 0.16f) else DavezAlabaster,
                                modifier = Modifier.clickable {
                                    clipboardManager.setText(AnnotatedString(message.text))
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isCopied = true
                                    Toast.makeText(context, "Messaggio copiato negli appunti", Toast.LENGTH_SHORT).show()
                                    coroutineScope.launch {
                                        delay(1800)
                                        isCopied = false
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = "Copia messaggio",
                                        tint = if (isCopied) AppleGreen else timeColor,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = if (isCopied) "Copiato" else "Copia",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (isCopied) AppleGreen else timeColor
                                    )
                                }
                            }

                            // 2. Tasto Rapido Condividi Risultati Agente / Messaggio
                            Surface(
                                shape = RoundedCornerShape(7.dp),
                                color = if (isUser) AppleWhite.copy(alpha = 0.16f) else DavezAlabaster,
                                modifier = Modifier.clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, message.text)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Condividi messaggio")
                                    context.startActivity(shareIntent)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Condividi messaggio",
                                        tint = timeColor,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "Condividi",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = timeColor
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            letterSpacing = (-0.2).sp
                        ),
                        color = timeColor
                    )
                }
            }
        }

        // =====================================================================
        // TASTIERA INLINE SQUIRCLE MODERNA (SOTTO LA BOLLA)
        // =====================================================================

        // 1. TASTIERA INLINE CONFLITTO D'AGENDA (±15 MIN)
        message.pendingConflict?.let { conflict ->
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier.widthIn(max = maxBubbleWidth),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PeritoStudioInlineButton(
                    text = "Sostituisci precedente",
                    leadingIcon = Icons.Default.Sync,
                    containerColor = DavezBordeaux,
                    contentColor = AppleWhite,
                    onClick = { viewModel.resolveConflictReplace(message.id, conflict) }
                )
                PeritoStudioInlineButton(
                    text = "Inserisci comunque",
                    leadingIcon = Icons.Default.Warning,
                    containerColor = AppleOrangeSoft,
                    contentColor = AppleOrange,
                    borderColor = AppleOrange.copy(alpha = 0.45f),
                    onClick = { viewModel.resolveConflictForce(message.id, conflict) }
                )
                PeritoStudioInlineButton(
                    text = "Annulla",
                    leadingIcon = Icons.Default.Close,
                    containerColor = AppleRedSoft,
                    contentColor = AppleRed,
                    borderColor = AppleRed.copy(alpha = 0.35f),
                    onClick = { viewModel.resolveConflictCancel(message.id) }
                )
            }
        }

        // 2. TASTIERA INLINE APPUNTAMENTO CONFERMATO -> APRI GOOGLE MAPS + CONDIVIDI + ELIMINA
        message.createdEvent?.let { ev ->
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier.widthIn(max = maxBubbleWidth),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (ev.luogo != "Da definire") {
                    PeritoStudioInlineButton(
                        text = "Apri Google Maps (${ev.luogo})",
                        leadingIcon = Icons.Default.Place,
                        containerColor = AppleWhite,
                        contentColor = DavezBordeaux,
                        borderColor = DavezBordeaux.copy(alpha = 0.32f),
                        onClick = { onOpenMaps(ev.luogo) }
                    )
                }
                PeritoStudioInlineButton(
                    text = "Modifica Orario",
                    leadingIcon = Icons.Default.Edit,
                    containerColor = AppleWhite,
                    contentColor = DavezBordeaux,
                    borderColor = DavezBordeaux.copy(alpha = 0.32f),
                    onClick = { showRescheduleDialog = true }
                )
                PeritoStudioInlineButton(
                    text = "Condividi Appuntamento (WhatsApp / SMS)",
                    leadingIcon = Icons.Default.Share,
                    containerColor = AppleWhite,
                    contentColor = DavezBordeaux,
                    borderColor = DavezBordeaux.copy(alpha = 0.32f),
                    onClick = {
                        val shareText = buildString {
                            append("📅 Appuntamento: ${ev.titolo}\n")
                            append("🗓️ Data: ${ev.startIso.take(10)}\n")
                            append("🕒 Orario: dalle ${ev.startIso.drop(11).take(5)} alle ${ev.endIso.drop(11).take(5)}\n")
                            if (ev.cliente.isNotBlank() && ev.cliente != "Cliente") append("👤 Con: ${ev.cliente}\n")
                            if (ev.luogo.isNotBlank() && ev.luogo != "Da definire") append("📍 Luogo: ${ev.luogo}\n")
                        }
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Condividi Appuntamento"))
                    }
                )
                PeritoStudioInlineButton(
                    text = "Elimina Appuntamento",
                    leadingIcon = Icons.Default.DeleteOutline,
                    containerColor = AppleRedSoft,
                    contentColor = AppleRed,
                    borderColor = AppleRed.copy(alpha = 0.30f),
                    onClick = { viewModel.deleteCalendarEvent(ev, notifyInChat = true) }
                )
            }

            if (showRescheduleDialog) {
                RescheduleAppointmentDialog(
                    event = ev,
                    onDismiss = { showRescheduleDialog = false },
                    onConfirm = { newTitle, newCategory, newStartIso, newEndIso, newNotes ->
                        viewModel.updateCalendarEvent(
                            event = ev,
                            newTitle = newTitle,
                            newCategory = newCategory,
                            newStartIso = newStartIso,
                            newEndIso = newEndIso,
                            newNotes = newNotes,
                            notifyInChat = true
                        )
                        showRescheduleDialog = false
                    }
                )
            }
        }

        // 3. TASTIERA INLINE BRIEFING /OGGI -> BOTTONI MAPS PER OGNI SOPRALLUOGO
        if (message.briefingEvents.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier.widthIn(max = maxBubbleWidth),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                message.briefingEvents.filter { it.luogo != "Da definire" }.forEach { ev ->
                    val startH = ev.startIso.drop(11).take(5)
                    PeritoStudioInlineButton(
                        text = "$startH • ${ev.luogo}",
                        leadingIcon = Icons.Default.Place,
                        containerColor = AppleWhite,
                        contentColor = DavezBordeaux,
                        borderColor = DavezBordeaux.copy(alpha = 0.32f),
                        onClick = { onOpenMaps(ev.luogo) }
                    )
                }
            }
        }

        // 4. TASTIERA INLINE DOCUMENTO ARCHIVIATO -> APRI / CONDIVIDI / ELIMINA + LE 6 CARTELLE SQUIRCLE
        message.archivedDocument?.let { doc ->
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier.widthIn(max = maxBubbleWidth),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PeritoStudioInlineButton(
                        text = "Apri",
                        leadingIcon = Icons.Default.Description,
                        containerColor = DavezBordeaux,
                        contentColor = AppleWhite,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenDocument(doc) }
                    )
                    PeritoStudioInlineButton(
                        text = "Invia",
                        leadingIcon = Icons.Default.Share,
                        containerColor = AppleWhite,
                        contentColor = DavezBordeaux,
                        borderColor = DavezBordeaux.copy(alpha = 0.30f),
                        modifier = Modifier.weight(1f),
                        onClick = { onShareDocument(doc) }
                    )
                    PeritoStudioInlineButton(
                        text = "Elimina",
                        leadingIcon = Icons.Default.DeleteOutline,
                        containerColor = AppleRedSoft,
                        contentColor = AppleRed,
                        borderColor = AppleRed.copy(alpha = 0.35f),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.deleteArchivedDocument(doc.id, notifyInChat = true) }
                    )
                }

                val categories = PeritoCategory.entries
                for (rowIndex in categories.indices step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (colIndex in 0..1) {
                            val idx = rowIndex + colIndex
                            if (idx < categories.size) {
                                val cat = categories[idx]
                                val isSelected = doc.category == cat
                                PeritoStudioInlineButton(
                                    text = cat.displayTitle,
                                    leadingIcon = if (isSelected) Icons.Default.Check else Icons.Default.Folder,
                                    containerColor = if (isSelected) AppleGreenSoft else AppleWhite,
                                    contentColor = if (isSelected) AppleLabelPrimary else DavezBordeaux,
                                    borderColor = if (isSelected) AppleGreen else DavezStoneBorder,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (!isSelected) {
                                            viewModel.changeDocumentCategory(doc.id, cat)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. CARD SQUIRCLE FILE TROVATI IN ARCHIVIO (/cerca o Fascicolo Cliente) CON TASTO ELIMINA
        if (message.matchedDocuments.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier.widthIn(max = maxBubbleWidth),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                message.matchedDocuments.forEach { doc ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = AppleWhite,
                        shadowElevation = 1.dp,
                        border = BorderStroke(0.8.dp, DavezStoneBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppleBlueSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = DavezBordeaux,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = doc.fileName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppleLabelPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "📂 ${doc.category.displayTitle} • ${doc.cliente.replace('_', ' ')}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppleLabelSecondary
                                )
                            }
                            IconButton(
                                onClick = { onOpenDocument(doc) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Apri",
                                    tint = DavezBordeaux,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onShareDocument(doc) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Condividi",
                                    tint = DavezBordeaux,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.deleteArchivedDocument(doc.id, notifyInChat = true) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Elimina file",
                                    tint = AppleRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// PLAYER NOTA VOCALE STILE WHATSAPP / IMESSAGE CON ONDA SONORA E VOCE REALE UTENTE (.WAV)
// =============================================================================
@Composable
fun WhatsAppVoiceNoteCard(
    durationSeconds: Int,
    isUser: Boolean,
    voiceAudioPath: String? = null,
    @Suppress("UNUSED_PARAMETER") spokenText: String = ""
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(voiceAudioPath) {
        onDispose {
            try {
                mediaPlayerRef?.stop()
            } catch (_: Throwable) {}
            try {
                mediaPlayerRef?.release()
            } catch (_: Throwable) {}
            mediaPlayerRef = null
        }
    }

    LaunchedEffect(isPlaying, voiceAudioPath) {
        if (isPlaying) {
            val wavFile = voiceAudioPath?.takeIf { it.isNotBlank() }?.let { File(it) }
            if (wavFile != null && wavFile.exists() && wavFile.length() > 44L) {
                var mp: MediaPlayer? = null
                try {
                    mp = MediaPlayer().apply {
                        setDataSource(wavFile.absolutePath)
                        prepare()
                        setOnCompletionListener {
                            isPlaying = false
                            progress = 0f
                        }
                        start()
                    }
                    mediaPlayerRef = mp
                    val totalMs = mp.duration.coerceAtLeast(durationSeconds * 1000).coerceAtLeast(500)
                    while (isPlaying && mp.isPlaying) {
                        progress = (mp.currentPosition.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
                        delay(40L)
                    }
                } catch (_: Throwable) {
                    Toast.makeText(context, "Impossibile riprodurre il file vocale", Toast.LENGTH_SHORT).show()
                } finally {
                    try {
                        mp?.stop()
                    } catch (_: Throwable) {}
                    try {
                        mp?.release()
                    } catch (_: Throwable) {}
                    if (mediaPlayerRef === mp) {
                        mediaPlayerRef = null
                    }
                    isPlaying = false
                    progress = 0f
                }
            } else {
                Toast.makeText(
                    context,
                    "Registrazione vocale originale non disponibile per questo messaggio",
                    Toast.LENGTH_SHORT
                ).show()
                isPlaying = false
                progress = 0f
            }
        } else {
            try {
                mediaPlayerRef?.stop()
            } catch (_: Throwable) {}
            try {
                mediaPlayerRef?.release()
            } catch (_: Throwable) {}
            mediaPlayerRef = null
            progress = 0f
        }
    }

    val cardBg = if (isUser) Color.White.copy(alpha = 0.14f) else DavezAlabaster
    val accent = if (isUser) Color(0xFFF5D0A9) else DavezBordeaux
    val unplayed = if (isUser) Color.White.copy(alpha = 0.35f) else DavezStoneBorder

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { testTag = "whatsapp_voice_note_card" }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val playInteraction = remember { MutableInteractionSource() }
            Surface(
                onClick = { isPlaying = !isPlaying },
                interactionSource = playInteraction,
                shape = CircleShape,
                color = accent,
                modifier = Modifier
                    .size(38.dp)
                    .emilPressScale(playInteraction)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausa vocale" else "Riproduci la tua voce reale",
                        tint = if (isUser) DavezBordeaux else AppleWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                ) {
                    val bars = 26
                    val space = size.width / bars
                    val bw = (space * 0.56f).coerceAtLeast(3f)
                    for (i in 0 until bars) {
                        val frac = i.toFloat() / bars.toFloat()
                        val seed = abs(sin(i * 1.15f + 0.4f))
                        val bh = (size.height * (0.28f + 0.68f * seed)).coerceIn(4.dp.toPx(), size.height)
                        val x = i * space + (space - bw) / 2f
                        val y = (size.height - bh) / 2f
                        drawRoundRect(
                            color = if (frac <= progress || (!isPlaying && i < 6)) accent else unplayed,
                            topLeft = Offset(x, y),
                            size = Size(bw, bh),
                            cornerRadius = CornerRadius(bw / 2f, bw / 2f)
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val mins = durationSeconds / 60
                    val secs = durationSeconds % 60
                    Text(
                        text = String.format(Locale.ITALIAN, "🎙️ %d:%02d", mins, secs),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = if (isUser) AppleWhite.copy(alpha = 0.9f) else DavezBordeaux,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Voce Originale • Trascrizione Live",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) Color(0xFFF5D0A9) else AppleLabelSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// =============================================================================
// CARD ALLEGATO MODERNA SQUIRCLE DENTRO LA BOLLA CHAT (PDF, .P7M, SCANSIONE 3D)
// =============================================================================
@Composable
fun ModernAttachmentBubbleCard(
    fileName: String,
    attachmentType: String,
    isUser: Boolean
) {
    val (emoji, badgeLabel, badgeColor) = when (attachmentType) {
        "P7M" -> Triple("🔐", "BUSTA CADES .P7M", Color(0xFFF5D0A9))
        "CAMERA_SCAN" -> Triple("📸", "SCANSIONE RILIEVO OCR", Color(0xFFA7F3D0))
        else -> Triple("📄", "DOCUMENTO PDF TECNICO", Color(0xFFEFE7E2))
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isUser) Color.White.copy(alpha = 0.14f) else DavezAlabaster,
        border = BorderStroke(
            0.8.dp,
            if (isUser) Color.White.copy(alpha = 0.22f) else DavezStoneBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { testTag = "modern_attachment_bubble_card" }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (isUser) Color.White.copy(alpha = 0.20f) else AppleBlueSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 20.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = badgeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser) badgeColor else DavezBordeaux,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isUser) AppleWhite else AppleLabelPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AppointmentVerificationSubCard(event: CalendarEventItem) {
    val startH = event.startIso.drop(11).take(5)
    val endH = event.endIso.drop(11).take(5)

    // Cartoon celebratory pop on mount
    val cardScale = remember { Animatable(0.90f) }
    LaunchedEffect(Unit) {
        if (ValueAnimator.areAnimatorsEnabled()) {
            cardScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.55f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        } else {
            cardScale.snapTo(1f)
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DavezAlabaster,
        border = BorderStroke(0.8.dp, AppleGreen.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale.value
                scaleY = cardScale.value
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = AppleWhite,
                border = BorderStroke(0.5.dp, DavezStoneBorder.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = DavezBordeaux,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "$startH–$endH",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = AppleLabelPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = AppleLabelSecondary,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "3 Alert: 24h • 60m • 30m",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppleLabelSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun PeritoStudioInlineButton(
    text: String,
    leadingIcon: ImageVector? = null,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        shadowElevation = 0.5.dp,
        border = borderColor?.let { BorderStroke(0.6.dp, it) },
        modifier = modifier
            .heightIn(min = 44.dp)
            .cartoonPressSquash(interactionSource, pressedScaleY = 0.93f, squashScaleX = 1.04f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = (-0.2).sp
                ),
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Converte la sintassi Markdown leggera (**grassetto** e `codice`) in AnnotatedString nativa.
 */
fun formatPeritoMessageText(raw: String, isUser: Boolean): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val n = raw.length
        while (i < n) {
            if (i + 1 < n && raw[i] == '*' && raw[i + 1] == '*') {
                val end = raw.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(raw.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }
            if (raw[i] == '*') {
                val end = raw.indexOf('*', i + 1)
                if (end != -1 && end > i + 1 && !raw.substring(i + 1, end).contains('\n')) {
                    withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                        append(raw.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }
            if (raw[i] == '`') {
                val end = raw.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isUser) Color(0xFFF5D0A9) else DavezBordeaux
                        )
                    ) {
                        append(raw.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }
            append(raw[i])
            i++
        }
    }
}

// =============================================================================
// FOGLIO MODALE IOS: ARCHIVIO 6 CARTELLE UFFICIALI DEL PERITO EDILE
// =============================================================================
@Composable
fun ArchiveFoldersSheetContent(
    uiState: PeritoUiState,
    viewModel: PeritoViewModel,
    onCameraClick: () -> Unit,
    onPickDocumentClick: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val filteredDocs = remember(uiState.archivedDocuments, uiState.selectedCategoryFilter) {
        if (uiState.selectedCategoryFilter == null) {
            uiState.archivedDocuments
        } else {
            uiState.archivedDocuments.filter { it.category == uiState.selectedCategoryFilter }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.88f)
            .background(AppleWhite)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📂 Archivio 6 Cartelle Perito",
                style = MaterialTheme.typography.titleLarge,
                color = AppleLabelPrimary
            )
            TextButton(onClick = onClose) {
                Text("Chiudi", color = DavezBordeaux, fontWeight = FontWeight.SemiBold)
            }
        }

        // Ricerca stile iOS
        OutlinedTextField(
            value = uiState.archiveSearchQuery,
            onValueChange = { viewModel.setArchiveSearchQuery(it) },
            placeholder = { Text("Cerca per cliente, CTU, condominio, fattura...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AppleLabelSecondary) },
            trailingIcon = {
                if (uiState.archiveSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setArchiveSearchQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Pulisci")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        // Filtri per le 6 Cartelle con Palette Bordeaux Coerente
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.selectedCategoryFilter == null,
                onClick = { viewModel.setCategoryFilter(null) },
                label = { Text("Tutte (${uiState.archivedDocuments.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DavezBordeaux,
                    selectedLabelColor = AppleWhite
                )
            )
            PeritoCategory.entries.forEach { cat ->
                val count = uiState.archivedDocuments.count { it.category == cat }
                FilterChip(
                    selected = uiState.selectedCategoryFilter == cat,
                    onClick = { viewModel.setCategoryFilter(cat) },
                    label = { Text("${cat.displayTitle} ($count)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DavezBordeaux,
                        selectedLabelColor = AppleWhite
                    )
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = AppleSecondaryGroupedBg)

        if (filteredDocs.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = AppleLabelTertiary
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Nessun documento in questa cartella",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppleLabelPrimary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Allega un PDF/.p7m o scatta una foto direttamente nella chat per classificarlo automaticamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppleLabelSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onCameraClick,
                        colors = ButtonDefaults.buttonColors(containerColor = DavezBordeaux),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("📸 Scatta Foto", color = AppleWhite)
                    }
                    OutlinedButton(
                        onClick = onPickDocumentClick,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("📎 Scegli File", color = DavezBordeaux)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredDocs, key = { it.id }) { doc ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        colors = CardDefaults.cardColors(containerColor = DavezAlabaster),
                        border = BorderStroke(0.8.dp, DavezStoneBorder),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📂 ${doc.category.displayTitle}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DavezBordeaux,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = doc.dateIso,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = AppleLabelSecondary
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = doc.fileName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppleLabelPrimary
                            )
                            Text(
                                text = "👤 ${doc.cliente.replace('_', ' ')} • 🏷️ ${doc.oggetto.replace('_', ' ')}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppleLabelSecondary
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            viewModel.archiveRepo.createShareOrOpenIntent(doc, asShare = false)?.let {
                                                context.startActivity(it)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DavezBordeaux),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Apri", color = AppleWhite)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.archiveRepo.createShareOrOpenIntent(doc, asShare = true)?.let {
                                                context.startActivity(Intent.createChooser(it, "Condividi ${doc.fileName}"))
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Condividi", color = DavezBordeaux)
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteArchivedDocument(doc.id) }) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Elimina",
                                        tint = AppleRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// FOGLIO MODALE IOS INTUITIVO: IMPOSTAZIONI RAPIDE & MODELLI IA ON-DEVICE
// =============================================================================
@Composable
fun AiEngineSettingsSheetContent(
    uiState: PeritoUiState,
    viewModel: PeritoViewModel,
    onRequestCalendarPerm: () -> Unit,
    onClose: () -> Unit
) {
    var briefingEnabled by remember(uiState.aiConfig.morningBriefingEnabled) {
        mutableStateOf(uiState.aiConfig.morningBriefingEnabled)
    }
    var studioNameInput by remember(uiState.aiConfig.studioProfile.studioName) {
        mutableStateOf(uiState.aiConfig.studioProfile.studioName)
    }
    var professionistaInput by remember(uiState.aiConfig.studioProfile.professionistaNome) {
        mutableStateOf(uiState.aiConfig.studioProfile.professionistaNome)
    }
    var alboOrdineInput by remember(uiState.aiConfig.studioProfile.alboIscrizione) {
        mutableStateOf(uiState.aiConfig.studioProfile.alboIscrizione)
    }
    var cittaInput by remember(uiState.aiConfig.studioProfile.citta) {
        mutableStateOf(uiState.aiConfig.studioProfile.citta)
    }
    var recapitoInput by remember(uiState.aiConfig.studioProfile.recapito) {
        mutableStateOf(uiState.aiConfig.studioProfile.recapito)
    }
    var showManifestUrlEditor by rememberSaveable { mutableStateOf(false) }
    var editedManifestUrl by rememberSaveable(uiState.aiConfig.otaManifestUrl) {
        mutableStateOf(uiState.aiConfig.otaManifestUrl)
    }
    var showCalendarAccountDialog by remember { mutableStateOf(false) }

    if (showCalendarAccountDialog) {
        SelectCalendarAccountDialog(
            availableCalendars = uiState.availableCalendars,
            selectedCalendarId = uiState.aiConfig.selectedCalendarId,
            onSelectCalendar = { viewModel.selectSystemCalendar(it) },
            onDismiss = { showCalendarAccountDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .background(AppleWhite)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Header FISSO in alto con Brand PeritoAI e pulsante di chiusura Apple HIG
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                PeritoAiBrandEmblem(size = 36.dp, showOnlineBadge = false)
                Column {
                    Text(
                        text = "Impostazioni & Modelli IA",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppleLabelPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "PeritoAI • Download in Background & Privacy",
                        style = MaterialTheme.typography.labelSmall,
                        color = DavezBordeaux,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Surface(
                onClick = onClose,
                shape = CircleShape,
                color = AppleSecondaryGroupedBg.copy(alpha = 0.85f),
                modifier = Modifier
                    .size(32.dp)
                    .semantics { contentDescription = "Chiudi impostazioni" }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Chiudi impostazioni",
                        tint = AppleLabelPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = DavezStoneBorder)

        // Contenuto scorrevole fluido a 60fps con LazyColumn (nessun conflitto col BottomSheet grazie a sheetGesturesEnabled=false)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // SEZIONE 0A: PROFILO STUDIO TECNICO (Personalizzabile dal Perito)
            item(key = "settings_studio_profile_section") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AppleGroupedBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🏛️ Profilo Studio Tecnico",
                                style = MaterialTheme.typography.titleSmall,
                                color = AppleLabelPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AppleBlueSoft
                            ) {
                                Text(
                                    text = "Personalizzato",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DavezBordeaux,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = "Configura l'intestazione dello studio per i fascicoli, le perizie e la barra superiore.",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppleLabelSecondary
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AppleWhite,
                            border = BorderStroke(0.5.dp, AppleSecondaryGroupedBg),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = studioNameInput,
                                    onValueChange = { studioNameInput = it },
                                    label = { Text("Nome Studio Tecnico") },
                                    placeholder = { Text("Es. Studio Tecnico Peritale") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DavezBordeaux,
                                        cursorColor = DavezBordeaux
                                    )
                                )

                                OutlinedTextField(
                                    value = professionistaInput,
                                    onValueChange = { professionistaInput = it },
                                    label = { Text("Professionista Responsabile") },
                                    placeholder = { Text("Es. Geom. Mario Rossi") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DavezBordeaux,
                                        cursorColor = DavezBordeaux
                                    )
                                )

                                OutlinedTextField(
                                    value = alboOrdineInput,
                                    onValueChange = { alboOrdineInput = it },
                                    label = { Text("Albo / Ordine Professionale") },
                                    placeholder = { Text("Es. Collegio Geometri Monza e Brianza") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DavezBordeaux,
                                        cursorColor = DavezBordeaux
                                    )
                                )

                                OutlinedTextField(
                                    value = cittaInput,
                                    onValueChange = { cittaInput = it },
                                    label = { Text("Città Sede Studio") },
                                    placeholder = { Text("Es. Monza (MB)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DavezBordeaux,
                                        cursorColor = DavezBordeaux
                                    )
                                )

                                OutlinedTextField(
                                    value = recapitoInput,
                                    onValueChange = { recapitoInput = it },
                                    label = { Text("Email / PEC / Telefono") },
                                    placeholder = { Text("Es. studio@perizietecniche.it") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = DavezBordeaux,
                                        cursorColor = DavezBordeaux
                                    )
                                )

                                val isModified = studioNameInput != uiState.aiConfig.studioProfile.studioName ||
                                    professionistaInput != uiState.aiConfig.studioProfile.professionistaNome ||
                                    alboOrdineInput != uiState.aiConfig.studioProfile.alboIscrizione ||
                                    cittaInput != uiState.aiConfig.studioProfile.citta ||
                                    recapitoInput != uiState.aiConfig.studioProfile.recapito

                                Button(
                                    onClick = {
                                        viewModel.updateStudioProfile(
                                            StudioProfile(
                                                studioName = studioNameInput.trim().ifBlank { "Studio Tecnico Peritale" },
                                                professionistaNome = professionistaInput.trim(),
                                                alboIscrizione = alboOrdineInput.trim(),
                                                citta = cittaInput.trim(),
                                                recapito = recapitoInput.trim()
                                            )
                                        )
                                    },
                                    enabled = isModified && studioNameInput.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = DavezBordeaux),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("💾 Salva Intestazione Studio", color = AppleWhite, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // SEZIONE 0: STATO SERVIZI & AUTOMAZIONI RAPIDE (1-Tap)
            item(key = "settings_services_section") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AppleGroupedBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "⚡ Servizi & Sincronizzazione Rapida",
                            style = MaterialTheme.typography.titleSmall,
                            color = AppleLabelPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        // Riga 1: Calendario Android/Google
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AppleWhite,
                            border = BorderStroke(0.5.dp, AppleSecondaryGroupedBg),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val isCalendarSyncActive = uiState.hasCalendarPermission && uiState.aiConfig.syncSystemCalendar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "📅 Calendario Telefono (±15 min)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppleLabelPrimary
                                    )
                                    Text(
                                        text = when {
                                            isCalendarSyncActive ->
                                                "Sincronizzato con Google/Android Calendar (3 promemoria automatici)"
                                            !uiState.hasNativeCalendarPermission ->
                                                "Permessi non concessi • Tocca per sincronizzare col telefono"
                                            else ->
                                                "Scollegato • Modalità Agenda Locale attiva (nessun sync con Google Calendar)"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppleLabelSecondary
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                if (isCalendarSyncActive) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = AppleGreenSoft
                                        ) {
                                            Text(
                                                text = "✅ Attivo",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AppleLabelPrimary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.disconnectCalendar() },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppleRed),
                                            border = BorderStroke(1.dp, AppleRed.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LinkOff,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = AppleRed
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text("Scollega", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppleRed)
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = onRequestCalendarPerm,
                                        colors = ButtonDefaults.buttonColors(containerColor = DavezBordeaux),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = AppleWhite
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Collega", fontSize = 12.sp, color = AppleWhite, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        // Selettore Account / Calendario specifico
                        val isCalendarSyncActive = uiState.hasCalendarPermission && uiState.aiConfig.syncSystemCalendar
                        if (isCalendarSyncActive) {
                            Surface(
                                onClick = { showCalendarAccountDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                color = AppleWhite,
                                border = BorderStroke(0.5.dp, AppleSecondaryGroupedBg),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "🎯 Account Calendario Sincronizzato",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AppleLabelPrimary
                                        )
                                        val calendarLabel = remember(uiState.aiConfig.selectedCalendarName, uiState.aiConfig.selectedCalendarAccount) {
                                            val name = uiState.aiConfig.selectedCalendarName
                                            val acc = uiState.aiConfig.selectedCalendarAccount
                                            when {
                                                name == null -> "Tutti gli account (sincronizzazione globale)"
                                                acc.isNullOrBlank() || acc.equals(name, ignoreCase = true) -> name
                                                else -> "$name ($acc)"
                                            }
                                        }
                                        Text(
                                            text = calendarLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DavezBordeaux,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    OutlinedButton(
                                        onClick = { showCalendarAccountDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(0.8.dp, DavezStoneBorder),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Cambia", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppleLabelPrimary)
                                    }
                                }
                            }
                        }

                        // Riga 2: Briefing Mattutino ore 08:00
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AppleWhite,
                            border = BorderStroke(0.5.dp, AppleSecondaryGroupedBg),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "☀️ Briefing Mattutino (ore 08:00)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppleLabelPrimary
                                    )
                                    Text(
                                        text = "Riepilogo automatico sopralluoghi di oggi, domani e fascicoli clienti",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppleLabelSecondary
                                    )
                                }
                                Switch(
                                    checked = briefingEnabled,
                                    onCheckedChange = {
                                        briefingEnabled = it
                                        viewModel.updateAiConfig(uiState.aiConfig.copy(morningBriefingEnabled = it))
                                    },
                                    colors = SwitchDefaults.colors(checkedTrackColor = AppleGreen)
                                )
                            }
                        }

                        // Riga 3: Sincronizzazione 6 Cartelle col PC (Documents/Perizie_Archivio)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AppleWhite,
                            border = BorderStroke(0.5.dp, AppleSecondaryGroupedBg),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "💻 Cartella Sync PC (6 Cartelle)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppleLabelPrimary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AppleGreenSoft
                                    ) {
                                        Text(
                                            text = "📂 Mirror Attivo",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AppleLabelPrimary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Percorso telefono: /storage/emulated/0/Documents/Perizie_Archivio/\nPronta per sincronizzazione automatica col PC tramite Syncthing (P2P zero-cloud), OneDrive/Onesync o Dropbox/Dropsync.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppleLabelSecondary
                                )
                            }
                        }
                    }
                }
            }

            // SEZIONE 1: MODELLI IA ON-DEVICE STUDIO RESTELLI
            item(key = "settings_models_section") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AppleGroupedBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🧠 Motore IA & Download Modelli",
                                style = MaterialTheme.typography.titleSmall,
                                color = DavezBordeaux,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (uiState.isReasoningReady) AppleGreenSoft else AppleOrangeSoft
                            ) {
                                Text(
                                    text = if (uiState.isReasoningReady) "Reasoning Attivo" else "Pronto all'Uso",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (uiState.isReasoningReady) AppleGreen else AppleOrange,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = "Il download avviene anche in background con notifica di avanzamento nella barra di stato Android. Puoi chiudere questa finestra e continuare a usare l'app mentre scarica:",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleLabelSecondary
                        )

                        ModelDownloadCatalogList(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                    }
                }
            }

            // SEZIONE OTA: AGGIORNAMENTI & VERSIONE APP
            item(key = "settings_ota_updates_section") {
                val context = LocalContext.current

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AppleGroupedBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔄 Aggiornamenti In-App (OTA)",
                                style = MaterialTheme.typography.titleSmall,
                                color = DavezBordeaux,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DavezBordeaux.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "v${uiState.currentAppVersionName} (${uiState.currentAppVersionCode})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = DavezBordeaux,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = "Verifica la presenza di nuove release APK e aggiorna l'app senza dover reinstallare manualmente da cavo o file manager.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleLabelSecondary
                        )

                        // Scheda Notifica Aggiornamento Disponibile (se presente)
                        uiState.availableUpdate?.let { update ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AppleWhite,
                                border = BorderStroke(1.dp, DavezBordeaux.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🚀 Nuova Versione: v${update.latestVersionName}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = DavezBordeaux
                                        )
                                        Text(
                                            text = "Build ${update.latestVersionCode}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AppleLabelSecondary
                                        )
                                    }

                                    Text(
                                        text = update.releaseNotes.ifBlank { "Miglioramenti generali delle prestazioni e stabilità." },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppleLabelPrimary
                                    )

                                    if (uiState.updateDownloadProgress != null) {
                                        val prog = uiState.updateDownloadProgress
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = uiState.updateStatusMessage ?: "Download in corso...",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = AppleLabelSecondary
                                                )
                                                Text(
                                                    text = "${(prog * 100).toInt()}%",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DavezBordeaux
                                                )
                                            }
                                            LinearProgressIndicator(
                                                progress = { prog },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp)),
                                                color = DavezBordeaux,
                                                trackColor = AppleBlueSoft
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { viewModel.startUpdateDownloadAndInstall(context) },
                                        enabled = uiState.updateDownloadProgress == null,
                                        colors = ButtonDefaults.buttonColors(containerColor = DavezBordeaux),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (uiState.updateDownloadProgress != null) "Download in corso..." else "Scarica e Installa v${update.latestVersionName}",
                                            color = AppleWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Riga Switch: Controllo automatico all'avvio
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AppleWhite,
                            border = BorderStroke(0.5.dp, AppleSecondaryGroupedBg),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Controllo automatico all'avvio",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppleLabelPrimary
                                    )
                                    Text(
                                        text = "Verifica la disponibilità di nuove versioni all'apertura dell'app",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppleLabelSecondary
                                    )
                                }
                                Switch(
                                    checked = uiState.aiConfig.autoCheckUpdatesOnStart,
                                    onCheckedChange = { viewModel.toggleAutoCheckUpdates(it) },
                                    colors = SwitchDefaults.colors(checkedTrackColor = AppleGreen)
                                )
                            }
                        }

                        // Pulsante Verifica Manuale Aggiornamenti
                        Button(
                            onClick = { viewModel.checkForUpdates(silent = false) },
                            enabled = !uiState.isCheckingUpdate && uiState.updateDownloadProgress == null,
                            colors = ButtonDefaults.buttonColors(containerColor = DavezBordeaux),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 42.dp)
                        ) {
                            if (uiState.isCheckingUpdate) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = AppleWhite,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Verifica in corso...", color = AppleWhite, fontSize = 13.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = AppleWhite
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Verifica Aggiornamenti Ora", color = AppleWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }

                        // Feedback messaggio di stato (es: "L'app è aggiornata all'ultima versione")
                        if (!uiState.updateStatusMessage.isNullOrBlank() && uiState.availableUpdate == null) {
                            Text(
                                text = uiState.updateStatusMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = DavezBordeaux,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        // Configurazione URL Manifest (opzionale / per sviluppatori e self-hosting)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showManifestUrlEditor = !showManifestUrlEditor },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚙️ Endpoint Manifest OTA",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppleLabelSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (showManifestUrlEditor) "Nascondi" else "Configura",
                                style = MaterialTheme.typography.labelSmall,
                                color = DavezBordeaux,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (showManifestUrlEditor) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = editedManifestUrl,
                                    onValueChange = { editedManifestUrl = it },
                                    label = { Text("URL file ota-update.json", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            editedManifestUrl = OtaUpdateManager.DEFAULT_MANIFEST_URL
                                            viewModel.updateOtaManifestUrl(OtaUpdateManager.DEFAULT_MANIFEST_URL)
                                        }
                                    ) {
                                        Text("Predefinito", fontSize = 11.sp, color = AppleLabelSecondary)
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Button(
                                        onClick = { viewModel.updateOtaManifestUrl(editedManifestUrl) },
                                        colors = ButtonDefaults.buttonColors(containerColor = DavezBordeaux),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Salva URL", fontSize = 11.sp, color = AppleWhite, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "settings_bottom_spacer") {
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun ModelDownloadCatalogList(
    uiState: PeritoUiState,
    viewModel: PeritoViewModel
) {
    val context = LocalContext.current
    val progress = uiState.downloadProgress
    val primaryModel = viewModel.modelDownloadManager.primaryReasoningModel
    val powerfulModel = viewModel.modelDownloadManager.powerfulReasoningModel
    val fixedModels = viewModel.modelDownloadManager.fixedTaskModels

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val ensureNotificationPermAndRun: (() -> Unit) -> Unit = { action ->
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                runCatching { notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            }
        }
        action()
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Pulsante 1-Click per scaricare tutti i 3 modelli in sequenza in background (3.44 GB)
        if (!uiState.areAllModelsReady && !progress.isDownloading) {
            Button(
                onClick = {
                    ensureNotificationPermAndRun {
                        viewModel.downloadAllMissingModels()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DavezBordeaux),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(
                    text = "⬇️ Scarica Tutti i 3 Modelli in Background (3.44 GB)",
                    color = AppleWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                )
            }
        }

        if (progress.isDownloading) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AppleBlueSoft,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val queueTag = if (progress.totalQueueCount > 1) {
                        " (${progress.currentQueueIndex}/${progress.totalQueueCount})"
                    } else ""
                    Text(
                        text = "⬇️ Download in Background$queueTag: ${progress.modelTitle}",
                        style = MaterialTheme.typography.labelLarge,
                        color = AppleBlueDark,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "🔔 Notifica Android attiva: puoi chiudere questa finestra o mettere l'app in background.",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppleLabelSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress.progressFraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = AppleBlue
                    )
                    Spacer(Modifier.height(6.dp))
                    val dlMb = String.format(Locale.ITALIAN, "%.1f", progress.downloadedBytes / (1024.0 * 1024.0))
                    val totMb = if (progress.totalBytes > 0) {
                        String.format(Locale.ITALIAN, "%.1f MB", progress.totalBytes / (1024.0 * 1024.0))
                    } else "..."
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${progress.progressPercent}% ($dlMb MB / $totMb)",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleLabelPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(onClick = { viewModel.cancelModelDownload() }) {
                            Text("Pausa", color = AppleRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        progress.error?.let { err ->
            Text(
                text = "⚠️ $err",
                style = MaterialTheme.typography.bodySmall,
                color = AppleRed
            )
        }

        // 1. MOTORE DI REASONING ON-DEVICE / LOCALE
        Text(
            text = "1️⃣ MOTORE DI REASONING (BASE 2B O POTENZIATO 4B)",
            style = MaterialTheme.typography.labelSmall,
            color = DavezBordeaux,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Puoi selezionare la versione Base 2B (ultra-veloce e leggera) oppure sostituirla con la versione Potenziata 4B per deduzioni logiche e perizie complesse.",
            style = MaterialTheme.typography.bodySmall,
            color = AppleLabelSecondary
        )

        // Modello Base: Qwen 3.5 2B
        DownloadableModelCard(
            model = primaryModel,
            isDownloaded = uiState.downloadedModelIds.contains(primaryModel.id),
            isActiveReasoning = uiState.aiConfig.reasoningModelId == primaryModel.id,
            isDownloading = progress.isDownloading,
            onDownload = {
                ensureNotificationPermAndRun {
                    viewModel.startModelDownload(primaryModel)
                }
            },
            onDelete = { viewModel.deleteDownloadedModel(primaryModel) },
            onSelectActive = { viewModel.selectActiveReasoningModel(primaryModel.id) }
        )

        // Modello Potenziato: Qwen 3.5 4B Instruct (con tag frob/qwen3.5-instruct:4b)
        DownloadableModelCard(
            model = powerfulModel,
            isDownloaded = uiState.downloadedModelIds.contains(powerfulModel.id),
            isActiveReasoning = uiState.aiConfig.reasoningModelId == powerfulModel.id,
            isDownloading = progress.isDownloading,
            onDownload = {
                ensureNotificationPermAndRun {
                    viewModel.startModelDownload(powerfulModel)
                }
            },
            onDelete = { viewModel.deleteDownloadedModel(powerfulModel) },
            onSelectActive = { viewModel.selectActiveReasoningModel(powerfulModel.id) }
        )

        Spacer(Modifier.height(4.dp))

        // 2. TRASCRIZIONE VOCALE INTEGRATA IN-APP
        Text(
            text = "🎙️ TRASCRIZIONE VOCALE INTEGRATA IN-APP",
            style = MaterialTheme.typography.labelSmall,
            color = AppleGreen,
            fontWeight = FontWeight.Bold
        )
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = AppleGreenSoft.copy(alpha = 0.45f),
            border = BorderStroke(1.2.dp, AppleGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎙️ TRASCRIZIONE VOCALE NATIVA",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppleGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AppleWhite
                    ) {
                        Text(
                            text = "0 MB • Integrato",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = AppleGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Google Speech Service In-App",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppleLabelPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Integrato direttamente nel registratore vocale: salva l'audio originale .wav della voce dell'utente e genera la trascrizione del testo in tempo reale.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppleLabelSecondary
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // 3. MOTORE OCR & VISIONE ON-DEVICE (ML Kit Nativo)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, DavezStoneBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "📄 OCR Documentale & Visione Foto Rilievi",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppleLabelPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Google ML Kit Text Recognition integrato nativamente: zero download aggiuntivi richiesti (risparmiati oltre 2 GB di spazio), elaborazione istantanea di PDF, PEC .p7m e foto lesioni.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppleLabelSecondary
                )
            }
        }

        if (fixedModels.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "⚡ DECISION MODEL ON-DEVICE (RIZZO FLOW)",
                style = MaterialTheme.typography.labelSmall,
                color = AppleLabelPrimary,
                fontWeight = FontWeight.Bold
            )
            fixedModels.forEach { fixedModel ->
                DownloadableModelCard(
                    model = fixedModel,
                    isDownloaded = uiState.downloadedModelIds.contains(fixedModel.id),
                    isDownloading = progress.isDownloading,
                    onDownload = {
                        ensureNotificationPermAndRun {
                            viewModel.startModelDownload(fixedModel)
                        }
                    },
                    onDelete = { viewModel.deleteDownloadedModel(fixedModel) }
                )
            }
        }
    }
}

@Composable
private fun DownloadableModelCard(
    model: DownloadableAiModel,
    isDownloaded: Boolean,
    isActiveReasoning: Boolean = false,
    isDownloading: Boolean = false,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onSelectActive: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = when {
            isActiveReasoning && isDownloaded -> AppleGreenSoft.copy(alpha = 0.45f)
            isActiveReasoning -> AppleBlueSoft.copy(alpha = 0.40f)
            isDownloaded -> AppleGreenSoft.copy(alpha = 0.25f)
            else -> AppleWhite
        },
        border = BorderStroke(
            width = if (isActiveReasoning) 1.8.dp else if (isDownloaded) 1.2.dp else 0.8.dp,
            color = when {
                isActiveReasoning && isDownloaded -> AppleGreen
                isActiveReasoning -> DavezBordeaux
                isDownloaded -> AppleGreen.copy(alpha = 0.7f)
                else -> DavezStoneBorder
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = model.roleBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (model.isPrimaryReasoning) DavezBordeaux else AppleLabelSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    if (isActiveReasoning) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DavezBordeaux
                        ) {
                            Text(
                                text = "ATTIVO",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppleWhite,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AppleGroupedBg
                ) {
                    Text(
                        text = model.sizeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = AppleLabelPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = model.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = AppleLabelPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = model.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AppleLabelSecondary
            )

            // Sezione versione suggerita per Ollama / Termux / PC (es. frob/qwen3.5-instruct:4b)
            model.suggestedOllamaTag?.let { tag ->
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AppleGroupedBg,
                    border = BorderStroke(0.6.dp, DavezStoneBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🏷️ Versione suggerita:",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppleLabelSecondary,
                                fontSize = 10.5.sp
                            )
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.Bold,
                                color = DavezBordeaux,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(tag))
                                Toast.makeText(context, "Copiato: $tag", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copia versione",
                                tint = DavezBordeaux,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (model.isPrimaryReasoning) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!isDownloaded) {
                            Button(
                                onClick = onDownload,
                                enabled = !isDownloading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DavezBordeaux
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                            ) {
                                Text(
                                    text = "⬇️ Scarica (${model.sizeLabel})",
                                    color = AppleWhite,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isActiveReasoning) AppleGreenSoft else AppleGroupedBg,
                                border = BorderStroke(1.dp, if (isActiveReasoning) AppleGreen else DavezStoneBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = if (isActiveReasoning) "⭐ Modello Attivo in Uso" else "✅ File GGUF Scaricato",
                                        color = if (isActiveReasoning) AppleGreen else AppleLabelPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = onDelete,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.heightIn(min = 44.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Elimina",
                                    tint = AppleRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Tasto per selezionare come modello attivo
                    if (!isActiveReasoning && onSelectActive != null) {
                        OutlinedButton(
                            onClick = onSelectActive,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, DavezBordeaux),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = DavezBordeaux
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 40.dp)
                        ) {
                            Text(
                                text = "⚡ Imposta come Modello Predefinito",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!isDownloaded) {
                        Button(
                            onClick = onDownload,
                            enabled = !isDownloading,
                            colors = ButtonDefaults.buttonColors(containerColor = DavezFotografiaSlate),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                        ) {
                            Text(
                                text = "⬇️ Scarica (${model.sizeLabel})",
                                color = AppleWhite,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Button(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                        ) {
                            Text(
                                text = "✅ Modello Scaricato",
                                fontSize = 13.sp
                            )
                        }
                        OutlinedButton(
                            onClick = onDelete,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 44.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Elimina",
                                tint = AppleRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FirstRunModelDownloadDialog(
    uiState: PeritoUiState,
    viewModel: PeritoViewModel
) {
    Dialog(
        onDismissRequest = { viewModel.dismissFirstRunModelDialog() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = AppleWhite,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 520.dp)
                .fillMaxHeight(0.86f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppleWhite)
            ) {
                // Header FISSO in alto (non scrolla e non fa saltare la lista)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 10.dp, top = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        PeritoAiBrandEmblem(size = 40.dp, showOnlineBadge = false)
                        Column {
                            Text(
                                text = "Configurazione Modelli IA",
                                style = MaterialTheme.typography.titleMedium,
                                color = AppleLabelPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when {
                                    uiState.downloadProgress.isDownloading -> "⬇️ Download attivo in Background"
                                    uiState.isReasoningReady -> "✅ Modello Reasoning Pronto"
                                    else -> "Pronto all'Uso"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (uiState.isReasoningReady) AppleGreen else DavezBordeaux,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.dismissFirstRunModelDialog() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi finestra modelli",
                            tint = AppleLabelSecondary
                        )
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = DavezStoneBorder)

                // Area centrale scorrevole con LazyColumn (scroll fluido a 60fps senza bug)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(key = "dialog_info_banner") {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AppleBlueSoft,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💡 Puoi avviare il download (continuerà in background con notifica Android) oppure toccare il pulsante in basso per entrare subito nella Chat.",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppleBlueDark,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    item(key = "dialog_models_catalog") {
                        ModelDownloadCatalogList(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = DavezStoneBorder)

                // Footer FISSO in basso (sempre cliccabile anche durante il download in background!)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppleWhite)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = { viewModel.dismissFirstRunModelDialog() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                uiState.downloadProgress.isDownloading -> DavezBordeaux
                                uiState.isReasoningReady -> AppleGreen
                                else -> DavezBordeaux
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = when {
                                uiState.downloadProgress.isDownloading -> "🔽 Continua in Background (Notifica attiva)"
                                uiState.isReasoningReady -> "✅ Entra nella Chat"
                                else -> "Continua nell'App"
                            },
                            color = AppleWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialogo interattivo elegante Apple HIG per la modifica rapida o precisa dell'orario
 * di un appuntamento (Sopralluogo, Incontro, Udienza, ecc.).
 */
@Composable
fun RescheduleAppointmentDialog(
    event: CalendarEventItem,
    onDismiss: () -> Unit,
    onConfirm: (newTitle: String, newCategory: String, newStartIso: String, newEndIso: String, newNotes: String) -> Unit
) {
    val context = LocalContext.current
    val isoFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss") }

    val initialDt = remember(event) {
        try {
            LocalDateTime.parse(event.startIso.take(19))
        } catch (_: Throwable) {
            LocalDateTime.now()
        }
    }
    val initialEndDt = remember(event) {
        try {
            LocalDateTime.parse(event.endIso.take(19))
        } catch (_: Throwable) {
            initialDt.plusHours(1)
        }
    }
    val durationMinutes = remember(initialDt, initialEndDt) {
        java.time.Duration.between(initialDt, initialEndDt).toMinutes().coerceAtLeast(15L)
    }

    var selectedCategory by remember {
        mutableStateOf(PeritoEventCategory.fromString(event.categoria.ifBlank { event.titolo }))
    }
    var titleText by remember { mutableStateOf(event.titolo) }
    var notesText by remember { mutableStateOf(event.note) }
    var selectedDate by remember { mutableStateOf(initialDt.toLocalDate()) }
    var selectedTime by remember { mutableStateOf(initialDt.toLocalTime()) }

    val openTimePicker = {
        val picker = android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                selectedTime = LocalTime.of(hourOfDay, minute)
            },
            selectedTime.hour,
            selectedTime.minute,
            true // 24 ore
        )
        picker.show()
    }

    val openDatePicker = {
        val picker = android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        )
        picker.show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = DavezBordeaux,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Modifica Impegno",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppleLabelPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Selezione Categoria (Chip orizzontali)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Categoria:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppleLabelSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PeritoEventCategory.entries.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val oldCat = selectedCategory
                                    selectedCategory = cat
                                    if (titleText.startsWith(oldCat.displayName, ignoreCase = true) ||
                                        titleText.startsWith(oldCat.defaultPrefix, ignoreCase = true) ||
                                        titleText.isBlank() ||
                                        titleText == "Appuntamento"
                                    ) {
                                        val suffix = titleText.substringAfter(" con ", "").let { if (it.isNotBlank()) " con $it" else "" }
                                        titleText = "${cat.defaultPrefix}$suffix"
                                    }
                                },
                                label = {
                                    Text(
                                        text = "${cat.emoji} ${cat.displayName}",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DavezBordeaux,
                                    selectedLabelColor = AppleWhite,
                                    containerColor = AppleSecondaryGroupedBg,
                                    labelColor = AppleLabelPrimary
                                )
                            )
                        }
                    }
                }

                // Titolo Appuntamento (modificabile)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Titolo Impegno:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppleLabelSecondary
                    )
                    OutlinedTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DavezBordeaux,
                            unfocusedBorderColor = DavezStoneBorder,
                            focusedLabelColor = DavezBordeaux
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Info Interlocutore / Luogo esistenti (se presenti)
                val hasClient = event.cliente.isNotBlank() && event.cliente != "Cliente" && !event.cliente.equals(event.titolo, ignoreCase = true) && !event.cliente.equals(event.categoria, ignoreCase = true)
                val hasLocation = event.luogo.isNotBlank() && event.luogo != "Da definire"
                if (hasClient || hasLocation) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AppleSecondaryGroupedBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (hasClient) {
                                Text(
                                    text = "👤 Con: ${event.cliente}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppleLabelSecondary
                                )
                            }
                            if (hasLocation) {
                                Text(
                                    text = "📍 ${event.luogo}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppleLabelSecondary
                                )
                            }
                        }
                    }
                }

                // Giorno: Oggi, Domani, o Data originaria / Altro giorno
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Data:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppleLabelSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val today = LocalDate.now()
                        val tomorrow = today.plusDays(1)
                        val originalDate = initialDt.toLocalDate()

                        val options = listOf(
                            "Oggi" to today,
                            "Domani" to tomorrow
                        ) + if (originalDate != today && originalDate != tomorrow) {
                            listOf(originalDate.format(DateTimeFormatter.ofPattern("dd/MM")) to originalDate)
                        } else emptyList()

                        options.forEach { (label, date) ->
                            val isSelected = selectedDate == date
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedDate = date },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DavezBordeaux,
                                    selectedLabelColor = AppleWhite,
                                    containerColor = AppleSecondaryGroupedBg,
                                    labelColor = AppleLabelPrimary
                                )
                            )
                        }

                        // Opzione Data Personalizzata (DatePicker nativo)
                        val isCustomDate = selectedDate != today && selectedDate != tomorrow && selectedDate != originalDate
                        FilterChip(
                            selected = isCustomDate,
                            onClick = openDatePicker,
                            label = {
                                Text(
                                    if (isCustomDate) "📅 ${selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}" else "Altra data 📅",
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DavezBordeaux,
                                selectedLabelColor = AppleWhite,
                                containerColor = AppleSecondaryGroupedBg,
                                labelColor = AppleLabelPrimary
                            )
                        )
                    }
                }

                // Orario Digitale Grande Interattivo (Tap per aprire il TimePicker nativo)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Nuovo Orario:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppleLabelSecondary
                    )
                    Surface(
                        onClick = openTimePicker,
                        shape = RoundedCornerShape(12.dp),
                        color = AppleWhite,
                        border = BorderStroke(1.5.dp, DavezBordeaux.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format(Locale.ITALIAN, "%02d:%02d", selectedTime.hour, selectedTime.minute),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = DavezBordeaux
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AppleBlueSoft
                            ) {
                                Text(
                                    text = "Cambia ✏️",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DavezBordeaux,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Quick Shift Chips: -1h, -30m, +30m, +1h
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Regolazione rapida:",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppleLabelSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val shifts = listOf(-60L to "-1h", -30L to "-30m", 30L to "+30m", 60L to "+1h")
                        shifts.forEach { (delta, label) ->
                            OutlinedButton(
                                onClick = {
                                    val newDt = LocalDateTime.of(selectedDate, selectedTime).plusMinutes(delta)
                                    selectedDate = newDt.toLocalDate()
                                    selectedTime = newDt.toLocalTime()
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.8.dp, DavezStoneBorder)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = AppleLabelPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // Note Personali per il Calendario (Opzionale)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Note per il Calendario:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppleLabelSecondary
                    )
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        placeholder = { Text("Nessuna nota automatica", fontSize = 12.sp, color = AppleLabelSecondary) },
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DavezBordeaux,
                            unfocusedBorderColor = DavezStoneBorder,
                            focusedLabelColor = DavezBordeaux
                        ),
                        supportingText = {
                            Text("Se vuoto, nessuna nota verrà inserita nel calendario del telefono.", fontSize = 10.5.sp, color = AppleLabelSecondary)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newStart = LocalDateTime.of(selectedDate, selectedTime)
                    val newEnd = newStart.plusMinutes(durationMinutes)
                    onConfirm(
                        titleText.ifBlank { selectedCategory.defaultPrefix },
                        selectedCategory.displayName,
                        newStart.format(isoFormatter),
                        newEnd.format(isoFormatter),
                        notesText.trim()
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DavezBordeaux),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Salva Modifiche", color = AppleWhite, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla", color = AppleLabelSecondary)
            }
        },
        containerColor = AppleWhite,
        shape = RoundedCornerShape(20.dp)
    )
}

// =============================================================================
// DIALOGO AGGIORNAMENTO IN-APP OVER-THE-AIR (OTA)
// =============================================================================
@Composable
fun AppUpdateDialog(
    manifest: OtaUpdateManifest,
    downloadProgress: Float?,
    statusMessage: String?,
    onUpdateNow: () -> Unit,
    onContinueWithOldVersion: () -> Unit
) {
    val isDownloading = downloadProgress != null
    Dialog(
        onDismissRequest = {
            if (!isDownloading) onContinueWithOldVersion()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isDownloading,
            dismissOnClickOutside = !isDownloading,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = AppleWhite,
            tonalElevation = 6.dp,
            shadowElevation = 14.dp,
            border = BorderStroke(0.8.dp, DavezStoneBorder),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header con Icona e Badge Versione
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DavezBordeauxSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🚀", fontSize = 22.sp)
                        }
                        Column {
                            Text(
                                text = "Aggiornamento Disponibile",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppleLabelPrimary
                            )
                            Text(
                                text = "Nuova release Assistente Perito",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppleLabelSecondary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DavezBordeaux.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "v${manifest.latestVersionName}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = DavezBordeaux,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Dettagli Release & Note di Rilascio
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DavezAlabaster,
                    border = BorderStroke(0.6.dp, DavezStoneBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("✨", fontSize = 14.sp)
                            Text(
                                text = "Novità di questa versione:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = DavezBordeaux
                            )
                        }
                        Text(
                            text = manifest.releaseNotes.ifBlank { "Miglioramenti generali delle prestazioni, stabilità e correzioni di bug." },
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleLabelPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Barra di Avanzamento Download (se attivo)
                if (downloadProgress != null) {
                    val progressFraction = downloadProgress
                    val percentInt = (progressFraction * 100).toInt()
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = statusMessage ?: "Download in corso...",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppleLabelSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "$percentInt%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = DavezBordeaux
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = DavezBordeaux,
                            trackColor = AppleBlueSoft
                        )
                    }
                }

                // Pulsanti Azione
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onUpdateNow,
                        enabled = !isDownloading,
                        colors = ButtonDefaults.buttonColors(containerColor = DavezBordeaux),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp)
                    ) {
                        Text(
                            text = if (isDownloading) "Download in corso..." else "Scarica e Installa Ora",
                            color = AppleWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    if (!isDownloading) {
                        OutlinedButton(
                            onClick = onContinueWithOldVersion,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(0.8.dp, DavezStoneBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                        ) {
                            Text(
                                text = "Continua con la versione vecchia",
                                color = AppleLabelSecondary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog per la selezione dell'account di calendario sincronizzato.
 * Consente al perito di scegliere un singolo calendario specifico (es. Studio Tecnico)
 * oppure sincronizzare tutti gli account rilevati sul telefono.
 */
@Composable
fun SelectCalendarAccountDialog(
    availableCalendars: List<SystemCalendarInfo>,
    selectedCalendarId: Long?,
    onSelectCalendar: (SystemCalendarInfo?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = DavezBordeaux,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Filtro Account Calendario",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppleLabelPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Scegli da quale account visualizzare e sincronizzare gli appuntamenti (es. account dello Studio Tecnico), escludendo gli altri account personali del telefono.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppleLabelSecondary
                )

                // Opzione 1: Tutti gli account
                val isAllSelected = selectedCalendarId == null
                Surface(
                    onClick = {
                        onSelectCalendar(null)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAllSelected) AppleBlueSoft else AppleWhite,
                    border = BorderStroke(
                        if (isAllSelected) 1.5.dp else 0.5.dp,
                        if (isAllSelected) DavezBordeaux else DavezStoneBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🌐 Tutti gli account del telefono",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isAllSelected) DavezBordeaux else AppleLabelPrimary
                            )
                            Text(
                                text = "Mostra impegni da qualsiasi calendario configurato",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppleLabelSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (isAllSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = DavezBordeaux,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Opzioni per ogni calendario rilevato
                if (availableCalendars.isEmpty()) {
                    Text(
                        text = "Nessun calendario di sistema rilevato o permessi non concessi.",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppleLabelSecondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    availableCalendars.forEach { cal ->
                        val isSelected = selectedCalendarId == cal.id
                        Surface(
                            onClick = {
                                onSelectCalendar(cal)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) AppleBlueSoft else AppleWhite,
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 0.5.dp,
                                if (isSelected) DavezBordeaux else DavezStoneBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(cal.color?.let { Color(it) } ?: DavezBordeaux)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = cal.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) DavezBordeaux else AppleLabelPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        val isOutlook = cal.accountType.contains("outlook", ignoreCase = true) ||
                                            cal.accountType.contains("exchange", ignoreCase = true) ||
                                            cal.accountType.contains("office365", ignoreCase = true) ||
                                            cal.accountType.contains("hotmail", ignoreCase = true) ||
                                            cal.accountType.contains("live.com", ignoreCase = true) ||
                                            cal.accountType.contains("msn.com", ignoreCase = true) ||
                                            cal.name.contains("outlook", ignoreCase = true)

                                        if (cal.isPrimary) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFE8F5E9)
                                            ) {
                                                Text(
                                                    text = "Principale",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF2E7D32),
                                                    fontWeight = FontWeight.Bold,
                                                    softWrap = false,
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        if (isOutlook) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFE5F1FB)
                                            ) {
                                                Text(
                                                    text = "Outlook",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF0078D4),
                                                    fontWeight = FontWeight.Bold,
                                                    softWrap = false,
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Subtitle dinamico: elimina duplicazioni tra nome calendario e account
                                    val subtitle = remember(cal.name, cal.accountName, cal.accountType) {
                                        val isSame = cal.accountName.isBlank() || cal.accountName.equals(cal.name, ignoreCase = true)
                                        val isOutlookAccount = cal.accountType.contains("outlook", ignoreCase = true) ||
                                            cal.accountType.contains("exchange", ignoreCase = true) ||
                                            cal.accountType.contains("office365", ignoreCase = true) ||
                                            cal.accountType.contains("hotmail", ignoreCase = true) ||
                                            cal.accountType.contains("live.com", ignoreCase = true)
                                        val providerLabel = when {
                                            isOutlookAccount -> "Microsoft Outlook / Exchange"
                                            cal.accountType.contains("google", ignoreCase = true) -> "Google Calendar"
                                            cal.accountType.contains("icloud", ignoreCase = true) || cal.accountType.contains("apple", ignoreCase = true) -> "iCloud"
                                            cal.accountType.contains("local", ignoreCase = true) || cal.accountType.contains("samsung", ignoreCase = true) -> "Calendario locale del dispositivo"
                                            cal.accountType.isNotBlank() -> cal.accountType
                                            else -> "Calendario dispositivo"
                                        }
                                        if (isSame) providerLabel else "${cal.accountName} • $providerLabel"
                                    }

                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppleLabelSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = DavezBordeaux,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Card di collegamento rapido Microsoft Outlook / Exchange
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF0F6FF),
                    border = BorderStroke(1.dp, Color(0xFFB9D7F7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0078D4)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Collega Microsoft Outlook",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF005A9E)
                                )
                                Text(
                                    text = "Sincronizza impegni e sopralluoghi da Microsoft 365 / Exchange",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppleLabelSecondary
                                )
                            }
                        }

                        Text(
                            text = "1. Con l'app Microsoft Outlook installata: apri Outlook > Impostazioni ⚙️ > tocca la tua email > attiva l'interruttore 'Sincronizza calendari'.\n2. Oppure aggiungi un account Exchange/Outlook nelle impostazioni di sistema del dispositivo.",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, lineHeight = 14.sp),
                            color = AppleLabelSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val pm = context.packageManager
                            val outlookIntent = pm.getLaunchIntentForPackage("com.microsoft.office.outlook")

                            if (outlookIntent != null) {
                                Button(
                                    onClick = {
                                        try {
                                            context.startActivity(outlookIntent)
                                        } catch (_: Throwable) {}
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0078D4)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Apri Outlook", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    val addAccountIntent = Intent(Settings.ACTION_ADD_ACCOUNT).apply {
                                        putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.microsoft.office.outlook", "com.microsoft.exchange", "com.google.android.exchange"))
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    val syncIntent = Intent(Settings.ACTION_SYNC_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try {
                                        context.startActivity(addAccountIntent)
                                    } catch (_: Throwable) {
                                        try {
                                            context.startActivity(syncIntent)
                                        } catch (_: Throwable) {}
                                    }
                                },
                                border = BorderStroke(1.dp, Color(0xFF0078D4)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Aggiungi Account", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0078D4))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Chiudi", color = DavezBordeaux, fontWeight = FontWeight.Bold)
            }
        }
    )
}



