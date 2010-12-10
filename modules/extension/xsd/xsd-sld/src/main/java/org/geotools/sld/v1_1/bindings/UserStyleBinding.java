package org.geotools.sld.v1_1.bindings;

import org.geotools.sld.bindings.SLDUserStyleBinding;
import org.geotools.sld.v1_1.SLD;
import org.geotools.styling.Description;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.xml.*;

import javax.xml.namespace.QName;

/**
 * Binding object for the element http://www.opengis.net/sld:UserStyle.
 * 
 * <p>
 * 
 * <pre>
 *  <code>
 *  &lt;xsd:element name="UserStyle"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;
 *          A UserStyle allows user-defined styling and is semantically
 *          equivalent to a WMS named style. External FeatureTypeStyles or
 *          CoverageStyles can be linked using an OnlineResource-element
 *        &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *      &lt;xsd:complexType&gt;
 *          &lt;xsd:sequence&gt;
 *              &lt;xsd:element minOccurs="0" ref="se:Name"/&gt;
 *              &lt;xsd:element minOccurs="0" ref="se:Description"/&gt;
 *              &lt;xsd:element minOccurs="0" ref="sld:IsDefault"/&gt;
 *              &lt;xsd:choice maxOccurs="unbounded"&gt;
 *                  &lt;xsd:element ref="se:FeatureTypeStyle"/&gt;
 *                  &lt;xsd:element ref="se:CoverageStyle"/&gt;
 *                  &lt;xsd:element ref="se:OnlineResource"/&gt;
 *              &lt;/xsd:choice&gt;
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
public class UserStyleBinding extends SLDUserStyleBinding {

    public UserStyleBinding(StyleFactory styleFactory) {
        super(styleFactory);
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     * 
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        Style style = (Style) super.parse(instance, node, value); 
        
        if (node.hasChild("Description")) {
            Description desc = (Description) node.getChildValue("Description");
            style.getDescription().setAbstract(desc.getAbstract());
            style.getDescription().setTitle(desc.getTitle());
        }
        
        //TODO: OnlineResource
        return style;
    }

}