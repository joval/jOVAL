# Copyright (C) 2013 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function List-ServiceInfo {
  foreach ($Service in Get-Service) {
    "Name: {0}" -f $Service.ServiceName
    "State: {0:X}" -f $Service.Status.value__
    "Type Flags: {0:X}" -f $Service.ServiceType.value__
    try {
      "Accept Flags: {0:X}" -f [jOVAL.Service.Probe]::GetAcceptFlags($Service.ServiceName)
    } catch {
    }
    foreach ($Dependency in $Service.ServicesDependedOn) {
      "Requires: {0}" -f $Dependency.ServiceName
    }
  }
}
