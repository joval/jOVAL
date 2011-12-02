# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

all: _sdk

_sdk: _schema
	@$(MAKE) --keep-going --directory=sdk all

_schema:
	@$(MAKE) --keep-going --directory=schema all
