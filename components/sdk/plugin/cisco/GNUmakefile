# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../../../..

include $(TOP)/common.mk

SLF4J_IMPL=$(COMPONENTS)/jovaldi/$(LIBDIR)/slf4j-jdk14-1.6.2.jar
API=$(OVAL_LIB)$(CLN)$(COMMON_LIB)$(CLN)$(COMMON_DEPS)$(CLN)$(JOVAL_CORE_LIB)$(CLN)$(PLUGIN_SHARED_LIB)
CLASSPATH="$(CLASSLIB)$(CLN)$(LIB)$(CLN)$(SRC)$(CLN)$(API)$(CLN)$(PLUGIN_REMOTE_DEPS)"
RUNTIMECP="$(CLASSLIB)$(CLN)$(API)$(CLN)$(LIB)$(CLN)$(PLUGIN_CISCO_LIB)$(CLN)$(SLF4J_IMPL)"

include classes.mk

CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

all: $(PLUGIN_CISCO_LIB)

$(PLUGIN_CISCO_LIB): classes resources
	$(JAR) cvf $@ -C $(BUILD)/ .

test:
	$(JRE) -Djava.library.path=$(LIBDIR) -classpath $(RUNTIMECP) org.joval.plugin.CiscoPlugin $(TOP)/content/test/cisco/cisco.ios.xml ios-tech.txt

clean:
	rm -rf $(BUILD)
	rm -f $(PLUGIN_CISCO_LIB)

resources:
	rm -f $(BUILD)/*.properties
	cp $(RSRC)/*.properties $(BUILD)
	echo version=$(JOVAL_VERSION) >> $(BUILD)/plugin.resources.properties

classes: classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

$(BUILD)/%.class: $(SRC)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $(subst PKG,,$@)
