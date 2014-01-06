<?xml version="1.0" encoding="UTF-8"?>
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron"
            xmlns:oval="http://oval.mitre.org/XMLSchema/oval-common-5"
            xmlns:xsd="http://www.w3.org/2001/XMLSchema"
            queryBinding="xslt">
   <sch:title>Schematron validation for an OVAL Directives file</sch:title>
   <sch:ns prefix="xsi" uri="http://www.w3.org/2001/XMLSchema-instance"/>
   <sch:ns prefix="oval" uri="http://oval.mitre.org/XMLSchema/oval-common-5"/>
   <sch:ns prefix="oval-dir" uri="http://oval.mitre.org/XMLSchema/oval-directives-5"/>
   <sch:ns prefix="oval-res" uri="http://oval.mitre.org/XMLSchema/oval-results-5"/>
   <sch:ns prefix="oval-def" uri="http://oval.mitre.org/XMLSchema/oval-definitions-5"/>
   <sch:ns xmlns:oval-sc="http://oval.mitre.org/XMLSchema/oval-system-characteristics-5"
           xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
           xmlns:tns="http://scap.nist.gov/schema/asset-identification/1.1"
           prefix="oval-sc"
           uri="http://oval.mitre.org/XMLSchema/oval-system-characteristics-5"/>
   <sch:phase id="oval">
      <sch:active pattern="oval_none_exist_value_dep"/>
   </sch:phase>
   <sch:phase id="oval-res">
      <sch:active pattern="oval-res_directives_include_oval_definitions"/>
      <sch:active pattern="oval-res_system"/>
      <sch:active pattern="oval-res_mask_rule"/>
      <sch:active pattern="oval-res_directives"/>
      <sch:active pattern="oval-res_testids"/>
   </sch:phase>
   <sch:phase id="oval-def">
      <sch:active pattern="oval-def_empty_def_doc"/>
      <sch:active pattern="oval-def_required_criteria"/>
      <sch:active pattern="oval-def_test_type"/>
      <sch:active pattern="oval-def_setobjref"/>
      <sch:active pattern="oval-def_literal_component"/>
      <sch:active pattern="oval-def_arithmeticfunctionrules"/>
      <sch:active pattern="oval-def_beginfunctionrules"/>
      <sch:active pattern="oval-def_concatfunctionrules"/>
      <sch:active pattern="oval-def_endfunctionrules"/>
      <sch:active pattern="oval-def_escaperegexfunctionrules"/>
      <sch:active pattern="oval-def_splitfunctionrules"/>
      <sch:active pattern="oval-def_substringfunctionrules"/>
      <sch:active pattern="oval-def_timedifferencefunctionrules"/>
      <sch:active pattern="oval-def_regexcapturefunctionrules"/>
      <sch:active pattern="oval-def_definition_entity_rules"/>
      <sch:active pattern="oval-def_no_var_ref_with_records"/>
      <sch:active pattern="oval-def_definition_entity_type_check_rules"/>
   </sch:phase>
   <sch:phase id="oval-sc">
      <sch:active pattern="oval-sc_entity_rules"/>
   </sch:phase>
   <sch:pattern id="oval_none_exist_value_dep">
                                   <sch:rule context="oval-def:oval_definitions/oval-def:tests/child::*">
                                        <sch:report test="@check='none exist'">
                                             DEPRECATED ATTRIBUTE VALUE IN: <sch:value-of select="name()"/> ATTRIBUTE VALUE:
                                        </sch:report>
                                   </sch:rule>
                              </sch:pattern>
   <sch:pattern id="oval-res_directives_include_oval_definitions">
                                        <sch:rule context="oval-res:oval_results/oval-res:directives[@include_source_definitions='true' or @include_source_definitions='1' or not(@include_source_definitions)]">
                                             <sch:assert test="ancestor::oval-res:oval_results[oval-def:oval_definitions]">
                                                  The source OVAL Definition document must be included when the directives include_source_definitions attribute is set to true.
                                             </sch:assert>
                                        </sch:rule>
                                        <sch:rule context="oval-res:oval_results/oval-res:directives[@include_source_definitions='false' or @include_source_definitions='0']">
                                             <sch:assert test="ancestor::oval-res:oval_results[not(oval-def:oval_definitions)]">
                                                  The source OVAL Definition document must not be included when the directives include_source_definitions attribute is set to false.
                                             </sch:assert>
                                        </sch:rule>
                                   </sch:pattern>
   <sch:pattern id="oval-res_system">
                         <sch:rule context="oval-res:system[oval-res:tests]">
                              <!-- Confirm that something somewhere expects full results -->
                              <sch:assert test="/oval-res:oval_results/oval-res:directives/*[@reported='true' or @reported='1']/@content='full'                                                 or /oval-res:oval_results/oval-res:directives/*[(@reported='true' or @reported='1') and not(@content)]                                                 or /oval-res:oval_results/oval-res:class_directives/*[@reported='true' or @reported='1']/@content='full'                                                 or /oval-res:oval_results/oval-res:class_directives/*[(@reported='true' or @reported='1') and not(@content)]">
                                   The tests element should not be included unless full results are to be provided (see directives)
                              </sch:assert>
                         </sch:rule>
                         <sch:rule context="oval-res:system[not(oval-res:tests)]">
                              <!-- Confirm that nothing anywhere expects full results -->                              
                              <sch:assert test="not(oval-res:oval_results/oval-res:directives/*[@reported='true' or @reported='1']/@content='full')                                                  and not(/oval-res:oval_results/oval-res:directives/*[(@reported='true' or @reported='1') and not(@content)])                                                 and not(/oval-res:oval_results/oval-res:class_directives/*[@reported='true' or @reported='1']/@content='full')                                                 and not(/oval-res:oval_results/oval-res:class_directives/*[(@reported='true' or @reported='1') and not(@content)])">
                                   The tests element should be included when full results are specified (see directives)
                              </sch:assert>
                         </sch:rule>
                    </sch:pattern>
   <sch:pattern id="oval-res_mask_rule">
                                   <sch:rule context="/oval-res:oval_results/oval-res:results/oval-res:system/oval-sc:oval_system_characteristics/oval-sc:system_data/*/*|/oval-res:oval_results/oval-res:results/oval-res:system/oval-sc:oval_system_characteristics/oval-sc:system_data/*/*/*">
                                        <sch:assert test="not(@mask) or @mask='false' or @mask='0' or .=''">item <sch:value-of select="../@id"/> - a value for the <sch:value-of select="name()"/> entity should only be supplied if the mask attribute is 'false'.</sch:assert>
                                   </sch:rule>
                              </sch:pattern>
   <sch:pattern id="oval-res_directives">
                         <!-- Check definition_true reported='true' and content='full' -->
                         <sch:rule context="oval-res:definition[@result='true' and oval-res:criteria]">
                              <!-- Check that the global directives say to report this and that there are no class directives for this class (to override the global directive),
                                   or that the class directive for this class says to report this. -->
                              <sch:assert test="((/oval-res:oval_results/oval-res:directives/oval-res:definition_true/@reported='true' or /oval-res:oval_results/oval-res:directives/oval-res:definition_true/@reported='1')                                    and not(oval-res:oval_results/oval-res:class_directives[@class = ./@class]))                                    or (/oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_true/@reported='true' or /oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_true/@reported='1')">
                                   <sch:value-of select="@definition_id"/> - definitions with a result of TRUE should not be included (see directives)
                              </sch:assert>
                              <sch:assert test="((/oval-res:oval_results/oval-res:directives/oval-res:definition_true/@content='full')                                    and not(/oval-res:oval_results/oval-res:class_directives[@class = ./@class]))                                    or (/oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_true/@content='full')">
                                   <sch:value-of select="@definition_id"/> - definitions with a result of TRUE should contain THIN content (see directives)
                              </sch:assert>
                         </sch:rule>
                         
                         <!-- Check definition_true reported='true' and content='thin' -->                         
                         <sch:rule context="oval-res:definition[@result='true' and not(oval-res:criteria)]">
                              <sch:assert test="((/oval-res:oval_results/oval-res:directives/oval-res:definition_true/@reported='true' or /oval-res:oval_results/oval-res:directives/oval-res:definition_true/@reported='1')                                    and not(/oval-res:oval_results/oval-res:class_directives[@class = ./@class]))                                    or (/oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_true/@reported='true' or /oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_true/@reported='1')">
                                   <sch:value-of select="@definition_id"/> - definitions with a result of TRUE should not be included (see directives)
                              </sch:assert>
                              <sch:assert test="((/oval-res:oval_results/oval-res:directives/oval-res:definition_true/@content='thin')                                    and not(/oval-res:oval_results/oval-res:class_directives[@class = ./@class]))                                    or (/oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_true/@content='thin')">
                                   <sch:value-of select="@definition_id"/> - definitions with a result of TRUE should contain FULL content (see directives)
                              </sch:assert>
                         </sch:rule>
                         
                         <!-- Check definition_false reported='true' and content='full' -->
                         <sch:rule context="oval-res:definition[@result='false' and oval-res:criteria]">
                              <sch:assert test="((/oval-res:oval_results/oval-res:directives/oval-res:definition_false/@reported='true' or /oval-res:oval_results/oval-res:directives/oval-res:definition_false/@reported='1')                                    and not(/oval-res:oval_results/oval-res:class_directives[@class = ./@class]))                                    or (/oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_false/@reported='true' or /oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_false/@reported='1')">
                                   <sch:value-of select="@definition_id"/> - definitions with a result of FALSE should not be included (see directives)
                              </sch:assert>
                              <sch:assert test="((/oval-res:oval_results/oval-res:directives/oval-res:definition_false/@content='full')                                    and not(/oval-res:oval_results/oval-res:class_directives[@class = ./@class]))                                    or (/oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_false/@content='full')">
                                   <sch:value-of select="@definition_id"/> - definitions with a result of FALSE should contain THIN content (see directives)
                              </sch:assert>
                         </sch:rule>
                         
                         <!-- Check definition_false reported='true' and content='thin' -->                         
                         <sch:rule context="oval-res:definition[@result='false' and not(oval-res:criteria)]">
                              <sch:assert test="((/oval-res:oval_results/oval-res:directives/oval-res:definition_false/@reported='true' or /oval-res:oval_results/oval-res:directives/oval-res:definition_false/@reported='1')                                    and not(/oval-res:oval_results/oval-res:class_directives[@class = ./@class]))                                    or (/oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_false/@reported='true' or /oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_false/@reported='1')">
                                   <sch:value-of select="@definition_id"/> - definitions with a result of FALSE should not be included (see directives)
                              </sch:assert>
                              <sch:assert test="((/oval-res:oval_results/oval-res:directives/oval-res:definition_false/@content='thin')                                    and not(/oval-res:oval_results/oval-res:class_directives[@class = ./@class]))                                    or (/oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_false/@content='thin')">
                                   <sch:value-of select="@definition_id"/> - definitions with a result of FALSE should contain FULL content (see directives)
                              </sch:assert>
                         </sch:rule>
                         
                         <!-- Check definition_unknown reported='true' and content='full' -->
                         <sch:rule context="oval-res:definition[@result='unknown' and oval-res:criteria]">
                              <sch:assert test="((/oval-res:oval_results/oval-res:directives/oval-res:definition_unknown/@reported='true' or /oval-res:oval_results/oval-res:directives/oval-res:definition_unknown/@reported='1')                                    and not(/oval-res:oval_results/oval-res:class_directives[@class = ./@class]))                                    or (/oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_unknown/@reported='true' or /oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_unknown/@reported='1')">
                                   <sch:value-of select="@definition_id"/> - definitions with a result of UNKNOWN should not be included (see directives)
                              </sch:assert>
                              <sch:assert test="((/oval-res:oval_results/oval-res:directives/oval-res:definition_unknown/@content='full')                                    and not(oval-res:oval_results/oval-res:class_directives[@class = ./@class]))                                    or (/oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_unknown/@content='full')">
                                   <sch:value-of select="@definition_id"/> - definitions with a result of UNKNOWN should contain THIN content (see directives)
                              </sch:assert>
                         </sch:rule>
                         
                         <!-- Check definition_unknown reported='true' and content='thin' -->                         
                         <sch:rule context="oval-res:definition[@result='unknown' and not(oval-res:criteria)]">
                              <sch:assert test="((/oval-res:oval_results/oval-res:directives/oval-res:definition_unknown/@reported='true' or /oval-res:oval_results/oval-res:directives/oval-res:definition_unknown/@reported='1')                                    and not(/oval-res:oval_results/oval-res:class_directives[@class = ./@class]))                                    or (/oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_unknown/@reported='true' or /oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_unknown/@reported='1')">
                                   <sch:value-of select="@definition_id"/> - definitions with a result of UNKNOWN should not be included (see directives)
                              </sch:assert>
                              <sch:assert test="((/oval-res:oval_results/oval-res:directives/oval-res:definition_unknown/@content='thin')                                    and not(/oval-res:oval_results/oval-res:class_directives[@class = ./@class]))                                    or (/oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_unknown/@content='thin')">
                                   <sch:value-of select="@definition_id"/> - definitions with a result of UNKNOWN should contain FULL content (see directives)
                              </sch:assert>
                         </sch:rule>
                         
                         <!-- Check definition_error reported='true' and content='full' -->
                         <sch:rule context="oval-res:definition[@result='error' and oval-res:criteria]">
                              <sch:assert test="((/oval-res:oval_results/oval-res:directives/oval-res:definition_error/@reported='true' or /oval-res:oval_results/oval-res:directives/oval-res:definition_error/@reported='1')                                    and not(/oval-res:oval_results/oval-res:class_directives[@class = ./@class]))                                    or (/oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_error/@reported='true' or /oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_error/@reported='1')">
                                   <sch:value-of select="@definition_id"/> - definitions with a result of ERROR should not be included (see directives)
                              </sch:assert>
                              <sch:assert test="((/oval-res:oval_results/oval-res:directives/oval-res:definition_error/@content='full')                                    and not(oval-res:oval_results/oval-res:class_directives[@class = ./@class]))                                    or (/oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_error/@content='full')">
                                   <sch:value-of select="@definition_id"/> - definitions with a result of ERROR should contain THIN content (see directives)
                              </sch:assert>
                         </sch:rule>
                         
                         <!-- Check definition_error reported='true' and content='thin' -->                         
                         <sch:rule context="oval-res:definition[@result='error' and not(oval-res:criteria)]">
                              <sch:assert test="((/oval-res:oval_results/oval-res:directives/oval-res:definition_error/@reported='true' or /oval-res:oval_results/oval-res:directives/oval-res:definition_error/@reported='1')                                    and not(/oval-res:oval_results/oval-res:class_directives[@class = ./@class]))                                    or (/oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_unknown/@reported='true' or /oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_unknown/@reported='1')">
                                   <sch:value-of select="@definition_id"/> - definitions with a result of ERROR should not be included (see directives)
                              </sch:assert>
                              <sch:assert test="((/oval-res:oval_results/oval-res:directives/oval-res:definition_error/@content='thin')                                    and not(/oval-res:oval_results/oval-res:class_directives[@class = ./@class]))                                    or (/oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_error/@content='thin')">
                                   <sch:value-of select="@definition_id"/> - definitions with a result of ERROR should contain FULL content (see directives)
                              </sch:assert>
                         </sch:rule>
                         
                         <!-- Check definition_not_evaluated reported='true' and content='full' -->
                         <sch:rule context="oval-res:definition[@result='not evaluated' and oval-res:criteria]">
                              <sch:assert test="((/oval-res:oval_results/oval-res:directives/oval-res:definition_not_evaluated/@reported='true' or /oval-res:oval_results/oval-res:directives/oval-res:definition_not_evaluated/@reported='1')                                    and not(/oval-res:oval_results/oval-res:class_directives[@class = ./@class]))                                    or (/oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_not_evaluated/@reported='true' or /oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_not_evaluated/@reported='1')">
                                   <sch:value-of select="@definition_id"/> - definitions with a result of NOT EVALUATED should not be included (see directives)
                              </sch:assert>
                              <sch:assert test="((/oval-res:oval_results/oval-res:directives/oval-res:definition_not_evaluated/@content='full')                                    and not(/oval-res:oval_results/oval-res:class_directives[@class = ./@class]))                                    or (/oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_not_evaluated/@content='full')">
                                   <sch:value-of select="@definition_id"/> - definitions with a result of NOT EVALUATED should contain THIN content (see directives)
                              </sch:assert>
                         </sch:rule>
                         
                         <!-- Check definition_not_evaluated reported='true' and content='thin' -->                         
                         <sch:rule context="oval-res:definition[@result='not evaluated' and not(oval-res:criteria)]">
                              <sch:assert test="((/oval-res:oval_results/oval-res:directives/oval-res:definition_not_evaluated/@reported='true' or /oval-res:oval_results/oval-res:directives/oval-res:definition_not_evaluated/@reported='1')                                    and not(/oval-res:oval_results/oval-res:class_directives[@class = ./@class]))                                    or (/oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_not_evaluated/@reported='true' or /oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_not_evaluated/@reported='1')">
                                   <sch:value-of select="@definition_id"/> - definitions with a result of NOT EVALUATED should not be included (see directives)
                              </sch:assert>
                              <sch:assert test="((/oval-res:oval_results/oval-res:directives/oval-res:definition_not_evaluated/@content='thin')                                    and not(/oval-res:oval_results/oval-res:class_directives[@class = ./@class]))                                    or (/oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_not_evaluated/@content='thin')">
                                   <sch:value-of select="@definition_id"/> - definitions with a result of NOT EVALUATED should contain FULL content (see directives)
                              </sch:assert>
                         </sch:rule>
                         
                         <!-- Check definition_not_applicable reported='true' and content='full' -->
                         <sch:rule context="oval-res:definition[@result='not applicable' and oval-res:criteria]">
                              <sch:assert test="((/oval-res:oval_results/oval-res:directives/oval-res:definition_not_applicable/@reported='true' or /oval-res:oval_results/oval-res:directives/oval-res:definition_not_applicable/@reported='1')                                    and not(/oval-res:oval_results/oval-res:class_directives[@class = ./@class]))                                    or (/oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_not_applicable/@reported='true' or /oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_not_applicable/@reported='1')">
                                   <sch:value-of select="@definition_id"/> - definitions with a result of NOT APPLICABLE should not be included (see directives)
                              </sch:assert>
                              <sch:assert test="((/oval-res:oval_results/oval-res:directives/oval-res:definition_not_applicable/@content='full')                                    and not(oval-res:oval_results/oval-res:class_directives[@class = ./@class]))                                    or (/oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_not_applicable/@content='full')">
                                   <sch:value-of select="@definition_id"/> - definitions with a result of NOT APPLICABLE should contain THIN content (see directives)
                              </sch:assert>
                         </sch:rule>
                         
                         <!-- Check definition_not_applicable reported='true' and content='thin' -->                         
                         <sch:rule context="oval-res:definition[@result='not applicable' and not(oval-res:criteria)]">
                              <sch:assert test="((/oval-res:oval_results/oval-res:directives/oval-res:definition_not_applicable/@reported='true' or /oval-res:oval_results/oval-res:directives/oval-res:definition_not_applicable/@reported='1')                                    and not(/oval-res:oval_results/oval-res:class_directives[@class = ./@class]))                                    or (/oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_not_applicable/@reported='true' or /oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_not_applicable/@reported='1')">
                                   <sch:value-of select="@definition_id"/> - definitions with a result of NOT APPLICABLE should not be included (see directives)
                              </sch:assert>
                              <sch:assert test="((/oval-res:oval_results/oval-res:directives/oval-res:definition_not_applicable/@content='thin')                                    and not(oval-res:oval_results/oval-res:class_directives[@class = ./@class]))                                    or (/oval-res:oval_results/oval-res:class_directives[@class = ./@class]/oval-res:definition_not_applicable/@content='thin')">
                                   <sch:value-of select="@definition_id"/> - definitions with a result of NOT APPLICABLE should contain FULL content (see directives)
                              </sch:assert>
                         </sch:rule>
                    </sch:pattern>
   <sch:pattern id="oval-res_testids">
                         <sch:rule context="oval-res:test">
                              <sch:assert test="@test_id = ../../oval-res:definitions//oval-res:criterion/@test_ref">
            <sch:value-of select="@test_id"/> - the specified test is not used in any definition's criteria</sch:assert>
                         </sch:rule>
                    </sch:pattern>
   <sch:pattern id="oval-def_empty_def_doc">
                    <sch:rule context="oval-def:oval_definitions">
                        <sch:assert test="oval-def:definitions or oval-def:tests or oval-def:objects or oval-def:states or oval-def:variables">A valid OVAL Definition document must contain at least one definitions, tests, objects, states, or variables element. The optional definitions, tests, objects, states, and variables sections define the specific characteristics that should be evaluated on a system to determine the truth values of the OVAL Definition Document. To be valid though, at least one definitions, tests, objects, states, or variables element must be present.</sch:assert>
                    </sch:rule>
                </sch:pattern>
   <sch:pattern id="oval-def_required_criteria">
                    <sch:rule context="oval-def:oval_definitions/oval-def:definitions/oval-def:definition[(@deprecated='false' or @deprecated='0') or not(@deprecated)]">
                        <sch:assert test="oval-def:criteria">A valid OVAL Definition must contain a criteria unless the definition is a deprecated definition.</sch:assert>
                    </sch:rule>
                </sch:pattern>
   <sch:pattern id="oval-def_test_type">
                    <sch:rule context="oval-def:oval_definitions/oval-def:tests/*[@check_existence='none_exist']">
                        <sch:assert test="not(*[local-name()='state'])">
            <sch:value-of select="@id"/> - No state should be referenced when check_existence has a value of 'none_exist'.</sch:assert>
                    </sch:rule>
                </sch:pattern>
   <sch:pattern id="oval-def_setobjref">
                    <sch:rule context="oval-def:oval_definitions/oval-def:objects/*/oval-def:set/oval-def:object_reference">
                        <sch:assert test="name(./../..) = name(ancestor::oval-def:oval_definitions/oval-def:objects/*[@id=current()])">
            <sch:value-of select="../../@id"/> - Each object referenced by the set must be of the same type as parent object</sch:assert>
                    </sch:rule>
                    <sch:rule context="oval-def:oval_definitions/oval-def:objects/*/oval-def:set/oval-def:set/oval-def:object_reference">
                        <sch:assert test="name(./../../..) = name(ancestor::oval-def:oval_definitions/oval-def:objects/*[@id=current()])">
            <sch:value-of select="../../../@id"/> - Each object referenced by the set must be of the same type as parent object</sch:assert>
                    </sch:rule>
                    <sch:rule context="oval-def:oval_definitions/oval-def:objects/*/oval-def:set/oval-def:set/oval-def:set/oval-def:object_reference">
                        <sch:assert test="name(./../../../..) = name(ancestor::oval-def:oval_definitions/oval-def:objects/*[@id=current()])">
            <sch:value-of select="../../../../@id"/> - Each object referenced by the set must be of the same type as parent object</sch:assert>
                    </sch:rule>
                </sch:pattern>
   <sch:pattern id="oval-def_literal_component">
                    <sch:rule context="oval-def:literal_component">
                        <sch:assert test="not(@datatype='record')">
            <sch:value-of select="ancestor::*/@id"/> - The 'record' datatype is prohibited on variables.</sch:assert>
                    </sch:rule>
                    <!--
                        <sch:rule context="oval-def:literal_component/*/*[not(@datatype)]">
                        </sch:rule>
                        <sch:rule context="oval-def:literal_component[@datatype='binary']">
                        <sch:assert test="matches(., '^[0-9a-fA-F]*$')"><sch:value-of select="../@id"/> - A value of '<sch:value-of select="."/>' for the <sch:value-of select="name()"/> entity is not valid given a datatype of binary.</sch:assert>
                        </sch:rule>
                        <sch:rule context="oval-def:literal_component[@datatype='boolean']">
                        <sch:assert test="matches(., '^true$|^false$|^1$|^0$')"><sch:value-of select="../@id"/> - A value of '<sch:value-of select="."/>' for the <sch:value-of select="name()"/> entity is not valid given a datatype of boolean.</sch:assert>
                        </sch:rule>
                        <sch:rule context="oval-def:literal_component[@datatype='evr_string']">
                        <sch:assert test="matches(., '^[^:\-]*:[^:\-]*-[^:\-]*$')"><sch:value-of select="../@id"/> - A value of '<sch:value-of select="."/>' for the <sch:value-of select="name()"/> entity is not valid given a datatype of evr_string.</sch:assert>
                        </sch:rule>
                        <sch:rule context="oval-def:literal_component[@datatype='fileset_revision']">
                        </sch:rule>
                        <sch:rule context="oval-def:literal_component[@datatype='float']">
                        <sch:assert test="matches(., '^[+\-]?[0-9]+([\.][0-9]+)?([eE][+\-]?[0-9]+)?$|^NaN$|^INF$|^\-INF$')"><sch:value-of select="../@id"/> - A value of '<sch:value-of select="."/>' for the <sch:value-of select="name()"/> entity is not valid given a datatype of float.</sch:assert>
                        </sch:rule>
                        <sch:rule context="oval-def:literal_component[@datatype='ios_version']">
                        </sch:rule>
                        <sch:rule context="oval-def:literal_component[@datatype='int']">
                        <sch:assert test="matches(., '^[+\-]?[0-9]+$')"><sch:value-of select="../@id"/> - A value of '<sch:value-of select="."/>' for the <sch:value-of select="name()"/> entity is not valid given a datatype of int.</sch:assert>
                        </sch:rule>
                        <sch:rule context="oval-def:literal_component[@datatype='string']">
                        </sch:rule>
                        <sch:rule context="oval-def:literal_component[@datatype='version']">
                        </sch:rule>
                    -->
                </sch:pattern>
   <sch:pattern id="oval-def_arithmeticfunctionrules">
                    <sch:rule context="oval-def:arithmetic/oval-def:literal_component">
                        <sch:assert test="@datatype='float' or @datatype='int'">A literal_component used by an arithmetic function must have a datatype of float or int.</sch:assert>
                    </sch:rule>
                    <sch:rule context="oval-def:arithmetic/oval-def:variable_component">
                        <sch:let name="var_ref" value="@var_ref"/>
                        <sch:assert test="ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype='float' or ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype='int'">The variable referenced by the arithmetic function must have a datatype of float or int.</sch:assert>
                    </sch:rule>
                </sch:pattern>
   <sch:pattern id="oval-def_beginfunctionrules">
                    <sch:rule context="oval-def:begin/oval-def:literal_component">
                        <sch:assert test="not(@datatype) or @datatype='string'">A literal_component used by the begin function must have a datatype of string.</sch:assert>
                    </sch:rule>
                    <sch:rule context="oval-def:begin/oval-def:variable_component">
                        <sch:let name="var_ref" value="@var_ref"/>
                        <sch:assert test="ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype = 'string'">The variable referenced by the begin function must have a datatype of string.</sch:assert>
                    </sch:rule>
                </sch:pattern>
   <sch:pattern id="oval-def_concatfunctionrules">
                        <sch:rule context="oval-def:concat/oval-def:literal_component">
                            <sch:assert test="not(@datatype) or @datatype='string'">A literal_component used by the concat function must have a datatype of string.</sch:assert>
                        </sch:rule>
                        <sch:rule context="oval-def:concat/oval-def:variable_component">
                            <sch:let name="var_ref" value="@var_ref"/>
                            <sch:assert test="ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype = 'string'">The variable referenced by the concat function must have a datatype of string.</sch:assert>
                        </sch:rule>
                    </sch:pattern>
   <sch:pattern id="oval-def_endfunctionrules">
                    <sch:rule context="oval-def:end/oval-def:literal_component">
                        <sch:assert test="not(@datatype) or @datatype='string'">A literal_component used by the end function must have a datatype of string.</sch:assert>
                    </sch:rule>
                    <sch:rule context="oval-def:end/oval-def:variable_component">
                        <sch:let name="var_ref" value="@var_ref"/>
                        <sch:assert test="ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype = 'string'">The variable referenced by the end function must have a datatype of string.</sch:assert>
                    </sch:rule>
                </sch:pattern>
   <sch:pattern id="oval-def_escaperegexfunctionrules">
                    <sch:rule context="oval-def:escape_regex/oval-def:literal_component">
                        <sch:assert test="not(@datatype) or @datatype='string'">A literal_component used by the escape_regex function must have a datatype of string.</sch:assert>
                    </sch:rule>
                    <sch:rule context="oval-def:escape_regex/oval-def:variable_component">
                        <sch:let name="var_ref" value="@var_ref"/>
                        <sch:assert test="ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype = 'string'">The variable referenced by the escape_regex function must have a datatype of string.</sch:assert>
                    </sch:rule>
                </sch:pattern>
   <sch:pattern id="oval-def_splitfunctionrules">
                    <sch:rule context="oval-def:split/oval-def:literal_component">
                        <sch:assert test="not(@datatype) or @datatype='string'">A literal_component used by the split function must have a datatype of string.</sch:assert>
                    </sch:rule>
                    <sch:rule context="oval-def:split/oval-def:variable_component">
                        <sch:let name="var_ref" value="@var_ref"/>
                        <sch:assert test="ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype = 'string'">The variable referenced by the split function must have a datatype of string.</sch:assert>
                    </sch:rule>
                </sch:pattern>
   <sch:pattern id="oval-def_substringfunctionrules">
                    <sch:rule context="oval-def:substring/oval-def:literal_component">
                        <sch:assert test="not(@datatype) or @datatype='string'">A literal_component used by the substring function must have a datatype of string.</sch:assert>
                    </sch:rule>
                    <sch:rule context="oval-def:substring/oval-def:variable_component">
                        <sch:let name="var_ref" value="@var_ref"/>
                        <sch:assert test="ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype = 'string'">The variable referenced by the substring function must have a datatype of string.</sch:assert>
                    </sch:rule>
                </sch:pattern>
   <sch:pattern id="oval-def_timedifferencefunctionrules">
                    <sch:rule context="oval-def:time_difference/oval-def:literal_component">
                        <sch:assert test="not(@datatype) or @datatype='string' or @datatype='int'">A literal_component used by the time_difference function must have a datatype of string or int.</sch:assert>
                    </sch:rule>
                    <sch:rule context="oval-def:time_difference/oval-def:variable_component">
                        <sch:let name="var_ref" value="@var_ref"/>
                        <sch:assert test="ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype='string' or ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype='int'">The variable referenced by the time_difference function must have a datatype of string or int.</sch:assert>
                    </sch:rule>
                </sch:pattern>
   <sch:pattern id="oval-def_regexcapturefunctionrules">
                    <sch:rule context="oval-def:regex_capture/oval-def:literal_component">
                        <sch:assert test="not(@datatype) or @datatype='string'">A literal_component used by the regex_capture function must have a datatype of string.</sch:assert>
                    </sch:rule>
                    <sch:rule context="oval-def:regex_capture/oval-def:variable_component">
                        <sch:let name="var_ref" value="@var_ref"/>
                        <sch:assert test="ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype = 'string'">The variable referenced by the regex_capture function must have a datatype of string.</sch:assert>
                    </sch:rule>
                </sch:pattern>
   <sch:pattern id="oval-def_definition_entity_rules">
                    <!-- These schematron rules are written to look at object and state entities as well as fields in states. -->
                    <!-- var_ref and var_check rules --> 
                    <sch:rule context="oval-def:objects/*/*[@var_ref]|oval-def:objects/*/*/*[@var_ref]|oval-def:states/*/*[@var_ref]|oval-def:states/*/*/*[@var_ref]">
                        <sch:let name="var_ref" value="@var_ref"/>
                        <sch:assert test=".=''">
            <sch:value-of select="../@id"/> - a var_ref has been supplied for the <sch:value-of select="name()"/> entity so no value should be provided</sch:assert>
                        <sch:assert test="( (not(@datatype)) and ('string' = ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype) ) or (@datatype = ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype)">
            <sch:value-of select="$var_ref"/> - inconsistent datatype between the variable and an associated var_ref</sch:assert>
                    </sch:rule>
                    <sch:rule context="oval-def:objects/*/*[@var_ref]|oval-def:objects/*/*/*[@var_ref]">
                        <sch:report test="not(@var_check)">
            <sch:value-of select="../@id"/> - a var_ref has been supplied for the <sch:value-of select="name()"/> entity so a var_check should also be provided</sch:report>
                    </sch:rule>
                    <sch:rule context="oval-def:objects/*/*[@var_check]|oval-def:objects/*/*/*[@var_check]">
                        <sch:assert test="@var_ref">
            <sch:value-of select="../@id"/> - a var_check has been supplied for the <sch:value-of select="name()"/> entity so a var_ref must also be provided</sch:assert>
                    </sch:rule>
                    <sch:rule context="oval-def:states/*/*[@var_ref]|oval-def:states/*/*/*[@var_ref]">
                        <sch:report test="not(@var_check)">
            <sch:value-of select="../@id"/> - a var_ref has been supplied for the <sch:value-of select="name()"/> entity so a var_check should also be provided</sch:report>
                    </sch:rule>
                    <sch:rule context="oval-def:states/*/*[@var_check]|oval-def:states/*/*/*[@var_check]">
                        <sch:assert test="@var_ref">
            <sch:value-of select="../@id"/> - a var_check has been supplied for the <sch:value-of select="name()"/> entity so a var_ref must also be provided</sch:assert>
                    </sch:rule>
                    <!-- datatype and operation rules -->
                    <sch:rule context="oval-def:objects/*/*[not(@datatype)]|oval-def:objects/*/*/*[not(@datatype)]|oval-def:states/*/*[not(@datatype)]|oval-def:states/*/*/*[not(@datatype)]">
                        <sch:assert test="not(@operation) or @operation='equals' or @operation='not equal' or @operation='case insensitive equals' or @operation='case insensitive not equal' or @operation='pattern match'">
            <sch:value-of select="../@id"/> - The use of '<sch:value-of select="@operation"/>' for the operation attribute of the <sch:value-of select="name()"/> entity is not valid given the lack of a declared datatype (hence a default datatype of string).</sch:assert>
                    </sch:rule>
                    <sch:rule context="oval-def:objects/*/*[@datatype='binary']|oval-def:objects/*/*/*[@datatype='binary']|oval-def:states/*/*[@datatype='binary']|oval-def:states/*/*/*[@datatype='binary']">
                        <sch:assert test="not(@operation) or @operation='equals' or @operation='not equal'">
            <sch:value-of select="../@id"/> - The use of '<sch:value-of select="@operation"/>' for the operation attribute of the <sch:value-of select="name()"/> entity is not valid given a datatype of binary.</sch:assert>
                        <!--<sch:assert test="matches(., '^[0-9a-fA-F]*$')"><sch:value-of select="../@id"/> - A value of '<sch:value-of select="."/>' for the <sch:value-of select="name()"/> entity is not valid given a datatype of binary.</sch:assert>-->
                    </sch:rule>
                    <sch:rule context="oval-def:objects/*/*[@datatype='boolean']|oval-def:objects/*/*/*[@datatype='boolean']|oval-def:states/*/*[@datatype='boolean']|oval-def:states/*/*/*[@datatype='boolean']">
                        <sch:assert test="not(@operation) or @operation='equals' or @operation='not equal'">
            <sch:value-of select="../@id"/> - The use of '<sch:value-of select="@operation"/>' for the operation attribute of the <sch:value-of select="name()"/> entity is not valid given a datatype of boolean.</sch:assert>
                        <!--<sch:assert test="matches(., '^true$|^false$|^1$|^0$')"><sch:value-of select="../@id"/> - A value of '<sch:value-of select="."/>' for the <sch:value-of select="name()"/> entity is not valid given a datatype of boolean.</sch:assert>-->
                    </sch:rule>
                    <sch:rule context="oval-def:objects/*/*[@datatype='evr_string']|oval-def:objects/*/*/*[@datatype='evr_string']|oval-def:states/*/*[@datatype='evr_string']|oval-def:states/*/*/*[@datatype='evr_string']">
                        <sch:assert test="not(@operation) or @operation='equals' or @operation='not equal' or  @operation='greater than' or @operation='greater than or equal' or @operation='less than' or @operation='less than or equal'">
            <sch:value-of select="../@id"/> - The use of '<sch:value-of select="@operation"/>' for the operation attribute of the <sch:value-of select="name()"/> entity is not valid given a datatype of evr_string.</sch:assert>
                        <!--<sch:assert test="matches(., '^[^:\-]*:[^:\-]*-[^:\-]*$')"><sch:value-of select="../@id"/> - A value of '<sch:value-of select="."/>' for the <sch:value-of select="name()"/> entity is not valid given a datatype of evr_string.</sch:assert>-->
                    </sch:rule>
                    <sch:rule context="oval-def:objects/*/*[@datatype='fileset_revision']|oval-def:objects/*/*/*[@datatype='fileset_revision']|oval-def:states/*/*[@datatype='fileset_revision']|oval-def:states/*/*/*[@datatype='fileset_revision']">
                        <sch:assert test="not(@operation) or @operation='equals' or @operation='not equal' or  @operation='greater than' or @operation='greater than or equal' or @operation='less than' or @operation='less than or equal'">
            <sch:value-of select="../@id"/> - The use of '<sch:value-of select="@operation"/>' for the operation attribute of the <sch:value-of select="name()"/> entity is not valid given a datatype of fileset_revision.</sch:assert>
                    </sch:rule>
                    <sch:rule context="oval-def:objects/*/*[@datatype='float']|oval-def:objects/*/*/*[@datatype='float']|oval-def:states/*/*[@datatype='float']|oval-def:states/*/*/*[@datatype='float']">
                        <sch:assert test="not(@operation) or @operation='equals' or @operation='not equal' or @operation='greater than' or @operation='greater than or equal' or @operation='less than' or @operation='less than or equal'">
            <sch:value-of select="../@id"/> - The use of '<sch:value-of select="@operation"/>' for the operation attribute of the <sch:value-of select="name()"/> entity is not valid given a datatype of float.</sch:assert>
                        <!--<sch:assert test="matches(., '^[+\-]?[0-9]+([\.][0-9]+)?([eE][+\-]?[0-9]+)?$|^NaN$|^INF$|^\-INF$')"><sch:value-of select="../@id"/> - A value of '<sch:value-of select="."/>' for the <sch:value-of select="name()"/> entity is not valid given a datatype of float.</sch:assert>-->
                    </sch:rule>
                    <sch:rule context="oval-def:objects/*/*[@datatype='ios_version']|oval-def:objects/*/*/*[@datatype='ios_version']|oval-def:states/*/*[@datatype='ios_version']|oval-def:states/*/*/*[@datatype='ios_version']">
                        <sch:assert test="not(@operation) or @operation='equals' or @operation='not equal' or @operation='greater than' or @operation='greater than or equal' or @operation='less than' or @operation='less than or equal'">
            <sch:value-of select="../@id"/> - The use of '<sch:value-of select="@operation"/>' for the operation attribute of the <sch:value-of select="name()"/> entity is not valid given a datatype of ios_version.</sch:assert>
                    </sch:rule>
                    <sch:rule context="oval-def:objects/*/*[@datatype='int']|oval-def:objects/*/*/*[@datatype='int']|oval-def:states/*/*[@datatype='int']|oval-def:states/*/*/*[@datatype='int']">
                        <sch:assert test="not(@operation) or @operation='equals' or @operation='not equal' or @operation='greater than' or @operation='greater than or equal' or @operation='less than' or @operation='less than or equal' or @operation='bitwise and' or @operation='bitwise or'">
            <sch:value-of select="../@id"/> - The use of '<sch:value-of select="@operation"/>' for the operation attribute of the <sch:value-of select="name()"/> entity is not valid given a datatype of int.</sch:assert>
                        <!--<sch:assert test="matches(., '^[+\-]?[0-9]+$')"><sch:value-of select="../@id"/> - A value of '<sch:value-of select="."/>' for the <sch:value-of select="name()"/> entity is not valid given a datatype of int.</sch:assert>-->
                    </sch:rule>
                    <sch:rule context="oval-def:objects/*/*[@datatype='ipv4_address']|oval-def:objects/*/*/*[@datatype='ipv4_address']|oval-def:states/*/*[@datatype='ipv4_address']|oval-def:states/*/*/*[@datatype='ipv4_address']">
                        <sch:assert test="not(@operation) or @operation='equals' or @operation='not equal' or @operation='greater than' or @operation='greater than or equal' or @operation='less than' or @operation='less than or equal' or @operation='subset of' or @operation='superset of'">
            <sch:value-of select="../@id"/> - The use of '<sch:value-of select="@operation"/>' for the operation attribute of the <sch:value-of select="name()"/> entity is not valid given a datatype of ipv4_address.</sch:assert>
                        <!-- TODO <sch:assert test="matches(we_need_regex_for_ipv4)"><sch:value-of select="../@id"/> - A value of '<sch:value-of select="."/>' for the <sch:value-of select="name()"/> entity is not valid given a datatype of ipv4_address.</sch:assert>-->
                    </sch:rule>
                    <sch:rule context="oval-def:objects/*/*[@datatype='ipv6_address']|oval-def:objects/*/*/*[@datatype='ipv6_address']|oval-def:states/*/*[@datatype='ipv6_address']|oval-def:states/*/*/*[@datatype='ipv6_address']">
                        <sch:assert test="not(@operation) or @operation='equals' or @operation='not equal' or @operation='greater than' or @operation='greater than or equal' or @operation='less than' or @operation='less than or equal' or @operation='subset of' or @operation='superset of'">
            <sch:value-of select="../@id"/> - The use of '<sch:value-of select="@operation"/>' for the operation attribute of the <sch:value-of select="name()"/> entity is not valid given a datatype of ipv6_address.</sch:assert>
                        <!-- TODO <sch:assert test="matches(we_need_regex_for_ipv6)"><sch:value-of select="../@id"/> - A value of '<sch:value-of select="."/>' for the <sch:value-of select="name()"/> entity is not valid given a datatype of ipv6_address.</sch:assert>-->
                    </sch:rule>
                    <sch:rule context="oval-def:objects/*/*[@datatype='string']|oval-def:objects/*/*/*[@datatype='string']|oval-def:states/*/*[@datatype='string']|oval-def:states/*/*/*[@datatype='string']">
                        <sch:assert test="not(@operation) or @operation='equals' or @operation='not equal' or @operation='case insensitive equals' or @operation='case insensitive not equal' or @operation='pattern match'">
            <sch:value-of select="../@id"/> - The use of '<sch:value-of select="@operation"/>' for the operation attribute of the <sch:value-of select="name()"/> entity is not valid given a datatype of string.</sch:assert>
                    </sch:rule>
                    <sch:rule context="oval-def:objects/*/*[@datatype='version']|oval-def:objects/*/*/*[@datatype='version']|oval-def:states/*/*[@datatype='version']|oval-def:states/*/*/*[@datatype='version']">
                        <sch:assert test="not(@operation) or @operation='equals' or @operation='not equal' or @operation='greater than' or @operation='greater than or equal' or @operation='less than' or @operation='less than or equal'">
            <sch:value-of select="../@id"/> - The use of '<sch:value-of select="@operation"/>' for the operation attribute of the <sch:value-of select="name()"/> entity is not valid given a datatype of version.</sch:assert>
                    </sch:rule>
                    <sch:rule context="oval-def:objects/*/*[@datatype='record']|oval-def:states/*/*[@datatype='record']">
                        <sch:assert test="not(@operation) or @operation='equals'">
            <sch:value-of select="../@id"/> - The use of '<sch:value-of select="@operation"/>' for the operation attribute of the <sch:value-of select="name()"/> entity is not valid given a datatype of record.</sch:assert>
                    </sch:rule>
                </sch:pattern>
   <sch:pattern id="oval-def_no_var_ref_with_records">
                    <sch:rule context="oval-def:objects/*/*[@datatype='record']|oval-def:states/*/*[@datatype='record']">
                        <sch:assert test="not(@var_ref)">
            <sch:value-of select="../@id"/> - The use of var_ref is prohibited when the datatype is 'record'.</sch:assert>
                    </sch:rule>
                </sch:pattern>
   <sch:pattern id="oval-def_definition_entity_type_check_rules">
                    <sch:rule context="oval-def:objects/*/*[not((@xsi:nil='1' or @xsi:nil='true')) and not(@var_ref) and @datatype='int']|oval-def:objects/*/*/*[not(@var_ref) and @datatype='int']|oval-def:states/*/*[not((@xsi:nil='1' or @xsi:nil='true')) and not(@var_ref) and @datatype='int']|oval-def:states/*/*/*[not(@var_ref) and @datatype='int']">
                        <sch:assert test="(not(contains(.,'.'))) and (number(.) = floor(.))">
            <sch:value-of select="../@id"/> - The datatype for the <sch:value-of select="name()"/> entity is 'int' but the value is not an integer.</sch:assert>
                        <!--  Must test for decimal point because number(x.0) = floor(x.0) is true -->
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
   <sch:diagnostics/>
</sch:schema>