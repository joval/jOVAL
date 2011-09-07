# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=..

include $(TOP)/common.mk

PRODUCTNAME=jOVAL
ZIPNAME=$(PRODUCTNAME)_$(JOVAL_VERSION)_Windows
TARNAME=$(PRODUCTNAME)_$(JOVAL_VERSION)_`uname -s`_`uname -p`

CLASSPATH="$(CLASSLIB)$(CLN)$(LIB)$(CLN)$(SCHEMALIB)$(CLN)$(SRC)"

include classes.mk

CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

include plugin/shared/classes.mk
include plugin/default/classes.mk
include plugin/remote/classes.mk

ALL_CLASSES=$(CLASSES) $(SHARED) $(DEFAULT) $(REMOTE)
ALL_PACKAGES=$(sort $(basename $(ALL_CLASSES)))

all: $(PRODUCTNAME).jar $(PLUGIN_DIRS)
	@$(MAKE) --keep-going --directory=plugin/shared install
	@$(MAKE) --keep-going --directory=plugin/default all
	@$(MAKE) --keep-going --directory=plugin/remote all

install: all
	@$(MAKE) --keep-going --directory=plugin/shared install
	@$(MAKE) --keep-going --directory=plugin/default install
	@$(MAKE) --keep-going --directory=plugin/remote install

javadocs:
	$(JAVA_HOME)/bin/javadoc -d $(DOCS) -sourcepath $(SRC) $(ALL_PACKAGES)

$(PRODUCTNAME).jar: classes resources
	$(JAR) cvf $@ -C $(BUILD)/ .

dist-clean: clean
	rm -f $(ZIPNAME)*.zip
	rm -f $(TARNAME).tar.gz
	rm -rf $(DIST)
	@$(MAKE) --keep-going --directory=plugin/shared dist-clean
	@$(MAKE) --keep-going --directory=plugin/default dist-clean
	@$(MAKE) --keep-going --directory=plugin/remote dist-clean

clean:
	rm -rf $(BUILD)
	rm -f $(PRODUCTNAME).jar
	@$(MAKE) --keep-going --directory=plugin/shared clean
	@$(MAKE) --keep-going --directory=plugin/default clean
	@$(MAKE) --keep-going --directory=plugin/remote clean

resources:
	rm -f $(BUILD)/joval*
	cp $(SRC)/rsrc/joval* $(BUILD)
	echo "version=$(JOVAL_VERSION)" >> $(BUILD)/joval.system.properties
	echo "build.date=`date`" >> $(BUILD)/joval.system.properties

classes: classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

$(BUILD)/%.class: $(SRC)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $(subst PKG,,$@)

dist: dist-$(ARCH)

dist-x86: install
	mkdir -p $(DIST)/x86
	mkdir -p $(DIST)/x86/lib
	cp $(PRODUCTNAME).jar $(DIST)/x86/lib
	cp $(SCHEMALIB) $(DIST)/x86/lib
	mkdir -p $(DIST)/x86/plugin/default
	cp -R plugin/default/dist/* $(DIST)/x86/plugin/default
	mkdir -p $(DIST)/x86/plugin/remote
	cp -R plugin/remote/dist/* $(DIST)/x86/plugin/remote
	cp -R $(RSRC)/* $(DIST)/x86
	cp -R $(JRE_HOME)/x86 $(DIST)/x86/jre

dist-x64: install
	mkdir -p $(DIST)/x64
	mkdir -p $(DIST)/x64/lib
	cp $(PRODUCTNAME).jar $(DIST)/x64/lib
	cp $(SCHEMALIB) $(DIST)/x64/lib
	mkdir -p $(DIST)/x64/plugin/default
	cp -R plugin/default/dist/* $(DIST)/x64/plugin/default
	mkdir -p $(DIST)/x64/plugin/remote
	cp -R plugin/remote/dist/* $(DIST)/x64/plugin/remote
	cp -R $(RSRC)/* $(DIST)/x64
	cp -R $(JRE_HOME)/x64 $(DIST)/x64/jre

release-win32: dist-x86
	$(JAVA_HOME)/bin/jar -cvfM $(ZIPNAME)_x86.zip -C $(DIST)/x86 .

release-win64: dist-x64
	$(JAVA_HOME)/bin/jar -cvfM $(ZIPNAME)_x64.zip -C $(DIST)/x64 .

release-tar: dist-$(ARCH)
	tar -cvf $(TARNAME).tar -C $(DIST)/$(ARCH) .
	gzip $(TARNAME).tar
