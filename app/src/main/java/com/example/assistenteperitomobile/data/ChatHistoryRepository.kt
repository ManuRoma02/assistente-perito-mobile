package com.example.assistenteperitomobile.data

import android.content.Context
import com.example.assistenteperitomobile.domain.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Repository robusto e thread-safe per la persistenza atomica della cronologia Chat.
 * Evita la perdita di messaggi tra un riavvio e l'altro dell'app dello studio.
 */
class ChatHistoryRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val mutex = Mutex()

    private val historyFile: File
        get() = File(context.filesDir, "chat_history.json")

    suspend fun loadMessages(): List<ChatMessage> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!historyFile.exists()) return@withContext emptyList()
            try {
                val raw = historyFile.readText()
                if (raw.isBlank()) return@withContext emptyList()
                json.decodeFromString<List<ChatMessage>>(raw)
            } catch (_: Throwable) {
                emptyList()
            }
        }
    }

    suspend fun saveMessages(messages: List<ChatMessage>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val serialized = json.encodeToString(messages)
                val tempFile = File(context.filesDir, "chat_history.json.tmp")
                tempFile.writeText(serialized, Charsets.UTF_8)
                if (tempFile.exists()) {
                    if (historyFile.exists()) historyFile.delete()
                    tempFile.renameTo(historyFile)
                }
            } catch (_: Throwable) {
                // Silently keep previous history if write fails
            }
        }
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                if (historyFile.exists()) {
                    historyFile.delete()
                }
            } catch (_: Throwable) {
            }
        }
    }
}
