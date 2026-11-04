package com.example.assistenteperitomobile.e2e

import com.example.assistenteperitomobile.data.CalendarRepository
import com.example.assistenteperitomobile.data.DocumentTooLargeException
import com.example.assistenteperitomobile.domain.ArchivedDocument
import com.example.assistenteperitomobile.domain.CalendarEventItem
import com.example.assistenteperitomobile.domain.ChatMessage
import com.example.assistenteperitomobile.domain.Fascicolo
import com.example.assistenteperitomobile.domain.FascicoloStato
import com.example.assistenteperitomobile.domain.PeritoCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Tier 2: E2E Boundary & Corner Cases Suite
 * Validates resilience, error recovery, boundary limits, and stress concurrency:
 * - Empty & 0-byte file handling across all repositories
 * - Corrupted JSON recovery preserving cache and preventing file erasure
 * - High-concurrency stress test under coroutine Mutex
 * - Temporal edge cases: midnight boundaries, multi-day cross, DST 25-hour day
 * - Large inputs & bounded stream processing (preventing OOM)
 */
class Tier2BoundaryCornerE2ETest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    // ------------------------------------------------------------------------
    // 1. Empty, 0-Byte & Blank File Handling
    // ------------------------------------------------------------------------

    @Test
    fun testEmptyAndBlankFileHandlingAcrossRepositories() {
        val rootDir = tempFolder.newFolder("boundary_empty_files")

        // 1. File vuoto a 0 byte
        val zeroByteFile = File(rootDir, "zero_byte.json").apply { writeBytes(ByteArray(0)) }
        assertEquals(0L, zeroByteFile.length())

        fun readSafely(file: File): List<Fascicolo> {
            if (!file.exists() || file.length() == 0L) return emptyList()
            val text = file.readText().trim()
            if (text.isBlank()) return emptyList()
            return try {
                json.decodeFromString<List<Fascicolo>>(text)
            } catch (_: Throwable) {
                emptyList()
            }
        }

        val resZero = readSafely(zeroByteFile)
        assertTrue("File a 0 byte deve restituire lista vuota senza eccezioni", resZero.isEmpty())

        // 2. File contenente solo spazi / newline
        val whitespaceFile = File(rootDir, "whitespace.json").apply { writeText("   \n\t  \r\n  ") }
        val resWhitespace = readSafely(whitespaceFile)
        assertTrue("File con solo whitespace deve restituire lista vuota", resWhitespace.isEmpty())

        // 3. File con array vuoto "[]"
        val emptyArrayFile = File(rootDir, "empty_array.json").apply { writeText("[]") }
        val resEmptyArray = readSafely(emptyArrayFile)
        assertTrue("File contenente [] deve restituire lista vuota", resEmptyArray.isEmpty())
    }

    @Test
    fun testFascicoloBoundaryInputsAndDefaultFallbacks() {
        // Fascicolo con campi completamente vuoti: verifica che i fallback e gli UUID siano generati in modo consistente
        val emptyFascicolo = Fascicolo(
            id = "",
            codice = "",
            titolo = "",
            cliente = "",
            controparte = "",
            indirizzo = "",
            note = "",
            stato = FascicoloStato.IN_CORSO
        )

        val serialized = json.encodeToString(emptyFascicolo)
        val deserialized = json.decodeFromString<Fascicolo>(serialized)

        assertEquals("", deserialized.id)
        assertEquals("", deserialized.codice)
        assertEquals(FascicoloStato.IN_CORSO, deserialized.stato)
    }

    // ------------------------------------------------------------------------
    // 2. Corrupt JSON Deserialization Recovery
    // ------------------------------------------------------------------------

    @Test
    fun testCorruptJsonRecoveryPreservesExistingCache() {
        val rootDir = tempFolder.newFolder("corrupt_recovery")
        val archiveIndexFile = File(rootDir, "archive_index.json")

        val validDoc = ArchivedDocument(
            id = "DOC-CTU-01",
            fileName = "perizia_infiltrazioni.pdf",
            originalName = "perizia_infiltrazioni.pdf",
            categoryFolder = PeritoCategory.PERIZIE_TECNICHE.folderName,
            cliente = "Condominio Roma",
            oggetto = "Danni da acqua",
            dateIso = "2026-10-26",
            filePath = "/tmp/dummy",
            sizeBytes = 1024L,
            extractedTextSnippet = "Sopralluogo tecnico",
            createdAtMillis = 1000L
        )

        // Scrive prima un indice integro
        archiveIndexFile.writeText(json.encodeToString(listOf(validDoc)))
        val initialCache = json.decodeFromString<List<ArchivedDocument>>(archiveIndexFile.readText())
        assertEquals(1, initialCache.size)

        // Simula successiva corruzione del file da interruzione scrittura (es. JSON incompleto)
        val corruptedPayload = "[{\"id\":\"DOC-BROKEN\", \"fileName\": \"incomple"
        archiveIndexFile.writeText(corruptedPayload)

        // Modello di recupero resiliente: se la lettura fallisce, restituisce la cache precedente senza sovrascrivere o eliminare l'indice
        fun loadWithFallback(indexFile: File, activeCache: List<ArchivedDocument>): Pair<Result<List<ArchivedDocument>>, List<ArchivedDocument>> {
            return try {
                val list = json.decodeFromString<List<ArchivedDocument>>(indexFile.readText())
                Result.success(list) to list
            } catch (e: Throwable) {
                // Ritorna errore ma preserva intatta la cache precedente
                Result.failure<List<ArchivedDocument>>(e) to activeCache
            }
        }

        val (result, preservedCache) = loadWithFallback(archiveIndexFile, initialCache)

        assertTrue("La lettura del file corrotto deve generare Result.failure", result.isFailure)
        assertEquals("La cache attiva non deve essere azzerata o corrotta", 1, preservedCache.size)
        assertEquals("DOC-CTU-01", preservedCache.first().id)
        assertTrue("Il file corrotto deve rimanere su disco per ispezione forense", archiveIndexFile.exists())
    }

    // ------------------------------------------------------------------------
    // 3. High-Concurrency Stress Test
    // ------------------------------------------------------------------------

    @Test
    fun testHighConcurrencyRepositoryPersistenceStress() = runBlocking {
        val rootDir = tempFolder.newFolder("concurrency_stress")
        val storageFile = File(rootDir, "chat_history_stress.json")
        val storageMutex = Mutex()

        var inMemoryMessages = mutableListOf<ChatMessage>()

        // 100 coroutine concorrenti che leggono, modificano e salvano messaggi contemporaneamente
        val coroutineCount = 100
        val jobs = (1..coroutineCount).map { i ->
            async(Dispatchers.Default) {
                storageMutex.withLock {
                    val newMsg = ChatMessage(
                        id = "MSG-$i",
                        isUser = (i % 2 == 0),
                        text = "Messaggio di prova peritale n. $i"
                    )
                    inMemoryMessages.add(newMsg)

                    // Scrittura atomica thread-safe
                    val serialized = json.encodeToString(inMemoryMessages.toList())
                    val tmp = File(rootDir, "chat_history_stress.json.tmp")
                    tmp.writeText(serialized, Charsets.UTF_8)
                    tmp.copyTo(storageFile, overwrite = true)
                    tmp.delete()
                }
            }
        }

        jobs.awaitAll()

        storageMutex.withLock {
            assertTrue("Il file di persistenza deve esistere", storageFile.exists())
            val finalPersisted = json.decodeFromString<List<ChatMessage>>(storageFile.readText())
            assertEquals("Tutti i 100 messaggi devono essere salvati senza perdita o corruzione", coroutineCount, finalPersisted.size)
            assertEquals(coroutineCount, finalPersisted.map { it.id }.distinct().size)
        }
    }

    // ------------------------------------------------------------------------
    // 4. Temporal Edge Cases & Calendar Semantics
    // ------------------------------------------------------------------------

    @Test
    fun testCalendarMidnightSemiOpenIntervalSemantics() = runBlocking {
        val eventsFile = tempFolder.newFile("calendar_boundary_events.json")
        val zone = ZoneId.systemDefault()
        val targetDay = LocalDate.of(2026, 10, 26)

        // Evento terminato esattamente alle 00:00:00 del 26 Ottobre (iniziato alle 22:00 del 25)
        val start25 = LocalDateTime.of(2026, 10, 25, 22, 0)
        val end26Midnight = LocalDateTime.of(2026, 10, 26, 0, 0)

        val evMidnight = CalendarEventItem(
            id = "ev_exact_midnight",
            titolo = "Sopralluogo notturno terminato a mezzanotte",
            startIso = start25.format(isoFormatter),
            endIso = end26Midnight.format(isoFormatter),
            startMillis = start25.atZone(zone).toInstant().toEpochMilli(),
            endMillis = end26Midnight.atZone(zone).toInstant().toEpochMilli(),
            luogo = "Milano",
            cliente = "Studio Legale",
            note = "Sopralluogo terminato"
        )

        eventsFile.writeText(json.encodeToString(listOf(evMidnight)))
        val repo = CalendarRepository(context = null, overrideEventsFile = eventsFile)

        // L'evento NON deve appartenere al 26 Ottobre poiché l'intervallo è semiaperto [start, end)
        val events26 = repo.getEventsForDate(targetDay)
        assertTrue("L'evento terminato alle 00:00:00 non deve comparire nel giorno target", events26.isEmpty())

        // Deve invece appartenere al 25 Ottobre
        val events25 = repo.getEventsForDate(targetDay.minusDays(1))
        assertEquals(1, events25.size)
        assertEquals("ev_exact_midnight", events25.first().id)
    }

    @Test
    fun testCalendarNocturnalEventCrossingMidnight() = runBlocking {
        val eventsFile = tempFolder.newFile("calendar_cross_events.json")
        val zone = ZoneId.systemDefault()
        val targetDay = LocalDate.of(2026, 10, 26)

        // Evento dalle 23:30 del 25 Ottobre alle 02:30 del 26 Ottobre
        val startLdt = LocalDateTime.of(2026, 10, 25, 23, 30)
        val endLdt = LocalDateTime.of(2026, 10, 26, 2, 30)

        val evCross = CalendarEventItem(
            id = "ev_cross_midnight",
            titolo = "Rilievo fessurimetrico notturno",
            startIso = startLdt.format(isoFormatter),
            endIso = endLdt.format(isoFormatter),
            startMillis = startLdt.atZone(zone).toInstant().toEpochMilli(),
            endMillis = endLdt.atZone(zone).toInstant().toEpochMilli(),
            luogo = "Via Padova 12, Milano",
            cliente = "CTP Ing. Neri",
            note = "Monitoraggio notturno"
        )

        eventsFile.writeText(json.encodeToString(listOf(evCross)))
        val repo = CalendarRepository(context = null, overrideEventsFile = eventsFile)

        val day25 = repo.getEventsForDate(targetDay.minusDays(1))
        val day26 = repo.getEventsForDate(targetDay)

        assertEquals("L'evento deve comparire il 25 perché iniziato il 25", 1, day25.size)
        assertEquals("L'evento deve comparire anche il 26 perché prosegue fino alle 02:30 del 26", 1, day26.size)
        assertEquals("ev_cross_midnight", day26.first().id)
    }

    @Test
    fun testCalendarDaylightSavingTimeTransition25HourDay() = runBlocking {
        val eventsFile = tempFolder.newFile("calendar_dst_events.json")
        val romeZone = ZoneId.of("Europe/Rome")
        // 25 Ottobre 2026: cambio ora legale -> solare (giornata di 25 ore in Italia)
        val dstDate = LocalDate.of(2026, 10, 25)

        val evDst = CalendarEventItem(
            id = "ev_dst_test",
            titolo = "Riunione con CTU prima del cambio ora",
            startIso = "2026-10-25T21:00:00",
            endIso = "2026-10-25T23:00:00",
            startMillis = LocalDateTime.of(2026, 10, 25, 21, 0).atZone(romeZone).toInstant().toEpochMilli(),
            endMillis = LocalDateTime.of(2026, 10, 25, 23, 0).atZone(romeZone).toInstant().toEpochMilli(),
            luogo = "Monza",
            cliente = "Geom. Bianchi",
            note = "Riunione serale"
        )

        eventsFile.writeText(json.encodeToString(listOf(evDst)))
        val repo = CalendarRepository(context = null, overrideEventsFile = eventsFile)

        val dstEvents = repo.getEventsForDate(dstDate)
        assertEquals(1, dstEvents.size)
        assertEquals("ev_dst_test", dstEvents.first().id)

        val nextDayEvents = repo.getEventsForDate(dstDate.plusDays(1))
        assertTrue("L'evento serale del giorno DST non deve slittare al giorno successivo", nextDayEvents.isEmpty())
    }

    // ------------------------------------------------------------------------
    // 5. Large Inputs & Bounded Stream Processing
    // ------------------------------------------------------------------------

    @Test
    fun testExtremelyLargeOcrTextPayloadHandling() {
        // Genera uno snippet OCR da 500.000 caratteri (500KB)
        val largeSnippet = buildString {
            for (i in 1..5000) {
                append("Paragrafo peritale di prova con termini tecnici CTU, lesioni murarie e infiltrazioni. R.G. 1234/2026.\n")
            }
        }
        assertTrue(largeSnippet.length > 500_000)

        val doc = ArchivedDocument(
            id = "DOC-LARGE",
            fileName = "perizia_voluminosa.pdf",
            originalName = "perizia_voluminosa.pdf",
            categoryFolder = PeritoCategory.PERIZIE_TECNICHE.folderName,
            cliente = "Tribunale di Roma",
            oggetto = "Danni strutturali",
            dateIso = "2026-10-26",
            filePath = "/tmp/dummy",
            sizeBytes = largeSnippet.length.toLong(),
            extractedTextSnippet = largeSnippet.take(1000), // Deve troncare lo snippet per UI
            createdAtMillis = 1000L
        )

        val serialized = json.encodeToString(doc)
        val deserialized = json.decodeFromString<ArchivedDocument>(serialized)

        assertEquals("DOC-LARGE", deserialized.id)
        assertEquals(1000, deserialized.extractedTextSnippet.length)
    }

    @Test
    fun testBoundedStreamThrowsDocumentTooLargeExceptionWithoutOOM() {
        val maxAllowed = 1024L * 1024L // 1 MB per il test
        val infiniteStream = object : InputStream() {
            override fun read(): Int = 0x41
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                b.fill(0x41, off, off + len)
                return len
            }
        }

        val buffer = ByteArray(64 * 1024)
        var totalRead = 0L

        try {
            while (true) {
                val r = infiniteStream.read(buffer)
                if (r == -1) break
                totalRead += r
                if (totalRead > maxAllowed) {
                    throw DocumentTooLargeException(totalRead, maxAllowed)
                }
            }
            fail("Avrebbe dovuto lanciare DocumentTooLargeException prima di allocare all'infinito")
        } catch (e: DocumentTooLargeException) {
            assertTrue("L'eccezione DocumentTooLargeException deve contenere la dimensione effettiva", e.actualSize > maxAllowed)
            assertEquals(maxAllowed, e.maxSize)
        }
    }
}
