package com.example.assistenteperitomobile.ui

import com.example.assistenteperitomobile.domain.FascicoloStato
import com.example.assistenteperitomobile.domain.MainAppTab
import com.example.assistenteperitomobile.theme.AppleBlue
import com.example.assistenteperitomobile.theme.AppleBlueDark
import com.example.assistenteperitomobile.theme.AppleGreen
import com.example.assistenteperitomobile.theme.AppleOrange
import com.example.assistenteperitomobile.theme.DavezBordeaux
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiNavigationVisualConsistencyTest {

    @Test
    fun testMainAppTabContainsOnlyReachablePrimaryTabs() {
        val tabNames = MainAppTab.entries.map { it.name }
        assertEquals("MainAppTab deve contenere solo CHAT e AGENDA", listOf("CHAT", "AGENDA"), tabNames)
        assertEquals("Assistente PeritoAI", MainAppTab.CHAT.title)
        assertEquals("Agenda Sopralluoghi", MainAppTab.AGENDA.title)
    }

    @Test
    fun testModelVisualStatusValuesAndLabels() {
        val statuses = ModelVisualStatus.entries
        assertEquals(3, statuses.size)
        assertEquals("Modello pronto", ModelVisualStatus.READY.label)
        assertEquals("Download in corso", ModelVisualStatus.DOWNLOADING.label)
        assertEquals("Modello non configurato", ModelVisualStatus.NOT_CONFIGURED.label)
    }

    @Test
    fun testBrandColorConsistencyAgainstBlueDrift() {
        // AppleBlue nel tema di progetto è un bordeaux scuro architettonico (0xFF5E1122), NON blu acceso iOS (#0071E3)
        val redChannel = AppleBlue.red
        val blueChannel = AppleBlue.blue
        assertTrue("AppleBlue deve avere componente rossa dominante (Bordeaux)", redChannel > blueChannel)

        // DavezBordeaux deve coincidere con il brand primario #4F0B18
        assertEquals(androidx.compose.ui.graphics.Color(0xFF4F0B18), DavezBordeaux)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF4F0B18), AppleBlueDark)
    }

    @Test
    fun testSemanticBackgroundAndSurfaceHierarchy() {
        // Verifica che lo sfondo del tema sia l'avorio Alabaster e le card siano superfici bianche
        assertEquals(androidx.compose.ui.graphics.Color(0xFFF8F4F1), com.example.assistenteperitomobile.theme.DavezAlabaster)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFFFFFF), com.example.assistenteperitomobile.theme.AppleWhite)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFD7C9BF), com.example.assistenteperitomobile.theme.DavezStoneBorder)
        assertTrue(
            "AppleWhite deve essere più luminoso o distinto dallo sfondo DavezAlabaster",
            com.example.assistenteperitomobile.theme.AppleWhite != com.example.assistenteperitomobile.theme.DavezAlabaster
        )
    }

    @Test
    fun testFascicoliPraticheScreenSupportsShowHeaderParameter() {
        val clazz = Class.forName("com.example.assistenteperitomobile.ui.FascicoliPraticheScreenKt")
        val methods = clazz.declaredMethods.filter { it.name.startsWith("FascicoliPraticheScreen") }
        assertTrue("FascicoliPraticheScreen deve essere presente nel package ui", methods.isNotEmpty())
        val hasBooleanParam = methods.any { method ->
            method.parameterTypes.any { it == java.lang.Boolean.TYPE }
        }
        assertTrue("FascicoliPraticheScreen deve accettare un parametro booleano (showHeader)", hasBooleanParam)
    }

    @Test
    fun testFascicoloStatoDistinctLabels() {
        val stati = FascicoloStato.entries
        assertTrue(stati.contains(FascicoloStato.APERTO))
        assertTrue(stati.contains(FascicoloStato.IN_CORSO))
        assertTrue(stati.contains(FascicoloStato.CHIUSO))
        val labels = stati.map { it.label }
        assertEquals("Ogni stato fascicolo deve avere un label univoco", labels.distinct().size, labels.size)
    }
}
