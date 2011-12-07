# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../../..

include $(TOP)/common.mk

API=$(SCHEMALIB)
CLASSPATH="$(CLASSLIB)$(CLN)$(LIB)$(CLN)$(SRC)$(CLN)$(API)"
RUNTIMECP="$(CLASSLIB)$(CLN)$(API)$(CLN)$(LIB)$(CLN)$(DEPS)$(CLN)$(JOVAL_CORE_LIB)"

include classes.mk

CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

all: $(JOVAL_CORE_LIB)

$(JOVAL_CORE_LIB): classes resources
	$(JAR) cvf $@ -C $(BUILD)/ .

clean:
	rm -rf $(BUILD)
	rm -f $(JOVAL_CORE_LIB)

resources:
	rm -f $(BUILD)/*.properties
	cp $(SRC)/rsrc/joval.system.properties $(BUILD)
	cp $(SRC)/rsrc/jovalmsg*.properties $(BUILD)
	echo "version=$(JOVAL_VERSION)" >> $(BUILD)/joval.system.properties
	echo "build.date=`date`" >> $(BUILD)/joval.system.properties

classes: classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

$(BUILD)/%.class: $(SRC)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $(subst PKG,,$@)
