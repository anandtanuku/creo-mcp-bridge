@echo off
setlocal
echo ==========================================================
echo        Creo MCP One-Click Setup for New PC
echo ==========================================================

:: 1. Compile Java Bridge
echo [1/2] Compiling Java Bridge...
cd java-bridge
call build.bat
cd ..

:: 2. Setup Python Virtual Environment in mcp-server
echo.
echo [2/2] Configuring Python Virtual Environment...
cd mcp-server
if not exist ".venv" (
    echo Creating isolated virtual environment in mcp-server\.venv ...
    python -m venv .venv
)
echo Installing dependencies into .venv ...
call .venv\Scripts\pip install -r requirements.txt
cd ..

echo.
echo ==========================================================
echo [SUCCESS] Setup complete!
echo To run:
echo   Step 1: run java-bridge\run_bridge.bat
echo   Step 2: run mcp-server\run_mcp.bat
echo ==========================================================
endlocal

