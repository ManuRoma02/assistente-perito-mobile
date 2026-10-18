@echo off
chcp 65001 >nul
title Invio Assistente_Perito_Mobile.apk su Telegram
echo ====================================================================
echo      INVIO DIRETTO DELL'APP (.APK) SUL TUO TELEGRAM
echo ====================================================================
echo.

set "APK_FILE=%~dp0Assistente_Perito_Mobile.apk"
if not exist "%APK_FILE%" (
    echo [ERRORE] File Assistente_Perito_Mobile.apk non trovato!
    pause
    exit /b 1
)

echo Invio di Assistente_Perito_Mobile.apk (45 MB) alla tua chat Telegram in corso...
echo Attendi qualche secondo...
curl.exe -s -F "chat_id=REDACTED_CHAT_ID" -F "document=@%APK_FILE%" -F "caption=📱 Ecco l'app nativa Assistente Perito Mobile (.APK)! Tocca il file dal telefono per installarla." "https://api.telegram.org/botREDACTED_TELEGRAM_BOT_TOKEN/sendDocument" > "%TEMP%\tg_apk_res.json"

findstr /C:"\"ok\":true" "%TEMP%\tg_apk_res.json" >nul
if %ERRORLEVEL% EQU 0 (
    echo.
    echo [OK] APK inviato con successo sul tuo Telegram!
    echo Apri Telegram sullo smartphone e tocca il file per installare l'app.
) else (
    echo.
    echo [AVVISO] Impossibile inviare via Telegram (verifica la connessione internet).
    type "%TEMP%\tg_apk_res.json"
)
echo.
pause
