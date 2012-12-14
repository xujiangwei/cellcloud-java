@echo off
if "%OS%" == "Windows_NT" setlocal
rem ---------------------------------------------------------------------------
rem Start script for the Cell Cloud
rem ---------------------------------------------------------------------------

rem Guess CC_HOME if not defined
set CURRENT_DIR=%cd%
if not "%CC_HOME%" == "" goto gotHome
set CC_HOME=%CURRENT_DIR%
if exist "%CC_HOME%\bin\cell.cmd" goto okHome
cd .
set CC_HOME=%cd%
cd %CURRENT_DIR%
:gotHome
if exist "%CC_HOME%\bin\cell.cmd" goto okHome
echo The CC_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end
:okHome

set EXECUTABLE=%CC_HOME%\bin\cell.cmd

rem Check that target executable exists
if exist "%EXECUTABLE%" goto okExec
echo Cannot find %EXECUTABLE%
echo This file is needed to run this program
goto end
:okExec

rem Get remaining unshifted command line arguments and save them in the
set CMD_LINE_ARGS=
:setArgs
if ""%1""=="""" goto doneSetArgs
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setArgs
:doneSetArgs

call "%EXECUTABLE%" start %CMD_LINE_ARGS%

:end
