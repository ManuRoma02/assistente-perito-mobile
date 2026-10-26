package com.example.assistenteperitomobile.domain.agents

import com.example.assistenteperitomobile.domain.AiEngineConfig
import com.example.assistenteperitomobile.domain.DocumentClassificationResult
import com.example.assistenteperitomobile.domain.PeritoAiEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import java.time.LocalDate

/**
 * Agent per la classificazione intelligente dei documenti nelle cartelle peritali.
 * Integra la pipeline OCR -> Heuristics -> RizzoFlow -> Local LLM con tracciabilità.
 */
class DocumentClassifierAgent(
    private val aiEngine: PeritoAiEngine
) {

    suspend fun classify(
        docText: String,
        originalFileName: String = "documento.pdf",
        referenceDate: LocalDate = LocalDate.now(),
        config: AiEngineConfig = AiEngineConfig(),
        traceId: String
    ): AgentResponse<DocumentClassificationResult> {
        val startNs = System.nanoTime()

        return try {
            kotlin.coroutines.coroutineContext.ensureActive()
            val result = aiEngine.classifyDocument(
                docText = docText,
                originalFileName = originalFileName,
                referenceDate = referenceDate,
                config = config
            )
            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000

            val isManual = result.categoria == "Da_Classificare_Manualmente"
            if (isManual) {
                AgentResponse.Degraded(
                    data = result,
                    reason = "Unrecognized document format or low quality text",
                    metrics = AgentExecutionMetrics(
                        traceId = traceId,
                        agentName = "DocumentClassifierAgent",
                        durationMs = elapsedMs,
                        confidence = 0.50f,
                        status = AgentStatus.DEGRADED,
                        notes = "Assigned to Da_Classificare_Manualmente"
                    )
                )
            } else {
                AgentResponse.Success(
                    data = result,
                    metrics = AgentExecutionMetrics(
                        traceId = traceId,
                        agentName = "DocumentClassifierAgent",
                        durationMs = elapsedMs,
                        confidence = 0.95f,
                        status = AgentStatus.SUCCESS,
                        notes = "Classified as ${result.categoria}"
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
                    agentName = "DocumentClassifierAgent",
                    durationMs = elapsedMs,
                    confidence = 0f,
                    status = AgentStatus.FAILED,
                    notes = t.message ?: "Classification failed"
                )
            )
        }
    }
}
