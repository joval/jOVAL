# Copyright (C) 2011, 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../..

include $(TOP)/common.mk

API=$(JSAF_CORE_LIB)$(CLN)$(JSAF_CORE_DEPS)$(CLN)$(JOVAL_CORE_LIB)$(CLN)$(SCAP_LIB)$(CLN)$(SVRL)
CLASSPATH="$(CLASSLIB)$(CLN)$LIBS)$(CLN)$(API)$(CLN)$(SRC)"

include classes.mk

CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

all: $(JOVALDI_LIB)

$(JOVALDI_LIB): classes resources
	$(JAR) cvf $@ -C $(BUILD)/ .

clean:
	rm -rf $(BUILD)
	rm -f $(JOVALDI_LIB)

resources:
	rm -f $(BUILD)/*.properties
	cp $(RSRC)/*.properties $(BUILD)

classes: classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

$(BUILD)/%.class: $(SRC)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $(subst PKG,,$@)

dist: $(DIST)/$(ARCH)

dist-all: $(DIST)/32 $(DIST)/64

dist-clean: clean
	rm -rf $(DIST)

$(DIST)/32: all $(DIST)/32/jre
	cp $(RSRC)/bin/$(PLATFORM)-32/* $@
	sed 's/\[VERSION\]/$(JOVAL_VERSION)/g' $(RSRC)/docs/Readme.txt > $(DIST)/32/Readme.txt
	cp -R $(RSRC)/lib $(DIST)/32
	cp -R $(RSRC)/licenses $(DIST)/32
	cp -R $(RSRC)/security $(DIST)/32
	cp -R $(RSRC)/xml $(DIST)/32
	cp -R $(SCAP)/schemas/* $(DIST)/32/xml
	rm $(DIST)/32/xml/bindings.xjb
	cp $(COMPONENTS)/schematron/*-schematron.xsl $(DIST)/32/xml
	cp $(JOVALDI_LIB) $(DIST)/32/lib
	cp $(SCAP_LIB) $(DIST)/32/lib
	cp $(SVRL) $(DIST)/32/lib
	cp $(JOVAL_CORE_LIB) $(DIST)/32/lib
	cp $(JSAF_CORE)/$(LIBDIR)/*.jar $(DIST)/32/lib
	cp $(JSAF_CORE_LIB) $(DIST)/32/lib
	mkdir -p $(DIST)/32/plugin
	rm -rf $(DIST)/32/plugin/default
	cp -R $(PLUGIN_LOCAL)/$(DIST) $(DIST)/32/plugin/default

$(DIST)/32/jre: $(JRE32_HOME)
	mkdir -p $@
	rm -rf $@
	cp -R $< $@

$(DIST)/64: all $(DIST)/64/jre
	cp $(RSRC)/bin/$(PLATFORM)-64/* $@
	sed 's/\[VERSION\]/$(JOVAL_VERSION)/g' $(RSRC)/docs/Readme.txt > $(DIST)/64/Readme.txt
	cp -R $(RSRC)/lib $(DIST)/64
	cp -R $(RSRC)/licenses $(DIST)/64
	cp -R $(RSRC)/security $(DIST)/64
	cp -R $(RSRC)/xml $(DIST)/64
	cp -R $(SCAP)/schemas/* $(DIST)/64/xml
	rm $(DIST)/64/xml/bindings.xjb
	cp $(COMPONENTS)/schematron/*-schematron.xsl $(DIST)/64/xml
	cp $(JOVALDI_LIB) $(DIST)/64/lib
	cp $(SCAP_LIB) $(DIST)/64/lib
	cp $(SVRL) $(DIST)/64/lib
	cp $(JOVAL_CORE_LIB) $(DIST)/64/lib
	cp $(JSAF_CORE)/$(LIBDIR)/*.jar $(DIST)/64/lib
	cp $(JSAF_CORE_LIB) $(DIST)/64/lib
	mkdir -p $(DIST)/64/plugin
	rm -rf $(DIST)/64/plugin/default
	cp -R $(PLUGIN_LOCAL)/$(DIST) $(DIST)/64/plugin/default

$(DIST)/64/jre: $(JRE64_HOME)
	mkdir -p $@
	rm -rf $@
	cp -R $< $@
