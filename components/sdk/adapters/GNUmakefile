# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../../..

include $(TOP)/common.mk

API=$(SCAP_LIB)$(CLN)$(COMMON_LIB)$(CLN)$(COMMON_DEPS)$(CLN)$(JOVAL_CORE_LIB)
CLASSPATH="$(CLASSLIB)$(CLN)$(LIB)$(CLN)$(SRC)$(CLN)$(API)"

include classes.mk

CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

all: $(ADAPTERS_LIB)

$(ADAPTERS_LIB): classes resources
	$(JAR) cvf $@ -C $(BUILD)/ .

clean:
	rm -rf $(BUILD)
	rm -f $(ADAPTERS_LIB)

resources: classdirs
	rm -f $(BUILD)/*.txt
	cp $(RSRC)/adapters.txt $(BUILD)
	rm -f $(BUILD)/*.properties
	cp $(RSRC)/windows.locales.properties $(BUILD)
	rm -f $(BUILD)/org/joval/oval/adapter/windows/*.psm1
	cp $(SRC)/org/joval/oval/adapter/windows/*.psm1 $(BUILD)/org/joval/oval/adapter/windows/
	cp $(SRC)/org/joval/oval/adapter/independent/*.psm1 $(BUILD)/org/joval/oval/adapter/independent/

classes: classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

$(BUILD)/%.class: $(SRC)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $(subst PKG,,$@)
