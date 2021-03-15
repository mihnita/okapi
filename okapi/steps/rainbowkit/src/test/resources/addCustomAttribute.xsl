<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlf="urn:oasis:names:tc:xliff:document:1.2">
	<xsl:template match="/xlf:xliff/xlf:file">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<xsl:attribute name="custom-attribute" namespace="custom-uri">custom-val</xsl:attribute>
			<xsl:apply-templates/>
		</xsl:copy>		
	</xsl:template>
	<!-- standard copy template -->
	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<xsl:apply-templates/>
		</xsl:copy>
	</xsl:template>	
</xsl:stylesheet>