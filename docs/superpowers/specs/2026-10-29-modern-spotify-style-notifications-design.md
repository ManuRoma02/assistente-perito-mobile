# Design Spec: Notifiche Moderne Rich Media Stile Spotify per Assistente Perito Mobile

> **Status:** Approvato dal committente (29 Ottobre 2026)  
> **Target Platform:** Android API 26+ (minSdk 26, targetSdk 35)  
> **Visual Archetype:** Double-Bezel Media Card, DavezBordeaux Gradient (`#730B1E` / `#23050C`), Glassmorphism, Material You / Spotify Scrubber.

---

## 1. Obiettivo & Visione

Le notifiche attuali dell'applicazione utilizzano l'icona generica grigia di sistema (`android.R.drawable.ic_menu_my_calendar`) e un blocco di testo piatto non formattato privo di pulsanti d'azione rapida, risultando datate e poco fruibili.

L'obiettivo è trasformare le notifiche del **Briefing Mattutino** e le notifiche di stato in **Media Card Ricche e Interattive** ispirate all'architettura delle notifiche di Spotify e dei moderni player Android 14/15, integrando:
1. **Cover Visuale Dinamica**: copertina squircle ad alto contrasto con giorno del mese, abbreviazione mese e pill di conteggio (`3 OGGI`).
2. **Gerarchia Tipografica di Pregio**: titolo in rilievo, categoria con pill cromatica dedicata (`🔥 Sinistro Incendio`), orario e cliente del prossimo sopralluogo imminente, con indirizzo leggibile.
3. **Scrubber Timeline della Giornata**: barra orizzontale a progressione visiva che scandisce le tappe orarie dei sopralluoghi previsti.
4. **Action Pills Operative (1-Tap)**:
   - `🗺️ Avvia Mappe`: avvia immediatamente la navigazione GPS con Google Maps/Waze verso il luogo del prossimo sopralluogo.
   - `📁 Fascicolo`: apre l'app direttamente sulla perizia o sul fascicolo cliente correlato.
   - `📅 Agenda`: apre la vista calendario giornaliera.
   - `⏰ Posticipa`: promemoria rapido di 15 minuti.

---

## 2. Architettura Tecnica

### 2.1 Layout Nativo RemoteViews
Android consente la personalizzazione grafica della notifica tramite `RemoteViews` combinato con lo stile `NotificationCompat.DecoratedCustomViewStyle()`.

- **Layout compatto (`res/layout/view_notification_briefing_collapsed.xml`)**:
  - Mostra la cover quadrata con data, titolo breve, prossimo appuntamento e pulsante rapido mappe.
- **Layout espanso (`res/layout/view_notification_briefing_expanded.xml`)**:
  - Mostra l'intera card con copertina visuale, pill di categoria, intestazione estesa, indirizzo, timeline orizzontale orari e i 4 pulsanti di azione.

### 2.2 ModernNotificationBuilder
Un modulo autonomo e testabile (`com.example.assistenteperitomobile.data.notifications.ModernNotificationBuilder`) responsabile di:
- Estrarre i dati salienti dagli eventi del giorno (`CalendarEventItem`).
- Generare dinamicamente il `Bitmap` della cover con giorno e mese (senza dipendere da file statici o download remoti).
- Formattare i testi e la timeline degli orari.
- Popolare le `RemoteViews` con i colori Davez/Apple (`DavezBordeaux`, bianco puro, rosa cipria per contrasto).
- Agganciare i `PendingIntent` corretti (navigazione geografica, navigazione tab dell'app e broadcast per posticipo).

### 2.3 Ricevitore Notifica / Azioni Dirette
Un `BroadcastReceiver` dedicato (`BriefingActionReceiver`) per gestire le azioni rapide dalla tendina notifiche senza costringere ad aprire l'app per un'azione secondaria (es. posticipo promemoria o logging dell'avvio navigatore).

---

## 3. Specifiche Visive & Cromatiche

- **Palette**:
  - Sfondo Card Primario: Sfumatura bordeaux scuro (`#23050C` ➔ `#140207`) con highlight superiore `rgba(255, 255, 255, 0.12)`.
  - Accento Pulsante Mappe: Gradiente Davez Red (`#E11D48` ➔ `#9F1239`).
  - Badge Categoria: Sfondo semitrasparente `rgba(225, 29, 72, 0.22)` con bordo sottile `rgba(225, 29, 72, 0.4)`.
  - Timeline Track: Barra scura `rgba(255, 255, 255, 0.12)` con riempimento gradiente Davez e nodi circolari luminosi per ogni appuntamento.
- **Icone e Risorse**:
  - Icona notifica nella barra di stato: `ic_perito_notification.xml` (monogramma vettoriale nitido 24x24dp).
  - Background shapes: drawables XML con angoli arrotondati (`rounded_card_bg.xml`, `rounded_pill_button.xml`, `rounded_cover_bg.xml`).

---

## 4. Requisiti di Test & Qualità
- Tutti i formati di dati (0 eventi, 1 evento, 3+ eventi) devono produrre una notifica coerente senza crash.
- Gestione sicura di indirizzi mancanti o titoli vuoti con fallback deterministici.
- Tutti i test unitari Gradle preesistenti (102 test) e i nuovi test per `ModernNotificationBuilder` devono passare al 100%.
