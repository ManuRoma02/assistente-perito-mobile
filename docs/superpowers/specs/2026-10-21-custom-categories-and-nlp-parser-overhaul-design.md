# Design Spec: Custom Categories & NLP Appointment Parser Overhaul

## 1. Intent & Context
During real-world field testing of the PeritoAI mobile app by Davide Restelli (Perito Industriale Edile, "RD"), several key requirements and parser friction points were identified:
1. **Lack of an APE Category**: Energy performance inspections (*Attestato di Prestazione Energetica*, APE) are a core service for the surveyor. Dictating "sopralluogo APE" lacked a dedicated category.
2. **Agency Hijacking Category**: Mentioning partner agencies (e.g. *Unioncasa*, *Agenzia Orso*) prematurely classified appointments into generic "Incontro Unioncasa" instead of keeping the specific technical activity (e.g., APE, Rilievo).
3. **Explicit Fields Ignored**: Explicit voice/text dictations like `cliente: Raiola` or `note: Mercuri` were not captured, leading to fallback hallucinations like "Studio" instead of "Raiola".
4. **Time Range Truncation**: Phrases like `ore 9 fino alle 13.00` missed the end time because the regex only checked for `dalle... alle`.
5. **Location Redundancy**: Dictating `in comune a Grandate per un accesso agli atti` captured the entire purpose phrase as the location, resulting in repetitive titles like `Accesso Atti (Comune a Grandate Per Un Accesso Agli Atti)`.
6. **Hardcoded Categories**: The surveyor needs the ability to add, edit, and delete categories directly from the app, associating custom keywords for AI voice/text recognition.

---

## 2. Goals & Non-Goals

### Goals
- **Dynamic Category Management**: Allow the surveyor to add, edit (name, emoji, keywords, default title), and delete categories inside the app.
- **Built-in `APE` Category**: Preconfigure the APE category (⚡) with default title `Sopralluogo APE` and recognition keywords (`ape`, `a.p.e.`, `attestato di prestazione energetica`, `certificazione energetica`).
- **Activity-First Disambiguation**: Technical activities (APE, Rilievo, Accesso Atti, Tribunale) take strict precedence over agency names. An agency mentioned alongside a technical inspection becomes the `cliente` / `committente`, not the category.
- **Explicit Keyword Extraction**: Detect `cliente:\s*([^\n,.]+)` and `note:\s*([^\n]+)` natively.
- **Comprehensive Time Range Parsing**: Support `ore <H> fino alle <H2>` and `dalle <H> fino alle <H2>`.
- **Location Purpose Stripping**: Strip purpose prepositions (e.g. `per un accesso agli atti`, `per rilievo`) from extracted location strings.
- **Surveyor Technical Jargon in Prompt**: Teach the LLM prompt and regex heuristics about:
  - `RD`: Davide Restelli (surveyor personal assignment).
  - `STA`: Studio Tecnico Associato (studio office assignment).
  - `APE`: Attestato di Prestazione Energetica.
  - `CILA`, `SCIA`, `PdC`: Pratiche edilizie comunali.
  - `DOCFA`: Pratica catastale.

### Non-Goals
- Cloud synchronization for categories: All category definitions are stored strictly locally in `PeritoAiConfig` (DataStore / SharedPreferences) for offline privacy.
- Advanced regex authoring by users: Users only supply comma-separated plain keywords (e.g. `sanatoria, condono, abuso`).

---

## 3. Architecture & Data Model

### 3.1 Data Model (`Models.kt`)
Create a persistent data class for category definitions:

```kotlin
@Serializable
enum class CategoryKind {
    ACTIVITY,   // Technical activities (APE, Rilievo, Comune, Tribunale, Sanatoria, etc.) - Priority 1
    AGENCY,     // Partner agencies / brokers (Unioncasa, Agenzia Orso, etc.) - Priority 2
    PERSONAL    // Meetings, meals, personal, other - Priority 3
}

@Serializable
data class PeritoCategoryDefinition(
    val id: String,
    val displayName: String,
    val emoji: String,
    val defaultPrefix: String,
    val kind: CategoryKind = CategoryKind.ACTIVITY,
    val keywords: List<String> = emptyList(),
    val isSystem: Boolean = false,
    val colorHex: String? = null
)
```

The app preloads the standard system categories (with backwards compatibility with `PeritoEventCategory`):
- `sopralluogo`: 🔍 Sopralluogo (`sopralluogo`, `visita`, `collaudo`)
- `rilievo`: 📐 Rilievo (`rilievo`, `rilievi`, `misurazioni`, `misure`)
- `ape`: ⚡ APE (`ape`, `a.p.e.`, `prestazione energetica`, `certificazione energetica`, `attestato energetico`)
- `comune`: 🏛️ Comune (`comune`, `accesso atti`, `accesso agli atti`, `ufficio tecnico`, `edilizia privata`, `cila`, `scia`, `pdc`)
- `tribunale`: ⚖️ Tribunale (`tribunale`, `udienza`, `giuramento`, `ctu`, `giudice`, `cancelleria`)
- `riunione`: 🏢 Riunione (`riunione`, `studio`, `meeting`, `ufficio`)
- `unioncasa`: 🏠 Unioncasa (`unioncasa`, `union casa`)
- `agenzia_orso`: 🐻 Agenzia Orso (`agenzia orso`, `orso`)
- `cliente`: 👤 Cliente (`cliente`, `appuntamento`, `chiamata`, `firma`, `consegna`)
- `personale`: ☕ Personale (`colazione`, `pranzo`, `cena`, `caffè`, `aperitivo`)
- `altro`: 📅 Altro (`impegno`)

In `PeritoAiConfig`:
```kotlin
val categories: List<PeritoCategoryDefinition> = defaultCategoriesList()
```

### 3.2 Category Identity, Deletion Policy & App-Wide Integration
- **Immutable Category ID**: The `id` is generated once at creation (`UUID` or sanitized slug) and remains strictly immutable. Editing a category alters `displayName`, `emoji`, `defaultPrefix`, and `keywords`, preserving references in `categoryFormats` and calendar events.
- **Deletion Policy**: System categories (`isSystem = true`) cannot be deleted (to preserve app core functionality), but their formats and keywords can be modified. Custom categories (`isSystem = false`) can be deleted; deleting a category cleans up its format mapping.
- **App-Wide Scope**: `PeritoCategoryDefinition` serves as the single source of truth for categories across the entire app:
  - `EditEventDialog` uses the dynamic list for category chips.
  - `AgendaVisivaScreen` uses the dynamic list for filter chips and event badge resolution.
  - When parsing appointments, the NLP engine dynamically searches the active categories.
- **Surveyor Jargon (RD / STA)**: When detected, the acronym is preserved in the appointment's `note` field (e.g., `Incarico: RD` or `Studio: STA`), keeping the title clean (`Sopralluogo APE`) as requested by the user.

### 3.2 Dynamic NLP & Parser (`PeritoAiEngine.kt`)
1. **Dynamic Category Detection**:
   - `detectCategory(text: String, categories: List<PeritoCategoryDefinition>): PeritoCategoryDefinition`
   - Evaluation order:
     1. Search for technical activity keywords (APE, Rilievo, Comune, Tribunale, etc.).
     2. If no technical activity is matched, search agency keywords (Unioncasa, Agenzia Orso).
     3. Fall back to personal/meeting/altro.
2. **Explicit Extraction Priority**:
   - `extractAppointmentClient`: Check `Regex("""\b(?:cliente|con|committente)\s*:\s*([^,\.\n]+)""", RegexOption.IGNORE_CASE)` first before heuristic entity matching.
   - `extractExplicitNote`: Check `Regex("""\b(?:note|nota|appunto|promemoria|avviso)\s*:\s*(.+)$""", RegexOption.IGNORE_CASE)` and strip from raw text to prevent bleeding into title or location.
3. **Time Range Expansion**:
   - Update `rangeRegex` in `extractTargetTime`:
     ```kotlin
     \b(?:dalle|alle|da|ore)\s+(\d{1,2})(?:[:.;,](\d{2}))?\s+(?:fino\s+alle|fino\s+a|alle|\ba\b)\s+(\d{1,2})(?:[:.;,](\d{2}))?\b
     ```
4. **Location Cleaning**:
   - In `extractLocation`: Strip trailing purpose clauses:
     ```kotlin
     candidate.split(Regex("""\b(?:per\s+(?:un\s+)?(?:accesso|rilievo|sopralluogo|pratica|visione))\b""", RegexOption.IGNORE_CASE)).first()
     ```
   - For `in comune a <Città>`, format as `Comune di <Città>`.

---

## 4. User Interface (`CategoryReferenceSettingsScreen.kt`)

### 4.1 Category Management Controls
- **Top Action Bar**: Add `+ Nuova Categoria` pill button.
- **Category Card Actions**:
  - Each accordion card receives an Edit icon (pencil) and, for non-essential/user categories, a Delete icon (trash).
  - Tapping Edit opens `AddEditCategoryDialog` pre-filled with the category's properties.
- **Add/Edit Dialog**:
  - **Nome Categoria**: e.g., "APE" or "Sanatoria".
  - **Emoji Icon**: text field (default ⚡ or picker).
  - **Titolo Predefinito**: e.g., "Sopralluogo APE".
  - **Parole Chiave per IA**: comma-separated string (e.g. `ape, energetica, certificazione, 110`).
  - Validation: Name cannot be blank; ID generated from slugified name.
- **Format Toggle Binding**:
  - Newly added categories automatically receive an entry in `categoryFormats` so the user can immediately toggle `showTime`, `showType`, `showSubject`, `showPlace`, `showNotes`.

---

## 5. Verification & Automated Tests
Unit tests in `PeritoAiEngineTest.kt`:
1. `test parse APE with Unioncasa agency and Mercuri note`:
   - Input: `"venerdì alle 11.30 ho un sopralluogo APE. cliente: Unioncasa note: Mercuri"`
   - Assert: `categoria == "APE"`, `titolo == "Sopralluogo APE"`, `cliente == "Unioncasa"`, `note == "Mercuri"`, start `11:30`, end `12:30`.
2. `test parse Comune access with clean location`:
   - Input: `"lunedì ore 10 sono in comune a Grandate per un accesso agli atti"`
   - Assert: `categoria == "Comune"`, `luogo == "Comune di Grandate"`, `titolo == "Accesso Atti - Comune di Grandate"`, not repeating "per un accesso agli atti".
3. `test parse Rilievo with explicit end time and client`:
   - Input: `"rilievo lunedì 26 ore 9 fino alle 13.00 a Como. cliente: Raiola"`
   - Assert: `categoria == "Rilievo"`, `cliente == "Raiola"`, `luogo == "Como"`, start `09:00`, end `13:00`.
4. `test dynamic custom category addition and recognition`:
   - Create custom category `Sanatoria` with keywords `["sanatoria", "condono"]`.
   - Parse `"martedì alle 15 sanatoria in via Roma"`.
   - Assert: `categoria == "Sanatoria"`.
