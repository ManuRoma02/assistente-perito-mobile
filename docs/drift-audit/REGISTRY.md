# Codebase Drift Registry

Repository: `Assistente_Perito_Mobile`
Audit Date: 2026-10-23
Last Remediation: 2026-10-25
Auditor: `agency-codebase-archaeologist`

---

## View 1: Master Findings

| Finding | Files | Type | Severity | Status |
|---|---|---|---|---|
| Broken primary calendar fallback logic | `CalendarRepository.kt` | Reversed fallback | Critical | Fixed |
| False-positive category changes & loose substring matches | `PeritoViewModel.kt`, `Models.kt`, `CalendarRepository.kt` | Logic flaw / False positive | Critical | Fixed |
| Orphaned multi-agent routing & doc divergence | `IntentRouterAgent.kt`, `AgentPipelineOrchestrator.kt`, `PeritoViewModel.kt` | Doc / Routing scope mismatch | Moderate | Fixed |
| Substring classification in system calendar events | `CalendarRepository.kt`, `Models.kt` | Keyword collision | Moderate | Fixed |
| Cross-session fascicolo leakage on external share | `PeritoViewModel.kt`, `MainActivity.kt` | State assumption | Moderate | Fixed |
| LAN cleartext traffic for local LLM mDNS endpoints | `network_security_config.xml` | Security boundary | Bounded Risk | Documented |
| Hardcoded model name in background service | `ReasoningBackgroundService.kt` | Cosmetic drift | Cosmetic | Fixed |

---

## View 2: Eras & Pattern Evolution

| Era | Commit Window | Architectural Pattern | Dominant Files |
|---|---|---|---|
| **Era 1 (Initial Engine & Storage)** | ≤ Oct 20 (v1.0.9–v1.0.10) | Hardcoded enums (`PeritoCategory`, `PeritoEventCategory`), static list fallbacks (`defaultCategoriesList()`), llama.cpp 2B model | `Models.kt`, `ArchiveRepository.kt`, `CalendarRepository.kt` |
| **Era 2 (Dynamic Taxonomy & Formats)** | Oct 21 (v1.0.11–v1.0.14) | Dynamic `PeritoCategoryDefinition`, per-category format strings, 4B model options, interactive UI | `CategoryReferenceSettingsScreen.kt`, `PeritoViewModel.kt`, `AgendaVisivaScreen.kt` |
| **Era 3 (Security & Agentic Orchestration)** | Oct 23 (v1.0.15) | Multi-agent abstraction layers (`IntentRouterAgent`, `AppointmentExtractorAgent`), FileProvider isolation, intent guards | `AgentPipelineOrchestrator.kt`, `IntentRouterAgent.kt`, `network_security_config.xml` |

---

## View 3: Responsibilities & Implementation Consistency

| Responsibility | Implementations Found | Consistent? | Remediation Notes |
|---|---|---|---|
| **Intent Routing** | `IntentRouterAgent.classify` vs `PeritoViewModel.sendUserMessage` | ✅ Consistent (UI only) | `agentOrchestrator.routeMessage` genera l'etichetta contestuale `processingLabel` per l'interfaccia; il dispatch esecutivo rimane deterministico in `PeritoViewModel`. |
| **Appointment Categorization** | `detectCategoryCorrection` vs `CalendarRepository.classifyEventCategory` | ✅ Yes | Matcher condiviso con confini di parola esatti; priorità: categorie personalizzate, attività tecniche specifiche e fallback legacy. |
| **Document Categorization** | `PeritoCategory` (enum, 6 folders) | ✅ Yes | Archivio documentale e cartelle isolate e coerenti. |
| **Writable Calendar Selection** | `CalendarRepository.resolveWritableCalendarId` | ✅ Yes | Helper condiviso tra repository e test con gerarchia: `matchedSelectedId ?: primaryId ?: firstWritableId`. |
| **Shared Document Assignment** | `PeritoViewModel.processDocumentUri` | ✅ Yes | Condivisioni esterne `ACTION_SEND` non ereditano `selectedFascicoloId`. |

---

## View 4: Risk Priority

### Critical (Data Integrity / Incorrect State)
- **Calendario Scrivibile**: `CalendarRepository.resolveWritableCalendarId` seleziona il calendario primario `isPrimary` su cursori multipli ed è testato direttamente sul codice di produzione.
- **Falsi Positivi Categorie**: `detectCategoryCorrection` previene modifiche accidentali a `lastEv` su messaggi ordinari contenenti "in realtà" o sottostringhe casuali come "aperto".

### Moderate (Degraded Functionality / Edge-Case Disagreement)
- **Classificazione Calendario**: `CalendarRepository.classifyEventCategory` sostituisce `contains` con confini di parola ed evita attribuzioni errate (es. "Aperitivo" per "APE").
- **Intent Routing UI**: `IntentRouterAgent` produce etichette semantiche per `processingLabel`; non altera il dispatch esecutivo deterministico.
- **Fascicoli Condivisi**: `processDocumentUri` isola le condivisioni esterne evitando leak del `selectedFascicoloId` attivo.
- **Rischio Delimitato LAN mDNS**: `<domain includeSubdomains="true">local</domain>` circoscrive il cleartext a server LLM LAN senza impattare WAN.

### Cosmetic (Display / Stale Strings)
- **Modello Notifica**: `ReasoningBackgroundService` visualizza l'etichetta del modello attivo con fallback pulito.
