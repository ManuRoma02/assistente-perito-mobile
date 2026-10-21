# Design Document: Schermata Personalizzazione Riferimento Appuntamenti per Categoria

**Data**: 21 Ottobre 2026  
**Autore**: Antigravity Assistant & Perito Utente  
**Stato**: Proposta validata  
**Target**: Android (Jetpack Compose, Kotlin, Material 3 con Davez Luxury Architectural Style)

---

## 1. Intent e Motivazione (The Why)
Il perito edile gestisce tipologie di appuntamenti molto diverse tra loro: da incontri istituzionali con agenzie immobiliari (es. *Unioncasa*, *Agenzia Orso*), a sopralluoghi tecnici di danno, udienze in tribunale e rilievi laser scanner.
Attualmente il riferimento mostrato nella card dell'Agenda segue un formato predefinito fisso. L'utente ha l'esigenza di poter configurare autonomamente, per ogni singola categoria, quali elementi informativi mostrare nel titolo/riferimento a colpo d'occhio:
- L'orario (es. `10:30`)
- Il tipo o nome categoria (es. `Sopralluogo`, `Rilievo Tecnico`)
- Il cliente, interlocutore o agenzia (es. `Unioncasa`, `Agenzia Orso`, `Geom. Bianchi`)
- Il luogo o indirizzo (es. `Via Roma 14, Monza`)
- L'oggetto o note essenziali (es. `Infiltrazioni terrazzo`)

L'utente deve poter scegliere liberamente: **tutti**, **solo alcuni**, o combinazioni personalizzate per ciascuna categoria.

---

## 2. Obiettivi, Non-Obiettivi e Anti-Obiettivi

### Obiettivi (Goals)
1. **Accessibilità**: Nuova voce nelle Impostazioni dell'app (*"Personalizza Riferimento Categorie"*) che apre una visuale dedicata con navigazione fluida e tasto per tornare indietro.
2. **Scelta per Categoria**: Supporto a tutte le categorie di appuntamento del perito (`PeritoEventCategory`: *Sopralluogo, Rilievo Tecnico, Unioncasa, Agenzia Orso, Tribunale/Udienza, Comune, Riunione Studio, Cliente, Personale, Altro*).
3. **UX Opzione A (Approvata dall'utente)**:
   - Lista a schede/accordion espandibili per ciascuna categoria.
   - Per ogni categoria, visualizzazione rapida a chip cliccabili per:
     - 🕒 Orario (`showTime`)
     - 🏷️ Tipo attività (`showType`)
     - 👤 Cliente / Agenzia (`showSubject`)
     - 📍 Luogo / Indirizzo (`showPlace`)
     - 📝 Oggetto / Note (`showNotes`)
   - Pulsanti di preset veloci: *"Tutti"*, *"Standard (Tipo + Soggetto + Luogo)"*, *"Minimale (Tipo + Soggetto)"*, *"Solo Soggetto"*.
   - **Anteprima Live Istantanea**: una card di esempio che riflette all'istante l'effetto delle selezioni fatte dall'utente.
4. **Persistenza Offline**: Le preferenze di formattazione per categoria vengono salvate persistentemente in `AiEngineConfig` (salvato su file JSON protetto nelle preferenze dell'app) e sopravvivono a riavvii dell'app.
5. **Applicazione Dinamica & Reattiva**: L'Agenda Visiva formatta il titolo dell'evento rispettando la configurazione della rispettiva categoria in tempo reale.

### Non-Obiettivi (Non-Goals)
- Nascondere o rimuovere le informazioni operative dal corpo espanso della card: quando la scheda appuntamento viene espansa dall'utente, i dati completi (luogo con tasto per aprire Google Maps, note integrali, fascicolo associato) rimangono sempre accessibili.
- Sovrascrivere distruttivamente titoli custom già presenti su Google Calendar se non contengono campi peritali riconosciuti.

### Anti-Obiettivi (Anti-Goals)
- Nessun modulo con form lenti o finestre di dialogo sovrapposte che costringono a conferme multiple per ogni singola categoria. L'interazione deve essere immediata, tattile e reattiva.
- Nessun layout shift o stuttering: animazioni fluide con hardware acceleration (Emil Kowalski micro-interactions).

---

## 3. Risposte e Decisioni Architetturali (Builder Decisions)

### 3.1 Ambito del Riferimento: Titolo Headline vs Corpo della Card
- La configurazione `CategoryReferenceFormat` governa il **titolo sintetico** (`event.titolo`), ovvero il riferimento visibile nella riga principale della scheda agenda, nei promemoria e nelle notifiche del mattino.
- Quando la card viene espansa con il tap, i campi dettagliati (orario completo, riga indirizzo con pulsante interattivo *Apri su Google Maps*, riga note e riga cliente) rimangono intatti e operativi. In questo modo il perito gode di un titolo compatto e personalizzato senza perdere alcuna funzionalità tecnica.

### 3.2 Eventi di Calendari Esterni (Google Calendar / Outlook)
- Per gli eventi creati all'interno dell'app (dettati a voce o creati da chat), i campi `cliente`, `luogo`, `note` sono già tipizzati e separati.
- Per gli eventi importati da Google Calendar:
  - Se il titolo segue il pattern peritale riconosciuto (es. `Tipo - Soggetto`), viene riformattato coerentemente con la categoria identificata.
  - Se il titolo è un testo generico o libero (es. "Dentista", "Riunione condominio"), il titolo originale viene sempre preservato come base per evitare qualsiasi perdita di informazioni dell'utente.

### 3.3 Scrittura su Calendario di Sistema (`CalendarContract.Events.TITLE`)
- Alla creazione di un nuovo appuntamento, il titolo scritto su Google Calendar riflette la regola impostata dall'utente per quella categoria, garantendo che anche sui widget di sistema e su Outlook l'impegno appaia con il riferimento desiderato dal perito.

### 3.4 Architettura di Navigazione nelle Impostazioni
- All'interno del `ModalBottomSheet` delle impostazioni (`AiEngineSettingsSheetContent`), la navigazione tra le Impostazioni Principali e la schermata di Dettaglio Categorie è gestita tramite `AnimatedContent` con transizione orizzontale fluida (*Slide & Fade*).
- La barra superiore della vista categorie presenta un pulsante `← Impostazioni` per tornare indietro istantaneamente, preservando lo stato senza ricaricamenti.

---

## 4. Modello Dati e Struttura Codice

### 4.1 Modello: `CategoryReferenceFormat` (in `Models.kt`)
```kotlin
@Immutable
@Serializable
data class CategoryReferenceFormat(
    val showTime: Boolean = false,
    val showType: Boolean = true,
    val showSubject: Boolean = true,
    val showPlace: Boolean = true,
    val showNotes: Boolean = false
) {
    fun formatTitle(
        startTimeIso: String?,
        categoryType: String,
        subject: String,
        place: String,
        notes: String
    ): String {
        val parts = mutableListOf<String>()
        if (showTime && !startTimeIso.isNullOrBlank()) {
            val h = startTimeIso.drop(11).take(5)
            if (h.isNotBlank()) parts.add(h)
        }
        if (showType && categoryType.isNotBlank()) {
            parts.add(categoryType)
        }
        if (showSubject && subject.isNotBlank() && subject != "Cliente") {
            parts.add(subject)
        }
        
        var base = parts.joinToString(" - ")
        if (base.isBlank()) {
            base = if (categoryType.isNotBlank()) categoryType else "Impegno"
        }
        
        val extras = mutableListOf<String>()
        if (showPlace && place.isNotBlank() && place != "Da definire") {
            extras.add(place)
        }
        if (showNotes && notes.isNotBlank()) {
            extras.add(notes)
        }
        
        return if (extras.isNotEmpty()) {
            "$base (${extras.joinToString(" • ")})"
        } else {
            base
        }
    }
}
```

### 4.2 Aggiornamento di `AiEngineConfig` (in `Models.kt`)
```kotlin
val categoryFormats: Map<String, CategoryReferenceFormat> = defaultCategoryFormats()
```
Default intelligenti per categoria:
- `UNIONCASA`: `showType = true`, `showSubject = true`, `showPlace = true`, `showTime = false`, `showNotes = false` -> *"Incontro - Unioncasa (Via Roma 14)"*
- `AGENZIA_ORSO`: `showType = true`, `showSubject = true`, `showPlace = true`, `showTime = false`, `showNotes = false` -> *"Rilievo Tecnico - Agenzia Orso (Via Garibaldi 3)"*
- `SOPRALLUOGO`: `showType = true`, `showSubject = true`, `showPlace = true`, `showTime = false`, `showNotes = false` -> *"Sopralluogo - Ing. Ferrari (Corso Sempione 88)"*
- `TRIBUNALE`: `showType = true`, `showSubject = true`, `showPlace = false`, `showTime = false`, `showNotes = true` -> *"Udienza Giuramento - Tribunale (Procedimento RG 4102)"*
- `RILIEVO`: `showType = true`, `showSubject = true`, `showPlace = true`, `showTime = false`, `showNotes = false` -> *"Rilievo Tecnico - Geom. Rossi (Via Dante 10)"*

---

## 5. Componente UI: `CategoryReferenceSettingsView`
Composto da:
1. Header elegante con pulsante Indietro, titolo *"Formato Riferimento Appuntamenti"* e badge *"Davez Luxury"*.
2. Testo introduttivo esplicativo.
3. Card Accordion per ciascuna delle 10 categorie di `PeritoEventCategory`:
   - Intestazione card: Icona/Emoji, Nome Categoria, chip riassuntivi dei campi attualmente attivi, chevron animato.
   - Contenuto espanso:
     - 5 Chip interattivi con stato ON/OFF evidente (colore d'accento `#4F0B18` e tick quando selezionati).
     - Rriga preset veloci: *"Tutti"*, *"Standard"*, *"Minimale"*, *"Solo Soggetto"*.
     - Box *"Anteprima in Agenda"*: card che mostra esattamente la resa grafica in tempo reale.
