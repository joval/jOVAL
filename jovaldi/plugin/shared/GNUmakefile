# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../../..

include $(TOP)/common.mk

PRODUCTNAME=plugin-shared

API=$(JOVAL)/jOVAL.jar;$(OVAL)/oval-schema-5.9.jar
CLASSPATH="$(CLASSLIB)$(CLN)$(LIB)$(CLN)$(SRC)$(CLN)$(API)$(CLN)$(DEPS)"

include classes.mk

CLASS_FILES:=$(foreach class, $(SHARED), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(SHARED)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

all: $(PRODUCTNAME).jar

install: all
	cp $(PRODUCTNAME).jar ../default/
	cp $(PRODUCTNAME).jar ../remote/

$(PRODUCTNAME).jar: classes
	$(JAR) cvf $@ -C $(BUILD)/ .

dist-clean: clean
	rm -f ../default/$(PRODUCTNAME).jar
	rm -f ../remote/$(PRODUCTNAME).jar

clean:
	rm -rf $(BUILD)
	rm -f $(PRODUCTNAME).jar

classes: classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

$(BUILD)/%.class: $(SRC)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $(subst PKG,,$@)
