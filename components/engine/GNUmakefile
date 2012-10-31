# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../..

include $(TOP)/common.mk

API=$(SCAP_LIB)
CLASSPATH="$(CLASSLIB)$(CLN)$(LIB)$(CLN)$(SRC)$(CLN)$(API)"

include classes.mk

CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

all: $(JOVAL_CORE_LIB)

$(JOVAL_CORE_LIB): classes resources
	$(JAR) cvf $@ -C $(BUILD)/ .

resources:
	rm -f $(BUILD)/*.ini
	cp $(RSRC)/*.ini $(BUILD)
	rm -f $(BUILD)/*.properties
	cp $(RSRC)/*.properties $(BUILD)
	echo -e "version=$(JOVAL_VERSION)\r\n" >> $(BUILD)/joval.system.properties
	echo "build.date=`date`" >> $(BUILD)/joval.system.properties

clean:
	rm -rf $(BUILD)
	rm -f $(JOVAL_CORE_LIB)

classes: classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

$(BUILD)/%.class: $(SRC)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $(subst PKG,,$@)
