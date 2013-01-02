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
    JAVACFLAGS=-Xlint:unchecked
    ifeq (x, x$(ARCH))
        ifeq (x, x$(PROCESSOR_ARCHITEW6432))
            ARCH=$(PROCESSOR_ARCHITECTURE)
        else
            ARCH=$(PROCESSOR_ARCHITEW6432)
        endif
    endif
else
    OS=$(shell uname)
    CLN=:
    JAVACFLAGS=-Xlint:unchecked -XDignore.symbol.file=true -Xbootclasspath/p:$(JAXB_HOME)/lib/jaxb-api.jar:$(JAXB_HOME)/lib/jaxb-impl.jar
    ARCH=$(shell uname -p)
endif

ifeq (Linux, $(findstring Linux,$(OS)))
    PLATFORM=linux
endif

NULL:=
SPACE:=$(NULL) # end of the line
SHELL=/bin/sh
CWD=$(shell pwd)

JAVA=$(JAVA_HOME)/bin/java
JAVA_VERSION=1.6
ifeq (1.7, $(findstring 1.7,`$(JAVA) -version`))
    JAVA_VERSION=1.7
endif
ifeq (x, x$(JRE_HOME))
    JRE_HOME=$(JAVA_HOME)/jre
endif
JRE=$(JRE_HOME)/bin/java

XJC=$(JAVA) -jar $(JAXB_HOME)/lib/jaxb-xjc.jar
XJCFLAGS=-enableIntrospection
JAVAC=$(JAVA_HOME)/bin/javac
JAR=$(JAVA_HOME)/bin/jar
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
JOVAL_CORE=$(COMPONENTS)/engine
JOVAL_CORE_LIB=$(JOVAL_CORE)/jOVALCore.jar
JOVAL_CORE_DEPS=$(subst $(SPACE),$(CLN),$(filter %.jar %.zip, $(wildcard $(JOVAL_CORE)/$(LIBDIR)/*)))
PLUGIN_LOCAL=$(COMPONENTS)/plugin
PLUGIN_LOCAL_LIB=$(PLUGIN_LOCAL)/jOVALPluginLocal.jar
PLUGIN_LOCAL_DEPS=$(subst $(SPACE),$(CLN),$(filter %.jar %.zip, $(wildcard $(PLUGIN_LOCAL)/$(LIBDIR)/*)))
XPERT=$(COMPONENTS)/xpert
XPERT_LIB=$(XPERT)/XPERT.jar
SCE=$(COMPONENTS)/sce
SCE_LIB=$(SCE)/sce-schema-$(SCE_VERSION).jar
