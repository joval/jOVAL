<?xml version="1.0"?>
<!--

  Copyright (C) 2012 jOVAL.org.  All rights reserved.
  This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

-->
<xsl:stylesheet xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:xccdf="http://checklists.nist.gov/xccdf/1.2">
  <xsl:output method="html" indent="yes"/>
  <xsl:template match="/">
    <html>
      <style type="text/css">
          TD.title {BACKGROUND-COLOR: #000000; COLOR: #ffc; TEXT-ALIGN: left; font: bold 12pt/14pt "Arial"} 
          TD.label {BACKGROUND-COLOR: #99cc99; font: 10pt/12pt "Arial"}
          TD.label2 {font: bold 10pt/14pt "Arial"}
          TD.text {font: 10pt/12pt "Arial"}

          .passA{background-color: #ACD685; font: 10pt/12pt "Arial"}
          .passB{background-color: #CBE6B3; font: 10pt/12pt "Arial"}

          .failA{background-color: #FFBC8F; font: 10pt/12pt "Arial"}
          .failB{background-color: #FFE0CC; font: 10pt/12pt "Arial"}

          .errorA{background-color: #FFDD75; font: 10pt/12pt "Arial"}
          .errorB{background-color: #FFECB3; font: 10pt/12pt "Arial"}

          .unknownA{background-color: #AEC8E0; font: 10pt/12pt "Arial"}
          .unknownB{background-color: #DAE6F1; font: 10pt/12pt "Arial"}

          .naA{background-color: #EEEEEE; font: 10pt/12pt "Arial"}
          .naB{background-color: #FFFFFF; font: 10pt/12pt "Arial"}

          .ncA{background-color: #EEEEEE; font: 10pt/12pt "Arial"}
          .ncB{background-color: #FFFFFF; font: 10pt/12pt "Arial"}

          .nsA{background-color: #EEEEEE; font: 10pt/12pt "Arial"}
          .nsB{background-color: #FFFFFF; font: 10pt/12pt "Arial"}

          .infoA{background-color: #DEC8E0; font: 10pt/12pt "Arial"}
          .infoB{background-color: #FAE6F1; font: 10pt/12pt "Arial"}

          .fixedA{background-color: #84C4B9; font: 10pt/12pt "Arial"}
          .fixedB{background-color: #A7D0FF; font: 10pt/12pt "Arial"}
        </style>
      <head>
        <title>XPERT&#8482; Benchmark Test Results</title>
      </head>
      <body>

        <table border="1" cellpadding="2" cellspacing="0" width="100%" bgcolor="#cccccc">
          <tr>
            <td class="title" colspan="5">XCCDF Scan Summary</td>
          </tr>
          <tr>
            <td class="label" nowrap="nowrap">System</td>
            <td class="label" nowrap="nowrap">Start Date</td>
            <td class="label" nowrap="nowrap">Start Time</td>
            <td class="label" nowrap="nowrap">End Date</td>
            <td class="label" nowrap="nowrap">End Time</td>
          </tr>
          <tr>
            <td class="text"><xsl:value-of select="/xccdf:Benchmark/xccdf:TestResult/@test-system"/></td>
            <td class="text">
              <xsl:call-template name="printDate">
                <xsl:with-param name="date" select="/xccdf:Benchmark/xccdf:TestResult/@start-time"/>
              </xsl:call-template>
            </td>
            <td class="text">
              <xsl:call-template name="printTime">
                <xsl:with-param name="date" select="/xccdf:Benchmark/xccdf:TestResult/@start-time"/>
              </xsl:call-template>
            </td>
            <td class="text">
              <xsl:call-template name="printDate">
                <xsl:with-param name="date" select="/xccdf:Benchmark/xccdf:TestResult/@end-time"/>
              </xsl:call-template>
            </td>
            <td class="text">
              <xsl:call-template name="printTime">
                <xsl:with-param name="date" select="/xccdf:Benchmark/xccdf:TestResult/@end-time"/>
              </xsl:call-template>
            </td>
          </tr>
        </table>

        <br/>

        <table border="1" cellpadding="2" cellspacing="0" width="100%" bgcolor="#cccccc">
          <tr>
            <td class="title" colspan="2">Benchmark Information</td>
          </tr>
          <tr>
            <td class="label2">Benchmark ID</td>
            <td class="text"><xsl:value-of select="/xccdf:Benchmark/xccdf:TestResult/xccdf:benchmark/@id"/></td>
          </tr>
          <tr>
            <td class="label2" width="20%">Benchmark Version</td>
            <td class="text"><xsl:value-of select="/xccdf:Benchmark/xccdf:TestResult/@version"/></td>
          </tr>
          <tr>
            <td class="label2">Profile ID</td>
            <td class="text"><xsl:value-of select="/xccdf:Benchmark/xccdf:TestResult/xccdf:profile/@idref"/>&#160;</td>
          </tr>
	</table>

        <table border="1" cellpadding="2" cellspacing="0" width="100%" bgcolor="#cccccc">
          <tr>
            <td class="title" colspan="2">Target Information</td>
          </tr>
          <xsl:for-each select="//xccdf:Benchmark/xccdf:TestResult/xccdf:target">
            <tr>
              <td class="label2">Target Name</td>
              <td class="text"><xsl:value-of select="./text()"/></td>
            </tr>
          </xsl:for-each>
          <xsl:for-each select="//xccdf:Benchmark/xccdf:TestResult/xccdf:identity">
            <tr>
              <td class="label2" width="20%">Identity</td>
              <td>
                <table border="1" cellpadding="1" cellspacing="0" bgcolor="#ffffff" width="100%">
                  <tr>
                    <td class="label2" width="20%">Name</td>
                    <td class="text"><xsl:value-of select="./text()"/></td>
                  </tr>
                  <tr>
                    <td class="label2">Authenticated</td>
                    <td class="text"><xsl:value-of select="./@authenticated"/></td>
                  </tr>
                  <tr>
                    <td class="label2">Privileged</td>
                    <td class="text"><xsl:value-of select="./@privileged"/></td>
                  </tr>
                </table>
              </td>
            </tr>
          </xsl:for-each>
          <tr>
            <td class="label2" width="20%">Interfaces</td>
            <td>
              <table border="1" cellpadding="1" cellspacing="0" bgcolor="#ffffff" width="100%">
                <xsl:for-each select="//xccdf:Benchmark/xccdf:TestResult/xccdf:target-address">
                  <xsl:sort select="./text()" data-type="text" order="ascending"/>
                  <tr>
                    <xsl:choose>
                      <xsl:when test="position() mod 2 = 1">
                        <xsl:attribute name="bgcolor">#eeeeee</xsl:attribute>
                      </xsl:when>
                      <xsl:when test="position() mod 2 = 0">
                        <xsl:attribute name="bgcolor">#ffffff</xsl:attribute>
                      </xsl:when>
                    </xsl:choose>
                    <td class="label2" width="20%">IP Address</td>
                    <td class="text"><xsl:value-of select="./text()"/></td>
                  </tr>
                </xsl:for-each>
              </table>
            </td>
          </tr>
        </table>

        <table border="1" cellpadding="2" cellspacing="0" width="100%">
          <tr>
            <td class="title" colspan="2">Benchmark Test Results</td>
          </tr>
          <tr>
            <td colspan="2">
              <xsl:call-template name="ResultColorTable"/>
            </td>
          </tr>
          <tr>
            <td class="label" align="center">Check ID</td>
            <td class="label" align="center" width="120">Result</td>
          </tr>

          <xsl:for-each select="//xccdf:Benchmark/xccdf:TestResult/xccdf:rule-result">
            <xsl:sort select="./xccdf:result/text()" data-type="text" order="ascending"/>
            <xsl:sort select="./@idref" data-type="text" order="ascending"/>
            <xsl:call-template name="RuleResult">
              <xsl:with-param name="ruleResultElt" select="."/>
            </xsl:call-template>
          </xsl:for-each>
        </table>

      </body>
    </html>
  </xsl:template>

  <xsl:template name="printDate">
    <xsl:param name="date"/>
    <xsl:variable name="year">
      <xsl:value-of select="substring-before($date, '-')"/>
    </xsl:variable>
    <xsl:variable name="mon">
      <xsl:value-of select="substring-before(substring-after($date, '-'), '-') - 1"/>
    </xsl:variable>
    <xsl:variable name="months">
      <xsl:value-of select="'Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec'"/>
    </xsl:variable>
    <xsl:variable name="month">
      <xsl:value-of select="substring($months, $mon * 4, 4)"/>
    </xsl:variable>
    <xsl:variable name="day">
      <xsl:value-of select="substring-before(substring-after(substring-after($date, '-'), '-'), 'T')"/>
    </xsl:variable>
    <xsl:value-of select="concat($day, ' ', $month, ' ', $year)"/>
  </xsl:template>

  <xsl:template name="printTime">
    <xsl:param name="date"/>
    <xsl:variable name="hh">
      <xsl:value-of select="format-number(substring-before(substring-after($date, 'T'), ':'), '00')"/>
    </xsl:variable>
    <xsl:variable name="mm">
      <xsl:value-of select="format-number(substring-before(substring-after($date, ':'), ':'), '00')"/>
    </xsl:variable>
    <xsl:variable name="ss">
      <xsl:value-of select="format-number(substring-before(substring-after(substring-after($date, ':'), ':'), '.'), '00')"/>
    </xsl:variable>
    <xsl:value-of select="concat($hh, ':', $mm, ':', $ss)"/>
  </xsl:template>

  <xsl:template name="RuleResult">
    <xsl:param name="ruleResultElt"/>
      <tr>
      <!-- set results to alternating colors -->
      <xsl:choose>
        <xsl:when test="$ruleResultElt/xccdf:result/text() = 'pass' and position() mod 2 = 1">
          <xsl:attribute name="class">passA</xsl:attribute>
        </xsl:when>
        <xsl:when test="$ruleResultElt/xccdf:result/text() = 'pass' and position() mod 2 = 0">
          <xsl:attribute name="class">passB</xsl:attribute>
        </xsl:when>
        <xsl:when test="$ruleResultElt/xccdf:result/text() = 'fail' and position() mod 2 = 1">
          <xsl:attribute name="class">failA</xsl:attribute>
        </xsl:when>
        <xsl:when test="$ruleResultElt/xccdf:result/text() = 'fail' and position() mod 2 = 0">
          <xsl:attribute name="class">failB</xsl:attribute>
        </xsl:when>
        <xsl:when test="$ruleResultElt/xccdf:result/text() = 'error' and position() mod 2 = 1">
          <xsl:attribute name="class">errorA</xsl:attribute>
        </xsl:when>
        <xsl:when test="$ruleResultElt/xccdf:result/text() = 'error' and position() mod 2 = 0">
          <xsl:attribute name="class">errorB</xsl:attribute>
        </xsl:when>
        <xsl:when test="$ruleResultElt/xccdf:result/text() = 'unknown' and position() mod 2 = 1">
          <xsl:attribute name="class">unknownA</xsl:attribute>
        </xsl:when>
        <xsl:when test="$ruleResultElt/xccdf:result/text() = 'unknown' and position() mod 2 = 0">
          <xsl:attribute name="class">unknownB</xsl:attribute>
        </xsl:when>
        <xsl:when test="$ruleResultElt/xccdf:result/text() = 'notapplicable' and position() mod 2 = 1">
          <xsl:attribute name="class">naA</xsl:attribute>
        </xsl:when>
        <xsl:when test="$ruleResultElt/xccdf:result/text() = 'notapplicable' and position() mod 2 = 0">
          <xsl:attribute name="class">naB</xsl:attribute>
        </xsl:when>
        <xsl:when test="$ruleResultElt/xccdf:result/text() = 'notchecked' and position() mod 2 = 1">
          <xsl:attribute name="class">ncA</xsl:attribute>
        </xsl:when>
        <xsl:when test="$ruleResultElt/xccdf:result/text() = 'notchecked' and position() mod 2 = 0">
          <xsl:attribute name="class">ncB</xsl:attribute>
        </xsl:when>
        <xsl:when test="$ruleResultElt/xccdf:result/text() = 'notselected' and position() mod 2 = 1">
          <xsl:attribute name="class">nsA</xsl:attribute>
        </xsl:when>
        <xsl:when test="$ruleResultElt/xccdf:result/text() = 'notselected' and position() mod 2 = 0">
          <xsl:attribute name="class">nsB</xsl:attribute>
        </xsl:when>
        <xsl:when test="$ruleResultElt/xccdf:result/text() = 'informational' and position() mod 2 = 1">
          <xsl:attribute name="class">infoA</xsl:attribute>
        </xsl:when>
        <xsl:when test="$ruleResultElt/xccdf:result/text() = 'informational' and position() mod 2 = 0">
          <xsl:attribute name="class">infoB</xsl:attribute>
        </xsl:when>
        <xsl:when test="$ruleResultElt/xccdf:result/text() = 'fixed' and position() mod 2 = 1">
          <xsl:attribute name="class">fixedA</xsl:attribute>
        </xsl:when>
        <xsl:when test="$ruleResultElt/xccdf:result/text() = 'fixed' and position() mod 2 = 0">
          <xsl:attribute name="class">fixedB</xsl:attribute>
        </xsl:when>
      </xsl:choose>
        <td>&#160;<xsl:value-of select="$ruleResultElt/@idref"/></td>
        <td align="center"><xsl:value-of select="$ruleResultElt/xccdf:result/text()"/></td>
    </tr>
  </xsl:template>

  <xsl:template name="ResultColorTable">
    <table border="0" cellspacing="0" cellpadding="0">
      <tr>
        <td>&#160;&#160;&#160;</td>
        <td>
          <table border="1" cellpadding="0" cellspacing="0">
            <tr>
              <td class="passA" width="10">&#160;</td>
              <td class="passB" width="10">&#160;</td>
              <td class="text">&#160;Pass&#160;&#160;</td>
            </tr>
          </table>
        </td>
        <td>&#160;&#160;&#160;</td>
        <td>
          <table border="1" cellpadding="0" cellspacing="0">
            <tr>
              <td class="failA" width="10">&#160;</td>
              <td class="failB" width="10">&#160;</td>
              <td class="text">&#160;Fail&#160;&#160;</td>
            </tr>
          </table>
        </td>
        <td>&#160;&#160;&#160;</td>
        <td>
          <table border="1" cellpadding="0" cellspacing="0">
            <tr>
              <td class="errorA" width="10">&#160;</td>
              <td class="errorB" width="10">&#160;</td>
              <td class="text">&#160;Error&#160;&#160;</td>
            </tr>
          </table>
        </td>
        <td>&#160;&#160;&#160;</td>
        <td>
          <table border="1" cellpadding="0" cellspacing="0">
            <tr>
              <td class="unknownA" width="10">&#160;</td>
              <td class="unknownB" width="10">&#160;</td>
              <td class="text">&#160;Unknown&#160;&#160;</td>
            </tr>
          </table>
        </td>
        <td>&#160;&#160;&#160;</td>
        <td>
          <table border="1" cellpadding="0" cellspacing="0">
            <tr>
              <td class="naA" width="10">&#160;</td>
              <td class="naB" width="10">&#160;</td>
              <td class="text">&#160;Not&#160;Applicable&#160;&#160;</td>
            </tr>
          </table>
        </td>
        <td>&#160;&#160;&#160;</td>
        <td>
          <table border="1" cellpadding="0" cellspacing="0">
            <tr>
              <td class="ncA" width="10">&#160;</td>
              <td class="ncB" width="10">&#160;</td>
              <td class="text">&#160;Not&#160;Checked&#160;&#160;</td>
            </tr>
          </table>
        </td>
        <td>&#160;&#160;&#160;</td>
        <td>
          <table border="1" cellpadding="0" cellspacing="0">
            <tr>
              <td class="nsA" width="10">&#160;</td>
              <td class="nsB" width="10">&#160;</td>
              <td class="text">&#160;Not&#160;Selected&#160;&#160;</td>
            </tr>
          </table>
        </td>
        <td>&#160;&#160;&#160;</td>
        <td>
          <table border="1" cellpadding="0" cellspacing="0">
            <tr>
              <td class="infoA" width="10">&#160;</td>
              <td class="infoB" width="10">&#160;</td>
              <td class="text">&#160;Informational&#160;&#160;</td>
            </tr>
          </table>
        </td>
        <td>&#160;&#160;&#160;</td>
        <td>
          <table border="1" cellpadding="0" cellspacing="0">
            <tr>
              <td class="fixedA" width="10">&#160;</td>
              <td class="fixedB" width="10">&#160;</td>
              <td class="text">&#160;Fixed&#160;&#160;</td>
            </tr>
          </table>
        </td>
      </tr>
    </table>
  </xsl:template>

</xsl:stylesheet>
