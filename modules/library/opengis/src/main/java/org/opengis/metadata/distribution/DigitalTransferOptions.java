/*$************************************************************************************************
 **
 ** $Id: DigitalTransferOptions.java 1264 2008-07-09 17:46:15Z desruisseaux $
 **
 ** $URL: https://geoapi.svn.sourceforge.net/svnroot/geoapi/tags/2.3-M1/geoapi/src/main/java/org/opengis/metadata/distribution/DigitalTransferOptions.java $
 **
 ** Copyright (C) 2004-2005 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/
package org.opengis.metadata.distribution;

import java.util.Collection;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.OnLineResource;
import org.opengis.annotation.UML;
import org.opengis.annotation.Profile;

import static org.opengis.annotation.Obligation.*;
import static org.opengis.annotation.Specification.*;
import static org.opengis.annotation.ComplianceLevel.*;


/**
 * Technical means and media by which a resource is obtained from the distributor.
 *
 * @version <A HREF="http://www.opengeospatial.org/standards/as#01-111">ISO 19115</A>
 * @author  Martin Desruisseaux (IRD)
 * @author  Cory Horner (Refractions Research)
 * @since   GeoAPI 2.0
 */
@Profile (level=CORE)
@UML(identifier="MD_DigitalTransferOptions", specification=ISO_19115)
public interface DigitalTransferOptions {
    /**
     * Tiles, layers, geographic areas, <cite>etc.</cite>, in which data is available.
     *
     * @return  Tiles, layers, geographic areas, <cite>etc.</cite> in which data is available, or {@code null}.
     */
    @UML(identifier="unitsOfDistribution", obligation=OPTIONAL, specification=ISO_19115)
    InternationalString getUnitsOfDistribution();

    /**
     * Estimated size of a unit in the specified transfer format, expressed in megabytes.
     * The transfer size is &gt; 0.0.
     * Returns {@code null} if the transfer size is unknown.
     *
     * @return Estimated size of a unit in the specified transfer format in megabytes, or {@code null}.
     */
    @UML(identifier="transferSize", obligation=OPTIONAL, specification=ISO_19115)
    Double getTransferSize();

    /**
     * Information about online sources from which the resource can be obtained.
     *
     * @return Online sources from which the resource can be obtained.
     */
    @Profile (level=CORE)
    @UML(identifier="onLine", obligation=OPTIONAL, specification=ISO_19115)
    Collection<? extends OnLineResource> getOnLines();

    /**
     * Information about offline media on which the resource can be obtained.
     *
     * @return  offline media on which the resource can be obtained, or {@code null}.
     */
    @UML(identifier="offLine", obligation=OPTIONAL, specification=ISO_19115)
    Medium getOffLine();
}
