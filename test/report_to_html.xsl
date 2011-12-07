<?xml version="1.0"?>
<xsl:stylesheet xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:oval="http://oval.mitre.org/XMLSchema/oval-common-5"
                xmlns:oval-res="http://oval.mitre.org/XMLSchema/oval-results-5"
                xmlns:report="http://www.joval.org/xml/report.xsd">
  <xsl:output method="html" indent="yes"/>
  <xsl:template match="/">
    <html>
      <head>
        <title>jOVAL Automated Test Report</title>
      </head>
      <body>
        <h2>jOVAL Automated Test Report</h2>
        <p>Elapsed time:
          <xsl:call-template name="printDuration">
            <xsl:with-param name="duration" select="/report:Report/@Runtime"/>
          </xsl:call-template>
        </p>
        <p>Generated:
          <xsl:call-template name="printDate">
            <xsl:with-param name="date" select="/report:Report/@Date"/>
          </xsl:call-template>
        </p>
        <table border="0" cellspacing="10">

          <xsl:for-each select="//report:TestSuite">
            <tr>
              <td style="border-style: solid; border-width: 1px">
                <table border="0" cellpadding="5" cellspacing="0" width="800">
                  <tr>
                    <td height="50" bgcolor="#dddddd" width="600"><b><xsl:value-of select="./@Name"/></b></td>
                    <td colspan="2" bgcolor="#dddddd" width="200">Run time:
                      <xsl:call-template name="printDuration">
                        <xsl:with-param name="duration" select="./@Runtime"/>
                      </xsl:call-template>
                    </td>
                    <xsl:apply-templates/>
                  </tr>
                </table>
              </td>
            </tr>
          </xsl:for-each>

        </table>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="//report:TestDocument">
    <tr bgcolor="#eeeeee">
      <td height="25"><b><xsl:value-of select="./@FileName"/></b></td>
        <td colspan="2">Run time: 
          <xsl:call-template name="printDuration">
            <xsl:with-param name="duration" select="./@Runtime"/>
          </xsl:call-template>
        </td>
      </tr>
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="//report:Error">
    <tr>
      <td colspan="3">
        <pre><xsl:value-of select="."/></pre>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="//report:TestResults">
    <xsl:for-each select="report:TestResult">
      <tr>
        <td><xsl:value-of select="./@DefinitionId"/></td>
          <xsl:choose>
            <xsl:when test="./@Result='PASSED'">
              <td colspan="2"><font color="#00ee00">PASSED</font></td>
            </xsl:when>
          <xsl:otherwise>
            <td><font color="#ee0000">FAILED</font></td>
            <td><img src="../rsrc/xmldoc.gif" width="16" height="16"/></td>
          </xsl:otherwise>
        </xsl:choose>
      </tr>
    </xsl:for-each>
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
    <xsl:variable name="hh">
      <xsl:value-of select="format-number(substring-before(substring-after($date, 'T'), ':'), '00')"/>
    </xsl:variable>
    <xsl:variable name="mm">
      <xsl:value-of select="format-number(substring-before(substring-after($date, ':'), ':'), '00')"/>
    </xsl:variable>
    <xsl:variable name="ss">
      <xsl:value-of select="format-number(substring-before(substring-after(substring-after($date, ':'), ':'), '.'), '00')"/>
    </xsl:variable>
    <xsl:value-of select="concat($day, ' ', $month, ' ', $year, ' ', $hh, ':', $mm, ':', $ss)"/>
  </xsl:template>

  <xsl:template name="printDuration">
    <xsl:param name="duration"/>
    <xsl:variable name="hours">
      <xsl:value-of select="format-number(substring-before(substring-after($duration, 'T'), 'H'), '00')"/>
    </xsl:variable>
    <xsl:variable name="minutes">
      <xsl:value-of select="format-number(substring-before(substring-after($duration, 'H'), 'M'), '00')"/>
    </xsl:variable>
    <xsl:variable name="seconds">
      <xsl:value-of select="format-number(substring-before(substring-after(substring-after($duration, 'T'), 'M'), 'S'), '00.000')"/>
    </xsl:variable>

    <xsl:value-of select="concat($hours, ':', $minutes, ':', $seconds)"/>
  </xsl:template>
</xsl:stylesheet>
