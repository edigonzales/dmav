<?xml version="1.0"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ili="http://www.interlis.ch/xtf/2.4/INTERLIS" xmlns:FixpunkteLV_V1_0="http://www.interlis.ch/xtf/2.4/FixpunkteLV_V1_0">

    <xsl:output indent="no"/>
    <xsl:strip-space elements="*"/>

    <xsl:param name="fosnr" select="'XXXX'"/>


    <!-- Achtung fn:doc-available und/oder fn:document liefert false / nichts zurück, wenn keine gültiges XML-Dokument. -->

    <xsl:variable name="FixpunkteLV_V1_0_LFP">
        <xsl:choose>
            <xsl:when test="doc-available(concat('FixpunkteLV_V1_0_LFP.', $fosnr, '.xtf'))">
                <xsl:copy-of select="document(concat('FixpunkteLV_V1_0_LFP.', $fosnr, '.xtf'))"/>
                <xsl:message>Found: DMAV_FixpunkteLV_V1_0 (LFP)</xsl:message>
            </xsl:when>
            <xsl:otherwise>
                <empty/>
                <!-- truly empty node: 
                <xsl:sequence select="()"/> -->
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="FixpunkteLV_V1_0_HFP">
        <xsl:choose>
            <xsl:when test="doc-available(concat('FixpunkteLV_V1_0_HFP.', $fosnr, '.xtf'))">
                <xsl:copy-of select="document(concat('FixpunkteLV_V1_0_HFP.', $fosnr, '.xtf'))"/>
                <xsl:message>Found: DMAV_FixpunkteLV_V1_0 (HFP)</xsl:message>
            </xsl:when>
            <xsl:otherwise>
                <empty/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="KGKCGC_FPDS2_V1_1">
        <xsl:choose>
            <xsl:when test="doc-available(concat('KGKCGC_FPDS2_V1_1.', $fosnr, '.xtf'))">
                <xsl:copy-of select="document(concat('KGKCGC_FPDS2_V1_1.', $fosnr, '.xtf'))"/>
                <xsl:message>Found: KGKCGC_FPDS2_V1_1</xsl:message>
            </xsl:when>
            <xsl:otherwise>
                <empty/>
                <!-- truly empty node: 
                <xsl:sequence select="()"/> -->
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="DMAV_FixpunkteAVKategorie3_V1_0">
        <xsl:choose>
            <xsl:when test="doc-available(concat('DMAV_FixpunkteAVKategorie3_V1_0.', $fosnr, '.xtf'))">
                <xsl:copy-of select="document(concat('DMAV_FixpunkteAVKategorie3_V1_0.', $fosnr, '.xtf'))"/>
                <xsl:message>Found: DMAV_FixpunkteAVKategorie3_V1_0</xsl:message>
            </xsl:when>
            <xsl:otherwise>
                <empty/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="DMAV_Bodenbedeckung_V1_0">
        <xsl:choose>
            <xsl:when test="doc-available(concat('DMAV_Bodenbedeckung_V1_0.', $fosnr, '.xtf'))">
                <xsl:copy-of select="document(concat('DMAV_Bodenbedeckung_V1_0.', $fosnr, '.xtf'))"/>
                <xsl:message>Found: DMAV_Bodenbedeckung_V1_0</xsl:message>
            </xsl:when>
            <xsl:otherwise>
                <empty/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="DMAV_Einzelobjekte_V1_0">
        <xsl:choose>
            <xsl:when test="doc-available(concat('DMAV_Einzelobjekte_V1_0.', $fosnr, '.xtf'))">
                <xsl:copy-of select="document(concat('DMAV_Einzelobjekte_V1_0.', $fosnr, '.xtf'))"/>
                <xsl:message>Found: DMAV_Einzelobjekte_V1_0</xsl:message>
            </xsl:when>
            <xsl:otherwise>
                <empty/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="DMAV_Nomenklatur_V1_0">
        <xsl:choose>
            <xsl:when test="doc-available(concat('DMAV_Nomenklatur_V1_0.', $fosnr, '.xtf'))">
                <xsl:copy-of select="document(concat('DMAV_Nomenklatur_V1_0.', $fosnr, '.xtf'))"/>
                <xsl:message>Found: DMAV_Nomenklatur_V1_0</xsl:message>
            </xsl:when>
            <xsl:otherwise>
                <empty/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="DMAV_Grundstuecke_V1_0">
        <xsl:choose>
            <xsl:when test="doc-available(concat('DMAV_Grundstuecke_V1_0.', $fosnr, '.xtf'))">
                <xsl:copy-of select="document(concat('DMAV_Grundstuecke_V1_0.', $fosnr, '.xtf'))"/>
                <xsl:message>Found: DMAV_Grundstuecke_V1_0</xsl:message>
            </xsl:when>
            <xsl:otherwise>
                <empty/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="DMAV_Rohrleitungen_V1_0">
        <xsl:choose>
            <xsl:when test="doc-available(concat('DMAV_Rohrleitungen_V1_0.', $fosnr, '.xtf'))">
                <xsl:copy-of select="document(concat('DMAV_Rohrleitungen_V1_0.', $fosnr, '.xtf'))"/>
                <xsl:message>Found: DMAV_Rohrleitungen_V1_0</xsl:message>
            </xsl:when>
            <xsl:otherwise>
                <empty/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="DMAV_HoheitsgrenzenLV_V1_0">
        <xsl:choose>
            <xsl:when test="doc-available(concat('DMAV_HoheitsgrenzenLV_V1_0.', $fosnr, '.xtf'))">
                <xsl:copy-of select="document(concat('DMAV_HoheitsgrenzenLV_V1_0.', $fosnr, '.xtf'))"/>
                <xsl:message>Found: DMAV_HoheitsgrenzenLV_V1_0</xsl:message>
            </xsl:when>
            <xsl:otherwise>
                <empty/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="DMAV_HoheitsgrenzenAV_V1_0">
        <xsl:choose>
            <xsl:when test="doc-available(concat('DMAV_HoheitsgrenzenAV_V1_0.', $fosnr, '.xtf'))">
                <xsl:copy-of select="document(concat('DMAV_HoheitsgrenzenAV_V1_0.', $fosnr, '.xtf'))"/>
                <xsl:message>Found: DMAV_HoheitsgrenzenAV_V1_0</xsl:message>
            </xsl:when>
            <xsl:otherwise>
                <empty/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="DMAV_Toleranzstufen_V1_0">
        <xsl:choose>
            <xsl:when test="doc-available(concat('DMAV_Toleranzstufen_V1_0.', $fosnr, '.xtf'))">
                <xsl:copy-of select="document(concat('DMAV_Toleranzstufen_V1_0.', $fosnr, '.xtf'))"/>
                <xsl:message>Found: DMAV_Toleranzstufen_V1_0</xsl:message>
            </xsl:when>
            <xsl:otherwise>
                <empty/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="DMAV_DauerndeBodenverschiebungen_V1_0">
        <xsl:choose>
            <xsl:when test="doc-available(concat('DMAV_DauerndeBodenverschiebungen_V1_0.', $fosnr, '.xtf'))">
                <xsl:copy-of select="document(concat('DMAV_DauerndeBodenverschiebungen_V1_0.', $fosnr, '.xtf'))"/>
                <xsl:message>Found: DMAV_DauerndeBodenverschiebungen_V1_0</xsl:message>
            </xsl:when>
            <xsl:otherwise>
                <empty/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="OfficialIndexOfLocalities_V1_0">
        <xsl:choose>
            <xsl:when test="doc-available(concat('OfficialIndexOfLocalities_V1_0.', $fosnr, '.xtf'))">
                <xsl:copy-of select="document(concat('OfficialIndexOfLocalities_V1_0.', $fosnr, '.xtf'))"/>
                <xsl:message>Found: OfficialIndexOfLocalities_V1_0</xsl:message>
            </xsl:when>
            <xsl:otherwise>
                <empty/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="DMAV_Gebaeudeadressen_V1_0">
        <xsl:choose>
            <xsl:when test="doc-available(concat('DMAV_Gebaeudeadressen_V1_0.', $fosnr, '.xtf'))">
                <xsl:copy-of select="document(concat('DMAV_Gebaeudeadressen_V1_0.', $fosnr, '.xtf'))"/>
                <xsl:message>Found: DMAV_Gebaeudeadressen_V1_0</xsl:message>
            </xsl:when>
            <xsl:otherwise>
                <empty/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="DMAVSUP_UntereinheitGrundbuch_V1_0">
        <xsl:choose>
            <xsl:when test="doc-available(concat('DMAVSUP_UntereinheitGrundbuch_V1_0.', $fosnr, '.xtf'))">
                <xsl:copy-of select="document(concat('DMAVSUP_UntereinheitGrundbuch_V1_0.', $fosnr, '.xtf'))"/>
                <xsl:message>Found: DMAVSUP_UntereinheitGrundbuch_V1_0</xsl:message>
            </xsl:when>
            <xsl:otherwise>
                <empty/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="DMAV_Dienstbarkeitsgrenzen_V1_0">
        <xsl:choose>
            <xsl:when test="doc-available(concat('DMAV_Dienstbarkeitsgrenzen_V1_0.', $fosnr, '.xtf'))">
                <xsl:copy-of select="document(concat('DMAV_Dienstbarkeitsgrenzen_V1_0.', $fosnr, '.xtf'))"/>
                <xsl:message>Found: DMAV_Dienstbarkeitsgrenzen_V1_0</xsl:message>
            </xsl:when>
            <xsl:otherwise>
                <empty/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>

    <xsl:template match="/ili:transfer">
        <!-- <xsl:message><xsl:value-of select="$fosnr"/></xsl:message> -->
        <!-- <xsl:message>Hallo Welt</xsl:message> -->
        
        <xsl:copy>
            <xsl:copy-of select="@*"/>
            <xsl:copy-of copy-namespaces="no" select="ili:headersection"/>
            <ili:datasection>
                <FixpunkteLV_V1_0:FixpunkteLV ili:bid="bb3b5f52-707b-4ac6-9cf8-4d03ef37bb3a">
                    <xsl:copy-of copy-namespaces="no" select="$FixpunkteLV_V1_0_LFP/ili:transfer/ili:datasection/FixpunkteLV_V1_0:FixpunkteLV/FixpunkteLV_V1_0:LFP1"/>
                    <xsl:copy-of copy-namespaces="no" select="$FixpunkteLV_V1_0_HFP/ili:transfer/ili:datasection/FixpunkteLV_V1_0:FixpunkteLV/FixpunkteLV_V1_0:HFP1"/>
                </FixpunkteLV_V1_0:FixpunkteLV>
                <!--<xsl:copy-of select="$KGKCGC_FPDS2_V1_1/ili:transfer/ili:datasection/*"/>-->
                <xsl:copy-of copy-namespaces="no" select="$KGKCGC_FPDS2_V1_1/ili:transfer/ili:datasection/*"/>
                <xsl:copy-of copy-namespaces="no" select="$DMAV_FixpunkteAVKategorie3_V1_0/ili:transfer/ili:datasection/*"/>
                <xsl:copy-of copy-namespaces="no" select="$DMAV_Bodenbedeckung_V1_0/ili:transfer/ili:datasection/*"/>
                <xsl:copy-of copy-namespaces="no" select="$DMAV_Einzelobjekte_V1_0/ili:transfer/ili:datasection/*"/>
                <xsl:copy-of copy-namespaces="no" select="$DMAV_Nomenklatur_V1_0/ili:transfer/ili:datasection/*"/>
                <xsl:copy-of copy-namespaces="no" select="$DMAV_Grundstuecke_V1_0/ili:transfer/ili:datasection/*"/>
                <xsl:copy-of copy-namespaces="no" select="$DMAV_Rohrleitungen_V1_0/ili:transfer/ili:datasection/*"/>
                <xsl:copy-of copy-namespaces="no" select="$DMAV_HoheitsgrenzenLV_V1_0/ili:transfer/ili:datasection/*"/>
                <xsl:copy-of copy-namespaces="no" select="$DMAV_HoheitsgrenzenAV_V1_0/ili:transfer/ili:datasection/*"/>
                <xsl:copy-of copy-namespaces="no" select="$DMAV_Toleranzstufen_V1_0/ili:transfer/ili:datasection/*"/>
                <xsl:copy-of copy-namespaces="no" select="$DMAV_DauerndeBodenverschiebungen_V1_0/ili:transfer/ili:datasection/*"/>
                <xsl:copy-of copy-namespaces="no" select="$OfficialIndexOfLocalities_V1_0/ili:transfer/ili:datasection/*"/>
                <xsl:copy-of copy-namespaces="no" select="$DMAV_Gebaeudeadressen_V1_0/ili:transfer/ili:datasection/*"/>
                <xsl:copy-of copy-namespaces="no" select="$DMAVSUP_UntereinheitGrundbuch_V1_0/ili:transfer/ili:datasection/*"/>
                <xsl:copy-of copy-namespaces="no" select="$DMAV_Dienstbarkeitsgrenzen_V1_0/ili:transfer/ili:datasection/*"/>
            </ili:datasection>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
