# Copyright (C) 2013 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function Get-SystemMetrics {
  $ErrorActionPreference = "Continue"
  $result = [jOVAL.SystemMetrics.Probe]::getMetrics()
  foreach($token in $result) {
    "{0}" -f $token
  }
}
