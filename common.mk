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

JOVAL_VERSION=0.B.1
OVAL_SCHEMA_VERSION=5.10

# If your system is 32-bit, set ARCH to x86
#ARCH=x86
ARCH=x64
JRE_HOME=$(TOP)/../../tools/jre160_27
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
GENSRC=$(TOP)/src-gen
OVAL=$(TOP)/oval-schema
JOVAL=$(TOP)/jovaldi
SCHEMALIB=$(OVAL)/oval-schema-$(OVAL_SCHEMA_VERSION).jar
LIBDIR=$(RSRC)/lib
LIB=$(subst $(SPACE),$(CLN),$(filter %.jar %.zip, $(wildcard $(LIBDIR)/*)))
