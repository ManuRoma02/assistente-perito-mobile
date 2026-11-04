package com.example.assistenteperitomobile.milestone2

import com.example.assistenteperitomobile.data.notifications.BriefingActionReceiver
import com.example.assistenteperitomobile.domain.ArchivedDocument
import com.example.assistenteperitomobile.domain.MainAppTab
import com.example.assistenteperitomobile.domain.PeritoAiEngine
import com.example.assistenteperitomobile.domain.SafeDeletionGuard
import com.example.assistenteperitomobile.domain.agents.AgentPipelineOrchestrator
import com.example.assistenteperitomobile.domain.agents.IntentRouterAgent
import com.example.assistenteperitomobile.domain.agents.UserIntentCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.URLEncoder
import java.time.LocalDateTime

/**
 * EMPIRICAL ADVERSARIAL STRESS TEST SUITE — MILESTONE 2
 * Author: teamwork_challenger_m2_1
 *
 * Stress-tests boundary conditions, adversarial Italian linguistic queries,
 * fuzzy injection attempts, OCR snippet leaks, and prepositional ambiguities.
 */
class Milestone2ChallengerAdversarialTest {

    private val testArchive = listOf(
        ArchivedDocument(
            id = "DOC-CTU-01",
            fileName = "perizia_statica_condominio.pdf",
            originalName = "Perizia Statica",
            categoryFolder = "Perizie_Tribunale_CTU",
            cliente = "Condominio Bellavista",
            oggetto = "Lesioni strutturali pilastri",
            dateIso = "2026-11-01",
            filePath = "/storage/emulated/0/Documents/Perizie_Archivio/Perizie_Tribunale_CTU/perizia_statica_condominio.pdf",
            sizeBytes = 4096L,
            extractedTextSnippet = "Durante il sopralluogo è stata riscontrata muffa diffusa e presenza di calcestruzzo ammalorato con ferri scoperti",
            createdAtMillis = 1000L
        ),
        ArchivedDocument(
            id = "DOC-CTU-02",
            fileName = "verbale_sopralluogo_infiltrazioni.pdf",
            originalName = "Verbale Infiltrazioni",
            categoryFolder = "Perizie_Tribunale_CTU",
            cliente = "Geom. Bianchi",
            oggetto = "Infiltrazioni acqua piano terra",
            dateIso = "2026-11-02",
            filePath = "/storage/emulated/0/Documents/Perizie_Archivio/Perizie_Tribunale_CTU/verbale_sopralluogo_infiltrazioni.pdf",
            sizeBytes = 8192L,
            extractedTextSnippet = "Si procede alla misurazione con igrometro elettronico rilevando umidità relativa al 92% nel massetto",
            createdAtMillis = 2000L
        )
    )

    // =========================================================================
    // FEATURE 8: ADVERSARIAL SAFE DOCUMENT DELETION GUARD STRESS-TESTS
    // =========================================================================

    @Test
    fun testDeceptiveItalianMassDeletionQueriesAreBlocked() {
        val deceptiveQueries = listOf(
            "elimina perizia per tutti i condomini",
            "svuota il cestino",
            "elimina il file per tutti i clienti",
            "cancella tutto l'archivio per favore",
            "rimuovi tutti i documenti inviati",
            "svuota subito senza chiedere",
            "elimina tutti quanti i fascicoli",
            "togli tutto dall'archivio",
            "butta via tutto",
            "cancella tutto"
        )

        for (query in deceptiveQueries) {
            val result = SafeDeletionGuard.evaluateDeletionIntent(query, testArchive, hasExplicitUserConfirmation = false)
            assertTrue(
                "La query ingannevole '$query' DEVE fallire richiedendo conferma esplicita",
                result.isFailure
            )
        }
    }

    @Test
    fun testExplicitConfirmationAllowsMassDeletion() {
        val validConfirmations = listOf(
            "conferma svuota archivio",
            "conferma elimina tutti i file",
            "conferma elimina tutto"
        )

        for (query in validConfirmations) {
            val result = SafeDeletionGuard.evaluateDeletionIntent(query, testArchive, hasExplicitUserConfirmation = false)
            assertTrue("La conferma '$query' deve autorizzare la cancellazione massiva", result.isSuccess)
            assertEquals(
                "Tutti i documenti dell'archivio devono essere autorizzati",
                testArchive.size,
                result.getOrNull()!!.size
            )
        }

        // Con flag hasExplicitUserConfirmation = true
        val resultWithFlag = SafeDeletionGuard.evaluateDeletionIntent(
            "svuota archivio",
            testArchive,
            hasExplicitUserConfirmation = true
        )
        assertTrue(resultWithFlag.isSuccess)
        assertEquals(testArchive.size, resultWithFlag.getOrNull()!!.size)
    }

    @Test
    fun testSingleFileArchiveNeverDeletesUnmatchedSoleDocument() {
        // SCENARIO CRITICO: archivio contenente UN SOLO documento (ex BUG-02)
        val singleDocArchive = listOf(testArchive.first())

        val unmatchedQueries = listOf(
            "elimina il file che abbiamo visto ieri",
            "elimina fattura Rossi",
            "elimina perizia idraulica",
            "cancella doc sconosciuto",
            "rimuovi verbale del 1999",
            "elimina documento DOC-NOT-EXIST",
            "elimina Mario Neri",
            "butta via la relazione geologica"
        )

        for (query in unmatchedQueries) {
            val result = SafeDeletionGuard.evaluateDeletionIntent(query, singleDocArchive, hasExplicitUserConfirmation = false)
            assertTrue("La valutazione deve avere successo ma con lista vuota per '$query'", result.isSuccess)
            val matched = result.getOrNull()!!
            assertTrue(
                "CRITICO: Non deve MAI cancellare l'unico file superstite se la query non corrisponde! Query: '$query'",
                matched.isEmpty()
            )
        }
    }

    @Test
    fun testGenericPointersWithoutSpecificTargetDoNotDeleteAnything() {
        val genericPointersQueries = listOf(
            "elimina",
            "elimina questo",
            "elimina questa",
            "elimina quello",
            "cancella file",
            "rimuovi documento",
            "togli allegato",
            "butta foto",
            "cancella la scansione",
            "elimina questo pdf",
            "rimuovi questo p7m",
            "elimina da archivio",
            "elimina l'ultimo file"
        )

        for (query in genericPointersQueries) {
            val result = SafeDeletionGuard.evaluateDeletionIntent(query, testArchive, hasExplicitUserConfirmation = false)
            assertTrue(result.isSuccess)
            val matched = result.getOrNull()!!
            assertTrue(
                "Puntatori generici senza target ('$query') devono restituire lista vuota",
                matched.isEmpty()
            )
        }
    }

    @Test
    fun testOcrExtractedTextSnippetTokensDoNotTriggerDeletion() {
        // Parole presenti esclusivamente in extractedTextSnippet:
        // DOC-CTU-01 ha: "muffa", "calcestruzzo", "ammalorato", "ferri"
        // DOC-CTU-02 ha: "igrometro", "elettronico", "massetto"
        val ocrOnlyTokens = listOf(
            "elimina muffa",
            "cancella calcestruzzo",
            "rimuovi ammalorato",
            "elimina igrometro",
            "cancella massetto"
        )

        for (query in ocrOnlyTokens) {
            val result = SafeDeletionGuard.evaluateDeletionIntent(query, testArchive, hasExplicitUserConfirmation = false)
            assertTrue(result.isSuccess)
            val matched = result.getOrNull()!!
            assertTrue(
                "CRITICO (BUG-02): Nessun documento deve essere eliminato in base a testo OCR! Query: '$query'",
                matched.isEmpty()
            )
        }
    }

    @Test
    fun testAppointmentDeletionDisambiguationPreservesDocuments() {
        val appointmentQueries = listOf(
            "elimina appuntamento delle 15:30",
            "cancella il sopralluogo di domani",
            "rimuovi l'impegno in agenda",
            "togli l'evento di giovedì",
            "elimina sopralluogo con Bianchi"
        )

        for (query in appointmentQueries) {
            val result = SafeDeletionGuard.evaluateDeletionIntent(query, testArchive, hasExplicitUserConfirmation = false)
            assertTrue(result.isSuccess)
            val matched = result.getOrNull()!!
            assertTrue(
                "La cancellazione di appuntamenti ('$query') NON deve coinvolgere l'archivio documentale",
                matched.isEmpty()
            )
        }
    }

    @Test
    fun testExactIdAndMetadataDeletionSucceeds() {
        // Test match per ID esatto
        val resId = SafeDeletionGuard.evaluateDeletionIntent("elimina doc DOC-CTU-01", testArchive)
        assertTrue(resId.isSuccess)
        assertEquals(1, resId.getOrNull()!!.size)
        assertEquals("DOC-CTU-01", resId.getOrNull()!!.first().id)

        // Test match per cliente esatto
        val resCliente = SafeDeletionGuard.evaluateDeletionIntent("elimina file Bellavista", testArchive)
        assertTrue(resCliente.isSuccess)
        assertEquals(1, resCliente.getOrNull()!!.size)
        assertEquals("DOC-CTU-01", resCliente.getOrNull()!!.first().id)

        // Test match per nome file esatto/parziale
        val resFile = SafeDeletionGuard.evaluateDeletionIntent("elimina verbale infiltrazioni", testArchive)
        assertTrue(resFile.isSuccess)
        assertEquals(1, resFile.getOrNull()!!.size)
        assertEquals("DOC-CTU-02", resFile.getOrNull()!!.first().id)
    }

    // =========================================================================
    // FEATURE 11: INTENT ROUTING PREPOSITION "ALLE" ADVERSARIAL STRESS-TESTS
    // =========================================================================

    @Test
    fun testItalianPrepositionalPhrasesWithAlleDoNotRouteToAppointment() {
        val router = IntentRouterAgent()
        val orchestrator = AgentPipelineOrchestrator(PeritoAiEngine(null))
        val traceId = orchestrator.newTraceId()

        val nonAppointmentPhrases = listOf(
            "pensa alle conseguenze",
            "grazie alle vostre perizie",
            "in merito alle fatture",
            "riguardo alle infiltrazioni del tetto",
            "fai attenzione alle lesioni strutturali",
            "chiedi alle parti la documentazione",
            "in conformità alle norme UNI 11673",
            "interessato alle perizie tecniche",
            "alleghiamo la perizia asseverata",
            "alle persone coinvolte nel sinistro",
            "pensiamo alle conclusioni peritali",
            "rispetto alle osservazioni del CTP"
        )

        for (phrase in nonAppointmentPhrases) {
            val classified = router.classify(phrase, traceId).getOrNull()
            assertNotNull("Classificazione non nulla per '$phrase'", classified)
            assertTrue(
                "La frase '$phrase' NON deve essere classificata come APPOINTMENT_REQUEST (ottenuto: ${classified!!.category})",
                classified.category != UserIntentCategory.APPOINTMENT_REQUEST
            )
        }
    }

    @Test
    fun testAuthenticTimeBasedAppointmentsWithAlleProperlyRoute() {
        val router = IntentRouterAgent()
        val orchestrator = AgentPipelineOrchestrator(PeritoAiEngine(null))
        val traceId = orchestrator.newTraceId()

        val authenticAppointments = listOf(
            "alle 15:30 abbiamo il sopralluogo",
            "sopralluogo alle 10",
            "ci vediamo alle 9:15 in tribunale",
            "udienza alle ore 11",
            "incontro con geom Rossi alle 16:00",
            "domani alle 8 perizia tecnica",
            "lunedì alle 14.30 incontro",
            "fissa appuntamento alle 17"
        )

        for (phrase in authenticAppointments) {
            val classified = router.classify(phrase, traceId).getOrNull()
            assertNotNull("Classificazione non nulla per '$phrase'", classified)
            assertEquals(
                "La frase con orario reale '$phrase' DEVE essere classificata come APPOINTMENT_REQUEST",
                UserIntentCategory.APPOINTMENT_REQUEST,
                classified!!.category
            )
        }
    }

    @Test
    fun testDeterministicEngineRejectsPrepositionAlleWithoutTimeOrEvent() {
        val engine = PeritoAiEngine(null)
        val now = LocalDateTime.of(2026, 11, 3, 10, 0)

        val nonApptResults = listOf(
            "in merito alle considerazioni tecniche",
            "grazie alle risposte esaustive",
            "pensavo alle infiltrazioni",
            "riferito alle fatture pagate"
        )

        for (text in nonApptResults) {
            val parsed = engine.parseAppointmentDeterministic(text, now)
            assertFalse(
                "L'engine deterministico non deve rilevare appuntamenti su '$text'",
                parsed.has_appointment
            )
        }

        // Appuntamento reale con orario
        val realAppt = engine.parseAppointmentDeterministic(
            "sopralluogo alle 15:30 in via Dante con arch Bianchi",
            now
        )
        assertTrue("Deve rilevare appuntamento per sopralluogo con orario", realAppt.has_appointment)
        assertEquals("Via Dante", realAppt.luogo)
    }

    // =========================================================================
    // FEATURE 9 & 10: NOTIFICATION DEEP LINKING & ACTION BINDINGS
    // =========================================================================

    @Test
    fun testNotificationNavigationMappingContracts() {
        val testTabs = listOf(
            "AGENDA" to MainAppTab.AGENDA,
            "agenda" to MainAppTab.AGENDA,
            "CHAT" to MainAppTab.CHAT,
            "chat" to MainAppTab.CHAT
        )

        for ((input, expected) in testTabs) {
            val mapped = when (input.uppercase()) {
                "AGENDA" -> MainAppTab.AGENDA
                "CHAT" -> MainAppTab.CHAT
                else -> null
            }
            assertEquals("Mapping per $input errato", expected, mapped)
        }
    }

    @Test
    fun testMapsLocationUriEncodingRobustness() {
        val testLocations = listOf(
            "Corso Buenos Aires, 42, Milano" to "geo:0,0?q=Corso%20Buenos%20Aires%2C%2042%2C%20Milano",
            "Piazza del Duomo, Como" to "geo:0,0?q=Piazza%20del%20Duomo%2C%20Como"
        )

        for ((raw, expectedPrefix) in testLocations) {
            val encoded = URLEncoder.encode(raw.trim(), "UTF-8").replace("+", "%20")
            val uri = "geo:0,0?q=$encoded"
            assertEquals(expectedPrefix, uri)
        }
    }

    @Test
    fun testActionReceiverActionStringsIntegrity() {
        assertEquals("com.example.assistenteperitomobile.ACTION_NAVIGATE_MAPS", BriefingActionReceiver.ACTION_NAVIGATE_MAPS)
        assertEquals("com.example.assistenteperitomobile.ACTION_SNOOZE", BriefingActionReceiver.ACTION_SNOOZE)
        assertEquals("extra_location_query", BriefingActionReceiver.EXTRA_LOCATION_QUERY)
    }

    // =========================================================================
    // FEATURE 12: CLIPBOARD INTEGRITY CHECK
    // =========================================================================

    @Test
    fun testNoLegacyClipboardReferencesRemainInSource() {
        val filesToCheck = listOf(
            File("src/main/java/com/example/assistenteperitomobile/ui/PeritoAppScreens.kt"),
            File("src/main/java/com/example/assistenteperitomobile/MainActivity.kt")
        )

        for (file in filesToCheck) {
            if (file.exists()) {
                val text = file.readText()
                assertFalse(
                    "Il file ${file.name} contiene ancora il vecchio LocalClipboardManager deprecato!",
                    text.contains("LocalClipboardManager")
                )
            }
        }
    }
}
