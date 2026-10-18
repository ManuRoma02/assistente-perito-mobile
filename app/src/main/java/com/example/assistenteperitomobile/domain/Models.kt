package com.example.assistenteperitomobile.domain

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

enum class PeritoCategory(
    val folderName: String,
    val displayTitle: String,
    val shortDesc: String
) {
    ATTI_TRIBUNALE(
        "Atti_Tribunale",
        "Atti Tribunale",
        "Nomine CTU, verbali d'udienza, decreti, quesiti del giudice, esecuzioni immobiliari"
    ),
    PERIZIE_TECNICHE(
        "Perizie_Tecniche",
        "Perizie Tecniche",
        "Relazioni tecniche, sopralluoghi, stime immobiliari, perizie danni e infiltrazioni"
    ),
    PLANIMETRIE_COMPUTI(
        "Planimetrie_Computi",
        "Planimetrie & Computi",
        "Visure catastali, elaborati planimetrici, computi metrici estimativi, elenchi prezzi, APE"
    ),
    FATTURE_PREVENTIVI(
        "Fatture_Preventivi",
        "Fatture & Preventivi",
        "Fatture, parcelle pro-forma, preventivi imprese edili, note spese, quietanze"
    ),
    CORRISPONDENZA_LEGALE(
        "Corrispondenza_Legale",
        "Corrispondenza Legale",
        "Diffide, PEC avvocati, messe in mora, osservazioni CTP, convocazioni parti"
    ),
    DA_CLASSIFICARE_MANUALMENTE(
        "Da_Classificare_Manualmente",
        "Da Classificare",
        "Documenti non pertinenti o privi di testo leggibile"
    );

    companion object {
        fun fromFolderName(name: String?): PeritoCategory {
            if (name.isNullOrBlank()) return DA_CLASSIFICARE_MANUALMENTE
            return entries.firstOrNull { it.folderName.equals(name.trim(), ignoreCase = true) }
                ?: DA_CLASSIFICARE_MANUALMENTE
        }
    }
}

@Immutable
@Serializable
data class DocumentClassificationResult(
    val categoria: String,
    val cliente_o_controparte: String,
    val oggetto_sintetico: String,
    val data_documento: String
)

@Immutable
@Serializable
data class StudioProfile(
    val studioName: String = "Studio Tecnico Peritale",
    val professionistaNome: String = "Perito Tecnico",
    val alboIscrizione: String = "",
    val citta: String = "",
    val recapito: String = ""
)

enum class MainAppTab(val title: String) {
    PRATICHE("Pratiche & Fascicoli"),
    AGENDA("Agenda Sopralluoghi"),
    CHAT("Assistente PeritoAI")
}

enum class FascicoloStato(val label: String) {
    APERTO("Aperto"),
    IN_CORSO("In Corso"),
    CHIUSO("Chiuso")
}

@Immutable
@Serializable
data class Fascicolo(
    val id: String,
    val codice: String,
    val titolo: String,
    val cliente: String,
    val controparte: String = "",
    val indirizzo: String = "",
    val stato: FascicoloStato = FascicoloStato.IN_CORSO,
    val note: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Immutable
@Serializable
data class ArchivedDocument(
    val id: String,
    val fileName: String,
    val originalName: String,
    val categoryFolder: String,
    val cliente: String,
    val oggetto: String,
    val dateIso: String,
    val filePath: String,
    val sizeBytes: Long,
    val extractedTextSnippet: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val fascicoloId: String? = null
) {
    val category: PeritoCategory
        get() = PeritoCategory.fromFolderName(categoryFolder)
}

@Immutable
@Serializable
data class AppointmentParseResult(
    val has_appointment: Boolean,
    val titolo: String = "",
    val data_inizio_iso: String? = null,
    val data_fine_iso: String? = null,
    val luogo: String = "Da definire",
    val cliente: String = "Cliente",
    val note: String = "",
    val is_ambiguo: Boolean = false,
    val motivo_ambiguita: String = "",
    val fascicoloId: String? = null
)

@Immutable
@Serializable
data class CalendarEventItem(
    val id: String,
    val systemCalendarEventId: Long? = null,
    val titolo: String,
    val startIso: String,
    val endIso: String,
    val startMillis: Long,
    val endMillis: Long,
    val luogo: String,
    val cliente: String,
    val note: String,
    val remindersMinutes: List<Int> = listOf(30, 60, 1440),
    val isFromSystemCalendar: Boolean = false,
    val fascicoloId: String? = null
)

@Immutable
@Serializable
data class PendingConflictState(
    val proposed: AppointmentParseResult,
    val conflictingEvents: List<CalendarEventItem>
)

@Immutable
@Serializable
data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val attachmentName: String? = null,
    val attachmentType: String? = null,
    val voiceDurationSeconds: Int? = null,
    val voiceAudioPath: String? = null,
    val pendingConflict: PendingConflictState? = null,
    val archivedDocument: ArchivedDocument? = null,
    val matchedDocuments: List<ArchivedDocument> = emptyList(),
    val createdEvent: CalendarEventItem? = null,
    val briefingEvents: List<CalendarEventItem> = emptyList()
)

@Immutable
@Serializable
data class AiEngineConfig(
    val useMediaPipeOnDevice: Boolean = true,
    val mediaPipeModelPath: String = "",
    val reasoningModelId: String = "qwen35_2b_gpt51_highiq_q4km",
    val fixedSpeechModel: String = "Google SpeechRecognizer In-App",
    val fixedOcrModel: String = "LightOnOCR-3 0.8B (Q4_K_M)",
    val fixedVisionModel: String = "MiniCPM5-2B (Q4_K_M)",
    val fixedVisionModelPath: String = "",
    val useLocalOllama: Boolean = false,
    val ollamaUrl: String = "http://127.0.0.1:11434/v1/chat/completions",
    val ollamaModel: String = "qwen3.5-fallback:latest",
    val useRizzoFlow: Boolean = false,
    val rizzoFlowUrl: String = "http://127.0.0.1:8017/v1/systemone",
    val morningBriefingEnabled: Boolean = true,
    val hasSeenModelSetupPrompt: Boolean = false,
    val syncSystemCalendar: Boolean = true,
    val otaManifestUrl: String = "https://raw.githubusercontent.com/manue/assistente-perito-mobile/main/ota-update.json",
    val autoCheckUpdatesOnStart: Boolean = true,
    val studioProfile: StudioProfile = StudioProfile()
)
