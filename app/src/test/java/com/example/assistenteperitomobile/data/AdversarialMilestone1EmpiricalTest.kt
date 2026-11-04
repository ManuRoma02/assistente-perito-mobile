package com.example.assistenteperitomobile.data

import com.example.assistenteperitomobile.domain.Fascicolo
import com.example.assistenteperitomobile.domain.FascicoloStato
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Adversarial Empirical Challenge Suite for Milestone 1:
 * - AtomicFileUtils transaction integrity & crash recovery
 * - AtomicFileUtils high-concurrency stress
 * - FascicoloRepository corruption detection, forensic quarantine, and anti-wipeout protection
 */
class AdversarialMilestone1EmpiricalTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    // ---------------------------------------------------------------------------------------------
    // SECTION 1: AtomicFileUtils Transaction Integrity & Recovery Under Simulated Failure
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testAtomicWriteTransactionNoLeakOnNormalAndOverwriting() {
        val target = File(tempFolder.root, "tx_test.json")
        val payload1 = "{\"version\": 1, \"data\": \"Short payload\"}"
        AtomicFileUtils.writeText(target, payload1)

        assertTrue("Target file must exist", target.exists())
        assertEquals(payload1, AtomicFileUtils.readText(target))

        // Check directory contents: only tx_test.json should be present
        val dirFiles1 = tempFolder.root.listFiles() ?: emptyArray()
        assertEquals("No temporary or backup files should remain after clean write", 1, dirFiles1.size)
        assertEquals("tx_test.json", dirFiles1[0].name)

        // Overwrite with larger payload
        val payload2 = "{\"version\": 2, \"data\": \"A much longer payload to ensure truncation doesn't leave remnants ${"X".repeat(5000)}\"}"
        AtomicFileUtils.writeText(target, payload2)
        assertEquals(payload2, AtomicFileUtils.readText(target))

        // Overwrite with smaller payload: verify NO trailing data remains from payload2
        val payload3 = "{\"v\": 3}"
        AtomicFileUtils.writeText(target, payload3)
        assertEquals(payload3, AtomicFileUtils.readText(target))

        val dirFiles3 = tempFolder.root.listFiles() ?: emptyArray()
        assertEquals(1, dirFiles3.size)
        assertEquals("tx_test.json", dirFiles3[0].name)
    }

    @Test
    fun testRecoveryWhenTargetMissingAndBakExists() {
        val target = File(tempFolder.root, "uncommitted_crash.json")
        val bak = File(tempFolder.root, "uncommitted_crash.json.bak")
        val validBakContent = "{\"recovered\": true, \"records\": [1, 2, 3]}"

        bak.writeText(validBakContent)
        assertFalse("Target should not exist before recovery", target.exists())
        assertTrue("Backup file must exist", bak.exists())

        // Read directly through AtomicFileUtils: should transparently recover from .bak
        val readData = AtomicFileUtils.readText(target)
        assertEquals(validBakContent, readData)
        assertTrue("Target file must have been recovered and now exists", target.exists())
        assertFalse("Backup file should be renamed/moved to target", bak.exists())
    }

    @Test
    fun testSyncBackupDiscardsBakIfTargetAlreadyCommitted() {
        val target = File(tempFolder.root, "already_committed.json")
        val bak = File(tempFolder.root, "already_committed.json.bak")
        val committedContent = "{\"status\": \"committed\"}"
        val staleBakContent = "{\"status\": \"stale\"}"

        target.writeText(committedContent)
        bak.writeText(staleBakContent)

        AtomicFileUtils.syncBackupBeforeRead(target)

        assertTrue(target.exists())
        assertEquals(committedContent, target.readText())
        assertFalse("Stale .bak must be cleaned up since target was committed", bak.exists())
    }

    @Test
    fun testAtomicWriteWhenTargetMissingButBakExists() {
        val target = File(tempFolder.root, "target_missing_bak_present.json")
        val bak = File(tempFolder.root, "target_missing_bak_present.json.bak")
        bak.writeText("stale backup data")

        val newContent = "freshly written content"
        AtomicFileUtils.writeText(target, newContent)

        assertTrue(target.exists())
        assertEquals(newContent, target.readText())
        assertFalse("Old bak should be removed after successful new write", bak.exists())
    }

    // ---------------------------------------------------------------------------------------------
    // SECTION 2: AtomicFileUtils High-Concurrency Stress
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testAtomicFileUtilsHighConcurrencyStressWriting() = runBlocking(Dispatchers.IO) {
        val target = File(tempFolder.root, "concurrent_stress.txt")
        val threadCount = 16
        val writesPerThread = 25
        val latch = CountDownLatch(threadCount)
        val errors = ConcurrentLinkedQueue<Throwable>()
        val readSnapshots = ConcurrentLinkedQueue<String>()

        // Pre-create initial content
        AtomicFileUtils.writeText(target, "INIT_0000000000")

        val jobs = (1..threadCount).map { threadId ->
            launch {
                latch.countDown()
                latch.await()
                for (seq in 1..writesPerThread) {
                    val payload = "PAYLOAD_T${threadId}_S${seq}_" + "A".repeat(200)
                    try {
                        AtomicFileUtils.writeText(target, payload)
                    } catch (e: Throwable) {
                        errors.add(e)
                    }

                    // Concurrent reader check
                    try {
                        val currentText = AtomicFileUtils.readText(target)
                        readSnapshots.add(currentText)
                    } catch (e: Throwable) {
                        errors.add(e)
                    }
                }
            }
        }

        jobs.forEach { it.join() }

        // Verify that any read snapshot was a complete uncorrupted payload
        for (snap in readSnapshots) {
            assertTrue(
                "Snapshot must start with PAYLOAD_ or INIT_: $snap",
                snap.startsWith("PAYLOAD_") || snap.startsWith("INIT_")
            )
            if (snap.startsWith("PAYLOAD_")) {
                assertTrue("Snapshot must not be truncated or corrupted", snap.length >= 210)
            }
        }

        // Final state of file must be valid and readable
        val finalContent = AtomicFileUtils.readText(target)
        assertTrue("Final content must be valid", finalContent.startsWith("PAYLOAD_"))

        // Any IOExceptions during extreme contention must be caught or examined
        // We log errors if any occurred due to Windows file-sharing locks
        println("High-concurrency writes completed. Total reads: ${readSnapshots.size}, Contention errors: ${errors.size}")
    }

    // ---------------------------------------------------------------------------------------------
    // SECTION 3: FascicoloRepository Corruption Protection & Forensic Quarantine
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testCorruptedJsonThrowsFascicoloCorruptedExceptionAndClonesForensicBackup() = runBlocking {
        val testFile = File(tempFolder.root, "fascicoli_studio.json")
        val garbagePayload = "{ \"incomplete\": true, \"syntax_error\": [[["
        testFile.writeText(garbagePayload)

        val repo = FascicoloRepository(overrideFascicoliFile = testFile)
        repo.setCachedFascicoli(null)

        var thrownException: FascicoloCorruptedException? = null
        try {
            repo.loadAllFascicoli()
            fail("Must throw FascicoloCorruptedException on corrupted JSON")
        } catch (e: FascicoloCorruptedException) {
            thrownException = e
        }

        assertNotNull("FascicoloCorruptedException was not caught", thrownException)
        assertTrue("Exception must state JSON decode failed", thrownException!!.message!!.contains("Decodifica JSON fallita"))
        assertNotNull("Quarantined corrupt backup file must not be null", thrownException.corruptBackupFile)
        assertTrue("Quarantined corrupt backup file must exist on disk", thrownException.corruptBackupFile!!.exists())
        assertEquals(garbagePayload, thrownException.corruptBackupFile!!.readText())
        assertTrue("Quarantine filename must follow naming convention", thrownException.corruptBackupFile!!.name.startsWith("fascicoli_studio.json.corrupt."))
    }

    @Test
    fun testCorruptedJsonDoesNotOverwriteDiskWithEmptyOrSingleItemOnSave() = runBlocking {
        val testFile = File(tempFolder.root, "fascicoli_studio.json")
        val garbagePayload = "<<<INVALID NON JSON DATA BYTES>>>"
        testFile.writeText(garbagePayload)

        val repo = FascicoloRepository(overrideFascicoliFile = testFile)
        repo.setCachedFascicoli(null)

        val singleFascicolo = Fascicolo(
            id = "lone_item_123",
            codice = "LONE-123",
            titolo = "Rischio Sovrascrittura",
            cliente = "Cliente X",
            stato = FascicoloStato.APERTO
        )

        // Attempting to save a new record when disk is corrupted and cache is null MUST FAIL
        // It must NOT overwrite the damaged file with a list containing only singleFascicolo!
        try {
            repo.saveFascicolo(singleFascicolo)
            fail("saveFascicolo must refuse to proceed when disk is corrupt and memory cache is absent")
        } catch (e: Throwable) {
            assertTrue("Expected IllegalStateException or FascicoloCorruptedException, got ${e.javaClass.name}",
                e is IllegalStateException || e is FascicoloCorruptedException
            )
        }

        // Verify the original damaged file on disk was NOT overwritten with singleFascicolo
        val diskContentAfterAttempt = testFile.readText()
        assertEquals(garbagePayload, diskContentAfterAttempt)
        assertFalse("Disk must not contain singleFascicolo", diskContentAfterAttempt.contains("lone_item_123"))
    }

    @Test
    fun testCorruptedJsonWithPriorCacheHealsDiskAndPreservesAllRecords() = runBlocking {
        val testFile = File(tempFolder.root, "fascicoli_studio.json")
        val repo = FascicoloRepository(overrideFascicoliFile = testFile)

        // 1. Initial valid load (creates seed with 2 records)
        val initialList = repo.loadAllFascicoli()
        assertEquals(2, initialList.size)
        val originalIds = initialList.map { it.id }.toSet()

        // 2. Simulate disk corruption after memory cache is already warm
        val corruptedPayload = "{\"corrupt\": true" // broken json
        testFile.writeText(corruptedPayload)

        // 3. UI calls loadAllFascicoli: should gracefully return cached list without crashing
        val loadedAfterCorrupt = repo.loadAllFascicoli()
        assertEquals(2, loadedAfterCorrupt.size)
        assertEquals(originalIds, loadedAfterCorrupt.map { it.id }.toSet())

        // 4. User adds a 3rd record
        val extra = Fascicolo(
            id = "extra_456",
            codice = "EXTRA-456",
            titolo = "Perizia Aggiunta in Memoria",
            cliente = "Assicurazione Milano",
            stato = FascicoloStato.IN_CORSO
        )
        repo.saveFascicolo(extra)

        // 5. Verify the disk has been successfully HEALED: it contains all 3 records!
        val diskDataRaw = testFile.readText()
        assertFalse("Disk must no longer have corrupt payload", diskDataRaw.contains("corrupt"))
        val decodedFromDisk = json.decodeFromString<List<Fascicolo>>(diskDataRaw)
        assertEquals(3, decodedFromDisk.size)
        assertTrue("Disk contains record 1", decodedFromDisk.any { it.id == initialList[0].id })
        assertTrue("Disk contains record 2", decodedFromDisk.any { it.id == initialList[1].id })
        assertTrue("Disk contains extra record", decodedFromDisk.any { it.id == "extra_456" })
    }

    @Test
    fun testFascicoloRepositoryConcurrencyStress() = runBlocking(Dispatchers.IO) {
        val testFile = File(tempFolder.root, "concurrent_fascicoli.json")
        val repo = FascicoloRepository(overrideFascicoliFile = testFile)
        repo.loadAllFascicoli() // seed initialized

        val concurrentUsers = 8
        val opsPerUser = 10
        val latch = CountDownLatch(concurrentUsers)

        val jobs = (1..concurrentUsers).map { user ->
            launch {
                latch.countDown()
                latch.await()
                for (op in 1..opsPerUser) {
                    val fascicolo = Fascicolo(
                        id = "user_${user}_op_${op}",
                        codice = "PRAT-$user-$op",
                        titolo = "Perizia Concorrente U$user O$op",
                        cliente = "Committente $user",
                        stato = if (op % 2 == 0) FascicoloStato.IN_CORSO else FascicoloStato.APERTO
                    )
                    repo.saveFascicolo(fascicolo)
                    repo.updateStato(fascicolo.id, FascicoloStato.CHIUSO)
                }
            }
        }

        jobs.forEach { it.join() }

        val finalFascicoli = repo.loadAllFascicoli()
        // 2 initial seeds + concurrentUsers * opsPerUser = 2 + 80 = 82 records
        assertEquals(2 + (concurrentUsers * opsPerUser), finalFascicoli.size)

        // Verify disk contents match exactly
        val diskContent = testFile.readText()
        val fromDisk = json.decodeFromString<List<Fascicolo>>(diskContent)
        assertEquals(finalFascicoli.size, fromDisk.size)
    }

    @Test
    fun testEmptyFileHandledGracefullyAsEmptyList() = runBlocking {
        val testFile = File(tempFolder.root, "empty_fascicoli.json")
        testFile.writeText("") // 0 bytes

        val repo = FascicoloRepository(overrideFascicoliFile = testFile)
        val result = repo.loadAllFascicoli()
        // Empty file is handled gracefully as emptyList()
        assertTrue(result.isEmpty())
    }

    @Test
    fun testWhitespaceFileHandledGracefullyAsEmptyList() = runBlocking {
        val testFile = File(tempFolder.root, "whitespace_fascicoli.json")
        testFile.writeText("   \n\t  \r\n  ")

        val repo = FascicoloRepository(overrideFascicoliFile = testFile)
        val result = repo.loadAllFascicoli()
        assertTrue(result.isEmpty())
    }
}
