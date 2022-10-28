<?xml version="1.0" encoding="UTF-8"?>
<!--These rules are for informational purposes only and DO NOT supersede the requirements in NIST SP 800-126 Rev 3.-->
<!--These rules may be revised at anytime. Comments/feedback on these rules are welcome.-->
<!--Private comments may be sent to scap@nist.gov.  Public comments may be sent to scap-dev@nist.gov.-->
<sch:schema xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2">
  <sch:ns prefix="ai" uri="http://scap.nist.gov/schema/asset-identification/1.1"/>
  <sch:ns prefix="arf" uri="http://scap.nist.gov/schema/asset-reporting-format/1.1"/>
  <sch:ns prefix="cat" uri="urn:oasis:names:tc:entity:xmlns:xml:catalog"/>
  <sch:ns prefix="cpe-dict" uri="http://cpe.mitre.org/dictionary/2.0"/>
  <sch:ns prefix="cpe-lang" uri="http://cpe.mitre.org/language/2.0"/>
  <sch:ns prefix="dc" uri="http://purl.org/dc/elements/1.1/"/>
  <sch:ns prefix="ds" uri="http://scap.nist.gov/schema/scap/source/1.2"/>
  <sch:ns prefix="dsig" uri="http://www.w3.org/2000/09/xmldsig#"/>
  <sch:ns prefix="java" uri="java:gov.nist.secauto.scap.validation.schematron"/>
  <sch:ns prefix="nvd-config" uri="http://scap.nist.gov/schema/feed/configuration/0.1"/>
  <sch:ns prefix="ocil" uri="http://scap.nist.gov/schema/ocil/2.0"/>
  <sch:ns prefix="oval-com" uri="http://oval.mitre.org/XMLSchema/oval-common-5"/>
  <sch:ns prefix="oval-def" uri="http://oval.mitre.org/XMLSchema/oval-definitions-5"/>
  <sch:ns prefix="oval-res" uri="http://oval.mitre.org/XMLSchema/oval-results-5"/>
  <sch:ns prefix="oval-sc" uri="http://oval.mitre.org/XMLSchema/oval-system-characteristics-5"/>
  <sch:ns prefix="rc" uri="http://scap.nist.gov/schema/reporting-core/1.1"/>
  <sch:ns prefix="scap" uri="http://scap.nist.gov/schema/scap/source/1.2"/>
  <sch:ns prefix="scap-con" uri="http://scap.nist.gov/schema/scap/constructs/1.2"/>
  <sch:ns prefix="tmsad" uri="http://scap.nist.gov/schema/xml-dsig/1.0"/>
  <sch:ns prefix="xccdf" uri="http://checklists.nist.gov/xccdf/1.2"/>
  <sch:ns prefix="xcf" uri="nist:scap:xslt:function"/>
  <sch:ns prefix="xinclude" uri="http://www.w3.org/2001/XInclude"/>
  <sch:ns prefix="xlink" uri="http://www.w3.org/1999/xlink"/>
  <sch:ns prefix="xml" uri="http://www.w3.org/XML/1998/namespace"/>
  <sch:ns prefix="xsd" uri="http://www.w3.org/2001/XMLSchema"/>
  <sch:pattern id="scap-result-general">
    <sch:rule id="scap-result-general-xccdf-rule-result" context="xccdf:rule-result">
      <sch:assert id="scap-result-general-xccdf-rule-ident" test="if (current()/ancestor::arf:asset-report-collection//xccdf:Rule[@id eq current()/@idref]/xccdf:ident/@system = 'http://cce.mitre.org' or current()/ancestor::arf:asset-report-collection//xccdf:Rule[@id eq current()/@idref]/xccdf:ident/@system = 'http://cpe.mitre.org' or current()/ancestor::arf:asset-report-collection//xccdf:Rule[@id eq current()/@idref]/xccdf:ident/@system = 'http://cve.mitre.org') then deep-equal(current()/xccdf:ident, current()/ancestor::arf:asset-report-collection//xccdf:Rule[@id eq current()/@idref]/xccdf:ident) else true()">RES-44-1</sch:assert>
      <sch:assert id="scap-result-xccdf-rule-result-result-val" test="( ( (every $m in current()//xccdf:check satisfies if(count(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m//xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m//xccdf:check-content-ref[1]/@name]) gt 1) then true() else if(not(exists(current()/xccdf:check/xccdf:check-content-ref/@name))) then true() else if(current()/xccdf:result eq 'error') then ./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m//xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m//xccdf:check-content-ref[1]/@name]/@result eq 'error' else if( current()/xccdf:result eq 'unknown' ) then ./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m//xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m//xccdf:check-content-ref[1]/@name]/@result eq 'unknown' else if(current()/@role eq 'unchecked') then current()/xccdf:result eq 'notchecked' else if( current()/xccdf:result eq 'notchecked' ) then ./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m//xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m//xccdf:check-content-ref[1]/@name]/@result eq 'not evaluated' else if((current()/xccdf:result eq 'fail' and (current()/xccdf:check[@negate eq 'false'] or not(exists(current()/xccdf:check/@negate)))) or ((current()/xccdf:result eq 'pass' or current()/xccdf:result eq 'fixed' )and (current()/xccdf:check[@negate eq 'true']))) then ( ( ./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m//xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m//xccdf:check-content-ref[1]/@name]/@result eq 'false' and ( matches(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@class,'^(compliance|inventory)$') or matches(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-def:definition[@id eq $m/xccdf:check-content-ref[1]/@name]/@class,'^(compliance|inventory)$') or matches(./ancestor::arf:asset-report-collection[1]//arf:report-request//oval-def:oval_definitions//oval-def:definition[@id eq $m//xccdf:check-content-ref[1]/@name][1]/@class,'^(compliance|inventory)$') ) )  or ( ./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m//xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m//xccdf:check-content-ref[1]/@name]/@result eq 'true' and ( matches(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@class,'^(vulnerability|patch)$') or matches(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-def:definition[@id eq $m/xccdf:check-content-ref[1]/@name]/@class[1],'^(vulnerability|patch)$') or matches(./ancestor::arf:asset-report-collection[1]//arf:report-request//oval-def:oval_definitions//oval-def:definition[@id eq $m//xccdf:check-content-ref[1]/@name][1]/@class,'^(vulnerability|patch)$') ) ) ) else if(((current()/xccdf:result eq 'pass' or current()/xccdf:result eq 'fixed' )and (current()/xccdf:check[@negate eq 'false'] or not(exists(current()/xccdf:check/@negate)))) or (current()/xccdf:result eq 'fail' and (current()/xccdf:check[@negate eq 'true']))) then ( ( ( ./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m//xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m//xccdf:check-content-ref[1]/@name]/@result eq 'true' ) and ( matches(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@class,'^(compliance|inventory)$') or matches(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-def:definition[@id eq $m/xccdf:check-content-ref[1]/@name]/@class,'^(compliance|inventory)$') or matches(./ancestor::arf:asset-report-collection[1]//arf:report-request//oval-def:oval_definitions//oval-def:definition[@id eq $m//xccdf:check-content-ref[1]/@name][1]/@class,'^(compliance|inventory)$') ) ) or ( ./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m//xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m//xccdf:check-content-ref[1]/@name]/@result eq 'false' and ( matches(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@class,'^(vulnerability|patch)$') or matches(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-def:definition[@id eq $m/xccdf:check-content-ref[1]/@name]/@class,'^(vulnerability|patch)$') or matches(./ancestor::arf:asset-report-collection[1]//arf:report-request//oval-def:oval_definitions//oval-def:definition[@id eq $m//xccdf:check-content-ref[1]/@name][1]/@class,'^(vulnerability|patch)$') ) ) ) else if( current()/xccdf:result eq 'notselected' or current()/xccdf:result eq 'informational' or current()/xccdf:result eq 'notapplicable') then true() else false() ) and ( if(some $m in xccdf:complex-check/xccdf:check satisfies count(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m//xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m//xccdf:check-content-ref[1]/@name]) gt 1 ) then true() else if( exists(current()/xccdf:complex-check[@operator eq 'AND'])) then ( if( current()/xccdf:result eq 'error' ) then some $m in xccdf:complex-check/xccdf:check satisfies ( ./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m//xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m//xccdf:check-content-ref[1]/@name]/@result eq 'error' ) else if( current()/xccdf:result eq 'unknown' ) then some $m in xccdf:complex-check/xccdf:check satisfies (./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m//xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m//xccdf:check-content-ref[1]/@name]/@result eq 'unknown') else if(current()/@role eq 'unchecked') then current()/xccdf:result eq 'notchecked' else if(current()/xccdf:result eq 'notchecked') then some $m in xccdf:complex-check/xccdf:check satisfies (./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m//xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m//xccdf:check-content-ref[1]/@name]/@result eq 'not evaluated') else if((current()/xccdf:result eq 'fail' and (current()/xccdf:complex-check[@negate eq 'false'] or not(exists(current()/xccdf:complex-check/@negate)))) or ((current()/xccdf:result eq 'pass' or current()/xccdf:result eq 'fixed') and (current()/xccdf:complex-check[@negate eq 'true']))) then( (some $m in xccdf:complex-check/xccdf:check satisfies ( ((./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@result eq 'false') or ($m[@negate eq 'true'] and ./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@result eq 'true') ) and ( matches(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@class,'^(compliance|inventory)$') or matches(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-def:definition[@id eq $m/xccdf:check-content-ref[1]/@name]/@class,'^(compliance|inventory)$') or matches(./ancestor::arf:asset-report-collection[1]//arf:report-request//oval-def:oval_definitions//oval-def:definition[@id eq $m//xccdf:check-content-ref[1]/@name][1]/@class,'^(compliance|inventory)$') ) ) or ( ((./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@result eq 'true') or ($m[@negate eq 'true'] and ./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@result eq 'false') ) and ( matches(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@class,'^(vulnerability|patch)$') or matches(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-def:definition[@id eq $m/xccdf:check-content-ref[1]/@name]/@class,'^(vulnerability|patch)$') or matches(./ancestor::arf:asset-report-collection[1]//arf:report-request//oval-def:oval_definitions//oval-def:definition[@id eq $m//xccdf:check-content-ref[1]/@name][1]/@class,'^(vulnerability|patch)$') ) ) ) ) else if( ((current()/xccdf:result eq 'pass' or current()/xccdf:result eq 'fixed') and (current()/xccdf:complex-check[@negate eq 'false'] or not(exists(current()/xccdf:complex-check/@negate)))) or ((current()/xccdf:result eq 'pass' or current()/xccdf:result eq 'fixed')and (current()/xccdf:complex-check[@negate eq 'true']))) then( (every $m in xccdf:complex-check/xccdf:check satisfies ( ( ( ./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@result eq 'true') or ($m[@negate eq 'true'] and ./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@result eq 'false') ) and ( matches(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@class,'^(compliance|inventory)$') or matches(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-def:definition[@id eq $m/xccdf:check-content-ref[1]/@name]/@class,'^(compliance|inventory)$') or matches(./ancestor::arf:asset-report-collection[1]//arf:report-request//oval-def:oval_definitions//oval-def:definition[@id eq $m//xccdf:check-content-ref[1]/@name][1]/@class,'^(compliance|inventory)$') ) ) or ( ((./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@result eq 'false') or ($m[@negate eq 'true'] and ./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@result eq 'true') ) and ( matches(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@class,'^(vulnerability|patch)$') or matches(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-def:definition[@id eq $m/xccdf:check-content-ref[1]/@name]/@class,'^(vulnerability|patch)$') or matches(./ancestor::arf:asset-report-collection[1]//arf:report-request//oval-def:oval_definitions//oval-def:definition[@id eq $m//xccdf:check-content-ref[1]/@name][1]/@class,'^(vulnerability|patch)$') ) ) ) ) else if( current()/xccdf:result eq 'notselected' or current()/xccdf:result eq 'informational' or current()/xccdf:result eq 'notapplicable') then (true()) else false() ) else true() ) and ( if(some $m in xccdf:complex-check/xccdf:check satisfies count(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m//xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m//xccdf:check-content-ref[1]/@name]) gt 1 ) then true() else if( exists(current()/xccdf:complex-check[@operator eq 'OR'])) then ( if( current()/xccdf:result eq 'error' ) then some $m in xccdf:complex-check/xccdf:check satisfies (./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m//xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m//xccdf:check-content-ref[1]/@name]/@result eq 'error') else if( current()/xccdf:result eq 'unknown' ) then some $m in xccdf:complex-check/xccdf:check satisfies (./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m//xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m//xccdf:check-content-ref[1]/@name]/@result eq 'unknown') else if(current()/@role eq 'unchecked') then current()/xccdf:result eq 'notchecked' else if(current()/xccdf:result eq 'notchecked') then some $m in xccdf:complex-check/xccdf:check satisfies (./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m//xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m//xccdf:check-content-ref[1]/@name]/@result eq 'not evaluated') else if((current()/xccdf:result eq 'fail' and (current()/xccdf:complex-check[@negate eq 'false'] or not(exists(current()/xccdf:complex-check/@negate)))) or ((current()/xccdf:result eq 'pass' or current()/xccdf:result eq 'fixed')and (current()/xccdf:complex-check[@negate eq 'true']))) then( (every $m in xccdf:complex-check/xccdf:check satisfies ( ((./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@result eq 'false') or($m[@negate eq 'true'] and ./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@result eq 'true') ) and ( matches(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@class,'^(compliance|inventory)$') or matches(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-def:definition[@id eq $m/xccdf:check-content-ref[1]/@name]/@class,'^(compliance|inventory)$') or matches(./ancestor::arf:asset-report-collection[1]//arf:report-request//oval-def:oval_definitions//oval-def:definition[@id eq $m//xccdf:check-content-ref[1]/@name][1]/@class,'^(compliance|inventory)$') ) ) or ( ((./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@result eq 'true') or($m[@negate eq 'true'] and ./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@result eq 'false') ) and ( matches(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@class,'^(vulnerability|patch)$') or matches(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-def:definition[@id eq $m/xccdf:check-content-ref[1]/@name]/@class,'^(vulnerability|patch)$') or matches(./ancestor::arf:asset-report-collection[1]//arf:report-request//oval-def:oval_definitions//oval-def:definition[@id eq $m//xccdf:check-content-ref[1]/@name][1]/@class,'^(vulnerability|patch)$') ) ) ) ) else if(((current()/xccdf:result eq 'pass' or current()/xccdf:result eq 'fixed') and (current()/xccdf:complex-check[@negate eq 'false'] or not(exists(current()/xccdf:complex-check/@negate)))) or ((current()/xccdf:result eq 'pass' or current()/xccdf:result eq 'fixed') and (current()/xccdf:complex-check[@negate eq 'true']))) then( (some $m in xccdf:complex-check/xccdf:check satisfies ( ((./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@result eq 'true') or($m[@negate eq 'true'] and ./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@result eq 'false')) and ( matches(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@class,'^(compliance|inventory)$') or matches(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-def:definition[@id eq $m/xccdf:check-content-ref[1]/@name]/@class,'^(compliance|inventory)$') or matches(./ancestor::arf:asset-report-collection[1]//arf:report-request//oval-def:oval_definitions//oval-def:definition[@id eq $m//xccdf:check-content-ref[1]/@name][1]/@class,'^(compliance|inventory)$') ) ) or ( ((./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@result eq 'false') or($m[@negate eq 'true'] and ./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@result eq 'true') ) and ( matches(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@class,'^(vulnerability|patch)$') or matches(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-def:definition[@id eq $m/xccdf:check-content-ref[1]/@name]/@class,'^(vulnerability|patch)$') or matches(./ancestor::arf:asset-report-collection[1]//arf:report-request//oval-def:oval_definitions//oval-def:definition[@id eq $m//xccdf:check-content-ref[1]/@name][1]/@class,'^(vulnerability|patch)$') ) ) ) ) else if( current()/xccdf:result eq 'notselected' or current()/xccdf:result eq 'informational' or current()/xccdf:result eq 'notapplicable') then (true()) else false() ) else true() ) ) or ( if (current()/xccdf:check/@system != 'http://oval.mitre.org/XMLSchema/oval-definitions-5') then true() else false() ) )">RES-126-1</sch:assert>
      <sch:assert id="scap-result-xccdf-rule-result-locate-oval-class-val" test="( (every $m in current()//xccdf:check satisfies if(not( exists(current()/xccdf:check/xccdf:check-content-ref/@name))) then true() else exists(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@class) or exists(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-def:definition[@id eq $m/xccdf:check-content-ref[1]/@name]/@class) or exists(./ancestor::arf:asset-report-collection[1]//arf:report-request//oval-def:oval_definitions//oval-def:definition[@id eq $m//xccdf:check-content-ref[1]/@name][1]/@class)) and (every $m in xccdf:complex-check/xccdf:check satisfies exists(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m/xccdf:check-content-ref[1]/@name]/@class) or exists(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/xccdf:check-content-ref[1]/@href]//oval-def:definition[@id eq $m/xccdf:check-content-ref[1]/@name]/@class) or exists(./ancestor::arf:asset-report-collection[1]//arf:report-request//oval-def:oval_definitions//oval-def:definition[@id eq $m//xccdf:check-content-ref[1]/@name][1]/@class)) ) or ( if (current()/xccdf:check/@system != 'http://oval.mitre.org/XMLSchema/oval-definitions-5') then true() else false() )">RES-126-2</sch:assert>
      <sch:assert id="scap-result-xccdf-rule-result-check-notapplicable" test="( (every $m in current()//xccdf:check satisfies if(count(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m//xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m//xccdf:check-content-ref[1]/@name]) gt 1) then true() else if(current()/xccdf:result eq 'notapplicable') then ./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m//xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m//xccdf:check-content-ref[1]/@name]/@result eq 'not applicable' or ./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m//xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m//xccdf:check-content-ref[1]/@name]/@result eq 'not evaluated' else true() ) and (every $m in xccdf:complex-check/xccdf:check satisfies if(count(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m//xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m//xccdf:check-content-ref[1]/@name]) gt 1) then true() else if(current()/xccdf:result eq 'notapplicable') then ./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m//xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m//xccdf:check-content-ref[1]/@name]/@result eq 'not applicable' or ./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m//xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m//xccdf:check-content-ref[1]/@name]/@result eq 'not evaluated' else true() ) )">RES-126-3</sch:assert>
      <sch:assert id="scap-result-xccdf-rule-result-multiple-oval-def" test="( ( every $m in current()//xccdf:check satisfies if(count(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m//xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m//xccdf:check-content-ref[1]/@name]) gt 1) then false() else true() ) and ( every $m in xccdf:complex-check/xccdf:check satisfies if(count(./ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m//xccdf:check-content-ref[1]/@href]//oval-res:definition[@definition_id eq $m//xccdf:check-content-ref[1]/@name]) gt 1) then false() else true() ) or ( if (current()/xccdf:check/@system != 'http://oval.mitre.org/XMLSchema/oval-definitions-5') then true() else false() ) )">RES-126-4</sch:assert>
      <sch:assert id="scap-result-xccdf-rule-result-with-no-name" test=" every $m in current()//xccdf:check satisfies ( if(not(exists($m/xccdf:check-content-ref/@name))) then count($m/ancestor::arf:asset-report-collection[1]/arf:report-requests//xccdf:Rule[@id eq $m/ancestor::node()/@idref]) eq 1 and not(exists($m/ancestor::arf:asset-report-collection[1]/arf:report-requests//xccdf:Rule[@id eq $m/ancestor::node()/@idref]/xccdf:check/xccdf:check-content-ref/@name)) and not(exists($m/ancestor::arf:asset-report-collection[1]/arf:report-requests//xccdf:Rule[@id eq $m/ancestor::node()/@idref]/xccdf:check[@multi-check eq 'true'])) else true() )">RES-126-5</sch:assert>
      <sch:assert id="scap-result-general-xccdf-rule-result-check-content-ref-exists" test="if( xccdf:result ne 'notapplicable' and xccdf:result ne 'notchecked' and xccdf:result ne 'notselected') then (exists(current()//xccdf:check) and (every $m in current()//xccdf:check satisfies if(not(exists($m/xccdf:check-content-ref/@name))) then exists($m/xccdf:check-content-ref/@href) and count($m/ancestor::arf:asset-report-collection[1]/arf:report-requests//xccdf:Rule[@id eq $m/ancestor::node()/@idref]) eq 1 and not(exists($m/ancestor::arf:asset-report-collection[1]/arf:report-requests//xccdf:Rule[@id eq $m/ancestor::node()/@idref]/xccdf:check/xccdf:check-content-ref/@name)) and not(exists($m/ancestor::arf:asset-report-collection[1]/arf:report-requests//xccdf:Rule[@id eq $m/ancestor::node()/@idref]/xccdf:check[@multi-check eq 'true'])) else exists(.//xccdf:check/xccdf:check-content-ref[exists(@href) and exists(@name)]))) else true()">RES-260-1</sch:assert>
      <sch:assert id="scap-result-general-xccdf-rule-result-check-ref-hash" test="(every $m in .//xccdf:check-content-ref satisfies current()/ancestor::arf:asset-report-collection[1]//arf:report[concat('#',@id) eq $m/@href]) and (every $m in .//xccdf:check-content-ref satisfies parent::*/parent::*/parent::*[concat('#',@id) ne $m/@href])">RES-370-1</sch:assert>
      <sch:assert id="scap-result-general-xccdf-rule-result-check-ref-valid" test="every $m in .//xccdf:check-content-ref satisfies if (contains($m/parent::xccdf:check/@system,'oval')) then current()/ancestor::arf:asset-report-collection[1]//arf:report[@id eq translate($m/@href,'#','')]/arf:content/*[local-name() eq 'oval_results'] else if (contains($m/parent::xccdf:check/@system,'ocil')) then current()/ancestor::arf:asset-report-collection[1]//arf:report[@id eq translate($m/@href,'#','')]/arf:content/*[local-name() eq 'ocil'] else false()">RES-370-2</sch:assert>
      <sch:assert id="scap-result-general-xccdf-rule-selected-unchecked" test="if(current()/ancestor::arf:asset-report-collection//arf:report-requests//xccdf:Rule[@id eq current()/@idref and (@selected eq 'true' or not(@selected)) and @role eq 'unchecked']) then current()/xccdf:result eq 'notchecked' else true()">RES-391-1</sch:assert>
      <sch:assert id="scap-result-general-xccdf-rule-selected-unscored" test="if(current()/ancestor::arf:asset-report-collection//arf:report-requests//xccdf:Rule[@id eq current()/@idref and (@selected eq 'true' or not(@selected)) and @role eq 'unscored']) then current()/xccdf:result eq 'informational' else true()">RES-391-2</sch:assert>
      <sch:assert id="scap-result-general-xccdf-rule-result-attrs-present" test="string-length(current()/@role) > 0 and string-length(current()/@severity) > 0 and string-length(current()/@weight) > 0">RES-392-1</sch:assert>
      <sch:assert id="scap-result-optional-attributes-xccdf-rule-result-set" test="exists(@role) and exists(@severity) and exists(@weight)">A-26-1</sch:assert>
    </sch:rule>
    <sch:rule id="scap-result-xccdf-rule" context="arf:asset-report-collection//arf:report-requests//xccdf:Rule">
      <!-- This requirement results is a warning to the end user for now -->
      <sch:assert id="scap-result-xccdf-rule-multi-check" test=" if (current()/xccdf:check[1]/@multi-check eq 'true') then false() else true()">RES-258-1</sch:assert>
      <sch:assert id="scap-result-optional-attributes-xccdf-rule-exists" test="exists(@selected) and exists(@weight) and exists(@role) and exists(@severity)">A-26-1</sch:assert>
    </sch:rule>
    <sch:rule id="scap-result-general-report" context="arf:report">
      <sch:assert id="scap-result-general-report-xccdf-use-test-result" test="if( namespace-uri(arf:content/*[1]) eq 'http://checklists.nist.gov/xccdf/1.2' ) then exists(arf:content/xccdf:TestResult) else true()">RES-131-1|arf:report <sch:value-of select="@id"/></sch:assert>
      <!-- This requirement results is a warning to the end user for now -->
      <sch:assert id="scap-result-must-have-report-element" test="false()">RES-366-1</sch:assert>
    </sch:rule>
    <sch:rule id="scap-result-general-xccdf-test-result" context="xccdf:TestResult">
      <sch:assert id="scap-result-general-xccdf-test-result-start-end-time" test="exists(@start-time) and exists(@end-time)">RES-133-1</sch:assert>
      <sch:assert id="scap-result-general-xccdf-test-result-test-system" test="exists(@test-system) and (matches(@test-system,'^[c][pP][eE]:/[AHOaho]?(:[A-Za-z0-9\._\-~%]*){0,6}$') or matches(@test-system,'^cpe:2\.3:[aho\*-](:(((\?*|\*?)([a-zA-Z0-9\-\._]|(\\[\\\*\?!&quot;#$$%&amp;\(\)\+,/:;&lt;=&gt;@\[\]\^`\{{\|}}~]))+(\?*|\*?))|[\*-])){5}(:(([a-zA-Z]{2,3}(-([a-zA-Z]{2}|[0-9]{3}))?)|[\*-]))(:(((\?*|\*?)([a-zA-Z0-9\-\._]|(\\[\\\*\?!&quot;#$$%&amp;\(\)\+,/:;&lt;=&gt;@\[\]\^`\{{\|}}~]))+(\?*|\*?))|[\*-])){4}$'))">RES-134-1</sch:assert>
      <sch:assert id="scap-result-general-xccdf-test-result-target-address" test="count(xccdf:target) &gt; 0 and count(xccdf:target-address) &gt; 0">RES-136-1</sch:assert>
      <sch:assert id="scap-result-general-xccdf-test-result-target-address-content" test="every $m in xccdf:target-address satisfies matches($m, '([0-9]|[1-9][0-9]|1([0-9][0-9])|2([0-4][0-9]|5[0-5]))\.([0-9]|[1-9][0-9]|1([0-9][0-9])|2([0-4][0-9]|5[0-5]))\.([0-9]|[1-9][0-9]|1([0-9][0-9])|2([0-4][0-9]|5[0-5]))\.([0-9]|[1-9][0-9]|1([0-9][0-9])|2([0-4][0-9]|5[0-5]))') or matches($m, '([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}')">RES-136-2</sch:assert>
      <sch:assert id="scap-result-general-xccdf-test-result-benchmark-id-req" test="string-length(current()/xccdf:benchmark/@id) > 0">RES-253-1</sch:assert>
      <sch:assert id="scap-result-general-xccdf-test-result-benchmark-href-req" test="if (starts-with(current()/xccdf:benchmark/@href,'#')) then exists(current()/ancestor::node()//ds:component[@id = substring(current()/xccdf:benchmark/@href, 2)]) else if (contains(current()/xccdf:benchmark/@href,'://')) then true() else false() ">RES-253-2</sch:assert>
      <sch:assert id="scap-result-general-xccdf-test-result-target-id-ref" test="exists(.//xccdf:target-id-ref[@system eq 'http://scap.nist.gov/schema/asset-identification/1.1' and @href eq '' and @name eq current()/ancestor::arf:asset-report-collection[1]/rc:relationships/rc:relationship[@subject eq current()/ancestor::arf:report[1]/@id and (resolve-QName(@type,.) eq QName('http://scap.nist.gov/vocabulary/arf/relationships/1.0#','isAbout') or resolve-QName(@type,.) eq QName('http://scap.nist.gov/specifications/arf/vocabulary/relationships/1.0#','isAbout'))]/rc:ref[1]])">RES-304-1</sch:assert>
      <sch:assert id="scap-result-general-xccdf-test-result-identity-provided" test="exists(xccdf:identity) and (every $m in xccdf:identity satisfies matches($m,'\S'))">RES-42-1</sch:assert>
    </sch:rule>
    <sch:rule id="scap-result-general-oval-results" context="oval-res:oval_results">
      <sch:assert id="scap-result-oval-results-result-type" test="(count(oval-res:directives/*) eq 6) and (every $m in oval-res:directives/* satisfies(($m/@reported eq 'true') and (if(exists($m/following-sibling::*)) then (if ($m/@content eq 'thin') then ($m/following-sibling::*[1]/@content eq 'thin' or not(exists($m/following-sibling::*[1]/@content))) else if ($m/@content eq 'full' or not(exists($m/@content))) then ($m/following-sibling::*[1]/@content eq 'full' or not(exists($m/following-sibling::*[1]/@content))) else false()) else true())))">RES-179-1</sch:assert>
       <sch:assert id="scap-result-oval-results-sys-with-or-wo-char" test="if( oval-res:directives/*[1]/@content eq 'full' or not(exists(oval-res:directives/*[1]/@content))) then ((exists(.//oval-sc:oval_system_characteristics/oval-sc:collected_objects) and exists(.//oval-sc:oval_system_characteristics/oval-sc:system_data)) or ((not(exists(.//oval-sc:oval_system_characteristics/oval-sc:collected_objects)) and not(exists(.//oval-sc:oval_system_characteristics/oval-sc:system_data))))) else true()">RES-181-1</sch:assert>
    </sch:rule>
    <sch:rule id="scap-result-general-ai-asset" context="arf:asset">
      <sch:assert id="scap-result-general-ai-asset-certain-fields-req" test="(exists(.//ai:ip-v4) or exists(.//ai:ip-v6)) and exists(.//ai:mac-address) and (.//ai:fqdn) and (.//ai:hostname)">RES-299-1|arf:asset <sch:value-of select="@id"/></sch:assert>
    </sch:rule>
    <sch:rule id="scap-result-general-report-collection" context="arf:asset-report-collection">
      <sch:assert id="scap-result-general-report-collection-include-report-request" test="exists(.//arf:report-request)">RES-300-1|arf:asset-report-collection <sch:value-of select="@id"/></sch:assert>
      <sch:assert id="scap-result-general-report-collection-report-request-not-included" test="exists(.//arf:report-request)">RES-300-3|arf:asset-report-collection <sch:value-of select="@id"/></sch:assert>
      <sch:assert id="scap-result-general-signature-sig-counter-sign-remove-orig" test="every $m in .//arf:extended-info/dsig:Signature satisfies count($m/ancestor::arf:asset-report-collection[1]//dsig:Signature[@Id eq $m/@Id]) eq 1">RES-316-1|arf:asset-report-collection <sch:value-of select="@id"/></sch:assert>
      <sch:assert id="scap-result-general-report-collection-sig-report-request" test="if( exists(.//arf:extended-info/dsig:Signature) ) then exists(.//arf:report-request) else true() ">RES-323-1|arf:asset-report-collection <sch:value-of select="@id"/></sch:assert>
    </sch:rule>
    <sch:rule id="scap-result-general-oval-sys" context="oval-sc:oval_system_characteristics">
      <sch:assert id="scap-result-general-oval-sys-system-info" test="if (exists(current()/oval-sc:system_info/scap-con:asset-identification/arf:object-ref)) then( some $m in current()/ancestor::arf:asset-report-collection[1]/rc:relationships/rc:relationship[@subject eq current()/ancestor::arf:report[1]/@id] satisfies (exists( $m[resolve-QName(@type,.) eq QName('http://scap.nist.gov/vocabulary/arf/relationships/1.0#','isAbout') or resolve-QName(@type,.) eq QName('http://scap.nist.gov/specifications/arf/vocabulary/relationships/1.0#','isAbout')]) and current()/oval-sc:system_info/scap-con:asset-identification/arf:object-ref/@ref-id eq $m/rc:ref[1])) else false()">RES-306-1</sch:assert>
    </sch:rule>
    <sch:rule id="scap-result-general-signature-sig" context="arf:extended-info/dsig:Signature">
      <sch:assert id="scap-result-general-signature-sig-sig-properties" test="exists(.//tmsad:signature-info)">RES-311-1|dsig:Signature <sch:value-of select="@Id"/></sch:assert>
      <sch:assert id="scap-result-general-signature-sig-first-ref" test="some $m in .//dsig:Reference[1] satisfies ($m/@URI eq '' or exists(current()//dsig:Object[concat('#',@Id) eq $m/@URI]))">RES-312-1|dsig:Signature <sch:value-of select="@Id"/></sch:assert>
      <sch:assert id="scap-result-general-signature-sig-xpath-filter" test="count(.//dsig:Reference[1]//dsig:Transform[@Algorithm eq 'http://www.w3.org/2002/06/xmldsig-filter2']) eq 2">RES-313-1|dsig:Signature <sch:value-of select="@Id"/></sch:assert>
      <sch:assert id="scap-result-general-signature-sig-sig-property" test="exists(.//dsig:SignatureProperties[concat('#',@Id) eq current()/dsig:SignedInfo/dsig:Reference[2]/@URI])">RES-314-1|dsig:Signature <sch:value-of select="@Id"/></sch:assert>
      <sch:assert id="scap-result-general-signature-sig-key-info" test="exists(dsig:KeyInfo)">RES-315-1|dsig:Signature <sch:value-of select="@Id"/></sch:assert>
    </sch:rule>
  </sch:pattern>
  <xsl:function xmlns:xcf="nist:scap:xslt:function" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" name="xcf:get-all-profile-parents">
    <xsl:param name="doc"/>
    <xsl:param name="node"/>
    <xsl:sequence select="$node"/>
    <xsl:if test="exists($node/@extends)">
      <xsl:sequence select="xcf:get-all-profile-parents($doc,$doc//xccdf:Benchmark//xccdf:Profile[@id eq $node/@extends])"/>
    </xsl:if>
  </xsl:function>
  <xsl:function xmlns:xcf="nist:scap:xslt:function" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" name="xcf:get-component">
    <xsl:param name="component-ref"/>
    <xsl:sequence select="$component-ref/ancestor::ds:data-stream-collection//ds:component[@id eq substring($component-ref/@xlink:href,2)]"/>
  </xsl:function>
  <xsl:function xmlns:xcf="nist:scap:xslt:function" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.w3.org/1999/XSL/Transform" name="xcf:get-all-def-children">
    <xsl:param name="doc"/>
    <xsl:param name="node"/>
    <xsl:sequence select="$node"/>
    <xsl:for-each select="$doc//oval-def:extend_definition[@definition_ref eq $node/@id]/ancestor::oval-def:definition">
      <xsl:sequence select="xcf:get-all-def-children($doc,current())"/>
    </xsl:for-each>
  </xsl:function>
  <xsl:function xmlns:xcf="nist:scap:xslt:function" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" name="xcf:ident-mapping">
    <xsl:param name="in-string"/>
    <xsl:choose>
      <xsl:when test="$in-string eq 'http://cce.mitre.org'">
        <xsl:value-of select="string('^(CCE|http://cce.mitre.org)$')"/>
      </xsl:when>
      <xsl:when test="$in-string eq 'http://cve.mitre.org'">
        <xsl:value-of select="string('^(CVE|http://cve.mitre.org)$')"/>
      </xsl:when>
      <xsl:when test="$in-string eq 'http://cpe.mitre.org'">
        <xsl:value-of select="string('^(CPE|http://cpe.mitre.org)$')"/>
      </xsl:when>
    </xsl:choose>
  </xsl:function>
  <xsl:function xmlns:xcf="nist:scap:xslt:function" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" name="xcf:get-all-parents">
    <xsl:param name="doc"/>
    <xsl:param name="node"/>
    <xsl:sequence select="$node"/>
    <xsl:for-each select="$node//oval-def:extend_definition">
      <xsl:sequence select="xcf:get-all-parents($doc,ancestor::oval-def:oval_definitions//*[@id eq current()/@definition_ref])"/>
    </xsl:for-each>
  </xsl:function>
  <xsl:function xmlns:xcf="nist:scap:xslt:function" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" name="xcf:get-ocil-var-ref">
    <xsl:param name="ocil_questionnaire"/>
    <xsl:variable name="initialSet">
      <xsl:for-each select="$ocil_questionnaire/ocil:actions/ocil:test_action_ref">
        <xsl:sequence select="ancestor::ocil:ocil/ocil:test_actions/ocil:numeric_question_test_action[@id eq current()]/ocil:when_equals[@var_ref]"/>
        <xsl:sequence select="ancestor::ocil:ocil/ocil:test_actions/ocil:string_question_test_action[@id eq current()]/ocil:when_pattern/ocil:pattern[@var_ref]"/>
        <xsl:sequence select="ancestor::ocil:ocil/ocil:test_actions/ocil:numeric_question_test_action[@id eq current()]/ocil:when_range/ocil:range/ocil:min[@var_ref]"/>
        <xsl:sequence select="ancestor::ocil:ocil/ocil:test_actions/ocil:numeric_question_test_action[@id eq current()]/ocil:when_range/ocil:range/ocil:max[@var_ref]"/>
        <xsl:for-each select="ancestor::ocil:ocil/ocil:test_actions/*[@id eq current()]">
          <xsl:sequence select="ancestor::ocil:ocil/ocil:questions/ocil:choice_question[@id eq current()/@question_ref]/ocil:choice[@var_ref]"/>
          <xsl:for-each select="ancestor::ocil:ocil/ocil:questions/ocil:choice_question[@id eq current()/@question_ref]/ocil:choice_group_ref">
            <xsl:sequence select="ancestor::ocil:questions/ocil:choice_group[@id eq current()]/ocil:choice[@var_ref]"/>
          </xsl:for-each>
        </xsl:for-each>
      </xsl:for-each>
    </xsl:variable>
    <xsl:for-each select="$initialSet/*">
      <xsl:sequence select="xcf:pass-ocil-var-ref($ocil_questionnaire,current())"/>
    </xsl:for-each>
  </xsl:function>
  <xsl:function xmlns:xcf="nist:scap:xslt:function" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" name="xcf:get-component-ref">
    <xsl:param name="catalog"/>
    <xsl:param name="uri"/>
    <xsl:variable name="component-ref-uri" select="xcf:resolve-in-catalog($catalog/*[1],$uri)"/>
    <xsl:if test="$component-ref-uri ne ''">
      <xsl:sequence select="$catalog/ancestor::ds:data-stream//ds:component-ref[@id eq substring($component-ref-uri,2)]"/>
    </xsl:if>
  </xsl:function>
  <xsl:function xmlns:xcf="nist:scap:xslt:function" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" name="xcf:pass-ocil-var-ref">
    <xsl:param name="ocil_questionnaire"/>
    <xsl:param name="var_ref"/>
    <xsl:sequence select="$var_ref"/>
    <xsl:for-each select="$ocil_questionnaire/ancestor::ocil:ocil/ocil:variables/ocil:local_variable[@id eq $var_ref/@var_ref]/ocil:set/ocil:when_range/ocil:min[@var_ref]">
      <xsl:sequence select="xcf:pass-ocil-var-ref($ocil_questionnaire,current())"/>
    </xsl:for-each>
    <xsl:for-each select="$ocil_questionnaire/ancestor::ocil:ocil/ocil:variables/ocil:local_variable[@id eq $var_ref/@var_ref]/ocil:set/ocil:when_range/ocil:max[@var_ref]">
      <xsl:sequence select="xcf:pass-ocil-var-ref($ocil_questionnaire,current())"/>
    </xsl:for-each>
  </xsl:function>
  <xsl:function xmlns:xcf="nist:scap:xslt:function" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" name="xcf:resolve-in-catalog">
    <xsl:param name="resolver-node"/>
    <xsl:param name="uri"/>
    <xsl:choose>
      <xsl:when test="starts-with($uri,'#')">
        <xsl:value-of select="$uri"/>
      </xsl:when>
      <xsl:when test="exists($resolver-node)">
        <xsl:choose>
          <xsl:when test="$resolver-node/local-name() eq 'uri'">
            <xsl:choose>
              <xsl:when test="$resolver-node/@name eq $uri">
                <xsl:value-of select="$resolver-node/@uri"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="xcf:resolve-in-catalog($resolver-node/following-sibling::*[1], $uri)"/>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:when>
          <xsl:when test="$resolver-node/local-name() eq 'rewriteURI'">
            <xsl:choose>
              <xsl:when test="starts-with($uri,$resolver-node/@uriStartString)">
                <xsl:value-of select="concat($resolver-node/@rewritePrefix,substring($uri,string-length($resolver-node/@uriStartString)+1))"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="xcf:resolve-in-catalog($resolver-node/following-sibling::*[1], $uri)"/>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:when>
        </xsl:choose>
      </xsl:when>
    </xsl:choose>
  </xsl:function>
  <xsl:function xmlns:xcf="nist:scap:xslt:function" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.w3.org/1999/XSL/Transform" name="xcf:get-all-external-vars">
    <xsl:param name="doc"/>
    <xsl:param name="node"/>
    <xsl:sequence select="$doc//oval-def:external_variable[@id eq $node/@var_ref]"/>
    <xsl:for-each select="$doc//*[@id eq $node/@var_ref]//*[@var_ref]">
      <xsl:sequence select="xcf:get-all-external-vars($doc,current())"/>
    </xsl:for-each>
  </xsl:function>
  <xsl:function xmlns:xcf="nist:scap:xslt:function" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" name="xcf:is-external-ref">
    <xsl:param name="catalog"/>
    <xsl:param name="uri"/>
    <xsl:variable name="comp-ref" select="xcf:get-component-ref($catalog,$uri)"/>
    <xsl:choose>
      <xsl:when test="exists($comp-ref)">
        <xsl:value-of select="not(starts-with($comp-ref/@xlink:href,'#'))"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="false()"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>
  <xsl:function xmlns:xcf="nist:scap:xslt:function" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" name="xcf:get-all-group-parents">
    <xsl:param name="doc"/>
    <xsl:param name="node"/>
    <xsl:sequence select="$node"/>
    <xsl:if test="exists($node/@extends)">
      <xsl:sequence select="xcf:get-all-group-parents($doc,$doc//xccdf:Benchmark//xccdf:Group[@id eq $node/@extends])"/>
    </xsl:if>
  </xsl:function>
  <xsl:function xmlns:xcf="nist:scap:xslt:function" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" name="xcf:get-locator-prefix">
    <xsl:param name="name"/>
    <xsl:variable name="subName" select="substring($name,1,string-length($name) - string-length(tokenize($name,'-')[last()]) - 1)"/>
    <xsl:choose>
      <xsl:when test="ends-with($subName,'cpe')">
        <xsl:value-of select="xcf:get-locator-prefix($subName)"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="tokenize($subName,'/')[last()]"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>
  <xsl:function xmlns:xcf="nist:scap:xslt:function" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" name="xcf:get-locator-prefix-res">
    <xsl:param name="name"/>
    <xsl:variable name="subName" select="substring($name,1,string-length($name) - string-length(tokenize($name,'-')[last()]) - 1)"/>
    <xsl:value-of select="xcf:get-locator-prefix($subName)"/>
  </xsl:function>
</sch:schema>
