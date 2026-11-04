package com.example.assistenteperitomobile.domain

import java.util.Locale

/**
 * Guardiano di sicurezza per prevenire cancellazioni accidentali e distruttive
 * di documenti peritali dall'archivio e dai fascicoli.
 *
 * Elimina la cancellazione massiva involontaria e il fallback su allDocs.first() (BUG-02).
 */
object SafeDeletionGuard {

    private val deleteVerbs = listOf("elimina", "cancella", "rimuovi", "butta", "togli", "svuota")
    private val genericPointers = setOf(
        "elimina", "cancella", "rimuovi", "butta", "togli", "svuota",
        "questo", "questa", "quello", "quella", "ultimo", "ultima", "l",
        "il", "lo", "la", "i", "gli", "le", "un", "una", "uno",
        "del", "dello", "della", "dei", "degli", "delle", "di", "da", "dall", "dalla", "dallo",
        "in", "su", "con", "per", "tra", "fra", "a", "ad", "via",
        "file", "documento", "doc", "allegato", "foto", "scansione", "pdf", "p7m", "archivio"
    )

    fun isDeletionIntent(text: String): Boolean {
        val lower = text.trim().lowercase(Locale.ITALIAN)
        return deleteVerbs.any { lower == it || lower.startsWith("$it ") || lower.contains(" $it ") }
    }

    fun isAppointmentTarget(text: String): Boolean {
        val lower = text.trim().lowercase(Locale.ITALIAN)
        return lower.contains("appuntamento") ||
            lower.contains("sopralluogo") ||
            lower.contains("impegno") ||
            lower.contains("evento") ||
            lower.contains("agenda")
    }

    /**
     * Valuta l'intenzione di eliminazione in modo sicuro.
     *
     * @param query Il testo inserito dall'utente.
     * @param docs La lista dei documenti attualmente presenti in archivio.
     * @param hasExplicitUserConfirmation Flag esplicito di conferma (da UI o token confermativo).
     * @return Result con la lista dei documenti autorizzati alla cancellazione, oppure failure se bloccato per sicurezza.
     */
    fun evaluateDeletionIntent(
        query: String,
        docs: List<ArchivedDocument>,
        hasExplicitUserConfirmation: Boolean = false
    ): Result<List<ArchivedDocument>> {
        val lower = query.trim().lowercase(Locale.ITALIAN)
        if (!isDeletionIntent(lower)) {
            return Result.success(emptyList())
        }

        if (isAppointmentTarget(lower)) {
            return Result.success(emptyList())
        }

        val isMassDelete = lower.contains("tutti") || lower.contains("tutto") || lower.contains("svuota")
        val hasExplicitConfirmation = hasExplicitUserConfirmation ||
            lower.startsWith("conferma ") ||
            lower.contains("conferma svuota") ||
            lower.contains("conferma elimina tutto") ||
            lower.contains("conferma elimina tutti")

        if (isMassDelete) {
            if (!hasExplicitConfirmation) {
                return Result.failure(IllegalStateException("Richiesta conferma esplicita per cancellazione massiva"))
            }
            return Result.success(docs)
        }

        // Estrazione token mirati preservando trattini e underscore per identificatori (es. DOC-CTU-01)
        val targetTokens = lower.replace(Regex("""[^a-zà-ú0-9\s\-_]"""), " ")
            .split(Regex("""\s+"""))
            .filter { it.length >= 2 && it !in genericPointers }

        if (targetTokens.isEmpty()) {
            // Nessun file specifico identificabile: rifiuta fallback impliciti a allDocs.first()
            return Result.success(emptyList())
        }

        val queryJoined = targetTokens.joinToString(" ")

        // 1. Corrispondenza per ID esatto
        val matchedById = docs.filter { doc ->
            doc.id.equals(queryJoined, ignoreCase = true) ||
            targetTokens.any { tok -> doc.id.equals(tok, ignoreCase = true) }
        }
        if (matchedById.isNotEmpty()) {
            return Result.success(matchedById)
        }

        // 2. Corrispondenza per nome file, cliente o oggetto (DIFESA BUG-02: MAI sui 600 caratteri di OCR snippet!)
        // Tutti i token specifici indicati dall'utente devono trovare riscontro nei metadati del documento
        val matchedByName = docs.filter { doc ->
            val searchableFields = "${doc.fileName} ${doc.originalName} ${doc.cliente} ${doc.oggetto}".lowercase(Locale.ITALIAN)
            targetTokens.all { tok -> searchableFields.contains(tok) }
        }

        if (matchedByName.isNotEmpty()) {
            return Result.success(matchedByName)
        }

        // 3. Fallback di sicurezza: se nessun match, restituisce emptyList anche se docs.size == 1 (DIFESA BUG-02)
        return Result.success(emptyList())
    }
}
