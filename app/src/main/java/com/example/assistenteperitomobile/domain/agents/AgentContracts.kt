package com.example.assistenteperitomobile.domain.agents

import java.util.UUID

/**
 * Standard Multi-Agent Contracts & Observability Layer.
 * Progettato per garantire tracciabilità (trace_id), fallbacks deterministici
 * e metriche di confidenza secondo i principi di distributed agent governance.
 */

enum class AgentStatus {
    SUCCESS,
    DEGRADED,
    FALLBACK,
    FAILED
}

enum class UserIntentCategory {
    SLASH_COMMAND,
    DAILY_BRIEFING,
    APPOINTMENT_REQUEST,
    ARCHIVE_SEARCH,
    CORRECTION_OR_DELETE,
    TECHNICAL_REASONING,
    UNCLEAR
}

data class AgentExecutionMetrics(
    val traceId: String = UUID.randomUUID().toString(),
    val spanId: String = UUID.randomUUID().toString(),
    val agentName: String,
    val durationMs: Long,
    val confidence: Float = 1.0f,
    val status: AgentStatus = AgentStatus.SUCCESS,
    val notes: String = ""
)

sealed class AgentResponse<out T> {
    abstract val metrics: AgentExecutionMetrics

    data class Success<T>(
        val data: T,
        override val metrics: AgentExecutionMetrics
    ) : AgentResponse<T>()

    data class Degraded<T>(
        val data: T,
        val reason: String,
        override val metrics: AgentExecutionMetrics
    ) : AgentResponse<T>()

    data class Failure(
        val error: Throwable,
        override val metrics: AgentExecutionMetrics
    ) : AgentResponse<Nothing>()

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Degraded -> data
        is Failure -> null
    }
}

data class ClassifiedIntent(
    val category: UserIntentCategory,
    val rawText: String,
    val cleanedQuery: String,
    val command: String? = null,
    val confidence: Float = 1.0f
)
