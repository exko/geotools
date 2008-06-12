/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 1999-2008, Open Source Geospatial Foundation (OSGeo)
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
 */
package org.geotools.axis;

import java.awt.RenderingHints;


/**
 * Rendering hints for tick's graduation.
 *
 * @since 2.0
 * @source $URL$
 * @version $Id$
 * @author Martin Desruisseaux (PMO, IRD)
 */
final class RenderingHintKey extends RenderingHints.Key {
    /**
     * The required base class.
     */
    private final Class<?> type;

    /**
     * Construct a rendering hint key.
     */
    protected RenderingHintKey(final Class<?> type, final int key) {
        super(key);
        this.type = type;
    }

    /**
     * Returns {@code true} if the specified object is a valid value for this key.
     */
    public boolean isCompatibleValue(final Object value) {
        return value!=null && type.isAssignableFrom(value.getClass());
    }
}
