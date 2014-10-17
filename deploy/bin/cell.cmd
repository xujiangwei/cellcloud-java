@echo off
setlocal enabledelayedexpansion
if "%OS%" == "Windows_NT" setlocal
rem ---------------------------------------------------------------------------
rem Start/Stop Script for the Cell Cloud
rem
rem Environment Variable Prequisites
rem
rem   CC_HOME      May point at your CC "build" directory.
rem
rem   CC_BASE      (Optional) Base directory for resolving dynamic portions
rem                of a CC installation.  If not present, resolves to
rem                the same directory that CC_HOME points to.
rem
rem   JAVA_HOME    Must point at your Java Development Kit installation.
rem                Required to run the with the "debug" argument.
rem
rem   JRE_HOME     Must point at your Java Runtime installation.
rem                Defaults to JAVA_HOME if empty.
rem
rem   JAVA_OPTS    (Optional) Java runtime options used when the "start",
rem                "stop", or "run" command is executed.
rem ---------------------------------------------------------------------------

rem Guess CC_HOME if not defined
set CURRENT_DIR=%cd%
if not "%CC_HOME%" == "" goto gotHome
set CC_HOME=%CURRENT_DIR%
if exist "%CC_HOME%\bin\cell.cmd" goto okHome
cd ..
set CC_HOME=%cd%
cd %CURRENT_DIR%
:gotHome
if exist "%CC_HOME%\bin\cell.cmd" goto okHome
echo The CC_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end
:okHome

rem Get standard environment variables
if "%CC_BASE%" == "" goto gotSetenvHome
if exist "%CC_BASE%\bin\setenv.bat" call "%CC_BASE%\bin\setenv.bat"
goto gotSetenvBase
:gotSetenvHome
if exist "%CC_HOME%\bin\setenv.bat" call "%CC_HOME%\bin\setenv.bat"
:gotSetenvBase

rem Get standard Java environment variables
if exist "%CC_HOME%\bin\setclasspath.cmd" goto okSetclasspath
echo Cannot find %CC_HOME%\bin\setclasspath.cmd
echo This file is needed to run this program
goto end
:okSetclasspath
set BASEDIR=%CC_HOME%
call "%CC_HOME%\bin\setclasspath.cmd" %1
if errorlevel 1 goto end

set CLASSPATH=%CLASSPATH%;%CC_HOME%\bin\nucleus-1.0.0.jar;%CC_HOME%\bin\cell-1.0.0.jar
for /r %CC_HOME%\libs\ %%i in (*.jar) do (
set file=%%i
set CLASSPATH=!CLASSPATH!;!file!
)

if not "%CC_BASE%" == "" goto gotBase
set CC_BASE=%CC_HOME%
:gotBase

rem ----- Execute The Requested Command ---------------------------------------

echo Using CC_BASE:   %CC_BASE%
echo Using CC_HOME:   %CC_HOME%
if ""%1"" == ""debug"" goto use_jdk
echo Using JRE_HOME:  %JRE_HOME%
goto java_dir_displayed
:use_jdk
echo Using JAVA_HOME:   %JAVA_HOME%
:java_dir_displayed

set _EXECJAVA=%_RUNJAVA%
set MAINCLASS=net.cellcloud.cell.Cell
set ACTION=start
set JAVA_OPTS="-server"
REM set JAVA_OPTS="-server -verbose:gc -Xms1024m -Xmx1024m -Xmn340m -Xss128k -XX:PermSize=128m -XX:MaxPermSize=128m -XX:SurvivorRatio=8 -XX:MaxTenuringThreshold=7 -XX:GCTimeRatio=19 -XX:+DisableExplicitGC -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -XX:+UseCMSCompactAtFullCollection -XX:-CMSParallelRemarkEnabled -XX:CMSInitiatingOccupancyFraction=70 -XX:SoftRefLRUPolicyMSPerMB=0 -XX:CMSFullGCsBeforeCompaction=10 -XX:+UseAdaptiveSizePolicy"

if ""%1"" == ""start"" goto doStart
if ""%1"" == ""stop"" goto doStop
if ""%1"" == ""version"" goto doVersion

echo Usage:  cell ( commands ... )
echo commands:
echo   start             Start Cell in a separate window
echo   stop              Stop Cell
echo   version           What version of Cell are you running
goto end


:doStart
shift
if not "%OS%" == "Windows_NT" goto noTitle
set _EXECJAVA=start "Cell Cloud" %_RUNJAVA%
goto gotTitle
:noTitle
set _EXECJAVA=start %_RUNJAVA%
:gotTitle
goto execCmd

:doStop
shift
set ACTION=stop
goto execCmd

:doVersion
%_EXECJAVA% -classpath "%CC_HOME%\bin\cell-1.0.0.jar" net.cellcloud.cell.VersionInfo
goto end


:execCmd
rem Get remaining unshifted command line arguments and save them in the
set CMD_LINE_ARGS=
:setArgs
if ""%1""=="""" goto doneSetArgs
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setArgs
:doneSetArgs

rem Execute Java with the applicable properties
%_EXECJAVA% %JAVA_OPTS% -classpath "%CLASSPATH%" %MAINCLASS% %CMD_LINE_ARGS% %ACTION%
goto end

:end
