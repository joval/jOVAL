# Copyright (C) 2016 JovalCM.com.  All rights reserved.
# This software is licensed under the LGPL 3.0 license available at http://www.gnu.org/licenses/lgpl.txt

TOP=$(realpath .)

include $(TOP)/common.mk

CLASSPATH="$(JAXB_LIB)"
ifeq (win, $(PLATFORM))
  SOURCEPATH="$(shell cygpath -w $(SCAP)/$(GEN))$(CLN)$(shell cygpath -w $(SCAP_EXT)/$(GEN))"
else
  SOURCEPATH=$(SCAP)/$(GEN)$(CLN)$(SCAP_EXT)/$(GEN)
endif

all: $(SCAP_LIB) $(SCAP_EXT_LIB) $(DOCS)/index.html $(CYBERSCOPE_LIB) $(DODARF_LIB)

$(DOCS)/index.html: $(SCAP_LIB) $(SCAP_EXT_LIB)
	mkdir -p $(DOCS)
ifeq (9, $(JAVA_VERSION))
# NB: A bug in Java 9.0.4's javadoc crashes when running against org.w3c.xml.signature.ObjectFactory, so we skip the package
	$(JAVADOC) $(JAVADOCFLAGS) -d $(DOCS) -classpath $(CLASSPATH) -sourcepath $(SOURCEPATH) -subpackages org:scap -exclude org.w3c.xml.signature
else
	$(JAVADOC) $(JAVADOCFLAGS) -d $(DOCS) -classpath $(CLASSPATH) -sourcepath $(SOURCEPATH) -subpackages org:scap
endif

$(SCAP_LIB): $(SCAP)/$(BINDINGS)
	@$(MAKE) --directory=$(SCAP)

$(SCAP_EXT_LIB): $(SCAP_EXT)/$(BINDINGS)
	@$(MAKE) --directory=$(SCAP_EXT)

$(CYBERSCOPE_LIB): $(CYBERSCOPE)/$(BINDINGS)
	@$(MAKE) --directory=$(CYBERSCOPE)

$(DODARF_LIB): $(DODARF)/$(BINDINGS)
	@$(MAKE) --directory=$(DODARF)

clean:
	rm -rf $(DOCS)
	@$(MAKE) --directory=$(SCAP) clean
	@$(MAKE) --directory=$(SCAP_EXT) clean
	@$(MAKE) --directory=$(CYBERSCOPE) clean
	@$(MAKE) --directory=$(DODARF) clean
