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
package org.geotools.styling;

import org.geotools.filter.Expression;


/**
 * @version $Id: MarkImpl.java,v 1.8 2003/07/22 15:52:10 ianturton Exp $
 * @author Ian Turton, CCG
 */
public class MarkImpl implements Mark {
    /**
     * The logger for the default core module.
     */
    private static final java.util.logging.Logger LOGGER = 
            java.util.logging.Logger.getLogger("org.geotools.styling");
    private static final org.geotools.filter.FilterFactory filterFactory = 
            org.geotools.filter.FilterFactory.createFilterFactory();
    Fill fill;
    Stroke stroke;

    //Polygon shape;
    private Expression wellKnownName = null;
    private Expression rotation = null;
    private Expression size = null;
    

    /** Creates a new instance of DefaultMark */
    protected MarkImpl() {
        LOGGER.fine("creating defaultMark");

        try {
            StyleFactory sfac = new StyleFactoryImpl();
            fill = sfac.getDefaultFill();
            stroke = sfac.getDefaultStroke();

            wellKnownName = filterFactory.createLiteralExpression("square");
            size = filterFactory.createLiteralExpression(new Integer(6));
            rotation = filterFactory.createLiteralExpression(new Double(0.0));
        } catch (org.geotools.filter.IllegalFilterException ife) {
            severe("<init>", "Failed to build default mark: ", ife);
        }
    }

    public MarkImpl(String name) {
        this();
        LOGGER.fine("creating " + name + " type mark");
        setWellKnownName(name);
    }

    /**
     * Convenience method for logging a message with an exception.
     */
    private static void severe(final String method, final String message, 
                               final Exception exception) {
        final java.util.logging.LogRecord record = new java.util.logging.LogRecord(
                                                           java.util.logging.Level.SEVERE, 
                                                           message);
        record.setSourceMethodName(method);
        record.setThrown(exception);
        LOGGER.log(record);
    }

    /**
     * This parameter defines which fill style to use when rendering the Mark.
     *
     * @return the Fill definition to use when rendering the Mark.
     */
    public Fill getFill() {
        return fill;
    }

    /**
     * This paramterer defines which stroke style should be used when
     * rendering the Mark.
     *
     * @return The Stroke definition to use when rendering the Mark.
     */
    public Stroke getStroke() {
        return stroke;
    }

    /**
     * This parameter gives the well-known name of the shape of the mark.<br>
     * Allowed names include at least "square", "circle", "triangle", "star",
     * "cross" and "x" though renderers may draw a different symbol instead
     * if they don't have a shape for all of these.<br>
     *
     * @return The well-known name of a shape.  The default value is "square".
     */
    public Expression getWellKnownName() {
        return wellKnownName;
    }

    /**
     * Setter for property fill.
     * @param fill New value of property fill.
     */
    public void setFill(org.geotools.styling.Fill fill) {
        this.fill = fill;
    }

    /**
     * Setter for property stroke.
     * @param stroke New value of property stroke.
     */
    public void setStroke(org.geotools.styling.Stroke stroke) {
        this.stroke = stroke;
    }

    public void setSize(Expression size) {
        this.size = size;
    }

    public void setSize(int size) {
        try {
            setSize(filterFactory.createLiteralExpression(new Integer(size)));
        } catch (org.geotools.filter.IllegalFilterException mfe) {
            severe("setSize", "Problem setting Opacity", mfe);
        }
    }

    /**
     * Setter for property wellKnownName.
     * @param wellKnownName New value of property wellKnownName.
     */
    public void setWellKnownName(Expression wellKnownName) {
        LOGGER.entering("DefaultMark", "setWellKnownName");
        this.wellKnownName = wellKnownName;
    }

    public void setWellKnownName(String name) {
        setWellKnownName(filterFactory.createLiteralExpression(name));
    }

    public void setRotation(Expression rotation) {
        this.rotation = rotation;
    }

    public void setRotation(double rotation) {
        try {
            setRotation(filterFactory.createLiteralExpression(
                                new Double(rotation)));
        } catch (org.geotools.filter.IllegalFilterException mfe) {
            severe("setRotation", "Problem setting Rotation", mfe);
        }
    }

    /**
     * Getter for property size.
     * @return Value of property size.
     */
    public Expression getSize() {
        return size;
    }

    /**
     * Getter for property rotation.
     * @return Value of property rotation.
     */
    public Expression getRotation() {
        return rotation;
    }

    public String toString() {
        return wellKnownName.toString();
    }
}