@echo off
setlocal enabledelayedexpansion

:: --- Configuration ---
set "JSON_FILE=dependencies.json"

:: --- 1. PREREQUISITES CHECK ---
if not exist "%JSON_FILE%" (
    echo Error: %JSON_FILE% not found in current directory.
    exit /b 1
)

:: --- 2. ARGUMENT PARSING ---
set "COMMAND=%~1"
set "ROOT_DIR=%~2"
set "SKIP_EXISTING=false"
set "FORCE_IF_EXISTING=false"

:: Check Command
if /i "%COMMAND%" neq "clone" if /i "%COMMAND%" neq "build" (
    echo Error: First argument must be 'clone' or 'build'.
    echo Usage: %~nx0 [clone^|build] [target_directory] [-s^|-f]
    exit /b 1
)

:: Check Root Dir
if "%ROOT_DIR%"=="" (
    echo Error: Second argument must be the target root directory.
    exit /b 1
)

:: Check Optional Flags
if /i "%~3"=="-s" set "SKIP_EXISTING=true"
if /i "%~3"=="--skip-existing" set "SKIP_EXISTING=true"
if /i "%~3"=="-f" set "FORCE_IF_EXISTING=true"
if /i "%~3"=="--force-checkout" set "FORCE_IF_EXISTING=true"

:: --- 3. PREPARE DIRECTORY ---
:: Get absolute path for JSON file
for %%i in ("%JSON_FILE%") do set "ABS_JSON_PATH=%%~fvi"

echo Mode: %COMMAND%
echo Target: %ROOT_DIR%
echo Skip Existing: %SKIP_EXISTING%
echo Force checkout if Existing: %FORCE_IF_EXISTING%
echo -------------------------------------

:: Force Flag Verification
if "%FORCE_IF_EXISTING%"=="true" (
    echo You activated -f force flag
    set /p "confirm=Are you sure you want to force checkout in existing folder? [y/N]: "
    if /i "!confirm!" neq "y" (
        echo Action cancelled.
        exit /b 0
    )
    echo Proceeding...
)

:: Create root dir if it doesn't exist
if not exist "%ROOT_DIR%" (
    echo Creating directory %ROOT_DIR%...
    mkdir "%ROOT_DIR%"
)

:: Move into the root directory
cd /d "%ROOT_DIR%" || exit /b 1

:: --- 4. MAIN LOOP ---
:: Get total count of dependencies
for /f "tokens=*" %%a in ('jq ".dependencies | length" "%ABS_JSON_PATH%"') do set "COUNT=%%a"
set /a "MAX_INDEX=%COUNT% - 1"

for /l %%i in (0,1,%MAX_INDEX%) do (
    :: Extract info using jq
    for /f "tokens=*" %%a in ('jq -r ".dependencies[%%i].name" "%ABS_JSON_PATH%"') do set "LABEL=%%a"
    for /f "tokens=*" %%a in ('jq -r ".dependencies[%%i].repository" "%ABS_JSON_PATH%"') do set "REPO=%%a"
    for /f "tokens=*" %%a in ('jq -r ".dependencies[%%i].sha1" "%ABS_JSON_PATH%"') do set "SHA1=%%a"
    for /f "tokens=*" %%a in ('jq -r ".dependencies[%%i].cmd" "%ABS_JSON_PATH%"') do set "B_CMD=%%a"

    :: Extract folder name from REPO URL
    for %%g in ("!REPO!") do set "DIR_NAME=%%~nxg"

    echo Processing: !LABEL! (!DIR_NAME!)

    :: --- EXISTENCE CHECK ---
    set "PROCEED=true"
    if exist "!DIR_NAME!" (
        if "%FORCE_IF_EXISTING%"=="true" (
            echo   ! Directory '!DIR_NAME!' exists. Forcing checkout.
        ) else if "%SKIP_EXISTING%"=="true" (
            echo   ! Directory '!DIR_NAME!' exists. Skipping.
            set "PROCEED=false"
        ) else (
            echo   X Error: Directory '!DIR_NAME!' already exists and skip flag is OFF.
            exit /b 1
        )
    ) else (
        echo   - Cloning !REPO!...
        git clone "!REPO!" "!DIR_NAME!" || (echo X Clone failed & exit /b 1)
    )

    if "!PROCEED!"=="true" (
        cd "!DIR_NAME!" || exit /b 1

        :: --- CHECKOUT ---
        echo   - Checking out !SHA1!...
        if "%FORCE_IF_EXISTING%"=="true" (
            git checkout -f "!SHA1!" >nul 2>&1
        ) else (
            git checkout "!SHA1!" >nul 2>&1
        )

        if errorlevel 1 (
            echo   X Error: Failed to checkout commit !SHA1!
            exit /b 1
        )

        :: --- BUILD ---
        if /i "%COMMAND%"=="build" (
            echo   - Running build: !B_CMD!
            call !B_CMD!
            if errorlevel 1 (
                echo   X Error: Build failed for !LABEL!
                exit /b 1
            )
            echo   - Build successful.
        )

        cd ..
    )
    echo -------------------------------------
)

echo All tasks completed.
pause