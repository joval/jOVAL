# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

Default: all

ifeq (cygwin, $(findstring cygwin,$(SHELL)))
    JAVA_HOME=$(TOP)/../../tools/jdk160_26
endif

ifndef JAVA_HOME
    JAVA_HOME=~/tools/jdk160_26
endif

ifeq (Windows, $(findstring Windows,$(OS)))
    CLN=;
else
    CLN=:
endif

NULL:=
SPACE:=$(NULL) # end of the line
SHELL=/bin/sh
CWD=$(shell pwd)

JOVAL_VERSION=5.10.1.0
OVAL_VERSION=5.10.1
XCCDF_VERSION=1.1
CPE_VERSION=2.3

# If your system is 32-bit, set ARCH to x86
#ARCH=x86
ARCH=x64
JRE_HOME=$(TOP)/../../tools/jre160_27
JRE=$(JRE_HOME)/$(ARCH)/bin/java
JAVA=$(JAVA_HOME)/bin/java
JAVAC=$(JAVA_HOME)/bin/javac
JAR=$(JAVA_HOME)/bin/jar
XJC=$(JAVA_HOME)/bin/xjc
JAVACFLAGS=-Xlint:unchecked -XDignore.symbol.file=true -deprecation
CLASSLIB=$(JAVA_HOME)/jre/lib/rt.jar
BUILD=build
DIST=dist
RSRC=rsrc
DOCS=docs/api
SRC=$(TOP)/src
COMPONENTS=$(TOP)/components
LIBDIR=$(RSRC)/lib
LIB=$(subst $(SPACE),$(CLN),$(filter %.jar %.zip, $(wildcard $(LIBDIR)/*)))
CPE=$(COMPONENTS)/cpe
CPE_LIB=$(CPE)/cpe-schema-$(CPE_VERSION).jar
OVAL=$(COMPONENTS)/oval
OVAL_LIB=$(OVAL)/oval-schema-$(OVAL_VERSION).jar
SVRL=$(COMPONENTS)/schematron/schema/svrl.jar
XCCDF=$(COMPONENTS)/xccdf
XCCDF_LIB=$(XCCDF)/xccdf-schema-$(XCCDF_VERSION).jar
SDK=$(COMPONENTS)/sdk
JOVAL_CORE=$(SDK)/engine
JOVAL_CORE_LIB=$(JOVAL_CORE)/jOVALCore.jar
JOVAL_CORE_DEPS=$(subst $(SPACE),$(CLN),$(filter %.jar %.zip, $(wildcard $(JOVAL_CORE)/$(LIBDIR)/*)))
PLUGIN_SHARED=$(SDK)/plugin/shared
PLUGIN_SHARED_LIB=$(PLUGIN_SHARED)/jOVALPluginShared.jar
PLUGIN_SHARED_DEPS=$(subst $(SPACE),$(CLN),$(filter %.jar %.zip, $(wildcard $(PLUGIN_SHARED)/$(LIBDIR)/*)))
PLUGIN_REMOTE=$(SDK)/plugin/remote
PLUGIN_REMOTE_LIB=$(PLUGIN_REMOTE)/jOVALPluginRemote.jar
PLUGIN_REMOTE_DEPS=$(subst $(SPACE),$(CLN),$(filter %.jar %.zip, $(wildcard $(PLUGIN_REMOTE)/$(LIBDIR)/*)))
PLUGIN_LOCAL=$(SDK)/plugin/local
PLUGIN_LOCAL_LIB=$(PLUGIN_LOCAL)/jOVALPluginLocal.jar
PLUGIN_LOCAL_DEPS=$(subst $(SPACE),$(CLN),$(filter %.jar %.zip, $(wildcard $(PLUGIN_LOCAL)/$(LIBDIR)/*)))
PLUGIN_CISCO=$(SDK)/plugin/cisco
PLUGIN_CISCO_LIB=$(PLUGIN_CISCO)/jOVALPluginCisco.jar
PLUGIN_CISCO_DEPS=$(subst $(SPACE),$(CLN),$(filter %.jar %.zip, $(wildcard $(PLUGIN_CISCO)/$(LIBDIR)/*)))
