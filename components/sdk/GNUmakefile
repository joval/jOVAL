# Copyright (C) 2011 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../..

include $(TOP)/common.mk

all:
	@$(MAKE) --keep-going --directory=common all
	@$(MAKE) --keep-going --directory=engine all
	@$(MAKE) --keep-going --directory=adapters all
	@$(MAKE) --keep-going --directory=plugin/local all

clean:
	@$(MAKE) --keep-going --directory=common clean
	@$(MAKE) --keep-going --directory=engine clean
	@$(MAKE) --keep-going --directory=adapters clean
	@$(MAKE) --keep-going --directory=plugin/local clean
