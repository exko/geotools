/*$************************************************************************************************
 **
 ** $Id: Extent.java 1422 2009-06-17 14:21:57Z desruisseaux $
 **
 ** $URL: https://geoapi.svn.sourceforge.net/svnroot/geoapi/tags/2.3-M2/geoapi/src/main/java/org/opengis/metadata/extent/Extent.java $
 **
 ** Copyright (C) 2004-2005 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/
package org.opengis.metadata.extent;

import java.util.Collection;
import org.opengis.util.InternationalString;
import org.opengis.annotation.Profile;
import org.opengis.annotation.UML;

import static org.opengis.annotation.Obligation.*;
import static org.opengis.annotation.Specification.*;
import static org.opengis.annotation.ComplianceLevel.*;


/**
 * Information about spatial, vertical, and temporal extent.
 * This interface has four optional attributes
 * ({@linkplain #getGeographicElements geographic elements},
 *  {@linkplain #getTemporalElements temporal elements}, and
 *  {@linkplain #getVerticalElements vertical elements}) and an element called
 *  {@linkplain #getDescription description}.
 *  At least one of the four shall be used.
 *
 * @version <A HREF="http://www.opengeospatial.org/standards/as#01-111">ISO 19115</A>
 * @author  Martin Desruisseaux (IRD)
 * @since   GeoAPI 1.0
 */
@UML(identifier="EX_Extent", specification=ISO_19115)
public interface Extent {
    /**
     * Returns the spatial and temporal extent for the referring object.
     *
     * @return The spatial and temporal extent, or {@code null} in none.
     *
     * @condition {@linkplain #getGeographicElements Geographic element},
     *            {@linkplain #getTemporalElements temporal element} and
     *            {@linkplain #getVerticalElements vertical element} not documented.
     */
    @UML(identifier="description", obligation=CONDITIONAL, specification=ISO_19115)
    InternationalString getDescription();

    /**
     * Provides geographic component of the extent of the referring object
     *
     * @return The geographic extent, or an empty set if none.
     *
     * @condition {@linkplain #getDescription Description},
     *            {@linkplain #getTemporalElements temporal element} and
     *            {@linkplain #getVerticalElements vertical element} not documented.
     */
    @Profile(level=CORE)
    @UML(identifier="geographicElement", obligation=CONDITIONAL, specification=ISO_19115)
    Collection<? extends GeographicExtent> getGeographicElements();

    /**
     * Provides temporal component of the extent of the referring object
     *
     * @return The temporal extent, or an empty set if none.
     *
     * @condition {@linkplain #getDescription Description},
     *            {@linkplain #getGeographicElements geographic element} and
     *            {@linkplain #getVerticalElements vertical element} not documented.
     */
    @Profile(level=CORE)
    @UML(identifier="temporalElement", obligation=CONDITIONAL, specification=ISO_19115)
    Collection<? extends TemporalExtent> getTemporalElements();

    /**
     * Provides vertical component of the extent of the referring object
     *
     * @return The vertical extent, or an empty set if none.
     *
     * @condition {@linkplain #getDescription Description},
     *            {@linkplain #getGeographicElements geographic element} and
     *            {@linkplain #getTemporalElements temporal element} not documented.
     */
    @Profile(level=CORE)
    @UML(identifier="verticalElement", obligation=CONDITIONAL, specification=ISO_19115)
    Collection<? extends VerticalExtent> getVerticalElements();
}
