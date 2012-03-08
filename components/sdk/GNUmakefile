# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../..

include $(TOP)/common.mk

SOURCEPATH="$(OVAL)/gen-src$(CLN)$(SRC)"
CLASSPATH="$(CLASSLIB)$(CLN)$(PLUGIN_SHARED_DEPS)$(CLN)$(PLUGIN_REMOTE_DEPS)$(CLN)$(PLUGIN_LOCAL_DEPS)$(CLN)$(PLUGIN_CISCO_DEPS)$(CLN)$(JOVAL_CORE_DEPS)"

all:
	@$(MAKE) --keep-going --directory=engine all
	@$(MAKE) --keep-going --directory=plugin/shared all
	@$(MAKE) --keep-going --directory=plugin/local all
	@$(MAKE) --keep-going --directory=plugin/remote all
	@$(MAKE) --keep-going --directory=plugin/cisco all

clean:
	@$(MAKE) --keep-going --directory=engine clean
	@$(MAKE) --keep-going --directory=plugin/shared clean
	@$(MAKE) --keep-going --directory=plugin/local clean
	@$(MAKE) --keep-going --directory=plugin/remote clean
	@$(MAKE) --keep-going --directory=plugin/cisco clean

javadocs:
	mkdir -p $(DOCS)
	$(JAVA_HOME)/bin/javadoc -quiet -J-Xmx512m -d $(DOCS) -classpath $(CLASSPATH) -sourcepath $(SOURCEPATH) -subpackages \
	oval:org.w3c            				\
	org.joval.intf.identity					\
	org.joval.intf.oval					\
	org.joval.intf.plugin					\
	org.joval.intf.util					\
	$(SRC)/org/joval/oval/CollectException.java		\
	$(SRC)/org/joval/oval/OvalException.java		\
	$(SRC)/org/joval/oval/ResolveException.java		\
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
	cp $(OVAL_LIB) $(DIST)
	cp $(JOVAL_CORE_LIB) $(DIST)
	$(JAR) cvf $(DIST)/jOVALRemote.jar -C $(PLUGIN_SHARED)/$(BUILD)/ .
	$(JAR) uvf $(DIST)/jOVALRemote.jar -C $(PLUGIN_REMOTE)/$(BUILD)/ .

dist: install javadocs
	cp -r docs $(DIST)
