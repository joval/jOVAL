# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../../../..

include $(TOP)/common.mk

API=$(OVAL_LIB)$(CLN)$(COMMON_LIB)$(CLN)$(COMMON_DEPS)$(CLN)$(JOVAL_CORE_LIB)
CLASSPATH="$(CLASSLIB)$(CLN)$(API)$(CLN)$(SRC)"

include classes.mk

CLASS_FILES:=$(foreach class, $(SHARED), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(SHARED)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

all: $(PLUGIN_SHARED_LIB)

$(PLUGIN_SHARED_LIB): classes
	$(JAR) cvf $@ -C $(BUILD)/ .

clean:
	rm -rf $(BUILD)
	rm -f $(PLUGIN_SHARED_LIB)

classes: classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

$(BUILD)/%.class: $(SRC)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $(subst PKG,,$@)
