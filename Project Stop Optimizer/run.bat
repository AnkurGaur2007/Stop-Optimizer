@echo off
setlocal
REM Smart City Route Navigator, one-click start.
REM Double-click this file. It checks Java, compiles the sources and opens the page.
REM No Maven, no Gradle, no downloaded dependencies.

REM 1. Always work from the folder this file sits in, even when its path has spaces.
cd /d "%~dp0"

REM 2. Port: default 8080, ROUTENAV_PORT overrides it, first argument wins.
set "PORT=8080"
if not "%ROUTENAV_PORT%"=="" set "PORT=%ROUTENAV_PORT%"
if not "%~1"=="" set "PORT=%~1"

echo.
echo ================================================
echo   Smart City Route Navigator
echo   http://localhost:%PORT%/
echo ================================================
echo.

REM 3. Java 17 or newer is required, and javac has to be there too.
set "JAVA_MAJOR="
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set "JAVA_MAJOR=%%~v"
if not defined JAVA_MAJOR goto no_java
for /f "tokens=1 delims=." %%m in ("%JAVA_MAJOR%") do set "JAVA_MAJOR=%%m"
if not defined JAVA_MAJOR goto no_java
if %JAVA_MAJOR% LSS 17 goto old_java
where javac >nul 2>&1
if errorlevel 1 goto no_jdk

REM 4. Refuse to start when something else already listens on that port.
set "IN_USE="
for /f "tokens=*" %%l in ('netstat -ano ^| findstr /i "LISTENING" ^| findstr /c:":%PORT% "') do set "IN_USE=1"
if defined IN_USE goto port_busy

REM 5. Compile every source file into out\. A fresh compile each time keeps
REM    the class files in step with the sources. javac expands the *.java
REM    wildcards itself, so the spaces in this project's path are no problem.
echo Compiling...
if exist out rmdir /s /q out
mkdir out
javac -encoding UTF-8 -d out src\routenav\*.java src\routenav\json\*.java src\routenav\server\*.java
if errorlevel 1 goto compile_failed
echo Compiled.
echo.

REM 6. A hidden PowerShell loop waits for the API, then opens the page once.
REM    It gives up after about a minute and leaves no window behind.
start "" /b powershell -NoProfile -WindowStyle Hidden -Command "$u='http://localhost:%PORT%/api/locations'; for ($i=0; $i -lt 60; $i++) { try { $r=Invoke-WebRequest -UseBasicParsing -Uri $u -TimeoutSec 2; if ($r.StatusCode -eq 200) { Start-Process 'http://localhost:%PORT%/'; exit 0 } } catch { Start-Sleep -Seconds 1 } }; exit 1"

REM 7. Start the server in this window, so closing the window stops it.
java -cp out routenav.Main %PORT%
set "APP_EXIT=%ERRORLEVEL%"

echo.
if "%APP_EXIT%"=="0" (
    echo The server has stopped.
) else (
    echo The app stopped with an error ^(exit code %APP_EXIT%^).
    echo Read the messages above.
)
pause
endlocal
exit /b %APP_EXIT%

:no_java
echo Java was not found.
echo Install JDK 17 or newer from https://adoptium.net and run this file again.
pause
exit /b 1

:old_java
echo Java %JAVA_MAJOR% was found, but this app needs JDK 17 or newer.
echo Get a newer JDK from https://adoptium.net and run this file again.
pause
exit /b 1

:no_jdk
echo javac was not found. You need a JDK, not only a JRE.
echo Install JDK 17 or newer from https://adoptium.net and run this file again.
pause
exit /b 1

:port_busy
echo Port %PORT% is already in use. Close the other program or run run.bat with another port,
echo for example: run.bat 8081
pause
exit /b 1

:compile_failed
echo The Java sources did not compile. The errors are above.
pause
exit /b 1
