<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:schold="http://www.ascc.net/xml/schematron"
                xmlns:iso="http://purl.oclc.org/dsdl/schematron"
                xmlns:xhtml="http://www.w3.org/1999/xhtml"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:oval="http://oval.mitre.org/XMLSchema/oval-common-5"
                xmlns:oval-def="http://oval.mitre.org/XMLSchema/oval-definitions-5"
                xmlns:ind-def="http://oval.mitre.org/XMLSchema/oval-definitions-5#independent"
                xmlns:aix-def="http://oval.mitre.org/XMLSchema/oval-definitions-5#aix"
                xmlns:apache-def="http://oval.mitre.org/XMLSchema/oval-definitions-5#apache"
                xmlns:catos-def="http://oval.mitre.org/XMLSchema/oval-definitions-5#catos"
                xmlns:esx-def="http://oval.mitre.org/XMLSchema/oval-definitions-5#esx"
                xmlns:freebsd-def="http://oval.mitre.org/XMLSchema/oval-definitions-5#freebsd"
                xmlns:hpux-def="http://oval.mitre.org/XMLSchema/oval-definitions-5#hpux"
                xmlns:ios-def="http://oval.mitre.org/XMLSchema/oval-definitions-5#ios"
                xmlns:linux-def="http://oval.mitre.org/XMLSchema/oval-definitions-5#linux"
                xmlns:macos-def="http://oval.mitre.org/XMLSchema/oval-definitions-5#macos"
                xmlns:pixos-def="http://oval.mitre.org/XMLSchema/oval-definitions-5#pixos"
                xmlns:sp-def="http://oval.mitre.org/XMLSchema/oval-definitions-5#sharepoint"
                xmlns:sol-def="http://oval.mitre.org/XMLSchema/oval-definitions-5#solaris"
                xmlns:unix-def="http://oval.mitre.org/XMLSchema/oval-definitions-5#unix"
                xmlns:win-def="http://oval.mitre.org/XMLSchema/oval-definitions-5#windows"
                version="1.0"><!--Implementers: please note that overriding process-prolog or process-root is 
    the preferred method for meta-stylesheets to use where possible. -->
<xsl:param name="archiveDirParameter"/>
   <xsl:param name="archiveNameParameter"/>
   <xsl:param name="fileNameParameter"/>
   <xsl:param name="fileDirParameter"/>
   <xsl:variable name="document-uri">
      <xsl:value-of select="document-uri(/)"/>
   </xsl:variable>

   <!--PHASES-->


<!--PROLOG-->


<!--XSD TYPES FOR XSLT2-->


<!--KEYS AND FUNCTIONS-->


<!--DEFAULT RULES-->


<!--MODE: SCHEMATRON-SELECT-FULL-PATH-->
<!--This mode can be used to generate an ugly though full XPath for locators-->
<xsl:template match="*" mode="schematron-select-full-path">
      <xsl:apply-templates select="." mode="schematron-get-full-path"/>
   </xsl:template>

   <!--MODE: SCHEMATRON-FULL-PATH-->
<!--This mode can be used to generate an ugly though full XPath for locators-->
<xsl:template match="*" mode="schematron-get-full-path">
      <xsl:apply-templates select="parent::*" mode="schematron-get-full-path"/>
      <xsl:text>/</xsl:text>
      <xsl:choose>
         <xsl:when test="namespace-uri()=''">
            <xsl:value-of select="name()"/>
            <xsl:variable name="p_1" select="1+    count(preceding-sibling::*[name()=name(current())])"/>
            <xsl:if test="$p_1&gt;1 or following-sibling::*[name()=name(current())]">[<xsl:value-of select="$p_1"/>]</xsl:if>
         </xsl:when>
         <xsl:otherwise>
            <xsl:text>*[local-name()='</xsl:text>
            <xsl:value-of select="local-name()"/>
            <xsl:text>']</xsl:text>
            <xsl:variable name="p_2"
                          select="1+   count(preceding-sibling::*[local-name()=local-name(current())])"/>
            <xsl:if test="$p_2&gt;1 or following-sibling::*[local-name()=local-name(current())]">[<xsl:value-of select="$p_2"/>]</xsl:if>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   <xsl:template match="@*" mode="schematron-get-full-path">
      <xsl:text>/</xsl:text>
      <xsl:choose>
         <xsl:when test="namespace-uri()=''">@<xsl:value-of select="name()"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:text>@*[local-name()='</xsl:text>
            <xsl:value-of select="local-name()"/>
            <xsl:text>' and namespace-uri()='</xsl:text>
            <xsl:value-of select="namespace-uri()"/>
            <xsl:text>']</xsl:text>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <!--MODE: SCHEMATRON-FULL-PATH-2-->
<!--This mode can be used to generate prefixed XPath for humans-->
<xsl:template match="node() | @*" mode="schematron-get-full-path-2">
      <xsl:for-each select="ancestor-or-self::*">
         <xsl:text>/</xsl:text>
         <xsl:value-of select="name(.)"/>
         <xsl:if test="preceding-sibling::*[name(.)=name(current())]">
            <xsl:text>[</xsl:text>
            <xsl:value-of select="count(preceding-sibling::*[name(.)=name(current())])+1"/>
            <xsl:text>]</xsl:text>
         </xsl:if>
      </xsl:for-each>
      <xsl:if test="not(self::*)">
         <xsl:text/>/@<xsl:value-of select="name(.)"/>
      </xsl:if>
   </xsl:template>
   <!--MODE: SCHEMATRON-FULL-PATH-3-->
<!--This mode can be used to generate prefixed XPath for humans 
	(Top-level element has index)-->
<xsl:template match="node() | @*" mode="schematron-get-full-path-3">
      <xsl:for-each select="ancestor-or-self::*">
         <xsl:text>/</xsl:text>
         <xsl:value-of select="name(.)"/>
         <xsl:if test="parent::*">
            <xsl:text>[</xsl:text>
            <xsl:value-of select="count(preceding-sibling::*[name(.)=name(current())])+1"/>
            <xsl:text>]</xsl:text>
         </xsl:if>
      </xsl:for-each>
      <xsl:if test="not(self::*)">
         <xsl:text/>/@<xsl:value-of select="name(.)"/>
      </xsl:if>
   </xsl:template>

   <!--MODE: GENERATE-ID-FROM-PATH -->
<xsl:template match="/" mode="generate-id-from-path"/>
   <xsl:template match="text()" mode="generate-id-from-path">
      <xsl:apply-templates select="parent::*" mode="generate-id-from-path"/>
      <xsl:value-of select="concat('.text-', 1+count(preceding-sibling::text()), '-')"/>
   </xsl:template>
   <xsl:template match="comment()" mode="generate-id-from-path">
      <xsl:apply-templates select="parent::*" mode="generate-id-from-path"/>
      <xsl:value-of select="concat('.comment-', 1+count(preceding-sibling::comment()), '-')"/>
   </xsl:template>
   <xsl:template match="processing-instruction()" mode="generate-id-from-path">
      <xsl:apply-templates select="parent::*" mode="generate-id-from-path"/>
      <xsl:value-of select="concat('.processing-instruction-', 1+count(preceding-sibling::processing-instruction()), '-')"/>
   </xsl:template>
   <xsl:template match="@*" mode="generate-id-from-path">
      <xsl:apply-templates select="parent::*" mode="generate-id-from-path"/>
      <xsl:value-of select="concat('.@', name())"/>
   </xsl:template>
   <xsl:template match="*" mode="generate-id-from-path" priority="-0.5">
      <xsl:apply-templates select="parent::*" mode="generate-id-from-path"/>
      <xsl:text>.</xsl:text>
      <xsl:value-of select="concat('.',name(),'-',1+count(preceding-sibling::*[name()=name(current())]),'-')"/>
   </xsl:template>

   <!--MODE: GENERATE-ID-2 -->
<xsl:template match="/" mode="generate-id-2">U</xsl:template>
   <xsl:template match="*" mode="generate-id-2" priority="2">
      <xsl:text>U</xsl:text>
      <xsl:number level="multiple" count="*"/>
   </xsl:template>
   <xsl:template match="node()" mode="generate-id-2">
      <xsl:text>U.</xsl:text>
      <xsl:number level="multiple" count="*"/>
      <xsl:text>n</xsl:text>
      <xsl:number count="node()"/>
   </xsl:template>
   <xsl:template match="@*" mode="generate-id-2">
      <xsl:text>U.</xsl:text>
      <xsl:number level="multiple" count="*"/>
      <xsl:text>_</xsl:text>
      <xsl:value-of select="string-length(local-name(.))"/>
      <xsl:text>_</xsl:text>
      <xsl:value-of select="translate(name(),':','.')"/>
   </xsl:template>
   <!--Strip characters--><xsl:template match="text()" priority="-1"/>

   <!--SCHEMA SETUP-->
<xsl:template match="/">
      <xsl:apply-templates select="/" mode="M36"/>
      <xsl:apply-templates select="/" mode="M37"/>
      <xsl:apply-templates select="/" mode="M38"/>
      <xsl:apply-templates select="/" mode="M39"/>
      <xsl:apply-templates select="/" mode="M40"/>
      <xsl:apply-templates select="/" mode="M41"/>
      <xsl:apply-templates select="/" mode="M42"/>
      <xsl:apply-templates select="/" mode="M43"/>
      <xsl:apply-templates select="/" mode="M44"/>
      <xsl:apply-templates select="/" mode="M45"/>
      <xsl:apply-templates select="/" mode="M46"/>
      <xsl:apply-templates select="/" mode="M47"/>
      <xsl:apply-templates select="/" mode="M48"/>
      <xsl:apply-templates select="/" mode="M49"/>
      <xsl:apply-templates select="/" mode="M50"/>
      <xsl:apply-templates select="/" mode="M51"/>
      <xsl:apply-templates select="/" mode="M52"/>
      <xsl:apply-templates select="/" mode="M53"/>
      <xsl:apply-templates select="/" mode="M54"/>
      <xsl:apply-templates select="/" mode="M55"/>
      <xsl:apply-templates select="/" mode="M56"/>
      <xsl:apply-templates select="/" mode="M57"/>
      <xsl:apply-templates select="/" mode="M58"/>
      <xsl:apply-templates select="/" mode="M59"/>
      <xsl:apply-templates select="/" mode="M60"/>
      <xsl:apply-templates select="/" mode="M61"/>
      <xsl:apply-templates select="/" mode="M62"/>
      <xsl:apply-templates select="/" mode="M63"/>
      <xsl:apply-templates select="/" mode="M64"/>
      <xsl:apply-templates select="/" mode="M65"/>
      <xsl:apply-templates select="/" mode="M66"/>
      <xsl:apply-templates select="/" mode="M67"/>
      <xsl:apply-templates select="/" mode="M68"/>
      <xsl:apply-templates select="/" mode="M69"/>
      <xsl:apply-templates select="/" mode="M70"/>
      <xsl:apply-templates select="/" mode="M71"/>
      <xsl:apply-templates select="/" mode="M72"/>
      <xsl:apply-templates select="/" mode="M73"/>
      <xsl:apply-templates select="/" mode="M74"/>
      <xsl:apply-templates select="/" mode="M75"/>
      <xsl:apply-templates select="/" mode="M76"/>
      <xsl:apply-templates select="/" mode="M77"/>
      <xsl:apply-templates select="/" mode="M78"/>
      <xsl:apply-templates select="/" mode="M79"/>
      <xsl:apply-templates select="/" mode="M80"/>
      <xsl:apply-templates select="/" mode="M81"/>
      <xsl:apply-templates select="/" mode="M82"/>
      <xsl:apply-templates select="/" mode="M83"/>
      <xsl:apply-templates select="/" mode="M84"/>
      <xsl:apply-templates select="/" mode="M85"/>
      <xsl:apply-templates select="/" mode="M86"/>
      <xsl:apply-templates select="/" mode="M87"/>
      <xsl:apply-templates select="/" mode="M88"/>
      <xsl:apply-templates select="/" mode="M89"/>
      <xsl:apply-templates select="/" mode="M90"/>
      <xsl:apply-templates select="/" mode="M91"/>
      <xsl:apply-templates select="/" mode="M92"/>
      <xsl:apply-templates select="/" mode="M93"/>
      <xsl:apply-templates select="/" mode="M94"/>
      <xsl:apply-templates select="/" mode="M95"/>
      <xsl:apply-templates select="/" mode="M96"/>
      <xsl:apply-templates select="/" mode="M97"/>
      <xsl:apply-templates select="/" mode="M98"/>
      <xsl:apply-templates select="/" mode="M99"/>
      <xsl:apply-templates select="/" mode="M100"/>
      <xsl:apply-templates select="/" mode="M101"/>
      <xsl:apply-templates select="/" mode="M102"/>
      <xsl:apply-templates select="/" mode="M103"/>
      <xsl:apply-templates select="/" mode="M104"/>
      <xsl:apply-templates select="/" mode="M105"/>
      <xsl:apply-templates select="/" mode="M106"/>
      <xsl:apply-templates select="/" mode="M107"/>
      <xsl:apply-templates select="/" mode="M108"/>
      <xsl:apply-templates select="/" mode="M109"/>
      <xsl:apply-templates select="/" mode="M110"/>
      <xsl:apply-templates select="/" mode="M111"/>
      <xsl:apply-templates select="/" mode="M112"/>
      <xsl:apply-templates select="/" mode="M113"/>
      <xsl:apply-templates select="/" mode="M114"/>
      <xsl:apply-templates select="/" mode="M115"/>
      <xsl:apply-templates select="/" mode="M116"/>
      <xsl:apply-templates select="/" mode="M117"/>
      <xsl:apply-templates select="/" mode="M118"/>
      <xsl:apply-templates select="/" mode="M119"/>
      <xsl:apply-templates select="/" mode="M120"/>
      <xsl:apply-templates select="/" mode="M121"/>
      <xsl:apply-templates select="/" mode="M122"/>
      <xsl:apply-templates select="/" mode="M123"/>
      <xsl:apply-templates select="/" mode="M124"/>
      <xsl:apply-templates select="/" mode="M125"/>
      <xsl:apply-templates select="/" mode="M126"/>
      <xsl:apply-templates select="/" mode="M127"/>
      <xsl:apply-templates select="/" mode="M128"/>
      <xsl:apply-templates select="/" mode="M129"/>
      <xsl:apply-templates select="/" mode="M130"/>
      <xsl:apply-templates select="/" mode="M131"/>
      <xsl:apply-templates select="/" mode="M132"/>
      <xsl:apply-templates select="/" mode="M133"/>
      <xsl:apply-templates select="/" mode="M134"/>
      <xsl:apply-templates select="/" mode="M135"/>
      <xsl:apply-templates select="/" mode="M136"/>
      <xsl:apply-templates select="/" mode="M137"/>
      <xsl:apply-templates select="/" mode="M138"/>
      <xsl:apply-templates select="/" mode="M139"/>
      <xsl:apply-templates select="/" mode="M140"/>
      <xsl:apply-templates select="/" mode="M141"/>
      <xsl:apply-templates select="/" mode="M142"/>
      <xsl:apply-templates select="/" mode="M143"/>
      <xsl:apply-templates select="/" mode="M144"/>
      <xsl:apply-templates select="/" mode="M145"/>
      <xsl:apply-templates select="/" mode="M146"/>
      <xsl:apply-templates select="/" mode="M147"/>
      <xsl:apply-templates select="/" mode="M148"/>
      <xsl:apply-templates select="/" mode="M149"/>
      <xsl:apply-templates select="/" mode="M150"/>
      <xsl:apply-templates select="/" mode="M151"/>
      <xsl:apply-templates select="/" mode="M152"/>
      <xsl:apply-templates select="/" mode="M153"/>
      <xsl:apply-templates select="/" mode="M154"/>
      <xsl:apply-templates select="/" mode="M155"/>
      <xsl:apply-templates select="/" mode="M156"/>
      <xsl:apply-templates select="/" mode="M157"/>
      <xsl:apply-templates select="/" mode="M158"/>
      <xsl:apply-templates select="/" mode="M159"/>
      <xsl:apply-templates select="/" mode="M160"/>
      <xsl:apply-templates select="/" mode="M161"/>
      <xsl:apply-templates select="/" mode="M162"/>
      <xsl:apply-templates select="/" mode="M163"/>
      <xsl:apply-templates select="/" mode="M164"/>
      <xsl:apply-templates select="/" mode="M165"/>
      <xsl:apply-templates select="/" mode="M166"/>
      <xsl:apply-templates select="/" mode="M167"/>
      <xsl:apply-templates select="/" mode="M168"/>
      <xsl:apply-templates select="/" mode="M169"/>
      <xsl:apply-templates select="/" mode="M170"/>
      <xsl:apply-templates select="/" mode="M171"/>
      <xsl:apply-templates select="/" mode="M172"/>
      <xsl:apply-templates select="/" mode="M173"/>
      <xsl:apply-templates select="/" mode="M174"/>
      <xsl:apply-templates select="/" mode="M175"/>
      <xsl:apply-templates select="/" mode="M176"/>
      <xsl:apply-templates select="/" mode="M177"/>
      <xsl:apply-templates select="/" mode="M178"/>
      <xsl:apply-templates select="/" mode="M179"/>
      <xsl:apply-templates select="/" mode="M180"/>
      <xsl:apply-templates select="/" mode="M181"/>
      <xsl:apply-templates select="/" mode="M182"/>
      <xsl:apply-templates select="/" mode="M183"/>
      <xsl:apply-templates select="/" mode="M184"/>
      <xsl:apply-templates select="/" mode="M185"/>
      <xsl:apply-templates select="/" mode="M186"/>
      <xsl:apply-templates select="/" mode="M187"/>
      <xsl:apply-templates select="/" mode="M188"/>
      <xsl:apply-templates select="/" mode="M189"/>
      <xsl:apply-templates select="/" mode="M190"/>
      <xsl:apply-templates select="/" mode="M191"/>
      <xsl:apply-templates select="/" mode="M192"/>
      <xsl:apply-templates select="/" mode="M193"/>
      <xsl:apply-templates select="/" mode="M194"/>
      <xsl:apply-templates select="/" mode="M195"/>
      <xsl:apply-templates select="/" mode="M196"/>
      <xsl:apply-templates select="/" mode="M197"/>
      <xsl:apply-templates select="/" mode="M198"/>
      <xsl:apply-templates select="/" mode="M199"/>
      <xsl:apply-templates select="/" mode="M200"/>
      <xsl:apply-templates select="/" mode="M201"/>
      <xsl:apply-templates select="/" mode="M202"/>
      <xsl:apply-templates select="/" mode="M203"/>
      <xsl:apply-templates select="/" mode="M204"/>
      <xsl:apply-templates select="/" mode="M205"/>
      <xsl:apply-templates select="/" mode="M206"/>
      <xsl:apply-templates select="/" mode="M207"/>
      <xsl:apply-templates select="/" mode="M208"/>
      <xsl:apply-templates select="/" mode="M209"/>
      <xsl:apply-templates select="/" mode="M210"/>
      <xsl:apply-templates select="/" mode="M211"/>
      <xsl:apply-templates select="/" mode="M212"/>
      <xsl:apply-templates select="/" mode="M213"/>
      <xsl:apply-templates select="/" mode="M214"/>
      <xsl:apply-templates select="/" mode="M215"/>
      <xsl:apply-templates select="/" mode="M216"/>
      <xsl:apply-templates select="/" mode="M217"/>
      <xsl:apply-templates select="/" mode="M218"/>
      <xsl:apply-templates select="/" mode="M219"/>
      <xsl:apply-templates select="/" mode="M220"/>
      <xsl:apply-templates select="/" mode="M221"/>
      <xsl:apply-templates select="/" mode="M222"/>
      <xsl:apply-templates select="/" mode="M223"/>
      <xsl:apply-templates select="/" mode="M224"/>
      <xsl:apply-templates select="/" mode="M225"/>
      <xsl:apply-templates select="/" mode="M226"/>
      <xsl:apply-templates select="/" mode="M227"/>
      <xsl:apply-templates select="/" mode="M228"/>
      <xsl:apply-templates select="/" mode="M229"/>
      <xsl:apply-templates select="/" mode="M230"/>
      <xsl:apply-templates select="/" mode="M231"/>
      <xsl:apply-templates select="/" mode="M232"/>
      <xsl:apply-templates select="/" mode="M233"/>
      <xsl:apply-templates select="/" mode="M234"/>
      <xsl:apply-templates select="/" mode="M235"/>
      <xsl:apply-templates select="/" mode="M236"/>
      <xsl:apply-templates select="/" mode="M237"/>
      <xsl:apply-templates select="/" mode="M238"/>
      <xsl:apply-templates select="/" mode="M239"/>
      <xsl:apply-templates select="/" mode="M240"/>
      <xsl:apply-templates select="/" mode="M241"/>
      <xsl:apply-templates select="/" mode="M242"/>
      <xsl:apply-templates select="/" mode="M243"/>
      <xsl:apply-templates select="/" mode="M244"/>
      <xsl:apply-templates select="/" mode="M245"/>
      <xsl:apply-templates select="/" mode="M246"/>
      <xsl:apply-templates select="/" mode="M247"/>
      <xsl:apply-templates select="/" mode="M248"/>
      <xsl:apply-templates select="/" mode="M249"/>
      <xsl:apply-templates select="/" mode="M250"/>
      <xsl:apply-templates select="/" mode="M251"/>
      <xsl:apply-templates select="/" mode="M252"/>
      <xsl:apply-templates select="/" mode="M253"/>
      <xsl:apply-templates select="/" mode="M254"/>
      <xsl:apply-templates select="/" mode="M255"/>
      <xsl:apply-templates select="/" mode="M256"/>
      <xsl:apply-templates select="/" mode="M257"/>
      <xsl:apply-templates select="/" mode="M258"/>
      <xsl:apply-templates select="/" mode="M259"/>
      <xsl:apply-templates select="/" mode="M260"/>
      <xsl:apply-templates select="/" mode="M261"/>
      <xsl:apply-templates select="/" mode="M262"/>
      <xsl:apply-templates select="/" mode="M263"/>
      <xsl:apply-templates select="/" mode="M264"/>
      <xsl:apply-templates select="/" mode="M265"/>
      <xsl:apply-templates select="/" mode="M266"/>
      <xsl:apply-templates select="/" mode="M267"/>
      <xsl:apply-templates select="/" mode="M268"/>
      <xsl:apply-templates select="/" mode="M269"/>
      <xsl:apply-templates select="/" mode="M270"/>
      <xsl:apply-templates select="/" mode="M271"/>
      <xsl:apply-templates select="/" mode="M272"/>
      <xsl:apply-templates select="/" mode="M273"/>
      <xsl:apply-templates select="/" mode="M274"/>
      <xsl:apply-templates select="/" mode="M275"/>
      <xsl:apply-templates select="/" mode="M276"/>
      <xsl:apply-templates select="/" mode="M277"/>
      <xsl:apply-templates select="/" mode="M278"/>
      <xsl:apply-templates select="/" mode="M279"/>
      <xsl:apply-templates select="/" mode="M280"/>
      <xsl:apply-templates select="/" mode="M281"/>
      <xsl:apply-templates select="/" mode="M282"/>
      <xsl:apply-templates select="/" mode="M283"/>
      <xsl:apply-templates select="/" mode="M284"/>
      <xsl:apply-templates select="/" mode="M285"/>
      <xsl:apply-templates select="/" mode="M286"/>
      <xsl:apply-templates select="/" mode="M287"/>
      <xsl:apply-templates select="/" mode="M288"/>
      <xsl:apply-templates select="/" mode="M289"/>
      <xsl:apply-templates select="/" mode="M290"/>
      <xsl:apply-templates select="/" mode="M291"/>
      <xsl:apply-templates select="/" mode="M292"/>
      <xsl:apply-templates select="/" mode="M293"/>
      <xsl:apply-templates select="/" mode="M294"/>
      <xsl:apply-templates select="/" mode="M295"/>
      <xsl:apply-templates select="/" mode="M296"/>
      <xsl:apply-templates select="/" mode="M297"/>
      <xsl:apply-templates select="/" mode="M298"/>
      <xsl:apply-templates select="/" mode="M299"/>
      <xsl:apply-templates select="/" mode="M300"/>
      <xsl:apply-templates select="/" mode="M301"/>
      <xsl:apply-templates select="/" mode="M302"/>
      <xsl:apply-templates select="/" mode="M303"/>
      <xsl:apply-templates select="/" mode="M304"/>
      <xsl:apply-templates select="/" mode="M305"/>
      <xsl:apply-templates select="/" mode="M306"/>
      <xsl:apply-templates select="/" mode="M307"/>
      <xsl:apply-templates select="/" mode="M308"/>
      <xsl:apply-templates select="/" mode="M309"/>
      <xsl:apply-templates select="/" mode="M310"/>
      <xsl:apply-templates select="/" mode="M311"/>
      <xsl:apply-templates select="/" mode="M312"/>
      <xsl:apply-templates select="/" mode="M313"/>
      <xsl:apply-templates select="/" mode="M314"/>
      <xsl:apply-templates select="/" mode="M315"/>
      <xsl:apply-templates select="/" mode="M316"/>
      <xsl:apply-templates select="/" mode="M317"/>
      <xsl:apply-templates select="/" mode="M318"/>
      <xsl:apply-templates select="/" mode="M319"/>
      <xsl:apply-templates select="/" mode="M320"/>
      <xsl:apply-templates select="/" mode="M321"/>
      <xsl:apply-templates select="/" mode="M322"/>
      <xsl:apply-templates select="/" mode="M323"/>
      <xsl:apply-templates select="/" mode="M324"/>
      <xsl:apply-templates select="/" mode="M325"/>
      <xsl:apply-templates select="/" mode="M326"/>
      <xsl:apply-templates select="/" mode="M327"/>
      <xsl:apply-templates select="/" mode="M328"/>
      <xsl:apply-templates select="/" mode="M329"/>
      <xsl:apply-templates select="/" mode="M330"/>
      <xsl:apply-templates select="/" mode="M331"/>
      <xsl:apply-templates select="/" mode="M332"/>
      <xsl:apply-templates select="/" mode="M333"/>
      <xsl:apply-templates select="/" mode="M334"/>
      <xsl:apply-templates select="/" mode="M335"/>
      <xsl:apply-templates select="/" mode="M336"/>
      <xsl:apply-templates select="/" mode="M337"/>
      <xsl:apply-templates select="/" mode="M338"/>
      <xsl:apply-templates select="/" mode="M339"/>
      <xsl:apply-templates select="/" mode="M340"/>
      <xsl:apply-templates select="/" mode="M341"/>
      <xsl:apply-templates select="/" mode="M342"/>
      <xsl:apply-templates select="/" mode="M343"/>
      <xsl:apply-templates select="/" mode="M344"/>
      <xsl:apply-templates select="/" mode="M345"/>
      <xsl:apply-templates select="/" mode="M346"/>
      <xsl:apply-templates select="/" mode="M347"/>
      <xsl:apply-templates select="/" mode="M348"/>
      <xsl:apply-templates select="/" mode="M349"/>
      <xsl:apply-templates select="/" mode="M350"/>
      <xsl:apply-templates select="/" mode="M351"/>
      <xsl:apply-templates select="/" mode="M352"/>
      <xsl:apply-templates select="/" mode="M353"/>
      <xsl:apply-templates select="/" mode="M354"/>
      <xsl:apply-templates select="/" mode="M355"/>
      <xsl:apply-templates select="/" mode="M356"/>
      <xsl:apply-templates select="/" mode="M357"/>
      <xsl:apply-templates select="/" mode="M358"/>
      <xsl:apply-templates select="/" mode="M359"/>
      <xsl:apply-templates select="/" mode="M360"/>
      <xsl:apply-templates select="/" mode="M361"/>
      <xsl:apply-templates select="/" mode="M362"/>
      <xsl:apply-templates select="/" mode="M363"/>
      <xsl:apply-templates select="/" mode="M364"/>
      <xsl:apply-templates select="/" mode="M365"/>
      <xsl:apply-templates select="/" mode="M366"/>
      <xsl:apply-templates select="/" mode="M367"/>
      <xsl:apply-templates select="/" mode="M368"/>
      <xsl:apply-templates select="/" mode="M369"/>
      <xsl:apply-templates select="/" mode="M370"/>
      <xsl:apply-templates select="/" mode="M371"/>
      <xsl:apply-templates select="/" mode="M372"/>
      <xsl:apply-templates select="/" mode="M373"/>
      <xsl:apply-templates select="/" mode="M374"/>
      <xsl:apply-templates select="/" mode="M375"/>
      <xsl:apply-templates select="/" mode="M376"/>
      <xsl:apply-templates select="/" mode="M377"/>
      <xsl:apply-templates select="/" mode="M378"/>
      <xsl:apply-templates select="/" mode="M379"/>
      <xsl:apply-templates select="/" mode="M380"/>
      <xsl:apply-templates select="/" mode="M381"/>
      <xsl:apply-templates select="/" mode="M382"/>
      <xsl:apply-templates select="/" mode="M383"/>
      <xsl:apply-templates select="/" mode="M384"/>
      <xsl:apply-templates select="/" mode="M385"/>
      <xsl:apply-templates select="/" mode="M386"/>
      <xsl:apply-templates select="/" mode="M387"/>
      <xsl:apply-templates select="/" mode="M388"/>
      <xsl:apply-templates select="/" mode="M389"/>
      <xsl:apply-templates select="/" mode="M390"/>
      <xsl:apply-templates select="/" mode="M391"/>
      <xsl:apply-templates select="/" mode="M392"/>
      <xsl:apply-templates select="/" mode="M393"/>
      <xsl:apply-templates select="/" mode="M394"/>
      <xsl:apply-templates select="/" mode="M395"/>
      <xsl:apply-templates select="/" mode="M396"/>
      <xsl:apply-templates select="/" mode="M397"/>
      <xsl:apply-templates select="/" mode="M398"/>
      <xsl:apply-templates select="/" mode="M399"/>
      <xsl:apply-templates select="/" mode="M400"/>
      <xsl:apply-templates select="/" mode="M401"/>
      <xsl:apply-templates select="/" mode="M402"/>
      <xsl:apply-templates select="/" mode="M403"/>
      <xsl:apply-templates select="/" mode="M404"/>
      <xsl:apply-templates select="/" mode="M405"/>
      <xsl:apply-templates select="/" mode="M406"/>
      <xsl:apply-templates select="/" mode="M407"/>
      <xsl:apply-templates select="/" mode="M408"/>
      <xsl:apply-templates select="/" mode="M409"/>
      <xsl:apply-templates select="/" mode="M410"/>
      <xsl:apply-templates select="/" mode="M411"/>
      <xsl:apply-templates select="/" mode="M412"/>
      <xsl:apply-templates select="/" mode="M413"/>
      <xsl:apply-templates select="/" mode="M414"/>
      <xsl:apply-templates select="/" mode="M415"/>
      <xsl:apply-templates select="/" mode="M416"/>
      <xsl:apply-templates select="/" mode="M417"/>
      <xsl:apply-templates select="/" mode="M418"/>
      <xsl:apply-templates select="/" mode="M419"/>
      <xsl:apply-templates select="/" mode="M420"/>
      <xsl:apply-templates select="/" mode="M421"/>
      <xsl:apply-templates select="/" mode="M422"/>
      <xsl:apply-templates select="/" mode="M423"/>
      <xsl:apply-templates select="/" mode="M424"/>
      <xsl:apply-templates select="/" mode="M425"/>
      <xsl:apply-templates select="/" mode="M426"/>
      <xsl:apply-templates select="/" mode="M427"/>
      <xsl:apply-templates select="/" mode="M428"/>
      <xsl:apply-templates select="/" mode="M429"/>
      <xsl:apply-templates select="/" mode="M430"/>
      <xsl:apply-templates select="/" mode="M431"/>
      <xsl:apply-templates select="/" mode="M432"/>
      <xsl:apply-templates select="/" mode="M433"/>
      <xsl:apply-templates select="/" mode="M434"/>
      <xsl:apply-templates select="/" mode="M435"/>
      <xsl:apply-templates select="/" mode="M436"/>
      <xsl:apply-templates select="/" mode="M437"/>
      <xsl:apply-templates select="/" mode="M438"/>
      <xsl:apply-templates select="/" mode="M439"/>
      <xsl:apply-templates select="/" mode="M440"/>
      <xsl:apply-templates select="/" mode="M441"/>
      <xsl:apply-templates select="/" mode="M442"/>
      <xsl:apply-templates select="/" mode="M443"/>
      <xsl:apply-templates select="/" mode="M444"/>
      <xsl:apply-templates select="/" mode="M445"/>
      <xsl:apply-templates select="/" mode="M446"/>
      <xsl:apply-templates select="/" mode="M447"/>
      <xsl:apply-templates select="/" mode="M448"/>
      <xsl:apply-templates select="/" mode="M449"/>
      <xsl:apply-templates select="/" mode="M450"/>
      <xsl:apply-templates select="/" mode="M451"/>
      <xsl:apply-templates select="/" mode="M452"/>
      <xsl:apply-templates select="/" mode="M453"/>
      <xsl:apply-templates select="/" mode="M454"/>
      <xsl:apply-templates select="/" mode="M455"/>
      <xsl:apply-templates select="/" mode="M456"/>
      <xsl:apply-templates select="/" mode="M457"/>
      <xsl:apply-templates select="/" mode="M458"/>
      <xsl:apply-templates select="/" mode="M459"/>
      <xsl:apply-templates select="/" mode="M460"/>
      <xsl:apply-templates select="/" mode="M461"/>
      <xsl:apply-templates select="/" mode="M462"/>
      <xsl:apply-templates select="/" mode="M463"/>
      <xsl:apply-templates select="/" mode="M464"/>
      <xsl:apply-templates select="/" mode="M465"/>
      <xsl:apply-templates select="/" mode="M466"/>
      <xsl:apply-templates select="/" mode="M467"/>
      <xsl:apply-templates select="/" mode="M468"/>
      <xsl:apply-templates select="/" mode="M469"/>
      <xsl:apply-templates select="/" mode="M470"/>
      <xsl:apply-templates select="/" mode="M471"/>
      <xsl:apply-templates select="/" mode="M472"/>
      <xsl:apply-templates select="/" mode="M473"/>
      <xsl:apply-templates select="/" mode="M474"/>
      <xsl:apply-templates select="/" mode="M475"/>
      <xsl:apply-templates select="/" mode="M476"/>
      <xsl:apply-templates select="/" mode="M477"/>
      <xsl:apply-templates select="/" mode="M478"/>
      <xsl:apply-templates select="/" mode="M479"/>
      <xsl:apply-templates select="/" mode="M480"/>
      <xsl:apply-templates select="/" mode="M481"/>
      <xsl:apply-templates select="/" mode="M482"/>
      <xsl:apply-templates select="/" mode="M483"/>
      <xsl:apply-templates select="/" mode="M484"/>
      <xsl:apply-templates select="/" mode="M485"/>
      <xsl:apply-templates select="/" mode="M486"/>
      <xsl:apply-templates select="/" mode="M487"/>
      <xsl:apply-templates select="/" mode="M488"/>
      <xsl:apply-templates select="/" mode="M489"/>
      <xsl:apply-templates select="/" mode="M490"/>
      <xsl:apply-templates select="/" mode="M491"/>
      <xsl:apply-templates select="/" mode="M492"/>
      <xsl:apply-templates select="/" mode="M493"/>
      <xsl:apply-templates select="/" mode="M494"/>
      <xsl:apply-templates select="/" mode="M495"/>
      <xsl:apply-templates select="/" mode="M496"/>
      <xsl:apply-templates select="/" mode="M497"/>
      <xsl:apply-templates select="/" mode="M498"/>
      <xsl:apply-templates select="/" mode="M499"/>
      <xsl:apply-templates select="/" mode="M500"/>
      <xsl:apply-templates select="/" mode="M501"/>
      <xsl:apply-templates select="/" mode="M502"/>
      <xsl:apply-templates select="/" mode="M503"/>
      <xsl:apply-templates select="/" mode="M504"/>
      <xsl:apply-templates select="/" mode="M505"/>
      <xsl:apply-templates select="/" mode="M506"/>
   </xsl:template>

   <!--SCHEMATRON PATTERNS-->


<!--PATTERN oval_none_exist_value_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:tests/child::*" priority="1000"
                 mode="M36">

		<!--REPORT -->
<xsl:if test="@check='none exist'">
                                             DEPRECATED ATTRIBUTE VALUE IN: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ATTRIBUTE VALUE:
                                        <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M36"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M36"/>
   <xsl:template match="@*|node()" priority="-2" mode="M36">
      <xsl:apply-templates select="*" mode="M36"/>
   </xsl:template>

   <!--PATTERN oval-def_empty_def_doc-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions" priority="1000" mode="M37">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="oval-def:definitions or oval-def:tests or oval-def:objects or oval-def:states or oval-def:variables"/>
         <xsl:otherwise>A valid OVAL Definition document must contain at least one definitions, tests, objects, states, or variables element. The optional definitions, tests, objects, states, and variables sections define the specific characteristics that should be evaluated on a system to determine the truth values of the OVAL Definition Document. To be valid though, at least one definitions, tests, objects, states, or variables element must be present.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M37"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M37"/>
   <xsl:template match="@*|node()" priority="-2" mode="M37">
      <xsl:apply-templates select="*" mode="M37"/>
   </xsl:template>

   <!--PATTERN oval-def_required_criteria-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:definitions/oval-def:definition[(@deprecated='false' or @deprecated='0') or not(@deprecated)]"
                 priority="1000"
                 mode="M38">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="oval-def:criteria"/>
         <xsl:otherwise>A valid OVAL Definition must contain a criteria unless the definition is a deprecated definition.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M38"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M38"/>
   <xsl:template match="@*|node()" priority="-2" mode="M38">
      <xsl:apply-templates select="*" mode="M38"/>
   </xsl:template>

   <!--PATTERN oval-def_test_type-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:tests/*[@check_existence='none_exist']"
                 priority="1000"
                 mode="M39">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(*[local-name()='state'])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="@id"/>
            <xsl:text/> - No state should be referenced when check_existence has a value of 'none_exist'.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M39"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M39"/>
   <xsl:template match="@*|node()" priority="-2" mode="M39">
      <xsl:apply-templates select="*" mode="M39"/>
   </xsl:template>

   <!--PATTERN oval-def_setobjref-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:objects/*/oval-def:set/oval-def:object_reference"
                 priority="1002"
                 mode="M40">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="name(./../..) = name(ancestor::oval-def:oval_definitions/oval-def:objects/*[@id=current()])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../../@id"/>
            <xsl:text/> - Each object referenced by the set must be of the same type as parent object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M40"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:objects/*/oval-def:set/oval-def:set/oval-def:object_reference"
                 priority="1001"
                 mode="M40">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="name(./../../..) = name(ancestor::oval-def:oval_definitions/oval-def:objects/*[@id=current()])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../../../@id"/>
            <xsl:text/> - Each object referenced by the set must be of the same type as parent object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M40"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:objects/*/oval-def:set/oval-def:set/oval-def:set/oval-def:object_reference"
                 priority="1000"
                 mode="M40">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="name(./../../../..) = name(ancestor::oval-def:oval_definitions/oval-def:objects/*[@id=current()])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../../../../@id"/>
            <xsl:text/> - Each object referenced by the set must be of the same type as parent object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M40"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M40"/>
   <xsl:template match="@*|node()" priority="-2" mode="M40">
      <xsl:apply-templates select="*" mode="M40"/>
   </xsl:template>

   <!--PATTERN oval-def_literal_component-->


	<!--RULE -->
<xsl:template match="oval-def:literal_component" priority="1000" mode="M41">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@datatype='record')"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="ancestor::*/@id"/>
            <xsl:text/> - The 'record' datatype is prohibited on variables.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M41"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M41"/>
   <xsl:template match="@*|node()" priority="-2" mode="M41">
      <xsl:apply-templates select="*" mode="M41"/>
   </xsl:template>

   <!--PATTERN oval-def_arithmeticfunctionrules-->


	<!--RULE -->
<xsl:template match="oval-def:arithmetic/oval-def:literal_component" priority="1001"
                 mode="M42">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@datatype='float' or @datatype='int'"/>
         <xsl:otherwise>A literal_component used by an arithmetic function must have a datatype of float or int.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M42"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:arithmetic/oval-def:variable_component" priority="1000"
                 mode="M42">
      <xsl:variable name="var_ref" select="@var_ref"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype='float' or ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype='int'"/>
         <xsl:otherwise>The variable referenced by the arithmetic function must have a datatype of float or int.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M42"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M42"/>
   <xsl:template match="@*|node()" priority="-2" mode="M42">
      <xsl:apply-templates select="*" mode="M42"/>
   </xsl:template>

   <!--PATTERN oval-def_beginfunctionrules-->


	<!--RULE -->
<xsl:template match="oval-def:begin/oval-def:literal_component" priority="1001" mode="M43">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@datatype) or @datatype='string'"/>
         <xsl:otherwise>A literal_component used by the begin function must have a datatype of string.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M43"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:begin/oval-def:variable_component" priority="1000" mode="M43">
      <xsl:variable name="var_ref" select="@var_ref"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype = 'string'"/>
         <xsl:otherwise>The variable referenced by the begin function must have a datatype of string.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M43"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M43"/>
   <xsl:template match="@*|node()" priority="-2" mode="M43">
      <xsl:apply-templates select="*" mode="M43"/>
   </xsl:template>

   <!--PATTERN oval-def_concatfunctionrules-->


	<!--RULE -->
<xsl:template match="oval-def:concat/oval-def:literal_component" priority="1001" mode="M44">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@datatype) or @datatype='string'"/>
         <xsl:otherwise>A literal_component used by the concat function must have a datatype of string.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M44"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:concat/oval-def:variable_component" priority="1000" mode="M44">
      <xsl:variable name="var_ref" select="@var_ref"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype = 'string'"/>
         <xsl:otherwise>The variable referenced by the concat function must have a datatype of string.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M44"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M44"/>
   <xsl:template match="@*|node()" priority="-2" mode="M44">
      <xsl:apply-templates select="*" mode="M44"/>
   </xsl:template>

   <!--PATTERN oval-def_endfunctionrules-->


	<!--RULE -->
<xsl:template match="oval-def:end/oval-def:literal_component" priority="1001" mode="M45">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@datatype) or @datatype='string'"/>
         <xsl:otherwise>A literal_component used by the end function must have a datatype of string.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M45"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:end/oval-def:variable_component" priority="1000" mode="M45">
      <xsl:variable name="var_ref" select="@var_ref"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype = 'string'"/>
         <xsl:otherwise>The variable referenced by the end function must have a datatype of string.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M45"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M45"/>
   <xsl:template match="@*|node()" priority="-2" mode="M45">
      <xsl:apply-templates select="*" mode="M45"/>
   </xsl:template>

   <!--PATTERN oval-def_escaperegexfunctionrules-->


	<!--RULE -->
<xsl:template match="oval-def:escape_regex/oval-def:literal_component" priority="1001"
                 mode="M46">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@datatype) or @datatype='string'"/>
         <xsl:otherwise>A literal_component used by the escape_regex function must have a datatype of string.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M46"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:escape_regex/oval-def:variable_component" priority="1000"
                 mode="M46">
      <xsl:variable name="var_ref" select="@var_ref"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype = 'string'"/>
         <xsl:otherwise>The variable referenced by the escape_regex function must have a datatype of string.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M46"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M46"/>
   <xsl:template match="@*|node()" priority="-2" mode="M46">
      <xsl:apply-templates select="*" mode="M46"/>
   </xsl:template>

   <!--PATTERN oval-def_splitfunctionrules-->


	<!--RULE -->
<xsl:template match="oval-def:split/oval-def:literal_component" priority="1001" mode="M47">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@datatype) or @datatype='string'"/>
         <xsl:otherwise>A literal_component used by the split function must have a datatype of string.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M47"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:split/oval-def:variable_component" priority="1000" mode="M47">
      <xsl:variable name="var_ref" select="@var_ref"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype = 'string'"/>
         <xsl:otherwise>The variable referenced by the split function must have a datatype of string.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M47"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M47"/>
   <xsl:template match="@*|node()" priority="-2" mode="M47">
      <xsl:apply-templates select="*" mode="M47"/>
   </xsl:template>

   <!--PATTERN oval-def_substringfunctionrules-->


	<!--RULE -->
<xsl:template match="oval-def:substring/oval-def:literal_component" priority="1001"
                 mode="M48">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@datatype) or @datatype='string'"/>
         <xsl:otherwise>A literal_component used by the substring function must have a datatype of string.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M48"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:substring/oval-def:variable_component" priority="1000"
                 mode="M48">
      <xsl:variable name="var_ref" select="@var_ref"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype = 'string'"/>
         <xsl:otherwise>The variable referenced by the substring function must have a datatype of string.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M48"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M48"/>
   <xsl:template match="@*|node()" priority="-2" mode="M48">
      <xsl:apply-templates select="*" mode="M48"/>
   </xsl:template>

   <!--PATTERN oval-def_timedifferencefunctionrules-->


	<!--RULE -->
<xsl:template match="oval-def:time_difference/oval-def:literal_component" priority="1001"
                 mode="M49">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@datatype) or @datatype='string' or @datatype='int'"/>
         <xsl:otherwise>A literal_component used by the time_difference function must have a datatype of string or int.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M49"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:time_difference/oval-def:variable_component" priority="1000"
                 mode="M49">
      <xsl:variable name="var_ref" select="@var_ref"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype='string' or ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype='int'"/>
         <xsl:otherwise>The variable referenced by the time_difference function must have a datatype of string or int.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M49"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M49"/>
   <xsl:template match="@*|node()" priority="-2" mode="M49">
      <xsl:apply-templates select="*" mode="M49"/>
   </xsl:template>

   <!--PATTERN oval-def_regexcapturefunctionrules-->


	<!--RULE -->
<xsl:template match="oval-def:regex_capture/oval-def:literal_component" priority="1001"
                 mode="M50">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@datatype) or @datatype='string'"/>
         <xsl:otherwise>A literal_component used by the regex_capture function must have a datatype of string.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M50"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:regex_capture/oval-def:variable_component" priority="1000"
                 mode="M50">
      <xsl:variable name="var_ref" select="@var_ref"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype = 'string'"/>
         <xsl:otherwise>The variable referenced by the regex_capture function must have a datatype of string.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M50"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M50"/>
   <xsl:template match="@*|node()" priority="-2" mode="M50">
      <xsl:apply-templates select="*" mode="M50"/>
   </xsl:template>

   <!--PATTERN oval-def_definition_entity_rules-->


	<!--RULE -->
<xsl:template match="oval-def:objects/*/*[@var_ref]|oval-def:objects/*/*/*[@var_ref]|oval-def:states/*/*[@var_ref]|oval-def:states/*/*/*[@var_ref]"
                 priority="1017"
                 mode="M51">
      <xsl:variable name="var_ref" select="@var_ref"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test=".=''"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - a var_ref has been supplied for the <xsl:text/>
            <xsl:value-of select="name()"/>
            <xsl:text/> entity so no value should be provided<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="( (not(@datatype)) and ('string' = ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype) ) or (@datatype = ancestor::oval-def:oval_definitions/oval-def:variables/*[@id=$var_ref]/@datatype)"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="$var_ref"/>
            <xsl:text/> - inconsistent datatype between the variable and an associated var_ref<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M51"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:objects/*/*[@var_ref]|oval-def:objects/*/*/*[@var_ref]"
                 priority="1016"
                 mode="M51">

		<!--REPORT -->
<xsl:if test="not(@var_check)">
            <xsl:text/>
         <xsl:value-of select="../@id"/>
         <xsl:text/> - a var_ref has been supplied for the <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> entity so a var_check should also be provided<xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M51"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:objects/*/*[@var_check]|oval-def:objects/*/*/*[@var_check]"
                 priority="1015"
                 mode="M51">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@var_ref"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - a var_check has been supplied for the <xsl:text/>
            <xsl:value-of select="name()"/>
            <xsl:text/> entity so a var_ref must also be provided<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M51"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:states/*/*[@var_ref]|oval-def:states/*/*/*[@var_ref]"
                 priority="1014"
                 mode="M51">

		<!--REPORT -->
<xsl:if test="not(@var_check)">
            <xsl:text/>
         <xsl:value-of select="../@id"/>
         <xsl:text/> - a var_ref has been supplied for the <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> entity so a var_check should also be provided<xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M51"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:states/*/*[@var_check]|oval-def:states/*/*/*[@var_check]"
                 priority="1013"
                 mode="M51">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@var_ref"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - a var_check has been supplied for the <xsl:text/>
            <xsl:value-of select="name()"/>
            <xsl:text/> entity so a var_ref must also be provided<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M51"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:objects/*/*[not(@datatype)]|oval-def:objects/*/*/*[not(@datatype)]|oval-def:states/*/*[not(@datatype)]|oval-def:states/*/*/*[not(@datatype)]"
                 priority="1012"
                 mode="M51">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals' or @operation='not equal' or @operation='case insensitive equals' or @operation='case insensitive not equal' or @operation='pattern match'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - The use of '<xsl:text/>
            <xsl:value-of select="@operation"/>
            <xsl:text/>' for the operation attribute of the <xsl:text/>
            <xsl:value-of select="name()"/>
            <xsl:text/> entity is not valid given the lack of a declared datatype (hence a default datatype of string).<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M51"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:objects/*/*[@datatype='binary']|oval-def:objects/*/*/*[@datatype='binary']|oval-def:states/*/*[@datatype='binary']|oval-def:states/*/*/*[@datatype='binary']"
                 priority="1011"
                 mode="M51">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals' or @operation='not equal'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - The use of '<xsl:text/>
            <xsl:value-of select="@operation"/>
            <xsl:text/>' for the operation attribute of the <xsl:text/>
            <xsl:value-of select="name()"/>
            <xsl:text/> entity is not valid given a datatype of binary.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M51"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:objects/*/*[@datatype='boolean']|oval-def:objects/*/*/*[@datatype='boolean']|oval-def:states/*/*[@datatype='boolean']|oval-def:states/*/*/*[@datatype='boolean']"
                 priority="1010"
                 mode="M51">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals' or @operation='not equal'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - The use of '<xsl:text/>
            <xsl:value-of select="@operation"/>
            <xsl:text/>' for the operation attribute of the <xsl:text/>
            <xsl:value-of select="name()"/>
            <xsl:text/> entity is not valid given a datatype of boolean.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M51"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:objects/*/*[@datatype='evr_string']|oval-def:objects/*/*/*[@datatype='evr_string']|oval-def:states/*/*[@datatype='evr_string']|oval-def:states/*/*/*[@datatype='evr_string']"
                 priority="1009"
                 mode="M51">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals' or @operation='not equal' or  @operation='greater than' or @operation='greater than or equal' or @operation='less than' or @operation='less than or equal'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - The use of '<xsl:text/>
            <xsl:value-of select="@operation"/>
            <xsl:text/>' for the operation attribute of the <xsl:text/>
            <xsl:value-of select="name()"/>
            <xsl:text/> entity is not valid given a datatype of evr_string.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M51"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:objects/*/*[@datatype='fileset_revision']|oval-def:objects/*/*/*[@datatype='fileset_revision']|oval-def:states/*/*[@datatype='fileset_revision']|oval-def:states/*/*/*[@datatype='fileset_revision']"
                 priority="1008"
                 mode="M51">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals' or @operation='not equal' or  @operation='greater than' or @operation='greater than or equal' or @operation='less than' or @operation='less than or equal'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - The use of '<xsl:text/>
            <xsl:value-of select="@operation"/>
            <xsl:text/>' for the operation attribute of the <xsl:text/>
            <xsl:value-of select="name()"/>
            <xsl:text/> entity is not valid given a datatype of fileset_revision.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M51"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:objects/*/*[@datatype='float']|oval-def:objects/*/*/*[@datatype='float']|oval-def:states/*/*[@datatype='float']|oval-def:states/*/*/*[@datatype='float']"
                 priority="1007"
                 mode="M51">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals' or @operation='not equal' or @operation='greater than' or @operation='greater than or equal' or @operation='less than' or @operation='less than or equal'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - The use of '<xsl:text/>
            <xsl:value-of select="@operation"/>
            <xsl:text/>' for the operation attribute of the <xsl:text/>
            <xsl:value-of select="name()"/>
            <xsl:text/> entity is not valid given a datatype of float.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M51"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:objects/*/*[@datatype='ios_version']|oval-def:objects/*/*/*[@datatype='ios_version']|oval-def:states/*/*[@datatype='ios_version']|oval-def:states/*/*/*[@datatype='ios_version']"
                 priority="1006"
                 mode="M51">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals' or @operation='not equal' or @operation='greater than' or @operation='greater than or equal' or @operation='less than' or @operation='less than or equal'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - The use of '<xsl:text/>
            <xsl:value-of select="@operation"/>
            <xsl:text/>' for the operation attribute of the <xsl:text/>
            <xsl:value-of select="name()"/>
            <xsl:text/> entity is not valid given a datatype of ios_version.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M51"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:objects/*/*[@datatype='int']|oval-def:objects/*/*/*[@datatype='int']|oval-def:states/*/*[@datatype='int']|oval-def:states/*/*/*[@datatype='int']"
                 priority="1005"
                 mode="M51">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals' or @operation='not equal' or @operation='greater than' or @operation='greater than or equal' or @operation='less than' or @operation='less than or equal' or @operation='bitwise and' or @operation='bitwise or'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - The use of '<xsl:text/>
            <xsl:value-of select="@operation"/>
            <xsl:text/>' for the operation attribute of the <xsl:text/>
            <xsl:value-of select="name()"/>
            <xsl:text/> entity is not valid given a datatype of int.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M51"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:objects/*/*[@datatype='ipv4_address']|oval-def:objects/*/*/*[@datatype='ipv4_address']|oval-def:states/*/*[@datatype='ipv4_address']|oval-def:states/*/*/*[@datatype='ipv4_address']"
                 priority="1004"
                 mode="M51">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals' or @operation='not equal' or @operation='greater than' or @operation='greater than or equal' or @operation='less than' or @operation='less than or equal' or @operation='subset of' or @operation='superset of'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - The use of '<xsl:text/>
            <xsl:value-of select="@operation"/>
            <xsl:text/>' for the operation attribute of the <xsl:text/>
            <xsl:value-of select="name()"/>
            <xsl:text/> entity is not valid given a datatype of ipv4_address.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M51"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:objects/*/*[@datatype='ipv6_address']|oval-def:objects/*/*/*[@datatype='ipv6_address']|oval-def:states/*/*[@datatype='ipv6_address']|oval-def:states/*/*/*[@datatype='ipv6_address']"
                 priority="1003"
                 mode="M51">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals' or @operation='not equal' or @operation='greater than' or @operation='greater than or equal' or @operation='less than' or @operation='less than or equal' or @operation='subset of' or @operation='superset of'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - The use of '<xsl:text/>
            <xsl:value-of select="@operation"/>
            <xsl:text/>' for the operation attribute of the <xsl:text/>
            <xsl:value-of select="name()"/>
            <xsl:text/> entity is not valid given a datatype of ipv6_address.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M51"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:objects/*/*[@datatype='string']|oval-def:objects/*/*/*[@datatype='string']|oval-def:states/*/*[@datatype='string']|oval-def:states/*/*/*[@datatype='string']"
                 priority="1002"
                 mode="M51">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals' or @operation='not equal' or @operation='case insensitive equals' or @operation='case insensitive not equal' or @operation='pattern match'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - The use of '<xsl:text/>
            <xsl:value-of select="@operation"/>
            <xsl:text/>' for the operation attribute of the <xsl:text/>
            <xsl:value-of select="name()"/>
            <xsl:text/> entity is not valid given a datatype of string.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M51"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:objects/*/*[@datatype='version']|oval-def:objects/*/*/*[@datatype='version']|oval-def:states/*/*[@datatype='version']|oval-def:states/*/*/*[@datatype='version']"
                 priority="1001"
                 mode="M51">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals' or @operation='not equal' or @operation='greater than' or @operation='greater than or equal' or @operation='less than' or @operation='less than or equal'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - The use of '<xsl:text/>
            <xsl:value-of select="@operation"/>
            <xsl:text/>' for the operation attribute of the <xsl:text/>
            <xsl:value-of select="name()"/>
            <xsl:text/> entity is not valid given a datatype of version.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M51"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="oval-def:objects/*/*[@datatype='record']|oval-def:states/*/*[@datatype='record']"
                 priority="1000"
                 mode="M51">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - The use of '<xsl:text/>
            <xsl:value-of select="@operation"/>
            <xsl:text/>' for the operation attribute of the <xsl:text/>
            <xsl:value-of select="name()"/>
            <xsl:text/> entity is not valid given a datatype of record.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M51"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M51"/>
   <xsl:template match="@*|node()" priority="-2" mode="M51">
      <xsl:apply-templates select="*" mode="M51"/>
   </xsl:template>

   <!--PATTERN oval-def_no_var_ref_with_records-->


	<!--RULE -->
<xsl:template match="oval-def:objects/*/*[@datatype='record']|oval-def:states/*/*[@datatype='record']"
                 priority="1000"
                 mode="M52">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@var_ref)"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - The use of var_ref is prohibited when the datatype is 'record'.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M52"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M52"/>
   <xsl:template match="@*|node()" priority="-2" mode="M52">
      <xsl:apply-templates select="*" mode="M52"/>
   </xsl:template>

   <!--PATTERN oval-def_definition_entity_type_check_rules-->


	<!--RULE -->
<xsl:template match="oval-def:objects/*/*[not((@xsi:nil='1' or @xsi:nil='true')) and not(@var_ref) and @datatype='int']|oval-def:objects/*/*/*[not(@var_ref) and @datatype='int']|oval-def:states/*/*[not((@xsi:nil='1' or @xsi:nil='true')) and not(@var_ref) and @datatype='int']|oval-def:states/*/*/*[not(@var_ref) and @datatype='int']"
                 priority="1000"
                 mode="M53">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="(not(contains(.,'.'))) and (number(.) = floor(.))"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - The datatype for the <xsl:text/>
            <xsl:value-of select="name()"/>
            <xsl:text/> entity is 'int' but the value is not an integer.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M53"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M53"/>
   <xsl:template match="@*|node()" priority="-2" mode="M53">
      <xsl:apply-templates select="*" mode="M53"/>
   </xsl:template>

   <!--PATTERN ind-def_famtst-->


	<!--RULE -->
<xsl:template match="ind-def:family_test/ind-def:object" priority="1001" mode="M54">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/ind-def:family_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a family_test must reference a family_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M54"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="ind-def:family_test/ind-def:state" priority="1000" mode="M54">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/ind-def:family_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a family_test must reference a family_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M54"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M54"/>
   <xsl:template match="@*|node()" priority="-2" mode="M54">
      <xsl:apply-templates select="*" mode="M54"/>
   </xsl:template>

   <!--PATTERN ind-def_filehash_test_dep-->


	<!--RULE -->
<xsl:template match="ind-def:filehash_test" priority="1000" mode="M55">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M55"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M55"/>
   <xsl:template match="@*|node()" priority="-2" mode="M55">
      <xsl:apply-templates select="*" mode="M55"/>
   </xsl:template>

   <!--PATTERN ind-def_hashtst-->


	<!--RULE -->
<xsl:template match="ind-def:filehash_test/ind-def:object" priority="1001" mode="M56">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/ind-def:filehash_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a filehash_test must reference a filesha1_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M56"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="ind-def:filehash_test/ind-def:state" priority="1000" mode="M56">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/ind-def:filehash_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a filehash_test must reference a filesha1_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M56"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M56"/>
   <xsl:template match="@*|node()" priority="-2" mode="M56">
      <xsl:apply-templates select="*" mode="M56"/>
   </xsl:template>

   <!--PATTERN ind-def_filehash_object_dep-->


	<!--RULE -->
<xsl:template match="ind-def:filehash_object" priority="1000" mode="M57">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED OBJECT: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M57"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M57"/>
   <xsl:template match="@*|node()" priority="-2" mode="M57">
      <xsl:apply-templates select="*" mode="M57"/>
   </xsl:template>

   <!--PATTERN ind-def_hashobjfilepath-->


	<!--RULE -->
<xsl:template match="ind-def:filehash_object/ind-def:filepath" priority="1000" mode="M58">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::ind-def:behaviors[@max_depth or @recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth and recurse_direction behaviors are not allowed with a filepath entity<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M58"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M58"/>
   <xsl:template match="@*|node()" priority="-2" mode="M58">
      <xsl:apply-templates select="*" mode="M58"/>
   </xsl:template>

   <!--PATTERN ind-def_hashobjfilepath2-->


	<!--RULE -->
<xsl:template match="ind-def:filehash_object/ind-def:filepath[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M59">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::ind-def:behaviors[@recurse_file_system='defined'])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_file_system behavior MUST not be set to 'defined' when a pattern match is used with a filepath entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M59"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M59"/>
   <xsl:template match="@*|node()" priority="-2" mode="M59">
      <xsl:apply-templates select="*" mode="M59"/>
   </xsl:template>

   <!--PATTERN ind-def_hashobjpath-->


	<!--RULE -->
<xsl:template match="ind-def:filehash_object/ind-def:path[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M60">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::ind-def:behaviors[@recurse_file_system='defined'])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_file_system behavior MUST not be set to 'defined' when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::ind-def:behaviors[@max_depth])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::ind-def:behaviors[@recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_direction behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::ind-def:behaviors[@recurse])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M60"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M60"/>
   <xsl:template match="@*|node()" priority="-2" mode="M60">
      <xsl:apply-templates select="*" mode="M60"/>
   </xsl:template>

   <!--PATTERN ind-def_hashobjfilename-->


	<!--RULE -->
<xsl:template match="ind-def:filehash_object/ind-def:filename" priority="1000" mode="M61">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="(@var_ref and .='') or not(.='') or (.='' and @operation = 'pattern match')"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - filename entity cannot be empty unless the xsi:nil attribute is set to true or a var_ref is used<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M61"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M61"/>
   <xsl:template match="@*|node()" priority="-2" mode="M61">
      <xsl:apply-templates select="*" mode="M61"/>
   </xsl:template>

   <!--PATTERN ind-def_filehash_state_dep-->


	<!--RULE -->
<xsl:template match="ind-def:filehash_state" priority="1000" mode="M62">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED STATE: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M62"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M62"/>
   <xsl:template match="@*|node()" priority="-2" mode="M62">
      <xsl:apply-templates select="*" mode="M62"/>
   </xsl:template>

   <!--PATTERN ind-def_filehash58_test-->


	<!--RULE -->
<xsl:template match="ind-def:filehash58_test/ind-def:object" priority="1001" mode="M63">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/ind-def:filehash58_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a filehash58_test must reference a filehash58_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M63"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="ind-def:filehash58_test/ind-def:state" priority="1000" mode="M63">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/ind-def:filehash58_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a filehash58_test must reference a filehash58_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M63"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M63"/>
   <xsl:template match="@*|node()" priority="-2" mode="M63">
      <xsl:apply-templates select="*" mode="M63"/>
   </xsl:template>

   <!--PATTERN ind-def_filehash58_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="ind-def:filehash58_object//oval-def:filter" priority="1000" mode="M64">
      <xsl:variable name="parent_object" select="ancestor::ind-def:filehash58_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#independent') and ($state_name='filehash58_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M64"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M64"/>
   <xsl:template match="@*|node()" priority="-2" mode="M64">
      <xsl:apply-templates select="*" mode="M64"/>
   </xsl:template>

   <!--PATTERN ind-def_hash58objfilepath-->


	<!--RULE -->
<xsl:template match="ind-def:filehash58_object/ind-def:filepath" priority="1000" mode="M65">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::ind-def:behaviors[@max_depth or @recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth and recurse_direction behaviors are not allowed with a filepath entity<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M65"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M65"/>
   <xsl:template match="@*|node()" priority="-2" mode="M65">
      <xsl:apply-templates select="*" mode="M65"/>
   </xsl:template>

   <!--PATTERN ind-def_hash58objfilepath2-->


	<!--RULE -->
<xsl:template match="ind-def:filehash58_object/ind-def:filepath[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M66">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::ind-def:behaviors[@recurse_file_system='defined'])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_file_system behavior MUST not be set to 'defined' when a pattern match is used with a filepath entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M66"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M66"/>
   <xsl:template match="@*|node()" priority="-2" mode="M66">
      <xsl:apply-templates select="*" mode="M66"/>
   </xsl:template>

   <!--PATTERN ind-def_hash58objpath-->


	<!--RULE -->
<xsl:template match="ind-def:filehash58_object/ind-def:path[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M67">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::ind-def:behaviors[@recurse_file_system='defined'])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_file_system behavior MUST not be set to 'defined' when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::ind-def:behaviors[@max_depth])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::ind-def:behaviors[@recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_direction behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::ind-def:behaviors[@recurse])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M67"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M67"/>
   <xsl:template match="@*|node()" priority="-2" mode="M67">
      <xsl:apply-templates select="*" mode="M67"/>
   </xsl:template>

   <!--PATTERN ind-def_hash58objfilename-->


	<!--RULE -->
<xsl:template match="ind-def:filehash58_object/ind-def:filename" priority="1000" mode="M68">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="(@var_ref and .='') or not(.='') or (.='' and @operation = 'pattern match')"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - filename entity cannot be empty unless the xsi:nil attribute is set to true or a var_ref is used<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M68"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M68"/>
   <xsl:template match="@*|node()" priority="-2" mode="M68">
      <xsl:apply-templates select="*" mode="M68"/>
   </xsl:template>

   <!--PATTERN ind-def_environmentvariable_test_dep-->


	<!--RULE -->
<xsl:template match="ind-def:environmentvariable_test" priority="1000" mode="M69">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M69"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M69"/>
   <xsl:template match="@*|node()" priority="-2" mode="M69">
      <xsl:apply-templates select="*" mode="M69"/>
   </xsl:template>

   <!--PATTERN ind-def_envtst-->


	<!--RULE -->
<xsl:template match="ind-def:environmentvariable_test/ind-def:object" priority="1001"
                 mode="M70">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/ind-def:environmentvariable_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an environmentvariable_test must reference a environmentvariable_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M70"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="ind-def:environmentvariable_test/ind-def:state" priority="1000"
                 mode="M70">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/ind-def:environmentvariable_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an environmentvariable_test must reference a environmentvariable_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M70"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M70"/>
   <xsl:template match="@*|node()" priority="-2" mode="M70">
      <xsl:apply-templates select="*" mode="M70"/>
   </xsl:template>

   <!--PATTERN ind-def_environmentvariable_object_dep-->


	<!--RULE -->
<xsl:template match="ind-def:environmentvariable_object" priority="1000" mode="M71">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED OBJECT: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M71"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M71"/>
   <xsl:template match="@*|node()" priority="-2" mode="M71">
      <xsl:apply-templates select="*" mode="M71"/>
   </xsl:template>

   <!--PATTERN ind-def_environmentvariable_state_dep-->


	<!--RULE -->
<xsl:template match="ind-def:environmentvariable_state" priority="1000" mode="M72">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED STATE: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M72"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M72"/>
   <xsl:template match="@*|node()" priority="-2" mode="M72">
      <xsl:apply-templates select="*" mode="M72"/>
   </xsl:template>

   <!--PATTERN ind-def_env58tst-->


	<!--RULE -->
<xsl:template match="ind-def:environmentvariable58_test/ind-def:object" priority="1001"
                 mode="M73">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/ind-def:environmentvariable58_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an environmentvariable58_test must reference a environmentvariable58_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M73"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="ind-def:environmentvariable58_test/ind-def:state" priority="1000"
                 mode="M73">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/ind-def:environmentvariable58_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an environmentvariable58_test must reference a environmentvariable58_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M73"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M73"/>
   <xsl:template match="@*|node()" priority="-2" mode="M73">
      <xsl:apply-templates select="*" mode="M73"/>
   </xsl:template>

   <!--PATTERN ind-def_environmentvariable58_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="ind-def:environmentvariable58_object//oval-def:filter" priority="1000"
                 mode="M74">
      <xsl:variable name="parent_object" select="ancestor::ind-def:environmentvariable58_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#independent') and ($state_name='environmentvariable58_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M74"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M74"/>
   <xsl:template match="@*|node()" priority="-2" mode="M74">
      <xsl:apply-templates select="*" mode="M74"/>
   </xsl:template>

   <!--PATTERN ind-def_ldaptst-->


	<!--RULE -->
<xsl:template match="ind-def:ldap_test/ind-def:object" priority="1001" mode="M75">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/ind-def:ldap_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an ldap_test must reference an ldap_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M75"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="ind-def:ldap_test/ind-def:state" priority="1000" mode="M75">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/ind-def:ldap_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an ldap_test must reference an ldap_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M75"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M75"/>
   <xsl:template match="@*|node()" priority="-2" mode="M75">
      <xsl:apply-templates select="*" mode="M75"/>
   </xsl:template>

   <!--PATTERN ind-def_ldap57_test-->


	<!--RULE -->
<xsl:template match="ind-def:ldap57_test/ind-def:object" priority="1001" mode="M76">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/ind-def:ldap57_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an ldap57_test must reference an ldap57_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M76"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="ind-def:ldap57_test/ind-def:state" priority="1000" mode="M76">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/ind-def:ldap57_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an ldap57_test must reference an ldap57_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M76"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M76"/>
   <xsl:template match="@*|node()" priority="-2" mode="M76">
      <xsl:apply-templates select="*" mode="M76"/>
   </xsl:template>

   <!--PATTERN ind-def_ldap57_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="ind-def:ldap57_object//oval-def:filter" priority="1000" mode="M77">
      <xsl:variable name="parent_object" select="ancestor::ind-def:ldap57_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#independent') and ($state_name='ldap57_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M77"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M77"/>
   <xsl:template match="@*|node()" priority="-2" mode="M77">
      <xsl:apply-templates select="*" mode="M77"/>
   </xsl:template>

   <!--PATTERN ind-def_ldap57stevalue-->


	<!--RULE -->
<xsl:template match="ind-def:ldap57_state/ind-def:value" priority="1000" mode="M78">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@datatype='record'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - datatype attribute for the value entity of a ldap57_state must be 'record'<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M78"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M78"/>
   <xsl:template match="@*|node()" priority="-2" mode="M78">
      <xsl:apply-templates select="*" mode="M78"/>
   </xsl:template>

   <!--PATTERN ind-def_sql_test_dep-->


	<!--RULE -->
<xsl:template match="ind-def:sql_test" priority="1000" mode="M79">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M79"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M79"/>
   <xsl:template match="@*|node()" priority="-2" mode="M79">
      <xsl:apply-templates select="*" mode="M79"/>
   </xsl:template>

   <!--PATTERN ind-def_sqltst-->


	<!--RULE -->
<xsl:template match="ind-def:sql_test/ind-def:object" priority="1001" mode="M80">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/ind-def:sql_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a sql_test must reference a sql_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M80"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="ind-def:sql_test/ind-def:state" priority="1000" mode="M80">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/ind-def:sql_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a sql_test must reference a sql_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M80"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M80"/>
   <xsl:template match="@*|node()" priority="-2" mode="M80">
      <xsl:apply-templates select="*" mode="M80"/>
   </xsl:template>

   <!--PATTERN ind-def_sql_object_dep-->


	<!--RULE -->
<xsl:template match="ind-def:sql_object" priority="1000" mode="M81">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED OBJECT: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M81"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M81"/>
   <xsl:template match="@*|node()" priority="-2" mode="M81">
      <xsl:apply-templates select="*" mode="M81"/>
   </xsl:template>

   <!--PATTERN ind-def_sqlobjdengine-->


	<!--RULE -->
<xsl:template match="ind-def:sql_object/ind-def:engine" priority="1000" mode="M82">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the engine entity of an sql_object should be 'equals', note that this overrules the general operation attribute validation (i.e. follow this one)<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M82"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M82"/>
   <xsl:template match="@*|node()" priority="-2" mode="M82">
      <xsl:apply-templates select="*" mode="M82"/>
   </xsl:template>

   <!--PATTERN ind-def_sqlobjversion-->


	<!--RULE -->
<xsl:template match="ind-def:sql_object/ind-def:version" priority="1000" mode="M83">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the version entity of an sql_object should be 'equals', note that this overrules the general operation attribute validation (i.e. follow this one)<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M83"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M83"/>
   <xsl:template match="@*|node()" priority="-2" mode="M83">
      <xsl:apply-templates select="*" mode="M83"/>
   </xsl:template>

   <!--PATTERN ind-def_sqlobjconnection_string-->


	<!--RULE -->
<xsl:template match="ind-def:sql_object/ind-def:connection_string" priority="1000" mode="M84">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the connection_string entity of an sql_object should be 'equals', note that this overrules the general operation attribute validation (i.e. follow this one)<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M84"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M84"/>
   <xsl:template match="@*|node()" priority="-2" mode="M84">
      <xsl:apply-templates select="*" mode="M84"/>
   </xsl:template>

   <!--PATTERN ind-def_sqlobjsql-->


	<!--RULE -->
<xsl:template match="ind-def:sql_object/ind-def:sql" priority="1000" mode="M85">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the sql entity of an sql_object should be 'equals', note that this overrules the general operation attribute validation (i.e. follow this one)<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M85"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M85"/>
   <xsl:template match="@*|node()" priority="-2" mode="M85">
      <xsl:apply-templates select="*" mode="M85"/>
   </xsl:template>

   <!--PATTERN ind-def_sql_state_dep-->


	<!--RULE -->
<xsl:template match="ind-def:sql_state" priority="1000" mode="M86">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED STATE: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M86"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M86"/>
   <xsl:template match="@*|node()" priority="-2" mode="M86">
      <xsl:apply-templates select="*" mode="M86"/>
   </xsl:template>

   <!--PATTERN ind-def_sql57_test-->


	<!--RULE -->
<xsl:template match="ind-def:sql57_test/ind-def:object" priority="1001" mode="M87">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/ind-def:sql57_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a sql57_test must reference a sql57_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M87"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="ind-def:sql57_test/ind-def:state" priority="1000" mode="M87">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/ind-def:sql57_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a sql57_test must reference a sql57_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M87"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M87"/>
   <xsl:template match="@*|node()" priority="-2" mode="M87">
      <xsl:apply-templates select="*" mode="M87"/>
   </xsl:template>

   <!--PATTERN ind-def_sql57_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="ind-def:sql57_object//oval-def:filter" priority="1000" mode="M88">
      <xsl:variable name="parent_object" select="ancestor::ind-def:sql57_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#independent') and ($state_name='sql57_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M88"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M88"/>
   <xsl:template match="@*|node()" priority="-2" mode="M88">
      <xsl:apply-templates select="*" mode="M88"/>
   </xsl:template>

   <!--PATTERN ind-def_sql57_object_dengine-->


	<!--RULE -->
<xsl:template match="ind-def:sql57_object/ind-def:engine" priority="1000" mode="M89">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the engine entity of an sql57_object should be 'equals', note that this overrules the general operation attribute validation (i.e. follow this one)<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M89"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M89"/>
   <xsl:template match="@*|node()" priority="-2" mode="M89">
      <xsl:apply-templates select="*" mode="M89"/>
   </xsl:template>

   <!--PATTERN ind-def_sql57_object_version-->


	<!--RULE -->
<xsl:template match="ind-def:sql57_object/ind-def:version" priority="1000" mode="M90">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the version entity of an sql57_object should be 'equals', note that this overrules the general operation attribute validation (i.e. follow this one)<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M90"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M90"/>
   <xsl:template match="@*|node()" priority="-2" mode="M90">
      <xsl:apply-templates select="*" mode="M90"/>
   </xsl:template>

   <!--PATTERN ind-def_sql57_object_connection_string-->


	<!--RULE -->
<xsl:template match="ind-def:sql57_object/ind-def:connection_string" priority="1000"
                 mode="M91">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the connection_string entity of an sql57_object should be 'equals', note that this overrules the general operation attribute validation (i.e. follow this one)<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M91"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M91"/>
   <xsl:template match="@*|node()" priority="-2" mode="M91">
      <xsl:apply-templates select="*" mode="M91"/>
   </xsl:template>

   <!--PATTERN ind-def_sql57_object_sql-->


	<!--RULE -->
<xsl:template match="ind-def:sql57_object/ind-def:sql" priority="1000" mode="M92">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the sql entity of an sql57_object should be 'equals', note that this overrules the general operation attribute validation (i.e. follow this one)<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M92"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M92"/>
   <xsl:template match="@*|node()" priority="-2" mode="M92">
      <xsl:apply-templates select="*" mode="M92"/>
   </xsl:template>

   <!--PATTERN ind-def_sql57steresult-->


	<!--RULE -->
<xsl:template match="ind-def:sql57_state/ind-def:result" priority="1000" mode="M93">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@datatype='record'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - datatype attribute for the result entity of a sql57_state must be 'record'<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M93"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M93"/>
   <xsl:template match="@*|node()" priority="-2" mode="M93">
      <xsl:apply-templates select="*" mode="M93"/>
   </xsl:template>

   <!--PATTERN ind-def_txt54tst-->


	<!--RULE -->
<xsl:template match="ind-def:textfilecontent54_test/ind-def:object" priority="1001"
                 mode="M94">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/ind-def:textfilecontent54_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a textfilecontent54_test must reference a textfilecontent54_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M94"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="ind-def:textfilecontent54_test/ind-def:state" priority="1000" mode="M94">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/ind-def:textfilecontent54_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a textfilecontent54_test must reference a textfilecontent54_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M94"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M94"/>
   <xsl:template match="@*|node()" priority="-2" mode="M94">
      <xsl:apply-templates select="*" mode="M94"/>
   </xsl:template>

   <!--PATTERN ind-def_textfilecontent54_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="ind-def:textfilecontent54_object//oval-def:filter" priority="1000"
                 mode="M95">
      <xsl:variable name="parent_object" select="ancestor::ind-def:textfilecontent54_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#independent') and ($state_name='textfilecontent54_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M95"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M95"/>
   <xsl:template match="@*|node()" priority="-2" mode="M95">
      <xsl:apply-templates select="*" mode="M95"/>
   </xsl:template>

   <!--PATTERN ind-def_txt54objfilepath-->


	<!--RULE -->
<xsl:template match="ind-def:textfilecontent54_object/ind-def:filepath" priority="1000"
                 mode="M96">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::ind-def:behaviors[@max_depth or @recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth and recurse_direction behaviors are not allowed with a filepath entity<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M96"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M96"/>
   <xsl:template match="@*|node()" priority="-2" mode="M96">
      <xsl:apply-templates select="*" mode="M96"/>
   </xsl:template>

   <!--PATTERN ind-def_txt54objfilepath2-->


	<!--RULE -->
<xsl:template match="ind-def:textfilecontent54_object/ind-def:filepath[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M97">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::ind-def:behaviors[@recurse_file_system='defined'])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_file_system behavior MUST not be set to 'defined' when a pattern match is used with a filepath entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M97"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M97"/>
   <xsl:template match="@*|node()" priority="-2" mode="M97">
      <xsl:apply-templates select="*" mode="M97"/>
   </xsl:template>

   <!--PATTERN ind-def_txt54objpath-->


	<!--RULE -->
<xsl:template match="ind-def:textfilecontent54_object/ind-def:path[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M98">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::ind-def:behaviors[@recurse_file_system='defined'])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_file_system behavior MUST not be set to 'defined' when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::ind-def:behaviors[@max_depth])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::ind-def:behaviors[@recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_direction behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::ind-def:behaviors[@recurse])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M98"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M98"/>
   <xsl:template match="@*|node()" priority="-2" mode="M98">
      <xsl:apply-templates select="*" mode="M98"/>
   </xsl:template>

   <!--PATTERN ind-def_txt54objfilename-->


	<!--RULE -->
<xsl:template match="ind-def:textfilecontent54_object/ind-def:filename" priority="1000"
                 mode="M99">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="(@var_ref and .='') or not(.='') or (.='' and @operation = 'pattern match')"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - filename entity cannot be empty unless the xsi:nil attribute is set to true or a var_ref is used<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M99"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M99"/>
   <xsl:template match="@*|node()" priority="-2" mode="M99">
      <xsl:apply-templates select="*" mode="M99"/>
   </xsl:template>

   <!--PATTERN ind-def_txt54objpattern-->


	<!--RULE -->
<xsl:template match="ind-def:textfilecontent54_object/ind-def:pattern" priority="1000"
                 mode="M100">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@operation='pattern match'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the pattern entity of a textfilecontent54_object should be 'pattern match'<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M100"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M100"/>
   <xsl:template match="@*|node()" priority="-2" mode="M100">
      <xsl:apply-templates select="*" mode="M100"/>
   </xsl:template>

   <!--PATTERN ind-def_txttst_dep-->


	<!--RULE -->
<xsl:template match="ind-def:textfilecontent_test" priority="1000" mode="M101">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M101"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M101"/>
   <xsl:template match="@*|node()" priority="-2" mode="M101">
      <xsl:apply-templates select="*" mode="M101"/>
   </xsl:template>

   <!--PATTERN ind-def_txttst-->


	<!--RULE -->
<xsl:template match="ind-def:textfilecontent_test/ind-def:object" priority="1001" mode="M102">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/ind-def:textfilecontent_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a textfilecontent_test must reference a textfilecontent_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M102"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="ind-def:textfilecontent_test/ind-def:state" priority="1000" mode="M102">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/ind-def:textfilecontent_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a textfilecontent_test must reference a textfilecontent_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M102"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M102"/>
   <xsl:template match="@*|node()" priority="-2" mode="M102">
      <xsl:apply-templates select="*" mode="M102"/>
   </xsl:template>

   <!--PATTERN ind-def_txtobj_dep-->


	<!--RULE -->
<xsl:template match="ind-def:textfilecontent_object" priority="1000" mode="M103">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED OBJECT: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M103"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M103"/>
   <xsl:template match="@*|node()" priority="-2" mode="M103">
      <xsl:apply-templates select="*" mode="M103"/>
   </xsl:template>

   <!--PATTERN ind-def_txtobjfilename-->


	<!--RULE -->
<xsl:template match="ind-def:textfilecontent_object/ind-def:filename" priority="1000"
                 mode="M104">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="(@var_ref and .='') or not(.='') or (.='' and @operation = 'pattern match')"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - filename entity cannot be empty unless the xsi:nil attribute is set to true or a var_ref is used<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M104"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M104"/>
   <xsl:template match="@*|node()" priority="-2" mode="M104">
      <xsl:apply-templates select="*" mode="M104"/>
   </xsl:template>

   <!--PATTERN ind-def_txtobjline-->


	<!--RULE -->
<xsl:template match="ind-def:textfilecontent_object/ind-def:line" priority="1000" mode="M105">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@operation='pattern match'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the line entity of a textfilecontent_object should be 'pattern match'<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M105"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M105"/>
   <xsl:template match="@*|node()" priority="-2" mode="M105">
      <xsl:apply-templates select="*" mode="M105"/>
   </xsl:template>

   <!--PATTERN ind-def_txtste_dep-->


	<!--RULE -->
<xsl:template match="ind-def:textfilecontent_state" priority="1000" mode="M106">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED STATE: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M106"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M106"/>
   <xsl:template match="@*|node()" priority="-2" mode="M106">
      <xsl:apply-templates select="*" mode="M106"/>
   </xsl:template>

   <!--PATTERN ind-def_vattst-->


	<!--RULE -->
<xsl:template match="ind-def:variable_test/ind-def:object" priority="1001" mode="M107">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/ind-def:variable_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a variable_test must reference a variable_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M107"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="ind-def:variable_test/ind-def:state" priority="1000" mode="M107">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/ind-def:variable_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a variable_test must reference a variable_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M107"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M107"/>
   <xsl:template match="@*|node()" priority="-2" mode="M107">
      <xsl:apply-templates select="*" mode="M107"/>
   </xsl:template>

   <!--PATTERN ind-def_variable_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="ind-def:variable_object//oval-def:filter" priority="1000" mode="M108">
      <xsl:variable name="parent_object" select="ancestor::ind-def:variable_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#independent') and ($state_name='variable_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M108"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M108"/>
   <xsl:template match="@*|node()" priority="-2" mode="M108">
      <xsl:apply-templates select="*" mode="M108"/>
   </xsl:template>

   <!--PATTERN ind-def_varobjvar_ref-->


	<!--RULE -->
<xsl:template match="ind-def:variable_object/ind-def:var_ref" priority="1000" mode="M109">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@var_ref)"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - var_ref attribute for the var_ref entity of a variable_object is prohibited.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M109"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M109"/>
   <xsl:template match="@*|node()" priority="-2" mode="M109">
      <xsl:apply-templates select="*" mode="M109"/>
   </xsl:template>

   <!--PATTERN ind-def_varobjvar_ref_exists-->


	<!--RULE -->
<xsl:template match="ind-def:variable_object/ind-def:var_ref" priority="1000" mode="M110">
      <xsl:variable name="varId" select="."/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="ancestor::oval-def:oval_definitions/oval-def:variables/*[@id = $varId]"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - referenced variable <xsl:text/>
            <xsl:value-of select="."/>
            <xsl:text/> not found. The var_ref entity must hold a variable id that exists in the document.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M110"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M110"/>
   <xsl:template match="@*|node()" priority="-2" mode="M110">
      <xsl:apply-templates select="*" mode="M110"/>
   </xsl:template>

   <!--PATTERN ind-def_varstevar_ref-->


	<!--RULE -->
<xsl:template match="ind-def:variable_state/ind-def:var_ref" priority="1000" mode="M111">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@var_ref)"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - var_ref attribute for the var_ref entity of a variable_state is prohibited.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M111"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M111"/>
   <xsl:template match="@*|node()" priority="-2" mode="M111">
      <xsl:apply-templates select="*" mode="M111"/>
   </xsl:template>

   <!--PATTERN ind-def_varstevar_ref_exists-->


	<!--RULE -->
<xsl:template match="ind-def:variable_state/ind-def:var_ref" priority="1000" mode="M112">
      <xsl:variable name="varId" select="."/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="ancestor::oval-def:oval_definitions/oval-def:variables/*[@id =  $varId]"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - referenced variable <xsl:text/>
            <xsl:value-of select="."/>
            <xsl:text/> not found. The var_ref entity must hold a variable id that exists in the document.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M112"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M112"/>
   <xsl:template match="@*|node()" priority="-2" mode="M112">
      <xsl:apply-templates select="*" mode="M112"/>
   </xsl:template>

   <!--PATTERN ind-def_xmltst-->


	<!--RULE -->
<xsl:template match="ind-def:xmlfilecontent_test/ind-def:object" priority="1001" mode="M113">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/ind-def:xmlfilecontent_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a xmlfilecontent_test must reference a xmlfilecontent_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M113"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="ind-def:xmlfilecontent_test/ind-def:state" priority="1000" mode="M113">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/ind-def:xmlfilecontent_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a xmlfilecontent_test must reference a xmlfilecontent_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M113"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M113"/>
   <xsl:template match="@*|node()" priority="-2" mode="M113">
      <xsl:apply-templates select="*" mode="M113"/>
   </xsl:template>

   <!--PATTERN ind-def_xmlfilecontent_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="ind-def:xmlfilecontent_object//oval-def:filter" priority="1000"
                 mode="M114">
      <xsl:variable name="parent_object" select="ancestor::ind-def:xmlfilecontent_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#independent') and ($state_name='xmlfilecontent_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M114"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M114"/>
   <xsl:template match="@*|node()" priority="-2" mode="M114">
      <xsl:apply-templates select="*" mode="M114"/>
   </xsl:template>

   <!--PATTERN ind-def_xmlobjfilepath-->


	<!--RULE -->
<xsl:template match="ind-def:xmlfilecontent_object/ind-def:filepath" priority="1000"
                 mode="M115">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::ind-def:behaviors[@max_depth or @recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth and recurse_direction behaviors are not allowed with a filepath entity<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M115"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M115"/>
   <xsl:template match="@*|node()" priority="-2" mode="M115">
      <xsl:apply-templates select="*" mode="M115"/>
   </xsl:template>

   <!--PATTERN ind-def_xmlobjfilepath2-->


	<!--RULE -->
<xsl:template match="ind-def:xmlfilecontent_object/ind-def:filepath[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M116">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::ind-def:behaviors[@recurse_file_system='defined'])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_file_system behavior MUST not be set to 'defined' when a pattern match is used with a filepath entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M116"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M116"/>
   <xsl:template match="@*|node()" priority="-2" mode="M116">
      <xsl:apply-templates select="*" mode="M116"/>
   </xsl:template>

   <!--PATTERN ind-def_xmlobjpath-->


	<!--RULE -->
<xsl:template match="ind-def:xmlfilecontent_object/ind-def:path[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M117">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::ind-def:behaviors[@recurse_file_system='defined'])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_file_system behavior MUST not be set to 'defined' when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::ind-def:behaviors[@max_depth])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::ind-def:behaviors[@recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_direction behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::ind-def:behaviors[@recurse])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M117"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M117"/>
   <xsl:template match="@*|node()" priority="-2" mode="M117">
      <xsl:apply-templates select="*" mode="M117"/>
   </xsl:template>

   <!--PATTERN ind-def_xmlobjfilename-->


	<!--RULE -->
<xsl:template match="ind-def:xmlfilecontent_object/ind-def:filename" priority="1000"
                 mode="M118">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="(@var_ref and .='') or not(.='') or (.='' and @operation = 'pattern match')"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - filename entity cannot be empty unless the xsi:nil attribute is set to true or a var_ref is used<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M118"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M118"/>
   <xsl:template match="@*|node()" priority="-2" mode="M118">
      <xsl:apply-templates select="*" mode="M118"/>
   </xsl:template>

   <!--PATTERN ind-def_xmlobjxpath-->


	<!--RULE -->
<xsl:template match="ind-def:xmlfilecontent_object/ind-def:xpath" priority="1000" mode="M119">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the xpath entity of a xmlfilecontent_object should be 'equals', note that this overrules the general operation attribute validation (i.e. follow this one)<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M119"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M119"/>
   <xsl:template match="@*|node()" priority="-2" mode="M119">
      <xsl:apply-templates select="*" mode="M119"/>
   </xsl:template>

   <!--PATTERN ind-def_ldaptype_timestamp_value_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:states/ind-def:ldap_state/ind-def:ldaptype|oval-def:oval_definitions/oval-def:states/ind-def:ldap57_state/ind-def:ldaptype"
                 priority="1000"
                 mode="M120">

		<!--REPORT -->
<xsl:if test=".='LDAPTYPE_TIMESTAMP'">
                                                            DEPRECATED ELEMENT VALUE IN: ldap_state ELEMENT VALUE: <xsl:text/>
         <xsl:value-of select="."/>
         <xsl:text/> 
                                                      <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M120"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M120"/>
   <xsl:template match="@*|node()" priority="-2" mode="M120">
      <xsl:apply-templates select="*" mode="M120"/>
   </xsl:template>

   <!--PATTERN ind-def_ldaptype_email_value_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:states/ind-def:ldap_state/ind-def:ldaptype|oval-def:oval_definitions/oval-def:states/ind-def:ldap57_state/ind-def:ldaptype"
                 priority="1000"
                 mode="M121">

		<!--REPORT -->
<xsl:if test=".='LDAPTYPE_EMAIL'">
                                                            DEPRECATED ELEMENT VALUE IN: ldap_state ELEMENT VALUE: <xsl:text/>
         <xsl:value-of select="."/>
         <xsl:text/> 
                                                      <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M121"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M121"/>
   <xsl:template match="@*|node()" priority="-2" mode="M121">
      <xsl:apply-templates select="*" mode="M121"/>
   </xsl:template>

   <!--PATTERN aix-def_interimfixtst-->


	<!--RULE -->
<xsl:template match="aix-def:interim_fix_test/aix-def:object" priority="1001" mode="M122">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/aix-def:interim_fix_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a <xsl:text/>
            <xsl:value-of select="name(..)"/>
            <xsl:text/> must reference a interim_fix_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M122"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="aix-def:interim_fix_test/aix-def:state" priority="1000" mode="M122">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/aix-def:interim_fix_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a <xsl:text/>
            <xsl:value-of select="name(..)"/>
            <xsl:text/> must reference a interim_fix_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M122"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M122"/>
   <xsl:template match="@*|node()" priority="-2" mode="M122">
      <xsl:apply-templates select="*" mode="M122"/>
   </xsl:template>

   <!--PATTERN aix-def_interim_fix_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="aix-def:interim_fix_object//oval-def:filter" priority="1000" mode="M123">
      <xsl:variable name="parent_object" select="ancestor::aix-def:interim_fix_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#aix') and ($state_name='interim_fix_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M123"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M123"/>
   <xsl:template match="@*|node()" priority="-2" mode="M123">
      <xsl:apply-templates select="*" mode="M123"/>
   </xsl:template>

   <!--PATTERN aix-def_filesettst-->


	<!--RULE -->
<xsl:template match="aix-def:fileset_test/aix-def:object" priority="1001" mode="M124">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/aix-def:fileset_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a fileset_test must reference a fileset_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M124"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="aix-def:fileset_test/aix-def:state" priority="1000" mode="M124">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/aix-def:fileset_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a fileset_test must reference a fileset_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M124"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M124"/>
   <xsl:template match="@*|node()" priority="-2" mode="M124">
      <xsl:apply-templates select="*" mode="M124"/>
   </xsl:template>

   <!--PATTERN aix-def_fileset_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="aix-def:fileset_object//oval-def:filter" priority="1000" mode="M125">
      <xsl:variable name="parent_object" select="ancestor::aix-def:fileset_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#aix') and ($state_name='fileset_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M125"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M125"/>
   <xsl:template match="@*|node()" priority="-2" mode="M125">
      <xsl:apply-templates select="*" mode="M125"/>
   </xsl:template>

   <!--PATTERN aix-def_fixtst-->


	<!--RULE -->
<xsl:template match="aix-def:fix_test/aix-def:object" priority="1001" mode="M126">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/aix-def:fix_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a fix_test must reference a fix_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M126"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="aix-def:fix_test/aix-def:state" priority="1000" mode="M126">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/aix-def:fix_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a fix_test must reference a fix_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M126"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M126"/>
   <xsl:template match="@*|node()" priority="-2" mode="M126">
      <xsl:apply-templates select="*" mode="M126"/>
   </xsl:template>

   <!--PATTERN aix-def_fix_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="aix-def:fix_object//oval-def:filter" priority="1000" mode="M127">
      <xsl:variable name="parent_object" select="ancestor::aix-def:fix_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#aix') and ($state_name='fix_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M127"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M127"/>
   <xsl:template match="@*|node()" priority="-2" mode="M127">
      <xsl:apply-templates select="*" mode="M127"/>
   </xsl:template>

   <!--PATTERN aix-def_notst-->


	<!--RULE -->
<xsl:template match="aix-def:no_test/aix-def:object" priority="1001" mode="M128">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/aix-def:no_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a <xsl:text/>
            <xsl:value-of select="name(..)"/>
            <xsl:text/> must reference a no_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M128"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="aix-def:no_test/aix-def:state" priority="1000" mode="M128">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/aix-def:no_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a <xsl:text/>
            <xsl:value-of select="name(..)"/>
            <xsl:text/> must reference a no_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M128"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M128"/>
   <xsl:template match="@*|node()" priority="-2" mode="M128">
      <xsl:apply-templates select="*" mode="M128"/>
   </xsl:template>

   <!--PATTERN aix-def_osleveltst-->


	<!--RULE -->
<xsl:template match="aix-def:oslevel_test/aix-def:object" priority="1001" mode="M129">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/aix-def:oslevel_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a oslevel_test must reference a oslevel_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M129"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="aix-def:oslevel_test/aix-def:state" priority="1000" mode="M129">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/aix-def:oslevel_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a oslevel_test must reference a oslevel_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M129"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M129"/>
   <xsl:template match="@*|node()" priority="-2" mode="M129">
      <xsl:apply-templates select="*" mode="M129"/>
   </xsl:template>

   <!--PATTERN apache-def_httpd_test_dep-->


	<!--RULE -->
<xsl:template match="apache-def:httpd_test" priority="1000" mode="M130">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M130"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M130"/>
   <xsl:template match="@*|node()" priority="-2" mode="M130">
      <xsl:apply-templates select="*" mode="M130"/>
   </xsl:template>

   <!--PATTERN apache-def_httpdtst-->


	<!--RULE -->
<xsl:template match="apache-def:httpd_test/apache-def:object" priority="1001" mode="M131">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/apache-def:httpd_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a httpd_test must reference a httpd_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M131"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="apache-def:httpd_test/apache-def:state" priority="1000" mode="M131">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/apache-def:httpd_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a httpd_test must reference a httpd_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M131"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M131"/>
   <xsl:template match="@*|node()" priority="-2" mode="M131">
      <xsl:apply-templates select="*" mode="M131"/>
   </xsl:template>

   <!--PATTERN apache-def_httpd_object_dep-->


	<!--RULE -->
<xsl:template match="apache-def:httpd_object" priority="1000" mode="M132">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED OBJECT: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M132"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M132"/>
   <xsl:template match="@*|node()" priority="-2" mode="M132">
      <xsl:apply-templates select="*" mode="M132"/>
   </xsl:template>

   <!--PATTERN apache-def_httpd_state_dep-->


	<!--RULE -->
<xsl:template match="apache-def:httpd_state" priority="1000" mode="M133">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED STATE: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M133"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M133"/>
   <xsl:template match="@*|node()" priority="-2" mode="M133">
      <xsl:apply-templates select="*" mode="M133"/>
   </xsl:template>

   <!--PATTERN catos-def_linetst-->


	<!--RULE -->
<xsl:template match="catos-def:line_test/catos-def:object" priority="1001" mode="M134">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/catos-def:line_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a line_test must reference a line_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M134"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="catos-def:line_test/catos-def:state" priority="1000" mode="M134">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/catos-def:line_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a line_test must reference a line_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M134"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M134"/>
   <xsl:template match="@*|node()" priority="-2" mode="M134">
      <xsl:apply-templates select="*" mode="M134"/>
   </xsl:template>

   <!--PATTERN catos-def_line_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="catos-def:line_object//oval-def:filter" priority="1000" mode="M135">
      <xsl:variable name="parent_object" select="ancestor::catos-def:line_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#catos') and ($state_name='line_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M135"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M135"/>
   <xsl:template match="@*|node()" priority="-2" mode="M135">
      <xsl:apply-templates select="*" mode="M135"/>
   </xsl:template>

   <!--PATTERN catos-def_moduletst-->


	<!--RULE -->
<xsl:template match="catos-def:module_test/catos-def:object" priority="1001" mode="M136">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/catos-def:module_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a module_test must reference a module_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M136"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="catos-def:module_test/catos-def:state" priority="1000" mode="M136">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/catos-def:module_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a module_test must reference a module_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M136"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M136"/>
   <xsl:template match="@*|node()" priority="-2" mode="M136">
      <xsl:apply-templates select="*" mode="M136"/>
   </xsl:template>

   <!--PATTERN catos-def_module_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="catos-def:module_object//oval-def:filter" priority="1000" mode="M137">
      <xsl:variable name="parent_object" select="ancestor::catos-def:module_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#catos') and ($state_name='module_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M137"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M137"/>
   <xsl:template match="@*|node()" priority="-2" mode="M137">
      <xsl:apply-templates select="*" mode="M137"/>
   </xsl:template>

   <!--PATTERN catos-def_version55_test-->


	<!--RULE -->
<xsl:template match="catos-def:version55_test/catos-def:object" priority="1001" mode="M138">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/catos-def:version55_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a version55_test must reference a version55_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M138"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="catos-def:version55_test/catos-def:state" priority="1000" mode="M138">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/catos-def:version55_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a version55_test must reference a version55_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M138"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M138"/>
   <xsl:template match="@*|node()" priority="-2" mode="M138">
      <xsl:apply-templates select="*" mode="M138"/>
   </xsl:template>

   <!--PATTERN catos-def_version_test_dep-->


	<!--RULE -->
<xsl:template match="catos-def:version_test" priority="1000" mode="M139">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M139"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M139"/>
   <xsl:template match="@*|node()" priority="-2" mode="M139">
      <xsl:apply-templates select="*" mode="M139"/>
   </xsl:template>

   <!--PATTERN catos-def_version_test-->


	<!--RULE -->
<xsl:template match="catos-def:version_test/catos-def:object" priority="1001" mode="M140">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/catos-def:version_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a version_test must reference a version_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M140"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="catos-def:version_test/catos-def:state" priority="1000" mode="M140">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/catos-def:version_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a version_test must reference a version_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M140"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M140"/>
   <xsl:template match="@*|node()" priority="-2" mode="M140">
      <xsl:apply-templates select="*" mode="M140"/>
   </xsl:template>

   <!--PATTERN catos-def_version_object_dep-->


	<!--RULE -->
<xsl:template match="catos-def:version_object" priority="1000" mode="M141">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED OBJECT: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M141"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M141"/>
   <xsl:template match="@*|node()" priority="-2" mode="M141">
      <xsl:apply-templates select="*" mode="M141"/>
   </xsl:template>

   <!--PATTERN catos-def_version_state_dep-->


	<!--RULE -->
<xsl:template match="catos-def:version_state" priority="1000" mode="M142">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED STATE: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M142"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M142"/>
   <xsl:template match="@*|node()" priority="-2" mode="M142">
      <xsl:apply-templates select="*" mode="M142"/>
   </xsl:template>

   <!--PATTERN esx-def_patch56tst-->


	<!--RULE -->
<xsl:template match="esx-def:patch56_test/esx-def:object" priority="1001" mode="M143">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/esx-def:patch56_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a patch56_test must reference a patch56_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M143"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="esx-def:patch56_test/esx-def:state" priority="1000" mode="M143">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/esx-def:patch56_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a patch56_test must reference a patch56_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M143"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M143"/>
   <xsl:template match="@*|node()" priority="-2" mode="M143">
      <xsl:apply-templates select="*" mode="M143"/>
   </xsl:template>

   <!--PATTERN esx-def_patch56_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="esx-def:patch56_object//oval-def:filter" priority="1000" mode="M144">
      <xsl:variable name="parent_object" select="ancestor::esx-def:patch56_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#esx') and ($state_name='patch56_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M144"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M144"/>
   <xsl:template match="@*|node()" priority="-2" mode="M144">
      <xsl:apply-templates select="*" mode="M144"/>
   </xsl:template>

   <!--PATTERN esx-def_patchtst_dep-->


	<!--RULE -->
<xsl:template match="esx-def:patch_test" priority="1000" mode="M145">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M145"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M145"/>
   <xsl:template match="@*|node()" priority="-2" mode="M145">
      <xsl:apply-templates select="*" mode="M145"/>
   </xsl:template>

   <!--PATTERN esx-def_patchtst-->


	<!--RULE -->
<xsl:template match="esx-def:patch_test/esx-def:object" priority="1001" mode="M146">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/esx-def:patch_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a patch_test must reference a patch_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M146"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="esx-def:patch_test/esx-def:state" priority="1000" mode="M146">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/esx-def:patch_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a patch_test must reference a patch_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M146"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M146"/>
   <xsl:template match="@*|node()" priority="-2" mode="M146">
      <xsl:apply-templates select="*" mode="M146"/>
   </xsl:template>

   <!--PATTERN esx-def_patchobj_dep-->


	<!--RULE -->
<xsl:template match="esx-def:patch_object" priority="1000" mode="M147">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED OBJECT: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M147"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M147"/>
   <xsl:template match="@*|node()" priority="-2" mode="M147">
      <xsl:apply-templates select="*" mode="M147"/>
   </xsl:template>

   <!--PATTERN esx-def_patchste_dep-->


	<!--RULE -->
<xsl:template match="esx-def:patch_state" priority="1000" mode="M148">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED STATE: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M148"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M148"/>
   <xsl:template match="@*|node()" priority="-2" mode="M148">
      <xsl:apply-templates select="*" mode="M148"/>
   </xsl:template>

   <!--PATTERN esx-def_versiontst-->


	<!--RULE -->
<xsl:template match="esx-def:version_test/esx-def:object" priority="1001" mode="M149">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/esx-def:version_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a version_test must reference a version_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M149"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="esx-def:version_test/esx-def:state" priority="1000" mode="M149">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/esx-def:version_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a version_test must reference a version_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M149"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M149"/>
   <xsl:template match="@*|node()" priority="-2" mode="M149">
      <xsl:apply-templates select="*" mode="M149"/>
   </xsl:template>

   <!--PATTERN esx-def_visdkmanagedobjecttst-->


	<!--RULE -->
<xsl:template match="esx-def:visdkmanagedobject_test/esx-def:object" priority="1001"
                 mode="M150">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/esx-def:visdkmanagedobject_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a visdkmanagedobject_test must reference a visdkmanagedobject_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M150"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="esx-def:visdkmanagedobject_test/esx-def:state" priority="1000"
                 mode="M150">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/esx-def:visdkmanagedobject_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a visdkmanagedobject_test must reference a visdkmanagedobject_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M150"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M150"/>
   <xsl:template match="@*|node()" priority="-2" mode="M150">
      <xsl:apply-templates select="*" mode="M150"/>
   </xsl:template>

   <!--PATTERN esx-def_visdkmanagedobject_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="esx-def:visdkmanagedobject_object//oval-def:filter" priority="1000"
                 mode="M151">
      <xsl:variable name="parent_object" select="ancestor::esx-def:visdkmanagedobject_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#esx') and ($state_name='visdkmanagedobject_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M151"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M151"/>
   <xsl:template match="@*|node()" priority="-2" mode="M151">
      <xsl:apply-templates select="*" mode="M151"/>
   </xsl:template>

   <!--PATTERN freebsd-def_portinfotst-->


	<!--RULE -->
<xsl:template match="freebsd-def:portinfo_test/freebsd-def:object" priority="1001"
                 mode="M152">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/freebsd-def:portinfo_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a portinfo_test must reference an portinfo_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M152"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="freebsd-def:portinfo_test/freebsd-def:state" priority="1000" mode="M152">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/freebsd-def:portinfo_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a portinfo_test must reference an portinfo_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M152"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M152"/>
   <xsl:template match="@*|node()" priority="-2" mode="M152">
      <xsl:apply-templates select="*" mode="M152"/>
   </xsl:template>

   <!--PATTERN freebsd-def_portinfo_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="freebsd-def:portinfo_object//oval-def:filter" priority="1000"
                 mode="M153">
      <xsl:variable name="parent_object" select="ancestor::freebsd-def:portinfo_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#freebsd') and ($state_name='portinfo_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M153"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M153"/>
   <xsl:template match="@*|node()" priority="-2" mode="M153">
      <xsl:apply-templates select="*" mode="M153"/>
   </xsl:template>

   <!--PATTERN hpux-def_getconf_test-->


	<!--RULE -->
<xsl:template match="hpux-def:getconf_test/hpux-def:object" priority="1001" mode="M154">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/hpux-def:getconf_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an getconf_test must reference an getconf_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M154"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="hpux-def:getconf_test/hpux-def:state" priority="1000" mode="M154">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/hpux-def:getconf_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an getconf_test must reference an getconf_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M154"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M154"/>
   <xsl:template match="@*|node()" priority="-2" mode="M154">
      <xsl:apply-templates select="*" mode="M154"/>
   </xsl:template>

   <!--PATTERN hpux-def_getconf_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="hpux-def:getconf_object//oval-def:filter" priority="1000" mode="M155">
      <xsl:variable name="parent_object" select="ancestor::hpux-def:getconf_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#hpux') and ($state_name='getconf_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M155"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M155"/>
   <xsl:template match="@*|node()" priority="-2" mode="M155">
      <xsl:apply-templates select="*" mode="M155"/>
   </xsl:template>

   <!--PATTERN hpux-def_ndd_test-->


	<!--RULE -->
<xsl:template match="hpux-def:ndd_test/hpux-def:object" priority="1001" mode="M156">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/hpux-def:ndd_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an ndd_test must reference an ndd_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M156"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="hpux-def:ndd_test/hpux-def:state" priority="1000" mode="M156">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/hpux-def:ndd_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an ndd_test must reference an ndd_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M156"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M156"/>
   <xsl:template match="@*|node()" priority="-2" mode="M156">
      <xsl:apply-templates select="*" mode="M156"/>
   </xsl:template>

   <!--PATTERN hpux-def_ndd_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="hpux-def:ndd_object//oval-def:filter" priority="1000" mode="M157">
      <xsl:variable name="parent_object" select="ancestor::hpux-def:ndd_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#hpux') and ($state_name='ndd_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M157"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M157"/>
   <xsl:template match="@*|node()" priority="-2" mode="M157">
      <xsl:apply-templates select="*" mode="M157"/>
   </xsl:template>

   <!--PATTERN hpux-def_patch53_test-->


	<!--RULE -->
<xsl:template match="hpux-def:patch53_test/hpux-def:object" priority="1001" mode="M158">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/hpux-def:patch53_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an patch53_test must reference an patch53_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M158"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="hpux-def:patch53_test/hpux-def:state" priority="1000" mode="M158">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/hpux-def:patch53_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an patch53_test must reference an patch53_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M158"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M158"/>
   <xsl:template match="@*|node()" priority="-2" mode="M158">
      <xsl:apply-templates select="*" mode="M158"/>
   </xsl:template>

   <!--PATTERN hpux-def_patch53_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="hpux-def:patch53_object//oval-def:filter" priority="1000" mode="M159">
      <xsl:variable name="parent_object" select="ancestor::hpux-def:patch53_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#hpux') and ($state_name='patch53_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M159"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M159"/>
   <xsl:template match="@*|node()" priority="-2" mode="M159">
      <xsl:apply-templates select="*" mode="M159"/>
   </xsl:template>

   <!--PATTERN hpux-def_patch_test_dep-->


	<!--RULE -->
<xsl:template match="hpux-def:patch_test" priority="1000" mode="M160">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M160"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M160"/>
   <xsl:template match="@*|node()" priority="-2" mode="M160">
      <xsl:apply-templates select="*" mode="M160"/>
   </xsl:template>

   <!--PATTERN hpux-def_patch_test-->


	<!--RULE -->
<xsl:template match="hpux-def:patch_test/hpux-def:object" priority="1001" mode="M161">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/hpux-def:patch_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an patch_test must reference an patch_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M161"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="hpux-def:patch_test/hpux-def:state" priority="1000" mode="M161">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/hpux-def:patch_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an patch_test must reference an patch_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M161"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M161"/>
   <xsl:template match="@*|node()" priority="-2" mode="M161">
      <xsl:apply-templates select="*" mode="M161"/>
   </xsl:template>

   <!--PATTERN hpux-def_patch_object_dep-->


	<!--RULE -->
<xsl:template match="hpux-def:patch_object" priority="1000" mode="M162">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED OBJECT: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M162"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M162"/>
   <xsl:template match="@*|node()" priority="-2" mode="M162">
      <xsl:apply-templates select="*" mode="M162"/>
   </xsl:template>

   <!--PATTERN hpux-def_patch_state_dep-->


	<!--RULE -->
<xsl:template match="hpux-def:patch_state" priority="1000" mode="M163">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED STATE: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M163"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M163"/>
   <xsl:template match="@*|node()" priority="-2" mode="M163">
      <xsl:apply-templates select="*" mode="M163"/>
   </xsl:template>

   <!--PATTERN hpux-def_swlist_test-->


	<!--RULE -->
<xsl:template match="hpux-def:swlist_test/hpux-def:object" priority="1001" mode="M164">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/hpux-def:swlist_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an swlist_test must reference an swlist_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M164"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="hpux-def:swlist_test/hpux-def:state" priority="1000" mode="M164">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/hpux-def:swlist_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an swlist_test must reference an swlist_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M164"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M164"/>
   <xsl:template match="@*|node()" priority="-2" mode="M164">
      <xsl:apply-templates select="*" mode="M164"/>
   </xsl:template>

   <!--PATTERN hpux-def_swlist_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="hpux-def:swlist_object//oval-def:filter" priority="1000" mode="M165">
      <xsl:variable name="parent_object" select="ancestor::hpux-def:swlist_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#hpux') and ($state_name='swlist_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M165"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M165"/>
   <xsl:template match="@*|node()" priority="-2" mode="M165">
      <xsl:apply-templates select="*" mode="M165"/>
   </xsl:template>

   <!--PATTERN hpux-def_trusted_test-->


	<!--RULE -->
<xsl:template match="hpux-def:trusted_test/hpux-def:object" priority="1001" mode="M166">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/hpux-def:trusted_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an trusted_test must reference an trusted_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M166"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="hpux-def:trusted_test/hpux-def:state" priority="1000" mode="M166">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/hpux-def:trusted_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an trusted_test must reference an trusted_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M166"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M166"/>
   <xsl:template match="@*|node()" priority="-2" mode="M166">
      <xsl:apply-templates select="*" mode="M166"/>
   </xsl:template>

   <!--PATTERN hpux-def_trusted_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="hpux-def:trusted_object//oval-def:filter" priority="1000" mode="M167">
      <xsl:variable name="parent_object" select="ancestor::hpux-def:trusted_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#hpux') and ($state_name='trusted_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M167"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M167"/>
   <xsl:template match="@*|node()" priority="-2" mode="M167">
      <xsl:apply-templates select="*" mode="M167"/>
   </xsl:template>

   <!--PATTERN ios-def_globaltst-->


	<!--RULE -->
<xsl:template match="ios-def:global_test/ios-def:object" priority="1001" mode="M168">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/ios-def:global_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a global_test must reference a global_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M168"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="ios-def:global_test/ios-def:state" priority="1000" mode="M168">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/ios-def:global_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a global_test must reference a global_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M168"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M168"/>
   <xsl:template match="@*|node()" priority="-2" mode="M168">
      <xsl:apply-templates select="*" mode="M168"/>
   </xsl:template>

   <!--PATTERN ios-def_global_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="ios-def:global_object//oval-def:filter" priority="1000" mode="M169">
      <xsl:variable name="parent_object" select="ancestor::ios-def:global_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#ios') and ($state_name='global_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M169"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M169"/>
   <xsl:template match="@*|node()" priority="-2" mode="M169">
      <xsl:apply-templates select="*" mode="M169"/>
   </xsl:template>

   <!--PATTERN ios-def_interfacetst-->


	<!--RULE -->
<xsl:template match="ios-def:interface_test/ios-def:object" priority="1001" mode="M170">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/ios-def:interface_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an interface_test must reference an interface_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M170"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="ios-def:interface_test/ios-def:state" priority="1000" mode="M170">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/ios-def:interface_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an interface_test must reference an interface_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M170"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M170"/>
   <xsl:template match="@*|node()" priority="-2" mode="M170">
      <xsl:apply-templates select="*" mode="M170"/>
   </xsl:template>

   <!--PATTERN ios-def_interface_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="ios-def:interface_object//oval-def:filter" priority="1000" mode="M171">
      <xsl:variable name="parent_object" select="ancestor::ios-def:interface_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#ios') and ($state_name='interface_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M171"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M171"/>
   <xsl:template match="@*|node()" priority="-2" mode="M171">
      <xsl:apply-templates select="*" mode="M171"/>
   </xsl:template>

   <!--PATTERN ios-def_linetst-->


	<!--RULE -->
<xsl:template match="ios-def:line_test/ios-def:object" priority="1001" mode="M172">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/ios-def:line_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a line_test must reference a line_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M172"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="ios-def:line_test/ios-def:state" priority="1000" mode="M172">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/ios-def:line_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a line_test must reference a line_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M172"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M172"/>
   <xsl:template match="@*|node()" priority="-2" mode="M172">
      <xsl:apply-templates select="*" mode="M172"/>
   </xsl:template>

   <!--PATTERN ios-def_line_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="ios-def:line_object//oval-def:filter" priority="1000" mode="M173">
      <xsl:variable name="parent_object" select="ancestor::ios-def:line_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#ios') and ($state_name='line_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M173"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M173"/>
   <xsl:template match="@*|node()" priority="-2" mode="M173">
      <xsl:apply-templates select="*" mode="M173"/>
   </xsl:template>

   <!--PATTERN ios-def_snmptst-->


	<!--RULE -->
<xsl:template match="ios-def:snmp_test/ios-def:object" priority="1001" mode="M174">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/ios-def:snmp_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a snmp_test must reference a snmp_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M174"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="ios-def:snmp_test/ios-def:state" priority="1000" mode="M174">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/ios-def:snmp_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a snmp_test must reference a snmp_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M174"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M174"/>
   <xsl:template match="@*|node()" priority="-2" mode="M174">
      <xsl:apply-templates select="*" mode="M174"/>
   </xsl:template>

   <!--PATTERN ios-def_tclshtst-->


	<!--RULE -->
<xsl:template match="ios-def:tclsh_test/ios-def:object" priority="1001" mode="M175">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/ios-def:tclsh_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a tclsh_test must reference a tclsh_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M175"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="ios-def:tclsh_test/ios-def:state" priority="1000" mode="M175">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/ios-def:tclsh_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a tclsh_test must reference a tclsh_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M175"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M175"/>
   <xsl:template match="@*|node()" priority="-2" mode="M175">
      <xsl:apply-templates select="*" mode="M175"/>
   </xsl:template>

   <!--PATTERN ios-def_ver55tst-->


	<!--RULE -->
<xsl:template match="ios-def:version55_test/ios-def:object" priority="1001" mode="M176">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/ios-def:version55_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a version55_test must reference a version_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M176"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="ios-def:version55_test/ios-def:state" priority="1000" mode="M176">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/ios-def:version55_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a version55_test must reference a version_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M176"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M176"/>
   <xsl:template match="@*|node()" priority="-2" mode="M176">
      <xsl:apply-templates select="*" mode="M176"/>
   </xsl:template>

   <!--PATTERN ios-def_vertst_dep-->


	<!--RULE -->
<xsl:template match="ios-def:version_test" priority="1000" mode="M177">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M177"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M177"/>
   <xsl:template match="@*|node()" priority="-2" mode="M177">
      <xsl:apply-templates select="*" mode="M177"/>
   </xsl:template>

   <!--PATTERN ios-def_vertst-->


	<!--RULE -->
<xsl:template match="ios-def:version_test/ios-def:object" priority="1001" mode="M178">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/ios-def:version_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a version_test must reference a version_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M178"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="ios-def:version_test/ios-def:state" priority="1000" mode="M178">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/ios-def:version_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a version_test must reference a version_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M178"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M178"/>
   <xsl:template match="@*|node()" priority="-2" mode="M178">
      <xsl:apply-templates select="*" mode="M178"/>
   </xsl:template>

   <!--PATTERN ios-def_verobj_dep-->


	<!--RULE -->
<xsl:template match="ios-def:version_object" priority="1000" mode="M179">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED OBJECT: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M179"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M179"/>
   <xsl:template match="@*|node()" priority="-2" mode="M179">
      <xsl:apply-templates select="*" mode="M179"/>
   </xsl:template>

   <!--PATTERN ios-def_verste_dep-->


	<!--RULE -->
<xsl:template match="ios-def:version_state" priority="1000" mode="M180">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED STATE: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M180"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M180"/>
   <xsl:template match="@*|node()" priority="-2" mode="M180">
      <xsl:apply-templates select="*" mode="M180"/>
   </xsl:template>

   <!--PATTERN linux-def_dpkginfo_test-->


	<!--RULE -->
<xsl:template match="linux-def:dpkginfo_test/linux-def:object" priority="1001" mode="M181">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/linux-def:dpkginfo_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an dpkginfo_test must reference an dpkginfo_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M181"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="linux-def:dpkginfo_test/linux-def:state" priority="1000" mode="M181">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/linux-def:dpkginfo_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an dpkginfo_test must reference an dpkginfo_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M181"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M181"/>
   <xsl:template match="@*|node()" priority="-2" mode="M181">
      <xsl:apply-templates select="*" mode="M181"/>
   </xsl:template>

   <!--PATTERN linux-def_dpkginfo_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="linux-def:dpkginfo_object//oval-def:filter" priority="1000" mode="M182">
      <xsl:variable name="parent_object" select="ancestor::linux-def:dpkginfo_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#linux') and ($state_name='dpkginfo_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M182"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M182"/>
   <xsl:template match="@*|node()" priority="-2" mode="M182">
      <xsl:apply-templates select="*" mode="M182"/>
   </xsl:template>

   <!--PATTERN linux-def_iflisteners_test-->


	<!--RULE -->
<xsl:template match="linux-def:iflisteners_test/linux-def:object" priority="1001" mode="M183">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/linux-def:iflisteners_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an iflisteners_test must reference an iflisteners_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M183"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="linux-def:iflisteners_test/linux-def:state" priority="1000" mode="M183">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/linux-def:iflisteners_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an iflisteners_test must reference an iflisteners_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M183"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M183"/>
   <xsl:template match="@*|node()" priority="-2" mode="M183">
      <xsl:apply-templates select="*" mode="M183"/>
   </xsl:template>

   <!--PATTERN linux-def_iflisteners_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="linux-def:iflisteners_object//oval-def:filter" priority="1000"
                 mode="M184">
      <xsl:variable name="parent_object" select="ancestor::linux-def:iflisteners_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#linux') and ($state_name='iflisteners_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M184"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M184"/>
   <xsl:template match="@*|node()" priority="-2" mode="M184">
      <xsl:apply-templates select="*" mode="M184"/>
   </xsl:template>

   <!--PATTERN linux-def_inetlisteningservers_test-->


	<!--RULE -->
<xsl:template match="linux-def:inetlisteningservers_test/linux-def:object" priority="1001"
                 mode="M185">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/linux-def:inetlisteningservers_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an inetlisteningservers_test must reference an inetlisteningservers_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M185"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="linux-def:inetlisteningservers_test/linux-def:state" priority="1000"
                 mode="M185">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/linux-def:inetlisteningservers_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an inetlisteningservers_test must reference an inetlisteningservers_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M185"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M185"/>
   <xsl:template match="@*|node()" priority="-2" mode="M185">
      <xsl:apply-templates select="*" mode="M185"/>
   </xsl:template>

   <!--PATTERN linux-def_inetlisteningservers_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="linux-def:inetlisteningservers_object//oval-def:filter" priority="1000"
                 mode="M186">
      <xsl:variable name="parent_object" select="ancestor::linux-def:inetlisteningservers_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#linux') and ($state_name='inetlisteningservers_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M186"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M186"/>
   <xsl:template match="@*|node()" priority="-2" mode="M186">
      <xsl:apply-templates select="*" mode="M186"/>
   </xsl:template>

   <!--PATTERN linux-def_partitiontst-->


	<!--RULE -->
<xsl:template match="linux-def:partition_test/linux-def:object" priority="1001" mode="M187">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/linux-def:partition_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a partition_test must reference a partition_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M187"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="linux-def:partition_test/linux-def:state" priority="1000" mode="M187">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/linux-def:partition_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a partition_test must reference a partition_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M187"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M187"/>
   <xsl:template match="@*|node()" priority="-2" mode="M187">
      <xsl:apply-templates select="*" mode="M187"/>
   </xsl:template>

   <!--PATTERN linux-def_partition_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="linux-def:partition_object//oval-def:filter" priority="1000" mode="M188">
      <xsl:variable name="parent_object" select="ancestor::linux-def:partition_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#linux') and ($state_name='partition_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M188"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M188"/>
   <xsl:template match="@*|node()" priority="-2" mode="M188">
      <xsl:apply-templates select="*" mode="M188"/>
   </xsl:template>

   <!--PATTERN linux-def_rpminfo_test-->


	<!--RULE -->
<xsl:template match="linux-def:rpminfo_test/linux-def:object" priority="1001" mode="M189">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/linux-def:rpminfo_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an rpminfo_test must reference an rpminfo_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M189"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="linux-def:rpminfo_test/linux-def:state" priority="1000" mode="M189">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/linux-def:rpminfo_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an rpminfo_test must reference an rpminfo_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M189"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M189"/>
   <xsl:template match="@*|node()" priority="-2" mode="M189">
      <xsl:apply-templates select="*" mode="M189"/>
   </xsl:template>

   <!--PATTERN linux-def_rpminfo_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="linux-def:rpminfo_object//oval-def:filter" priority="1000" mode="M190">
      <xsl:variable name="parent_object" select="ancestor::linux-def:rpminfo_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#linux') and ($state_name='rpminfo_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M190"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M190"/>
   <xsl:template match="@*|node()" priority="-2" mode="M190">
      <xsl:apply-templates select="*" mode="M190"/>
   </xsl:template>

   <!--PATTERN linux-def_rpmverifytst_dep-->


	<!--RULE -->
<xsl:template match="linux-def:rpmverify_test" priority="1000" mode="M191">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M191"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M191"/>
   <xsl:template match="@*|node()" priority="-2" mode="M191">
      <xsl:apply-templates select="*" mode="M191"/>
   </xsl:template>

   <!--PATTERN linux-def_rpmverify_test-->


	<!--RULE -->
<xsl:template match="linux-def:rpmverify_test/linux-def:object" priority="1001" mode="M192">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/linux-def:rpmverify_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an rpmverify_test must reference an rpmverify_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M192"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="linux-def:rpmverify_test/linux-def:state" priority="1000" mode="M192">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/linux-def:rpmverify_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an rpmverify_test must reference an rpmverify_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M192"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M192"/>
   <xsl:template match="@*|node()" priority="-2" mode="M192">
      <xsl:apply-templates select="*" mode="M192"/>
   </xsl:template>

   <!--PATTERN linux-def_rpmverify_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="linux-def:rpmverify_object//oval-def:filter" priority="1000" mode="M193">
      <xsl:variable name="parent_object" select="ancestor::linux-def:rpmverify_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#linux') and ($state_name='rpmverify_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M193"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M193"/>
   <xsl:template match="@*|node()" priority="-2" mode="M193">
      <xsl:apply-templates select="*" mode="M193"/>
   </xsl:template>

   <!--PATTERN linux-def_rpmverifyobj_dep-->


	<!--RULE -->
<xsl:template match="linux-def:rpmverify_object" priority="1000" mode="M194">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED OBJECT: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M194"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M194"/>
   <xsl:template match="@*|node()" priority="-2" mode="M194">
      <xsl:apply-templates select="*" mode="M194"/>
   </xsl:template>

   <!--PATTERN linux-def_rpmverifyste_dep-->


	<!--RULE -->
<xsl:template match="linux-def:rpmverify_state" priority="1000" mode="M195">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED STATE: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M195"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M195"/>
   <xsl:template match="@*|node()" priority="-2" mode="M195">
      <xsl:apply-templates select="*" mode="M195"/>
   </xsl:template>

   <!--PATTERN linux-def_rpmverifyfile_test-->


	<!--RULE -->
<xsl:template match="linux-def:rpmverifyfile_test/linux-def:object" priority="1001"
                 mode="M196">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/linux-def:rpmverifyfile_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an rpmverifyfile_test must reference an rpmverifyfile_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M196"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="linux-def:rpmverifyfile_test/linux-def:state" priority="1000"
                 mode="M196">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/linux-def:rpmverifyfile_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an rpmverifyfile_test must reference an rpmverifyfile_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M196"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M196"/>
   <xsl:template match="@*|node()" priority="-2" mode="M196">
      <xsl:apply-templates select="*" mode="M196"/>
   </xsl:template>

   <!--PATTERN linux-def_rpmverifyfile_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="linux-def:rpmverifyfile_object//oval-def:filter" priority="1000"
                 mode="M197">
      <xsl:variable name="parent_object" select="ancestor::linux-def:rpmverifyfile_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#linux') and ($state_name='rpmverifyfile_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M197"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M197"/>
   <xsl:template match="@*|node()" priority="-2" mode="M197">
      <xsl:apply-templates select="*" mode="M197"/>
   </xsl:template>

   <!--PATTERN linux-def_rpmverifypackage_test-->


	<!--RULE -->
<xsl:template match="linux-def:rpmverifypackage_test/linux-def:object" priority="1001"
                 mode="M198">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/linux-def:rpmverifypackage_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an rpmverifypackage_test must reference an rpmverifypackage_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M198"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="linux-def:rpmverifypackage_test/linux-def:state" priority="1000"
                 mode="M198">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/linux-def:rpmverifypackage_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an rpmverifypackage_test must reference an rpmverifypackage_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M198"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M198"/>
   <xsl:template match="@*|node()" priority="-2" mode="M198">
      <xsl:apply-templates select="*" mode="M198"/>
   </xsl:template>

   <!--PATTERN linux-def_rpmverifypackage_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="linux-def:rpmverifypackage_object//oval-def:filter" priority="1000"
                 mode="M199">
      <xsl:variable name="parent_object" select="ancestor::linux-def:rpmverifypackage_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#linux') and ($state_name='rpmverifypackage_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M199"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M199"/>
   <xsl:template match="@*|node()" priority="-2" mode="M199">
      <xsl:apply-templates select="*" mode="M199"/>
   </xsl:template>

   <!--PATTERN linux-def_selinuxbooleantst-->


	<!--RULE -->
<xsl:template match="linux-def:selinuxboolean_test/linux-def:object" priority="1001"
                 mode="M200">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/linux-def:selinuxboolean_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an selinuxboolean_test must reference an selinuxboolean_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M200"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="linux-def:selinuxboolean_test/linux-def:state" priority="1000"
                 mode="M200">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/linux-def:selinuxboolean_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an selinuxboolean_test must reference an selinuxboolean_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M200"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M200"/>
   <xsl:template match="@*|node()" priority="-2" mode="M200">
      <xsl:apply-templates select="*" mode="M200"/>
   </xsl:template>

   <!--PATTERN linux-def_selinuxboolean_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="linux-def:selinuxboolean_object//oval-def:filter" priority="1000"
                 mode="M201">
      <xsl:variable name="parent_object" select="ancestor::linux-def:selinuxboolean_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#linux') and ($state_name='selinuxboolean_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M201"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M201"/>
   <xsl:template match="@*|node()" priority="-2" mode="M201">
      <xsl:apply-templates select="*" mode="M201"/>
   </xsl:template>

   <!--PATTERN linux-def_selinuxsecuritycontexttst-->


	<!--RULE -->
<xsl:template match="linux-def:selinuxsecuritycontext_test/linux-def:object" priority="1001"
                 mode="M202">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/linux-def:selinuxsecuritycontext_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an selinuxsecuritycontext_test must reference an selinuxsecuritycontext_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M202"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="linux-def:selinuxsecuritycontext_test/linux-def:state" priority="1000"
                 mode="M202">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/linux-def:selinuxsecuritycontext_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an selinuxsecuritycontext_test must reference an selinuxsecuritycontext_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M202"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M202"/>
   <xsl:template match="@*|node()" priority="-2" mode="M202">
      <xsl:apply-templates select="*" mode="M202"/>
   </xsl:template>

   <!--PATTERN linux-def_selinuxsecuritycontext_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="linux-def:selinuxsecuritycontext_object//oval-def:filter"
                 priority="1000"
                 mode="M203">
      <xsl:variable name="parent_object" select="ancestor::linux-def:selinuxsecuritycontext_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#linux') and ($state_name='selinuxsecuritycontext_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M203"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M203"/>
   <xsl:template match="@*|node()" priority="-2" mode="M203">
      <xsl:apply-templates select="*" mode="M203"/>
   </xsl:template>

   <!--PATTERN linux-def_selinuxsecuritycontext_objectfilepath-->


	<!--RULE -->
<xsl:template match="linux-def:selinuxsecuritycontext_object/linux-def:filepath"
                 priority="1000"
                 mode="M204">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::linux-def:behaviors[@max_depth or @recurse or @recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth, recurse, and recurse_direction behaviors are not allowed with a filepath entity<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M204"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M204"/>
   <xsl:template match="@*|node()" priority="-2" mode="M204">
      <xsl:apply-templates select="*" mode="M204"/>
   </xsl:template>

   <!--PATTERN unix-def_selinuxsecuritycontext_objectfilepath2-->


	<!--RULE -->
<xsl:template match="linux-def:selinuxsecuritycontext_object/linux-def:filepath[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M205">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::linux-def:behaviors[@recurse_file_system='defined'])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_file_system behavior MUST not be set to 'defined' when a pattern match is used with a filepath entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M205"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M205"/>
   <xsl:template match="@*|node()" priority="-2" mode="M205">
      <xsl:apply-templates select="*" mode="M205"/>
   </xsl:template>

   <!--PATTERN linux-def_selinuxsecuritycontext_objectpath-->


	<!--RULE -->
<xsl:template match="linux-def:selinuxsecuritycontext_object/linux-def:path[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M206">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::linux-def:behaviors[@recurse_file_system='defined'])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_file_system behavior MUST not be set to 'defined' when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::linux-def:behaviors[@max_depth])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::linux-def:behaviors[@recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_direction behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::linux-def:behaviors[@recurse])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M206"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M206"/>
   <xsl:template match="@*|node()" priority="-2" mode="M206">
      <xsl:apply-templates select="*" mode="M206"/>
   </xsl:template>

   <!--PATTERN linux-def_selinuxsecuritycontext_objectfilename-->


	<!--RULE -->
<xsl:template match="linux-def:selinuxsecuritycontext_object/linux-def:filename"
                 priority="1000"
                 mode="M207">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="(@var_ref and .='') or ((@xsi:nil='1' or @xsi:nil='true') and .='') or not(.='') or (.='' and @operation = 'pattern match')"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - filename entity cannot be empty unless the xsi:nil attribute is set to true or a var_ref is used<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M207"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M207"/>
   <xsl:template match="@*|node()" priority="-2" mode="M207">
      <xsl:apply-templates select="*" mode="M207"/>
   </xsl:template>

   <!--PATTERN linux-def_slackwarepkginfo_test-->


	<!--RULE -->
<xsl:template match="linux-def:slackwarepkginfo_test/linux-def:object" priority="1001"
                 mode="M208">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/linux-def:slackwarepkginfo_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an slackwarepkginfo_test must reference an slackwarepkginfo_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M208"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="linux-def:slackwarepkginfo_test/linux-def:state" priority="1000"
                 mode="M208">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/linux-def:slackwarepkginfo_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an slackwarepkginfo_test must reference an slackwarepkginfo_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M208"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M208"/>
   <xsl:template match="@*|node()" priority="-2" mode="M208">
      <xsl:apply-templates select="*" mode="M208"/>
   </xsl:template>

   <!--PATTERN linux-def_slackwarepkginfo_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="linux-def:slackwarepkginfo_object//oval-def:filter" priority="1000"
                 mode="M209">
      <xsl:variable name="parent_object" select="ancestor::linux-def:slackwarepkginfo_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#linux') and ($state_name='slackwarepkginfo_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M209"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M209"/>
   <xsl:template match="@*|node()" priority="-2" mode="M209">
      <xsl:apply-templates select="*" mode="M209"/>
   </xsl:template>

   <!--PATTERN macos-def_accountinfo_test-->


	<!--RULE -->
<xsl:template match="macos-def:accountinfo_test/macos-def:object" priority="1001" mode="M210">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/macos-def:accountinfo_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an accountinfo_test must reference an accountinfo_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M210"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="macos-def:accountinfo_test/macos-def:state" priority="1000" mode="M210">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/macos-def:accountinfo_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an accountinfo_test must reference an accountinfo_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M210"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M210"/>
   <xsl:template match="@*|node()" priority="-2" mode="M210">
      <xsl:apply-templates select="*" mode="M210"/>
   </xsl:template>

   <!--PATTERN macos-def_accountinfo_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="macos-def:accountinfo_object//oval-def:filter" priority="1000"
                 mode="M211">
      <xsl:variable name="parent_object" select="ancestor::macos-def:accountinfo_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#macos') and ($state_name='accountinfo_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M211"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M211"/>
   <xsl:template match="@*|node()" priority="-2" mode="M211">
      <xsl:apply-templates select="*" mode="M211"/>
   </xsl:template>

   <!--PATTERN macos-def_diskutiltst-->


	<!--RULE -->
<xsl:template match="macos-def:diskutil_test/macos-def:object" priority="1001" mode="M212">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/macos-def:diskutil_object/@id"/>
         <xsl:otherwise>
                                          <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a diskutil_test must reference a diskutil_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M212"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="macos-def:diskutil_test/macos-def:state" priority="1000" mode="M212">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/macos-def:diskutil_state/@id"/>
         <xsl:otherwise>
                                          <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a diskutil_test must reference a diskutil_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M212"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M212"/>
   <xsl:template match="@*|node()" priority="-2" mode="M212">
      <xsl:apply-templates select="*" mode="M212"/>
   </xsl:template>

   <!--PATTERN macos-def_diskutil_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="macos-def:diskutil_object//oval-def:filter" priority="1000" mode="M213">
      <xsl:variable name="parent_object" select="ancestor::macos-def:diskutil_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#macos') and ($state_name='diskutil_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M213"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M213"/>
   <xsl:template match="@*|node()" priority="-2" mode="M213">
      <xsl:apply-templates select="*" mode="M213"/>
   </xsl:template>

   <!--PATTERN macos-def_inetlisteningserverstst_dep-->


	<!--RULE -->
<xsl:template match="macos-def:inetlisteningservers_test" priority="1000" mode="M214">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M214"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M214"/>
   <xsl:template match="@*|node()" priority="-2" mode="M214">
      <xsl:apply-templates select="*" mode="M214"/>
   </xsl:template>

   <!--PATTERN macos-def_inetlisteningservers_test-->


	<!--RULE -->
<xsl:template match="macos-def:inetlisteningservers_test/macos-def:object" priority="1001"
                 mode="M215">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/macos-def:inetlisteningservers_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an inetlisteningservers_test must reference an inetlisteningservers_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M215"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="macos-def:inetlisteningservers_test/macos-def:state" priority="1000"
                 mode="M215">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/macos-def:inetlisteningservers_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an inetlisteningservers_test must reference an inetlisteningservers_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M215"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M215"/>
   <xsl:template match="@*|node()" priority="-2" mode="M215">
      <xsl:apply-templates select="*" mode="M215"/>
   </xsl:template>

   <!--PATTERN macos-def_inetlisteningserversobj_dep-->


	<!--RULE -->
<xsl:template match="macos-def:inetlisteningservers_object" priority="1000" mode="M216">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED OBJECT: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M216"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M216"/>
   <xsl:template match="@*|node()" priority="-2" mode="M216">
      <xsl:apply-templates select="*" mode="M216"/>
   </xsl:template>

   <!--PATTERN macos-def_inetlisteningservers_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="macos-def:inetlisteningservers_object//oval-def:filter" priority="1000"
                 mode="M217">
      <xsl:variable name="parent_object" select="ancestor::macos-def:inetlisteningservers_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#macos') and ($state_name='inetlisteningservers_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M217"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M217"/>
   <xsl:template match="@*|node()" priority="-2" mode="M217">
      <xsl:apply-templates select="*" mode="M217"/>
   </xsl:template>

   <!--PATTERN macos-def_inetlisteningserversste_dep-->


	<!--RULE -->
<xsl:template match="macos-def:inetlisteningservers_state" priority="1000" mode="M218">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED STATE: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M218"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M218"/>
   <xsl:template match="@*|node()" priority="-2" mode="M218">
      <xsl:apply-templates select="*" mode="M218"/>
   </xsl:template>

   <!--PATTERN macos-def_inetlisteningserver510_test-->


	<!--RULE -->
<xsl:template match="macos-def:inetlisteningserver510_test/macos-def:object" priority="1001"
                 mode="M219">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/macos-def:inetlisteningserver510_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an inetlisteningserver510_test must reference an inetlisteningserver510_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M219"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="macos-def:inetlisteningserver510_test/macos-def:state" priority="1000"
                 mode="M219">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/macos-def:inetlisteningserver510_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an inetlisteningserver510_test must reference an inetlisteningserver510_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M219"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M219"/>
   <xsl:template match="@*|node()" priority="-2" mode="M219">
      <xsl:apply-templates select="*" mode="M219"/>
   </xsl:template>

   <!--PATTERN macos-def_inetlisteningserver510_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="macos-def:inetlisteningserver510_object//oval-def:filter"
                 priority="1000"
                 mode="M220">
      <xsl:variable name="parent_object" select="ancestor::macos-def:inetlisteningserver510_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#macos') and ($state_name='inetlisteningserver510_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M220"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M220"/>
   <xsl:template match="@*|node()" priority="-2" mode="M220">
      <xsl:apply-templates select="*" mode="M220"/>
   </xsl:template>

   <!--PATTERN macos-def_nvram_test-->


	<!--RULE -->
<xsl:template match="macos-def:nvram_test/macos-def:object" priority="1001" mode="M221">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/macos-def:nvram_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an nvram_test must reference an nvram_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M221"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="macos-def:nvram_test/macos-def:state" priority="1000" mode="M221">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/macos-def:nvram_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an nvram_test must reference an nvram_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M221"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M221"/>
   <xsl:template match="@*|node()" priority="-2" mode="M221">
      <xsl:apply-templates select="*" mode="M221"/>
   </xsl:template>

   <!--PATTERN macos-def_nvram_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="macos-def:nvram_object//oval-def:filter" priority="1000" mode="M222">
      <xsl:variable name="parent_object" select="ancestor::macos-def:nvram_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#macos') and ($state_name='nvram_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M222"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M222"/>
   <xsl:template match="@*|node()" priority="-2" mode="M222">
      <xsl:apply-templates select="*" mode="M222"/>
   </xsl:template>

   <!--PATTERN macos-def_plist_test_dep-->


	<!--RULE -->
<xsl:template match="macos-def:plist_test" priority="1000" mode="M223">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M223"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M223"/>
   <xsl:template match="@*|node()" priority="-2" mode="M223">
      <xsl:apply-templates select="*" mode="M223"/>
   </xsl:template>

   <!--PATTERN macos-def_plist_test-->


	<!--RULE -->
<xsl:template match="macos-def:plist_test/macos-def:object" priority="1001" mode="M224">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/macos-def:plist_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a plist_test must reference a plist_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M224"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="macos-def:plist_test/macos-def:state" priority="1000" mode="M224">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/macos-def:plist_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a plist_test must reference a plist_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M224"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M224"/>
   <xsl:template match="@*|node()" priority="-2" mode="M224">
      <xsl:apply-templates select="*" mode="M224"/>
   </xsl:template>

   <!--PATTERN macos-def_plist_object_dep-->


	<!--RULE -->
<xsl:template match="macos-def:plist_object" priority="1000" mode="M225">

		<!--REPORT -->
<xsl:if test="true()">
                                          DEPRECATED OBJECT: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
                                    <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M225"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M225"/>
   <xsl:template match="@*|node()" priority="-2" mode="M225">
      <xsl:apply-templates select="*" mode="M225"/>
   </xsl:template>

   <!--PATTERN macos-def_plist_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="macos-def:plist_object//oval-def:filter" priority="1000" mode="M226">
      <xsl:variable name="parent_object" select="ancestor::macos-def:plist_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#macos') and ($state_name='plist_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M226"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M226"/>
   <xsl:template match="@*|node()" priority="-2" mode="M226">
      <xsl:apply-templates select="*" mode="M226"/>
   </xsl:template>

   <!--PATTERN macos-def_plistobjfilepath-->


	<!--RULE -->
<xsl:template match="macos-def:plist_object/macos-def:filepath" priority="1000" mode="M227">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals'"/>
         <xsl:otherwise>
                                                                                          <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the filepath entity of a plist_object should be 'equals'
                                                                                    <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M227"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M227"/>
   <xsl:template match="@*|node()" priority="-2" mode="M227">
      <xsl:apply-templates select="*" mode="M227"/>
   </xsl:template>

   <!--PATTERN macos-def_plist_state_dep-->


	<!--RULE -->
<xsl:template match="macos-def:plist_state" priority="1000" mode="M228">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED STATE: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M228"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M228"/>
   <xsl:template match="@*|node()" priority="-2" mode="M228">
      <xsl:apply-templates select="*" mode="M228"/>
   </xsl:template>

   <!--PATTERN macos-def_plist510_test-->


	<!--RULE -->
<xsl:template match="macos-def:plist510_test/macos-def:object" priority="1001" mode="M229">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/macos-def:plist510_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a plist510_test must reference a plist510_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M229"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="macos-def:plist510_test/macos-def:state" priority="1000" mode="M229">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/macos-def:plist510_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a plist510_test must reference a plist510_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M229"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M229"/>
   <xsl:template match="@*|node()" priority="-2" mode="M229">
      <xsl:apply-templates select="*" mode="M229"/>
   </xsl:template>

   <!--PATTERN macos-def_plist510_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="macos-def:plist510_object//oval-def:filter" priority="1000" mode="M230">
      <xsl:variable name="parent_object" select="ancestor::macos-def:plist510_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#macos') and ($state_name='plist510_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M230"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M230"/>
   <xsl:template match="@*|node()" priority="-2" mode="M230">
      <xsl:apply-templates select="*" mode="M230"/>
   </xsl:template>

   <!--PATTERN macos-def_plist510objappid-->


	<!--RULE -->
<xsl:template match="macos-def:plist510_object/macos-def:app_id" priority="1000" mode="M231">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(../macos-def:key/@xsi:nil='true' or ../macos-def:key/@xsi:nil='1')"/>
         <xsl:otherwise>
                                                                                          <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - key entity must not be nil when the app_id entity is used because preferences require a key.
                                                                                    <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M231"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M231"/>
   <xsl:template match="@*|node()" priority="-2" mode="M231">
      <xsl:apply-templates select="*" mode="M231"/>
   </xsl:template>

   <!--PATTERN macos-def_plist510objfilepath-->


	<!--RULE -->
<xsl:template match="macos-def:plist510_object/macos-def:filepath" priority="1000"
                 mode="M232">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals'"/>
         <xsl:otherwise>
                                                                                          <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the filepath entity of a plist510_object should be 'equals'
                                                                                    <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M232"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M232"/>
   <xsl:template match="@*|node()" priority="-2" mode="M232">
      <xsl:apply-templates select="*" mode="M232"/>
   </xsl:template>

   <!--PATTERN macos-def_pwpolicy_test_dep-->


	<!--RULE -->
<xsl:template match="macos-def:pwpolicy_test" priority="1000" mode="M233">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M233"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M233"/>
   <xsl:template match="@*|node()" priority="-2" mode="M233">
      <xsl:apply-templates select="*" mode="M233"/>
   </xsl:template>

   <!--PATTERN macos-def_pwpolicy_test-->


	<!--RULE -->
<xsl:template match="macos-def:pwpolicy_test/macos-def:object" priority="1001" mode="M234">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/macos-def:pwpolicy_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an pwpolicy_test must reference an pwpolicy_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M234"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="macos-def:pwpolicy_test/macos-def:state" priority="1000" mode="M234">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/macos-def:pwpolicy_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an pwpolicy_test must reference an pwpolicy_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M234"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M234"/>
   <xsl:template match="@*|node()" priority="-2" mode="M234">
      <xsl:apply-templates select="*" mode="M234"/>
   </xsl:template>

   <!--PATTERN macos-def_pwpolicy_object_dep-->


	<!--RULE -->
<xsl:template match="macos-def:pwpolicy_object" priority="1000" mode="M235">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED OBJECT: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M235"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M235"/>
   <xsl:template match="@*|node()" priority="-2" mode="M235">
      <xsl:apply-templates select="*" mode="M235"/>
   </xsl:template>

   <!--PATTERN macos-def_pwpolicy_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="macos-def:pwpolicy_object//oval-def:filter" priority="1000" mode="M236">
      <xsl:variable name="parent_object" select="ancestor::macos-def:pwpolicy_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#macos') and ($state_name='pwpolicy_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M236"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M236"/>
   <xsl:template match="@*|node()" priority="-2" mode="M236">
      <xsl:apply-templates select="*" mode="M236"/>
   </xsl:template>

   <!--PATTERN macos-def_pwpobjuserpass-->


	<!--RULE -->
<xsl:template match="macos-def:pwpolicy_object/macos-def:userpass" priority="1000"
                 mode="M237">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the userpass entity of a pwpolicy_object should be 'equals', note that this overrules the general operation attribute validation (i.e. follow this one)<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M237"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M237"/>
   <xsl:template match="@*|node()" priority="-2" mode="M237">
      <xsl:apply-templates select="*" mode="M237"/>
   </xsl:template>

   <!--PATTERN macos-def_pwpolicy_state_dep-->


	<!--RULE -->
<xsl:template match="macos-def:pwpolicy_state" priority="1000" mode="M238">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED STATE: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M238"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M238"/>
   <xsl:template match="@*|node()" priority="-2" mode="M238">
      <xsl:apply-templates select="*" mode="M238"/>
   </xsl:template>

   <!--PATTERN macos-def_pwpolicy59_test-->


	<!--RULE -->
<xsl:template match="macos-def:pwpolicy59_test/macos-def:object" priority="1001" mode="M239">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/macos-def:pwpolicy59_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an pwpolicy59_test must reference an pwpolicy59_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M239"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="macos-def:pwpolicy59_test/macos-def:state" priority="1000" mode="M239">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/macos-def:pwpolicy59_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an pwpolicy59_test must reference an pwpolicy59_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M239"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M239"/>
   <xsl:template match="@*|node()" priority="-2" mode="M239">
      <xsl:apply-templates select="*" mode="M239"/>
   </xsl:template>

   <!--PATTERN macos-def_pwpolicy59_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="macos-def:pwpolicy59_object//oval-def:filter" priority="1000"
                 mode="M240">
      <xsl:variable name="parent_object" select="ancestor::macos-def:pwpolicy59_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#macos') and ($state_name='pwpolicy59_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M240"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M240"/>
   <xsl:template match="@*|node()" priority="-2" mode="M240">
      <xsl:apply-templates select="*" mode="M240"/>
   </xsl:template>

   <!--PATTERN macos-def_pwp59objusername-->


	<!--RULE -->
<xsl:template match="macos-def:pwpolicy59_object/macos-def:username" priority="1000"
                 mode="M241">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not((@xsi:nil='1' or @xsi:nil='true')) or ../macos-def:userpass/@xsi:nil='true' or ../macos-def:userpass/@xsi:nil='1'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - userpass entity must be nil when username entity is nil<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M241"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M241"/>
   <xsl:template match="@*|node()" priority="-2" mode="M241">
      <xsl:apply-templates select="*" mode="M241"/>
   </xsl:template>

   <!--PATTERN macos-def_pwp59objuserpass-->


	<!--RULE -->
<xsl:template match="macos-def:pwpolicy59_object/macos-def:userpass" priority="1000"
                 mode="M242">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the userpass entity of a pwpolicy59_object should be 'equals', note that this overrules the general operation attribute validation (i.e. follow this one)<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not((@xsi:nil='1' or @xsi:nil='true')) or ../macos-def:username/@xsi:nil='true' or ../macos-def:username/@xsi:nil='1'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - username entity must be nil when userpass entity is nil<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M242"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M242"/>
   <xsl:template match="@*|node()" priority="-2" mode="M242">
      <xsl:apply-templates select="*" mode="M242"/>
   </xsl:template>

   <!--PATTERN pixos-def_linetst-->


	<!--RULE -->
<xsl:template match="pixos-def:line_test/pixos-def:object" priority="1001" mode="M243">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/pixos-def:line_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a line_test must reference a line_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M243"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="pixos-def:line_test/pixos-def:state" priority="1000" mode="M243">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/pixos-def:line_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a line_test must reference a line_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M243"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M243"/>
   <xsl:template match="@*|node()" priority="-2" mode="M243">
      <xsl:apply-templates select="*" mode="M243"/>
   </xsl:template>

   <!--PATTERN pixos-def_line_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="pixos-def:line_object//oval-def:filter" priority="1000" mode="M244">
      <xsl:variable name="parent_object" select="ancestor::pixos-def:line_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#pixos') and ($state_name='line_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M244"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M244"/>
   <xsl:template match="@*|node()" priority="-2" mode="M244">
      <xsl:apply-templates select="*" mode="M244"/>
   </xsl:template>

   <!--PATTERN pixos-def_vertst-->


	<!--RULE -->
<xsl:template match="pixos-def:version_test/pixos-def:object" priority="1001" mode="M245">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/pixos-def:version_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a version_test must reference a version_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M245"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="pixos-def:version_test/pixos-def:state" priority="1000" mode="M245">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/pixos-def:version_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a version_test must reference a version_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M245"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M245"/>
   <xsl:template match="@*|node()" priority="-2" mode="M245">
      <xsl:apply-templates select="*" mode="M245"/>
   </xsl:template>

   <!--PATTERN sp-def_webapptst-->


	<!--RULE -->
<xsl:template match="sp-def:spwebapplication_test/sp-def:object" priority="1001" mode="M246">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/sp-def:spwebapplication_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a spwebapplication_test must reference an spwebapplication_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M246"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="sp-def:spwebapplication_test/sp-def:state" priority="1000" mode="M246">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/sp-def:spwebapplication_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a spwebapplication_test must reference an spwebapplication_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M246"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M246"/>
   <xsl:template match="@*|node()" priority="-2" mode="M246">
      <xsl:apply-templates select="*" mode="M246"/>
   </xsl:template>

   <!--PATTERN sp-def_spwebapplication_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="sp-def:spwebapplication_object//oval-def:filter" priority="1000"
                 mode="M247">
      <xsl:variable name="parent_object" select="ancestor::sp-def:spwebapplication_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#sharepoint') and ($state_name='spwebapplication_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M247"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M247"/>
   <xsl:template match="@*|node()" priority="-2" mode="M247">
      <xsl:apply-templates select="*" mode="M247"/>
   </xsl:template>

   <!--PATTERN sp-def_grptst-->


	<!--RULE -->
<xsl:template match="sp-def:spgroup_test/sp-def:object" priority="1001" mode="M248">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/sp-def:spgroup_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a spgroup_test must reference a spgroup_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M248"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="sp-def:spgroup_test/sp-def:state" priority="1000" mode="M248">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/sp-def:spgroup_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a spgroup_test must reference a spgroup_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M248"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M248"/>
   <xsl:template match="@*|node()" priority="-2" mode="M248">
      <xsl:apply-templates select="*" mode="M248"/>
   </xsl:template>

   <!--PATTERN sp-def_spgroup_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="sp-def:spgroup_object//oval-def:filter" priority="1000" mode="M249">
      <xsl:variable name="parent_object" select="ancestor::sp-def:spgroup_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#sharepoint') and ($state_name='spgroup_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M249"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M249"/>
   <xsl:template match="@*|node()" priority="-2" mode="M249">
      <xsl:apply-templates select="*" mode="M249"/>
   </xsl:template>

   <!--PATTERN sp-def_webtst-->


	<!--RULE -->
<xsl:template match="sp-def:spweb_test/sp-def:object" priority="1001" mode="M250">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/sp-def:spweb_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a spweb_test must reference an spweb_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M250"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="sp-def:spweb_test/sp-def:state" priority="1000" mode="M250">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/sp-def:spweb_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a spweb_test must reference an spweb_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M250"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M250"/>
   <xsl:template match="@*|node()" priority="-2" mode="M250">
      <xsl:apply-templates select="*" mode="M250"/>
   </xsl:template>

   <!--PATTERN sp-def_spweb_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="sp-def:spweb_object//oval-def:filter" priority="1000" mode="M251">
      <xsl:variable name="parent_object" select="ancestor::sp-def:spweb_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#sharepoint') and ($state_name='spweb_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M251"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M251"/>
   <xsl:template match="@*|node()" priority="-2" mode="M251">
      <xsl:apply-templates select="*" mode="M251"/>
   </xsl:template>

   <!--PATTERN sp-def_listtst-->


	<!--RULE -->
<xsl:template match="sp-def:splist_test/sp-def:object" priority="1001" mode="M252">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/sp-def:splist_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a splist_test must reference an splist_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M252"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="sp-def:splist_test/sp-def:state" priority="1000" mode="M252">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/sp-def:splist_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a splist_test must reference an splist_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M252"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M252"/>
   <xsl:template match="@*|node()" priority="-2" mode="M252">
      <xsl:apply-templates select="*" mode="M252"/>
   </xsl:template>

   <!--PATTERN sp-def_splist_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="sp-def:splist_object//oval-def:filter" priority="1000" mode="M253">
      <xsl:variable name="parent_object" select="ancestor::sp-def:splist_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#sharepoint') and ($state_name='splist_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M253"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M253"/>
   <xsl:template match="@*|node()" priority="-2" mode="M253">
      <xsl:apply-templates select="*" mode="M253"/>
   </xsl:template>

   <!--PATTERN sp-def_avstst-->


	<!--RULE -->
<xsl:template match="sp-def:spantivirussettings_test/sp-def:object" priority="1001"
                 mode="M254">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/sp-def:spantivirussettings_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a spantivirussettings_test must reference an spantivirussettings_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M254"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="sp-def:spantivirussettings_test/sp-def:state" priority="1000"
                 mode="M254">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/sp-def:spantivirussettings_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a spantivirussettings_test must reference an spantivirussettings_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M254"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M254"/>
   <xsl:template match="@*|node()" priority="-2" mode="M254">
      <xsl:apply-templates select="*" mode="M254"/>
   </xsl:template>

   <!--PATTERN sp-def_spantivirussettings_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="sp-def:spantivirussettings_object//oval-def:filter" priority="1000"
                 mode="M255">
      <xsl:variable name="parent_object" select="ancestor::sp-def:spantivirussettings_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#sharepoint') and ($state_name='spantivirussettings_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M255"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M255"/>
   <xsl:template match="@*|node()" priority="-2" mode="M255">
      <xsl:apply-templates select="*" mode="M255"/>
   </xsl:template>

   <!--PATTERN sp-def_siteadmintst-->


	<!--RULE -->
<xsl:template match="sp-def:spsiteadministration_test/sp-def:object" priority="1001"
                 mode="M256">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/sp-def:spsiteadministration_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a spsiteadministration_test must reference an spsiteadministration_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M256"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="sp-def:spsiteadministration_test/sp-def:state" priority="1000"
                 mode="M256">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/sp-def:spsiteadministration_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a spsiteadministration_test must reference an spsiteadministration_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M256"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M256"/>
   <xsl:template match="@*|node()" priority="-2" mode="M256">
      <xsl:apply-templates select="*" mode="M256"/>
   </xsl:template>

   <!--PATTERN sp-def_spsiteadministration_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="sp-def:spsiteadministration_object//oval-def:filter" priority="1000"
                 mode="M257">
      <xsl:variable name="parent_object" select="ancestor::sp-def:spsiteadministration_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#sharepoint') and ($state_name='spsiteadministration_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M257"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M257"/>
   <xsl:template match="@*|node()" priority="-2" mode="M257">
      <xsl:apply-templates select="*" mode="M257"/>
   </xsl:template>

   <!--PATTERN sp-def_sitetst-->


	<!--RULE -->
<xsl:template match="sp-def:spsite_test/sp-def:object" priority="1001" mode="M258">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/sp-def:spsite_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a spsite_test must reference an spsite_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M258"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="sp-def:spsite_test/sp-def:state" priority="1000" mode="M258">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/sp-def:spsite_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a spsite_test must reference an spsite_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M258"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M258"/>
   <xsl:template match="@*|node()" priority="-2" mode="M258">
      <xsl:apply-templates select="*" mode="M258"/>
   </xsl:template>

   <!--PATTERN sp-def_spsite_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="sp-def:spsite_object//oval-def:filter" priority="1000" mode="M259">
      <xsl:variable name="parent_object" select="ancestor::sp-def:spsite_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#sharepoint') and ($state_name='spsite_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M259"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M259"/>
   <xsl:template match="@*|node()" priority="-2" mode="M259">
      <xsl:apply-templates select="*" mode="M259"/>
   </xsl:template>

   <!--PATTERN sp_def_spsite_state_url_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:states/sp-def:spsite_state/sp-def:url"
                 priority="1000"
                 mode="M260">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED ENTITY IN: sp-def:spsite_state <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M260"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M260"/>
   <xsl:template match="@*|node()" priority="-2" mode="M260">
      <xsl:apply-templates select="*" mode="M260"/>
   </xsl:template>

   <!--PATTERN sp-def_crtst-->


	<!--RULE -->
<xsl:template match="sp-def:spcrawlrule_test/sp-def:object" priority="1001" mode="M261">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/sp-def:spcrawlrule_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a spcrawlrule_test must reference an spcrawlrule_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M261"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="sp-def:spcrawlrule_test/sp-def:state" priority="1000" mode="M261">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/sp-def:spcrawlrule_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a spcrawlrule_test must reference an spcrawlrule_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M261"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M261"/>
   <xsl:template match="@*|node()" priority="-2" mode="M261">
      <xsl:apply-templates select="*" mode="M261"/>
   </xsl:template>

   <!--PATTERN sp-def_spcrawlrule_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="sp-def:spcrawlrule_object//oval-def:filter" priority="1000" mode="M262">
      <xsl:variable name="parent_object" select="ancestor::sp-def:spcrawlrule_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#sharepoint') and ($state_name='spcrawlrule_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M262"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M262"/>
   <xsl:template match="@*|node()" priority="-2" mode="M262">
      <xsl:apply-templates select="*" mode="M262"/>
   </xsl:template>

   <!--PATTERN sp-def_spjobdefinition_test_dep-->


	<!--RULE -->
<xsl:template match="sp-def:spjobdefinition_test" priority="1000" mode="M263">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M263"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M263"/>
   <xsl:template match="@*|node()" priority="-2" mode="M263">
      <xsl:apply-templates select="*" mode="M263"/>
   </xsl:template>

   <!--PATTERN sp-def_jobdeftst-->


	<!--RULE -->
<xsl:template match="sp-def:spjobdefinition_test/sp-def:object" priority="1001" mode="M264">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/sp-def:spjobdefinition_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a spjobdefinition_test must reference an spjobdefinition_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M264"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="sp-def:spjobdefinition_test/sp-def:state" priority="1000" mode="M264">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/sp-def:spjobdefinition_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a spjobdefinition_test must reference an spjobdefinition_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M264"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M264"/>
   <xsl:template match="@*|node()" priority="-2" mode="M264">
      <xsl:apply-templates select="*" mode="M264"/>
   </xsl:template>

   <!--PATTERN sp-def_spjobdefinition_object_dep-->


	<!--RULE -->
<xsl:template match="sp-def:spjobdefinition_object" priority="1000" mode="M265">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M265"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M265"/>
   <xsl:template match="@*|node()" priority="-2" mode="M265">
      <xsl:apply-templates select="*" mode="M265"/>
   </xsl:template>

   <!--PATTERN sp-def_spjobdefinition_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="sp-def:spjobdefinition_object//oval-def:filter" priority="1000"
                 mode="M266">
      <xsl:variable name="parent_object" select="ancestor::sp-def:spjobdefinition_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#sharepoint') and ($state_name='spjobdefinition_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M266"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M266"/>
   <xsl:template match="@*|node()" priority="-2" mode="M266">
      <xsl:apply-templates select="*" mode="M266"/>
   </xsl:template>

   <!--PATTERN sp-def_spjobdefinition_state_dep-->


	<!--RULE -->
<xsl:template match="sp-def:spjobdefinition_state" priority="1000" mode="M267">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M267"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M267"/>
   <xsl:template match="@*|node()" priority="-2" mode="M267">
      <xsl:apply-templates select="*" mode="M267"/>
   </xsl:template>

   <!--PATTERN sp-def_jobdef510tst-->


	<!--RULE -->
<xsl:template match="sp-def:spjobdefinition510_test/sp-def:object" priority="1001"
                 mode="M268">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/sp-def:spjobdefinition510_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a spjobdefinition510_test must reference an spjobdefinition510_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M268"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="sp-def:spjobdefinition510_test/sp-def:state" priority="1000" mode="M268">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/sp-def:spjobdefinition510_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a spjobdefinition510_test must reference an spjobdefinition510_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M268"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M268"/>
   <xsl:template match="@*|node()" priority="-2" mode="M268">
      <xsl:apply-templates select="*" mode="M268"/>
   </xsl:template>

   <!--PATTERN sp-def_spjobdefinition510_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="sp-def:spjobdefinition510_object//oval-def:filter" priority="1000"
                 mode="M269">
      <xsl:variable name="parent_object" select="ancestor::sp-def:spjobdefinition510_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#sharepoint') and ($state_name='spjobdefinition510_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M269"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M269"/>
   <xsl:template match="@*|node()" priority="-2" mode="M269">
      <xsl:apply-templates select="*" mode="M269"/>
   </xsl:template>

   <!--PATTERN sp-def_bbtst-->


	<!--RULE -->
<xsl:template match="sp-def:bestbet_test/sp-def:object" priority="1001" mode="M270">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/sp-def:bestbet_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a bestbet_test must reference an bestbet_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M270"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="sp-def:bestbet_test/sp-def:state" priority="1000" mode="M270">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/sp-def:bestbet_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a bestbet_test must reference an bestbet_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M270"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M270"/>
   <xsl:template match="@*|node()" priority="-2" mode="M270">
      <xsl:apply-templates select="*" mode="M270"/>
   </xsl:template>

   <!--PATTERN sp-def_bestbet_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="sp-def:bestbet_object//oval-def:filter" priority="1000" mode="M271">
      <xsl:variable name="parent_object" select="ancestor::sp-def:bestbet_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#sharepoint') and ($state_name='bestbet_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M271"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M271"/>
   <xsl:template match="@*|node()" priority="-2" mode="M271">
      <xsl:apply-templates select="*" mode="M271"/>
   </xsl:template>

   <!--PATTERN sp-def_infopolicycolltst-->


	<!--RULE -->
<xsl:template match="sp-def:policycoll_test/sp-def:object" priority="1001" mode="M272">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/sp-def:policycoll_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a policycoll_test must reference an policycoll_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M272"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="sp-def:policycoll_test/sp-def:state" priority="1000" mode="M272">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/sp-def:policycoll_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a policycoll_test must reference an policycoll_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M272"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M272"/>
   <xsl:template match="@*|node()" priority="-2" mode="M272">
      <xsl:apply-templates select="*" mode="M272"/>
   </xsl:template>

   <!--PATTERN sp-def_infopolicycoll_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="sp-def:infopolicycoll_object//oval-def:filter" priority="1000"
                 mode="M273">
      <xsl:variable name="parent_object" select="ancestor::sp-def:infopolicycoll_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#sharepoint') and ($state_name='infopolicycoll_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M273"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M273"/>
   <xsl:template match="@*|node()" priority="-2" mode="M273">
      <xsl:apply-templates select="*" mode="M273"/>
   </xsl:template>

   <!--PATTERN sp-def_diagnosticsservicetest-->


	<!--RULE -->
<xsl:template match="sp-def:spdiagnosticsservice_test/sp-def:object" priority="1001"
                 mode="M274">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/sp-def:spdiagnosticsservice_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an spdiagnosticsservice_test must reference an spdiagnosticsservice_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M274"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="sp-def:spdiagnosticsservice_test/sp-def:state" priority="1000"
                 mode="M274">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/sp-def:spdiagnosticsservice_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an spdiagnosticsservice_test must reference an spdiagnosticsservice_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M274"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M274"/>
   <xsl:template match="@*|node()" priority="-2" mode="M274">
      <xsl:apply-templates select="*" mode="M274"/>
   </xsl:template>

   <!--PATTERN sp-def_spdiagnosticsservice_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="sp-def:spdiagnosticsservice_object//oval-def:filter" priority="1000"
                 mode="M275">
      <xsl:variable name="parent_object" select="ancestor::sp-def:spdiagnosticsservice_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#sharepoint') and ($state_name='spdiagnosticsservice_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M275"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M275"/>
   <xsl:template match="@*|node()" priority="-2" mode="M275">
      <xsl:apply-templates select="*" mode="M275"/>
   </xsl:template>

   <!--PATTERN sp-def_diagnostics_level_test-->


	<!--RULE -->
<xsl:template match="sp-def:spdiagnosticslevel_test/sp-def:object" priority="1001"
                 mode="M276">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/sp-def:spdiagnosticslevel_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an spdiagnosticslevel_test must reference an spdiagnosticslevel_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M276"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="sp-def:spdiagnosticslevel_test/sp-def:state" priority="1000" mode="M276">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/sp-def:spdiagnosticslevel_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an spdiagnosticslevel_test must reference an spdiagnosticslevel_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M276"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M276"/>
   <xsl:template match="@*|node()" priority="-2" mode="M276">
      <xsl:apply-templates select="*" mode="M276"/>
   </xsl:template>

   <!--PATTERN sp-def_spdiagnosticslevel_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="sp-def:spdiagnosticslevel_object//oval-def:filter" priority="1000"
                 mode="M277">
      <xsl:variable name="parent_object" select="ancestor::sp-def:spdiagnosticslevel_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#sharepoint') and ($state_name='spdiagnosticslevel_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M277"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M277"/>
   <xsl:template match="@*|node()" priority="-2" mode="M277">
      <xsl:apply-templates select="*" mode="M277"/>
   </xsl:template>

   <!--PATTERN sp-def_policyfeature_test-->


	<!--RULE -->
<xsl:template match="sp-def:sppolicyfeature_test/sp-def:object" priority="1001" mode="M278">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/sp-def:sppolicyfeature_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an sppolicyfeature_test must reference an sppolicyfeature_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M278"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="sp-def:sppolicyfeature_test/sp-def:state" priority="1000" mode="M278">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/sp-def:sppolicyfeature_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an sppolicyfeature_test must reference an sppolicyfeature_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M278"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M278"/>
   <xsl:template match="@*|node()" priority="-2" mode="M278">
      <xsl:apply-templates select="*" mode="M278"/>
   </xsl:template>

   <!--PATTERN sp-def_sppolicyfeature_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="sp-def:sppolicyfeature_object//oval-def:filter" priority="1000"
                 mode="M279">
      <xsl:variable name="parent_object" select="ancestor::sp-def:sppolicyfeature_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#sharepoint') and ($state_name='sppolicyfeature_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M279"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M279"/>
   <xsl:template match="@*|node()" priority="-2" mode="M279">
      <xsl:apply-templates select="*" mode="M279"/>
   </xsl:template>

   <!--PATTERN sp-def_policy_test-->


	<!--RULE -->
<xsl:template match="sp-def:sppolicy_test/sp-def:object" priority="1001" mode="M280">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/sp-def:sppolicy_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an sppolicy_test must reference an sppolicy_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M280"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="sp-def:sppolicy_test/sp-def:state" priority="1000" mode="M280">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/sp-def:sppolicy_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an sppolicy_test must reference an sppolicy_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M280"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M280"/>
   <xsl:template match="@*|node()" priority="-2" mode="M280">
      <xsl:apply-templates select="*" mode="M280"/>
   </xsl:template>

   <!--PATTERN sol-def_isainfotst-->


	<!--RULE -->
<xsl:template match="sol-def:isainfo_test/sol-def:object" priority="1001" mode="M281">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/sol-def:isainfo_object/@id"/>
         <xsl:otherwise>
                                          <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an isainfo_test must reference an isainfo_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M281"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="sol-def:isainfo_test/sol-def:state" priority="1000" mode="M281">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/sol-def:isainfo_state/@id"/>
         <xsl:otherwise>
                                          <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an isainfo_test must reference an isainfo_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M281"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M281"/>
   <xsl:template match="@*|node()" priority="-2" mode="M281">
      <xsl:apply-templates select="*" mode="M281"/>
   </xsl:template>

   <!--PATTERN sol-def_ndd_test-->


	<!--RULE -->
<xsl:template match="sol-def:ndd_test/sol-def:object" priority="1001" mode="M282">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/sol-def:ndd_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an ndd_test must reference an ndd_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M282"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="sol-def:ndd_test/sol-def:state" priority="1000" mode="M282">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/sol-def:ndd_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an ndd_test must reference an ndd_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M282"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M282"/>
   <xsl:template match="@*|node()" priority="-2" mode="M282">
      <xsl:apply-templates select="*" mode="M282"/>
   </xsl:template>

   <!--PATTERN sol-def_ndd_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="sol-def:ndd_object//oval-def:filter" priority="1000" mode="M283">
      <xsl:variable name="parent_object" select="ancestor::sol-def:ndd_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#solaris') and ($state_name='ndd_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M283"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M283"/>
   <xsl:template match="@*|node()" priority="-2" mode="M283">
      <xsl:apply-templates select="*" mode="M283"/>
   </xsl:template>

   <!--PATTERN sol-def_packagetst-->


	<!--RULE -->
<xsl:template match="sol-def:package_test/sol-def:object" priority="1001" mode="M284">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/sol-def:package_object/@id"/>
         <xsl:otherwise>
                                          <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a package_test must reference a package_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M284"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="sol-def:package_test/sol-def:state" priority="1000" mode="M284">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/sol-def:package_state/@id"/>
         <xsl:otherwise>
                                          <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a package_test must reference a package_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M284"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M284"/>
   <xsl:template match="@*|node()" priority="-2" mode="M284">
      <xsl:apply-templates select="*" mode="M284"/>
   </xsl:template>

   <!--PATTERN sol-def_package_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="sol-def:package_object//oval-def:filter" priority="1000" mode="M285">
      <xsl:variable name="parent_object" select="ancestor::sol-def:package_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#solaris') and ($state_name='package_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M285"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M285"/>
   <xsl:template match="@*|node()" priority="-2" mode="M285">
      <xsl:apply-templates select="*" mode="M285"/>
   </xsl:template>

   <!--PATTERN sol-def_packagechecktst-->


	<!--RULE -->
<xsl:template match="sol-def:packagecheck_test/sol-def:object" priority="1001" mode="M286">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/sol-def:packagecheck_object/@id"/>
         <xsl:otherwise>
                                          <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a packagecheck_test must reference a packagecheck_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M286"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="sol-def:packagecheck_test/sol-def:state" priority="1000" mode="M286">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/sol-def:packagecheck_state/@id"/>
         <xsl:otherwise>
                                          <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a packagecheck_test must reference a packagecheck_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M286"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M286"/>
   <xsl:template match="@*|node()" priority="-2" mode="M286">
      <xsl:apply-templates select="*" mode="M286"/>
   </xsl:template>

   <!--PATTERN sol-def_packagecheck_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="sol-def:packagecheck_object//oval-def:filter" priority="1000"
                 mode="M287">
      <xsl:variable name="parent_object" select="ancestor::sol-def:packagecheck_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#solaris') and ($state_name='packagecheck_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M287"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M287"/>
   <xsl:template match="@*|node()" priority="-2" mode="M287">
      <xsl:apply-templates select="*" mode="M287"/>
   </xsl:template>

   <!--PATTERN sol-def_patch54tst-->


	<!--RULE -->
<xsl:template match="sol-def:patch54_test/sol-def:object" priority="1001" mode="M288">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/sol-def:patch54_object/@id"/>
         <xsl:otherwise>
                                          <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a patch54_test must reference a patch54_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M288"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="sol-def:patch54_test/sol-def:state" priority="1000" mode="M288">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/sol-def:patch_state/@id"/>
         <xsl:otherwise>
                                          <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a patch54_test must reference a patch_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M288"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M288"/>
   <xsl:template match="@*|node()" priority="-2" mode="M288">
      <xsl:apply-templates select="*" mode="M288"/>
   </xsl:template>

   <!--PATTERN sol-def_patchtst_dep-->


	<!--RULE -->
<xsl:template match="sol-def:patch_test" priority="1000" mode="M289">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M289"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M289"/>
   <xsl:template match="@*|node()" priority="-2" mode="M289">
      <xsl:apply-templates select="*" mode="M289"/>
   </xsl:template>

   <!--PATTERN sol-def_patchtst-->


	<!--RULE -->
<xsl:template match="sol-def:patch_test/sol-def:object" priority="1001" mode="M290">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/sol-def:patch_object/@id"/>
         <xsl:otherwise>
                                          <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a patch_test must reference a patch_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M290"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="sol-def:patch_test/sol-def:state" priority="1000" mode="M290">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/sol-def:patch_state/@id"/>
         <xsl:otherwise>
                                          <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a patch_test must reference a patch_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M290"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M290"/>
   <xsl:template match="@*|node()" priority="-2" mode="M290">
      <xsl:apply-templates select="*" mode="M290"/>
   </xsl:template>

   <!--PATTERN sol-def_patch54_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="sol-def:patch54_object//oval-def:filter" priority="1000" mode="M291">
      <xsl:variable name="parent_object" select="ancestor::sol-def:patch54_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#solaris') and ($state_name='patch_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M291"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M291"/>
   <xsl:template match="@*|node()" priority="-2" mode="M291">
      <xsl:apply-templates select="*" mode="M291"/>
   </xsl:template>

   <!--PATTERN sol-def_patchobj_dep-->


	<!--RULE -->
<xsl:template match="sol-def:patch_object" priority="1000" mode="M292">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED OBJECT: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M292"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M292"/>
   <xsl:template match="@*|node()" priority="-2" mode="M292">
      <xsl:apply-templates select="*" mode="M292"/>
   </xsl:template>

   <!--PATTERN sol-def_smftst-->


	<!--RULE -->
<xsl:template match="sol-def:smf_test/sol-def:object" priority="1001" mode="M293">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/sol-def:smf_object/@id"/>
         <xsl:otherwise>
                                          <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a smf_test must reference a smf_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M293"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="sol-def:smf_test/sol-def:state" priority="1000" mode="M293">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/sol-def:smf_state/@id"/>
         <xsl:otherwise>
                                          <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a smf_test must reference a smf_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M293"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M293"/>
   <xsl:template match="@*|node()" priority="-2" mode="M293">
      <xsl:apply-templates select="*" mode="M293"/>
   </xsl:template>

   <!--PATTERN sol-def_smf_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="sol-def:smf_object//oval-def:filter" priority="1000" mode="M294">
      <xsl:variable name="parent_object" select="ancestor::sol-def:smf_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#solaris') and ($state_name='smf_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M294"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M294"/>
   <xsl:template match="@*|node()" priority="-2" mode="M294">
      <xsl:apply-templates select="*" mode="M294"/>
   </xsl:template>

   <!--PATTERN unix-def_dnscachetst-->


	<!--RULE -->
<xsl:template match="unix-def:dnscache_test/unix-def:object" priority="1001" mode="M295">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/unix-def:dnscache_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a dnscache_test must reference a dnscache_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M295"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="unix-def:dnscache_test/unix-def:state" priority="1000" mode="M295">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/unix-def:dnscache_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a dnscache_test must reference a dnscache_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M295"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M295"/>
   <xsl:template match="@*|node()" priority="-2" mode="M295">
      <xsl:apply-templates select="*" mode="M295"/>
   </xsl:template>

   <!--PATTERN unix-def_dnscache_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="unix-def:dnscache_object//oval-def:filter" priority="1000" mode="M296">
      <xsl:variable name="parent_object" select="ancestor::unix-def:dnscache_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#unix') and ($state_name='dnscache_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M296"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M296"/>
   <xsl:template match="@*|node()" priority="-2" mode="M296">
      <xsl:apply-templates select="*" mode="M296"/>
   </xsl:template>

   <!--PATTERN unix-def_filetst-->


	<!--RULE -->
<xsl:template match="unix-def:file_test/unix-def:object" priority="1001" mode="M297">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/unix-def:file_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a file_test must reference a file_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M297"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="unix-def:file_test/unix-def:state" priority="1000" mode="M297">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/unix-def:file_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a file_test must reference a file_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M297"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M297"/>
   <xsl:template match="@*|node()" priority="-2" mode="M297">
      <xsl:apply-templates select="*" mode="M297"/>
   </xsl:template>

   <!--PATTERN unix-def_file_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="unix-def:file_object//oval-def:filter" priority="1000" mode="M298">
      <xsl:variable name="parent_object" select="ancestor::unix-def:file_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#unix') and ($state_name='file_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M298"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M298"/>
   <xsl:template match="@*|node()" priority="-2" mode="M298">
      <xsl:apply-templates select="*" mode="M298"/>
   </xsl:template>

   <!--PATTERN unix-def_fileobjfilepath-->


	<!--RULE -->
<xsl:template match="unix-def:file_object/unix-def:filepath" priority="1000" mode="M299">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::unix-def:behaviors[@max_depth or @recurse or @recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth, recurse, and recurse_direction behaviors are not allowed with a filepath entity<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M299"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M299"/>
   <xsl:template match="@*|node()" priority="-2" mode="M299">
      <xsl:apply-templates select="*" mode="M299"/>
   </xsl:template>

   <!--PATTERN unix-def_fileobjfilepath2-->


	<!--RULE -->
<xsl:template match="unix-def:file_object/unix-def:filepath[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M300">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::unix-def:behaviors[@recurse_file_system='defined'])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_file_system behavior MUST not be set to 'defined' when a pattern match is used with a filepath entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M300"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M300"/>
   <xsl:template match="@*|node()" priority="-2" mode="M300">
      <xsl:apply-templates select="*" mode="M300"/>
   </xsl:template>

   <!--PATTERN unix-def_fileobjpath-->


	<!--RULE -->
<xsl:template match="unix-def:file_object/unix-def:path[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M301">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::unix-def:behaviors[@recurse_file_system='defined'])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_file_system behavior MUST not be set to 'defined' when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::unix-def:behaviors[@max_depth])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::unix-def:behaviors[@recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_direction behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::unix-def:behaviors[@recurse])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M301"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M301"/>
   <xsl:template match="@*|node()" priority="-2" mode="M301">
      <xsl:apply-templates select="*" mode="M301"/>
   </xsl:template>

   <!--PATTERN unix-def_file_objectfilename-->


	<!--RULE -->
<xsl:template match="unix-def:file_object/unix-def:filename" priority="1000" mode="M302">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="(@var_ref and .='') or (@xsi:nil='1' or @xsi:nil='true') or not(.='') or (.='' and @operation = 'pattern match')"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - filename entity cannot be empty unless the xsi:nil attribute is set to true or a var_ref is used<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M302"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M302"/>
   <xsl:template match="@*|node()" priority="-2" mode="M302">
      <xsl:apply-templates select="*" mode="M302"/>
   </xsl:template>

   <!--PATTERN unix-def_recurse_value_file_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:objects/unix-def:file_object/unix-def:behaviors"
                 priority="1000"
                 mode="M303">

		<!--REPORT -->
<xsl:if test="@recurse='files'">DEPRECATED ATTRIBUTE VALUE IN: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ATTRIBUTE VALUE: <xsl:text/>
         <xsl:value-of select="@recurse"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>

		    <!--REPORT -->
<xsl:if test="@recurse='files and directories'">DEPRECATED ATTRIBUTE VALUE IN: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ATTRIBUTE VALUE: <xsl:text/>
         <xsl:value-of select="@recurse"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>

		    <!--REPORT -->
<xsl:if test="@recurse='none'"> DEPRECATED ATTRIBUTE VALUE IN: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ATTRIBUTE VALUE: <xsl:text/>
         <xsl:value-of select="@recurse"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M303"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M303"/>
   <xsl:template match="@*|node()" priority="-2" mode="M303">
      <xsl:apply-templates select="*" mode="M303"/>
   </xsl:template>

   <!--PATTERN unix-def_file_ea_tst-->


	<!--RULE -->
<xsl:template match="unix-def:filextendedattribute_test/unix-def:object" priority="1001"
                 mode="M304">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/unix-def:fileextendedattribute_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a fileextendedattribute_test must reference a fileextendedattribute_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M304"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="unix-def:fileextendedattribute_test/unix-def:state" priority="1000"
                 mode="M304">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/unix-def:fileextendedattribute_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a fileextendedattribute_test must reference a fileextendedattribute_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M304"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M304"/>
   <xsl:template match="@*|node()" priority="-2" mode="M304">
      <xsl:apply-templates select="*" mode="M304"/>
   </xsl:template>

   <!--PATTERN unix-def_fileextendedattribute_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="unix-def:fileextendedattribute_object//oval-def:filter" priority="1000"
                 mode="M305">
      <xsl:variable name="parent_object" select="ancestor::unix-def:fileextendedattribute_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#unix') and ($state_name='fileextendedattribute_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M305"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M305"/>
   <xsl:template match="@*|node()" priority="-2" mode="M305">
      <xsl:apply-templates select="*" mode="M305"/>
   </xsl:template>

   <!--PATTERN unix-def_file_ea_objfilepath-->


	<!--RULE -->
<xsl:template match="unix-def:fileextendedattribute_object/unix-def:filepath" priority="1000"
                 mode="M306">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::unix-def:behaviors[@max_depth or @recurse or @recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth, recurse, and recurse_direction behaviors are not allowed with a filepath entity<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M306"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M306"/>
   <xsl:template match="@*|node()" priority="-2" mode="M306">
      <xsl:apply-templates select="*" mode="M306"/>
   </xsl:template>

   <!--PATTERN unix-def_file_ea_objfilepath2-->


	<!--RULE -->
<xsl:template match="unix-def:fileextendedattribute_object/unix-def:filepath[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M307">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::unix-def:behaviors[@recurse_file_system='defined'])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_file_system behavior MUST not be set to 'defined' when a pattern match is used with a filepath entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M307"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M307"/>
   <xsl:template match="@*|node()" priority="-2" mode="M307">
      <xsl:apply-templates select="*" mode="M307"/>
   </xsl:template>

   <!--PATTERN unix-def_file_ea_objpath-->


	<!--RULE -->
<xsl:template match="unix-def:fileextendedattribute_object/unix-def:path[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M308">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::unix-def:behaviors[@recurse_file_system='defined'])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_file_system behavior MUST not be set to 'defined' when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::unix-def:behaviors[@max_depth])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::unix-def:behaviors[@recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_direction behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::unix-def:behaviors[@recurse])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M308"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M308"/>
   <xsl:template match="@*|node()" priority="-2" mode="M308">
      <xsl:apply-templates select="*" mode="M308"/>
   </xsl:template>

   <!--PATTERN unix-def_fileextendedattribute_objectfilename-->


	<!--RULE -->
<xsl:template match="unix-def:fileextendedattribute_object/unix-def:filename" priority="1000"
                 mode="M309">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="(@var_ref and .='') or ((@xsi:nil='1' or @xsi:nil='true') and .='') or not(.='') or (.='' and @operation = 'pattern match')"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - filename entity cannot be empty unless the xsi:nil attribute is set to true or a var_ref is used<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M309"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M309"/>
   <xsl:template match="@*|node()" priority="-2" mode="M309">
      <xsl:apply-templates select="*" mode="M309"/>
   </xsl:template>

   <!--PATTERN unix-def_gconf_test-->


	<!--RULE -->
<xsl:template match="unix-def:gconf_test/unix-def:object" priority="1001" mode="M310">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/unix-def:gconf_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a gconf_test must reference an gconf_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M310"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="unix-def:gconf_test/unix-def:state" priority="1000" mode="M310">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/unix-def:gconf_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a gconf_test must reference an gconf_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M310"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M310"/>
   <xsl:template match="@*|node()" priority="-2" mode="M310">
      <xsl:apply-templates select="*" mode="M310"/>
   </xsl:template>

   <!--PATTERN unix-def_gconf_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="unix-def:gconf_object//oval-def:filter" priority="1000" mode="M311">
      <xsl:variable name="parent_object" select="ancestor::unix-def:gconf_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#unix') and ($state_name='gconf_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M311"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M311"/>
   <xsl:template match="@*|node()" priority="-2" mode="M311">
      <xsl:apply-templates select="*" mode="M311"/>
   </xsl:template>

   <!--PATTERN unix-def_gconfobjsource-->


	<!--RULE -->
<xsl:template match="unix-def:gconf_object/unix-def:source" priority="1000" mode="M312">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals'"/>
         <xsl:otherwise>
                                                                                    <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the source entity of a gconf_object should be 'equals'
                                                                              <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M312"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M312"/>
   <xsl:template match="@*|node()" priority="-2" mode="M312">
      <xsl:apply-templates select="*" mode="M312"/>
   </xsl:template>

   <!--PATTERN unix-def_inetdtst-->


	<!--RULE -->
<xsl:template match="unix-def:inetd_test/unix-def:object" priority="1001" mode="M313">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/unix-def:inetd_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an inetd_test must reference an inetd_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M313"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="unix-def:inetd_test/unix-def:state" priority="1000" mode="M313">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/unix-def:inetd_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an inetd_test must reference an inetd_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M313"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M313"/>
   <xsl:template match="@*|node()" priority="-2" mode="M313">
      <xsl:apply-templates select="*" mode="M313"/>
   </xsl:template>

   <!--PATTERN unix-def_inetd_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="unix-def:inetd_object//oval-def:filter" priority="1000" mode="M314">
      <xsl:variable name="parent_object" select="ancestor::unix-def:inetd_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#unix') and ($state_name='inetd_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M314"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M314"/>
   <xsl:template match="@*|node()" priority="-2" mode="M314">
      <xsl:apply-templates select="*" mode="M314"/>
   </xsl:template>

   <!--PATTERN unix-def_interfacetst-->


	<!--RULE -->
<xsl:template match="unix-def:interface_test/unix-def:object" priority="1001" mode="M315">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/unix-def:interface_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an interface_test must reference an interface_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M315"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="unix-def:interface_test/unix-def:state" priority="1000" mode="M315">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/unix-def:interface_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an interface_test must reference an interface_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M315"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M315"/>
   <xsl:template match="@*|node()" priority="-2" mode="M315">
      <xsl:apply-templates select="*" mode="M315"/>
   </xsl:template>

   <!--PATTERN unix-def_interface_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="unix-def:interface_object//oval-def:filter" priority="1000" mode="M316">
      <xsl:variable name="parent_object" select="ancestor::unix-def:interface_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#unix') and ($state_name='interface_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M316"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M316"/>
   <xsl:template match="@*|node()" priority="-2" mode="M316">
      <xsl:apply-templates select="*" mode="M316"/>
   </xsl:template>

   <!--PATTERN unix-def_passwordtst-->


	<!--RULE -->
<xsl:template match="unix-def:password_test/unix-def:object" priority="1001" mode="M317">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/unix-def:password_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a password_test must reference a password_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M317"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="unix-def:password_test/unix-def:state" priority="1000" mode="M317">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/unix-def:password_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a password_test must reference a password_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M317"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M317"/>
   <xsl:template match="@*|node()" priority="-2" mode="M317">
      <xsl:apply-templates select="*" mode="M317"/>
   </xsl:template>

   <!--PATTERN unix-def_password_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="unix-def:password_object//oval-def:filter" priority="1000" mode="M318">
      <xsl:variable name="parent_object" select="ancestor::unix-def:password_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#unix') and ($state_name='password_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M318"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M318"/>
   <xsl:template match="@*|node()" priority="-2" mode="M318">
      <xsl:apply-templates select="*" mode="M318"/>
   </xsl:template>

   <!--PATTERN unix-def_processtst_dep-->


	<!--RULE -->
<xsl:template match="unix-def:process_test" priority="1000" mode="M319">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M319"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M319"/>
   <xsl:template match="@*|node()" priority="-2" mode="M319">
      <xsl:apply-templates select="*" mode="M319"/>
   </xsl:template>

   <!--PATTERN unix-def_processtst-->


	<!--RULE -->
<xsl:template match="unix-def:process_test/unix-def:object" priority="1001" mode="M320">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/unix-def:process_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a process_test must reference a process_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M320"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="unix-def:process_test/unix-def:state" priority="1000" mode="M320">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/unix-def:process_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a process_test must reference a process_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M320"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M320"/>
   <xsl:template match="@*|node()" priority="-2" mode="M320">
      <xsl:apply-templates select="*" mode="M320"/>
   </xsl:template>

   <!--PATTERN unix-def_processobj_dep-->


	<!--RULE -->
<xsl:template match="unix-def:process_object" priority="1000" mode="M321">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED OBJECT: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M321"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M321"/>
   <xsl:template match="@*|node()" priority="-2" mode="M321">
      <xsl:apply-templates select="*" mode="M321"/>
   </xsl:template>

   <!--PATTERN unix-def_processste_dep-->


	<!--RULE -->
<xsl:template match="unix-def:process_state" priority="1000" mode="M322">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED STATE: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M322"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M322"/>
   <xsl:template match="@*|node()" priority="-2" mode="M322">
      <xsl:apply-templates select="*" mode="M322"/>
   </xsl:template>

   <!--PATTERN unix-def_process58tst-->


	<!--RULE -->
<xsl:template match="unix-def:process58_test/unix-def:object" priority="1001" mode="M323">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/unix-def:process58_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a process58_test must reference a process58_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M323"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="unix-def:process58_test/unix-def:state" priority="1000" mode="M323">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/unix-def:process58_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a process58_test must reference a process58_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M323"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M323"/>
   <xsl:template match="@*|node()" priority="-2" mode="M323">
      <xsl:apply-templates select="*" mode="M323"/>
   </xsl:template>

   <!--PATTERN unix-def_process58_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="unix-def:process58_object//oval-def:filter" priority="1000" mode="M324">
      <xsl:variable name="parent_object" select="ancestor::unix-def:process58_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#unix') and ($state_name='process58_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M324"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M324"/>
   <xsl:template match="@*|node()" priority="-2" mode="M324">
      <xsl:apply-templates select="*" mode="M324"/>
   </xsl:template>

   <!--PATTERN unix-def_routingtable_test-->


	<!--RULE -->
<xsl:template match="unix-def:routingtable_test/unix-def:object" priority="1001" mode="M325">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/unix-def:routingtable_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a routingtable_test must reference an routingtable_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M325"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="unix-def:routingtable_test/unix-def:state" priority="1000" mode="M325">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/unix-def:routingtable_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a routingtable_test must reference an routingtable_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M325"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M325"/>
   <xsl:template match="@*|node()" priority="-2" mode="M325">
      <xsl:apply-templates select="*" mode="M325"/>
   </xsl:template>

   <!--PATTERN unix-def_routingtable_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="unix-def:routingtable_object//oval-def:filter" priority="1000"
                 mode="M326">
      <xsl:variable name="parent_object" select="ancestor::unix-def:routingtable_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#unix') and ($state_name='routingtable_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M326"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M326"/>
   <xsl:template match="@*|node()" priority="-2" mode="M326">
      <xsl:apply-templates select="*" mode="M326"/>
   </xsl:template>

   <!--PATTERN unix-def_runleveltst-->


	<!--RULE -->
<xsl:template match="unix-def:runlevel_test/unix-def:object" priority="1001" mode="M327">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/unix-def:runlevel_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a runlevel_test must reference a runlevel_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M327"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="unix-def:runlevel_test/unix-def:state" priority="1000" mode="M327">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/unix-def:runlevel_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a runlevel_test must reference a runlevel_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M327"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M327"/>
   <xsl:template match="@*|node()" priority="-2" mode="M327">
      <xsl:apply-templates select="*" mode="M327"/>
   </xsl:template>

   <!--PATTERN unix-def_runlevel_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="unix-def:runlevel_object//oval-def:filter" priority="1000" mode="M328">
      <xsl:variable name="parent_object" select="ancestor::unix-def:runlevel_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#unix') and ($state_name='runlevel_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M328"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M328"/>
   <xsl:template match="@*|node()" priority="-2" mode="M328">
      <xsl:apply-templates select="*" mode="M328"/>
   </xsl:template>

   <!--PATTERN unix-def_sccstst_dep-->


	<!--RULE -->
<xsl:template match="unix-def:sccs_test" priority="1000" mode="M329">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M329"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M329"/>
   <xsl:template match="@*|node()" priority="-2" mode="M329">
      <xsl:apply-templates select="*" mode="M329"/>
   </xsl:template>

   <!--PATTERN unix-def_sccstst-->


	<!--RULE -->
<xsl:template match="unix-def:sccs_test/unix-def:object" priority="1001" mode="M330">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/unix-def:sccs_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a sccs_test must reference a sccs_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M330"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="unix-def:sccs_test/unix-def:state" priority="1000" mode="M330">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/unix-def:sccs_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a sccs_test must reference a sccs_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M330"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M330"/>
   <xsl:template match="@*|node()" priority="-2" mode="M330">
      <xsl:apply-templates select="*" mode="M330"/>
   </xsl:template>

   <!--PATTERN unix-def_sccsobj_dep-->


	<!--RULE -->
<xsl:template match="unix-def:sccs_object" priority="1000" mode="M331">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED OBJECT: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M331"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M331"/>
   <xsl:template match="@*|node()" priority="-2" mode="M331">
      <xsl:apply-templates select="*" mode="M331"/>
   </xsl:template>

   <!--PATTERN unix-def_sccs_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="unix-def:sccs_object//oval-def:filter" priority="1000" mode="M332">
      <xsl:variable name="parent_object" select="ancestor::unix-def:sccs_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#unix') and ($state_name='sccs_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M332"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M332"/>
   <xsl:template match="@*|node()" priority="-2" mode="M332">
      <xsl:apply-templates select="*" mode="M332"/>
   </xsl:template>

   <!--PATTERN unix-def_sccsobjfilepath-->


	<!--RULE -->
<xsl:template match="unix-def:sccs_object/unix-def:filepath" priority="1000" mode="M333">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::unix-def:behaviors[@max_depth or @recurse or @recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth, recurse, and recurse_direction behaviors are not allowed with a filepath entity<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M333"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M333"/>
   <xsl:template match="@*|node()" priority="-2" mode="M333">
      <xsl:apply-templates select="*" mode="M333"/>
   </xsl:template>

   <!--PATTERN unix-def_sccsobjfilepath2-->


	<!--RULE -->
<xsl:template match="unix-def:sccs_object/unix-def:filepath[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M334">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::unix-def:behaviors[@recurse_file_system='defined'])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_file_system behavior MUST not be set to 'defined' when a pattern match is used with a filepath entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M334"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M334"/>
   <xsl:template match="@*|node()" priority="-2" mode="M334">
      <xsl:apply-templates select="*" mode="M334"/>
   </xsl:template>

   <!--PATTERN unix-def_sccsobjpath-->


	<!--RULE -->
<xsl:template match="unix-def:sccs_object/unix-def:path[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M335">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::unix-def:behaviors[@recurse_file_system='defined'])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_file_system behavior MUST not be set to 'defined' when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::unix-def:behaviors[@max_depth])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::unix-def:behaviors[@recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_direction behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::unix-def:behaviors[@recurse])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M335"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M335"/>
   <xsl:template match="@*|node()" priority="-2" mode="M335">
      <xsl:apply-templates select="*" mode="M335"/>
   </xsl:template>

   <!--PATTERN unix-def_sccs_objectfilename-->


	<!--RULE -->
<xsl:template match="unix-def:sccs_object/unix-def:filename" priority="1000" mode="M336">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="(@var_ref and .='') or not(.='') or (.='' and @operation = 'pattern match')"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - filename entity cannot be empty unless the xsi:nil attribute is set to true or a var_ref is used<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M336"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M336"/>
   <xsl:template match="@*|node()" priority="-2" mode="M336">
      <xsl:apply-templates select="*" mode="M336"/>
   </xsl:template>

   <!--PATTERN unix-def_sccsste_dep-->


	<!--RULE -->
<xsl:template match="unix-def:sccs_state" priority="1000" mode="M337">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED STATE: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M337"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M337"/>
   <xsl:template match="@*|node()" priority="-2" mode="M337">
      <xsl:apply-templates select="*" mode="M337"/>
   </xsl:template>

   <!--PATTERN unix-def_shadowtst-->


	<!--RULE -->
<xsl:template match="unix-def:shadow_test/unix-def:object" priority="1001" mode="M338">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/unix-def:shadow_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a shadow_test must reference a shadow_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M338"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="unix-def:shadow_test/unix-def:state" priority="1000" mode="M338">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/unix-def:shadow_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a shadow_test must reference a shadow_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M338"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M338"/>
   <xsl:template match="@*|node()" priority="-2" mode="M338">
      <xsl:apply-templates select="*" mode="M338"/>
   </xsl:template>

   <!--PATTERN unix-def_shadow_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="unix-def:shadow_object//oval-def:filter" priority="1000" mode="M339">
      <xsl:variable name="parent_object" select="ancestor::unix-def:shadow_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#unix') and ($state_name='shadow_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M339"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M339"/>
   <xsl:template match="@*|node()" priority="-2" mode="M339">
      <xsl:apply-templates select="*" mode="M339"/>
   </xsl:template>

   <!--PATTERN unix-def_sysctltst-->


	<!--RULE -->
<xsl:template match="unix-def:sysctl_test/unix-def:object" priority="1001" mode="M340">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/unix-def:sysctl_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a sysctl_test must reference a sysctl_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M340"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="unix-def:sysctl_test/unix-def:state" priority="1000" mode="M340">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/unix-def:sysctl_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a sysctl_test must reference a sysctl_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M340"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M340"/>
   <xsl:template match="@*|node()" priority="-2" mode="M340">
      <xsl:apply-templates select="*" mode="M340"/>
   </xsl:template>

   <!--PATTERN unix-def_sysctl_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="unix-def:sysctl_object//oval-def:filter" priority="1000" mode="M341">
      <xsl:variable name="parent_object" select="ancestor::unix-def:sysctl_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#unix') and ($state_name='sysctl_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M341"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M341"/>
   <xsl:template match="@*|node()" priority="-2" mode="M341">
      <xsl:apply-templates select="*" mode="M341"/>
   </xsl:template>

   <!--PATTERN unix-def_unametst-->


	<!--RULE -->
<xsl:template match="unix-def:uname_test/unix-def:object" priority="1001" mode="M342">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/unix-def:uname_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a uname_test must reference a uname_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M342"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="unix-def:uname_test/unix-def:state" priority="1000" mode="M342">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/unix-def:uname_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a uname_test must reference a uname_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M342"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M342"/>
   <xsl:template match="@*|node()" priority="-2" mode="M342">
      <xsl:apply-templates select="*" mode="M342"/>
   </xsl:template>

   <!--PATTERN unix-def_xinetdtst-->


	<!--RULE -->
<xsl:template match="unix-def:xinetd_test/unix-def:object" priority="1001" mode="M343">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/unix-def:xinetd_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a xinetd_test must reference a xinetd_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M343"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="unix-def:xinetd_test/unix-def:state" priority="1000" mode="M343">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/unix-def:xinetd_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a xinetd_test must reference a xinetd_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M343"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M343"/>
   <xsl:template match="@*|node()" priority="-2" mode="M343">
      <xsl:apply-templates select="*" mode="M343"/>
   </xsl:template>

   <!--PATTERN unix-def_xinetd_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="unix-def:xinetd_object//oval-def:filter" priority="1000" mode="M344">
      <xsl:variable name="parent_object" select="ancestor::unix-def:xinetd_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#unix') and ($state_name='xinetd_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M344"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M344"/>
   <xsl:template match="@*|node()" priority="-2" mode="M344">
      <xsl:apply-templates select="*" mode="M344"/>
   </xsl:template>

   <!--PATTERN win-def_attst-->


	<!--RULE -->
<xsl:template match="win-def:accesstoken_test/win-def:object" priority="1001" mode="M345">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:accesstoken_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an accesstoken_test must reference an accesstoken_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M345"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:accesstoken_test/win-def:state" priority="1000" mode="M345">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:accesstoken_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an accesstoken_test must reference an accesstoken_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M345"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M345"/>
   <xsl:template match="@*|node()" priority="-2" mode="M345">
      <xsl:apply-templates select="*" mode="M345"/>
   </xsl:template>

   <!--PATTERN win-def_accesstoken_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:accesstoken_object//oval-def:filter" priority="1000" mode="M346">
      <xsl:variable name="parent_object" select="ancestor::win-def:accesstoken_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='accesstoken_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M346"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M346"/>
   <xsl:template match="@*|node()" priority="-2" mode="M346">
      <xsl:apply-templates select="*" mode="M346"/>
   </xsl:template>

   <!--PATTERN win-def_at_include_group_value_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:objects/win-def:accesstoken_object/win-def:behaviors"
                 priority="1000"
                 mode="M347">

		<!--REPORT -->
<xsl:if test="@include_group">DEPRECATED BEHAVIOR IN: win-def:accesstoken_object<xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M347"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M347"/>
   <xsl:template match="@*|node()" priority="-2" mode="M347">
      <xsl:apply-templates select="*" mode="M347"/>
   </xsl:template>

   <!--PATTERN win-def_at_resolve_group_value_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:objects/win-def:accesstoken_object/win-def:behaviors"
                 priority="1000"
                 mode="M348">

		<!--REPORT -->
<xsl:if test="@resolve_group">DEPRECATED BEHAVIOR IN: win-def:accesstoken_object <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M348"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M348"/>
   <xsl:template match="@*|node()" priority="-2" mode="M348">
      <xsl:apply-templates select="*" mode="M348"/>
   </xsl:template>

   <!--PATTERN win-def_adtst-->


	<!--RULE -->
<xsl:template match="win-def:activedirectory_test/win-def:object" priority="1001" mode="M349">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:activedirectory_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an activedirectory_test must reference an activedirectory_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M349"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:activedirectory_test/win-def:state" priority="1000" mode="M349">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:activedirectory_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an activedirectory_test must reference an activedirectory_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M349"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M349"/>
   <xsl:template match="@*|node()" priority="-2" mode="M349">
      <xsl:apply-templates select="*" mode="M349"/>
   </xsl:template>

   <!--PATTERN win-def_activedirectory57_test-->


	<!--RULE -->
<xsl:template match="win-def:activedirectory57_test/win-def:object" priority="1001"
                 mode="M350">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:activedirectory57_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an activedirectory57_test must reference an activedirectory57_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M350"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:activedirectory57_test/win-def:state" priority="1000"
                 mode="M350">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:activedirectory57_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an activedirectory57_test must reference an activedirectory57_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M350"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M350"/>
   <xsl:template match="@*|node()" priority="-2" mode="M350">
      <xsl:apply-templates select="*" mode="M350"/>
   </xsl:template>

   <!--PATTERN win-def_activedirectory57_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:activedirectory57_object//oval-def:filter" priority="1000"
                 mode="M351">
      <xsl:variable name="parent_object" select="ancestor::win-def:activedirectory57_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='activedirectory57_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M351"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M351"/>
   <xsl:template match="@*|node()" priority="-2" mode="M351">
      <xsl:apply-templates select="*" mode="M351"/>
   </xsl:template>

   <!--PATTERN win-def_ad57stevalue-->


	<!--RULE -->
<xsl:template match="win-def:activedirectory57_state/win-def:value" priority="1000"
                 mode="M352">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@datatype='record'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - datatype attribute for the value entity of a activedirectory57_state must be 'record'<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M352"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M352"/>
   <xsl:template match="@*|node()" priority="-2" mode="M352">
      <xsl:apply-templates select="*" mode="M352"/>
   </xsl:template>

   <!--PATTERN win-def_aeptst-->


	<!--RULE -->
<xsl:template match="win-def:auditeventpolicy_test/win-def:object" priority="1001"
                 mode="M353">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:auditeventpolicy_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an auditeventpolicy_test must reference an auditeventpolicy_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M353"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:auditeventpolicy_test/win-def:state" priority="1000" mode="M353">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:auditeventpolicy_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an auditeventpolicy_test must reference an auditeventpolicy_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M353"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M353"/>
   <xsl:template match="@*|node()" priority="-2" mode="M353">
      <xsl:apply-templates select="*" mode="M353"/>
   </xsl:template>

   <!--PATTERN win-def_aepstst-->


	<!--RULE -->
<xsl:template match="win-def:auditeventpolicysubcategories_test/win-def:object"
                 priority="1001"
                 mode="M354">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:auditeventpolicysubcategories_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an auditeventpolicysubcategories_test must reference an auditeventpolicysubcategories_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M354"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:auditeventpolicysubcategories_test/win-def:state"
                 priority="1000"
                 mode="M354">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:auditeventpolicysubcategories_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an auditeventpolicysubcategories_test must reference an auditeventpolicysubcategories_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M354"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M354"/>
   <xsl:template match="@*|node()" priority="-2" mode="M354">
      <xsl:apply-templates select="*" mode="M354"/>
   </xsl:template>

   <!--PATTERN win-def_cmdlet_test-->


	<!--RULE -->
<xsl:template match="win-def:cmdlet_test/win-def:object" priority="1001" mode="M355">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:cmdlet_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a cmdlet_test must reference a cmdlet_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M355"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:cmdlet_test/win-def:state" priority="1000" mode="M355">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:cmdlet_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a cmdlet_test must reference a cmdlet_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M355"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M355"/>
   <xsl:template match="@*|node()" priority="-2" mode="M355">
      <xsl:apply-templates select="*" mode="M355"/>
   </xsl:template>

   <!--PATTERN win-def_cmdlet_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:cmdlet_object//oval-def:filter" priority="1000" mode="M356">
      <xsl:variable name="parent_object" select="ancestor::win-def:cmdlet_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='cmdlet_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M356"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M356"/>
   <xsl:template match="@*|node()" priority="-2" mode="M356">
      <xsl:apply-templates select="*" mode="M356"/>
   </xsl:template>

   <!--PATTERN win-def_cmdletobjmodule_name-->


	<!--RULE -->
<xsl:template match="win-def:cmdlet_object/win-def:module_name" priority="1000" mode="M357">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the module_name entity of a cmdlet_object must be 'equals'<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M357"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M357"/>
   <xsl:template match="@*|node()" priority="-2" mode="M357">
      <xsl:apply-templates select="*" mode="M357"/>
   </xsl:template>

   <!--PATTERN win-def_cmdletobjmodule_id-->


	<!--RULE -->
<xsl:template match="win-def:cmdlet_object/win-def:module_id" priority="1000" mode="M358">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the module_id entity of a cmdlet_object must be 'equals'<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M358"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M358"/>
   <xsl:template match="@*|node()" priority="-2" mode="M358">
      <xsl:apply-templates select="*" mode="M358"/>
   </xsl:template>

   <!--PATTERN win-def_cmdletobjmodule_version-->


	<!--RULE -->
<xsl:template match="win-def:cmdlet_object/win-def:module_version" priority="1000"
                 mode="M359">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the module_version entity of a cmdlet_object must be 'equals'<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M359"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M359"/>
   <xsl:template match="@*|node()" priority="-2" mode="M359">
      <xsl:apply-templates select="*" mode="M359"/>
   </xsl:template>

   <!--PATTERN win-def_cmdletobjverb-->


	<!--RULE -->
<xsl:template match="win-def:cmdlet_object/win-def:verb" priority="1000" mode="M360">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the verb entity of a cmdlet_object must be 'equals'<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M360"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M360"/>
   <xsl:template match="@*|node()" priority="-2" mode="M360">
      <xsl:apply-templates select="*" mode="M360"/>
   </xsl:template>

   <!--PATTERN win-def_cmdletobjnoun-->


	<!--RULE -->
<xsl:template match="win-def:cmdlet_object/win-def:noun" priority="1000" mode="M361">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the noun entity of a cmdlet_object must be 'equals'<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M361"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M361"/>
   <xsl:template match="@*|node()" priority="-2" mode="M361">
      <xsl:apply-templates select="*" mode="M361"/>
   </xsl:template>

   <!--PATTERN win-def_cmdletobjparameters-->


	<!--RULE -->
<xsl:template match="win-def:cmdlet_object/win-def:parameters" priority="1000" mode="M362">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@datatype='record'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - datatype attribute for the parameters entity of a cmdlet_object must be 'record'<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M362"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M362"/>
   <xsl:template match="@*|node()" priority="-2" mode="M362">
      <xsl:apply-templates select="*" mode="M362"/>
   </xsl:template>

   <!--PATTERN win-def_cmdletobjselect-->


	<!--RULE -->
<xsl:template match="win-def:cmdlet_object/win-def:select" priority="1000" mode="M363">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@datatype='record'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - datatype attribute for the select entity of a cmdlet_object must be 'record'<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M363"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M363"/>
   <xsl:template match="@*|node()" priority="-2" mode="M363">
      <xsl:apply-templates select="*" mode="M363"/>
   </xsl:template>

   <!--PATTERN win-def_cmdletsteparameters-->


	<!--RULE -->
<xsl:template match="win-def:cmdlet_state/win-def:parameters" priority="1000" mode="M364">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@datatype='record'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - datatype attribute for the parameters entity of a cmdlet_state must be 'record'<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M364"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M364"/>
   <xsl:template match="@*|node()" priority="-2" mode="M364">
      <xsl:apply-templates select="*" mode="M364"/>
   </xsl:template>

   <!--PATTERN win-def_cmdletsteselect-->


	<!--RULE -->
<xsl:template match="win-def:cmdlet_state/win-def:select" priority="1000" mode="M365">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@datatype='record'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - datatype attribute for the select entity of a cmdlet_state must be 'record'<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M365"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M365"/>
   <xsl:template match="@*|node()" priority="-2" mode="M365">
      <xsl:apply-templates select="*" mode="M365"/>
   </xsl:template>

   <!--PATTERN win-def_cmdletstevalue-->


	<!--RULE -->
<xsl:template match="win-def:cmdlet_state/win-def:value" priority="1000" mode="M366">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@datatype='record'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - datatype attribute for the value entity of a cmdlet_state must be 'record'<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M366"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M366"/>
   <xsl:template match="@*|node()" priority="-2" mode="M366">
      <xsl:apply-templates select="*" mode="M366"/>
   </xsl:template>

   <!--PATTERN win-def_dnscachetst-->


	<!--RULE -->
<xsl:template match="win-def:dnscache_test/win-def:object" priority="1001" mode="M367">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:dnscache_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a dnscache_test must reference a dnscache_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M367"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:dnscache_test/win-def:state" priority="1000" mode="M367">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:dnscache_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a dnscache_test must reference a dnscache_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M367"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M367"/>
   <xsl:template match="@*|node()" priority="-2" mode="M367">
      <xsl:apply-templates select="*" mode="M367"/>
   </xsl:template>

   <!--PATTERN win-def_dnscache_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:dnscache_object//oval-def:filter" priority="1000" mode="M368">
      <xsl:variable name="parent_object" select="ancestor::win-def:dnscache_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='dnscache_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M368"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M368"/>
   <xsl:template match="@*|node()" priority="-2" mode="M368">
      <xsl:apply-templates select="*" mode="M368"/>
   </xsl:template>

   <!--PATTERN win-def_filetst-->


	<!--RULE -->
<xsl:template match="win-def:file_test/win-def:object" priority="1001" mode="M369">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:file_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a file_test must reference a file_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M369"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:file_test/win-def:state" priority="1000" mode="M369">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:file_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a file_test must reference a file_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M369"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M369"/>
   <xsl:template match="@*|node()" priority="-2" mode="M369">
      <xsl:apply-templates select="*" mode="M369"/>
   </xsl:template>

   <!--PATTERN win-def_file_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:file_object//oval-def:filter" priority="1000" mode="M370">
      <xsl:variable name="parent_object" select="ancestor::win-def:file_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='file_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M370"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M370"/>
   <xsl:template match="@*|node()" priority="-2" mode="M370">
      <xsl:apply-templates select="*" mode="M370"/>
   </xsl:template>

   <!--PATTERN win-def_fileobjfilepath-->


	<!--RULE -->
<xsl:template match="win-def:file_object/win-def:filepath" priority="1000" mode="M371">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@max_depth or @recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth and recurse_direction behaviors are not allowed with a filepath entity<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M371"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M371"/>
   <xsl:template match="@*|node()" priority="-2" mode="M371">
      <xsl:apply-templates select="*" mode="M371"/>
   </xsl:template>

   <!--PATTERN win-def_fileobjfilepath2-->


	<!--RULE -->
<xsl:template match="win-def:file_object/win-def:filepath[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M372">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@recurse_file_system='defined'])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_file_system behavior MUST not be set to 'defined' when a pattern match is used with a filepath entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M372"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M372"/>
   <xsl:template match="@*|node()" priority="-2" mode="M372">
      <xsl:apply-templates select="*" mode="M372"/>
   </xsl:template>

   <!--PATTERN win-def_fileobjpath-->


	<!--RULE -->
<xsl:template match="win-def:file_object/win-def:path[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M373">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@recurse_file_system='defined'])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_file_system behavior MUST not be set to 'defined' when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@max_depth])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_direction behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M373"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M373"/>
   <xsl:template match="@*|node()" priority="-2" mode="M373">
      <xsl:apply-templates select="*" mode="M373"/>
   </xsl:template>

   <!--PATTERN win-def_fileobjfilename-->


	<!--RULE -->
<xsl:template match="win-def:file_object/win-def:filename" priority="1000" mode="M374">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="(not(contains(.,'\') or contains(.,'/') or contains(.,':') or contains(.,'*') or contains(.,'?') or contains(.,'&gt;') or contains(.,'|'))) or (@operation='pattern match')"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - filename entity cannot contain the characters / \ : * ? &gt; | <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(@var_ref and .='') or ((@xsi:nil='1' or @xsi:nil='true') and .='') or not(@var_ref)"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - filename entity cannot be empty unless the xsi:nil attribute is set to true or a var_ref is used<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M374"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M374"/>
   <xsl:template match="@*|node()" priority="-2" mode="M374">
      <xsl:apply-templates select="*" mode="M374"/>
   </xsl:template>

   <!--PATTERN win-def_filestefilename-->


	<!--RULE -->
<xsl:template match="win-def:file_state/win-def:filename" priority="1000" mode="M375">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="(not(contains(.,'\') or contains(.,'/') or contains(.,':') or contains(.,'*') or contains(.,'?') or contains(.,'&gt;') or contains(.,'|'))) or (@operation='pattern match')"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - filename entity cannot contain the characters / \ : * ? &gt; | <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M375"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M375"/>
   <xsl:template match="@*|node()" priority="-2" mode="M375">
      <xsl:apply-templates select="*" mode="M375"/>
   </xsl:template>

   <!--PATTERN win-def_fap53tst-->


	<!--RULE -->
<xsl:template match="win-def:fileauditedpermissions53_test/win-def:object" priority="1001"
                 mode="M376">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:fileauditedpermissions53_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a fileauditedpermissions53_test must reference a fileauditedpermissions53_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M376"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:fileauditedpermissions53_test/win-def:state" priority="1000"
                 mode="M376">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:fileauditedpermissions53_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a fileauditedpermissions53_test must reference a fileauditedpermissions53_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M376"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M376"/>
   <xsl:template match="@*|node()" priority="-2" mode="M376">
      <xsl:apply-templates select="*" mode="M376"/>
   </xsl:template>

   <!--PATTERN win-def_fileauditedpermissions53_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:fileauditedpermissions53_object//oval-def:filter"
                 priority="1000"
                 mode="M377">
      <xsl:variable name="parent_object" select="ancestor::win-def:fileauditedpermissions53_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='fileauditedpermissions53_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M377"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M377"/>
   <xsl:template match="@*|node()" priority="-2" mode="M377">
      <xsl:apply-templates select="*" mode="M377"/>
   </xsl:template>

   <!--PATTERN win-def_fap53objfilepath-->


	<!--RULE -->
<xsl:template match="win-def:fileauditedpermissions53_object/win-def:filepath"
                 priority="1000"
                 mode="M378">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@max_depth or @recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth and recurse_direction behaviors are not allowed with a filepath entity<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M378"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M378"/>
   <xsl:template match="@*|node()" priority="-2" mode="M378">
      <xsl:apply-templates select="*" mode="M378"/>
   </xsl:template>

   <!--PATTERN win-def_fap53objfilepath2-->


	<!--RULE -->
<xsl:template match="win-def:fileauditedpermissions53_object/win-def:filepath[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M379">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@recurse_file_system='defined'])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_file_system behavior MUST not be set to 'defined' when a pattern match is used with a filepath entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M379"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M379"/>
   <xsl:template match="@*|node()" priority="-2" mode="M379">
      <xsl:apply-templates select="*" mode="M379"/>
   </xsl:template>

   <!--PATTERN win-def_fap53objpath-->


	<!--RULE -->
<xsl:template match="win-def:fileauditedpermissions53_object/win-def:path[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M380">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@recurse_file_system='defined'])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_file_system behavior MUST not be set to 'defined' when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@max_depth])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_direction behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M380"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M380"/>
   <xsl:template match="@*|node()" priority="-2" mode="M380">
      <xsl:apply-templates select="*" mode="M380"/>
   </xsl:template>

   <!--PATTERN win-def_fap53objfilename-->


	<!--RULE -->
<xsl:template match="win-def:fileauditedpermissions53_object/win-def:filename"
                 priority="1000"
                 mode="M381">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="(not(contains(.,'\') or contains(.,'/') or contains(.,':') or contains(.,'*') or contains(.,'?') or contains(.,'&gt;') or contains(.,'|'))) or (@operation='pattern match')"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - filename entity cannot contain the characters / \ : * ? &gt; | <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(@var_ref and .='') or ((@xsi:nil='1' or @xsi:nil='true') and .='') or not(.='') or (.='' and @operation='pattern match')"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - filename entity cannot be empty unless the xsi:nil attribute is set to true or a var_ref is used<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M381"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M381"/>
   <xsl:template match="@*|node()" priority="-2" mode="M381">
      <xsl:apply-templates select="*" mode="M381"/>
   </xsl:template>

   <!--PATTERN win-def_fap53stefilename-->


	<!--RULE -->
<xsl:template match="win-def:fileauditedpermissions53_state/win-def:filename" priority="1000"
                 mode="M382">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="(not(contains(.,'\') or contains(.,'/') or contains(.,':') or contains(.,'*') or contains(.,'?') or contains(.,'&gt;') or contains(.,'|'))) or (@operation='pattern match')"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - filename entity cannot contain the characters / \ : * ? &gt; | <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M382"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M382"/>
   <xsl:template match="@*|node()" priority="-2" mode="M382">
      <xsl:apply-templates select="*" mode="M382"/>
   </xsl:template>

   <!--PATTERN win-def_fap53_include_group_value_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:objects/win-def:fileauditedpermissions53_object/win-def:behaviors"
                 priority="1000"
                 mode="M383">

		<!--REPORT -->
<xsl:if test="@include_group">DEPRECATED BEHAVIOR IN: win-def:fileauditedpermissions53_object<xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M383"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M383"/>
   <xsl:template match="@*|node()" priority="-2" mode="M383">
      <xsl:apply-templates select="*" mode="M383"/>
   </xsl:template>

   <!--PATTERN win-def_fap53_resolve_group_value_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:objects/win-def:fileauditedpermissions53_object/win-def:behaviors"
                 priority="1000"
                 mode="M384">

		<!--REPORT -->
<xsl:if test="@resolve_group">DEPRECATED BEHAVIOR IN: fileauditedpermissions53_object <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M384"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M384"/>
   <xsl:template match="@*|node()" priority="-2" mode="M384">
      <xsl:apply-templates select="*" mode="M384"/>
   </xsl:template>

   <!--PATTERN win-def_faptst_dep-->


	<!--RULE -->
<xsl:template match="win-def:fileauditedpermissions_test" priority="1000" mode="M385">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M385"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M385"/>
   <xsl:template match="@*|node()" priority="-2" mode="M385">
      <xsl:apply-templates select="*" mode="M385"/>
   </xsl:template>

   <!--PATTERN win-def_faptst-->


	<!--RULE -->
<xsl:template match="win-def:fileauditedpermissions_test/win-def:object" priority="1001"
                 mode="M386">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:fileauditedpermissions_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a fileauditedpermissions_test must reference a fileauditedpermissions_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M386"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:fileauditedpermissions_test/win-def:state" priority="1000"
                 mode="M386">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:fileauditedpermissions_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a fileauditedpermissions_test must reference a fileauditedpermissions_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M386"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M386"/>
   <xsl:template match="@*|node()" priority="-2" mode="M386">
      <xsl:apply-templates select="*" mode="M386"/>
   </xsl:template>

   <!--PATTERN win-def_fapobj_dep-->


	<!--RULE -->
<xsl:template match="win-def:fileauditedpermissions_object" priority="1000" mode="M387">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED OBJECT: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M387"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M387"/>
   <xsl:template match="@*|node()" priority="-2" mode="M387">
      <xsl:apply-templates select="*" mode="M387"/>
   </xsl:template>

   <!--PATTERN win-def_fapobjfilename-->


	<!--RULE -->
<xsl:template match="win-def:fileauditedpermissions_object/win-def:filename" priority="1000"
                 mode="M388">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="(not(contains(.,'\') or contains(.,'/') or contains(.,':') or contains(.,'*') or contains(.,'?') or contains(.,'&gt;') or contains(.,'|'))) or (@operation='pattern match')"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - filename entity cannot contain the characters / \ : * ? &gt; | <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(@var_ref and .='') or ((@xsi:nil='1' or @xsi:nil='true') and .='') or not(.='') or (.='' and @operation='pattern match')"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - filename entity cannot be empty unless the xsi:nil attribute is set to true or a var_ref is used<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M388"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M388"/>
   <xsl:template match="@*|node()" priority="-2" mode="M388">
      <xsl:apply-templates select="*" mode="M388"/>
   </xsl:template>

   <!--PATTERN win-def_fapste_dep-->


	<!--RULE -->
<xsl:template match="win-def:fileauditedpermissions_state" priority="1000" mode="M389">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED STATE: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M389"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M389"/>
   <xsl:template match="@*|node()" priority="-2" mode="M389">
      <xsl:apply-templates select="*" mode="M389"/>
   </xsl:template>

   <!--PATTERN win-def_fapstefilename-->


	<!--RULE -->
<xsl:template match="win-def:fileauditedpermissions_state/win-def:filename" priority="1000"
                 mode="M390">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="(not(contains(.,'\') or contains(.,'/') or contains(.,':') or contains(.,'*') or contains(.,'?') or contains(.,'&gt;') or contains(.,'|'))) or (@operation='pattern match')"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - filename entity cannot contain the characters / \ : * ? &gt; | <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M390"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M390"/>
   <xsl:template match="@*|node()" priority="-2" mode="M390">
      <xsl:apply-templates select="*" mode="M390"/>
   </xsl:template>

   <!--PATTERN win-def_fap_include_group_value_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:objects/win-def:fileauditedpermissions_object/win-def:behaviors"
                 priority="1000"
                 mode="M391">

		<!--REPORT -->
<xsl:if test="@include_group">DEPRECATED BEHAVIOR IN: win-def:fileauditedpermissions_object<xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M391"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M391"/>
   <xsl:template match="@*|node()" priority="-2" mode="M391">
      <xsl:apply-templates select="*" mode="M391"/>
   </xsl:template>

   <!--PATTERN win-def_fap_resolve_group_value_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:objects/win-def:fileauditedpermissions_object/win-def:behaviors"
                 priority="1000"
                 mode="M392">

		<!--REPORT -->
<xsl:if test="@resolve_group">DEPRECATED BEHAVIOR IN: fileauditedpermissions_object <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M392"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M392"/>
   <xsl:template match="@*|node()" priority="-2" mode="M392">
      <xsl:apply-templates select="*" mode="M392"/>
   </xsl:template>

   <!--PATTERN win-def_fer53tst-->


	<!--RULE -->
<xsl:template match="win-def:fileeffectiverights53_test/win-def:object" priority="1001"
                 mode="M393">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:fileeffectiverights53_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a fileeffectiverights53_test must reference a fileeffectiverights53_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M393"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:fileeffectiverights53_test/win-def:state" priority="1000"
                 mode="M393">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:fileeffectiverights53_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a fileeffectiverights53_test must reference a fileeffectiverights53_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M393"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M393"/>
   <xsl:template match="@*|node()" priority="-2" mode="M393">
      <xsl:apply-templates select="*" mode="M393"/>
   </xsl:template>

   <!--PATTERN win-def_fileeffectiverights53_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:fileeffectiverights53_object//oval-def:filter" priority="1000"
                 mode="M394">
      <xsl:variable name="parent_object" select="ancestor::win-def:fileeffectiverights53_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='fileeffectiverights53_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M394"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M394"/>
   <xsl:template match="@*|node()" priority="-2" mode="M394">
      <xsl:apply-templates select="*" mode="M394"/>
   </xsl:template>

   <!--PATTERN win-def_fer53objfilepath-->


	<!--RULE -->
<xsl:template match="win-def:fileeffectiverights53_object/win-def:filepath" priority="1000"
                 mode="M395">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@max_depth or @recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth and recurse_direction behaviors are not allowed with a filepath entity<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M395"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M395"/>
   <xsl:template match="@*|node()" priority="-2" mode="M395">
      <xsl:apply-templates select="*" mode="M395"/>
   </xsl:template>

   <!--PATTERN win-def_fer53objfilepath2-->


	<!--RULE -->
<xsl:template match="win-def:fileeffectiverights53_object/win-def:filepath[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M396">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@recurse_file_system='defined'])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_file_system behavior MUST not be set to 'defined' when a pattern match is used with a filepath entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M396"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M396"/>
   <xsl:template match="@*|node()" priority="-2" mode="M396">
      <xsl:apply-templates select="*" mode="M396"/>
   </xsl:template>

   <!--PATTERN win-def_fer53objpath-->


	<!--RULE -->
<xsl:template match="win-def:fileeffectiverights53_object/win-def:path[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M397">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@recurse_file_system='defined'])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_file_system behavior MUST not be set to 'defined' when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@max_depth])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_direction behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M397"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M397"/>
   <xsl:template match="@*|node()" priority="-2" mode="M397">
      <xsl:apply-templates select="*" mode="M397"/>
   </xsl:template>

   <!--PATTERN win-def_fer53objfilename-->


	<!--RULE -->
<xsl:template match="win-def:fileeffectiverights53_object/win-def:filename" priority="1000"
                 mode="M398">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="(not(contains(.,'\') or contains(.,'/') or contains(.,':') or contains(.,'*') or contains(.,'?') or contains(.,'&gt;') or contains(.,'|'))) or (@operation='pattern match')"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - filename entity cannot contain the characters / \ : * ? &gt; | <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(@var_ref and .='') or ((@xsi:nil='1' or @xsi:nil='true') and .='') or not(.='') or (.='' and @operation='pattern match')"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - filename entity cannot be empty unless the xsi:nil attribute is set to true or a var_ref is used<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M398"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M398"/>
   <xsl:template match="@*|node()" priority="-2" mode="M398">
      <xsl:apply-templates select="*" mode="M398"/>
   </xsl:template>

   <!--PATTERN win-def_fer53stefilename-->


	<!--RULE -->
<xsl:template match="win-def:fileeffectiverights53_state/win-def:filename" priority="1000"
                 mode="M399">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="(not(contains(.,'\') or contains(.,'/') or contains(.,':') or contains(.,'*') or contains(.,'?') or contains(.,'&gt;') or contains(.,'|'))) or (@operation='pattern match')"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - filename entity cannot contain the characters / \ : * ? &gt; | <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M399"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M399"/>
   <xsl:template match="@*|node()" priority="-2" mode="M399">
      <xsl:apply-templates select="*" mode="M399"/>
   </xsl:template>

   <!--PATTERN win-def_ffr53_include_group_value_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:objects/win-def:fileeffectiverights53_object/win-def:behaviors"
                 priority="1000"
                 mode="M400">

		<!--REPORT -->
<xsl:if test="@include_group">DEPRECATED BEHAVIOR IN: win-def:fileeffectiverights53_object<xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M400"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M400"/>
   <xsl:template match="@*|node()" priority="-2" mode="M400">
      <xsl:apply-templates select="*" mode="M400"/>
   </xsl:template>

   <!--PATTERN win-def_ffr53_resolve_group_value_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:objects/win-def:fileeffectiverights53_object/win-def:behaviors"
                 priority="1000"
                 mode="M401">

		<!--REPORT -->
<xsl:if test="@resolve_group">DEPRECATED BEHAVIOR IN: fileeffectiverights53_object <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M401"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M401"/>
   <xsl:template match="@*|node()" priority="-2" mode="M401">
      <xsl:apply-templates select="*" mode="M401"/>
   </xsl:template>

   <!--PATTERN win-def_fertst_dep-->


	<!--RULE -->
<xsl:template match="win-def:fileeffectiverights_test" priority="1000" mode="M402">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M402"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M402"/>
   <xsl:template match="@*|node()" priority="-2" mode="M402">
      <xsl:apply-templates select="*" mode="M402"/>
   </xsl:template>

   <!--PATTERN win-def_fertst-->


	<!--RULE -->
<xsl:template match="win-def:fileeffectiverights_test/win-def:object" priority="1001"
                 mode="M403">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:fileeffectiverights_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a fileeffectiverights_test must reference a fileeffectiverights_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M403"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:fileeffectiverights_test/win-def:state" priority="1000"
                 mode="M403">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:fileeffectiverights_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a fileeffectiverights_test must reference a fileeffectiverights_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M403"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M403"/>
   <xsl:template match="@*|node()" priority="-2" mode="M403">
      <xsl:apply-templates select="*" mode="M403"/>
   </xsl:template>

   <!--PATTERN win-def_ferobj_dep-->


	<!--RULE -->
<xsl:template match="win-def:fileeffectiverights_object" priority="1000" mode="M404">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED OBJECT: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M404"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M404"/>
   <xsl:template match="@*|node()" priority="-2" mode="M404">
      <xsl:apply-templates select="*" mode="M404"/>
   </xsl:template>

   <!--PATTERN win-def_fefobjfilename-->


	<!--RULE -->
<xsl:template match="win-def:fileeffectiverights_object/win-def:filename" priority="1000"
                 mode="M405">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="(not(contains(.,'\') or contains(.,'/') or contains(.,':') or contains(.,'*') or contains(.,'?') or contains(.,'&gt;') or contains(.,'|'))) or (@operation='pattern match')"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - filename entity cannot contain the characters / \ : * ? &gt; | <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(@var_ref and .='') or ((@xsi:nil='1' or @xsi:nil='true') and .='') or not(.='') or (.='' and @operation='pattern match')"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - filename entity cannot be empty unless the xsi:nil attribute is set to true or a var_ref is used<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M405"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M405"/>
   <xsl:template match="@*|node()" priority="-2" mode="M405">
      <xsl:apply-templates select="*" mode="M405"/>
   </xsl:template>

   <!--PATTERN win-def_ferste_dep-->


	<!--RULE -->
<xsl:template match="win-def:fileeffectiverights_state" priority="1000" mode="M406">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED STATE: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M406"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M406"/>
   <xsl:template match="@*|node()" priority="-2" mode="M406">
      <xsl:apply-templates select="*" mode="M406"/>
   </xsl:template>

   <!--PATTERN win-def_ferstefilename-->


	<!--RULE -->
<xsl:template match="win-def:fileeffectiverights_state/win-def:filename" priority="1000"
                 mode="M407">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="(not(contains(.,'\') or contains(.,'/') or contains(.,':') or contains(.,'*') or contains(.,'?') or contains(.,'&gt;') or contains(.,'|'))) or (@operation='pattern match')"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - filename entity cannot contain the characters / \ : * ? &gt; | <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M407"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M407"/>
   <xsl:template match="@*|node()" priority="-2" mode="M407">
      <xsl:apply-templates select="*" mode="M407"/>
   </xsl:template>

   <!--PATTERN win-def_ffr_include_group_value_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:objects/win-def:fileeffectiverights_object/win-def:behaviors"
                 priority="1000"
                 mode="M408">

		<!--REPORT -->
<xsl:if test="@include_group">DEPRECATED BEHAVIOR IN: win-def:fileeffectiverights_object<xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M408"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M408"/>
   <xsl:template match="@*|node()" priority="-2" mode="M408">
      <xsl:apply-templates select="*" mode="M408"/>
   </xsl:template>

   <!--PATTERN win-def_ffr_resolve_group_value_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:objects/win-def:fileeffectiverights_object/win-def:behaviors"
                 priority="1000"
                 mode="M409">

		<!--REPORT -->
<xsl:if test="@resolve_group">DEPRECATED BEHAVIOR IN: fileeffectiverights_object <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M409"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M409"/>
   <xsl:template match="@*|node()" priority="-2" mode="M409">
      <xsl:apply-templates select="*" mode="M409"/>
   </xsl:template>

   <!--PATTERN win-def_grouptst-->


	<!--RULE -->
<xsl:template match="win-def:group_test/win-def:object" priority="1001" mode="M410">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:group_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a group_test must reference a group_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M410"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:group_test/win-def:state" priority="1000" mode="M410">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:group_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a group_test must reference a group_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M410"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M410"/>
   <xsl:template match="@*|node()" priority="-2" mode="M410">
      <xsl:apply-templates select="*" mode="M410"/>
   </xsl:template>

   <!--PATTERN win-def_group_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:group_object//oval-def:filter" priority="1000" mode="M411">
      <xsl:variable name="parent_object" select="ancestor::win-def:group_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='group_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M411"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M411"/>
   <xsl:template match="@*|node()" priority="-2" mode="M411">
      <xsl:apply-templates select="*" mode="M411"/>
   </xsl:template>

   <!--PATTERN win-def_groupsidtst-->


	<!--RULE -->
<xsl:template match="win-def:group_sid_test/win-def:object" priority="1001" mode="M412">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:group_sid_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a group_sid_test must reference a group_sid_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M412"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:group_sid_test/win-def:state" priority="1000" mode="M412">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:group_sid_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a group_sid_test must reference a group_sid_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M412"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M412"/>
   <xsl:template match="@*|node()" priority="-2" mode="M412">
      <xsl:apply-templates select="*" mode="M412"/>
   </xsl:template>

   <!--PATTERN win-def_group_sid_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:group_sid_object//oval-def:filter" priority="1000" mode="M413">
      <xsl:variable name="parent_object" select="ancestor::win-def:group_sid_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='group_sid_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M413"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M413"/>
   <xsl:template match="@*|node()" priority="-2" mode="M413">
      <xsl:apply-templates select="*" mode="M413"/>
   </xsl:template>

   <!--PATTERN win-def_wininterfacetst-->


	<!--RULE -->
<xsl:template match="win-def:interface_test/win-def:object" priority="1001" mode="M414">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:interface_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of an interface_test must reference an interface_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M414"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:interface_test/win-def:state" priority="1000" mode="M414">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:interface_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of an interface_test must reference an interface_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M414"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M414"/>
   <xsl:template match="@*|node()" priority="-2" mode="M414">
      <xsl:apply-templates select="*" mode="M414"/>
   </xsl:template>

   <!--PATTERN win-def_interface_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:interface_object//oval-def:filter" priority="1000" mode="M415">
      <xsl:variable name="parent_object" select="ancestor::win-def:interface_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='interface_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M415"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M415"/>
   <xsl:template match="@*|node()" priority="-2" mode="M415">
      <xsl:apply-templates select="*" mode="M415"/>
   </xsl:template>

   <!--PATTERN win-def_lptst-->


	<!--RULE -->
<xsl:template match="win-def:lockoutpolicy_test/win-def:object" priority="1001" mode="M416">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:lockoutpolicy_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a lockoutpolicy_test must reference a lockoutpolicy_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M416"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:lockoutpolicy_test/win-def:state" priority="1000" mode="M416">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:lockoutpolicy_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a lockoutpolicy_test must reference a lockoutpolicy_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M416"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M416"/>
   <xsl:template match="@*|node()" priority="-2" mode="M416">
      <xsl:apply-templates select="*" mode="M416"/>
   </xsl:template>

   <!--PATTERN win-def_metabasetst-->


	<!--RULE -->
<xsl:template match="win-def:metabase_test/win-def:object" priority="1001" mode="M417">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:metabase_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a metabase_test must reference a metabase_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M417"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:metabase_test/win-def:state" priority="1000" mode="M417">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:metabase_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a metabase_test must reference a metabase_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M417"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M417"/>
   <xsl:template match="@*|node()" priority="-2" mode="M417">
      <xsl:apply-templates select="*" mode="M417"/>
   </xsl:template>

   <!--PATTERN win-def_metabase_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:metabase_object//oval-def:filter" priority="1000" mode="M418">
      <xsl:variable name="parent_object" select="ancestor::win-def:metabase_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='metabase_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M418"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M418"/>
   <xsl:template match="@*|node()" priority="-2" mode="M418">
      <xsl:apply-templates select="*" mode="M418"/>
   </xsl:template>

   <!--PATTERN win-def_pptst-->


	<!--RULE -->
<xsl:template match="win-def:passwordpolicy_test/win-def:object" priority="1001" mode="M419">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:passwordpolicy_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a passwordpolicy_test must reference a passwordpolicy_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M419"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:passwordpolicy_test/win-def:state" priority="1000" mode="M419">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:passwordpolicy_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a passwordpolicy_test must reference a passwordpolicy_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M419"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M419"/>
   <xsl:template match="@*|node()" priority="-2" mode="M419">
      <xsl:apply-templates select="*" mode="M419"/>
   </xsl:template>

   <!--PATTERN win-def_peheaderobjfilepath-->


	<!--RULE -->
<xsl:template match="win-def:peheader_object/win-def:filepath" priority="1000" mode="M420">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@max_depth or @recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth and recurse_direction behaviors are not allowed with a filepath entity<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M420"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M420"/>
   <xsl:template match="@*|node()" priority="-2" mode="M420">
      <xsl:apply-templates select="*" mode="M420"/>
   </xsl:template>

   <!--PATTERN win-def_peheaderobjfilepath2-->


	<!--RULE -->
<xsl:template match="win-def:peheader_object/win-def:filepath[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M421">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@recurse_file_system='defined'])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_file_system behavior MUST not be set to 'defined' when a pattern match is used with a filepath entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M421"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M421"/>
   <xsl:template match="@*|node()" priority="-2" mode="M421">
      <xsl:apply-templates select="*" mode="M421"/>
   </xsl:template>

   <!--PATTERN win-def_peheaderobjpath-->


	<!--RULE -->
<xsl:template match="win-def:peheader_object/win-def:filepath[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M422">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@recurse_file_system='defined'])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_file_system behavior MUST not be set to 'defined' when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@max_depth])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_direction behavior MUST not be used when a pattern match is used with a path entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M422"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M422"/>
   <xsl:template match="@*|node()" priority="-2" mode="M422">
      <xsl:apply-templates select="*" mode="M422"/>
   </xsl:template>

   <!--PATTERN win-def_peheaderobjfilename-->


	<!--RULE -->
<xsl:template match="win-def:peheader_object/win-def:filename" priority="1000" mode="M423">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="(not(contains(.,'\') or contains(.,'/') or contains(.,':') or contains(.,'*') or contains(.,'?') or contains(.,'&gt;') or contains(.,'|'))) or (@operation='pattern match')"/>
         <xsl:otherwise>
                                                                                                <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - filename entity cannot contain the characters / \ : * ? &gt; | <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M423"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M423"/>
   <xsl:template match="@*|node()" priority="-2" mode="M423">
      <xsl:apply-templates select="*" mode="M423"/>
   </xsl:template>

   <!--PATTERN win-def_winporttst-->


	<!--RULE -->
<xsl:template match="win-def:port_test/win-def:object" priority="1001" mode="M424">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:port_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a port_test must reference a port_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M424"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:port_test/win-def:state" priority="1000" mode="M424">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:port_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a port_test must reference a port_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M424"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M424"/>
   <xsl:template match="@*|node()" priority="-2" mode="M424">
      <xsl:apply-templates select="*" mode="M424"/>
   </xsl:template>

   <!--PATTERN win-def_port_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:port_object//oval-def:filter" priority="1000" mode="M425">
      <xsl:variable name="parent_object" select="ancestor::win-def:port_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='port_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M425"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M425"/>
   <xsl:template match="@*|node()" priority="-2" mode="M425">
      <xsl:apply-templates select="*" mode="M425"/>
   </xsl:template>

   <!--PATTERN win-def_pertst-->


	<!--RULE -->
<xsl:template match="win-def:printereffectiverights_test/win-def:object" priority="1001"
                 mode="M426">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:printereffectiverights_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a printereffectiverights_test must reference a printereffectiverights_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M426"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:printereffectiverights_test/win-def:state" priority="1000"
                 mode="M426">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:printereffectiverights_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a printereffectiverights_test must reference a printereffectiverights_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M426"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M426"/>
   <xsl:template match="@*|node()" priority="-2" mode="M426">
      <xsl:apply-templates select="*" mode="M426"/>
   </xsl:template>

   <!--PATTERN win-def_printereffectiverights_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:printereffectiverights_object//oval-def:filter" priority="1000"
                 mode="M427">
      <xsl:variable name="parent_object" select="ancestor::win-def:printereffectiverights_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='printereffectiverights_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M427"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M427"/>
   <xsl:template match="@*|node()" priority="-2" mode="M427">
      <xsl:apply-templates select="*" mode="M427"/>
   </xsl:template>

   <!--PATTERN win-def_pef_include_group_value_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:objects/win-def:printereffectiverights_object/win-def:behaviors"
                 priority="1000"
                 mode="M428">

		<!--REPORT -->
<xsl:if test="@include_group">DEPRECATED BEHAVIOR IN: win-def:printereffectiverights_object<xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M428"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M428"/>
   <xsl:template match="@*|node()" priority="-2" mode="M428">
      <xsl:apply-templates select="*" mode="M428"/>
   </xsl:template>

   <!--PATTERN win-def_pef_resolve_group_value_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:objects/win-def:printereffectiverights_object/win-def:behaviors"
                 priority="1000"
                 mode="M429">

		<!--REPORT -->
<xsl:if test="@resolve_group">DEPRECATED BEHAVIOR IN: printereffectiverights_object <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M429"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M429"/>
   <xsl:template match="@*|node()" priority="-2" mode="M429">
      <xsl:apply-templates select="*" mode="M429"/>
   </xsl:template>

   <!--PATTERN win-def_processtst_dep-->


	<!--RULE -->
<xsl:template match="win-def:process_test" priority="1000" mode="M430">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M430"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M430"/>
   <xsl:template match="@*|node()" priority="-2" mode="M430">
      <xsl:apply-templates select="*" mode="M430"/>
   </xsl:template>

   <!--PATTERN win-def_processtst-->


	<!--RULE -->
<xsl:template match="win-def:process_test/win-def:object" priority="1001" mode="M431">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:process_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a process_test must reference a process_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M431"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:process_test/win-def:state" priority="1000" mode="M431">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:process_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a process_test must reference a process_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M431"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M431"/>
   <xsl:template match="@*|node()" priority="-2" mode="M431">
      <xsl:apply-templates select="*" mode="M431"/>
   </xsl:template>

   <!--PATTERN win-def_processobj_dep-->


	<!--RULE -->
<xsl:template match="win-def:process_object" priority="1000" mode="M432">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED OBJECT: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M432"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M432"/>
   <xsl:template match="@*|node()" priority="-2" mode="M432">
      <xsl:apply-templates select="*" mode="M432"/>
   </xsl:template>

   <!--PATTERN win-def_processste_dep-->


	<!--RULE -->
<xsl:template match="win-def:process_state" priority="1000" mode="M433">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED STATE: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M433"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M433"/>
   <xsl:template match="@*|node()" priority="-2" mode="M433">
      <xsl:apply-templates select="*" mode="M433"/>
   </xsl:template>

   <!--PATTERN win-def_process58tst-->


	<!--RULE -->
<xsl:template match="win-def:process58_test/win-def:object" priority="1001" mode="M434">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:process58_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a process58_test must reference a process58_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M434"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:process58_test/win-def:state" priority="1000" mode="M434">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:process58_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a process58_test must reference a process58_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M434"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M434"/>
   <xsl:template match="@*|node()" priority="-2" mode="M434">
      <xsl:apply-templates select="*" mode="M434"/>
   </xsl:template>

   <!--PATTERN win-def_process58_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:process58_object//oval-def:filter" priority="1000" mode="M435">
      <xsl:variable name="parent_object" select="ancestor::win-def:process58_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='process58_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M435"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M435"/>
   <xsl:template match="@*|node()" priority="-2" mode="M435">
      <xsl:apply-templates select="*" mode="M435"/>
   </xsl:template>

   <!--PATTERN win-def_regtst-->


	<!--RULE -->
<xsl:template match="win-def:registry_test/win-def:object" priority="1001" mode="M436">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:registry_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a registry_test must reference a registry_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M436"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:registry_test/win-def:state" priority="1000" mode="M436">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:registry_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a registry_test must reference a registry_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M436"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M436"/>
   <xsl:template match="@*|node()" priority="-2" mode="M436">
      <xsl:apply-templates select="*" mode="M436"/>
   </xsl:template>

   <!--PATTERN win-def_registry_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:registry_object//oval-def:filter" priority="1000" mode="M437">
      <xsl:variable name="parent_object" select="ancestor::win-def:registry_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='registry_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M437"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M437"/>
   <xsl:template match="@*|node()" priority="-2" mode="M437">
      <xsl:apply-templates select="*" mode="M437"/>
   </xsl:template>

   <!--PATTERN win-def_regobjkey-->


	<!--RULE -->
<xsl:template match="win-def:registry_object/win-def:key[@xsi:nil='true' or @xsi:nil='1']"
                 priority="1000"
                 mode="M438">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="../win-def:name/@xsi:nil='true' or ../win-def:name/@xsi:nil='1'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - name entity must be nil when key is nil<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M438"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M438"/>
   <xsl:template match="@*|node()" priority="-2" mode="M438">
      <xsl:apply-templates select="*" mode="M438"/>
   </xsl:template>

   <!--PATTERN win-def_regobjkey2-->


	<!--RULE -->
<xsl:template match="win-def:registry_object/win-def:key[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M439">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@max_depth])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth behavior MUST not be used when a pattern match is used with a key entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_direction behavior MUST not be used when a pattern match is used with a key entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M439"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M439"/>
   <xsl:template match="@*|node()" priority="-2" mode="M439">
      <xsl:apply-templates select="*" mode="M439"/>
   </xsl:template>

   <!--PATTERN win-def_rap53tst-->


	<!--RULE -->
<xsl:template match="win-def:regkeyauditedpermissions53_test/win-def:object" priority="1001"
                 mode="M440">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:regkeyauditedpermissions53_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a regkeyauditedpermissions53_test must reference a regkeyauditedpermissions53_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M440"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:regkeyauditedpermissions53_test/win-def:state" priority="1000"
                 mode="M440">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:regkeyauditedpermissions53_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a regkeyauditedpermissions53_test must reference a regkeyauditedpermissions53_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M440"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M440"/>
   <xsl:template match="@*|node()" priority="-2" mode="M440">
      <xsl:apply-templates select="*" mode="M440"/>
   </xsl:template>

   <!--PATTERN win-def_regkeyauditedpermissions53_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:regkeyauditedpermissions53_object//oval-def:filter"
                 priority="1000"
                 mode="M441">
      <xsl:variable name="parent_object"
                    select="ancestor::win-def:regkeyauditedpermissions53_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='regkeyauditedpermissions53_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M441"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M441"/>
   <xsl:template match="@*|node()" priority="-2" mode="M441">
      <xsl:apply-templates select="*" mode="M441"/>
   </xsl:template>

   <!--PATTERN win-def_regkeyauditedpermissions53_objectkey-->


	<!--RULE -->
<xsl:template match="win-def:regkeyauditedpermissions53_object/win-def:key[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M442">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@max_depth])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth behavior MUST not be used when a pattern match is used with a key entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_direction behavior MUST not be used when a pattern match is used with a key entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M442"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M442"/>
   <xsl:template match="@*|node()" priority="-2" mode="M442">
      <xsl:apply-templates select="*" mode="M442"/>
   </xsl:template>

   <!--PATTERN win-def_rap53stestandard_synchronize-->


	<!--RULE -->
<xsl:template match="win-def:regkeyauditedpermissions53_state/win-def:standard_synchronize"
                 priority="1000"
                 mode="M443">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED ELEMENT: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M443"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M443"/>
   <xsl:template match="@*|node()" priority="-2" mode="M443">
      <xsl:apply-templates select="*" mode="M443"/>
   </xsl:template>

   <!--PATTERN win-def_rka53_include_group_value_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:objects/win-def:regkeyauditedpermissions53_object/win-def:behaviors"
                 priority="1000"
                 mode="M444">

		<!--REPORT -->
<xsl:if test="@include_group">DEPRECATED BEHAVIOR IN: win-def:regkeyauditedpermissions53_object<xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M444"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M444"/>
   <xsl:template match="@*|node()" priority="-2" mode="M444">
      <xsl:apply-templates select="*" mode="M444"/>
   </xsl:template>

   <!--PATTERN win-def_rka53_resolve_group_value_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:objects/win-def:regkeyauditedpermissions53_object/win-def:behaviors"
                 priority="1000"
                 mode="M445">

		<!--REPORT -->
<xsl:if test="@resolve_group">DEPRECATED BEHAVIOR IN: regkeyauditedpermissions53_object <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M445"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M445"/>
   <xsl:template match="@*|node()" priority="-2" mode="M445">
      <xsl:apply-templates select="*" mode="M445"/>
   </xsl:template>

   <!--PATTERN win-def_raptst_dep-->


	<!--RULE -->
<xsl:template match="win-def:regkeyauditedpermissions_test" priority="1000" mode="M446">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M446"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M446"/>
   <xsl:template match="@*|node()" priority="-2" mode="M446">
      <xsl:apply-templates select="*" mode="M446"/>
   </xsl:template>

   <!--PATTERN win-def_raptst-->


	<!--RULE -->
<xsl:template match="win-def:regkeyauditedpermissions_test/win-def:object" priority="1001"
                 mode="M447">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:regkeyauditedpermissions_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a regkeyauditedpermissions_test must reference a regkeyauditedpermissions_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M447"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:regkeyauditedpermissions_test/win-def:state" priority="1000"
                 mode="M447">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:regkeyauditedpermissions_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a regkeyauditedpermissions_test must reference a regkeyauditedpermissions_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M447"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M447"/>
   <xsl:template match="@*|node()" priority="-2" mode="M447">
      <xsl:apply-templates select="*" mode="M447"/>
   </xsl:template>

   <!--PATTERN win-def_rapobj_dep-->


	<!--RULE -->
<xsl:template match="win-def:regkeyauditedpermissions_object" priority="1000" mode="M448">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED OBJECT: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M448"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M448"/>
   <xsl:template match="@*|node()" priority="-2" mode="M448">
      <xsl:apply-templates select="*" mode="M448"/>
   </xsl:template>

   <!--PATTERN win-def_regkeyauditedpermissions_objectkey-->


	<!--RULE -->
<xsl:template match="win-def:regkeyauditedpermissions_object/win-def:key[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M449">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@max_depth])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth behavior MUST not be used when a pattern match is used with a key entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_direction behavior MUST not be used when a pattern match is used with a key entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M449"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M449"/>
   <xsl:template match="@*|node()" priority="-2" mode="M449">
      <xsl:apply-templates select="*" mode="M449"/>
   </xsl:template>

   <!--PATTERN win-def_rapste_dep-->


	<!--RULE -->
<xsl:template match="win-def:regkeyauditedpermissions_state" priority="1000" mode="M450">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED STATE: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M450"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M450"/>
   <xsl:template match="@*|node()" priority="-2" mode="M450">
      <xsl:apply-templates select="*" mode="M450"/>
   </xsl:template>

   <!--PATTERN win-def_rka_include_group_value_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:objects/win-def:regkeyauditedpermissions_object/win-def:behaviors"
                 priority="1000"
                 mode="M451">

		<!--REPORT -->
<xsl:if test="@include_group">DEPRECATED BEHAVIOR IN: win-def:regkeyauditedpermissions_object<xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M451"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M451"/>
   <xsl:template match="@*|node()" priority="-2" mode="M451">
      <xsl:apply-templates select="*" mode="M451"/>
   </xsl:template>

   <!--PATTERN win-def_rka_resolve_group_value_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:objects/win-def:regkeyauditedpermissions_object/win-def:behaviors"
                 priority="1000"
                 mode="M452">

		<!--REPORT -->
<xsl:if test="@resolve_group">DEPRECATED BEHAVIOR IN: regkeyauditedpermissions_object <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M452"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M452"/>
   <xsl:template match="@*|node()" priority="-2" mode="M452">
      <xsl:apply-templates select="*" mode="M452"/>
   </xsl:template>

   <!--PATTERN win-def_rer53tst-->


	<!--RULE -->
<xsl:template match="win-def:regkeyeffectiverights53_test/win-def:object" priority="1001"
                 mode="M453">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:regkeyeffectiverights53_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a regkeyeffectiverights53_test must reference a regkeyeffectiverights53_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M453"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:regkeyeffectiverights53_test/win-def:state" priority="1000"
                 mode="M453">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:regkeyeffectiverights53_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a regkeyeffectiverights53_test must reference a regkeyeffectiverights53_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M453"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M453"/>
   <xsl:template match="@*|node()" priority="-2" mode="M453">
      <xsl:apply-templates select="*" mode="M453"/>
   </xsl:template>

   <!--PATTERN win-def_regkeyeffectiverights53_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:regkeyeffectiverights53_object//oval-def:filter" priority="1000"
                 mode="M454">
      <xsl:variable name="parent_object" select="ancestor::win-def:regkeyeffectiverights53_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='regkeyeffectiverights53_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M454"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M454"/>
   <xsl:template match="@*|node()" priority="-2" mode="M454">
      <xsl:apply-templates select="*" mode="M454"/>
   </xsl:template>

   <!--PATTERN win-def_regkeyeffectiverights53_objectkey-->


	<!--RULE -->
<xsl:template match="win-def:regkeyeffectiverights53_object/win-def:key[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M455">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@max_depth])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth behavior MUST not be used when a pattern match is used with a key entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_direction behavior MUST not be used when a pattern match is used with a key entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M455"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M455"/>
   <xsl:template match="@*|node()" priority="-2" mode="M455">
      <xsl:apply-templates select="*" mode="M455"/>
   </xsl:template>

   <!--PATTERN win-def_rer53stestandard_synchronize-->


	<!--RULE -->
<xsl:template match="win-def:regkeyeffectiverights53_state/win-def:standard_synchronize"
                 priority="1000"
                 mode="M456">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED ELEMENT: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M456"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M456"/>
   <xsl:template match="@*|node()" priority="-2" mode="M456">
      <xsl:apply-templates select="*" mode="M456"/>
   </xsl:template>

   <!--PATTERN win-def_rke53_include_group_value_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:objects/win-def:regkeyeffectiverights53_object/win-def:behaviors"
                 priority="1000"
                 mode="M457">

		<!--REPORT -->
<xsl:if test="@include_group">DEPRECATED BEHAVIOR IN: win-def:regkeyeffectiverights53_object<xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M457"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M457"/>
   <xsl:template match="@*|node()" priority="-2" mode="M457">
      <xsl:apply-templates select="*" mode="M457"/>
   </xsl:template>

   <!--PATTERN win-def_rke53_resolve_group_value_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:objects/win-def:regkeyeffectiverights53_object/win-def:behaviors"
                 priority="1000"
                 mode="M458">

		<!--REPORT -->
<xsl:if test="@resolve_group">DEPRECATED BEHAVIOR IN: regkeyeffectiverights53_object <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M458"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M458"/>
   <xsl:template match="@*|node()" priority="-2" mode="M458">
      <xsl:apply-templates select="*" mode="M458"/>
   </xsl:template>

   <!--PATTERN win-def_rertst_dep-->


	<!--RULE -->
<xsl:template match="win-def:regkeyeffectiverights_test" priority="1000" mode="M459">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M459"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M459"/>
   <xsl:template match="@*|node()" priority="-2" mode="M459">
      <xsl:apply-templates select="*" mode="M459"/>
   </xsl:template>

   <!--PATTERN win-def_rertst-->


	<!--RULE -->
<xsl:template match="win-def:regkeyeffectiverights_test/win-def:object" priority="1001"
                 mode="M460">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:regkeyeffectiverights_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a regkeyeffectiverights_test must reference a regkeyeffectiverights_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M460"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:regkeyeffectiverights_test/win-def:state" priority="1000"
                 mode="M460">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:regkeyeffectiverights_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a regkeyeffectiverights_test must reference a regkeyeffectiverights_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M460"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M460"/>
   <xsl:template match="@*|node()" priority="-2" mode="M460">
      <xsl:apply-templates select="*" mode="M460"/>
   </xsl:template>

   <!--PATTERN win-def_rerobj_dep-->


	<!--RULE -->
<xsl:template match="win-def:regkeyeffectiverights_object" priority="1000" mode="M461">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED OBJECT: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M461"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M461"/>
   <xsl:template match="@*|node()" priority="-2" mode="M461">
      <xsl:apply-templates select="*" mode="M461"/>
   </xsl:template>

   <!--PATTERN win-def_regkeyeffectiverights_objectkey-->


	<!--RULE -->
<xsl:template match="win-def:regkeyeffectiverights_object/win-def:key[not(@operation='equals' or not(@operation))]"
                 priority="1000"
                 mode="M462">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@max_depth])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the max_depth behavior MUST not be used when a pattern match is used with a key entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(preceding-sibling::win-def:behaviors[@recurse_direction])"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the recurse_direction behavior MUST not be used when a pattern match is used with a key entity.<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M462"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M462"/>
   <xsl:template match="@*|node()" priority="-2" mode="M462">
      <xsl:apply-templates select="*" mode="M462"/>
   </xsl:template>

   <!--PATTERN win-def_rerste_dep-->


	<!--RULE -->
<xsl:template match="win-def:regkeyeffectiverights_state" priority="1000" mode="M463">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED STATE: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M463"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M463"/>
   <xsl:template match="@*|node()" priority="-2" mode="M463">
      <xsl:apply-templates select="*" mode="M463"/>
   </xsl:template>

   <!--PATTERN win-def_rke_include_group_value_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:objects/win-def:regkeyeffectiverights_object/win-def:behaviors"
                 priority="1000"
                 mode="M464">

		<!--REPORT -->
<xsl:if test="@include_group">DEPRECATED BEHAVIOR IN: win-def:regkeyeffectiverights_object<xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M464"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M464"/>
   <xsl:template match="@*|node()" priority="-2" mode="M464">
      <xsl:apply-templates select="*" mode="M464"/>
   </xsl:template>

   <!--PATTERN win-def_rke_resolve_group_value_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:objects/win-def:regkeyeffectiverights_object/win-def:behaviors"
                 priority="1000"
                 mode="M465">

		<!--REPORT -->
<xsl:if test="@resolve_group">DEPRECATED BEHAVIOR IN: regkeyeffectiverights_object <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M465"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M465"/>
   <xsl:template match="@*|node()" priority="-2" mode="M465">
      <xsl:apply-templates select="*" mode="M465"/>
   </xsl:template>

   <!--PATTERN win-def_servicetst-->


	<!--RULE -->
<xsl:template match="win-def:service_test/win-def:object" priority="1001" mode="M466">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:service_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a service_test must reference a service_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M466"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:service_test/win-def:state" priority="1000" mode="M466">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:service_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a service_test must reference a service_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M466"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M466"/>
   <xsl:template match="@*|node()" priority="-2" mode="M466">
      <xsl:apply-templates select="*" mode="M466"/>
   </xsl:template>

   <!--PATTERN win-def_service_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:service_object//oval-def:filter" priority="1000" mode="M467">
      <xsl:variable name="parent_object" select="ancestor::win-def:service_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='service_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M467"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M467"/>
   <xsl:template match="@*|node()" priority="-2" mode="M467">
      <xsl:apply-templates select="*" mode="M467"/>
   </xsl:template>

   <!--PATTERN win-def_svcertst-->


	<!--RULE -->
<xsl:template match="win-def:serviceeffectiverights_test/win-def:object" priority="1001"
                 mode="M468">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:serviceeffectiverights_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a serviceeffectiverights_test must reference a serviceeffectiverights_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M468"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:serviceeffectiverights_test/win-def:state" priority="1000"
                 mode="M468">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:serviceeffectiverights_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a serviceeffectiverights_test must reference a serviceeffectiverights_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M468"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M468"/>
   <xsl:template match="@*|node()" priority="-2" mode="M468">
      <xsl:apply-templates select="*" mode="M468"/>
   </xsl:template>

   <!--PATTERN win-def_serviceeffectiverights_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:serviceeffectiverights_object//oval-def:filter" priority="1000"
                 mode="M469">
      <xsl:variable name="parent_object" select="ancestor::win-def:serviceeffectiverights_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='serviceeffectiverights_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M469"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M469"/>
   <xsl:template match="@*|node()" priority="-2" mode="M469">
      <xsl:apply-templates select="*" mode="M469"/>
   </xsl:template>

   <!--PATTERN win-def_srtst-->


	<!--RULE -->
<xsl:template match="win-def:sharedresource_test/win-def:object" priority="1001" mode="M470">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:sharedresource_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a sharedresource_test must reference a sharedresource_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M470"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:sharedresource_test/win-def:state" priority="1000" mode="M470">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:sharedresource_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a sharedresource_test must reference a sharedresource_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M470"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M470"/>
   <xsl:template match="@*|node()" priority="-2" mode="M470">
      <xsl:apply-templates select="*" mode="M470"/>
   </xsl:template>

   <!--PATTERN win-def_sharedresource_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:sharedresource_object//oval-def:filter" priority="1000"
                 mode="M471">
      <xsl:variable name="parent_object" select="ancestor::win-def:sharedresource_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='sharedresource_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M471"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M471"/>
   <xsl:template match="@*|node()" priority="-2" mode="M471">
      <xsl:apply-templates select="*" mode="M471"/>
   </xsl:template>

   <!--PATTERN win-def_sraptst-->


	<!--RULE -->
<xsl:template match="win-def:sharedresourceauditedpermissions_test/win-def:object"
                 priority="1001"
                 mode="M472">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:sharedresourceauditedpermissions_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a sharedresourceauditedpermissions_test must reference a sharedresourceauditedpermissions_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M472"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:sharedresourceauditedpermissions_test/win-def:state"
                 priority="1000"
                 mode="M472">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:sharedresourceauditedpermissions_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a sharedresourceauditedpermissions_test must reference a sharedresourceauditedpermissions_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M472"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M472"/>
   <xsl:template match="@*|node()" priority="-2" mode="M472">
      <xsl:apply-templates select="*" mode="M472"/>
   </xsl:template>

   <!--PATTERN win-def_sharedresourceauditedpermissions_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:sharedresourceauditedpermissions_object//oval-def:filter"
                 priority="1000"
                 mode="M473">
      <xsl:variable name="parent_object"
                    select="ancestor::win-def:sharedresourceauditedpermissions_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='sharedresourceauditedpermissions_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M473"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M473"/>
   <xsl:template match="@*|node()" priority="-2" mode="M473">
      <xsl:apply-templates select="*" mode="M473"/>
   </xsl:template>

   <!--PATTERN win-def_srertst-->


	<!--RULE -->
<xsl:template match="win-def:sharedresourceeffectiverights_test/win-def:object"
                 priority="1001"
                 mode="M474">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:sharedresourceeffectiverights_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a sharedresourceeffectiverights_test must reference a sharedresourceeffectiverights_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M474"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:sharedresourceeffectiverights_test/win-def:state"
                 priority="1000"
                 mode="M474">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:sharedresourceeffectiverights_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a sharedresourceeffectiverights_test must reference a sharedresourceeffectiverights_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M474"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M474"/>
   <xsl:template match="@*|node()" priority="-2" mode="M474">
      <xsl:apply-templates select="*" mode="M474"/>
   </xsl:template>

   <!--PATTERN win-def_sharedresourceeffectiverights_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:sharedresourceeffectiverights_object//oval-def:filter"
                 priority="1000"
                 mode="M475">
      <xsl:variable name="parent_object"
                    select="ancestor::win-def:sharedresourceeffectiverights_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='sharedresourceeffectiverights_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M475"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M475"/>
   <xsl:template match="@*|node()" priority="-2" mode="M475">
      <xsl:apply-templates select="*" mode="M475"/>
   </xsl:template>

   <!--PATTERN win-def_sidtst-->


	<!--RULE -->
<xsl:template match="win-def:sid_test/win-def:object" priority="1001" mode="M476">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:sid_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a sid_test must reference a sid_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M476"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:sid_test/win-def:state" priority="1000" mode="M476">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:sid_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a sid_test must reference a sid_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M476"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M476"/>
   <xsl:template match="@*|node()" priority="-2" mode="M476">
      <xsl:apply-templates select="*" mode="M476"/>
   </xsl:template>

   <!--PATTERN win-def_sid_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:sid_object//oval-def:filter" priority="1000" mode="M477">
      <xsl:variable name="parent_object" select="ancestor::win-def:sid_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='sid_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M477"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M477"/>
   <xsl:template match="@*|node()" priority="-2" mode="M477">
      <xsl:apply-templates select="*" mode="M477"/>
   </xsl:template>

   <!--PATTERN win-def_sidsidtst-->


	<!--RULE -->
<xsl:template match="win-def:sid_sid_test/win-def:object" priority="1001" mode="M478">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:sid_sid_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a sid_sid_test must reference a sid_sid_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M478"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:sid_sid_test/win-def:state" priority="1000" mode="M478">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:sid_sid_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a sid_sid_test must reference a sid_sid_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M478"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M478"/>
   <xsl:template match="@*|node()" priority="-2" mode="M478">
      <xsl:apply-templates select="*" mode="M478"/>
   </xsl:template>

   <!--PATTERN win-def_sid_sid_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:sid_sid_object//oval-def:filter" priority="1000" mode="M479">
      <xsl:variable name="parent_object" select="ancestor::win-def:sid_sid_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='sid_sid_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M479"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M479"/>
   <xsl:template match="@*|node()" priority="-2" mode="M479">
      <xsl:apply-templates select="*" mode="M479"/>
   </xsl:template>

   <!--PATTERN win-def_uactst-->


	<!--RULE -->
<xsl:template match="win-def:uac_test/win-def:object" priority="1001" mode="M480">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:uac_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a uac_test must reference a uac_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M480"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:uac_test/win-def:state" priority="1000" mode="M480">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:uac_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a uac_test must reference a uac_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M480"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M480"/>
   <xsl:template match="@*|node()" priority="-2" mode="M480">
      <xsl:apply-templates select="*" mode="M480"/>
   </xsl:template>

   <!--PATTERN win-def_usertst-->


	<!--RULE -->
<xsl:template match="win-def:user_test/win-def:object" priority="1001" mode="M481">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:user_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a user_test must reference a user_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M481"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:user_test/win-def:state" priority="1000" mode="M481">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:user_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a user_test must reference a user_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M481"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M481"/>
   <xsl:template match="@*|node()" priority="-2" mode="M481">
      <xsl:apply-templates select="*" mode="M481"/>
   </xsl:template>

   <!--PATTERN win-def_user_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:user_object//oval-def:filter" priority="1000" mode="M482">
      <xsl:variable name="parent_object" select="ancestor::win-def:user_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='user_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M482"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M482"/>
   <xsl:template match="@*|node()" priority="-2" mode="M482">
      <xsl:apply-templates select="*" mode="M482"/>
   </xsl:template>

   <!--PATTERN win-def_usersid55tst-->


	<!--RULE -->
<xsl:template match="win-def:user_sid55_test/win-def:object" priority="1001" mode="M483">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:user_sid55_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a user_sid55_test must reference a user_sid55_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M483"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:user_sid55_test/win-def:state" priority="1000" mode="M483">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:user_sid55_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a user_sid55_test must reference a user_sid55_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M483"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M483"/>
   <xsl:template match="@*|node()" priority="-2" mode="M483">
      <xsl:apply-templates select="*" mode="M483"/>
   </xsl:template>

   <!--PATTERN win-def_user_sid55_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:user_sid55_object//oval-def:filter" priority="1000" mode="M484">
      <xsl:variable name="parent_object" select="ancestor::win-def:user_sid55_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='user_sid55_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M484"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M484"/>
   <xsl:template match="@*|node()" priority="-2" mode="M484">
      <xsl:apply-templates select="*" mode="M484"/>
   </xsl:template>

   <!--PATTERN win-def_usersidtst_dep-->


	<!--RULE -->
<xsl:template match="win-def:user_sid_test" priority="1000" mode="M485">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M485"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M485"/>
   <xsl:template match="@*|node()" priority="-2" mode="M485">
      <xsl:apply-templates select="*" mode="M485"/>
   </xsl:template>

   <!--PATTERN win-def_usersidtst-->


	<!--RULE -->
<xsl:template match="win-def:user_sid_test/win-def:object" priority="1001" mode="M486">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:user_sid_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a user_sid_test must reference a user_sid_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M486"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:user_sid_test/win-def:state" priority="1000" mode="M486">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:user_sid_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a user_sid_test must reference a user_sid_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M486"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M486"/>
   <xsl:template match="@*|node()" priority="-2" mode="M486">
      <xsl:apply-templates select="*" mode="M486"/>
   </xsl:template>

   <!--PATTERN win-def_usersidobj_dep-->


	<!--RULE -->
<xsl:template match="win-def:user_sid_object" priority="1000" mode="M487">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED OBJECT: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M487"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M487"/>
   <xsl:template match="@*|node()" priority="-2" mode="M487">
      <xsl:apply-templates select="*" mode="M487"/>
   </xsl:template>

   <!--PATTERN win-def_usersidste_dep-->


	<!--RULE -->
<xsl:template match="win-def:user_sid_state" priority="1000" mode="M488">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED STATE: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M488"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M488"/>
   <xsl:template match="@*|node()" priority="-2" mode="M488">
      <xsl:apply-templates select="*" mode="M488"/>
   </xsl:template>

   <!--PATTERN win-def_volumetst-->


	<!--RULE -->
<xsl:template match="win-def:volume_test/win-def:object" priority="1001" mode="M489">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:volume_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a volume_test must reference a volume_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M489"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:volume_test/win-def:state" priority="1000" mode="M489">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:volume_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a volume_test must reference a volume_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M489"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M489"/>
   <xsl:template match="@*|node()" priority="-2" mode="M489">
      <xsl:apply-templates select="*" mode="M489"/>
   </xsl:template>

   <!--PATTERN win-def_volume_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:volume_object//oval-def:filter" priority="1000" mode="M490">
      <xsl:variable name="parent_object" select="ancestor::win-def:volume_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='volume_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M490"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M490"/>
   <xsl:template match="@*|node()" priority="-2" mode="M490">
      <xsl:apply-templates select="*" mode="M490"/>
   </xsl:template>

   <!--PATTERN win-def_wmitst_dep-->


	<!--RULE -->
<xsl:template match="win-def:wmi_test" priority="1000" mode="M491">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED TEST: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M491"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M491"/>
   <xsl:template match="@*|node()" priority="-2" mode="M491">
      <xsl:apply-templates select="*" mode="M491"/>
   </xsl:template>

   <!--PATTERN win-def_wmitst-->


	<!--RULE -->
<xsl:template match="win-def:wmi_test/win-def:object" priority="1001" mode="M492">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:wmi_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a wmi_test must reference a wmi_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M492"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:wmi_test/win-def:state" priority="1000" mode="M492">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:wmi_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a wmi_test must reference a wmi_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M492"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M492"/>
   <xsl:template match="@*|node()" priority="-2" mode="M492">
      <xsl:apply-templates select="*" mode="M492"/>
   </xsl:template>

   <!--PATTERN win-def_wmiobj_dep-->


	<!--RULE -->
<xsl:template match="win-def:wmi_object" priority="1000" mode="M493">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED OBJECT: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M493"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M493"/>
   <xsl:template match="@*|node()" priority="-2" mode="M493">
      <xsl:apply-templates select="*" mode="M493"/>
   </xsl:template>

   <!--PATTERN win-def_wmiobjnamespace-->


	<!--RULE -->
<xsl:template match="win-def:wmi_object/win-def:namespace" priority="1000" mode="M494">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the namespace entity of a wmi_object should be 'equals', note that this overrules the general operation attribute validation (i.e. follow this one)<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M494"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M494"/>
   <xsl:template match="@*|node()" priority="-2" mode="M494">
      <xsl:apply-templates select="*" mode="M494"/>
   </xsl:template>

   <!--PATTERN win-def_wmiobjwql-->


	<!--RULE -->
<xsl:template match="win-def:wmi_object/win-def:wql" priority="1000" mode="M495">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the wql entity of a wmi_object should be 'equals', note that this overrules the general operation attribute validation (i.e. follow this one)<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M495"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M495"/>
   <xsl:template match="@*|node()" priority="-2" mode="M495">
      <xsl:apply-templates select="*" mode="M495"/>
   </xsl:template>

   <!--PATTERN win-def_wmiste_dep-->


	<!--RULE -->
<xsl:template match="win-def:wmi_state" priority="1000" mode="M496">

		<!--REPORT -->
<xsl:if test="true()">DEPRECATED STATE: <xsl:text/>
         <xsl:value-of select="name()"/>
         <xsl:text/> ID: <xsl:text/>
         <xsl:value-of select="@id"/>
         <xsl:text/>
         <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M496"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M496"/>
   <xsl:template match="@*|node()" priority="-2" mode="M496">
      <xsl:apply-templates select="*" mode="M496"/>
   </xsl:template>

   <!--PATTERN win-def_wmi57tst-->


	<!--RULE -->
<xsl:template match="win-def:wmi57_test/win-def:object" priority="1001" mode="M497">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:wmi57_object/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a wmi57_test must reference a wmi57_object<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M497"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:wmi57_test/win-def:state" priority="1000" mode="M497">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:wmi57_state/@id"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a wmi57_test must reference a wmi57_state<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M497"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M497"/>
   <xsl:template match="@*|node()" priority="-2" mode="M497">
      <xsl:apply-templates select="*" mode="M497"/>
   </xsl:template>

   <!--PATTERN win-def_wmi57_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:wmi57_object//oval-def:filter" priority="1000" mode="M498">
      <xsl:variable name="parent_object" select="ancestor::win-def:wmi57_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='wmi57_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M498"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M498"/>
   <xsl:template match="@*|node()" priority="-2" mode="M498">
      <xsl:apply-templates select="*" mode="M498"/>
   </xsl:template>

   <!--PATTERN win-def_wmi57objnamespace-->


	<!--RULE -->
<xsl:template match="win-def:wmi57_object/win-def:namespace" priority="1000" mode="M499">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the namespace entity of a wmi57_object should be 'equals', note that this overrules the general operation attribute validation (i.e. follow this one)<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M499"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M499"/>
   <xsl:template match="@*|node()" priority="-2" mode="M499">
      <xsl:apply-templates select="*" mode="M499"/>
   </xsl:template>

   <!--PATTERN win-def_wmi57objwql-->


	<!--RULE -->
<xsl:template match="win-def:wmi57_object/win-def:wql" priority="1000" mode="M500">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the wql entity of a wmi57_object should be 'equals', note that this overrules the general operation attribute validation (i.e. follow this one)<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M500"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M500"/>
   <xsl:template match="@*|node()" priority="-2" mode="M500">
      <xsl:apply-templates select="*" mode="M500"/>
   </xsl:template>

   <!--PATTERN win-def_wmi57steresult-->


	<!--RULE -->
<xsl:template match="win-def:wmi57_state/win-def:result" priority="1000" mode="M501">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@datatype='record'"/>
         <xsl:otherwise>
            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - datatype attribute for the result entity of a wmi57_object must be 'record'<xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M501"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M501"/>
   <xsl:template match="@*|node()" priority="-2" mode="M501">
      <xsl:apply-templates select="*" mode="M501"/>
   </xsl:template>

   <!--PATTERN win-def_wuaupdatesearchertst-->


	<!--RULE -->
<xsl:template match="win-def:wuaupdatesearcher_test/win-def:object" priority="1001"
                 mode="M502">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@object_ref=ancestor::oval-def:oval_definitions/oval-def:objects/win-def:wuaupdatesearcher_object/@id"/>
         <xsl:otherwise>
                            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the object child element of a wuaupdatesearcher_test must reference a wuaupdatesearcher_object
                        <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M502"/>
   </xsl:template>

	  <!--RULE -->
<xsl:template match="win-def:wuaupdatesearcher_test/win-def:state" priority="1000"
                 mode="M502">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="@state_ref=ancestor::oval-def:oval_definitions/oval-def:states/win-def:wuaupdatesearcher_state/@id"/>
         <xsl:otherwise>
                            <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - the state child element of a wuaupdatesearcher_test must reference a wuaupdatesearcher_state
                        <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M502"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M502"/>
   <xsl:template match="@*|node()" priority="-2" mode="M502">
      <xsl:apply-templates select="*" mode="M502"/>
   </xsl:template>

   <!--PATTERN win-def_wuaupdatesearcher_object_verify_filter_state-->


	<!--RULE -->
<xsl:template match="win-def:wuaupdatesearcher_object//oval-def:filter" priority="1000"
                 mode="M503">
      <xsl:variable name="parent_object" select="ancestor::win-def:wuaupdatesearcher_object"/>
      <xsl:variable name="parent_object_id" select="$parent_object/@id"/>
      <xsl:variable name="state_ref" select="."/>
      <xsl:variable name="reffed_state"
                    select="ancestor::oval-def:oval_definitions/oval-def:states/*[@id=$state_ref]"/>
      <xsl:variable name="state_name" select="local-name($reffed_state)"/>
      <xsl:variable name="state_namespace" select="namespace-uri($reffed_state)"/>

		    <!--ASSERT -->
<xsl:choose>
         <xsl:when test="(($state_namespace='http://oval.mitre.org/XMLSchema/oval-definitions-5#windows') and ($state_name='wuaupdatesearcher_state'))"/>
         <xsl:otherwise>State referenced in filter for <xsl:text/>
            <xsl:value-of select="name($parent_object)"/>
            <xsl:text/> '<xsl:text/>
            <xsl:value-of select="$parent_object_id"/>
            <xsl:text/>' is of the wrong type. <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M503"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M503"/>
   <xsl:template match="@*|node()" priority="-2" mode="M503">
      <xsl:apply-templates select="*" mode="M503"/>
   </xsl:template>

   <!--PATTERN win-def_wuaupdatesearcherobjsearchcriteria-->


	<!--RULE -->
<xsl:template match="win-def:wuaupdatesearcher_object/win-def:search_criteria"
                 priority="1000"
                 mode="M504">

		<!--ASSERT -->
<xsl:choose>
         <xsl:when test="not(@operation) or @operation='equals'"/>
         <xsl:otherwise>
                                                        <xsl:text/>
            <xsl:value-of select="../@id"/>
            <xsl:text/> - operation attribute for the search_criteria entity of a wuaupdatesearcher_object must be 'equals'
                                                    <xsl:value-of select="string('&#xA;')"/>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="*" mode="M504"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M504"/>
   <xsl:template match="@*|node()" priority="-2" mode="M504">
      <xsl:apply-templates select="*" mode="M504"/>
   </xsl:template>

   <!--PATTERN win-def_ssr_stype_special_value_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:states/win-def:sharedresource_state/win-def:shared_type"
                 priority="1000"
                 mode="M505">

		<!--REPORT -->
<xsl:if test=".='STYPE_SPECIAL'">
                                                            DEPRECATED ELEMENT VALUE IN: sharedresource_state ELEMENT VALUE: <xsl:text/>
         <xsl:value-of select="."/>
         <xsl:text/>
                                                      <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M505"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M505"/>
   <xsl:template match="@*|node()" priority="-2" mode="M505">
      <xsl:apply-templates select="*" mode="M505"/>
   </xsl:template>

   <!--PATTERN win-def_ssr_stype_temporary_value_dep-->


	<!--RULE -->
<xsl:template match="oval-def:oval_definitions/oval-def:states/win-def:sharedresource_state/win-def:shared_type"
                 priority="1000"
                 mode="M506">

		<!--REPORT -->
<xsl:if test=".='STYPE_TEMPORARY'">
                                                            DEPRECATED ELEMENT VALUE IN: sharedresource_state ELEMENT VALUE: <xsl:text/>
         <xsl:value-of select="."/>
         <xsl:text/>
                                                      <xsl:value-of select="string('&#xA;')"/>
      </xsl:if>
      <xsl:apply-templates select="*" mode="M506"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M506"/>
   <xsl:template match="@*|node()" priority="-2" mode="M506">
      <xsl:apply-templates select="*" mode="M506"/>
   </xsl:template>
</xsl:stylesheet>