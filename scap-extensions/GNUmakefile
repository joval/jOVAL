# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=$(realpath ../..)

include $(TOP)/common.mk

GEN=gen-src
SCHEMADIR=schemas
BINDINGS=$(SCHEMADIR)/bindings.xjb
CATALOG=schemas.cat
EPISODE=schemas.episode
XJCFLAGS_EXT=-extension -catalog $(CATALOG) -episode $(EPISODE)
CLASSPATH="$(CLASSLIB)$(CLN)$(SCAP_LIB)$(CLN)$(GEN)"

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

all: $(SCAP_EXT_LIB)

$(SCAP_EXT_LIB): $(GEN) classes resources $(DOCS)
	$(JAR) cvf $@ -C $(BUILD)/ .

$(DOCS): $(GEN)
	$(JAVADOC) -J-Xmx512m -d $(DOCS) -sourcepath $(GEN) -subpackages org:scap

clean:
	rm -f $(EPISODE)
	rm -f $(SCAP_EXT_LIB)
	rm -rf $(BUILD)
	rm -rf $(GEN)
	rm -rf $(DOCS)

resources:
	mkdir -p $(BUILD)/META-INF
	sed 's/$(BROKEN)/$(FIXED)/' $(EPISODE) > $(BUILD)/META-INF/sun-jaxb.episode
	mkdir -p $(BUILD)/scapx/schemas
	cp $(CATALOG) $(BUILD)/scapx
	cp $(RSRC)/registry.ini $(BUILD)/scapx
	cp -R $(SCHEMADIR)/* $(BUILD)/scapx/schemas
	rm $(BUILD)/scapx/schemas/bindings.xjb

classes: classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

$(GEN): $(SCHEMA_FILES)
	mkdir -p $@
	$(XJC) $(XJCFLAGS) $(XJCFLAGS_EXT) $(SCHEMA_FILES) $(SCAP_LIB) -b $(BINDINGS) -d $(GEN)
	find $(GEN) -name ObjectFactory.java | grep core | xargs rm

$(BUILD)/%.class: $(GEN)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $@
