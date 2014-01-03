# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../..

include $(TOP)/common.mk

API=$(JSAF_CORE_LIB)$(CLN)$(JSAF_CORE_DEPS)$(CLN)$(SCAP_LIB)$(CLN)$(SCAP_EXT_LIB)
CLASSPATH="$(CLASSLIB)$(CLN)$(LIBS)$(CLN)$(API)$(CLN)$(SRC)"
RUNTIMECP="$(CLASSLIB)$(CLN)$(LIBS)$(CLN)$(API)$(CLN)$(JOVAL_CORE_LIB)"

include classes.mk

CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

all: $(JOVAL_CORE_LIB)

$(JOVAL_CORE_LIB): classes resources
	$(JAR) cvf $@ -C $(BUILD)/ .

resources:
	rm -f $(BUILD)/*.properties
	cp $(RSRC)/*.properties $(BUILD)
	cp $(RSRC)/functions.txt $(BUILD)
	rm -f $(BUILD)/org/joval/scap/xccdf/*.xsl
	cp $(SRC)/org/joval/scap/xccdf/xccdf_convert_1.1.4_to_1.2.xsl $(BUILD)/org/joval/scap/xccdf
	echo "productName=jOVAL(tm) SDK" >> $(BUILD)/joval.system.properties
	echo "version=$(JOVAL_VERSION)" >> $(BUILD)/joval.system.properties
	echo "build.date=`date`" >> $(BUILD)/joval.system.properties

clean:
	rm -rf $(BUILD)
	rm -f $(JOVAL_CORE_LIB)

classes: classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

$(BUILD)/%.class: $(SRC)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $(subst PKG,,$@)

test:
#	$(JRE) -Djava.library.path=$(LIBDIR) -classpath $(RUNTIMECP) org.joval.scap.oval.types.EvrStringType 32:9.8.2-0.17.rc1.el6.3 32:9.8.2-0.17.rc1.el6_4.4
	$(JRE) -Djava.library.path=$(LIBDIR) -classpath $(RUNTIMECP) org.joval.xml.XPathTools gconf.xml /gconf/entry[@name=\'idle_delay\']/@value
