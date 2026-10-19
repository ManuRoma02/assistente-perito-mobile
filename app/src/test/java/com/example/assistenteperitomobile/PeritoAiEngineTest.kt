package com.example.assistenteperitomobile

import com.example.assistenteperitomobile.domain.AiEngineConfig
import com.example.assistenteperitomobile.domain.PeritoAiEngine
import com.example.assistenteperitomobile.domain.PeritoCategory
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.system.measureTimeMillis

class PeritoAiEngineTest {

    private val engine = PeritoAiEngine(null)
    private val refDate = LocalDate.of(2026, 10, 10)
    private val refNow = LocalDateTime.of(2026, 10, 10, 9, 0)

    @Test
    fun `test 6 categorie classificazione documenti e normalizzazione nome file`() = runTest {
        // 1. Atti_Tribunale
        val ctu = engine.classifyDocument(
            "TRIBUNALE ORDINARIO DI MILANO - Sezione Civile. Il Giudice nomina CTU nel procedimento R.G. 1234/2026 contro Condominio Aurora. Quesito peritale.",
            "nomina_ctu.pdf",
            refDate
        )
        assertEquals(PeritoCategory.ATTI_TRIBUNALE.folderName, ctu.categoria)

        // 2. Perizie_Tecniche
        val perizia = engine.classifyDocument(
            "RELAZIONE TECNICA PERITALE. Sopralluogo per infiltrazioni d'acqua e danni da umidità presso Condominio Sole.",
            "relazione.pdf",
            refDate
        )
        assertEquals(PeritoCategory.PERIZIE_TECNICHE.folderName, perizia.categoria)

        // 3. Planimetrie_Computi
        val computo = engine.classifyDocument(
            "COMPUTO METRICO ESTIMATIVO ed elenco prezzi per rifacimento copertura. Visura catastale Foglio 12 Particella 440 Subalterno 5.",
            "computo.pdf",
            refDate
        )
        assertEquals(PeritoCategory.PLANIMETRIE_COMPUTI.folderName, computo.categoria)

        // 4. Fatture_Preventivi
        val fattura = engine.classifyDocument(
            "FATTURA N. 18/2026. Imponibile Euro 1.500,00 + Cassa Geometri + IVA 22%. Ritenuta d'acconto e IBAN per bonifico.",
            "fattura18.pdf",
            refDate
        )
        assertEquals(PeritoCategory.FATTURE_PREVENTIVI.folderName, fattura.categoria)

        // 5. Corrispondenza_Legale
        val diffida = engine.classifyDocument(
            "STUDIO LEGALE AVV. ROSSI. Diffida formale e messa in mora in nome e per conto del cliente. Osservazioni CTP.",
            "pec_avvocato.pdf",
            refDate
        )
        assertEquals(PeritoCategory.CORRISPONDENZA_LEGALE.folderName, diffida.categoria)

        // 6. Da_Classificare_Manualmente (testo vuoto o irrilevante)
        val unknown = engine.classifyDocument("...", "img001.jpg", refDate)
        assertEquals(PeritoCategory.DA_CLASSIFICARE_MANUALMENTE.folderName, unknown.categoria)

        // Verifica sanitizzazione nome file contro caratteri illegali
        val safeName = engine.buildNormalizedFilename(ctu, "../nomina:ctu*?.pdf.p7m")
        assertFalse(safeName.contains(".."))
        assertFalse(safeName.contains(":"))
        assertFalse(safeName.contains("*"))
        assertFalse(safeName.endsWith(".p7m"))
    }

    @Test
    fun `test estrazione appuntamento chiaro e auto-correzione anzi no`() = runTest {
        // Appuntamento chiaro domani alle 10:30
        val clear = engine.parseAppointment(
            "Fissa sopralluogo domani alle 10:30 in Via Roma 15 con Ing. Rossi",
            refNow
        )
        assertTrue(clear.has_appointment)
        assertFalse(clear.is_ambiguo)
        assertEquals("2026-10-11T10:30:00", clear.data_inizio_iso)
        assertEquals("2026-10-11T11:30:00", clear.data_fine_iso)
        assertTrue(clear.luogo.contains("Via Roma 15"))
        assertTrue(clear.cliente.contains("Rossi"))

        // Auto-correzione "anzi no" nello stesso messaggio vocale
        val selfCorrected = engine.parseAppointment(
            "Sopralluogo domani alle 9:00 in Via Garibaldi 20 con Arch. Verdi, anzi no scusa facciamo dopodomani alle 16:30",
            refNow
        )
        assertTrue(selfCorrected.has_appointment)
        assertFalse(selfCorrected.is_ambiguo)
        assertEquals("2026-10-12T16:30:00", selfCorrected.data_inizio_iso)
        assertTrue(selfCorrected.luogo.contains("Via Garibaldi 20"))
        assertTrue(selfCorrected.cliente.contains("Verdi"))

        // Caso reale utente: punto e virgola nell'orario ("13;30"), luogo colloquiale ("a casa di marco") e argomento ("scrivere un rapporto della planimetria")
        val colloquial = engine.parseAppointment(
            "domani alle 13;30 a casa di marco incontro con Elena che dobbiamo scrivere un rapporto della planimetria",
            refNow
        )
        assertTrue(colloquial.has_appointment)
        assertFalse(colloquial.is_ambiguo)
        assertEquals("2026-10-11T13:30:00", colloquial.data_inizio_iso)
        assertEquals("2026-10-11T14:30:00", colloquial.data_fine_iso)
        assertEquals("Casa di Marco", colloquial.luogo)
        assertEquals("Elena", colloquial.cliente)
        assertTrue(colloquial.titolo.contains("Elena"))
        assertTrue(colloquial.titolo.contains("rapporto della planimetria", ignoreCase = true))
    }

    @Test
    fun `test rilevamento ambiguita per orario mancante o settimana prossima`() = runTest {
        val vagueTime = engine.parseAppointment(
            "Ci vediamo domani nel pomeriggio in Via Dante 8 con Geom. Neri",
            refNow
        )
        assertTrue(vagueTime.has_appointment)
        assertTrue(vagueTime.is_ambiguo)
        assertTrue(vagueTime.motivo_ambiguita.isNotBlank())

        val vagueWeek = engine.parseAppointment(
            "Fissiamo un sopralluogo settimana prossima con Ing. Bianchi",
            refNow
        )
        assertTrue(vagueWeek.has_appointment)
        assertTrue(vagueWeek.is_ambiguo)
    }

    @Test
    fun `test rilevamento query di ricerca archivio in chat ed esclusione comandi elimina`() {
        val q = engine.detectArchiveSearchQuery("Mandami la perizia infiltrazioni del Condominio Sole")
        assertNotNull(q)
        assertTrue(q!!.contains("perizia"))
        assertTrue(q.contains("sole"))

        // Comandi di eliminazione NON devono mai essere interpretati come ricerca file
        assertNull(engine.detectArchiveSearchQuery("elimina questo file"))
        assertNull(engine.detectArchiveSearchQuery("elimina nomina ctu"))
        assertNull(engine.detectArchiveSearchQuery("cancella la perizia"))
        assertNull(engine.detectArchiveSearchQuery("rimuovi questo documento"))
    }

    @Test
    fun `test filtro anti-allucinazione OCR immagini prive di senso e appuntamenti in documenti`() {
        // Immagine vuota o con rumore visivo non deve essere considerata testo OCR valido
        assertFalse(engine.isMeaningfulOcrText(""))
        assertFalse(engine.isMeaningfulOcrText("   "))
        assertFalse(engine.isMeaningfulOcrText("| 1 . _"))
        assertFalse(engine.isMeaningfulOcrText("ab 12"))
        assertTrue(engine.isMeaningfulOcrText("Tribunale di Milano Nomina CTU"))

        // Un testo generico senza convocazione/appuntamento + orario NON deve generare appuntamenti casuali
        assertFalse(engine.hasExplicitAppointmentInDocument("Relazione tecnica perizia rilievo danni immobile infiltrazioni"))
        assertFalse(engine.hasExplicitAppointmentInDocument("Fattura n. 12 del 10/10/2026 per perizia tecnica"))
        assertTrue(engine.hasExplicitAppointmentInDocument("Convocazione sopralluogo fissato domani alle 10:30 in Via Verdi 8"))
    }

    @Test
    fun `test rizzo flow routing con protocollo tipizzato e fallback di rete`() = runTest {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        var lastReceivedPayload = ""
        var responseCategory = PeritoCategory.PERIZIE_TECNICHE.folderName
        var shouldFail = false

        server.createContext("/v1/systemone") { exchange ->
            if (shouldFail) {
                exchange.sendResponseHeaders(500, 0)
                exchange.responseBody.close()
                return@createContext
            }
            lastReceivedPayload = exchange.requestBody.bufferedReader().readText()
            val responseJson = """
                {
                    "model": "spark-x2.5-1.7b-rizzo-flow",
                    "decisions": {
                        "cartella_peritale": {
                            "value": "$responseCategory",
                            "confidence": 0.985
                        }
                    }
                }
            """.trimIndent()
            val bytes = responseJson.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.write(bytes)
            exchange.responseBody.close()
        }
        server.start()

        val port = server.address.port
        val config = AiEngineConfig(
            useRizzoFlow = true,
            rizzoFlowUrl = "http://127.0.0.1:$port/v1/systemone"
        )

        try {
            // 1. Verifica risposta corretta e payload conforme al protocollo Rizzo Flow
            val resPerizia = engine.classifyDocument(
                "Documento tecnico peritale con rilievo lesioni strutturali",
                "doc_strutturale.pdf",
                refDate,
                config
            )
            assertEquals(PeritoCategory.PERIZIE_TECNICHE.folderName, resPerizia.categoria)
            assertTrue(lastReceivedPayload.contains("cartella_peritale"))
            assertTrue(lastReceivedPayload.contains("choice"))

            // 2. Cambio categoria ritornata dal decision model (es. Atti_Tribunale)
            responseCategory = PeritoCategory.ATTI_TRIBUNALE.folderName
            val resAtti = engine.classifyDocument(
                "Ordinanza di comparizione parti davanti al Giudice Istruttore",
                "ordinanza.pdf",
                refDate,
                config
            )
            assertEquals(PeritoCategory.ATTI_TRIBUNALE.folderName, resAtti.categoria)

            // 3. Fallback graceful se il server Rizzo Flow fallisce (HTTP 500)
            shouldFail = true
            val fallbackRes = engine.classifyDocument(
                "FATTURA N. 99/2026. Saldo onorario perizia tecnica.",
                "fattura.pdf",
                refDate,
                config
            )
            // Deve fare fallback sul deterministico/Qwen e classificare comunque correttamente
            assertEquals(PeritoCategory.FATTURE_PREVENTIVI.folderName, fallbackRes.categoria)

            // 4. Fallback se Rizzo Flow è disabilitato nella config
            val disabledConfig = config.copy(useRizzoFlow = false)
            val resDisabled = engine.classifyDocument(
                "Diffida e messa in mora ad adempiere entro 15 giorni",
                "diffida.pdf",
                refDate,
                disabledConfig
            )
            assertEquals(PeritoCategory.CORRISPONDENZA_LEGALE.folderName, resDisabled.categoria)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `test benchmark prestazionale latenza rizzo flow rispetto a qwen`() = runTest {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/v1/systemone") { exchange ->
            val responseJson = """
                {"decisions": {"cartella_peritale": {"value": "${PeritoCategory.PERIZIE_TECNICHE.folderName}"}}}
            """.trimIndent()
            val bytes = responseJson.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.write(bytes)
            exchange.responseBody.close()
        }
        server.start()

        val port = server.address.port
        val config = AiEngineConfig(
            useRizzoFlow = true,
            rizzoFlowUrl = "http://127.0.0.1:$port/v1/systemone"
        )

        try {
            // Eseguiamo 10 classificazioni consecutive con Rizzo Flow e misuriamo il throughput
            val iterations = 10
            val totalTimeMs = measureTimeMillis {
                repeat(iterations) {
                    val result = engine.classifyDocument(
                        "Relazione tecnica descrittiva perizia infiltrazioni acqua e muffa",
                        "relazione_test_$it.pdf",
                        refDate,
                        config
                    )
                    assertEquals(PeritoCategory.PERIZIE_TECNICHE.folderName, result.categoria)
                }
            }
            val avgTimePerDoc = totalTimeMs.toDouble() / iterations
            // In locale il roundtrip di una decisione Rizzo Flow deve essere rapido (< 100ms per doc)
            assertTrue("La latenza media per documento ($avgTimePerDoc ms) deve essere < 100ms", avgTimePerDoc < 100.0)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `test estrazione appuntamenti informali bar locali e persone`() = runTest {
        // 1. Caso reale utente: "appuntamento per domani deiv a fare colazione a non solo dolce di Albate alle 9:30"
        val userCase1 = engine.parseAppointment(
            "appuntamento per domani deiv a fare colazione a non solo dolce di Albate alle 9:30",
            refNow
        )
        assertTrue(userCase1.has_appointment)
        assertFalse(userCase1.is_ambiguo)
        assertEquals("2026-10-11T09:30:00", userCase1.data_inizio_iso)
        assertEquals("Deiv", userCase1.cliente)
        assertEquals("Non Solo Dolce di Albate", userCase1.luogo)
        assertTrue(userCase1.titolo.contains("Colazione"))
        assertTrue(userCase1.titolo.contains("Deiv"))

        // 2. Caso vocale con range orario e interlocutore: "Domani vado con Dave a colazione Dalle 10:30 alle 11:45"
        val userCase2 = engine.parseAppointment(
            "Domani vado con Dave a colazione Dalle 10:30 alle 11:45",
            refNow
        )
        assertTrue(userCase2.has_appointment)
        assertFalse(userCase2.is_ambiguo)
        assertEquals("2026-10-11T10:30:00", userCase2.data_inizio_iso)
        assertEquals("2026-10-11T11:45:00", userCase2.data_fine_iso)
        assertEquals("Dave", userCase2.cliente)
        assertTrue(userCase2.titolo.contains("Colazione"))
        assertTrue(userCase2.titolo.contains("Dave"))

        // 3. Caso bar con comune prima dell'orario
        val barCase = engine.parseAppointment(
            "domani alle 9:30 colazione con deiv al bar san marco di Como",
            refNow
        )
        assertTrue(barCase.has_appointment)
        assertEquals("Bar San Marco di Como", barCase.luogo)
        assertEquals("Deiv", barCase.cliente)

        // 4. Caso ristorante con cliente e luogo
        val restCase = engine.parseAppointment(
            "pranzo domani alle 13 al ristorante la pergola di cernobbio con ing rossi",
            refNow
        )
        assertTrue(restCase.has_appointment)
        assertEquals("Ristorante la Pergola di Cernobbio", restCase.luogo)
        assertTrue(restCase.cliente.contains("Rossi"))
    }

    @Test
    fun `test estrazione range orario e filtro negazioni orarie`() = runTest {
        // 1. Caso reale utente: "domani vado con tedo in palestra a Lurate Caccivio alle 17:45 fino alle 20"
        val gymCase = engine.parseAppointment(
            "domani vado con tedo in palestra a Lurate Caccivio alle 17:45 fino alle 20",
            refNow
        )
        assertTrue(gymCase.has_appointment)
        assertFalse(gymCase.is_ambiguo)
        assertEquals("2026-10-11T17:45:00", gymCase.data_inizio_iso)
        assertEquals("2026-10-11T20:00:00", gymCase.data_fine_iso)
        assertTrue(gymCase.cliente.contains("Tedo"))
        assertTrue(gymCase.luogo.contains("Palestra a Lurate Caccivio"))

        // 2. Test diretto extractTargetTime con range vari
        val range1 = engine.extractTargetTime("alle 17:45 fino alle 20")
        assertNotNull(range1)
        assertEquals(LocalTime.of(17, 45), range1!!.first)
        assertEquals(LocalTime.of(20, 0), range1.second)

        val range2 = engine.extractTargetTime("dalle 17:45 alle 20")
        assertNotNull(range2)
        assertEquals(LocalTime.of(17, 45), range2!!.first)
        assertEquals(LocalTime.of(20, 0), range2.second)

        val range3 = engine.extractTargetTime("17:45 - 20:00")
        assertNotNull(range3)
        assertEquals(LocalTime.of(17, 45), range3!!.first)
        assertEquals(LocalTime.of(20, 0), range3.second)

        // 3. Test negazioni orarie (es. "alle 17:45 non alle 20")
        val negation1 = engine.extractTargetTime("cambia l ora, andiamo in palestra alle 17:45 non alle 20")
        assertNotNull(negation1)
        assertEquals(LocalTime.of(17, 45), negation1!!.first)
        assertNull(negation1.second)

        val negation2 = engine.extractTargetTime("non alle 20 ma alle 17:45")
        assertNotNull(negation2)
        assertEquals(LocalTime.of(17, 45), negation2!!.first)
        assertNull(negation2.second)

        val negation3 = engine.extractTargetTime("sposta orario, non alle 20, facciamo alle 17:45")
        assertNotNull(negation3)
        assertEquals(LocalTime.of(17, 45), negation3!!.first)
        assertNull(negation3.second)
    }

    @Test
    fun `test unpacking openai chat completion response envelope`() {
        val openAiPayload = """
            {
              "id": "chatcmpl-987",
              "object": "chat.completion",
              "created": 1677652288,
              "choices": [{
                "index": 0,
                "message": {
                  "role": "assistant",
                  "content": "{\"has_appointment\": true, \"titolo\": \"Sopralluogo CTU\"}"
                }
              }]
            }
        """.trimIndent()

        val extracted = engine.extractContentFromApiResponse(openAiPayload)
        assertNotNull(extracted)
        assertTrue(extracted!!.contains("\"has_appointment\": true"))
        assertTrue(extracted.contains("\"Sopralluogo CTU\""))
    }

    @Test
    fun `test unpacking ollama native generate response envelope`() {
        val ollamaPayload = """
            {
              "model": "qwen2.5:1.5b",
              "created_at": "2026-10-19T09:00:00Z",
              "response": "{\"has_appointment\": false, \"motivo\": \"Nessun appuntamento\"}",
              "done": true
            }
        """.trimIndent()

        val extracted = engine.extractContentFromApiResponse(ollamaPayload)
        assertNotNull(extracted)
        assertTrue(extracted!!.contains("\"has_appointment\": false"))
    }

    @Test
    fun `test buildClassificationPrompt structure studio profile injection and zero cantiere`() {
        val config = AiEngineConfig(
            studioProfile = com.example.assistenteperitomobile.domain.StudioProfile(
                studioName = "Studio Tecnico Restelli & Associati",
                professionistaNome = "Geom. Mario Rossi"
            )
        )
        val prompt = engine.buildClassificationPrompt("Documento di prova perizia", refDate, config)

        // 1. Iniezione dinamica dello studio
        assertTrue(prompt.contains("Studio Tecnico Restelli & Associati"))

        // 2. Presenza di tutte le 6 categorie peritali ufficiali
        assertTrue(prompt.contains("Atti_Tribunale"))
        assertTrue(prompt.contains("Perizie_Tecniche"))
        assertTrue(prompt.contains("Planimetrie_Computi"))
        assertTrue(prompt.contains("Fatture_Preventivi"))
        assertTrue(prompt.contains("Corrispondenza_Legale"))
        assertTrue(prompt.contains("Da_Classificare_Manualmente"))

        // 3. Regole anti-allucinazione e schema JSON
        assertTrue(prompt.contains("ANTI-ALLUCINAZIONE"))
        assertTrue(prompt.contains("cliente_o_controparte"))
        assertTrue(prompt.contains("data_documento"))
        assertTrue(prompt.contains("<esempio>"))

        // 4. Zero occorrenze di "cantiere"
        assertFalse(prompt.lowercase().contains("cantiere"))
    }

    @Test
    fun `test buildAppointmentPrompt ISO temporal anchoring few-shots and zero cantiere`() {
        val config = AiEngineConfig(
            studioProfile = com.example.assistenteperitomobile.domain.StudioProfile(
                studioName = "Studio Tecnico Restelli & Associati",
                professionistaNome = "Geom. Mario Rossi"
            )
        )
        val testNow = LocalDateTime.of(2026, 10, 19, 14, 30)
        val prompt = engine.buildAppointmentPrompt("domani alle 10 incontro con Deiv", testNow, config)

        // 1. Iniezione dinamica dello studio
        assertTrue(prompt.contains("Studio Tecnico Restelli & Associati"))

        // 2. Ancoraggio temporale ISO
        assertTrue(prompt.contains("2026-10-19T14:30"))
        assertTrue(prompt.contains("2026-10-20")) // domani

        // 3. Presenza di regole, schema e few-shots
        assertTrue(prompt.contains("has_appointment"))
        assertTrue(prompt.contains("data_inizio_iso"))
        assertTrue(prompt.contains("data_fine_iso"))
        assertTrue(prompt.contains("ANTI-ALLUCINAZIONE"))
        assertTrue(prompt.contains("<esempio>"))

        // 4. Zero occorrenze di "cantiere"
        assertFalse(prompt.lowercase().contains("cantiere"))
    }

    @Test
    fun `test powerful reasoning model 4B configuration and frob qwen35-instruct 4b tag`() {
        val powerful = com.example.assistenteperitomobile.data.DownloadableAiModel(
            id = "qwen35_4b_instruct_q4km",
            roleBadge = "🚀 REASONING POTENZIATO (4B)",
            title = "Qwen 3.5 - 4B Instruct (Potenziato)",
            subtitle = "Versione 4B ad alta capacità logica per perizie complesse e stime articolate (2.74 GB). Suggerito sulla card: frob/qwen3.5-instruct:4b (Ollama / GGUF).",
            sizeLabel = "2.74 GB",
            expectedBytes = 2_942_177_280L,
            fileName = "Qwen3.5-4B-Q4_K_M.gguf",
            downloadUrl = "https://huggingface.co/bartowski/Qwen_Qwen3.5-4B-GGUF/resolve/main/Qwen3.5-4B-Q4_K_M.gguf",
            isPrimaryReasoning = true,
            suggestedOllamaTag = "frob/qwen3.5-instruct:4b"
        )
        assertEquals("qwen35_4b_instruct_q4km", powerful.id)
        assertEquals("frob/qwen3.5-instruct:4b", powerful.suggestedOllamaTag)
        assertEquals("Qwen3.5-4B-Q4_K_M.gguf", powerful.fileName)
        assertTrue(powerful.isPrimaryReasoning)

        val config = AiEngineConfig(reasoningModelId = "qwen35_4b_instruct_q4km")
        assertEquals("qwen35_4b_instruct_q4km", config.reasoningModelId)

        // Verify zero occurrences of "cantiere" in prompts
        val prompt = engine.buildClassificationPrompt("Documento perizia", refDate, config)
        assertFalse(prompt.lowercase().contains("cantiere"))
    }
}

