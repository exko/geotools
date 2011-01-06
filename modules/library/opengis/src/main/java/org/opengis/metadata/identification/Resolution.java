/*$************************************************************************************************
 **
 ** $Id: Resolution.java 1422 2009-06-17 14:21:57Z desruisseaux $
 **
 ** $URL: https://geoapi.svn.sourceforge.net/svnroot/geoapi/tags/2.3-M2/geoapi/src/main/java/org/opengis/metadata/identification/Resolution.java $
 **
 ** Copyright (C) 2004-2005 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/
package org.opengis.metadata.identification;

import org.opengis.annotation.UML;
import org.opengis.annotation.Profile;

import static org.opengis.annotation.Obligation.*;
import static org.opengis.annotation.Specification.*;
import static org.opengis.annotation.ComplianceLevel.*;


/**
 * Level of detail expressed as a scale factor or a ground distance.
 *
 * @version <A HREF="http://www.opengeospatial.org/standards/as#01-111">ISO 19115</A>
 * @author  Martin Desruisseaux (IRD)
 * @author  Cory Horner (Refractions Research)
 * @since   GeoAPI 2.0
 */
@UML(identifier="MD_Resolution", specification=ISO_19115)
public interface Resolution {
    /**
     * Level of detail expressed as the scale of a comparable hardcopy map or chart.
     * Only one of {@linkplain #getEquivalentScale equivalent scale} and
     * {@linkplain #getDistance ground sample distance} may be provided.
     *
     * @return Level of detail expressed as the scale of a comparable hardcopy, or {@code null}.
     *
     * @condition {@linkplain #getDistance() Distance} not documented.
     */
    @Profile(level=CORE)
    @UML(identifier="equivalentScale", obligation=CONDITIONAL, specification=ISO_19115)
    RepresentativeFraction getEquivalentScale();

    /**
     * Ground sample distance.
     * Only one of {@linkplain #getEquivalentScale equivalent scale} and
     * {@linkplain #getDistance ground sample distance} may be provided.
     *
     * @return The ground sample distance, or {@code null}.
     *
     * @todo change return type to ISO 19103 {@code Distance} or to JScience {@code Measure}.
     * @unitof Distance
     *
     * @condition {@linkplain #getEquivalentScale() Equivalent scale} not documented.
     */
    @Profile(level=CORE)
    @UML(identifier="distance", obligation=CONDITIONAL, specification=ISO_19115)
    Double getDistance();
}
