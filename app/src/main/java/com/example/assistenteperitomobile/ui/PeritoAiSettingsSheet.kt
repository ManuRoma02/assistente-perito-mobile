package com.example.assistenteperitomobile.ui

import android.Manifest
import android.content.ClipData
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.assistenteperitomobile.data.DownloadableAiModel
import com.example.assistenteperitomobile.domain.CategoryReferenceFormat
import com.example.assistenteperitomobile.domain.PeritoEventCategory
import com.example.assistenteperitomobile.domain.StudioProfile
import com.example.assistenteperitomobile.theme.LocalPeritoSemanticColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun AiEngineSettingsSheetContent(
    uiState: PeritoUiState,
    viewModel: PeritoViewModel,
    onRequestCalendarPerm: () -> Unit,
    onClose: () -> Unit
) {
    val semanticColors = LocalPeritoSemanticColors.current
    val haptic = LocalHapticFeedback.current

    var briefingEnabled by remember(uiState.aiConfig.morningBriefingEnabled) {
        mutableStateOf(uiState.aiConfig.morningBriefingEnabled)
    }
    var briefingTime by remember(uiState.aiConfig.morningBriefingTime) {
        mutableStateOf(uiState.aiConfig.morningBriefingTime.ifBlank { "08:30" })
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

    var expandedSections by rememberSaveable { mutableStateOf(setOf<String>()) }
    val toggleSection: (String) -> Unit = { key ->
        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
        expandedSections = if (expandedSections.contains(key)) expandedSections - key else expandedSections + key
    }

    var currentSubScreen by rememberSaveable { mutableStateOf("main") }
    var predictiveBackProgress by remember { mutableFloatStateOf(0f) }

    PredictiveBackHandler(enabled = currentSubScreen != "main") { progress: Flow<BackEventCompat> ->
        try {
            progress.collect { backEvent ->
                predictiveBackProgress = backEvent.progress
            }
            predictiveBackProgress = 0f
            currentSubScreen = "main"
        } catch (e: CancellationException) {
            predictiveBackProgress = 0f
            throw e
        }
    }

    AnimatedContent(
        targetState = currentSubScreen,
        transitionSpec = {
            if (targetState == "category_formats") {
                slideInHorizontally(
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                ) { width -> width } + fadeIn(tween(300, easing = FastOutSlowInEasing)) togetherWith
                    slideOutHorizontally(
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                    ) { width -> -width } + fadeOut(tween(240, easing = FastOutLinearInEasing))
            } else {
                slideInHorizontally(
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                ) { width -> -width } + fadeIn(tween(300, easing = FastOutSlowInEasing)) togetherWith
                    slideOutHorizontally(
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                    ) { width -> width } + fadeOut(tween(240, easing = FastOutLinearInEasing))
            }
        },
        label = "settings_subscreen_transition"
    ) { screen ->
        if (screen == "category_formats") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .graphicsLayer {
                        translationX = predictiveBackProgress * 120f
                        alpha = (1f - predictiveBackProgress * 0.28f).coerceIn(0.4f, 1f)
                    }
            ) {
                CategoryReferenceSettingsContent(
                    config = uiState.aiConfig,
                    onUpdateFormat = { catId, format ->
                        viewModel.updateCategoryReferenceFormat(catId, format)
                    },
                    onResetDefaults = {
                        viewModel.resetCategoryReferenceFormats()
                    },
                    onAddCategory = { name, emoji, defaultTitle, keywords ->
                        viewModel.addCustomCategory(name, emoji, defaultTitle, keywords)
                    },
                    onUpdateCategory = { id, name, emoji, defaultTitle, keywords ->
                        viewModel.updateCustomCategory(id, name, emoji, defaultTitle, keywords)
                    },
                    onDeleteCategory = { id ->
                        viewModel.deleteCustomCategory(id)
                    },
                    onBack = { currentSubScreen = "main" }
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
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
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "PeritoAI • Download in Background & Privacy",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onClose()
                        },
                        shape = CircleShape,
                        color = semanticColors.secondaryGroupedBg.copy(alpha = 0.85f),
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "Chiudi impostazioni" }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Chiudi impostazioni",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // TENDINA 1: PERSONALIZZAZIONE FORMATO RIFERIMENTO CATEGORIE
                    item(key = "tendina_category_formats") {
                        SettingsTendinaCard(
                            title = "🏷️ Formato Riferimento Categorie",
                            subtitle = "Personalizza cosa mostrare nel titolo per ogni categoria nell'agenda",
                            badgeText = "${uiState.aiConfig.categoryFormats.size} Categorie",
                            badgeColor = semanticColors.brandSoft,
                            badgeTextColor = MaterialTheme.colorScheme.primary,
                            isExpanded = expandedSections.contains("category_formats"),
                            onToggle = { toggleSection("category_formats") }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Configura l'ordine e la visibilità delle informazioni visualizzate nel riferimento degli appuntamenti (Orario, Tipo, Soggetto, Luogo, Note).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(0.5.dp, semanticColors.secondaryGroupedBg),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        PeritoEventCategory.values().take(4).forEach { cat ->
                                            val format = uiState.aiConfig.categoryFormats[cat.id] ?: CategoryReferenceFormat()
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${cat.emoji} ${cat.displayName}",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                val summaryParts = listOfNotNull(
                                                    if (format.showTime) "Orario" else null,
                                                    if (format.showSubject) "Soggetto" else null,
                                                    if (format.showPlace) "Luogo" else null
                                                )
                                                Text(
                                                    text = if (summaryParts.isEmpty()) "Solo tipo" else summaryParts.joinToString(" • "),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                        currentSubScreen = "category_formats"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Personalizza Regole e Anteprima Completa",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    // TENDINA 2: PROFILO STUDIO TECNICO
                    item(key = "tendina_studio_profile") {
                        SettingsTendinaCard(
                            title = "🏛️ Profilo Studio Tecnico",
                            subtitle = "Configura l'intestazione dello studio per fascicoli, perizie e barra superiore",
                            badgeText = if (studioNameInput.isNotBlank()) "Personalizzato" else "Predefinito",
                            badgeColor = semanticColors.brandSoft,
                            badgeTextColor = MaterialTheme.colorScheme.primary,
                            isExpanded = expandedSections.contains("studio_profile"),
                            onToggle = { toggleSection("studio_profile") }
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
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            cursorColor = MaterialTheme.colorScheme.primary
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
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            cursorColor = MaterialTheme.colorScheme.primary
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
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            cursorColor = MaterialTheme.colorScheme.primary
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
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            cursorColor = MaterialTheme.colorScheme.primary
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
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            cursorColor = MaterialTheme.colorScheme.primary
                                        )
                                    )

                                    val isModified = studioNameInput != uiState.aiConfig.studioProfile.studioName ||
                                        professionistaInput != uiState.aiConfig.studioProfile.professionistaNome ||
                                        alboOrdineInput != uiState.aiConfig.studioProfile.alboIscrizione ||
                                        cittaInput != uiState.aiConfig.studioProfile.citta ||
                                        recapitoInput != uiState.aiConfig.studioProfile.recapito

                                    Button(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
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
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 44.dp)
                                    ) {
                                        Text(
                                            "💾 Salva Intestazione Studio",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // TENDINA 3: BRIEFING MATTUTINO QUOTIDIANO (Composable in PeritoAppScreens.kt per Milestone3 test contract)
                    item(key = "tendina_morning_briefing") {
                        MorningBriefingTendinaCard(
                            briefingEnabled = briefingEnabled,
                            briefingTime = briefingTime,
                            isExpanded = expandedSections.contains("morning_briefing"),
                            onToggle = { toggleSection("morning_briefing") },
                            onBriefingEnabledChange = { enabled ->
                                briefingEnabled = enabled
                                viewModel.updateAiConfig(
                                    uiState.aiConfig.copy(
                                        morningBriefingEnabled = enabled,
                                        morningBriefingTime = briefingTime
                                    )
                                )
                            },
                            onBriefingTimeChange = { formatted ->
                                briefingTime = formatted
                                viewModel.updateAiConfig(
                                    uiState.aiConfig.copy(
                                        morningBriefingEnabled = briefingEnabled,
                                        morningBriefingTime = formatted
                                    )
                                )
                            },
                            onTriggerTestBriefing = { viewModel.triggerTestMorningBriefing() }
                        )
                    }

                    // TENDINA 4: CALENDARIO GOOGLE & OUTLOOK
                    item(key = "tendina_calendar_sync") {
                        val isCalendarSyncActive = uiState.hasCalendarPermission && uiState.aiConfig.syncSystemCalendar
                        SettingsTendinaCard(
                            title = "📅 Calendario Google & Outlook",
                            subtitle = "Sincronizzazione bidirezionale col telefono e 3 promemoria automatici",
                            badgeText = if (isCalendarSyncActive) "✅ Attivo" else "Non collegato",
                            badgeColor = if (isCalendarSyncActive) semanticColors.successSoft else semanticColors.secondaryGroupedBg,
                            badgeTextColor = if (isCalendarSyncActive) semanticColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                            isExpanded = expandedSections.contains("calendar_sync"),
                            onToggle = { toggleSection("calendar_sync") }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(0.5.dp, semanticColors.secondaryGroupedBg),
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
                                                text = "Calendario Android / Google",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = when {
                                                    isCalendarSyncActive ->
                                                        "Sincronizzato con Google/Android Calendar (3 promemoria automatici)"
                                                    !uiState.hasNativeCalendarPermission ->
                                                        "Permessi non concessi • Tocca per sincronizzare col telefono"
                                                    else ->
                                                        "Scollegato • Modalità Agenda Locale attiva"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        if (isCalendarSyncActive) {
                                            OutlinedButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                                    viewModel.disconnectCalendar()
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                modifier = Modifier.heightIn(min = 44.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.LinkOff,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text("Scollega", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                                    onRequestCalendarPerm()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.heightIn(min = 44.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Link,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.onPrimary
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text("Collega", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }

                                if (isCalendarSyncActive) {
                                    Surface(
                                        onClick = { showCalendarAccountDialog = true },
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(0.5.dp, semanticColors.secondaryGroupedBg),
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
                                                    color = MaterialTheme.colorScheme.onSurface
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
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            OutlinedButton(
                                                onClick = { showCalendarAccountDialog = true },
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                modifier = Modifier.heightIn(min = 44.dp)
                                            ) {
                                                Text("Cambia", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TENDINA 5: MIRROR PC & ARCHIVIO LOCALE
                    item(key = "tendina_pc_sync") {
                        SettingsTendinaCard(
                            title = "💻 Mirror PC & Archivio Locale",
                            subtitle = "Archivio Documents/Perizie_Archivio (6 Cartelle) per sincronizzazione PC",
                            badgeText = "📂 Mirror Attivo",
                            badgeColor = semanticColors.successSoft,
                            badgeTextColor = MaterialTheme.colorScheme.onSurface,
                            isExpanded = expandedSections.contains("pc_sync"),
                            onToggle = { toggleSection("pc_sync") }
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
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Cartella Archivio Locale:",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = semanticColors.groupedBg,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "/storage/emulated/0/Documents/Perizie_Archivio/",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                    Text(
                                        text = "Tutti i fascicoli, verbali e allegati fotografici sono salvati qui in formato standard aperto, pronti per essere sincronizzati in tempo reale con il computer dello studio tramite Syncthing (P2P zero-cloud), Microsoft OneDrive o Dropbox/Dropsync.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // TENDINA 6: MOTORE IA ON-DEVICE & MODELLI
                    item(key = "tendina_ai_models") {
                        SettingsTendinaCard(
                            title = "🧠 Motore IA & Modelli On-Device",
                            subtitle = "Download modelli GGUF e Whisper per elaborazione locale 100% confidenziale",
                            badgeText = if (uiState.isReasoningReady) "Reasoning Attivo" else "Pronto all'Uso",
                            badgeColor = if (uiState.isReasoningReady) semanticColors.successSoft else semanticColors.warningSoft,
                            badgeTextColor = if (uiState.isReasoningReady) semanticColors.success else semanticColors.warning,
                            isExpanded = expandedSections.contains("ai_models"),
                            onToggle = { toggleSection("ai_models") }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Il download avviene anche in background con notifica di avanzamento nella barra di stato Android. Puoi chiudere questa finestra e continuare a usare l'app mentre scarica:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                ModelDownloadCatalogList(
                                    uiState = uiState,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }

                    // TENDINA 7: AGGIORNAMENTI IN-APP (OTA) (Composable in PeritoAppScreens.kt per Milestone3 test contract)
                    item(key = "tendina_ota_updates") {
                        UpdatesTendinaCard(
                            uiState = uiState,
                            isExpanded = expandedSections.contains("ota_updates"),
                            showManifestUrlEditor = showManifestUrlEditor,
                            editedManifestUrl = editedManifestUrl,
                            onToggle = { toggleSection("ota_updates") },
                            onToggleManifestEditor = { showManifestUrlEditor = !showManifestUrlEditor },
                            onEditedManifestUrlChange = { editedManifestUrl = it },
                            onToggleAutoCheckUpdates = { viewModel.toggleAutoCheckUpdates(it) },
                            onCheckForUpdates = { viewModel.checkForUpdates(silent = false) },
                            onStartUpdateDownloadAndInstall = { ctx -> viewModel.startUpdateDownloadAndInstall(ctx) },
                            onUpdateOtaManifestUrl = { url -> viewModel.updateOtaManifestUrl(url) },
                            onResetDefaultManifestUrl = { defaultUrl ->
                                editedManifestUrl = defaultUrl
                                viewModel.updateOtaManifestUrl(defaultUrl)
                            }
                        )
                    }

                    item(key = "settings_bottom_spacer") {
                        Spacer(Modifier.height(40.dp))
                    }
                }
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
    val haptic = LocalHapticFeedback.current
    val semanticColors = LocalPeritoSemanticColors.current
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
        val missingModels = remember(uiState.downloadedModelIds, uiState.aiConfig.reasoningModelId) {
            viewModel.getMissingModelsForActiveConfig()
        }
        val missingBytes = remember(missingModels) {
            missingModels.sumOf { it.expectedBytes }
        }
        val missingNames = remember(missingModels) {
            missingModels.joinToString(" + ") { it.title }
        }

        if (missingModels.isNotEmpty() && !progress.isDownloading) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        ensureNotificationPermAndRun {
                            viewModel.downloadAllMissingModels()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(
                        text = "⬇️ Scarica Modelli Richiesti (${missingModels.size}) • ${viewModel.modelDownloadManager.formatByteSize(missingBytes)}",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }
                Text(
                    text = "Include: $missingNames",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        if (progress.isDownloading) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = semanticColors.brandSoft,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val queueTag = if (progress.totalQueueCount > 1) {
                        " (${progress.currentQueueIndex}/${progress.totalQueueCount})"
                    } else ""
                    Text(
                        text = "⬇️ Download in Background$queueTag: ${progress.modelTitle}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "🔔 Notifica Android attiva: puoi chiudere questa finestra o mettere l'app in background.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress.progressFraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
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
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Reject)
                                viewModel.cancelModelDownload()
                            }
                        ) {
                            Text("Pausa", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        progress.error?.let { err ->
            Text(
                text = "⚠️ $err",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Text(
            text = "1️⃣ MOTORE DI REASONING (BASE 2B O POTENZIATO 4B)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Puoi selezionare la versione Base 2B (ultra-veloce e leggera) oppure sostituirla con la versione Potenziata 4B per deduzioni logiche e perizie complesse.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

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

        Text(
            text = "🎙️ TRASCRIZIONE VOCALE INTEGRATA IN-APP",
            style = MaterialTheme.typography.labelSmall,
            color = semanticColors.success,
            fontWeight = FontWeight.Bold
        )
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = semanticColors.successSoft.copy(alpha = 0.45f),
            border = BorderStroke(1.2.dp, semanticColors.success),
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
                        color = semanticColors.success,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "0 MB • Integrato",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = semanticColors.success,
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Integrato direttamente nel registratore vocale: salva l'audio originale .wav della voce dell'utente e genera la trascrizione del testo in tempo reale.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "📄 OCR Documentale & Visione Foto Rilievi",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Google ML Kit Text Recognition integrato nativamente: zero download aggiuntivi richiesti (risparmiati oltre 2 GB di spazio), elaborazione istantanea di PDF, PEC .p7m e foto lesioni.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (fixedModels.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "⚡ DECISION MODEL ON-DEVICE (RIZZO FLOW)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
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
    val clipboard = LocalClipboard.current
    val haptic = LocalHapticFeedback.current
    val semanticColors = LocalPeritoSemanticColors.current
    val coroutineScope = rememberCoroutineScope()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = when {
            isActiveReasoning && isDownloaded -> semanticColors.successSoft.copy(alpha = 0.45f)
            isActiveReasoning -> semanticColors.brandSoft.copy(alpha = 0.40f)
            isDownloaded -> semanticColors.successSoft.copy(alpha = 0.25f)
            else -> MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (isActiveReasoning) 1.8.dp else if (isDownloaded) 1.2.dp else 0.8.dp,
            color = when {
                isActiveReasoning && isDownloaded -> semanticColors.success
                isActiveReasoning -> MaterialTheme.colorScheme.primary
                isDownloaded -> semanticColors.success.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.outline
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
                        color = if (model.isPrimaryReasoning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    if (isActiveReasoning) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "ATTIVO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = semanticColors.groupedBg
                ) {
                    Text(
                        text = model.sizeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface,
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
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = model.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            model.suggestedOllamaTag?.let { tag ->
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = semanticColors.groupedBg,
                    border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline),
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.5.sp
                            )
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                coroutineScope.launch {
                                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("PeritoAI", tag)))
                                }
                                Toast.makeText(context, "Copiato: $tag", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copia versione",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
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
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    onDownload()
                                },
                                enabled = !isDownloading,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                            ) {
                                Text(
                                    text = "⬇️ Scarica (${model.sizeLabel})",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isActiveReasoning) semanticColors.successSoft else semanticColors.groupedBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (isActiveReasoning) semanticColors.success else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = if (isActiveReasoning) "⭐ Modello Attivo in Uso" else "✅ File GGUF Scaricato",
                                        color = if (isActiveReasoning) semanticColors.success else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Reject)
                                    onDelete()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.heightIn(min = 44.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Elimina",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (!isActiveReasoning && onSelectActive != null) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onSelectActive()
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
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
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                onDownload()
                            },
                            enabled = !isDownloading,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                        ) {
                            Text(
                                text = "⬇️ Scarica (${model.sizeLabel})",
                                color = MaterialTheme.colorScheme.onTertiary,
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
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Reject)
                                onDelete()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 44.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Elimina",
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
