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
        XJC=$(JAVA) -jar $(JAXB_HOME)/lib/jaxb-xjc.jar
    endif
else
    $(error "Unsupported Java version: $(RAW_JAVA_VERSION)")
endif
PLATFORM=unknown
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
  ifeq (Linux, $(findstring Linux,$(OS)))
    PLATFORM=linux
  endif
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

NULL:=
SPACE:=$(NULL) # end of the line
SHELL=/bin/sh
CWD=$(shell pwd)

JAVAC=$(JAVA_HOME)/bin/javac
JAR=$(JAVA_HOME)/bin/jar
CLASSLIB=$(JAVA_HOME)/jre/lib/rt.jar

JRE=$(JRE_HOME)/bin/java
XJCFLAGS=-enableIntrospection

BUILD=build
RSRC=rsrc
DOCS=docs/api

SCAP=$(TOP)/scap
SCAP_LIB=$(SCAP)/scap-schema-$(SCAP_VERSION).jar
SCAP_EXT=$(TOP)/scap-extensions
SCAP_EXT_LIB=$(SCAP)/scap-schema-extensions.jar
