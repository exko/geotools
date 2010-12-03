package org.geotools.se.v1_1.bindings;

import org.geotools.se.v1_1.SE;
import org.geotools.sld.bindings.SLDGraphicBinding;
import org.geotools.styling.AnchorPoint;
import org.geotools.styling.Displacement;
import org.geotools.styling.Graphic;
import org.geotools.styling.StyleFactory;
import org.geotools.xml.*;

import javax.xml.namespace.QName;

/**
 * Binding object for the element http://www.opengis.net/se:Graphic.
 * 
 * <p>
 * 
 * <pre>
 *  <code>
 *  &lt;xsd:element name="Graphic" type="se:GraphicType"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;
 *          A "Graphic" specifies or refers to a "graphic Symbolizer" with inherent
 *          shape, size, and coloring.
 *        &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *  &lt;/xsd:element&gt; 
 * 	
 *   </code>
 * </pre>
 * 
 * </p>
 * 
 * @generated
 */
public class GraphicBinding extends SLDGraphicBinding {

    public GraphicBinding(StyleFactory styleFactory) {
        super(styleFactory);
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return SE.Graphic;
    }

    @Override
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        Graphic g = (Graphic) super.parse(instance, node, value);
        if (node.hasChild(AnchorPoint.class)) {
            g.setAnchorPoint((AnchorPoint)node.getChildValue(AnchorPoint.class));
        }
        if (node.hasChild(Displacement.class)) {
            g.setDisplacement((Displacement)node.getChildValue(Displacement.class));
        }
        return g;
    }
}