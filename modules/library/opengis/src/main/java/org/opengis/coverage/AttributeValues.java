/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2011, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2005 Open Geospatial Consortium Inc.
 *    
 *    All Rights Reserved. http://www.opengis.org/legal/
 */
package org.opengis.coverage;

import org.opengis.util.Record;
import org.opengis.annotation.UML;

import static org.opengis.annotation.Obligation.*;
import static org.opengis.annotation.Specification.*;


/**
 * Represents an element from the range of the {@linkplain Coverage coverage}.
 *
 * @version ISO 19123:2004
 * @author  Martin Desruisseaux (IRD)
 * @since   GeoAPI 2.1
 *
 * @see Coverage#getRangeElements
 */
@UML(identifier="CV_AttributeValues", specification=ISO_19123)
public interface AttributeValues {
    /**
     * Returns a record containing one value for each attribute, as specified in the
     * {@linkplain Coverage#getRangeType coverage's range type}.
     * <p>
     * <B>Examples:</B>
     * <ul>
     *   <li>A coverage with a single (scalar) value (such as elevation).</li>
     *   <li>A coverage with a series (array / tensor) of values all defined in the same way
     *       (such as brightness values in different parts of the electromagnetic spectrum).</li>
     * </ul>
     *
     * @return The value for each attribute.
     */
    @UML(identifier="values", obligation=MANDATORY, specification=ISO_19123)
    Record getValues();
}
