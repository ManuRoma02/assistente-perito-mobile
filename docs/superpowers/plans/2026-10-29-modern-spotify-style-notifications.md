# Notifiche Moderne Rich Media Stile Spotify Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Trasformare le notifiche del Briefing Mattutino e le notifiche di sistema da semplici testi generici a Media Card ricche e interattive in stile Spotify, con copertina dinamica della data, timeline orizzontale degli appuntamenti e action pill operative ad 1-tap (`Avvia Mappe`, `Fascicolo`, `Agenda`, `Posticipa`).

**Architecture:** Creare un builder nativo `ModernNotificationBuilder` che genera notifiche personalizzate tramite `RemoteViews` e `NotificationCompat.DecoratedCustomViewStyle`. Realizzare i layout XML compatto ed espanso in `res/layout/`, le risorse vettoriali in `res/drawable/`, un generatore dinamico di cover bitmap, e collegare le azioni ai `PendingIntent` e a un `BroadcastReceiver` dedicato.

**Tech Stack:** Kotlin 2.x, Android SDK (API 26+), NotificationCompat, RemoteViews, Vector Drawables, Canvas/Bitmap rendering, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-29-modern-spotify-style-notifications-design.md`

## Global Constraints

- Android API 26+ compatibility (minSdk 26, targetSdk 35).
- Compatibilità con notifiche di sistema Android 12, 13, 14 e 15 (Material You, DecoratedCustomViewStyle).
- Nessun dipendenza da librerie terze o asset pesanti: rendering nativo di forme e bitmap cover via Android Graphics Canvas.
- Azioni 1-tap conformi alla sicurezza Android 12+ (`FLAG_IMMUTABLE` / `FLAG_UPDATE_CURRENT`).
- Preservazione del canale di notifica esistente `perito_morning_briefing` con orario dinamico.

## Review Focus

1. **Zero appuntamenti nella giornata**: la notifica non deve rompersi né mostrare stringhe vuote, ma visualizzare una card accogliente ("Nessun sopralluogo per oggi", cover serena, tasto per aprire l'archivio).
2. **Appuntamenti senza luogo o con indirizzo lungo**: gestione pulita con fallback (`"Luogo da definire"`) e troncamento elegante senza layout shift né sovrapposizione testi.
3. **Pulsante "Avvia Mappe" con indirizzo reale o non valido**: intent `geo:0,0?q=...` ben formattato con fallback sicuro sull'app in caso di assenza di app mappe esterne.
4. **Android 12+ PendingIntent Mutability**: tutti i `PendingIntent` per click, azioni e dismiss devono dichiarare esplicitamente `FLAG_IMMUTABLE` o `FLAG_MUTABLE`.
5. **Reattività e rendering su lockscreen scure e chiare**: contrasto ottimale sia con tema scuro che con tema chiaro di sistema.

---

### Task 1: Risorse Grafiche e Layout XML per Notifiche Custom

**Files:**
- Create: `app/src/main/res/drawable/ic_perito_notification.xml`
- Create: `app/src/main/res/drawable/bg_notification_card.xml`
- Create: `app/src/main/res/drawable/bg_notification_cover.xml`
- Create: `app/src/main/res/drawable/bg_notification_action_primary.xml`
- Create: `app/src/main/res/drawable/bg_notification_action_secondary.xml`
- Create: `app/src/main/res/drawable/bg_category_pill.xml`
- Create: `app/src/main/res/layout/view_notification_briefing_collapsed.xml`
- Create: `app/src/main/res/layout/view_notification_briefing_expanded.xml`

**Interfaces:**
- Produces: Layout e drawable Android standard identificati da `R.layout.view_notification_briefing_collapsed`, `R.layout.view_notification_briefing_expanded`, `R.drawable.ic_perito_notification`.

- [ ] **Step 1: Creare l'icona vettoriale per la status bar `ic_perito_notification.xml`**
  - Disegnare un monogramma bilancia peritale / scudo minimale 24x24dp con riempimento bianco puro.

- [ ] **Step 2: Creare i drawable di sfondo e pulsanti in `app/src/main/res/drawable/`**
  - `bg_notification_card.xml`: rettangolo arrotondato (16dp) con gradiente bordeaux scuro `#23050C` ➔ `#140207`.
  - `bg_notification_cover.xml`: squircle (12dp) per la data/copertina.
  - `bg_notification_action_primary.xml`: pill arrotondato (999dp) gradiente bordeaux acceso per azione Mappe.
  - `bg_notification_action_secondary.xml`: pill semitrasparente per Fascicolo e Agenda.
  - `bg_category_pill.xml`: pill semitrasparente per la categoria del sinistro.

- [ ] **Step 3: Creare il layout compatto `view_notification_briefing_collapsed.xml`**
  - Struttura: ImageView per cover data/conteggio, TextView titolo, TextView sottotitolo/orario, pulsante rapido mappe.

- [ ] **Step 4: Creare il layout esteso `view_notification_briefing_expanded.xml`**
  - Struttura: Card completa in stile Spotify con cover artistica, pill categoria, titolo evento principale, indirizzo con icona mappa, barra timeline orizzontale orari e riga con 4 pulsanti pill (`Avvia Mappe`, `Fascicolo`, `Agenda`, `Posticipa`).

- [ ] **Step 5: Verificare la compilazione delle risorse XML**
  - Eseguire: `.\gradlew.bat compileDebugResources`
  - Risultato atteso: PASS.

- [ ] **Step 6: Commit del Task 1**
  - Commit: `git add app/src/main/res/ && git commit -m "feat(notifications): add custom RemoteViews layouts and vector drawables for Spotify-style notification"`

---

### Task 2: Modello Dati e Test Unitari di Sintesi Notifica

**Files:**
- Create: `app/src/main/java/com/example/assistenteperitomobile/data/notifications/NotificationBriefingModel.kt`
- Create: `app/src/test/java/com/example/assistenteperitomobile/NotificationBriefingModelTest.kt`

**Interfaces:**
- Produces: `NotificationBriefingData`, `NotificationTimelineStep`, `NotificationBriefingFormatter.computeBriefingData(...)`

- [ ] **Step 1: Scrivere i test unitari in `NotificationBriefingModelTest.kt`**
  - Test per giornata con 3 appuntamenti (estrazione primo appuntamento, conteggio, timeline orari, categoria).
  - Test per giornata con 1 appuntamento.
  - Test per giornata libera da sopralluoghi (zero eventi).
  - Test per gestione indirizzi mancanti o con caratteri speciali.

- [ ] **Step 2: Eseguire il test per verificare il fallimento**
  - Eseguire: `$env:JAVA_HOME = "C:\Users\manue\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.12.1+1"; .\gradlew.bat testDebugUnitTest --tests com.example.assistenteperitomobile.NotificationBriefingModelTest`
  - Risultato atteso: FAIL (classi non ancora implementate).

- [ ] **Step 3: Implementare `NotificationBriefingModel.kt`**
  - Definire data class `NotificationBriefingData` e `NotificationTimelineStep`.
  - Implementare l'algoritmo di formattazione ed estrazione in `NotificationBriefingFormatter`.

- [ ] **Step 4: Eseguire il test per verificare che passi**
  - Eseguire: `$env:JAVA_HOME = "C:\Users\manue\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.12.1+1"; .\gradlew.bat testDebugUnitTest --tests com.example.assistenteperitomobile.NotificationBriefingModelTest`
  - Risultato atteso: PASS.

- [ ] **Step 5: Commit del Task 2**
  - Commit: `git add app/src/main/java/com/example/assistenteperitomobile/data/notifications/NotificationBriefingModel.kt app/src/test/java/com/example/assistenteperitomobile/NotificationBriefingModelTest.kt && git commit -m "feat(notifications): add notification briefing data model and unit tests"`

---

### Task 3: Implementazione di `ModernNotificationBuilder` & `BriefingActionReceiver`

**Files:**
- Create: `app/src/main/java/com/example/assistenteperitomobile/data/notifications/ModernNotificationBuilder.kt`
- Create: `app/src/main/java/com/example/assistenteperitomobile/data/notifications/BriefingActionReceiver.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `NotificationBriefingData` da Task 2, layout XML da Task 1.
- Produces: `ModernNotificationBuilder.buildBriefingNotification(context, data): Notification` e registrazione del receiver in Manifest.

- [ ] **Step 1: Implementare `BriefingActionReceiver.kt`**
  - Riceve intent per:
    - `"ACTION_SNOOZE"`: ripianifica notifica a 15 minuti.
    - `"ACTION_NAVIGATE_MAPS"`: apre URI `geo:0,0?q=...` o intent browser Maps.
  - Registrare in `AndroidManifest.xml` con `android:exported="false"`.

- [ ] **Step 2: Implementare `ModernNotificationBuilder.kt`**
  - Generazione bitmap cover con Canvas: giorno ("29"), mese ("OTT"), badge conteggio.
  - Popolamento delle `RemoteViews` compatto ed esteso (`setTextViewText`, `setImageViewBitmap`, `setOnClickPendingIntent`).
  - Creazione `NotificationCompat.Builder`:
    - `setSmallIcon(R.drawable.ic_perito_notification)`
    - `setStyle(NotificationCompat.DecoratedCustomViewStyle())`
    - `setCustomContentView(collapsedViews)`
    - `setCustomBigContentView(expandedViews)`
    - `setColor(0xFF730B1E.toInt())`
    - `setColorized(true)`
    - `setPriority(NotificationCompat.PRIORITY_HIGH)`
    - `setAutoCancel(true)`

- [ ] **Step 3: Verificare compilazione e registrazione Manifest**
  - Eseguire: `$env:JAVA_HOME = "C:\Users\manue\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.12.1+1"; .\gradlew.bat assembleDebug`
  - Risultato atteso: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit del Task 3**
  - Commit: `git add app/src/main/java/com/example/assistenteperitomobile/data/notifications/ app/src/main/AndroidManifest.xml && git commit -m "feat(notifications): implement ModernNotificationBuilder and BriefingActionReceiver"`

---

### Task 4: Integrazione in `MorningBriefingWorker`

**Files:**
- Modify: `app/src/main/java/com/example/assistenteperitomobile/data/MorningBriefingWorker.kt`

**Interfaces:**
- Consumes: `ModernNotificationBuilder.buildBriefingNotification`
- Modifies: `MorningBriefingWorker.sendBriefingNotification` e `MorningBriefingWorker.showNotification`.

- [ ] **Step 1: Aggiornare `MorningBriefingWorker.sendBriefingNotification`**
  - Raccogliere `todayEvents` e `tomorrowEvents` da `CalendarRepository`.
  - Calcolare `NotificationBriefingData` tramite `NotificationBriefingFormatter`.
  - Costruire la notifica moderna con `ModernNotificationBuilder`.
  - Inviare la notifica tramite `NotificationManager`.

- [ ] **Step 2: Mantenere fallback sicuro per dispositivi che non supportano Custom Views**
  - In caso di eccezione durante la generazione delle RemoteViews, ricadere silenziosamente sul `BigTextStyle` nativo.

- [ ] **Step 3: Verificare con tutti i test unitari**
  - Eseguire: `$env:JAVA_HOME = "C:\Users\manue\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.12.1+1"; .\gradlew.bat test`
  - Risultato atteso: BUILD SUCCESSFUL (tutti i test passano).

- [ ] **Step 4: Commit del Task 4**
  - Commit: `git add app/src/main/java/com/example/assistenteperitomobile/data/MorningBriefingWorker.kt && git commit -m "feat(notifications): integrate modern rich notification into MorningBriefingWorker"`

---

### Task 5: Suite Completa di Test, Build e Verifica Release

**Files:**
- Run full test suite & assemble release.

- [ ] **Step 1: Esecuzione completa dei test unitari**
  - Eseguire: `$env:JAVA_HOME = "C:\Users\manue\AppData\Local\Programs\Eclipse Adoptium\jdk-21.0.12.1+1"; .\gradlew.bat test`
  - Verificare che il conteggio dei test passati sia >= 106.

- [ ] **Step 2: Aggiornare il grafo con `graft build`**
  - Eseguire: `graft build`

- [ ] **Step 3: Verifica stato Git pulito**
  - Eseguire: `git status`
