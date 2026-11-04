package com.example.assistenteperitomobile.data

import com.example.assistenteperitomobile.domain.Fascicolo
import com.example.assistenteperitomobile.domain.FascicoloStato
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

class FascicoloRepositoryResilienceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    @Test
    fun testInitialSeedCreationAndPersistence() = runBlocking {
        val testFile = File(tempFolder.root, "fascicoli_studio.json")
        assertFalse(testFile.exists())

        val repo = FascicoloRepository(overrideFascicoliFile = testFile)
        val initialList = repo.loadAllFascicoli()

        assertEquals(2, initialList.size)
        assertTrue("Seed file must be written to disk", testFile.exists())
        assertTrue("Cache must be initialized", repo.getCachedFascicoli() != null)
    }

    @Test
    fun testAtomicPersistenceAndMutation() = runBlocking {
        val testFile = File(tempFolder.root, "fascicoli_studio.json")
        val repo = FascicoloRepository(overrideFascicoliFile = testFile)
        repo.loadAllFascicoli()

        val newFascicolo = Fascicolo(
            id = UUID.randomUUID().toString(),
            codice = "TEST-2026-999",
            titolo = "Perizia Strutturale Edificio Test",
            cliente = "Studio Legale Rossi",
            stato = FascicoloStato.IN_CORSO
        )
        repo.saveFascicolo(newFascicolo)

        val loaded = repo.loadAllFascicoli()
        assertEquals(3, loaded.size)
        assertEquals("TEST-2026-999", loaded.first().codice)

        // Verifica aggiornamento stato
        val updated = repo.updateStato(newFascicolo.id, FascicoloStato.CHIUSO)
        assertNotNull(updated)
        assertEquals(FascicoloStato.CHIUSO, updated?.stato)

        // Nessun file .bak o .tmp residuo
        val bakFile = File(testFile.parentFile, "${testFile.name}.bak")
        assertFalse("Backup .bak must be cleaned up on clean write", bakFile.exists())
    }

    @Test
    fun testCorruptedDiskThrowsFascicoloCorruptedExceptionAndQuarantines() = runBlocking {
        val testFile = File(tempFolder.root, "fascicoli_studio.json")
        val corruptContent = "{ this is invalid and corrupted json payload [[["
        testFile.writeText(corruptContent)

        val repo = FascicoloRepository(overrideFascicoliFile = testFile)
        // Cache is null

        try {
            repo.loadAllFascicoli()
            fail("Should throw FascicoloCorruptedException on corrupted JSON without cache")
        } catch (e: FascicoloCorruptedException) {
            assertTrue("Exception message should identify JSON decode failure", e.message?.contains("Decodifica JSON fallita") == true)
            assertNotNull("Corrupt backup file must be reported", e.corruptBackupFile)
            assertTrue("Quarantine file must exist on disk", e.corruptBackupFile?.exists() == true)
            assertEquals(corruptContent, e.corruptBackupFile?.readText())
        }
    }

    @Test
    fun testCorruptedDiskPreservesInMemoryCacheAndAllowsUiContinuity() = runBlocking {
        val testFile = File(tempFolder.root, "fascicoli_studio.json")
        val repo = FascicoloRepository(overrideFascicoliFile = testFile)
        val validList = repo.loadAllFascicoli() // cache is now populated with seed
        assertEquals(2, validList.size)

        // Corrompiamo il file su disco
        testFile.writeText("{ invalid json corrupt byte ")

        // loadAllFascicoli deve restituire la cache intatta senza lanciare eccezioni alla UI
        val cachedFallback = repo.loadAllFascicoli()
        assertEquals(2, cachedFallback.size)
        assertEquals(validList.map { it.id }, cachedFallback.map { it.id })

        // Il salvataggio di un nuovo fascicolo deve usare la cache in memoria e riparare il file su disco
        val extraFascicolo = Fascicolo(
            id = UUID.randomUUID().toString(),
            codice = "REPAIRED-001",
            titolo = "Perizia di Recupero Post-Crash",
            cliente = "Cliente Assicurato",
            stato = FascicoloStato.APERTO
        )
        repo.saveFascicolo(extraFascicolo)

        val repairedList = repo.loadAllFascicoli()
        assertEquals(3, repairedList.size)
        assertTrue("Tutti i fascicoli precedenti + quello nuovo devono essere preservati", repairedList.any { it.codice == "REPAIRED-001" })
    }

    @Test
    fun testCorruptedDiskWithoutCacheRejectsSaveToPreventDataLoss() = runBlocking {
        val testFile = File(tempFolder.root, "fascicoli_studio.json")
        val corruptContent = "CORRUPT_BYTES"
        testFile.writeText(corruptContent)

        val repo = FascicoloRepository(overrideFascicoliFile = testFile)
        // Ensure cache is empty
        repo.setCachedFascicoli(null)

        val newItem = Fascicolo(
            id = "lone_item",
            codice = "LONE-001",
            titolo = "Tentativo Distruttivo",
            cliente = "Nessuno",
            stato = FascicoloStato.APERTO
        )

        try {
            repo.saveFascicolo(newItem)
            fail("saveFascicolo must refuse to proceed when disk is corrupt and memory cache is absent")
        } catch (e: Exception) {
            assertTrue(
                "Must throw exception protecting existing data",
                e is IllegalStateException || e is FascicoloCorruptedException
            )
        }
    }

    @Test
    fun testAtomicBackupRecoveryOnInterruptedWrite() = runBlocking {
        val testFile = File(tempFolder.root, "fascicoli_studio.json")
        val bakFile = File(tempFolder.root, "fascicoli_studio.json.bak")

        val seedData = listOf(
            Fascicolo(
                id = "recovered_1",
                codice = "REC-2026",
                titolo = "Perizia da Backup",
                cliente = "Tribunale",
                stato = FascicoloStato.IN_CORSO
            )
        )
        bakFile.writeText(json.encodeToString(seedData))
        if (testFile.exists()) testFile.delete()

        // Simula riavvio dopo crash: target mancante, .bak presente
        val repo = FascicoloRepository(overrideFascicoliFile = testFile)
        val loaded = repo.loadAllFascicoli()

        assertEquals(1, loaded.size)
        assertEquals("REC-2026", loaded.first().codice)
        assertTrue("Target file must be restored from .bak", testFile.exists())
    }
}
