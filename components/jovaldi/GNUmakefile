# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../..

include $(TOP)/common.mk

PRODUCTNAME=jovaldi
ZIPNAME=$(PRODUCTNAME)_$(JOVAL_VERSION)_Windows
TARNAME=$(PRODUCTNAME)_$(JOVAL_VERSION)_`uname -s`_`uname -p`

API=$(JOVAL_CORE)/jOVALCore.jar$(CLN)$(JOVAL_DEPS)$(CLN)$(SCHEMALIB)
PSHARED=$(COMPONENTS)/sdk/$(DIST)/jOVALPluginShared.jar
CLASSPATH="$(CLASSLIB)$(CLN)$(LIB)$(CLN)$(API)$(CLN)$(PSHARED)$(CLN)$(SRC)"

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
	mkdir -p $(DIST)/x86/lib
	cp $(PRODUCTNAME).jar $(DIST)/x86/lib
	cp $(SCHEMALIB) $(DIST)/x86/lib
	cp $(JOVAL_CORE)/jOVALCore.jar $(DIST)/x86/lib
	mkdir -p $(DIST)/x86/plugin/default/lib
	cp -R plugin/default/rsrc/* $(DIST)/x86/plugin/default
	cp -R $(COMPONENTS)/sdk/$(DIST)/jOVALPluginShared.jar $(DIST)/x86/plugin/default/lib
	cp -R $(COMPONENTS)/sdk/$(DIST)/jOVALPluginLocal.jar $(DIST)/x86/plugin/default/lib
	cp -R $(COMPONENTS)/sdk/plugin/local/$(LIBDIR)/* $(DIST)/x86/plugin/default/lib
	mkdir -p $(DIST)/x86/plugin/remote/lib
	cp -R plugin/remote/rsrc/* $(DIST)/x86/plugin/remote
	cp -R $(COMPONENTS)/sdk/$(DIST)/jOVALPluginShared.jar $(DIST)/x86/plugin/remote/lib
	cp -R $(COMPONENTS)/sdk/$(DIST)/jOVALPluginRemote.jar $(DIST)/x86/plugin/remote/lib
	cp -R $(COMPONENTS)/sdk/plugin/remote/$(LIBDIR)/*.jar $(DIST)/x86/plugin/remote/lib
	cp -R $(RSRC)/* $(DIST)/x86
#	cp -R $(JRE_HOME)/x86 $(DIST)/x86/jre

dist-x64: all
	mkdir -p $(DIST)/x64
	mkdir -p $(DIST)/x64/lib
	cp $(PRODUCTNAME).jar $(DIST)/x64/lib
	cp $(SCHEMALIB) $(DIST)/x64/lib
	cp $(JOVAL_CORE)/jOVALCore.jar $(DIST)/x64/lib
	mkdir -p $(DIST)/x64/plugin/default/lib
	cp -R plugin/default/rsrc/* $(DIST)/x64/plugin/default
	cp -R $(COMPONENTS)/sdk/$(DIST)/jOVALPluginShared.jar $(DIST)/x64/plugin/default/lib
	cp -R $(COMPONENTS)/sdk/$(DIST)/jOVALPluginLocal.jar $(DIST)/x64/plugin/default/lib
	cp -R $(COMPONENTS)/sdk/plugin/local/$(LIBDIR)/* $(DIST)/x64/plugin/default/lib
	mkdir -p $(DIST)/x64/plugin/remote/lib
	cp -R plugin/remote/rsrc/* $(DIST)/x64/plugin/remote
	cp -R $(COMPONENTS)/sdk/$(DIST)/jOVALPluginShared.jar $(DIST)/x64/plugin/remote/lib
	cp -R $(COMPONENTS)/sdk/$(DIST)/jOVALPluginRemote.jar $(DIST)/x64/plugin/remote/lib
	cp -R $(COMPONENTS)/sdk/plugin/remote/$(LIBDIR)/*.jar $(DIST)/x64/plugin/remote/lib
	cp -R $(RSRC)/* $(DIST)/x64
#	cp -R $(JRE_HOME)/x64 $(DIST)/x64/jre

release-win32: dist-x86
	$(JAVA_HOME)/bin/jar -cvfM $(ZIPNAME)_x86.zip -C $(DIST)/x86 .

release-win64: dist-x64
	$(JAVA_HOME)/bin/jar -cvfM $(ZIPNAME)_x64.zip -C $(DIST)/x64 .

release-tar: dist-$(ARCH)
	tar -cvf $(TARNAME).tar -C $(DIST)/$(ARCH) .
	gzip $(TARNAME).tar
