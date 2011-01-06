/*$************************************************************************************************
 **
 ** $Id: GeodeticDatum.java 1265 2008-07-09 18:24:37Z desruisseaux $
 **
 ** $URL: https://geoapi.svn.sourceforge.net/svnroot/geoapi/tags/2.3-M1/geoapi/src/main/java/org/opengis/referencing/datum/GeodeticDatum.java $
 **
 ** Copyright (C) 2003-2005 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/
package org.opengis.referencing.datum;

import org.opengis.annotation.UML;

import static org.opengis.annotation.Obligation.*;
import static org.opengis.annotation.Specification.*;


/**
 * Defines the location and precise orientation in 3-dimensional space of a defined ellipsoid
 * (or sphere) that approximates the shape of the earth. Used also for Cartesian coordinate
 * system centered in this ellipsoid (or sphere).
 *
 * @version <A HREF="http://portal.opengeospatial.org/files/?artifact_id=6716">Abstract specification 2.0</A>
 * @author  Martin Desruisseaux (IRD)
 * @since   GeoAPI 1.0
 *
 * @see Ellipsoid
 * @see PrimeMeridian
 */
@UML(identifier="CD_GeodeticDatum", specification=ISO_19111)
public interface GeodeticDatum extends Datum {
    /**
     * Returns the ellipsoid.
     *
     * @return The ellipsoid.
     */
    @UML(identifier="usesEllipsoid", obligation=MANDATORY, specification=ISO_19111)
    Ellipsoid getEllipsoid();

    /**
     * Returns the prime meridian.
     *
     * @return The prime meridian.
     */
    @UML(identifier="usesPrimeMeridian", obligation=MANDATORY, specification=ISO_19111)
    PrimeMeridian getPrimeMeridian();
}
