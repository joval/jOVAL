# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

TOP=../../../../..

include $(TOP)/common.mk

PRODUCT=accesstoken

all: exe

install: all
	cp -f $(BUILD)/$(PRODUCT).exe $(WIN32_PROBES_DESTINATION)/

clean:
	rm -rf $(BUILD)

resources: $(BUILD) $(BUILD)/$(PRODUCT).res

$(BUILD):
	mkdir -p $(BUILD)

$(BUILD)/$(PRODUCT).res: $(RSRC)/$(PRODUCT).rc
	$(RC) /fo $@ $<

exe: resources $(BUILD)/$(PRODUCT).exe

$(BUILD)/$(PRODUCT).exe: $(SRC)/native/probes/$(PRODUCT).cpp
	$(CC) $(CCFLAGS) /Fo$(BUILD)/$(PRODUCT).obj $(MS_INCLUDES) $< /link $(KERNEL32) $(UUID) $(OLDNAMES) $(ADVAPI32) $(USER32) $(MSVCRT) $(BUILD)/$(PRODUCT).res /OUT:$@ /MACHINE:IX86
