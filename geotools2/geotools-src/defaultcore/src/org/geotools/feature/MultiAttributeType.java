/*
 *    Geotools - OpenSource mapping toolkit
 *    (C) 2002, Centre for Computational Geography
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
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.geotools.feature;

import java.util.Iterator;
import java.util.List;


/**
 * Class to handle more than one occurance of an attribute.  There may  be
 * better ways to do this, but this seems to work.
 *
 * @author Chris Holmes
 * @version $Id: MultiAttributeType.java,v 1.3 2003/07/22 19:18:32 cholmesny Exp $
 */
public class MultiAttributeType extends DefaultAttributeType {
    /** Number of instances of this attribute in the schema. */
    private int maxOccur = 1;

    /** The AttributeType to check each object of the list against. */
    private AttributeType validator;

    /** The minimum number of occurances of this attribute to validate. */
    private int minOccur = 1;

    /**
     * Constructor with validator.
     *
     * @param validator Name of this attribute.
     */
    public MultiAttributeType(AttributeType validator) {
        super(validator.getName(), List.class, false);
        this.validator = validator;
    }

    /**
     * Constructor with validator and maxOccurs
     *
     * @param validator Name of this attribute.
     * @param maxOccur Number of instances of this attribute in the schema.
     */
    public MultiAttributeType(AttributeType validator, int maxOccur) {
        this(validator);
        this.maxOccur = maxOccur;
    }

    /**
     * Constructor with validator, minOccurs and maxOccurs
     *
     * @param validator Name of this attribute.
     * @param maxOccur Number of instances of this attribute in the schema.
     * @param minOccur Class type of this attribute.
     */
    public MultiAttributeType(AttributeType validator, int maxOccur,
        int minOccur) {
        this(validator, maxOccur);
        this.minOccur = minOccur;
    }

    /**
     * Gets the maxOccur of this attribute.
     *
     * @return MaxOccur.
     */
    public int getMaxOccurs() {
        return maxOccur;
    }

    /**
     * Gets the minimum number of elements that pass the validator that must be
     * in the list to validate.
     *
     * @return MaxOccur.
     */
    public int getMinOccurs() {
        return minOccur;
    }

    /**
     * Returns whether the attribute is a geometry. Should this be false?  Even
     * if the attributes are geometries? Because this itself isn't actually a
     * geometry, so it can't be used as a geometry.
     *
     * @return true if the attribute's type is a geometry.
     */
    public boolean isGeometry() {
        return false;
    }

    /**
     * Returns a clone of this object.
     *
     * @return a copy of this attribute type.
     *
     * @throws CloneNotSupportedException if clone is not supported.
     */
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Whether the tested object is a Feature and its attributes validate
     * against the featureType.   An IllegalArgumentException reporting the
     * error in validation is thrown if validation fails..
     *
     * @param attribute The object to be tested for validity.
     *
     * @throws IllegalArgumentException if the object does not validate.
     */
    public void validate(Object attribute) throws IllegalArgumentException {
        super.validate(attribute);

        if (attribute instanceof List) {
            int occurs = ((List) attribute).size();

            if (occurs < minOccur) {
                String mesg = "The list of attributes is " + occurs + " long."
                    + "  It must not be less than minOccurs: " + minOccur;
                throw new IllegalArgumentException(mesg);
            }

            if (occurs > maxOccur) {
                String mesg = "The list of attributes is " + occurs + " long."
                    + "  It must not be greater than maxOccurs: " + maxOccur;
                throw new IllegalArgumentException(mesg);
            }

            for (Iterator iter = ((List) attribute).iterator(); 
                 iter.hasNext();) {
                validator.validate(iter.next());
            }
        } else {
            //REVISIT: allow just one if it's not in a list?
            String msg = attribute.getClass().getName()
                + " is not an accetable"
                + " class for a multiAttributeType.  Must be of type List";

            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * If a single object is passed in then it is parsed into a list with just
     * it as the element.  If an array is passed in then it is turned into a
     * list.
     *
     * @param value the object to attempt parsing of.
     *
     * @return <code>value</code> converted to the preferred storage of this
     *         <code>AttributeType</code>.  If no parsing was possible then
     *         the same object is returned.
     *
     * @throws IllegalArgumentException if parsing is attempted and is
     *         unsuccessful.
     *
     * @task REVISIT: implement this method as described in this comment.
     */
    public Object parse(Object value) throws IllegalArgumentException {
        return value;
    }

    /**
     * Gets a representation of this object as a string.
     *
     * @return A representation of this object as a string
     */
    public String toString() {
        StringBuffer returnString = new StringBuffer("MultiAttributeType [ ");

        returnString.append("name=").append(name).append(',');
        returnString.append("type=").append(type.getName()).append(',');
        returnString.append("maxOccurs=").append(maxOccur).append(',');
        returnString.append("minOccur=").append(minOccur).append(" ]");

        return returnString.toString();
    }
}
