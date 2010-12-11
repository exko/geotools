package org.geotools.se.v1_1.bindings;

import org.geotools.se.v1_1.SE;
import org.geotools.sld.bindings.SLDParameterValueTypeBinding;
import org.geotools.xml.*;
import org.opengis.filter.FilterFactory;

import javax.xml.namespace.QName;

/**
 * Binding object for the type http://www.opengis.net/se:ParameterValueType.
 * 
 * <p>
 * 
 * <pre>
 *  <code>
 *  &lt;xsd:complexType mixed="true" name="ParameterValueType"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;
 *          The "ParameterValueType" uses WFS-Filter expressions to give
 *          values for SE graphic parameters.  A "mixed" element-content
 *          model is used with textual substitution for values.
 *        &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *      &lt;xsd:sequence maxOccurs="unbounded" minOccurs="0"&gt;
 *          &lt;xsd:element ref="ogc:expression"/&gt;
 *      &lt;/xsd:sequence&gt;
 *  &lt;/xsd:complexType&gt; 
 * 	
 *   </code>
 * </pre>
 * 
 * </p>
 * 
 * @generated
 */
public class ParameterValueTypeBinding extends SLDParameterValueTypeBinding {

    public ParameterValueTypeBinding(FilterFactory filterFactory) {
        super(filterFactory);
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return SE.ParameterValueType;
    }
}