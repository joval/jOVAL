# Copyright (C) 2015 jOVAL.org.  All rights reserved.
# This software is licensed under the LGPL 3.0 license available at http://www.gnu.org/licenses/lgpl.txt

TOP=$(realpath .)

include $(TOP)/common.mk

ifeq (win, $(PLATFORM))
  SOURCEPATH="$(shell cygpath -w $(SCAP)/$(GEN))$(CLN)$(shell cygpath -w $(SCAP_EXT)/$(GEN))"
else
  SOURCEPATH=$(SCAP)/$(GEN)$(CLN)$(SCAP_EXT)/$(GEN)
endif

all: $(SCAP_LIB) $(SCAP_EXT_LIB) $(DOCS)/index.html

$(DOCS)/index.html: $(SCAP_LIB) $(SCAP_EXT_LIB)
	mkdir -p $(DOCS)
	$(JAVADOC) -J-Xmx512m -d $(DOCS) -sourcepath $(SOURCEPATH) -subpackages org:scap

$(SCAP_LIB): $(SCAP)/$(BINDINGS)
	@$(MAKE) --directory=scap

$(SCAP_EXT_LIB): $(SCAP_EXT)/$(BINDINGS)
	@$(MAKE) --directory=scap-extensions

clean:
	rm -rf $(DOCS)
	@$(MAKE) --directory=scap clean
	@$(MAKE) --directory=scap-extensions clean
