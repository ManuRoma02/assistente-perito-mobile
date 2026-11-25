package com.example.assistenteperitomobile.ui

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.media.MediaPlayer
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.assistenteperitomobile.R
import com.example.assistenteperitomobile.data.MorningBriefingWorker
import com.example.assistenteperitomobile.data.ota.OtaUpdateManager
import com.example.assistenteperitomobile.data.ota.OtaUpdateManifest
import com.example.assistenteperitomobile.domain.ArchivedDocument
import com.example.assistenteperitomobile.domain.CalendarEventItem
import com.example.assistenteperitomobile.domain.ChatMessage
import com.example.assistenteperitomobile.domain.Fascicolo
import com.example.assistenteperitomobile.domain.MainAppTab
import com.example.assistenteperitomobile.domain.PeritoCategory
import com.example.assistenteperitomobile.domain.PeritoCategoryDefinition
import com.example.assistenteperitomobile.domain.PeritoEventCategory
import com.example.assistenteperitomobile.domain.SystemCalendarInfo
import com.example.assistenteperitomobile.domain.defaultCategoriesList
import com.example.assistenteperitomobile.theme.LocalPeritoSemanticColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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
    val context = LocalContext.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val animationsEnabled = remember(context) { PeritoMotionTokens.areAnimationsEnabled(context) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = if (animationsEnabled) PeritoMotionTokens.snappySpring else snap(),
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
    val context = LocalContext.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val animationsEnabled = remember(context) { PeritoMotionTokens.areAnimationsEnabled(context) }
    val scaleY by animateFloatAsState(
        targetValue = if (isPressed) pressedScaleY else 1f,
        animationSpec = if (animationsEnabled) PeritoMotionTokens.snappySpring else snap(),
        label = "cartoon_squash_y"
    )
    val scaleX by animateFloatAsState(
        targetValue = if (isPressed) squashScaleX else 1f,
        animationSpec = if (animationsEnabled) PeritoMotionTokens.snappySpring else snap(),
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
// exact luxury architectural ("davez") Bordeaux (#4F0B18) & Alabaster (#F8F4F1) style!
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
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "orb_3d_rotation")
    val animatedPhase = infiniteTransition.animateFloat(
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
    val animatedPulse = infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    // Interactive Touch OrbitControls (/threejs)
    var userYaw by remember { mutableFloatStateOf(0.35f) }
    var userPitch by remember { mutableFloatStateOf(-0.28f) }

    // Architectural Orb Palette — crisp contrast on both Light & Dark surfaces
    val orbBordeaux = Color(0xFF4F0B18)
    val orbBordeauxHover = Color(0xFF6A1628)
    val orbBordeauxSoft = Color(0xFF7D2A3B)
    val orbAlabaster = Color(0xFFF8F4F1)
    val orbTravertine = Color(0xFFEFE7E2)
    val orbGold = Color(0xFFF5D0A9)

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
        val animationsEnabled = PeritoMotionTokens.areAnimationsEnabled(context)
        val phase = if (isThinking && animationsEnabled) animatedPhase.value else 0.35f
        val pulse = if (isThinking && animationsEnabled) animatedPulse.value else 1.0f
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = (this.size.minDimension / 2f) * 0.84f * pulse

        // 1. Ambient Volumetric Bloom Glow (davez Bordeaux & Warm Champagne)
        drawCircle(
            brush = Brush.radialGradient(
                colors = if (isThinking) {
                    listOf(
                        orbBordeauxHover.copy(alpha = 0.48f),
                        orbBordeauxSoft.copy(alpha = 0.26f),
                        orbGold.copy(alpha = 0.14f),
                        Color.Transparent
                    )
                } else {
                    listOf(
                        orbBordeaux.copy(alpha = 0.28f),
                        orbBordeauxSoft.copy(alpha = 0.14f),
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
                    orbBordeauxHover,
                    orbBordeaux,
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
                orbGold.copy(alpha = depthAlpha)
            } else {
                orbAlabaster.copy(alpha = depthAlpha * 0.84f)
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
                color = if (isRoofEdge) orbAlabaster.copy(alpha = alpha) else orbTravertine.copy(alpha = alpha),
                start = Offset(projX[i], projY[i]),
                end = Offset(projX[j], projY[j]),
                strokeWidth = if (isRoofEdge) 1.8.dp.toPx() else 1.3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 5. Champagne Gold AI Spark at the Roof Apex
        drawCircle(
            color = orbGold,
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

enum class ModelVisualStatus(val label: String) {
    READY("Modello pronto"),
    DOWNLOADING("Download in corso"),
    NOT_CONFIGURED("Modello non configurato")
}

@Composable
fun PeritoAiBrandEmblem(
    size: Dp = 40.dp,
    showOnlineBadge: Boolean = true,
    modelStatus: ModelVisualStatus = ModelVisualStatus.READY
) {
    val semanticColors = LocalPeritoSemanticColors.current
    val badgeColor = when (modelStatus) {
        ModelVisualStatus.READY -> semanticColors.success
        ModelVisualStatus.DOWNLOADING -> semanticColors.warning
        ModelVisualStatus.NOT_CONFIGURED -> semanticColors.labelTertiary
    }
    val badgeDesc = "Logo PeritoAI RD • ${modelStatus.label}"

    Box(
        modifier = Modifier
            .size(size + 2.dp)
            .semantics { contentDescription = badgeDesc }
    ) {
        Image(
            painter = painterResource(id = R.drawable.rd_logo_colore),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(size)
        )
        if (showOnlineBadge) {
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(1.5.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun AssistentePeritoApp(viewModel: PeritoViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val semanticColors = LocalPeritoSemanticColors.current

    var showArchiveSheet by rememberSaveable { mutableStateOf(false) }
    var showAiSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showInAppCameraSheet by rememberSaveable { mutableStateOf(false) }

    // Permessi Calendario
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.onPermissionsUpdated()
    }

    val requestCalendarPermissions = {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR
            )
        )
    }

    val handleCalendarConnect = {
        if (!uiState.hasNativeCalendarPermission) {
            requestCalendarPermissions()
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

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            MorningBriefingWorker.scheduleDailyBriefing(context)
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
                "Permesso fotocamera necessario",
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

    LaunchedEffect(uiState.pendingSharedText) {
        val pending = uiState.pendingSharedText
        if (!pending.isNullOrBlank()) {
            prefillCommandInChat(pending)
            viewModel.clearPendingSharedText()
        }
    }

    LaunchedEffect(uiState.openArchiveSheetRequested) {
        if (uiState.openArchiveSheetRequested) {
            showArchiveSheet = true
            viewModel.clearOpenArchiveSheetRequest()
        }
    }

    var agendaBackProgress by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler(
        enabled = uiState.selectedTab == MainAppTab.AGENDA &&
            !showArchiveSheet &&
            !showAiSettingsSheet &&
            !showInAppCameraSheet &&
            !showAttachSheet
    ) { progress: Flow<BackEventCompat> ->
        try {
            progress.collect { backEvent ->
                agendaBackProgress = backEvent.progress
            }
            agendaBackProgress = 0f
            viewModel.selectTab(MainAppTab.CHAT)
        } catch (e: CancellationException) {
            agendaBackProgress = 0f
            throw e
        }
    }

    SharedTransitionLayout {
        val sharedTransitionScope = this
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                PeritoStudioTopBar(
                    archivedCount = uiState.archivedDocuments.size,
                    hasCalendarPermission = uiState.hasCalendarPermission,
                    isProcessing = uiState.isProcessing,
                    isModelReady = uiState.isReasoningReady,
                    isModelDownloading = uiState.downloadProgress.isDownloading,
                    studioTitle = uiState.aiConfig.studioProfile.studioName,
                    fascicoli = uiState.fascicoli,
                    activeFascicoloId = uiState.activeFascicoloId,
                    onSelectActiveFascicolo = { viewModel.setActiveFascicolo(it) },
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
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .imePadding()
            ) {
                // Toast/Banner di conferma adattivo light/dark
                AnimatedVisibility(
                    visible = uiState.statusBanner != null,
                    enter = fadeIn(PeritoMotionTokens.gentleFade) + scaleIn(
                        initialScale = 0.94f,
                        animationSpec = PeritoMotionTokens.snappySpring
                    ),
                    exit = fadeOut(PeritoMotionTokens.gentleFade) + scaleOut(
                        targetScale = 0.96f,
                        animationSpec = PeritoMotionTokens.gentleFade
                    )
                ) {
                    uiState.statusBanner?.let { banner ->
                        Surface(
                            color = semanticColors.brandSoft,
                            modifier = Modifier
                                .fillMaxWidth()
                                .sharedBounds(
                                    sharedTransitionScope.rememberSharedContentState("status_banner"),
                                    animatedVisibilityScope = this@AnimatedVisibility
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = banner,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                        viewModel.clearStatusBanner()
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Chiudi avviso",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = uiState.selectedTab == MainAppTab.CHAT && uiState.easterEggMessage != null,
                    enter = fadeIn(PeritoMotionTokens.gentleFade) + expandVertically(),
                    exit = fadeOut(PeritoMotionTokens.gentleFade) + shrinkVertically()
                ) {
                    uiState.easterEggMessage?.let { message ->
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .sharedBounds(
                                    sharedTransitionScope.rememberSharedContentState("easter_egg_banner"),
                                    animatedVisibilityScope = this@AnimatedVisibility
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "🤏", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                        viewModel.dismissEasterEgg()
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Chiudi battuta",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Barra compatta non bloccante quando un modello IA si scarica in background
                AnimatedVisibility(
                    visible = uiState.downloadProgress.isDownloading && !uiState.showFirstRunModelDialog && !showAiSettingsSheet,
                    enter = fadeIn(PeritoMotionTokens.gentleFade) + expandVertically(),
                    exit = fadeOut(PeritoMotionTokens.gentleFade) + shrinkVertically()
                ) {
                    val prog = uiState.downloadProgress
                    val dlMb = String.format(Locale.ITALIAN, "%.1f", prog.downloadedBytes / (1024.0 * 1024.0))
                    val totMb = if (prog.totalBytes > 0) {
                        String.format(Locale.ITALIAN, "%.1f MB", prog.totalBytes / (1024.0 * 1024.0))
                    } else "..."
                    val queueBadge = if (prog.totalQueueCount > 1) " (${prog.currentQueueIndex}/${prog.totalQueueCount})" else ""
                    Surface(
                        onClick = { showAiSettingsSheet = true },
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .sharedBounds(
                                sharedTransitionScope.rememberSharedContentState("bg_download_banner"),
                                animatedVisibilityScope = this@AnimatedVisibility
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⬇️ Download in background$queueBadge: ${prog.modelTitle}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${prog.progressPercent}% ($dlMb/$totMb)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.width(4.dp))
                                TextButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.Reject)
                                        viewModel.cancelModelDownload()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.heightIn(min = 44.dp)
                                ) {
                                    Text("Pausa", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            LinearProgressIndicator(
                                progress = { prog.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = semanticColors.brandSoft
                            )
                        }
                    }
                }

                // CONTENUTO A SCORRIMENTO ORIZZONTALE (HORIZONTAL PAGER)
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer {
                            if (pagerState.currentPage == 1 && agendaBackProgress > 0f) {
                                translationX = agendaBackProgress * 90f
                                alpha = (1f - agendaBackProgress * 0.15f).coerceIn(0.6f, 1f)
                            }
                        }
                ) { page ->
                    when (page) {
                        0 -> {
                            PeritoStudioChatScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                onPrefillCommand = prefillCommandInChat,
                                sharedTransitionScope = sharedTransitionScope,
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
                    sharedTransitionScope = sharedTransitionScope,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (showAttachSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAttachSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    dragHandle = { AppleSheetDragHandle() },
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    containerColor = MaterialTheme.colorScheme.background
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
                        onCaptureBurstFiles = { burstFiles ->
                            showInAppCameraSheet = false
                            viewModel.processCameraBurstFiles(burstFiles)
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

            if (showArchiveSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showArchiveSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    dragHandle = { AppleSheetDragHandle() },
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    containerColor = MaterialTheme.colorScheme.surface
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

            if (showAiSettingsSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAiSettingsSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    sheetGesturesEnabled = false,
                    dragHandle = { AppleSheetDragHandle() },
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    AiEngineSettingsSheetContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onRequestCalendarPerm = handleCalendarConnect,
                        onClose = { showAiSettingsSheet = false }
                    )
                }
            }

            if (uiState.showFirstRunModelDialog) {
                FirstRunModelDownloadDialog(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }

            if (uiState.showUpdateDialog && uiState.availableUpdate != null) {
                val currentCode = remember {
                    try {
                        androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(
                            context.packageManager.getPackageInfo(context.packageName, 0)
                        ).toInt()
                    } catch (_: Throwable) { 1 }
                }
                AppUpdateDialog(
                    manifest = uiState.availableUpdate!!,
                    downloadProgress = uiState.updateDownloadProgress,
                    statusMessage = uiState.updateStatusMessage,
                    currentVersionCode = currentCode,
                    onUpdateNow = { viewModel.startUpdateDownloadAndInstall(context) },
                    onContinueWithOldVersion = { viewModel.dismissUpdateDialog() }
                )
            }
        }
    }
}

// =============================================================================
// APPLE SHEET DRAG HANDLE (APPLE HIG SPEC)
// Standard iOS Modal grabber pill (36x5dp, continuous rounded corners)
// =============================================================================
@Composable
fun AppleSheetDragHandle(tint: Color? = null) {
    val resolvedTint = tint ?: LocalPeritoSemanticColors.current.labelTertiary.copy(alpha = 0.35f)
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
                    color = resolvedTint,
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
@OptIn(ExperimentalSharedTransitionApi::class)
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
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null
) {
    val haptic = LocalHapticFeedback.current
    val semanticColors = LocalPeritoSemanticColors.current
    val isSlashMode = inputFieldValue.text.startsWith("/")

    Surface(
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.50f)),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. POPUP SUGGERIMENTI COMANDI SLASH (/)
            AnimatedVisibility(
                visible = isSlashMode && !isRecordingVoice,
                enter = fadeIn(PeritoMotionTokens.gentleFade) + expandVertically(
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                ),
                exit = fadeOut(PeritoMotionTokens.gentleFade) + shrinkVertically(
                    animationSpec = tween(200, easing = FastOutLinearInEasing)
                )
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

                val popupModifier = if (sharedTransitionScope != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedBounds(
                            rememberSharedContentState("slash_commands_popup"),
                            animatedVisibilityScope = this@AnimatedVisibility
                        )
                    }
                } else Modifier

                Box(modifier = popupModifier) {
                    SlashCommandsPopupCard(
                        commands = filtered,
                        typedQuery = typedQuery,
                        onSelectCommand = { cmd ->
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onInputFieldValueChange(
                                TextFieldValue(
                                    text = cmd.insertText,
                                    selection = TextRange(cmd.insertText.length)
                                )
                            )
                        },
                        onExecuteDirectCommand = { cmd ->
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onInputFieldValueChange(TextFieldValue(""))
                            onSendMessage(cmd.command)
                        },
                        onDismiss = {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onInputFieldValueChange(TextFieldValue(""))
                        }
                    )
                }
            }

            // 2. BARRA DI SCRITTURA LIQUID GLASS CON [+] E MIC/SEND MORPH
            AnimatedContent(
                targetState = isRecordingVoice,
                transitionSpec = {
                    (fadeIn(PeritoMotionTokens.gentleFade) + scaleIn(
                        initialScale = 0.95f,
                        animationSpec = PeritoMotionTokens.snappySpring
                    )).togetherWith(
                        fadeOut(PeritoMotionTokens.gentleFade) + scaleOut(
                            targetScale = 0.97f,
                            animationSpec = PeritoMotionTokens.gentleFade
                        )
                    )
                },
                label = "unified_voice_bar_morph"
            ) { recordingActive ->
                val morphModifier = if (sharedTransitionScope != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedBounds(
                            rememberSharedContentState("bottom_input_bar"),
                            animatedVisibilityScope = this@AnimatedContent
                        )
                    }
                } else Modifier

                Box(modifier = morphModifier) {
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
                            val attachInteraction = remember { MutableInteractionSource() }
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    onOpenAttachSheet()
                                },
                                interactionSource = attachInteraction,
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.60f)),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .size(48.dp)
                                    .emilPressScale(attachInteraction)
                                    .semantics {
                                        contentDescription = "Azioni e allegati"
                                        testTag = "chat_attach_button"
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Azioni e allegati",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp, max = 110.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                                        RoundedCornerShape(24.dp)
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
                                        color = semanticColors.labelTertiary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = inputFieldValue,
                                    onValueChange = onInputFieldValueChange,
                                    textStyle = TextStyle(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 15.5.sp,
                                        lineHeight = 21.sp,
                                        letterSpacing = (-0.2).sp
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    maxLines = 4,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(inputFocusRequester)
                                        .semantics { contentDescription = "Campo messaggio chat" }
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            val hasText = inputFieldValue.text.isNotBlank()
                            val sendInteraction = remember { MutableInteractionSource() }
                            Surface(
                                onClick = {
                                    if (hasText) {
                                        val toSend = inputFieldValue.text.trim()
                                        if (toSend.isNotEmpty()) {
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
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
                                color = MaterialTheme.colorScheme.primary,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .size(48.dp)
                                    .emilPressScale(sendInteraction)
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
                                                initialScale = 0.84f,
                                                animationSpec = PeritoMotionTokens.snappySpring
                                            ) + fadeIn(PeritoMotionTokens.gentleFade)).togetherWith(
                                                scaleOut(
                                                    targetScale = 0.84f,
                                                    animationSpec = PeritoMotionTokens.gentleFade
                                                ) + fadeOut(PeritoMotionTokens.gentleFade)
                                            )
                                        },
                                        label = "mic_send_morph"
                                    ) { showSend ->
                                        if (showSend) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Send,
                                                contentDescription = "Invia",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = "Registra Nota Vocale In-App",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. SELETTORE SCHEDE (Material 3 Segmented Dock)
            LiquidFluidTabDock(
                selectedTab = selectedTab,
                onSelectTab = onSelectTab,
                agendaCount = agendaCount
            )
        }
    }
}

// =============================================================================
// SELETTORE SCHEDE (Material 3 SingleChoiceSegmentedButtonRow)
// =============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidFluidTabDock(
    selectedTab: MainAppTab,
    onSelectTab: (MainAppTab) -> Unit,
    agendaCount: Int,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
    ) {
        SegmentedButton(
            selected = selectedTab == MainAppTab.CHAT,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onSelectTab(MainAppTab.CHAT)
            },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = MaterialTheme.colorScheme.surface,
                activeContentColor = MaterialTheme.colorScheme.primary,
                inactiveContainerColor = Color.Transparent,
                inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                activeBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.50f),
                inactiveBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            ),
            icon = {
                Icon(
                    imageVector = if (selectedTab == MainAppTab.CHAT) Icons.Default.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    tint = if (selectedTab == MainAppTab.CHAT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            },
            label = {
                Text(
                    text = "Assistente",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (selectedTab == MainAppTab.CHAT) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = (-0.2).sp
                    )
                )
            }
        )

        SegmentedButton(
            selected = selectedTab == MainAppTab.AGENDA,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onSelectTab(MainAppTab.AGENDA)
            },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = MaterialTheme.colorScheme.surface,
                activeContentColor = MaterialTheme.colorScheme.primary,
                inactiveContainerColor = Color.Transparent,
                inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                activeBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.50f),
                inactiveBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            ),
            icon = {
                Icon(
                    imageVector = if (selectedTab == MainAppTab.AGENDA) Icons.Default.CalendarMonth else Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = if (selectedTab == MainAppTab.AGENDA) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            },
            label = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "Agenda",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (selectedTab == MainAppTab.AGENDA) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = (-0.2).sp
                        )
                    )
                    if (agendaCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = if (selectedTab == MainAppTab.AGENDA) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            },
                            modifier = Modifier.defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp)
                            ) {
                                Text(
                                    text = if (agendaCount > 99) "99+" else agendaCount.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = if (selectedTab == MainAppTab.AGENDA) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                            }
                        }
                    }
                }
            }
        )
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
    isModelReady: Boolean = false,
    isModelDownloading: Boolean = false,
    studioTitle: String = "Studio Tecnico Peritale",
    fascicoli: List<Fascicolo> = emptyList(),
    activeFascicoloId: String? = null,
    onSelectActiveFascicolo: (String?) -> Unit = {},
    onOpenArchive: () -> Unit,
    onOpenAiModels: () -> Unit,
    onRequestCalendarPerm: () -> Unit,
    onClearChat: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val semanticColors = LocalPeritoSemanticColors.current
    var showConfirmClear by remember { mutableStateOf(false) }
    var showFascicoloMenu by remember { mutableStateOf(false) }

    val activeFascicolo = remember(fascicoli, activeFascicoloId) {
        fascicoli.find { it.id == activeFascicoloId }
    }

    val modelStatus = when {
        isModelDownloading -> ModelVisualStatus.DOWNLOADING
        isModelReady -> ModelVisualStatus.READY
        else -> ModelVisualStatus.NOT_CONFIGURED
    }

    if (showConfirmClear && onClearChat != null) {
        AlertDialog(
            onDismissRequest = { showConfirmClear = false },
            title = {
                Text(
                    text = "Cancella cronologia chat",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = "Vuoi svuotare tutti i messaggi della conversazione corrente? I documenti archiviati e i fascicoli rimarranno intatti.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Reject)
                        showConfirmClear = false
                        onClearChat()
                    }
                ) {
                    Text("Svuota Chat", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClear = false }) {
                    Text("Annulla", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 62.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PeritoAiBrandEmblem(size = 42.dp, showOnlineBadge = true, modelStatus = modelStatus)

            Spacer(Modifier.width(10.dp))

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
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = semanticColors.brandSoft,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
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
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                val modelStatusLabel = when (modelStatus) {
                    ModelVisualStatus.READY -> "Modello pronto"
                    ModelVisualStatus.DOWNLOADING -> "Download modello in corso..."
                    ModelVisualStatus.NOT_CONFIGURED -> "Modello non configurato"
                }
                Text(
                    text = if (isProcessing) "✨ Elaborazione IA in corso..." else "$studioTitle • $modelStatusLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!hasCalendarPermission) {
                val calInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onRequestCalendarPerm()
                    },
                    interactionSource = calInteraction,
                    shape = CircleShape,
                    color = semanticColors.warningSoft,
                    modifier = Modifier
                        .size(44.dp)
                        .emilPressScale(calInteraction)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Collega Calendario",
                            tint = semanticColors.warning,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
            }

            val modInteraction = remember { MutableInteractionSource() }
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onOpenAiModels()
                },
                interactionSource = modInteraction,
                shape = RoundedCornerShape(14.dp),
                color = semanticColors.brandSoft,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(6.dp))
            val arcInteraction = remember { MutableInteractionSource() }
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onOpenArchive()
                },
                interactionSource = arcInteraction,
                shape = RoundedCornerShape(14.dp),
                color = semanticColors.brandSoft,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                modifier = Modifier
                    .size(44.dp)
                    .emilPressScale(arcInteraction)
                    .semantics {
                        contentDescription = "Archivio Documentale"
                        testTag = "top_bar_archive_button"
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Archivio Documentale",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    if (archivedCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 3.dp)
                            ) {
                                Text(
                                    text = if (archivedCount > 99) "99+" else archivedCount.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }

            if (onClearChat != null) {
                Spacer(Modifier.width(6.dp))
                val clearInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Reject)
                        showConfirmClear = true
                    },
                    interactionSource = clearInteraction,
                    shape = RoundedCornerShape(14.dp),
                    color = semanticColors.brandSoft,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
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
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Active Fascicolo Selector Chip (1-tap commessa switcher)
        if (fascicoli.isNotEmpty() || activeFascicolo != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 6.dp)
            ) {
                val chipInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        showFascicoloMenu = true
                    },
                    interactionSource = chipInteraction,
                    shape = RoundedCornerShape(50),
                    color = if (activeFascicolo != null) semanticColors.brandSoft else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        0.8.dp,
                        if (activeFascicolo != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .emilPressScale(chipInteraction)
                        .semantics { testTag = "top_bar_active_fascicolo_chip" }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = if (activeFascicolo != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (activeFascicolo != null) {
                                "Commessa attiva: ${activeFascicolo.codice} • ${activeFascicolo.titolo}"
                            } else {
                                "Seleziona commessa / fascicolo attivo"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (activeFascicolo != null) FontWeight.Bold else FontWeight.Medium,
                            color = if (activeFascicolo != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Cambia commessa attiva",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showFascicoloMenu,
                    onDismissRequest = { showFascicoloMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Nessuna commessa (Generale)",
                                fontWeight = if (activeFascicoloId == null) FontWeight.Bold else FontWeight.Normal,
                                color = if (activeFascicoloId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingIcon = {
                            if (activeFascicoloId == null) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            showFascicoloMenu = false
                            onSelectActiveFascicolo(null)
                        }
                    )
                    fascicoli.forEach { fascicolo ->
                        val isSelected = fascicolo.id == activeFascicoloId
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = "${fascicolo.codice} • ${fascicolo.titolo}",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = fascicolo.cliente,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                showFascicoloMenu = false
                                onSelectActiveFascicolo(fascicolo.id)
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
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
    val semanticColors = LocalPeritoSemanticColors.current

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
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
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isReasoningReady) semanticColors.successSoft else semanticColors.warningSoft
                        ) {
                            Text(
                                text = if (isReasoningReady) "Qwen 3.5 Attivo" else "Modello Richiesto",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isReasoningReady) semanticColors.success else semanticColors.warning,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Il rigore delle misure e l'IA locale: registra note vocali, scansiona atti o smista nelle 6 cartelle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

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
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val animationsEnabled = remember(context) { PeritoMotionTokens.areAnimationsEnabled(context) }

    val iconBounce by animateFloatAsState(
        targetValue = if (isPressed) 0.84f else 1.0f,
        animationSpec = if (animationsEnabled) PeritoMotionTokens.snappySpring else snap(),
        label = "bento_icon_bounce"
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.emilPressScale(interactionSource, pressedScale = 0.95f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .graphicsLayer {
                        scaleX = iconBounce
                        scaleY = iconBounce
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = (-0.2).sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// =============================================================================
// BOLLA SEMANTICA DI RAGIONAMENTO IA IN CORSO (/agentic-craft-ui Pillar 1 & 4)
// =============================================================================
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AgenticThinkingBubble(
    label: String,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val context = LocalContext.current
    val semanticColors = LocalPeritoSemanticColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "thinking_orb_breathe")
    val breathePhase = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "breathe_phase"
    )

    Surface(
        shape = RoundedCornerShape(
            topStart = 22.dp,
            topEnd = 22.dp,
            bottomStart = 6.dp,
            bottomEnd = 22.dp
        ),
        color = semanticColors.brandSoft,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = Modifier
            .widthIn(min = 180.dp, max = 330.dp)
            .graphicsLayer {
                val animationsEnabled = PeritoMotionTokens.areAnimationsEnabled(context)
                val phase = if (animationsEnabled) breathePhase.value else 0f
                scaleX = 1f + sin(phase) * 0.025f
                scaleY = 1f - sin(phase) * 0.025f
            }
            .semantics { testTag = "agentic_thinking_bubble" }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val orbModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    Modifier.sharedBounds(
                        rememberSharedContentState("agentic_thinking_orb"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            } else Modifier
            Perito3DThinkingOrb(isThinking = true, size = 32.dp, modifier = orbModifier)
            Text(
                text = label.ifBlank { "🧠 Qwen 3.5 2B (Q4_K_M) • Analisi in corso..." },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// =============================================================================
// SCHERMATA ASSISTENTE CHAT PERITALE
// =============================================================================
@OptIn(ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PeritoStudioChatScreen(
    uiState: PeritoUiState,
    viewModel: PeritoViewModel,
    onPrefillCommand: (String) -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null
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

    LaunchedEffect(uiState.messages.size, uiState.isProcessing) {
        val totalItems = uiState.messages.size + 1 + (if (uiState.isProcessing) 1 else 0)
        if (uiState.messages.size > 1 || uiState.isProcessing) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

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
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .imeNestedScroll(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "hero_header_3d") {
                PeritoInteractiveHeroHeader(
                    isProcessing = uiState.isProcessing,
                    isReasoningReady = uiState.isReasoningReady,
                    onSendOggi = { viewModel.sendUserMessage("/oggi") },
                    onPrefillSearch = { onPrefillCommand("/cerca ") },
                    onPrefillDelete = { onPrefillCommand("/elimina ") }
                )
            }

            if (uiState.messages.isEmpty() && !uiState.isProcessing) {
                item(key = "chat_empty_state") {
                    PeritoEmptyErrorState(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        title = "Nessun messaggio in cronologia",
                        subtitle = "Scrivi una richiesta, usa /oggi o allega un documento per iniziare.",
                        actionLabel = "Briefing di oggi",
                        onAction = { viewModel.sendUserMessage("/oggi") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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

            item(key = "thinking_orb_bubble") {
                AnimatedVisibility(
                    visible = uiState.isProcessing,
                    enter = fadeIn(PeritoMotionTokens.gentleFade) + scaleIn(
                        initialScale = 0.92f,
                        animationSpec = PeritoMotionTokens.snappySpring
                    ),
                    exit = fadeOut(PeritoMotionTokens.gentleFade) + scaleOut(
                        targetScale = 0.92f,
                        animationSpec = PeritoMotionTokens.gentleFade
                    )
                ) {
                    AgenticThinkingBubble(
                        label = uiState.processingLabel,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = this@AnimatedVisibility
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showScrollToBottom,
            enter = fadeIn(PeritoMotionTokens.gentleFade) + scaleIn(
                initialScale = 0.86f,
                animationSpec = PeritoMotionTokens.snappySpring
            ),
            exit = fadeOut(PeritoMotionTokens.gentleFade) + scaleOut(
                targetScale = 0.86f,
                animationSpec = PeritoMotionTokens.gentleFade
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 12.dp)
        ) {
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    coroutineScope.launch {
                        val totalItems = uiState.messages.size + 1
                        listState.animateScrollToItem(totalItems - 1, scrollOffset = 10_000)
                    }
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Scorri in fondo",
                        tint = MaterialTheme.colorScheme.primary
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
@Composable
fun SlashCommandsPopupCard(
    commands: List<PeritoSlashCommand>,
    typedQuery: String,
    onSelectCommand: (PeritoSlashCommand) -> Unit,
    onExecuteDirectCommand: (PeritoSlashCommand) -> Unit,
    onDismiss: () -> Unit
) {
    val semanticColors = LocalPeritoSemanticColors.current

    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
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
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (typedQuery.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        ) {
                            Text(
                                text = "/$typedQuery",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Chiudi suggerimenti",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

            if (commands.isEmpty()) {
                PeritoEmptyErrorState(
                    icon = Icons.Default.Search,
                    title = "Nessun comando trovato",
                    subtitle = "Nessun comando per \"/$typedQuery\". Prova /orario, /sposta, /oggi, /fissa o /cerca.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )
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
                            color = MaterialTheme.colorScheme.surface,
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
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                            .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), RoundedCornerShape(9.dp)),
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
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = semanticColors.brandSoft
                                            ) {
                                                Text(
                                                    text = cmd.syntax,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 10.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(1.dp))
                                        Text(
                                            text = cmd.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (cmd.isDirectAction) {
                                    Surface(
                                        onClick = { onExecuteDirectCommand(cmd) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 6.dp)
                                    ) {
                                        Text(
                                            text = "Esegui ⚡",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Usa ↗",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
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

// (SlashCommandsPopupCard moved above; InAppVoiceRecordingBar extracted to PeritoVoiceRecordingBar.kt)

// =============================================================================
// FOGLIO MODALE ALLEGATI SQUIRCLE STILE WHATSAPP / IOS (APPLE HIG & RESPONSIVE)
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
    val semanticColors = LocalPeritoSemanticColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .semantics { testTag = "modern_attachment_sheet" },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = (-0.3).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val closeInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onClose()
                    },
                    interactionSource = closeInteraction,
                    shape = CircleShape,
                    color = semanticColors.secondaryGroupedBg,
                    modifier = Modifier
                        .size(48.dp)
                        .emilPressScale(closeInteraction, pressedScale = 0.90f)
                        .semantics { contentDescription = "Chiudi finestra allegati" }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

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
                            tintColor = MaterialTheme.colorScheme.primary,
                            bgColor = semanticColors.brandSoft,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenScanner
                        )
                        SquircleAttachmentTile(
                            emoji = "📄",
                            title = "File & .p7m",
                            subtitle = "Importa da Telefono",
                            tintColor = MaterialTheme.colorScheme.tertiary,
                            bgColor = MaterialTheme.colorScheme.tertiaryContainer,
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
                            tintColor = semanticColors.success,
                            bgColor = semanticColors.successSoft,
                            modifier = Modifier.weight(1f),
                            onClick = onStartVoiceNote
                        )
                        SquircleAttachmentTile(
                            emoji = "📂",
                            title = "6 Cartelle",
                            subtitle = "Archivio & Perizie",
                            tintColor = semanticColors.warning,
                            bgColor = semanticColors.warningSoft,
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
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        },
        interactionSource = interaction,
        shape = RoundedCornerShape(18.dp),
        color = bgColor,
        border = BorderStroke(0.8.dp, tintColor.copy(alpha = 0.22f)),
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
                    .background(MaterialTheme.colorScheme.surface)
                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
    val semanticColors = LocalPeritoSemanticColors.current

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(50),
        color = semanticColors.brandSoft,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
        modifier = Modifier
            .heightIn(min = 44.dp)
            .emilPressScale(interactionSource, pressedScale = 0.95f)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
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
    val semanticColors = LocalPeritoSemanticColors.current
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleBg = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val timeColor = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f) else semanticColors.labelTertiary

    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }
    var showRescheduleDialog by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val textSelectionColors = remember(isUser, primaryColor, onPrimaryColor) {
        TextSelectionColors(
            handleColor = if (isUser) onPrimaryColor else primaryColor,
            backgroundColor = if (isUser) onPrimaryColor.copy(alpha = 0.35f) else primaryColor.copy(alpha = 0.22f)
        )
    }

    val dissolveProgress by animateFloatAsState(
        targetValue = if (isClearingHistory) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "dissolve_vaporize"
    )

    val isFreshMessage = remember(message.id) {
        (System.currentTimeMillis() - message.timestampMillis) < 2500L
    }
    val entranceProgress = remember(message.id) {
        Animatable(if (isFreshMessage) 0.85f else 1f)
    }
    LaunchedEffect(message.id) {
        if (isFreshMessage && PeritoMotionTokens.areAnimationsEnabled(context)) {
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

    val codeColor = if (isUser) Color(0xFFF5D0A9) else primaryColor
    val formattedFullText = remember(message.text, isUser, codeColor) {
        formatPeritoMessageText(message.text, isUser, codeColor)
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
        Surface(
            color = bubbleBg,
            shadowElevation = if (isUser) 2.dp else 1.dp,
            border = if (isUser) null else BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(
                topStart = 22.dp,
                topEnd = 22.dp,
                bottomStart = if (isUser) 22.dp else 6.dp,
                bottomEnd = if (isUser) 6.dp else 22.dp
            ),
            modifier = Modifier
                .widthIn(min = 90.dp, max = maxBubbleWidth)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = 0.86f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (message.voiceDurationSeconds != null || message.attachmentType == "VOICE") {
                    WhatsAppVoiceNoteCard(
                        durationSeconds = message.voiceDurationSeconds ?: 4,
                        isUser = isUser,
                        voiceAudioPath = message.voiceAudioPath,
                        spokenText = message.text
                    )
                    Spacer(Modifier.height(6.dp))
                } else if (message.attachmentName != null) {
                    ModernAttachmentBubbleCard(
                        fileName = message.attachmentName,
                        attachmentType = message.attachmentType ?: "PDF",
                        isUser = isUser
                    )
                    Spacer(Modifier.height(6.dp))
                }

                if (message.attachmentName == null || message.voiceDurationSeconds != null) {
                    CompositionLocalProvider(LocalTextSelectionColors provides textSelectionColors) {
                        SelectionContainer {
                            Text(
                                text = formattedFullText,
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
                    if (message.text.isNotBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(7.dp),
                                color = if (isUser) onPrimaryColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.background,
                                modifier = Modifier.clickable {
                                    coroutineScope.launch {
                                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("PeritoAI", message.text)))
                                    }
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    isCopied = true
                                    Toast.makeText(context, "Messaggio copiato", Toast.LENGTH_SHORT).show()
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
                                        tint = if (isCopied) semanticColors.success else timeColor,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = if (isCopied) "Copiato" else "Copia",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (isCopied) semanticColors.success else timeColor
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(7.dp),
                                color = if (isUser) onPrimaryColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.background,
                                modifier = Modifier.clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
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
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = { viewModel.resolveConflictReplace(message.id, conflict) }
                )
                PeritoStudioInlineButton(
                    text = "Inserisci comunque",
                    leadingIcon = Icons.Default.Warning,
                    containerColor = semanticColors.warningSoft,
                    contentColor = semanticColors.warning,
                    borderColor = semanticColors.warning.copy(alpha = 0.45f),
                    onClick = { viewModel.resolveConflictForce(message.id, conflict) }
                )
                PeritoStudioInlineButton(
                    text = "Annulla",
                    leadingIcon = Icons.Default.Close,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error,
                    borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
                    onClick = { viewModel.resolveConflictCancel(message.id) }
                )
            }
        }

        // 2. TASTIERA INLINE APPUNTAMENTO CONFERMATO
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
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f),
                        onClick = { onOpenMaps(ev.luogo) }
                    )
                }
                PeritoStudioInlineButton(
                    text = "Modifica Orario",
                    leadingIcon = Icons.Default.Edit,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f),
                    onClick = { showRescheduleDialog = true }
                )
                PeritoStudioInlineButton(
                    text = "Condividi Appuntamento (WhatsApp / SMS)",
                    leadingIcon = Icons.Default.Share,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f),
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
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error,
                    borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.30f),
                    onClick = { viewModel.deleteCalendarEvent(ev, notifyInChat = true) }
                )
            }

            if (showRescheduleDialog) {
                RescheduleAppointmentDialog(
                    event = ev,
                    availableCategories = uiState.aiConfig.categories,
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

        // 3. TASTIERA INLINE BRIEFING /OGGI
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
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f),
                        onClick = { onOpenMaps(ev.luogo) }
                    )
                }
            }
        }

        // 4. TASTIERA INLINE DOCUMENTO ARCHIVIATO
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
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { onOpenDocument(doc) }
                    )
                    PeritoStudioInlineButton(
                        text = "Invia",
                        leadingIcon = Icons.Default.Share,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                        modifier = Modifier.weight(1f),
                        onClick = { onShareDocument(doc) }
                    )
                    PeritoStudioInlineButton(
                        text = "Elimina",
                        leadingIcon = Icons.Default.DeleteOutline,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error,
                        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
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
                                    containerColor = if (isSelected) semanticColors.successSoft else MaterialTheme.colorScheme.surface,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                                    borderColor = if (isSelected) semanticColors.success else MaterialTheme.colorScheme.outline,
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

        // 5. CARD SQUIRCLE FILE TROVATI IN ARCHIVIO
        if (message.matchedDocuments.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier.widthIn(max = maxBubbleWidth),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                message.matchedDocuments.forEach { doc ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp,
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(semanticColors.brandSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = doc.fileName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "📂 ${doc.category.displayTitle} • ${doc.cliente.replace('_', ' ')}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    onOpenDocument(doc)
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Apri",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    onShareDocument(doc)
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Condividi",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Reject)
                                    viewModel.deleteArchivedDocument(doc.id, notifyInChat = true)
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Elimina file",
                                    tint = MaterialTheme.colorScheme.error,
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
            try { mediaPlayerRef?.stop() } catch (_: Throwable) {}
            try { mediaPlayerRef?.release() } catch (_: Throwable) {}
            mediaPlayerRef = null
        }
    }

    LaunchedEffect(isPlaying, voiceAudioPath) {
        if (isPlaying) {
            val wavFile = voiceAudioPath?.takeIf { it.isNotBlank() }?.let { File(it) }
            val hasValidAudio = withContext(Dispatchers.IO) {
                wavFile != null && wavFile.exists() && wavFile.length() > 44L
            }
            if (hasValidAudio && wavFile != null) {
                var mp: MediaPlayer? = null
                try {
                    mp = withContext(Dispatchers.IO) {
                        val player = MediaPlayer()
                        try {
                            player.setDataSource(wavFile.absolutePath)
                            player.prepare()
                            player
                        } catch (t: Throwable) {
                            runCatching { player.release() }
                            throw t
                        }
                    }
                    mp.setOnCompletionListener {
                        isPlaying = false
                        progress = 0f
                    }
                    mp.start()
                    mediaPlayerRef = mp
                    val totalMs = mp.duration.coerceAtLeast(durationSeconds * 1000).coerceAtLeast(500)
                    while (isPlaying && mp.isPlaying) {
                        progress = (mp.currentPosition.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
                        delay(40L)
                    }
                } catch (e: Throwable) {
                    if (e is CancellationException) throw e
                    Toast.makeText(context, "Impossibile riprodurre il file vocale", Toast.LENGTH_SHORT).show()
                } finally {
                    try { mp?.stop() } catch (_: Throwable) {}
                    try { mp?.release() } catch (_: Throwable) {}
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
            try { mediaPlayerRef?.stop() } catch (_: Throwable) {}
            try { mediaPlayerRef?.release() } catch (_: Throwable) {}
            mediaPlayerRef = null
            progress = 0f
        }
    }

    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val primary = MaterialTheme.colorScheme.primary
    val cardBg = if (isUser) onPrimary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.background
    val accent = if (isUser) Color(0xFFF5D0A9) else primary
    val unplayed = if (isUser) onPrimary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline

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
                    .size(44.dp)
                    .emilPressScale(playInteraction)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausa vocale" else "Riproduci la tua voce reale",
                        tint = if (isUser) primary else onPrimary,
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
                        color = if (isUser) onPrimary.copy(alpha = 0.9f) else primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Voce Originale • Trascrizione Live",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) Color(0xFFF5D0A9) else MaterialTheme.colorScheme.onSurfaceVariant,
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
    val semanticColors = LocalPeritoSemanticColors.current
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val (emoji, badgeLabel, badgeColor) = when (attachmentType) {
        "P7M" -> Triple("🔐", "BUSTA CADES .P7M", Color(0xFFF5D0A9))
        "CAMERA_SCAN" -> Triple("📸", "SCANSIONE RILIEVO OCR", Color(0xFFA7F3D0))
        else -> Triple("📄", "DOCUMENTO PDF TECNICO", Color(0xFFEFE7E2))
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isUser) onPrimary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.background,
        border = BorderStroke(
            0.8.dp,
            if (isUser) onPrimary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.outline
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
                    .background(if (isUser) onPrimary.copy(alpha = 0.20f) else semanticColors.brandSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 20.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = badgeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser) badgeColor else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isUser) onPrimary else MaterialTheme.colorScheme.onSurface,
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
    val context = LocalContext.current
    val semanticColors = LocalPeritoSemanticColors.current
    val startH = event.startIso.drop(11).take(5)
    val endH = event.endIso.drop(11).take(5)

    val cardScale = remember { Animatable(0.90f) }
    LaunchedEffect(Unit) {
        if (PeritoMotionTokens.areAnimationsEnabled(context)) {
            cardScale.animateTo(
                targetValue = 1f,
                animationSpec = PeritoMotionTokens.snappySpring
            )
        } else {
            cardScale.snapTo(1f)
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(0.8.dp, semanticColors.success.copy(alpha = 0.45f)),
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
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "$startH–$endH",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface,
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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "3 Alert: 24h • 60m • 30m",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        shadowElevation = 0.5.dp,
        border = borderColor?.let { BorderStroke(0.6.dp, it) },
        modifier = modifier
            .heightIn(min = 44.dp)
            .emilPressScale(interactionSource, pressedScale = 0.96f)
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
fun formatPeritoMessageText(
    raw: String,
    isUser: Boolean,
    codeColor: Color = if (isUser) Color(0xFFF5D0A9) else Color(0xFF4F0B18)
): AnnotatedString {
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
                            color = codeColor
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
    val haptic = LocalHapticFeedback.current
    val semanticColors = LocalPeritoSemanticColors.current
    var activeArchiveTab by rememberSaveable { mutableIntStateOf(0) }
    var docPendingDelete by remember { mutableStateOf<ArchivedDocument?>(null) }
    val filteredDocs = remember(uiState.archivedDocuments, uiState.selectedCategoryFilter) {
        if (uiState.selectedCategoryFilter == null) {
            uiState.archivedDocuments
        } else {
            uiState.archivedDocuments.filter { it.category == uiState.selectedCategoryFilter }
        }
    }

    if (docPendingDelete != null) {
        val targetDoc = docPendingDelete!!
        AlertDialog(
            onDismissRequest = { docPendingDelete = null },
            title = {
                Text(
                    text = "Elimina documento",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = "Sei sicuro di voler eliminare definitivamente \"${targetDoc.fileName}\"? Questa operazione non può essere annullata.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Reject)
                        val idToDelete = targetDoc.id
                        docPendingDelete = null
                        viewModel.deleteArchivedDocument(idToDelete)
                    }
                ) {
                    Text("Elimina Documento", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { docPendingDelete = null }) {
                    Text("Annulla", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.88f)
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (activeArchiveTab == 0) "📂 Archivio 6 Cartelle" else "💼 Fascicoli & Pratiche",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onClose()
                },
                modifier = Modifier.heightIn(min = 44.dp)
            ) {
                Text("Chiudi", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeArchiveTab == 0,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    activeArchiveTab = 0
                },
                label = { Text("📂 Documenti (${uiState.archivedDocuments.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            FilterChip(
                selected = activeArchiveTab == 1,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    activeArchiveTab = 1
                },
                label = { Text("💼 Fascicoli (${uiState.fascicoli.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }

        if (activeArchiveTab == 1) {
            FascicoliPraticheScreen(
                uiState = uiState,
                viewModel = viewModel,
                onOpenInAppCamera = onCameraClick,
                onPickDocumentClick = onPickDocumentClick,
                showHeader = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            OutlinedTextField(
                value = uiState.archiveSearchQuery,
                onValueChange = { viewModel.setArchiveSearchQuery(it) },
                placeholder = { Text("Cerca per cliente, CTU, condominio, fattura...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (uiState.archiveSearchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.setArchiveSearchQuery("") },
                            modifier = Modifier.size(48.dp)
                        ) {
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedCategoryFilter == null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        viewModel.setCategoryFilter(null)
                    },
                    label = { Text("Tutte (${uiState.archivedDocuments.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                PeritoCategory.entries.forEach { cat ->
                    val count = uiState.archivedDocuments.count { it.category == cat }
                    FilterChip(
                        selected = uiState.selectedCategoryFilter == cat,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            viewModel.setCategoryFilter(cat)
                        },
                        label = { Text("${cat.displayTitle} ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = semanticColors.secondaryGroupedBg)

            if (uiState.archiveErrorMessage != null) {
                PeritoEmptyErrorState(
                    icon = Icons.Default.Warning,
                    title = "Errore lettura archivio",
                    subtitle = "${uiState.archiveErrorMessage}. I file su disco sono preservati.",
                    isError = true,
                    actionLabel = "Riprova",
                    onAction = { viewModel.refreshAllData() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            if (filteredDocs.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    PeritoEmptyErrorState(
                        icon = Icons.Default.Folder,
                        title = "Nessun documento in questa cartella",
                        subtitle = "Allega un PDF/.p7m o scatta una foto direttamente nella chat per classificarlo automaticamente.",
                        actionLabel = "📸 Scatta Foto",
                        onAction = onCameraClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onPickDocumentClick()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Text("📎 Scegli File", color = MaterialTheme.colorScheme.primary)
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
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
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
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = doc.dateIso,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = doc.fileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "👤 ${doc.cliente.replace('_', ' ')} • 🏷️ ${doc.oggetto.replace('_', ' ')}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                                viewModel.archiveRepo.createShareOrOpenIntent(doc, asShare = false)?.let {
                                                    context.startActivity(it)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.heightIn(min = 44.dp)
                                        ) {
                                            Text("Apri", color = MaterialTheme.colorScheme.onPrimary)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                                viewModel.archiveRepo.createShareOrOpenIntent(doc, asShare = true)?.let {
                                                    context.startActivity(Intent.createChooser(it, "Condividi ${doc.fileName}"))
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.heightIn(min = 44.dp)
                                        ) {
                                            Text("Condividi", color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.Reject)
                                            docPendingDelete = doc
                                        },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Elimina",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
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
}

// =============================================================================
// COMPONENTE TENDINA ACCORDION COMPRIMIBILE (Dark Mode & Motion Tokens)
// =============================================================================
@Composable
fun SettingsTendinaCard(
    title: String,
    subtitle: String,
    badgeText: String? = null,
    badgeColor: Color? = null,
    badgeTextColor: Color? = null,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val semanticColors = LocalPeritoSemanticColors.current
    val resolvedBadgeColor = badgeColor ?: semanticColors.brandSoft
    val resolvedBadgeTextColor = badgeTextColor ?: MaterialTheme.colorScheme.primary
    val animationsEnabled = remember(context) { PeritoMotionTokens.areAnimationsEnabled(context) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = if (animationsEnabled) PeritoMotionTokens.snappySpring else snap(),
        label = "tendina_chevron"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = semanticColors.groupedBg,
        border = BorderStroke(
            if (isExpanded) 1.dp else 0.5.dp,
            if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        shadowElevation = if (isExpanded) 1.5.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring<IntSize>(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (badgeText != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = resolvedBadgeColor
                            ) {
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = resolvedBadgeTextColor,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isExpanded) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                            CircleShape
                        )
                        .border(
                            0.5.dp,
                            if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Comprimi sezione" else "Espandi sezione",
                        tint = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { rotationZ = chevronRotation }
                    )
                }
            }

            if (isExpanded) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun MorningBriefingTendinaCard(
    briefingEnabled: Boolean,
    briefingTime: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onBriefingEnabledChange: (Boolean) -> Unit,
    onBriefingTimeChange: (String) -> Unit,
    onTriggerTestBriefing: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val semanticColors = LocalPeritoSemanticColors.current
    val (currentHour, currentMinute) = MorningBriefingWorker.parseBriefingTime(briefingTime)
    val openBriefingTimePicker = {
        val picker = android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val formatted = String.format(Locale.ROOT, "%02d:%02d", hourOfDay, minute)
                onBriefingTimeChange(formatted)
            },
            currentHour,
            currentMinute,
            true
        )
        picker.show()
    }

    SettingsTendinaCard(
        title = "☀️ Briefing Mattutino Quotidiano",
        subtitle = "Riepilogo automatico alle $briefingTime con sopralluoghi, scadenze e fascicoli",
        badgeText = if (briefingEnabled) "Attivo ($briefingTime)" else "Disattivato",
        badgeColor = if (briefingEnabled) semanticColors.successSoft else semanticColors.secondaryGroupedBg,
        badgeTextColor = if (briefingEnabled) semanticColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
        isExpanded = isExpanded,
        onToggle = onToggle
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, semanticColors.secondaryGroupedBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .toggleable(
                            value = briefingEnabled,
                            role = Role.Switch,
                            onValueChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onBriefingEnabledChange(it)
                            }
                        )
                        .semantics { testTag = "briefing_toggle_row" }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Attiva Notifica ore $briefingTime",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Riepiloga automaticamente i sopralluoghi di oggi, domani e fascicoli clienti senza dover aprire l'app",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = briefingEnabled,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(checkedTrackColor = semanticColors.success)
                    )
                }
                if (briefingEnabled) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 0.5.dp,
                        color = semanticColors.secondaryGroupedBg
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Orario Notifica",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tocca per impostare l'orario personalizzato",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .clickable { openBriefingTimePicker() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = "Cambia orario briefing",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = briefingTime,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    val presetTimes = listOf("07:30", "08:00", "08:30", "09:00")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        presetTimes.forEach { preset ->
                            val isSelected = briefingTime == preset
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else semanticColors.secondaryGroupedBg.copy(alpha = 0.45f),
                                border = BorderStroke(
                                    if (isSelected) 1.dp else 0.5.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else semanticColors.secondaryGroupedBg
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                        onBriefingTimeChange(preset)
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = preset,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onTriggerTestBriefing()
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Invia Notifica Briefing di Prova Subito",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpdatesTendinaCard(
    uiState: PeritoUiState,
    isExpanded: Boolean,
    showManifestUrlEditor: Boolean,
    editedManifestUrl: String,
    onToggle: () -> Unit,
    onToggleManifestEditor: () -> Unit,
    onEditedManifestUrlChange: (String) -> Unit,
    onToggleAutoCheckUpdates: (Boolean) -> Unit,
    onCheckForUpdates: () -> Unit,
    onStartUpdateDownloadAndInstall: (android.content.Context) -> Unit,
    onUpdateOtaManifestUrl: (String) -> Unit,
    onResetDefaultManifestUrl: (String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val semanticColors = LocalPeritoSemanticColors.current

    SettingsTendinaCard(
        title = "🔄 Aggiornamenti In-App (OTA)",
        subtitle = "Verifica release APK e aggiorna direttamente l'app senza passaggi manuali",
        badgeText = "v${uiState.currentAppVersionName} (${uiState.currentAppVersionCode})",
        badgeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        badgeTextColor = MaterialTheme.colorScheme.primary,
        isExpanded = isExpanded,
        onToggle = onToggle
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Verifica la presenza di nuove release e aggiorna l'app senza dover reinstallare manualmente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            uiState.availableUpdate?.let { update ->
                val currentCode = remember {
                    try {
                        androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(
                            context.packageManager.getPackageInfo(context.packageName, 0)
                        ).toInt()
                    } catch (_: Throwable) { 1 }
                }
                val isNewer = update.latestVersionCode > currentCode
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
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
                                text = if (isNewer) "🚀 Nuova Versione: v${update.latestVersionName}" else "📦 Release Disponibile: v${update.latestVersionName}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isNewer) "Build ${update.latestVersionCode} (Nuova)" else "Build ${update.latestVersionCode} (Reinstalla)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = formatPeritoMessageText(
                                update.releaseNotes.ifBlank { "Miglioramenti generali delle prestazioni e stabilità." },
                                isUser = false,
                                codeColor = MaterialTheme.colorScheme.primary
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${(prog * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { prog },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = semanticColors.brandSoft
                                )
                            }
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                onStartUpdateDownloadAndInstall(context)
                            },
                            enabled = uiState.updateDownloadProgress == null,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                        ) {
                            Text(
                                text = if (uiState.updateDownloadProgress != null) {
                                    "Download in corso..."
                                } else if (isNewer) {
                                    "Scarica e Installa v${update.latestVersionName}"
                                } else {
                                    "Scarica e Reinstalla v${update.latestVersionName}"
                                },
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, semanticColors.secondaryGroupedBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .toggleable(
                        value = uiState.aiConfig.autoCheckUpdatesOnStart,
                        role = Role.Switch,
                        onValueChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onToggleAutoCheckUpdates(it)
                        }
                    )
                    .semantics { testTag = "auto_check_updates_row" }
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Verifica nuove versioni all'apertura dell'app",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.aiConfig.autoCheckUpdatesOnStart,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(checkedTrackColor = semanticColors.success)
                    )
                }
            }

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onCheckForUpdates()
                },
                enabled = !uiState.isCheckingUpdate && uiState.updateDownloadProgress == null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
            ) {
                if (uiState.isCheckingUpdate) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Verifica in corso...", color = MaterialTheme.colorScheme.onPrimary, fontSize = 13.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Verifica Aggiornamenti Ora", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            if (!uiState.updateStatusMessage.isNullOrBlank() && uiState.availableUpdate == null) {
                Text(
                    text = uiState.updateStatusMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clickable { onToggleManifestEditor() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚙️ Endpoint Manifest OTA",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (showManifestUrlEditor) "Nascondi" else "Configura",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (showManifestUrlEditor) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = editedManifestUrl,
                        onValueChange = onEditedManifestUrlChange,
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
                            onClick = { onResetDefaultManifestUrl(OtaUpdateManager.DEFAULT_MANIFEST_URL) },
                            modifier = Modifier.heightIn(min = 44.dp)
                        ) {
                            Text("Predefinito", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(4.dp))
                        Button(
                            onClick = { onUpdateOtaManifestUrl(editedManifestUrl) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 44.dp)
                        ) {
                            Text("Salva URL", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
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
    val semanticColors = LocalPeritoSemanticColors.current

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
            color = MaterialTheme.colorScheme.surface,
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
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 6.dp, top = 12.dp, bottom = 10.dp),
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
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when {
                                    uiState.downloadProgress.isDownloading -> "⬇️ Download attivo in Background"
                                    uiState.isReasoningReady -> "✅ Modello Reasoning Pronto"
                                    else -> "Pronto all'Uso"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (uiState.isReasoningReady) semanticColors.success else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.dismissFirstRunModelDialog() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi finestra modelli",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

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
                            color = semanticColors.brandSoft,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💡 Puoi avviare il download (continuerà in background con notifica Android) oppure toccare il pulsante in basso per entrare subito nella Chat.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
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

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = { viewModel.dismissFirstRunModelDialog() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                uiState.downloadProgress.isDownloading -> MaterialTheme.colorScheme.primary
                                uiState.isReasoningReady -> semanticColors.success
                                else -> MaterialTheme.colorScheme.primary
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
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RescheduleAppointmentDialog(
    event: CalendarEventItem,
    availableCategories: List<PeritoCategoryDefinition> = defaultCategoriesList(),
    onDismiss: () -> Unit,
    onConfirm: (newTitle: String, newCategory: String, newStartIso: String, newEndIso: String, newNotes: String) -> Unit
) {
    val context = LocalContext.current
    val semanticColors = LocalPeritoSemanticColors.current
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

    val initialCat = remember(event, availableCategories) {
        val catStr = event.categoria.ifBlank { event.titolo }.trim()
        availableCategories.find { it.id.equals(catStr, ignoreCase = true) || it.displayName.equals(catStr, ignoreCase = true) }
            ?: availableCategories.find { it.id == PeritoEventCategory.fromString(catStr).id }
            ?: availableCategories.firstOrNull()
            ?: PeritoCategoryDefinition(id = "altro", displayName = "Altro", emoji = "📌", defaultPrefix = "Appuntamento")
    }
    var selectedCategory by remember { mutableStateOf(initialCat) }
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
            true
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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Modifica Impegno",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Categoria:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableCategories.forEach { cat ->
                            val isSelected = selectedCategory.id == cat.id
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
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = semanticColors.secondaryGroupedBg,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Titolo Impegno:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                val hasClient = event.cliente.isNotBlank() && event.cliente != "Cliente" && !event.cliente.equals(event.titolo, ignoreCase = true) && !event.cliente.equals(event.categoria, ignoreCase = true)
                val hasLocation = event.luogo.isNotBlank() && event.luogo != "Da definire"
                if (hasClient || hasLocation) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = semanticColors.secondaryGroupedBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (hasClient) {
                                Text(
                                    text = "👤 Con: ${event.cliente}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (hasLocation) {
                                Text(
                                    text = "📍 ${event.luogo}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Data:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = semanticColors.secondaryGroupedBg,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

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
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = semanticColors.secondaryGroupedBg,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Nuovo Orario:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        onClick = openTimePicker,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
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
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = semanticColors.brandSoft
                            ) {
                                Text(
                                    text = "Cambia ✏️",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Regolazione rapida:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    .heightIn(min = 40.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Note per il Calendario:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        placeholder = { Text("Nessuna nota automatica", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        supportingText = {
                            Text("Se vuoto, nessuna nota verrà inserita nel calendario del telefono.", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Salva Modifiche", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
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
    onContinueWithOldVersion: () -> Unit,
    currentVersionCode: Int = 0
) {
    val semanticColors = LocalPeritoSemanticColors.current
    val isDownloading = downloadProgress != null
    val isReinstall = currentVersionCode > 0 && manifest.latestVersionCode <= currentVersionCode
    val isBlocking = manifest.mandatory && !isReinstall
    Dialog(
        onDismissRequest = {
            if (!isDownloading && !isBlocking) onContinueWithOldVersion()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isDownloading && !isBlocking,
            dismissOnClickOutside = !isDownloading && !isBlocking,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 14.dp,
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
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
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🚀", fontSize = 22.sp)
                        }
                        Column {
                            Text(
                                text = if (isReinstall) "Release Disponibile" else "Aggiornamento Disponibile",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isReinstall) "Pacchetto ufficiale Assistente Perito" else "Nuova release Assistente Perito",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "v${manifest.latestVersionName}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline),
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
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = formatPeritoMessageText(
                                manifest.releaseNotes.ifBlank { "Miglioramenti generali delle prestazioni, stabilità e correzioni di bug." },
                                isUser = false,
                                codeColor = MaterialTheme.colorScheme.primary
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp
                        )
                    }
                }

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
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "$percentInt%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = semanticColors.brandSoft
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val buttonLabel = when {
                        isDownloading -> "Download in corso..."
                        manifest.fileSize > 0 -> {
                            val sizeStr = String.format(java.util.Locale.ITALIAN, "%.1f MB", manifest.fileSize / (1024.0 * 1024.0))
                            if (isReinstall) "Scarica e Reinstalla ($sizeStr)" else "Scarica e Installa Ora ($sizeStr)"
                        }
                        isReinstall -> "Scarica e Reinstalla"
                        else -> "Scarica e Installa Ora"
                    }
                    Button(
                        onClick = onUpdateNow,
                        enabled = !isDownloading,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp)
                    ) {
                        Text(
                            text = buttonLabel,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    if (!isDownloading && !isBlocking) {
                        OutlinedButton(
                            onClick = onContinueWithOldVersion,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                        ) {
                            Text(
                                text = if (isReinstall) "Chiudi" else "Ricorda più tardi",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    } else if (isBlocking) {
                        Text(
                            text = "⚠️ Aggiornamento obbligatorio per continuare",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelectCalendarAccountDialog(
    availableCalendars: List<SystemCalendarInfo>,
    selectedCalendarId: Long?,
    onSelectCalendar: (SystemCalendarInfo?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val semanticColors = LocalPeritoSemanticColors.current

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
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Filtro Account Calendario",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val isAllSelected = selectedCalendarId == null
                Surface(
                    onClick = {
                        onSelectCalendar(null)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAllSelected) semanticColors.brandSoft else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        if (isAllSelected) 1.5.dp else 0.5.dp,
                        if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
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
                                color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Mostra impegni da qualsiasi calendario configurato",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (isAllSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (availableCalendars.isEmpty()) {
                    Text(
                        text = "Nessun calendario di sistema rilevato o permessi non concessi.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            color = if (isSelected) semanticColors.brandSoft else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 0.5.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
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
                                        .background(cal.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary)
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
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
                                                color = semanticColors.successSoft
                                            ) {
                                                Text(
                                                    text = "Principale",
                                                    fontSize = 10.sp,
                                                    color = semanticColors.success,
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)),
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
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Sincronizza impegni e sopralluoghi da Microsoft 365 / Exchange",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "1. Con l'app Microsoft Outlook installata: apri Outlook > Impostazioni ⚙️ > tocca la tua email > attiva l'interruttore 'Sincronizza calendari'.\n2. Oppure aggiungi un account Exchange/Outlook nelle impostazioni di sistema del dispositivo.",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, lineHeight = 14.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
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
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
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
                Text("Chiudi", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}



