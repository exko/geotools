/*
 * Geotools - OpenSource mapping toolkit
 * (C) 2000, Institut de Recherche pour le D�veloppement
 * (C) 1999, P�ches et Oc�ans Canada
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
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
 *
 * Contacts:
 *     UNITED KINGDOM: James Macgill
 *             mailto:j.macgill@geog.leeds.ac.uk
 *
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement / US-Espace
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 */
package org.geotools.axis;

// Graphics and geometry
import java.awt.Font;
import java.awt.Shape;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.IllegalPathStateException;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.RenderingHints;

// Events
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

// Miscellaneous
import java.util.Map;
import java.util.Locale;
import java.io.Serializable;
import java.lang.UnsupportedOperationException;
import java.util.ConcurrentModificationException;

// Geotools dependencies
import org.geotools.cs.AxisInfo;
import org.geotools.cs.AxisOrientation;

// Resources
import org.geotools.resources.XMath;
import org.geotools.resources.Utilities;
import org.geotools.resources.XDimension2D;
import org.geotools.resources.XAffineTransform;


/**
 * An axis as a graduated line. <code>Axis2D</code> objets are really {@link Line2D}
 * objects with a {@link Graduation}. Because axis are {@link Line2D}, they can be
 * located anywhere in a widget with any orientation. Lines are drawn from starting
 * point
 * ({@linkplain #getX1 <var>x<sub>1</sub></var>},{@linkplain #getY1 <var>y<sub>1</sub></var>})
 * to end point
 * ({@linkplain #getX2 <var>x<sub>2</sub></var>},{@linkplain #getY2 <var>y<sub>2</sub></var>}),
 * using a graduation from minimal value {@link Graduation#getMinimum} to maximal
 * value {@link Graduation#getMaximum}.
 *
 * Note the line's coordinates (<var>x<sub>1</sub></var>,<var>y<sub>1</sub></var>) and
 * (<var>x<sub>2</sub></var>,<var>y<sub>2</sub></var>) are completly independant of
 * graduation minimal and maximal values. Line's coordinates should be expressed in
 * some units convenient for rendering, as pixels or point (1/72 of inch). On the
 * opposite, graduation can have any arbitrary units, which is given by
 * {@link Graduation#getUnit}. The static method {@link #createAffineTransform} can
 * be used for mapping logical coordinates to pixels coordinates for an arbitrary
 * pair of <code>Axis2D</code> objects, which doesn't need to be perpendicular.
 *
 * @version $Id: Axis2D.java,v 1.1 2003/03/07 23:36:09 desruisseaux Exp $
 * @author Martin Desruisseaux
 *
 * @see AxisInfo
 * @see AxisOrientation
 * @see Graduation
 */
public class Axis2D extends Line2D implements Cloneable, Serializable {
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -8396436909942389360L;

    /**
     * Coordonn�es des premier et dernier points de l'axe. Ces coordonn�es
     * sont exprim�es en "points" (1/72 de pouce), ce qui n'a rien � voir
     * avec les unit�s de {@link Graduation#getMinimum} et {@link Graduation#getMaximum}.
     */
    private float x1=8, y1=8, x2=648, y2=8;

    /**
     * Longueur des graduations, en points. Chaque graduations sera trac�e � partir de
     * <code>[sub]TickStart</code> (g�n�ralement 0) jusqu'� <code>[sub]TickEnd</code>.
     * Par convention, des valeurs positives d�signent l'int�rieur du graphique et des
     * valeurs n�gatives l'ext�rieur.
     */
    private float tickStart=0, tickEnd=9, subTickStart=0, subTickEnd=5;

    /**
     * Indique dans quelle direction se trouve la graduation de l'axe. La valeur -1 indique
     * qu'il faudrait tourner l'axe dans le sens des aiguilles d'une montre pour qu'il soit
     * par-dessus sa graduation. La valeur +1 indique au contraire qu'il faudrait le tourner
     * dans le sens inverse des aiguilles d'une montre pour le m�me effet.
     */
    private byte relativeCCW = +1;

    /**
     * Mod�le qui contient les minimum, maximum et la graduation de l'axe.
     */
    private final Graduation graduation;

    /**
     * Compte le nombre de modifications apport�es � l'axe,
     * afin de d�tecter les changements faits pendant qu'un
     * it�rateur balaye la graduation.
     */
    private transient int modCount;

    /**
     * Indique si {@link #getPathIterator} doit retourner {@link #iterator}.
     * Ce champ prend temporairement la valeur de <code>true</code> pendant
     * l'ex�cution de {@link #paint}.
     */
    private transient boolean isPainting;

    /**
     * It�rateur utilis� pour dessiner l'axe lors du dernier appel de
     * la m�thode {@link #paint}. Cet it�rateur sera r�utilis� autant
     * que possible afin de diminuer le nombre d'objets cr��s lors de
     * chaque tra�age.
     */
    private transient TickPathIterator iterator;

    /**
     * Coordonn�es de la bo�te englobant l'axe (<u>sans</u> ses �tiquettes
     * de graduation) lors du dernier tra�age par la m�thode {@link #paint}.
     * Ces coordonn�es sont ind�pendantes de {@link #lastContext} et ont �t�
     * obtenues sans transformation affine "utilisateur".
     */
    private transient Rectangle2D axisBounds;

    /**
     * Coordonn�es de la bo�te englobant les �tiquettes de graduations (<u>sans</u>
     * le reste de l'axe) lors du dernier tra�age par la m�thode {@link #paint}. Ces
     * coordonn�es ont �t� calcul�es en utilisant {@link #lastContext} mais ont �t�
     * obtenues sans transformation affine "utilisateur".
     */
    private transient Rectangle2D labelBounds;

    /**
     * Coordonn�es de la bo�te englobant la l�gende de l'axe lors du dernier tra�age
     * par la m�thode {@link #paint}. Ces coordonn�es ont �t� calcul�es en utilisant
     * {@link #lastContext} mais ont �t� obtenues sans transformation affine "utilisateur".
     */
    private transient Rectangle2D legendBounds;

    /**
     * Dernier objet {@link FontRenderContext} a avoir �t�
     * utilis� lors du tra�age par la m�thode {@link #paint}.
     */
    private transient FontRenderContext lastContext;

    /**
     * Largeur et hauteur maximales des �tiquettes de la graduation, ou
     * <code>null</code> si cette dimension n'a pas encore �t� d�termin�e.
     */
    private transient Dimension2D maximumSize;

    /**
     * A default font to use when no rendering hint were provided for the
     * {@link Graduation#TICK_LABEL_FONT} key. Cached here only for performance.
     */
    private transient Font defaultFont;

    /**
     * A set of rendering hints for this axis.
     */
    private RenderingHints hints;

    /**
     * Construct an axis with a default {@link NumberGraduation}.
     */
    public Axis2D() {
        this(new NumberGraduation(null));
    }

    /**
     * Construct an axis with the specified graduation.
     */
    public Axis2D(final Graduation graduation) {
        this.graduation = graduation;
        graduation.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent event) {
                synchronized (Axis2D.this) {
                    modCount++;
                    clearCache();
                }
            }
        });
    }

    /**
     * Returns the axis's graduation.
     */
    public Graduation getGraduation() {
        return graduation;
    }

    /**
     * Returns the <var>x</var> coordinate of the start point. By convention,
     * this coordinate should be in pixels or points (1/72 of inch) for proper
     * positionning of ticks and labels.
     *
     * @see #getY1
     * @see #getX2
     * @see #setLine
     */
    public double getX1() {
        return x1;
    }

    /**
     * Returns the <var>x</var> coordinate of the end point. By convention,
     * this coordinate should be in pixels or points (1/72 of inch) for proper
     * positionning of ticks and labels.
     *
     * @see #getY2
     * @see #getX1
     * @see #setLine
     */
    public double getX2() {
        return x2;
    }

    /**
     * Returns the <var>y</var> coordinate of the start point. By convention,
     * this coordinate should be in pixels or points (1/72 of inch) for proper
     * positionning of ticks and labels.
     *
     * @see #getX1
     * @see #getY2
     * @see #setLine
     */
    public double getY1() {
        return y1;
    }

    /**
     * Returns the <var>y</var> coordinate of the end point. By convention,
     * this coordinate should be in pixels or points (1/72 of inch) for proper
     * positionning of ticks and labels.
     *
     * @see #getX2
     * @see #getY1
     * @see #setLine
     */
    public double getY2() {
        return y2;
    }

    /**
     * Returns the (<var>x</var>,<var>y</var>) coordinates of the start point.
     * By convention, those coordinates should be in pixels or points (1/72 of
     * inch) for proper positionning of ticks and labels.
     */
    public synchronized Point2D getP1() {
        return new Point2D.Float(x1,y1);
    }

    /**
     * Returns the (<var>x</var>,<var>y</var>) coordinates of the end point.
     * By convention, those coordinates should be in pixels or points (1/72 of
     * inch) for proper positionning of ticks and labels.
     */
    public synchronized Point2D getP2() {
        return new Point2D.Float(x2,y2);
    }

    /**
     * Returns the axis length. This is the distance between starting point (@link #getP1 P1})
     * and end point ({@link #getP2 P2}). This length is usually measured in pixels or points
     * (1/72 of inch).
     */
    public synchronized double getLength() {
        return XMath.hypot(getX1()-getX2(), getY1()-getY2());
    }

    /**
     * Returns a bounding box for this axis. The bounding box includes
     * the axis's line ({@link #getP1 P1}) to ({@link #getP2 P2}), the
     * axis's ticks and all labels.
     *
     * @see #getX1
     * @see #getY1
     * @see #getX2
     * @see #getY2
     */
    public synchronized Rectangle2D getBounds2D() {
        if (axisBounds == null) {
            paint(null); // Force the computation of bounding box size.
        }
        final Rectangle2D bounds = (Rectangle2D) axisBounds.clone();
        if (labelBounds !=null) bounds.add(labelBounds );
        if (legendBounds!=null) bounds.add(legendBounds);
        return bounds;
    }

    /**
     * Sets the location of the endpoints of this <code>Axis2D</code> to the specified
     * coordinates. Coordinates should be in pixels (for screen rendering) or points
     * (for paper rendering). Using points units make it easy to render labels with
     * a raisonable font size, no matter the screen resolution or the axis graduation.
     *
     * @param x1 Coordinate <var>x</var> of starting point.
     * @param y1 Coordinate <var>y</var> of starting point
     * @param x2 Coordinate <var>x</var> of end point.
     * @param y2 Coordinate <var>y</var> of end point.
     * @throws IllegalArgumentException If a coordinate is <code>NaN</code> or infinite.
     *
     * @see #getX1
     * @see #getY1
     * @see #getX2
     * @see #getY2
     */
    public synchronized void setLine(final double x1, final double y1,
                                     final double x2, final double y2)
        throws IllegalArgumentException
    {
        final float fx1 = (float) x1; AbstractGraduation.ensureFinite("x1", fx1);
        final float fy1 = (float) y1; AbstractGraduation.ensureFinite("y1", fy1);
        final float fx2 = (float) x2; AbstractGraduation.ensureFinite("x2", fx2);
        final float fy2 = (float) y2; AbstractGraduation.ensureFinite("y2", fy2);
        modCount++; // Must be first
        this.x1 = fx1;
        this.y1 = fy1;
        this.x2 = fx2;
        this.y2 = fy2;
        clearCache();
    }

    /**
     * Returns <code>true</code> if the axis would have to rotate clockwise in order to
     * overlaps its graduation.
     */
    public boolean isLabelClockwise() {
        return relativeCCW < 0;
    }

    /**
     * Sets the label's locations relative to this axis. Value <code>true</code> means
     * that the axis would have to rotate clockwise in order to overlaps its graduation.
     * Value <code>false</code> means that the axis would have to rotate counter-clockwise
     * in order to overlaps its graduation.
     */
    public synchronized void setLabelClockwise(final boolean c) {
        modCount++; // Must be first
        relativeCCW = c ? (byte) -1 : (byte) +1;
    }

    /**
     * Returns a default font to use when no rendering hint were provided for
     * the {@link Graduation#TICK_LABEL_FONT} key.
     *
     * @return A default font (never <code>null</code>).
     */
    private synchronized Font getDefaultFont() {
        if (defaultFont == null) {
            defaultFont = new Font("SansSerif", Font.PLAIN, 9);
        }
        return defaultFont;
    }

    /**
     * Returns an iterator object that iterates along the <code>Axis2D</code> boundary
     * and provides access to the geometry of the shape outline. The shape includes
     * the axis line, graduation and labels. If an optional {@link AffineTransform}
     * is specified, the coordinates returned in the iteration are transformed accordingly.
     */
    public java.awt.geom.PathIterator getPathIterator(final AffineTransform transform) {
        return getPathIterator(transform, java.lang.Double.NaN);
    }

    /**
     * Returns an iterator object that iterates along the <code>Axis2D</code> boundary
     * and provides access to the geometry of the shape outline. The shape includes
     * the axis line, graduation and labels. If an optional {@link AffineTransform}
     * is specified, the coordinates returned in the iteration are transformed accordingly.
     */
    public synchronized java.awt.geom.PathIterator getPathIterator(final AffineTransform transform,
                                                                   final double flatness)
    {
        if (isPainting) {
            if (iterator != null) {
                iterator.rewind(transform);
            } else {
                iterator = new TickPathIterator(transform);
            }
            return iterator;
        }
        return new PathIterator(transform, flatness);
    }

    /**
     * Draw this axis in the specified graphics context. This method is equivalents
     * to <code>Graphics2D.draw(this)</code>.  However, this method may be slightly
     * faster and produce better quality output.
     *
     * @param graphics The graphics context to use for drawing.
     */
    public synchronized void paint(final Graphics2D graphics) {
        if (!(getLength()>0)) {
            return;
        }
        /*
         * Initialise l'it�rateur en appelant 'init' (contrairement � 'getPathIterator'
         * qui n'appelle que 'rewind') pour des r�sultats plus rapides et plus constants.
         */
        if (iterator != null) {
            iterator.init(null);
        } else {
            iterator = new TickPathIterator(null);
        }
        final TickPathIterator iterator = this.iterator;
        final boolean sameContext;
        final Shape   clip;
        if (graphics != null) {
            clip = graphics.getClip();
            iterator.setFontRenderContext(graphics.getFontRenderContext());
            iterator.setRenderingHint(graphics, Graduation.LABEL_FONT);
            iterator.setRenderingHint(graphics, Graduation.TICK_LABEL_FONT);
            final FontRenderContext context = iterator.getFontRenderContext();
            sameContext = clip!=null && context.equals(lastContext);
        } else {
            clip        = null;
            sameContext = false;
            iterator.setFontRenderContext(null);
        }
        /*
         * Calcule (si ce n'�tait pas d�j� fait) les coordonn�es d'un rectangle qui englobe l'axe et
         * sa graduation (mais sans les �tiquettes de graduation).  Cette information nous permettra
         * de v�rifier s'il est vraiment n�cessaire de redessiner l'axe en v�rifiant s'il intercepte
         * avec le "clip" du graphique.
         */
        if (axisBounds == null) {
            axisBounds = new Rectangle2D.Float(Math.min(x1,x2), Math.min(y1,y2),
                                               Math.abs(x2-x1), Math.abs(y2-y1));
            while (!iterator.isDone()) {
                axisBounds.add(iterator.point);
                iterator.next();
            }
        }
        /*
         * Dessine l'axe et ses barres de graduation (mais sans les �tiquettes).
         */
        if (graphics != null) {
            if (clip==null || clip.intersects(axisBounds)) try {
                isPainting = true;
                graphics.draw(this);
            } finally {
                isPainting = false;
            }
        }
        /*
         * Dessine les �tiquettes de graduations. Ce bloc peut etre ex�cut� m�me si
         * 'graphics' est nul.  Dans ce cas, les �tiquettes ne seront pas dessin�es
         * mais le calcul de l'espace qu'elles occupent sera quand m�me effectu�.
         */
        if (!sameContext || labelBounds==null || clip.intersects(labelBounds) || maximumSize==null)
        {
            Rectangle2D lastLabelBounds = labelBounds = null;
            double            maxWidth  = 0;
            double            maxHeight = 0;
            iterator.rewind();
            while (iterator.hasNext()) {
                if (iterator.isMajorTick()) {
                    final GlyphVector glyphs = iterator.currentLabelGlyphs();
                    final Rectangle2D bounds = iterator.currentLabelBounds();
                    if (glyphs!=null && bounds!=null) {
                        if (lastLabelBounds==null || !lastLabelBounds.intersects(bounds)) {
                            if (graphics!=null && (clip==null || clip.intersects(bounds))) {
                                graphics.drawGlyphVector(glyphs, (float)bounds.getMinX(),
                                                                 (float)bounds.getMaxY());
                            }
                            lastLabelBounds = bounds;
                            final double width  = bounds.getWidth();
                            final double height = bounds.getHeight();
                            if (width  > maxWidth)  maxWidth =width;
                            if (height > maxHeight) maxHeight=height;
                        }
                        if (labelBounds == null) {
                            labelBounds = new Rectangle2D.Float();
                            labelBounds.setRect(bounds);
                        } else {
                            labelBounds.add(bounds);
                        }
                    }
                }
                iterator.nextMajor();
            }
            maximumSize = new XDimension2D.Float((float)maxWidth, (float)maxHeight);
        }
        /*
         * Ecrit la l�gende de l'axe. Ce bloc peut etre ex�cut� m�me si
         * 'graphics' est nul.  Dans ce cas, la l�gende ne sera pas �crite
         * mais le calcul de l'espace qu'elle occupe sera quand m�me effectu�.
         */
        if (!sameContext || legendBounds==null || clip.intersects(legendBounds)) {
            final String label = graduation.getLabel(true);
            if (label != null) {
                final Font font = iterator.getLabelFont();
                final GlyphVector glyphs = font.createGlyphVector(iterator.getFontRenderContext(), label);
                final AffineTransform rotatedTr = new AffineTransform();
                final Rectangle2D bounds = iterator.centerAxisLabel(glyphs.getVisualBounds(),
                                                                    rotatedTr, maximumSize);
                if (graphics != null) {
                    final AffineTransform currentTr = graphics.getTransform();
                    try {
                        graphics.transform(rotatedTr);
                        graphics.drawGlyphVector(glyphs, (float)bounds.getMinX(),
                                                         (float)bounds.getMaxY());
                    } finally {
                        graphics.setTransform(currentTr);
                    }
                }
                legendBounds = XAffineTransform.transform(rotatedTr, bounds, bounds);
            }
        }
        lastContext = iterator.getFontRenderContext();
    }

    /**
     * Returns the value of a single preference for the rendering algorithms. Hint categories
     * include controls for label fonts and colors. Some of the keys and their associated values
     * are defined in the {@link Graduation} interface.
     *
     * @param  key The key corresponding to the hint to get. 
     * @return An object representing the value for the specified hint key, or <code>null</code>
     *         if no value is associated to the specified key.
     *
     * @see Graduation#TICK_LABEL_FONT
     * @see Graduation#LABEL_FONT
     */
    public synchronized Object getRenderingHint(final RenderingHints.Key key) {
        return (hints!=null) ? hints.get(key) : null;
    }

    /**
     * Sets the value of a single preference for the rendering algorithms. Hint categories
     * include controls for label fonts and colors.  Some of the keys and their associated
     * values are defined in the {@link Graduation} interface.
     *
     * @param key The key of the hint to be set.
     * @param value The value indicating preferences for the specified hint category.
     *              A <code>null</code> value removes any hint for the specified key.
     *
     * @see Graduation#TICK_LABEL_FONT
     * @see Graduation#LABEL_FONT
     */
    public synchronized void setRenderingHint(final RenderingHints.Key key, final Object value) {
        modCount++;
        if (value != null) {
            if (hints == null) {
                hints = new RenderingHints(key, value);
            } else {
                hints.put(key, value);
            }
        } else if (hints != null) {
            hints.remove(key);
            if (hints.isEmpty()) {
                hints = null;
            }
        }
    }

    /**
     * Efface la cache interne. Cette m�thode doit �tre appel�e
     * chaque fois que des propri�t�s de l'axe ont chang�es.
     */
    private void clearCache() {
        axisBounds   = null;
        labelBounds  = null;
        legendBounds = null;
        maximumSize  = null;
    }

    /**
     * Returns a string representation of this axis.
     */
    public String toString() {
        final StringBuffer buffer = new StringBuffer(Utilities.getShortClassName(this));
        buffer.append("[\"");
        buffer.append(graduation.getLabel(true));
        buffer.append("\"]");
        return buffer.toString();
    }

    /**
     * Creates an affine transform mapping logical to pixels coordinates for a pair
     * of axis. The affine transform will maps coordinates in the following way:
     *
     * <ul>
     *   <li>For each input coordinates (<var>x</var>,<var>y</var>), the <var>x</var> and
     *       <var>y</var> values are expressed in the same units than the <code>xAxis</code>
     *       and <code>yAxis</code> graduations, respectively.</li>
     *   <li>The output point is the pixel's coordinates for the (<var>x</var>,<var>y</var>)
     *       values. Changing the <var>x</var> value move the pixel location in parallel with
     *       the <code>xAxis</code>, which may or may not be horizontal. Changing the <var>y</var>
     *       value move the pixel location in parallel with the <code>yAxis</code>, which may or
     *       may not be vertical.</li>
     * </ul>
     *
     * @param  xAxis The <var>x</var> axis. This axis doesn't have to be horizontal;
     *               it can have any orientation, including vertical.
     * @param  yAxis The <var>y</var> axis. This axis doesn't have to be vertical;
     *               it can have any orientation, including horizontal.
     * @return An affine transform mapping logical to pixels coordinates.
     */
    public static AffineTransform createAffineTransform(final Axis2D xAxis, final Axis2D yAxis) {
        /*   x
         *  |
         *  |\
         *  |  \ P     Soit:       X  :  l'axe des <var>x</var> du graphique.
         *  |   |                  Y  :  l'axe des <var>y</var> du graphique.
         *   \  |                  P  :  un point � placer sur le graphique.
         *     \|             (Px,Py) :  les composantes du point P selon les axes x et y.
         *       \ y          (Pi,Pj) :  les composantes du point P en coordonn�es "pixels".
         *
         * D�signons par <b>ex</b> et <b>ey</b> des vecteurs unitaires dans la direction de l'axe des
         * <var>x</var> et l'axe des <var>y</var> respectivement. D�signons par <b>i</b> et <b>j</b>
         * des vecteurs unitaires vers le droite et vers le haut de l'�cran respectivement. On peut
         * d�composer les vecteurs unitaires <b>ex</b> et <b>ey</b> par:
         *
         *          ex = exi*i + exj*j
         *          ey = eyi*i + eyj*j
         * Donc,    P  = Px*ex + Py*ey   =   (Px*exi+Py*eyi)*i + (Px*exj + Py*eyj)*j
         *
         * Cette relation ne s'applique que si les deux syst�mes de coordonn�es (xy et ij) ont
         * la m�me origine. En pratique, ce ne sera pas le cas. Il faut donc compliquer un peu:
         *
         *      Pi = (Px-Ox)*exi+(Py-Oy)*eyi + Oi        o� (Ox,Oy) sont les minimums des axes des x et y.
         *      Pj = (Px-Ox)*exj+(Py-Oy)*eyj + Oj           (Oi,Oj) est l'origine du syst�me d'axe ij.
         *
         * [ Pi ]   [ exi   eyi   Oi-(Ox*exi+Oy*eyi) ][ Px ]     [ exi*Px + eyi*Py + Oi-(Ox*exi+Oy*oyi) ]
         * [ Pj ] = [ exj   eyj   Oj-(Ox*exj+Oy*eyj) ][ Py ]  =  [ exj*Px + eyj*Py + Oj-(Ox*exj+Oy*oyj) ]
         * [  1 ]   [  0    0              1         ][  1 ]     [                   1                  ]
         */
        synchronized (xAxis) {
            synchronized (yAxis) {
                final Graduation mx = xAxis.getGraduation();
                final Graduation my = yAxis.getGraduation();
                double ox = mx.getRange();
                double oy = my.getRange();
                double exi = ((double) xAxis.getX2() - (double) xAxis.getX1()) / ox;
                double exj = ((double) xAxis.getY2() - (double) xAxis.getY1()) / ox;
                double eyi = ((double) yAxis.getX2() - (double) yAxis.getX1()) / oy;
                double eyj = ((double) yAxis.getY2() - (double) yAxis.getY1()) / oy;
                ox = mx.getMinimum();
                oy = my.getMinimum();
                return new AffineTransform(exi, exj, eyi, eyj,
                                           xAxis.x1 - (ox*exi+oy*eyi),
                                           yAxis.y1 - (ox*exj+oy*eyj));
            }
        }
    }





    ////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                            ////////
    ////////                            TICK AND PATH ITERATORS                         ////////
    ////////                                                                            ////////
    ////////////////////////////////////////////////////////////////////////////////////////////


    /**
     * Iterator object that iterates along the graduation ticks and provides
     * access to the graduation values. Each <code>Axis2D.TickIterator</code>
     * object traverses the graduation of the underlying {@link Axis2D} object
     * independently from any other {@link TickIterator} objects in use at
     * the same time. If a change occurs in the underlying {@link Axis2D} object
     * during the iteration, then {@link #refresh} must be invoked in order to
     * reset the iterator as if a new instance was created. Except for {@link #refresh}
     * method, using the iterator after a change in the underlying {@link Axis2D}
     * may thrown a {@link ConcurrentModificationException}.
     *
     * @version $Id: Axis2D.java,v 1.1 2003/03/07 23:36:09 desruisseaux Exp $
     * @author Martin Desruisseaux
     */
    public class TickIterator implements org.geotools.axis.TickIterator {
        /**
         * The underyling tick iterator.
         */
        private org.geotools.axis.TickIterator iterator;

        /**
         * A copy of {@link Axis2D#hints} rendering hints. A copy is required because some hints
         * (especially {@link Graduation#VISUAL_AXIS_LENGTH} and
         * {@link Graduation#VISUAL_TICK_SPACING}) are going to be overwriten. This set may also
         * contains additional hints provided by {@link Graphics2D} in the {@link Axis2D#paint}
         * method. This object will never be <code>null</code>.
         */
        private final RenderingHints hints;

        /**
         * <code>scaleX</code> and <code>scaleY</code> are used for scaling
         * logical coordinates to pixel coordinates. Those scale factors
         * <strong>must</strong> be the same than the one that appears in
         * {@link Axis2D#createAffineTransform}.
         */
        private double scaleX, scaleY;

        /**
         * <code>(tickX, tickY)</code> is a unitary vector perpendicular to the axis.
         */
        private double tickX, tickY;

        /**
         * The minimum value {@link Graduation#getMinimum}.
         * This value is copied here for faster access.
         */
        private double minimum;

        /**
         * Value returned by the last call to {@link #currentLabel}.
         * This value is cached here in order to avoid that
         * {@link #getGlyphVector} compute it again.
         */
        private transient String label;

        /**
         * Value returned by the last call to {@link #getGlyphVector}. This
         * value is cached here in order to avoir that {@link #getBounds}
         * compute it again.
         */
        private transient GlyphVector glyphs;

        /**
         * The font to use for rendering tick. If a rendering hint was provided for the
         * {@link Graduation#TICK_LABEL_FONT} key, then the value is used as the font.
         * Otherwise, a default font is created and used.
         */
        private transient Font font;

        /**
         * The font context from {@link Graphics2D#getFontContext},
         * or <code>null</code> for a default one.
         */
        private transient FontRenderContext fontContext;

        /**
         * Value of {@link Axis2D#modCount} when {@link #init} was
         * last invoked. This value is used in order to detect
         * changes to the underlying {@link Axis2D} during iteration.
         */
        private transient int modCount;

        /**
         * Construct an iterator.
         *
         * @param fontContext Information needed to correctly measure text, or
         *        <code>null</code> if unknow. This object is usually given by
         *        {@link Graphics2D#getFontRenderContext}.
         */
        public TickIterator(final FontRenderContext fontContext) {
            this.hints = new RenderingHints(Axis2D.this.hints);
            this.fontContext = fontContext;
            refresh();
        }

        /**
         * Copy a rendering hints from the specified {@link Graphics2D}, providing that
         * it is not already defined.
         */
        final void setRenderingHint(final Graphics2D graphics, final RenderingHints.Key key) {
            if (hints.get(key) == null) {
                final Object value = graphics.getRenderingHint(key);
                if (value != null) {
                    hints.put(key, value);
                }
            }
        }

        /**
         * Tests if the iterator has more ticks.
         */
        public boolean hasNext() {
            return iterator.hasNext();
        }

        /**
         * Tests if the current tick is a major one.
         *
         * @return <code>true</code> if current tick is a major tick,
         *         or <code>false</code> if it is a minor tick.
         */
        public boolean isMajorTick() {
            return iterator.isMajorTick();
        }

        /**
         * Returns the value for current tick. The current tick may be major or minor.
         */
        public double currentValue() {
            return iterator.currentValue();
        }

        /**
         * Returns the coordinates of the intersection point between current tick
         * and the underlying axis. Units are the same than axis start point
         * ({@linkplain #getX1 <var>x<sub>1</sub></var>},{@linkplain #getY1 <var>y<sub>1</sub></var>})
         * and end point
         * ({@linkplain #getX2 <var>x<sub>2</sub></var>},{@linkplain #getY2 <var>y<sub>2</sub></var>}).
         * This is usually pixels.
         *
         * @param  dest A destination point that stores the intersection coordinates,
         *         or <code>null</code> to create a new {@link Point2D} object.
         * @return <code>dest</code>, or a new {@link Point2D} object if <code>dest</code> was null.
         */
        public Point2D currentPosition(final Point2D dest) {
            final double     position = currentValue()-minimum;
            final double x = position*scaleX + getX1();
            final double y = position*scaleY + getY1();
            ensureValid();
            if (dest != null) {
                dest.setLocation(x,y);
                return dest;
            }
            return new Point2D.Float((float)x, (float)y);
        }

        /**
         * Returns the coordinates of the current tick.
         * Units are the same than axis start point
         * ({@linkplain #getX1 <var>x<sub>1</sub></var>},{@linkplain #getY1 <var>y<sub>1</sub></var>})
         * and end point
         * ({@linkplain #getX2 <var>x<sub>2</sub></var>},{@linkplain #getY2 <var>y<sub>2</sub></var>}).
         * This is usually pixels.
         *
         * @param  dest A destination line that stores the current tick coordinates,
         *         or <code>null</code> to create a new {@link Line2D} object.
         * @return <code>dest</code>, or a new {@link Line2D} object if <code>dest</code> was null.
         */
        public Line2D currentTick(final Line2D dest) {
            final boolean isMajorTick = isMajorTick();
            final double     position = currentValue()-minimum;
            final double x  = position*scaleX + getX1();
            final double y  = position*scaleY + getY1();
            final double s1 = isMajorTick ? tickStart : subTickStart;
            final double s2 = isMajorTick ? tickEnd   : subTickEnd;
            final double x1 = x+tickX*s1;
            final double y1 = y+tickY*s1;
            final double x2 = x+tickX*s2;
            final double y2 = y+tickY*s2;
            ensureValid();
            if (dest != null) {
                dest.setLine(x1, y1, x2, y2);
                return dest;
            }
            return new Line2D.Float((float)x1, (float)y1, (float)x2, (float)y2);
        }

        /**
         * Returns the label for current tick. This method is usually invoked
         * only for major ticks, but may be invoked for minor ticks as well.
         * This method returns <code>null</code> if it can't produces a label
         * for current tick.
         */
        public String currentLabel() {
            if (label == null) {
                label = iterator.currentLabel();
            }
            return label;
        }

        /**
         * Returns the label for current tick as a glyphs vector. This method is used
         * together with {@link #currentLabelBounds} for labels rendering. <strong>Do
         * not change the returned {@link GlyphVector}</strong>, since the glyphs vector
         * is not cloned for performance raisons. This method returns <code>null</code>
         * if it can't produces a glyph vector for current tick.
         */
        public GlyphVector currentLabelGlyphs() {
            if (glyphs == null) {
                final String label = currentLabel();
                if (label != null) {
                    glyphs = getTickFont().createGlyphVector(getFontRenderContext(), label);
                }
            }
            return glyphs;
        }

        /**
         * Returns a bounding vector for the current tick label.
         * Units are the same than axis start point
         * ({@linkplain #getX1 <var>x<sub>1</sub></var>},{@linkplain #getY1 <var>y<sub>1</sub></var>})
         * and end point
         * ({@linkplain #getX2 <var>x<sub>2</sub></var>},{@linkplain #getY2 <var>y<sub>2</sub></var>}).
         * This is usually pixels. This method can be used as in the example below:
         *
         * <pre>
         * {@link Axis2D.TickIterator} iterator = axis.new {@link Axis2D.TickIterator TickIterator}(null};
         * while (iterator.{@link #hasNext() hasNext()}) {
         *     {@link GlyphVector} glyphs = iterator.{@link Axis2D.TickIterator#currentLabelGlyphs() currentLabelGlyphs()};
         *     {@link Rectangle2D} bounds = iterator.{@link Axis2D.TickIterator#currentLabelBounds() currentLabelBounds()};
         *     graphics.drawGlyphVector(glyphs, (float)bounds.getMinX(), (float)bounds.getMaxY());
         *     iterator.{@link #next() next()};
         * }
         * </pre>
         *
         * This method returns <code>null</code> if it can't compute bounding box for current tick.
         */
        public Rectangle2D currentLabelBounds() {
            final GlyphVector glyphs = currentLabelGlyphs();
            if (glyphs == null) {
                return null;
            }
            final Rectangle2D bounds = glyphs.getVisualBounds();
            final double      height = bounds.getHeight();
            final double      width  = bounds.getWidth();
            final double  tickStart  = (0.5*height)-Math.min(Axis2D.this.tickStart, 0);
            final double    position = currentValue()-minimum;
            final double x= position*scaleX + getX1();
            final double y= position*scaleY + getY1();
            bounds.setRect(x - (1+tickX)*(0.5*width)  - tickX*tickStart,
                           y + (1-tickY)*(0.5*height) - tickY*tickStart - height,
                           width, height);
            ensureValid();
            return bounds;
        }

        /**
         * Returns the font for tick labels. This is the font used for drawing the tick label
         * formatted by {@link TickIterator#currentLabel}.
         *
         * @return The font (never <code>null</code>).
         */
        private Font getTickFont() {
            if (font == null) {
                Object candidate = hints.get(Graduation.TICK_LABEL_FONT);
                if (candidate instanceof Font) {
                    font = (Font) candidate;
                } else {
                    font = getDefaultFont();
                }
            }
            return font;
        }

        /**
         * Returns the font for axis label. This is the font used for drawing the label
         * formatted by {@link Graduation#getLabel}.
         *
         * @return The font (never <code>null</code>).
         */
        final Font getLabelFont() {
            Object candidate = hints.get(Graduation.LABEL_FONT);
            if (candidate instanceof Font) {
                return (Font) candidate;
            }
            final Font font = getTickFont();
            return font.deriveFont(Font.BOLD, font.getSize2D() * (12f/9));
        }

        /**
         * Retourne un rectangle centr� vis-�-vis l'axe. Les coordonn�es de ce rectangle seront
         * les m�mes que celles de l'axe, habituellement des pixels ou des points (1/72 de pouce).
         * Cette m�thode s'utilise typiquement comme suit:
         *
         * <pre>
         * Graphics2D           graphics = ...
         * FontRenderContext fontContext = graphics.getFontRenderContext();
         * TickIterator         iterator = axis.new TickIterator(graphics.getFontRenderContext());
         * Font                     font = iterator.getLabelFont();
         * String                  label = axis.getGraduation().getLabel(true);
         * GlyphVector            glyphs = font.createGlyphVector(fontContext, label);
         * Rectangle2D            bounds = centerAxisLabel(glyphs.getVisualBounds());
         * graphics.drawGlyphVector(glyphs, (float)bounds.getMinX(), (float)bounds.getMaxY());
         * </pre>
         *
         * @param  bounds  Un rectangle englobant les caract�res � �crire. La position
         *                 (<var>x</var>,<var>y</var>) de ce rectangle est g�n�ralement
         *                 (mais pas obligatoirement) l'origine (0,0). Ce rectangle est
         *                 habituellement obtenu par un appel �
         *                 {@link Font#createGlyphVector(FontContext,String)}.
         * @param toRotate Si non-nul, transformation affine sur laquelle appliquer une rotation
         *                 �gale � l'angle de l'axe.  Cette m�thode peut limiter la rotation aux
         *                 quadrants 1 et 2 afin de conserver une lecture agr�able du texte.
         * @param maximumSize Largeur et hauteur maximales des �tiquettes de graduation. Cette
         *                 information est utilis�e pour �carter l'�tiquette de l'axe suffisament
         *                 pour qu'elle n'�crase pas les �tiquettes de graduation.
         * @return Le rectangle <code>bounds</code>, modifi� pour �tre centr� sur l'axe.
         */
        final Rectangle2D centerAxisLabel(final Rectangle2D     bounds,
                                          final AffineTransform toRotate,
                                          final Dimension2D maximumSize)
        {
            final double height = bounds.getHeight();
            final double width  = bounds.getWidth();
            final double tx = 0;
            final double ty = height + Math.abs(maximumSize.getWidth()*tickX) + Math.abs(maximumSize.getHeight()*tickY);
            final double x1 = getX1();
            final double y1 = getY1();
            final double x2 = getX2();
            final double y2 = getY2();
            /////////////////////////////////////
            //// Compute unit vector (ux,uy) ////
            /////////////////////////////////////
            double ux = (double) x2 - (double) x1;
            double uy = (double) y2 - (double) y1;
            double ul = Math.sqrt(ux*ux + uy*uy);
            ux /= ul;
            uy /= ul;
            //////////////////////////////////////////////
            //// Get the central position of the axis ////
            //////////////////////////////////////////////
            double x = 0.5 * (x1+x2);
            double y = 0.5 * (y1+y2);
            ////////////////////////////////////////
            //// Apply the parallel translation ////
            ////////////////////////////////////////
            x += ux*tx;
            y += uy*tx;
            ////////////////////////////////////
            //// Adjust sign of unit vector ////
            ////////////////////////////////////
            ux *= relativeCCW;
            uy *= relativeCCW;
            /////////////////////////////////////////////
            //// Apply the perpendicular translation ////
            /////////////////////////////////////////////
            x += uy*ty;
            y -= ux*ty;
            ///////////////////////////////////
            //// Offset the point for text ////
            ///////////////////////////////////
            final double anchorX = x;
            final double anchorY = y;
            if (toRotate == null) {
                y += 0.5*height * (1-ux);
                x -= 0.5*width  * (1-uy);
            } else {
                if (ux < 0) {
                    ux = -ux;
                    uy = -uy;
                    y += height;
                }
                x -= 0.5*width;
                toRotate.rotate(Math.atan2(uy,ux), anchorX, anchorY);
            }
            bounds.setRect(x, y-height, width, height);
            ensureValid();
            return bounds;
        }

        /**
         * Moves the iterator to the next minor or major tick.
         */
        public void next() {
            this.label  = null;
            this.glyphs = null;
            iterator.next();
        }

        /**
         * Moves the iterator to the next major tick. This move
         * ignore any minor ticks between current position and
         * the next major tick.
         */
        public void nextMajor() {
            this.label  = null;
            this.glyphs = null;
            iterator.nextMajor();
        }

        /**
         * Reset the iterator on its first tick.
         * All other properties are left unchanged.
         */
        public void rewind() {
            this.label  = null;
            this.glyphs = null;
            iterator.rewind();
        }

        /**
         * Reset the iterator on its first tick. If some axis properies have
         * changed (e.g. minimum and/or maximum values), then the new settings
         * are taken in account. This {@link #refresh} method help to reduce
         * garbage-collection by constructing an <code>Axis2D.TickIterator</code>
         * object only once and reuse it for each axis's rendering.
         */
        public void refresh() {
            synchronized (Axis2D.this) {
                this.label  = null;
                this.glyphs = null;
                // Do NOT modify 'fontContext'.

                final Graduation graduation = getGraduation();
                final double     dx = getX2()-getX1();
                final double     dy = getY2()-getY1();
                final double  range = graduation.getRange();
                final double length = Math.sqrt(dx*dx + dy*dy);
                hints.put(Graduation.VISUAL_AXIS_LENGTH, new java.lang.Double(length));

                this.scaleX   =  dx/range;
                this.scaleY   =  dy/range;
                this.tickX    = -dy/length*relativeCCW;
                this.tickY    = +dx/length*relativeCCW;
                this.minimum  = graduation.getMinimum();
                this.iterator = graduation.getTickIterator(hints, iterator);
                this.modCount = Axis2D.this.modCount;
            }
        }

        /**
         * Returns the locale used for formatting tick labels.
         */
        public Locale getLocale() {
            return iterator.getLocale();
        }

        /**
         * Retourne le contexte utilis� pour dessiner les caract�res.
         * Cette m�thode ne retourne jamais <code>null</code>.
         */
        final FontRenderContext getFontRenderContext() {
            if (fontContext == null) {
                fontContext = new FontRenderContext(null, false, false);
            }
            return fontContext;
        }

        /**
         * Sp�cifie le contexte � utiliser pour dessiner les caract�res,
         * ou <code>null</code> pour utiliser un contexte par d�faut.
         */
        final void setFontRenderContext(final FontRenderContext context) {
            fontContext = context;
        }

        /**
         * V�rifie que l'axe n'a pas chang� depuis le dernier appel de {@link #init}.
         * Cette m�thode doit �tre appel�e <u>� la fin</u> des m�thodes de cette classe
         * qui lisent les champs de {@link Axis2D}.
         */
        final void ensureValid() {
            if (this.modCount != Axis2D.this.modCount) {
                throw new ConcurrentModificationException();
            }
        }
    }




    /**
     * It�rateur balayant l'axe et ses barres de graduations pour leur tra�age.
     * Cet it�rateur ne balaye pas les �tiquettes de graduations.  Puisque cet
     * it�rateur ne retourne que des droites et jamais de courbes, il ne prend
     * pas d'argument <code>flatness</code>.
     *
     * @version $Id: Axis2D.java,v 1.1 2003/03/07 23:36:09 desruisseaux Exp $
     * @author Martin Desruisseaux
     */
    private class TickPathIterator extends TickIterator implements java.awt.geom.PathIterator {
        /**
         * Transformation affine � appliquer sur les donn�es. Il doit s'agir
         * d'une transformation affine appropri�e pour l'�criture de texte
         * (g�n�ralement en pixels ou en points). Il ne s'agit <u>pas</u> de
         * la transformation affine cr��e par {@link Axis2D#createAffineTransform}.
         */
        protected AffineTransform transform;

        /**
         * Coordonn�es de la prochaine graduation � retourner par une des m�thodes
         * <code>currentSegment(...)</code>. Ces coordonn�es n'auront <u>pas</u>
         * �t� transform�es selon la transformation affine {@link #transform}.
         */
        private final Line2D.Double line = new Line2D.Double();

        /**
         * Coordonn�es du prochain point � retourner par une des m�thodes
         * <code>currentSegment(...)</code>. Ces coordonn�es auront �t�
         * transform�es selon la transformation affine {@link #transform}.
         */
        private final Point2D.Double point = new Point2D.Double();

        /**
         * Type du prochain segment. Ce type est retourn� par les m�thodes
         * <code>currentSegment(...)</code>. Il doit s'agir en g�n�ral d'une
         * des constantes {@link #SEG_MOVETO} ou {@link #SEG_LINETO}.
         */
        private int type = SEG_MOVETO;

        /**
         * Entier indiquant quel sera le prochain item a retourner (d�but ou
         * fin d'une graduation, d�but ou fin de l'axe, etc.). Il doit s'agir
         * d'une des constantes {@link #AXIS_MOVETO}, {@link #AXIS_LINETO},
         * {@link #TICK_MOVETO},  {@link #TICK_LINETO}, etc.
         */
        private int nextType = AXIS_MOVETO;

        /** Constante pour {@link #nextType}.*/ private static final int AXIS_MOVETO = 0;
        /** Constante pour {@link #nextType}.*/ private static final int AXIS_LINETO = 1;
        /** Constante pour {@link #nextType}.*/ private static final int TICK_MOVETO = 2;
        /** Constante pour {@link #nextType}.*/ private static final int TICK_LINETO = 3;

        /**
         * Construit un it�rateur.
         *
         * @param transform Transformation affine � appliquer sur les donn�es. Il doit
         *            s'agir d'une transformation affine appropri�e pour l'�criture de
         *            texte (g�n�ralement en pixels ou en points). Il ne s'agit <u>pas</u>
         *            de la transformation affine cr��e par {@link Axis2D#createAffineTransform}.
         */
        public TickPathIterator(final AffineTransform transform) {
            super(null);
            // 'refresh' est appel�e par le constructeur parent.
            this.transform=transform;
            next();
        }

        /**
         * Initialise cet it�rateur.  Cette m�thode peut �tre appel�e
         * pour r�utiliser un it�rateur qui a d�j� servit, plut�t que
         * d'en construire un autre.
         *
         * @param transform Transformation affine � appliquer sur les donn�es. Il doit
         *        s'agir d'une transformation affine appropri�e pour l'�criture de
         *        texte (g�n�ralement en pixels ou en points). Il ne s'agit <u>pas</u>
         *        de la transformation affine cr��e par {@link Axis2D#createAffineTransform}.
         */
        final void init(final AffineTransform transform) {
            refresh();
            setFontRenderContext(null);
            this.type      = SEG_MOVETO;
            this.nextType  = AXIS_MOVETO;
            this.transform = transform;
            next();
        }

        /**
         * Repositione l'it�rateur au d�but de la graduation
         * avec une nouvelle transformation affine.
         */
        public void rewind(final AffineTransform transform) {
            super.rewind();
            // Keep 'fontContext'.
            this.type      = SEG_MOVETO;
            this.nextType  = AXIS_MOVETO;
            this.transform = transform;
            next();
        }

        /**
         * Repositione l'it�rateur au d�but de la graduation
         * en conservant la transformation affine actuelle.
         */
        public final void rewind() {
            rewind(transform);
        }

        /**
         * Return the winding rule for determining the insideness of the path.
         */
        public int getWindingRule() {
            return WIND_NON_ZERO;
        }

        /**
         * Tests if the iteration is complete.
         */
        public boolean isDone() {
            return nextType==TICK_LINETO && !hasNext();
        }

        /**
         * Returns the coordinates and type of the current path segment
         * in the iteration. The return value is the path segment type:
         * <code>SEG_MOVETO</code> or <code>SEG_LINETO</code>.
         */
        public int currentSegment(final float[] coords) {
            coords[0] = (float) point.x;
            coords[1] = (float) point.y;
            return type;
        }

        /**
         * Returns the coordinates and type of the current path segment
         * in the iteration. The return value is the path segment type:
         * <code>SEG_MOVETO</code> or <code>SEG_LINETO</code>.
         */
        public int currentSegment(final double[] coords) {
            coords[0] = point.x;
            coords[1] = point.y;
            return type;
        }

        /**
         * Moves the iterator to the next segment of the path forwards
         * along the primary direction of traversal as long as there are
         * more points in that direction.
         */
        public void next() {
            switch (nextType) {
                default: { // Should not happen
                    throw new IllegalPathStateException(Integer.toString(nextType));
                }
                case AXIS_MOVETO: { // Premier point de l'axe
                    point.x  = getX1();
                    point.y  = getY1();
                    type     = SEG_MOVETO;
                    nextType = AXIS_LINETO;
                    break;
                }
                case AXIS_LINETO: { // Fin de l'axe
                    point.x  = getX2();
                    point.y  = getY2();
                    type     = SEG_LINETO;
                    nextType = TICK_MOVETO;
                    break;
                }
                case TICK_MOVETO: { // Premier point d'une graduation
                    currentTick(line);
                    point.x  = line.x1;
                    point.y  = line.y1;
                    type     = SEG_MOVETO;
                    nextType = TICK_LINETO;
                    break;
                }
                case TICK_LINETO: { // Dernier point d'une graduation
                    point.x  = line.x2;
                    point.y  = line.y2;
                    type     = SEG_LINETO;
                    nextType = TICK_MOVETO;
                    prepareLabel();
                    super.next();
                    break;
                }
            }
            if (transform != null) {
                transform.transform(point, point);
            }
            ensureValid();
        }

        /**
         * M�thode appel�e automatiquement par {@link #next} pour
         * indiquer qu'il faudra se pr�parer � tracer une �tiquette.
         */
        protected void prepareLabel() {
        }
    }




    /**
     * It�rateur balayant l'axe et ses barres de graduations pour leur tra�age.
     * Cet it�rateur balaye aussi les �tiquettes de graduations.
     *
     * @version $Id: Axis2D.java,v 1.1 2003/03/07 23:36:09 desruisseaux Exp $
     * @author Martin Desruisseaux
     */
    private final class PathIterator extends TickPathIterator {
        /**
         * Controle le remplacement des courbes par des droites. La valeur
         * {@link Double#NaN} indique qu'un tel remplacement n'a pas lieu.
         */
        private final double flatness;

        /**
         * Chemin de l'�tiquette {@link #label}.
         */
        private java.awt.geom.PathIterator path;

        /**
         * Etiquette de graduation � tracer.
         */
        private Shape label;

        /**
         * Rectangle englobant l'�tiquette
         * {@link #label} courante.
         */
        private Rectangle2D labelBounds;

        /**
         * Valeur maximale de <code>labelBounds.getWidth()</code>
         * trouv�e jusqu'� maintenant.
         */
        private double maxWidth=0;

        /**
         * Valeur maximale de <code>labelBounds.getHeight()</code>
         * trouv�e jusqu'� maintenant.
         */
        private double maxHeight=0;

        /**
         * Prend la valeur <code>true</code> lorsque
         * la l�gende de l'axe a �t� �crite.
         */
        private boolean isDone;

        /**
         * Construit un it�rateur.
         *
         * @param transform Transformation affine � appliquer sur les donn�es. Il doit
         *        s'agir d'une transformation affine appropri�e pour l'�criture de
         *        texte (g�n�ralement en pixels ou en points). Il ne s'agit <u>pas</u>
         *        de la transformation affine cr��e par {@link Axis2D#createAffineTransform}.
         * @param flatness Contr�le le remplacement des courbes par des droites. La valeur
         *        {@link Double#NaN} indique qu'un tel remplacement ne doit pas �tre fait.
         */
        public PathIterator(final AffineTransform transform, final double flatness) {
            super(transform);
            this.flatness = flatness;
        }

        /**
         * Retourne un it�rateur balayant
         * la forme g�om�trique sp�cifi�e.
         */
        private java.awt.geom.PathIterator getPathIterator(final Shape shape) {
            return java.lang.Double.isNaN(flatness) ? shape.getPathIterator(transform)
                                                    : shape.getPathIterator(transform, flatness);
        }

        /**
         * Lance une exception; cet it�rateur n'est con�u
         * pour n'�tre utilis� qu'une seule fois.
         */
        public void rewind(final AffineTransform transform) {
            throw new UnsupportedOperationException();
        }

        /**
         * Tests if the iteration is complete.
         */
        public boolean isDone() {
            return (path!=null) ? path.isDone() : super.isDone();
        }

        /**
         * Returns the coordinates and type of
         * the current path segment in the iteration.
         */
        public int currentSegment(final float[] coords) {
            return (path!=null) ? path.currentSegment(coords) : super.currentSegment(coords);
        }

        /**
         * Returns the coordinates and type of
         * the current path segment in the iteration.
         */
        public int currentSegment(final double[] coords) {
            return (path!=null) ? path.currentSegment(coords) : super.currentSegment(coords);
        }

        /**
         * Moves the iterator to the next segment of the path forwards
         * along the primary direction of traversal as long as there are
         * more points in that direction.
         */
        public void next() {
            if (path != null) {
                path.next();
                if (!path.isDone()) {
                    return;
                }
                path = null;
            }
            if (label != null) {
                path  = getPathIterator(label);
                label = null;
                if (path != null) {
                    if (!path.isDone()) {
                        return;
                    }
                    path=null;
                }
            }
            if (!isDone) {
                super.next();
                if (isDone()) {
                    /*
                     * Quand tout le reste est termin�, pr�pare
                     * l'�criture de la l�gende de l'axe.
                     */
                    isDone = true;
                    final String label = graduation.getLabel(true);
                    if (label != null) {
                        final GlyphVector glyphs;
                        glyphs = getLabelFont().createGlyphVector(getFontRenderContext(), label);
                        if (transform != null) {
                            transform = new AffineTransform(transform);
                        } else {
                            transform = new AffineTransform();
                        }
                        final Rectangle2D bounds = centerAxisLabel(glyphs.getVisualBounds(),
                                                   transform,
                                                   new XDimension2D.Double(maxWidth, maxHeight));
                        path = getPathIterator(glyphs.getOutline((float)bounds.getMinX(),
                                                                 (float)bounds.getMaxY()));
                    }
                }
            }
        }

        /**
         * M�thode appel�e automatiquement par {@link #next} pour
         * indiquer qu'il faudra se pr�parer � tracer une �tiquette.
         */
        protected void prepareLabel() {
            if (isMajorTick()) {
                final GlyphVector glyphs = currentLabelGlyphs();
                final Rectangle2D bounds = currentLabelBounds();
                if (glyphs!=null && bounds!=null) {
                    if (labelBounds==null || !labelBounds.intersects(bounds)) {
                        label=glyphs.getOutline((float)bounds.getMinX(), (float)bounds.getMaxY());
                        final double width  = bounds.getWidth();
                        final double height = bounds.getHeight();
                        if (width  > maxWidth)  maxWidth =width;
                        if (height > maxHeight) maxHeight=height;
                        labelBounds=bounds;
                    }
                }
            }
        }
    }
}
