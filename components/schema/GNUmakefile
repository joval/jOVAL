# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../..

include $(TOP)/common.mk

SCHEMADIR=xsd-$(OVAL_SCHEMA_VERSION)
BINDINGS=$(SCHEMADIR)/bindings.xjb
CLASSPATH="$(CLASSLIB)$(CLN)$(LIB)$(CLN)$(GENSRC)"

include classes.mk
include schemas.mk

SCHEMA_FILES:=$(foreach schema, $(SCHEMAS), $(SCHEMADIR)/$(schema))
CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

all: $(SCHEMALIB)

$(SCHEMALIB): sources classes resources javadocs
	$(JAR) cvf $@ -C $(BUILD)/ .

javadocs: sources
	$(JAVA_HOME)/bin/javadoc -J-Xmx512m -d $(DOCS) -sourcepath $(GENSRC) -subpackages oval:org

clean:
	rm -f $(SCHEMALIB)
	rm -rf $(BUILD)
	rm -rf $(GENSRC)
	rm -rf $(DOCS)

resources:
	cp $(RSRC)/* $(BUILD)

classes: classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

sources: $(GENSRC)
	$(XJC) $(SCHEMA_FILES) -b $(BINDINGS) -d $(GENSRC)

$(GENSRC):
	mkdir -p $@

$(BUILD)/%.class: $(GENSRC)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $(subst PKG,,$@)
