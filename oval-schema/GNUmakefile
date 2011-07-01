# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=..

include $(TOP)/common.mk

PRODUCTNAME=oval-schema-$(OVAL_SCHEMA_VERSION)

SCHEMADIR=schema-$(OVAL_SCHEMA_VERSION)
CLASSPATH="$(CLASSLIB)$(CLN)$(LIB)$(CLN)$(GENSRC)"

include classes.mk
include schemas.mk

SCHEMA_FILES:=$(foreach schema, $(SCHEMAS), $(SCHEMADIR)/$(schema))
CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

all: $(PRODUCTNAME).jar

$(PRODUCTNAME).jar: sources classes resources
	$(JAR) cvf $@ -C $(BUILD)/ .

javadocs: sources
	$(JAVA_HOME)/bin/javadoc -J-Xmx512m -d $(DOCS) -classpath $(CLASSPATH) $(PACKAGES)

clean:
	rm -rf $(BUILD)
	rm -rf $(GENSRC)/*
	rm -f $(PRODUCTNAME).jar

resources:
	cp $(RSRC)/* $(BUILD)

classes: classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

sources:
	$(XJC) $(SCHEMA_FILES) -d $(GENSRC)

$(GENSRC):
	mkdir $@

nowhere/%.xsd: $(SCHEMADIR)/%.xsd
	$(XJC) $< -d $(GENSRC)
	touch $@

$(BUILD)/%.class: $(GENSRC)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $(subst PKG,,$@)
