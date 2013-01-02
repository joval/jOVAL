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
#JAVA_HOME=$(TOP)/../../tools/jdk1.6.0_25
JAVA_HOME=$(TOP)/../../tools/jdk1.7.0_03

#
# JAXB_HOME is where you've installed JAXB
#
JAXB_HOME=$(TOP)/../../tools/jaxb-ri-2.2.6

#
# ARCH defines the distribution target architecture.  It should correspond to your platform
# architecture. Left unset, it will be determined automatically.
#
#ARCH=x86
#ARCH=AMD64
#ARCH=x86_64

#
# JRE_HOME is the install path for the JRE that will be bundled with the distribution. Left
# unset, the JRE included with the JDK will be used. If you are overriding ARCH, you will
# need to set this to a JRE of the desired architecture.
#
#JRE_HOME=$(TOP)/../../tools/jre170_03_x86
