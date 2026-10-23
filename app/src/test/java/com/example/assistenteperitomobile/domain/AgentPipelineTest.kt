package com.example.assistenteperitomobile.domain

import com.example.assistenteperitomobile.domain.agents.AgentPipelineOrchestrator
import com.example.assistenteperitomobile.domain.agents.AgentStatus
import com.example.assistenteperitomobile.domain.agents.IntentRouterAgent
import com.example.assistenteperitomobile.domain.agents.UserIntentCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class AgentPipelineTest {

    private val router = IntentRouterAgent()
    private val aiEngine = PeritoAiEngine(context = null)
    private val orchestrator = AgentPipelineOrchestrator(aiEngine)

    @Test
    fun testIntentRouterSlashCommands() {
        val traceId = orchestrator.newTraceId()
        val intentToday = router.classify("/oggi", traceId).getOrNull()
        assertNotNull(intentToday)
        assertEquals(UserIntentCategory.SLASH_COMMAND, intentToday!!.category)
        assertEquals("/oggi", intentToday.command)

        val intentSearch = router.classify("/cerca perizia infiltrazioni", traceId).getOrNull()
        assertNotNull(intentSearch)
        assertEquals(UserIntentCategory.SLASH_COMMAND, intentSearch!!.category)
        assertEquals("/cerca", intentSearch.command)
        assertEquals("perizia infiltrazioni", intentSearch.cleanedQuery)
    }

    @Test
    fun testIntentRouterNaturalLanguage() {
        val traceId = orchestrator.newTraceId()

        val briefRes = router.classify("dimmi il briefing di oggi", traceId).getOrNull()
        assertNotNull(briefRes)
        assertEquals(UserIntentCategory.DAILY_BRIEFING, briefRes!!.category)

        val searchRes = router.classify("cerca fattura geom rossi", traceId).getOrNull()
        assertNotNull(searchRes)
        assertEquals(UserIntentCategory.ARCHIVE_SEARCH, searchRes!!.category)
        assertEquals("fattura geom rossi", searchRes.cleanedQuery)

        val deleteRes = router.classify("elimina appuntamento delle 15", traceId).getOrNull()
        assertNotNull(deleteRes)
        assertEquals(UserIntentCategory.CORRECTION_OR_DELETE, deleteRes!!.category)
        assertEquals("appuntamento delle 15", deleteRes.cleanedQuery)

        val apptRes = router.classify("sopralluogo domani alle 10:30 in via Milano", traceId).getOrNull()
        assertNotNull(apptRes)
        assertEquals(UserIntentCategory.APPOINTMENT_REQUEST, apptRes!!.category)

        val techRes = router.classify("quali sono i termini di deposito della bozza CTU secondo il cpc?", traceId).getOrNull()
        assertNotNull(techRes)
        assertEquals(UserIntentCategory.TECHNICAL_REASONING, techRes!!.category)
    }

    @Test
    fun testOrchestratorAppointmentExtractionWithTrace() = runBlocking {
        val traceId = orchestrator.newTraceId()
        val refDate = LocalDateTime.of(2026, 3, 10, 9, 0)
        val text = "Sopralluogo domani alle 15:30 in Via Roma 15 a Como con Ing. Rossi"

        val response = orchestrator.extractAppointment(
            messageText = text,
            now = refDate,
            config = AiEngineConfig(),
            traceId = traceId
        )

        assertEquals(AgentStatus.SUCCESS, response.metrics.status)
        assertEquals(traceId, response.metrics.traceId)
        assertEquals("AppointmentExtractorAgent", response.metrics.agentName)
        assertTrue(response.metrics.durationMs >= 0)

        val data = response.getOrNull()
        assertNotNull(data)
        assertTrue(data!!.has_appointment)
        assertEquals("2026-03-11T15:30:00", data.data_inizio_iso)
        assertTrue(data.titolo.contains("Sopralluogo"))
    }

    @Test
    fun testOrchestratorDocumentClassificationTrace() = runBlocking {
        val traceId = orchestrator.newTraceId()
        val text = "TRIBUNALE DI COMO - R.G. 1234/2026 - Verbale di Giuramento del CTU Perito Tecnico"

        val response = orchestrator.classifyDocument(
            docText = text,
            originalFileName = "Verbale_CTU.pdf",
            traceId = traceId
        )

        assertEquals(traceId, response.metrics.traceId)
        assertEquals("DocumentClassifierAgent", response.metrics.agentName)
        val data = response.getOrNull()
        assertNotNull(data)
        assertEquals(PeritoCategory.ATTI_TRIBUNALE.folderName, data!!.categoria)
    }
}
