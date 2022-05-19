# Copyright (C) 2015-2021 JovalCM.com.  All rights reserved.
# This software is licensed under the LGPL 3.0 license available at http://www.gnu.org/licenses/lgpl.txt

SCAP_VERSION=1.3.0.0.0-9
OVAL_VERSION=5.11.2

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
  else ifeq (Darwin, $(findstring Darwin,$(OS)))
    PLATFORM=mac
  endif
  CLN=:
endif
ifeq (win, $(PLATFORM))
    WIN_JAVA_HOME=$(shell cygpath -u $(JAVA_HOME))
    JAVA=$(WIN_JAVA_HOME)/bin/java.exe
    JAR=$(WIN_JAVA_HOME)/bin/jar.exe
    JAVADOC=$(WIN_JAVA_HOME)/bin/javadoc.exe
    JAVAC=$(WIN_JAVA_HOME)/bin/javac.exe
    NUMPROCS=1
else
    JAVA=$(JAVA_HOME)/bin/java
    JAR=$(JAVA_HOME)/bin/jar
    JAVADOC=$(JAVA_HOME)/bin/javadoc
    JAVAC=$(JAVA_HOME)/bin/javac
    ifeq (mac, $(PLATFORM))
        NUMPROCS=$(shell sysctl -n hw.ncpu)
    else
        NUMPROCS=$(shell nproc)
    endif
endif
RAW_JAVA_VERSION:=$(shell $(JAVA_HOME)/bin/java -version 2>&1)
ifeq (11, $(findstring 11, $(findstring "11., $(RAW_JAVA_VERSION))))
    JAVA_VERSION=11
    JAVADOCFLAGS=-Xdoclint:none -J-Xmx512m
else ifeq (12, $(findstring 12, $(findstring "12., $(RAW_JAVA_VERSION))))
    JAVA_VERSION=12
    JAVADOCFLAGS=-Xdoclint:none -J-Xmx512m
else ifeq (13, $(findstring 13, $(findstring "13., $(RAW_JAVA_VERSION))))
    JAVA_VERSION=13
    JAVADOCFLAGS=-Xdoclint:none -J-Xmx512m
else ifeq (14, $(findstring 14, $(findstring "14., $(RAW_JAVA_VERSION))))
    JAVA_VERSION=14
    JAVADOCFLAGS=-Xdoclint:none -J-Xmx512m
else ifeq (15, $(findstring 15, $(findstring "15., $(RAW_JAVA_VERSION))))
    JAVA_VERSION=15
    JAVADOCFLAGS=-Xdoclint:none -J-Xmx512m
else ifeq (16, $(findstring 16, $(findstring "16., $(RAW_JAVA_VERSION))))
    JAVA_VERSION=16
    JAVADOCFLAGS=-Xdoclint:none -J-Xmx512m
else ifeq (17, $(findstring 17, $(findstring "17., $(RAW_JAVA_VERSION))))
    JAVA_VERSION=17
    JAVADOCFLAGS=-Xdoclint:none -J-Xmx512m
else ifeq (18, $(findstring 18, $(findstring "18., $(RAW_JAVA_VERSION))))
    JAVA_VERSION=18
    JAVADOCFLAGS=-Xdoclint:none -J-Xmx512m
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
OVAL_SRC=$(THIRDPARTY)/OVAL/oval-schemas
OVAL_SCHEMA=$(SCHEMADIR)/oval
BINDINGS=$(SCHEMADIR)/bindings.xjb
CATALOG=schemas.xml
EPISODE=schemas.episode
XJCFLAGS=-enableIntrospection -catalog $(CATALOG) -episode $(EPISODE)
XJCFLAGS_EXT=$(XJCFLAGS) -extension -Xnamespace-prefix
XJC=$(JAVA) -Djavax.xml.accessExternalSchema=all -Dcom.sun.tools.xjc.XJCFacade.nohack=true -cp $(XJC_LIBS) com.sun.tools.xjc.XJCFacade

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

THIRDPARTY=$(TOP)/3rd-party
SAXON_LIB=$(THIRDPARTY)/saxon9he.jar
JAXB_BIND_API_LIB=$(THIRDPARTY)/jakarta.xml.bind-api-4.0.0.jar
XJC_LIBS=$(THIRDPARTY)/jaxb-xjc-3.0.2.jar$(CLN)$(THIRDPARTY)/jaxb-core-3.0.2.jar$(CLN)$(THIRDPARTY)/jaxb-impl-3.0.2.jar$(CLN)$(JAXB_BIND_API_LIB)$(CLN)$(THIRDPARTY)/jakarta.activation-api-2.1.0.jar$(CLN)$(THIRDPARTY)/istack-commons-runtime-4.1.1.jar$(CLN)$(THIRDPARTY)/jaxb2-namespace-prefix-2.0.jar$(CLN)$(THIRDPARTY)/rngom-3.0.2.jar$(CLN)$(THIRDPARTY)/relaxng-datatype-3.0.2.jar$(CLN)$(THIRDPARTY)/dtd-parser-1.5.0.jar$(CLN)$(THIRDPARTY)/xsom-3.0.2.jar$(CLN)$(THIRDPARTY)/codemodel-3.0.2.jar$(CLN)$(THIRDPARTY)/txw2-3.0.2.jar
