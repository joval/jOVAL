# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../..

include $(TOP)/common.mk

SOURCEPATH="$(GENSRC)$(CLN)$(SRC)"
CLASSPATH="$(PLUGIN_SHARED_DEPS)$(CLN)$(PLUGIN_REMOTE_DEPS)$(CLN)$(JOVAL_CORE_DEPS)"

all:
	@$(MAKE) --keep-going --directory=engine all
	@$(MAKE) --keep-going --directory=plugin/shared all
	@$(MAKE) --keep-going --directory=plugin/local all
	@$(MAKE) --keep-going --directory=plugin/remote all

clean:
	@$(MAKE) --keep-going --directory=engine clean
	@$(MAKE) --keep-going --directory=plugin/shared clean
	@$(MAKE) --keep-going --directory=plugin/local clean
	@$(MAKE) --keep-going --directory=plugin/remote clean

javadocs:
	mkdir -p $(DOCS)
	$(JAVA_HOME)/bin/javadoc -J-Xmx512m -d $(DOCS) -classpath $(CLASSPATH) -sourcepath $(SOURCEPATH) -subpackages \
	oval:org.w3c            				\
	org.joval.intf.identity					\
	org.joval.intf.oval					\
	org.joval.intf.plugin					\
	org.joval.intf.util					\
	org.joval.plugin					\
	$(SRC)/org/joval/intf/system/IBaseSession.java		\
	$(SRC)/org/joval/intf/system/IProcess.java		\
	$(SRC)/org/joval/util/JOVALSystem.java			\
	$(SRC)/org/joval/util/JOVALMsg.java

dist-clean: clean
	rm -rf $(DIST)
	rm -rf $(DOCS)

install: all
	mkdir -p $(DIST)
	cp $(SCHEMALIB) $(DIST)
	cp $(JOVAL_CORE_LIB) $(DIST)
	cp $(PLUGIN_SHARED_LIB) $(DIST)
	cp $(PLUGIN_LOCAL_LIB) $(DIST)
	cp $(PLUGIN_REMOTE_LIB) $(DIST)
	cp -r $(RSRC)/* $(DIST)

dist: install javadocs
	cp -r docs $(DIST)
