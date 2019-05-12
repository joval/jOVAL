# Copyright (C) 2015 jOVAL.org.  All rights reserved.
# This software is licensed under the LGPL 3.0 license available at http://www.gnu.org/licenses/lgpl.txt

TOP=..
include $(TOP)/common.mk

CLASSPATH="$(CLASSLIB)$(CLN)$(GEN)"

include classes.mk

SCHEMA_FILES:=$(shell for file in `find $(SCHEMADIR) -type f -name *.xsd | grep -v xml.xsd | grep -v unofficial`; do echo $$file; done)
OVAL_DOCS:=$(foreach schema, $(wildcard $(OVAL_SCHEMA)/*.xsd), $(DOCS)/$(notdir $(basename $(schema))).html)
CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

SCHEMATRON=schematron
SCH=gen-sch
OVAL_SCH:=$(foreach file, $(wildcard $(SCHEMATRON)/*.xml), $(SCH)/$(notdir $(basename $(file))).xsl)

#
# Make namespaces optional in the episode bindings
#
BROKEN=<bindings scd=\"x-schema::tns\"
FIXED=<bindings scd=\"x-schema::tns\" if-exists=\"true\"

all: $(SCAP_LIB) $(OVAL_DOCS) $(DOCS)/index.html schematrons

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
	rm -rf $(OVAL_SCHEMA)/tmp

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

$(GEN): _oval $(SCHEMA_FILES)
	mkdir -p $@
	$(XJC) $(XJCFLAGS_EXT) $(SCHEMA_FILES) -b $(BINDINGS) -d $(GEN)

$(BUILD)/%.class: $(GEN)/%.java
	$(JAVAC) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $@

$(DOCS)/index.html: $(RSRC)/index.html
	sed 's/\[VERSION\]/$(OVAL_VERSION)/g' $< > $@

$(DOCS)/%.html: $(OVAL_SCHEMA)/tmp/%.xsd
	$(JAVA) -jar $(SAXON_LIB) -s:$< -xsl:rsrc/oval_xsd2html.xsl -o:$@

$(SCH):
	mkdir -p $@

$(SCH)/%.sch: $(SCHEMATRON)/%.xml
	$(JAVA) -jar $(SAXON_LIB) -s:$< -xsl:rsrc/ExtractSchFromXSD.xsl -o:$@

%.xsl: %.sch
	$(JAVA) -jar $(SAXON_LIB) -s:$< -xsl:rsrc/iso_svrl_for_xslt1.xsl -o:$@

OVAL_SCHEMA_FILES:=$(foreach schema, $(shell find $(OVAL_OFFICIAL) -name *.xsd ! -name xmldsig*), $(OVAL_SCHEMA)/tmp/$(notdir $(schema)))

_oval: $(OVAL_SCHEMA)/tmp $(OVAL_SCHEMA_FILES)

$(OVAL_SCHEMA)/tmp:
	mkdir -p $@

$(OVAL_SCHEMA)/tmp/%.xsd: $(OVAL_OFFICIAL)/%.xsd
	sed 's/ schemaLocation=.*\//\//g' $< > $@
