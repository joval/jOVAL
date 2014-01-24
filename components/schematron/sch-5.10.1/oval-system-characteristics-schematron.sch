<?xml version="1.0" encoding="UTF-8"?>
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron"
            xmlns:oval="http://oval.mitre.org/XMLSchema/oval-common-5"
            xmlns:xsd="http://www.w3.org/2001/XMLSchema"
            queryBinding="xslt">
   <sch:title>Schematron validation for an OVAL System Characteristics file</sch:title>
   <sch:ns prefix="xsi" uri="http://www.w3.org/2001/XMLSchema-instance"/>
   <sch:ns prefix="oval" uri="http://oval.mitre.org/XMLSchema/oval-common-5"/>
   <sch:ns prefix="oval-sc"
           uri="http://oval.mitre.org/XMLSchema/oval-system-characteristics-5"/>
   <sch:ns prefix="ind-sc"
           uri="http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#independent"/>
   <sch:ns prefix="aix-sc"
           uri="http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#aix"/>
   <sch:ns prefix="apache-sc"
           uri="http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#apache"/>
   <sch:ns prefix="catos-sc"
           uri="http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#catos"/>
   <sch:ns prefix="esx-sc"
           uri="http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#esx"/>
   <sch:ns prefix="freebsd-sc"
           uri="http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#freebsd"/>
   <sch:ns prefix="hpux-sc"
           uri="http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#hpux"/>
   <sch:ns prefix="ios-sc"
           uri="http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#ios"/>
   <sch:ns prefix="linux-sc"
           uri="http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#linux"/>
   <sch:ns prefix="macos-sc"
           uri="http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#macos"/>
   <sch:ns prefix="pixos-sc"
           uri="http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#pixos"/>
   <sch:ns prefix="sp-sc"
           uri="http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#sharepoint"/>
   <sch:ns prefix="sol-sc"
           uri="http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#solaris"/>
   <sch:ns prefix="unix-sc"
           uri="http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#unix"/>
   <sch:ns prefix="win-sc"
           uri="http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#windows"/>
   <sch:ns prefix="oval-def" uri="http://oval.mitre.org/XMLSchema/oval-definitions-5"/>
   <sch:phase id="oval">
      <sch:active pattern="oval_none_exist_value_dep"/>
   </sch:phase>
   <sch:phase id="oval-sc">
      <sch:active pattern="oval-sc_entity_rules"/>
   </sch:phase>
   <sch:phase id="ind-sc">
      <sch:active pattern="ind-sc_filehash_item_dep"/>
      <sch:active pattern="ind-sc_environmentvariable_item_dep"/>
      <sch:active pattern="ind-sc_ldap57_itemvalue"/>
      <sch:active pattern="ind-sc_sql_item_dep"/>
      <sch:active pattern="ind-sc_sql57_itemresult"/>
      <sch:active pattern="ind-sc_txtitemline"/>
      <sch:active pattern="ind-sc_ldaptype_timestamp_value_dep"/>
      <sch:active pattern="ind-sc_ldaptype_email_value_dep"/>
   </sch:phase>
   <sch:phase id="apache-sc">
      <sch:active pattern="apache-sc_httpd_item_dep"/>
   </sch:phase>
   <sch:phase id="catos-sc">
      <sch:active pattern="catos-sc_versionitemcatos_major_release"/>
      <sch:active pattern="catos-sc_versionitemcatos_individual_release"/>
      <sch:active pattern="catos-sc_versionitemcatos_version_id"/>
   </sch:phase>
   <sch:phase id="esx-sc">
      <sch:active pattern="esx-sc_patchitempatch_number"/>
   </sch:phase>
   <sch:phase id="ios-sc">
      <sch:active pattern="ios-sc_versionitemmajor_release"/>
      <sch:active pattern="ios-sc_versionitemtrain_number"/>
   </sch:phase>
   <sch:phase id="linux-sc">
      <sch:active pattern="linux-sc_rpmverify_item_dep"/>
   </sch:phase>
   <sch:phase id="macos-sc">
      <sch:active pattern="macos-sc_inetlisteningserveritem_dep"/>
      <sch:active pattern="macos-sc_pwpolicy_item_dep"/>
   </sch:phase>
   <sch:phase id="sp-sc">
      <sch:active pattern="sp-sc_spjobdefinition_item_dep"/>
   </sch:phase>
   <sch:phase id="unix-sc">
      <sch:active pattern="unix-sc_processitem_dep"/>
      <sch:active pattern="unix-sc_sccsitem_dep"/>
   </sch:phase>
   <sch:phase id="win-sc">
      <sch:active pattern="win-sc_activedirectory57_itemvalue"/>
      <sch:active pattern="win-sc_cmdletitemparameters"/>
      <sch:active pattern="win-sc_cmdletitemselect"/>
      <sch:active pattern="win-sc_cmdletitemvalue"/>
      <sch:active pattern="win-sc_fileaudititemtrustee_name"/>
      <sch:active pattern="win-sc_feritemtrustee_name"/>
      <sch:active pattern="win-sc_regitemkey"/>
      <sch:active pattern="win-sc_rapitemtrustee_name"/>
      <sch:active pattern="win-sc_rapitemstandard_synchronize"/>
      <sch:active pattern="win-sc_reritemtrustee_name"/>
      <sch:active pattern="win-sc_reritemstandard_synchronize"/>
      <sch:active pattern="win-sc_wmi_item_dep"/>
      <sch:active pattern="win-sc_wmi57_itemresult"/>
   </sch:phase>
   <sch:pattern id="oval_none_exist_value_dep">
                                   <sch:rule context="oval-def:oval_definitions/oval-def:tests/child::*">
                                        <sch:report test="@check='none exist'">
                                             DEPRECATED ATTRIBUTE VALUE IN: <sch:value-of select="name()"/> ATTRIBUTE VALUE:
                                        </sch:report>
                                   </sch:rule>
                              </sch:pattern>
   <sch:pattern id="oval-sc_entity_rules">
                    <sch:rule context="oval-sc:system_data/*/*|oval-sc:system_data/*/*/*">
                        <sch:assert test="not(@status) or @status='exists' or .=''">item <sch:value-of select="../@id"/> - a value for the <sch:value-of select="name()"/> entity should only be supplied if the status attribute is 'exists'</sch:assert>
                        <!--<sch:assert test="if (@datatype='binary') then (matches(., '^[0-9a-fA-F]*$')) else (1=1)"><sch:value-of select="../@id"/> - A value of '<sch:value-of select="."/>' for the <sch:value-of select="name()"/> entity is not valid given a datatype of binary.</sch:assert>-->
                        <!--<sch:assert test="if (@datatype='boolean') then (matches(., '^true$|^false$|^1$|^0$')) else (1=1)"><sch:value-of select="../@id"/> - A value of '<sch:value-of select="."/>' for the <sch:value-of select="name()"/> entity is not valid given a datatype of boolean.</sch:assert>-->
                        <!--<sch:assert test="if (@datatype='evr_string') then (matches(., '^[^:\-]*:[^:\-]*-[^:\-]*$')) else (1=1)"><sch:value-of select="../@id"/> - A value of '<sch:value-of select="."/>' for the <sch:value-of select="name()"/> entity is not valid given a datatype of evr_string.</sch:assert>-->
                        <!--<sch:assert test="if (@datatype='float') then (matches(., '^[+\-]?[0-9]+([\.][0-9]+)?([eE][+\-]?[0-9]+)?$|^NaN$|^INF$|^\-INF$')) else (1=1)"><sch:value-of select="../@id"/> - A value of '<sch:value-of select="."/>' for the <sch:value-of select="name()"/> entity is not valid given a datatype of float.</sch:assert>-->
                        <!--<sch:assert test="if (@datatype='int') then (matches(., '^[+\-]?[0-9]+$')) else (1=1)"><sch:value-of select="../@id"/> - A value of '<sch:value-of select="."/>' for the <sch:value-of select="name()"/> entity is not valid given a datatype of int.</sch:assert>-->
                    </sch:rule>
                    <sch:rule context="oval-sc:system_data/*/*[not((@xsi:nil='1' or @xsi:nil='true')) and @datatype='int']|oval-sc:system_data/*/*/*[not((@xsi:nil='1' or @xsi:nil='true')) and @datatype='int']">
                        <sch:assert test="(not(contains(.,'.'))) and (number(.) = floor(.))">
            <sch:value-of select="../@id"/> - The datatype for the <sch:value-of select="name()"/> entity is 'int' but the value is not an integer.</sch:assert>
                        <!--  Must test for decimal point because number(x.0) = floor(x.0) is true -->                        
                    </sch:rule>
                </sch:pattern>
   <sch:pattern id="ind-sc_filehash_item_dep">
                         <sch:rule context="ind-sc:filehash_item">
                              <sch:report test="true()">DEPRECATED ITEM: <sch:value-of select="name()"/> ID: <sch:value-of select="@id"/>
         </sch:report>
                         </sch:rule>
                    </sch:pattern>
   <sch:pattern id="ind-sc_environmentvariable_item_dep">
                         <sch:rule context="ind-sc:environmentvariable_item">
                              <sch:report test="true()">DEPRECATED ITEM: <sch:value-of select="name()"/> ID: <sch:value-of select="@id"/>
         </sch:report>
                         </sch:rule>
                    </sch:pattern>
   <sch:pattern id="ind-sc_ldap57_itemvalue">
                                                  <sch:rule context="ind-sc:ldap57_item/ind-sc:value">
                                                       <sch:assert test="@datatype='record'">
            <sch:value-of select="../@id"/> - datatype attribute for the value entity of a ldap57_item must be 'record'</sch:assert>
                                                  </sch:rule>
                                             </sch:pattern>
   <sch:pattern id="ind-sc_sql_item_dep">
                         <sch:rule context="ind-sc:sql_item">
                              <sch:report test="true()">DEPRECATED ITEM: <sch:value-of select="name()"/> ID: <sch:value-of select="@id"/>
         </sch:report>
                         </sch:rule>
                    </sch:pattern>
   <sch:pattern id="ind-sc_sql57_itemresult">
                                                  <sch:rule context="ind-sc:sql57_item/ind-sc:result">
                                                       <sch:assert test="@datatype='record'">
            <sch:value-of select="../@id"/> - datatype attribute for the result entity of a sql57_item must be 'record'</sch:assert>
                                                  </sch:rule>
                                             </sch:pattern>
   <sch:pattern id="ind-sc_txtitemline">
                                                  <sch:rule context="ind-sc:textfilecontent_item/ind-sc:line">
                                                        <sch:report test="true()">DEPRECATED ELEMENT: <sch:value-of select="name()"/> ID: <sch:value-of select="@id"/>
         </sch:report>
                                                  </sch:rule>
                                             </sch:pattern>
   <sch:pattern id="ind-sc_ldaptype_timestamp_value_dep">
                                        <sch:rule context="oval-sc:oval_system_characteristics/oval-sc:system_data/ind-sc:ldap_item/ind-sc:ldaptype|oval-sc:oval_system_characteristics/oval-sc:system_data/ind-sc:ldap57_item/ind-sc:ldaptype">
                                             <sch:report test=".='LDAPTYPE_TIMESTAMP'">
                                                  DEPRECATED ELEMENT VALUE IN: ldap_item ELEMENT VALUE: <sch:value-of select="."/> 
                                             </sch:report>
                                        </sch:rule>
                                   </sch:pattern>
   <sch:pattern id="ind-sc_ldaptype_email_value_dep">
                                        <sch:rule context="oval-sc:oval_system_characteristics/oval-sc:system_data/ind-sc:ldap_item/ind-sc:ldaptype|oval-sc:oval_system_characteristics/oval-sc:system_data/ind-sc:ldap57_item/ind-sc:ldaptype">
                                             <sch:report test=".='LDAPTYPE_EMAIL'">
                                                  DEPRECATED ELEMENT VALUE IN: ldap_item ELEMENT VALUE: <sch:value-of select="."/> 
                                             </sch:report>
                                        </sch:rule>
                                   </sch:pattern>
   <sch:pattern id="apache-sc_httpd_item_dep">
					<sch:rule context="apache-sc:httpd_item">
						   <sch:report test="true()">DEPRECATED ITEM: <sch:value-of select="name()"/> ID: <sch:value-of select="@id"/>
         </sch:report>
					</sch:rule>
				</sch:pattern>
   <sch:pattern id="catos-sc_versionitemcatos_major_release">
                                                  <sch:rule context="catos-sc:version_item/catos-sc:catos_major_release">
                                                        <sch:report test="true()">DEPRECATED ELEMENT: <sch:value-of select="name()"/> ID: <sch:value-of select="@id"/>
         </sch:report>
                                                  </sch:rule>
                                             </sch:pattern>
   <sch:pattern id="catos-sc_versionitemcatos_individual_release">
                                                  <sch:rule context="catos-sc:version_item/catos-sc:catos_individual_release">
                                                        <sch:report test="true()">DEPRECATED ELEMENT: <sch:value-of select="name()"/> ID: <sch:value-of select="@id"/>
         </sch:report>
                                                  </sch:rule>
                                             </sch:pattern>
   <sch:pattern id="catos-sc_versionitemcatos_version_id">
                                                  <sch:rule context="catos-sc:version_item/catos-sc:catos_version_id">
                                                        <sch:report test="true()">DEPRECATED ELEMENT: <sch:value-of select="name()"/> ID: <sch:value-of select="@id"/>
         </sch:report>
                                                  </sch:rule>
                                             </sch:pattern>
   <sch:pattern id="esx-sc_patchitempatch_number">
                                                            <sch:rule context="esx-sc:patch_item/esx-sc:patch_number">
                                                                  <sch:report test="true()">DEPRECATED ELEMENT: <sch:value-of select="name()"/> ID: <sch:value-of select="@id"/>
         </sch:report>
                                                            </sch:rule>
                                                      </sch:pattern>
   <sch:pattern id="ios-sc_versionitemmajor_release">
                                                  <sch:rule context="ios-sc:version_item/ios-sc:major_release">
                                                        <sch:report test="true()">DEPRECATED ELEMENT: <sch:value-of select="name()"/> ID: <sch:value-of select="@id"/>
         </sch:report>
                                                  </sch:rule>
                                             </sch:pattern>
   <sch:pattern id="ios-sc_versionitemtrain_number">
                                                  <sch:rule context="ios-sc:version_item/ios-sc:train_number">
                                                        <sch:report test="true()">DEPRECATED ELEMENT: <sch:value-of select="name()"/> ID: <sch:value-of select="@id"/>
         </sch:report>
                                                  </sch:rule>
                                             </sch:pattern>
   <sch:pattern id="linux-sc_rpmverify_item_dep">
                         <sch:rule context="linux-sc:rpmverify_item">
                              <sch:report test="true()">DEPRECATED ITEM: <sch:value-of select="name()"/> ID: <sch:value-of select="@id"/>
         </sch:report>
                         </sch:rule>
                    </sch:pattern>
   <sch:pattern id="macos-sc_inetlisteningserveritem_dep">
                         <sch:rule context="macos-sc:inetlisteningserver_item">
                              <sch:report test="true()">DEPRECATED ITEM: <sch:value-of select="name()"/> ID: <sch:value-of select="@id"/>
         </sch:report>
                         </sch:rule>
                    </sch:pattern>
   <sch:pattern id="macos-sc_pwpolicy_item_dep">
                         <sch:rule context="macos-sc:pwpolicy_item">
                              <sch:report test="true()">DEPRECATED ITEM: <sch:value-of select="name()"/> ID: <sch:value-of select="@id"/>
         </sch:report>
                         </sch:rule>
                    </sch:pattern>
   <sch:pattern id="sp-sc_spjobdefinition_item_dep">
                              <sch:rule context="sp-sc:spjobdefinition_item">
                                    <sch:report test="true()">DEPRECATED ITEM: <sch:value-of select="name()"/> ID: <sch:value-of select="@id"/>
         </sch:report>
                              </sch:rule>
                        </sch:pattern>
   <sch:pattern id="unix-sc_processitem_dep">
                         <sch:rule context="unix-sc:process_item">
                              <sch:report test="true()">DEPRECATED ITEM: <sch:value-of select="name()"/> ID: <sch:value-of select="@id"/>
         </sch:report>
                         </sch:rule>
                    </sch:pattern>
   <sch:pattern id="unix-sc_sccsitem_dep">
                         <sch:rule context="unix-sc:sccs_item">
                              <sch:report test="true()">DEPRECATED ITEM: <sch:value-of select="name()"/> ID: <sch:value-of select="@id"/>
         </sch:report>
                         </sch:rule>
                    </sch:pattern>
   <sch:pattern id="win-sc_activedirectory57_itemvalue">
                                                  <sch:rule context="win-sc:activedirectory57_item/win-sc:value">
                                                       <sch:assert test="@datatype='record'">
            <sch:value-of select="../@id"/> - datatype attribute for the value entity of a activedirectory57_item must be 'record'</sch:assert>
                                                  </sch:rule>
                                             </sch:pattern>
   <sch:pattern id="win-sc_cmdletitemparameters">
                                                  <sch:rule context="win-sc:cmdlet_item/win-sc:parameters">
                                                       <sch:assert test="@datatype='record'">
            <sch:value-of select="../@id"/> - datatype attribute for the parameters entity of a cmdlet_item must be 'record'</sch:assert>
                                                  </sch:rule>
                                             </sch:pattern>
   <sch:pattern id="win-sc_cmdletitemselect">
                                                  <sch:rule context="win-sc:cmdlet_item/win-sc:select">
                                                       <sch:assert test="@datatype='record'">
            <sch:value-of select="../@id"/> - datatype attribute for the select entity of a cmdlet_item must be 'record'</sch:assert>
                                                  </sch:rule>
                                             </sch:pattern>
   <sch:pattern id="win-sc_cmdletitemvalue">
                                                  <sch:rule context="win-sc:cmdlet_item/win-sc:value">
                                                       <sch:assert test="@datatype='record'">
            <sch:value-of select="../@id"/> - datatype attribute for the value entity of a cmdlet_item must be 'record'</sch:assert>
                                                  </sch:rule>
                                             </sch:pattern>
   <sch:pattern id="win-sc_fileaudititemtrustee_name">
                                                  <sch:rule context="win-sc:fileauditedpermissions_item/win-sc:trustee_name">
                                                        <sch:report test="true()">DEPRECATED ELEMENT: <sch:value-of select="name()"/> ID: <sch:value-of select="@id"/>
         </sch:report>
                                                  </sch:rule>
                                             </sch:pattern>
   <sch:pattern id="win-sc_feritemtrustee_name">
                                                  <sch:rule context="win-sc:fileeffectiverights_item/win-sc:trustee_name">
                                                        <sch:report test="true()">DEPRECATED ELEMENT: <sch:value-of select="name()"/> ID: <sch:value-of select="@id"/>
         </sch:report>
                                                  </sch:rule>
                                             </sch:pattern>
   <sch:pattern id="win-sc_regitemkey">
                                                  <sch:rule context="win-sc:registry_item/win-sc:key[@xsi:nil='true' or @xsi:nil='1']">
                                                       <sch:assert test="../win-sc:name/@xsi:nil='true' or ../win-sc:name/@xsi:nil='1'">
            <sch:value-of select="../@id"/> - name entity must be nil when key is nil</sch:assert>
                                                  </sch:rule>
                                             </sch:pattern>
   <sch:pattern id="win-sc_rapitemtrustee_name">
                                                  <sch:rule context="win-sc:regkeyauditedpermissions_item/win-sc:trustee_name">
                                                        <sch:report test="true()">DEPRECATED ELEMENT: <sch:value-of select="name()"/> ID: <sch:value-of select="@id"/>
         </sch:report>
                                                  </sch:rule>
                                             </sch:pattern>
   <sch:pattern id="win-sc_rapitemstandard_synchronize">
                                                  <sch:rule context="win-sc:regkeyauditedpermissions_item/win-sc:standard_synchronize">
                                                       <sch:report test="true()">DEPRECATED ELEMENT: <sch:value-of select="name()"/> ID: <sch:value-of select="@id"/>
         </sch:report>
                                                  </sch:rule>
                                             </sch:pattern>
   <sch:pattern id="win-sc_reritemtrustee_name">
                                                  <sch:rule context="win-sc:regkeyeffectiverights_item/win-sc:trustee_name">
                                                        <sch:report test="true()">DEPRECATED ELEMENT: <sch:value-of select="name()"/> ID: <sch:value-of select="@id"/>
         </sch:report>
                                                  </sch:rule>
                                             </sch:pattern>
   <sch:pattern id="win-sc_reritemstandard_synchronize">
                                                  <sch:rule context="win-sc:regkeyeffectiverights_item/win-sc:standard_synchronize">
                                                       <sch:report test="true()">DEPRECATED ELEMENT: <sch:value-of select="name()"/> ID: <sch:value-of select="@id"/>
         </sch:report>
                                                  </sch:rule>
                                             </sch:pattern>
   <sch:pattern id="win-sc_wmi_item_dep">
                         <sch:rule context="win-sc:wmi_item">
                              <sch:report test="true()">DEPRECATED ITEM: <sch:value-of select="name()"/> ID: <sch:value-of select="@id"/>
         </sch:report>
                         </sch:rule>
                    </sch:pattern>
   <sch:pattern id="win-sc_wmi57_itemresult">
                                                  <sch:rule context="win-sc:wmi57_item/win-sc:result">
                                                       <sch:assert test="@datatype='record'">
            <sch:value-of select="../@id"/> - datatype attribute for the result entity of a wmi57_item must be 'record'</sch:assert>
                                                  </sch:rule>
                                             </sch:pattern>
   <sch:diagnostics/>
</sch:schema>