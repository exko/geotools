/*$************************************************************************************************
 **
 ** $Id: Telephone.java 1264 2008-07-09 17:46:15Z desruisseaux $
 **
 ** $URL: https://geoapi.svn.sourceforge.net/svnroot/geoapi/tags/2.3-M2/geoapi/src/main/java/org/opengis/metadata/citation/Telephone.java $
 **
 ** Copyright (C) 2004-2005 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/
package org.opengis.metadata.citation;

import java.util.Collection;
import org.opengis.annotation.UML;

import static org.opengis.annotation.Obligation.*;
import static org.opengis.annotation.Specification.*;


/**
 * Telephone numbers for contacting the responsible individual or organization.
 *
 * @version <A HREF="http://www.opengeospatial.org/standards/as#01-111">ISO 19115</A>
 * @author  Martin Desruisseaux (IRD)
 * @author  Cory Horner (Refractions Research)
 * @since   GeoAPI 1.0
 */
@UML(identifier="CI_Telephone", specification=ISO_19115)
public interface Telephone {
    /**
     * Telephone numbers by which individuals can speak to the responsible organization or individual.
     *
     * @return Telephone numbers by which individuals can speak to the responsible organization or individual.
     *
     * @since GeoAPI 2.1
     */
    @UML(identifier="voice", obligation=OPTIONAL, specification=ISO_19115)
    Collection<String> getVoices();

    /**
     * Telephone numbers of a facsimile machine for the responsible organization or individual.
     *
     * @return Telephone numbers of a facsimile machine for the responsible organization or individual.
     *
     * @since GeoAPI 2.1
     */
    @UML(identifier="facsimile", obligation=OPTIONAL, specification=ISO_19115)
    Collection<String> getFacsimiles();
}
