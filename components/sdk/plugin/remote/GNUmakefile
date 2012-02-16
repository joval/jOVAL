# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../../../..

include $(TOP)/common.mk

SLF4J_IMPL=$(COMPONENTS)/jovaldi/$(LIBDIR)/slf4j-jdk14-1.6.2.jar
API=$(JOVAL_CORE_LIB)$(CLN)$(JOVAL_CORE_DEPS)$(CLN)$(OVAL_LIB)
CLASSPATH="$(CLASSLIB)$(CLN)$(API)$(CLN)$(LIB)$(CLN)$(PLUGIN_SHARED_LIB)$(CLN)$(SRC)"
RUNTIMECP="$(CLASSLIB)$(CLN)$(API)$(CLN)$(LIB)$(CLN)$(PLUGIN_SHARED_LIB)$(CLN)$(PLUGIN_REMOTE_LIB)$(CLN)$(SLF4J_IMPL)"

include classes.mk

CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

all: $(PLUGIN_REMOTE_LIB)

$(PLUGIN_REMOTE_LIB): classes resources
	$(JAR) cvf $@ -C $(BUILD)/ .

netconf:
	$(JRE) -Djava.library.path=$(LIBDIR) -classpath $(RUNTIMECP) org.joval.test.Netconf test.properties

test:
	$(JRE) -Djava.library.path=$(LIBDIR) -classpath $(RUNTIMECP) org.joval.test.Remote test.properties

clean:
	rm -rf $(BUILD)
	rm -f $(PLUGIN_REMOTE_LIB)

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
