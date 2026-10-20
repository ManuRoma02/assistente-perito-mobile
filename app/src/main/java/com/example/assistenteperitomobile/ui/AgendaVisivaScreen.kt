package com.example.assistenteperitomobile.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistenteperitomobile.domain.CalendarEventItem
import com.example.assistenteperitomobile.domain.PeritoEventCategory
import com.example.assistenteperitomobile.theme.AppleBlueDark
import com.example.assistenteperitomobile.theme.AppleBlueSoft
import com.example.assistenteperitomobile.theme.AppleGreen
import com.example.assistenteperitomobile.theme.AppleLabelPrimary
import com.example.assistenteperitomobile.theme.AppleLabelSecondary
import com.example.assistenteperitomobile.theme.AppleLabelTertiary
import com.example.assistenteperitomobile.theme.AppleRed
import com.example.assistenteperitomobile.theme.AppleWhite
import com.example.assistenteperitomobile.theme.DavezAlabaster
import com.example.assistenteperitomobile.theme.DavezBordeaux
import com.example.assistenteperitomobile.theme.DavezStoneBorder
import com.example.assistenteperitomobile.theme.DavezTravertine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Schermata Agenda Visiva: visualizza i sopralluoghi e le udienze in ordine cronologico
 * con indicatore di conflitto, promemoria nativi e pulsante diretto per avviare il navigatore Google Maps.
 */
@Composable
fun AgendaVisivaScreen(
    uiState: PeritoUiState,
    viewModel: PeritoViewModel,
    onRequestCalendarPerm: () -> Unit,
    onDirectVoiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var eventToReschedule by remember { mutableStateOf<CalendarEventItem?>(null) }

    val today = remember { LocalDate.now() }
    val tomorrow = remember(today) { today.plusDays(1) }
    val dayAfterTomorrow = remember(today) { today.plusDays(2) }
    val endOfWeek = remember(today) {
        today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    }

    // Calcolo giorni rimanenti della settimana corrente (da dopodomani fino a domenica corrente)
    val remainingWeekDays = remember(today, endOfWeek) {
        val list = mutableListOf<LocalDate>()
        var d = dayAfterTomorrow
        while (!d.isAfter(endOfWeek)) {
            list.add(d)
            d = d.plusDays(1)
        }
        list
    }
    val remainingWeekDaysIso = remember(remainingWeekDays) {
        remainingWeekDays.map { it.toString() }.toSet()
    }

    // Settimana successiva (da Lunedì a Domenica della prossima settimana)
    val nextWeekStart = remember(endOfWeek) { endOfWeek.plusDays(1) }
    val nextWeekEnd = remember(nextWeekStart) { nextWeekStart.plusDays(6) }
    val nextWeekStartIso = remember(nextWeekStart) { nextWeekStart.toString() }
    val nextWeekEndIso = remember(nextWeekEnd) { nextWeekEnd.toString() }

    val dayFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN)
    }
    val nextWeekRangeLabel = remember(nextWeekStart, nextWeekEnd) {
        val f = DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN)
        "Da ${f.format(nextWeekStart)} a ${f.format(nextWeekEnd)}"
    }

    val otherEvents = remember(uiState.upcomingEvents, uiState.todayEvents, uiState.tomorrowEvents) {
        uiState.upcomingEvents.filter { ev ->
            !uiState.todayEvents.any { it.id == ev.id } && !uiState.tomorrowEvents.any { it.id == ev.id }
        }
    }

    // Eventi nei giorni rimanenti di questa settimana
    val currentWeekEvents = remember(otherEvents, remainingWeekDaysIso) {
        otherEvents.filter { it.startIso.take(10) in remainingWeekDaysIso }
    }

    // Eventi della SETTIMANA SUCCESSIVA
    val nextWeekEvents = remember(otherEvents, nextWeekStartIso, nextWeekEndIso) {
        otherEvents.filter {
            val d = it.startIso.take(10)
            d >= nextWeekStartIso && d <= nextWeekEndIso
        }
    }

    // Eventi oltre la settimana successiva
    val laterEvents = remember(otherEvents, nextWeekEndIso) {
        otherEvents.filter { it.startIso.take(10) > nextWeekEndIso }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DavezAlabaster)
    ) {
        // Top Card Informativa Agenda con sfondo uniforme DavezAlabaster
        Surface(
            color = DavezAlabaster,
            border = BorderStroke(0.5.dp, DavezStoneBorder.copy(alpha = 0.5f)),
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
                            text = "Agenda Sopralluoghi",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold
                            ),
                            color = AppleLabelPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val todayCount = uiState.todayEvents.size
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (todayCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(DavezBordeaux)
                                )
                            }
                            Text(
                                text = when (todayCount) {
                                    0 -> "Nessun sopralluogo per oggi"
                                    1 -> "1 sopralluogo in programma oggi"
                                    else -> "$todayCount sopralluoghi in programma oggi"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (todayCount > 0) DavezBordeaux else AppleLabelSecondary,
                                fontWeight = if (todayCount > 0) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }

                    Button(
                        onClick = onDirectVoiceClick,
                        colors = ButtonDefaults.buttonColors(containerColor = DavezBordeaux),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Detta Impegno", color = AppleWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (!uiState.hasCalendarPermission) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFF3CD),
                        border = BorderStroke(0.5.dp, Color(0xFFFFC107)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFF856404), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (!uiState.hasNativeCalendarPermission) {
                                        "Permessi Calendario mancanti: gli impegni restano salvati solo localmente."
                                    } else {
                                        "Calendario di sistema scollegato: gli impegni restano salvati nell'Agenda locale."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF856404)
                                )
                            }
                            TextButton(
                                onClick = {
                                    if (!uiState.hasNativeCalendarPermission) {
                                        onRequestCalendarPerm()
                                    } else {
                                        viewModel.connectCalendar()
                                    }
                                }
                            ) {
                                Text(
                                    text = if (!uiState.hasNativeCalendarPermission) "Attiva" else "Collega",
                                    color = DavezBordeaux,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                } else {
                    var showCalendarAccountDialog by remember { mutableStateOf(false) }
                    if (showCalendarAccountDialog) {
                        SelectCalendarAccountDialog(
                            availableCalendars = uiState.availableCalendars,
                            selectedCalendarId = uiState.aiConfig.selectedCalendarId,
                            onSelectCalendar = { viewModel.selectSystemCalendar(it) },
                            onDismiss = { showCalendarAccountDialog = false }
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Surface(
                        onClick = { showCalendarAccountDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        color = AppleWhite,
                        border = BorderStroke(0.5.dp, DavezStoneBorder.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = DavezBordeaux,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Account: ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppleLabelSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                val accountLabel = remember(uiState.aiConfig.selectedCalendarName, uiState.aiConfig.selectedCalendarAccount) {
                                    val name = uiState.aiConfig.selectedCalendarName
                                    val acc = uiState.aiConfig.selectedCalendarAccount
                                    when {
                                        name == null -> "Tutti gli account"
                                        acc.isNullOrBlank() || acc.equals(name, ignoreCase = true) -> name
                                        else -> "$name ($acc)"
                                    }
                                }
                                Text(
                                    text = accountLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DavezBordeaux,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                            Text(
                                text = "Cambia ▾",
                                style = MaterialTheme.typography.labelSmall,
                                color = DavezBordeaux,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Timeline Giornaliera
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sezione OGGI
            item(key = "header_today") {
                SectionHeaderBadge(
                    title = "OGGI",
                    count = uiState.todayEvents.size,
                    color = DavezBordeaux
                )
            }

            if (uiState.todayEvents.isEmpty()) {
                item(key = "empty_today") {
                    EmptyEventPlaceholder(message = "Nessun impegno o sopralluogo fissato per oggi.")
                }
            } else {
                items(uiState.todayEvents, key = { it.id }) { ev ->
                    AgendaEventCard(
                        event = ev,
                        isToday = true,
                        onOpenMap = {
                            viewModel.calendarRepo.launchGoogleMaps(context, ev.luogo)
                        },
                        onReschedule = { eventToReschedule = ev },
                        onDelete = { viewModel.deleteCalendarEvent(ev) }
                    )
                }
            }

            // Sezione DOMANI
            item(key = "header_tomorrow") {
                SectionHeaderBadge(
                    title = "DOMANI",
                    count = uiState.tomorrowEvents.size,
                    color = AppleBlueDark
                )
            }

            if (uiState.tomorrowEvents.isEmpty()) {
                item(key = "empty_tomorrow") {
                    EmptyEventPlaceholder(message = "Nessun sopralluogo programmato per domani.")
                }
            } else {
                items(uiState.tomorrowEvents, key = { it.id }) { ev ->
                    AgendaEventCard(
                        event = ev,
                        isToday = false,
                        onOpenMap = {
                            viewModel.calendarRepo.launchGoogleMaps(context, ev.luogo)
                        },
                        onReschedule = { eventToReschedule = ev },
                        onDelete = { viewModel.deleteCalendarEvent(ev) }
                    )
                }
            }

            // Sezione QUESTA SETTIMANA • GIORNI RIMANENTI (se ci sono giorni da dopodomani a domenica)
            if (remainingWeekDays.isNotEmpty()) {
                item(key = "header_this_week") {
                    SectionHeaderBadge(
                        title = "QUESTA SETTIMANA • GIORNI RIMANENTI",
                        count = currentWeekEvents.size,
                        color = DavezBordeaux
                    )
                }

                remainingWeekDays.forEach { day ->
                    val dayIso = day.toString()
                    val dayLabel = dayFormatter.format(day).replaceFirstChar { it.uppercase(Locale.ITALIAN) }
                    val dayEvents = currentWeekEvents.filter { it.startIso.take(10) == dayIso }

                    item(key = "day_header_$dayIso") {
                        DaySubHeaderBadge(
                            dayLabel = dayLabel,
                            eventCount = dayEvents.size
                        )
                    }

                    if (dayEvents.isEmpty()) {
                        item(key = "day_empty_$dayIso") {
                            EmptyDaySlotPlaceholder(dayName = dayLabel)
                        }
                    } else {
                        items(dayEvents, key = { it.id }) { ev ->
                            AgendaEventCard(
                                event = ev,
                                isToday = false,
                                onOpenMap = {
                                    viewModel.calendarRepo.launchGoogleMaps(context, ev.luogo)
                                },
                                onReschedule = { eventToReschedule = ev },
                                onDelete = { viewModel.deleteCalendarEvent(ev) }
                            )
                        }
                    }
                }
            }

            // Sezione SETTIMANA SUCCESSIVA (Card compatte ed espandibili al tap con fisica a molla Emil Kowalski)
            item(key = "header_next_week") {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    SectionHeaderBadge(
                        title = "SETTIMANA SUCCESSIVA",
                        count = nextWeekEvents.size,
                        color = AppleBlueDark
                    )
                    Text(
                        text = "$nextWeekRangeLabel • Tocca per espandere i dettagli",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppleLabelSecondary,
                        modifier = Modifier.padding(start = 2.dp, top = 2.dp, bottom = 4.dp)
                    )
                }
            }

            if (nextWeekEvents.isEmpty()) {
                item(key = "empty_next_week") {
                    EmptyEventPlaceholder(message = "Nessun sopralluogo programmato per la settimana successiva ($nextWeekRangeLabel).")
                }
            } else {
                items(nextWeekEvents, key = { it.id }) { ev ->
                    NextWeekExpandableEventCard(
                        event = ev,
                        onOpenMap = {
                            viewModel.calendarRepo.launchGoogleMaps(context, ev.luogo)
                        },
                        onReschedule = { eventToReschedule = ev },
                        onDelete = { viewModel.deleteCalendarEvent(ev) }
                    )
                }
            }

            // Se ci sono ulteriori impegni oltre la settimana successiva
            if (laterEvents.isNotEmpty()) {
                item(key = "header_later") {
                    SectionHeaderBadge(
                        title = "SUCCESSIVI • OLTRE LA SETTIMANA SUCCESSIVA",
                        count = laterEvents.size,
                        color = AppleLabelSecondary
                    )
                }

                items(laterEvents, key = { it.id }) { ev ->
                    AgendaEventCard(
                        event = ev,
                        isToday = false,
                        onOpenMap = {
                            viewModel.calendarRepo.launchGoogleMaps(context, ev.luogo)
                        },
                        onReschedule = { eventToReschedule = ev },
                        onDelete = { viewModel.deleteCalendarEvent(ev) }
                    )
                }
            }
        }
    }

    eventToReschedule?.let { ev ->
        RescheduleAppointmentDialog(
            event = ev,
            onDismiss = { eventToReschedule = null },
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
                eventToReschedule = null
            }
        )
    }
}

@Composable
fun SectionHeaderBadge(title: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = color
        )
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(20.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 10.5.sp
                )
            }
        }
    }
}

/**
 * Card compatta ed espandibile per gli appuntamenti della settimana successiva (/emilkowalski-animation).
 * - Default: compresso, mostra solo i dettagli essenziali (orario, giorno, categoria e titolo sintetico).
 * - Al tap: espansione fluida a molla su GPU (animateContentSize + rotazione chevron 180° su graphicsLayer)
 *   rivelando indirizzo completo, Google Maps, cliente, note e azioni Modifica/Elimina.
 */
@Composable
fun NextWeekExpandableEventCard(
    event: CalendarEventItem,
    onOpenMap: () -> Unit,
    onReschedule: () -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "next_week_chevron_rot"
    )

    val startTime = event.startIso.drop(11).take(5)
    val endTime = event.endIso.drop(11).take(5)
    val dateIso = event.startIso.take(10)
    val dateLabel = remember(dateIso) {
        runCatching {
            val d = LocalDate.parse(dateIso)
            d.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.ITALIAN)).replaceFirstChar { it.uppercase(Locale.ITALIAN) }
        }.getOrDefault(dateIso)
    }

    val catEnum = remember(event.categoria, event.titolo) {
        PeritoEventCategory.fromString(event.categoria.ifBlank { event.titolo })
    }

    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            isExpanded = !isExpanded
        },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(14.dp),
        color = AppleWhite,
        border = BorderStroke(
            if (isExpanded) 1.dp else 0.5.dp,
            if (isExpanded) AppleBlueDark.copy(alpha = 0.45f) else DavezStoneBorder
        ),
        shadowElevation = if (isExpanded) 2.dp else 0.5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .cartoonPressSquash(interactionSource, pressedScaleY = 0.97f, squashScaleX = 1.015f)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // RIGA PRINCIPALE (Sintetica - Sempre Visibile)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pillola di accento verticale blu Apple
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(34.dp)
                        .background(
                            color = AppleBlueDark,
                            shape = RoundedCornerShape(2.dp)
                        )
                )

                // Informazioni essenziali
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DavezTravertine,
                            border = BorderStroke(0.5.dp, DavezStoneBorder.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "$dateLabel • $startTime-$endTime",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AppleLabelPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 11.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AppleBlueDark.copy(alpha = 0.10f),
                            border = BorderStroke(0.5.dp, AppleBlueDark.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = "${catEnum.emoji} ${catEnum.displayName}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = AppleBlueDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    // Titolo sintetico
                    Text(
                        text = event.titolo,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.1).sp
                        ),
                        color = AppleLabelPrimary,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Chevron rotante senza reflow di layout (GPU graphicsLayer)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer { rotationZ = chevronRotation },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Comprimi dettagli" else "Espandi tutti i dettagli",
                        tint = AppleLabelSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // DETTAGLI COMPLETI (Visibili solo se espanso)
            if (isExpanded) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.6.dp)
                        .background(DavezStoneBorder.copy(alpha = 0.5f))
                )
                Spacer(Modifier.height(10.dp))

                // Cliente (se specificato e significativo)
                if (event.cliente.isNotBlank() && event.cliente != "Cliente" && !event.cliente.equals(event.titolo, ignoreCase = true) && !event.cliente.equals(event.categoria, ignoreCase = true)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AppleLabelSecondary, modifier = Modifier.size(13.dp))
                        Text(
                            text = "Cliente: ${event.cliente}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleLabelSecondary
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }

                // Note personali
                if (event.note.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = AppleLabelSecondary,
                            modifier = Modifier.size(13.dp).padding(top = 1.dp)
                        )
                        Text(
                            text = event.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleLabelSecondary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Indirizzo e Google Maps
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = AppleRed, modifier = Modifier.size(14.dp))
                        Text(
                            text = event.luogo,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleLabelPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (event.luogo.isNotBlank() && event.luogo != "Da definire") {
                        Surface(
                            onClick = onOpenMap,
                            shape = RoundedCornerShape(8.dp),
                            color = DavezTravertine,
                            border = BorderStroke(0.5.dp, AppleBlueDark.copy(alpha = 0.35f)),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null, tint = AppleBlueDark, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Google Maps", color = AppleBlueDark, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Pulsanti Azione Modifica ed Elimina
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onReschedule,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = DavezBordeaux, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Modifica", color = DavezBordeaux, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.width(4.dp))

                    TextButton(
                        onClick = onDelete,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = AppleRed.copy(alpha = 0.85f), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Elimina", color = AppleRed.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun AgendaEventCard(
    event: CalendarEventItem,
    isToday: Boolean,
    onOpenMap: () -> Unit,
    onReschedule: () -> Unit,
    onDelete: () -> Unit
) {
    val startTime = event.startIso.drop(11).take(5)
    val endTime = event.endIso.drop(11).take(5)
    val dateLabel = event.startIso.take(10)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AppleWhite,
        border = BorderStroke(if (isToday) 1.dp else 0.5.dp, if (isToday) DavezBordeaux.copy(alpha = 0.45f) else DavezStoneBorder),
        shadowElevation = if (isToday) 2.dp else 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Apple Calendar vertical accent pill
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(52.dp)
                    .background(
                        color = if (isToday) DavezBordeaux else AppleBlueDark,
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Orario + Data + Pulsanti Modifica ed Elimina
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (isToday) DavezBordeaux else AppleLabelSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "$startTime - $endTime",
                            style = MaterialTheme.typography.titleMedium.copy(
                                letterSpacing = (-0.2).sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = if (isToday) DavezBordeaux else AppleLabelPrimary
                        )
                        if (!isToday) {
                            Text(
                                text = "($dateLabel)",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppleLabelSecondary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = onReschedule,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Modifica orario",
                                tint = DavezBordeaux,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Rimuovi appuntamento",
                                tint = AppleRed.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Categoria Badge
                val catEnum = PeritoEventCategory.fromString(event.categoria.ifBlank { event.titolo })
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DavezBordeaux.copy(alpha = 0.10f),
                    border = BorderStroke(0.6.dp, DavezBordeaux.copy(alpha = 0.30f))
                ) {
                    Text(
                        text = "${catEnum.emoji} ${catEnum.displayName}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = DavezBordeaux,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Titolo
                Text(
                    text = event.titolo,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Serif,
                        letterSpacing = (-0.1).sp
                    ),
                    color = AppleLabelPrimary
                )

                // Cliente (mostrato solo se specificato e diverso da titolo e categoria)
                if (event.cliente.isNotBlank() && event.cliente != "Cliente" && !event.cliente.equals(event.titolo, ignoreCase = true) && !event.cliente.equals(event.categoria, ignoreCase = true)) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AppleLabelSecondary, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(text = event.cliente, style = MaterialTheme.typography.bodySmall, color = AppleLabelSecondary)
                    }
                }

                // Note personali per il calendario (se presenti)
                if (event.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = AppleLabelSecondary, modifier = Modifier.size(13.dp).padding(top = 1.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = event.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleLabelSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Luogo & Pulsante Google Maps
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = AppleRed, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = event.luogo,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleLabelPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (event.luogo.isNotBlank() && event.luogo != "Da definire") {
                        Surface(
                            onClick = onOpenMap,
                            shape = RoundedCornerShape(8.dp),
                            color = DavezTravertine,
                            border = BorderStroke(0.5.dp, AppleBlueDark.copy(alpha = 0.35f)),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null, tint = AppleBlueDark, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Google Maps", color = AppleBlueDark, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Promemoria automatici
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AppleGreen, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Promemoria automatici attivi (24h, 1h, 30m prima)",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppleGreen,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DaySubHeaderBadge(
    dayLabel: String,
    eventCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = DavezBordeaux,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = dayLabel,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = AppleLabelPrimary
            )
        }
        if (eventCount > 0) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DavezBordeaux.copy(alpha = 0.12f)
            ) {
                Text(
                    text = if (eventCount == 1) "1 sopralluogo" else "$eventCount sopralluoghi",
                    style = MaterialTheme.typography.labelSmall,
                    color = DavezBordeaux,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        } else {
            Text(
                text = "Libero",
                style = MaterialTheme.typography.labelSmall,
                color = AppleLabelTertiary
            )
        }
    }
}

@Composable
fun EmptyDaySlotPlaceholder(dayName: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DavezAlabaster,
        border = BorderStroke(0.5.dp, DavezStoneBorder.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = AppleLabelTertiary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Nessun sopralluogo programmato per $dayName",
                style = MaterialTheme.typography.bodySmall,
                color = AppleLabelSecondary
            )
        }
    }
}

@Composable
fun EmptyEventPlaceholder(message: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DavezAlabaster,
        border = BorderStroke(0.5.dp, DavezStoneBorder.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = AppleLabelSecondary,
            modifier = Modifier.padding(14.dp)
        )
    }
}
