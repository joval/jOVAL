# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../..

include $(TOP)/common.mk

PRODUCTNAME=jovaldi
ZIPNAME=$(PRODUCTNAME)_$(JOVAL_VERSION)_Windows
TARNAME=$(PRODUCTNAME)_$(JOVAL_VERSION)_`uname -s`_`uname -p`

API=$(JOVAL_CORE_LIB)$(CLN)$(JOVAL_CORE_DEPS)$(CLN)$(SCHEMALIB)
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
	cp $(SRC)/rsrc/jovaldi* $(BUILD)

classes: classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

$(BUILD)/%.class: $(SRC)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $(subst PKG,,$@)

dist-clean: clean
	rm -f $(ZIPNAME)*.zip
	rm -f $(TARNAME).tar.gz
	rm -rf $(DIST)

dist: dist-$(ARCH)

dist-x86: all
	mkdir -p $(DIST)/x86
	cp -R $(RSRC)/* $(DIST)/x86
	cp $(PRODUCTNAME).jar $(DIST)/x86/lib
	cp $(SCHEMALIB) $(DIST)/x86/lib
	cp $(JOVAL_CORE_LIB) $(DIST)/x86/lib
	cp -R $(JOVAL_CORE)/$(LIBDIR)/* $(DIST)/x86/lib
	mkdir -p $(DIST)/x86/plugin/default/lib
	cp -R plugin/default/rsrc/* $(DIST)/x86/plugin/default
	cp -R $(PLUGIN_SHARED_LIB) $(DIST)/x86/plugin/default/lib
	cp -R $(PLUGIN_SHARED)/$(LIBDIR)/* $(DIST)/x86/plugin/default/lib
	cp -R $(PLUGIN_LOCAL_LIB) $(DIST)/x86/plugin/default/lib
	cp -R $(PLUGIN_LOCAL)/$(LIBDIR)/* $(DIST)/x86/plugin/default/lib
	mkdir -p $(DIST)/x86/plugin/remote/lib
	cp -R plugin/remote/rsrc/* $(DIST)/x86/plugin/remote
	cp -R $(PLUGIN_SHARED_LIB) $(DIST)/x86/plugin/remote/lib
	cp -R $(PLUGIN_SHARED)/$(LIBDIR)/*.jar $(DIST)/x86/plugin/remote/lib
	cp -R $(PLUGIN_REMOTE_LIB) $(DIST)/x86/plugin/remote/lib
	cp -R $(PLUGIN_REMOTE)/$(LIBDIR)/*.jar $(DIST)/x86/plugin/remote/lib
	cp -R $(JRE_HOME)/x86 $(DIST)/x86/jre

dist-x64: all
	mkdir -p $(DIST)/x64
	cp -R $(RSRC)/* $(DIST)/x64
	cp $(PRODUCTNAME).jar $(DIST)/x64/lib
	cp $(SCHEMALIB) $(DIST)/x64/lib
	cp $(JOVAL_CORE_LIB) $(DIST)/x64/lib
	cp -R $(JOVAL_CORE)/$(LIBDIR)/* $(DIST)/x64/lib
	mkdir -p $(DIST)/x64/plugin/default/lib
	cp -R plugin/default/rsrc/* $(DIST)/x64/plugin/default
	cp -R $(PLUGIN_SHARED_LIB) $(DIST)/x64/plugin/default/lib
	cp -R $(PLUGIN_SHARED)/$(LIBDIR)/* $(DIST)/x64/plugin/default/lib
	cp -R $(PLUGIN_LOCAL_LIB) $(DIST)/x64/plugin/default/lib
	cp -R $(PLUGIN_LOCAL)/$(LIBDIR)/* $(DIST)/x64/plugin/default/lib
	mkdir -p $(DIST)/x64/plugin/remote/lib
	cp -R plugin/remote/rsrc/* $(DIST)/x64/plugin/remote
	cp -R $(PLUGIN_SHARED_LIB) $(DIST)/x64/plugin/remote/lib
	cp -R $(PLUGIN_SHARED)/$(LIBDIR)/*.jar $(DIST)/x64/plugin/remote/lib
	cp -R $(PLUGIN_REMOTE_LIB) $(DIST)/x64/plugin/remote/lib
	cp -R $(PLUGIN_REMOTE)/$(LIBDIR)/*.jar $(DIST)/x64/plugin/remote/lib
	cp -R $(JRE_HOME)/x64 $(DIST)/x64/jre

release-win32: dist-x86
	$(JAVA_HOME)/bin/jar -cvfM $(ZIPNAME)_x86.zip -C $(DIST)/x86 .

release-win64: dist-x64
	$(JAVA_HOME)/bin/jar -cvfM $(ZIPNAME)_x64.zip -C $(DIST)/x64 .

release-tar: dist-$(ARCH)
	tar -cvf $(TARNAME).tar -C $(DIST)/$(ARCH) .
	gzip $(TARNAME).tar
