@REM Copyright (c) 2011 jOVAL.org.  All rights reserved.
@REM This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
@ECHO OFF

SET INSTALL_DIR=%~dp0
SET LIB=%INSTALL_DIR%lib
SET PLUGIN=%INSTALL_DIR%plugin
IF NOT DEFINED JAVA_HOME SET JAVA_HOME=%INSTALL_DIR%jre
SET JMEM=-Xmx2048m
"%JAVA_HOME%\bin\java" %JMEM% -cp "%LIB%\jOVALCore.jar";"%LIB%\oval-schema-5.10.1.jar";"%LIB%\cal10n-api-0.7.4.jar";"%LIB%\slf4j-api-1.6.2.jar";"%LIB%\slf4j-ext-1.6.2.jar";"%LIB%\slf4j-jdk14-1.6.2.jar";"%PLUGIN%\cisco\lib\jOVALPluginShared.jar";"%PLUGIN%\cisco\lib\jOVALPluginCisco.jar";"%PLUGIN%\cisco\lib\tftp.jar" org.joval.plugin.CiscoPlugin %*
