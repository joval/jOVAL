# Copyright (C) 2012 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function Get-AccessTokens {
  $ErrorActionPreference = "Continue" 
  foreach ($SID in $input) {
    $Result = [jOVAL.AccessToken.Probe]::getAccessTokens($SID)
    "[{0}]" -f $SID
    foreach ($Token in $Result) {
      "{0}: true" -f $Token
    }
  }
}
