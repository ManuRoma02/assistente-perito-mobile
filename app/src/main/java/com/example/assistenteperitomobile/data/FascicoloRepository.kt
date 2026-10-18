package com.example.assistenteperitomobile.data

import android.content.Context
import com.example.assistenteperitomobile.domain.Fascicolo
import com.example.assistenteperitomobile.domain.FascicoloStato
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Repository per la gestione e persistenza atomica dei Fascicoli/Pratiche dello Studio Tecnico.
 * Permette di raggruppare perizie, documenti, foto e appuntamenti sotto un'unica commessa.
 */
class FascicoloRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val mutex = Mutex()

    private val fascicoliFile: File
        get() = File(context.filesDir, "fascicoli_studio.json")

    suspend fun loadAllFascicoli(): List<Fascicolo> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!fascicoliFile.exists()) {
                val initialSeed = createInitialSeed()
                saveInternal(initialSeed)
                return@withContext initialSeed
            }
            try {
                val raw = fascicoliFile.readText()
                if (raw.isBlank()) return@withContext emptyList()
                val list = json.decodeFromString<List<Fascicolo>>(raw)
                list.sortedByDescending { it.updatedAtMillis }
            } catch (_: Throwable) {
                emptyList()
            }
        }
    }

    suspend fun getFascicoloById(id: String): Fascicolo? = withContext(Dispatchers.IO) {
        loadAllFascicoli().firstOrNull { it.id == id }
    }

    suspend fun saveFascicolo(item: Fascicolo): Fascicolo = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = loadInternal().toMutableList()
            val existingIdx = current.indexOfFirst { it.id == item.id }
            val updated = item.copy(updatedAtMillis = System.currentTimeMillis())
            if (existingIdx >= 0) {
                current[existingIdx] = updated
            } else {
                current.add(0, updated)
            }
            saveInternal(current)
            updated
        }
    }

    suspend fun createNewFascicolo(
        codice: String,
        titolo: String,
        cliente: String,
        controparte: String = "",
        indirizzo: String = "",
        note: String = ""
    ): Fascicolo {
        val f = Fascicolo(
            id = UUID.randomUUID().toString(),
            codice = codice.ifBlank { "PRAT-${System.currentTimeMillis().toString().takeLast(4)}" },
            titolo = titolo.ifBlank { "Nuova Perizia Tecnica" },
            cliente = cliente.ifBlank { "Committente" },
            controparte = controparte,
            indirizzo = indirizzo,
            stato = FascicoloStato.IN_CORSO,
            note = note
        )
        return saveFascicolo(f)
    }

    suspend fun updateStato(id: String, nuovoStato: FascicoloStato): Fascicolo? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = loadInternal().toMutableList()
            val idx = current.indexOfFirst { it.id == id }
            if (idx >= 0) {
                val updated = current[idx].copy(
                    stato = nuovoStato,
                    updatedAtMillis = System.currentTimeMillis()
                )
                current[idx] = updated
                saveInternal(current)
                updated
            } else null
        }
    }

    suspend fun deleteFascicolo(id: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = loadInternal().toMutableList()
            val removed = current.removeAll { it.id == id }
            if (removed) {
                saveInternal(current)
            }
            removed
        }
    }

    private fun loadInternal(): List<Fascicolo> {
        if (!fascicoliFile.exists()) return emptyList()
        return try {
            json.decodeFromString<List<Fascicolo>>(fascicoliFile.readText())
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun saveInternal(items: List<Fascicolo>) {
        try {
            val serialized = json.encodeToString(items)
            val tmp = File(context.filesDir, "fascicoli_studio.json.tmp")
            tmp.writeText(serialized, Charsets.UTF_8)
            if (tmp.exists()) {
                if (fascicoliFile.exists()) fascicoliFile.delete()
                tmp.renameTo(fascicoliFile)
            }
        } catch (_: Throwable) {
        }
    }

    private fun createInitialSeed(): List<Fascicolo> {
        val now = System.currentTimeMillis()
        return listOf(
            Fascicolo(
                id = UUID.randomUUID().toString(),
                codice = "CTU-2026-042",
                titolo = "Infiltrazioni Terrazzo Condominio Aurora",
                cliente = "Tribunale Ordinario - Giudice Dott. Moretti",
                controparte = "Impresa EdilCostruzioni Srl",
                indirizzo = "Via Dante 28, Milano",
                stato = FascicoloStato.IN_CORSO,
                note = "Sopralluogo preliminare da fissare con CTP delle parti. Richiesta verifica guaine bituminose.",
                createdAtMillis = now - 86_400_000L * 3,
                updatedAtMillis = now - 3600_000L
            ),
            Fascicolo(
                id = UUID.randomUUID().toString(),
                codice = "SIN-2026-881",
                titolo = "Sinistro RCA Veicolo Targato FX492ZZ",
                cliente = "Assicurazioni Generali / Dott.ssa Banchi",
                controparte = "Sig. Mario Rossi",
                indirizzo = "Carrozzeria Milano Nord, Viale Monza 120",
                stato = FascicoloStato.APERTO,
                note = "Verifica compatibilità danni su paraurti anteriore e montante destro.",
                createdAtMillis = now - 86_400_000L,
                updatedAtMillis = now
            )
        )
    }
}
