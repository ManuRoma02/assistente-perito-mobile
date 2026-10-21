# Personalizzazione Riferimento Appuntamenti per Categoria Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Permettere al perito di personalizzare, per ciascuna categoria di appuntamento, quali informazioni (Orario, Tipo, Cliente/Agenzia, Luogo, Note) visualizzare nel riferimento sintetico dell'appuntamento, tramite una schermata dedicata nelle Impostazioni con schede espandibili ad accordion, preset rapidi e anteprima live in stile Davez / Apple HIG.

**Architecture:** Aggiungere un modello dati immutabile e serializzabile `CategoryReferenceFormat` all'interno di `AiEngineConfig`, con default intelligenti per ciascuna delle 10 categorie di `PeritoEventCategory`. Creare il composable dedicato `CategoryReferenceSettingsContent` in `app/src/main/java/com/example/assistenteperitomobile/ui/CategoryReferenceSettingsScreen.kt` navigabile dal foglio impostazioni tramite `AnimatedContent`. Integrare il formattatore dinamico in `PeritoViewModel`, `AgendaVisivaScreen` e `CalendarRepository`.

**Tech Stack:** Kotlin 2.x, Jetpack Compose (Material 3), kotlinx.serialization, StateFlow, Coroutines, Davez Architectural Palette (`#4F0B18`, Alabaster, Travertine), Apple HIG design patterns.

**Spec:** `docs/superpowers/specs/2026-10-21-category-reference-settings-design.md`

## Global Constraints

- Android API 26+ compatibility (minSdk 26, targetSdk 35).
- Offline-first: tutte le preferenze di formattazione salvate localmente in `AiEngineConfig` senza dipendenze cloud.
- Stile Davez Luxury Architectural: colore primario `#4F0B18`, sfondi card `#FAF7F2` e bordi sottili `0.5.dp`.
- Apple HIG: touch target minimi di 48dp, chiarezza tipografica, transizioni animate senza layout shift.
- Preservazione dei dettagli operativi: espandendo la card appuntamento in Agenda, tutti i tasti (Google Maps, Note, Cliente) rimangono intatti e funzionanti.

## Review Focus

1. **Tutti i toggle disattivati per una categoria**: se l'utente deseleziona Orario, Tipo, Soggetto, Luogo e Note, il sistema deve ricadere su un fallback sicuro (il nome della categoria o "Impegno") invece di generare un titolo vuoto.
2. **Eventi importati da Google Calendar senza campi separati**: non sovrascrivere o svuotare il titolo custom di eventi esterni se i campi `cliente`/`note` sono vuoti nel calendario di sistema.
3. **Formattazione con caratteri speciali o indirizzi lunghi**: gestione pulita delle parentesi `(Luogo • Note)` senza doppi separatori o stringhe `null`/`Da definire`.
4. **Navigazione avanti e indietro nelle Impostazioni**: il ritorno da `CategoryReferenceSettingsContent` alle impostazioni principali deve avvenire fluidamente tramite `AnimatedContent` senza perdere lo stato bozza di altri campi nelle impostazioni.
5. **Reattività istantanea dell'Agenda**: ogni modifica a un formato di categoria deve aggiornare immediatamente la vista dell'Agenda e dell'anteprima senza richiedere riavvii o ricaricamenti manuali.

---

### Task 1: Modello Dati e Test Unitari di Formattazione (`CategoryReferenceFormat`)

**Files:**
- Modify: `app/src/main/java/com/example/assistenteperitomobile/domain/Models.kt:155-254`
- Create: `app/src/test/java/com/example/assistenteperitomobile/CategoryReferenceFormatTest.kt`

- [ ] **Step 1: Scrivere i test unitari per `CategoryReferenceFormat` in `CategoryReferenceFormatTest.kt`**
  - Test per combinazione completa (Tutti i campi attivi: Orario + Tipo + Soggetto + Luogo + Note).
  - Test per configurazione minimale (Solo Tipo + Soggetto).
  - Test per configurazione specifica (Solo Soggetto / Agenzia, es. Unioncasa).
  - Test per fallback sicuro con tutti i campi disattivati.
  - Test per esclusione automatica di "Cliente" generico e "Da definire".

- [ ] **Step 2: Eseguire i test per verificare il fallimento**
  - Comando: `./gradlew testDebugUnitTest --tests com.example.assistenteperitomobile.CategoryReferenceFormatTest`
  - Risultato atteso: Fail (simboli non ancora definiti).

- [ ] **Step 3: Implementare `CategoryReferenceFormat` e `defaultCategoryFormats()` in `Models.kt`**
  - Creare la data class `@Serializable @Immutable data class CategoryReferenceFormat(...)`.
  - Implementare `fun formatTitle(...)` con formattazione robusta: base `[Orario] - [Tipo] - [Soggetto]` e dettagli opzionali `(Luogo • Note)`.
  - Creare la mappa predefinita `defaultCategoryFormats()` per le 10 categorie di `PeritoEventCategory`.
  - Aggiungere `categoryFormats: Map<String, CategoryReferenceFormat> = defaultCategoryFormats()` a `AiEngineConfig`.

- [ ] **Step 4: Eseguire i test unitari e verificare che passino**
  - Comando: `./gradlew testDebugUnitTest --tests com.example.assistenteperitomobile.CategoryReferenceFormatTest`
  - Risultato atteso: BUILD SUCCESSFUL (tutti i test passano).

- [ ] **Step 5: Commit del Task 1**
  - Comando: `git add app/src/main/java/com/example/assistenteperitomobile/domain/Models.kt app/src/test/java/com/example/assistenteperitomobile/CategoryReferenceFormatTest.kt && git commit -m "feat: add CategoryReferenceFormat data model with per-category presets and unit tests"`

---

### Task 2: Logica ViewModel e Integrazione Generazione Titoli

**Files:**
- Modify: `app/src/main/java/com/example/assistenteperitomobile/ui/PeritoViewModel.kt`
- Modify: `app/src/main/java/com/example/assistenteperitomobile/data/CalendarRepository.kt`
- Modify: `app/src/main/java/com/example/assistenteperitomobile/domain/PeritoAiEngine.kt`

- [ ] **Step 1: Aggiungere metodi di gestione formato in `PeritoViewModel.kt`**
  - Aggiungere funzione `updateCategoryReferenceFormat(categoryId: String, format: CategoryReferenceFormat)`.
  - Aggiungere funzione `resetCategoryReferenceFormats()`.
  - Aggiungere funzione helper `formatEventReference(event: CalendarEventItem): String` che recupera la categoria dell'evento, estrae la configurazione da `aiConfig.value.categoryFormats` e calcola il titolo dinamico.

- [ ] **Step 2: Integrare `CategoryReferenceFormat` nella creazione eventi in `CalendarRepository.kt` e `PeritoAiEngine.kt`**
  - In `CalendarRepository.createEvent`, utilizzare il formato configurato per la categoria dell'evento (se disponibile nella config) per calcolare l'effective title da salvare su SQLite/Google Calendar.
  - In `PeritoAiEngine.kt`, passare la configurazione attiva durante l'estrazione appuntamento e la generazione della conferma.

- [ ] **Step 3: Eseguire i test unitari complessivi**
  - Comando: `./gradlew testDebugUnitTest`
  - Risultato atteso: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit del Task 2**
  - Comando: `git add app/src/main/java/com/example/assistenteperitomobile/ui/PeritoViewModel.kt app/src/main/java/com/example/assistenteperitomobile/data/CalendarRepository.kt app/src/main/java/com/example/assistenteperitomobile/domain/PeritoAiEngine.kt && git commit -m "feat: integrate category reference formatting in ViewModel and CalendarRepository"`

---

### Task 3: Schermata Composable `CategoryReferenceSettingsScreen.kt`

**Files:**
- Create: `app/src/main/java/com/example/assistenteperitomobile/ui/CategoryReferenceSettingsScreen.kt`

- [ ] **Step 1: Implementare la schermata completa con architettura Davez / Apple HIG**
  - Header con pulsante Indietro (`← Impostazioni`), titolo "Formato Riferimento" e sottotitolo.
  - Card Accordion per ciascuna delle 10 categorie di `PeritoEventCategory`:
    - Stato espanso/compresso con animazione `animateContentSize()` e rotazione chevron via `graphicsLayer`.
    - Badge sintetico dei campi attivi nella barra compressa.
    - 5 Chip interattivi a selezione rapida (🕒 Orario, 🏷️ Tipo, 👤 Soggetto/Agenzia, 📍 Luogo, 📝 Note).
    - Riga pulsanti di preset (*Tutti*, *Standard*, *Minimale*, *Solo Soggetto*).
    - Box di Anteprima Live istantanea che mostra esattamente la card in formato Davez con titolo aggiornato in tempo reale.
    - Pulsante di ripristino default in basso.

- [ ] **Step 2: Verifica di compilazione**
  - Comando: `./gradlew compileDebugKotlin`
  - Risultato atteso: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit del Task 3**
  - Comando: `git add app/src/main/java/com/example/assistenteperitomobile/ui/CategoryReferenceSettingsScreen.kt && git commit -m "feat: implement CategoryReferenceSettingsScreen with interactive accordion cards and live preview"`

---

### Task 4: Navigazione nelle Impostazioni e Rendering Reattivo nell'Agenda

**Files:**
- Modify: `app/src/main/java/com/example/assistenteperitomobile/ui/PeritoAppScreens.kt`
- Modify: `app/src/main/java/com/example/assistenteperitomobile/ui/AgendaVisivaScreen.kt`

- [ ] **Step 1: Aggiungere transizione animata in `AiEngineSettingsSheetContent` in `PeritoAppScreens.kt`**
  - Gestire stato `var currentSettingsView by remember { mutableStateOf(SettingsSubScreen.MAIN) }`.
  - Aggiungere la riga di navigazione "🏷️ Formato Riferimento Categorie" con freccia `>` e descrizione sintetica.
  - Inserire `AnimatedContent` con transizione `slideInHorizontally` / `slideOutHorizontally` per passare fluidamente tra `MainSettings` e `CategoryReferenceSettingsContent`.
  - Gestire il `BackHandler` Android per tornare alle impostazioni principali se si preme il tasto indietro del sistema operativo.

- [ ] **Step 2: Aggiornare `AgendaVisivaScreen.kt` per applicare il riferimento dinamico**
  - In `AgendaEventCard` e `NextWeekExpandableEventCard`, visualizzare il riferimento calcolato tramite `viewModel.formatEventReference(event)` come headline del titolo.
  - Mantenere nel corpo espanso tutti i comandi operativi (pulsante Google Maps, note estese, cliente, fascicolo).

- [ ] **Step 3: Compilare e verificare tutti i test unitari**
  - Comando: `./gradlew testDebugUnitTest && ./gradlew assembleDebug`
  - Risultato atteso: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit del Task 4**
  - Comando: `git add app/src/main/java/com/example/assistenteperitomobile/ui/PeritoAppScreens.kt app/src/main/java/com/example/assistenteperitomobile/ui/AgendaVisivaScreen.kt && git commit -m "feat: connect category reference settings navigation and dynamic agenda card rendering"`

---

### Task 5: Verifica Live End-to-End su Emulatore Android con ARTEMIS

**Files:**
- Android Emulator (`emulator-5554`)

- [ ] **Step 1: Installare l'APK aggiornato sull'emulatore**
  - Comando: `./gradlew installDebug`

- [ ] **Step 2: Verificare l'apertura delle Impostazioni e la navigazione nella nuova schermata**
  - Avviare l'app, aprire le Impostazioni (icona ingranaggio).
  - Cliccare sulla voce *"Formato Riferimento Categorie"*.
  - Verificare che la schermata si apra con animazione fluida.
  - Acquisire screenshot di verifica (`screenshot_category_reference_settings.png`).

- [ ] **Step 3: Modificare i campi per una categoria (es. Unioncasa o Rilievo) e verificare l'anteprima live**
  - Espandere la scheda Unioncasa.
  - Attivare/disattivare i chip e provare i preset veloci.
  - Verificare che l'anteprima mostri istantaneamente il titolo aggiornato.

- [ ] **Step 4: Tornare all'Agenda e verificare che gli appuntamenti riflettano la personalizzazione**
  - Tornare indietro con il tasto `← Impostazioni` e chiudere il foglio.
  - Verificare che gli appuntamenti nell'Agenda mostrino il titolo secondo il formato impostato.
  - Acquisire screenshot di verifica finale (`screenshot_agenda_custom_references.png`).
