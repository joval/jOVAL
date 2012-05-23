# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../..

include $(TOP)/common.mk

SOURCEPATH="$(OVAL)/gen-src$(CLN)$(SRC)"
API=$(PLUGIN_REMOTE_DEPS)$(CLN)$(PLUGIN_LOCAL_DEPS)$(CLN)$(PLUGIN_CISCO_DEPS)$(CLN)$(COMMON)$(CLN)$(COMMON_DEPS)
CLASSPATH="$(CLASSLIB)$(CLN)$(API)"

all:
	@$(MAKE) --keep-going --directory=common all
	@$(MAKE) --keep-going --directory=engine all
	@$(MAKE) --keep-going --directory=adapters all
	@$(MAKE) --keep-going --directory=plugin/local all
	@$(MAKE) --keep-going --directory=plugin/remote all
	@$(MAKE) --keep-going --directory=plugin/offline all

clean:
	@$(MAKE) --keep-going --directory=common clean
	@$(MAKE) --keep-going --directory=engine clean
	@$(MAKE) --keep-going --directory=adapters clean
	@$(MAKE) --keep-going --directory=plugin/local clean
	@$(MAKE) --keep-going --directory=plugin/remote clean
	@$(MAKE) --keep-going --directory=plugin/offline clean

javadocs:
	mkdir -p $(DOCS)
	$(JAVA_HOME)/bin/javadoc -quiet -J-Xmx512m -XDignore.symbol.file=true -d $(DOCS) -classpath $(CLASSPATH) -sourcepath $(SOURCEPATH) -subpackages \
	oval:org.w3c            				\
	org.joval.intf.identity					\
	org.joval.intf.oval					\
	org.joval.intf.plugin					\
	org.joval.intf.util					\
	$(SRC)/org/joval/oval/CollectException.java		\
	$(SRC)/org/joval/oval/OvalException.java		\
	$(SRC)/org/joval/plugin/BasePlugin.java			\
	$(SRC)/org/joval/plugin/RemotePlugin.java		\
	$(SRC)/org/joval/intf/system/IBaseSession.java		\
	$(SRC)/org/joval/intf/system/IProcess.java		\
	$(SRC)/org/joval/util/JOVALSystem.java			\
	$(SRC)/org/joval/util/JOVALMsg.java

dist-clean: clean
	rm -rf $(DIST)
	rm -rf $(DOCS)

install: all
	mkdir -p $(DIST)
	cp -r $(RSRC)/* $(DIST)
	cp sample/TrivialScanner.java $(DIST)
	cp $(OVAL_LIB) $(DIST)
	$(JAR) cvf $(DIST)/jOVAL-SDK.jar -C $(COMMON)/$(BUILD)/ .
	$(JAR) uvf $(DIST)/jOVAL-SDK.jar -C $(PLUGIN_REMOTE)/$(BUILD)/ .
	$(JAR) uvf $(DIST)/jOVAL-SDK.jar -C $(JOVAL_CORE)/$(BUILD)/ .
	$(JAR) uvf $(DIST)/jOVAL-SDK.jar -C $(ADAPTERS)/$(BUILD)/ .

dist: install javadocs
	cp -r docs $(DIST)
