# Copyright (C) 2016 JovalCM.com.  All rights reserved.
# This software is licensed under the LGPL 3.0 license available at http://www.gnu.org/licenses/lgpl.txt

TOP=$(realpath .)

include $(TOP)/common.mk

ifeq (win, $(PLATFORM))
  SOURCEPATH="$(shell cygpath -w $(SCAP)/$(GEN))$(CLN)$(shell cygpath -w $(SCAP_EXT)/$(GEN))"
else
  SOURCEPATH=$(SCAP)/$(GEN)$(CLN)$(SCAP_EXT)/$(GEN)
endif

all: $(SCAP_LIB) $(SCAP_EXT_LIB) $(DOCS)/index.html $(CYBERSCOPE_LIB) $(DODARF_LIB)

$(DOCS)/index.html: $(SCAP_LIB) $(SCAP_EXT_LIB)
	mkdir -p $(DOCS)
	$(JAVADOC) $(JAVADOCFLAGS) -d $(DOCS) -sourcepath $(SOURCEPATH) -subpackages org:scap

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
