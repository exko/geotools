/*
 *    Geotools2 - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2002, Geotools Project Managment Committee (PMC)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 */
/*
 * Created on Jan 24, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.geotools.validation.spatial;

import com.vividsolutions.jts.geom.Envelope;
import org.geotools.validation.ValidationResults;
import java.util.Map;


/**
 * PolygonNotOverlappingLineValidation purpose.
 * 
 * <p>
 * Checks that the polygon is not overlapping the line.
 * </p>
 *
 * @author dzwiers, Refractions Research, Inc.
 * @author $Author: dmzwiers $ (last modification)
 * @version $Id: PolygonNotOverlappingLineValidation.java,v 1.2 2004/02/17 17:19:13 dmzwiers Exp $
 */
public class PolygonNotOverlappingLineValidation
    extends PolygonLineAbstractValidation {
    /**
     * PolygonNotOverlappingLineValidation constructor.
     * 
     * <p>
     * Description
     * </p>
     */
    public PolygonNotOverlappingLineValidation() {
        super();
    }

    /**
     * Check that the polygon is not overlapping the line.
     *
     * @param layers Map of FeatureSource by "dataStoreID:typeName"
     * @param envelope The bounding box that encloses the unvalidated data
     * @param results Used to coallate results information
     *
     * @return <code>true</code> if all the features pass this test.
     *
     * @throws Exception DOCUMENT ME!
     */
    public boolean validate(Map layers, Envelope envelope,
        ValidationResults results) throws Exception {
        results.warning(null, "Validation not yet implemented");

        return false;
    }
}
