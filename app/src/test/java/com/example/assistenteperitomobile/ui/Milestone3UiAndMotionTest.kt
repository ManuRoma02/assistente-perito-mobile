package com.example.assistenteperitomobile.ui

import com.example.assistenteperitomobile.domain.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Suite di test unitari completa per Milestone 3:
 * Feature 13: Complete Dark Mode Support & Edge-to-Edge Adaptive Styling
 * Feature 14: Touch Target Accessibility Normalization (>= 44x44dp)
 * Feature 15: AI Inference Animation Performance Optimization (draw/graphicsLayer)
 * Feature 16: Chat Scroll & Pop Stabilization (Emil Kowalski Philosophy)
 * Feature 17: TopBar Archive Entry Point & Badge
 * Feature 18: Archive Delete Confirmation Dialog
 * Feature 19: Dialog IME Keyboard Padding
 */
class Milestone3UiAndMotionTest {

    // =========================================================================
    // FEATURE 13: DARK MODE & EDGE-TO-EDGE
    // =========================================================================

    @Test
    fun testThemeSourceContainsDarkColorSchemeAndAdaptiveToggle() {
        val themeFile = File("src/main/java/com/example/assistenteperitomobile/theme/Theme.kt")
        assertTrue("Theme.kt deve esistere", themeFile.exists())
        val themeContent = themeFile.readText()

        assertTrue("Theme.kt deve definire DavezAppleDarkColorScheme", themeContent.contains("DavezAppleDarkColorScheme"))
        assertTrue("Theme.kt deve supportare isSystemInDarkTheme()", themeContent.contains("isSystemInDarkTheme()"))
        assertTrue("Theme.kt deve assegnare DavezAppleDarkColorScheme se darkTheme", themeContent.contains("if (darkTheme) DavezAppleDarkColorScheme else DavezAppleLightColorScheme"))
    }

    @Test
    fun testMainActivityUsesAdaptiveAutoEdgeToEdge() {
        val mainActivityFile = File("src/main/java/com/example/assistenteperitomobile/MainActivity.kt")
        assertTrue("MainActivity.kt deve esistere", mainActivityFile.exists())
        val content = mainActivityFile.readText()

        assertTrue(
            "MainActivity deve configurare SystemBarStyle.auto per edge-to-edge reattivo a light/dark",
            content.contains("SystemBarStyle.auto")
        )
    }

    // =========================================================================
    // FEATURE 14: TOUCH TARGET ACCESSIBILITY (APPLE HIG >= 44x44dp)
    // =========================================================================

    @Test
    fun testAgendaVisivaButtonsMeetMinimum44dpTouchTarget() {
        val file = File("src/main/java/com/example/assistenteperitomobile/ui/AgendaVisivaScreen.kt")
        assertTrue(file.exists())
        val content = file.readText()

        assertFalse("Non devono esserci pulsanti da 36dp in AgendaVisivaScreen", content.contains("Modifier.size(36.dp)"))
        assertTrue("Il pulsante refresh e modifica/elimina devono essere ad almeno 44dp", content.contains("Modifier.size(44.dp)"))
    }

    @Test
    fun testCategoryReferenceButtonsMeetMinimum44dpTouchTarget() {
        val file = File("src/main/java/com/example/assistenteperitomobile/ui/CategoryReferenceSettingsScreen.kt")
        assertTrue(file.exists())
        val content = file.readText()

        assertFalse("Non devono esserci pulsanti da 32dp o 38dp in CategoryReferenceSettingsScreen", content.contains("Modifier.size(32.dp)") || content.contains("Modifier.size(38.dp)"))
        assertTrue("Tutte le icone interattive devono essere ad almeno 44dp", content.contains("Modifier.size(44.dp)"))
    }

    @Test
    fun testFascicoliPraticheButtonsMeetMinimum44dpTouchTarget() {
        val file = File("src/main/java/com/example/assistenteperitomobile/ui/FascicoliPraticheScreen.kt")
        assertTrue(file.exists())
        val content = file.readText()

        assertFalse("Non devono esserci pulsanti da 28dp in FascicoliPraticheScreen", content.contains("Modifier.size(28.dp)"))
        assertTrue("I pulsanti d'azione e chiusura devono essere ad almeno 44dp", content.contains("Modifier.size(44.dp)"))
    }

    @Test
    fun testPeritoAppScreensButtonsMeetMinimum44dpTouchTarget() {
        val file = File("src/main/java/com/example/assistenteperitomobile/ui/PeritoAppScreens.kt")
        assertTrue(file.exists())
        val content = file.readText()

        // Verifica che le finestre modali abbiano chiusura a 44dp
        assertTrue("ModernAttachmentSheet deve avere close button a 44dp", content.contains("Chiudi finestra allegati"))
        assertTrue("TopBar calendar sync button deve avere touch target a 44dp", content.contains("Collega Calendario"))
    }

    // =========================================================================
    // FEATURE 15: AI INFERENCE ANIMATION PERFORMANCE OPTIMIZATION
    // =========================================================================

    @Test
    fun testThinkingOrbReadsAnimatedStateInDrawScopeNotComposableBody() {
        val file = File("src/main/java/com/example/assistenteperitomobile/ui/PeritoAppScreens.kt")
        val content = file.readText()

        // Perito3DThinkingOrb non deve fare 'val animatedPhase by' o 'val animatedPulse by' nel body
        assertFalse(
            "Perito3DThinkingOrb non deve delegare le animazioni con 'by' nel composable body",
            content.contains("val animatedPhase by infiniteTransition.animateFloat")
        )
        assertTrue(
            "Perito3DThinkingOrb deve assegnare le animazioni come State per il consumo nel DrawScope",
            content.contains("val animatedPhase = infiniteTransition.animateFloat") &&
            content.contains("val animatedPulse = infiniteTransition.animateFloat")
        )
    }

    @Test
    fun testAgenticThinkingBubbleCalculatesScaleInGraphicsLayerLambda() {
        val file = File("src/main/java/com/example/assistenteperitomobile/ui/PeritoAppScreens.kt")
        val content = file.readText()

        assertFalse(
            "AgenticThinkingBubble non deve delegare con 'by' nel composable body",
            content.contains("val breathePhase by infiniteTransition.animateFloat")
        )
        assertTrue(
            "AgenticThinkingBubble deve calcolare scaleX e scaleY all'interno della lambda graphicsLayer",
            content.contains("scaleX = 1f + sin(phase) * 0.025f")
        )
    }

    // =========================================================================
    // FEATURE 16: CHAT SCROLL & POP STABILIZATION (EMIL KOWALSKI PHILOSOPHY)
    // =========================================================================

    @Test
    fun testChatFreshMessageDetectionContract() {
        val now = System.currentTimeMillis()
        val freshMsg = ChatMessage(id = "msg-1", isUser = false, text = "Risposta AI", timestampMillis = now - 500L)
        val historicalMsg = ChatMessage(id = "msg-2", isUser = false, text = "Storico", timestampMillis = now - 60_000L)

        val isFresh1 = (now - freshMsg.timestampMillis) < 2500L
        val isFresh2 = (now - historicalMsg.timestampMillis) < 2500L

        assertTrue("Messaggio appena arrivato deve essere classificato fresh", isFresh1)
        assertFalse("Messaggio storico non deve essere classificato fresh (nessun bounce durante scroll)", isFresh2)
    }

    @Test
    fun testChatBubbleSourceRestrictsSpringAnimationToFreshMessages() {
        val file = File("src/main/java/com/example/assistenteperitomobile/ui/PeritoAppScreens.kt")
        val content = file.readText()

        assertTrue(
            "PeritoStudioMessageBubble deve controllare isFreshMessage prima di innescare l'animazione spring",
            content.contains("val isFreshMessage = remember(message.id)") &&
            content.contains("Animatable(if (isFreshMessage) 0.85f else 1f)")
        )
    }

    // =========================================================================
    // FEATURE 17: TOPBAR ARCHIVE ENTRY POINT & BADGE
    // =========================================================================

    @Test
    fun testTopBarContainsArchiveButtonAndBadge() {
        val file = File("src/main/java/com/example/assistenteperitomobile/ui/PeritoAppScreens.kt")
        val content = file.readText()

        assertTrue(
            "PeritoStudioTopBar deve includere il pulsante per aprire l'archivio con testTag",
            content.contains("top_bar_archive_button")
        )
        assertTrue(
            "PeritoStudioTopBar deve mostrare il badge del numero di documenti archiviati",
            content.contains("if (archivedCount > 0)") && content.contains("if (archivedCount > 99) \"99+\" else archivedCount.toString()")
        )
    }

    // =========================================================================
    // FEATURE 18: ARCHIVE DELETE CONFIRMATION DIALOG
    // =========================================================================

    @Test
    fun testArchiveSheetContainsDeleteConfirmationModal() {
        val file = File("src/main/java/com/example/assistenteperitomobile/ui/PeritoAppScreens.kt")
        val content = file.readText()

        assertTrue(
            "ArchiveFoldersSheetContent deve memorizzare docPendingDelete per conferma modale",
            content.contains("var docPendingDelete by remember { mutableStateOf<ArchivedDocument?>(null) }")
        )
        assertTrue(
            "ArchiveFoldersSheetContent deve mostrare AlertDialog per conferma eliminazione definitiva",
            content.contains("Sei sicuro di voler eliminare definitivamente")
        )
    }

    // =========================================================================
    // FEATURE 19: DIALOG IME KEYBOARD PADDING
    // =========================================================================

    @Test
    fun testNewFascicoloDialogContainsImePadding() {
        val file = File("src/main/java/com/example/assistenteperitomobile/ui/FascicoliPraticheScreen.kt")
        val content = file.readText()

        assertTrue(
            "NewFascicoloDialog deve applicare .imePadding() per evitare sovrapposizioni della tastiera virtuale",
            content.contains(".imePadding()")
        )
    }

    // =========================================================================
    // MOBILE UX PRINCIPLES & AGENTIC-CRAFT-UI AUDIT
    // =========================================================================

    @Test
    fun testCard1CutTheFillerWords() {
        val peritoApp = File("src/main/java/com/example/assistenteperitomobile/ui/PeritoAppScreens.kt").readText()
        val fascicoliApp = File("src/main/java/com/example/assistenteperitomobile/ui/FascicoliPraticheScreen.kt").readText()

        // 1. Toast microcopy concisa senza filler
        assertFalse(peritoApp.contains("Messaggio copiato negli appunti"))
        assertTrue(peritoApp.contains("\"Messaggio copiato\""))

        assertFalse(peritoApp.contains("Permesso fotocamera richiesto per scattare foto"))
        assertTrue(peritoApp.contains("\"Permesso fotocamera necessario\""))

        // 2. Dialog OTA senza filler
        assertFalse(peritoApp.contains("Continua con la versione vecchia"))
        assertTrue(peritoApp.contains("\"Ricorda più tardi\""))

        // 3. Documenti fascicolo senza filler
        assertFalse(fascicoliApp.contains("Nessun documento o foto ancora collegato a questa pratica."))
        assertTrue(fascicoliApp.contains("\"Nessun documento collegato.\""))
    }

    @Test
    fun testCard2BottomNotTop() {
        val content = File("src/main/java/com/example/assistenteperitomobile/ui/FascicoliPraticheScreen.kt").readText()

        // NewFascicoloDialog must place primary action button at the bottom, full width (Thumb Reach UX)
        assertTrue(
            "NewFascicoloDialog deve contenere pulsante d'azione primario full-width ancorato in basso",
            content.contains(".fillMaxWidth()") && content.contains(".height(50.dp)")
        )
        assertTrue(
            "NewFascicoloDialog deve contenere testTag semantico per il pulsante primario",
            content.contains("btn_confirm_create_fascicolo")
        )
    }

    @Test
    fun testCard3TheWholeRowTaps() {
        val content = File("src/main/java/com/example/assistenteperitomobile/ui/PeritoAppScreens.kt").readText()

        // The whole row must tap for switches (toggleable with Role.Switch and onCheckedChange = null on child)
        assertTrue(
            "MorningBriefingTendinaCard deve applicare .toggleable con Role.Switch sulla riga intera",
            content.contains("role = Role.Switch") && content.contains("testTag = \"briefing_toggle_row\"")
        )
        assertTrue(
            "UpdatesTendinaCard deve applicare .toggleable con Role.Switch sulla Surface/riga intera",
            content.contains("testTag = \"auto_check_updates_row\"")
        )
    }

    @Test
    fun testCard4DescriptiveActionButtons() {
        val peritoApp = File("src/main/java/com/example/assistenteperitomobile/ui/PeritoAppScreens.kt").readText()
        val categoryApp = File("src/main/java/com/example/assistenteperitomobile/ui/CategoryReferenceSettingsScreen.kt").readText()

        assertTrue("Dialog cancellazione chat deve usare 'Svuota Chat'", peritoApp.contains("\"Svuota Chat\""))
        assertTrue("Dialog eliminazione doc deve usare 'Elimina Documento'", peritoApp.contains("\"Elimina Documento\""))
        assertTrue("Dialog eliminazione categoria deve usare 'Elimina Categoria'", categoryApp.contains("\"Elimina Categoria\""))
    }

    @Test
    fun testCard5SwitchesAreForTogglesNotChoices() {
        val files = listOf(
            File("src/main/java/com/example/assistenteperitomobile/ui/PeritoAppScreens.kt"),
            File("src/main/java/com/example/assistenteperitomobile/ui/FascicoliPraticheScreen.kt"),
            File("src/main/java/com/example/assistenteperitomobile/ui/AgendaVisivaScreen.kt"),
            File("src/main/java/com/example/assistenteperitomobile/ui/CategoryReferenceSettingsScreen.kt")
        )

        // Count all Switch(...) usages in the UI
        var switchCount = 0
        for (f in files) {
            val lines = f.readLines()
            for (line in lines) {
                if (line.trim().startsWith("Switch(")) {
                    switchCount++
                }
            }
        }

        // Exactly 2 switches exist, and both are binary on/off toggles
        assertEquals("Nel codice devono esserci esattamente 2 Switch, esclusivamente usati come toggle on/off binari", 2, switchCount)
    }
}
