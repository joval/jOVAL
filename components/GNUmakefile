# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

all: _jovaldi

rebuild:
	@$(MAKE) --keep-going --directory=sdk all
	@$(MAKE) --keep-going --directory=xpert all
	@$(MAKE) --keep-going --directory=jovaldi dist

clean:
	@$(MAKE) --keep-going --directory=scap clean
	@$(MAKE) --keep-going --directory=schematron clean
	@$(MAKE) --keep-going --directory=sdk clean
	@$(MAKE) --keep-going --directory=jovaldi dist-clean
	@$(MAKE) --keep-going --directory=xpert clean

_jovaldi: _sdk _schematron _xpert
	@$(MAKE) --keep-going --directory=jovaldi dist

_sdk: _scap
	@$(MAKE) --keep-going --directory=sdk all

_scap:
	@$(MAKE) --keep-going --directory=scap all

_schematron:
	@$(MAKE) --keep-going --directory=schematron all

_xpert: _scap
	@$(MAKE) --keep-going --directory=xpert all
