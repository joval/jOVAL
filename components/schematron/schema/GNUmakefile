# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../../..

include $(TOP)/common.mk

CLASSPATH="$(CLASSLIB)$(CLN)src"

include classes.mk

CLASS_FILES:=$(foreach class, $(CLASSES), $(BUILD)/$(subst .,/,$(class)).class)
PACKAGES=$(sort $(basename $(CLASSES)))
PACKAGEDIRS=$(subst .,/,$(PACKAGES))

all: svrl.jar

svrl.jar: classes resources
	$(JAR) cvf $@ -C $(BUILD)/ .

clean:
	rm -rf src
	rm -rf $(BUILD)
	rm -f *.jar

resources:
	rm -f $(BUILD)/*.properties
	cp $(RSRC)/*.properties $(BUILD)

src: xsd/svrl.xsd
	mkdir -p src
	$(XJC) xsd/svrl.xsd -b xsd/bindings.xjb -d src

javadocs: src
	$(JAVA_HOME)/bin/javadoc -J-Xmx512m -d $(DOCS) -sourcepath src -subpackages org.iso.svrl

classes: src classdirs $(CLASS_FILES)

classdirs: $(foreach pkg, $(PACKAGEDIRS), $(BUILD)/$(pkg)/)

$(BUILD)/%.class: src/%.java
	$(JAVAC) $(JAVACFLAGS) -d $(BUILD) -classpath $(CLASSPATH) $<

$(BUILD)/%/:
	mkdir -p $(subst PKG,,$@)
