# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../..

include $(TOP)/common.mk

API=$(JSAF_CORE_LIB)$(CLN)$(JSAF_CORE_DEPS)$(CLN)$(JOVAL_CORE_LIB)$(CLN)$(SCAP_LIB)$(CLN)$(JPE_LIB)
CLASSPATH="$(CLASSLIB)$(CLN)$(API)$(CLN)$(LIBS)$(CLN)$(SRC)"
RUNTIMECP="$(CLASSLIB)$(CLN)$(API)$(CLN)$(LIBS)$(CLN)$(PLUGIN_LOCAL_LIB)"

include classes.mk

CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

all: $(PLUGIN_LOCAL_LIB)

$(PLUGIN_LOCAL_LIB): classes resources
	$(JAR) cvf $@ -C $(BUILD)/ .

test:
	$(JRE) -Djava.library.path=$(LIBDIR) -classpath $(RUNTIMECP) org.joval.test.Default test.properties

native:
	cat CSharpCompiler.ps1 | powershell -File -

native-clean:
	rm -f $(RSRC)/assembly/Environmentvariable58.dll
	rm -f $(RSRC)/assembly/SHA224.dll
	rm -f $(RSRC)/assembly/Accesstoken.dll
	rm -f $(RSRC)/assembly/Auditeventpolicy.dll
	rm -f $(RSRC)/assembly/Effectiverights.dll
	rm -f $(RSRC)/assembly/Lockoutpolicy.dll
	rm -f $(RSRC)/assembly/Metabase.dll
	rm -f $(RSRC)/assembly/Process58.dll
	rm -f $(RSRC)/assembly/Registry.dll
	rm -f $(RSRC)/assembly/Service.dll
	rm -f $(RSRC)/assembly/Systemmetric.dll

dist: all
	mkdir -p $(DIST)/lib
	cp -R $(RSRC)/plugin.properties $(DIST)
	cp -R $(JSAF_PROVIDER)/$(LIBDIR)/* $(DIST)/lib
	cp $(JSAF_PROVIDER_LIB) $(DIST)/lib
	cp $(JPE_LIB) $(DIST)/lib
	cp -R $(PLUGIN_LOCAL)/$(LIBDIR)/* $(DIST)/lib
	cp $(PLUGIN_LOCAL_LIB) $(DIST)/lib

dist-clean: clean
	rm -rf $(DIST)

clean:
	rm -rf $(BUILD)
	rm -f $(PLUGIN_LOCAL_LIB)

resources:
	rm -f $(BUILD)/*.txt $(BUILD)/*.properties
	cp $(RSRC)/adapters.txt $(BUILD)
	cp $(RSRC)/plugin.resources.properties $(BUILD)
	echo version=$(JOVAL_VERSION) >> $(BUILD)/plugin.resources.properties
	rm -f $(BUILD)/org/joval/scap/oval/adapter/independent/*.psm1 \
	      $(BUILD)/org/joval/scap/oval/adapter/independent/*.dll \
	      $(BUILD)/org/joval/scap/oval/adapter/windows/*.psm1 \
	      $(BUILD)/org/joval/scap/oval/adapter/windows/*.dll \
	      $(BUILD)/org/joval/scap/oval/adapter/windows/*.properties
	cp $(SRC)/org/joval/scap/oval/adapter/independent/*.psm1 $(BUILD)/org/joval/scap/oval/adapter/independent/
	cp $(RSRC)/assembly/Environmentvariable58.dll $(BUILD)/org/joval/scap/oval/adapter/independent/
	cp $(RSRC)/assembly/SHA224.dll $(BUILD)/org/joval/scap/oval/adapter/independent/
	cp $(SRC)/org/joval/scap/oval/adapter/windows/*.psm1 $(BUILD)/org/joval/scap/oval/adapter/windows/
	cp $(SRC)/org/joval/scap/oval/adapter/windows/*.properties $(BUILD)/org/joval/scap/oval/adapter/windows/
	cp $(RSRC)/assembly/Accesstoken.dll $(BUILD)/org/joval/scap/oval/adapter/windows/
	cp $(RSRC)/assembly/Auditeventpolicy.dll $(BUILD)/org/joval/scap/oval/adapter/windows/
	cp $(RSRC)/assembly/Effectiverights.dll $(BUILD)/org/joval/scap/oval/adapter/windows/
	cp $(RSRC)/assembly/Lockoutpolicy.dll $(BUILD)/org/joval/scap/oval/adapter/windows/
	cp $(RSRC)/assembly/Metabase.dll $(BUILD)/org/joval/scap/oval/adapter/windows/
	cp $(RSRC)/assembly/Process58.dll $(BUILD)/org/joval/scap/oval/adapter/windows/
	cp $(RSRC)/assembly/Registry.dll $(BUILD)/org/joval/scap/oval/adapter/windows/
	cp $(RSRC)/assembly/Service.dll $(BUILD)/org/joval/scap/oval/adapter/windows/
	cp $(RSRC)/assembly/Systemmetric.dll $(BUILD)/org/joval/scap/oval/adapter/windows/

classes: classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

$(BUILD)/%.class: $(SRC)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $(subst PKG,,$@)
