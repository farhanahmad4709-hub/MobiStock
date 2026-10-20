@REM ----------------------------------------------------------------------------
@REM  Maven Wrapper startup batch script for Windows
@REM ----------------------------------------------------------------------------
@REM  Required ENV vars:
@REM    JAVA_HOME - location of a JDK home dir
@REM ----------------------------------------------------------------------------

@echo off
setlocal

set MAVEN_PROJECTBASEDIR=%~dp0
set MAVEN_WRAPPER_PROPERTIES="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties"

@REM Check if maven-wrapper.jar exists, download if not
set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"

if not exist %WRAPPER_JAR% (
    echo Downloading Maven Wrapper...
    powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar' -OutFile '%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar'}"
)

@REM Check for JAVA_HOME
if not "%JAVA_HOME%"=="" goto OkJHome
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.
goto error

:OkJHome
set MAVEN_CMD="%JAVA_HOME%\bin\java.exe"

@REM Download Maven if wrapper jar doesn't include it
for /f "tokens=1,2 delims==" %%a in ('findstr "distributionUrl" %MAVEN_WRAPPER_PROPERTIES%') do set MAVEN_DIST_URL=%%b

set MAVEN_HOME=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\dists\apache-maven-3.9.6

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Downloading Apache Maven 3.9.6...
    mkdir "%MAVEN_HOME%" 2>nul
    powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $url='https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip'; $out='%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\dists\maven.zip'; Invoke-WebRequest -Uri $url -OutFile $out; Expand-Archive -Path $out -DestinationPath '%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\dists' -Force; Remove-Item $out}"
    @REM Maven extracts into a subfolder
    if exist "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\dists\apache-maven-3.9.6" (
        set MAVEN_HOME=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\dists\apache-maven-3.9.6
    )
)

"%MAVEN_HOME%\bin\mvn.cmd" %*

:error
endlocal
