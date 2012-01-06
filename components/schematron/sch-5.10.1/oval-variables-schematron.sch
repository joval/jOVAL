<?xml version="1.0" encoding="UTF-8"?>
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron"
            xmlns:oval="http://oval.mitre.org/XMLSchema/oval-common-5"
            xmlns:xsd="http://www.w3.org/2001/XMLSchema"
            queryBinding="xslt">
   <sch:title>Schematron validation for an OVAL Variables file</sch:title>
   <sch:ns prefix="xsi" uri="http://www.w3.org/2001/XMLSchema-instance"/>
   <sch:ns prefix="oval" uri="http://oval.mitre.org/XMLSchema/oval-common-5"/>
   <sch:ns prefix="oval-var" uri="http://oval.mitre.org/XMLSchema/oval-variables-5"/>
   <sch:ns prefix="oval-def" uri="http://oval.mitre.org/XMLSchema/oval-definitions-5"/>
   <sch:phase id="oval">
      <sch:active pattern="oval_none_exist_value_dep"/>
   </sch:phase>
   <sch:pattern id="oval_none_exist_value_dep">
                                   <sch:rule context="oval-def:oval_definitions/oval-def:tests/child::*">
                                        <sch:report test="@check='none exist'">
                                             DEPRECATED ATTRIBUTE VALUE IN: <sch:value-of select="name()"/> ATTRIBUTE VALUE:
                                        </sch:report>
                                   </sch:rule>
                              </sch:pattern>
   <sch:diagnostics/>
</sch:schema>