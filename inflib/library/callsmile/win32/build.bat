@rem *******************************************************************************
@rem  Copyright (c) 2000, 2005 IBM Corporation and others.
@rem  All rights reserved. This program and the accompanying materials
@rem  are made available under the terms of the Eclipse Public License v1.0
@rem  which accompanies this distribution, and is available at
@rem  http://www.eclipse.org/legal/epl-v10.html
@rem
@rem  Contributors:
@rem      IBM Corporation - initial API and implementation
@rem      Kevin Cornell (Rational Software Corporation)
@rem      keith cascio 20060316 (UCLA Automated Reasoning Group)
@rem **********************************************************************
@rem
@rem  Usage: sh build.sh [<optional switches>] [clean]
@rem
@rem    where the optional switches are:
@rem        -output <PROGRAM_OUTPUT>  - executable filename ("eclipse")
@rem        -os     <DEFAULT_OS>      - default Eclipse "-os" value (qnx)
@rem        -arch   <DEFAULT_OS_ARCH> - default Eclipse "-arch" value (x86)
@rem        -ws     <DEFAULT_WS>      - default Eclipse "-ws" value (photon)
@rem
@rem
@rem     This script can also be invoked with the "clean" argument.
@rem
@rem NOTE: The C compiler needs to be setup. This script has been
@rem       tested against Microsoft Visual C and C++ Compiler 6.0.
@rem
@rem Uncomment the lines below and edit MSVC_HOME to point to the
@rem correct root directory of the compiler installation, if you
@rem want this to be done by this script.
@rem
@rem ******
@echo off

vol >nul 2>&1
call mypaths.bat > nul 2>&1
if errorlevel 1 goto error_missing_mypaths

if not "%MSVC_HOME%" == "" goto MAKE
set MSVC_HOME=k:\dev\products\msvc60\vc98
call %MSVC_HOME%\bin\vcvars32.bat
if not "%mssdk%" == "" goto MAKE
set mssdk=K:\dev\PRODUCTS\PLATSDK\feb2003
call %mssdk%\setenv.bat

:MAKE

rem --------------------------
rem Define default values for environment variables used in the makefiles.
rem --------------------------
set programOutput=callsmile.dll
set defaultOS=win32
set defaultOSArch=x86
set defaultWS=win32
set makefile=make_win32.mak
set OS=Windows
rem author keith cascio, since 20060117
rem define environment variable "NODEBUG" to prevent nmake from linking debug libraries, as described in Win32.mak line 43, included from NtWin32.mak, in Microsoft SDK \include directory
set NODEBUG=1

rem --------------------------
rem Parse the command line arguments and override the default values.
rem --------------------------
set extraArgs=
:WHILE
if "%1" == "" goto WHILE_END
    if "%2" == ""       goto LAST_ARG

    if "%1" == "-os" (
		set defaultOS=%2
		shift
		goto NEXT )
    if "%1" == "-arch" (
		set defaultOSArch=%2
		shift
		goto NEXT )
    if "%1" == "-ws" (
		set defaultWS=%2
		shift
		goto NEXT )
    if "%1" == "-output" (
		set programOutput=%2
		shift
		goto NEXT )

:LAST_ARG
        set extraArgs=%extraArgs% %1

:NEXT
    shift
    goto WHILE
:WHILE_END

rem --------------------------
rem Set up environment variables needed by the makefile.
rem --------------------------
set PROGRAM_OUTPUT=%programOutput%
set DEFAULT_OS=%defaultOS%
set DEFAULT_OS_ARCH=%defaultOSArch%
set DEFAULT_WS=%defaultWS%
set OUTPUT_DIR=.

rem --------------------------
rem Run nmake to build the executable.
rem --------------------------
if "%extraArgs%" == "" goto MAKE_ALL

nmake -f %makefile% %extraArgs%
goto :EOF

:MAKE_ALL
echo Building %OS% UPitt "SMILE" JNI DLL. Defaults: -os %DEFAULT_OS% -arch %DEFAULT_OS_ARCH% -ws %DEFAULT_WS%
nmake -f %makefile% clean
nmake -f %makefile% %1 %2 %3 %4
goto :EOF

:error_missing_mypaths
  echo Error: unable to find or execute mypaths.bat
goto :EOF
