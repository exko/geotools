/*$************************************************************************************************
 **
 ** $Id: NoSuchIdentifierException.java 1421 2009-06-04 20:18:07Z desruisseaux $
 **
 ** $URL: https://geoapi.svn.sourceforge.net/svnroot/geoapi/tags/2.3-M2/geoapi/src/main/java/org/opengis/referencing/NoSuchIdentifierException.java $
 **
 ** Copyright (C) 2003-2005 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/
package org.opengis.referencing;

import org.opengis.annotation.Extension;
import org.opengis.metadata.Identifier;


/**
 * Thrown when a {@linkplain org.opengis.referencing.operation.MathTransform math transform}
 * as been requested with an unknow {@linkplain org.opengis.referencing.operation.OperationMethod
 * operation method} identifier.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   GeoAPI 1.0
 *
 * @see org.opengis.referencing.operation.MathTransformFactory#createParameterizedTransform
 */
@Extension
public class NoSuchIdentifierException extends FactoryException {
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -6846799994429345902L;

    /**
     * The {@linkplain Identifier#getCode identifier code}.
     */
    private final String identifier;

    /**
     * Constructs an exception with the specified detail message and classification name.
     *
     * @param  message The detail message. The detail message is saved
     *         for later retrieval by the {@link #getMessage()} method.
     * @param identifier {@linkplain ReferenceIdentifier#getCode identifier code}.
     */
    public NoSuchIdentifierException(final String message, final String identifier) {
        super(message);
        this.identifier = identifier;
    }

    /**
     * Returns the {@linkplain ReferenceIdentifier#getCode identifier code}.
     *
     * @return The identifier code.
     */
    public String getIdentifierCode() {
        return identifier;
    }
}
