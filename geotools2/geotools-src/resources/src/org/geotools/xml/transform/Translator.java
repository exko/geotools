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
package org.geotools.xml.transform;

import org.xml.sax.helpers.NamespaceSupport;
import org.geotools.xml.transform.TransformerBase.SchemaLocationSupport;

/**
 * A Translator is used in an XMLEncoding process by the FeatureTransformer 
 * class.
 * @author  Ian Schneider
 */
public interface Translator {
    
    /**
     * Obtain the namespace prefixes and URIs to be included in the output
     * document.
     * @return An instance of NamespaceSupport.
     */
    NamespaceSupport getNamespaceSupport();
    
    /**
     * Get the default URI used by this Translator for encoding. Optional.
     */
    String getDefaultNamespace();
    
    /**
     * Get the default prefix used by this Translator for encoding. Optional.
     */
    String getDefaultPrefix();
    
    /**
     * Encode the object.
     * @param o The Object to encode.
     * @throws IllegalArgumentException if the Object is not encodeable.
     */
    void encode(Object o) throws IllegalArgumentException;

    /**
     * Gets the location of the schemas used in this translator.  Optional.
     */
    SchemaLocationSupport getSchemaLocationSupport();
    
}
