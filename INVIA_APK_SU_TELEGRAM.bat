@echo off
chcp 65001 >nul
title Invio Assistente_Perito_Mobile.apk su Telegram
echo ====================================================================
echo      INVIO DIRETTO DELL'APP (.APK) SUL TUO TELEGRAM
echo ====================================================================
echo.

set "APK_FILE=%~dp0Assistente_Perito_Mobile.apk"
if not exist "%APK_FILE%" (
    echo [ERRORE] File Assistente_Perito_Mobile.apk non trovato! Esegui prima COMPILA_APK.bat.
    pause
    exit /b 1
)

if exist "%~dp0telegram_config.bat" (
    call "%~dp0telegram_config.bat"
)

if "%TG_BOT_TOKEN%"=="" (
    echo [CONFIGURAZIONE TELEGRAM RICHIESTA]
    echo Crea il file telegram_config.bat (vedi telegram_config.bat.example)
    echo oppure inserisci i parametri ora:
    echo.
    set /p TG_BOT_TOKEN="Inserisci Token Bot Telegram: "
)

if "%TG_CHAT_ID%"=="" (
    set /p TG_CHAT_ID="Inserisci Chat ID Telegram: "
)

if "%TG_BOT_TOKEN%"=="" (
    echo [ERRORE] Token Bot non specificato.
    pause
    exit /b 1
)

if "%TG_CHAT_ID%"=="" (
    echo [ERRORE] Chat ID non specificato.
    pause
    exit /b 1
)

echo.
echo Invio di Assistente_Perito_Mobile.apk alla tua chat Telegram in corso...
echo Attendi qualche secondo...
curl.exe -s -F "chat_id=%TG_CHAT_ID%" -F "document=@%APK_FILE%" -F "caption=📱 Ecco l'app nativa Assistente Perito Mobile (.APK)! Tocca il file dal telefono per installarla." "https://api.telegram.org/bot%TG_BOT_TOKEN%/sendDocument" > "%TEMP%\tg_apk_res.json"

findstr /C:"\"ok\":true" "%TEMP%\tg_apk_res.json" >nul
if %ERRORLEVEL% EQU 0 (
    echo.
    echo [OK] APK inviato con successo sul tuo Telegram!
    echo Apri Telegram sullo smartphone e tocca il file per installare l'app.
) else (
    echo.
    echo [AVVISO] Impossibile inviare via Telegram (verifica connessione o credenziali).
    type "%TEMP%\tg_apk_res.json"
)
echo.
pause
