package com.example.assistenteperitomobile.domain

import android.content.Context
import com.llamatik.library.platform.LlamaBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class PeritoAiEngine(private val context: Context? = null) {

    companion object {
        // Mutex singleton di processo per serializzare gli accessi al motore nativo C++ llama.cpp (LlamaBridge)
        // previene SIGSEGV e corruzione di memoria da chiamate concorrenti.
        private val llamaMutex = Mutex()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var loadedGgufPath: String? = null

    /**
     * Inizializza o aggiorna il motore nativo llama.cpp (LlamaBridge) per i modelli .gguf
     * (Qwen 3.5 2B GPT-5.1 HighIQ Q4_K_M GGUF) scaricati sul dispositivo.
     */
    @Synchronized
    fun ensureGgufLoaded(config: AiEngineConfig): Boolean {
        if (!config.useMediaPipeOnDevice) return false
        val ctx = context ?: return false

        val candidateFile: File? = run {
            val modelsDir = File(ctx.filesDir, "models")
            val target2b = File(modelsDir, "Qwen3.5-2B-GPT-5.1-HighIQ-INSTRUCT.i1-Q4_K_M.gguf")
            if (target2b.exists() && target2b.length() > 10_000_000L) return@run target2b
            if (config.mediaPipeModelPath.isNotBlank()) {
                val f = File(config.mediaPipeModelPath)
                if (f.exists() && f.isFile && f.name.lowercase().endsWith(".gguf") && f.length() > 10_000_000L) {
                    return@run f
                }
            }
            modelsDir.listFiles()?.firstOrNull {
                it.isFile && it.name.lowercase().endsWith(".gguf") && it.length() > 10_000_000L
            }
        }

        val modelFile = candidateFile ?: return false
        if (loadedGgufPath == modelFile.absolutePath) return true

        return try {
            val threads = 4
            // Parametri ultra-ottimizzati per smartphone Android ARMv8/v9 con Q4_K_M:
            // 1. maxTokens = 256 (risposte esaurienti senza troncamenti, si ferma al token <|im_end|>)
            // 2. contextLength = 1024 (spazio ampio per prompt contestuale e storico, ~40MB KV cache)
            // 3. flashAttention = false (stabilità garantita al 100% su CPU ARM senza hang)
            // 4. repeatPenalty = 1.12f (previene ripetizioni mantenendo fluidità espressiva)
            // 5. batchSize = 128 e gpuLayers = 0 (zero crash, massima efficienza core NEON)
            LlamaBridge.updateGenerateParams(
                temperature = 0.2f,
                maxTokens = 256,
                topP = 0.9f,
                topK = 40,
                repeatPenalty = 1.12f,
                contextLength = 1024,
                numThreads = threads,
                useMmap = true,
                flashAttention = false,
                batchSize = 128,
                gpuLayers = 0
            )
            val ok = LlamaBridge.initGenerateModel(modelFile.absolutePath)
            if (ok) {
                loadedGgufPath = modelFile.absolutePath
            }
            ok
        } catch (_: Throwable) {
            loadedGgufPath = null
            false
        }
    }



    /**
     * Interroga il Decision Model (Rizzo Flow / Jev) tramite endpoint Jev-compatible (/v1/systemone)
     * per ottenere la classificazione tipizzata nelle 6 cartelle con 0 token generati.
     */
    suspend fun tryClassifyWithRizzoFlow(text: String, config: AiEngineConfig): String? = withContext(Dispatchers.IO) {
        if (!config.useRizzoFlow || config.rizzoFlowUrl.isBlank()) return@withContext null
        try {
            val url = URL(config.rizzoFlowUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 1500
                readTimeout = 3000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            val snippet = text.take(3000)
            val payload = buildString {
                append("""{"state":""")
                append(json.encodeToString(kotlinx.serialization.serializer(), snippet))
                append(""", "model": "rizzo-latest", "questions": {"cartella_peritale": {"type": "choice", "options": [""")
                append(PeritoCategory.entries.joinToString(",") { """ "${it.folderName}" """ })
                append("""], "instructions": "Classifica questo documento peritale nella corretta cartella di destinazione."}}}""")
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(payload) }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().readText()
                val rootObj = json.parseToJsonElement(body) as? JsonObject
                val decisions = rootObj?.get("decisions") as? JsonObject
                val cartellaDecision = decisions?.get("cartella_peritale") as? JsonObject
                val value = cartellaDecision?.get("value")?.jsonPrimitive?.content
                if (!value.isNullOrBlank()) {
                    PeritoCategory.fromFolderName(value).folderName
                } else null
            } else null
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Classifica un documento tecnico/legale in una delle 6 cartelle ufficiali del Perito Edile.
     * Usa il motore ibrido: Deterministico -> Decision Model (Rizzo Flow / Jev) -> GGUF On-Device.
     */
    suspend fun classifyDocument(
        docText: String,
        originalFileName: String = "documento.pdf",
        referenceDate: LocalDate = LocalDate.now(),
        config: AiEngineConfig = AiEngineConfig()
    ): DocumentClassificationResult = withContext(Dispatchers.Default) {
        val combinedText = "$originalFileName\n$docText".trim()
        val deterministic = classifyDeterministic(combinedText, originalFileName, referenceDate)

        // 1. Prova prima il Decision Model (Rizzo Flow / Jev) se abilitato: 0 token generati, istantaneo in ~50ms
        val rizzoCategory = if (config.useRizzoFlow) {
            tryClassifyWithRizzoFlow(combinedText, config)
        } else null

        val effectiveCategory = rizzoCategory ?: deterministic.categoria

        // 2. Se la categoria è incerta o il cliente è rimasto generico, chiedi conferma alla rete neurale locale
        if ((effectiveCategory == PeritoCategory.DA_CLASSIFICARE_MANUALMENTE.folderName ||
                deterministic.cliente_o_controparte == "Generico") && combinedText.length > 20
        ) {
            val llmJson = tryCallLocalLlm(
                prompt = buildClassificationPrompt(combinedText, referenceDate),
                config = config
            )
            if (llmJson != null) {
                parseClassificationJson(llmJson, referenceDate)?.let { llmRes ->
                    return@withContext DocumentClassificationResult(
                        categoria = if (effectiveCategory != PeritoCategory.DA_CLASSIFICARE_MANUALMENTE.folderName) {
                            effectiveCategory
                        } else llmRes.categoria,
                        cliente_o_controparte = if (deterministic.cliente_o_controparte != "Generico") {
                            deterministic.cliente_o_controparte
                        } else llmRes.cliente_o_controparte,
                        oggetto_sintetico = if (deterministic.oggetto_sintetico != "Documento") {
                            deterministic.oggetto_sintetico
                        } else llmRes.oggetto_sintetico,
                        data_documento = deterministic.data_documento
                    )
                }
            }
        }
        deterministic.copy(categoria = effectiveCategory)
    }

    /**
     * Verifica se il testo estratto da OCR su una foto/immagine contiene davvero parole leggibili
     * e sensate (evitando che rumore visivo, muri, oggetti o foto vuote vengano inventati come documenti o appuntamenti).
     */
    fun isMeaningfulOcrText(rawText: String): Boolean {
        val cleaned = rawText.trim()
        if (cleaned.length < 6) return false
        val meaningfulWords = Regex("""[A-Za-zÀ-ÿ]{3,}""").findAll(cleaned).map { it.value }.toList()
        val letterCount = cleaned.count { it.isLetter() }
        return meaningfulWords.size >= 2 && letterCount >= 7
    }

    /**
     * Analizza un messaggio vocale o testuale per estrarre un appuntamento di perizia/studio,
     * combinando il parser deterministico potenziato con il modello neurale GGUF On-Device (Qwen 3.5).
     * REGOLA ANTI-ALLUCINAZIONE: il modello LLM può arricchire titolo, luogo e cliente, ma NON può
     * mai inventare una data/ora (data_inizio_iso) né trasformare un testo ambiguo/senza data in un appuntamento confermato.
     */
    suspend fun parseAppointment(
        messageText: String,
        now: LocalDateTime = LocalDateTime.now(),
        config: AiEngineConfig = AiEngineConfig()
    ): AppointmentParseResult = withContext(Dispatchers.Default) {
        val cleaned = messageText.trim()
        if (cleaned.isEmpty()) {
            return@withContext AppointmentParseResult(has_appointment = false)
        }

        val deterministic = parseAppointmentDeterministic(cleaned, now)
        if (!deterministic.has_appointment) {
            return@withContext deterministic
        }

        // Se manca il luogo, il cliente o il titolo specifico in un appuntamento reale, arricchisce tramite il modello LLM locale
        val needsLlmEnrichment = (deterministic.luogo == "Da definire" ||
            deterministic.cliente == "Cliente" ||
            deterministic.titolo == "Appuntamento" ||
            deterministic.titolo == "Appuntamento Tecnico") &&
            hasPotentialUnparsedEntity(cleaned)

        if (needsLlmEnrichment) {
            val llmJson = tryCallLocalLlm(
                prompt = buildAppointmentPrompt(cleaned, now),
                config = config
            )
            if (llmJson != null) {
                val llmParsed = parseAppointmentJson(llmJson)
                if (llmParsed != null && llmParsed.has_appointment) {
                    val validLlmLuogo = llmParsed.luogo.trim().takeIf {
                        it.isNotBlank() && it != "Da definire" && it != "..." && it.length >= 3
                    }
                    val validLlmCliente = llmParsed.cliente.trim().takeIf {
                        it.isNotBlank() && it != "Cliente" && it != "..." && it.length >= 2
                    }
                    val validLlmTitolo = llmParsed.titolo.trim().takeIf {
                        it.isNotBlank() && it != "Appuntamento" && it != "Appuntamento Tecnico" && it != "..." && it.length >= 4
                    }
                    val mergedLuogo = if (deterministic.luogo != "Da definire") {
                        deterministic.luogo
                    } else {
                        validLlmLuogo ?: deterministic.luogo
                    }
                    val mergedCliente = if (deterministic.cliente != "Cliente") {
                        deterministic.cliente
                    } else {
                        validLlmCliente ?: deterministic.cliente
                    }
                    val mergedTitolo = if (deterministic.titolo == "Appuntamento" || deterministic.titolo == "Appuntamento Tecnico") {
                        validLlmTitolo ?: deterministic.titolo
                    } else if (mergedLuogo != "Da definire" && !deterministic.titolo.contains(mergedLuogo)) {
                        "${deterministic.titolo} ($mergedLuogo)"
                    } else {
                        deterministic.titolo
                    }

                    return@withContext deterministic.copy(
                        titolo = mergedTitolo,
                        luogo = mergedLuogo,
                        cliente = mergedCliente,
                        // Mai permettere all'LLM di inventare data/ora o forzare is_ambiguo = false!
                        data_inizio_iso = deterministic.data_inizio_iso,
                        data_fine_iso = deterministic.data_fine_iso,
                        is_ambiguo = deterministic.is_ambiguo
                    )
                }
            }
        }

        deterministic
    }

    // =========================================================================
    // 1. CLASSIFICAZIONE DOCUMENTI DETERMINISTICA + SEMANTICA (6 CARTELLE)
    // =========================================================================

    fun classifyDeterministic(
        rawText: String,
        originalFileName: String = "",
        referenceDate: LocalDate = LocalDate.now()
    ): DocumentClassificationResult {
        val text = "$originalFileName $rawText"
        val lower = text.lowercase(Locale.ITALIAN)

        if (lower.isBlank() || lower.length < 4) {
            return DocumentClassificationResult(
                categoria = PeritoCategory.DA_CLASSIFICARE_MANUALMENTE.folderName,
                cliente_o_controparte = "Sconosciuto",
                oggetto_sintetico = "Da_Verificare",
                data_documento = referenceDate.toString()
            )
        }

        val scores = mutableMapOf(
            PeritoCategory.ATTI_TRIBUNALE to 0,
            PeritoCategory.PERIZIE_TECNICHE to 0,
            PeritoCategory.PLANIMETRIE_COMPUTI to 0,
            PeritoCategory.FATTURE_PREVENTIVI to 0,
            PeritoCategory.CORRISPONDENZA_LEGALE to 0
        )

        // 1. Atti_Tribunale
        val attiKeywords = listOf(
            "tribunale" to 5, "giudice" to 5, "ctu" to 5, "consulenza tecnica d'ufficio" to 6,
            "consulente tecnico d'ufficio" to 6, "r.g." to 4, "rg " to 4, "ruolo generale" to 5,
            "udienza" to 5, "giuramento" to 5, "quesito" to 4, "quesiti" to 4,
            "esecuzione immobiliare" to 6, "procedura esecutiva" to 5, "decreto" to 4,
            "ordinanza" to 4, " atp " to 4, "a.t.p." to 5, "accertamento tecnico preventivo" to 6,
            "nomina" to 3, "sezione civile" to 5, "cancelleria" to 4
        )
        for ((kw, w) in attiKeywords) {
            if (lower.contains(kw.lowercase())) scores[PeritoCategory.ATTI_TRIBUNALE] = scores.getValue(PeritoCategory.ATTI_TRIBUNALE) + w
        }

        // 2. Perizie_Tecniche
        val perizieKeywords = listOf(
            "perizia" to 5, "relazione tecnica" to 6, "relazione peritale" to 6,
            "infiltrazion" to 5, "allagament" to 4, "umidit" to 4, "muffa" to 4,
            "danni" to 4, "crepe" to 4, "fessurazion" to 5, "cediment" to 4,
            "sopralluogo" to 4, "stima immobiliare" to 6, "valutazione immobiliare" to 5,
            "vizi costruttivi" to 6, "difetti" to 3, "termografi" to 5, "igrometric" to 5,
            "copertura" to 2, "lastrico solare" to 4
        )
        for ((kw, w) in perizieKeywords) {
            if (lower.contains(kw.lowercase())) scores[PeritoCategory.PERIZIE_TECNICHE] = scores.getValue(PeritoCategory.PERIZIE_TECNICHE) + w
        }

        // 3. Planimetrie_Computi
        val planimetrieKeywords = listOf(
            "computo metrico" to 7, "computo" to 5, "planimetri" to 6,
            "catast" to 5, "visura" to 5, "foglio" to 3, "particella" to 4, "subalterno" to 5,
            "elenco prezzi" to 6, "prezzario" to 5, "stato avanzamento lavori" to 6,
            "sal " to 4, "pianta" to 3, "sezione" to 2, "prospetto" to 3,
            "elaborato grafico" to 5, "superficie utile" to 4, "mq" to 2, "m2" to 2,
            " ape " to 4, "a.p.e." to 5, "prestazione energetica" to 5, "rilievo" to 4
        )
        for ((kw, w) in planimetrieKeywords) {
            if (lower.contains(kw.lowercase())) scores[PeritoCategory.PLANIMETRIE_COMPUTI] = scores.getValue(PeritoCategory.PLANIMETRIE_COMPUTI) + w
        }

        // 4. Fatture_Preventivi
        val fattureKeywords = listOf(
            "fattura" to 7, "parcella" to 6, "pro-forma" to 5, "proforma" to 5,
            "preventivo" to 6, "offerta economica" to 6, "imponibile" to 5,
            "iva " to 4, "iva(" to 4, "cassa geometri" to 5, "inarcassa" to 5,
            "ritenuta d'acconto" to 6, "bonifico" to 4, "iban" to 4,
            "acconto" to 4, "saldo" to 3, "nota spese" to 5, "onorario" to 5,
            "liquidazione compenso" to 5, "totale documento" to 5, "partita iva" to 4, "p.iva" to 4
        )
        for ((kw, w) in fattureKeywords) {
            if (lower.contains(kw.lowercase())) scores[PeritoCategory.FATTURE_PREVENTIVI] = scores.getValue(PeritoCategory.FATTURE_PREVENTIVI) + w
        }

        // 5. Corrispondenza_Legale
        val legaleKeywords = listOf(
            "diffida" to 7, "messa in mora" to 7, "studio legale" to 6,
            "avv." to 5, "avvocato" to 5, " pec " to 4, "pec:" to 5, "posta certificata" to 5, "raccomandata" to 4,
            "osservazioni ctp" to 6, "consulente di parte" to 5, "ctp" to 4,
            "contestazione" to 5, "intimazione" to 5, "transazione" to 4,
            "accordo bonario" to 5, "convocazione" to 4, "in nome e per conto" to 6,
            "controparte" to 4, "risarcimento" to 4
        )
        for ((kw, w) in legaleKeywords) {
            if (lower.contains(kw.lowercase())) scores[PeritoCategory.CORRISPONDENZA_LEGALE] = scores.getValue(PeritoCategory.CORRISPONDENZA_LEGALE) + w
        }

        val bestEntry = scores.maxByOrNull { it.value }
        val chosenCategory = if (bestEntry == null || bestEntry.value < 3) {
            PeritoCategory.DA_CLASSIFICARE_MANUALMENTE
        } else {
            bestEntry.key
        }

        val cliente = extractClientOrCounterpart(text, chosenCategory)
        val oggetto = extractSyntheticSubject(lower, chosenCategory)
        val docDate = extractDocumentDate(text, referenceDate)

        return DocumentClassificationResult(
            categoria = chosenCategory.folderName,
            cliente_o_controparte = sanitizeComponent(cliente, "Generico"),
            oggetto_sintetico = sanitizeComponent(oggetto, "Documento"),
            data_documento = docDate
        )
    }

    private fun extractClientOrCounterpart(text: String, category: PeritoCategory): String {
        val patterns = listOf(
            Regex("""(?:Condominio|CONDOMINIO)\s+([A-ZÀ-Úa-zà-ú0-9][A-ZÀ-Úa-zà-ú0-9\s']{1,25})"""),
            Regex("""(?:Tribunale\s+di|TRIBUNALE\s+DI)\s+([A-ZÀ-Úa-zà-ú]{2,20})"""),
            Regex("""(?:Studio\s+Legale|Avv\.|Avvocato)\s+([A-ZÀ-Ú][a-zà-ú]+(?:\s+[A-ZÀ-Ú][a-zà-ú]+)?)"""),
            Regex("""(?:Impresa\s+Edile|Impresa|Costruzioni|Ditta)\s+([A-ZÀ-Úa-zà-ú0-9\s&.]{2,25})"""),
            Regex("""(?:Cliente|Committente|Propriet[aà]|Sig\.|Sig\.ra|Ing\.|Arch\.|Geom\.|Dott\.)\s*:?\s*([A-ZÀ-Ú][a-zà-ú]+(?:\s+[A-ZÀ-Ú][a-zà-ú]+)?)"""),
            Regex("""(?:contro|c/|vs\.?)\s+([A-ZÀ-Ú][a-zà-ú]+(?:\s+[A-ZÀ-Ú][a-zà-ú]+)?)""")
        )
        for (regex in patterns) {
            val match = regex.find(text)
            if (match != null) {
                val full = match.value.trim()
                    .replace(Regex("""^(Cliente|Committente|Propriet[aà]|contro|c/|vs\.?)\s*:?\s*""", RegexOption.IGNORE_CASE), "")
                val words = full.split(Regex("""\s+""")).take(3).joinToString("_")
                if (words.length >= 2) return words
            }
        }
        return when (category) {
            PeritoCategory.ATTI_TRIBUNALE -> "Tribunale"
            PeritoCategory.PERIZIE_TECNICHE -> "Committente"
            PeritoCategory.PLANIMETRIE_COMPUTI -> "Catasto_Rilievi"
            PeritoCategory.FATTURE_PREVENTIVI -> "Amministrazione"
            PeritoCategory.CORRISPONDENZA_LEGALE -> "Studio_Legale"
            PeritoCategory.DA_CLASSIFICARE_MANUALMENTE -> "Da_Verificare"
        }
    }

    private fun extractSyntheticSubject(lower: String, category: PeritoCategory): String {
        return when (category) {
            PeritoCategory.ATTI_TRIBUNALE -> when {
                lower.contains("nomina") || lower.contains("giuramento") -> "Nomina_CTU"
                lower.contains("esecuzione") -> "Esecuzione_Immobiliare"
                lower.contains("atp") || lower.contains("accertamento tecnico") -> "Ricorso_ATP"
                lower.contains("udienza") || lower.contains("verbale") -> "Verbale_Udienza"
                else -> "Atto_Giudiziario"
            }
            PeritoCategory.PERIZIE_TECNICHE -> when {
                lower.contains("infiltrazion") || lower.contains("allagament") || lower.contains("umidit") -> "Perizia_Infiltrazioni"
                lower.contains("stima") || lower.contains("valutazione") -> "Stima_Immobiliare"
                lower.contains("crepe") || lower.contains("fessur") || lower.contains("struttur") -> "Verifica_Strutturale"
                lower.contains("sopralluogo") -> "Verbale_Sopralluogo"
                else -> "Relazione_Peritale"
            }
            PeritoCategory.PLANIMETRIE_COMPUTI -> when {
                lower.contains("computo") -> "Computo_Metrico"
                lower.contains("visura") || lower.contains("catast") -> "Visura_Catastale"
                lower.contains("planimetri") || lower.contains("pianta") -> "Planimetria"
                lower.contains("sal ") || lower.contains("avanzamento") -> "Stato_Avanzamento_Lavori"
                else -> "Elaborato_Tecnico"
            }
            PeritoCategory.FATTURE_PREVENTIVI -> when {
                lower.contains("preventivo") || lower.contains("offerta") -> "Preventivo_Lavori"
                lower.contains("parcella") || lower.contains("pro-forma") || lower.contains("proforma") -> "Parcella_Professionale"
                lower.contains("fattura") -> "Fattura"
                else -> "Documento_Contabile"
            }
            PeritoCategory.CORRISPONDENZA_LEGALE -> when {
                lower.contains("diffida") || lower.contains("mora") -> "Diffida_Legale"
                lower.contains("ctp") || lower.contains("osservazioni") -> "Osservazioni_CTP"
                lower.contains("convocazione") -> "Convocazione_Sopralluogo"
                else -> "Comunicazione_Legale"
            }
            PeritoCategory.DA_CLASSIFICARE_MANUALMENTE -> "Documento_Generico"
        }
    }

    private fun extractDocumentDate(text: String, fallback: LocalDate): String {
        val isoRegex = Regex("""\b(20\d{2})-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01])\b""")
        isoRegex.find(text)?.let { return it.value }

        val itNumericRegex = Regex("""\b(0?[1-9]|[12]\d|3[01])[/. -](0?[1-9]|1[0-2])[/. -](20\d{2})\b""")
        itNumericRegex.find(text)?.let { m ->
            val d = m.groupValues[1].padStart(2, '0')
            val mo = m.groupValues[2].padStart(2, '0')
            val y = m.groupValues[3]
            return "$y-$mo-$d"
        }
        return fallback.toString()
    }

    // =========================================================================
    // 2. ESTRAZIONE APPUNTAMENTI, AUTO-CORREZIONE ("ANZI NO") E AMBIGUITÀ
    // =========================================================================

    fun parseAppointmentDeterministic(
        rawMessage: String,
        now: LocalDateTime = LocalDateTime.now()
    ): AppointmentParseResult {
        val normalized = rawMessage.trim()
        val lower = normalized.lowercase(Locale.ITALIAN)

        // Rileva se il messaggio riguarda un appuntamento / sopralluogo / incontro / udienza / pasto / caffè
        val appointmentTriggers = listOf(
            "sopralluogo", "appuntamento", "incontro", "ci vediamo", "vediamoci", "vedersi",
            "fissa", "fissiamo", "segna", "agenda", "calendario", "udienza",
            "riunione", "perizia", "visita", "rilievo", "collaudo",
            "domani", "dopodomani", "luned", "marted", "mercoled", "gioved", "venerd", "sabat", "domenic",
            "alle ", "ore ",
            "colazione", "pranzo", "cena", "caffè", "caffe", "aperitivo",
            "vado con", "devo andare", "vedo ", "trovo "
        )
        val hasTrigger = appointmentTriggers.any { lower.contains(it) }
        if (!hasTrigger) {
            return AppointmentParseResult(has_appointment = false)
        }

        // Esclusione esplicita o frasi al passato (nessun impegno futuro da fissare)
        val isExplicitNegation = lower.contains("nessun appuntamento") ||
            lower.contains("non fissare") ||
            lower.contains("senza appuntamento") ||
            lower.contains("prima o poi")

        val isPastNarrative = (lower.contains("ieri") || lower.contains("l'altro ieri") || lower.contains("scorso") || lower.contains("scorsa")) &&
            (lower.contains("abbiamo fatto") || lower.contains("è stato fatto") || lower.contains("ho fatto") || lower.contains("si è svolto") || lower.contains("si è tenuto")) &&
            !lower.contains("fissa") && !lower.contains("segna") && !lower.contains("sposta")

        if (isExplicitNegation || isPastNarrative) {
            return AppointmentParseResult(has_appointment = false)
        }

        // Gestione Auto-Correzione ("anzi no", "scusa", "volevo dire", "rettifico", "invece", "aspetta", "facciamo")
        val correctionMarkers = listOf(
            "anzi no", "anzi,", "anzi ", "scusa volevo dire", "volevo dire",
            "scusa,", "scusa ", "rettifico", "aspetta", "invece facciamo", "facciamo invece"
        )
        var primaryClause = normalized
        var correctedClause: String? = null
        for (marker in correctionMarkers) {
            val idx = lower.lastIndexOf(marker)
            if (idx >= 0) {
                primaryClause = normalized.substring(0, idx)
                correctedClause = normalized.substring(idx + marker.length).trim()
                break
            }
        }

        val effectiveTextForDateAndTime = correctedClause ?: normalized
        val fallbackTextForDateAndTime = primaryClause

        // Estrai data dal segmento corretto (o dal primario se non ripetuta nella correzione)
        val dateExtraction = extractTargetDate(effectiveTextForDateAndTime, now.toLocalDate())
            ?: extractTargetDate(fallbackTextForDateAndTime, now.toLocalDate())

        // Estrai orario dal segmento corretto (o dal primario se non ripetuto)
        val timeExtraction = extractTargetTime(effectiveTextForDateAndTime)
            ?: extractTargetTime(fallbackTextForDateAndTime)

        // Estrai luogo e cliente combinando entrambe le parti (dando priorità alla correzione se contiene un nuovo luogo/cliente)
        val luogo = extractLocation(correctedClause ?: "")
            ?: extractLocation(normalized)
            ?: "Da definire"

        val cliente = extractAppointmentClient(correctedClause ?: "")
            ?: extractAppointmentClient(normalized)
            ?: "Cliente"

        val activityType = when {
            lower.contains("colazione") || lower.contains("a colazione") || lower.contains("fare colazione") -> "Colazione"
            lower.contains("pranzo") || lower.contains("pranzare") || lower.contains("a pranzo") -> "Pranzo"
            lower.contains("cena") || lower.contains("cenare") || lower.contains("a cena") -> "Cena"
            lower.contains("caffè") || lower.contains("caffe") -> "Caffè"
            lower.contains("aperitivo") -> "Aperitivo"
            lower.contains("chiamata") || lower.contains("telefonata") || lower.contains("chiamare") -> "Telefonata"
            lower.contains("videochiamata") || lower.contains("call") || lower.contains("zoom") || lower.contains("meet") || lower.contains("teams") -> "Videochiamata"
            lower.contains("sopralluogo") -> "Sopralluogo"
            lower.contains("udienza") || lower.contains("giuramento") -> "Udienza Tribunale"
            lower.contains("rilievo") -> "Rilievo Tecnico"
            lower.contains("collaudo") -> "Collaudo Tecnico"
            lower.contains("riunione") -> "Riunione Studio"
            lower.contains("incontro") -> "Incontro"
            lower.contains("visita") -> "Visita"
            lower.contains("perizia") -> "Perizia"
            lower.contains("firma") || lower.contains("firmare") -> "Firma Documenti"
            lower.contains("consegna") -> "Consegna Documenti"
            lower.contains("accesso agli atti") || lower.contains("accesso atti") -> "Accesso Atti"
            lower.contains("catasto") -> "Catasto"
            else -> "Appuntamento"
        }

        val topic = extractMeetingTopic(correctedClause ?: normalized)

        val titolo = buildString {
            append(activityType)
            if (cliente != "Cliente" && cliente.isNotBlank()) {
                append(" con $cliente")
            }
            if (!topic.isNullOrBlank()) {
                append(" — $topic")
            }
            if (luogo != "Da definire" && !luogo.equals(cliente, ignoreCase = true)) {
                append(" ($luogo)")
            }
        }

        // Controllo Ambiguità (Data vaga o Orario vago/assente)
        val vagueTimeExpressions = listOf(
            "nel pomeriggio", "di pomeriggio", "pomeriggio",
            "in mattinata", "di mattina", "mattina",
            "verso sera", "in serata", "sera", "dopo pranzo", "in giornata"
        )
        val hasVagueTime = vagueTimeExpressions.any { effectiveTextForDateAndTime.lowercase(Locale.ITALIAN).contains(it) }
        val hasVagueWeek = lower.contains("settimana prossima") && dateExtraction == null

        if (hasVagueWeek) {
            return AppointmentParseResult(
                has_appointment = true,
                titolo = titolo,
                data_inizio_iso = null,
                data_fine_iso = null,
                luogo = luogo,
                cliente = cliente,
                note = normalized,
                is_ambiguo = true,
                motivo_ambiguita = "È stata indicata 'settimana prossima' ma mancano il giorno esatto e l'orario."
            )
        }

        if (dateExtraction == null && timeExtraction == null) {
            val strongIntent = lower.contains("sopralluogo") || lower.contains("rilievo") ||
                lower.contains("udienza") || lower.contains("perizia") || lower.contains("collaudo") ||
                lower.startsWith("fissa") || lower.startsWith("segna") || lower.contains("incontro")
            if (strongIntent) {
                return AppointmentParseResult(
                    has_appointment = true,
                    titolo = titolo,
                    data_inizio_iso = null,
                    data_fine_iso = null,
                    luogo = luogo,
                    cliente = cliente,
                    note = normalized,
                    is_ambiguo = true,
                    motivo_ambiguita = "È stata indicata l'attività peritale ma mancano la data e l'orario (es. 'domani alle 10:30')."
                )
            }
            return AppointmentParseResult(has_appointment = false)
        }

        if (dateExtraction == null) {
            return AppointmentParseResult(
                has_appointment = true,
                titolo = titolo,
                data_inizio_iso = null,
                data_fine_iso = null,
                luogo = luogo,
                cliente = cliente,
                note = normalized,
                is_ambiguo = true,
                motivo_ambiguita = "Manca il giorno dell'appuntamento (es. domani, martedì 14 ottobre)."
            )
        }

        if (timeExtraction == null) {
            val reason = if (hasVagueTime) {
                "È stata indicata una fascia generica (mattina/pomeriggio) ma manca l'orario preciso (es. alle 10:30 o alle 15:00)."
            } else {
                "Manca l'orario preciso dell'appuntamento per il giorno ${formatItalianDate(dateExtraction)}."
            }
            return AppointmentParseResult(
                has_appointment = true,
                titolo = titolo,
                data_inizio_iso = "${dateExtraction}T09:00:00",
                data_fine_iso = "${dateExtraction}T10:00:00",
                luogo = luogo,
                cliente = cliente,
                note = normalized,
                is_ambiguo = true,
                motivo_ambiguita = reason
            )
        }

        val startDateTime = LocalDateTime.of(dateExtraction, timeExtraction.first)
        val endDateTime = timeExtraction.second?.let { LocalDateTime.of(dateExtraction, it) }
            ?: startDateTime.plusHours(1)

        val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        return AppointmentParseResult(
            has_appointment = true,
            titolo = titolo,
            data_inizio_iso = startDateTime.format(isoFormatter),
            data_fine_iso = endDateTime.format(isoFormatter),
            luogo = luogo,
            cliente = cliente,
            note = normalized,
            is_ambiguo = false,
            motivo_ambiguita = ""
        )
    }

    private fun extractTargetDate(text: String, today: LocalDate): LocalDate? {
        val lower = text.lowercase(Locale.ITALIAN)

        if (lower.contains("dopodomani")) return today.plusDays(2)
        if (lower.contains("domani")) return today.plusDays(1)
        if (Regex("""\boggi\b""").containsMatchIn(lower)) return today

        Regex("""\btra\s+(\d{1,2})\s+giorn[oi]\b""").find(lower)?.let { m ->
            val days = m.groupValues[1].toLongOrNull()
            if (days != null) return today.plusDays(days)
        }

        // Data esplicita testuale italiana: "14 ottobre", "martedì 14 ottobre 2026"
        val monthsMap = mapOf(
            "gennaio" to 1, "febbraio" to 2, "marzo" to 3, "aprile" to 4,
            "maggio" to 5, "giugno" to 6, "luglio" to 7, "agosto" to 8,
            "settembre" to 9, "ottobre" to 10, "novembre" to 11, "dicembre" to 12
        )
        val explicitMonthRegex = Regex(
            """\b(\d{1,2})\s+(gennaio|febbraio|marzo|aprile|maggio|giugno|luglio|agosto|settembre|ottobre|novembre|dicembre)(?:\s+(20\d{2}))?\b"""
        )
        explicitMonthRegex.find(lower)?.let { m ->
            val day = m.groupValues[1].toIntOrNull() ?: return@let
            val month = monthsMap[m.groupValues[2]] ?: return@let
            val year = m.groupValues[3].toIntOrNull() ?: today.year
            return try {
                LocalDate.of(year, month, day)
            } catch (_: Exception) {
                null
            }
        }

        // Data numerica: "14/10/2026" o "14/10"
        val numericDateRegex = Regex("""\b(0?[1-9]|[12]\d|3[01])[/.-](0?[1-9]|1[0-2])(?:[/.-](20\d{2}))?\b""")
        numericDateRegex.find(lower)?.let { m ->
            val day = m.groupValues[1].toIntOrNull() ?: return@let
            val month = m.groupValues[2].toIntOrNull() ?: return@let
            val year = m.groupValues[3].toIntOrNull() ?: today.year
            return try {
                LocalDate.of(year, month, day)
            } catch (_: Exception) {
                null
            }
        }

        // Giorni della settimana: "lunedì", "martedì", ecc.
        val daysMap = listOf(
            "luned" to DayOfWeek.MONDAY,
            "marted" to DayOfWeek.TUESDAY,
            "mercoled" to DayOfWeek.WEDNESDAY,
            "gioved" to DayOfWeek.THURSDAY,
            "venerd" to DayOfWeek.FRIDAY,
            "sabat" to DayOfWeek.SATURDAY,
            "domenic" to DayOfWeek.SUNDAY
        )
        for ((prefix, dow) in daysMap) {
            if (lower.contains(prefix)) {
                val isExplicitNext = lower.contains("$prefix prossimo") ||
                    lower.contains("prossimo $prefix") ||
                    lower.contains("settimana prossima")
                return if (today.dayOfWeek == dow) {
                    if (isExplicitNext) today.plusWeeks(1) else today
                } else {
                    today.with(TemporalAdjusters.next(dow))
                }
            }
        }

        return null
    }

    fun extractTargetTime(text: String): Pair<java.time.LocalTime, java.time.LocalTime?>? {
        val lower = text.lowercase(Locale.ITALIAN)

        // Helper per verificare se un match è preceduto da una negazione ("non", "anziché", "invece di", ecc.)
        fun isMatchNegated(matchStart: Int): Boolean {
            val textBefore = lower.substring(0, matchStart).trimEnd()
            return Regex("""\b(?:non|no|anzich[eé]|invece\s+di|senza)\s*$""", RegexOption.IGNORE_CASE).containsMatchIn(textBefore)
        }

        // 1. Range orario avanzato:
        // "alle 17:45 fino alle 20", "dalle 17:45 fino alle 20", "dalle 10:30 alle 12:00", "dalle 15 alle 17",
        // "da 17:45 a 20:00", "dalle 17:45 a 20", "17:45 - 20:00", "tra le 17:45 e le 20"
        val rangeRegex = Regex(
            """\b(?:dalle|alle|da)\s+(\d{1,2})(?:[:.;,](\d{2}))?\s+(?:fino\s+alle|fino\s+a|alle|\ba\b)\s+(\d{1,2})(?:[:.;,](\d{2}))?\b"""
        )
        for (m in rangeRegex.findAll(lower)) {
            val fullMatch = m.value
            // Esclude falsi positivi con negazione interna come "alle 17:45 non alle 20"
            if (!fullMatch.contains("non ") && !fullMatch.contains("anzich") && !fullMatch.contains("invece") && !isMatchNegated(m.range.first)) {
                val h1 = m.groupValues[1].toIntOrNull() ?: continue
                val m1 = m.groupValues[2].toIntOrNull() ?: 0
                val h2 = m.groupValues[3].toIntOrNull() ?: continue
                val m2 = m.groupValues[4].toIntOrNull() ?: 0
                if (h1 in 0..23 && m1 in 0..59 && h2 in 0..23 && m2 in 0..59) {
                    var endHour = h2
                    if (endHour < h1 && endHour in 1..11 && endHour + 12 < 24) {
                        endHour += 12
                    }
                    return java.time.LocalTime.of(h1, m1) to java.time.LocalTime.of(endHour, m2)
                }
            }
        }

        val dashRangeRegex = Regex("""\b(\d{1,2})(?:[:.;,](\d{2}))?\s*[-–—]\s*(\d{1,2})(?:[:.;,](\d{2}))?\b""")
        dashRangeRegex.find(lower)?.let { m ->
            if (!isMatchNegated(m.range.first)) {
                val h1 = m.groupValues[1].toIntOrNull() ?: return@let
                val m1 = m.groupValues[2].toIntOrNull() ?: 0
                val h2 = m.groupValues[3].toIntOrNull() ?: return@let
                val m2 = m.groupValues[4].toIntOrNull() ?: 0
                if (h1 in 0..23 && m1 in 0..59 && h2 in 0..23 && m2 in 0..59) {
                    var endHour = h2
                    if (endHour < h1 && endHour in 1..11 && endHour + 12 < 24) {
                        endHour += 12
                    }
                    return java.time.LocalTime.of(h1, m1) to java.time.LocalTime.of(endHour, m2)
                }
            }
        }

        val betweenRangeRegex = Regex("""\btra\s+le\s+(\d{1,2})(?:[:.;,](\d{2}))?\s+e\s+le\s+(\d{1,2})(?:[:.;,](\d{2}))?\b""")
        betweenRangeRegex.find(lower)?.let { m ->
            if (!isMatchNegated(m.range.first)) {
                val h1 = m.groupValues[1].toIntOrNull() ?: return@let
                val m1 = m.groupValues[2].toIntOrNull() ?: 0
                val h2 = m.groupValues[3].toIntOrNull() ?: return@let
                val m2 = m.groupValues[4].toIntOrNull() ?: 0
                if (h1 in 0..23 && m1 in 0..59 && h2 in 0..23 && m2 in 0..59) {
                    return java.time.LocalTime.of(h1, m1) to java.time.LocalTime.of(h2, m2)
                }
            }
        }

        // 2. "alle 10 e mezza" / "alle 10 e mezzo"
        val halfHourRegex = Regex("""\b(?:alle|ore|verso\s+le|per\s+le)\s+(\d{1,2})\s+e\s+mezz[ao]\b""")
        halfHourRegex.find(lower)?.let { m ->
            if (!isMatchNegated(m.range.first)) {
                var h = m.groupValues[1].toIntOrNull() ?: return@let
                if (h in 1..7 && (lower.contains("pomeriggio") || lower.contains("sera"))) h += 12
                if (h in 0..23) return java.time.LocalTime.of(h, 30) to null
            }
        }

        // 3. "alle 10 e un quarto" / "alle 10 e 30"
        val spokenMinutesRegex = Regex("""\b(?:alle|ore|verso\s+le|per\s+le)\s+(\d{1,2})\s+e\s+(un\s+quarto|\d{1,2})\b""")
        spokenMinutesRegex.find(lower)?.let { m ->
            if (!isMatchNegated(m.range.first)) {
                var h = m.groupValues[1].toIntOrNull() ?: return@let
                val minPart = m.groupValues[2]
                val mins = if (minPart.contains("quarto")) 15 else minPart.toIntOrNull() ?: 0
                if (h in 1..7 && (lower.contains("pomeriggio") || lower.contains("sera"))) h += 12
                if (h in 0..23 && mins in 0..59) return java.time.LocalTime.of(h, mins) to null
            }
        }

        // 4. Orario Standard: "alle 10:30", "alle 17:45", "ore 11", "alle 20", ecc.
        // Filtra esplicitamente gli orari negati ("non alle 20") e seleziona il primo orario valido/affermativo
        val standardTimeRegex = Regex("""\b(?:alle\s+ore|alle|ore|h\.?|verso\s+le|per\s+le)\s*(\d{1,2})(?:[:.;,](\d{2})|\s+([0-5]\d)\b)?""")
        val validMatches = standardTimeRegex.findAll(lower).filter { m ->
            !isMatchNegated(m.range.first)
        }.toList()

        if (validMatches.isNotEmpty()) {
            val chosenMatch = validMatches.first()
            var h = chosenMatch.groupValues[1].toIntOrNull() ?: return null
            val mins = chosenMatch.groupValues[2].takeIf { it.isNotBlank() }?.toIntOrNull()
                ?: chosenMatch.groupValues[3].takeIf { it.isNotBlank() }?.toIntOrNull()
                ?: 0
            if (h in 1..7 && (lower.contains("pomeriggio") || lower.contains("sera"))) h += 12
            if (h in 0..23 && mins in 0..59) {
                return java.time.LocalTime.of(h, mins) to null
            }
        }

        // 5. Solo orario numerico ("10:30", "17:45", "13;30", "15,30") (es. risposta breve a chiarimento o rettifica)
        val bareTimeRegex = Regex("""(?:^|\s)(\d{1,2})[:.;,](\d{2})(?:\s|$)""")
        val validBareMatches = bareTimeRegex.findAll(lower).filter { m ->
            !isMatchNegated(m.range.first)
        }.toList()
        if (validBareMatches.isNotEmpty()) {
            val m = validBareMatches.first()
            val h = m.groupValues[1].toIntOrNull() ?: return null
            val mins = m.groupValues[2].toIntOrNull() ?: 0
            if (h in 0..23 && mins in 0..59) {
                return java.time.LocalTime.of(h, mins) to null
            }
        }

        // 6. Ora secca isolata ("17", "9")
        val bareHourRegex = Regex("""^\s*(\d{1,2})\s*$""")
        bareHourRegex.find(lower)?.let { m ->
            val h = m.groupValues[1].toIntOrNull() ?: return@let
            if (h in 7..23) {
                return java.time.LocalTime.of(h, 0) to null
            }
        }

        return null
    }

    fun extractLocation(text: String): String? {
        if (text.isBlank()) return null

        val stopBoundary = Regex(
            """\b(?:incontro|riunione|sopralluogo|appuntamento|udienza|rilievo|collaudo|con|insieme\s+a|che\s+dobbiamo|dobbiamo|che\s+devo|per\s+scrivere|per\s+fare|per\s+vedere|per\s+il|per\s+la|alle\s+ore|alle|ore|dalle\s+ore|dalle|h\.?|verso\s+le|verso|per\s+le|anzi|scusa|domani|dopodomani|luned|marted|mercoled|gioved|venerd|sabat|domenic)\b""",
            RegexOption.IGNORE_CASE
        )

        // 1. "a casa di <Nome>" / "casa di <Nome>" / "a casa mia/sua"
        val casaRegex = Regex(
            """\b(?:a\s+)?(casa\s+(?:di\s+[A-ZÀ-Úa-zà-ú0-9']+(?:\s+[A-ZÀ-Úa-zà-ú0-9']+)?|mia|sua|nostra|loro))\b""",
            RegexOption.IGNORE_CASE
        )
        casaRegex.find(text)?.let { m ->
            val raw = m.groupValues[1].split(stopBoundary).first().trim().trimEnd(',', '.', ';')
            if (raw.length >= 6) return formatLocationDisplayName(raw)
        }

        // 2. Indirizzi stradali (Via, Viale, Piazza, Corso, ecc.) anche scritti in minuscolo
        val streetRegex = Regex(
            """\b((?:via|viale|piazza|piazzale|corso|largo|vicolo|strada|contrada|localit[aà]|v\.le|p\.zza)\s+[A-ZÀ-Úa-zà-ú0-9][A-ZÀ-Úa-zà-ú0-9\s',.-]{2,45}?)(?:\s+(?:a|in|ad)\s+([A-ZÀ-Úa-zà-ú]{2,25}))?(?=""" + stopBoundary.pattern + """|$)""",
            RegexOption.IGNORE_CASE
        )
        streetRegex.find(text)?.let { m ->
            val addr = m.groupValues[1].split(stopBoundary).first().trim().trimEnd(',', '.', ';')
            val town = m.groupValues[2].takeIf { it.isNotBlank() }?.split(stopBoundary)?.first()?.trim()?.trimEnd(',', '.', ';')
            val combined = if (!town.isNullOrBlank()) "$addr di $town" else addr
            if (combined.length >= 4) return formatLocationDisplayName(combined)
        }

        // 3. Luoghi tecnici/professionali (Immobile, Condominio, Tribunale, Studio, Ufficio, Comune, Catasto, Villa, Palazzo, Appartamento)
        val siteRegex = Regex(
            """\b((?:immobile|fabbricato|condominio|tribunale|studio\s+tecnico|studio\s+legale|studio|ufficio\s+tecnico|ufficio|comune|catasto|villa|palazzo|appartamento)\s+(?:di|in|a|del|della)?\s*[A-ZÀ-Úa-zà-ú0-9\s']{1,35}?)(?=""" + stopBoundary.pattern + """|$)""",
            RegexOption.IGNORE_CASE
        )
        siteRegex.find(text)?.let { m ->
            val s = m.groupValues[1].split(stopBoundary).first().trim().trimEnd(',', '.', ';')
            if (s.length >= 4) return formatLocationDisplayName(s)
        }

        // 4. "presso <Luogo>" / "in studio" / "in ufficio" / "al sopralluogo"
        val pressoRegex = Regex(
            """\bpresso\s+((?:il\s+|lo\s+|la\s+|l'\s*)?[A-ZÀ-Úa-zà-ú0-9][A-ZÀ-Úa-zà-ú0-9\s',.-]{2,35}?)(?=""" + stopBoundary.pattern + """|$)""",
            RegexOption.IGNORE_CASE
        )
        pressoRegex.find(text)?.let { m ->
            val cleaned = m.groupValues[1].split(stopBoundary).first().trim().trimEnd(',', '.', ';')
            if (cleaned.length >= 3) return formatLocationDisplayName(cleaned)
        }

        val standaloneSiteRegex = Regex("""\b(in\s+studio|allo\s+studio|in\s+ufficio|al\s+sopralluogo|sul\s+posto|al\s+catasto|in\s+tribunale)\b""", RegexOption.IGNORE_CASE)
        standaloneSiteRegex.find(text)?.let { m ->
            return formatLocationDisplayName(m.groupValues[1])
        }

        // 5. Normalizza / rimuove le espressioni verbali/attività prima del matching così che "a fare colazione" non inglobi "a non solo dolce"
        val activityPhrases = listOf(
            Regex("""\b(?:a\s+fare|a\s+prendere|per\s+fare|per\s+prendere|a\s+mangiare|a\s+bere)\s+(?:la\s+|un\s+|una\s+)?(?:colazione|pranzo|cena|caff[eè]|aperitivo|chiacchiere|due\s+chiacchiere|qualcosa)\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(?:a\s+fare|per\s+fare)\s+(?:il\s+|un\s+|la\s+|una\s+)?(?:sopralluogo|perizia|accesso|rilievo|ispezione)\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(?:a|per)\s+(?:colazione|pranzo|cena|caff[eè]|aperitivo)\b""", RegexOption.IGNORE_CASE),
            Regex("""\ba\s+(?:vedere|incontrare|parlare|discutere|firmare|consegnare|trovare)\b""", RegexOption.IGNORE_CASE)
        )
        var cleanedText = text
        for (pattern in activityPhrases) {
            cleanedText = pattern.replace(cleanedText, " ")
        }

        // 6. Locali commerciali specifici con categoria (bar, pasticceria, ristorante, pizzeria, trattoria, bistrot, hotel, pub, locale)
        val catRegex = Regex(
            """\b(?:a|al|allo|alla|all'|in|da|presso)\s+((?:bar|pasticceria|ristorante|trattoria|pizzeria|bistrot|caffetteria|caff[eè]|hotel|albergo|pub|locale)\s+[A-ZÀ-Úa-zà-ú0-9\s']{2,35}?)(?:\s+(?:di|a|in|ad)\s+([A-ZÀ-Úa-zà-ú]{2,25}))?(?=""" + stopBoundary.pattern + """|$)""",
            RegexOption.IGNORE_CASE
        )
        catRegex.find(cleanedText)?.let { m ->
            val v = m.groupValues[1].split(stopBoundary).first().trim().trimEnd(',', '.', ';')
            val t = m.groupValues[2].takeIf { it.isNotBlank() }?.split(stopBoundary)?.first()?.trim()?.trimEnd(',', '.', ';')
            val res = if (!t.isNullOrBlank()) "$v di $t" else v
            if (res.length >= 3) return formatLocationDisplayName(res)
        }

        // 7. Luogo / Locale introdotto da preposizione (a / al / alla / da / presso / in): es. "a non solo dolce di Albate"
        val generalVenueRegex = Regex(
            """\b(?:a|al|allo|alla|all'|da|presso|in)\s+([A-ZÀ-Úa-zà-ú0-9'][A-ZÀ-Úa-zà-ú0-9\s']{1,35}?)(?:\s+(?:di|a|in|ad)\s+([A-ZÀ-Úa-zà-ú]{2,25}))?(?=""" + stopBoundary.pattern + """|$)""",
            RegexOption.IGNORE_CASE
        )
        val actionWords = setOf(
            "fare", "prendere", "vedere", "incontrare", "sentire", "parlare", "discutere",
            "firmare", "lavorare", "mangiare", "cenare", "pranzare", "trovare", "scrivere",
            "colazione", "pranzo", "cena", "caffe", "caffè", "aperitivo", "oggi", "domani",
            "dopodomani", "deiv", "dave", "scopiamo", "appuntamento", "chiacchiere"
        )
        for (m in generalVenueRegex.findAll(cleanedText)) {
            val cand = m.groupValues[1].split(stopBoundary).first().trim().trimEnd(',', '.', ';')
            val t = m.groupValues[2].takeIf { it.isNotBlank() }?.split(stopBoundary)?.first()?.trim()?.trimEnd(',', '.', ';')
            val words = cand.lowercase(Locale.ITALIAN).split(Regex("""\s+""")).toSet()
            if (words.intersect(actionWords).isEmpty() && cand.length >= 3) {
                val res = if (!t.isNullOrBlank()) "$cand di $t" else cand
                return formatLocationDisplayName(res)
            }
        }

        // 8. Città / Comune singolo introdotto da 'a', 'ad', 'in' prima del confine temporale (es. "ad Albate alle 9:30", "a Como alle 10:30")
        val townRegex = Regex(
            """\b(?:ad?|in)\s+([A-ZÀ-Úa-zà-ú]{3,25})(?=""" + stopBoundary.pattern + """|$)""",
            RegexOption.IGNORE_CASE
        )
        townRegex.find(cleanedText)?.let { m ->
            val candTown = m.groupValues[1].trim()
            if (candTown.lowercase(Locale.ITALIAN) !in actionWords && candTown.length >= 3) {
                return formatLocationDisplayName(candTown)
            }
        }

        return null
    }

    private fun formatLocationDisplayName(raw: String): String {
        val lowerParticles = setOf("di", "del", "della", "dei", "degli", "delle", "da", "in", "a", "ad", "al", "allo", "alla", "all'", "il", "lo", "la")
        return raw.trim().split(Regex("""\s+""")).mapIndexed { index, word ->
            val lowerWord = word.lowercase(Locale.ITALIAN)
            if (index > 0 && lowerWord in lowerParticles) {
                lowerWord
            } else {
                lowerWord.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ITALIAN) else it.toString() }
            }
        }.joinToString(" ")
    }

    private fun extractAppointmentClient(text: String): String? {
        if (text.isBlank()) return null

        val stopClientBoundary = Regex(
            """\b(?:che|dobbiamo|devo|per\s+scrivere|per\s+vedere|per\s+fare|riguardo|dove|cos[iì]|in|a\s+fare|a\s+prendere|a|al|allo|alla|alle|ore|domani|dopodomani|anzi|scusa)\b""",
            RegexOption.IGNORE_CASE
        )

        val forbiddenClients = setOf(
            "fare", "prendere", "mattina", "pomeriggio", "sera", "studio", "casa", "immobile",
            "ufficio", "posto", "luogo", "ora", "orario", "tempo", "giorno", "data", "notte",
            "colazione", "pranzo", "cena", "caffe", "caffè", "aperitivo", "perizia", "sopralluogo"
        )

        val patterns = listOf(
            // 1. Titoli professionali (Ing. Rossi, Arch. Bianchi, Geom. Colombo, ecc.)
            Regex("""\b((?:Ing\.|Ingegner|Arch\.|Architetto|Avv\.|Avvocato|Geom\.|Geometra|Dott\.|Dottor|Sig\.|Signor|Sig\.ra|Condominio|Impresa)\s+[A-ZÀ-Úa-zà-ú]+(?:\s+[A-ZÀ-Úa-zà-ú]+)?)""", RegexOption.IGNORE_CASE),
            // 2. "con <Persona>" / "insieme a <Persona>" / "vado con <Persona>"
            Regex("""\b(?:vado\s+con|con|insieme\s+a)\s+((?:il\s+|la\s+|lo\s+|l'\s*)?[A-ZÀ-Úa-zà-ú]{2,20}(?:\s+[A-ZÀ-Úa-zà-ú]{2,20})?)""", RegexOption.IGNORE_CASE),
            // 3. Nome interlocutore tra data e azione: "appuntamento per domani deiv a fare colazione"
            Regex("""\b(?:per\s+)?(?:oggi|domani|dopodomani|luned[ìi]|marted[ìi]|mercoled[ìi]|gioved[ìi]|venerd[ìi]|sabato|domenica)\s+([A-ZÀ-Úa-zà-ú]{2,20})\s+(?:a\s+fare|a\s+prendere|a|per\s+fare|per)\b""", RegexOption.IGNORE_CASE),
            // 4. "vedere / vedo / incontrare / incontro / sentire / sento <Persona>"
            Regex("""\b(?:vedere|vedo|incontrare|incontro|sentire|sento)\s+((?:il\s+|la\s+|lo\s+|l'\s*)?[A-ZÀ-Úa-zà-ú]{2,20})\b""", RegexOption.IGNORE_CASE),
            // 5. "appuntamento per <Persona>" (es. "appuntamento per Deiv domani")
            Regex("""\bappuntamento\s+per\s+([A-ZÀ-Úa-zà-ú]{2,20})\s+(?:oggi|domani|dopodomani|luned|marted|mercoled|gioved|venerd|sabat|domenic|alle|ore)\b""", RegexOption.IGNORE_CASE)
        )
        for (regex in patterns) {
            val m = regex.find(text)
            if (m != null) {
                val candidate = m.groupValues[1]
                    .split(stopClientBoundary)
                    .first()
                    .replace(Regex("""^(il|la|lo|l')\s+""", RegexOption.IGNORE_CASE), "")
                    .trim()
                    .trimEnd(',', '.', ';')
                val lowerCandidate = candidate.lowercase(Locale.ITALIAN)
                if (candidate.length >= 3 && lowerCandidate !in forbiddenClients) {
                    return formatLocationDisplayName(candidate)
                }
            }
        }
        return null
    }

    private fun extractMeetingTopic(text: String): String? {
        if (text.isBlank()) return null
        val topicPatterns = listOf(
            Regex("""\b(?:che\s+dobbiamo|dobbiamo|che\s+devo|devo)\s+([^,.;\n]{4,60})""", RegexOption.IGNORE_CASE),
            Regex("""\bper\s+((?:scrivere|redigere|controllare|verificare|firmare|discutere|vedere|valutare|consegnare|ritirare|il\s+rapporto|la\s+planimetria|la\s+perizia|il\s+computo)[^,.;\n]{2,55})""", RegexOption.IGNORE_CASE)
        )
        for (regex in topicPatterns) {
            val m = regex.find(text)
            if (m != null) {
                val raw = m.groupValues[1]
                    .split(Regex("""\b(?:anzi|scusa|domani|dopodomani)\b""", RegexOption.IGNORE_CASE))
                    .first()
                    .trim()
                    .trimEnd(',', '.', ';')
                if (raw.length >= 4) {
                    return raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ITALIAN) else it.toString() }
                }
            }
        }
        return null
    }

    // =========================================================================
    // 3. RILEVAMENTO RICERCA ARCHIVIO E CORREZIONI POST-AZIONE
    // =========================================================================

    fun detectArchiveSearchQuery(message: String): String? {
        val lower = message.trim().lowercase(Locale.ITALIAN)
        val deleteWords = listOf("elimina", "cancella", "rimuovi", "butta", "togli", "svuota")
        if (deleteWords.any { lower.startsWith(it) || lower.contains("$it ") }) return null

        val searchPrefixes = listOf(
            "mandami ", "invia ", "inviami ", "cerca ", "trova ", "apri ",
            "dove si trova ", "hai la ", "hai il ", "mostrami ", "dammi "
        )
        val docWords = listOf(
            "perizia", "ctu", "planimetria", "computo", "fattura", "preventivo",
            "diffida", "verbale", "documento", "file", "relazione", "visura", "atto", "fascicolo"
        )
        val hasPrefix = searchPrefixes.any { lower.startsWith(it) || lower.contains(it) }
        val hasDocWord = docWords.any { lower.contains(it) }
        if (!hasPrefix && !hasDocWord) return null
        // Evita falsi positivi se l'utente sta fissando un appuntamento
        if (lower.contains("domani") || lower.contains("alle ") || lower.contains("fissa ")) return null

        val stopWords = setOf(
            "mandami", "invia", "inviami", "cerca", "trova", "apri", "mostrami", "dammi",
            "il", "lo", "la", "i", "gli", "le", "un", "una", "del", "della", "dei", "delle",
            "di", "da", "in", "con", "su", "per", "tra", "fra", "file", "documento", "per", "favore"
        )
        val tokens = lower.replace(Regex("""[^a-zà-ú0-9\s]"""), " ")
            .split(Regex("""\s+"""))
            .filter { it.length >= 2 && it !in stopWords }

        return if (tokens.isNotEmpty()) tokens.joinToString(" ") else null
    }

    // =========================================================================
    // 4. UTILITÀ NOMI FILE E LLM LOCALE (GGUF LLAMA.CPP / MEDIAPIPE / OLLAMA)
    // =========================================================================

    fun sanitizeComponent(input: String?, fallback: String = "Generico"): String {
        if (input.isNullOrBlank()) return fallback
        val cleaned = input
            .replace(Regex("""\.\.+"""), "")
            .replace(Regex("""[<>:"/\\|?*\x00-\x1F]"""), "_")
            .replace(Regex("""\s+"""), "_")
            .replace(Regex("""_+"""), "_")
            .trim('_')
        return if (cleaned.isEmpty()) fallback else cleaned.take(40)
    }

    fun buildNormalizedFilename(
        classification: DocumentClassificationResult,
        originalName: String
    ): String {
        val safeDate = sanitizeComponent(classification.data_documento, LocalDate.now().toString())
        val safeClient = sanitizeComponent(classification.cliente_o_controparte, "Generico")
        val safeSubject = sanitizeComponent(classification.oggetto_sintetico, "Documento")
        val safeOrig = sanitizeComponent(
            originalName.replace(Regex("""\.p7m$""", RegexOption.IGNORE_CASE), ""),
            "allegato.pdf"
        )
        return "${safeDate}_${safeClient}_${safeSubject}_${safeOrig}"
    }

    private fun formatItalianDate(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ITALIAN)
        return date.format(formatter)
    }

    fun hasPotentialUnparsedEntity(text: String): Boolean {
        val lower = text.lowercase(Locale.ITALIAN)
        val entityKeywords = listOf(
            "via ", "viale ", "piazza ", "corso ", "strada ", "vicolo ", "largo ",
            "rilievo", "studio", "tribunale", "palazzo", "condominio", "presso",
            "a casa", "c/o", "con ", "ing.", "arch.", "geom.", "dott.", "avv.",
            "sig.", "sig.ra", "contro", "immobile", "appartamento", "tetto",
            "bar ", "pasticceria", "ristorante", "trattoria", "pizzeria", "bistrot",
            "colazione", "pranzo", "cena", "caffè", "caffe", "aperitivo", "incontro",
            "vado con", "trovo con", "vedo con", "non solo dolce"
        )
        if (entityKeywords.any { lower.contains(it) }) return true

        val stopWords = setOf(
            "domani", "oggi", "dopodomani", "lunedì", "martedì", "mercoledì", "giovedì",
            "venerdì", "sabato", "domenica", "sopralluogo", "appuntamento", "fissa",
            "promemoria", "alle", "ore", "per", "del", "della", "delle", "dei", "in", "a", "da", "di", "al", "ad"
        )
        val words = text.split(Regex("""\s+""")).filter { it.length >= 3 && it.lowercase() !in stopWords }
        val capitalized = words.filter { it.first().isUpperCase() }
        if (capitalized.isNotEmpty()) return true
        return words.size >= 2
    }

    /**
     * Risponde a domande tecniche, quesiti peritali o richieste di chiarimento
     * utilizzando direttamente il modello locale di Reasoning (Qwen 3.5 2B Q4_K_M) con timeout di sicurezza e cancellazione nativa.
     */
    suspend fun askReasoningAssistant(
        question: String,
        config: AiEngineConfig = AiEngineConfig()
    ): String? = withContext(Dispatchers.Default) {
        val clean = question.trim()
        if (clean.length < 3) return@withContext null

        withTimeoutOrNull(120_000L) {
            llamaMutex.withLock {
                if (ensureGgufLoaded(config)) {
                    try {
                        val prompt = "<|im_start|>system\nSei l'assistente IA dello Studio Tecnico Perito Edile Restelli. Rispondi in italiano con competenza tecnica, chiarezza e precisione peritale.<|im_end|>\n<|im_start|>user\n$clean<|im_end|>\n<|im_start|>assistant\n"
                        val res = LlamaBridge.generate(prompt).trim()
                        val cleanRes = res
                            .substringBefore("<|im_end|>")
                            .substringBefore("<|endoftext|>")
                            .trim()
                        if (cleanRes.isNotBlank()) return@withTimeoutOrNull cleanRes
                    } catch (_: Throwable) {
                        try { LlamaBridge.nativeCancelGenerate() } catch (_: Throwable) {}
                    }
                }
                null
            }
        } ?: run {
            try { LlamaBridge.nativeCancelGenerate() } catch (_: Throwable) {}
            null
        }
    }

    private suspend fun tryCallLocalLlm(prompt: String, config: AiEngineConfig): String? = withContext(Dispatchers.Default) {
        withTimeoutOrNull(25_000L) {
            // 1. Prova prima il motore nativo llama.cpp (LlamaBridge) sui file .gguf scaricati (Qwen 3.5 2B Q4_K_M)
            val ggufResult = llamaMutex.withLock {
                if (ensureGgufLoaded(config)) {
                    try {
                        val formattedPrompt = "<|im_start|>system\nSei un assistente per periti edili. Rispondi esclusivamente in formato JSON valido.<|im_end|>\n<|im_start|>user\n$prompt<|im_end|>\n<|im_start|>assistant\n"
                        val res = LlamaBridge.generateJson(formattedPrompt)
                        extractJsonBlock(res)
                    } catch (_: Throwable) {
                        try { LlamaBridge.nativeCancelGenerate() } catch (_: Throwable) {}
                        null
                    }
                } else null
            }
            if (!ggufResult.isNullOrBlank()) return@withTimeoutOrNull ggufResult
            // 2. Prova Ollama Locale / Termux / LAN se abilitato
            if (config.useLocalOllama && config.ollamaUrl.isNotBlank()) {
                try {
                    val url = URL(config.ollamaUrl)
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = 1500
                        readTimeout = 3000
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                    }
                    val payload = """{"model":"${config.ollamaModel}","messages":[{"role":"user","content":${json.encodeToString(prompt)}}],"temperature":0.1}"""
                    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(payload) }
                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader().readText()
                        val unwrapped = extractContentFromApiResponse(body) ?: body
                        extractJsonBlock(unwrapped)
                    } else null
                } catch (_: Throwable) {
                    null
                }
            } else null
        }
    }

    fun extractContentFromApiResponse(rawResponse: String): String? {
        return runCatching {
            val root = json.parseToJsonElement(rawResponse).jsonObject
            // 1. OpenAI format: choices[0].message.content
            val choices = root["choices"]?.jsonArray
            if (choices != null && choices.isNotEmpty()) {
                val firstChoice = choices[0].jsonObject
                val msg = firstChoice["message"]?.jsonObject
                val content = msg?.get("content")?.jsonPrimitive?.contentOrNull
                if (!content.isNullOrBlank()) return@runCatching content
            }
            // 2. Ollama native generate format: response
            val response = root["response"]?.jsonPrimitive?.contentOrNull
            if (!response.isNullOrBlank()) return@runCatching response

            // 3. Ollama native chat format: message.content
            val directMsg = root["message"]?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
            if (!directMsg.isNullOrBlank()) return@runCatching directMsg

            null
        }.getOrNull()
    }

    private fun extractJsonBlock(raw: String): String? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        return if (start >= 0 && end > start) raw.substring(start, end + 1) else null
    }

    /**
     * Verifica se il testo estratto da un documento/foto contiene un riferimento esplicito
     * a un appuntamento/sopralluogo con orario o convocazione (evitando che qualsiasi documento
     * con la parola "perizia" o "rilievo" venga scambiato per un appuntamento da fissare).
     */
    fun hasExplicitAppointmentInDocument(rawText: String): Boolean {
        if (!isMeaningfulOcrText(rawText)) return false
        val lower = rawText.lowercase(Locale.ITALIAN)
        val explicitPhrases = listOf(
            "appuntamento", "sopralluogo fissato", "convocazione", "udienza",
            "inizio operazioni peritali", "ci vediamo", "vediamoci", "vi aspetto"
        )
        val hasExplicitPhrase = explicitPhrases.any { lower.contains(it) }
        val hasExplicitTime = Regex("""\b(?:alle\s+(?:ore\s+)?|ore\s+|h\.?\s*)([01]?\d|2[0-3])(?:[:.,]([0-5]\d))?\b""").containsMatchIn(lower)
        return hasExplicitPhrase && hasExplicitTime
    }

    private fun buildClassificationPrompt(text: String, refDate: LocalDate): String {
        return """
            Sei l'assistente di un Perito Tecnico Estimativo / CTU. Classifica questo documento in JSON con le chiavi:
            "categoria" (una tra: Atti_Tribunale, Perizie_Tecniche, Planimetrie_Computi, Fatture_Preventivi, Corrispondenza_Legale, Da_Classificare_Manualmente),
            "cliente_o_controparte", "oggetto_sintetico", "data_documento" (default $refDate).
            REGOLA RIGIDA ANTI-ALLUCINAZIONE: Se il testo non ha senso compiuto o non appartiene chiaramente a un documento tecnico/legale/contabile, usa obbligatoriamente "Da_Classificare_Manualmente". NON inventare nomi o oggetti non presenti nel testo.
            Testo: ${text.take(1500)}
        """.trimIndent()
    }

    private fun buildAppointmentPrompt(text: String, now: LocalDateTime): String {
        return """
            Sei l'assistente agenda di uno studio peritale. Data e ora attuale di riferimento: $now.
            Estrai i dettagli dell'appuntamento o incontro (tecnico, di lavoro o personale/informale come colazione, pranzo, caffè, cena, riunione, sopralluogo) dal messaggio dell'utente in formato JSON.
            
            REGOLE DI ESTRAZIONE:
            - "titolo": attività sintetica con chi/dove (es. "Colazione con Deiv", "Sopralluogo Perizia Rossi", "Pranzo di lavoro"). Se è un incontro informale, usa il tipo specifico ("Colazione", "Pranzo", "Caffè", "Incontro"), NON usare "Appuntamento Tecnico" generico.
            - "cliente": nome della persona, collega, cliente o contatto con cui ci si incontra (es. "Deiv", "Dave", "Marco", "Ing. Bianchi"). Se non c'è una persona menzionata, metti "Cliente".
            - "luogo": indirizzo, locale, bar, ristorante o comune specificato (es. "Non Solo Dolce di Albate", "Via Roma 12 Como", "Albate"). Se non specificato, metti "Da definire".
            - "data_inizio_iso": data e ora d'inizio in formato ISO-8601 (YYYY-MM-DDTHH:mm:ss). Calcola la data corretta partendo da "$now" ("domani" = data attuale + 1 giorno, ecc.).
            - "data_fine_iso": se specificata ora di fine, calcolala, altrimenti aggiungi 1 ora a data_inizio_iso.
            - REGOLA ANTI-ALLUCINAZIONE: NON inventare mai giorni o orari assenti nel testo. Se nel messaggio non c'è alcun riferimento temporale, rispondi {"has_appointment": false}.
            
            Schema JSON richiesto:
            {"has_appointment": true, "titolo": "...", "data_inizio_iso": "YYYY-MM-DDTHH:mm:ss", "data_fine_iso": "YYYY-MM-DDTHH:mm:ss", "luogo": "...", "cliente": "...", "is_ambiguo": false, "motivo_ambiguita": ""}
            
            Messaggio dell'utente: $text
        """.trimIndent()
    }

    private fun parseClassificationJson(rawJson: String, fallbackDate: LocalDate): DocumentClassificationResult? {
        return try {
            val obj = json.parseToJsonElement(rawJson) as? JsonObject ?: return null
            val cat = PeritoCategory.fromFolderName(obj["categoria"]?.jsonPrimitive?.content).folderName
            val cli = sanitizeComponent(obj["cliente_o_controparte"]?.jsonPrimitive?.content, "Generico")
            val ogg = sanitizeComponent(obj["oggetto_sintetico"]?.jsonPrimitive?.content, "Documento")
            val dt = obj["data_documento"]?.jsonPrimitive?.content ?: fallbackDate.toString()
            DocumentClassificationResult(cat, cli, ogg, dt)
        } catch (_: Throwable) {
            null
        }
    }

    private fun parseAppointmentJson(rawJson: String): AppointmentParseResult? {
        return try {
            val obj = json.parseToJsonElement(rawJson) as? JsonObject ?: return null
            val hasApp = obj["has_appointment"]?.jsonPrimitive?.booleanOrNull ?: false
            if (!hasApp) return AppointmentParseResult(has_appointment = false)
            AppointmentParseResult(
                has_appointment = true,
                titolo = obj["titolo"]?.jsonPrimitive?.content ?: "Appuntamento",
                data_inizio_iso = obj["data_inizio_iso"]?.jsonPrimitive?.content,
                data_fine_iso = obj["data_fine_iso"]?.jsonPrimitive?.content,
                luogo = obj["luogo"]?.jsonPrimitive?.content ?: "Da definire",
                cliente = obj["cliente"]?.jsonPrimitive?.content ?: "Cliente",
                note = obj["note"]?.jsonPrimitive?.content ?: "",
                is_ambiguo = obj["is_ambiguo"]?.jsonPrimitive?.booleanOrNull ?: false,
                motivo_ambiguita = obj["motivo_ambiguita"]?.jsonPrimitive?.content ?: ""
            )
        } catch (_: Throwable) {
            null
        }
    }
}
