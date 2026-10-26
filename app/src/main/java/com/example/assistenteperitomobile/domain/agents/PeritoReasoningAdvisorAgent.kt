package com.example.assistenteperitomobile.domain.agents

import com.example.assistenteperitomobile.domain.AiEngineConfig
import com.example.assistenteperitomobile.domain.PeritoAiEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive

/**
 * Agent specializzato nell'assistenza tecnica e forense (CTU, CTP, stime estimative, DPR 380/01).
 * Gestisce l'interrogazione dell'LLM di ragionamento locale con timeout protetto e recovery.
 */
class PeritoReasoningAdvisorAgent(
    private val aiEngine: PeritoAiEngine
) {

    suspend fun answer(
        question: String,
        config: AiEngineConfig = AiEngineConfig(),
        traceId: String
    ): AgentResponse<String> {
        val startNs = System.nanoTime()

        return try {
            kotlin.coroutines.coroutineContext.ensureActive()
            val answer = aiEngine.askReasoningAssistant(
                question = question,
                config = config
            )
            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000

            if (!answer.isNullOrBlank()) {
                AgentResponse.Success(
                    data = answer,
                    metrics = AgentExecutionMetrics(
                        traceId = traceId,
                        agentName = "PeritoReasoningAdvisorAgent",
                        durationMs = elapsedMs,
                        confidence = 0.90f,
                        status = AgentStatus.SUCCESS
                    )
                )
            } else {
                AgentResponse.Degraded(
                    data = "",
                    reason = "Model returned empty answer or timed out",
                    metrics = AgentExecutionMetrics(
                        traceId = traceId,
                        agentName = "PeritoReasoningAdvisorAgent",
                        durationMs = elapsedMs,
                        confidence = 0.20f,
                        status = AgentStatus.DEGRADED,
                        notes = "Inference null or empty"
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000
            AgentResponse.Failure(
                error = t,
                metrics = AgentExecutionMetrics(
                    traceId = traceId,
                    agentName = "PeritoReasoningAdvisorAgent",
                    durationMs = elapsedMs,
                    confidence = 0f,
                    status = AgentStatus.FAILED,
                    notes = t.message ?: "Reasoning generation failed"
                )
            )
        }
    }
}
