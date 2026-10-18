@echo off
chcp 65001 >nul
title Installazione Assistente Perito Mobile su Smartphone Android
echo ====================================================================
echo      INSTALLAZIONE ASSISTENTE PERITO MOBILE (.APK) SU ANDROID
echo ====================================================================
echo.

set "APK_FILE=%~dp0Assistente_Perito_Mobile.apk"
if not exist "%APK_FILE%" (
    set "APK_FILE=%~dp0app\build\outputs\apk\debug\app-debug.apk"
)

if not exist "%APK_FILE%" (
    echo [ERRORE] File APK non trovato. Esegui prima COMPILA_APK.bat.
    pause
    exit /b 1
)

set "ADB_CMD=adb"
if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
    set "ADB_CMD=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
)

echo Verifica smartphone Android collegato via USB (Debug USB attivo)...
"%ADB_CMD%" devices
echo.
echo Installazione di Assistente_Perito_Mobile.apk in corso...
"%ADB_CMD%" install -r "%APK_FILE%"
if %ERRORLEVEL% EQU 0 (
    echo.
    echo [OK] App installata con successo sullo smartphone!
    echo Avvio dell'app sul telefono...
    "%ADB_CMD%" shell monkey -p com.example.assistenteperitomobile -c android.intent.category.LAUNCHER 1 >nul 2>&1
) else (
    echo.
    echo [INFO] Nessun telefono rilevato via USB oppure Debug USB non autorizzato.
    echo Puoi copiare o inviare direttamente il file:
    echo   %APK_FILE%
    echo sul tuo telefono (es. tramite Telegram o cavo USB) e toccarlo per installarlo!
)
echo.
pause
