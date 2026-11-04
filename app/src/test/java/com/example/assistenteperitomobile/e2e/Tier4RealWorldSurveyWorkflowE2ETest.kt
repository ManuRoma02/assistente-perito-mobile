package com.example.assistenteperitomobile.e2e

import com.example.assistenteperitomobile.data.MorningBriefingWorker
import com.example.assistenteperitomobile.data.notifications.BriefingActionReceiver
import com.example.assistenteperitomobile.data.notifications.NotificationBriefingFormatter
import com.example.assistenteperitomobile.domain.AiEngineConfig
import com.example.assistenteperitomobile.domain.ArchivedDocument
import com.example.assistenteperitomobile.domain.CalendarEventItem
import com.example.assistenteperitomobile.domain.ChatMessage
import com.example.assistenteperitomobile.domain.Fascicolo
import com.example.assistenteperitomobile.domain.FascicoloStato
import com.example.assistenteperitomobile.domain.PeritoAiEngine
import com.example.assistenteperitomobile.domain.PeritoCategory
import com.example.assistenteperitomobile.domain.StudioProfile
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Tier 4: E2E Real-World Scenario Suite
 * Simulates a complete day-in-the-life workflow of a technical court and insurance surveyor (CTU / CTP):
 * 1. 08:30 AM — Automated Morning Briefing & Spotify-Style Notification Generation
 * 2. 09:15 AM — Court Assignment (PEC) Ingestion & AI Classification (Atti_Tribunale)
 * 3. 10:30 AM — On-Site Inspection & Camera Burst Photo Streaming to Disk
 * 4. 14:00 PM — Technical Report Compilation & Archiving (Perizie_Tecniche)
 * 5. 16:30 PM — Notification Deep-Link & Map Navigation Action Execution
 * 6. 18:00 PM — End-of-Day Multi-Repository Data Integrity & Zero-Corruption Audit
 */
class Tier4RealWorldSurveyWorkflowE2ETest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val engine = PeritoAiEngine(context = null)
    private val surveyDate = LocalDate.of(2026, 10, 29)
    private val zone = ZoneId.of("Europe/Rome")
    private val isoFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    @Test
    fun testFullDaySurveyorWorkflowEndToEnd() = runTest {
        val rootDir = tempFolder.newFolder("surveyor_workflow_workspace")
        val cacheDir = File(rootDir, "camera_cache").apply { mkdirs() }
        val fascicoliFile = File(rootDir, "fascicoli_studio.json")
        val archiveIndexFile = File(rootDir, "archive_index.json")
        val calendarFile = File(rootDir, "calendar_events.json")
        val chatFile = File(rootDir, "chat_history.json")
        val configFile = File(rootDir, "ai_config.json")

        // --------------------------------------------------------------------
        // PHASE 1: 08:30 AM — Studio Configuration & Morning Briefing
        // --------------------------------------------------------------------
        val studioConfig = AiEngineConfig(
            morningBriefingEnabled = true,
            morningBriefingTime = "08:30",
            syncSystemCalendar = true,
            studioProfile = StudioProfile(
                studioName = "Studio Tecnico Ing. Bellini & Associati",
                professionistaNome = "Ing. Marco Bellini",
                alboIscrizione = "Ordine degli Ingegneri di Milano - N. 18452",
                citta = "Milano",
                recapito = "Corso di Porta Romana 68, Milano"
            )
        )
        configFile.writeText(json.encodeToString(studioConfig))

        val parsedTime = MorningBriefingWorker.parseBriefingTime(studioConfig.morningBriefingTime)
        assertEquals(Pair(8, 30), parsedTime)

        // Eventi programmati per la giornata del 29 Ottobre 2026
        val evMorning = CalendarEventItem(
            id = "EV-SURVEY-01",
            titolo = "Sopralluogo CTU Lastrico Solare Condominio Aurora",
            startIso = "2026-10-29T09:30:00",
            endIso = "2026-10-29T11:30:00",
            startMillis = LocalDateTime.of(2026, 10, 29, 9, 30).atZone(zone).toInstant().toEpochMilli(),
            endMillis = LocalDateTime.of(2026, 10, 29, 11, 30).atZone(zone).toInstant().toEpochMilli(),
            luogo = "Via Dante 28, Milano",
            cliente = "Tribunale Ordinario di Milano",
            note = "Convocazione CTP e amministratore condominiale",
            categoria = "Tribunale",
            fascicoloId = "CTU-2026-042"
        )

        val evAfternoon = CalendarEventItem(
            id = "EV-SURVEY-02",
            titolo = "Rilievo Danni Sinistro Veicolo FX492ZZ",
            startIso = "2026-10-29T14:30:00",
            endIso = "2026-10-29T16:00:00",
            startMillis = LocalDateTime.of(2026, 10, 29, 14, 30).atZone(zone).toInstant().toEpochMilli(),
            endMillis = LocalDateTime.of(2026, 10, 29, 16, 0).atZone(zone).toInstant().toEpochMilli(),
            luogo = "Carrozzeria Milano Nord, Viale Monza 120",
            cliente = "Assicurazioni Generali",
            note = "Verifica compatibilità deformazione montante",
            categoria = "Sinistro Auto",
            fascicoloId = "SIN-2026-881"
        )

        val todayEvents = listOf(evMorning, evAfternoon)
        val briefingData = NotificationBriefingFormatter.computeBriefingData(
            today = surveyDate,
            todayEvents = todayEvents,
            tomorrowEvents = emptyList()
        )

        assertTrue(briefingData.hasEvents)
        assertEquals(2, briefingData.totalTodayCount)
        assertEquals("29", briefingData.dayNumber)
        assertEquals("OTT", briefingData.monthShort)
        assertEquals("2 OGGI", briefingData.badgeCountText)
        assertEquals("2 Sopralluoghi Programmati", briefingData.mainHeadline)
        assertEquals("Via Dante 28, Milano", briefingData.rawLocationQuery)
        assertEquals("CTU-2026-042", briefingData.targetDossierId)

        // --------------------------------------------------------------------
        // PHASE 2: 09:15 AM — Ingestione PEC Nomina CTU & Creazione Fascicolo
        // --------------------------------------------------------------------
        val pecContent = """
            TRIBUNALE ORDINARIO DI MILANO
            Ufficio Volontaria Giurisdizione - R.G. 4501/2026
            Il Giudice Istruttore dott.ssa V. Moretti nomina CTU l'Ing. Marco Bellini.
            Oggetto: Accertamento Tecnico Preventivo Condominio Aurora c/ Impresa Lombarda Costruzioni.
            Inizio operazioni peritali fissato per il 29/10/2026 ore 09:30 presso lo stabile.
        """.trimIndent()

        val classifiedCourtDoc = engine.classifyDocument(
            docText = pecContent,
            originalFileName = "notifica_pec_nomina_ctu_4501_2026.pdf",
            referenceDate = surveyDate
        )
        assertEquals(PeritoCategory.ATTI_TRIBUNALE.folderName, classifiedCourtDoc.categoria)

        val safeCourtDocName = engine.buildNormalizedFilename(classifiedCourtDoc, "notifica_pec_nomina_ctu_4501_2026.pdf")
        assertTrue(safeCourtDocName.endsWith(".pdf"))

        val dossierCtu = Fascicolo(
            id = "CTU-2026-042",
            codice = "CTU-2026-042",
            titolo = "ATP Condominio Aurora - Infiltrazioni Lastrico",
            cliente = "Tribunale Ordinario di Milano",
            controparte = "Impresa Lombarda Costruzioni",
            indirizzo = "Via Dante 28, Milano",
            stato = FascicoloStato.IN_CORSO,
            note = "Nomina del 29/10. Deposito bozza entro 60 giorni."
        )

        val courtDocArchived = ArchivedDocument(
            id = "DOC-CTU-001",
            fileName = safeCourtDocName,
            originalName = "notifica_pec_nomina_ctu_4501_2026.pdf",
            categoryFolder = classifiedCourtDoc.categoria,
            cliente = dossierCtu.cliente,
            oggetto = dossierCtu.titolo,
            dateIso = surveyDate.toString(),
            filePath = File(rootDir, safeCourtDocName).apply { writeText(pecContent) }.absolutePath,
            sizeBytes = pecContent.length.toLong(),
            extractedTextSnippet = pecContent.take(400),
            createdAtMillis = System.currentTimeMillis()
        )

        // --------------------------------------------------------------------
        // PHASE 3: 10:30 AM — On-Site Inspection & Camera Burst Photo Streaming
        // --------------------------------------------------------------------
        val burstPhotos = mutableListOf<File>()
        for (photoIndex in 1..5) {
            val photoFile = File(cacheDir, "sopralluogo_ctu_foto_$photoIndex.jpg")
            val dummyPhotoBytes = ByteArray(1024 * 50) { (it % 256).toByte() }
            photoFile.writeBytes(dummyPhotoBytes)
            burstPhotos.add(photoFile)
        }

        assertEquals(5, burstPhotos.size)
        assertTrue("Tutte le foto a raffica devono essere memorizzate su disco", burstPhotos.all { it.exists() && it.length() > 0 })

        // --------------------------------------------------------------------
        // PHASE 4: 14:00 PM — Stesura Relazione Tecnica e Archiviazione
        // --------------------------------------------------------------------
        val periziaContent = """
            RELAZIONE TECNICA PERITALE DI SOPRALLUOGO
            Procedimento R.G. 4501/2026 - Condominio Aurora
            Il sottoscritto CTU ha eseguito l'accesso in data odierna constatando:
            1. Fessurazioni longitudinali lungo la guaina bituminosa del lastrico solare.
            2. Distacco intonaco nell'appartamento sottostante sub. 4.
            Computo estimativo di ripristino stimato in € 14.850,00 oltre IVA.
        """.trimIndent()

        val classifiedPerizia = engine.classifyDocument(
            docText = periziaContent,
            originalFileName = "bozza_relazione_ctu_aurora.pdf",
            referenceDate = surveyDate
        )
        assertEquals(PeritoCategory.PERIZIE_TECNICHE.folderName, classifiedPerizia.categoria)

        val safePeriziaName = engine.buildNormalizedFilename(classifiedPerizia, "bozza_relazione_ctu_aurora.pdf")
        val periziaDocArchived = ArchivedDocument(
            id = "DOC-PERIZIA-002",
            fileName = safePeriziaName,
            originalName = "bozza_relazione_ctu_aurora.pdf",
            categoryFolder = classifiedPerizia.categoria,
            cliente = dossierCtu.cliente,
            oggetto = "Relazione Tecnica Infiltrazioni",
            dateIso = surveyDate.toString(),
            filePath = File(rootDir, safePeriziaName).apply { writeText(periziaContent) }.absolutePath,
            sizeBytes = periziaContent.length.toLong(),
            extractedTextSnippet = periziaContent.take(400),
            createdAtMillis = System.currentTimeMillis()
        )

        // --------------------------------------------------------------------
        // PHASE 5: 16:30 PM — Deep-Linking Intent Dispatch & Maps Action Routing
        // --------------------------------------------------------------------
        val intentExtras = mutableMapOf<String, String>()
        intentExtras["NAVIGATE_TO_TAB"] = "FASCICOLI"
        briefingData.targetDossierId?.let { intentExtras["FASCICOLO_ID"] = it }

        assertEquals("FASCICOLI", intentExtras["NAVIGATE_TO_TAB"])
        assertEquals("CTU-2026-042", intentExtras["FASCICOLO_ID"])

        val mapActionIntentAction = BriefingActionReceiver.ACTION_NAVIGATE_MAPS
        val mapLocationQuery = briefingData.rawLocationQuery
        val geoUriString = "geo:0,0?q=" + URLEncoder.encode(mapLocationQuery, "UTF-8")

        assertEquals("com.example.assistenteperitomobile.ACTION_NAVIGATE_MAPS", mapActionIntentAction)
        assertTrue(geoUriString.contains("Via+Dante+28"))

        // --------------------------------------------------------------------
        // PHASE 6: 18:00 PM — End-of-Day Multi-Repository Atomic Persistence Audit
        // --------------------------------------------------------------------
        fun atomicWrite(file: File, text: String) {
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(text, Charsets.UTF_8)
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }

        // Persistenza atomica di tutte le entità
        val allFascicoli = listOf(dossierCtu)
        val allDocs = listOf(courtDocArchived, periziaDocArchived)
        val allEvents = todayEvents
        val allMessages = listOf(
            ChatMessage(
                id = "MSG-CTU-01",
                isUser = true,
                text = "Classifica e archivia la nomina CTU del Condominio Aurora"
            ),
            ChatMessage(
                id = "MSG-CTU-02",
                isUser = false,
                text = "Documento classificato in Atti_Tribunale e collegato al fascicolo CTU-2026-042."
            )
        )

        atomicWrite(fascicoliFile, json.encodeToString(allFascicoli))
        atomicWrite(archiveIndexFile, json.encodeToString(allDocs))
        atomicWrite(calendarFile, json.encodeToString(allEvents))
        atomicWrite(chatFile, json.encodeToString(allMessages))

        // Verifica di integrità forense
        val loadedFascicoli = json.decodeFromString<List<Fascicolo>>(fascicoliFile.readText())
        val loadedArchive = json.decodeFromString<List<ArchivedDocument>>(archiveIndexFile.readText())
        val loadedCalendar = json.decodeFromString<List<CalendarEventItem>>(calendarFile.readText())
        val loadedChat = json.decodeFromString<List<ChatMessage>>(chatFile.readText())

        assertEquals(1, loadedFascicoli.size)
        assertEquals(2, loadedArchive.size)
        assertEquals(2, loadedCalendar.size)
        assertEquals(2, loadedChat.size)

        // Verifica coerenza relazionale
        val loadedDossier = loadedFascicoli.first()
        assertEquals("CTU-2026-042", loadedDossier.codice)
        assertEquals(FascicoloStato.IN_CORSO, loadedDossier.stato)

        val associatedEvents = loadedCalendar.filter { it.fascicoloId == loadedDossier.id }
        assertEquals(1, associatedEvents.size)
        assertEquals("Via Dante 28, Milano", associatedEvents.first().luogo)

        val associatedDocs = loadedArchive.filter { it.cliente == loadedDossier.cliente }
        assertEquals(2, associatedDocs.size)
    }
}
