package com.example.assistenteperitomobile.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistenteperitomobile.domain.AiEngineConfig
import com.example.assistenteperitomobile.domain.CategoryReferenceFormat
import com.example.assistenteperitomobile.domain.PeritoCategoryDefinition
import com.example.assistenteperitomobile.domain.PeritoEventCategory
import com.example.assistenteperitomobile.theme.LocalPeritoSemanticColors
import java.util.Locale

/**
 * Schermata di personalizzazione delle informazioni da visualizzare nel riferimento
 * per ciascuna categoria di appuntamento (accessibile dalle Impostazioni).
 */
@Composable
fun CategoryReferenceSettingsContent(
    config: AiEngineConfig,
    onUpdateFormat: (categoryId: String, format: CategoryReferenceFormat) -> Unit,
    onResetDefaults: () -> Unit,
    onAddCategory: ((name: String, emoji: String, defaultTitle: String, keywords: List<String>) -> Unit)? = null,
    onUpdateCategory: ((id: String, name: String, emoji: String, defaultTitle: String, keywords: List<String>) -> Unit)? = null,
    onDeleteCategory: ((id: String) -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalPeritoSemanticColors.current
    val haptic = LocalHapticFeedback.current
    var expandedCategoryId by remember { mutableStateOf<String?>(PeritoEventCategory.UNIONCASA.id) }
    var isCreatingCategory by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<PeritoCategoryDefinition?>(null) }
    var categoryToDelete by remember { mutableStateOf<PeritoCategoryDefinition?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar stile Davez / Apple HIG
        Surface(
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 0.5.dp,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onBack()
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Torna alle Impostazioni",
                        tint = MaterialTheme.colorScheme.primary
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Gestisci categorie e formato dei titoli in agenda",
                        style = MaterialTheme.typography.labelSmall,
                        color = semantic.labelTertiary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "🏷️ Riferimenti",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
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
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "Tocca una categoria per espanderla e configurare gli elementi del titolo. Puoi aggiungere nuove categorie personalizzate con parole chiave dedicate che l'IA riconoscerà nei messaggi vocali e scritti.",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
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
            // Pulsante Aggiungi Nuova Categoria
            item {
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        isCreatingCategory = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+ Nuova Categoria Personalizzata",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            items(config.categories, key = { it.id }) { category ->
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
                    },
                    onEditCategory = {
                        categoryToEdit = category
                    },
                    onDeleteCategory = {
                        categoryToDelete = category
                    }
                )
            }

            // Pulsante di Ripristino
            item {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Reject)
                        onResetDefaults()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
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

    if (isCreatingCategory) {
        AddEditCategoryDialog(
            categoryToEdit = null,
            onDismiss = { isCreatingCategory = false },
            onSave = { name, emoji, defTitle, kws ->
                onAddCategory?.invoke(name, emoji, defTitle, kws)
                isCreatingCategory = false
            }
        )
    }

    if (categoryToEdit != null) {
        AddEditCategoryDialog(
            categoryToEdit = categoryToEdit,
            onDismiss = { categoryToEdit = null },
            onSave = { name, emoji, defTitle, kws ->
                categoryToEdit?.let { target ->
                    onUpdateCategory?.invoke(target.id, name, emoji, defTitle, kws)
                }
                categoryToEdit = null
            }
        )
    }

    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "⚠️", fontSize = 20.sp)
                    Text(
                        text = "Elimina Categoria",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            },
            text = {
                Text(
                    text = "Sei sicuro di voler eliminare la categoria '${categoryToDelete?.displayName}'? Questa operazione rimuoverà la categoria e le sue impostazioni di formato.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Reject)
                        categoryToDelete?.let { target ->
                            onDeleteCategory?.invoke(target.id)
                        }
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Elimina Categoria", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        categoryToDelete = null
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text("Annulla", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

/**
 * Dialog modale per creare o modificare una categoria (nome, icona, prefisso predefinito, parole chiave).
 */
@Composable
fun AddEditCategoryDialog(
    categoryToEdit: PeritoCategoryDefinition?,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, defaultTitle: String, keywords: List<String>) -> Unit
) {
    val semantic = LocalPeritoSemanticColors.current
    val haptic = LocalHapticFeedback.current
    var name by remember(categoryToEdit) { mutableStateOf(categoryToEdit?.displayName ?: "") }
    var emoji by remember(categoryToEdit) { mutableStateOf(categoryToEdit?.emoji ?: "⚡") }
    var defaultTitle by remember(categoryToEdit) { mutableStateOf(categoryToEdit?.defaultPrefix ?: "") }
    var keywordsStr by remember(categoryToEdit) { mutableStateOf(categoryToEdit?.keywords?.joinToString(", ") ?: "") }

    val isEditing = categoryToEdit != null
    val isValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = if (emoji.isNotBlank()) emoji else "🏷️", fontSize = 22.sp)
                Text(
                    text = if (isEditing) "Modifica Categoria" else "Nuova Categoria",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isEditing)
                        "Personalizza il nome, l'icona, il prefisso di default e le parole chiave usate dall'IA per riconoscere questa categoria nei messaggi vocali e scritti."
                    else
                        "Crea una nuova categoria per la tua agenda. L'assistente IA la userà automaticamente per categorizzare e formattare gli appuntamenti quando riconosce le parole chiave.",
                    style = MaterialTheme.typography.bodySmall,
                    color = semantic.labelTertiary,
                    lineHeight = 17.sp
                )

                // Row: Emoji + Nome
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { emoji = it.take(4) },
                        label = { Text("Icona") },
                        modifier = Modifier.width(76.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (!isEditing && defaultTitle.isBlank()) {
                                defaultTitle = it
                            }
                        },
                        label = { Text("Nome Categoria *") },
                        placeholder = { Text("es. Sanatoria, APE") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Prefisso Titolo Predefinito
                OutlinedTextField(
                    value = defaultTitle,
                    onValueChange = { defaultTitle = it },
                    label = { Text("Prefisso Titolo Predefinito") },
                    placeholder = { Text("es. Sopralluogo APE, Pratica Sanatoria") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                // Parole Chiave per IA
                OutlinedTextField(
                    value = keywordsStr,
                    onValueChange = { keywordsStr = it },
                    label = { Text("Parole Chiave IA (separate da virgola)") },
                    placeholder = { Text("es. ape, energetica, 110, attestato") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        val kws = keywordsStr.split(",")
                            .map { it.trim().lowercase(Locale.ITALIAN) }
                            .filter { it.isNotBlank() }
                        onSave(name.trim(), emoji.trim().ifBlank { "📌" }, defaultTitle.trim().ifBlank { name.trim() }, kws)
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isEditing) "Salva Modifiche" else "Crea Categoria", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text("Annulla", color = MaterialTheme.colorScheme.onSurface)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

/**
 * Singola scheda Accordion per configurare il formato di una categoria con Live Preview.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryAccordionCard(
    category: PeritoCategoryDefinition,
    format: CategoryReferenceFormat,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onUpdateFormat: (CategoryReferenceFormat) -> Unit,
    onEditCategory: (() -> Unit)? = null,
    onDeleteCategory: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val semantic = LocalPeritoSemanticColors.current
    val haptic = LocalHapticFeedback.current
    val animationsEnabled = remember(context) { PeritoMotionTokens.areAnimationsEnabled(context) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = if (animationsEnabled) PeritoMotionTokens.snappySpring else snap(),
        label = "chevronRotation"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            if (isExpanded) 1.dp else 0.5.dp,
            if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 2.dp else 0.5.dp),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = if (animationsEnabled) PeritoMotionTokens.contentSizeSpring else snap())
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Intestazione Card cliccabile per espandere/collassare
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onToggleExpand()
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji / Icona Categoria
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape),
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
                        color = MaterialTheme.colorScheme.onSurface
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

                // Icone Azione: Modifica & Elimina
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onEditCategory?.invoke()
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Modifica categoria",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (!category.isSystem) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onDeleteCategory?.invoke()
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Elimina categoria",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            modifier = Modifier.size(17.dp)
                        )
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
                        tint = semantic.labelTertiary
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
                    if (category.keywords.isNotEmpty()) {
                        Text(
                            text = "Parole chiave IA: ${category.keywords.joinToString(", ")}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                            color = semantic.labelTertiary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Text(
                        text = "Elementi da includere nel titolo:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = semantic.labelTertiary
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
                            color = semantic.labelTertiary
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
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            onToggle()
        },
        shape = RoundedCornerShape(20.dp),
        color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
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
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.5.sp
                ),
                color = if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
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
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        },
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
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
    val semantic = LocalPeritoSemanticColors.current
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isMuted) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isMuted) semantic.labelTertiary else MaterialTheme.colorScheme.primary,
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
    category: PeritoCategoryDefinition,
    format: CategoryReferenceFormat
) {
    // Esempio realistico per la categoria specifica
    val sampleSubject = when (category.id.lowercase()) {
        "unioncasa" -> "Unioncasa"
        "agenzia_orso" -> "Agenzia Orso"
        "ape" -> "Unioncasa"
        "sopralluogo" -> "Ing. Ferrari"
        "rilievo" -> "Geom. Bianchi"
        "tribunale" -> "Tribunale Civile"
        "comune" -> "Ufficio Tecnico"
        "riunione" -> "Arch. Neri"
        "cliente" -> "Dott. De Luca"
        "personale" -> ""
        else -> "Studio Associato"
    }
    val samplePlace = when (category.id.lowercase()) {
        "unioncasa" -> "Via Roma 14, Monza"
        "agenzia_orso" -> "Via Garibaldi 3, Milano"
        "tribunale" -> "Via Freguglia 1"
        "comune" -> "Piazza Scala 2"
        else -> "Via Dante 10"
    }
    val sampleNotes = when (category.id.lowercase()) {
        "tribunale" -> "Giuramento CTU R.G. 4102/2026"
        "unioncasa" -> "Stima immobile"
        "rilievo" -> "Rilievo scanner laser"
        "ape" -> "APE residenziale cl. E"
        else -> "Verifica fessurazioni"
    }

    val simulatedTitle = format.formatTitle(
        startTimeIso = "2026-10-22T10:30:00",
        categoryType = category.defaultPrefix,
        subject = sampleSubject,
        place = samplePlace,
        notes = sampleNotes
    )

    val semantic = LocalPeritoSemanticColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(12.dp))
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
                color = MaterialTheme.colorScheme.primary
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
            ) {
                Text(
                    text = "${category.emoji} ${category.displayName}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
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
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Dettaglio secondario visibile
        Text(
            text = "📍 $samplePlace • 🕒 10:30 - 11:30",
            style = MaterialTheme.typography.labelSmall,
            color = semantic.labelTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
