# In-App OTA Self-Update Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide an Over-The-Air (OTA) in-app update mechanism that checks a remote manifest, alerts users when a new APK is available, allows 1-tap download and installation with progress, and offers manual update management in Studio Settings.

**Architecture:** An `OtaUpdateManager` component fetches an HTTP JSON manifest (`OtaUpdateManifest`), compares `latestVersionCode` with the running app's `PackageInfo.versionCode`, streams the APK download to app cache with live percentage feedback, and dispatches the native Android `PackageInstaller` via `FileProvider`. The UI integrates an automatic dialog on startup, a manual check card in Settings, and a `/aggiorna` slash command.

**Tech Stack:** Kotlin, Jetpack Compose, Android `FileProvider`, `DownloadManager` / `HttpURLConnection`, Coroutines & StateFlow.

**Spec:** User requirement: "deve tipo uscire un avviso sull app della persona che gli dice che c'è un aggiornamento e lui lo scarica o continua con la versione vecchia, dalle impostazioni può scaricare la nuova".

## Global Constraints
- Target platform: Android (minSdk 26, targetSdk 37).
- Standard Android permissions: `android.permission.REQUEST_INSTALL_PACKAGES` added to manifest.
- Architecture: Zero third-party proprietary bloat; robust native Android networking and `FileProvider` intent invocation.
- Aesthetic: Apple HIG / Davez Studio Alabaster (`#F8F6F2`) and Bordeaux (`#6B1D2F`).

## Review Focus
1. APK download interrupted by network drop -> cleanly handle retry without leaving corrupt APK files.
2. Android 8.0+ Unknown App Sources permission -> if `canRequestPackageInstalls()` is false, guide user to system settings toggle and resume install.
3. Version code comparisons -> ensure integer comparison (`latestVersionCode > currentVersionCode`) prevents re-prompting on identical or older versions.
4. Dismissing non-mandatory updates -> "Continua con la versione vecchia" stores dismiss timestamp to avoid harassing the user on every tab switch.
5. Security -> APK must be served via HTTPS and shared to Android package installer strictly via secure `FileProvider` content URI.

---

### Task 1: Permissions & Manifest Configuration

**Files:**
- Modify: `app/src/main/AndroidManifest.xml:10-15`

**Interfaces:**
- Produces: `android.permission.REQUEST_INSTALL_PACKAGES` permission in Manifest.

- [ ] **Step 1: Add `REQUEST_INSTALL_PACKAGES` permission to AndroidManifest.xml**
- [ ] **Step 2: Verify `file_paths.xml` covers cacheDir download location**
- [ ] **Step 3: Run `./gradlew processDebugManifest` to verify manifest merges cleanly**

---

### Task 2: Data Models & OTA Update Manager

**Files:**
- Create: `app/src/main/java/com/example/assistenteperitomobile/data/ota/OtaUpdateModels.kt`
- Create: `app/src/main/java/com/example/assistenteperitomobile/data/ota/OtaUpdateManager.kt`
- Test: `app/src/test/java/com/example/assistenteperitomobile/data/ota/OtaUpdateManagerTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  data class OtaUpdateManifest(
      val latestVersionCode: Int,
      val latestVersionName: String,
      val downloadUrl: String,
      val releaseNotes: String,
      val mandatory: Boolean = false,
      val publishedAt: String = ""
  )
  
  class OtaUpdateManager(context: Context) {
      fun getCurrentVersionCode(): Int
      fun getCurrentVersionName(): String
      suspend fun checkForUpdate(manifestUrl: String): Result<OtaUpdateManifest?>
      suspend fun downloadApk(
          downloadUrl: String,
          onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
      ): Result<File>
      fun canRequestPackageInstalls(): Boolean
      fun createInstallIntent(apkFile: File): Intent
      fun createPermissionSettingsIntent(): Intent
  }
  ```

- [ ] **Step 1: Write unit tests for version code comparison and manifest parsing in `OtaUpdateManagerTest.kt`**
- [ ] **Step 2: Run test to verify it fails (`OtaUpdateManager` not created yet)**
- [ ] **Step 3: Implement `OtaUpdateModels.kt` with serialization/deserialization**
- [ ] **Step 4: Implement `OtaUpdateManager.kt` with background streaming download, progress callbacks, and FileProvider URI generation**
- [ ] **Step 5: Run tests and verify they pass**

---

### Task 3: ViewModel & State Integration

**Files:**
- Modify: `app/src/main/java/com/example/assistenteperitomobile/ui/PeritoViewModel.kt`
- Modify: `app/src/main/java/com/example/assistenteperitomobile/domain/Models.kt`

**Interfaces:**
- Consumes: `OtaUpdateManager`, `OtaUpdateManifest`
- Produces:
  ```kotlin
  // In PeritoUiState:
  val availableUpdate: OtaUpdateManifest? = null,
  val isCheckingUpdate: Boolean = false,
  val updateDownloadProgress: Float? = null,
  val updateStatusMessage: String? = null,
  val showUpdateDialog: Boolean = false,
  val otaManifestUrl: String = ...
  
  // In PeritoViewModel:
  fun checkForUpdates(silent: Boolean = false)
  fun startUpdateDownloadAndInstall(manifest: OtaUpdateManifest, context: Context)
  fun dismissUpdateDialog()
  fun updateOtaManifestUrl(newUrl: String)
  ```

- [ ] **Step 1: Add update fields to `PeritoUiState` in `PeritoViewModel.kt`**
- [ ] **Step 2: Add `otaUpdateManager` instance and startup update check logic in `PeritoViewModel.init`**
- [ ] **Step 3: Implement `checkForUpdates`, `startUpdateDownloadAndInstall`, `dismissUpdateDialog`**
- [ ] **Step 4: Add `/aggiorna` command to slash commands handler in `PeritoViewModel.kt`**

---

### Task 4: UI Dialog & Settings Sheet Integration

**Files:**
- Modify: `app/src/main/java/com/example/assistenteperitomobile/ui/PeritoAppScreens.kt`

**Interfaces:**
- Consumes: `uiState.availableUpdate`, `uiState.showUpdateDialog`, `uiState.updateDownloadProgress`
- Produces:
  ```kotlin
  @Composable
  fun AppUpdateDialog(
      update: OtaUpdateManifest,
      downloadProgress: Float?,
      statusMessage: String?,
      onUpdateNow: () -> Unit,
      onDismiss: () -> Unit
  )
  ```

- [ ] **Step 1: Implement `AppUpdateDialog` composable with Apple HIG squircle styling, release notes display, progress bar, and "Aggiorna Ora" / "Continua con la versione vecchia" buttons**
- [ ] **Step 2: Add `AppUpdateDialog` to root `AssistentePeritoApp` triggered by `uiState.showUpdateDialog && uiState.availableUpdate != null`**
- [ ] **Step 3: Add "Aggiornamenti & Versione" section in `AiEngineSettingsSheetContent` showing current version, manual "Verifica Aggiornamenti" button, and manifest configuration**
- [ ] **Step 4: Register `/aggiorna` in `PeritoSlashCommandsList`**

---

### Task 5: Compilation, Verification & Telegram APK Delivery

**Files:**
- Verify: Full Gradle build `./gradlew assembleDebug --no-daemon`
- Deliver: `app/build/outputs/apk/debug/app-debug.apk` via Telegram MCP tool `send_apk`

- [ ] **Step 1: Run full debug compilation with `./gradlew assembleDebug --no-daemon`**
- [ ] **Step 2: Verify APK generation and check file size**
- [ ] **Step 3: Send compiled APK to Telegram**
- [ ] **Step 4: Update task checklist artifact**
