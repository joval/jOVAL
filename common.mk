# Copyright (C) 2015 jOVAL.org.  All rights reserved.
# This software is licensed under the LGPL 3.0 license available at http://www.gnu.org/licenses/lgpl.txt

SCAP_VERSION=1.2

Default: all

ifeq (x, x$(JAVA_HOME))
    $(error "Please set the JAVA_HOME environment variable.")
endif
JAVA=$(JAVA_HOME)/bin/java
RAW_JAVA_VERSION:=$(shell $(JAVA) -version 2>&1)
ifeq (1.8, $(findstring 1.8, $(RAW_JAVA_VERSION)))
    JAVA_VERSION=1.8
    XJC=$(JAVA_HOME)/bin/xjc
else ifeq (1.7, $(findstring 1.7, $(RAW_JAVA_VERSION)))
    JAVA_VERSION=1.7
    XJC=$(JAVA_HOME)/bin/xjc
else ifeq (1.6, $(findstring 1.6, $(RAW_JAVA_VERSION)))
    JAVA_VERSION=1.6
    ifeq (x, x$(JAXB_HOME))
        $(error "You must set the JAXB_HOME environment variable when using Java 6.")
    else
        XJC=$(JAVA) -jar '$(JAXB_HOME)/lib/jaxb-xjc.jar'
    endif
else
    $(error "Unsupported Java version: $(RAW_JAVA_VERSION)")
endif
PLATFORM=unknown
ifeq (Windows, $(findstring Windows,$(OS)))
  PLATFORM=win
  CLN=;
else
  OS=$(shell uname)
  ifeq (Linux, $(findstring Linux,$(OS)))
    PLATFORM=linux
  endif
  CLN=:
endif

NULL:=
SPACE:=$(NULL) # end of the line
SHELL=/bin/sh
CWD=$(shell pwd)

JAVAC=$(JAVA_HOME)/bin/javac
JAR=$(JAVA_HOME)/bin/jar
JAVADOC=$(JAVA_HOME)/bin/javadoc
CLASSLIB=$(JAVA_HOME)/jre/lib/rt.jar

JRE=$(JRE_HOME)/bin/java
XJCFLAGS=-enableIntrospection

BUILD=build
RSRC=rsrc
DOCS=docs

SCAP=$(TOP)/scap
SCAP_LIB=$(SCAP)/scap-schema-$(SCAP_VERSION).jar
SCAP_EXT=$(TOP)/scap-extensions
SCAP_EXT_LIB=$(SCAP_EXT)/scap-schema-extensions.jar
