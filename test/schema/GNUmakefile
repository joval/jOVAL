# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../..

include $(TOP)/common.mk

PRODUCTNAME=ReportSchema

SCHEMADIR=xsd
GEN=gen-src
BINDINGS=$(SCHEMADIR)/bindings.xjb
CLASSPATH="$(CLASSLIB)$(CLN)$(LIB)$(CLN)$(GEN)"

include classes.mk
include schemas.mk

SCHEMA_FILES:=$(foreach schema, $(SCHEMAS), $(SCHEMADIR)/$(schema))
CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

all: $(PRODUCTNAME).jar

$(PRODUCTNAME).jar: sources classes javadocs
	$(JAR) cvf $@ -C $(BUILD)/ .

javadocs: sources
	$(JAVA_HOME)/bin/javadoc -J-Xmx512m -d $(DOCS) -sourcepath $(GEN) -subpackages org.joval

clean:
	rm -f $(PRODUCTNAME).jar
	rm -rf $(BUILD)
	rm -rf $(GEN)
	rm -rf $(DOCS)

classes: classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

sources: $(GEN)
	$(XJC) $(XJCFLAGS) $(SCHEMA_FILES) -b $(BINDINGS) -d $(GEN)

$(GEN):
	mkdir -p $@

$(BUILD)/%.class: $(GEN)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $(subst PKG,,$@)
