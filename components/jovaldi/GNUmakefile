# Copyright (C) 2011, 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../..

include $(TOP)/common.mk

PRODUCTNAME=jovaldi
ZIPNAME=$(PRODUCTNAME)_$(JOVAL_VERSION)_Windows
TARNAME=$(PRODUCTNAME)_$(JOVAL_VERSION)_`uname -s`
DIST32=$(DIST)/x86
BIN32=$(RSRC)/bin/$(PLATFORM)-x86
DIST64=$(DIST)/x64
BIN64=$(RSRC)/bin/$(PLATFORM)-x64

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

dist: dist-$(ARCH)

dist-clean: clean
	rm -f $(ZIPNAME)*.zip
	rm -f $(TARNAME)*.tar.gz
	rm -rf $(DIST)

$(DIST32)/jre : $(JRE32_HOME)
	mkdir -p $(DIST32)
	rm -rf $@
	cp -R $< $@

dist-x86: all $(DIST32)/jre
	cp -R $(BIN32)/* $(DIST32)
	sed 's/\[VERSION\]/$(JOVAL_VERSION)/g' $(RSRC)/docs/Readme.txt > $(DIST32)/Readme.txt
	cp -R $(RSRC)/docs/Remote.txt $(DIST32)/Remote.txt
	cp -R $(RSRC)/lib $(DIST32)
	cp -R $(RSRC)/licenses $(DIST32)
	cp -R $(RSRC)/xml $(DIST32)
	cp -R $(SCAP)/schemas/oval-$(OVAL_VERSION)/*.xsd $(DIST32)/xml
	cp -R $(SCAP)/schemas/xml/xmldsig-core-schema.xsd $(DIST32)/xml
	cp $(COMPONENTS)/schematron/*-schematron.xsl $(DIST32)/xml
	cp $(PRODUCTNAME).jar $(DIST32)/lib
	cp $(COMMON_LIB) $(DIST32)/lib
	cp $(COMMON)/$(LIBDIR)/*.jar $(DIST32)/lib
	cp $(ADAPTERS_LIB) $(DIST32)/lib
	cp $(SCAP_LIB) $(DIST32)/lib
	cp $(SVRL) $(DIST32)/lib
	cp $(JOVAL_CORE_LIB) $(DIST32)/lib
	cp $(XPERT_LIB) $(DIST32)/lib
	mkdir -p $(DIST32)/plugin/default/lib
	cp -R plugin/default/rsrc/* $(DIST32)/plugin/default
	cp -R $(PLUGIN_LOCAL_LIB) $(DIST32)/plugin/default/lib
	cp -R $(PLUGIN_LOCAL)/$(LIBDIR)/* $(DIST32)/plugin/default/lib
	mkdir -p $(DIST)/x86/plugin/remote/lib
	cp -R plugin/remote/rsrc/* $(DIST32)/plugin/remote
	cp -R $(PLUGIN_REMOTE_LIB) $(DIST32)/plugin/remote/lib
	cp -R $(PLUGIN_REMOTE)/$(LIBDIR)/*.jar $(DIST32)/plugin/remote/lib
	cp $(WSMAN_LIB) $(DIST32)/plugin/remote/lib
	mkdir -p $(DIST)/x86/plugin/offline/lib
	cp -R plugin/offline/rsrc/* $(DIST32)/plugin/offline
	cp -R $(PLUGIN_OFFLINE_LIB) $(DIST32)/plugin/offline/lib
	cp -R $(PLUGIN_OFFLINE)/$(LIBDIR)/*.jar $(DIST32)/plugin/offline/lib

$(DIST64)/jre : $(JRE64_HOME)
	mkdir -p $(DIST64)
	rm -rf $@
	cp -R $< $@

dist-x64: all $(DIST64)/jre
	cp -R $(BIN64)/* $(DIST64)
	sed 's/\[VERSION\]/$(JOVAL_VERSION)/g' $(RSRC)/docs/Readme.txt > $(DIST64)/Readme.txt
	cp -R $(RSRC)/docs/Remote.txt $(DIST64)/Remote.txt
	cp -R $(RSRC)/lib $(DIST64)
	cp -R $(RSRC)/licenses $(DIST64)
	cp -R $(RSRC)/xml $(DIST64)
	cp -R $(SCAP)/schemas/oval-$(OVAL_VERSION)/*.xsd $(DIST64)/xml
	cp -R $(SCAP)/schemas/xml/xmldsig-core-schema.xsd $(DIST64)/xml
	cp $(COMPONENTS)/schematron/*-schematron.xsl $(DIST64)/xml
	cp $(PRODUCTNAME).jar $(DIST64)/lib
	cp $(COMMON_LIB) $(DIST64)/lib
	cp $(COMMON)/$(LIBDIR)/*.jar $(DIST64)/lib
	cp $(ADAPTERS_LIB) $(DIST64)/lib
	cp $(SCAP_LIB) $(DIST64)/lib
	cp $(SVRL) $(DIST64)/lib
	cp $(JOVAL_CORE_LIB) $(DIST64)/lib
	mkdir -p $(DIST64)/plugin/default/lib
	cp $(XPERT_LIB) $(DIST64)/lib
	cp -R plugin/default/rsrc/* $(DIST64)/plugin/default
	cp -R $(PLUGIN_LOCAL_LIB) $(DIST64)/plugin/default/lib
	cp -R $(PLUGIN_LOCAL)/$(LIBDIR)/* $(DIST64)/plugin/default/lib
	mkdir -p $(DIST)/x64/plugin/remote/lib
	cp -R plugin/remote/rsrc/* $(DIST64)/plugin/remote
	cp -R $(PLUGIN_REMOTE_LIB) $(DIST64)/plugin/remote/lib
	cp -R $(PLUGIN_REMOTE)/$(LIBDIR)/*.jar $(DIST64)/plugin/remote/lib
	cp $(WSMAN_LIB) $(DIST64)/plugin/remote/lib
	mkdir -p $(DIST)/x64/plugin/offline/lib
	cp -R plugin/offline/rsrc/* $(DIST64)/plugin/offline
	cp -R $(PLUGIN_OFFLINE_LIB) $(DIST64)/plugin/offline/lib
	cp -R $(PLUGIN_OFFLINE)/$(LIBDIR)/*.jar $(DIST64)/plugin/offline/lib

release-win32: dist-x86
	$(JAVA_HOME)/bin/jar -cvfM $(ZIPNAME)_x86.zip -C $(DIST32) .

release-win64: dist-x64
	$(JAVA_HOME)/bin/jar -cvfM $(ZIPNAME)_x64.zip -C $(DIST64) .

release-tar32: dist-x86
	tar -cvf $(TARNAME)_x86.tar -C $(DIST32) .
	gzip $(TARNAME)_x86.tar

release-tar64: dist-x64
	tar -cvf $(TARNAME)_x64.tar -C $(DIST64) .
	gzip $(TARNAME)_x64.tar
