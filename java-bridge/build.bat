@echo off
setlocal

echo ==========================================================
echo        Building Creo MCP Java Bridge (Java 17)
echo ==========================================================

if not exist "bin" mkdir bin

set CP=lib\*

echo Compiling Java source files...
javac --release 17 -encoding UTF-8 -cp "%CP%" -d bin src\com\creomcp\bridge\util\*.java src\com\creomcp\bridge\handler\*.java src\com\creomcp\bridge\*.java

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [SUCCESS] Compilation finished successfully. Bytecode written to bin\
endlocal
