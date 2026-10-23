package com.example.assistenteperitomobile.domain.agents

import java.util.Locale

/**
 * Agent specializzato nella classificazione dell'intento dell'utente.
 * Esegue prima una valutazione deterministica (<2ms) ad alta confidenza
 * per evitare chiamate inutili all'LLM e prevenire routing errati.
 */
class IntentRouterAgent {

    fun classify(rawText: String, traceId: String): AgentResponse<ClassifiedIntent> {
        val startNs = System.nanoTime()
        val text = rawText.trim()
        val lower = text.lowercase(Locale.ITALIAN)

        val intent = when {
            lower.startsWith("/") -> {
                val cmd = lower.substringBefore(" ")
                ClassifiedIntent(
                    category = UserIntentCategory.SLASH_COMMAND,
                    rawText = text,
                    cleanedQuery = text.substringAfter(" ", "").trim(),
                    command = cmd,
                    confidence = 1.0f
                )
            }
            lower == "oggi" || lower.contains("briefing") ||
            lower.contains("impegni di oggi") || lower.contains("programma di oggi") -> {
                ClassifiedIntent(
                    category = UserIntentCategory.DAILY_BRIEFING,
                    rawText = text,
                    cleanedQuery = text,
                    confidence = 0.95f
                )
            }
            lower.startsWith("elimina ") || lower.startsWith("cancella ") ||
            lower.startsWith("rimuovi ") || lower.startsWith("annulla ") -> {
                ClassifiedIntent(
                    category = UserIntentCategory.CORRECTION_OR_DELETE,
                    rawText = text,
                    cleanedQuery = text.replaceFirst(Regex("^(elimina|cancella|rimuovi|annulla)\\s+", RegexOption.IGNORE_CASE), "").trim(),
                    confidence = 0.95f
                )
            }
            lower.startsWith("cerca ") || lower.startsWith("trova ") ||
            lower.startsWith("mandami ") || lower.startsWith("mostrami ") -> {
                ClassifiedIntent(
                    category = UserIntentCategory.ARCHIVE_SEARCH,
                    rawText = text,
                    cleanedQuery = text.replaceFirst(Regex("^(cerca|trova|mandami|mostrami)\\s+", RegexOption.IGNORE_CASE), "").trim(),
                    confidence = 0.90f
                )
            }
            lower.contains("sopralluogo") || lower.contains("appuntamento") ||
            lower.contains("udienza") || lower.contains("riunione") ||
            lower.contains("domani") || lower.contains("lunedì") || lower.contains("martedì") ||
            lower.contains("mercoledì") || lower.contains("giovedì") || lower.contains("venerdì") ||
            lower.contains("sabato") || lower.contains("domenica") || lower.contains("alle ore") ||
            lower.contains("alle ") || Regex("\\b\\d{1,2}[:.]\\d{2}\\b").containsMatchIn(lower) -> {
                ClassifiedIntent(
                    category = UserIntentCategory.APPOINTMENT_REQUEST,
                    rawText = text,
                    cleanedQuery = text,
                    confidence = 0.85f
                )
            }
            else -> {
                ClassifiedIntent(
                    category = UserIntentCategory.TECHNICAL_REASONING,
                    rawText = text,
                    cleanedQuery = text,
                    confidence = 0.80f
                )
            }
        }

        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000
        return AgentResponse.Success(
            data = intent,
            metrics = AgentExecutionMetrics(
                traceId = traceId,
                agentName = "IntentRouterAgent",
                durationMs = elapsedMs,
                confidence = intent.confidence,
                status = AgentStatus.SUCCESS
            )
        )
    }
}
