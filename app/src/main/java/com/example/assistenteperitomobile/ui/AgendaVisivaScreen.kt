package com.example.assistenteperitomobile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.assistenteperitomobile.domain.CalendarEventItem
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

    // Calcolo giorni rimanenti della settimana corrente (da dopodomani fino a domenica)
    val remainingWeekDays = remember(today, endOfWeek) {
        val list = mutableListOf<LocalDate>()
        var d = dayAfterTomorrow
        while (!d.isAfter(endOfWeek)) {
            list.add(d)
            d = d.plusDays(1)
        }
        list
    }

    // Se siamo a fine settimana (venerdì/sabato/domenica), mostra i giorni rimanenti + la settimana successiva lavorativa
    val targetDays = remember(today, remainingWeekDays) {
        if (remainingWeekDays.size >= 2) {
            remainingWeekDays
        } else {
            val combined = remainingWeekDays.toMutableList()
            val nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            for (i in 0 until 5) {
                val nextDay = nextMonday.plusDays(i.toLong())
                if (!combined.contains(nextDay) && nextDay != tomorrow) {
                    combined.add(nextDay)
                }
            }
            combined
        }
    }

    val dayFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN)
    }

    val otherEvents = remember(uiState.upcomingEvents, uiState.todayEvents, uiState.tomorrowEvents) {
        uiState.upcomingEvents.filter { ev ->
            !uiState.todayEvents.any { it.id == ev.id } && !uiState.tomorrowEvents.any { it.id == ev.id }
        }
    }

    val targetDaysIso = remember(targetDays) { targetDays.map { it.toString() }.toSet() }
    val weeklyEvents = remember(otherEvents, targetDaysIso) {
        otherEvents.filter { it.startIso.take(10) in targetDaysIso }
    }

    val laterEvents = remember(otherEvents, targetDays) {
        val lastTargetDate = targetDays.lastOrNull()
        if (lastTargetDate != null) {
            otherEvents.filter { it.startIso.take(10) > lastTargetDate.toString() }
        } else emptyList()
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
                        Text(
                            text = if (uiState.todayEvents.isNotEmpty()) {
                                "☀️ ${uiState.todayEvents.size} sopralluogh${if (uiState.todayEvents.size == 1) "o" else "i"} in programma oggi"
                            } else {
                                "Nessun sopralluogo per oggi"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (uiState.todayEvents.isNotEmpty()) DavezBordeaux else AppleLabelSecondary,
                            fontWeight = if (uiState.todayEvents.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
                        )
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
                            val intent = viewModel.calendarRepo.createGoogleMapsIntent(ev.luogo)
                            context.startActivity(intent)
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
                            val intent = viewModel.calendarRepo.createGoogleMapsIntent(ev.luogo)
                            context.startActivity(intent)
                        },
                        onReschedule = { eventToReschedule = ev },
                        onDelete = { viewModel.deleteCalendarEvent(ev) }
                    )
                }
            }

            // Sezione PROGRAMMA SETTIMANALE • GIORNI RIMANENTI
            item(key = "header_weekly") {
                SectionHeaderBadge(
                    title = "PROGRAMMA SETTIMANALE • GIORNI RIMANENTI",
                    count = weeklyEvents.size,
                    color = DavezBordeaux
                )
            }

            if (targetDays.isEmpty()) {
                item(key = "empty_weekly") {
                    EmptyEventPlaceholder(message = "Nessun altro impegno programmato per questa settimana.")
                }
            } else {
                targetDays.forEach { day ->
                    val dayIso = day.toString()
                    val dayLabel = dayFormatter.format(day).replaceFirstChar { it.uppercase(Locale.ITALIAN) }
                    val dayEvents = weeklyEvents.filter { it.startIso.take(10) == dayIso }

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
                                    val intent = viewModel.calendarRepo.createGoogleMapsIntent(ev.luogo)
                                    context.startActivity(intent)
                                },
                                onReschedule = { eventToReschedule = ev },
                                onDelete = { viewModel.deleteCalendarEvent(ev) }
                            )
                        }
                    }
                }
            }

            // Se ci sono ulteriori impegni oltre il programma settimanale corrente
            if (laterEvents.isNotEmpty()) {
                item(key = "header_later") {
                    SectionHeaderBadge(
                        title = "SUCCESSIVI",
                        count = laterEvents.size,
                        color = AppleLabelSecondary
                    )
                }

                items(laterEvents, key = { it.id }) { ev ->
                    AgendaEventCard(
                        event = ev,
                        isToday = false,
                        onOpenMap = {
                            val intent = viewModel.calendarRepo.createGoogleMapsIntent(ev.luogo)
                            context.startActivity(intent)
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
            onConfirm = { newStartIso, newEndIso ->
                viewModel.rescheduleCalendarEvent(
                    event = ev,
                    newStartIso = newStartIso,
                    newEndIso = newEndIso,
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

                // Cliente
                if (event.cliente.isNotBlank() && event.cliente != "Cliente") {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AppleLabelSecondary, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(text = event.cliente, style = MaterialTheme.typography.bodySmall, color = AppleLabelSecondary)
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
                    text = "$eventCount sopralluogh${if (eventCount == 1) "o" else "i"}",
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
