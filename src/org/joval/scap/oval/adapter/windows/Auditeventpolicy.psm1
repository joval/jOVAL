# Copyright (C) 2013 jOVAL.org.  All rights reserved.
# This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt
#
function Get-AuditEventPolicies {
  $Data = [jOVAL.AuditEventPolicy.Probe]::GetPolicies();
  foreach ($Category in $Data.Keys) {
    "{0}: {1}" -f $Category, $Data[$Category]
  }
}

function Get-AuditEventSubcategoryPolicies {
  $Data = [jOVAL.AuditEventPolicy.Probe]::GetSubcategoryPolicies();
  foreach ($Category in $Data.Keys) {
    "{0}:" -f $Category
    foreach ($Subcategory in $Data[$Category].Keys) {
      "  {0}: {1}" -f $Subcategory, $Data[$Category][$Subcategory]
    }
  }
}
