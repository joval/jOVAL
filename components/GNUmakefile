# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

rebuild:
	@$(MAKE) --keep-going --directory=sdk install
	@$(MAKE) --keep-going --directory=jovaldi dist

all: _jovaldi

_jovaldi: _sdk
	@$(MAKE) --keep-going --directory=jovaldi dist

_sdk: _schema
	@$(MAKE) --keep-going --directory=sdk dist

_schema:
	@$(MAKE) --keep-going --directory=schema all
