package com.example.assistenteperitomobile

import com.example.assistenteperitomobile.data.CalendarRepository
import com.example.assistenteperitomobile.domain.PeritoAiEngine
import com.example.assistenteperitomobile.domain.PeritoCategoryDefinition
import com.example.assistenteperitomobile.domain.agents.AgentPipelineOrchestrator
import com.example.assistenteperitomobile.domain.agents.UserIntentCategory
import com.example.assistenteperitomobile.domain.defaultCategoriesList
import com.example.assistenteperitomobile.domain.detectCategoryCorrection
import com.example.assistenteperitomobile.domain.resolveEventCategoryVisuals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DriftRemediationTest {

    private val engine = PeritoAiEngine()
    private val orchestrator = AgentPipelineOrchestrator(engine)

    /**
     * Verifica la classificazione semantica degli intenti in AgentPipelineOrchestrator.
     * NOTA ARCHITETTURALE: Nel flusso di produzione, routedIntent genera l'etichetta semantica
     * di caricamento per la UI (processingLabel in PeritoViewModel), non il dispatch esecutivo
     * che rimane deterministico all'interno di PeritoViewModel.
     */
    @Test
    fun testIntentClassificationForUiProcessingLabel() {
        val traceId = orchestrator.newTraceId()

        val slash = orchestrator.routeMessage("/oggi ore 9", traceId)
        assertEquals(UserIntentCategory.SLASH_COMMAND, slash.category)
        assertEquals("/oggi", slash.command)

        val briefing = orchestrator.routeMessage("impegni di oggi", traceId)
        assertEquals(UserIntentCategory.DAILY_BRIEFING, briefing.category)

        val search = orchestrator.routeMessage("cerca perizia Rossi", traceId)
        assertEquals(UserIntentCategory.ARCHIVE_SEARCH, search.category)

        val delete = orchestrator.routeMessage("elimina appuntamento delle 15", traceId)
        assertEquals(UserIntentCategory.CORRECTION_OR_DELETE, delete.category)

        val appointment = orchestrator.routeMessage("Sopralluogo domani alle 10", traceId)
        assertEquals(UserIntentCategory.APPOINTMENT_REQUEST, appointment.category)
    }

    /**
     * Esercita direttamente CalendarRepository.resolveWritableCalendarId (codice di produzione).
     */
    @Test
    fun testPrimaryCalendarFallbackSelectionInRepository() {
        // Caso 1: Calendario non-primario per primo nei risultati, primario per secondo.
        // Risoluzione corretta: deve selezionare il primario (200L).
        val rows = listOf(
            CalendarRepository.Companion.CalendarCandidate(id = 100L, isPrimary = false, accessLevel = 700),
            CalendarRepository.Companion.CalendarCandidate(id = 200L, isPrimary = true, accessLevel = 700)
        ).asSequence()
        val selectedWithoutTarget = CalendarRepository.resolveWritableCalendarId(rows, targetId = null)
        assertEquals(200L, selectedWithoutTarget)

        // Caso 2: targetId esplicito fornito (300L)
        val rowsWithTarget = listOf(
            CalendarRepository.Companion.CalendarCandidate(id = 100L, isPrimary = false, accessLevel = 700),
            CalendarRepository.Companion.CalendarCandidate(id = 200L, isPrimary = true, accessLevel = 700),
            CalendarRepository.Companion.CalendarCandidate(id = 300L, isPrimary = false, accessLevel = 700)
        ).asSequence()
        val selectedWithTarget = CalendarRepository.resolveWritableCalendarId(rowsWithTarget, targetId = 300L)
        assertEquals(300L, selectedWithTarget)

        // Caso 3: Nessun targetId e nessun primario -> seleziona il primo scrivibile
        val rowsNoPrimary = listOf(
            CalendarRepository.Companion.CalendarCandidate(id = 50L, isPrimary = false, accessLevel = 700),
            CalendarRepository.Companion.CalendarCandidate(id = 60L, isPrimary = false, accessLevel = 700)
        ).asSequence()
        assertEquals(50L, CalendarRepository.resolveWritableCalendarId(rowsNoPrimary, targetId = null))

        // Caso 4: Righe in sola lettura (< 500) ignorate
        val rowsReadOnly = listOf(
            CalendarRepository.Companion.CalendarCandidate(id = 10L, isPrimary = true, accessLevel = 200),
            CalendarRepository.Companion.CalendarCandidate(id = 20L, isPrimary = false, accessLevel = 700)
        ).asSequence()
        assertEquals(20L, CalendarRepository.resolveWritableCalendarId(rowsReadOnly, targetId = null))
    }

    /**
     * Esercita CalendarRepository.classifyEventCategory (codice di produzione)
     * verificando confini di parola, categorie dinamiche e rispetto delle priorità.
     */
    @Test
    fun testCalendarEventClassificationInProductionRepository() {
        val customCategories = listOf(
            PeritoCategoryDefinition(
                id = "sanatoria",
                displayName = "Sanatoria Edilizia",
                emoji = "📜",
                defaultPrefix = "Pratica Sanatoria",
                keywords = listOf("sanatoria", "condono")
            )
        )
        val fullCategories = defaultCategoriesList() + customCategories

        // 1. Categoria personalizzata dinamica
        val catCustom = CalendarRepository.classifyEventCategory("Pratica Sanatoria - Via Roma", fullCategories)
        assertEquals("Sanatoria Edilizia", catCustom)

        // 2. Confini di parola: "Aperitivo" NON deve essere classificato come APE
        val catAperitivo = CalendarRepository.classifyEventCategory("Aperitivo con colleghi studio", fullCategories)
        assertEquals("Personale", catAperitivo)

        // 3. Confini di parola: "Corso" NON deve essere classificato come Agenzia Orso
        val catCorso = CalendarRepository.classifyEventCategory("Corso di formazione per periti", fullCategories)
        assertTrue("Non deve essere classificato come Agenzia Orso", catCorso != "Agenzia Orso")

        // 4. Priorità: attività tecnica specifica (APE) ha priorità su generica (Sopralluogo)
        val catApe = CalendarRepository.classifyEventCategory("Sopralluogo APE via Milano", fullCategories)
        assertEquals("APE", catApe)

        // 5. Fallback legacy con lista categorie vuota
        val catLegacy = CalendarRepository.classifyEventCategory("Udienza CTU Tribunale", emptyList())
        assertEquals("Tribunale", catLegacy)
    }

    /**
     * Esercita detectCategoryCorrection testando casi di regressione:
     * - Categorie dinamiche
     * - Parole che contengono per caso una keyword (es. "aperto", "corso", "lasciare")
     * - Messaggi ordinari con "in realtà / è un"
     * - Correzioni valide
     */
    @Test
    fun testDetectCategoryCorrectionRegressionCases() {
        val customCategory = PeritoCategoryDefinition(
            id = "sanatoria",
            displayName = "Sanatoria Edilizia",
            emoji = "📜",
            defaultPrefix = "Pratica Sanatoria",
            keywords = listOf("sanatoria", "condono"),
            isSystem = false
        )
        val categories = defaultCategoriesList() + customCategory

        // A. Categorie dinamiche (valide)
        val dyn1 = detectCategoryCorrection("cambia categoria in Sanatoria Edilizia", categories)
        assertNotNull(dyn1)
        assertEquals("Sanatoria Edilizia", dyn1?.displayName)

        val dyn2 = detectCategoryCorrection("in realtà è una sanatoria", categories)
        assertNotNull(dyn2)
        assertEquals("Sanatoria Edilizia", dyn2?.displayName)

        // B. Parole che contengono casualmente sottostringhe di keyword (falsi positivi evitati)
        assertNull("Non deve matchare 'ape' dentro 'aperto'",
            detectCategoryCorrection("In realtà ho aperto la cartella peritale", categories))
        assertNull("Non deve matchare 'orso' dentro 'corso'",
            detectCategoryCorrection("Oggi ho seguito un corso per periti", categories))
        assertNull("Non deve matchare 'scia' dentro 'lasciare'",
            detectCategoryCorrection("Volevo lasciare la perizia in studio", categories))

        // C. Messaggi ordinari con 'in realtà' o 'è un/una' (non sono correzioni di categoria)
        assertNull(detectCategoryCorrection("In realtà è un buon giorno per sentirci", categories))
        assertNull(detectCategoryCorrection("In realtà è un documento importante", categories))
        assertNull(detectCategoryCorrection("In realtà è una perizia complessa", categories))
        assertNull(detectCategoryCorrection("In realtà è un caso difficile", categories))
        assertNull(detectCategoryCorrection("Qual è invece la data di udienza?", categories))

        // D. Correzioni valide esplicite
        val corr1 = detectCategoryCorrection("cambia categoria in Riunione", categories)
        assertEquals("Riunione", corr1?.displayName)

        val corr2 = detectCategoryCorrection("modifica categoria in APE", categories)
        assertEquals("APE", corr2?.displayName)

        val corr3 = detectCategoryCorrection("in realtà è un sopralluogo", categories)
        assertEquals("Sopralluogo", corr3?.displayName)

        val corr4 = detectCategoryCorrection("invece è una riunione", categories)
        assertEquals("Riunione", corr4?.displayName)

        val corr5 = detectCategoryCorrection("non è un sopralluogo, è una riunione", categories)
        assertEquals("Riunione", corr5?.displayName)

        val corr6 = detectCategoryCorrection("non è una riunione, ma un sopralluogo", categories)
        assertEquals("Sopralluogo", corr6?.displayName)

        val corr7 = detectCategoryCorrection("è in tribunale", categories)
        assertEquals("Tribunale", corr7?.displayName)

        val corr8 = detectCategoryCorrection("è in comune", categories)
        assertEquals("Comune", corr8?.displayName)
    }

    /**
     * Esercita resolveEventCategoryVisuals (usato da NextWeekExpandableEventCard e AgendaEventCard in AgendaVisivaScreen)
     * per evitare false classificazioni su sottostringhe (es. "Aperitivo" per APE, "Corso" per Agenzia Orso),
     * preservando supporto a categorie dinamiche, match esatti e fallback legacy.
     */
    @Test
    fun testResolveEventCategoryVisuals() {
        val customCategory = PeritoCategoryDefinition(
            id = "sanatoria",
            displayName = "Sanatoria Edilizia",
            emoji = "📜",
            defaultPrefix = "Pratica Sanatoria",
            keywords = listOf("sanatoria", "condono"),
            isSystem = false
        )
        val categories = defaultCategoriesList() + customCategory

        // 1. Confini di parola: "Aperitivo" NON deve essere classificato come APE (⚡), bensì come Personale (☕)
        val (emojiApe, nameApe) = resolveEventCategoryVisuals(categoria = "", titolo = "Aperitivo con amici", categories = categories)
        assertEquals("☕", emojiApe)
        assertEquals("Personale", nameApe)

        // 2. Confini di parola: "Corso" NON deve essere classificato come Agenzia Orso (🐻)
        val (emojiOrso, nameOrso) = resolveEventCategoryVisuals(categoria = "", titolo = "Corso di formazione per periti", categories = categories)
        assertTrue("Non deve essere Agenzia Orso", nameOrso != "Agenzia Orso")
        assertTrue("Non deve avere emoji orso", emojiOrso != "🐻")

        // 3. Categoria dinamica personalizzata (per titolo o categoria esplicita)
        val (emojiDynTitle, nameDynTitle) = resolveEventCategoryVisuals(categoria = "", titolo = "Pratica Sanatoria - Via Roma", categories = categories)
        assertEquals("📜", emojiDynTitle)
        assertEquals("Sanatoria Edilizia", nameDynTitle)

        val (emojiDynCat, nameDynCat) = resolveEventCategoryVisuals(categoria = "Sanatoria Edilizia", titolo = "Sopralluogo generico", categories = categories)
        assertEquals("📜", emojiDynCat)
        assertEquals("Sanatoria Edilizia", nameDynCat)

        // 4. Match esatto su id o displayName di sistema
        val (emojiApeDirect, nameApeDirect) = resolveEventCategoryVisuals(categoria = "APE", titolo = "", categories = categories)
        assertEquals("⚡", emojiApeDirect)
        assertEquals("APE", nameApeDirect)

        val (emojiTrib, nameTrib) = resolveEventCategoryVisuals(categoria = "tribunale", titolo = "Incontro", categories = categories)
        assertEquals("⚖️", emojiTrib)
        assertEquals("Tribunale", nameTrib)

        // 5. Fallback legacy con lista categorie vuota
        val (emojiLegacy, nameLegacy) = resolveEventCategoryVisuals(categoria = "", titolo = "Udienza CTU Tribunale", categories = emptyList())
        assertEquals("⚖️", emojiLegacy)
        assertEquals("Tribunale", nameLegacy)

        val (emojiLegacyAperitivo, nameLegacyAperitivo) = resolveEventCategoryVisuals(categoria = "", titolo = "Aperitivo con amici", categories = emptyList())
        assertEquals("☕", emojiLegacyAperitivo)
        assertEquals("Personale", nameLegacyAperitivo)
    }

    @Test
    fun testNetworkSecurityConfigAllowsLocalMdns() {
        val file = File("src/main/res/xml/network_security_config.xml")
        val altFile = File("app/src/main/res/xml/network_security_config.xml")
        val target = if (file.exists()) file else altFile
        assertTrue("network_security_config.xml must exist", target.exists())
        val xml = target.readText()
        assertTrue("Must include local mDNS domain", xml.contains("<domain includeSubdomains=\"true\">local</domain>"))
    }
}
