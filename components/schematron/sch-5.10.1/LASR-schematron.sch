<?xml version="1.0" encoding="UTF-8"?>
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron">

    <sch:ns prefix="ar" uri="http://scap.nist.gov/schema/asset-reporting-format/1.0" />
    <sch:ns prefix="ai" uri="http://scap.nist.gov/schema/asset-identification/1.0" />
    <sch:ns prefix="sr" uri="http://scap.nist.gov/schema/lightweight-asset-summary-results/1.0" />
    <sch:ns prefix="xNL" uri="urn:oasis:names:tc:ciq:xnl:3" />
    <sch:ns prefix="xsd" uri="http://www.w3.org/2001/XMLSchema" />

    <sch:pattern>
        <sch:rule context="ar:Subject">
            <sch:assert test="count(*) eq count(ai:Organization)">The ar:Subject MUST be populated with only ai:Organization elements</sch:assert>
        </sch:rule>
	<sch:rule context="ar:Subject/ai:Organization">
	    <sch:assert test="exists(xNL:OrganisationName)">Each ai:Organization in ar:Subject MUST have at least one xNL:OrganisationName</sch:assert>
	</sch:rule>
        <sch:rule context="ar:ReportMetadata">
            <sch:assert test="exists(ar:Tool/ai:Software)" flag="warning">The ar:Tool in ar:ReportMetadata SHOULD exist and it SHOULD contain at least one ai:Software</sch:assert>
        </sch:rule>
        <sch:rule context="ai:Software">
            <sch:assert test="exists(ai:CPE)">The ai:Software MUST contain at least one ai:CPE</sch:assert>
            <sch:assert test="matches(ai:CPE,'^[c][pP][eE]:/[AHOaho]?(:[A-Za-z0-9\._\-~%]*){0,6}$')">The ai:CPE in ai:Software MUST contain a CPE</sch:assert>
        </sch:rule>
        <sch:rule context="sr:AggregateValue[@type='COUNT']">
            <sch:assert test="matches(.,'^[1-9]\d*$')">Each sr:AggregateValue with @type = 'COUNT' MUST contain an integer value</sch:assert>
        </sch:rule>
        <sch:rule context="sr:AggregateValue[@type='AVERAGE']">
            <sch:assert test="matches(.,'^(0|[1-9]\d*|\d+\.\d+)$')">Each sr:AggregateValue with @type = 'AVERAGE' MUST contain a decimal value</sch:assert>
        </sch:rule>
        <sch:rule context="ar:ReportPayload">
            <sch:assert test="exists(sr:SummaryReport)">The ar:ReportPayload MUST contain a sr:SummaryReport as an immediate child</sch:assert>
        </sch:rule>
        <sch:rule context="sr:SummaryReport">
            <sch:assert test="@id eq 'FISMA_auto_feed_fy10'">The sr:SummaryReport MUST have an @id with value 'FISMA_auto_feed_fy10'</sch:assert>
            <sch:assert test="@version eq '1.0beta1'">The sr:SummaryReport MUST have a @version with value '1.0beta1'</sch:assert>
            <sch:assert test="count(sr:DataPoint) eq 3">Exactly 3 sr:DataPoint MUST be provided on sr:SummaryReport</sch:assert>
            <sch:assert test="exists(sr:DataPoint[@id eq 'configuration_management_agency_deviations'])">sr:DataPoint with @id = 'configuration_management_agency_deviations' MUST exists on sr:SummaryReport</sch:assert>
            <sch:assert test="exists(sr:DataPoint[@id eq 'vulnerability_management_product_vulnerabilities'])">sr:DataPoint with @id = 'vulnerability_management_product_vulnerabilities' MUST exists on sr:SummaryReport</sch:assert>
            <sch:assert test="exists(sr:DataPoint[@id eq 'inventory_management_product_inventory'])">sr:DataPoint with @id = 'inventory_management_product_inventory' MUST exists on sr:SummaryReport</sch:assert>
        </sch:rule>
        <sch:rule context="sr:DataPoint[@id eq 'configuration_management_agency_deviations']/sr:GroupedData">
            <sch:assert test="exists(sr:NamedAttribute[@name eq 'checklist_name'])">The first level sr:GroupedData in sr:DataPoint with @id 'configuration_management_agency_deviations' MUST have a sr:NamedAttribute with @name = 'checklist_name'</sch:assert>
        </sch:rule>
        <sch:rule context="sr:DataPoint[@id eq 'configuration_management_agency_deviations']/sr:GroupedData/sr:GroupedData">
            <sch:assert test="exists(sr:NamedAttribute[@name eq 'checklist_version'])">The second level sr:GroupedData in sr:DataPoint with @id 'configuration_management_agency_deviations' MUST have a sr:NamedAttribute with @name = 'checklist_version'</sch:assert>
        </sch:rule>
        <sch:rule context="sr:DataPoint[@id eq 'configuration_management_agency_deviations']/sr:GroupedData/sr:GroupedData/sr:GroupedData">
            <sch:assert test="exists(sr:NamedAttribute[@name eq 'checklist_profile'])">The third level sr:GroupedData in sr:DataPoint with @id 'configuration_management_agency_deviations' MUST have a sr:NamedAttribute with @name = 'checklist_profile'</sch:assert>
            <sch:assert test="count(sr:AggregateValue) eq 1">The third level sr:GroupedData in sr:DataPoint with @id 'configuration_management_agency_deviations' MUST have exactly one sr:AggregateValue</sch:assert>
            <sch:assert test="count(sr:AggregateValue) eq count(sr:AggregateValue[@name eq 'number_of_systems' and @type eq 'COUNT'])">Each sr:AggregateValue in the third level sr:GroupedData in sr:DataPoint with @id 'configuration_management_agency_deviations' MUST have @name = 'number_of_systems' and @type = 'COUNT'</sch:assert>
            <sch:assert test="number(sr:AggregateValue) ge number(sr:GroupedData/sr:AggregateValue)">The sr:AggregateValue in the third level sr:GroupedData in sr:DataPoint with @id 'configuration_management_agency_deviations' MUST be an integer.  The sr:AggregateValue in the sr:GroupData below the previous one MUST also be an integer.  The former integer MUST be greater than or equal to the latter integer.</sch:assert>
        </sch:rule>
        <sch:rule context="sr:DataPoint[@id eq 'configuration_management_agency_deviations']/sr:GroupedData/sr:GroupedData/sr:GroupedData/sr:GroupedData">
            <sch:assert test="exists(sr:NamedAttribute[@name eq 'http://cce.mitre.org'])">The forth level sr:GroupedData in sr:DataPoint with @id 'configuration_management_agency_deviations' MUST have a sr:NamedAttribute with @name = 'http://cce.mitre.org'</sch:assert>
            <sch:assert test="matches(sr:NamedAttribute, '^CCE-\d{4}-\d$') and (sum(for $j in (for $i in reverse(string-to-codepoints(concat(substring(sr:NamedAttribute,5,4),substring(sr:NamedAttribute,10,1))))[position() mod 2 eq 0] return ($i - 48) * 2, for $i in reverse(string-to-codepoints(concat(substring(sr:NamedAttribute,5,4),substring(sr:NamedAttribute,10,1))))[position() mod 2 eq 1] return ($i - 48)) return ($j mod 10, $j idiv 10)) mod 10) eq 0">The forth level sr:GroupedData in sr:DataPoint with @id 'configuration_management_agency_deviations' MUST have a sr:NamedAttribute with a CCE as a value</sch:assert>
            <sch:assert test="count(sr:AggregateValue) eq 2">The forth level sr:GroupedData in sr:DataPoint with @id 'configuration_management_agency_deviations' MUST have exactly two sr:AggregateValue</sch:assert>
            <sch:assert test="count(sr:AggregateValue[@name eq 'non_compliant_systems' and @type eq 'COUNT']) eq 1">The forth level sr:GroupedData in sr:DataPoint with @id 'configuration_management_agency_deviations' MUST have sr:AggregateValue with @name = 'non_compliant_systems' and @type = 'COUNT'</sch:assert>
            <sch:assert test="count(sr:AggregateValue[@name eq 'number_of_exceptions' and @type eq 'COUNT']) eq 1">The forth level sr:GroupedData in sr:DataPoint with @id 'configuration_management_agency_deviations' MUST have sr:AggregateValue with @name = 'number_of_exceptions' and @type = 'COUNT'</sch:assert>
            <sch:assert test="count(sr:GroupedData) eq 0">The forth level sr:GroupedData in sr:DataPoint with @id 'configuration_management_agency_deviations' MUST have no nested sr:GroupedData</sch:assert>
        </sch:rule>
        <sch:rule context="sr:DataPoint[@id eq 'vulnerability_management_product_vulnerabilities']/sr:GroupedData">
            <sch:assert test="exists(sr:NamedAttribute[@name eq 'http://cve.mitre.org'])">The first level sr:GroupedData in sr:DataPoint with @id 'vulnerability_management_product_vulnerabilities' MUST have a sr:NamedAttribute with @name = 'http://cve.mitre.org'</sch:assert>
            <sch:assert test="starts-with(sr:NamedAttribute,'CVE-')">The first level sr:GroupedData in sr:DataPoint with @id 'vulnerability_management_product_vulnerabilities' MUST have a sr:NamedAttribute with a CVE as a value</sch:assert>
            <sch:assert test="count(sr:GroupedData) eq 0">The first level sr:GroupedData in sr:DataPoint with @id 'vulnerability_management_product_vulnerabilities' MUST have no nested sr:GroupedData</sch:assert>
            <sch:assert test="count(sr:AggregateValue) eq 1">The first level sr:GroupedData in sr:DataPoint with @id 'vulnerability_management_product_vulnerabilities' MUST have exactly one sr:AggregateValue</sch:assert>
            <sch:assert test="count(sr:AggregateValue[@name eq 'number_of_affected_systems' and @type eq 'COUNT']) eq 1">The first level sr:GroupedData in sr:DataPoint with @id 'vulnerability_management_product_vulnerabilities' MUST have sr:AggregateValue with @name = 'number_of_affected_systems' and @type = 'COUNT'</sch:assert>
        </sch:rule>
        <sch:rule context="sr:DataPoint[@id eq 'inventory_management_product_inventory']/sr:GroupedData">
            <sch:assert test="exists(sr:AssetAttribute/ai:Software)">The first level sr:GroupedData in sr:DataPoint with @id 'inventory_management_product_inventory' MUST have a sr:AssetAttribute with a ai:Software</sch:assert>
            <sch:assert test="exists(sr:AssetAttribute[@name eq 'operating_system'])">The first level sr:GroupedData in sr:DataPoint with @id 'inventory_management_product_inventory' MUST have a sr:AssetAttribute with @name = 'operating_system'</sch:assert>
            <sch:assert test="count(sr:GroupedData) eq 0">The first level sr:GroupedData in sr:DataPoint with @id 'inventory_management_product_inventory' MUST have no nested sr:GroupedData</sch:assert>
            <sch:assert test="count(sr:AggregateValue) eq 1">The first level sr:GroupedData in sr:DataPoint with @id 'inventory_management_product_inventory' MUST have exactly one sr:AggregateValue</sch:assert>
            <sch:assert test="exists(sr:AggregateValue[@name eq 'number_of_systems' and @type eq 'COUNT'])">The first level sr:GroupedData in sr:DataPoint with @id 'inventory_management_product_inventory' MUST have a sr:AggregateValue with @name = 'number_of_systems' and @type = 'COUNT'</sch:assert>
        </sch:rule>
        
    </sch:pattern>
</sch:schema>
