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

@Serializable
enum class PeritoEventCategory(
    val id: String,
    val displayName: String,
    val emoji: String,
    val defaultPrefix: String
) {
    SOPRALLUOGO("sopralluogo", "Sopralluogo", "🔍", "Sopralluogo"),
    RILIEVO("rilievo", "Rilievo", "📐", "Rilievo Tecnico"),
    UNIONCASA("unioncasa", "Unioncasa", "🏠", "Incontro Unioncasa"),
    AGENZIA_ORSO("agenzia_orso", "Agenzia Orso", "🐻", "Appuntamento Agenzia Orso"),
    COMUNE("comune", "Comune", "🏛️", "Incontro Comune"),
    TRIBUNALE("tribunale", "Tribunale", "⚖️", "Udienza / Tribunale"),
    RIUNIONE("riunione", "Riunione", "🏢", "Riunione Studio"),
    CLIENTE("cliente", "Cliente", "👤", "Appuntamento Cliente"),
    PERSONALE("personale", "Personale", "☕", "Personale"),
    ALTRO("altro", "Altro", "📅", "Impegno");

    companion object {
        fun fromString(value: String?): PeritoEventCategory {
            if (value.isNullOrBlank()) return SOPRALLUOGO
            val norm = value.trim().lowercase(java.util.Locale.ITALIAN)
            return entries.firstOrNull {
                it.id.equals(norm, ignoreCase = true) ||
                it.displayName.equals(norm, ignoreCase = true)
            } ?: when {
                norm.contains("unioncasa") || norm.contains("union casa") -> UNIONCASA
                norm.contains("agenzia orso") || norm.contains("orso") -> AGENZIA_ORSO
                norm.contains("riliev") || norm.contains("misuraz") || norm.contains("misure") -> RILIEVO
                norm.contains("comun") || norm.contains("catast") || norm.contains("ufficio tecnic") || norm.contains("sportello") || norm.contains("edilizia") || norm.contains("accesso atti") -> COMUNE
                norm.contains("tribunal") || norm.contains("udienz") || norm.contains("giudic") || norm.contains("ctu") || norm.contains("giurament") || norm.contains("cancelleria") -> TRIBUNALE
                norm.contains("riunion") || norm.contains("studi") || norm.contains("collegh") -> RIUNIONE
                norm.contains("caff") || norm.contains("pranz") || norm.contains("cen") || norm.contains("aperitiv") || norm.contains("colazion") || norm.contains("person") -> PERSONALE
                norm.contains("client") || norm.contains("firm") || norm.contains("consegn") -> CLIENTE
                norm.contains("soprall") || norm.contains("periz") || norm.contains("collaud") || norm.contains("visita") -> SOPRALLUOGO
                else -> SOPRALLUOGO
            }
        }
    }
}

@Immutable
@Serializable
data class CategoryReferenceFormat(
    val showTime: Boolean = false,
    val showType: Boolean = true,
    val showSubject: Boolean = true,
    val showPlace: Boolean = true,
    val showNotes: Boolean = false
) {
    fun formatTitle(
        startTimeIso: String?,
        categoryType: String,
        subject: String,
        place: String,
        notes: String
    ): String {
        val parts = mutableListOf<String>()
        if (showTime && !startTimeIso.isNullOrBlank()) {
            val h = startTimeIso.drop(11).take(5)
            if (h.isNotBlank()) parts.add(h)
        }
        if (showType && categoryType.isNotBlank()) {
            parts.add(categoryType)
        }
        if (showSubject && subject.isNotBlank() && subject != "Cliente") {
            parts.add(subject)
        }

        var base = parts.joinToString(" - ")
        if (base.isBlank()) {
            base = if (categoryType.isNotBlank()) categoryType else "Impegno"
        }

        val extras = mutableListOf<String>()
        if (showPlace && place.isNotBlank() && place != "Da definire") {
            extras.add(place)
        }
        if (showNotes && notes.isNotBlank()) {
            extras.add(notes)
        }

        return if (extras.isNotEmpty()) {
            "$base (${extras.joinToString(" • ")})"
        } else {
            base
        }
    }
}

fun defaultCategoryFormats(): Map<String, CategoryReferenceFormat> = mapOf(
    PeritoEventCategory.SOPRALLUOGO.id to CategoryReferenceFormat(
        showTime = false, showType = true, showSubject = true, showPlace = true, showNotes = false
    ),
    PeritoEventCategory.RILIEVO.id to CategoryReferenceFormat(
        showTime = false, showType = true, showSubject = true, showPlace = true, showNotes = false
    ),
    PeritoEventCategory.UNIONCASA.id to CategoryReferenceFormat(
        showTime = false, showType = true, showSubject = true, showPlace = true, showNotes = false
    ),
    PeritoEventCategory.AGENZIA_ORSO.id to CategoryReferenceFormat(
        showTime = false, showType = true, showSubject = true, showPlace = true, showNotes = false
    ),
    PeritoEventCategory.COMUNE.id to CategoryReferenceFormat(
        showTime = false, showType = true, showSubject = true, showPlace = true, showNotes = false
    ),
    PeritoEventCategory.TRIBUNALE.id to CategoryReferenceFormat(
        showTime = false, showType = true, showSubject = true, showPlace = false, showNotes = true
    ),
    PeritoEventCategory.RIUNIONE.id to CategoryReferenceFormat(
        showTime = false, showType = true, showSubject = true, showPlace = true, showNotes = false
    ),
    PeritoEventCategory.CLIENTE.id to CategoryReferenceFormat(
        showTime = false, showType = true, showSubject = true, showPlace = true, showNotes = false
    ),
    PeritoEventCategory.PERSONALE.id to CategoryReferenceFormat(
        showTime = false, showType = true, showSubject = false, showPlace = true, showNotes = false
    ),
    PeritoEventCategory.ALTRO.id to CategoryReferenceFormat(
        showTime = false, showType = true, showSubject = true, showPlace = true, showNotes = false
    )
)

@Immutable
@Serializable
data class SystemCalendarInfo(
    val id: Long,
    val name: String,
    val accountName: String,
    val accountType: String,
    val isPrimary: Boolean = false,
    val color: Int? = null,
    val canWrite: Boolean = true
)

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
    val fascicoloId: String? = null,
    val categoria: String = "Sopralluogo"
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
    val fascicoloId: String? = null,
    val categoria: String = "Sopralluogo"
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
    val selectedCalendarId: Long? = null,
    val selectedCalendarAccount: String? = null,
    val selectedCalendarName: String? = null,
    val otaManifestUrl: String = "https://raw.githubusercontent.com/ManuRoma02/assistente-perito-mobile/main/ota-update.json",
    val autoCheckUpdatesOnStart: Boolean = true,
    val studioProfile: StudioProfile = StudioProfile(),
    val categoryFormats: Map<String, CategoryReferenceFormat> = defaultCategoryFormats()
)
