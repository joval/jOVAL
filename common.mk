# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

include $(TOP)/customize.mk

JOVAL_VERSION=5.10.1.1_Dev
SCAP_VERSION=1.2
OVAL_VERSION=5.10.1
SCE_VERSION=1.0

Default: all

PLATFORM=unknown
ifeq (Windows, $(findstring Windows,$(OS)))
    PLATFORM=win
    CLN=;
else
    OS=$(shell uname)
    CLN=:
endif

ifeq (Linux, $(findstring Linux,$(OS)))
    PLATFORM=linux
endif

NULL:=
SPACE:=$(NULL) # end of the line
SHELL=/bin/sh
CWD=$(shell pwd)

ifeq (x86, $(ARCH))
    JRE=$(JRE32_HOME)/bin/java
else
    JRE=$(JRE64_HOME)/bin/java
endif
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
SVRL=$(COMPONENTS)/schematron/schema/svrl.jar
SCAP=$(COMPONENTS)/scap
SCAP_LIB=$(SCAP)/scap-schema-$(SCAP_VERSION).jar
SDK=$(COMPONENTS)/sdk
COMMON=$(SDK)/common
COMMON_LIB=$(SDK)/common/jOVALCommon.jar
COMMON_DEPS=$(subst $(SPACE),$(CLN),$(filter %.jar %.zip, $(wildcard $(COMMON)/$(LIBDIR)/*)))
JOVAL_CORE=$(SDK)/engine
JOVAL_CORE_LIB=$(JOVAL_CORE)/jOVALCore.jar
ADAPTERS=$(SDK)/adapters
ADAPTERS_LIB=$(ADAPTERS)/jOVALAdapters.jar
ADAPTERS_DEPS=$(subst $(SPACE),$(CLN),$(filter %.jar %.zip, $(wildcard $(ADAPTERS)/$(LIBDIR)/*)))
PLUGIN_REMOTE=$(SDK)/plugin/remote
PLUGIN_REMOTE_LIB=$(PLUGIN_REMOTE)/jOVALPluginRemote.jar
PLUGIN_REMOTE_DEPS=$(subst $(SPACE),$(CLN),$(filter %.jar %.zip, $(wildcard $(PLUGIN_REMOTE)/$(LIBDIR)/*)))
PLUGIN_LOCAL=$(SDK)/plugin/local
PLUGIN_LOCAL_LIB=$(PLUGIN_LOCAL)/jOVALPluginLocal.jar
PLUGIN_LOCAL_DEPS=$(subst $(SPACE),$(CLN),$(filter %.jar %.zip, $(wildcard $(PLUGIN_LOCAL)/$(LIBDIR)/*)))
PLUGIN_OFFLINE=$(SDK)/plugin/offline
PLUGIN_OFFLINE_LIB=$(PLUGIN_OFFLINE)/jOVALPluginOffline.jar
PLUGIN_OFFLINE_DEPS=$(subst $(SPACE),$(CLN),$(filter %.jar %.zip, $(wildcard $(PLUGIN_OFFLINE)/$(LIBDIR)/*)))
XPERT=$(COMPONENTS)/xpert
XPERT_LIB=$(XPERT)/XPERT.jar
SCE=$(COMPONENTS)/sce
SCE_LIB=$(SCE)/sce-schema-$(SCE_VERSION).jar
