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
JAVA_HOME=$(TOP)/../../tools/jdk160_31

#
# ARCH defines the default distribution target architecture.  It should correspond to your
# platform architecture.
#
#ARCH=x86
ARCH=x64

#
# JRE32_HOME is the install path for a 32-bit JRE.
# This does not have to be set if you will never create a 32-bit distribution.
#
JRE32_HOME=$(TOP)/../../tools/jre160_31_x86

#
# JRE64_HOME is the install path for a 64-bit JRE.
# This does not have to be set if you will never create a 64-bit distribution.
#
JRE64_HOME=$(TOP)/../../tools/jre160_31_x64
