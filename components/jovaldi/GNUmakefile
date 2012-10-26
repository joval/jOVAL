# Copyright (C) 2011, 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../..

include $(TOP)/common.mk

PRODUCTNAME=jovaldi
DIST_DIR=$(DIST)/$(ARCH)
BIN_DIR=$(RSRC)/bin/$(PLATFORM)-$(ARCH)

API=$(COMMON_LIB)$(CLN)$(COMMON_DEPS)$(CLN)$(JOVAL_CORE_LIB)$(CLN)$(SCAP_LIB)$(CLN)$(SVRL)
CLASSPATH="$(CLASSLIB)$(CLN)$(LIB)$(CLN)$(API)$(CLN)$(SRC)"

include classes.mk

CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

all: $(PRODUCTNAME).jar

$(PRODUCTNAME).jar: classes resources
	$(JAR) cvf $@ -C $(BUILD)/ .

clean:
	rm -rf $(BUILD)
	rm -f $(PRODUCTNAME).jar

resources:
	rm -f $(BUILD)/*.properties
	cp $(RSRC)/*.properties $(BUILD)

classes: classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

$(BUILD)/%.class: $(SRC)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $(subst PKG,,$@)

dist-clean: clean
	rm -rf $(DIST_DIR)

$(DIST_DIR)/jre : $(JRE_HOME)
	mkdir -p $(DIST_DIR)
	rm -rf $@
	cp -R $< $@

dist: all $(DIST_DIR)/jre
	cp -R $(BIN_DIR)/* $(DIST_DIR)
	sed 's/\[VERSION\]/$(JOVAL_VERSION)/g' $(RSRC)/docs/Readme.txt > $(DIST_DIR)/Readme.txt
	cp -R $(RSRC)/lib $(DIST_DIR)
	cp -R $(RSRC)/licenses $(DIST_DIR)
	cp -R $(RSRC)/xml $(DIST_DIR)
	cp -R $(SCAP)/schemas/oval-$(OVAL_VERSION)/*.xsd $(DIST_DIR)/xml
	cp -R $(SCAP)/schemas/xml/xmldsig-core-schema.xsd $(DIST_DIR)/xml
	cp $(COMPONENTS)/schematron/*-schematron.xsl $(DIST_DIR)/xml
	cp $(PRODUCTNAME).jar $(DIST_DIR)/lib
	cp $(COMMON_LIB) $(DIST_DIR)/lib
	cp $(COMMON)/$(LIBDIR)/*.jar $(DIST_DIR)/lib
	cp $(ADAPTERS_LIB) $(DIST_DIR)/lib
	cp $(SCAP_LIB) $(DIST_DIR)/lib
	cp $(SVRL) $(DIST_DIR)/lib
	cp $(JOVAL_CORE_LIB) $(DIST_DIR)/lib
	cp $(XPERT_LIB) $(DIST_DIR)/lib
	mkdir -p $(DIST_DIR)/security
	cp $(XPERT)/$(RSRC)/security/cacerts.jks $(DIST_DIR)/security
	mkdir -p $(DIST_DIR)/plugin/default/lib
	cp -R $(PLUGIN_LOCAL)/$(RSRC)/plugin.properties $(DIST_DIR)/plugin/default/plugin.properties
	cp -R $(PLUGIN_LOCAL_LIB) $(DIST_DIR)/plugin/default/lib
	cp -R $(PLUGIN_LOCAL)/$(LIBDIR)/* $(DIST_DIR)/plugin/default/lib
