@echo off
setlocal enabledelayedexpansion

rem set DCC_HOME to <this-folder> in following path "<this-folder>\bin\shell.bat"
for %%? in ("%~dp0..") do set DCC_HOME=%%~f?
rem echo Resolved DCC_HOME: '%DCC_HOME%'

cd /D "%DCC_HOME%"

rem Build a classpath containing all jars
rem the following lines are working because the current dir is changed to DCC_HOME
for %%a in ("lib\*.jar") do set DCC_CP=!DCC_CP!%%a;
set DCC_CP=config;%DCC_CP%

java -Xmx1024m -Dflash.message.disabled=false -Duser.language=en -Djline.nobell=true -Droo.console.ansi=true -cp "%DCC_CP%" org.metaeffekt.dcc.shell.DccShell %*
set SHELL_EXIT_CODE = %ERRORLEVEL%

:end
exit /B %SHELL_EXIT_CODE%