package com.example.assistenteperitomobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.assistenteperitomobile.domain.ArchivedDocument
import com.example.assistenteperitomobile.domain.Fascicolo
import com.example.assistenteperitomobile.domain.FascicoloStato
import com.example.assistenteperitomobile.theme.AppleBlueDark
import com.example.assistenteperitomobile.theme.AppleBlueSoft
import com.example.assistenteperitomobile.theme.AppleSecondaryGroupedBg
import com.example.assistenteperitomobile.theme.AppleGreen
import com.example.assistenteperitomobile.theme.AppleLabelPrimary
import com.example.assistenteperitomobile.theme.AppleLabelSecondary
import com.example.assistenteperitomobile.theme.AppleRed
import com.example.assistenteperitomobile.theme.AppleWhite
import com.example.assistenteperitomobile.theme.DavezAlabaster
import com.example.assistenteperitomobile.theme.DavezBordeaux
import com.example.assistenteperitomobile.theme.DavezStoneBorder

/**
 * Schermata di Gestione Fascicoli / Pratiche per il Perito Tecnico.
 * Raggruppa perizie, foto di sopralluogo, documenti e appuntamenti per singola commessa/sinistro.
 */
@Composable
fun FascicoliPraticheScreen(
    uiState: PeritoUiState,
    viewModel: PeritoViewModel,
    onOpenInAppCamera: () -> Unit,
    onPickDocumentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by rememberSaveable { mutableStateOf<FascicoloStato?>(null) }
    var showNewFascicoloDialog by rememberSaveable { mutableStateOf(false) }
    var expandedFascicoloId by rememberSaveable { mutableStateOf<String?>(null) }

    val filteredList = remember(uiState.fascicoli, selectedFilter) {
        if (selectedFilter == null) {
            uiState.fascicoli
        } else {
            uiState.fascicoli.filter { it.stato == selectedFilter }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DavezAlabaster)
    ) {
        // Intestazione & Pulsante Crea Fascicolo
        Surface(
            color = AppleWhite,
            border = BorderStroke(0.5.dp, DavezStoneBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Fascicoli & Pratiche",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold
                            ),
                            color = AppleLabelPrimary
                        )
                        Text(
                            text = "${uiState.fascicoli.size} commesse registrate nello studio",
                            style = MaterialTheme.typography.labelMedium,
                            color = AppleLabelSecondary
                        )
                    }

                    Button(
                        onClick = { showNewFascicoloDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DavezBordeaux),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = AppleWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Nuovo Fascicolo",
                            color = AppleWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Filtri per Stato (Tutti / In Corso / Aperti / Chiusi)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("Tutti (${uiState.fascicoli.size})", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DavezBordeaux,
                            selectedLabelColor = AppleWhite
                        )
                    )
                    FascicoloStato.entries.forEach { stato ->
                        val count = uiState.fascicoli.count { it.stato == stato }
                        FilterChip(
                            selected = selectedFilter == stato,
                            onClick = { selectedFilter = if (selectedFilter == stato) null else stato },
                            label = { Text("${stato.label} ($count)", fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DavezBordeaux,
                                selectedLabelColor = AppleWhite
                            )
                        )
                    }
                }
            }
        }

        // Lista Fascicoli
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = AppleLabelSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Nessun fascicolo trovato",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppleLabelPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Tocca '+ Nuovo Fascicolo' per creare la prima commessa o perizia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleLabelSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList, key = { it.id }) { fascicolo ->
                    val isExpanded = expandedFascicoloId == fascicolo.id
                    val docsForThisFascicolo = remember(uiState.archivedDocuments, fascicolo.id) {
                        uiState.archivedDocuments.filter { it.fascicoloId == fascicolo.id }
                    }

                    FascicoloCard(
                        fascicolo = fascicolo,
                        documents = docsForThisFascicolo,
                        isExpanded = isExpanded,
                        onCardClick = {
                            expandedFascicoloId = if (isExpanded) null else fascicolo.id
                        },
                        onAddPhoto = {
                            viewModel.selectFascicoloFilter(fascicolo.id)
                            onOpenInAppCamera()
                        },
                        onAttachDoc = {
                            viewModel.selectFascicoloFilter(fascicolo.id)
                            onPickDocumentClick()
                        },
                        onToggleStato = {
                            val nextStato = when (fascicolo.stato) {
                                FascicoloStato.APERTO -> FascicoloStato.IN_CORSO
                                FascicoloStato.IN_CORSO -> FascicoloStato.CHIUSO
                                FascicoloStato.CHIUSO -> FascicoloStato.IN_CORSO
                            }
                            viewModel.updateFascicoloStato(fascicolo.id, nextStato)
                        },
                        onOpenDoc = { doc ->
                            val intent = viewModel.archiveRepo.createShareOrOpenIntent(doc, asShare = false)
                            if (intent != null) context.startActivity(intent)
                        },
                        onShareDoc = { doc ->
                            val intent = viewModel.archiveRepo.createShareOrOpenIntent(doc, asShare = true)
                            if (intent != null) context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }

    if (showNewFascicoloDialog) {
        NewFascicoloDialog(
            onDismiss = { showNewFascicoloDialog = false },
            onConfirm = { cod, tit, cli, cont, ind, not ->
                viewModel.createNewFascicolo(cod, tit, cli, cont, ind, not)
                showNewFascicoloDialog = false
            }
        )
    }
}

@Composable
fun FascicoloCard(
    fascicolo: Fascicolo,
    documents: List<ArchivedDocument>,
    isExpanded: Boolean,
    onCardClick: () -> Unit,
    onAddPhoto: () -> Unit,
    onAttachDoc: () -> Unit,
    onToggleStato: () -> Unit,
    onOpenDoc: (ArchivedDocument) -> Unit,
    onShareDoc: (ArchivedDocument) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AppleWhite,
        border = BorderStroke(0.6.dp, DavezStoneBorder),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Fascicolo: Codice + Pill Stato
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AppleBlueSoft,
                    border = BorderStroke(0.5.dp, DavezBordeaux.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = fascicolo.codice,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = DavezBordeaux,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (fascicolo.stato) {
                        FascicoloStato.IN_CORSO -> DavezBordeaux.copy(alpha = 0.12f)
                        FascicoloStato.APERTO -> AppleBlueSoft
                        FascicoloStato.CHIUSO -> AppleSecondaryGroupedBg
                    },
                    modifier = Modifier.clickable { onToggleStato() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    when (fascicolo.stato) {
                                        FascicoloStato.IN_CORSO -> DavezBordeaux
                                        FascicoloStato.APERTO -> AppleBlueDark
                                        FascicoloStato.CHIUSO -> AppleLabelSecondary
                                    }
                                )
                        )
                        Text(
                            text = fascicolo.stato.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = when (fascicolo.stato) {
                                FascicoloStato.IN_CORSO -> DavezBordeaux
                                FascicoloStato.APERTO -> AppleBlueDark
                                FascicoloStato.CHIUSO -> AppleLabelSecondary
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Titolo Pratica
            Text(
                text = fascicolo.titolo,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                ),
                color = AppleLabelPrimary
            )

            Spacer(Modifier.height(6.dp))

            // Cliente & Controparte
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = AppleLabelSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Cliente: ${fascicolo.cliente}" + if (fascicolo.controparte.isNotBlank()) " • Controparte: ${fascicolo.controparte}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppleLabelSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (fascicolo.indirizzo.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = AppleRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = fascicolo.indirizzo,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppleLabelSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Badge Documenti & Foto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📎 ${documents.size} file archiviati",
                    style = MaterialTheme.typography.labelSmall,
                    color = DavezBordeaux,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = if (isExpanded) "Comprimi ▲" else "Dettagli & File ▼",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppleLabelSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Sezione Espansa: Documenti & Azioni Rapide
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    if (fascicolo.note.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DavezAlabaster,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "📝 Note: ${fascicolo.note}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppleLabelPrimary,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    // Pulsanti Rapidi di Acquisizione per questo Fascicolo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onAddPhoto,
                            colors = ButtonDefaults.buttonColors(containerColor = DavezBordeaux),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Foto Sopralluogo", fontSize = 11.5.sp)
                        }

                        OutlinedButton(
                            onClick = onAttachDoc,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.8.dp, DavezBordeaux),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = DavezBordeaux, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Allega File", color = DavezBordeaux, fontSize = 11.5.sp)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (documents.isEmpty()) {
                        Text(
                            text = "Nessun documento o foto ancora collegato a questa pratica.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleLabelSecondary
                        )
                    } else {
                        Text(
                            text = "Documenti del Fascicolo:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppleLabelPrimary
                        )
                        Spacer(Modifier.height(6.dp))

                        documents.forEach { doc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = null,
                                        tint = DavezBordeaux,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = doc.fileName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppleLabelPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = { onOpenDoc(doc) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("Apri", fontSize = 11.sp, color = DavezBordeaux, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(
                                        onClick = { onShareDoc(doc) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = "Condividi",
                                            tint = AppleLabelSecondary,
                                            modifier = Modifier.size(14.dp)
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

@Composable
fun NewFascicoloDialog(
    onDismiss: () -> Unit,
    onConfirm: (codice: String, titolo: String, cliente: String, controparte: String, indirizzo: String, note: String) -> Unit
) {
    var codice by rememberSaveable { mutableStateOf("PRAT-${System.currentTimeMillis().toString().takeLast(4)}") }
    var titolo by rememberSaveable { mutableStateOf("") }
    var cliente by rememberSaveable { mutableStateOf("") }
    var controparte by rememberSaveable { mutableStateOf("") }
    var indirizzo by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = AppleWhite,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nuovo Fascicolo / Pratica",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppleLabelPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = AppleLabelSecondary)
                    }
                }

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = codice,
                    onValueChange = { codice = it },
                    label = { Text("Codice Pratica (es. CTU-2026-01)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DavezBordeaux),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = titolo,
                    onValueChange = { titolo = it },
                    label = { Text("Oggetto / Titolo Perizia *") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DavezBordeaux),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = cliente,
                    onValueChange = { cliente = it },
                    label = { Text("Cliente / Committente / Tribunale *") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DavezBordeaux),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = controparte,
                    onValueChange = { controparte = it },
                    label = { Text("Controparte / Compagnia (opzionale)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DavezBordeaux),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = indirizzo,
                    onValueChange = { indirizzo = it },
                    label = { Text("Luogo / Indirizzo Rilievo o Perizia (opzionale)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DavezBordeaux),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Memo periziali") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DavezBordeaux),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annulla", color = AppleLabelSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (titolo.isNotBlank() && cliente.isNotBlank()) {
                                onConfirm(codice, titolo, cliente, controparte, indirizzo, note)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DavezBordeaux),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Crea Fascicolo", color = AppleWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
