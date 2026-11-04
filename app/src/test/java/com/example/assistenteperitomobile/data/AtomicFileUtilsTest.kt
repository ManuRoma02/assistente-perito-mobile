package com.example.assistenteperitomobile.data

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AtomicFileUtilsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testAtomicWriteAndRead() {
        val target = File(tempFolder.root, "atomic_test.txt")
        val content = "Studio Peritale Davez - Rilievo Tecnico Atomico"

        AtomicFileUtils.writeText(target, content)

        assertTrue(target.exists())
        assertEquals(content, AtomicFileUtils.readText(target))

        // Verifica assenza di file .tmp o .bak residui
        val files = tempFolder.root.listFiles() ?: emptyArray()
        assertEquals(1, files.size)
        assertEquals("atomic_test.txt", files[0].name)
    }

    @Test
    fun testAtomicWriteOverwritesCleanlyWithoutDataLoss() {
        val target = File(tempFolder.root, "atomic_overwrite.txt")
        AtomicFileUtils.writeText(target, "Versione 1")
        assertEquals("Versione 1", AtomicFileUtils.readText(target))

        AtomicFileUtils.writeText(target, "Versione 2 Aggiornata con Dati Completi")
        assertEquals("Versione 2 Aggiornata con Dati Completi", AtomicFileUtils.readText(target))
    }

    @Test
    fun testSyncBackupBeforeReadRestoresFromBakWhenTargetMissing() {
        val target = File(tempFolder.root, "crash_recovery.txt")
        val bak = File(tempFolder.root, "crash_recovery.txt.bak")

        bak.writeText("Dati salvati nel backup precedente al crash")
        if (target.exists()) target.delete()

        // syncBackupBeforeRead deve ripristinare il file target
        AtomicFileUtils.syncBackupBeforeRead(target)

        assertTrue(target.exists())
        assertEquals("Dati salvati nel backup precedente al crash", target.readText())
    }

    @Test
    fun testSyncBackupBeforeReadDiscardsBakWhenTargetExists() {
        val target = File(tempFolder.root, "committed.txt")
        val bak = File(tempFolder.root, "committed.txt.bak")

        target.writeText("Nuovo Contenuto Comitato")
        bak.writeText("Vecchio Contenuto Obsoleto")

        AtomicFileUtils.syncBackupBeforeRead(target)

        assertTrue("Target deve rimanere presente", target.exists())
        assertEquals("Nuovo Contenuto Comitato", target.readText())
        assertFalse("Il vecchio .bak deve essere eliminato poiché target era già stato completato", bak.exists())
    }

    @Test
    fun testAtomicDeleteRemovesBothTargetAndBak() {
        val target = File(tempFolder.root, "to_delete.txt")
        val bak = File(tempFolder.root, "to_delete.txt.bak")

        target.writeText("content")
        bak.writeText("backup")

        val deleted = AtomicFileUtils.delete(target)
        assertTrue(deleted)
        assertFalse(target.exists())
        assertFalse(bak.exists())
    }
}
