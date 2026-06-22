@echo off
chcp 65001 >nul
setlocal

set "ROOT_DIR=%~dp0"
set "CLIENT_DIR=%ROOT_DIR%client"

echo ============================================
echo  WorldCup Management Frontend
echo  URL: http://127.0.0.1:5173
echo  Only frontend will start. Backend is not started.
echo  Close this window to stop the frontend service.
echo ============================================
echo.

if not exist "%CLIENT_DIR%\package.json" (
  echo [ERROR] package.json not found: %CLIENT_DIR%\package.json
  echo Please place this script in the project root directory.
  pause
  exit /b 1
)

where node >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Node.js was not found. Please install Node.js first.
  pause
  exit /b 1
)

where npm >nul 2>nul
if errorlevel 1 (
  echo [ERROR] npm was not found. Please install Node.js/npm first.
  pause
  exit /b 1
)

pushd "%CLIENT_DIR%"

if not exist "node_modules" (
  echo [INIT] node_modules not found. Installing frontend dependencies...
  call npm install
  if errorlevel 1 (
    echo [ERROR] npm install failed.
    popd
    pause
    exit /b 1
  )
  echo.
)

echo [START] Starting frontend dev server...
echo Open: http://127.0.0.1:5173
echo.
call npm run dev
set "EXIT_CODE=%ERRORLEVEL%"

popd
echo.
echo Frontend process exited with code %EXIT_CODE%.
pause
exit /b %EXIT_CODE%