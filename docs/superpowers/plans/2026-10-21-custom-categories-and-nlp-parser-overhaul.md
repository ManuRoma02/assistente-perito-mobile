# Custom Categories & NLP Parser Overhaul Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Overhaul the NLP appointment parsing engine to resolve real-world edge cases (APE category, agency disambiguation, explicit `cliente:` and `note:` extraction, time range parsing, location cleaning) and allow the surveyor to dynamically add, edit, and delete categories with recognition keywords directly in the app.

**Architecture:** 
- Promote categories from a hardcoded Kotlin enum to a persistent, dynamic data model `PeritoCategoryDefinition` stored in `PeritoAiConfig` (DataStore), preloaded with system defaults including the new `APE` category.
- Overhaul `PeritoAiEngine` heuristics and LLM prompt to match dynamic categories by keywords, give technical activities strict precedence over partner agencies, extract explicit `cliente:` and `note:` fields, parse multi-hour ranges (`ore 9 fino alle 13.00`), and clean location strings.
- Expose category management in `CategoryReferenceSettingsScreen` via `+ Nuova Categoria` dialog and edit/delete actions, cascading dynamic categories into `AgendaVisivaScreen` and event edit dialogs.

**Tech Stack:** Kotlin, Jetpack Compose, Android DataStore / Serialization, Coroutines, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-21-custom-categories-and-nlp-parser-overhaul-design.md`

## Global Constraints
- `latestVersionCode = 15`, `latestVersionName = "1.0.14"` in `app/build.gradle.kts` and `ota-update.json`.
- Release notes in `ota-update.json` must be strictly concise (1 line per point) with bold keywords (`**Parola Chiave**: ...`).
- Zero APK transfers via Telegram (strictly in-app OTA / GitHub release).
- Preserve existing system category IDs (`sopralluogo`, `rilievo`, `ape`, `comune`, `tribunale`, `riunione`, `unioncasa`, `agenzia_orso`, `cliente`, `personale`, `altro`).
- Category `id` must be immutable once created.

## Review Focus
1. **Activity vs Agency Conflict**: Input `"venerdì alle 11.30 ho un sopralluogo APE. cliente: Unioncasa note: Mercuri"` must produce `categoria = "APE"`, `cliente = "Unioncasa"`, `note = "Mercuri"`, and `titolo = "Sopralluogo APE"`, NOT `"Incontro Unioncasa"`.
2. **Explicit Client Extraction**: Input `"rilievo lunedì 26 ore 9 fino alle 13.00 a Como. cliente: Raiola"` must extract `cliente = "Raiola"` and `luogo = "Como"`, NOT falling back to `"Studio"` or `"Da definire"`.
3. **Multi-Hour Time Ranges**: Input `"ore 9 fino alle 13.00"` must set `data_fine_iso` to `13:00`, not defaulting to 1 hour (`10:00`).
4. **Location Redundancy**: Input `"in comune a Grandate per un accesso agli atti"` must extract `luogo = "Comune di Grandate"` and `titolo = "Accesso Atti - Comune di Grandate"`, without duplicating `"per un accesso agli atti"`.
5. **Dynamic Category Lifecycle**: Creating a custom category `"Sanatoria"` with keywords `["sanatoria", "condono"]` must immediately be recognized by the parser on `"martedì alle 10 sanatoria in via Milano"` and appear in the agenda filter.

---

### Task 1: Persistent Data Model & Default Categories (`PeritoCategoryDefinition`)

**Files:**
- Modify: `app/src/main/java/com/example/assistenteperitomobile/domain/Models.kt`
- Create: `app/src/test/java/com/example/assistenteperitomobile/PeritoCategoryDefinitionTest.kt`

**Interfaces:**
- Produces: `data class PeritoCategoryDefinition(val id: String, val displayName: String, val emoji: String, val defaultPrefix: String, val keywords: List<String> = emptyList(), val isSystem: Boolean = false, val colorHex: String? = null)`
- Produces: `fun defaultCategoriesList(): List<PeritoCategoryDefinition>`
- Modifies: `PeritoAiConfig` adding `val categories: List<PeritoCategoryDefinition> = defaultCategoriesList()`

- [ ] **Step 1: Write the failing test**
Create `app/src/test/java/com/example/assistenteperitomobile/PeritoCategoryDefinitionTest.kt`:
```kotlin
package com.example.assistenteperitomobile

import com.example.assistenteperitomobile.domain.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class PeritoCategoryDefinitionTest {
    @Test
    fun testDefaultCategoriesListContainsApeAndSystemCategories() {
        val defaults = defaultCategoriesList()
        val ape = defaults.firstOrNull { it.id == "ape" }
        assertNotNull("APE category must exist in defaults", ape)
        assertEquals("APE", ape!!.displayName)
        assertEquals("⚡", ape.emoji)
        assertTrue(ape.isSystem)
        assertTrue(ape.keywords.contains("ape"))
        assertTrue(ape.keywords.contains("prestazione energetica"))
    }

    @Test
    fun testSerializationAndConfigCompatibility() {
        val json = Json { ignoreUnknownKeys = true }
        val config = PeritoAiConfig()
        val encoded = json.encodeToString(PeritoAiConfig.serializer(), config)
        val decoded = json.decodeFromString(PeritoAiConfig.serializer(), encoded)
        assertTrue(decoded.categories.any { it.id == "ape" })
        assertTrue(decoded.categoryFormats.containsKey("ape"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew testDebugUnitTest --tests com.example.assistenteperitomobile.PeritoCategoryDefinitionTest`
Expected: FAIL (unresolved reference `PeritoCategoryDefinition`, `defaultCategoriesList`)

- [ ] **Step 3: Implement `PeritoCategoryDefinition` and `defaultCategoriesList` in `Models.kt`**
Add `PeritoCategoryDefinition` to `Models.kt`. Preload 11 categories:
- `sopralluogo` (🔍 Sopralluogo, defaultPrefix: "Sopralluogo", keywords: ["sopralluogo", "visita", "collaudo"])
- `rilievo` (📐 Rilievo, defaultPrefix: "Rilievo Tecnico", keywords: ["rilievo", "rilievi", "misurazioni", "misure"])
- `ape` (⚡ APE, defaultPrefix: "Sopralluogo APE", keywords: ["ape", "a.p.e.", "prestazione energetica", "certificazione energetica", "attestato energetico"])
- `comune` (🏛️ Comune, defaultPrefix: "Incontro Comune", keywords: ["comune", "accesso atti", "accesso agli atti", "ufficio tecnico", "edilizia privata", "cila", "scia", "pdc"])
- `tribunale` (⚖️ Tribunale, defaultPrefix: "Udienza Tribunale", keywords: ["tribunale", "udienza", "giuramento", "ctu", "giudice", "cancelleria"])
- `riunione` (🏢 Riunione, defaultPrefix: "Riunione Studio", keywords: ["riunione", "studio", "meeting", "ufficio"])
- `unioncasa` (🏠 Unioncasa, defaultPrefix: "Incontro Unioncasa", keywords: ["unioncasa", "union casa"])
- `agenzia_orso` (🐻 Agenzia Orso, defaultPrefix: "Appuntamento Agenzia Orso", keywords: ["agenzia orso", "orso"])
- `cliente` (👤 Cliente, defaultPrefix: "Appuntamento", keywords: ["cliente", "appuntamento", "chiamata", "firma", "consegna"])
- `personale` (☕ Personale, defaultPrefix: "Personale", keywords: ["colazione", "pranzo", "cena", "caffè", "caffe", "aperitivo"])
- `altro` (📅 Altro, defaultPrefix: "Impegno", keywords: ["impegno"])

Update `defaultCategoryFormats()` to include `"ape"`:
```kotlin
"ape" to CategoryReferenceFormat(
    categoryId = "ape",
    showTime = true,
    showType = true,
    showSubject = false,
    showPlace = true,
    showNotes = false
)
```
Add `val categories: List<PeritoCategoryDefinition> = defaultCategoriesList()` to `PeritoAiConfig`.

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew testDebugUnitTest --tests com.example.assistenteperitomobile.PeritoCategoryDefinitionTest`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/assistenteperitomobile/domain/Models.kt app/src/test/java/com/example/assistenteperitomobile/PeritoCategoryDefinitionTest.kt
git commit -m "feat(domain): add PeritoCategoryDefinition with dynamic category support and APE preset"
```

---

### Task 2: NLP Parser Overhaul in `PeritoAiEngine`

**Files:**
- Modify: `app/src/main/java/com/example/assistenteperitomobile/domain/PeritoAiEngine.kt`
- Modify: `app/src/test/java/com/example/assistenteperitomobile/PeritoAiEngineTest.kt`

**Interfaces:**
- Consumes: `PeritoCategoryDefinition` and `defaultCategoriesList()` from Task 1
- Modifies: `PeritoAiEngine.parseAppointment(text: String, refNow: LocalDateTime = LocalDateTime.now(), categories: List<PeritoCategoryDefinition> = defaultCategoriesList()): AppointmentParseResult`
- Modifies: `extractAppointmentClient`, `extractExplicitNote`, `extractTargetTime`, `extractLocation`

- [ ] **Step 1: Write the failing tests for Deiv's real-world cases**
Add to `PeritoAiEngineTest.kt`:
```kotlin
@Test
fun testParseApeWithAgencyAndExplicitNote() = runTest {
    val input = "venerdì alle 11.30 ho un sopralluogo APE. cliente: Unioncasa note: Mercuri"
    val res = engine.parseAppointment(input, refNow)
    assertTrue(res.has_appointment)
    assertEquals("APE", res.categoria)
    assertEquals("Sopralluogo APE", res.titolo)
    assertEquals("Unioncasa", res.cliente)
    assertEquals("Mercuri", res.note)
    assertEquals("2026-10-16T11:30:00", res.data_inizio_iso)
    assertEquals("2026-10-16T12:30:00", res.data_fine_iso)
}

@Test
fun testParseComuneAccessoAttiCleanLocation() = runTest {
    val input = "lunedì ore 10 sono in comune a Grandate per un accesso agli atti"
    val res = engine.parseAppointment(input, refNow)
    assertTrue(res.has_appointment)
    assertEquals("Comune", res.categoria)
    assertEquals("Comune di Grandate", res.luogo)
    assertEquals("Accesso Atti - Comune di Grandate", res.titolo)
    assertFalse(res.titolo.contains("per un accesso agli atti", ignoreCase = true))
}

@Test
fun testParseRilievoWithMultiHourRangeAndExplicitClient() = runTest {
    val input = "rilievo lunedì 26 ore 9 fino alle 13.00 a Como. cliente: Raiola"
    val res = engine.parseAppointment(input, refNow)
    assertTrue(res.has_appointment)
    assertEquals("Rilievo", res.categoria)
    assertEquals("Raiola", res.cliente)
    assertEquals("Como", res.luogo)
    assertEquals("2026-10-26T09:00:00", res.data_inizio_iso)
    assertEquals("2026-10-26T13:00:00", res.data_fine_iso)
    assertEquals("Rilievo Tecnico - Raiola", res.titolo)
}

@Test
fun testParseDynamicCustomCategory() = runTest {
    val customList = defaultCategoriesList() + PeritoCategoryDefinition(
        id = "sanatoria",
        displayName = "Sanatoria",
        emoji = "📋",
        defaultPrefix = "Pratica Sanatoria",
        keywords = listOf("sanatoria", "condono", "abuso")
    )
    val input = "martedì alle 15 sanatoria in via Roma con Geom. Neri"
    val res = engine.parseAppointment(input, refNow, categories = customList)
    assertEquals("Sanatoria", res.categoria)
    assertTrue(res.titolo.startsWith("Pratica Sanatoria"))
}
```

- [ ] **Step 2: Run tests to verify they fail**
Run: `./gradlew testDebugUnitTest --tests com.example.assistenteperitomobile.PeritoAiEngineTest`
Expected: FAIL on the 4 new tests.

- [ ] **Step 3: Implement NLP enhancements in `PeritoAiEngine.kt`**
1. **Dynamic Category Detection**:
   - Signature: `detectCategory(text: String, categories: List<PeritoCategoryDefinition>): PeritoCategoryDefinition`
   - Matching priority:
     - First, match specific technical activities (APE, Rilievo, Comune, Tribunale, custom categories not matching agency names).
     - Second, match partner agencies (Unioncasa, Agenzia Orso).
     - Third, personal/riunione/altro fallback.
2. **Explicit Client Extraction (`extractAppointmentClient`)**:
   - Check `Regex("""\b(?:cliente|con|committente)\s*:\s*([^,\.\n]+)""", RegexOption.IGNORE_CASE)` at the very beginning of `extractAppointmentClient`.
   - Strip prefixes and forbidden keywords, return formatted titlecase.
3. **Explicit Note Extraction (`extractExplicitNote`)**:
   - Check `Regex("""\b(?:note|nota|appunto|promemoria|avviso)\s*:\s*(.+)$""", RegexOption.IGNORE_CASE)`.
   - In `parseAppointment`, remove the explicit `note:...` clause from the text before location and client extraction so it doesn't pollute location.
4. **Time Range Expansion (`extractTargetTime`)**:
   - Update `rangeRegex`:
     ```kotlin
     val rangeRegex = Regex(
         """\b(?:dalle|alle|da|ore)\s+(\d{1,2})(?:[:.;,](\d{2}))?\s+(?:fino\s+alle|fino\s+a|alle|\ba\b)\s+(\d{1,2})(?:[:.;,](\d{2}))?\b"""
     )
     ```
5. **Location Cleaning & Purpose Stripping (`extractLocation`)**:
   - Clean location matches by trimming purpose suffixes:
     ```kotlin
     val cleanCandidate = rawCandidate.split(Regex("""\b(?:per\s+(?:un\s+)?(?:accesso|rilievo|sopralluogo|pratica|visione|atti))\b""", RegexOption.IGNORE_CASE)).first().trim()
     ```
   - Match `in comune a <Città>` -> format as `Comune di <Città>`.
6. **Title Generation for APE**:
   - If category is `APE`, format title as `"Sopralluogo APE"`.
7. **Prompt Enrichment**:
   - Add surveyor technical abbreviations to the LLM system prompt:
     `RD = Mario Rossi (assegnato a lui)`
     `STA = Studio Tecnico Associato`
     `APE = Attestato di Prestazione Energetica (Sopralluogo APE)`
     `CILA / SCIA / PdC = Pratiche Edilizie`
     `DOCFA = Pratica Catastale`

- [ ] **Step 4: Run tests to verify they pass**
Run: `./gradlew testDebugUnitTest --tests com.example.assistenteperitomobile.PeritoAiEngineTest`
Expected: PASS (all existing + new tests passing).

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/assistenteperitomobile/domain/PeritoAiEngine.kt app/src/test/java/com/example/assistenteperitomobile/PeritoAiEngineTest.kt
git commit -m "fix(nlp): overhaul appointment parsing with APE category, agency precedence, explicit fields, and multi-hour ranges"
```

---

### Task 3: ViewModel & CalendarRepository Dynamic Integration

**Files:**
- Modify: `app/src/main/java/com/example/assistenteperitomobile/ui/PeritoViewModel.kt`
- Modify: `app/src/main/java/com/example/assistenteperitomobile/data/CalendarRepository.kt`
- Modify: `app/src/test/java/com/example/assistenteperitomobile/PeritoViewModelTest.kt` (or unit test)

**Interfaces:**
- Consumes: `PeritoCategoryDefinition`, `PeritoAiConfig.categories`
- Produces: `PeritoViewModel.addCustomCategory(name: String, emoji: String, defaultTitle: String, keywords: List<String>)`
- Produces: `PeritoViewModel.updateCustomCategory(id: String, name: String, emoji: String, defaultTitle: String, keywords: List<String>)`
- Produces: `PeritoViewModel.deleteCustomCategory(id: String)`

- [ ] **Step 1: Write unit test for ViewModel category CRUD operations**
Add test in `app/src/test/java/com/example/assistenteperitomobile/PeritoViewModelCategoryTest.kt`:
```kotlin
package com.example.assistenteperitomobile

import com.example.assistenteperitomobile.domain.*
import org.junit.Assert.*
import org.junit.Test

class PeritoViewModelCategoryTest {
    @Test
    fun testAddEditDeleteCustomCategory() {
        val initialConfig = PeritoAiConfig()
        val catId = "cat_test_sanatoria"
        val newCat = PeritoCategoryDefinition(
            id = catId,
            displayName = "Sanatoria",
            emoji = "📜",
            defaultPrefix = "Pratica Sanatoria",
            keywords = listOf("sanatoria", "condono")
        )
        val withAdded = initialConfig.copy(
            categories = initialConfig.categories + newCat,
            categoryFormats = initialConfig.categoryFormats + (catId to CategoryReferenceFormat(catId))
        )
        assertTrue(withAdded.categories.any { it.id == catId })
        assertTrue(withAdded.categoryFormats.containsKey(catId))

        val withDeleted = withAdded.copy(
            categories = withAdded.categories.filter { it.id != catId },
            categoryFormats = withAdded.categoryFormats - catId
        )
        assertFalse(withDeleted.categories.any { it.id == catId })
        assertFalse(withDeleted.categoryFormats.containsKey(catId))
    }
}
```

- [ ] **Step 2: Run test to verify it passes**
Run: `./gradlew testDebugUnitTest --tests com.example.assistenteperitomobile.PeritoViewModelCategoryTest`
Expected: PASS

- [ ] **Step 3: Implement Category Management in `PeritoViewModel.kt` & `CalendarRepository.kt`**
1. In `PeritoViewModel`:
   - `addCustomCategory(name: String, emoji: String, defaultTitle: String, keywords: List<String>)`:
     Generate id: `slugify(name) + "_" + UUID.randomUUID().toString().take(6)`.
     Add to `aiConfig.categories`.
     Initialize `categoryFormats[id] = CategoryReferenceFormat(id)`.
     Persist config via `aiConfigRepo.saveConfig(updated)`.
   - `updateCustomCategory(id: String, name: String, emoji: String, defaultTitle: String, keywords: List<String>)`:
     Find category by `id`, update properties, preserve `id`, persist config.
   - `deleteCustomCategory(id: String)`:
     If `!isSystem`, remove from `categories` and `categoryFormats`, persist config.
   - In `processUserMessage` / `parseAppointment`: Pass `_uiState.value.aiConfig.categories` to `engine.parseAppointment(...)`.
2. In `CalendarRepository.kt`:
   - In `createEventFromProposed`: resolve activity type and format title using `aiConfig.categoryFormats` dynamically rather than hardcoded enum branches.

- [ ] **Step 4: Run unit tests**
Run: `./gradlew testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/assistenteperitomobile/ui/PeritoViewModel.kt app/src/main/java/com/example/assistenteperitomobile/data/CalendarRepository.kt app/src/test/java/com/example/assistenteperitomobile/PeritoViewModelCategoryTest.kt
git commit -m "feat(viewmodel): wire dynamic category CRUD operations and calendar repository formatting"
```

---

### Task 4: Interactive Category Management UI in `CategoryReferenceSettingsScreen` & Agenda

**Files:**
- Modify: `app/src/main/java/com/example/assistenteperitomobile/ui/CategoryReferenceSettingsScreen.kt`
- Modify: `app/src/main/java/com/example/assistenteperitomobile/ui/AgendaVisivaScreen.kt`

**Interfaces:**
- Consumes: `PeritoViewModel.addCustomCategory`, `updateCustomCategory`, `deleteCustomCategory`
- Consumes: `uiState.aiConfig.categories`, `uiState.aiConfig.categoryFormats`

- [ ] **Step 1: Implement `AddEditCategoryDialog` in `CategoryReferenceSettingsScreen.kt`**
Create Composable `AddEditCategoryDialog`:
- Fields:
  - `Nome Categoria`: OutlinedTextField (e.g. "APE", "Sanatoria", "DOCFA")
  - `Emoji`: OutlinedTextField (default "⚡" or picker)
  - `Titolo Predefinito`: OutlinedTextField (e.g. "Sopralluogo APE")
  - `Parole Chiave per IA`: OutlinedTextField with hint "Separate da virgola (es. ape, energetica, 110)"
- Buttons: "Annulla" and "Salva Categoria" (DavezBordeaux, disabled if name is blank).

- [ ] **Step 2: Add Top "+ Nuova Categoria" Button and Action Icons on Cards**
In `CategoryReferenceSettingsScreen.kt`:
- Above the category cards list, add a prominent, elegant Davez Luxury pill button:
  `[ + Aggiungi Categoria ]`
- In each category card header:
  - Add an Edit button (Pencil icon) to open `AddEditCategoryDialog` pre-filled.
  - If `!category.isSystem`, add a Delete button (Trash icon) with a confirmation dialog:
    `"Eliminare la categoria '${category.displayName}'?"`

- [ ] **Step 3: Update `AgendaVisivaScreen.kt` & `EditEventDialog` to Use Dynamic Categories**
- In `AgendaVisivaScreen.kt`:
  - Category filter chips row: Iterate over `uiState.aiConfig.categories` instead of `PeritoEventCategory.entries`.
  - In `EditEventDialog`: Display category selector chips from `uiState.aiConfig.categories`.
  - In event cards: Resolve emoji and display name from dynamic categories, falling back safely if not found.

- [ ] **Step 4: Run unit tests and verify build**
Run: `./gradlew testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/assistenteperitomobile/ui/CategoryReferenceSettingsScreen.kt app/src/main/java/com/example/assistenteperitomobile/ui/AgendaVisivaScreen.kt
git commit -m "feat(ui): add interactive category creation, editing, and deletion in settings and agenda"
```

---

### Task 5: Version Bump, Build & In-App Release Verification

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `ota-update.json`

**Interfaces:**
- Produces: `v1.0.14` (Build 15)
- Produces: `app-release.apk`
- GitHub Release: `v1.0.14`

- [ ] **Step 1: Bump version in `app/build.gradle.kts`**
Set `versionCode = 15` and `versionName = "1.0.14"`.

- [ ] **Step 2: Update `ota-update.json` with concise release notes**
Following the rule (1 line per point, bold keywords):
```json
{
  "latestVersionCode": 15,
  "latestVersionName": "1.0.14",
  "downloadUrl": "https://github.com/ManuRoma02/assistente-perito-mobile/releases/download/v1.0.14/app-release.apk",
  "releaseNotes": "• **Nuova categoria APE**: riconoscimento automatico di sopralluoghi energetici e titolo dedicato\n• **Gestione categorie in-app**: crea, modifica ed elimina categorie e parole chiave direttamente dalle impostazioni\n• **Risoluzione conflitti agenzie**: le agenzie (Unioncasa, Orso) vanno nel cliente senza alterare la categoria del sopralluogo\n• **Campi espliciti e orari**: supporto per 'cliente: nome', 'note: testo' e fasce orarie estese ('ore 9 fino alle 13')",
  "mandatory": false,
  "publishedAt": "2026-10-21T18:45:00Z"
}
```

- [ ] **Step 3: Run unit tests**
Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL (all tests passing)

- [ ] **Step 4: Build release APK**
Run: `./gradlew assembleRelease`
Expected: BUILD SUCCESSFUL (`app/build/outputs/apk/release/app-release.apk` generated)

- [ ] **Step 5: Push commit and publish GitHub Release v1.0.14**
Run:
```bash
git add app/build.gradle.kts ota-update.json
git commit -m "release: v1.0.14 (15) - dynamic categories, APE preset and parser overhaul"
git push origin main
gh release create v1.0.14 "app/build/outputs/apk/release/app-release.apk" --title "v1.0.14 - Categorie Personalizzabili, Preset APE & NLP Parser Overhaul" --notes "• **Nuova categoria APE**: riconoscimento automatico di sopralluoghi energetici e titolo dedicato
• **Gestione categorie in-app**: crea, modifica ed elimina categorie e parole chiave direttamente dalle impostazioni
• **Risoluzione conflitti agenzie**: le agenzie (Unioncasa, Orso) vanno nel cliente senza alterare la categoria del sopralluogo
• **Campi espliciti e orari**: supporto per 'cliente: nome', 'note: testo' e fasce orarie estese ('ore 9 fino alle 13')"
```

- [ ] **Step 6: Live Device Verification on `emulator-5554`**
- Install debug APK on `emulator-5554` via `./gradlew installDebug`.
- Open app, test parsing of `"venerdì alle 11.30 ho un sopralluogo APE. cliente: Unioncasa note: Mercuri"`.
- Open Settings -> Formato Riferimento Categorie.
- Tap "+ Aggiungi Categoria", create "Sanatoria" with emoji 📜 and keywords "sanatoria, condono".
- Verify "Sanatoria" appears in the list and can be edited.
- Capture screen dump or screenshot to verify flawless rendering.
