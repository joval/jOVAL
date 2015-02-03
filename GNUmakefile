# Copyright (C) 2015 jOVAL.org.  All rights reserved.
# This software is licensed under the LGPL 3.0 license available at http://www.gnu.org/licenses/lgpl.txt

TOP=$(realpath .)

include $(TOP)/common.mk

all:
	@$(MAKE) --directory=scap all
	@$(MAKE) --directory=scap-extensions all

clean:
	@$(MAKE) --directory=scap clean
	@$(MAKE) --directory=scap-extensions clean
