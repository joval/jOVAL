# Copyright (C) 2015 jOVAL.org.  All rights reserved.
# This software is licensed under the LGPL 3.0 license available at http://www.gnu.org/licenses/lgpl.txt

TOP=..
include $(TOP)/common.mk

CLASSPATH="$(CLASSLIB)$(CLN)$(GEN)"

include classes.mk
include schemas.mk

SCHEMA_FILES:=$(foreach schema, $(SCHEMAS), $(SCHEMADIR)/$(schema))
CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

#
# Make namespaces optional in the episode bindings
#
BROKEN=<bindings scd=\"x-schema::tns\"
FIXED=<bindings scd=\"x-schema::tns\" if-exists=\"true\"

all: $(SCAP_LIB)

$(SCAP_LIB): $(GEN) classes resources
	$(JAR) cvf $@ -C $(BUILD)/ .

clean:
	rm -f $(EPISODE)
	rm -f $(SCAP_LIB)
	rm -rf $(BUILD)
	rm -rf $(GEN)

resources:
	mkdir -p $(BUILD)/META-INF
	sed 's/$(BROKEN)/$(FIXED)/' $(EPISODE) > $(BUILD)/META-INF/sun-jaxb.episode
	mkdir -p $(BUILD)/scap/schemas
	cp $(CATALOG) $(BUILD)/scap
	cp $(RSRC)/registry.ini $(BUILD)/scap
	cp -R $(SCHEMADIR)/* $(BUILD)/scap/schemas
	rm $(BUILD)/scap/schemas/bindings.xjb

classes: classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

$(GEN): $(SCHEMA_FILES)
	mkdir -p $@
	$(XJC) $(XJCFLAGS) $(SCHEMA_FILES) -b $(BINDINGS) -d $(GEN)

$(BUILD)/%.class: $(GEN)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $@
