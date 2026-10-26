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
    CHAT("Assistente PeritoAI"),
    AGENDA("Agenda Sopralluoghi")
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
    APE("ape", "APE", "⚡", "Sopralluogo APE"),
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
                Regex("""\bape\b""", RegexOption.IGNORE_CASE).containsMatchIn(norm) || norm.contains("prestazione energetica") || norm.contains("attestato energetico") -> APE
                norm.contains("unioncasa") || norm.contains("union casa") -> UNIONCASA
                norm.contains("agenzia orso") || Regex("""\borso\b""", RegexOption.IGNORE_CASE).containsMatchIn(norm) -> AGENZIA_ORSO
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
data class PeritoCategoryDefinition(
    val id: String,
    val displayName: String,
    val emoji: String,
    val defaultPrefix: String,
    val keywords: List<String> = emptyList(),
    val isSystem: Boolean = false,
    val colorHex: String? = null
)

fun defaultCategoriesList(): List<PeritoCategoryDefinition> = listOf(
    PeritoCategoryDefinition(
        id = "sopralluogo",
        displayName = "Sopralluogo",
        emoji = "🔍",
        defaultPrefix = "Sopralluogo",
        keywords = listOf("sopralluogo", "visita", "collaudo"),
        isSystem = true
    ),
    PeritoCategoryDefinition(
        id = "rilievo",
        displayName = "Rilievo",
        emoji = "📐",
        defaultPrefix = "Rilievo Tecnico",
        keywords = listOf("rilievo", "rilievi", "misurazioni", "misure"),
        isSystem = true
    ),
    PeritoCategoryDefinition(
        id = "ape",
        displayName = "APE",
        emoji = "⚡",
        defaultPrefix = "Sopralluogo APE",
        keywords = listOf("ape", "a.p.e.", "prestazione energetica", "certificazione energetica", "attestato energetico"),
        isSystem = true
    ),
    PeritoCategoryDefinition(
        id = "comune",
        displayName = "Comune",
        emoji = "🏛️",
        defaultPrefix = "Incontro Comune",
        keywords = listOf("comune", "accesso atti", "accesso agli atti", "ufficio tecnico", "edilizia privata", "cila", "scia", "pdc"),
        isSystem = true
    ),
    PeritoCategoryDefinition(
        id = "tribunale",
        displayName = "Tribunale",
        emoji = "⚖️",
        defaultPrefix = "Udienza / Tribunale",
        keywords = listOf("tribunale", "udienza", "giuramento", "ctu", "giudice", "cancelleria"),
        isSystem = true
    ),
    PeritoCategoryDefinition(
        id = "riunione",
        displayName = "Riunione",
        emoji = "🏢",
        defaultPrefix = "Riunione Studio",
        keywords = listOf("riunione", "studio", "meeting", "ufficio"),
        isSystem = true
    ),
    PeritoCategoryDefinition(
        id = "unioncasa",
        displayName = "Unioncasa",
        emoji = "🏠",
        defaultPrefix = "Incontro Unioncasa",
        keywords = listOf("unioncasa", "union casa"),
        isSystem = true
    ),
    PeritoCategoryDefinition(
        id = "agenzia_orso",
        displayName = "Agenzia Orso",
        emoji = "🐻",
        defaultPrefix = "Appuntamento Agenzia Orso",
        keywords = listOf("agenzia orso", "orso"),
        isSystem = true
    ),
    PeritoCategoryDefinition(
        id = "cliente",
        displayName = "Cliente",
        emoji = "👤",
        defaultPrefix = "Appuntamento Cliente",
        keywords = listOf("cliente", "appuntamento", "chiamata", "firma", "consegna"),
        isSystem = true
    ),
    PeritoCategoryDefinition(
        id = "personale",
        displayName = "Personale",
        emoji = "☕",
        defaultPrefix = "Personale",
        keywords = listOf("colazione", "pranzo", "cena", "caffè", "caffe", "aperitivo"),
        isSystem = true
    ),
    PeritoCategoryDefinition(
        id = "altro",
        displayName = "Altro",
        emoji = "📅",
        defaultPrefix = "Impegno",
        keywords = listOf("impegno"),
        isSystem = true
    )
)

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
    PeritoEventCategory.APE.id to CategoryReferenceFormat(
        showTime = false, showType = true, showSubject = false, showPlace = true, showNotes = false
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
    val categoryFormats: Map<String, CategoryReferenceFormat> = defaultCategoryFormats(),
    val categories: List<PeritoCategoryDefinition> = defaultCategoriesList()
)

typealias PeritoAiConfig = AiEngineConfig

/**
 * Matcher semantico robusto con confini di parola per definizioni di categoria.
 * Rispetta rigorosamente la priorità:
 * 1. Categorie personalizzate utente (!isSystem)
 * 2. Attività tecniche specifiche (ape, rilievo, comune, tribunale)
 * 3. Agenzie partner (unioncasa, agenzia_orso)
 * 4. Studio e personali (personale, riunione)
 * 5. Generiche fallback (sopralluogo, cliente, altro)
 */
fun matchCategoryDefinition(
    text: String,
    categories: List<PeritoCategoryDefinition>
): PeritoCategoryDefinition? {
    if (text.isBlank() || categories.isEmpty()) return null
    val lower = text.lowercase(java.util.Locale.ITALIAN)

    fun matchesKeywords(def: PeritoCategoryDefinition): Boolean {
        val terms = (listOf(def.displayName, def.defaultPrefix) + def.keywords).filter { it.isNotBlank() }
        return terms.any { term ->
            val cleanKw = term.trim().lowercase(java.util.Locale.ITALIAN)
            if (cleanKw.isEmpty()) return@any false
            val startBoundary = if (cleanKw.first().isLetterOrDigit()) """(?<![\p{L}\p{N}_])""" else ""
            val endBoundary = if (cleanKw.last().isLetterOrDigit()) """(?![\p{L}\p{N}_])""" else ""
            Regex("""$startBoundary${Regex.escape(cleanKw)}$endBoundary""", RegexOption.IGNORE_CASE).containsMatchIn(lower)
        }
    }

    // 1. Categorie personalizzate dell'utente (priorità massima)
    val customDefs = categories.filter { !it.isSystem }
    customDefs.firstOrNull { matchesKeywords(it) }?.let { return it }

    // 2. Attività tecniche specifiche (APE, Rilievo, Comune, Tribunale)
    val specificIds = listOf("ape", "rilievo", "comune", "tribunale")
    for (id in specificIds) {
        categories.firstOrNull { it.id == id && matchesKeywords(it) }?.let { return it }
    }

    // 3. Agenzie partner (Unioncasa, Agenzia Orso)
    val agencyIds = listOf("unioncasa", "agenzia_orso")
    for (id in agencyIds) {
        categories.firstOrNull { it.id == id && matchesKeywords(it) }?.let { return it }
    }

    // 4. Attività studio e personali (Personale, Riunione)
    val studioIds = listOf("personale", "riunione")
    for (id in studioIds) {
        categories.firstOrNull { it.id == id && matchesKeywords(it) }?.let { return it }
    }

    // 5. Categorie generiche fallback (Sopralluogo, Cliente, Altro)
    val genericIds = listOf("sopralluogo", "cliente", "altro")
    for (id in genericIds) {
        categories.firstOrNull { it.id == id && matchesKeywords(it) }?.let { return it }
    }

    return null
}

/**
 * Riconosce una correzione esplicita di categoria in linguaggio naturale.
 * Ignora messaggi ordinari contenenti parole generiche come "in realtà", "invece", "è un/una".
 */
fun detectCategoryCorrection(
    text: String,
    categories: List<PeritoCategoryDefinition>
): PeritoCategoryDefinition? {
    if (text.isBlank() || categories.isEmpty()) return null

    val isExplicitCommand = Regex(
        """\b(?:cambia(?:re)?|modifica(?:re)?|imposta(?:re)?|correggi(?:re)?|aggiorna(?:re)?)\s+(?:la\s+)?categoria\b""" +
        """|\bcategoria\s*:\s*""" +
        """|\b(?:cambia|modifica|imposta|correggi|aggiorna)\s+in\s+""",
        RegexOption.IGNORE_CASE
    ).containsMatchIn(text)

    val isExplicitCorrection = Regex(
        """\b(?:non\s+è\s+(?:un['\s]|una\s+|in\s+)[^\n,;]+?(?:,\s*|\s+)(?:ma|invece|anzi|è)\s+(?:un['\s]|una\s+|in\s+)?\b)""" +
        """|\b(?:in\s+realtà|invece|anzi)\s+(?:è|si\s+tratta\s+di)\s+(?:un['\s]|una\s+|in\s+)?\b""" +
        """|\bè\s+invece\s+(?:un['\s]|una\s+|in\s+)?\b""" +
        """|(?<![\p{L}\p{N}_])è\s+(?:un\s+appuntamento\s+in\s+comune|in\s+comune|in\s+tribunale|una\s+riunione|un\s+sopralluogo)\b""",
        RegexOption.IGNORE_CASE
    ).containsMatchIn(text)

    if (!isExplicitCommand && !isExplicitCorrection) return null

    val contrastMatch = Regex(
        """\bnon\s+è\s+.*?(?:,\s*|\s+)(?:ma|invece|anzi|è)\s+(?:un['\s]|una\s+|in\s+)?(.*)$""",
        RegexOption.IGNORE_CASE
    ).find(text)
    val targetSegment = contrastMatch?.groupValues?.get(1)?.trim() ?: text

    return matchCategoryDefinition(targetSegment, categories)
}

/**
 * Risolve la coppia visiva (emoji, displayName) per un appuntamento in agenda.
 * Utilizzato da NextWeekExpandableEventCard e AgendaEventCard per evitare
 * false classificazioni su sottostringhe casuali, supportando categorie dinamiche e fallback legacy.
 */
fun resolveEventCategoryVisuals(
    categoria: String,
    titolo: String,
    categories: List<PeritoCategoryDefinition> = emptyList()
): Pair<String, String> {
    val catStr = categoria.ifBlank { titolo }.trim()
    if (catStr.isBlank()) {
        val defaultFallback = PeritoEventCategory.SOPRALLUOGO
        return defaultFallback.emoji to defaultFallback.displayName
    }

    // 1. Match esatto su id o displayName (priorità categorie personalizzate)
    val exactCustom = categories.filter { !it.isSystem }.firstOrNull {
        it.id.equals(catStr, ignoreCase = true) || it.displayName.equals(catStr, ignoreCase = true)
    }
    if (exactCustom != null) return exactCustom.emoji to exactCustom.displayName

    val exactSystem = categories.filter { it.isSystem }.firstOrNull {
        it.id.equals(catStr, ignoreCase = true) || it.displayName.equals(catStr, ignoreCase = true)
    }
    if (exactSystem != null) return exactSystem.emoji to exactSystem.displayName

    // 2. Match semantico con confini di parola e rispetto delle priorità
    val matched = matchCategoryDefinition(catStr, categories)
    if (matched != null) {
        return matched.emoji to matched.displayName
    }

    // 3. Fallback legacy enum
    val fallback = PeritoEventCategory.fromString(catStr)
    return fallback.emoji to fallback.displayName
}

@Immutable
data class ReasoningModelResolution(
    val selectedModelId: String,
    val resolvedFile: java.io.File?,
    val isFallback: Boolean,
    val effectiveModelId: String?,
    val effectiveLabel: String
)
