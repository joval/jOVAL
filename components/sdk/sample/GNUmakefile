# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../../..

include $(TOP)/common.mk

SDK_JAR=$(SDK)/$(DIST)/jOVAL-SDK.jar
API=$(SCAP_LIB)$(CLN)$(COMMON_LIB)$(CLN)$(COMMON_DEPS)$(CLN)$(JOVAL_CORE_LIB)$(CLN)$(PLUGIN_REMOTE_LIB)
CLASSPATH="$(CLASSLIB)$(CLN)$(SDK_JAR)$(CLN)$(API)$(CLN)$(LIB)$(CLN)."

CLASSES=TrivialScanner
CLASS_FILES:=$(foreach class, $(CLASSES), $(subst .,/,$(class)).class)

all: classes

clean:
	rm -rf *.class

classes: $(CLASS_FILES)

%.class: %.java
	$(JAVAC) $(JAVACFLAGS) -classpath $(CLASSPATH) $<

test:
	$(JAVA) -classpath $(CLASSPATH) TrivialScanner definitions.xml plugin.properties
