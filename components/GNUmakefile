# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=..

include $(TOP)/common.mk

all: _jovaldi

# Target to build everything but the data models
rebuild:
	@$(MAKE) --keep-going --directory=engine all
	@$(MAKE) --keep-going --directory=plugin dist
	@$(MAKE) --keep-going --directory=jovaldi dist

# Target to delete everything but the data models
logic-clean:
	@$(MAKE) --keep-going --directory=plugin clean
	@$(MAKE) --keep-going --directory=jovaldi clean
	@$(MAKE) --keep-going --directory=engine clean

clean: logic-clean
	@$(MAKE) --keep-going --directory=$(JSAF_COMPONENTS) clean
	@$(MAKE) --keep-going --directory=jovaldi dist-clean
	@$(MAKE) --keep-going --directory=scap clean
	@$(MAKE) --keep-going --directory=schematron clean
	@$(MAKE) --keep-going --directory=$(JPE_HOME) clean

_jovaldi: _engine _plugin _schematron
	@$(MAKE) --keep-going --directory=jovaldi dist

_engine: _jsaf-core _scap
	@$(MAKE) --keep-going --directory=engine all

_plugin: _engine _jsaf-provider _jpe
	@$(MAKE) --keep-going --directory=plugin dist

_jsaf-core:
	@$(MAKE) --keep-going --directory=$(JSAF_CORE) all

_jsaf-provider: _jsaf-core
	@$(MAKE) --keep-going --directory=$(JSAF_PROVIDER) all

_jpe:
	@$(MAKE) --keep-going --directory=$(JPE_HOME) all

_scap:
	@$(MAKE) --keep-going --directory=scap all
	@$(MAKE) --keep-going --directory=scap-extensions all

_schematron:
	@$(MAKE) --keep-going --directory=schematron all
