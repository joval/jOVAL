# Copyright (C) 2011, 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../..

include $(TOP)/common.mk

JOVALDI_LIB=$(COMPONENTS)/jovaldi/jovaldi.jar
API=$(JSAF_CORE_LIB)$(CLN)$(JOVAL_CORE_LIB)$(CLN)$(JOVAL_CORE_DEPS)$(CLN)$(SCAP_LIB)$(CLN)$(JOVALDI_LIB)
CLASSPATH="$(CLASSLIB)$(CLN)$(LIB)$(CLN)$(API)$(CLN)$(SRC)"
RUNTIMECP="$(CLASSLIB)$(CLN)$(LIB)$(CLN)$(API)$(CLN)$(XPERT_LIB)"

include classes.mk

CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

all: $(XPERT_LIB)

$(XPERT_LIB): classes resources
	$(JAR) cvf $@ -C $(BUILD)/ .

clean:
	rm -rf $(BUILD)
	rm -f $(XPERT_LIB)

classes: classdirs $(CLASS_FILES)

resources:
	rm -f $(BUILD)/*.properties
	cp $(RSRC)/*.properties $(BUILD)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

$(BUILD)/%.class: $(SRC)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $(subst PKG,,$@)

test:
	$(JRE) -classpath $(RUNTIMECP) org.joval.scap.xccdf.engine.XPERT $(TOP)/content/xccdf/u_windows_7_v1r7_stig_benchmark_20120127
#	$(JRE) -classpath $(RUNTIMECP) org.joval.scap.xccdf.engine.XPERT $(TOP)/content/xccdf/Win7-54-1.2.0.0/Win7
