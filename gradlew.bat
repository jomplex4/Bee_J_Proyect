@echo off
setlocal
where gradle >nul 2>nul
if %ERRORLEVEL%==0 (
  gradle %*
  exit /b %ERRORLEVEL%
)
set "ROOT=%~dp0"
set "DIST=%ROOT%.gradle-bootstrap\gradle-8.13\bin\gradle.bat"
if not exist "%DIST%" (
  if not exist "%ROOT%.gradle-bootstrap" mkdir "%ROOT%.gradle-bootstrap"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.13-bin.zip' -OutFile '%ROOT%.gradle-bootstrap\gradle.zip'; Expand-Archive -Force '%ROOT%.gradle-bootstrap\gradle.zip' '%ROOT%.gradle-bootstrap'; Remove-Item '%ROOT%.gradle-bootstrap\gradle.zip'"
  if errorlevel 1 exit /b 1
)
call "%DIST%" %*
exit /b %ERRORLEVEL%
