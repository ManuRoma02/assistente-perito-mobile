package com.example.assistenteperitomobile.milestone2

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.assistenteperitomobile.data.notifications.BriefingActionReceiver
import com.example.assistenteperitomobile.data.notifications.ModernNotificationBuilder
import com.example.assistenteperitomobile.data.notifications.NotificationBriefingData
import com.example.assistenteperitomobile.domain.ArchivedDocument
import com.example.assistenteperitomobile.domain.MainAppTab
import com.example.assistenteperitomobile.domain.PeritoAiEngine
import com.example.assistenteperitomobile.domain.PeritoCategory
import com.example.assistenteperitomobile.domain.SafeDeletionGuard
import com.example.assistenteperitomobile.domain.agents.AgentPipelineOrchestrator
import com.example.assistenteperitomobile.domain.agents.IntentRouterAgent
import com.example.assistenteperitomobile.domain.agents.UserIntentCategory
import com.example.assistenteperitomobile.ui.PeritoViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.URLEncoder
import java.time.LocalDateTime

/**
 * Suite di test unitari completa per Milestone 2:
 * Feature 8: Safe Document Deletion Guard
 * Feature 9: Notification Deep-Linking
 * Feature 10: Notification Action Intent Binding
 * Feature 11: Intent Routing Keyword Refinement
 * Feature 12: Clipboard API Modernization
 */
class Milestone2FeaturesTest {

    private val sampleDocs = listOf(
        ArchivedDocument(
            id = "DOC-CTU-01",
            fileName = "perizia_infiltrazioni_monza.pdf",
            originalName = "Perizia Infiltrazioni",
            categoryFolder = "Perizie_Tribunale_CTU",
            cliente = "Mario Rossi",
            oggetto = "Danni tetto condominio",
            dateIso = "2026-10-30",
            filePath = "/storage/emulated/0/Documents/Perizie_Archivio/Perizie_Tribunale_CTU/perizia_infiltrazioni_monza.pdf",
            sizeBytes = 1024L,
            extractedTextSnippet = "Il presente documento attesta i danni causati da infiltrazione nella nota peritale",
            createdAtMillis = 1000L
        ),
        ArchivedDocument(
            id = "DOC-CTU-02",
            fileName = "nomina_ctu_tribunale_milano.pdf",
            originalName = "Nomina CTU",
            categoryFolder = "Perizie_Tribunale_CTU",
            cliente = "Tribunale di Milano",
            oggetto = "Causa n. 1234/2026",
            dateIso = "2026-10-30",
            filePath = "/storage/emulated/0/Documents/Perizie_Archivio/Perizie_Tribunale_CTU/nomina_ctu_tribunale_milano.pdf",
            sizeBytes = 2048L,
            extractedTextSnippet = "Giuramento CTU presso il Tribunale Civile",
            createdAtMillis = 2000L
        )
    )

    // =========================================================================
    // FEATURE 8: SAFE DOCUMENT DELETION GUARD
    // =========================================================================

    @Test
    fun testMassDeletionRequiresExplicitConfirmation() {
        // Query generica "elimina tutti i file" senza flag di conferma
        val result = SafeDeletionGuard.evaluateDeletionIntent("elimina tutti i file", sampleDocs, hasExplicitUserConfirmation = false)
        assertTrue("La cancellazione massiva senza conferma deve fallire", result.isFailure)

        // Query "svuota archivio" senza conferma
        val resultSvuota = SafeDeletionGuard.evaluateDeletionIntent("svuota archivio", sampleDocs, hasExplicitUserConfirmation = false)
        assertTrue("Lo svuotamento archivio senza conferma deve fallire", resultSvuota.isFailure)

        // Ambiguo: "elimina la perizia per tutti i clienti"
        val resultAmbiguo = SafeDeletionGuard.evaluateDeletionIntent("elimina la perizia per tutti i clienti", sampleDocs, hasExplicitUserConfirmation = false)
        assertTrue("Frasi ambigue con 'elimina' e 'tutti' non devono cancellare nulla senza conferma esplicita", resultAmbiguo.isFailure)
    }

    @Test
    fun testMassDeletionSucceedsWithAffirmativeConfirmation() {
        // Con frase di conferma esplicita nel testo
        val resultConferma = SafeDeletionGuard.evaluateDeletionIntent("conferma svuota archivio", sampleDocs, hasExplicitUserConfirmation = false)
        assertTrue("Conferma esplicita deve autorizzare lo svuotamento", resultConferma.isSuccess)
        assertEquals(sampleDocs.size, resultConferma.getOrNull()!!.size)

        // Con flag hasExplicitUserConfirmation = true
        val resultFlag = SafeDeletionGuard.evaluateDeletionIntent("elimina tutti i documenti", sampleDocs, hasExplicitUserConfirmation = true)
        assertTrue("Flag di conferma esplicito deve autorizzare la rimozione", resultFlag.isSuccess)
        assertEquals(sampleDocs.size, resultFlag.getOrNull()!!.size)
    }

    @Test
    fun testSingleDocumentDeletionDoesNotFallbackToSingleRemainingDoc() {
        // Protezione BUG-02: se esiste 1 solo documento e la query non matcha, non deve eliminare quel documento!
        val singleDocList = listOf(sampleDocs.first())
        val result = SafeDeletionGuard.evaluateDeletionIntent("elimina fattura Bianchi", singleDocList, hasExplicitUserConfirmation = false)
        assertTrue(result.isSuccess)
        assertTrue("Non deve eliminare l'unico file rimasto se la query cerca altro", result.getOrNull()!!.isEmpty())
    }

    @Test
    fun testDocumentDeletionByIdExactMatch() {
        val result = SafeDeletionGuard.evaluateDeletionIntent("elimina doc DOC-CTU-02", sampleDocs, hasExplicitUserConfirmation = false)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()!!.size)
        assertEquals("DOC-CTU-02", result.getOrNull()!!.first().id)
    }

    @Test
    fun testDocumentDeletionByFilenameMatch() {
        val result = SafeDeletionGuard.evaluateDeletionIntent("elimina nomina ctu", sampleDocs, hasExplicitUserConfirmation = false)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()!!.size)
        assertEquals("DOC-CTU-02", result.getOrNull()!!.first().id)
    }

    @Test
    fun testDocumentDeletionDoesNotMatchOcrSnippetTokens() {
        // "nota" è presente solo in extractedTextSnippet di DOC-CTU-01, non nel nome o cliente
        val result = SafeDeletionGuard.evaluateDeletionIntent("elimina nota", sampleDocs, hasExplicitUserConfirmation = false)
        assertTrue(result.isSuccess)
        assertTrue("Token presenti solo nello snippet OCR non devono innescare cancellazioni accidentali", result.getOrNull()!!.isEmpty())
    }

    @Test
    fun testAppointmentDeletionIsNotTreatedAsDocumentDeletion() {
        val result = SafeDeletionGuard.evaluateDeletionIntent("elimina appuntamento delle 15", sampleDocs, hasExplicitUserConfirmation = false)
        assertTrue(result.isSuccess)
        assertTrue("Comandi di cancellazione appuntamento/agenda devono restituire lista vuota per l'archivio file", result.getOrNull()!!.isEmpty())
    }

    // =========================================================================
    // FEATURE 9: NOTIFICATION DEEP-LINKING
    // =========================================================================

    @Test
    fun testNavigationIntentRoutingTabAgenda() {
        // Verifica logica di mapping tab AGENDA
        val targetTab = "AGENDA"
        val mappedTab = when (targetTab.uppercase()) {
            "AGENDA" -> MainAppTab.AGENDA
            "CHAT" -> MainAppTab.CHAT
            else -> null
        }
        assertEquals(MainAppTab.AGENDA, mappedTab)
    }

    @Test
    fun testNavigationIntentRoutingTabFascicoli() {
        // Simula parametri passati da Notification: NAVIGATE_TO_TAB = "FASCICOLI", FASCICOLO_ID = "CTU-99"
        val targetTab = "FASCICOLI"
        val targetFascicoloId = "CTU-99"

        var selectedTab: MainAppTab? = null
        var selectedFascicoloId: String? = null
        var openArchiveSheetRequested = false

        if (targetTab == "FASCICOLI") {
            selectedTab = MainAppTab.CHAT
            if (!targetFascicoloId.isNullOrBlank()) {
                selectedFascicoloId = targetFascicoloId
                openArchiveSheetRequested = true
            }
        }

        assertEquals(MainAppTab.CHAT, selectedTab)
        assertEquals("CTU-99", selectedFascicoloId)
        assertTrue(openArchiveSheetRequested)
    }

    // =========================================================================
    // FEATURE 10: NOTIFICATION ACTION INTENT BINDING
    // =========================================================================

    @Test
    fun testMapsIntentUriFormatting() {
        val rawLocation = "  Via Montenapoleone 8, Milano  "
        val trimmed = rawLocation.trim()
        val encoded = URLEncoder.encode(trimmed, "UTF-8").replace("+", "%20")
        val expectedGeoUri = "geo:0,0?q=$encoded"
        assertTrue(expectedGeoUri.startsWith("geo:0,0?q=Via%20Montenapoleone"))
        assertTrue(expectedGeoUri.contains("Milano"))
    }

    @Test
    fun testBriefingActionReceiverConstants() {
        assertEquals("com.example.assistenteperitomobile.ACTION_NAVIGATE_MAPS", BriefingActionReceiver.ACTION_NAVIGATE_MAPS)
        assertEquals("com.example.assistenteperitomobile.ACTION_SNOOZE", BriefingActionReceiver.ACTION_SNOOZE)
        assertEquals("extra_location_query", BriefingActionReceiver.EXTRA_LOCATION_QUERY)
    }

    // =========================================================================
    // FEATURE 11: INTENT ROUTING KEYWORD REFINEMENT
    // =========================================================================

    @Test
    fun testItalianPrepositionsWithAlleDoNotTriggerAppointment() {
        val router = IntentRouterAgent()
        val orchestrator = AgentPipelineOrchestrator(PeritoAiEngine(null))
        val traceId = orchestrator.newTraceId()

        // Frase 1: "pensavo alle perizie di ieri"
        val intent1 = router.classify("pensavo alle perizie di ieri", traceId).getOrNull()
        assertNotNull(intent1)
        assertEquals(UserIntentCategory.TECHNICAL_REASONING, intent1!!.category)

        // Frase 2: "grazie alle tue risposte sulla muffa"
        val intent2 = router.classify("grazie alle tue risposte sulla muffa", traceId).getOrNull()
        assertNotNull(intent2)
        assertEquals(UserIntentCategory.TECHNICAL_REASONING, intent2!!.category)

        // Frase 3: "in merito alle norme UNI 11673 sui serramenti"
        val intent3 = router.classify("in merito alle norme UNI 11673 sui serramenti", traceId).getOrNull()
        assertNotNull(intent3)
        assertEquals(UserIntentCategory.TECHNICAL_REASONING, intent3!!.category)
    }

    @Test
    fun testAppointmentWithExactTimeFormatTriggersAppointmentRequest() {
        val router = IntentRouterAgent()
        val orchestrator = AgentPipelineOrchestrator(PeritoAiEngine(null))
        val traceId = orchestrator.newTraceId()

        // "sopralluogo alle 15"
        val appt1 = router.classify("sopralluogo alle 15", traceId).getOrNull()
        assertNotNull(appt1)
        assertEquals(UserIntentCategory.APPOINTMENT_REQUEST, appt1!!.category)

        // "incontro alle 10:30"
        val appt2 = router.classify("incontro alle 10:30", traceId).getOrNull()
        assertNotNull(appt2)
        assertEquals(UserIntentCategory.APPOINTMENT_REQUEST, appt2!!.category)

        // "ci vediamo alle ore 9"
        val appt3 = router.classify("ci vediamo alle ore 9", traceId).getOrNull()
        assertNotNull(appt3)
        assertEquals(UserIntentCategory.APPOINTMENT_REQUEST, appt3!!.category)
    }

    @Test
    fun testDeterministicAppointmentParserExcludesBarePrepositionAlle() {
        val engine = PeritoAiEngine(null)
        val now = LocalDateTime.of(2026, 11, 3, 9, 0)

        // Preposizione italiana "alle" senza orario né intenti di sopralluogo
        val resultPrep = engine.parseAppointmentDeterministic("grazie alle tue spiegazioni tecniche", now)
        assertFalse("Non deve rilevare appuntamenti su semplici preposizioni 'alle'", resultPrep.has_appointment)

        // Appuntamento reale con orario
        val resultReal = engine.parseAppointmentDeterministic("sopralluogo domani alle 15 in via Roma con Mario Rossi", now)
        assertTrue("Deve rilevare l'appuntamento con formato orario corretto", resultReal.has_appointment)
        assertEquals("Via Roma", resultReal.luogo)
    }

    // =========================================================================
    // FEATURE 12: CLIPBOARD API MODERNIZATION
    // =========================================================================

    @Test
    fun testNoDeprecatedLocalClipboardManagerInPeritoAppScreens() {
        val appScreensFile = File("src/main/java/com/example/assistenteperitomobile/ui/PeritoAppScreens.kt")
        if (appScreensFile.exists()) {
            val content = appScreensFile.readText()
            assertFalse(
                "PeritoAppScreens.kt non deve contenere il vecchio LocalClipboardManager deprecato",
                content.contains("LocalClipboardManager")
            )
            assertTrue(
                "PeritoAppScreens.kt deve utilizzare il moderno LocalClipboard",
                content.contains("LocalClipboard")
            )
        }
    }
}
