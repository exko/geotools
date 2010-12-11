package org.geotools.sld.v1_1.bindings;

import org.geotools.data.DataStore;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.sld.bindings.SLDUserLayerBinding;
import org.geotools.sld.v1_1.SLD;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.UserLayer;
import org.geotools.xml.*;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;

import javax.xml.namespace.QName;

/**
 * Binding object for the element http://www.opengis.net/sld:UserLayer.
 * 
 * <p>
 * 
 * <pre>
 *  <code>
 *  &lt;xsd:element name="UserLayer"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;
 *          A UserLayer allows a user-defined layer to be built from WFS and
 *          WCS data.
 *        &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *      &lt;xsd:complexType&gt;
 *          &lt;xsd:sequence&gt;
 *              &lt;xsd:element minOccurs="0" ref="se:Name"/&gt;
 *              &lt;xsd:element minOccurs="0" ref="se:Description"/&gt;
 *              &lt;xsd:choice minOccurs="0"&gt;
 *                  &lt;xsd:element ref="sld:RemoteOWS"/&gt;
 *                  &lt;xsd:element ref="sld:InlineFeature"/&gt;
 *              &lt;/xsd:choice&gt;
 *              &lt;xsd:choice minOccurs="0"&gt;
 *                  &lt;xsd:element ref="sld:LayerFeatureConstraints"/&gt;
 *                  &lt;xsd:element ref="sld:LayerCoverageConstraints"/&gt;
 *              &lt;/xsd:choice&gt;
 *              &lt;xsd:element maxOccurs="unbounded" ref="sld:UserStyle"/&gt;
 *          &lt;/xsd:sequence&gt;
 *      &lt;/xsd:complexType&gt;
 *  &lt;/xsd:element&gt; 
 * 	
 *   </code>
 * </pre>
 * 
 * </p>
 * 
 * @generated
 */
public class UserLayerBinding extends SLDUserLayerBinding {

    public UserLayerBinding(StyleFactory styleFactory) {
        super(styleFactory);
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        UserLayer layer  = (UserLayer) super.parse(instance, node, value);
        
        //TODO: description
        
        if (node.hasChild("InlineFeature")) {
            SimpleFeatureCollection features = (SimpleFeatureCollection) node.getChildValue("InlineFeature");
            SimpleFeatureType type = features.getSchema();
            
            layer.setInlineFeatureType(type);
            layer.setInlineFeatureDatastore(toDataStore(features));
        }

        //TODO:LayerCoverageConstraints
        return layer;
    }

    DataStore toDataStore(FeatureCollection features) {
        return new MemoryDataStore(features);
    }

}