# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=..

include $(TOP)/common.mk

API=$(JOVAL_CORE)/jOVALCore.jar$(CLN)$(JOVAL_DEPS)$(CLN)$(SCHEMALIB)
PSHARED=$(COMPONENTS)/sdk/$(DIST)/jOVALPluginShared.jar

REMOTELIBDIR=$(COMPONENTS)/sdk/plugin/remote/$(LIBDIR)
REMOTEDEPS=$(subst $(SPACE),$(CLN),$(filter %.jar %.zip, $(wildcard $(REMOTELIBDIR)/*)))
PREMOTE=$(COMPONENTS)/sdk/$(DIST)/jOVALPluginRemote.jar$(CLN)$(REMOTEDEPS)

CLASSPATH="$(CLASSLIB)$(CLN)$(CLN)$(API)$(CLN)$(PSHARED)$(CLN)$(PREMOTE)$(CLN)$(SRC)"
RUNTIMECP="$(CLASSLIB)$(CLN)$(CLN)$(API)$(CLN)$(PSHARED)$(CLN)$(PREMOTE)$(CLN)."

CLASSES=TestMain
CLASS_FILES:=$(foreach class, $(CLASSES), $(subst .,/,$(class)).class)

all: classes

clean:
	rm -rf *.class
	rm test.log
	rm -rf report

classes: $(CLASS_FILES)

%.class: %.java
	$(JAVAC) $(JAVACFLAGS) -classpath $(CLASSPATH) $<

test:
	$(JAVA) -classpath $(RUNTIMECP) TestMain plugin.properties
