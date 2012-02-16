# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../../..

include $(TOP)/common.mk

ENGINE=$(OVAL_LIB)$(CLN)$(JOVAL_CORE_LIB)$(CLN)$(JOVAL_CORE_DEPS)
PLUGIN=$(PLUGIN_SHARED_DEPS)$(CLN)$(PLUGIN_REMOTE_DEPS)$(CLN)$(SDK)/$(DIST)/jOVALRemote.jar
CLASSPATH="$(CLASSLIB)$(CLN)$(ENGINE)$(CLN)$(PLUGIN)$(CLN)$(LIB)$(CLN)."

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
