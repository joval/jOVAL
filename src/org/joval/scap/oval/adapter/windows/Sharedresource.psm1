# Copyright (C) 2013 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function Get-Shares {
  foreach ($Share in [jOVAL.SharedResource.Probe]::List()) {
    "{"
    "Name: {0}" -f $Share.shi2_netname
    "Type: 0x{0:X8}" -f $Share.shi2_type
    "Path: {0}" -f $Share.shi2_path
    "Max Uses: {0:D}" -f $Share.shi2_max_uses
    "Current Uses: {0:D}" -f $Share.shi2_current_uses
    "Permissions: 0x{0:X}" -f $Share.shi2_current_uses
    "}"
  }
}
