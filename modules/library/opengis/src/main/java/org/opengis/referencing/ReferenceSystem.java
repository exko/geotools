/*$************************************************************************************************
 **
 ** $Id: ReferenceSystem.java 1414 2009-06-02 17:15:45Z desruisseaux $
 **
 ** $URL: https://geoapi.svn.sourceforge.net/svnroot/geoapi/tags/2.3-M2/geoapi/src/main/java/org/opengis/referencing/ReferenceSystem.java $
 **
 ** Copyright (C) 2003-2005 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/
package org.opengis.referencing;

import org.opengis.metadata.extent.Extent;
import org.opengis.util.InternationalString;
import org.opengis.annotation.UML;

import static org.opengis.annotation.Obligation.*;
import static org.opengis.annotation.Specification.*;


/**
 * Description of a spatial and temporal reference system used by a dataset.
 *
 * @departure
 *   This interface was initially derived from an ISO 19111 specification published in 2003. Later
 *   revisions (in 2005) rely on an interface defined in ISO 19115 instead. The annotations were
 *   updated accordingly, but this interface is still defined in the referencing package instead
 *   of the metadata package for this historical reason.
 *
 * @version <A HREF="http://portal.opengeospatial.org/files/?artifact_id=6716">Abstract specification 2.0</A>
 * @author  Martin Desruisseaux (IRD)
 * @since   GeoAPI 1.0
 *
 * @see org.opengis.referencing.crs.CoordinateReferenceSystem
 */
@UML(identifier="RS_ReferenceSystem", specification=ISO_19115)
public interface ReferenceSystem extends IdentifiedObject {
    /**
     * Key for the <code>{@value}</code> property to be given to the
     * {@linkplain ObjectFactory object factory} <code>createFoo(&hellip;)</code> methods.
     * This is used for setting the value to be returned by {@link #getDomainOfValidity}.
     *
     * @see #getDomainOfValidity
     *
     * @since GeoAPI 2.1
     */
    String DOMAIN_OF_VALIDITY_KEY = "domainOfValidity";

    /**
     * Key for the <code>{@value}</code> property to be given to the
     * {@linkplain ObjectFactory object factory} <code>createFoo(&hellip;)</code> methods.
     * This is used for setting the value to be returned by {@link #getScope}.
     *
     * @see #getScope
     */
    String SCOPE_KEY = "scope";

    /**
     * Area or region or timeframe in which this (coordinate) reference system is valid.
     *
     * @return The reference system valid domain, or {@code null} if not available.
     *
     * @since GeoAPI 2.1
     *
     * @departure
     *   This method is conform to the specification published in 2003. Later revisions changed
     *   the multiplicity, so the return type should now be a collection. The singleton has been
     *   preserved in GeoAPI for historical reasons, and also because {@code Extent} attributes
     *   already allow collections.
     */
    @UML(identifier="domainOfValidity", obligation=OPTIONAL, specification=ISO_19111)
    Extent getDomainOfValidity();

    /**
     * Description of domain of usage, or limitations of usage, for which this
     * Reference System object is valid.
     *
     * @return The domain of usage, or {@code null} if none.
     *
     * @departure
     *   This method is conform to the specification published in 2003. Later revision moved
     *   this attribute to subclass, but GeoAPI keep this method here for historical reasons.
     *   The obligation still optional, as opposed to ISO 19111:2007 which make this attribute
     *   mandatory while mandating the text "<cite>not known</cite>" if the scope is unknown.
     *   Finally the return value still a singleton as in the 2003 version, as opposed to the
     *   2007 version which would mandate a collection.
     */
    @UML(identifier="SC_CRS.scope", obligation=OPTIONAL, specification=ISO_19111)
    InternationalString getScope();
}
