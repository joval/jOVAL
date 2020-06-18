<?xml version="1.0" encoding="UTF-8"?>
<!--
  Copyright (C) 2020 JovalCM.com.  All rights reserved.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" version="1.0">

    <xsl:template match='@*|node()'>
        <xsl:copy>
            <xsl:apply-templates select='@*|node()'/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="xs:complexType[@name='EntityStateFamilyType']"/>
    <xsl:template match="xs:complexType[@name='EntityItemFamilyType']"/>

</xsl:stylesheet>

