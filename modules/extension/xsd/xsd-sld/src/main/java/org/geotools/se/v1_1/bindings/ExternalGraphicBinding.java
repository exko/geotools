package org.geotools.se.v1_1.bindings;

import org.geotools.se.v1_1.SE;
import org.geotools.sld.bindings.SLDExternalGraphicBinding;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.StyleFactory;
import org.geotools.xml.*;

import javax.xml.namespace.QName;

/**
 * Binding object for the element http://www.opengis.net/se:ExternalGraphic.
 * 
 * <p>
 * 
 * <pre>
 *  <code>
 *  &lt;xsd:element name="ExternalGraphic" type="se:ExternalGraphicType"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;
 *          An "ExternalGraphic" gives a reference to a raster or vector
 *          graphical object, either online or inline, in an externally-defined
 *          graphic format.
 *        &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *  &lt;/xsd:element&gt; 
 * 	
 *   </code>
 * </pre>
 * 
 * <pre>
 *       <code>
 *  &lt;xsd:complexType name="ExternalGraphicType"&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:choice&gt;
 *              &lt;xsd:element ref="se:OnlineResource"/&gt;
 *              &lt;xsd:element ref="se:InlineContent"/&gt;
 *          &lt;/xsd:choice&gt;
 *          &lt;xsd:element ref="se:Format"/&gt;
 *          &lt;xsd:element maxOccurs="unbounded" minOccurs="0" ref="se:ColorReplacement"/&gt;
 *      &lt;/xsd:sequence&gt;
 *  &lt;/xsd:complexType&gt; 
 *              
 *        </code>
 * </pre>
 * 
 * </p>
 * 
 * @generated
 */
public class ExternalGraphicBinding extends SLDExternalGraphicBinding {

    public ExternalGraphicBinding(StyleFactory styleFactory) {
        super(styleFactory);
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return SE.ExternalGraphic;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        ExternalGraphic g = (ExternalGraphic) super.parse(instance, node, value);
        
        if (node.hasChild("InlineContent")) {
            //TODO: implement
        }
        
        return g;
    }

}