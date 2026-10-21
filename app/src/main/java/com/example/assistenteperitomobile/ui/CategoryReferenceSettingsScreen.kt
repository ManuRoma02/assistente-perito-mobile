package com.example.assistenteperitomobile.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistenteperitomobile.domain.AiEngineConfig
import com.example.assistenteperitomobile.domain.CategoryReferenceFormat
import com.example.assistenteperitomobile.domain.PeritoEventCategory

// Palette Davez Luxury Architectural & Apple HIG
private val DavezBurgundy = Color(0xFF4F0B18)
private val DavezAlabaster = Color(0xFFFAF7F2)
private val DavezTravertine = Color(0xFFEFE9E1)
private val DavezCharcoal = Color(0xFF1D1D1F)
private val DavezMuted = Color(0xFF86868B)
private val AppleBlue = Color(0xFF0071E3)
private val AppleGreen = Color(0xFF34C759)

/**
 * Schermata di personalizzazione delle informazioni da visualizzare nel riferimento
 * per ciascuna categoria di appuntamento (accessibile dalle Impostazioni).
 */
@Composable
fun CategoryReferenceSettingsContent(
    config: AiEngineConfig,
    onUpdateFormat: (categoryId: String, format: CategoryReferenceFormat) -> Unit,
    onResetDefaults: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedCategoryId by remember { mutableStateOf<String?>(PeritoEventCategory.UNIONCASA.id) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top Bar stile Davez / Apple HIG
        Surface(
            color = DavezAlabaster,
            shadowElevation = 0.5.dp,
            border = BorderStroke(0.5.dp, DavezTravertine)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Torna alle Impostazioni",
                        tint = DavezBurgundy
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Formato Riferimento Categorie",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        ),
                        color = DavezCharcoal
                    )
                    Text(
                        text = "Scegli cosa mostrare nel titolo per ogni categoria",
                        style = MaterialTheme.typography.labelSmall,
                        color = DavezMuted
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DavezBurgundy.copy(alpha = 0.08f),
                    border = BorderStroke(0.5.dp, DavezBurgundy.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "🏷️ Riferimenti",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = DavezBurgundy
                        )
                    )
                }
            }
        }

        // Descrizione contestuale
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .background(DavezAlabaster, RoundedCornerShape(12.dp))
                .border(BorderStroke(0.5.dp, DavezTravertine), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "Tocca una categoria per espanderla e selezionare quali elementi informativi visualizzare nel titolo dell'appuntamento (Agenda, Calendario e Notifiche). L'anteprima si aggiorna all'istante.",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = DavezCharcoal.copy(alpha = 0.85f)
            )
        }

        // Lista Categorie a Schede Accordion
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(PeritoEventCategory.entries, key = { it.id }) { category ->
                val format = config.categoryFormats[category.id] ?: CategoryReferenceFormat()
                val isExpanded = expandedCategoryId == category.id

                CategoryAccordionCard(
                    category = category,
                    format = format,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        expandedCategoryId = if (isExpanded) null else category.id
                    },
                    onUpdateFormat = { newFormat ->
                        onUpdateFormat(category.id, newFormat)
                    }
                )
            }

            // Pulsante di Ripristino
            item {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onResetDefaults,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DavezBurgundy
                    ),
                    border = BorderStroke(1.dp, DavezBurgundy.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ripristina Formati Predefiniti per Tutte le Categorie",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

/**
 * Singola scheda Accordion per configurare il formato di una categoria con Live Preview.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryAccordionCard(
    category: PeritoEventCategory,
    format: CategoryReferenceFormat,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onUpdateFormat: (CategoryReferenceFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "chevronRotation"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isExpanded) DavezAlabaster else Color.White),
        border = BorderStroke(
            if (isExpanded) 1.dp else 0.5.dp,
            if (isExpanded) DavezBurgundy.copy(alpha = 0.4f) else DavezTravertine
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 2.dp else 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Intestazione Card cliccabile per espandere/collassare
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji / Icona Categoria
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(DavezBurgundy.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = category.emoji, fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.1).sp
                        ),
                        color = DavezCharcoal
                    )

                    // Pillole riassuntive dei campi attivi nello stato compresso
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (format.showTime) FormatMiniBadge("🕒 Orario")
                        if (format.showType) FormatMiniBadge("🏷️ Tipo")
                        if (format.showSubject) FormatMiniBadge("👤 Soggetto")
                        if (format.showPlace) FormatMiniBadge("📍 Luogo")
                        if (format.showNotes) FormatMiniBadge("📝 Note")
                        if (!format.showTime && !format.showType && !format.showSubject && !format.showPlace && !format.showNotes) {
                            FormatMiniBadge("Minimo", isMuted = true)
                        }
                    }
                }

                // Chevron rotante con animazione GPU
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer { rotationZ = chevronRotation },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Comprimi" else "Espandi",
                        tint = DavezMuted
                    )
                }
            }

            // Corpo espanso con controlli interattivi e anteprima live
            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Elementi da includere nel titolo:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = DavezMuted
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 5 Chip Interattivi (🕒 Orario, 🏷️ Tipo, 👤 Soggetto, 📍 Luogo, 📝 Note)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ToggleFieldChip(
                            label = "🕒 Orario",
                            checked = format.showTime,
                            onToggle = { onUpdateFormat(format.copy(showTime = !format.showTime)) }
                        )
                        ToggleFieldChip(
                            label = "🏷️ Tipo",
                            checked = format.showType,
                            onToggle = { onUpdateFormat(format.copy(showType = !format.showType)) }
                        )
                        ToggleFieldChip(
                            label = "👤 Soggetto / Agenzia",
                            checked = format.showSubject,
                            onToggle = { onUpdateFormat(format.copy(showSubject = !format.showSubject)) }
                        )
                        ToggleFieldChip(
                            label = "📍 Luogo",
                            checked = format.showPlace,
                            onToggle = { onUpdateFormat(format.copy(showPlace = !format.showPlace)) }
                        )
                        ToggleFieldChip(
                            label = "📝 Note",
                            checked = format.showNotes,
                            onToggle = { onUpdateFormat(format.copy(showNotes = !format.showNotes)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Pulsanti di Preset Rapidi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Preset:",
                            style = MaterialTheme.typography.labelSmall,
                            color = DavezMuted
                        )

                        PresetButton(label = "Tutti") {
                            onUpdateFormat(
                                format.copy(
                                    showTime = true,
                                    showType = true,
                                    showSubject = true,
                                    showPlace = true,
                                    showNotes = true
                                )
                            )
                        }

                        PresetButton(label = "Standard") {
                            onUpdateFormat(
                                format.copy(
                                    showTime = false,
                                    showType = true,
                                    showSubject = true,
                                    showPlace = true,
                                    showNotes = false
                                )
                            )
                        }

                        PresetButton(label = "Minimale") {
                            onUpdateFormat(
                                format.copy(
                                    showTime = false,
                                    showType = true,
                                    showSubject = true,
                                    showPlace = false,
                                    showNotes = false
                                )
                            )
                        }

                        PresetButton(label = "Solo Soggetto") {
                            onUpdateFormat(
                                format.copy(
                                    showTime = false,
                                    showType = false,
                                    showSubject = true,
                                    showPlace = false,
                                    showNotes = false
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Box Anteprima Live in stile Card Agenda
                    CategoryLivePreviewBox(
                        category = category,
                        format = format
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Chip cliccabile per attivare/disattivare un elemento informativo.
 */
@Composable
private fun ToggleFieldChip(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(20.dp),
        color = if (checked) DavezBurgundy else Color.White,
        border = BorderStroke(
            1.dp,
            if (checked) DavezBurgundy else DavezTravertine
        ),
        shadowElevation = if (checked) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.5.sp
                ),
                color = if (checked) Color.White else DavezCharcoal
            )
        }
    }
}

/**
 * Pulsante compatto per applicare un preset veloce di formattazione.
 */
@Composable
private fun PresetButton(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = DavezTravertine.copy(alpha = 0.5f),
        border = BorderStroke(0.5.dp, DavezTravertine)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = DavezCharcoal
            )
        )
    }
}

/**
 * Badge microscopico mostrato nella riga compressa.
 */
@Composable
private fun FormatMiniBadge(
    label: String,
    isMuted: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isMuted) Color.LightGray.copy(alpha = 0.3f) else DavezBurgundy.copy(alpha = 0.08f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                color = if (isMuted) DavezMuted else DavezBurgundy,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

/**
 * Box che mostra l'anteprima in tempo reale identica alla card dell'Agenda Visiva.
 */
@Composable
private fun CategoryLivePreviewBox(
    category: PeritoEventCategory,
    format: CategoryReferenceFormat
) {
    // Esempio realistico per la categoria specifica
    val sampleSubject = when (category) {
        PeritoEventCategory.UNIONCASA -> "Unioncasa"
        PeritoEventCategory.AGENZIA_ORSO -> "Agenzia Orso"
        PeritoEventCategory.APE -> "Unioncasa"
        PeritoEventCategory.SOPRALLUOGO -> "Ing. Ferrari"
        PeritoEventCategory.RILIEVO -> "Geom. Bianchi"
        PeritoEventCategory.TRIBUNALE -> "Tribunale Civile"
        PeritoEventCategory.COMUNE -> "Ufficio Tecnico"
        PeritoEventCategory.RIUNIONE -> "Arch. Neri"
        PeritoEventCategory.CLIENTE -> "Dott. De Luca"
        PeritoEventCategory.PERSONALE -> ""
        PeritoEventCategory.ALTRO -> "Studio Associato"
    }
    val samplePlace = when (category) {
        PeritoEventCategory.UNIONCASA -> "Via Roma 14, Monza"
        PeritoEventCategory.AGENZIA_ORSO -> "Via Garibaldi 3, Milano"
        PeritoEventCategory.TRIBUNALE -> "Via Freguglia 1"
        PeritoEventCategory.COMUNE -> "Piazza Scala 2"
        else -> "Via Dante 10"
    }
    val sampleNotes = when (category) {
        PeritoEventCategory.TRIBUNALE -> "Giuramento CTU R.G. 4102/2026"
        PeritoEventCategory.UNIONCASA -> "Stima immobile"
        PeritoEventCategory.RILIEVO -> "Rilievo scanner laser"
        else -> "Verifica fessurazioni"
    }

    val simulatedTitle = format.formatTitle(
        startTimeIso = "2026-10-22T10:30:00",
        categoryType = category.defaultPrefix,
        subject = sampleSubject,
        place = samplePlace,
        notes = sampleNotes
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(BorderStroke(0.5.dp, DavezBurgundy.copy(alpha = 0.25f)), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Anteprima in Agenda:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = DavezBurgundy
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = AppleBlue.copy(alpha = 0.1f),
                border = BorderStroke(0.5.dp, AppleBlue.copy(alpha = 0.25f))
            ) {
                Text(
                    text = "${category.emoji} ${category.displayName}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppleBlue
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Headline dell'appuntamento (Riferimento generato dinamicamente)
        Text(
            text = simulatedTitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.1).sp
            ),
            color = DavezCharcoal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Dettaglio secondario visibile
        Text(
            text = "📍 $samplePlace • 🕒 10:30 - 11:30",
            style = MaterialTheme.typography.labelSmall,
            color = DavezMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
