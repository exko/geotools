/*$************************************************************************************************
 **
 ** $Id: Series.java 1294 2008-08-05 08:41:11Z desruisseaux $
 **
 ** $URL: https://geoapi.svn.sourceforge.net/svnroot/geoapi/tags/2.3-M2/geoapi/src/main/java/org/opengis/metadata/citation/Series.java $
 **
 ** Copyright (C) 2004-2005 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/
package org.opengis.metadata.citation;

import org.opengis.util.InternationalString;
import org.opengis.annotation.UML;

import static org.opengis.annotation.Obligation.*;
import static org.opengis.annotation.Specification.*;


/**
 * Information about the series, or aggregate dataset, to which a dataset belongs.
 *
 * @version <A HREF="http://www.opengeospatial.org/standards/as#01-111">ISO 19115</A>
 * @author  Martin Desruisseaux (IRD)
 * @since   GeoAPI 1.0
 */
@UML(identifier="CI_Series", specification=ISO_19115)
public interface Series {
    /**
     * Name of the series, or aggregate dataset, of which the dataset is a part.
     * Returns {@code null} if none.
     *
     * @return The name of the series or aggregate dataset.
     */
    @UML(identifier="name", obligation=OPTIONAL, specification=ISO_19115)
    InternationalString getName();

    /**
     * Information identifying the issue of the series.
     *
     * @return Information identifying the issue of the series.
     */
    @UML(identifier="issueIdentification", obligation=OPTIONAL, specification=ISO_19115)
    String getIssueIdentification();

    /**
     * Details on which pages of the publication the article was published.
     *
     * @return Details on which pages of the publication the article was published.
     */
    @UML(identifier="page", obligation=OPTIONAL, specification=ISO_19115)
    String getPage();
}
