# Copyright (C) 2015-2021 jOVAL.org.  All rights reserved.
# This software is licensed under the LGPL 3.0 license available at http://www.gnu.org/licenses/lgpl.txt

TOP=..
include $(TOP)/common.mk

CLASSPATH="$(CLASSLIB)$(CLN)$(GEN)"

include classes.mk

SCHEMA_FILES:=$(shell for file in `find $(SCHEMADIR) -type f -name *.xsd | grep -v xml.xsd | grep -v unofficial`; do echo $$file; done)
CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

SCHEMATRON=schematron
SCH=gen-sch
OVAL_SCH:=$(foreach file, $(wildcard $(SCHEMATRON)/*.xml), $(SCH)/$(notdir $(basename $(file)))-schematron.xsl)

OVAL_OFFICIAL=$(OVAL_SCHEMA)/official
OVAL_SCHEMA_FILES:=$(foreach schema, $(shell find $(OVAL_SRC) -name *.xsd ! -name xmldsig*), $(OVAL_OFFICIAL)/$(notdir $(schema)))
OVAL_DOCS:=$(foreach schema, $(shell find $(OVAL_SRC) -name *.xsd ! -name xmldsig*), $(DOCS)/$(notdir $(basename $(schema))).html)
OVAL_EXT_DOCS:=$(foreach schema, $(shell find $(SCAP_EXT)/$(SCHEMADIR) -name *.xsd ! -name xmldsig*), $(DOCS)/ext/$(notdir $(basename $(schema))).html)

#
# Make namespaces optional in the episode bindings
#
BROKEN=<bindings scd=\"x-schema::tns\"
FIXED=<bindings scd=\"x-schema::tns\" if-exists=\"true\"

all: $(SCAP_LIB) $(DOCS)/index.html ovaldocs schematrons

schematrons: $(OVAL_SCH)

$(SCAP_LIB): $(GEN) classes resources
	$(JAR) cvf $@ -C $(BUILD)/ .

clean:
	rm -f $(EPISODE)
	rm -f $(SCAP_LIB)
	rm -rf $(BUILD)
	rm -rf $(GEN)
	rm -rf $(DOCS)
	rm -rf $(SCH)
	rm -rf $(OVAL_OFFICIAL)

resources:
	mkdir -p $(BUILD)/META-INF
	sed 's/$(BROKEN)/$(FIXED)/' $(EPISODE) > $(BUILD)/META-INF/sun-jaxb.episode
	mkdir -p $(BUILD)/scap/schemas
	cp $(CATALOG) $(BUILD)/scap
	cp $(RSRC)/registry.ini $(BUILD)/scap
	cp -R $(SCHEMADIR)/* $(BUILD)/scap/schemas
	rm $(BUILD)/scap/schemas/bindings.xjb

classes: classdirs
	$(MAKE) -j$(NUMPROCS) classfiles

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

classfiles: $(CLASS_FILES)

$(GEN): $(OVAL_OFFICIAL) $(OVAL_SCHEMA_FILES) $(SCHEMA_FILES)
	mkdir -p $@
	$(XJC) $(XJCFLAGS_EXT) $(SCHEMA_FILES) -b $(BINDINGS) -d $(GEN)

$(BUILD)/%.class: $(GEN)/%.java
	$(JAVAC) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $@

$(DOCS)/index.html: $(RSRC)/index.html
	mkdir -p $(DOCS)/ext
	sed 's/\[VERSION\]/$(OVAL_VERSION)/g' $< > $@

ovaldocs:
	$(MAKE) -j$(NUMPROCS) $(OVAL_DOCS) $(OVAL_EXT_DOCS)

$(DOCS)/ext/%.html: $(SCAP_EXT)/$(SCHEMADIR)/%.xsd
	$(JAVA) -jar $(SAXON_LIB) -s:$< -xsl:rsrc/oval_xsd2html.xsl -o:$@

$(DOCS)/%.html: $(OVAL_SCHEMA)/merged/%.xsd
	$(JAVA) -jar $(SAXON_LIB) -s:$< -xsl:rsrc/oval_xsd2html.xsl -o:$@

$(SCH):
	mkdir -p $@

$(SCH)/%-schematron.sch: $(SCHEMATRON)/%.xml
	$(JAVA) -jar $(SAXON_LIB) -s:$< -xsl:rsrc/ExtractSchFromXSD.xsl -o:$@

%.xsl: %.sch
	$(JAVA) -jar $(SAXON_LIB) -s:$< -xsl:rsrc/iso_svrl_for_xslt1.xsl -o:$@

$(OVAL_OFFICIAL):
	mkdir -p $@

$(OVAL_OFFICIAL)/oval-common-schema.xsd: $(OVAL_SRC)/oval-common-schema.xsd
	$(JAVA) -jar $(SAXON_LIB) -s:$< -xsl:$(OVAL_SCHEMA)/unofficial/strip-redefined-common.xsl -o:$@
	sed -i.bak 's/ schemaLocation=.*\//\//g' $@
	rm $(OVAL_OFFICIAL)/*.bak

$(OVAL_OFFICIAL)/independent-%.xsd: $(OVAL_SRC)/independent-%.xsd
	$(JAVA) -jar $(SAXON_LIB) -s:$< -xsl:$(OVAL_SCHEMA)/unofficial/strip-redefined-independent.xsl -o:$@
	sed -i.bak 's/ schemaLocation=.*\//\//g' $@
	rm $(OVAL_OFFICIAL)/*.bak

$(OVAL_OFFICIAL)/%.xsd: $(OVAL_SRC)/%.xsd
	sed 's/ schemaLocation=.*\//\//g' $< > $@
