/*
 * Geotools - OpenSource mapping toolkit
 * (C) 2003, Centre for Computational Geography
 * (C) 2001, Institut de Recherche pour le D�veloppement
 * (C) 1998, P�ches et Oc�ans Canada
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
package org.geotools.renderer.j2d;

// Geometric shapes
import java.awt.Shape;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Dimension2D;
import java.awt.geom.AffineTransform;

// User interface and Java2D rendering
import java.awt.Stroke;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.EventQueue;
import java.awt.BasicStroke;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;

// Miscellaneous J2SE
import java.util.Locale;
import java.util.EventListener;

// Java Advanced Imaging
import javax.media.jai.PlanarImage; // For Javadoc

// Geotools dependencies
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.ct.TransformException;
import org.geotools.ct.MathTransform;
import org.geotools.resources.XMath;
import org.geotools.resources.Utilities;
import org.geotools.resources.CTSUtilities;
import org.geotools.resources.XAffineTransform;
import org.geotools.resources.renderer.Resources;
import org.geotools.resources.renderer.ResourceKeys;


/**
 * Base class for layers to be rendered using the {@linkplain Renderer renderer for Java2D}.
 * When a layer is being {@linkplain Renderer#addLayer(RenderedLayer) added} to a renderer,
 * the following methods are automatically invoked:
 *
 * <blockquote><pre>
 * {@link #setCoordinateSystem setCoordinateSystem}({@link Renderer#getCoordinateSystem renderingCS});
 * {@link #setVisible setVisible}(true);
 * </pre></blockquote>
 *
 * @version $Id: RenderedLayer.java,v 1.9 2003/02/20 11:18:08 desruisseaux Exp $
 * @author Martin Desruisseaux
 *
 * @see Renderer
 * @see RenderingContext
 */
public abstract class RenderedLayer {
    /**
     * The default stroke to use if no stroke can be infered from
     * {@link #getPreferredPixelSize}.
     */
    static final Stroke DEFAULT_STROKE = new BasicStroke(0);

    /**
     * The renderer that own this layer, or <code>null</code>
     * if this layer has not yet been added to a renderer.
     */
    transient Renderer renderer;

    /**
     * Syst�me de coordonn�es utilis� pour cette couche. Les m�thodes {@link #getPreferredArea}
     * et {@link #setPreferredArea} utilisent ce syst�me de coordonn�es. Ce champ ne doit jamais
     * �tre nul.
     */
    private CoordinateSystem coordinateSystem = GeographicCoordinateSystem.WGS84;

    /**
     * Forme g�om�trique englobant la r�gion dans laquelle la couche a �t� dessin�e lors du
     * dernier appel de la m�thode {@link #paint}.  Les coordonn�es de cette r�gion doivent
     * �tre en exprim�es en coordonn�es de Java2D ({@link RenderingContext#textCS}).
     * La valeur <code>null</code> signifie qu'on peut consid�rer que cette couche occupe la
     * totalit� de la surface dessinable.
     */
    private transient Shape paintedArea;

    /**
     * Coordonn�es g�ographiques couvertes par cette couche. Ces coordonn�es doivent
     * �tre exprim�ees selon le syst�me de coordonn�es <code>coordinateSystem</code>.
     * Une valeur nulle signifie que cette couche n'a pas de limites bien d�limit�es.
     *
     * @see #getPreferredArea
     * @see #setPreferredArea
     */
    private Rectangle2D preferredArea;

    /**
     * Dimension pr�f�r�e des pixels pour un zoom rapproch�. Une valeur
     * nulle signifie qu'aucune dimension pr�f�r�e n'a �t� sp�cifi�e.
     *
     * @see #getPreferredPixelSize
     * @see #setPreferredPixelSize
     */
    private Dimension2D preferredPixelSize;

    /**
     * Largeur par d�faut des lignes � tracer. La valeur <code>null</code>
     * signifie que cette largeur doit �tre recalcul�e. Cette largeur sera
     * d�termin�e � partir de la valeur de {@link #preferredPixelSize}.
     */
    private transient Stroke stroke;

    /**
     * Indique si cette couche est visible. Les couches sont invisibles par d�faut.
     * L'appel de {@link Renderer#addLayer(RenderedLayer)} appelera syst�matiquement
     * <code>setVisible(true)</code>.
     *
     * @see #setVisible
     */
    private boolean visible;

    /**
     * Ordre <var>z</var> � laquelle cette couche doit �tre dessin�e. Les couches avec un
     * <var>z</var> �lev� seront dessin�es par dessus les couches avec un <var>z</var> bas.
     * Typiquement, cet ordre <var>z</var> devrait �tre l'altitude en m�tres de la couche
     * (par exemple -30 pour l'isobath � 30 m�tres de profondeur). La valeur
     * {@link Float#POSITIVE_INFINITY} fait dessiner une couche par dessus tout le reste,
     * tandis que la valeur {@link Float#NEGATIVE_INFINITY} la fait dessiner en dessous.
     * La valeur {@link Float#NaN} n'est pas valide. La valeur par d�faut est
     * {@link Float#POSITIVE_INFINITY}.
     *
     * @see #getZOrder
     * @see #setZOrder
     */
    private float zOrder = Float.POSITIVE_INFINITY;

    /**
     * Listeners to be notified about any changes in this layer's properties.
     * Examples of properties that may change are:
     * <code>"coordinateSystem"</code>,
     * <code>"preferredArea"</code>,
     * <code>"preferredPixelSize"</code>,
     * <code>"zOrder"</code> and
     * <code>"visible"</code>.
     */
    protected final PropertyChangeSupport listeners;

    /**
     * Construct a new rendered layer. The {@linkplain #getCoordinateSystem coordinate system}
     * default to {@linkplain GeographicCoordinateSystem#WGS84 WGS 1984} and the {@linkplain
     * #getZOrder z-order} default to positive infinity (i.e. this layer is drawn on top of
     * everything else). Subclasses should invokes <code>setXXX</code> methods in order to
     * define properly this layer's properties.
     *
     * @see #setCoordinateSystem
     * @see #setPreferredArea
     * @see #setPreferredPixelSize
     * @see #setZOrder
     */
    public RenderedLayer() {
        listeners = new PropertyChangeSupport(this);
    }

    /**
     * Returns this layer's name. Default implementation returns the class name with
     * the layer's {@linkplain #getZOrder z-order}.
     *
     * @param  locale The desired locale, or <code>null</code> for a default locale.
     * @return This layer's name.
     *
     * @see #getLocale
     * @see Renderer#getName
     */
    public String getName(final Locale locale) {
        return Utilities.getShortClassName(this) + '[' + getZOrder() + ']';
    }

    /**
     * Returns to locale for this layer. The default implementation inherit the locale
     * of its {@link Renderer}, if it has one. Otherwise, a default locale is returned.
     *
     * @see Renderer#getLocale
     * @see Component#getLocale
     */
    public Locale getLocale() {
        final Renderer renderer = this.renderer;
        return (renderer!=null) ? renderer.getLocale() : Locale.getDefault();
    }

    /**
     * Returns the two-dimensional rendering coordinate system. This is usually the
     * {@linkplain Renderer#getCoordinateSystem renderer's coordinate system}. This
     * CS is always two-dimensional and is used by most methods like {@link #getPreferredArea}
     * and {@link #getPreferredPixelSize}.
     *
     * @return The coordinate system for this rendered layer.
     *
     * @see #setCoordinateSystem
     * @see #getPreferredArea
     * @see #getPreferredPixelSize
     * @see RenderingContext#mapCS
     */
    public final CoordinateSystem getCoordinateSystem() {
        return coordinateSystem;
    }

    /**
     * Set the rendering coordinate system for this layer. This method is usually invoked
     * in any of the following cases:
     * <ul>
     *   <li>From this <code>RenderedLayer</code>'s constructor.</li>
     *   <li>When this layer has just been added to a {@link Renderer}.</li>
     *   <li>When {@link Renderer#setCoordinateSystem} has been invoked.</li>
     * </ul>
     * This method invalidate the {@linkplain #getPreferredArea preferred area} and
     * the {@linkplain #getPreferredPixelSize preferred pixel size}  (i.e. set them
     * to <code>null</code>). Subclasses should overrides this method and transform
     * their internal data here, if needed.
     *
     * @param  cs The coordinate system. If the specified coordinate system has more than
     *            two dimensions, then it must be a {@link CompoundCoordinateSystem} with
     *            a two dimensional {@link CompoundCoordinateSystem#getHeadCS headCS}.
     * @throws TransformException If <code>cs</code> can't be reduced to a two-dimensional
     *         coordinate system, or if this method do not accept the new coordinate system
     *         for some other reason. In case of failure, this method should keep the old CS
     *         and leave this layer in a consistent state.
     */
    protected void setCoordinateSystem(final CoordinateSystem cs) throws TransformException {
        final CoordinateSystem oldCS;
        synchronized (getTreeLock()) {
            oldCS = coordinateSystem;
            coordinateSystem   = CTSUtilities.getCoordinateSystem2D(cs);
            preferredArea      = null;
            preferredPixelSize = null;
        }
        listeners.firePropertyChange("coordinateSystem", oldCS, cs);
    }

    /**
     * Returns the preferred area for this layer. This is the default area to show before any
     * zoom is applied. This is usually (but not always) the bounding box of the underlying data.
     *
     * @return The preferred area in the {@linkplain #getCoordinateSystem rendering coordinate
     *         system}, or <code>null</code> if unknow or not applicable.
     *
     * @see #getPreferredPixelSize
     * @see #getCoordinateSystem
     */
    public Rectangle2D getPreferredArea() {
        final Rectangle2D preferredArea = this.preferredArea;
        return (preferredArea!=null) ? (Rectangle2D) preferredArea.clone() : null;
    }

    /**
     * Set the preferred area for this layer. This method do not change the georeferencing of
     * the layer data. The preferred area change only the default area to be shown in a window.
     *
     * @see #getPreferredArea
     * @see #setPreferredPixelSize
     * @see #getCoordinateSystem
     */
    public void setPreferredArea(final Rectangle2D area) {
        final Rectangle2D oldArea;
        synchronized (getTreeLock()) {
            paintedArea = null;
            oldArea = preferredArea;
            preferredArea = (area!=null) ? (Rectangle2D)area.clone() : null;
        }
        listeners.firePropertyChange("preferredArea", oldArea, area);
    }

    /**
     * Returns the preferred pixel size in rendering coordinates. For image layers, this is
     * the size of image's pixels. For other kind of layers, "pixel size" are to be understood
     * as some dimension representative of the layer's resolution.
     *
     * @return The preferred pixel size in this {@linkplain #getCoordinateSystem rendering
     *         coordinate system}, or <code>null</code> if none.
     *
     * @see #getPreferredArea
     * @see #getCoordinateSystem
     */
    public Dimension2D getPreferredPixelSize() {
        final Dimension2D preferredPixelSize = this.preferredPixelSize;
        return (preferredPixelSize!=null) ? (Dimension2D) preferredPixelSize.clone() : null;
    }

    /**
     * Set the preferred pixel size in "real world" coordinates. For images, this is the
     * size of image's pixels in units of {@link #getCoordinateSystem}. For other kind of
     * layers, "pixel size" is to be understood as some raisonable resolution for the
     * underlying data. For example an isoline layer may returns the isoline's mean resolution.
     *
     * @param size The preferred pixel size, or <code>null</code> if there is none.
     *
     * @see #getPreferredPixelSize
     * @see #setPreferredArea
     * @see #getCoordinateSystem
     */
    public void setPreferredPixelSize(final Dimension2D size) {
        final Dimension2D oldSize;
        synchronized (getTreeLock()) {
            stroke = null;
            oldSize = preferredPixelSize;
            preferredPixelSize = (size!=null) ? (Dimension2D)size.clone() : null;
        }
        listeners.firePropertyChange("preferredPixelSize", oldSize, size);
    }

    /**
     * Retourne l'ordre <var>z</var> � laquelle cette couche devrait �tre dessin�e.
     * Les couches avec un <var>z</var> �lev� seront dessin�es par dessus celles
     * qui ont un <var>z</var> plus bas. La valeur retourn�e par d�faut est
     * {@link Float#POSITIVE_INFINITY}.
     *
     * @see #setZOrder
     */
    public float getZOrder() {
        return zOrder;
    }

    /**
     * Modifie l'altitude <var>z</var> � laquelle sera dessin�e cette couche. La
     * valeur sp�cifi�e viendra remplacer la valeur par d�faut que retournait
     * normalement {@link #getZOrder}.
     *
     * @throws IllegalArgumentException si <code>zorder</code> est {@link Float#NaN}.
     */
    public void setZOrder(final float zOrder) throws IllegalArgumentException {
        if (Float.isNaN(zOrder)) {
            throw new IllegalArgumentException(String.valueOf(zOrder));
        }
        final float oldZOrder;
        synchronized (getTreeLock()) {
            oldZOrder = this.zOrder;
            if (zOrder == oldZOrder) {
                return;
            }
            this.zOrder = zOrder;
            repaint();
        }
        listeners.firePropertyChange("zOrder", new Float(oldZOrder), new Float(zOrder));
    }

    /**
     * Determines whether this layer should be visible when its container is visible.
     *
     * @return <code>true</code> if the layer is visible, <code>false</code> otherwise.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Sp�cifie si cette couche doit �tre visible ou non. Cette m�thode peut �tre
     * appel�e pour cacher momentan�ment une couche. Elle est aussi appel�e de
     * fa�on syst�matique lorsque cette couche est ajout�e ou retir�e d'un
     * {@link Renderer}:
     *
     * <ul>
     *   <li><code>{@link Renderer#addLayer(RenderedLayer) Renderer.addLayer}(this)</code>
     *       appelera <code>setVisible(true)</code>.</li>
     *   <li><code>{@link Renderer#removeLayer(RenderedLayer) Renderer.removeLayer}(this)</code>
     *       appelera <code>setVisible(false)</code>.</li>
     * </ul>
     */
    public void setVisible(final boolean visible) {
        synchronized (getTreeLock()) {
            if (visible == this.visible) {
                return;
            }
            this.visible = visible;
            repaint();
        }
        listeners.firePropertyChange("visible", !visible, visible);
    }

    /**
     * Indique que cette couche a besoin d'�tre red�ssin�e. La couche ne sera pas redessin�e
     * immediatement, mais seulement un peu plus tard. Cette m�thode <code>repaint()</code>
     * peut �tre appel�e � partir de n'importe quel thread (pas n�cessairement celui de
     * <i>Swing</i>).
     */
    public void repaint() {
        repaint(paintedArea!=null ? paintedArea.getBounds() : null);
    }

    /**
     * Indique qu'une partie de cette couche a besoin d'�tre red�ssin�e.
     * Cette m�thode peut �tre appel�e � partir de n'importe quel thread
     * (pas n�cessairement celui de <i>Swing</i>).
     *
     * @param bounds Coordonn�es (en points) de la partie � redessiner.
     */
    final void repaint(final Rectangle bounds) {
        final Renderer renderer = this.renderer;
        if (renderer == null) {
            return;
        }
        final Component mapPane = renderer.mapPane;
        if (mapPane == null) {
            return;
        }
        if (EventQueue.isDispatchThread()) {
            if (bounds == null) {
                mapPane.repaint();
            } else {
                mapPane.repaint(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        } else {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    repaint(bounds);
                }
            });
        }
    }

    /**
     * Paint this object. This method is invoked by {@link Renderer} every time this layer needs
     * to be repainted. By default, painting is done in the {@linkplain RenderingContext#mapCS
     * rendering coordinate system} (usually "real world" metres). This method is responsible for
     * transformations from its own underlying data CS to the {@linkplain RenderingContext#mapCS
     * rendering CS} if needed. The {@link RenderingContext} object provides informations for such
     * transformations:
     *
     * <ul>
     * <li><p><code>context.{@link RenderingContext#getMathTransform getMathTransform}(underlyingCS,
     *                      context.{@link RenderingContext#mapCS mapCS} )</code><br>
     * Returns a transform from the underlying CS to the rendering CS.</p></li>
     *
     * <li><p><code>context.{@link RenderingContext#getMathTransform getMathTransform}(
     *                      context.{@link RenderingContext#mapCS mapCS},
     *                      context.{@link RenderingContext#textCS textCS} )</code><br>
     * Returns a transform from the rendering CS to the Java2D CS in "dots" units
     * (usually 1/72 of inch). This transformation is zoom dependent.</p></li>
     *
     * <li><p><code>context.{@link RenderingContext#getMathTransform getMathTransform}(
     *                      context.{@link RenderingContext#textCS textCS},
     *                      context.{@link RenderingContext#deviceCS deviceCS} )</code><br>
     * Returns a transform from the Java2D CS to the device CS. This transformation is
     * device dependent, but not zoom sensitive. When the output device is the screen,
     * then this is the identity transform (except if the rendering occurs in a clipped
     * area of the widget).</p></li>
     * </ul>
     *
     * <p>The {@link RenderingContext} object can takes care of configuring {@link Graphics2D}
     * with the right transform for a limited set of particular CS (namely, only CS leading to
     * an {@linkplain AffineTransform affine transform}). This is convenient for switching between
     * {@linkplain RenderingContext#mapCS rendering CS} (the one used for drawing map features)
     * and {@linkplain RenderingContext#textCS Java2D CS} (the one used for rendering texts and
     * labels). Example:</p>
     *
     * <blockquote><pre>
     * &nbsp;Shape paint(RenderingContext context) {
     * &nbsp;    Graphics2D graphics = context.getGraphics();
     * &nbsp;    // <cite>Paint here map features in geographic coordinates (usually m or �)</cite>
     * &nbsp;    context.addPaintedArea(...); // Optional
     * &nbsp;
     * &nbsp;    context.setCoordinateSystem(context.textCS);
     * &nbsp;    // <cite>Write here text or label. Coordinates are in <u>dots</u>.</cite>
     * &nbsp;    context.addPaintedArea(...); // Optional
     * &nbsp;
     * &nbsp;    context.setCoordinateSystem(context.mapCS);
     * &nbsp;    // <cite>Continue here the rendering of map features in geographic coordinates</cite>
     * &nbsp;    context.addPaintedArea(...); // Optional
     * &nbsp;}
     * </pre></blockquote>
     *
     * During the rendering process, implementations are encouraged to declare a (potentially
     * approximative) bounding shape of their painted area with calls to
     * {@link RenderingContext#addPaintedArea(Shape)}. This is an optional operation: providing
     * those hints only help {@link Renderer} to speed up future rendering and events processing.
     *
     * @param  context Information relatives to the rendering context. This object ontains the
     *         {@link Graphics2D} to use and methods for getting {@link MathTransform} objects.
     *         This temporary object will be destroy once the rendering is completed. Consequently,
     *         do not keep a reference to it outside this <code>paint</code> method.
     * @throws TransformException If a coordinate transformation failed during the rendering
     *         process.
     */
    protected abstract void paint(final RenderingContext context) throws TransformException;

    /**
     * Paint this layer and update the {@link #paintedArea} field. If this layer is not visible
     * or if <code>clipBounds</code> doesn't intersect {@link #paintedArea}, then this method do
     * nothing.
     *
     * @param context Information relatives to the rendering context. Will be passed
     *        unchanged to {@link #paint}.
     * @param clipBounds The area to paint, in Java2D coordinates ({@link RenderingContext#textCS}).
     */
    final void update(final RenderingContext context, final Rectangle clipBounds)
            throws TransformException
    {
        assert Thread.holdsLock(getTreeLock());
        if (visible) {
            if (paintedArea==null || clipBounds==null || paintedArea.intersects(clipBounds)) {
                if (stroke == null) {
                    final Dimension2D s = getPreferredPixelSize();
                    if (s != null) {
                        stroke = new BasicStroke((float)XMath.hypot(s.getWidth(), s.getHeight()));
                    } else {
                        stroke = DEFAULT_STROKE;
                    }
                }
                context.getGraphics().setStroke(stroke);
                context.paintedArea = null;
                paint(context);
                this.paintedArea = context.paintedArea;
                context.paintedArea = null;
            }
        }
    }

    /**
     * Hints that this layer might be painted in the near future. Some implementations may
     * spawn a thread to compute the data while others may ignore the hint. The default
     * implementation does nothing.
     *
     * @param  context Information relatives to the rendering context. This object contains
     *         methods for querying the area to be painted in arbitrary coordinate system.
     *         This temporary object will be destroy once the rendering is completed.
     *         Consequently, do not keep a reference to it outside this <code>prefetch</code>
     *         method.
     *
     * @see PlanarImage#prefetchTiles
     */
    protected void prefetch(final RenderingContext context) {
    }

    /**
     * Format a value for the current mouse position. This method doesn't have to format the
     * mouse coordinate (this is {@link MouseCoordinateFormat#format(GeoMouseEvent)} business).
     * Instead, it is invoked for formatting a value at current mouse position. For example a
     * remote sensing image of Sea Surface Temperature (SST) can format the temperature in
     * geophysical units (e.g. "12�C"). The default implementation do nothing and returns
     * <code>false</code>.
     *
     * @param  event The mouse event.
     * @param  toAppendTo The destination buffer for formatting a value.
     * @return <code>true</code> if this method has formatted a value, or <code>false</code>
     *         otherwise. If this method returns <code>true</code>, then the next layers (with
     *         smaller {@linkplain RenderedLayer#getZOrder z-order}) will not be queried.
     *
     * @see MouseCoordinateFormat#format(GeoMouseEvent)
     */
    boolean formatValue(final GeoMouseEvent event, final StringBuffer toAppendTo) {
        return false;
    }

    /**
     * Retourne le texte � afficher dans une bulle lorsque la souris tra�ne sur la couche.
     * L'impl�mentation par d�faut retourne toujours <code>null</code>, ce qui signifie que
     * cette couche n'a aucun texte � afficher (les autres couches seront alors interrog�es).
     * Les classes d�riv�es peuvent red�finir cette m�thode pour retourner un texte apr�s avoir
     * v�rifi� que les coordonn�es de <code>event</code> correspondent bien � un point de la
     * couche.
     *
     * @param  event Coordonn�es du curseur de la souris.
     * @return Le texte � afficher lorsque la souris tra�ne sur la couche.
     *         Ce texte peut �tre nul pour signifier qu'il ne faut rien �crire.
     *
     * @see Renderer#getToolTipText
     */
    String getToolTipText(final GeoMouseEvent event) {
        return null;
    }

    /**
     * Tells if this layer <strong>may</strong> contains the specified point. This method
     * performs only a fast check. Subclasses will have to perform a more exhautive check
     * in their event processing methods. The coordinate system is the
     * {@link RenderingContext#textCS} used the last time this layer was rendered.
     *
     * @param  x <var>x</var> coordinate.
     * @param  y <var>y</var> coordinate.
     * @return <code>true</code> if this layer is visible and may contains the specified point.
     */
    final boolean contains(final int x, final int y) {
        assert Thread.holdsLock(getTreeLock());
        if (visible) {
            final Shape paintedArea = this.paintedArea;
            return (paintedArea==null) || paintedArea.contains(x,y);
        }
        return false;
    }

    /**
     * Invoked every time the {@link Renderer}'s zoom changed. A zoom change require two
     * updates to {@link #paintedArea}:
     * <ul>
     *   <li>Since <code>paintedArea</code> is in {@link RenderingContext#textCS} and since
     *       the transform between the Java2D and the rendering CS is zoom-dependent, a change
     *       of zoom requires a change of <code>paintedArea</code>.</li>
     * <li>Since the zoom change may bring some new area inside the widget bounds, this new
     *     area may need to be rendered and should be added to <code>paintedArea</code>.</li>
     * </ul>
     *
     * Note: The <code>change</code> affine transform must be a change in the <strong>Java2D
     *       coordinate space</strong> ({@link RenderingContext#textCS}). But the transform
     *       given by {@link org.geotools.gui.swing.ZoomPane#fireZoomChange} is in the rendering
     *       coordinate space ({@link RenderingContext#mapCS}). Conversion can be performed
     *       as this:
     *
     *       Lets <var>C</var> by the change in Java2D space, and <var>Z</var> be the
     *       {@linkplain org.geotools.gui.swing.ZoomPane#zoom zoom}.  Then the change
     *       in Java2D space is <code>ZCZ<sup>-1</sup></code>.
     *
     *       Additionnaly, in order to avoir rounding error, it may be safe to expand slightly
     *       the transformed shape. It may be done with the following operations on the change
     *       matrix, where (x,y) is the widget center:
     *       <blockquote><pre>
     *          translate(x, y);           // final translation
     *          scale(1.00001, 1.00001);   // scale around anchor
     *          translate(-x, -y);         // translate anchor to origin
     *       </pre></blockquote>
     *
     * @param change The zoom <strong>change</strong> in <strong>Java2D</strong> coordinate
     *        system, or <code>null</code> if unknow. If <code>null</code>, then this layer
     *        will be fully redrawn during the next rendering.
     */
    final void zoomChanged(final AffineTransform change) {
        assert Thread.holdsLock(getTreeLock());
        if (paintedArea == null) {
            return;
        }
        if (change!=null && renderer!=null) {
            final Component mapPane = renderer.mapPane;
            if (mapPane != null) {
                final Area newArea = new Area(mapPane.getBounds());
                newArea.subtract(newArea.createTransformedArea(change));
                final Area area = (paintedArea instanceof Area) ?    (Area)paintedArea
                                                                : new Area(paintedArea);
                area.transform(change);
                area.add(newArea);
                paintedArea = area;
                return;
            }
        }
        paintedArea = null;
    }

    /**
     * Add a property change listener to the listener list.  The listener is registered for
     * all properties. For example, methods {@link #setVisible}, {@link #setZOrder},
     * {@link #setPreferredArea} and {@link #setPreferredPixelSize} will fire
     * <code>"visible"</code>, <code>"zOrder"</code>, <code>"preferredArea"</code>
     * and <code>"preferredPixelSize"</code> change events.
     *
     * @param listener The property change listener to be added
     */
    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        listeners.addPropertyChangeListener(listener);
    }

    /**
     * Remove a property change listener from the listener list.
     * This removes a <code>PropertyChangeListener</code> that
     * was registered for all properties.
     *
     * @param listener The property change listener to be removed
     */
    public void removePropertyChangeListener(final PropertyChangeListener listener) {
        listeners.removePropertyChangeListener(listener);
    }

    /**
     * Returns the lock for synchronisation.
     */
    protected final Object getTreeLock() {
        final Renderer renderer = this.renderer;
        return (renderer!=null) ? (Object)renderer : (Object)this;
    }

    /**
     * Efface les donn�es qui avaient �t� conserv�es dans une cache interne. L'appel
     * de cette m�thode permettra de lib�rer un peu de m�moire � d'autres fins. Elle
     * sera appel�e lorsque qu'on aura d�termin� que la couche <code>this</code>  ne
     * sera plus affich�e avant un certain temps.  Cette m�thode ne doit pas changer
     * le param�trage de cette couche; son seul impact sera de rendre le prochain
     * tra�age un peu plus lent.
     */
    void clearCache() {
        assert Thread.holdsLock(getTreeLock());
        paintedArea = null;
        stroke      = null;
    }

    /**
     * Provides a hint that a layer will no longer be accessed from a reference in user
     * space. The results are equivalent to those that occur when the program loses its
     * last reference to this layer, the garbage collector discovers this, and finalize
     * is called. This can be used as a hint in situations where waiting for garbage
     * collection would be overly conservative.
     * <br><br>
     * The results of referencing a layer after a call to <code>dispose()</code> are undefined.
     * However, invoking this method more than once is safe.  Note that this method is invoked
     * automatically by {@link Renderer#dispose}, but not from any {@link Renderer#removeLayer
     * remove(...)} method (in order to allow moving layers between different renderers).
     *
     * @see Renderer#dispose
     * @see PlanarImage#dispose
     */
    public void dispose() {
        synchronized (getTreeLock()) {
            if (renderer != null) {
                renderer.removeLayer(this);
            }
            clearCache();
            preferredArea      = null;
            preferredPixelSize = null;
            visible            = false;
            zOrder             = Float.POSITIVE_INFINITY;
            coordinateSystem   = GeographicCoordinateSystem.WGS84;
            final PropertyChangeListener[] list = listeners.getPropertyChangeListeners();
            for (int i=list.length; --i>=0;) {
                listeners.removePropertyChangeListener(list[i]);
            }
        }
    }
}
