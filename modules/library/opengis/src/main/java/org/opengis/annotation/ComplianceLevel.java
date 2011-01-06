/*$************************************************************************************************
 **
 ** $Id: ComplianceLevel.java 1422 2009-06-17 14:21:57Z desruisseaux $
 **
 ** $URL: https://geoapi.svn.sourceforge.net/svnroot/geoapi/tags/2.3-M2/geoapi/src/main/java/org/opengis/annotation/ComplianceLevel.java $
 **
 ** Copyright (C) 2003-2005 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/
package org.opengis.annotation;


/**
 * Compliance level for elements. The international standards defines an extensive set of
 * metadata elements. Typically only a subset of the full number of elements is used.
 * However, it is essential that a basic minimum number of metadata elements be maintained
 * for a dataset.
 *
 * @version <A HREF="http://www.opengeospatial.org/standards/as#01-111">ISO 19115</A>
 * @author  Martin Desruisseaux (IRD)
 * @since   GeoAPI 2.0
 */
public enum ComplianceLevel {
    /**
     * Core metadata elements required to identify a dataset, typically for catalogue purposes.
     * This level specifies metadata elements answering the following questions: "Does a dataset
     * on a specific topic exist (what)?", "For a specific place (where)?", "For a specific date
     * or period (when)?" and "A point of contact to learn more about or order the dataset (who)?".
     * Using the recommended {@linkplain Obligation#OPTIONAL optional} elements in addition to the
     * {@linkplain Obligation#MANDATORY mandatory} elements will increase interoperability,
     * allowing users to understand without ambiguity the geographic data and the related metadata
     * provided by either the producer or the distributor.
     */
    CORE,

    /**
     * Indicates a required element of the spatial profile.
     *
     * @deprecated This enum is not defined by ISO.
     */
    @Deprecated
    SPATIAL,

    /**
     * Indicates a required element of the feature profile.
     *
     * @deprecated This enum is not defined by ISO.
     */
    @Deprecated
    FEATURE,

    /**
     * Indicates a required element of the data provider profile.
     *
     * @deprecated This enum is not defined by ISO.
     */
    @Deprecated
    DATA_PROVIDER,

    /**
     * Indicates a required element of the display object profile.
     *
     * @deprecated This enum is not defined by ISO.
     */
    @Deprecated
    DISPLAY_OBJECT,

    /**
     * Indicates a required element of the editable display object profile.
     *
     * @deprecated This enum is not defined by ISO.
     */
    @Deprecated
    EDITABLE_DISPLAY_OBJECT,

    /**
     * Indicates a required element of the feature display object profile.
     *
     * @deprecated This enum is not defined by ISO.
     */
    @Deprecated
    FEATURE_DISPLAY_OBJECT
}
