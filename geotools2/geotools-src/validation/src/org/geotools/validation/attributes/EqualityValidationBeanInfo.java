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
/*
 * Created on Jan 22, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.geotools.validation.attributes;

import org.geotools.validation.DefaultFeatureValidationBeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.ResourceBundle;


/**
 * GazetteerNameValidationBeanInfo purpose.
 * 
 * <p>
 * Description of GazetteerNameValidationBeanInfo ...
 * </p>
 *
 * @author dzwiers, Refractions Research, Inc.
 * @author $Author: dmzwiers $ (last modification)
 * @version $Id: EqualityValidationBeanInfo.java,v 1.2 2004/02/17 17:19:15 dmzwiers Exp $
 */
public class EqualityValidationBeanInfo extends DefaultFeatureValidationBeanInfo {
    /**
     * GazetteerNameValidationBeanInfo constructor.
     * 
     * <p>
     * Description
     * </p>
     */
    public EqualityValidationBeanInfo() {
        super();
    }

    /**
     * Implementation of getPropertyDescriptors.
     *
     * @return
     *
     * @see java.beans.BeanInfo#getPropertyDescriptors()
     */
    public PropertyDescriptor[] getPropertyDescriptors() {
        PropertyDescriptor[] pd2 = super.getPropertyDescriptors();
        ResourceBundle resourceBundle = getResourceBundle(EqualityValidation.class);

        if (pd2 == null) {
            pd2 = new PropertyDescriptor[0];
        }

        PropertyDescriptor[] pd = new PropertyDescriptor[pd2.length + 3];
        int i = 0;

        for (; i < pd2.length; i++)
            pd[i] = pd2[i];

        try {
            pd[i] = createPropertyDescriptor("attributeName",
                    EqualityValidation.class, resourceBundle);
            pd[i].setExpert(false);
            pd[i + 1] = createPropertyDescriptor("expected",
                    EqualityValidation.class, resourceBundle);
            pd[i + 1].setExpert(true);
            pd[i + 2] = createPropertyDescriptor("filter",
                    EqualityValidation.class, resourceBundle);
            pd[i + 2].setExpert(true);
        } catch (IntrospectionException e) {
            pd = pd2;

            // TODO error, log here
            e.printStackTrace();
        }

        return pd;
    }
}
