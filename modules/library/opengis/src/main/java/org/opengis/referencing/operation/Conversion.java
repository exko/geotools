/*$************************************************************************************************
 **
 ** $Id: Conversion.java 1415 2009-06-03 12:41:40Z desruisseaux $
 **
 ** $URL: https://geoapi.svn.sourceforge.net/svnroot/geoapi/tags/2.3-M2/geoapi/src/main/java/org/opengis/referencing/operation/Conversion.java $
 **
 ** Copyright (C) 2003-2005 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/
package org.opengis.referencing.operation;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.annotation.UML;
import static org.opengis.annotation.Obligation.*;
import static org.opengis.annotation.Specification.*;


/**
 * An operation on coordinates that does not include any change of Datum. The best-known
 * example of a coordinate conversion is a map projection. The parameters describing
 * coordinate conversions are defined rather than empirically derived.
 * <p>
 * Note that some conversions have no parameters.
 *
 * @version <A HREF="http://portal.opengeospatial.org/files/?artifact_id=6716">Abstract specification 2.0</A>
 * @author  Martin Desruisseaux (IRD)
 * @since   GeoAPI 1.0
 *
 * @see Transformation
 */
@UML(identifier="CC_Conversion", specification=ISO_19111)
public interface Conversion extends Operation {
    /**
     * Returns the source CRS. Conversions may have a source CRS that
     * is not specified here, but through
     * {@link org.opengis.referencing.crs.GeneralDerivedCRS#getBaseCRS} instead.
     *
     * @return The source CRS, or {@code null} if not available.
     */
    @UML(identifier="sourceCRS", obligation=OPTIONAL, specification=ISO_19111)
    CoordinateReferenceSystem getSourceCRS();

    /**
     * Returns the target CRS. {@linkplain Conversion Conversions} may have a target CRS
     * that is not specified here, but through
     * {@link org.opengis.referencing.crs.GeneralDerivedCRS} instead.
     *
     * @return The target CRS, or {@code null} if not available.
     */
    @UML(identifier="targetCRS", obligation=OPTIONAL, specification=ISO_19111)
    CoordinateReferenceSystem getTargetCRS();

    /**
     * This attribute is declared in {@link CoordinateOperation} but is not used in a conversion.
     *
     * @return Always {@code null}.
     */
    @UML(identifier="operationVersion", obligation=FORBIDDEN, specification=ISO_19111)
    String getOperationVersion();
}
