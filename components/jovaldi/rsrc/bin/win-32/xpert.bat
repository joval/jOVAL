@REM Copyright (c) 2012 jOVAL.org.  All rights reserved.
@REM This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
@ECHO OFF

SET INSTALL_DIR=%~dp0
SET LIB=%INSTALL_DIR%lib
SET PLUGIN=%INSTALL_DIR%plugin
IF NOT DEFINED JAVA_HOME SET JAVA_HOME=%INSTALL_DIR%jre
SET INSTALL_DIR=%INSTALL_DIR:~0,-1%
SET JMEM=-Xmx1024m
"%JAVA_HOME%\bin\java" %JMEM%  -Djava.library.path="%PLUGIN%\default\lib" -Dxpert.baseDir="%INSTALL_DIR%" -cp "%LIB%\*;" org.joval.scap.xccdf.engine.XPERT %*
