# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

all: _xpert

rebuild:
	@$(MAKE) --keep-going --directory=sdk install
	@$(MAKE) --keep-going --directory=xpert all
	@$(MAKE) --keep-going --directory=jovaldi dist

clean:
	@$(MAKE) --keep-going --directory=cpe clean
	@$(MAKE) --keep-going --directory=jovaldi dist-clean
	@$(MAKE) --keep-going --directory=oval clean
	@$(MAKE) --keep-going --directory=schematron clean
	@$(MAKE) --keep-going --directory=sdk dist-clean
	@$(MAKE) --keep-going --directory=xccdf clean
	@$(MAKE) --keep-going --directory=xpert clean

_jovaldi: _sdk _schematron _xpert
	@$(MAKE) --keep-going --directory=jovaldi dist

_sdk: _oval
	@$(MAKE) --keep-going --directory=sdk dist

_oval:
	@$(MAKE) --keep-going --directory=oval all

_schematron:
	@$(MAKE) --keep-going --directory=schematron all

_cpe:
	@$(MAKE) --keep-going --directory=cpe all

_xccdf:
	@$(MAKE) --keep-going --directory=xccdf all

_xpert: _oval _cpe _xccdf
	@$(MAKE) --keep-going --directory=xpert all
