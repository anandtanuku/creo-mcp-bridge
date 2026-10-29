@echo off
setlocal

echo ==========================================================
echo        Starting Creo MCP Java Bridge Server
echo ==========================================================

:: 1. Auto-detect Creo Common Files directory if not explicitly set
if "%CREO_COMMON%"=="" (
    for /d %%D in ("C:\Program Files\PTC\Creo*") do (
        if exist "%%D\Common Files\x86e_win64\lib\pfcasyncmt.dll" (
            set "CREO_COMMON=%%D\Common Files"
        )
    )
)

:: Fallback if auto-detect didn't find it
if "%CREO_COMMON%"=="" (
    set "CREO_COMMON=C:\Program Files\PTC\Creo 9.0.10.0\Common Files"
)

set CREO_ENV=x86e_win64

echo Using Creo Directory: "%CREO_COMMON%"

set PATH=%CREO_COMMON%\%CREO_ENV%\lib;%CREO_COMMON%\%CREO_ENV%\obj;%PATH%
set PRO_COMM_MSG_EXE=%CREO_COMMON%\%CREO_ENV%\obj\pro_comm_msg.exe

set CP=bin;lib\*

java -cp "%CP%" com.creomcp.bridge.MainBridgeServer %*

endlocal
