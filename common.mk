# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

include $(TOP)/customize.mk

JOVAL_VERSION=5.11.DEV
SCAP_VERSION=1.2
OVAL_VERSION=5.11
SCE_VERSION=1.0

Default: all

PLATFORM=unknown
JAVA=$(JAVA_HOME)/bin/java
JAVA_VERSION=1.6
ifeq (1.7, $(findstring 1.7,`$(JAVA) -version`))
    XJC=$(JAVA_HOME)/bin/xjc
    JAVA_VERSION=1.7
else
    XJC=$(JAVA) -jar $(JAXB_HOME)/lib/jaxb-xjc.jar
endif
ifeq (Windows, $(findstring Windows,$(OS)))
  PLATFORM=win
  CLN=;
  JAVACFLAGS=-Xlint:unchecked
  ifeq (x, x$(ARCH))
    ifeq (x, x$(PROCESSOR_ARCHITEW6432))
      ifeq (x86, $(findstring x86,$(PROCESSOR_ARCHITECTURE)))
        ARCH=32
      else
        ARCH=64
      endif
    else
      ARCH=64
    endif
  endif
else
  OS=$(shell uname)
  CLN=:
  ifeq (1.7, $(JAVA_VERSION))
    JAVACFLAGS=-Xlint:unchecked -XDignore.symbol.file=true
  else
    JAVACFLAGS=-Xlint:unchecked -XDignore.symbol.file=true -Xbootclasspath/p:$(JAXB_HOME)/lib/jaxb-api.jar:$(JAXB_HOME)/lib/jaxb-impl.jar
  endif
  ifeq (64, $(findstring 64,$(shell uname -p)))
    ARCH=64
  else
    ARCH=32
  endif
endif

ifeq (Linux, $(findstring Linux,$(OS)))
    PLATFORM=linux
endif

NULL:=
SPACE:=$(NULL) # end of the line
SHELL=/bin/sh
CWD=$(shell pwd)

JAVAC=$(JAVA_HOME)/bin/javac
JAR=$(JAVA_HOME)/bin/jar
CLASSLIB=$(JAVA_HOME)/jre/lib/rt.jar

ifeq (x, x$(JRE64_HOME))
    JRE_HOME=$(JRE32_HOME)
else
    JRE_HOME=$(JRE64_HOME)
endif
JRE=$(JRE_HOME)/bin/java
XJCFLAGS=-enableIntrospection

BUILD=build
DIST=dist
RSRC=rsrc
DOCS=docs/api
SRC=$(TOP)/src
COMPONENTS=$(TOP)/components
LIBDIR=$(RSRC)/lib
LIBS=$(subst $(SPACE),$(CLN),$(filter %.jar %.zip, $(wildcard $(LIBDIR)/*)))

JSAF_SRC=$(JSAF_HOME)/src
JSAF_COMPONENTS=$(JSAF_HOME)/components
JSAF_CORE=$(JSAF_COMPONENTS)/facade
JSAF_CORE_LIB=$(JSAF_CORE)/jSAF.jar
JSAF_CORE_DEPS=$(subst $(SPACE),$(CLN),$(filter %.jar %.zip, $(wildcard $(JSAF_CORE)/$(LIBDIR)/*)))
JSAF_PROVIDER=$(JSAF_COMPONENTS)/provider
JSAF_PROVIDER_LIB=$(JSAF_PROVIDER)/jSAF-Provider.jar
JSAF_PROVIDER_DEPS=$(subst $(SPACE),$(CLN),$(filter %.jar %.zip, $(wildcard $(JSAF_PROVIDER)/$(LIBDIR)/*)))

JPE_LIB=$(JPE_HOME)/jPE.jar

SVRL=$(COMPONENTS)/schematron/schema/svrl.jar
SCAP=$(COMPONENTS)/scap
SCAP_LIB=$(SCAP)/scap-schema-$(SCAP_VERSION).jar
SCAP_EXT=$(COMPONENTS)/scap-extensions
SCAP_EXT_LIB=$(SCAP)/scap-schema-extensions.jar
JOVALDI=$(COMPONENTS)/jovaldi
JOVALDI_LIB=$(JOVALDI)/jovaldi.jar
JOVAL_CORE=$(COMPONENTS)/engine
JOVAL_CORE_LIB=$(JOVAL_CORE)/jOVAL.jar
PLUGIN_LOCAL=$(COMPONENTS)/plugin
PLUGIN_LOCAL_LIB=$(PLUGIN_LOCAL)/jOVAL-Plugin.jar
PLUGIN_LOCAL_DEPS=$(subst $(SPACE),$(CLN),$(filter %.jar %.zip, $(wildcard $(PLUGIN_LOCAL)/$(LIBDIR)/*)))
SCE=$(COMPONENTS)/sce
SCE_LIB=$(SCE)/sce-schema-$(SCE_VERSION).jar
