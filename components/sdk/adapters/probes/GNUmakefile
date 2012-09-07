# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../../../..

include $(TOP)/common.mk

ifeq (win, $(PLATFORM))
    ACCESSTOKEN=_accesstoken
endif

install: $(ACCESSTOKEN)

clean:
	@$(MAKE) --keep-going --directory=accesstoken clean
	rm $(WIN32_PROBES_DESTINATION)/*.exe

_accesstoken:
	@$(MAKE) --keep-going --directory=accesstoken install
