<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:cdf="http://checklists.nist.gov/xccdf/1.1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:cpe="http://cpe.mitre.org/dictionary/2.0" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:xhtml="http://www.w3.org/1999/xhtml" xmlns:dsig="http://www.w3.org/2000/09/xmldsig#" xmlns="http://checklists.nist.gov/xccdf/1.2" exclude-result-prefixes="xs cdf" version="2.0">
    <xsl:output method="xml" indent="yes"/>
    <xsl:template match="comment()">
        <!-- Copy comments -->
        <xsl:variable name="newline">
            <xsl:text>
</xsl:text>
        </xsl:variable>
        <xsl:comment>
            <xsl:value-of select="."/>
        </xsl:comment>
        <xsl:value-of select="$newline"/>
    </xsl:template>
    <xsl:template match="*">
        <!--  Fix namespaces and update references to Rule, Group, etc. -->
        <xsl:choose>
            <xsl:when test="namespace-uri(.)='http://checklists.nist.gov/xccdf/1.1'">
                <xsl:element name="{local-name()}" namespace="http://checklists.nist.gov/xccdf/1.2">
                    <xsl:copy-of select="@*"/>
                    <xsl:if test="name(.)!='notice' and name(.)!='platform' and name(.)!='fix'">
                        <xsl:if test="@id">
                            <xsl:attribute name="id" select="@id"/>
                        </xsl:if>
                        <xsl:if test="@value-id">
                            <xsl:attribute name="value-id" select="@value-id"/>
                        </xsl:if>
                        <xsl:if test="@idref">
                            <xsl:choose>
                                <xsl:when test="name()='requires'">
                                    <xsl:attribute name="idref">
                                        <xsl:call-template name="Requires">
                                            <xsl:with-param name="string" select="string(@idref)"/>
                                        </xsl:call-template>
                                    </xsl:attribute>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:variable name="idref-value" select="string(@idref)"/>
                                    <xsl:choose>
                                        <xsl:when test="//cdf:Group[@id=$idref-value]">
                                            <xsl:attribute name="idref" select="$idref-value"/>
                                        </xsl:when>
                                        <xsl:when test="//cdf:Rule[@id=$idref-value]">
                                            <xsl:attribute name="idref" select="$idref-value"/>
                                        </xsl:when>
                                        <xsl:when test="//cdf:Value[@id=$idref-value]">
                                            <xsl:attribute name="idref" select="$idref-value"/>
                                        </xsl:when>
                                        <xsl:when test="//cdf:Profile[@id=$idref-value]">
                                            <xsl:attribute name="idref" select="$idref-value"/>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:attribute name="idref" select="$idref-value"/>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:if>
                    </xsl:if>
                    <xsl:apply-templates/>
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy copy-namespaces="no">
                    <xsl:copy-of select="@*"/>
                    <xsl:if test="name(.)!='notice' and name(.)!='platform' and name(.)!='fix'">
                        <xsl:if test="@id">
                            <xsl:attribute name="id" select="@id"/>
                        </xsl:if>
                        <xsl:if test="@value-id">
                            <xsl:attribute name="value-id" select="@value-id"/>
                        </xsl:if>
                        <xsl:if test="@idref">
                            <xsl:choose>
                                <xsl:when test="name()='cdf:requires'">
                                    <xsl:attribute name="idref">
                                        <xsl:call-template name="Requires">
                                            <xsl:with-param name="string" select="string(@idref)"/>
                                        </xsl:call-template>
                                    </xsl:attribute>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:variable name="idref-value" select="string(@idref)"/>
                                    <xsl:choose>
                                        <xsl:when test="//cdf:Group[@id=$idref-value]">
                                            <xsl:attribute name="idref" select="$idref-value"/>
                                        </xsl:when>
                                        <xsl:when test="//cdf:Rule[@id=$idref-value]">
                                            <xsl:attribute name="idref" select="$idref-value"/>
                                        </xsl:when>
                                        <xsl:when test="//cdf:Value[@id=$idref-value]">
                                            <xsl:attribute name="idref" select="$idref-value"/>
                                        </xsl:when>
                                        <xsl:when test="//cdf:Profile[@id=$idref-value]">
                                            <xsl:attribute name="idref" select="$idref-value"/>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:attribute name="idref" select="$idref-value"/>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:if>
                    </xsl:if>
                    <xsl:apply-templates/>
                </xsl:copy>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template name="Requires">
        <xsl:param name="string"/>
        <xsl:choose>
            <xsl:when test="contains($string, ' ')">
                <xsl:choose>
                    <xsl:when test="//cdf:Group[@id=substring-before($string, ' ')]">
                        <xsl:value-of select="substring-before($string, ' ')"/>
                    </xsl:when>
                    <xsl:when test="//cdf:Rule[@id=substring-before($string, ' ')]">
                        <xsl:value-of select="substring-before($string, ' ')"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="substring-before($string, ' ')"/>
                    </xsl:otherwise>
                </xsl:choose>
                <xsl:text> </xsl:text>
                <xsl:call-template name="Requires">
                    <xsl:with-param name="string" select="substring-after($string, ' ')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:choose>
                    <xsl:when test="//cdf:Group[@id=$string]">
                        <xsl:value-of select="$string"/>
                    </xsl:when>
                    <xsl:when test="//cdf:Rule[@id=$string]">
                        <xsl:value-of select="$string"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$string"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template match="cdf:Benchmark">
        <!--
		Update Benchmark id
		-->
        <xsl:element name="Benchmark">
            <!-- Copy attributes -->
            <xsl:copy-of select="@*"/>
            <!-- Update id attribute -->
            <xsl:attribute name="id" select="@id"/>
            <!-- Update schemaLocation attribute -->
            <xsl:attribute name="xsi:schemaLocation">
                <xsl:call-template name="SchemaLoc">
                    <xsl:with-param name="string" select="string(@xsi:schemaLocation)"/>
                </xsl:call-template>
            </xsl:attribute>
            <!-- Copy child elements -->
            <xsl:apply-templates select="*|comment()"/>
        </xsl:element>
    </xsl:template>
    <xsl:template name="SchemaLoc">
        <xsl:param name="string"/>
        <xsl:choose>
            <xsl:when test="contains($string, ' ')">
                <xsl:choose>
                    <xsl:when test="substring-before($string, ' ')='http://checklists.nist.gov/xccdf/1.1'">
                        <xsl:text>http://checklists.nist.gov/xccdf/1.2 xccdf_1.2.xsd</xsl:text>
                        <xsl:if test="substring-after(substring-after($string, ' '), ' ') ">
                            <xsl:text> </xsl:text>
                            <xsl:call-template name="SchemaLoc">
                                <xsl:with-param name="string" select="substring-after(substring-after($string, ' '), ' ')"/>
                            </xsl:call-template>
                        </xsl:if>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="substring-before($string, ' ')"/>
                        <xsl:text> </xsl:text>
                        <xsl:call-template name="SchemaLoc">
                            <xsl:with-param name="string" select="substring-after($string, ' ')"/>
                        </xsl:call-template>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$string"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template match="cdf:version">
        <!-- Purpose is to check for deprecated elements in Benchmark 
         and place any found into a metadata element - we check here
         because version is a required element in Benchmark which occurs just
		before metadata elments.  If we find any of the the deprecated elements
        we copy them to a metadata element.
		 -->
        <xsl:element name="version">
            <xsl:copy-of select="@*"/>
            <xsl:value-of select="."/>
        </xsl:element>
        <xsl:if test="../cdf:platform-definitions|../cdf:Platform-Specifcation|../cdf:cpe-list">
            <xsl:element name="metadata">
                <xsl:copy-of select="../cdf:platform-definitions|../cdf:Platform-Specifcation|../cdf:cpe-list"/>
            </xsl:element>
        </xsl:if>
    </xsl:template>
    <xsl:template match="cdf:platform-definitions|cdf:Platform-Specification|cdf:cpe-list">
		<!-- do nothing - these are handled in the version template, above -->
	</xsl:template>
    <xsl:template match="cdf:Group">
        <!--
		Update Group id
		-->
        <xsl:element name="Group">
            <!-- Copy the attributes -->
            <xsl:copy-of select="@*"/>
            <!-- Update id attribute -->
            <xsl:attribute name="id" select="@id"/>
            <!-- If present, update extends attribute -->
            <xsl:if test="@extends">
                <xsl:variable name="extends-value" select="string(@extends)"/>
                <xsl:choose>
                    <xsl:when test="//cdf:Group[@id=$extends-value]">
                        <xsl:attribute name="extends" select="@extends"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:attribute name="extends" select="@extends"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:if>
            <!-- Copy child elements -->
            <xsl:apply-templates select="*|comment()"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="cdf:Rule">
        <!--
		Update Rule id
		-->
        <xsl:element name="Rule">
            <!-- Copy the attributes -->
            <xsl:copy-of select="@*"/>
            <!-- Update id attribute -->
            <xsl:attribute name="id" select="@id"/>
            <!-- If present, update extends attribute -->
            <xsl:if test="@extends">
                <xsl:variable name="extends-value" select="string(@extends)"/>
                <xsl:choose>
                    <xsl:when test="//cdf:Rule[@id=$extends-value]">
                        <xsl:attribute name="extends" select="@extends"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:attribute name="extends" select="@extends"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:if>
            <!-- Copy child elements -->
            <xsl:choose>
                <xsl:when test="cdf:impact-metric">
                    <xsl:apply-templates select="cdf:status|cdf:dc-status|cdf:version|title|cdf:description|cdf:warning|cdf:question|cdf:reference|cdf:metadata|comment()"/>
                    <xsl:element name="metadata">
                        <xsl:copy-of select="cdf:impact-metric"/>
                    </xsl:element>
                    <xsl:apply-templates select="cdf:rationale|cdf:platform|cdf:requires|cdf:conflicts|cdf:ident|cdf:profile-note|cdf:fixtext|fix|cdf:check|cdf:complex-check|cdf:signature"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select="*|comment()"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:element>
    </xsl:template>
    <xsl:template match="cdf:impact-metric">
		<!-- do nothing - this element is handled in the Rule template. -->
	</xsl:template>
    <xsl:template match="cdf:Value">
        <!--
		Update Value id
		-->
        <xsl:element name="Value">
            <!-- Copy the attributes -->
            <xsl:copy-of select="@*"/>
            <!-- Update id attribute -->
            <xsl:attribute name="id" select="@id"/>
            <!-- If present, update extends attribute -->
            <xsl:if test="@extends">
                <xsl:variable name="extends-value" select="string(@extends)"/>
                <xsl:choose>
                    <xsl:when test="//cdf:Value[@id=$extends-value]">
                        <xsl:attribute name="extends" select="@extends"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:attribute name="extends" select="@extends"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:if>
            <!-- Copy child elements -->
            <xsl:apply-templates select="*|comment()"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="cdf:Profile">
        <!--
		Update Profile id
		-->
        <xsl:element name="Profile">
            <!-- Copy the attributes -->
            <xsl:copy-of select="@*"/>
            <!-- Update id attribute -->
            <xsl:attribute name="id" select="@id"/>
            <!-- If present, update extends attribute -->
            <xsl:if test="@extends">
                <xsl:variable name="extends-value" select="string(@extends)"/>
                <xsl:choose>
                    <xsl:when test="//cdf:Profile[@id=$extends-value]">
                        <xsl:attribute name="extends" select="@extends"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:attribute name="extends" select="@extends"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:if>
            <!-- Copy child elements -->
            <xsl:apply-templates select="*|comment()"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="cdf:TestResult">
        <!--
		Update TestResult id
		-->
        <xsl:element name="TestResult">
            <!-- Copy the attributes -->
            <xsl:copy-of select="@*"/>
            <!-- Update id attribute -->
            <xsl:attribute name="id" select="@id"/>
            <!-- Copy child elements -->
            <xsl:apply-templates select="*|comment()"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="cdf:sub">
        <!--
		Change sub element to indicate legacy
		-->
        <xsl:element name="sub">
            <!-- Copy the attributes -->
            <xsl:copy-of select="@*"/>
            <!-- Add the use attribute -->
            <xsl:attribute name="use">legacy</xsl:attribute>
        </xsl:element>
    </xsl:template>
</xsl:stylesheet>
