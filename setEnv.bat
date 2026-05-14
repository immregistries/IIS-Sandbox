@echo off
:: Configuration
set "ENV_FILE=.env"

:: Check if the file exists
if not exist "%ENV_FILE%" (
    echo [ERROR] %ENV_FILE% not found.
    exit /b 1
)

echo Loading environment variables from %ENV_FILE%...

:: Loop through the file
:: eol=#  -> Ignore lines starting with #
:: delims== -> Split the line at the = character
:: tokens=1* -> %%A gets the key, %%B gets everything after the first =
for /f "usebackq eol=# tokens=1* delims==" %%A in ("%ENV_FILE%") do (
    :: Trim and set the variable
    set "%%A=%%B"
    echo   Set: %%A
)

echo.
echo All variables from %ENV_FILE% have been set for this session.