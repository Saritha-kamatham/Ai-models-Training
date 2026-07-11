@echo off
@setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.

@REM Strip trailing backslash from DIRNAME to prevent escaping closing quotes in Java arguments
set "PROJECT_DIR=%DIRNAME%"
if "%PROJECT_DIR:~-1%" == "\" set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"

set WRAPPER_DIR_PATH=%PROJECT_DIR%\.mvn\wrapper
set WRAPPER_JAR_PATH=%WRAPPER_DIR_PATH%\maven-wrapper.jar

@REM Create the wrapper directory if not exists
if not exist "%WRAPPER_DIR_PATH%" (
    mkdir "%WRAPPER_DIR_PATH%"
)

@REM Download jar if not exists using curl
if not exist "%WRAPPER_JAR_PATH%" (
    echo Maven Wrapper jar not found. Attempting to download using curl...
    curl -fSLo "%WRAPPER_JAR_PATH%" "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
)

@REM Fallback to powershell if curl failed
if not exist "%WRAPPER_JAR_PATH%" (
    echo Curl download failed. Attempting download using powershell...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object Net.WebClient).DownloadFile('https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar', '%WRAPPER_JAR_PATH%')"
)

@REM Verify file download success
if not exist "%WRAPPER_JAR_PATH%" (
    echo.
    echo ERROR: Failed to download maven-wrapper.jar using both curl and powershell.
    echo.
    exit /B 1
)

@REM Configure Java path
if not "%JAVA_HOME%" == "" (
    set JAVA_EXE="%JAVA_HOME%\bin\java.exe"
) else (
    set JAVA_EXE=java.exe
)

@REM Verify Java runs
%JAVA_EXE% -version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ERROR: java.exe was not found in your PATH or JAVA_HOME. Please install Java.
    exit /B 1
)

@REM Run the wrapper main class
%JAVA_EXE% %MAVEN_OPTS% -classpath "%WRAPPER_JAR_PATH%" "-Dmaven.multiModuleProjectDirectory=%PROJECT_DIR%" org.apache.maven.wrapper.MavenWrapperMain %*
if %ERRORLEVEL% neq 0 (
    exit /B 1
)

@endlocal
