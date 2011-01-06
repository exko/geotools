/*$************************************************************************************************
 **
 ** $Id: InvalidParameterCardinalityException.java 1421 2009-06-04 20:18:07Z desruisseaux $
 **
 ** $URL: https://geoapi.svn.sourceforge.net/svnroot/geoapi/tags/2.3-M2/geoapi/src/main/java/org/opengis/parameter/InvalidParameterCardinalityException.java $
 **
 ** Copyright (C) 2003-2005 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/
package org.opengis.parameter;

import java.util.List;
import org.opengis.annotation.Extension;


/**
 * Throws if adding a {@linkplain ParameterValue parameter value} to a
 * {@linkplain ParameterValueGroup group} would result in more parameters
 * than the {@linkplain ParameterDescriptor#getMaximumOccurs maximum occurence}
 * allowed. This operation may be throws during {@linkplain List#add} or
 * {@linkplain List#remove} operation on the list returned by
 * {@link ParameterValueGroup#values}.
 * <p>
 * <b>Note:</b> This exception is of kind "{@linkplain IllegalStateException illegal state}"
 * rather than "{@linkplain IllegalArgumentException illegal argument}" because it is not
 * caused by a bad argument; it is rather a consequence of an {@linkplain ParameterValueGroup
 * parameter value group} being "full".
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   GeoAPI 2.0
 *
 * @see ParameterValueGroup#values
 * @see ParameterDescriptor#getMinimumOccurs
 * @see ParameterDescriptor#getMaximumOccurs
 */
@Extension
public class InvalidParameterCardinalityException extends IllegalStateException {
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 4030549323541812311L;

    /**
     * The name of the parameter with invalid cardinality.
     */
    private final String parameterName;

    /**
     * Creates an exception with the specified message and parameter name.
     *
     * @param message The detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     * @param parameterName The name of the parameter with invalid cardinality.
     */
    public InvalidParameterCardinalityException(String message, String parameterName) {
        super(message);
        this.parameterName = parameterName;
    }

    /**
     * Returns the name of the parameter with invalid cardinality.
     *
     * @return The name of the parameter with invalid cardinality.
     */
    public String getParameterName() {
        return parameterName;
    }
}
