# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../../..

include $(TOP)/common.mk

SCHEMADIR=schemas
GEN=gen-src
BINDINGS=$(SCHEMADIR)/bindings.xjb
CATALOG=schemas.cat
EPISODE=schemas.episode
XJCFLAGS_EXT=-catalog $(CATALOG) -episode $(EPISODE)
CLASSPATH="$(CLASSLIB)$(CLN)$(GEN)"

include classes.mk

SCHEMA_FILES=$(SCHEMADIR)/svrl.xsd
CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

#
# Make namespaces optional in the episode bindings
#
BROKEN=<bindings scd=\"x-schema::tns\"
FIXED=<bindings scd=\"x-schema::tns\" if-exists=\"true\"

all: $(SVRL)

$(SVRL): $(GEN) classes resources $(DOCS)
	$(JAR) cvf $@ -C $(BUILD)/ .

$(DOCS): $(GEN)
	$(JAVA_HOME)/bin/javadoc -J-Xmx512m -d $(DOCS) -sourcepath $(GEN) -subpackages org.iso.svrl

clean:
	rm -f $(EPISODE)
	rm -f $(SVRL)
	rm -rf $(BUILD)
	rm -rf $(GEN)
	rm -rf $(DOCS)

resources:
	mkdir -p $(BUILD)/META-INF
	sed 's/$(BROKEN)/$(FIXED)/' $(EPISODE) > $(BUILD)/META-INF/sun-jaxb.episode
	mkdir -p $(BUILD)/svrl/schemas
	cp $(RSRC)/registry.ini $(BUILD)/svrl
	cp $(CATALOG) $(BUILD)/svrl
	cp -R $(SCHEMADIR)/* $(BUILD)/svrl/schemas
	rm $(BUILD)/svrl/schemas/bindings.xjb

classes: $(GEN) classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

$(GEN): $(SCHEMA_FILES)
	mkdir -p $@
	$(XJC) $(XJCFLAGS) $(XJCFLAGS_EXT) $(SCHEMA_FILES) -b $(BINDINGS) -d $(GEN)

$(BUILD)/%.class: $(GEN)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $(subst PKG,,$@)
