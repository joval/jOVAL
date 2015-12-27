# Copyright (C) 2015 jOVAL.org.  All rights reserved.
# This software is licensed under the LGPL 3.0 license available at http://www.gnu.org/licenses/lgpl.txt

SCAP_VERSION=1.2
OVAL_VERSION=5.11.1

Default: all

ifeq (x, x$(JAVA_HOME))
    $(error "Please set the JAVA_HOME environment variable.")
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
ifeq (win, $(PLATFORM))
    WIN_JAVA_HOME=$(shell cygpath -u $(JAVA_HOME))
    JAVA=$(WIN_JAVA_HOME)/bin/java.exe
    JAR=$(WIN_JAVA_HOME)/bin/jar.exe
    JAVADOC=$(WIN_JAVA_HOME)/bin/javadoc.exe
    JAVAC=$(WIN_JAVA_HOME)/bin/javac.exe
    CLASSLIB=$(shell cygpath -w $(JAVA_HOME))\jre\lib\rt.jar
else
    JAVA=$(JAVA_HOME)/bin/java
    JAR=$(JAVA_HOME)/bin/jar
    JAVADOC=$(JAVA_HOME)/bin/javadoc
    JAVAC=$(JAVA_HOME)/bin/javac
    CLASSLIB=$(JAVA_HOME)/jre/lib/rt.jar
endif
RAW_JAVA_VERSION:=$(shell $(JAVA_HOME)/bin/java -version 2>&1)
ifeq (1.8, $(findstring 1.8, $(RAW_JAVA_VERSION)))
    JAVA_VERSION=1.8
    XJC=$(JAVA_HOME)/bin/xjc
    JAVADOCFLAGS=-Xdoclint:none -J-Xmx512m
else ifeq (1.7, $(findstring 1.7, $(RAW_JAVA_VERSION)))
    JAVA_VERSION=1.7
    XJC=$(JAVA_HOME)/bin/xjc
    JAVADOCFLAGS=-J-Xmx512m
else ifeq (1.6, $(findstring 1.6, $(RAW_JAVA_VERSION)))
    JAVA_VERSION=1.6
    ifeq (x, x$(JAXB_HOME))
        $(error "You must set the JAXB_HOME environment variable when using Java 6.")
    else ifeq (win, $(PLATFORM))
        XJC=$(JAVA) -jar '$(shell cygpath -w $(JAXB_HOME))\lib\jaxb-xjc.jar'
    else
        XJC=$(JAVA) -jar $(JAXB_HOME)/lib/jaxb-xjc.jar
    endif
    JAVADOCFLAGS=-J-Xmx512m
else
    $(error "Unsupported Java version: $(RAW_JAVA_VERSION)")
endif

NULL:=
SPACE:=$(NULL) # end of the line
SHELL=/bin/sh
CWD=$(shell pwd)

BUILD=build
RSRC=rsrc
DOCS=docs
GEN=gen-src
SCHEMADIR=schemas
OVAL_SCHEMA=$(SCHEMADIR)/oval-$(OVAL_VERSION)
BINDINGS=$(SCHEMADIR)/bindings.xjb
CATALOG=schemas.cat
EPISODE=schemas.episode
XJCFLAGS=-enableIntrospection -catalog $(CATALOG) -episode $(EPISODE)
XJCFLAGS_EXT=$(XJCFLAGS) -extension

#
# Make namespaces optional in the episode bindings
#
BROKEN=<bindings scd=\"x-schema::tns\"
FIXED=<bindings scd=\"x-schema::tns\" if-exists=\"true\"

SCAP=$(TOP)/scap
SCAP_LIB=$(SCAP)/scap-schema-$(SCAP_VERSION).jar
SCAP_EXT=$(TOP)/scap-extensions
SCAP_EXT_LIB=$(SCAP_EXT)/scap-schema-extensions.jar
CYBERSCOPE=$(TOP)/cyberscope
CYBERSCOPE_LIB=$(CYBERSCOPE)/cyberscope-schema.jar
DODARF=$(TOP)/dod-arf
DODARF_LIB=$(DODARF)/DoD-ARF-schema.jar

SAXON=$(TOP)/Saxon
SAXON_LIB=$(SAXON)/saxon9he.jar
