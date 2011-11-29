# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../..

include $(TOP)/common.mk

CLASSPATH="$(CLASSLIB)$(CLN)$(LIB)$(CLN)."
RUNTIMECP="$(CLASSLIB)$(CLN)$(LIB)$(CLN)$(DEPS)$(CLN)."

CLASSES=TestMain
CLASS_FILES:=$(foreach class, $(CLASSES), $(subst .,/,$(class)).class)

all: classes

clean:
	rm -rf *.class
	rm -rf $(LIBDIR)

classes: $(CLASS_FILES)

%.class: %.java
	$(JAVAC) $(JAVACFLAGS) -classpath $(CLASSPATH) $<

test:
	$(JAVA) -classpath $(RUNTIMECP) TestMain plugin.properties
