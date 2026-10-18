@echo off
chcp 65001 >nul
title Compilazione APK - Assistente Perito Mobile
echo ====================================================================
echo      COMPILAZIONE ASSISTENTE PERITO MOBILE (.APK)
echo ====================================================================
echo.
if exist "C:\Program Files\Java\jdk-22" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-22"
)
if exist "%LOCALAPPDATA%\Android\Sdk" (
    set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
)
call "%~dp0gradlew.bat" testDebugUnitTest assembleDebug
if %ERRORLEVEL% EQU 0 (
    copy /Y "%~dp0app\build\outputs\apk\debug\app-debug.apk" "%~dp0Assistente_Perito_Mobile.apk" >nul
    echo.
    echo [OK] APK compilato e pronto in:
    echo      %~dp0Assistente_Perito_Mobile.apk
) else (
    echo.
    echo [ERRORE] Compilazione fallita.
)
pause
