<?xml version="1.0"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<!--Identity template, 
        provides default behavior that copies all content into the output -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <!--More specific template for renaming the localizable tag for text APO template -->
    <xsl:template match="localizable[matches(@id, '.*.body.text$')]">  
        <localizable_text>
            <xsl:apply-templates select="@*|node()"/>
        </localizable_text>
    </xsl:template>

</xsl:stylesheet>