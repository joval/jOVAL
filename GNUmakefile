# Copyright (C) 2015 jOVAL.org.  All rights reserved.
# This software is licensed under the LGPL 3.0 license available at http://www.gnu.org/licenses/lgpl.txt

TOP=$(realpath .)

include $(TOP)/common.mk

all: $(DOCS)

$(DOCS): libs
	mkdir -p $(DOCS)
	$(JAVADOC) -J-Xmx512m -d $(DOCS) -sourcepath scap/gen-src$(CLN)scap-extensions/gen-src -subpackages org:scap

libs:
	@$(MAKE) --directory=scap
	@$(MAKE) --directory=scap-extensions

clean:
	rm -rf $(DOCS)
	@$(MAKE) --directory=scap clean
	@$(MAKE) --directory=scap-extensions clean
