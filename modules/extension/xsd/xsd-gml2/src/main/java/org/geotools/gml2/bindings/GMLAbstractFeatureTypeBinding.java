/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.gml2.bindings;

import javax.xml.namespace.QName;

import org.geotools.gml2.FeatureTypeCache;
import org.geotools.gml2.GML;
import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.BindingWalkerFactory;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;
import org.opengis.feature.simple.SimpleFeature;


/**
 * Binding object for the type http://www.opengis.net/gml:AbstractFeatureType.
 *
 * <p>
 *        <pre>
 *         <code>
 *  &lt;complexType name="AbstractFeatureType" abstract="true"&gt;
 *      &lt;annotation&gt;
 *          &lt;documentation&gt;         An abstract feature provides a set of
 *              common properties. A concrete          feature type must
 *              derive from this type and specify additional
 *              properties in an application schema. A feature may
 *              optionally          possess an identifying attribute
 *              (&apos;fid&apos;).       &lt;/documentation&gt;
 *      &lt;/annotation&gt;
 *      &lt;sequence&gt;
 *          &lt;element ref="gml:description" minOccurs="0"/&gt;
 *          &lt;element ref="gml:name" minOccurs="0"/&gt;
 *          &lt;element ref="gml:boundedBy" minOccurs="0"/&gt;
 *          &lt;!-- additional properties must be specified in an application schema --&gt;
 *      &lt;/sequence&gt;
 *      &lt;attribute name="fid" type="ID" use="optional"/&gt;
 *  &lt;/complexType&gt;
 *
 *          </code>
 *         </pre>
 * </p>
 *
 * @generated
 */
public class GMLAbstractFeatureTypeBinding extends AbstractComplexBinding {
    /** Cache of feature types */
    FeatureTypeCache ftCache;

    /** factory for loading bindings */
    BindingWalkerFactory bwFactory;

    public GMLAbstractFeatureTypeBinding(FeatureTypeCache ftCache, BindingWalkerFactory bwFactory) {
        this.ftCache = ftCache;
        this.bwFactory = bwFactory;
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return GML.AbstractFeatureType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return SimpleFeature.class;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value)
        throws Exception {
        return GML2ParsingUtils.parseFeature(instance, node, value, ftCache, bwFactory);
    }

    public Object getProperty(Object object, QName name)
        throws Exception {
        SimpleFeature feature = (SimpleFeature) object;

        //JD: here we only handle the "GML" attributes, all the application 
        // schema attributes are handled by FeaturePropertyExtractor
        //JD: TODO: handle all properties here and kill FeautrePropertyExtractor
        if (GML.name.equals(name)) {
            return feature.getAttribute("name");
        }

        if (GML.description.equals(name)) {
            return feature.getAttribute("description");
        }

        if (GML.location.equals(name)) {
            return feature.getAttribute("location");
        }

        if (GML.boundedBy.equals(name)) {
            return feature.getBounds();
        }

        if (feature.getFeatureType().getDescriptor(name.getLocalPart()) != null) {
            return feature.getAttribute(name.getLocalPart());
        }

        return null;
    }
}
