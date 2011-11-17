# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../../..

include $(TOP)/common.mk

PRODUCTNAME=plugin

API=$(JOVAL)/jOVAL.jar$(CLN)$(SCHEMALIB)$(CLN)$(JOVAL_DEPS)
SHARED=plugin-shared.jar
CLASSPATH="$(CLASSLIB)$(CLN)$(LIB)$(CLN)$(SRC)$(CLN)$(API)$(CLN)$(SHARED)"
RUNTIMECP="$(CLASSLIB)$(CLN)$(API)$(CLN)$(LIB)$(CLN)$(DEPS)$(CLN)$(SHARED)$(CLN)$(PRODUCTNAME).jar"

include classes.mk

CLASS_FILES:=$(foreach class, $(REMOTE), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(REMOTE)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

all: $(PRODUCTNAME).jar

install: all
	mkdir -p $(DIST)
	cp -R $(RSRC)/* $(DIST)
	cp $(PRODUCTNAME).jar $(DIST)/lib
	cp $(SHARED) $(DIST)/lib

$(PRODUCTNAME).jar: classes resources
	$(JAR) cvf $@ -C $(BUILD)/ .

test:
	$(JAVA) -classpath $(RUNTIMECP) org.joval.test.Remote test.properties

dist-clean: clean
	rm -rf $(DIST)

clean:
	rm -rf $(BUILD)
	rm -f $(PRODUCTNAME).jar

resources:
	rm -f $(BUILD)/*.properties*
	cp $(SRC)/rsrc/plugin-remote/* $(BUILD)
	echo version=$(JOVAL_VERSION) >> $(BUILD)/plugin.resources.properties

classes: classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

$(BUILD)/%.class: $(SRC)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $(subst PKG,,$@)
