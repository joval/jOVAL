# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../../../..

include $(TOP)/common.mk

API=$(JOVAL_CORE)/jOVALCore.jar$(CLN)$(JOVAL_CORE_DEPS)$(CLN)$(SCHEMALIB)
SHARED=$(PLUGIN_SHARED)/jOVALPluginShared.jar
CLASSPATH="$(CLASSLIB)$(CLN)$(LIB)$(CLN)$(SRC)$(CLN)$(API)$(CLN)$(SHARED)"
RUNTIMECP="$(CLASSLIB)$(CLN)$(API)$(CLN)$(LIB)$(CLN)$(SHARED)$(CLN)$(PLUGIN_REMOTE_LIB)"

include classes.mk

CLASS_FILES:=$(foreach class, $(REMOTE), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(REMOTE)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

all: $(PLUGIN_REMOTE_LIB)

$(PLUGIN_REMOTE_LIB): classes resources
	$(JAR) cvf $@ -C $(BUILD)/ .

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
