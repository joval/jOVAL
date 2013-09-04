# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
# INSTRUCTIONS:
#
# This file is intended to be modified to suit your build environment.
# $(TOP) is defined to be this directory -- the top-level directory for the jOVAL source tree.
#

#
# JAVA_HOME is where you've installed your JDK.
#
#JAVA_HOME=$(TOP)/../../tools/jdk1.7.0_21
JAVA_HOME=$(TOP)/../../tools/jdk1.6.0_26

#
# JSAF_HOME is the location of the jSAF FOSS git repository
# See http://github.org/joval/jSAF
#
JSAF_HOME=$(TOP)/../jSAF

#
# JPE_HOME is the location of the jPE FOSS git repository
# See http://github.org/joval/jPE
#
JPE_HOME=$(TOP)/../jPE

#
# JRE[ARCH]_HOME is the install path for the JRE that will be bundled with the distribution of
# architecture ARCH.
#
JRE32_HOME=$(TOP)/../../tools/jre160_31_x86
JRE64_HOME=$(TOP)/../../tools/jre160_31_x64
#JRE64_HOME=$(JAVA_HOME)/jre

#
# JAXB_HOME is where you've installed JAXB
# Note: This is only required when building with a pre-Java7 JDK
#
JAXB_HOME=$(TOP)/../../tools/jaxb-ri-2.2.6
