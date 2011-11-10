# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../..

include $(TOP)/common.mk

PRODUCTNAME=jOVALRemotePlugin

API=../engine/jOVALCore.jar$(CLN)$(SCHEMALIB)$(CLN)$(JOVAL_DEPS)
CLASSPATH="$(CLASSLIB)$(CLN)$(LIB)$(CLN)$(SRC)$(CLN)$(API)"
RUNTIMECP="$(CLASSLIB)$(CLN)$(API)$(CLN)$(LIB)$(CLN)$(DEPS)$(CLN)$(PRODUCTNAME).jar"

include classes.mk

CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

all: $(PRODUCTNAME).jar

$(PRODUCTNAME).jar: classes resources
	$(JAR) cvf $@ -C $(BUILD)/ .

clean:
	rm -rf $(BUILD)
	rm -f $(PRODUCTNAME).jar

resources:
	rm -f $(BUILD)/*.properties
	cp $(SRC)/rsrc/plugin-remote/*.properties $(BUILD)
	echo version=$(JOVAL_VERSION) >> $(BUILD)/joval.system.properties

classes: classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

$(BUILD)/%.class: $(SRC)/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $(subst PKG,,$@)

javadocs:
	mkdir -p ../docs/api
	$(JAVA_HOME)/bin/javadoc -J-Xmx512m -d ../docs/api -sourcepath "$(GENSRC)$(CLN)$(SRC)" -subpackages \
	oval:org.w3c            \
	org.joval.intf.oval     \
	org.joval.intf.plugin   \
	org.joval.intf.di       \
	org.joval.plugin        \
	$(SRC)/org/joval/util/JOVALSystem.java $(SRC)/org/joval/util/JOVALMsg.java
