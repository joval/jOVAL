# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../..

include $(TOP)/common.mk

GEN=gen-src
CLASSPATH="$(CLASSLIB)$(CLN)$(JOVAL_CORE_LIB)$(CLN)$(COMMON_DEPS)$(CLN)$(SVRL)$(CLN)$(GEN)"

include schemas.mk
include classes.mk

CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

all: $(WSMAN_LIB)

$(WSMAN_LIB): classes resources $(DOCS)
	$(JAR) cvf $@ -C $(BUILD)/ .

install: $(WSMAN_LIB)
	cp $(WSMAN_LIB) $(PLUGIN_REMOTE)/$(RSRC)/lib

clean:
	rm -rf $(BUILD)
	rm -rf $(GEN)
	rm -rf $(DOCS)
	rm -f $(WSMAN_LIB)

$(DOCS): $(GEN)
	$(JAVA_HOME)/bin/javadoc -J-Xmx512m -d $(DOCS) -sourcepath $(GEN) $(PACKAGES)

$(GEN): $(SCHEMA_FILES)
	mkdir -p $@
	$(XJC) $(SCHEMA_FILES) -b bindings.xjb -d $(GEN)

resources:
	cp $(RSRC)/ws-man.properties $(BUILD)

classes: $(GEN) classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

$(BUILD)/%.class: $(GEN)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $(subst PKG,,$@)
