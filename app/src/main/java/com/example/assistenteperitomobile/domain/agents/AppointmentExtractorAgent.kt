package com.example.assistenteperitomobile.domain.agents

import com.example.assistenteperitomobile.domain.AiEngineConfig
import com.example.assistenteperitomobile.domain.AppointmentParseResult
import com.example.assistenteperitomobile.domain.PeritoAiEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import java.time.LocalDateTime

/**
 * Agent per l'estrazione e validazione di impegni e appuntamenti peritali.
 * Implementa la logica a fallback deterministico-first con arricchimento neurale.
 */
class AppointmentExtractorAgent(
    private val aiEngine: PeritoAiEngine
) {

    suspend fun extract(
        messageText: String,
        now: LocalDateTime = LocalDateTime.now(),
        config: AiEngineConfig = AiEngineConfig(),
        traceId: String
    ): AgentResponse<AppointmentParseResult> {
        val startNs = System.nanoTime()

        return try {
            kotlin.coroutines.coroutineContext.ensureActive()
            val result = aiEngine.parseAppointment(
                messageText = messageText,
                now = now,
                config = config,
                categories = config.categories
            )
            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000

            if (result.has_appointment) {
                if (result.is_ambiguo) {
                    AgentResponse.Degraded(
                        data = result,
                        reason = result.motivo_ambiguita,
                        metrics = AgentExecutionMetrics(
                            traceId = traceId,
                            agentName = "AppointmentExtractorAgent",
                            durationMs = elapsedMs,
                            confidence = 0.65f,
                            status = AgentStatus.DEGRADED,
                            notes = "Ambiguity detected: ${result.motivo_ambiguita}"
                        )
                    )
                } else {
                    AgentResponse.Success(
                        data = result,
                        metrics = AgentExecutionMetrics(
                            traceId = traceId,
                            agentName = "AppointmentExtractorAgent",
                            durationMs = elapsedMs,
                            confidence = 0.95f,
                            status = AgentStatus.SUCCESS
                        )
                    )
                }
            } else {
                AgentResponse.Success(
                    data = result,
                    metrics = AgentExecutionMetrics(
                        traceId = traceId,
                        agentName = "AppointmentExtractorAgent",
                        durationMs = elapsedMs,
                        confidence = 0.90f,
                        status = AgentStatus.SUCCESS,
                        notes = "No appointment found in input"
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
                    agentName = "AppointmentExtractorAgent",
                    durationMs = elapsedMs,
                    confidence = 0f,
                    status = AgentStatus.FAILED,
                    notes = t.message ?: "Unknown error"
                )
            )
        }
    }
}
