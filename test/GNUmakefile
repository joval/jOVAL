# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

#
# Note, you must make the components before you can build or run the tests.
#

TOP=..

include $(TOP)/common.mk

REPORT_SCHEMA_LIB=schema/ReportSchema.jar
PLUGIN=$(PLUGIN_SHARED_LIB)$(CLN)$(PLUGIN_SHARED_DEPS)$(CLN)$(PLUGIN_REMOTE_LIB)$(CLN)$(PLUGIN_REMOTE_DEPS)$(CLN)$(PLUGIN_LOCAL_LIB)$(CLN)$(PLUGIN_LOCAL_DEPS)
API=$(SCHEMALIB)$(CLN)$(JOVAL_CORE_LIB)$(CLN)$(JOVAL_CORE_DEPS)$(CLN)$(REPORT_SCHEMA_LIB)
CLASSPATH="$(CLASSLIB)$(CLN)$(API)$(CLN)$(PLUGIN)$(CLN)$(LIB)$(CLN)$(SRC)"
RUNTIMECP="$(CLASSLIB)$(CLN)$(API)$(CLN)$(PLUGIN)$(CLN)$(LIB)$(CLN)$(BUILD)"

include classes.mk

CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

all: $(REPORT_SCHEMA_LIB) classes

test:
	$(JRE) -classpath $(RUNTIMECP) -Djava.library.path=$(PLUGIN_LOCAL)/$(LIBDIR) org.joval.test.automation.Main test.ini

clean:
	@$(MAKE) --keep-going --directory=schema clean
	rm -rf $(BUILD)

classes: classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

$(BUILD)/%.class: $(SRC)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $(subst PKG,,$@)

$(REPORT_SCHEMA_LIB): schema/xsd/report.xsd
	@$(MAKE) --keep-going --directory=schema all
