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
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/**
 * Eccezione di dominio scagliata quando il file di persistenza dei fascicoli è corrotto o illeggibile.
 */
class FascicoloCorruptedException(
    message: String,
    cause: Throwable? = null,
    val corruptBackupFile: File? = null
) : RuntimeException(message, cause)

/**
 * Helper per la persistenza atomica dei file su disco, compatibile sia su runtime Android (ART)
 * sia in test unitari JVM host (testDebugUnitTest) senza dipendenze da framework mock.
 */
internal object AtomicFileUtils {
    private const val TAG = "AtomicFileUtils"

    private fun safeLog(msg: String, tr: Throwable? = null) {
        runCatching {
            if (tr != null) android.util.Log.w(TAG, msg, tr) else android.util.Log.w(TAG, msg)
        }.onFailure {
            System.err.println("[$TAG] $msg ${tr?.message ?: ""}")
        }
    }

    /**
     * Ripristina lo stato coerente prima della lettura se un'operazione di scrittura
     * precedente è stata interrotta (presenza di file .bak).
     */
    fun syncBackupBeforeRead(target: File) {
        val parent = target.parentFile ?: return
        val bakFile = File(parent, "${target.name}.bak")
        if (bakFile.exists()) {
            if (target.exists()) {
                // Il file target esiste ed è completo (la rinomina tmp->target è avvenuta con successo
                // ma il processo è terminato prima di eliminare .bak). Eliminiamo il vecchio .bak.
                bakFile.delete()
            } else {
                // Il target non esiste ma esiste .bak: ripristiniamo il backup prima della lettura.
                val restored = bakFile.renameTo(target) || runCatching {
                    Files.move(bakFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    true
                }.getOrDefault(false)
                if (!restored) {
                    safeLog("Impossibile ripristinare il backup per ${target.absolutePath}")
                }
            }
        }
    }

    /**
     * Legge in modo sicuro e atomico il contenuto grezzo di un file, ripristinando il backup .bak se necessario.
     */
    fun readFully(target: File): ByteArray {
        syncBackupBeforeRead(target)
        if (!target.exists()) throw java.io.FileNotFoundException("File non trovato: ${target.absolutePath}")
        return target.readBytes()
    }

    fun readText(target: File, charset: java.nio.charset.Charset = Charsets.UTF_8): String {
        return String(readFully(target), charset)
    }

    /**
     * Scrive in modo atomico e transazionale [bytes] su [target]:
     * 1. Scrive su un file .tmp univoco e invoca sync() sul FileDescriptor.
     * 2. Se [target] esiste, lo sposta in .bak come rete di salvataggio.
     * 3. Sposta .tmp su [target].
     * 4. Rimuove .bak solo a trasferimento ultimato con successo.
     */
    fun writeBytes(target: File, bytes: ByteArray) {
        val parent = target.parentFile ?: File(".")
        if (!parent.exists()) parent.mkdirs()

        val tmpFile = File(parent, "${target.name}.${System.currentTimeMillis()}.${UUID.randomUUID()}.tmp")
        val bakFile = File(parent, "${target.name}.bak")

        try {
            FileOutputStream(tmpFile).use { fos ->
                fos.write(bytes)
                fos.flush()
                try {
                    fos.fd.sync()
                } catch (_: Throwable) {}
            }
        } catch (e: Throwable) {
            runCatching { tmpFile.delete() }
            throw IOException("Fallita scrittura su file temporaneo per ${target.absolutePath}", e)
        }

        // Se target esiste, spostalo su .bak
        if (target.exists()) {
            if (bakFile.exists()) bakFile.delete()
            val backedUp = target.renameTo(bakFile) || runCatching {
                Files.move(target.toPath(), bakFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                true
            }.getOrDefault(false)

            if (!backedUp) {
                runCatching { tmpFile.delete() }
                throw IOException("Impossibile creare backup di sicurezza prima di sovrascrivere ${target.absolutePath}")
            }
        }

        // Sposta .tmp su target
        val movedToTarget = tmpFile.renameTo(target) || runCatching {
            Files.move(tmpFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            true
        }.getOrDefault(false)

        if (!movedToTarget) {
            // Ripristina da .bak se possibile
            if (bakFile.exists() && !target.exists()) {
                bakFile.renameTo(target)
            }
            runCatching { tmpFile.delete() }
            throw IOException("Impossibile rinominare ${tmpFile.name} in ${target.name}")
        }

        // Operazione completata con successo: elimina .bak
        if (bakFile.exists()) {
            bakFile.delete()
        }
    }

    fun writeText(target: File, text: String, charset: java.nio.charset.Charset = Charsets.UTF_8) {
        writeBytes(target, text.toByteArray(charset))
    }

    fun delete(target: File): Boolean {
        val parent = target.parentFile
        if (parent != null) {
            val bakFile = File(parent, "${target.name}.bak")
            if (bakFile.exists()) bakFile.delete()
        }
        return target.delete()
    }
}

/**
 * Repository per la gestione e persistenza atomica dei Fascicoli/Pratiche dello Studio Tecnico.
 * Permette di raggruppare perizie, documenti, foto e appuntamenti sotto un'unica commessa.
 */
class FascicoloRepository(
    private val context: Context? = null,
    private val overrideFascicoliFile: File? = null
) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val mutex = Mutex()

    @Volatile
    private var cachedFascicoli: List<Fascicolo>? = null

    private val fascicoliFile: File
        get() = overrideFascicoliFile ?: File(context?.filesDir, "fascicoli_studio.json")

    private val filesDir: File
        get() = overrideFascicoliFile?.parentFile ?: context?.filesDir ?: File(".")

    private fun safeLogE(tag: String, msg: String, tr: Throwable? = null) {
        runCatching {
            if (tr != null) android.util.Log.e(tag, msg, tr) else android.util.Log.e(tag, msg)
        }.onFailure {
            System.err.println("[$tag] ERROR: $msg ${tr?.message ?: ""}")
        }
    }

    internal fun getCachedFascicoli(): List<Fascicolo>? = cachedFascicoli
    internal fun setCachedFascicoli(list: List<Fascicolo>?) { cachedFascicoli = list }

    suspend fun loadAllFascicoli(): List<Fascicolo> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val bakFile = File(fascicoliFile.parentFile, "${fascicoliFile.name}.bak")
            if (!fascicoliFile.exists() && !bakFile.exists()) {
                val initialSeed = createInitialSeed()
                saveInternal(initialSeed)
                cachedFascicoli = initialSeed
                return@withContext initialSeed
            }
            try {
                val list = loadInternal()
                cachedFascicoli = list
                list
            } catch (e: FascicoloCorruptedException) {
                safeLogE("FascicoloRepo", "File fascicoli corrotto: ${e.message}", e)
                cachedFascicoli ?: throw e
            }
        }
    }

    suspend fun getFascicoloById(id: String): Fascicolo? = withContext(Dispatchers.IO) {
        loadAllFascicoli().firstOrNull { it.id == id }
    }

    suspend fun saveFascicolo(item: Fascicolo): Fascicolo = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = try {
                loadInternal().toMutableList()
            } catch (e: FascicoloCorruptedException) {
                cachedFascicoli?.toMutableList()
                    ?: throw IllegalStateException("Impossibile salvare il fascicolo: archivio su disco corrotto e cache assente", e)
            }
            val existingIdx = current.indexOfFirst { it.id == item.id }
            val updated = item.copy(updatedAtMillis = System.currentTimeMillis())
            if (existingIdx >= 0) {
                current[existingIdx] = updated
            } else {
                current.add(0, updated)
            }
            saveInternal(current)
            cachedFascicoli = current
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
            val current = try {
                loadInternal().toMutableList()
            } catch (e: FascicoloCorruptedException) {
                cachedFascicoli?.toMutableList() ?: throw e
            }
            val idx = current.indexOfFirst { it.id == id }
            if (idx >= 0) {
                val updated = current[idx].copy(
                    stato = nuovoStato,
                    updatedAtMillis = System.currentTimeMillis()
                )
                current[idx] = updated
                saveInternal(current)
                cachedFascicoli = current
                updated
            } else null
        }
    }

    suspend fun deleteFascicolo(id: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = try {
                loadInternal().toMutableList()
            } catch (e: FascicoloCorruptedException) {
                cachedFascicoli?.toMutableList() ?: throw e
            }
            val removed = current.removeAll { it.id == id }
            if (removed) {
                saveInternal(current)
                cachedFascicoli = current
            }
            removed
        }
    }

    private fun loadInternal(): List<Fascicolo> {
        val bakFile = File(fascicoliFile.parentFile, "${fascicoliFile.name}.bak")
        if (!fascicoliFile.exists() && !bakFile.exists()) return emptyList()

        val raw = try {
            AtomicFileUtils.readText(fascicoliFile)
        } catch (ioe: Throwable) {
            val corruptBackup = quarantineCorruptFile(fascicoliFile)
            throw FascicoloCorruptedException("Lettura fallita per fascicoli_studio.json", ioe, corruptBackup)
        }

        if (raw.isBlank()) return emptyList()

        return try {
            val list = json.decodeFromString<List<Fascicolo>>(raw)
            list.sortedByDescending { it.updatedAtMillis }
        } catch (se: Throwable) {
            val corruptBackup = quarantineCorruptFile(fascicoliFile)
            throw FascicoloCorruptedException("Decodifica JSON fallita per fascicoli_studio.json", se, corruptBackup)
        }
    }

    private fun quarantineCorruptFile(target: File): File? {
        return runCatching {
            if (target.exists()) {
                val corruptBackup = File(filesDir, "fascicoli_studio.json.corrupt.${System.currentTimeMillis()}")
                target.copyTo(corruptBackup, overwrite = true)
                safeLogE("FascicoloRepo", "File corrotto clonato per sicurezza forense in: ${corruptBackup.absolutePath}")
                corruptBackup
            } else null
        }.getOrNull()
    }

    private fun saveInternal(items: List<Fascicolo>) {
        try {
            val serialized = json.encodeToString(items)
            AtomicFileUtils.writeText(fascicoliFile, serialized)
        } catch (e: Throwable) {
            safeLogE("FascicoloRepo", "Errore salvataggio atomico fascicoli", e)
            throw IOException("Impossibile salvare atomicamente fascicoli_studio.json", e)
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
