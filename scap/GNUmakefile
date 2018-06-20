# Copyright (C) 2015 jOVAL.org.  All rights reserved.
# This software is licensed under the LGPL 3.0 license available at http://www.gnu.org/licenses/lgpl.txt

TOP=..
include $(TOP)/common.mk

CLASSPATH="$(CLASSLIB)$(CLN)$(GEN)"

include classes.mk

SCHEMA_FILES:=$(shell for file in `find $(SCHEMADIR) -type f -name *.xsd | grep -v xml.xsd`; do echo $$file; done)
OVAL_DOCS:=$(foreach schema, $(wildcard $(OVAL_SCHEMA)/*.xsd), $(DOCS)/$(notdir $(basename $(schema))).html)
CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

SCHEMATRON=schematron
SCH=gen-sch
OVAL_SCH:=$(SCH)/oval-definitions-schematron.xsl \
	  $(SCH)/oval-directives-schematron.xsl \
	  $(SCH)/oval-results-schematron.xsl \
	  $(SCH)/oval-system-characteristics-schematron.xsl \
	  $(SCH)/oval-variables-schematron.xsl \

#
# Make namespaces optional in the episode bindings
#
BROKEN=<bindings scd=\"x-schema::tns\"
FIXED=<bindings scd=\"x-schema::tns\" if-exists=\"true\"

all: $(SCAP_LIB) $(OVAL_DOCS) $(DOCS)/index.html schematrons

schematrons: $(OVAL_SCH)

$(SCAP_LIB): $(GEN) classes resources
	$(JAR) cvf $@ -C $(BUILD)/ .

$(CATALOG): $(CATALOG_TEMPLATE)
	cat $< | sed 's/\[OVAL_VERSION\]/$(OVAL_VERSION)/g' > $@

clean:
	rm -f $(CATALOG)
	rm -f $(EPISODE)
	rm -f $(SCAP_LIB)
	rm -rf $(BUILD)
	rm -rf $(GEN)
	rm -rf $(DOCS)
	rm -rf $(SCH)

resources:
	mkdir -p $(BUILD)/META-INF
	sed 's/$(BROKEN)/$(FIXED)/' $(EPISODE) > $(BUILD)/META-INF/sun-jaxb.episode
	mkdir -p $(BUILD)/scap/schemas
	mv $(CATALOG) $(BUILD)/scap
	cp $(RSRC)/registry.ini $(BUILD)/scap
	cp -R $(SCHEMADIR)/* $(BUILD)/scap/schemas
	rm $(BUILD)/scap/schemas/bindings.xjb

classes: classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

$(GEN): $(SCHEMA_FILES) $(CATALOG)
	mkdir -p $@
	$(XJC) $(XJCFLAGS_EXT) $(SCHEMA_FILES) -b $(BINDINGS) -d $(GEN)

$(BUILD)/%.class: $(GEN)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $@

$(DOCS)/index.html: $(RSRC)/index.html
	sed 's/\[VERSION\]/$(OVAL_VERSION)/g' $< > $@

$(DOCS)/%.html: $(OVAL_SCHEMA)/%.xsd
	$(JAVA) -jar $(SAXON_LIB) -s:$< -xsl:rsrc/oval_xsd2html.xsl -o:$@

$(SCH):
	mkdir -p $@

$(SCHEMATRON)/%.xml: $(SCHEMATRON)/%.template
	cat $< | sed 's/\[OVAL_VERSION\]/$(OVAL_VERSION)/g' > $@

$(SCH)/%-schematron.sch: $(SCHEMATRON)/%.xml
	$(JAVA) -jar $(SAXON_LIB) -s:$< -xsl:rsrc/ExtractSchFromXSD.xsl -o:$@

%.xsl: %.sch
	$(JAVA) -jar $(SAXON_LIB) -s:$< -xsl:rsrc/iso_svrl_for_xslt1.xsl -o:$@
