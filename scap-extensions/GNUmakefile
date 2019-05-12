# Copyright (C) 2016 JovalCM.com.  All rights reserved.
# This software is licensed under the LGPL 3.0 license available at http://www.gnu.org/licenses/lgpl.txt

TOP=..
include $(TOP)/common.mk

CLASSPATH="$(CLASSLIB)$(CLN)$(SCAP_LIB)$(CLN)$(GEN)"

include classes.mk

SCHEMA_FILES:=$(shell for file in `find $(SCHEMADIR) -type f -name *.xsd`; do echo $$file; done)
CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

all: $(SCAP_EXT_LIB)

$(SCAP_EXT_LIB): $(GEN) classes resources
	$(JAR) cvf $@ -C $(BUILD)/ .

clean:
	rm -f $(EPISODE)
	rm -f $(SCAP_EXT_LIB)
	rm -rf $(BUILD)
	rm -rf $(GEN)

resources:
	mkdir -p $(BUILD)/META-INF
	sed 's/$(BROKEN)/$(FIXED)/' $(EPISODE) > $(BUILD)/META-INF/sun-jaxb.episode
	mkdir -p $(BUILD)/scapx/schemas
	cp $(CATALOG) $(BUILD)/scapx
	cp $(RSRC)/registry.ini $(BUILD)/scapx
	cp -R $(SCHEMADIR)/* $(BUILD)/scapx/schemas
	rm $(BUILD)/scapx/schemas/bindings.xjb

classes: classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

$(GEN): $(SCHEMA_FILES)
	mkdir -p $@
	$(XJC) $(XJCFLAGS_EXT) $(SCHEMA_FILES) $(SCAP_LIB) -b $(BINDINGS) -d $(GEN)
	find $(GEN) -name ObjectFactory.java | grep core | xargs rm

$(BUILD)/%.class: $(GEN)/%.java
	$(JAVAC) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $@
