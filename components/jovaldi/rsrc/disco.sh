#!/bin/sh
# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
if [ "x${JAVA_HOME}" == x ]; then
    export JAVA_HOME=./jre
fi
if [ `uname -p` == x86_64 ]; then
    export JMEM=-Xmx2048m
else
    export JMEM=-Xmx1024m
fi
${JAVA_HOME}/bin/java ${JMEM} -cp lib/jovaldi.jar:lib/oval-schema-5.10.1.jar:lib/jOVALCore.jar:lib/cal10n-api-0.7.4.jar:lib/slf4j-api-1.6.2.jar:lib/slf4j-ext-1.6.2.jar:lib/slf4j-jdk14-1.6.2.jar:plugin/cisco/lib/jOVALPluginShared.jar:plugin/cisco/lib/jOVALPluginCisco.jar org.joval.plugin.CiscoPlugin "$@"
