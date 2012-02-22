#!/bin/sh
# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
export INSTALL_DIR=`dirname ${0}`
export LIB=${INSTALL_DIR}/lib
if [ "x${JAVA_HOME}" == x ]; then
    export JAVA_HOME=${INSTALL_DIR}/jre
fi
export JMEM=-Xmx2048m
${JAVA_HOME}/bin/java ${JMEM} "-Djovaldi.baseDir=${INSTALL_DIR}" -cp ${LIB}/jovaldi.jar:${LIB}/oval-schema-5.10.1.jar:${LIB}/jOVALCore.jar:${LIB}/cal10n-api-0.7.4.jar:${LIB}/slf4j-api-1.6.2.jar:${LIB}/slf4j-ext-1.6.2.jar:${LIB}/slf4j-jdk14-1.6.2.jar:${LIB}/svrl.jar org.joval.oval.di.Main "$@"
