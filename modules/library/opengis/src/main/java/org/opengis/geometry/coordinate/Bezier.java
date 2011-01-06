/*$************************************************************************************************
 **
 ** $Id: Bezier.java 1356 2009-02-20 10:02:26Z desruisseaux $
 **
 ** $URL: https://geoapi.svn.sourceforge.net/svnroot/geoapi/tags/2.3-M2/geoapi-pending/src/main/java/org/opengis/geometry/coordinate/Bezier.java $
 **
 ** Copyright (C) 2003-2005 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/
package org.opengis.geometry.coordinate;

import org.opengis.annotation.UML;

import static org.opengis.annotation.Specification.*;


/**
 * Polynomial splines that use Bezier or Bernstein polynomials for interpolation
 * purposes. An <var>n</var>-long control point array shall create a polynomial
 * curve of degree <var>n</var> that defines the entire curve segment. These curves
 * are defined in terms of a set of basis functions called the Bézier or Bernstein
 * polynomials.
 *
 * @version <A HREF="http://www.opengeospatial.org/standards/as">ISO 19107</A>
 * @author Martin Desruisseaux (IRD)
 * @since GeoAPI 2.0
 *
 * @todo Add equations from ISO 19107 to the javadoc.
 */
@UML(identifier="GM_Bezier", specification=ISO_19107)
public interface Bezier extends BSplineCurve {
}
