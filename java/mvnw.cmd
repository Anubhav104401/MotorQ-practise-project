@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM ----------------------------------------------------------------------------
@echo off
setlocal

where mvn >nul 2>nul
if %ERRORLEVEL% equ 0 (
    mvn %*
    exit /b %ERRORLEVEL%
)

set "FALLBACK_MVN=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.16\56ba1f9f\bin\mvn.cmd"
if exist "%FALLBACK_MVN%" (
    call "%FALLBACK_MVN%" %*
    exit /b %ERRORLEVEL%
)

echo "mvn" was not found in PATH or at %FALLBACK_MVN%
exit /b 1
