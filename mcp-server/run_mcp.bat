@echo off
setlocal

set PYTHONPATH=%~dp0;%PYTHONPATH%

if exist "%~dp0.venv\Scripts\python.exe" (
    "%~dp0.venv\Scripts\python.exe" -m creo_mcp.server
) else (
    python -m creo_mcp.server
)

endlocal
