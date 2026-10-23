package com.example.assistenteperitomobile.domain.agents

import com.example.assistenteperitomobile.domain.AiEngineConfig
import com.example.assistenteperitomobile.domain.AppointmentParseResult
import com.example.assistenteperitomobile.domain.DocumentClassificationResult
import com.example.assistenteperitomobile.domain.PeritoAiEngine
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Orchestratore gerarchico della pipeline multi-agente peritale.
 * Implementa il pattern Router -> Specialized Worker -> Verifier con tracciabilità unificata.
 */
class AgentPipelineOrchestrator(
    private val aiEngine: PeritoAiEngine
) {
    val router = IntentRouterAgent()
    val appointmentAgent = AppointmentExtractorAgent(aiEngine)
    val documentAgent = DocumentClassifierAgent(aiEngine)
    val reasoningAgent = PeritoReasoningAdvisorAgent(aiEngine)

    fun newTraceId(): String = UUID.randomUUID().toString()

    fun routeMessage(rawText: String, traceId: String = newTraceId()): ClassifiedIntent {
        return router.classify(rawText, traceId).getOrNull() ?: ClassifiedIntent(
            category = UserIntentCategory.UNCLEAR,
            rawText = rawText,
            cleanedQuery = rawText,
            confidence = 0f
        )
    }

    suspend fun extractAppointment(
        messageText: String,
        now: LocalDateTime = LocalDateTime.now(),
        config: AiEngineConfig = AiEngineConfig(),
        traceId: String = newTraceId()
    ): AgentResponse<AppointmentParseResult> {
        return appointmentAgent.extract(messageText, now, config, traceId)
    }

    suspend fun classifyDocument(
        docText: String,
        originalFileName: String = "documento.pdf",
        referenceDate: LocalDate = LocalDate.now(),
        config: AiEngineConfig = AiEngineConfig(),
        traceId: String = newTraceId()
    ): AgentResponse<DocumentClassificationResult> {
        return documentAgent.classify(docText, originalFileName, referenceDate, config, traceId)
    }

    suspend fun askReasoning(
        question: String,
        config: AiEngineConfig = AiEngineConfig(),
        traceId: String = newTraceId()
    ): AgentResponse<String> {
        return reasoningAgent.answer(question, config, traceId)
    }
}
