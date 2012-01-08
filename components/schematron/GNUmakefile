# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../..

include $(TOP)/common.mk

SCHDIR=sch-$(OVAL_SCHEMA_VERSION)

CLASSPATH="$(CLASSLIB)$(CLN)$(JOVAL_CORE_LIB)$(CLN)$(JOVAL_CORE_DEPS)$(CLN)$(SCHEMALIB)$(CLN)$(SRC)"
RUNTIMECP="$(CLASSLIB)$(CLN)$(JOVAL_CORE_LIB)$(CLN)$(JOVAL_CORE_DEPS)$(CLN)$(SCHEMALIB)$(CLN)$(BUILD)"

include classes.mk

CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

include schemas.mk

TRANSFORM_FILES:=$(foreach base, $(SCHEMAS), $(base).xsl)

all: classes $(TRANSFORM_FILES)

clean:
	rm -rf $(BUILD)
	rm -f *.xsl

test:
	$(JRE) -classpath $(RUNTIMECP) org.joval.oval.xml.SchematronValidator definitions $(TOP)/content/test/microsoft.windows.7.xml oval-definitions-schematron.xsl out.xml

%.xsl: $(SCHDIR)/%.sch
	$(JRE) -classpath $(RUNTIMECP) org.joval.oval.xml.SchematronCompiler $(RSRC)/ISO $< $@

sources:
	mkdir -p gen-src
	$(XJC) xsd/svrl.xsd -b xsd/bindings.xjb -d gen-src

classes: classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

$(BUILD)/%.class: $(SRC)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $(subst PKG,,$@)
