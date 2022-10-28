<?xml version="1.0" encoding="UTF-8"?>
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt">
    <sch:title>Schematron validation for an XCCDF file</sch:title>
    <!--<sch:ns prefix="scap" uri="http://scap.nist.gov/schema/scap/source/1.2" />-->
    <sch:ns prefix="xccdf" uri="http://checklists.nist.gov/xccdf/1.2" />
    <sch:ns prefix="cpe2" uri="http://cpe.mitre.org/language/2.0" />
    <sch:ns prefix="xhtml" uri="http://www.w3.org/1999/xhtml" />
    <sch:ns prefix="dc" uri="http://purl.org/dc/elements/1.1/" />
    <sch:ns prefix="dsig" uri="http://www.w3.org/2000/09/xmldsig#" />
    <sch:phase id="Benchmark">
        <!-- Errors -->
        <sch:active pattern="xhtml_object_data_prefix"/>
        <sch:active pattern="benchmark_multiple_status_dates"/>
        <sch:active pattern="benchmark_extend_self_type"/>
        <sch:active pattern="benchmark_check_import_empty"/>
        <sch:active pattern="benchmark_unique_match_selector"/>
        <sch:active pattern="benchmark_omitted_match_selector"/>
        <sch:active pattern="benchmark_unique_lower_bound_selector"/>
        <sch:active pattern="benchmark_omitted_lower_bound_selector"/>
        <sch:active pattern="benchmark_unique_upper_bound_selector"/>
        <sch:active pattern="benchmark_omitted_upper_bound_selector"/>
        <sch:active pattern="benchmark_unique_choices_selector"/>
        <sch:active pattern="benchmark_omitted_choices_selector"/>
        <sch:active pattern="benchmark_unique_value_value_selector"/>
        <sch:active pattern="benchmark_omitted_value_value_selector"/>
        <sch:active pattern="benchmark_unique_value_default_selector"/>
        <sch:active pattern="benchmark_omitted_value_default_selector"/>
        <sch:active pattern="benchmark_rule_group_idref_exists"/>
        <sch:active pattern="benchmark_value_idref_exists"/>
        <sch:active pattern="benchmark_rule_result_override"/>
        <sch:active pattern="benchmark_rule_result_idref_exists"/>
        <!-- Warnings -->
        <sch:active pattern="xhtml_object_data_values"/>
        <sch:active pattern="xhtml_object_data_values_exist"/>
        <sch:active pattern="xhtml_a_name_link_prefix"/>
        <sch:active pattern="xccdf_signature_dsig_ref"/>
        <sch:active pattern="xccdf_contains_title_child"/>
        <sch:active pattern="benchmark_contains_metadata"/>
        <sch:active pattern="benchmark_platform_invalid_prefix"/>
        <sch:active pattern="benchmark_platform_prefix_deprecated"/>
        <sch:active pattern="benchmark_platform_specification_exists"/>
        <sch:active pattern="benchmark_platform_exists"/>
        <sch:active pattern="benchmark_description_exists"/>
        <!-- benchmark_required_exists -->
        <sch:active pattern="benchmark_conflicts_exists"/>
        <sch:active pattern="benchmark_note_tag_exists"/>
        <sch:active pattern="benchmark_value_match_datatype"/>
        <sch:active pattern="benchmark_value_bounds_datatype"/>
        <sch:active pattern="benchmark_default_value_value_selector"/>
        <sch:active pattern="benchmark_default_value_default_selector"/>
        <sch:active pattern="benchmark_default_value_match_selector"/>
        <sch:active pattern="benchmark_default_value_lower_bound_selector"/>
        <sch:active pattern="benchmark_default_value_upper_bound_selector"/>
        <sch:active pattern="benchmark_default_value_choices_selector"/>
        <sch:active pattern="benchmark_multi_check_true"/>
        <sch:active pattern="benchmark_default_rule_check_selector"/>
        <sch:active pattern="benchmark_value_description_question_exists"/>
        <sch:active pattern="benchmark_testresult_test_system_exists"/>
        <sch:active pattern="benchmark_rule_result_single_child"/>
        <sch:active pattern="benchmark_rule_result_check_valid"/>
        <sch:active pattern="benchmark_group_deprecated_extends"/>
        <sch:active pattern="benchmark_group_deprecated_abstract"/>
        <sch:active pattern="benchmark_rule_deprecated_impact_metric"/>
        <!-- Tailoring -->
        <!-- Errors -->
        <sch:active pattern="xhtml_object_data_prefix"/>
        <sch:active pattern="tailoring_profile_extends_valid"/>
        <!-- Warnings -->
        <sch:active pattern="xhtml_object_data_values"/>
        <sch:active pattern="xhtml_object_data_values_exist"/>
        <sch:active pattern="xhtml_a_name_link_prefix"/>
        <sch:active pattern="xccdf_signature_dsig_ref"/>
        <sch:active pattern="xccdf_contains_title_child"/>
        <sch:active pattern="tailoring_group_deprecated_abstract"/>
    </sch:phase>
    <sch:phase id="Tailoring">
        <!-- Errors -->
        <sch:active pattern="xhtml_object_data_prefix"/>
        <sch:active pattern="tailoring_profile_extends_valid"/>
        <!-- Warnings -->
        <sch:active pattern="xhtml_object_data_values"/>
        <sch:active pattern="xhtml_object_data_values_exist"/>
        <sch:active pattern="xhtml_a_name_link_prefix"/>
        <sch:active pattern="xccdf_signature_dsig_ref"/>
        <sch:active pattern="xccdf_contains_title_child"/>
        <sch:active pattern="tailoring_group_deprecated_abstract"/>
    </sch:phase>
    <sch:phase id="ARF-Check">
        <!-- Errors -->
        <sch:active pattern="xhtml_object_data_prefix"/>
        <sch:active pattern="benchmark_multiple_status_dates"/>
        <sch:active pattern="benchmark_extend_self_type"/>
        <sch:active pattern="benchmark_check_import_empty"/>
        <sch:active pattern="benchmark_unique_match_selector"/>
        <sch:active pattern="benchmark_omitted_match_selector"/>
        <sch:active pattern="benchmark_unique_lower_bound_selector"/>
        <sch:active pattern="benchmark_omitted_lower_bound_selector"/>
        <sch:active pattern="benchmark_unique_upper_bound_selector"/>
        <sch:active pattern="benchmark_omitted_upper_bound_selector"/>
        <sch:active pattern="benchmark_unique_choices_selector"/>
        <sch:active pattern="benchmark_omitted_choices_selector"/>
        <sch:active pattern="benchmark_unique_value_value_selector"/>
        <sch:active pattern="benchmark_omitted_value_value_selector"/>
        <sch:active pattern="benchmark_unique_value_default_selector"/>
        <sch:active pattern="benchmark_omitted_value_default_selector"/>
        <sch:active pattern="benchmark_rule_group_idref_exists"/>
        <sch:active pattern="benchmark_value_idref_exists"/>
        <!-- Warnings -->
        <sch:active pattern="xhtml_object_data_values"/>
        <sch:active pattern="xhtml_object_data_values_exist"/>
        <sch:active pattern="xhtml_a_name_link_prefix"/>
        <sch:active pattern="xccdf_signature_dsig_ref"/>
        <sch:active pattern="xccdf_contains_title_child"/>
        <sch:active pattern="benchmark_contains_metadata"/>
        <sch:active pattern="benchmark_platform_invalid_prefix"/>
        <sch:active pattern="benchmark_platform_prefix_deprecated"/>
        <sch:active pattern="benchmark_platform_specification_exists"/>
        <sch:active pattern="benchmark_platform_exists"/>
        <sch:active pattern="benchmark_description_exists"/>
        <!-- benchmark_required_exists -->
        <sch:active pattern="benchmark_conflicts_exists"/>
        <sch:active pattern="benchmark_note_tag_exists"/>
        <sch:active pattern="benchmark_value_match_datatype"/>
        <sch:active pattern="benchmark_value_bounds_datatype"/>
        <sch:active pattern="benchmark_default_value_value_selector"/>
        <sch:active pattern="benchmark_default_value_default_selector"/>
        <sch:active pattern="benchmark_default_value_match_selector"/>
        <sch:active pattern="benchmark_default_value_lower_bound_selector"/>
        <sch:active pattern="benchmark_default_value_upper_bound_selector"/>
        <sch:active pattern="benchmark_default_value_choices_selector"/>
        <sch:active pattern="benchmark_multi_check_true"/>
        <sch:active pattern="benchmark_default_rule_check_selector"/>
        <sch:active pattern="benchmark_value_description_question_exists"/>
        <sch:active pattern="benchmark_testresult_test_system_exists"/>
        <sch:active pattern="benchmark_group_deprecated_extends"/>
        <sch:active pattern="benchmark_group_deprecated_abstract"/>
        <sch:active pattern="benchmark_rule_deprecated_impact_metric"/>
    </sch:phase>
    <!-- ************************************************************** -->
    <!-- **********  Rules for Benchmark/Tailoring Elements  ********** -->
    <!-- ************************************************************** -->
    <!-- Errors -->
    <sch:pattern id="xhtml_object_data_prefix">
        <sch:rule context="xhtml:object">
            <sch:assert test="starts-with(@data,'#xccdf:')">Error: The @data attribute of an XHTML 'object' element must be prefixed with '#xccdf:'. See the XCCDF 1.2.1 specification, Section 7.2.3.6.3.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <!-- Warnings -->
    <sch:pattern id="xhtml_object_data_values">
        <sch:rule context="xhtml:object">
            <sch:assert flag="WARNING" test="starts-with(@data,'#xccdf:title:') or starts-with(@data,'#xccdf:value:')">Warning: The @data attribute of an XHTML 'object' element should have a pattern of either '#xccdf:value:[id]' or '#xccdf:title:[id]'. See the XCCDF 1.2.1 specification, Section 7.2.3.6.3.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="xhtml_object_data_values_exist">
        <sch:rule context="xhtml:object[starts-with(@data,'#xccdf:value:')]">
            <sch:let name="object_data_value_valid" value="not(substring-after(@data,'#xccdf:value:')='')"/>
            <sch:let name="object_data_id" value="substring-after(@data,'#xccdf:value:')"/>
            <sch:assert flag="WARNING" test="($object_data_value_valid) and (/*//xccdf:plain-text[@id=$object_data_id]|/*//xccdf:Value[@id=$object_data_id]|/*//xccdf:fact[@id=$object_data_id])">Warning: The given id '<sch:value-of select="$object_data_id"/>' should match a the @id attribute of a 'plain-text', 'Value', or 'fact' element. See the XCCDF 1.2.1 specification, Section 7.2.3.6.3.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="xhtml_a_name_link_prefix"> <!-- CMS - update to use the @href attribute, and verify that everything following the link: matches some <a> element's @name value -->
        <sch:rule context="xhtml:a[@name]">
            <sch:assert flag="WARNING" test="starts-with(@name,'#xccdf:link:')">Warning: The @name attribute of an XHTML 'a' element should be prefixed with '#xccdf:link:'. See the XCCDF 1.2.1 specification, Section 7.2.3.6.4.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="xccdf_signature_dsig_ref">
        <sch:rule context="xccdf:signature">
            <sch:assert flag="WARNING" test="./dsig:Signature/dsig:Reference">Warning: A 'signature' element should contain an XML dsig 'Signature' element, which should contain one or more 'References' elements. See the XCCDF 1.2.1 specification, Section 6.2.7.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="xccdf_contains_title_child">
        <sch:rule context="xccdf:Benchmark|xccdf:Rule|xccdf:Group|xccdf:Value|xccdf:Profile|xccdf:Tailoring/xccdf:title">
            <sch:assert flag="WARNING" test="./xccdf:title">Warning: A '<sch:value-of select="local-name()"/>' element should contain a 'title' element.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <!-- ************************************************************** -->
    <!-- ***************  Rules for Benchmark Elements  *************** -->
    <!-- ************************************************************** -->
    <!-- Errors -->
    <sch:pattern id="benchmark_multiple_status_dates">
        <sch:rule context="xccdf:Benchmark|xccdf:Rule|xccdf:Group|xccdf:Value|xccdf:Profile">
            <sch:assert test="(count(./xccdf:status) &lt; 2) or ((count(./xccdf:status) &gt; 1) and not(./xccdf:status[not(@date)]))">Error: A '<sch:value-of select="local-name()"/>' element with multiple 'status' elements must have the date attribute present in all 'status' elements. See the XCCDF 1.2.1 specification, Section 6.2.8.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_extend_self_type">
        <sch:rule context="xccdf:Rule[@extends]|xccdf:Group[@extends]|xccdf:Value[@extends]|xccdf:Profile[@extends]">
            <sch:let name="extend_ref" value="./@extends"/>
            <sch:assert test="local-name() = local-name(/*//xccdf:Rule[@id = $extend_ref]|/*//xccdf:Group[@id = $extend_ref]|/*//xccdf:Value[@id = $extend_ref]|/*//xccdf:Profile[@id = $extend_ref])">Error: A '<sch:value-of select="local-name()"/>' element with an @extends attribute must extend another '<sch:value-of select="local-name()"/>' element. </sch:assert>
        </sch:rule>
        <!-- Circular reference check?? "See the XCCDF 1.2.1 specification, Section 6.3.1." -->
    </sch:pattern>
    <sch:pattern id="benchmark_check_import_empty">
        <sch:rule context="xccdf:Rule/xccdf:check/xccdf:check-import">  
            <sch:assert test="count(./*|text()) = 0">Error: A 'check-import' element within a 'Rule' element must have an empty body. See the XCCDF 1.2.1 specification, Section 6.4.4.4.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_unique_match_selector">
        <sch:rule context="xccdf:Value[count(xccdf:match)>1]/xccdf:match">
            <sch:let name="selector" value="concat('',./@selector)"/>       
            <sch:assert test="count(../xccdf:match[(@selector = $selector or (not(@selector) and $selector = ''))]) = 1">Error: A 'Value' element containing multiple 'match' elements must have a unique @selector attribute for each 'match' element.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_omitted_match_selector">
        <sch:rule context="xccdf:Value[count(xccdf:match[not(@selector)]) > 1]/xccdf:match[not(@selector)]">
            <sch:assert test="false()">Error: A 'Value' element may only contain zero or one 'match' elements with an omitted @selector attribute.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_unique_lower_bound_selector">
        <sch:rule context="xccdf:Value[count(xccdf:lower-bound)>1]/xccdf:lower-bound">
            <sch:let name="selector" value="concat('',./@selector)"/>       
            <sch:assert test="count(../xccdf:lower-bound[(@selector = $selector or (not(@selector) and $selector = ''))]) = 1">Error: A 'Value' element containing multiple 'lower-bound' elements must have a unique @selector attribute for each 'lower-bound' element.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_omitted_lower_bound_selector">
        <sch:rule context="xccdf:Value[count(xccdf:lower-bound[not(@selector)]) > 1]/xccdf:lower-bound[not(@selector)]">
            <sch:assert test="false()">Error: A 'Value' element may only contain zero or one 'lower-bound' elements with an omitted @selector attribute.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_unique_upper_bound_selector">
        <sch:rule context="xccdf:Value[count(xccdf:upper-bound)>1]/xccdf:upper-bound">
            <sch:let name="selector" value="concat('',./@selector)"/>       
            <sch:assert test="count(../xccdf:upper-bound[(@selector = $selector or (not(@selector) and $selector = ''))]) = 1">Error: A 'Value' element containing multiple 'upper-bound' elements must have a unique @selector attribute for each 'upper-bound' element.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_omitted_upper_bound_selector">
        <sch:rule context="xccdf:Value[count(xccdf:upper-bound[not(@selector)]) > 1]/xccdf:upper-bound[not(@selector)]">
            <sch:assert test="false()">Error: A 'Value' element may only contain zero or one 'upper-bound' elements with an omitted @selector attribute.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_unique_choices_selector">
        <sch:rule context="xccdf:Value[count(xccdf:choices)>1]/xccdf:choices">
            <sch:let name="selector" value="concat('',./@selector)"/>       
            <sch:assert test="count(../xccdf:choices[(@selector = $selector or (not(@selector) and $selector = ''))]) = 1">Error: A 'Value' element containing multiple 'choices' elements must have a unique @selector attribute for each 'choices' element.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_omitted_choices_selector">
        <sch:rule context="xccdf:Value[count(xccdf:choices[not(@selector)]) > 1]/xccdf:choices[not(@selector)]">
            <sch:assert test="false()">Error: A 'Value' element may only contain zero or one 'choices' elements with an omitted @selector attribute.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_unique_value_value_selector">
        <sch:rule context="xccdf:Value[count(xccdf:value|xccdf:complex-value)>1]/xccdf:value|xccdf:Value[count(xccdf:value|xccdf:complex-value)>1]/xccdf:complex-value">
            <sch:let name="selector" value="concat('',./@selector)"/>       
            <sch:assert test="count(../xccdf:value[(@selector = $selector or (not(@selector) and $selector = ''))]|../xccdf:complex-value[(@selector = $selector or (not(@selector) and $selector = ''))]) = 1">Error: A 'Value' element containing multiple 'value' or 'complex-value' elements must have a unique @selector attribute for each 'value' or 'complex-value' element.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_omitted_value_value_selector">
        <sch:rule context="xccdf:Value[count(xccdf:value[not(@selector)]|xccdf:complex-value[not(@selector)]) > 1]">
            <sch:assert test="false()">Error: A 'Value' element may only contain zero or one 'value' or 'complex-value' elements with an omitted @selector attribute.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_unique_value_default_selector">
        <sch:rule context="xccdf:Value[count(xccdf:default|xccdf:complex-default)>1]/xccdf:default|xccdf:Value[count(xccdf:default|xccdf:complex-default)>1]/xccdf:complex-default">
            <sch:let name="selector" value="concat('',./@selector)"/>       
            <sch:assert test="count(../xccdf:default[(@selector = $selector or (not(@selector) and $selector = ''))]|../xccdf:complex-default[(@selector = $selector or (not(@selector) and $selector = ''))]) = 1">Error: A 'Value' element containing multiple 'default' or 'complex-default' elements must have a unique @selector attribute for each 'default' or 'complex-default' element.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_omitted_value_default_selector">
        <sch:rule context="xccdf:Value[count(xccdf:default[not(@selector)]|xccdf:complex-default[not(@selector)]) > 1]">
            <sch:assert test="false()">Error: A 'Value' element may only contain zero or one 'default' or 'complex-default' elements with an omitted @selector attribute.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_rule_group_idref_exists">
        <sch:rule context="xccdf:Benchmark/xccdf:Profile[xccdf:select]/xccdf:select|xccdf:Benchmark/xccdf:Profile[xccdf:refine-rule]/xccdf:refine-rule">
            <sch:let name="idref" value="./@idref"/>
            <sch:assert test="/*//xccdf:Rule[@id=$idref or @cluster-id=$idref]|/*//xccdf:Group[@id=$idref or @cluster-id=$idref]">Error: The given @idref attribute '<sch:value-of select="$idref"/>' must match a the @id or @cluster-id attributes of a 'Rule' or 'Group' element. See the XCCDF 1.2.1 specification, Section 6.5.3.</sch:assert>
        </sch:rule>
    </sch:pattern>

    <sch:pattern id="benchmark_value_idref_exists">
        <sch:rule context="xccdf:Benchmark/xccdf:Profile[xccdf:refine-value|xccdf:set-value|xccdf:set-complex-value]/xccdf:refine-value|xccdf:Benchmark/xccdf:Profile/xccdf:set-value|xccdf:Benchmark/xccdf:Profile/xccdf:set-complex-value">
            <sch:let name="idref" value="./@idref"/>
            <sch:assert test="/*//xccdf:Value[@id=$idref or @cluster-id=$idref]">Error: The given @idref attribute '<sch:value-of select="$idref"/>' must match a the @id or @cluster-id attributes of a 'Value' element. See the XCCDF 1.2.1 specification, Section 6.5.3.</sch:assert>
        </sch:rule>
    </sch:pattern>

    <sch:pattern id="benchmark_rule_result_override">
        <sch:rule context="xccdf:TestResult[xccdf:rule-result[xccdf:override]]/xccdf:rule-result/xccdf:result">
            <sch:let name="current_result" value="."/>
            <sch:assert test="../xccdf:override/xccdf:new-result = $current_result">Error: The value of the 'result' element must match the value of the 'new-result' element in an 'override' element when present. See the XCCDF 1.2.1 specification, Section 6.6.4.3.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_rule_result_idref_exists">
        <sch:rule context="xccdf:TestResult[xccdf:rule-result]/xccdf:rule-result">
            <sch:let name="idref" value="./@idref"/>
            <sch:assert test="./ancestor::xccdf:Benchmark//xccdf:Rule[@id=$idref]">Error: The given @idref attribute '<sch:value-of select="$idref"/>' must match a the @id attribute of a 'Rule' element. See the XCCDF 1.2.1 specification, Section 6.6.4.1.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="ARF_benchmark_rule_result_idref_exists">
        <sch:rule context="xccdf:TestResult[xccdf:rule-result]/xccdf:rule-result">
            <sch:let name="idref" value="./@idref"/>
            <sch:assert test="./ancestor::xccdf:Benchmark//xccdf:Rule[@id=$idref]">Error: The given @idref attribute '<sch:value-of select="$idref"/>' must match a the @id attribute of a 'Rule' element. See the XCCDF 1.2.1 specification, Section 6.6.4.1.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <!-- Warnings -->
    <sch:pattern id="benchmark_contains_metadata">
        <sch:rule context="xccdf:Benchmark[(count(xccdf:metadata) = 0) or (not(xccdf:metadata/*[namespace-uri() = 'http://purl.org/dc/elements/1.1/']))]">
            <sch:assert flag="WARNING" test="false()">Warning: A 'Benchmark' element should have a 'metadata' element, and it should contain a child from the Dublin Core schema. See the XCCDF 1.2.1 specification, Section 6.2.4.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_platform_invalid_prefix">
        <sch:rule context="xccdf:platform[not(starts-with(@idref,'cpe:/') or starts-with(@idref,'cpe:2.3:') or starts-with(@idref,'#'))]">
            <sch:assert flag="WARNING" test="false()">Warning: The @idref attribute of a 'platform' element must begin with 'cpe:/' (CPE name version 2.2 and earlier),'cpe:2.3:' (CPE name version 2.3), or '#' (followed by the @id value of a CPE 'platform-specification' element). See the XCCDF 1.2.1 specification, Section 6.2.5.</sch:assert>    
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_platform_prefix_deprecated">
        <sch:rule context="xccdf:platform[starts-with(@idref,'cpe:/')]">
            <sch:assert flag="WARNING" test="false()">Warning: The 'cpe:/' prefix (CPE URI binding) is allowed within an @idref attribute, but the CPE Formatted String binding is preferred. See the XCCDF 1.2.1 specification, Section 6.2.5.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_platform_specification_exists">
        <sch:rule context="xccdf:platform[starts-with(@idref,'#')]">
            <sch:let name="platformid" value="substring-after(@idref,'#')"/>
            <sch:assert flag="WARNING" test="./ancestor::xccdf:Benchmark/cpe2:platform-specification/cpe2:platform[@id = $platformid]">Warning: No matching 'platform' element with an @id of '<sch:value-of select="$platformid"/>' found within a 'platform-specification' element of the 'Benchmark' element. See the XCCDF 1.2.1 specification, Section 6.2.5.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_platform_exists">
        <sch:rule context="xccdf:Benchmark[not(xccdf:platform)]">
            <sch:assert flag="WARNING" test="false()">Warning: The 'Benchmark' element has no platform specified, which implies the benchmark applies to all platforms. Applicable platforms should be indicate if possible. See the XCCDF 1.2.1 specification, Section 6.2.5.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_description_exists">
        <sch:rule context="xccdf:Benchmark[not(xccdf:description)]">
            <sch:assert flag="WARNING" test="false()">Warning: A 'Benchmark' element should have at least one 'description' element.  See the XCCDF 1.2.1 specification, Section 6.3.2.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <!-- benchmark_required_exists -->
    <!--
       <sch:pattern id="benchmark_required_exists">
            <sch:rule context="xccdf:Rule[xccdf:requires]/xccdf:requires|xccdf:Group[xccdf:requires]/xccdf:requires">
                <sch:let name="idlist" value="@idref"/>
                <sch:assert test=" ">Warning: A '<sch:value-of select="local-name()"/>' element with multiple 'status' elements must have the date attribute present.</sch:assert>
                /*//xccdf:Profile[contains($idlist,@id)]
            </sch:rule>
        </sch:pattern>
    -->
    <sch:pattern id="benchmark_conflicts_exists">
        <sch:rule context="xccdf:Rule[xccdf:conflicts]/xccdf:conflicts|xccdf:Group[xccdf:conflicts]/xccdf:conflicts">
            <sch:let name="idref" value="@idref"/>
            <sch:let name="parent_id" value="../@id"/>
            <sch:assert flag="WARNING" test="./ancestor::xccdf:Benchmark/xccdf:Rule[not(@id = $parent_id) and @id = $idref]|./ancestor::xccdf:Benchmark/xccdf:Group[not(@id = $parent_id) and @id = $idref]">Warning: The @idref attribute in a 'conflicts' element should match the @id attribute of a different 'Rule' or 'Group' element. See the XCCDF 1.2.1 specification, Section 6.4.1.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_note_tag_exists">
        <sch:rule context="xccdf:Profile[@note-tag]">
            <sch:let name="note_id" value="@note-tag"/>
            <sch:assert flag="WARNING" test="./ancestor::xccdf:Benchmark/xccdf:Rule[xccdf:profile-note[@tag = $note_id]]">Warning: The @note-tag attribute in a 'Profile' element should match the @tag attribute of a 'profile-note' element within a 'Rule' element. See the XCCDF 1.2.1 specification, Section 6.4.4.2.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_value_match_datatype">
        <sch:rule context="xccdf:Value[xccdf:match]">
            <sch:assert test="not(@type) or @type = 'string' or @type = 'number'">Warning: The datatype of a 'Value' element should be 'string' or 'number' if there is a child 'match' element. See the XCCDF 1.2.1 specification, Section 6.4.5.2.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_value_bounds_datatype">
        <sch:rule context="xccdf:Value[xccdf:lower-bound|xccdf:upper-bound]">
            <sch:assert flag="WARNING" test="@type and @type = 'number'">Warning: The datatype of a 'Value' element should be 'number' if there is a child 'lower-bound' or 'upper-bound' element. See the XCCDF 1.2.1 specification, Section 6.4.5.2.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_default_value_value_selector">
        <sch:rule context="xccdf:Value[xccdf:value|xccdf:complex-value][count(xccdf:value[not(@selector) or @selector = '']|xccdf:complex-value[not(@selector) or @selector = '']) = 0]">
            <sch:assert flag="WARNING" test="false()">Warning: All 'value' or 'complex-value' elements have non-empty @selector attribute values. The default selection will be the first of these elements. To explicitly designate a default, remove the selector of the default element. See the XCCDF 1.2.1 specification, Section 6.4.5.5.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_default_value_default_selector">
        <sch:rule context="xccdf:Value[xccdf:default|xccdf:complex-default][count(xccdf:default[not(@selector) or @selector = '']|xccdf:complex-default[not(@selector) or @selector = '']) = 0]">
            <sch:assert flag="WARNING" test="false()">Warning: All 'default' or 'complex-default' elements have non-empty @selector attribute values. The default selection will be the first of these elements. To explicitly designate a default, remove the selector of the default element. See the XCCDF 1.2.1 specification, Section 6.4.5.5.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_default_value_match_selector">
        <sch:rule context="xccdf:Value[xccdf:match][count(xccdf:match[not(@selector) or @selector = '']) = 0]">
            <sch:assert flag="WARNING" test="false()">Warning: All 'match' elements have non-empty @selector attribute values. This means that, by default, no 'match' element is used. To designate a default, remove the @selector from the desired default element. See the XCCDF 1.2.1 specification, Section 6.4.5.5.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_default_value_lower_bound_selector">
        <sch:rule context="xccdf:Value[xccdf:lower-bound][count(xccdf:lower-bound[not(@selector) or @selector = '']) = 0]">
            <sch:assert flag="WARNING" test="false()">Warning: All 'lower-bound' elements have non-empty @selector attribute values. This means that, by default, no 'lower-bound' element is used. To designate a default, remove the @selector from the desired default element. See the XCCDF 1.2.1 specification, Section 6.4.5.5..</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_default_value_upper_bound_selector">
        <sch:rule context="xccdf:Value[xccdf:upper-bound][count(xccdf:upper-bound[not(@selector) or @selector = '']) = 0]">
            <sch:assert flag="WARNING" test="false()">Warning: All 'upper-bound' elements have non-empty @selector attribute values. This means that, by default, no 'upper-bound' element is used. To designate a default, remove the @selector from the desired default element. See the XCCDF 1.2.1 specification, Section 6.4.5.5.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_default_value_choices_selector">
        <sch:rule context="xccdf:Value[xccdf:choices][count(xccdf:choices[not(@selector) or @selector = '']) = 0]">
            <sch:assert flag="WARNING" test="false()">Warning: All 'choices' elements have non-empty @selector attribute values. This means that, by default, no 'choices' element is used. To designate a default, remove the @selector from the desired default element. See the XCCDF 1.2.1 specification, Section 6.4.5.5.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_multi_check_true">
        <sch:rule context="xccdf:complex-check[xccdf:check[@multi-check = 'true']]/xccdf:check[@multi-check = 'true']">
            <sch:assert flag="WARNING" test="false()">Warning: A 'check' element within a 'complex-check' element with its @multi-check attribute set to 'true' must be ignored by the tools. See the XCCDF 1.2.1 specification, Section 6.4.4.4.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_default_rule_check_selector">
        <sch:rule context="xccdf:Rule[xccdf:check][count(xccdf:check[not(@selector) or @selector = '']) = 0]">
            <sch:assert flag="WARNING" test="false()">Warning: All 'check' elements have non-empty @selector attribute values. This means that, by default, no 'check' element is used. To designate a default, remove the @selector from the desired default element. See the XCCDF 1.2.1 specification, Section 6.4.4.4.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_value_description_question_exists">
        <sch:rule context="xccdf:Value[not(xccdf:description|xccdf:question)]">
            <sch:assert flag="WARNING" test="false()">Warning: A 'Value' element should contain at least one 'description' or 'question' element. See the XCCDF 1.2.1 specification, Section 6.4.5.4.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_testresult_test_system_exists">
        <sch:rule context="xccdf:TestResult[not(@test-system)]">
            <sch:assert flag="WARNING" test="false()">Warning: A 'TestResult' element should have a @test-system attribute. See the XCCDF 1.2.1 specification, Section 6.6.2.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_rule_result_single_child">
        <sch:rule context="xccdf:TestResult/xccdf:rule-result">
            <sch:assert flag="WARNING" test="count(./xccdf:check|./xccdf:complex-check) = 1">Warning: A 'rule-result' element should have exactly one child 'check' or 'complex-check' element. This is the conventional way of linking to the checking-system results for this Rule.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_rule_result_check_valid">
        <sch:rule context="xccdf:TestResult/xccdf:rule-result/xccdf:check">
            <sch:let name="selector" value="concat('',./@selector)"/>
            <sch:let name="idref" value="../@idref"/>
            <sch:assert flag="WARNING" test="./ancestor::xccdf:Benchmark//xccdf:Rule[@id=$idref]/xccdf:check[(@selector = $selector) or (not(@selector) and $selector = '')]">Warning: A 'check' element within a 'TestResult/rule-result' element should have a matching @selector attribute as a 'check' element within the referenced 'Rule' element.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_group_deprecated_extends">
        <sch:rule context="xccdf:Group[@extends]">
            <sch:assert flag="WARNING" test="false()">Warning: Deprecated attribute @extends found within 'Group' element. See the XCCDF 1.2.1 specification, Sections 6.4.1 and A.4.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_group_deprecated_abstract">
        <sch:rule context="xccdf:Group[@abstract='true']">
            <sch:assert flag="WARNING" test="false()">Warning: Deprecated behavior of @abstract attribute set to 'true' found within 'Group' element.  See the XCCDF 1.2.1 specification, Sections 6.4.1 and A.4.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <sch:pattern id="benchmark_rule_deprecated_impact_metric">
        <sch:rule context="xccdf:Rule[xccdf:impact-metric]">
            <sch:assert flag="WARNING" test="false()">Warning: Deprecated 'impact-metric' element found within a 'Rule' element. See the XCCDF 1.2.1 specification, Sections 6.4.4.2 and A.4.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <!-- ************************************************************** -->
    <!-- ***************  Rules for Tailoring Elements  *************** -->
    <!-- ************************************************************** -->
    <!-- Errors -->
    <sch:pattern id="tailoring_profile_extends_valid">
        <sch:rule context="xccdf:Tailoring/xccdf:Profile[@extends]">
            <sch:let name="extends_id" value="@extends"/>
            <sch:let name="current_id" value="@id"/>
            <sch:assert test="not(../xccdf:Profile[not(@id = $current_id)][@id = $extends_id])">Error: A 'Profile' element within a 'Tailoring' element may not extend another 'Profile' in that 'Tailoring' element. See the XCCDF 1.2.1 specification, Section 6.7.3.</sch:assert>
        </sch:rule>
    </sch:pattern>
    <!-- Warnings -->
    <sch:pattern id="tailoring_group_deprecated_abstract">
        <sch:rule context="xccdf:Tailoring/xccdf:Profile[@abstract='true']">
            <sch:assert flag="WARNING" test="false()">Warning: 'Profiles' in 'Tailoring' elements may not be extended, so any declared abstract will never be used. See the XCCDF 1.2.1 specification, Section 6.7.2.</sch:assert>
        </sch:rule>
    </sch:pattern>
</sch:schema>
