/*$************************************************************************************************
 **
 ** $Id: BilinearGrid.java 1356 2009-02-20 10:02:26Z desruisseaux $
 **
 ** $URL: https://geoapi.svn.sourceforge.net/svnroot/geoapi/tags/2.3-M2/geoapi-pending/src/main/java/org/opengis/geometry/coordinate/BilinearGrid.java $
 **
 ** Copyright (C) 2003-2005 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/
package org.opengis.geometry.coordinate;

import org.opengis.annotation.UML;

import static org.opengis.annotation.Specification.*;


/**
 * A {@linkplain GriddedSurface gridded surface} that uses line strings as the horizontal and
 * vertical curves. This is not a polygonal surface, since each of the grid squares is a ruled
 * surface, and not necessarily planar.
 *
 * @version <A HREF="http://www.opengeospatial.org/standards/as">ISO 19107</A>
 * @author Martin Desruisseaux (IRD)
 * @since GeoAPI 2.0
 */
@UML(identifier="GM_BilinearGrid", specification=ISO_19107)
public interface BilinearGrid extends GriddedSurface {
}
