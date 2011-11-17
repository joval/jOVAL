# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=..

include $(TOP)/common.mk

CLASSPATH="$(CLASSLIB)$(CLN)$(LIB)$(CLN)$(SRC)$(CLN)$(API)"

all:
	@$(MAKE) --keep-going --directory=engine all
	@$(MAKE) --keep-going --directory=plugin all

clean:
	@$(MAKE) --keep-going --directory=engine clean
	@$(MAKE) --keep-going --directory=plugin clean
	@$(MAKE) --keep-going --directory=sample clean

javadocs:
	@$(MAKE) --keep-going --directory=plugin javadocs

test:
	@$(MAKE) --keep-going --directory=sample test

install: all
	mkdir -p sample/rsrc/lib
	cp $(SCHEMALIB) sample/rsrc/lib
	cp engine/rsrc/lib/*.jar sample/rsrc/lib
	cp engine/jOVAL*.jar sample/rsrc/lib
	cp plugin/rsrc/lib/*.jar sample/rsrc/lib
	cp plugin/jOVAL*.jar sample/rsrc/lib

sample:
	@$(MAKE) --keep-going --directory=sample all

dist-clean: clean
	rm -rf $(DIST)

dist: install javadocs
	mkdir -p $(DIST)
	cp $(SCHEMALIB) $(DIST)
	cp engine/jOVAL*.jar $(DIST)
	cp plugin/jOVAL*.jar $(DIST)
	cp -r docs $(DIST)
