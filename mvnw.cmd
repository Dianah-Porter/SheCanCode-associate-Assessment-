@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.2

@IF "%__MVNW_ARG0_NAME__%"=="" (SET "BASE_DIR=%~dp0")

@SET MAVEN_PROJECTBASEDIR=%MAVEN_BASEDIR%
@IF NOT "%MAVEN_PROJECTBASEDIR%"=="" GOTO endDetectBaseDir

@SET EXEC_DIR=%BASE_DIR%
@SET WDIR=%EXEC_DIR%
:findBaseDir
@IF EXIST "%WDIR%\.mvn" GOTO baseDirFound
@cd ..
@SET WDIR=%CD%
@GOTO findBaseDir
:baseDirFound
@SET MAVEN_PROJECTBASEDIR=%WDIR%
@cd "%EXEC_DIR%"
:endDetectBaseDir

@SET WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar
@SET WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties

@IF NOT EXIST "%WRAPPER_JAR%" (
  @FOR /F "usebackq tokens=2 delims==" %%i IN ("%WRAPPER_PROPERTIES%") DO (
    @IF NOT "%%i"=="" SET WRAPPER_URL=%%i
    @GOTO :foundWrapperUrl
  )
  :foundWrapperUrl
  @IF NOT "%WRAPPER_URL%"=="" (
    @powershell -Command "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%'"
  )
)

@IF DEFINED JAVA_HOME (
  @SET "JAVACMD=%JAVA_HOME%\bin\java.exe"
) ELSE (
  @SET "JAVACMD=java.exe"
)

@FOR /F "usebackq tokens=2 delims==" %%i IN (`findstr /i "distributionUrl" "%WRAPPER_PROPERTIES%"`) DO SET DISTRIBUTION_URL=%%i

@"%JAVACMD%" %MAVEN_OPTS% ^
  -classpath "%WRAPPER_JAR%" ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  org.apache.maven.wrapper.MavenWrapperMain ^
  "%DISTRIBUTION_URL%" %*
