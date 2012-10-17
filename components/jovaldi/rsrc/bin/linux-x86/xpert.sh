#!/bin/sh
# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
export INSTALL_DIR=`dirname ${0}`
export LIB=${INSTALL_DIR}/lib
export PLUGIN=${INSTALL_DIR}/plugin
if [ "x${JAVA_HOME}" == x ]; then
    export JAVA_HOME=${INSTALL_DIR}/jre
fi
export JMEM=-Xmx1024m
${JAVA_HOME}/bin/java ${JMEM} "-Dxpert.baseDir=${INSTALL_DIR}" -cp "${LIB}/*" org.joval.scap.xccdf.engine.XPERT "$@"
