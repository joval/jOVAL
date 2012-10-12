# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../..

include $(TOP)/common.mk

GEN=gen-src
SCHEMADIR=schemas
BINDINGS=$(SCHEMADIR)/bindings.xjb
CLASSPATH="$(CLASSLIB)$(CLN)$(GEN)"

include classes.mk
include schemas.mk

SCHEMA_FILES:=$(foreach schema, $(SCHEMAS), $(SCHEMADIR)/$(schema))
CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

all: $(SCAP_LIB)

$(SCAP_LIB): $(GEN) classes resources $(DOCS)
	$(JAR) cvf $@ -C $(BUILD)/ .

$(DOCS): $(GEN)
	$(JAVA_HOME)/bin/javadoc -J-Xmx512m -d $(DOCS) -sourcepath $(GEN) -subpackages org:ai:arf:cpe:ocil:oval:scap:xccdf

clean:
	rm -f $(SCAP_LIB)
	rm -rf $(BUILD)
	rm -rf $(GEN)
	rm -rf $(DOCS)

resources:
	cp $(RSRC)/* $(BUILD)

classes: classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

$(GEN): $(SCHEMA_FILES)
	mkdir -p $@
	$(XJC) $(XJCFLAGS) $(SCHEMA_FILES) -b $(BINDINGS) -d $(GEN)

$(BUILD)/%.class: $(GEN)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $(subst PKG,,$@)
