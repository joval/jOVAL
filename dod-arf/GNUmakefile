# Copyright (C) 2013 jOVAL.org.  All rights reserved.
# This software is licensed under the LGPL 3.0 license available at http://www.gnu.org/licenses/lgpl.txt

TOP=..
include $(TOP)/common.mk

CLASSPATH="$(CLASSLIB)$(CLN)$(SCAP_LIB)$(CLN)$(GEN)"

include classes.mk

SCHEMA_FILES:=$(shell for file in `find $(SCHEMADIR) -type f -name *.xsd | grep -v xml.xsd`; do echo $$file; done)
CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

all: $(DODARF_LIB)

$(DODARF_LIB): $(GEN) classes resources $(DOCS)
	$(JAR) cvf $@ -C $(BUILD)/ .

$(DOCS): $(GEN)
	$(JAVADOC) $(JAVADOCFLAGS) -d $(DOCS) -sourcepath $(GEN) -subpackages gov:mil.disa

clean:
	rm -f $(EPISODE)
	rm -f $(DODARF_LIB)
	rm -rf $(BUILD)
	rm -rf $(GEN)
	rm -rf $(DOCS)

resources:
	mkdir -p $(BUILD)/META-INF
	sed 's/$(BROKEN)/$(FIXED)/' $(EPISODE) > $(BUILD)/META-INF/sun-jaxb.episode
	mkdir -p $(BUILD)/dodarf/schemas
	cp $(CATALOG) $(BUILD)/dodarf
	cp $(RSRC)/registry.ini $(BUILD)/dodarf
	cp -R $(SCHEMADIR)/* $(BUILD)/dodarf/schemas
	rm $(BUILD)/dodarf/schemas/bindings.xjb

classes: classdirs
	$(MAKE) -j$(NUMPROCS) classfiles

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

classfiles: $(CLASS_FILES)

$(GEN): $(SCHEMA_FILES)
	mkdir -p $@
	$(XJC) $(XJCFLAGS_EXT) $(SCHEMA_FILES) $(SCAP_LIB) -b $(BINDINGS) -d $(GEN)
	find $(GEN) -name ObjectFactory.java | grep scap\.cpe | xargs rm

$(BUILD)/%.class: $(GEN)/%.java
	$(JAVAC) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $@
