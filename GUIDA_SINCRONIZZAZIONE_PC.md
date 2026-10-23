# 📂 Guida Completa alla Sincronizzazione delle 6 Cartelle (Telefono Android ↔ PC Windows)
**Progetto:** PeritoAI — Studio Tecnico Peritale (`Assistente_Perito_Mobile`)

---

## 1. Come Funziona l'Archivio su Android (`ArchiveRepository.kt`)

Ogni volta che **PeritoAI** classifica un documento (PDF, busta crittografica `.p7m` sbustata o scansione da fotocamera), oppure quando sposti o elimini un file dalla chat, l'app mantiene automaticamente sincronizzata una **copia speculare 1:1** nella memoria pubblica condivisa del telefono:

```text
/storage/emulated/0/Documents/Perizie_Archivio/
├── Atti_Tribunale/
├── Perizie_Tecniche/
├── Planimetrie_Computi/
├── Fatture_Preventivi/
├── Corrispondenza_Legale/
└── Da_Classificare_Manualmente/
```

* **Nomenclatura automatica dei file:** `YYYY-MM-DD_<Cliente>_<Oggetto>_<NomeOriginale>.pdf` (es. `2026-10-11_Tribunale_Milano_Nomina_CTU_atto.pdf`).
* Poiché la cartella risiede in `Memoria interna -> Documenti -> Perizie_Archivio`, **qualsiasi app di sincronizzazione Android può leggerla e sincronizzarla in tempo reale con il PC Windows dello studio**.

---

## 2. Confronto Rapido delle 5 Opzioni di Sincronizzazione

| Opzione | Passa dal Cloud? | Limiti di Spazio | Funziona in Mobilità (4G/5G)? | Costo | Ideale per... |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **1. Syncthing (P2P)** | ❌ No (Diretto Telefono↔PC) | **Illimitato** (spazio disco) | ✅ Sì (Relay crittografato) | **Gratis (Open Source)** | **Massima privacy (CTU/Tribunale), zero abbonamenti, velocità istantanea** |
| **2. Dropbox + Dropsync** | ✅ Sì (Cloud Dropbox) | 2 GB gratis / 2 TB Pro | ✅ Sì | Gratis (fino a 1 cartella su Dropsync) | Chi usa già **Dropbox** sul PC dello studio |
| **3. OneDrive + Onesync** | ✅ Sì (Cloud Microsoft) | 5 GB gratis / 1 TB M365 | ✅ Sì | Gratis (fino a 1 cartella su Onesync) | Chi ha già **Office 365 / OneDrive** integrato in Windows 11 |
| **4. Google Drive + DriveSync** | ✅ Sì (Cloud Google) | 15 GB gratis / 100+ GB One | ✅ Sì | Gratis (fino a 1 cartella su DriveSync) | Chi usa già l'account Google per **Google Calendar** |
| **5. Rete Studio SMB + FolderSync** | ❌ No (Solo Wi-Fi Studio) | **Illimitato** (disco PC/NAS) | ⚠️ Sincronizza al rientro in Wi-Fi | Gratis / ~5€ una tantum | Studi con **NAS (Synology/QNAP)** o PC fisso condiviso in LAN |

---

## 3. Opzione 1 (Consigliata): Syncthing — Diretta Telefono ↔ PC (Zero Cloud, 100% Privacy)

**Perché sceglierla:** Non carica atti giudiziari o perizie su server esterni, non ha limiti di gigabyte e aggiorna la cartella sul PC Windows entro 2–3 secondi dallo smistamento sul telefono (sia in Wi-Fi sia da fuori studio in 4G/5G).

### Passo A — Sul PC Windows
1. Scarica e installa **SyncTrayzor** (oppure **Syncthing Windows Setup** da `https://syncthing.net`).
2. Avvialo: si aprirà il pannello locale (`http://127.0.0.1:8384`).
3. Crea sul PC la cartella `C:\Users\<TuoUtente>\Documents\Perizie_Archivio`.
4. Nel menu in alto a destra di Syncthing clicca **Azioni -> Mostra ID** (comparirà un **Codice QR**).

### Passo B — Sul Telefono Android
1. Installa **Syncthing-Fork** dal Google Play Store (o F-Droid).
2. Concedi il permesso di accesso ai file e disattiva il risparmio energetico per l'app.
3. Vai nella scheda **Dispositivi** -> tocca **`+`** in alto -> scansiona il **Codice QR** mostrato sul PC Windows e salva.
4. Sul PC Windows comparirà un avviso *"Il dispositivo Android vuole connettersi"*: clicca **Aggiungi Dispositivo**.
5. Sul telefono vai nella scheda **Cartelle** -> tocca **`+`**:
   * **Etichetta Cartella:** `Perizie_Archivio`
   * **ID Cartella:** `perizie-archivio`
   * **Percorso Cartella:** seleziona `/storage/emulated/0/Documents/Perizie_Archivio`
   * **Attiva spunta sul dispositivo:** attiva l'interruttore del tuo **PC Windows**
   * **Tipo di Cartella:** `Invia e Ricevi` (Bidirezionale)
   * **Osserva cambiamenti (File Watcher):** `Attivo`
6. Sul PC Windows comparirà il banner *"Android vuole condividere la cartella Perizie_Archivio"*: clicca **Condividi** e imposta come percorso `C:\Users\<TuoUtente>\Documents\Perizie_Archivio`.

> ✅ **Risultato:** Ogni PDF o foto classificata da PeritoAI compare dopo pochi istanti nella rispettiva sottocartella del PC Windows.

---

## 4. Opzione 2: Dropbox (PC Windows + App Android *Dropsync* o *FolderSync*)

> **Nota tecnica Android:** L'app ufficiale di Dropbox su Android non sincronizza in background cartelle arbitrarie della memoria interna del telefono. Per sincronizzare automaticamente `Documents/Perizie_Archivio` si usa **Dropsync (Autosync for Dropbox)** o **FolderSync**.

### Passo A — Sul PC Windows
1. Assicurati di avere l'app desktop **Dropbox** installata sul PC.
2. Dentro la tua cartella Dropbox crea una cartella chiamata `Perizie_Archivio` (es. `C:\Users\<TuoUtente>\Dropbox\Perizie_Archivio`).

### Passo B — Sul Telefono Android
1. Installa **Dropsync (Autosync for Dropbox)** (la versione gratuita permette di sincronizzare 1 coppia di cartelle complete con tutte le 6 sottocartelle incluse).
2. Apri Dropsync -> Accedi al tuo account Dropbox.
3. Tocca **Crea coppia di cartelle sincronizzate**:
   * **Cartella nel dispositivo (Locale):** `/storage/emulated/0/Documents/Perizie_Archivio`
   * **Cartella in Dropbox (Remota):** `/Perizie_Archivio`
   * **Metodo di sincronizzazione:** `Bidirezionale (Two-way)`
4. Nelle impostazioni di Dropsync attiva **Sincronizzazione istantanea al rilevamento modifiche** (*Monitor local file changes*).

> ✅ **Risultato:** Appena PeritoAI salva o sposta un documento nelle 6 cartelle, Dropsync lo carica su Dropbox e il PC Windows lo scarica subito in `C:\Users\<TuoUtente>\Dropbox\Perizie_Archivio\<Categoria>`.

---

## 5. Opzione 3: Microsoft OneDrive (PC Windows + App Android *Onesync* o *FolderSync*)

**Ideale se il cliente usa Windows 10/11 con OneDrive già attivo.**

### Passo A — Sul PC Windows
1. Apri la cartella **OneDrive** in Esplora File di Windows e crea la cartella `Perizie_Archivio` (`C:\Users\<TuoUtente>\OneDrive\Perizie_Archivio`).
2. Fai **clic destro** sulla cartella `Perizie_Archivio` -> seleziona **"Conserva sempre su questo dispositivo"** (così i PDF sono sempre scaricati fisicamente sul disco del PC anche offline).

### Passo B — Sul Telefono Android
1. Installa **Onesync (Autosync for OneDrive)** dal Play Store.
2. Collega l'account Microsoft dello studio.
3. Configura la coppia di cartelle:
   * **Cartella locale sul telefono:** `/storage/emulated/0/Documents/Perizie_Archivio`
   * **Cartella remota OneDrive:** `/Perizie_Archivio`
   * **Modalità:** `Bidirezionale (Two-way)` con monitoraggio modifiche attivo.

---

## 6. Opzione 4: Google Drive (PC Windows + App Android *DriveSync* o *FolderSync*)

**Ideale se il perito usa già l'account Google per sincronizzare Google Calendar.**

1. **Sul PC Windows:** Installa **Google Drive per Desktop** e crea `Il mio Drive\Perizie_Archivio` (abilitando *"Disponibile offline"*).
2. **Sul Telefono Android:** Installa **Autosync for Google Drive (DriveSync)**, collega lo stesso account Google e accoppia:
   * **Locale:** `/storage/emulated/0/Documents/Perizie_Archivio`
   * **Remota:** `/Perizie_Archivio`
   * **Modalità:** `Bidirezionale (Two-way)`

---

## 7. Opzione 5: Cartella di Rete Locale dello Studio / NAS (SMB3 senza Cloud)

Se in studio c'è un PC fisso sempre acceso o un NAS (Synology, QNAP, Fritz!NAS) e si vuole sincronizzare **solo tramite il Wi-Fi dello studio** senza internet né programmi aggiuntivi sul PC:

1. **Sul PC Windows (o NAS):** Condividi in rete locale la cartella `C:\Users\<TuoUtente>\Documents\Perizie_Archivio` con permessi di Lettura/Scrittura per l'utente dello studio (es. indirizzo `\\192.168.1.100\Perizie_Archivio`).
2. **Sul Telefono Android:** Installa **FolderSync**:
   * Aggiungi un account di tipo **SMB3 (Windows Share)** inserendo l'IP locale del PC/NAS e le credenziali Windows.
   * Crea un *Folderpair* tra `/storage/emulated/0/Documents/Perizie_Archivio` e `\\192.168.1.100\Perizie_Archivio`.
   * Attiva l'opzione **"Sincronizza solo quando connesso al Wi-Fi dello Studio"**: quando il perito rientra in studio dal sopralluogo, il telefono riversa automaticamente tutte le scansioni, le perizie e gli atti direttamente sul PC.
