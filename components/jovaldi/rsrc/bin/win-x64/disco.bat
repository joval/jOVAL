@REM Copyright (c) 2011 jOVAL.org.  All rights reserved.
@REM This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
@ECHO OFF

SET INSTALL_DIR=%~dp0
SET LIB=%INSTALL_DIR%lib
SET PLUGIN=%INSTALL_DIR%plugin
IF NOT DEFINED JAVA_HOME SET JAVA_HOME=%INSTALL_DIR%jre
SET JMEM=-Xmx2048m
"%JAVA_HOME%\bin\java" %JMEM% -cp "%LIB%\*";"%PLUGIN%\offline\lib\*" org.joval.plugin.OfflinePlugin %*
