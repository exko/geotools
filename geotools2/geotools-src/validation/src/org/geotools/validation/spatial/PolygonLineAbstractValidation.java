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

import org.geotools.validation.DefaultIntegrityValidation;


/**
 * PointCoveredByLineValidation purpose.
 * 
 * <p>
 * Basic typeref functionality for a line-polygon validation.
 * </p>
 *
 * @author dzwiers, Refractions Research, Inc.
 * @author $Author: dmzwiers $ (last modification)
 * @version $Id: PolygonLineAbstractValidation.java,v 1.2 2004/02/17 17:19:13 dmzwiers Exp $
 */
public abstract class PolygonLineAbstractValidation
    extends DefaultIntegrityValidation {
    private String restrictedLineTypeRef;
    private String polygonTypeRef;

    /**
     * PointCoveredByLineValidation constructor.
     * 
     * <p>
     * Super
     * </p>
     */
    public PolygonLineAbstractValidation() {
        super();
    }

    /**
     * Implementation of getTypeNames. Should be called by sub-classes is being
     * overwritten.
     *
     * @return Array of typeNames, or empty array for all, null for disabled
     *
     * @see org.geotools.validation.Validation#getTypeNames()
     */
    public String[] getTypeRefs() {
        if ((polygonTypeRef == null) || (restrictedLineTypeRef == null)) {
            return null;
        }

        return new String[] { polygonTypeRef, restrictedLineTypeRef };
    }

    /**
     * Access polygonTypeRef property.
     *
     * @return Returns the polygonTypeRef.
     */
    public final String getRestrictedLineTypeRef() {
        return restrictedLineTypeRef;
    }

    /**
     * Set polygonTypeRef to polygonTypeRef.
     *
     * @param lineTypeRef The polygonTypeRef to set.
     */
    public final void setRestrictedLineTypeRef(String lineTypeRef) {
        this.restrictedLineTypeRef = lineTypeRef;
    }

    /**
     * Access restrictedLineTypeRef property.
     *
     * @return Returns the restrictedLineTypeRef.
     */
    public final String getPolygonTypeRef() {
        return polygonTypeRef;
    }

    /**
     * Set restrictedLineTypeRef to restrictedLineTypeRef.
     *
     * @param polygonTypeRef The restrictedLineTypeRef to set.
     */
    public final void setPolygonTypeRef(String polygonTypeRef) {
        this.polygonTypeRef = polygonTypeRef;
    }
}
