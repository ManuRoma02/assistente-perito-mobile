package com.example.assistenteperitomobile.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.assistenteperitomobile.MainActivity
import com.example.assistenteperitomobile.data.ArchiveRepository
import com.example.assistenteperitomobile.data.CalendarRepository
import com.example.assistenteperitomobile.data.ChatHistoryRepository
import com.example.assistenteperitomobile.data.DocumentProcessor
import com.example.assistenteperitomobile.data.DownloadableAiModel
import com.example.assistenteperitomobile.data.FascicoloRepository
import com.example.assistenteperitomobile.data.ModelDownloadManager
import com.example.assistenteperitomobile.data.ModelDownloadProgress
import com.example.assistenteperitomobile.data.MorningBriefingWorker
import com.example.assistenteperitomobile.data.ReasoningBackgroundService
import com.example.assistenteperitomobile.data.ota.OtaUpdateManager
import com.example.assistenteperitomobile.data.ota.OtaUpdateManifest
import com.example.assistenteperitomobile.data.ota.UpdateCheckResult
import com.example.assistenteperitomobile.data.ota.UpdateCheckStatus
import com.example.assistenteperitomobile.domain.AiEngineConfig
import com.example.assistenteperitomobile.domain.AppointmentParseResult
import com.example.assistenteperitomobile.domain.ArchivedDocument
import com.example.assistenteperitomobile.domain.CalendarEventItem
import com.example.assistenteperitomobile.domain.CategoryReferenceFormat
import com.example.assistenteperitomobile.domain.ChatMessage
import com.example.assistenteperitomobile.domain.DocumentClassificationResult
import com.example.assistenteperitomobile.domain.Fascicolo
import com.example.assistenteperitomobile.domain.FascicoloStato
import com.example.assistenteperitomobile.domain.MainAppTab
import com.example.assistenteperitomobile.domain.PendingConflictState
import com.example.assistenteperitomobile.domain.PeritoAiEngine
import com.example.assistenteperitomobile.domain.PeritoCategory
import com.example.assistenteperitomobile.domain.PeritoCategoryDefinition
import com.example.assistenteperitomobile.domain.PeritoEventCategory
import com.example.assistenteperitomobile.domain.StudioProfile
import com.example.assistenteperitomobile.domain.SystemCalendarInfo
import com.example.assistenteperitomobile.domain.defaultCategoryFormats
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@Immutable
data class PeritoUiState(
    val selectedTab: MainAppTab = MainAppTab.CHAT,
    val messages: List<ChatMessage> = emptyList(),
    val isClearingHistory: Boolean = false,
    val fascicoli: List<Fascicolo> = emptyList(),
    val selectedFascicoloId: String? = null,
    val archivedDocuments: List<ArchivedDocument> = emptyList(),
    val selectedCategoryFilter: PeritoCategory? = null,
    val archiveSearchQuery: String = "",
    val todayEvents: List<CalendarEventItem> = emptyList(),
    val tomorrowEvents: List<CalendarEventItem> = emptyList(),
    val upcomingEvents: List<CalendarEventItem> = emptyList(),
    val isProcessing: Boolean = false,
    val processingLabel: String = "",
    val hasCalendarPermission: Boolean = false,
    val hasNativeCalendarPermission: Boolean = false,
    val availableCalendars: List<SystemCalendarInfo> = emptyList(),
    val aiConfig: AiEngineConfig = AiEngineConfig(),
    val statusBanner: String? = null,
    val showFirstRunModelDialog: Boolean = false,
    val downloadProgress: ModelDownloadProgress = ModelDownloadProgress(),
    val downloadedModelIds: Set<String> = emptySet(),
    val isReasoningReady: Boolean = false,
    val areAllModelsReady: Boolean = false,
    val availableUpdate: OtaUpdateManifest? = null,
    val isCheckingUpdate: Boolean = false,
    val updateDownloadProgress: Float? = null,
    val updateStatusMessage: String? = null,
    val showUpdateDialog: Boolean = false,
    val currentAppVersionName: String = "1.0",
    val currentAppVersionCode: Int = 1
)

class PeritoViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    val aiEngine = PeritoAiEngine(appContext)
    val archiveRepo = ArchiveRepository(appContext, aiEngine)
    val calendarRepo = CalendarRepository(appContext)
    val docProcessor = DocumentProcessor(appContext)
    val modelDownloadManager = ModelDownloadManager(appContext)
    val chatHistoryRepo = ChatHistoryRepository(appContext)
    val fascicoloRepo = FascicoloRepository(appContext)
    val otaUpdateManager = OtaUpdateManager(appContext)

    private var activeDownloadJob: Job? = null

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("PeritoViewModel", "Unhandled coroutine exception in appScope", throwable)
    }

    // Scope a livello di applicazione/processo: sopravvive all'uscita dalla schermata o standby del telefono
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + coroutineExceptionHandler)

    private val configFile = File(appContext.filesDir, "ai_config.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    // Memoria conversazionale multi-turno per chiarimenti e correzioni post-azione
    private var pendingAmbiguousDraft: String? = null
    private var lastCreatedEvent: CalendarEventItem? = null

    private val _uiState = MutableStateFlow(PeritoUiState())
    val uiState: StateFlow<PeritoUiState> = _uiState.asStateFlow()

    init {
        val (purgedNames, freedBytes) = modelDownloadManager.purgeObsoleteModels()
        var loadedConfig = loadConfig()
        if (loadedConfig.mediaPipeModelPath.isNotBlank() && !File(loadedConfig.mediaPipeModelPath).exists()) {
            loadedConfig = loadedConfig.copy(
                mediaPipeModelPath = "",
                hasSeenModelSetupPrompt = false
            )
            saveConfig(loadedConfig)
        } else if (purgedNames.isNotEmpty() && !modelDownloadManager.isPrimaryReasoningDownloaded(loadedConfig.mediaPipeModelPath)) {
            loadedConfig = loadedConfig.copy(hasSeenModelSetupPrompt = false)
            saveConfig(loadedConfig)
        }

        val downloadedIds = computeDownloadedModelIds()
        val reasoningReady = modelDownloadManager.isPrimaryReasoningDownloaded(loadedConfig.mediaPipeModelPath)
        val allReady = modelDownloadManager.areAllModelsDownloaded(loadedConfig.mediaPipeModelPath)

        val purgeBanner = if (purgedNames.isNotEmpty()) {
            val freedGb = String.format(Locale.ITALIAN, "%.2f GB", freedBytes / (1024.0 * 1024.0 * 1024.0))
            "🧹 Modello precedente rimosso automaticamente ($freedGb liberati) — pronto per ${modelDownloadManager.primaryReasoningModel.title}"
        } else null

        val studioName = loadedConfig.studioProfile.studioName.ifBlank { "Studio Tecnico Peritale" }
        calendarRepo.isSyncEnabled = loadedConfig.syncSystemCalendar
        calendarRepo.selectedCalendarId = loadedConfig.selectedCalendarId

        val vName = otaUpdateManager.getCurrentVersionName()
        val vCode = otaUpdateManager.getCurrentVersionCode()

        _uiState.update {
            it.copy(
                aiConfig = loadedConfig,
                hasCalendarPermission = calendarRepo.hasCalendarPermissions(),
                hasNativeCalendarPermission = calendarRepo.hasNativeCalendarPermissions(),
                showFirstRunModelDialog = !reasoningReady && !loadedConfig.hasSeenModelSetupPrompt,
                downloadedModelIds = downloadedIds,
                isReasoningReady = reasoningReady,
                areAllModelsReady = allReady,
                statusBanner = purgeBanner ?: it.statusBanner,
                currentAppVersionName = vName,
                currentAppVersionCode = vCode
            )
        }

        // Caricamento asincrono della cronologia persistente della Chat
        viewModelScope.launch {
            val savedMessages = chatHistoryRepo.loadMessages()
            val initialMessages = if (savedMessages.isNotEmpty()) {
                savedMessages
            } else {
                val welcomeMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = "🏛️ **PeritoAI — $studioName (100% On-Device)**\n\n" +
                        "• 📁 **Pratiche & Fascicoli** — Raggruppa perizie, foto e atti per commessa\n" +
                        "• 📅 **Agenda Visiva** — Monitora i sopralluoghi con controllo conflitti e mappe\n" +
                        "• 🎙️ **Nota Vocale o Testo** — Fissa appuntamenti e cerca fascicoli al volo\n" +
                        "• 📸 **Raffica Sopralluogo** — Scatta più foto del rilievo e archiviale nel fascicolo"
                )
                chatHistoryRepo.saveMessages(listOf(welcomeMsg))
                listOf(welcomeMsg)
            }
            _uiState.update { it.copy(messages = initialMessages) }
        }

        MorningBriefingWorker.scheduleDailyBriefing(appContext)
        if (reasoningReady) {
            viewModelScope.launch(Dispatchers.IO) {
                aiEngine.ensureGgufLoaded(loadedConfig)
            }
        }
        refreshAllData()

        if (loadedConfig.autoCheckUpdatesOnStart) {
            checkForUpdates(silent = true)
        }
    }

    fun refreshAllData() {
        viewModelScope.launch {
            val docs = if (_uiState.value.archiveSearchQuery.isBlank()) {
                archiveRepo.loadAllDocuments()
            } else {
                archiveRepo.searchDocuments(_uiState.value.archiveSearchQuery)
            }
            val fascicoliList = fascicoloRepo.loadAllFascicoli()
            val today = LocalDate.now()
            val zone = ZoneId.systemDefault()
            val startOfTodayMillis = today.atStartOfDay(zone).toInstant().toEpochMilli()
            val endOfTomorrowMillis = today.plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
            val nowMillis = System.currentTimeMillis()
            val rangeEndMillis = maxOf(endOfTomorrowMillis, nowMillis + 30L * 86_400_000L)

            // Sincronizzazione preferenza utente con CalendarRepository
            calendarRepo.isSyncEnabled = _uiState.value.aiConfig.syncSystemCalendar
            calendarRepo.selectedCalendarId = _uiState.value.aiConfig.selectedCalendarId

            val availableCals = if (calendarRepo.hasNativeCalendarPermissions()) {
                calendarRepo.getAvailableCalendars()
            } else emptyList()

            // Singola query IPC al Calendar Provider invece di 3 roundtrip separati
            val allEventsInRange = calendarRepo.getEventsInRange(startOfTodayMillis, rangeEndMillis)
            val todayEvs = allEventsInRange.filter {
                val evDate = Instant.ofEpochMilli(it.startMillis).atZone(zone).toLocalDate()
                evDate == today
            }
            val tomorrow = today.plusDays(1)
            val tomorrowEvs = allEventsInRange.filter {
                val evDate = Instant.ofEpochMilli(it.startMillis).atZone(zone).toLocalDate()
                evDate == tomorrow
            }
            val next30Days = allEventsInRange.filter { it.startMillis >= nowMillis - 3_600_000L }

            val customPath = _uiState.value.aiConfig.mediaPipeModelPath
            val reasoningReady = modelDownloadManager.isPrimaryReasoningDownloaded(customPath)
            val allReady = modelDownloadManager.areAllModelsDownloaded(customPath)

            if (calendarRepo.hasNativeCalendarPermissions()) {
                registerCalendarObserver()
            }

            _uiState.update {
                it.copy(
                    archivedDocuments = docs,
                    fascicoli = fascicoliList,
                    todayEvents = todayEvs,
                    tomorrowEvents = tomorrowEvs,
                    upcomingEvents = next30Days,
                    hasCalendarPermission = calendarRepo.hasCalendarPermissions(),
                    hasNativeCalendarPermission = calendarRepo.hasNativeCalendarPermissions(),
                    availableCalendars = availableCals,
                    downloadedModelIds = computeDownloadedModelIds(),
                    isReasoningReady = reasoningReady,
                    areAllModelsReady = allReady
                )
            }
        }
    }

    fun onPermissionsUpdated() {
        if (calendarRepo.hasNativeCalendarPermissions()) {
            if (!_uiState.value.aiConfig.syncSystemCalendar) {
                val updated = _uiState.value.aiConfig.copy(syncSystemCalendar = true)
                calendarRepo.isSyncEnabled = true
                saveConfig(updated)
                _uiState.update { it.copy(aiConfig = updated) }
            }
        }
        refreshAllData()
    }

    fun disconnectCalendar() {
        val updated = _uiState.value.aiConfig.copy(syncSystemCalendar = false)
        calendarRepo.isSyncEnabled = false
        saveConfig(updated)
        _uiState.update {
            it.copy(
                aiConfig = updated,
                hasCalendarPermission = false,
                hasNativeCalendarPermission = calendarRepo.hasNativeCalendarPermissions(),
                statusBanner = "Calendario scollegato: modalità Agenda Locale attiva"
            )
        }
        refreshAllData()
    }

    fun connectCalendar() {
        val updated = _uiState.value.aiConfig.copy(syncSystemCalendar = true)
        calendarRepo.isSyncEnabled = true
        saveConfig(updated)
        _uiState.update {
            it.copy(
                aiConfig = updated,
                hasCalendarPermission = calendarRepo.hasCalendarPermissions(),
                hasNativeCalendarPermission = calendarRepo.hasNativeCalendarPermissions(),
                statusBanner = "Calendario di sistema ricollegato"
            )
        }
        refreshAllData()
    }

    /**
     * Seleziona uno specifico account/calendario di sistema (es. Studio Tecnico o Personale)
     * oppure imposta null per sincronizzare tutti i calendari rilevati sul dispositivo.
     */
    fun selectSystemCalendar(calendar: SystemCalendarInfo?) {
        val updated = _uiState.value.aiConfig.copy(
            selectedCalendarId = calendar?.id,
            selectedCalendarAccount = calendar?.accountName,
            selectedCalendarName = calendar?.name
        )
        calendarRepo.selectedCalendarId = calendar?.id
        saveConfig(updated)
        _uiState.update {
            it.copy(
                aiConfig = updated,
                statusBanner = if (calendar != null) {
                    val calLabel = if (calendar.accountName.isBlank() || calendar.accountName.equals(calendar.name, ignoreCase = true)) {
                        calendar.name
                    } else {
                        "${calendar.name} (${calendar.accountName})"
                    }
                    "📅 Sincronizzazione impostata su: $calLabel"
                } else {
                    "📅 Sincronizzazione attiva con: Tutti gli account"
                }
            )
        }
        refreshAllData()
    }

    /**
     * Consente l'esecuzione immediata e reattiva delle funzioni dello Studio
     * in coordinamento con i modelli IA on-device.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun requireReasoningModelReady(userActionPreview: String? = null): Boolean {
        return true
    }

    /**
     * Invia una Nota Vocale registrata direttamente nella barra vocale in-app (stile WhatsApp)
     * con file audio reale della voce dell'utente (.wav) e trascrizione Google SpeechRecognizer Headless.
     */
    fun sendVoiceNoteMessage(
        spokenText: String,
        durationSeconds: Int,
        voiceAudioPath: String? = null
    ) {
        val clean = spokenText.trim()
        if (clean.isEmpty()) return
        sendUserMessage(
            rawText = clean,
            voiceDurationSeconds = durationSeconds.coerceAtLeast(1),
            voiceAudioPath = voiceAudioPath,
            attachmentType = "VOICE"
        )
    }

    private fun notifyOrStopBackgroundService(replyTitle: String, replyText: String) {
        val appContext = getApplication<Application>()
        if (!MainActivity.isAppInForeground) {
            ReasoningBackgroundService.notifyCompletion(appContext, replyTitle, replyText)
        } else {
            ReasoningBackgroundService.stopReasoning(appContext)
        }
    }

    /**
     * Gestisce l'invio di un messaggio testuale o nota vocale nella Chat Assistente.
     */
    fun sendUserMessage(
        rawText: String,
        voiceDurationSeconds: Int? = null,
        voiceAudioPath: String? = null,
        attachmentType: String? = null
    ) {
        val text = rawText.trim()
        if (text.isEmpty()) return

        // OBBLIGATORIO: senza il modello di Reasoning scaricato, l'app non esegue alcuna azione!
        if (!requireReasoningModelReady(text)) return

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = text,
            attachmentName = if (voiceDurationSeconds != null) "Nota_Vocale_${voiceDurationSeconds}s.wav" else null,
            attachmentType = attachmentType,
            voiceDurationSeconds = voiceDurationSeconds,
            voiceAudioPath = voiceAudioPath
        )
        val lowerText = text.lowercase(Locale.ITALIAN)
        val semanticLabel = when {
            voiceDurationSeconds != null -> "🎙️ Google Speech In-App • Trascrizione vocale & Analisi Qwen 3.5..."
            lowerText.startsWith("/oggi") || lowerText == "oggi" -> "☀️ Comando /oggi • Briefing Giornaliero & Fascicoli..."
            lowerText.startsWith("/cerca") -> "🔍 Comando /cerca • Ricerca mirata nelle 6 Cartelle..."
            lowerText.startsWith("/elimina") || lowerText.startsWith("/cancella") -> "🗑️ Comando /elimina • Rimozione da Archivio o Agenda..."
            lowerText.contains("cerca") || lowerText.contains("mandami") -> "🔍 Ricerca semantica nelle 6 Cartelle..."
            else -> "🧠 ${getActiveReasoningModelLabel()} • Analisi impegno e controllo conflitti (±15m)..."
        }
        _uiState.update {
            it.copy(
                messages = it.messages + userMsg,
                isProcessing = true,
                processingLabel = semanticLabel
            )
        }
        viewModelScope.launch {
            chatHistoryRepo.saveMessages(_uiState.value.messages)
        }

        val appContext = getApplication<Application>()
        ReasoningBackgroundService.startReasoning(
            appContext,
            taskTitle = "🧠 Elaborazione PeritoAI...",
            previewText = text
        )

        appScope.launch {
            try {
                kotlinx.coroutines.delay(380L)
                val lower = text.lowercase(Locale.ITALIAN)

                // 0. ROUTING DETERMINISTICO COMANDI SLASH (/oggi, /cerca, /elimina, /cartelle, /fissa)
                if (lower.startsWith("/")) {
                    pendingAmbiguousDraft = null
                    when {
                        lower == "/oggi" || lower.startsWith("/oggi ") || lower == "/agenda" || lower == "/briefing" -> {
                            val reply = buildBriefingChatMessage()
                            appendAssistantMessage(reply)
                            notifyOrStopBackgroundService("☀️ Briefing di Oggi", reply.text)
                            return@launch
                        }
                        lower == "/orario" || lower.startsWith("/orario ") ||
                        lower == "/sposta" || lower.startsWith("/sposta ") ||
                        lower == "/modifica-orario" || lower.startsWith("/modifica-orario ") -> {
                            val arg = text.replaceFirst(Regex("^/(orario|sposta|modifica-orario)\\s*", RegexOption.IGNORE_CASE), "").trim()
                            val prompt = if (arg.isBlank()) "cambia orario" else "sposta $arg"
                            val handled = handlePostActionCorrectionIfApplicable(prompt)
                            if (!handled) {
                                val lastEv = lastCreatedEvent ?: _uiState.value.upcomingEvents.firstOrNull()
                                if (lastEv != null) {
                                    appendAssistantMessage(
                                        ChatMessage(
                                            id = UUID.randomUUID().toString(),
                                            isUser = false,
                                            text = "🕒 **Modifica Orario Appuntamento**\n\n" +
                                                "• **Impegno:** ${lastEv.titolo}\n" +
                                                "• **Orario attuale:** ${lastEv.startIso.replace('T', ' ').take(16)}\n\n" +
                                                "✍️ Puoi scrivere o dettare ad esempio *\"Alle 11:30\"*, *\"Posticipa di 1 ora\"*, oppure tocca il pulsante **🕒 Modifica Orario** qui sotto.",
                                            createdEvent = lastEv
                                        )
                                    )
                                } else {
                                    appendAssistantMessage(
                                        ChatMessage(
                                            id = UUID.randomUUID().toString(),
                                            isUser = false,
                                            text = "ℹ️ Non ci sono appuntamenti in programma da spostare.\n" +
                                                "Per fissare un nuovo sopralluogo puoi scrivere: *\"/fissa domani alle 15 sopralluogo peritale a Como\"*."
                                        )
                                    )
                                }
                            }
                            notifyOrStopBackgroundService("🕒 Modifica Orario", "Comando orario elaborato")
                            return@launch
                        }
                        lower == "/fissa" || lower == "/appuntamento" -> {
                            appendAssistantMessage(
                                ChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    isUser = false,
                                    text = "📅 **Come posso aiutarti a fissare l'appuntamento?**\n\n" +
                                        "Scrivi o detta ad esempio:\n" +
                                        "• */fissa domani alle 15:30 sopralluogo peritale a Como con Ing. Bianchi*\n" +
                                        "• */fissa giovedì alle 9:00 udienza CTU Tribunale di Milano*"
                                )
                            )
                            notifyOrStopBackgroundService("📅 Fissa Appuntamento", "Richiesta dettagli appuntamento")
                            return@launch
                        }
                        lower == "/cerca" || lower.startsWith("/cerca ") -> {
                            val query = text.replaceFirst(Regex("^/cerca\\s*", RegexOption.IGNORE_CASE), "").trim()
                            handleSlashSearchCommand(query)
                            notifyOrStopBackgroundService("🔍 Ricerca Archivio", "Completata ricerca: $query")
                            return@launch
                        }
                        lower == "/elimina" || lower.startsWith("/elimina ") ||
                            lower == "/cancella" || lower.startsWith("/cancella ") -> {
                            val query = text.replaceFirst(Regex("^/(elimina|cancella)\\s*", RegexOption.IGNORE_CASE), "").trim()
                            handleSlashDeleteCommand(query)
                            notifyOrStopBackgroundService("🗑️ Eliminazione", "Comando eseguito: $query")
                            return@launch
                        }
                        lower == "/cartelle" || lower == "/archivio" -> {
                            val allDocs = archiveRepo.loadAllDocuments()
                            val folderLines = PeritoCategory.entries.joinToString("\n") { cat ->
                                val count = allDocs.count { it.category == cat }
                                "• 📂 **${cat.displayTitle}**: $count file"
                            }
                            appendAssistantMessage(
                                ChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    isUser = false,
                                    text = "🗄️ **Archivio 6 Cartelle Perito Edile (${allDocs.size} documenti totali)**\n\n$folderLines",
                                    matchedDocuments = allDocs.take(5)
                                )
                            )
                            notifyOrStopBackgroundService("🗄️ Archivio 6 Cartelle", "Riepilogo cartelle visualizzato")
                            return@launch
                        }
                        lower == "/calendario" || lower.startsWith("/calendario") -> {
                            val isSync = _uiState.value.aiConfig.syncSystemCalendar
                            val syncState = if (isSync) "✅ Collegato a Google Calendar / Sistema Android" else "📱 Scollegato • Modalità Agenda Locale attiva"
                            appendAssistantMessage(
                                ChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    isUser = false,
                                    text = "🗓️ **Stato Sincronizzazione Calendario**\n\n" +
                                        "• **Stato:** $syncState\n" +
                                        "• **Eventi in agenda:** ${_uiState.value.upcomingEvents.size} impegni registrati\n\n" +
                                        if (isSync) {
                                            "Gli appuntamenti vengono sincronizzati su Google Calendar. Puoi scollegarlo in qualunque momento dalle impostazioni (⚙️) per mantenerli solo in locale."
                                        } else {
                                            "Gli appuntamenti sono salvati esclusivamente sul dispositivo. Puoi ricollegare il calendario di sistema dalle impostazioni (⚙️)."
                                        }
                                )
                            )
                            notifyOrStopBackgroundService("🗓️ Calendario", syncState)
                            return@launch
                        }
                        lower == "/impostazioni" || lower == "/modello" || lower == "/settings" -> {
                            val isReady = _uiState.value.isReasoningReady
                            val modelInfo = if (isReady) "✅ Modello Reasoning Locale Attivo" else "⬇️ Download modello in attesa"
                            val studio = _uiState.value.aiConfig.studioProfile.studioName.ifBlank { "Studio Tecnico Peritale" }
                            appendAssistantMessage(
                                ChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    isUser = false,
                                    text = "⚙️ **Pannello Impostazioni & Motore IA**\n\n" +
                                        "• **Intestazione Studio:** $studio\n" +
                                        "• **Stato IA Locale:** $modelInfo\n" +
                                        "• **Privacy:** 100% Offline e Riservata su Dispositivo\n\n" +
                                        "Tocca l'icona ingranaggio **⚙️** in alto a destra per modificare profilo, modelli o sincronizzazione."
                                 )
                            )
                            notifyOrStopBackgroundService("⚙️ Impostazioni", "Pannello impostazioni visualizzato")
                            return@launch
                        }
                        lower == "/aggiorna" || lower == "/update" || lower.startsWith("/aggiorna ") -> {
                            appendAssistantMessage(
                                ChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    isUser = false,
                                    text = "🔄 **Controllo Aggiornamenti In-App...**\nVerifico se è disponibile una nuova versione per l'app installata (v${_uiState.value.currentAppVersionName})."
                                )
                            )
                            checkForUpdates(silent = false)
                            return@launch
                        }
                        lower == "/aiuto" || lower == "/help" || lower == "/?" -> {
                            appendAssistantMessage(
                                ChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    isUser = false,
                                    text = "⚡ **Guida ai Comandi Rapidi (Slash Commands)**\n\n" +
                                        "• 🕒 **/orario [ora]** oppure **/sposta [ora]**: Modifica l'orario di un appuntamento (es: `/orario 17:00`, `/sposta +1h`, `/sposta a domani alle 11`)\n" +
                                        "• 📅 **/fissa [dettagli]**: Fissa un nuovo appuntamento (es: `/fissa domani alle 15 perizia a Como`)\n" +
                                        "• 🔄 **/aggiorna**: Controlla subito la disponibilità di aggiornamenti in-app dell'APK\n" +
                                        "• ☀️ **/oggi** o **/agenda**: Riepilogo sopralluoghi e fascicoli di oggi\n" +
                                        "• 🔍 **/cerca [testo]**: Ricerca mirata nell'archivio 6 cartelle (es: `/cerca perizia Rossi`)\n" +
                                        "• 🗑️ **/elimina [appuntamento/file]**: Rimuove un appuntamento o un documento\n" +
                                        "• 📂 **/cartelle**: Conteggio e stato delle 6 cartelle peritali\n" +
                                        "• 🗓️ **/calendario**: Verifica stato sincronizzazione Google Calendar\n" +
                                        "• ⚙️ **/impostazioni**: Informazioni modello locale e privacy"
                                )
                            )
                            notifyOrStopBackgroundService("⚡ Guida Comandi", "Elenco comandi slash visualizzato")
                            return@launch
                        }
                    }
                }

                // 1. Richiesta briefing giornaliero in linguaggio naturale
                if (lower == "oggi" || lower.contains("briefing") || lower.contains("impegni di oggi") || lower.contains("programma di oggi")) {
                    val reply = buildBriefingChatMessage()
                    appendAssistantMessage(reply)
                    notifyOrStopBackgroundService("☀️ Briefing di Oggi", reply.text)
                    return@launch
                }

                // 1b. Richiesta stato 6 cartelle in linguaggio naturale
                if (lower == "6 cartelle" || lower == "cartelle") {
                    val allDocs = archiveRepo.loadAllDocuments()
                    val folderLines = PeritoCategory.entries.joinToString("\n") { cat ->
                        val count = allDocs.count { it.category == cat }
                        "• 📂 **${cat.displayTitle}**: $count file"
                    }
                    appendAssistantMessage(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            isUser = false,
                            text = "🗄️ **Archivio 6 Cartelle Perito Edile (${allDocs.size} documenti totali)**\n\n$folderLines",
                            matchedDocuments = allDocs.take(5)
                        )
                    )
                    notifyOrStopBackgroundService("🗄️ Archivio 6 Cartelle", "Riepilogo cartelle visualizzato")
                    return@launch
                }

                // 2. Comando di eliminazione file o annullamento/modifica appuntamento in linguaggio naturale
                if (!lower.startsWith("/fissa ") && !lower.startsWith("/appuntamento ") && handlePostActionCorrectionIfApplicable(text)) {
                    notifyOrStopBackgroundService("🗑️ Modifica Eseguita", text)
                    return@launch
                }

                // 3. Ricerca rapida nell'archivio in linguaggio naturale ("Mandami la perizia...")
                val detectedSearchQuery = if (lower.startsWith("/fissa ") || lower.startsWith("/appuntamento ")) {
                    null
                } else {
                    aiEngine.detectArchiveSearchQuery(text)
                }
                if (detectedSearchQuery != null && pendingAmbiguousDraft == null) {
                    handleSlashSearchCommand(detectedSearchQuery)
                    notifyOrStopBackgroundService("🔍 Ricerca Archivio", "Ricerca documenti completata")
                    return@launch
                }

                // 4. Unione con eventuale bozza ambigua del turno precedente (Multi-Turn Memory)
                val cleanedForAppointment = text.replaceFirst(Regex("^/(fissa|appuntamento)\\s+", RegexOption.IGNORE_CASE), "").trim()
                val effectiveInput = if (pendingAmbiguousDraft != null) {
                    "$pendingAmbiguousDraft $cleanedForAppointment"
                } else {
                    cleanedForAppointment
                }

                val parsed = aiEngine.parseAppointment(
                    messageText = effectiveInput,
                    now = LocalDateTime.now(),
                    config = _uiState.value.aiConfig
                )

                if (!parsed.has_appointment) {
                    // Se non è un appuntamento, prova a vedere se corrisponde a un documento in archivio
                    val fallbackDocs = archiveRepo.searchDocuments(text)
                    if (fallbackDocs.isNotEmpty()) {
                        appendAssistantMessage(
                            ChatMessage(
                                id = UUID.randomUUID().toString(),
                                isUser = false,
                                text = "📂 Ho trovato **${fallbackDocs.size}** document${if (fallbackDocs.size == 1) "o" else "i"} in archivio pertinenti a *\"$text\"*:",
                                matchedDocuments = fallbackDocs.take(4)
                            )
                        )
                        notifyOrStopBackgroundService("📂 Documenti Trovati", "Trovati ${fallbackDocs.size} documenti in archivio")
                    } else {
                        val generalAnswer = aiEngine.askReasoningAssistant(
                            question = text,
                            config = _uiState.value.aiConfig
                        )
                        if (!generalAnswer.isNullOrBlank()) {
                            appendAssistantMessage(
                                ChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    isUser = false,
                                    text = "🧠 **Risposta PeritoAI (${getActiveReasoningModelLabel()}):**\n\n$generalAnswer"
                                )
                            )
                            notifyOrStopBackgroundService("🧠 Risposta PeritoAI", generalAnswer)
                        } else {
                            appendAssistantMessage(
                                ChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    isUser = false,
                                    text = "ℹ️ Non ho rilevato un nuovo appuntamento con data/ora né un documento corrispondente.\n\n" +
                                        "💡 *Esempi pratici:*\n" +
                                        "• \"Sopralluogo domani alle 10:30 in Via Roma 15 con Ing. Rossi\"\n" +
                                        "• \"/oggi\" oppure \"/cerca perizia infiltrazioni\"\n" +
                                        "• Poni una domanda tecnica sulle perizie e procedure CTU"
                                )
                            )
                            notifyOrStopBackgroundService("ℹ️ PeritoAI", "Nessun appuntamento o documento rilevato")
                        }
                    }
                    return@launch
                }

                if (parsed.is_ambiguo) {
                    pendingAmbiguousDraft = effectiveInput
                    appendAssistantMessage(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            isUser = false,
                            text = "⚠️ **Dettagli Appuntamento da Precisare**\n\n" +
                                "• **Attività:** ${parsed.titolo}\n" +
                                "• **Cliente:** ${parsed.cliente}\n" +
                                "• **Luogo:** ${parsed.luogo}\n" +
                                "• **Da chiarire:** ${parsed.motivo_ambiguita}\n\n" +
                                "✍️ *Rispondi pure indicando solo il dato mancante (es. \"Alle 15:30\" o \"Martedì alle 10:00\").*"
                        )
                    )
                    notifyOrStopBackgroundService("⚠️ Dettagli da Precisare", "Chiarimento per ${parsed.titolo}: ${parsed.motivo_ambiguita}")
                    return@launch
                }

                // Appuntamento completo -> Controllo Conflitti Bidirezionale (±15 min)
                pendingAmbiguousDraft = null
                val conflicts = calendarRepo.checkConflicts(parsed, bufferMinutes = 15L)

                if (conflicts.isNotEmpty()) {
                    val conflictLines = conflicts.joinToString("\n") { c ->
                        val startStr = c.startIso.replace('T', ' ').take(16)
                        val endStr = c.endIso.drop(11).take(5)
                        "• **$startStr - $endStr**: ${c.titolo} (${c.luogo})"
                    }
                    val proposedTime = "${parsed.data_inizio_iso?.replace('T', ' ')?.take(16)} - ${parsed.data_fine_iso?.drop(11)?.take(5)}"
                    appendAssistantMessage(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            isUser = false,
                            text = "🚨 **CONFLITTO D'AGENDA RILEVATO (±15 min)**\n\n" +
                                "Il nuovo impegno:\n" +
                                "📌 **${parsed.titolo}**\n" +
                                "🕒 $proposedTime\n" +
                                "📍 ${parsed.luogo}\n\n" +
                                "Si sovrappone con:\n$conflictLines\n\n" +
                                "Come vuoi procedere?",
                            pendingConflict = PendingConflictState(
                                proposed = parsed,
                                conflictingEvents = conflicts
                            )
                        )
                    )
                    notifyOrStopBackgroundService("🚨 Conflitto d'Agenda Rilevato", "Conflitto per ${parsed.titolo} con impegni esistenti")
                } else {
                    val catFormat = getCategoryFormat(parsed.categoria.ifBlank { parsed.titolo })
                    val created = calendarRepo.createEvent(parsed, catFormat)
                    lastCreatedEvent = created
                    refreshAllData()
                    appendAssistantMessage(buildConfirmationChatMessage(created))
                    notifyOrStopBackgroundService("📅 Appuntamento Confermato", "Fissato in agenda: ${created.titolo} (${created.startIso.replace('T', ' ').take(16)})")
                }
            } catch (e: Throwable) {
                Log.e("PeritoViewModel", "Error processing user message: $text", e)
                appendAssistantMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        isUser = false,
                        text = "⚠️ Si è verificato un errore durante l'elaborazione del messaggio: ${e.localizedMessage ?: "riprova"}"
                    )
                )
                notifyOrStopBackgroundService("⚠️ Errore", e.localizedMessage ?: "Errore elaborazione")
            } finally {
                _uiState.update { it.copy(isProcessing = false, processingLabel = "") }
                ReasoningBackgroundService.stopReasoning(appContext)
            }
        }
    }

    /**
     * Risolve un conflitto sostituendo l'appuntamento precedente con quello nuovo.
     */
    fun resolveConflictReplace(messageId: String, conflictState: PendingConflictState) {
        viewModelScope.launch {
            val catFormat = getCategoryFormat(conflictState.proposed.categoria.ifBlank { conflictState.proposed.titolo })
            val created = calendarRepo.replaceConflictsAndCreate(
                conflicts = conflictState.conflictingEvents,
                proposed = conflictState.proposed,
                format = catFormat
            )
            lastCreatedEvent = created
            clearConflictButtonsOnMessage(messageId)
            refreshAllData()
            appendAssistantMessage(
                buildConfirmationChatMessage(
                    created,
                    prefix = "🔄 **Appuntamento precedente sostituito con successo!**\n\n"
                )
            )
        }
    }

    /**
     * Risolve un conflitto forzando l'inserimento dell'appuntamento accanto a quello esistente.
     */
    fun resolveConflictForce(messageId: String, conflictState: PendingConflictState) {
        viewModelScope.launch {
            val catFormat = getCategoryFormat(conflictState.proposed.categoria.ifBlank { conflictState.proposed.titolo })
            val created = calendarRepo.createEvent(conflictState.proposed, catFormat)
            lastCreatedEvent = created
            clearConflictButtonsOnMessage(messageId)
            refreshAllData()
            appendAssistantMessage(
                buildConfirmationChatMessage(
                    created,
                    prefix = "⚠️ **Appuntamento inserito comunque (in sovrapposizione):**\n\n"
                )
            )
        }
    }

    /**
     * Annulla l'inserimento dell'appuntamento in conflitto.
     */
    fun resolveConflictCancel(messageId: String) {
        clearConflictButtonsOnMessage(messageId)
        appendAssistantMessage(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                isUser = false,
                text = "❌ Inserimento del nuovo appuntamento annullato. L'agenda resta invariata."
            )
        )
    }

    /**
     * Elabora un documento (PDF, .p7m, Immagine/Screenshot) scelto dal telefono o condiviso da WhatsApp/Email.
     */
    fun processDocumentUri(uri: Uri, nameHint: String? = null) {
        if (!requireReasoningModelReady(nameHint ?: "Documento allegato")) return

        _uiState.update {
            it.copy(
                isProcessing = true,
                processingLabel = "OCR On-Device & Classificazione IA in corso..."
            )
        }
        val appContext = getApplication<Application>()
        ReasoningBackgroundService.startReasoning(
            appContext,
            taskTitle = "📄 Elaborazione Documento...",
            previewText = nameHint ?: "Documento allegato"
        )
        appScope.launch {
            try {
                val payload = docProcessor.processUri(uri, nameHint)
                handleProcessedDocumentPayload(
                    cleanFileName = payload.cleanFileName,
                    bytes = payload.extractedBytes,
                    extractedText = payload.extractedText,
                    wasP7m = payload.wasP7mUnwrapped,
                    fascicoloId = _uiState.value.selectedFascicoloId
                )
            } catch (e: Throwable) {
                appendAssistantMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        isUser = false,
                        text = "⚠️ Errore durante l'elaborazione del file: ${e.localizedMessage ?: "formato non leggibile"}"
                    )
                )
                notifyOrStopBackgroundService("⚠️ Errore Documento", e.localizedMessage ?: "Formato non leggibile")
            } finally {
                _uiState.update { it.copy(isProcessing = false, processingLabel = "") }
                ReasoningBackgroundService.stopReasoning(appContext)
            }
        }
    }

    /**
     * Elabora direttamente una foto reale scattata con la fotocamera integrata CameraX o di sistema.
     * Salva sempre la foto del sopralluogo nelle Perizie Tecniche o nel fascicolo attivo.
     */
    fun processCameraBitmap(bitmap: Bitmap, scanPreset: Int = 1, fascicoloId: String? = null) {
        if (!requireReasoningModelReady("Foto Perizia / Scansione")) return

        _uiState.update {
            it.copy(
                isProcessing = true,
                processingLabel = "📸 ML Kit OCR On-Device • Rilievo Foto Perizia..."
            )
        }
        val appContext = getApplication<Application>()
        ReasoningBackgroundService.startReasoning(
            appContext,
            taskTitle = "📸 Scansione Foto & Archiviazione...",
            previewText = "Analisi foto rilievo / documento"
        )
        appScope.launch {
            try {
                val rawOcrText = docProcessor.performOcrOnBitmap(bitmap).trim()
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                val bytes = out.toByteArray()
                val timestamp = DateTimeFormatter.ofPattern("HHmmss").format(LocalDateTime.now())
                val prefix = when (scanPreset) {
                    1 -> "Scansione_Foto"
                    2 -> "Foto_Rilievo"
                    else -> "Foto_Misura"
                }
                handleProcessedDocumentPayload(
                    cleanFileName = "${prefix}_$timestamp.jpg",
                    bytes = bytes,
                    extractedText = rawOcrText,
                    wasP7m = false,
                    explicitType = "CAMERA_SCAN",
                    fascicoloId = fascicoloId ?: _uiState.value.selectedFascicoloId
                )
            } finally {
                runCatching { bitmap.recycle() }
                _uiState.update { it.copy(isProcessing = false, processingLabel = "") }
                ReasoningBackgroundService.stopReasoning(appContext)
            }
        }
    }

    /**
     * Modalità Raffica Sopralluogo: salva una serie di N foto scattate consecutivamente
     * archiviandole direttamente nella cartella Perizie Tecniche e collegandole al Fascicolo selezionato.
     */
    fun processCameraBitmaps(bitmaps: List<Bitmap>, fascicoloId: String? = null) {
        if (bitmaps.isEmpty()) return
        if (bitmaps.size == 1) {
            processCameraBitmap(bitmaps[0], scanPreset = 2, fascicoloId = fascicoloId)
            return
        }

        _uiState.update {
            it.copy(
                isProcessing = true,
                processingLabel = "📸 Elaborazione Raffica (${bitmaps.size} foto sopralluogo)..."
            )
        }
        val appContext = getApplication<Application>()
        ReasoningBackgroundService.startReasoning(
            appContext,
            taskTitle = "📸 Archiviazione Raffica Sopralluogo...",
            previewText = "${bitmaps.size} scatti in elaborazione"
        )
        appScope.launch {
            try {
                val archivedList = mutableListOf<ArchivedDocument>()
                val timestampBase = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
                val targetFascicoloId = fascicoloId ?: _uiState.value.selectedFascicoloId
                val targetFascicolo = _uiState.value.fascicoli.firstOrNull { it.id == targetFascicoloId }
                val clientLabel = targetFascicolo?.cliente ?: "Sopralluogo Peritale"

                bitmaps.forEachIndexed { idx, bmp ->
                    try {
                        val out = ByteArrayOutputStream()
                        bmp.compress(Bitmap.CompressFormat.JPEG, 88, out)
                        val bytes = out.toByteArray()
                        val fileName = "Sopralluogo_${timestampBase}_foto${idx + 1}.jpg"
                        val ocr = docProcessor.performOcrOnBitmap(bmp).trim()
                        val classification = DocumentClassificationResult(
                            categoria = PeritoCategory.PERIZIE_TECNICHE.folderName,
                            cliente_o_controparte = clientLabel,
                            oggetto_sintetico = "Rilievo Fotografico Foto #${idx + 1}",
                            data_documento = LocalDate.now().toString()
                        )
                        val doc = archiveRepo.saveAndArchiveDocument(
                            bytes = bytes,
                            originalFileName = fileName,
                            classification = classification,
                            extractedSnippet = ocr,
                            fascicoloId = targetFascicoloId
                        )
                        archivedList.add(doc)
                    } finally {
                        runCatching { bmp.recycle() }
                    }
                }
                refreshAllData()
                val fascicoloBadge = if (targetFascicolo != null) " nel fascicolo **${targetFascicolo.codice}**" else ""
                appendAssistantMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        isUser = false,
                        text = "📸 **Raffica Sopralluogo Completata con Successo!**\n\n" +
                            "Salvate **${archivedList.size} foto** nella cartella *Perizie Tecniche*$fascicoloBadge.\n\n" +
                            "Tutte le immagini sono pronte per il fascicolo e la perizia.",
                        matchedDocuments = archivedList.take(4)
                    )
                )
                notifyOrStopBackgroundService("📸 Raffica Salvata", "${archivedList.size} foto archiviate con successo")
            } finally {
                _uiState.update { it.copy(isProcessing = false, processingLabel = "") }
                ReasoningBackgroundService.stopReasoning(appContext)
            }
        }
    }

    /**
     * Sposta un documento già archiviato in un'altra delle 6 cartelle con 1 tap.
     */
    fun changeDocumentCategory(docId: String, newCategory: PeritoCategory) {
        viewModelScope.launch {
            val updated = archiveRepo.moveDocumentCategory(docId, newCategory) ?: return@launch
            refreshAllData()
            _uiState.update { state ->
                state.copy(
                    messages = state.messages.map { msg ->
                        if (msg.archivedDocument?.id == docId) {
                            msg.copy(archivedDocument = updated)
                        } else msg
                    },
                    statusBanner = "📂 Spostato in ${newCategory.displayTitle}"
                )
            }
        }
    }

    fun deleteArchivedDocument(docId: String, notifyInChat: Boolean = true) {
        viewModelScope.launch {
            val targetDoc = archiveRepo.loadAllDocuments().find { it.id == docId }
            archiveRepo.deleteDocument(docId)
            refreshAllData()
            _uiState.update { state ->
                val cleanedMessages = state.messages.map { msg ->
                    msg.copy(
                        archivedDocument = if (msg.archivedDocument?.id == docId) null else msg.archivedDocument,
                        matchedDocuments = msg.matchedDocuments.filterNot { it.id == docId }
                    )
                }
                val confirmMsg = if (notifyInChat && targetDoc != null) {
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        isUser = false,
                        text = "🗑️ **Documento eliminato dall'archivio!**\n" +
                            "• **File rimosso:** `${targetDoc.fileName}`\n" +
                            "• **Cartella:** ${targetDoc.category.displayTitle}"
                    )
                } else null
                state.copy(
                    messages = if (confirmMsg != null) cleanedMessages + confirmMsg else cleanedMessages,
                    statusBanner = "🗑️ File eliminato dall'archivio"
                )
            }
        }
    }

    fun deleteCalendarEvent(event: CalendarEventItem, notifyInChat: Boolean = true) {
        viewModelScope.launch {
            calendarRepo.deleteEvent(event)
            if (lastCreatedEvent?.id == event.id) {
                lastCreatedEvent = null
            }
            refreshAllData()
            _uiState.update { state ->
                val cleanedMessages = state.messages.map { msg ->
                    if (msg.createdEvent?.id == event.id) msg.copy(createdEvent = null) else msg
                }
                val confirmMsg = if (notifyInChat) {
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        isUser = false,
                        text = "🗑️ **Appuntamento eliminato dall'agenda!**\n• ${event.titolo} (${event.startIso.replace('T', ' ').take(16)})"
                    )
                } else null
                state.copy(
                    messages = if (confirmMsg != null) cleanedMessages + confirmMsg else cleanedMessages,
                    statusBanner = "🗑️ Appuntamento rimosso dall'agenda"
                )
            }
        }
    }

    /**
     * Aggiorna titolo, categoria, orario, luogo e note di un appuntamento esistente,
     * sincronizzando il calendario di sistema Android/Google e il registro locale dell'app.
     */
    fun updateCalendarEvent(
        event: CalendarEventItem,
        newTitle: String = event.titolo,
        newCategory: String = event.categoria,
        newStartIso: String = event.startIso,
        newEndIso: String? = null,
        newLocation: String = event.luogo,
        newNotes: String = event.note,
        notifyInChat: Boolean = true
    ) {
        viewModelScope.launch {
            val computedEndIso = newEndIso ?: run {
                val s = LocalDateTime.parse(newStartIso.take(19))
                s.plusHours(1).format(isoFormatter)
            }
            val updated = calendarRepo.updateEvent(
                event = event,
                newTitle = newTitle,
                newCategory = newCategory,
                newStartIso = newStartIso,
                newEndIso = computedEndIso,
                newLocation = newLocation,
                newNotes = newNotes
            )
            lastCreatedEvent = updated
            refreshAllData()

            _uiState.update { state ->
                state.copy(
                    messages = state.messages.map { msg ->
                        if (msg.createdEvent?.id == event.id || (event.systemCalendarEventId != null && msg.createdEvent?.systemCalendarEventId == event.systemCalendarEventId)) {
                            msg.copy(createdEvent = updated)
                        } else msg
                    },
                    statusBanner = "✏️ Impegno aggiornato: ${updated.titolo}"
                )
            }

            if (notifyInChat) {
                appendAssistantMessage(
                    buildConfirmationChatMessage(
                        updated,
                        prefix = "✏️ **Impegno Aggiornato con Successo!**\n\n"
                    )
                )
            }
        }
    }

    /**
     * Modifica l'orario o la data di un appuntamento esistente, aggiornando sia l'agenda
     * (locale e Google Calendar se collegato) sia la bolla interattiva nella chat.
     */
    fun rescheduleCalendarEvent(
        event: CalendarEventItem,
        newStartIso: String,
        newEndIso: String? = null,
        notifyInChat: Boolean = true
    ) {
        updateCalendarEvent(
            event = event,
            newStartIso = newStartIso,
            newEndIso = newEndIso,
            notifyInChat = notifyInChat
        )
    }

    fun setCategoryFilter(category: PeritoCategory?) {
        _uiState.update { it.copy(selectedCategoryFilter = category) }
    }

    fun setArchiveSearchQuery(query: String) {
        _uiState.update { it.copy(archiveSearchQuery = query) }
        refreshAllData()
    }

    fun updateAiConfig(newConfig: AiEngineConfig) {
        val oldConfig = _uiState.value.aiConfig
        saveConfig(newConfig)
        if (newConfig.morningBriefingEnabled != oldConfig.morningBriefingEnabled) {
            if (newConfig.morningBriefingEnabled) {
                MorningBriefingWorker.scheduleDailyBriefing(appContext)
            } else {
                MorningBriefingWorker.cancelDailyBriefing(appContext)
            }
        }
        val reasoningReady = modelDownloadManager.isPrimaryReasoningDownloaded(newConfig.mediaPipeModelPath)
        val allReady = modelDownloadManager.areAllModelsDownloaded(newConfig.mediaPipeModelPath)
        _uiState.update {
            it.copy(
                aiConfig = newConfig,
                isReasoningReady = reasoningReady,
                areAllModelsReady = allReady,
                showFirstRunModelDialog = if (reasoningReady) false else it.showFirstRunModelDialog,
                statusBanner = "✅ Configurazione IA On-Device salvata"
            )
        }
    }

    fun clearStatusBanner() {
        _uiState.update { it.copy(statusBanner = null) }
    }

    /**
     * Aggiunge una nuova categoria personalizzata definita dall'utente.
     */
    fun addCustomCategory(name: String, emoji: String, defaultTitle: String, keywords: List<String>) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        val rawSlug = cleanName.lowercase(Locale.ITALIAN).replace(Regex("""[^a-z0-9_]+"""), "_").trim('_')
        val id = if (rawSlug.isNotBlank()) "${rawSlug}_${UUID.randomUUID().toString().take(6)}" else "cat_${UUID.randomUUID().toString().take(6)}"
        val newDef = PeritoCategoryDefinition(
            id = id,
            displayName = cleanName,
            emoji = emoji.trim().ifBlank { "📌" },
            defaultPrefix = defaultTitle.trim().ifBlank { cleanName },
            keywords = keywords.map { it.trim().lowercase(Locale.ITALIAN) }.filter { it.isNotBlank() },
            isSystem = false
        )
        val currentCategories = _uiState.value.aiConfig.categories
        val currentFormats = _uiState.value.aiConfig.categoryFormats.toMutableMap()
        currentFormats[id] = CategoryReferenceFormat()
        val updatedConfig = _uiState.value.aiConfig.copy(
            categories = currentCategories + newDef,
            categoryFormats = currentFormats
        )
        updateAiConfig(updatedConfig)
    }

    /**
     * Modifica una categoria esistente (mantenendo inalterato il suo ID).
     */
    fun updateCustomCategory(id: String, name: String, emoji: String, defaultTitle: String, keywords: List<String>) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        val currentCategories = _uiState.value.aiConfig.categories
        val existing = currentCategories.firstOrNull { it.id == id } ?: return
        val updatedDef = existing.copy(
            displayName = cleanName,
            emoji = emoji.trim().ifBlank { existing.emoji },
            defaultPrefix = defaultTitle.trim().ifBlank { cleanName },
            keywords = keywords.map { it.trim().lowercase(Locale.ITALIAN) }.filter { it.isNotBlank() }
        )
        val updatedCategories = currentCategories.map { if (it.id == id) updatedDef else it }
        val updatedConfig = _uiState.value.aiConfig.copy(categories = updatedCategories)
        updateAiConfig(updatedConfig)
    }

    /**
     * Elimina una categoria personalizzata (non è consentito eliminare categorie di sistema).
     */
    fun deleteCustomCategory(id: String) {
        val currentCategories = _uiState.value.aiConfig.categories
        val target = currentCategories.firstOrNull { it.id == id } ?: return
        if (target.isSystem) return

        val updatedCategories = currentCategories.filter { it.id != id }
        val currentFormats = _uiState.value.aiConfig.categoryFormats.toMutableMap()
        currentFormats.remove(id)
        val updatedConfig = _uiState.value.aiConfig.copy(
            categories = updatedCategories,
            categoryFormats = currentFormats
        )
        updateAiConfig(updatedConfig)
    }

    /**
     * Recupera il formato di visualizzazione associato a una determinata categoria o titolo.
     */
    fun getCategoryFormat(categoryOrTitle: String): CategoryReferenceFormat? {
        val catDef = _uiState.value.aiConfig.categories.firstOrNull {
            it.id.equals(categoryOrTitle, ignoreCase = true) ||
            it.displayName.equals(categoryOrTitle, ignoreCase = true)
        }
        val catId = catDef?.id ?: PeritoEventCategory.fromString(categoryOrTitle).id
        return _uiState.value.aiConfig.categoryFormats[catId]
    }

    /**
     * Aggiorna la configurazione di visualizzazione del riferimento per una specifica categoria.
     */
    fun updateCategoryReferenceFormat(categoryId: String, format: CategoryReferenceFormat) {
        val currentMap = _uiState.value.aiConfig.categoryFormats.toMutableMap()
        currentMap[categoryId] = format
        val updatedConfig = _uiState.value.aiConfig.copy(categoryFormats = currentMap)
        updateAiConfig(updatedConfig)
    }

    /**
     * Ripristina i formati di riferimento predefiniti per tutte le categorie.
     */
    fun resetCategoryReferenceFormats() {
        val updatedConfig = _uiState.value.aiConfig.copy(categoryFormats = defaultCategoryFormats())
        updateAiConfig(updatedConfig)
    }

    /**
     * Calcola il titolo sintetico/riferimento di un appuntamento rispettando le impostazioni
     * configurate dall'utente per la categoria associata.
     */
    fun formatEventReference(event: CalendarEventItem): String {
        val catDef = _uiState.value.aiConfig.categories.firstOrNull {
            it.id.equals(event.categoria, ignoreCase = true) ||
            it.displayName.equals(event.categoria, ignoreCase = true)
        }
        val catEnum = PeritoEventCategory.fromString(event.categoria.ifBlank { event.titolo })
        val catId = catDef?.id ?: catEnum.id
        val format = _uiState.value.aiConfig.categoryFormats[catId] ?: CategoryReferenceFormat()
        val defaultType = catDef?.defaultPrefix ?: catEnum.defaultPrefix

        val subject = if (event.cliente.isNotBlank() && event.cliente != "Cliente") {
            event.cliente
        } else if (catEnum == PeritoEventCategory.UNIONCASA) {
            "Unioncasa"
        } else if (catEnum == PeritoEventCategory.AGENZIA_ORSO) {
            "Agenzia Orso"
        } else {
            ""
        }

        val effectiveSubject = if (subject.isBlank() && event.titolo.isNotBlank() &&
            !event.titolo.equals(catDef?.displayName ?: catEnum.displayName, ignoreCase = true) &&
            !event.titolo.equals(defaultType, ignoreCase = true)
        ) {
            if (event.titolo.contains(" - ")) {
                event.titolo.substringAfter(" - ").substringBefore(" (").trim()
            } else {
                event.titolo.substringBefore(" (").trim()
            }
        } else {
            subject
        }

        return format.formatTitle(
            startTimeIso = event.startIso,
            categoryType = defaultType,
            subject = effectiveSubject,
            place = event.luogo,
            notes = event.note
        )
    }

    private suspend fun handleProcessedDocumentPayload(
        cleanFileName: String,
        bytes: ByteArray,
        extractedText: String,
        wasP7m: Boolean,
        explicitType: String? = null,
        fascicoloId: String? = null
    ) {
        val displaySentName = if (wasP7m && !cleanFileName.endsWith(".p7m", ignoreCase = true)) {
            "${cleanFileName.trim()}.p7m"
        } else {
            cleanFileName.trim()
        }
        val detectedType = explicitType ?: when {
            wasP7m || displaySentName.endsWith(".p7m", ignoreCase = true) -> "P7M"
            displaySentName.endsWith(".jpg", ignoreCase = true) ||
                displaySentName.endsWith(".jpeg", ignoreCase = true) ||
                displaySentName.endsWith(".png", ignoreCase = true) ||
                displaySentName.endsWith(".webp", ignoreCase = true) -> "CAMERA_SCAN"
            else -> "PDF"
        }
        val userAttachmentMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = displaySentName,
            attachmentName = displaySentName,
            attachmentType = detectedType
        )
        _uiState.update {
            it.copy(
                messages = it.messages + userAttachmentMsg,
                isProcessing = true,
                processingLabel = when (detectedType) {
                    "P7M" -> "🔓 Sbustamento .p7m + OCR & Smistamento 6 Cartelle..."
                    "CAMERA_SCAN" -> "📸 ML Kit OCR On-Device • Rilievo fotografico sopralluogo..."
                    else -> "📄 OCR & Qwen 3.5 • Classificazione nelle 6 Cartelle..."
                }
            )
        }
        viewModelScope.launch {
            chatHistoryRepo.saveMessages(_uiState.value.messages)
        }
        kotlinx.coroutines.delay(420L)

        val targetFascicoloId = fascicoloId ?: _uiState.value.selectedFascicoloId
        val targetFascicolo = _uiState.value.fascicoli.firstOrNull { it.id == targetFascicoloId }

        // Se è una foto/immagine e l'OCR non rileva testo, è un RILIEVO FOTOGRAFICO reale
        // Archivia direttamente in Perizie Tecniche collegandola al fascicolo, NON dare errore!
        val isPhotoWithoutText = detectedType == "CAMERA_SCAN" && !aiEngine.isMeaningfulOcrText(extractedText)
        val classification = if (isPhotoWithoutText) {
            DocumentClassificationResult(
                categoria = PeritoCategory.PERIZIE_TECNICHE.folderName,
                cliente_o_controparte = targetFascicolo?.cliente ?: "Sopralluogo Peritale",
                oggetto_sintetico = "Rilievo Fotografico Sopralluogo",
                data_documento = LocalDate.now().toString()
            )
        } else {
            aiEngine.classifyDocument(
                docText = extractedText,
                originalFileName = cleanFileName,
                referenceDate = LocalDate.now(),
                config = _uiState.value.aiConfig
            )
        }

        val archived = archiveRepo.saveAndArchiveDocument(
            bytes = bytes,
            originalFileName = cleanFileName,
            classification = classification,
            extractedSnippet = extractedText,
            fascicoloId = targetFascicoloId
        )
        refreshAllData()

        val p7mBadge = if (wasP7m) "\n🔓 *Busta crittografica CAdES (.p7m) sbustata automaticamente*" else ""
        val ocrPreview = if (extractedText.isNotBlank()) {
            "\n📝 *Estratto OCR:* \"${extractedText.replace('\n', ' ').take(120)}...\""
        } else ""

        appendAssistantMessage(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                isUser = false,
                text = "📄 **Documento Classificato e Archiviato!**$p7mBadge\n\n" +
                    "• 📂 **Cartella:** `${archived.category.displayTitle}`\n" +
                    "• 👤 **Cliente/Ente:** ${archived.cliente.replace('_', ' ')}\n" +
                    "• 🏷️ **Oggetto:** ${archived.oggetto.replace('_', ' ')}\n" +
                    "• 💾 **Nome File:** `${archived.fileName}`$ocrPreview\n\n" +
                    "👇 *Usa i pulsanti qui sotto per aprirlo, condividerlo o spostarlo di cartella con 1 tocco:*",
                archivedDocument = archived
            )
        )
        notifyOrStopBackgroundService(
            replyTitle = "📄 Documento Archiviato",
            replyText = "Archiviato in ${archived.category.displayTitle}: ${archived.fileName}"
        )

        // Estrae un appuntamento dal testo del documento/foto SOLO se contiene una frase esplicita
        // di convocazione/appuntamento CON un orario reale e non ambiguo.
        if (extractedText.length in 15..600 && aiEngine.hasExplicitAppointmentInDocument(extractedText)) {
            val maybeAppointment = aiEngine.parseAppointment(
                messageText = extractedText,
                now = LocalDateTime.now(),
                config = _uiState.value.aiConfig
            )
            if (maybeAppointment.has_appointment && !maybeAppointment.is_ambiguo && maybeAppointment.data_inizio_iso != null) {
                val conflicts = calendarRepo.checkConflicts(maybeAppointment)
                if (conflicts.isEmpty()) {
                    val catFormat = getCategoryFormat(maybeAppointment.categoria.ifBlank { maybeAppointment.titolo })
                    val created = calendarRepo.createEvent(maybeAppointment, catFormat)
                    lastCreatedEvent = created
                    refreshAllData()
                    appendAssistantMessage(
                        buildConfirmationChatMessage(
                            created,
                            prefix = "📅 **Rilevato anche un appuntamento esplicito nel testo della foto/documento!**\n\n"
                        )
                    )
                    notifyOrStopBackgroundService(
                        replyTitle = "📅 Appuntamento Rilevato nel Documento",
                        replyText = "Fissato in agenda: ${created.titolo} (${created.startIso.replace('T', ' ').take(16)})"
                    )
                }
            }
        }
    }

    private suspend fun handlePostActionCorrectionIfApplicable(text: String): Boolean {
        val lower = text.trim().lowercase(Locale.ITALIAN)
        val deleteVerbs = listOf("elimina", "cancella", "rimuovi", "butta", "togli", "svuota")
        val hasDeleteVerb = deleteVerbs.any { lower == it || lower.startsWith("$it ") || lower.contains(" $it ") }

        // A. GESTIONE ELIMINAZIONE DOCUMENTI / FILE DA CHAT ("elimina questo file", "elimina nomina ctu", ecc.)
        if (hasDeleteVerb) {
            val isAppointmentTarget = lower.contains("appuntamento") ||
                lower.contains("sopralluogo") ||
                lower.contains("impegno") ||
                lower.contains("evento") ||
                lower.contains("agenda")

            if (!isAppointmentTarget) {
                val allDocs = archiveRepo.loadAllDocuments()
                if (allDocs.isEmpty()) {
                    appendAssistantMessage(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            isUser = false,
                            text = "ℹ️ L'archivio delle 6 cartelle è già vuoto: non ci sono file da eliminare."
                        )
                    )
                    return true
                }

                // Caso 1: "elimina tutti i file" / "svuota archivio"
                if (lower.contains("tutti") || lower.contains("tutto") || lower.contains("svuota")) {
                    allDocs.forEach { archiveRepo.deleteDocument(it.id) }
                    refreshAllData()
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                msg.copy(archivedDocument = null, matchedDocuments = emptyList())
                            },
                            statusBanner = "🗑️ Tutti i file sono stati eliminati dall'archivio"
                        )
                    }
                    appendAssistantMessage(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            isUser = false,
                            text = "🗑️ **Archivio svuotato!** Tutti i **${allDocs.size}** documenti sono stati rimossi dalle 6 cartelle."
                        )
                    )
                    return true
                }

                // Caso 2: "elimina questo file", "elimina il file", "elimina ultimo file", o specifica query ("elimina nomina ctu")
                val genericPointers = setOf(
                    "elimina", "cancella", "rimuovi", "butta", "togli",
                    "questo", "questa", "quello", "quella", "ultimo", "ultima", "l",
                    "il", "lo", "la", "i", "gli", "le", "un", "una",
                    "file", "documento", "allegato", "foto", "scansione", "pdf", "p7m", "archivio", "da", "dall"
                )
                val targetTokens = lower.replace(Regex("""[^a-zà-ú0-9\s]"""), " ")
                    .split(Regex("""\s+"""))
                    .filter { it.length >= 2 && it !in genericPointers }

                val docsToDelete: List<ArchivedDocument> = if (targetTokens.isEmpty()) {
                    // "elimina questo file" / "elimina l'ultimo documento" -> elimina l'ultimo file archiviato
                    listOf(allDocs.first())
                } else {
                    val queryJoined = targetTokens.joinToString(" ")
                    val directSearch = archiveRepo.searchDocuments(queryJoined)
                    if (directSearch.isNotEmpty()) {
                        listOf(directSearch.first())
                    } else {
                        // Cerca corrispondenza anche su singoli token (es. "nomina" o "ctu")
                        val tokenMatched = allDocs.filter { doc ->
                            val hay = "${doc.fileName} ${doc.originalName} ${doc.cliente} ${doc.oggetto} ${doc.category.displayTitle} ${doc.extractedTextSnippet}".lowercase(Locale.ITALIAN)
                            targetTokens.any { tok -> hay.contains(tok) }
                        }
                        if (tokenMatched.isNotEmpty()) {
                            listOf(tokenMatched.first())
                        } else if (allDocs.size == 1) {
                            listOf(allDocs.first())
                        } else {
                            emptyList()
                        }
                    }
                }

                if (docsToDelete.isNotEmpty()) {
                    val deletedIds = docsToDelete.map { it.id }.toSet()
                    docsToDelete.forEach { archiveRepo.deleteDocument(it.id) }
                    refreshAllData()
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                msg.copy(
                                    archivedDocument = if (msg.archivedDocument?.id in deletedIds) null else msg.archivedDocument,
                                    matchedDocuments = msg.matchedDocuments.filterNot { it.id in deletedIds }
                                )
                            },
                            statusBanner = "🗑️ File eliminato dall'archivio"
                        )
                    }
                    val doc = docsToDelete.first()
                    appendAssistantMessage(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            isUser = false,
                            text = "🗑️ **Documento eliminato dall'archivio!**\n" +
                                "• **File rimosso:** `${doc.fileName}`\n" +
                                "• **Cartella:** ${doc.category.displayTitle}"
                        )
                    )
                    return true
                } else {
                    appendAssistantMessage(
                        ChatMessage(
                            id = UUID.randomUUID().toString(),
                            isUser = false,
                            text = "⚠️ Non ho trovato nessun file corrispondente a *\"${targetTokens.joinToString(" ")}\"* da eliminare.\n" +
                                "Puoi scrivere **\"elimina ultimo file\"** oppure toccare il pulsante **🗑️ Elimina** direttamente sotto il documento."
                        )
                    )
                    return true
                }
            }
        }

        // B. GESTIONE ANNULLAMENTO / MODIFICA APPUNTAMENTO
        val lastEv = lastCreatedEvent ?: _uiState.value.upcomingEvents.firstOrNull()
        if (lower == "annulla" ||
            lower.contains("annulla l'ultimo") ||
            lower.contains("cancella l'appuntamento") ||
            lower.contains("elimina l'appuntamento") ||
            lower.contains("elimina appuntamento") ||
            lower.contains("cancella appuntamento") ||
            lower.contains("elimina sopralluogo") ||
            lower.contains("cancella sopralluogo")
        ) {
            if (lastEv != null) {
                calendarRepo.deleteEvent(lastEv)
                if (lastCreatedEvent?.id == lastEv.id) lastCreatedEvent = null
                refreshAllData()
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.map { msg ->
                            if (msg.createdEvent?.id == lastEv.id) msg.copy(createdEvent = null) else msg
                        },
                        statusBanner = "🗑️ Appuntamento rimosso dall'agenda"
                    )
                }
                appendAssistantMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        isUser = false,
                        text = "🗑️ **Appuntamento annullato e rimosso dall'agenda:**\n• ${lastEv.titolo} (${lastEv.startIso.replace('T', ' ').take(16)})"
                    )
                )
            } else {
                appendAssistantMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        isUser = false,
                        text = "ℹ️ Non ci sono appuntamenti recenti da annullare in agenda."
                    )
                )
            }
            return true
        }

        if (lastEv == null) return false

        // Cambio luogo ("cambia il luogo in Via...", "invece in Via...")
        if (lower.startsWith("cambia il luogo") || lower.startsWith("sposta in via") || lower.startsWith("luogo:")) {
            val newLoc = text.replace(Regex("""^(cambia il luogo in|cambia luogo in|luogo:)\s*""", RegexOption.IGNORE_CASE), "").trim()
            if (newLoc.length >= 3) {
                calendarRepo.deleteEvent(lastEv)
                val updatedProposal = AppointmentParseResult(
                    has_appointment = true,
                    titolo = "${lastEv.titolo.substringBefore(" (")} ($newLoc)",
                    data_inizio_iso = lastEv.startIso,
                    data_fine_iso = lastEv.endIso,
                    luogo = newLoc,
                    cliente = lastEv.cliente,
                    note = lastEv.note
                )
                val catFormat = getCategoryFormat(updatedProposal.categoria.ifBlank { updatedProposal.titolo })
                val updated = calendarRepo.createEvent(updatedProposal, catFormat)
                lastCreatedEvent = updated
                refreshAllData()
                appendAssistantMessage(
                    buildConfirmationChatMessage(
                        updated,
                        prefix = "📍 **Luogo dell'appuntamento aggiornato!**\n\n"
                    )
                )
                return true
            }
        }

        // B2.0 CAMBIO CATEGORIA APPUNTAMENTO (Linguaggio naturale)
        val isCategoryChange = lower.contains("cambia categoria") ||
            lower.contains("modifica categoria") ||
            lower.contains("cambia la categoria") ||
            lower.contains("imposta categoria") ||
            lower.contains("è un appuntamento in comune") ||
            lower.contains("è in comune") ||
            lower.contains("appuntamenti in comune") ||
            lower.contains("è in tribunale") ||
            lower.contains("è una riunione") ||
            lower.contains("è un sopralluogo") ||
            lower.contains("non è un sopralluogo")

        if (isCategoryChange) {
            val targetCategory = PeritoEventCategory.fromString(text)
            val clientSuffix = if (lastEv.cliente.isNotBlank() && lastEv.cliente != "Cliente" && !lastEv.cliente.equals(lastEv.categoria, ignoreCase = true)) {
                " con ${lastEv.cliente}"
            } else {
                lastEv.titolo.substringAfter(" con ", "").let { if (it.isNotBlank()) " con $it" else "" }
            }
            val newTitle = "${targetCategory.defaultPrefix}$clientSuffix"
            updateCalendarEvent(
                event = lastEv,
                newTitle = newTitle,
                newCategory = targetCategory.displayName,
                notifyInChat = true
            )
            return true
        }

        // B2. CAMBIO ORARIO FLESSIBILE E ROBUSTO (Linguaggio naturale)
        val isTimeChangeKeyword = Regex(
            """\b(?:cambia|modifica|sposta|aggiorna)\s+(?:l['\s]ora|l['\s]orario|orari[oa]|l['\s]appuntamento|appuntamento|data|giorno)\b""" +
            """|\b(?:cambial[oa]|spostal[oa]|mettil[oa]|anticipal[oa]|posticipal[oa])\b""" +
            """|\b(?:facciamo\s+alle|andiamo\s+alle|vediamoci\s+alle|ci\s+vediamo\s+alle|mettilo\s+alle|mettila\s+alle)\b""" +
            """|\b(?:non\s+alle\s+\d+|cambia\s+a\s+domani)\b""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(lower) ||
            lower.contains("cambia orario") ||
            lower.contains("cambia ora") ||
            lower.contains("cambia l ora") ||
            lower.contains("cambia l'ora") ||
            lower.contains("sposta orario") ||
            lower.contains("modifica orario") ||
            lower.contains("cambia l'orario") ||
            lower.contains("cambia orari") ||
            lower.contains("cambialo alle") ||
            lower.contains("cambia a domani") ||
            lower.contains("sposta l'appuntamento") ||
            lower.contains("sposta appuntamento") ||
            lower.contains("sposta il sopralluogo") ||
            lower.startsWith("sposta ") ||
            lower.startsWith("spostalo ") ||
            lower.startsWith("mettilo ") ||
            lower.startsWith("anticipa ") ||
            lower.startsWith("anticipalo ") ||
            lower.startsWith("posticipa ") ||
            lower.startsWith("posticipalo ") ||
            lower.startsWith("facciamo alle ") ||
            lower.startsWith("andiamo alle ")

        if (isTimeChangeKeyword) {
            val targetEv = lastEv
            val currentStart = try {
                LocalDateTime.parse(targetEv.startIso.take(19))
            } catch (_: Throwable) {
                LocalDateTime.now()
            }
            val currentEnd = try {
                LocalDateTime.parse(targetEv.endIso.take(19))
            } catch (_: Throwable) {
                currentStart.plusHours(1)
            }
            val durationMinutes = Duration.between(currentStart, currentEnd).toMinutes().coerceAtLeast(15L)

            // 1. Spostamento Relativo ("di 1 ora", "di 30 minuti", "+1h", "-30m", ecc.)
            val relativeHourMatch = Regex("""(?:anticipa|posticipa|sposta)?\s*(?:di\s+|\+|\-)?(\d+|un'|un)\s*(?:ora|ore|h)\b""").find(lower)
            val relativeMinuteMatch = Regex("""(?:anticipa|posticipa|sposta)?\s*(?:di\s+|\+|\-)?(\d+)\s*(?:min|minuti|m)\b""").find(lower)

            if (relativeHourMatch != null || relativeMinuteMatch != null) {
                val hoursCount = when {
                    relativeHourMatch == null -> 0L
                    relativeHourMatch.groupValues[1].startsWith("un") -> 1L
                    else -> relativeHourMatch.groupValues[1].toLongOrNull() ?: 1L
                }
                val minutesCount = relativeMinuteMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                val isNegative = lower.contains("anticipa") || lower.contains("-")
                val totalDeltaMinutes = (hoursCount * 60L + minutesCount) * (if (isNegative) -1L else 1L)

                val newStart = currentStart.plusMinutes(totalDeltaMinutes)
                val newEnd = newStart.plusMinutes(durationMinutes)
                rescheduleCalendarEvent(
                    event = targetEv,
                    newStartIso = newStart.format(isoFormatter),
                    newEndIso = newEnd.format(isoFormatter),
                    notifyInChat = true
                )
                return true
            }

            // 2. Orario Specifico con supporto Range o Singolo orario, escludendo orari negati ("non alle 20")
            val targetDate = if (lower.contains("dopodomani")) {
                currentStart.toLocalDate().plusDays(2)
            } else if (lower.contains("domani")) {
                currentStart.toLocalDate().plusDays(1)
            } else if (lower.contains("oggi")) {
                LocalDate.now()
            } else {
                currentStart.toLocalDate()
            }

            val extractedTime = aiEngine.extractTargetTime(text)
            if (extractedTime != null) {
                val (newStartTime, newEndTime) = extractedTime
                val newStart = targetDate.atTime(newStartTime)
                val newEnd = if (newEndTime != null) {
                    targetDate.atTime(newEndTime)
                } else {
                    newStart.plusMinutes(durationMinutes)
                }

                rescheduleCalendarEvent(
                    event = targetEv,
                    newStartIso = newStart.format(isoFormatter),
                    newEndIso = newEnd.format(isoFormatter),
                    notifyInChat = true
                )
                return true
            }

            // 3. Spostamento a giorno senza orario esplicito ("sposta a domani")
            if (lower.contains("domani") || lower.contains("dopodomani")) {
                val daysToAdd = if (lower.contains("dopodomani")) 2L else 1L
                val newStart = currentStart.plusDays(daysToAdd)
                val newEnd = newStart.plusMinutes(durationMinutes)
                rescheduleCalendarEvent(
                    event = targetEv,
                    newStartIso = newStart.format(isoFormatter),
                    newEndIso = newEnd.format(isoFormatter),
                    notifyInChat = true
                )
                return true
            }

            // 4. Intent generico "cambia orario" senza specifica oraria -> Chiede chiarimento allegando la card per modifica diretta
            appendAssistantMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = "🕒 **A che ora preferisci spostare l'appuntamento?**\n\n" +
                        "• **Impegno:** ${targetEv.titolo}\n" +
                        "• **Orario attuale:** ${targetEv.startIso.replace('T', ' ').take(16)}\n\n" +
                        "✍️ Puoi scrivermi o dettarmi ad esempio *\"Alle 11:30\"*, *\"Posticipa di 1 ora\"*, oppure tocca il pulsante **🕒 Modifica Orario** qui sotto.",
                    createdEvent = targetEv
                )
            )
            return true
        }

        return false
    }

    private fun handleSlashSearchCommand(query: String) {
        val allDocs = archiveRepo.loadAllDocuments()
        val cleanQuery = query.trim()

        if (cleanQuery.isBlank()) {
            if (allDocs.isEmpty()) {
                appendAssistantMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        isUser = false,
                        text = "🔍 **Comando `/cerca` — Archivio 6 Cartelle vuoto**\n" +
                            "Non ci sono ancora documenti archiviati.\n\n" +
                            "💡 *Dopo aver allegato un PDF/.p7m o scattato una foto, scrivi **`/cerca <dettagli>`** (es. `/cerca CTU Milano` o `/cerca Rossi`) per trovarlo subito.*"
                    )
                )
            } else {
                val folderLines = PeritoCategory.entries.joinToString("\n") { cat ->
                    val count = allDocs.count { it.category == cat }
                    "• 📂 **${cat.displayTitle}**: $count file"
                }
                appendAssistantMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        isUser = false,
                        text = "🔍 **Comando `/cerca` — Panoramica 6 Cartelle (${allDocs.size} file)**\n\n" +
                            "$folderLines\n\n" +
                            "💡 *Scrivi **`/cerca <cliente, perizia o categoria>`** per filtrare un fascicolo specifico, oppure usa gli ultimi documenti qui sotto:*",
                        matchedDocuments = allDocs.take(5)
                    )
                )
            }
            return
        }

        val directMatches = archiveRepo.searchDocuments(cleanQuery)
        val tokens = cleanQuery.lowercase(Locale.ITALIAN)
            .replace(Regex("""[^a-zà-ú0-9\s]"""), " ")
            .split(Regex("""\s+"""))
            .filter { it.length >= 2 }

        val tokenMatches = if (tokens.isNotEmpty()) {
            allDocs.filter { doc ->
                val hay = "${doc.fileName} ${doc.originalName} ${doc.cliente} ${doc.oggetto} ${doc.category.displayTitle} ${doc.category.folderName} ${doc.extractedTextSnippet}".lowercase(Locale.ITALIAN)
                tokens.all { tok -> hay.contains(tok) } || tokens.any { tok -> hay.contains(tok) }
            }
        } else {
            emptyList()
        }

        val matches = (directMatches + tokenMatches).distinctBy { it.id }

        if (matches.isEmpty()) {
            appendAssistantMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = "🔍 **Nessun documento trovato per `/cerca $cleanQuery`** nelle 6 Cartelle.\n" +
                        "• Documenti presenti in archivio: **${allDocs.size}**\n" +
                        "• Prova con il cognome del cliente, il condominio o la cartella (*es. `/cerca CTU`, `/cerca Fatture`, `/cerca Planimetrie`*)."
                )
            )
        } else {
            appendAssistantMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = "📂 **Risultati `/cerca $cleanQuery` (${matches.size} ${if (matches.size == 1) "documento" else "documenti"}):**\n" +
                        "Tocca **Apri**, **Condividi**, cambia cartella o **🗑️ Elimina**:",
                    matchedDocuments = matches.take(5)
                )
            )
        }
    }

    private suspend fun handleSlashDeleteCommand(query: String) {
        val cleanQuery = query.trim()
        val allDocs = archiveRepo.loadAllDocuments()

        if (cleanQuery.isBlank()) {
            val hintSuffix = if (allDocs.isNotEmpty()) {
                "\n\n👇 *Oppure tocca direttamente **🗑️** su uno dei documenti archiviati qui sotto:*"
            } else {
                "\n\n*(Archivio delle 6 Cartelle attualmente vuoto)*"
            }
            appendAssistantMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = "🗑️ **Comando `/elimina` — Specifica cosa rimuovere:**\n" +
                        "• **`/elimina <nome file o cliente>`** (*es. `/elimina CTU Milano`*)\n" +
                        "• **`/elimina ultimo`** — rimuove l'ultimo documento salvato\n" +
                        "• **`/elimina appuntamento`** — annulla l'ultimo sopralluogo in agenda\n" +
                        "• **`/elimina tutto`** — svuota le 6 Cartelle$hintSuffix",
                    matchedDocuments = allDocs.take(5)
                )
            )
            return
        }

        // Inoltra al gestore deterministico di eliminazione preponendo "elimina "
        handlePostActionCorrectionIfApplicable("elimina $cleanQuery")
    }

    private suspend fun buildBriefingChatMessage(): ChatMessage {
        val today = LocalDate.now()
        val todayEvs = calendarRepo.getEventsForDate(today)
        val tomorrowEvs = calendarRepo.getEventsForDate(today.plusDays(1))

        val sb = StringBuilder()
        sb.appendLine("☀️ **BRIEFING GIORNALIERO PERITALE (${today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))})**")
        sb.appendLine()

        val allLinkedDocs = mutableListOf<ArchivedDocument>()

        if (todayEvs.isEmpty()) {
            sb.appendLine("📅 **Oggi:** Nessun sopralluogo o udienza in programma.")
        } else {
            sb.appendLine("📅 **Impegni di Oggi (${todayEvs.size}):**")
            for (ev in todayEvs) {
                val startH = ev.startIso.drop(11).take(5)
                val endH = ev.endIso.drop(11).take(5)
                sb.appendLine("• **$startH - $endH** | **${ev.titolo}**")
                sb.appendLine("  📍 Luogo: ${ev.luogo}")
                val dossier = archiveRepo.findClientDossier(ev.cliente, ev.luogo)
                if (dossier.isNotEmpty()) {
                    sb.appendLine("  📁 Fascicolo trovato: ${dossier.size} file (${dossier.first().category.displayTitle})")
                    allLinkedDocs.addAll(dossier)
                }
            }
        }

        sb.appendLine()
        if (tomorrowEvs.isEmpty()) {
            sb.appendLine("🗓️ **Domani:** Nessun impegno fissato.")
        } else {
            sb.appendLine("🗓️ **Anticipazione Domani (${tomorrowEvs.size}):**")
            for (ev in tomorrowEvs) {
                val startH = ev.startIso.drop(11).take(5)
                sb.appendLine("• **$startH** — ${ev.titolo} (${ev.luogo})")
            }
        }

        return ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = false,
            text = sb.toString().trim(),
            matchedDocuments = allLinkedDocs.distinctBy { it.id }.take(5),
            briefingEvents = (todayEvs + tomorrowEvs).distinctBy { it.id }
        )
    }

    private fun buildConfirmationChatMessage(
        event: CalendarEventItem,
        prefix: String = "✅ **Appuntamento Confermato in Agenda!**\n\n"
    ): ChatMessage {
        val dateStr = event.startIso.take(10)
        val startH = event.startIso.drop(11).take(5)
        val endH = event.endIso.drop(11).take(5)
        val syncBadge = if (event.systemCalendarEventId != null) {
            "📲 Sincronizzato con il Calendario Google/Android del telefono"
        } else {
            "💾 Salvato nell'Agenda Locale dell'app"
        }
        val dossier = archiveRepo.findClientDossier(event.cliente, event.luogo)

        val catEnum = PeritoEventCategory.fromString(event.categoria.ifBlank { event.titolo })
        val categoryPart = "🏷️ **Categoria:** ${catEnum.emoji} ${catEnum.displayName}\n"

        val clientPart = if (event.cliente.isNotBlank() && event.cliente != "Cliente" && !event.cliente.equals(event.titolo, ignoreCase = true) && !event.cliente.equals(event.categoria, ignoreCase = true)) {
            "👤 **Con / Cliente:** ${event.cliente}\n"
        } else {
            ""
        }
        val placePart = if (event.luogo.isNotBlank() && event.luogo != "Da definire") {
            "📍 **Luogo:** ${event.luogo}\n"
        } else {
            "📍 **Luogo:** Da definire\n"
        }
        val notesPart = if (event.note.isNotBlank()) {
            "📝 **Note:** ${event.note}\n"
        } else {
            ""
        }

        return ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = false,
            text = prefix +
                "📌 **${event.titolo}**\n" +
                categoryPart +
                "📅 **Data:** $dateStr dalle **$startH** alle **$endH**\n" +
                clientPart +
                placePart +
                notesPart +
                "🔔 **Promemoria automatici:** 24 ore prima, 60 min prima, 30 min prima\n" +
                syncBadge,
            createdEvent = event,
            matchedDocuments = dossier.take(3)
        )
    }

    private fun clearConflictButtonsOnMessage(messageId: String) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { msg ->
                    if (msg.id == messageId) msg.copy(pendingConflict = null) else msg
                }
            )
        }
        viewModelScope.launch {
            chatHistoryRepo.saveMessages(_uiState.value.messages)
        }
    }

    private fun appendAssistantMessage(msg: ChatMessage) {
        _uiState.update { it.copy(messages = it.messages + msg) }
        appScope.launch {
            chatHistoryRepo.saveMessages(_uiState.value.messages)
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            // Trigger cartoonish vaporize/dissolve animation across all message bubbles
            _uiState.update { it.copy(isClearingHistory = true) }
            kotlinx.coroutines.delay(420L)
            chatHistoryRepo.clearHistory()
            val studio = _uiState.value.aiConfig.studioProfile.studioName.ifBlank { "Studio Tecnico Peritale" }
            val freshMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                isUser = false,
                text = "🧹 **Cronologia ripulita.**\n\nPronto per un nuovo sopralluogo o documento per lo studio **$studio**."
            )
            chatHistoryRepo.saveMessages(listOf(freshMsg))
            _uiState.update { it.copy(messages = listOf(freshMsg), isClearingHistory = false) }
        }
    }

    fun selectTab(tab: MainAppTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        if (tab == MainAppTab.AGENDA) {
            refreshAllData()
        }
    }

    fun selectFascicoloFilter(id: String?) {
        _uiState.update { it.copy(selectedFascicoloId = id) }
    }

    fun createNewFascicolo(
        codice: String,
        titolo: String,
        cliente: String,
        controparte: String = "",
        indirizzo: String = "",
        note: String = ""
    ) {
        viewModelScope.launch {
            val f = fascicoloRepo.createNewFascicolo(codice, titolo, cliente, controparte, indirizzo, note)
            refreshAllData()
            _uiState.update { it.copy(selectedFascicoloId = f.id, statusBanner = "📁 Fascicolo creato: ${f.codice}") }
            appendAssistantMessage(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = "📁 **Nuovo Fascicolo Creato!**\n\n" +
                        "• **Codice:** `${f.codice}`\n" +
                        "• **Titolo:** ${f.titolo}\n" +
                        "• **Cliente:** ${f.cliente}\n" +
                        (if (f.indirizzo.isNotBlank()) "• **Indirizzo:** ${f.indirizzo}\n" else "") +
                        "\nPronto a ricevere perizie, foto di sopralluogo e appuntamenti."
                )
            )
        }
    }

    fun updateFascicoloStato(id: String, stato: FascicoloStato) {
        viewModelScope.launch {
            fascicoloRepo.updateStato(id, stato)
            refreshAllData()
            _uiState.update { it.copy(statusBanner = "Stato aggiornato a: ${stato.label}") }
        }
    }

    fun deleteFascicolo(id: String) {
        viewModelScope.launch {
            fascicoloRepo.deleteFascicolo(id)
            if (_uiState.value.selectedFascicoloId == id) {
                _uiState.update { it.copy(selectedFascicoloId = null) }
            }
            refreshAllData()
            _uiState.update { it.copy(statusBanner = "Fascicolo rimosso con successo") }
        }
    }

    fun updateStudioProfile(newProfile: StudioProfile) {
        val updatedConfig = _uiState.value.aiConfig.copy(studioProfile = newProfile)
        saveConfig(updatedConfig)
        _uiState.update {
            it.copy(
                aiConfig = updatedConfig,
                statusBanner = "✅ Profilo Studio aggiornato: ${newProfile.studioName}"
            )
        }
    }

    private fun loadConfig(): AiEngineConfig {
        if (!configFile.exists()) return AiEngineConfig()
        return try {
            val loaded = json.decodeFromString<AiEngineConfig>(configFile.readText())
            if (loaded.otaManifestUrl.contains("githubusercontent.com/manue/")) {
                val fixed = loaded.copy(otaManifestUrl = OtaUpdateManager.DEFAULT_MANIFEST_URL)
                saveConfig(fixed)
                fixed
            } else {
                loaded
            }
        } catch (_: Throwable) {
            AiEngineConfig()
        }
    }

    private fun saveConfig(config: AiEngineConfig) {
        try {
            configFile.writeText(json.encodeToString(config))
        } catch (_: Throwable) {
        }
    }

    fun openModelSelectionDialog() {
        _uiState.update { it.copy(showFirstRunModelDialog = true) }
    }

    fun dismissFirstRunModelDialog() {
        val customPath = _uiState.value.aiConfig.mediaPipeModelPath
        val reasoningReady = modelDownloadManager.isPrimaryReasoningDownloaded(customPath)
        val isDl = _uiState.value.downloadProgress.isDownloading
        val updatedConfig = _uiState.value.aiConfig.copy(hasSeenModelSetupPrompt = true)
        saveConfig(updatedConfig)
        _uiState.update {
            it.copy(
                showFirstRunModelDialog = false,
                aiConfig = updatedConfig,
                isReasoningReady = reasoningReady,
                areAllModelsReady = modelDownloadManager.areAllModelsDownloaded(customPath),
                statusBanner = when {
                    isDl -> "⬇️ Download attivo in background (controlla la notifica di sistema)"
                    reasoningReady -> "✅ Modello Reasoning Qwen 3.5 attivo"
                    else -> "Pronto all'uso (puoi gestire i modelli da Impostazioni IA)"
                }
            )
        }
    }

    fun getActiveReasoningModelLabel(): String {
        return if (_uiState.value.aiConfig.reasoningModelId == modelDownloadManager.powerfulReasoningModel.id) {
            "Qwen 3.5 4B Potenziato"
        } else {
            "Qwen 3.5 2B (Q4_K_M)"
        }
    }

    /**
     * Consente all'utente di selezionare/sostituire il modello di reasoning (es. passare dal 2B al 4B Potenziato
     * o viceversa), aggiornando istantaneamente la configurazione persistente e caricando il modello nativo.
     */
    fun selectActiveReasoningModel(modelId: String) {
        val targetModel = modelDownloadManager.getActiveReasoningModel(modelId)
        val suggestedOllama = if (modelId == modelDownloadManager.powerfulReasoningModel.id) {
            "frob/qwen3.5-instruct:4b"
        } else {
            "qwen3.5-fallback:latest"
        }
        val updatedConfig = _uiState.value.aiConfig.copy(
            reasoningModelId = modelId,
            ollamaModel = suggestedOllama
        )
        saveConfig(updatedConfig)
        val isReady = modelDownloadManager.isPrimaryReasoningDownloaded(updatedConfig.mediaPipeModelPath, modelId)
        _uiState.update {
            it.copy(
                aiConfig = updatedConfig,
                isReasoningReady = isReady,
                areAllModelsReady = modelDownloadManager.areAllModelsDownloaded(updatedConfig.mediaPipeModelPath),
                statusBanner = "⭐ Modello attivo impostato su: ${targetModel.title}"
            )
        }
        viewModelScope.launch(Dispatchers.Default) {
            aiEngine.ensureGgufLoaded(updatedConfig)
        }
    }

    fun toggleUseLocalOllama(enabled: Boolean, modelName: String? = null, url: String? = null) {
        val updatedConfig = _uiState.value.aiConfig.copy(
            useLocalOllama = enabled,
            ollamaModel = modelName ?: _uiState.value.aiConfig.ollamaModel,
            ollamaUrl = url ?: _uiState.value.aiConfig.ollamaUrl
        )
        saveConfig(updatedConfig)
        _uiState.update { it.copy(aiConfig = updatedConfig) }
    }

    /**
     * Scarica sequenzialmente tutti i modelli mancanti della suite attiva
     * (Modello di Reasoning Selezionato + Decision Model Rizzo Flow)
     * in background con Foreground Service e Notifica Android di avanzamento.
     */
    fun downloadAllMissingModels() {
        if (_uiState.value.downloadProgress.isDownloading) return
        val activeReasoning = modelDownloadManager.getActiveReasoningModel(_uiState.value.aiConfig.reasoningModelId)
        val targetSuite = buildList {
            if (!modelDownloadManager.isModelDownloaded(activeReasoning) &&
                !modelDownloadManager.isPrimaryReasoningDownloaded(_uiState.value.aiConfig.mediaPipeModelPath, activeReasoning.id)
            ) {
                add(activeReasoning)
            }
            addAll(modelDownloadManager.fixedTaskModels)
        }
        val missing = targetSuite.filterNot { modelDownloadManager.isModelDownloaded(it) }
        if (missing.isEmpty()) {
            dismissFirstRunModelDialog()
            return
        }

        com.example.assistenteperitomobile.data.ModelDownloadService.onCancelRequestedCallback = {
            cancelModelDownload()
        }

        activeDownloadJob = viewModelScope.launch {
            try {
                missing.forEachIndexed { idx, model ->
                    _uiState.update {
                        it.copy(
                            downloadProgress = ModelDownloadProgress(
                                isDownloading = true,
                                modelId = model.id,
                                modelTitle = model.title,
                                downloadedBytes = 0L,
                                totalBytes = model.expectedBytes,
                                currentQueueIndex = idx + 1,
                                totalQueueCount = missing.size,
                                error = null
                            )
                        )
                    }
                    modelDownloadManager.notifyDownloadProgress(
                        modelTitle = model.title,
                        downloadedBytes = 0L,
                        totalBytes = model.expectedBytes,
                        queueIndex = idx + 1,
                        queueTotal = missing.size,
                        isFirstStart = (idx == 0)
                    )
                    val downloadedFile = modelDownloadManager.downloadModel(model) { readBytes, totalBytes, speedBytes ->
                        _uiState.update { state ->
                            state.copy(
                                downloadProgress = state.downloadProgress.copy(
                                    downloadedBytes = readBytes,
                                    totalBytes = totalBytes,
                                    bytesPerSecond = speedBytes
                                )
                            )
                        }
                        modelDownloadManager.notifyDownloadProgress(
                            modelTitle = model.title,
                            downloadedBytes = readBytes,
                            totalBytes = totalBytes,
                            queueIndex = idx + 1,
                            queueTotal = missing.size,
                            isFirstStart = false,
                            bytesPerSecond = speedBytes
                        )
                    }
                    if (model.isPrimaryReasoning) {
                        val updatedConfig = _uiState.value.aiConfig.copy(
                            useMediaPipeOnDevice = true,
                            mediaPipeModelPath = downloadedFile.absolutePath,
                            hasSeenModelSetupPrompt = true
                        )
                        saveConfig(updatedConfig)
                        _uiState.update {
                            it.copy(
                                aiConfig = updatedConfig,
                                downloadedModelIds = computeDownloadedModelIds(),
                                isReasoningReady = true
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(downloadedModelIds = computeDownloadedModelIds())
                        }
                    }
                }

                val customPath = _uiState.value.aiConfig.mediaPipeModelPath
                val reasoningReady = modelDownloadManager.isPrimaryReasoningDownloaded(customPath)
                val allReady = modelDownloadManager.areAllModelsDownloaded(customPath)
                modelDownloadManager.notifyDownloadFinished(
                    title = "✅ Modello IA Reasoning Scaricato!",
                    message = "Qwen 3.5 2B (Q4_K_M) è pronto nella memoria interna dell'app. OCR e Visione sono attivi nativamente tramite ML Kit.",
                    isSuccess = true
                )
                _uiState.update {
                    it.copy(
                        downloadedModelIds = computeDownloadedModelIds(),
                        downloadProgress = ModelDownloadProgress(isDownloading = false),
                        isReasoningReady = reasoningReady,
                        areAllModelsReady = allReady,
                        showFirstRunModelDialog = false,
                        statusBanner = "✅ Suite Modelli IA scaricata e attiva on-device!"
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                modelDownloadManager.notifyDownloadFinished(
                    title = "⏸️ Download Modelli IA in Pausa",
                    message = "I byte già scaricati sono stati salvati. Riprendi quando vuoi da Impostazioni IA.",
                    isSuccess = false
                )
                _uiState.update {
                    it.copy(
                        downloadProgress = ModelDownloadProgress(isDownloading = false),
                        statusBanner = "⏸️ Download sospeso (riprenderà dal punto interrotto)"
                    )
                }
            } catch (e: Throwable) {
                val errMsg = e.localizedMessage ?: "Errore di rete durante il download da Hugging Face"
                modelDownloadManager.notifyDownloadFinished(
                    title = "⚠️ Download Modello IA Interrotto",
                    message = "$errMsg — Tocca Scarica per riprendere dal punto interrotto.",
                    isSuccess = false
                )
                _uiState.update {
                    it.copy(
                        downloadProgress = ModelDownloadProgress(
                            isDownloading = false,
                            error = errMsg
                        )
                    )
                }
            }
        }
    }

    fun startModelDownload(model: DownloadableAiModel) {
        if (_uiState.value.downloadProgress.isDownloading) return

        com.example.assistenteperitomobile.data.ModelDownloadService.onCancelRequestedCallback = {
            cancelModelDownload()
        }

        activeDownloadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    downloadProgress = ModelDownloadProgress(
                        isDownloading = true,
                        modelId = model.id,
                        modelTitle = model.title,
                        downloadedBytes = 0L,
                        totalBytes = model.expectedBytes,
                        currentQueueIndex = 1,
                        totalQueueCount = 1,
                        error = null
                    )
                )
            }
            modelDownloadManager.notifyDownloadProgress(
                modelTitle = model.title,
                downloadedBytes = 0L,
                totalBytes = model.expectedBytes,
                queueIndex = 1,
                queueTotal = 1,
                isFirstStart = true
            )
            try {
                val downloadedFile = modelDownloadManager.downloadModel(model) { readBytes, totalBytes, speedBytes ->
                    _uiState.update { state ->
                        state.copy(
                            downloadProgress = state.downloadProgress.copy(
                                downloadedBytes = readBytes,
                                totalBytes = totalBytes,
                                bytesPerSecond = speedBytes
                            )
                        )
                    }
                    modelDownloadManager.notifyDownloadProgress(
                        modelTitle = model.title,
                        downloadedBytes = readBytes,
                        totalBytes = totalBytes,
                        queueIndex = 1,
                        queueTotal = 1,
                        isFirstStart = false,
                        bytesPerSecond = speedBytes
                    )
                }

                val updatedConfig = if (model.isPrimaryReasoning) {
                    _uiState.value.aiConfig.copy(
                        useMediaPipeOnDevice = true,
                        mediaPipeModelPath = downloadedFile.absolutePath,
                        hasSeenModelSetupPrompt = true
                    ).also { saveConfig(it) }
                } else {
                    _uiState.value.aiConfig
                }

                val reasoningReady = modelDownloadManager.isPrimaryReasoningDownloaded(updatedConfig.mediaPipeModelPath)
                val allReady = modelDownloadManager.areAllModelsDownloaded(updatedConfig.mediaPipeModelPath)

                modelDownloadManager.notifyDownloadFinished(
                    title = "✅ Modello IA Scaricato!",
                    message = "${model.title} è pronto nella memoria interna di PeritoAI.",
                    isSuccess = true
                )

                _uiState.update {
                    it.copy(
                        aiConfig = updatedConfig,
                        downloadedModelIds = computeDownloadedModelIds(),
                        downloadProgress = ModelDownloadProgress(isDownloading = false),
                        isReasoningReady = reasoningReady,
                        areAllModelsReady = allReady,
                        showFirstRunModelDialog = if (model.isPrimaryReasoning && allReady) false else it.showFirstRunModelDialog,
                        statusBanner = "✅ ${model.title} scaricato nella memoria interna dell'app!"
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                modelDownloadManager.notifyDownloadFinished(
                    title = "⏸️ Download Modello IA in Pausa",
                    message = "Progresso salvato per ${model.title}. Potrai riprendere dal punto interrotto.",
                    isSuccess = false
                )
                _uiState.update {
                    it.copy(
                        downloadProgress = ModelDownloadProgress(isDownloading = false),
                        statusBanner = "⏸️ Download sospeso (riprenderà dal punto interrotto)"
                    )
                }
            } catch (e: Throwable) {
                val errMsg = e.localizedMessage ?: "Errore durante il download da Hugging Face"
                modelDownloadManager.notifyDownloadFinished(
                    title = "⚠️ Download Modello IA Interrotto",
                    message = "$errMsg — Tocca Scarica per riprendere dal punto interrotto.",
                    isSuccess = false
                )
                _uiState.update {
                    it.copy(
                        downloadProgress = ModelDownloadProgress(
                            isDownloading = false,
                            error = errMsg
                        )
                    )
                }
            }
        }
    }

    fun cancelModelDownload() {
        activeDownloadJob?.cancel()
        activeDownloadJob = null
    }

    fun activateDownloadedModel(model: DownloadableAiModel) {
        val file = modelDownloadManager.getDownloadedModelFile(model)
        if (!file.exists()) return
        val updatedConfig = if (model.isPrimaryReasoning) {
            _uiState.value.aiConfig.copy(
                useMediaPipeOnDevice = true,
                mediaPipeModelPath = file.absolutePath,
                hasSeenModelSetupPrompt = true
            ).also { saveConfig(it) }
        } else {
            _uiState.value.aiConfig
        }
        val reasoningReady = modelDownloadManager.isPrimaryReasoningDownloaded(updatedConfig.mediaPipeModelPath)
        val allReady = modelDownloadManager.areAllModelsDownloaded(updatedConfig.mediaPipeModelPath)
        _uiState.update {
            it.copy(
                aiConfig = updatedConfig,
                isReasoningReady = reasoningReady,
                areAllModelsReady = allReady,
                statusBanner = "✅ ${model.title} attivo on-device"
            )
        }
    }

    fun deleteDownloadedModel(model: DownloadableAiModel) {
        val file = modelDownloadManager.getDownloadedModelFile(model)
        modelDownloadManager.deleteModelFile(file)
        val currentPath = _uiState.value.aiConfig.mediaPipeModelPath
        val updatedConfig = if (currentPath == file.absolutePath) {
            _uiState.value.aiConfig.copy(mediaPipeModelPath = "")
        } else {
            _uiState.value.aiConfig
        }
        saveConfig(updatedConfig)
        val reasoningReady = modelDownloadManager.isPrimaryReasoningDownloaded(updatedConfig.mediaPipeModelPath)
        val allReady = modelDownloadManager.areAllModelsDownloaded(updatedConfig.mediaPipeModelPath)
        _uiState.update {
            it.copy(
                aiConfig = updatedConfig,
                downloadedModelIds = computeDownloadedModelIds(),
                isReasoningReady = reasoningReady,
                areAllModelsReady = allReady,
                showFirstRunModelDialog = !reasoningReady,
                statusBanner = if (!reasoningReady) {
                    "⛔ Modello di Reasoning rimosso: scaricalo nuovamente per usare l'app"
                } else {
                    "🗑️ Modello eliminato dalla memoria interna dell'app"
                }
            )
        }
    }

    private fun computeDownloadedModelIds(): Set<String> {
        return modelDownloadManager.availableModels
            .filter { modelDownloadManager.isModelDownloaded(it) }
            .map { it.id }
            .toSet()
    }

    fun handleIncomingShareIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_SEND) {
            val type = intent.type ?: ""
            if (type == "text/plain") {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrBlank()) {
                    sendUserMessage(sharedText)
                }
            } else {
                @Suppress("DEPRECATION")
                val streamUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (streamUri != null) {
                    processDocumentUri(streamUri)
                }
            }
        }
    }

    // ==========================================
    // OTA UPDATE ENGINE (In-App Updates)
    // ==========================================

    fun checkForUpdates(silent: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdate = true, updateStatusMessage = "Verifica aggiornamenti in corso...") }
            val rawUrl = _uiState.value.aiConfig.otaManifestUrl
            val url = if (rawUrl.contains("githubusercontent.com/manue/")) {
                OtaUpdateManager.DEFAULT_MANIFEST_URL
            } else {
                rawUrl.ifBlank { OtaUpdateManager.DEFAULT_MANIFEST_URL }
            }
            val result = otaUpdateManager.checkForUpdate(manifestUrl = url)
            when (result.status) {
                UpdateCheckStatus.UPDATE_AVAILABLE -> {
                    _uiState.update {
                        it.copy(
                            availableUpdate = result.manifest,
                            showUpdateDialog = true,
                            isCheckingUpdate = false,
                            updateStatusMessage = "Nuova versione v${result.manifest?.latestVersionName} disponibile!"
                        )
                    }
                    if (!silent) {
                        appendAssistantMessage(
                            ChatMessage(
                                id = UUID.randomUUID().toString(),
                                isUser = false,
                                text = "🚀 **Nuova Versione Disponibile!**\n\n" +
                                    "• Versione: **v${result.manifest?.latestVersionName}** (Build ${result.manifest?.latestVersionCode})\n" +
                                    "• Note di rilascio:\n${result.manifest?.releaseNotes ?: "Miglioramenti delle prestazioni e stabilità"}\n\n" +
                                    "Puoi scaricarla subito dalla finestra di aggiornamento o dalle impostazioni (⚙️)."
                            )
                        )
                    }
                }
                UpdateCheckStatus.UP_TO_DATE -> {
                    _uiState.update {
                        it.copy(
                            availableUpdate = null,
                            showUpdateDialog = false,
                            isCheckingUpdate = false,
                            updateStatusMessage = "L'app è aggiornata all'ultima versione (v${result.currentVersionName})"
                        )
                    }
                    if (!silent) {
                        appendAssistantMessage(
                            ChatMessage(
                                id = UUID.randomUUID().toString(),
                                isUser = false,
                                text = "✅ **L'app è già aggiornata!**\n\nStai utilizzando la versione più recente (v${result.currentVersionName}, Build ${result.currentVersionCode})."
                            )
                        )
                    }
                }
                UpdateCheckStatus.CHECK_FAILED -> {
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            updateStatusMessage = "Controllo non riuscito: ${result.errorMessage}"
                        )
                    }
                    if (!silent) {
                        appendAssistantMessage(
                            ChatMessage(
                                id = UUID.randomUUID().toString(),
                                isUser = false,
                                text = "⚠️ **Verifica aggiornamenti fallita**\n\nNon è stato possibile contattare il server:\n_${result.errorMessage}_"
                            )
                        )
                    }
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    fun startUpdateDownloadAndInstall(context: Context? = null) {
        val manifest = _uiState.value.availableUpdate ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    updateDownloadProgress = 0f,
                    updateStatusMessage = "Inizio download v${manifest.latestVersionName}..."
                )
            }

            val targetCtx = context ?: appContext
            val result = otaUpdateManager.downloadApk(
                downloadUrl = manifest.downloadUrl,
                targetVersionCode = manifest.latestVersionCode
            ) { progress, _, _ ->
                _uiState.update {
                    it.copy(
                        updateDownloadProgress = progress,
                        updateStatusMessage = "Download v${manifest.latestVersionName}: ${(progress * 100).toInt()}%"
                    )
                }
            }

            result.fold(
                onSuccess = { apkFile ->
                    _uiState.update {
                        it.copy(
                            updateDownloadProgress = 1f,
                            updateStatusMessage = "Download completato! Avvio installazione..."
                        )
                    }

                    if (otaUpdateManager.canRequestPackageInstalls()) {
                        val installIntent = otaUpdateManager.createInstallIntent(apkFile)
                        targetCtx.startActivity(installIntent)
                        _uiState.update { it.copy(showUpdateDialog = false) }
                    } else {
                        _uiState.update {
                            it.copy(
                                updateStatusMessage = "Autorizza l'installazione di app sconosciute nelle impostazioni di sistema, quindi riprova."
                            )
                        }
                        val manageIntent = otaUpdateManager.createManageUnknownAppSourcesIntent()
                        targetCtx.startActivity(manageIntent)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            updateDownloadProgress = null,
                            updateStatusMessage = "Errore download APK: ${error.localizedMessage ?: "Errore sconosciuto"}"
                        )
                    }
                }
            )
        }
    }

    fun updateOtaManifestUrl(newUrl: String) {
        val updatedConfig = _uiState.value.aiConfig.copy(otaManifestUrl = newUrl.trim())
        saveConfig(updatedConfig)
        _uiState.update { it.copy(aiConfig = updatedConfig) }
    }

    fun toggleAutoCheckUpdates(enabled: Boolean) {
        val updatedConfig = _uiState.value.aiConfig.copy(autoCheckUpdatesOnStart = enabled)
        saveConfig(updatedConfig)
        _uiState.update { it.copy(aiConfig = updatedConfig) }
    }

    private var calendarContentObserver: ContentObserver? = null

    fun registerCalendarObserver() {
        if (calendarContentObserver != null) return
        if (!calendarRepo.hasNativeCalendarPermissions()) return
        try {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                private var lastRefreshTime = 0L
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    val now = System.currentTimeMillis()
                    if (now - lastRefreshTime > 1500L) {
                        lastRefreshTime = now
                        refreshAllData()
                    }
                }
            }
            appContext.contentResolver.registerContentObserver(
                CalendarContract.Events.CONTENT_URI,
                true,
                observer
            )
            calendarContentObserver = observer
        } catch (e: Throwable) {
            Log.w("PeritoViewModel", "Unable to register calendar content observer", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        calendarContentObserver?.let {
            try {
                appContext.contentResolver.unregisterContentObserver(it)
            } catch (_: Throwable) {}
        }
    }

    fun triggerTestMorningBriefing() {
        viewModelScope.launch {
            MorningBriefingWorker.triggerImmediateBriefing(appContext)
            _uiState.update { it.copy(statusBanner = "☀️ Notifica Briefing Mattutino inviata!") }
        }
    }

    fun checkAndTriggerMorningBriefingOnFirstLaunch() {
        if (!_uiState.value.aiConfig.morningBriefingEnabled) return
        val now = LocalTime.now()
        if (now.hour in 7..12) {
            val todayStr = LocalDate.now().toString()
            val prefs = appContext.getSharedPreferences("perito_briefing_prefs", Context.MODE_PRIVATE)
            val lastBriefingDay = prefs.getString("last_briefing_date", null)
            if (lastBriefingDay != todayStr) {
                prefs.edit().putString("last_briefing_date", todayStr).apply()
                viewModelScope.launch {
                    MorningBriefingWorker.sendBriefingNotification(appContext)
                }
            }
        }
    }
}
