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
import java.awt.Stroke;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Dimension2D;
import java.awt.geom.AffineTransform;

// User interface and Java2D rendering
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.event.EventListenerList;

// Miscellaneous J2SE
import java.util.Locale;
import java.util.EventListener;
import java.util.logging.Level;
import java.util.logging.LogRecord;

// Geotools dependencies
import org.geotools.cs.CoordinateSystem;
import org.geotools.ct.TransformException;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.resources.Utilities;
import org.geotools.resources.CTSUtilities;
import org.geotools.resources.XAffineTransform;
import org.geotools.resources.renderer.Resources;
import org.geotools.resources.renderer.ResourceKeys;


/**
 * Base class for layers to be rendered using the Java2D renderer. Each layer can use
 * its own {@linkplain CoordinateSystem coordinate system} (CS) for its underlying data.
 * Transformations to the {@link RendereringContext#mapCS rendering coordinate system}
 * are performed on the fly at rendering time.
 *
 * @version $Id: RenderedObject.java,v 1.3 2003/01/22 23:06:49 desruisseaux Exp $
 * @author Martin Desruisseaux
 */
public abstract class RenderedObject {
    /**
     * Minimum amout of milliseconds during rendering before logging a message.
     * A message will be logged only if rendering take longer. This is used for
     * tracking down performance bottleneck.
     */
    private static final int TIME_THRESHOLD = 200;

    /**
     * The component where to send {@link Component#repaint()} request,
     * or <code>null</code> if none.
     */
    transient Component mapPane;

    /**
     * Forme g�om�trique englobant la r�gion dans laquelle la couche a �t� dessin�e lors du
     * dernier appel de la m�thode {@link #paint}.  Les coordonn�es de cette r�gion doivent
     * �tre en exprim�es en coordonn�es du p�riph�rique ({@link RenderingContext#deviceCS}).
     * La valeur <code>null</code> signifie qu'on peut consid�rer que cette couche occupe la
     * totalit� de la surface dessinable.
     */
    private transient Shape paintedArea;

    /**
     * Syst�me de coordonn�es utilis� pour cette couche. Les m�thodes {@link #getPreferredArea}
     * et {@link #setPreferredArea} utilisent ce syst�me de coordonn�es. Ce champ ne doit jamais
     * �tre nul.
     */
    private CoordinateSystem coordinateSystem = GeographicCoordinateSystem.WGS84;

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
    private Stroke stroke;

    /**
     * Indique si cette couche est visible. Les couches sont invisibles par d�faut. L'appel
     * de {@link Renderer#add} appelera syst�matiquement <code>setVisible(true)</code>.
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
     * The tools for this layer, or <code>null</code> if none.
     *
     * @see #getTools
     * @see #setTools
     */
    private Tools tools;

    /**
     * Liste des objets int�ress�s � �tre inform�s des
     * changements apport�s � cet objet <code>RenderedObject</code>.
     */
    private final EventListenerList listenerList = new EventListenerList();

    /**
     * Construct a new <code>RenderedObject</code> layer. The {@linkplain #getCoordinateSystem
     * coordinate system} default to {@linkplain GeographicCoordinateSystem#WGS84 WGS 1984}
     * and the {@linkplain #getZOrder z-order} default to positive infinity (i.e. this layer
     * is drawn on top of everything else). Subclasses should invokes <code>setXXX</code>
     * methods in order to define properly the properties for this layer.
     *
     * @see #setZOrder
     * @see #setCoordinateSystem
     * @see #setPreferredArea
     * @see #setPreferredPixelSize
     */
    public RenderedObject() {
    }

    /**
     * Returns this layer's name. Default implementation returns class name and its z-order.
     *
     * @param  locale The desired locale, or <code>null</code> for a default locale.
     * @return This layer's name.
     *
     * @see Renderer#getLocale
     * @see Component#getLocale
     */
    public String getName(final Locale locale) {
        return Utilities.getShortClassName(this) + '[' + getZOrder() + ']';
    }

    /**
     * Returns the two-dimensional coordinate system for the underlying data.  This
     * coordinate system is used by most methods like {@link #getPreferredArea} and
     * {@link #getPreferredPixelSize}.
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
     * Set the coordinate system for the underlying data. This method is usually invoked
     * only once, at construction time.  If the specified coordinate system has more than
     * two dimensions, then it must be a {@link CompoundCoordinateSystem} with a two
     * dimensional {@link CompoundCoordinateSystem#getHeadCS headCS}.
     *
     * @param  cs The coordinate system.
     * @throws TransformException If <code>cs</code> can't be reduced to a two-dimensional
     *         coordinate system., or if data can't be transformed for some other reason.
     */
    public synchronized void setCoordinateSystem(final CoordinateSystem cs)
            throws TransformException
    {
        final CoordinateSystem oldCS = coordinateSystem;
        coordinateSystem = CTSUtilities.getCoordinateSystem2D(cs);
        firePropertyChange("coordinateSystem", oldCS, cs);
    }

    /**
     * Retourne les coordonn�es g�ographiques de cette couche. Les coordonn�es retourn�es ne sont
     * pas oblig�es d'englober toute la couche (quoique ce soit souvent le cas).  Elles indiquent
     * plut�t la partie de la couche que l'on souhaite voir appara�tre dans le zoom par d�faut.
     * Le rectangle retourn� sera exprim� selon le syst�me de coordonn�es retourn� par
     * {@link #getCoordinateSystem}. Si cette couche n'a pas de limites g�ographiques bien
     * d�finies (par exemple si elle n'est qu'une l�gende ou l'�chelle de la carte), alors
     * cette m�thode peut retourner <code>null</code>.
     *
     * @see #setPreferredArea
     * @see #getPreferredPixelSize
     * @see #getCoordinateSystem
     */
    public final Rectangle2D getPreferredArea() {
        final Rectangle2D preferredArea = this.preferredArea;
        return (preferredArea!=null) ? (Rectangle2D) preferredArea.clone() : null;
    }

    /**
     * Modifie les coordonn�es g�ographiques de cette couche. L'appel de cette m�thode ne modifie
     * par le g�or�f�rencement; elle affecte simplement la r�gion qui sera affich�e par d�faut
     * dans une fen�tre.
     *
     * @see #getPreferredArea
     * @see #setPreferredPixelSize
     * @see #getCoordinateSystem
     */
    public synchronized void setPreferredArea(final Rectangle2D area) {
        paintedArea = null;
        firePropertyChange("preferredArea", preferredArea,
                           preferredArea=(area!=null) ? (Rectangle2D) area.clone() : null);
    }

    /**
     * Returns the preferred pixel size for a close
     * zoom, or <code>null</code> if there is none.
     *
     * @see #setPreferredPixelSize
     * @see #getPreferredArea
     * @see #getCoordinateSystem
     */
    public final Dimension2D getPreferredPixelSize() {
        final Dimension2D preferredPixelSize = this.preferredPixelSize;
        return (preferredPixelSize!=null) ? (Dimension2D) preferredPixelSize.clone() : null;
    }

    /**
     * Set the preferred pixel size for a close zoom. For images, the preferred pixel
     * size is the image's pixel size (in units of {@link #getCoordinateSystem}). For
     * other kind of object, this "pixel" size should be some raisonable resolution
     * for the underlying data. For example a layer drawing an isoline may use the
     * isoline's mean resolution.
     *
     * @param size The preferred pixel size, or <code>null</code> if there is none.
     *
     * @see #getPreferredPixelSize
     * @see #setPreferredArea
     * @see #getCoordinateSystem
     */
    public synchronized void setPreferredPixelSize(final Dimension2D size) {
        stroke = null;
        firePropertyChange("preferredPixelSize", preferredPixelSize,
                           preferredPixelSize=(size!=null) ? (Dimension2D)size.clone() : null);
    }

    /**
     * Retourne l'ordre <var>z</var> � laquelle cette couche devrait �tre dessin�e.
     * Les couches avec un <var>z</var> �lev� seront dessin�es par dessus celles
     * qui ont un <var>z</var> plus bas. La valeur retourn�e par d�faut est
     * {@link Float#POSITIVE_INFINITY}.
     *
     * @see #setZOrder
     */
    public final float getZOrder() {
        return zOrder;
    }

    /**
     * Modifie l'altitude <var>z</var> � laquelle sera dessin�e cette couche. La
     * valeur sp�cifi�e viendra remplacer la valeur par d�faut que retournait
     * normalement {@link #getZOrder}.
     *
     * @throws IllegalArgumentException si <code>zorder</code> est {@link Float#NaN}.
     */
    public synchronized void setZOrder(final float zOrder) throws IllegalArgumentException {
        if (!Float.isNaN(zOrder)) {
            final float oldZOrder = this.zOrder;
            if (zOrder != oldZOrder) {
                this.zOrder = zOrder;
                repaint();
                firePropertyChange("zOrder", new Float(oldZOrder), new Float(zOrder));
            }
        } else {
            throw new IllegalArgumentException(String.valueOf(zOrder));
        }
    }

    /**
     * Returns the tools for this layer, or <code>null</code> if none.
     * Tools are used for processing mouse and keyboard events. Tools
     * may be changed at anytime, for example according some user selection.
     *
     * @see Tools#getToolTipText
     * @see Tools#getPopupMenu
     * @see Tools#mouseClicked
     */
    public final Tools getTools() {
        return tools;
    }

    /**
     * Set the tools for this layer.
     *
     * @param tools The new tools, or <code>null</code> for removing any set of tools.
     */
    public synchronized void setTools(final Tools tools) {
        firePropertyChange("tools", this.tools, this.tools=tools);
    }

    /**
     * Determines whether this layer should be visible when its container is visible.
     *
     * @return <code>true</code> if the layer is visible, <code>false</code> otherwise.
     */
    public final boolean isVisible() {
        return visible;
    }

    /**
     * Sp�cifie si cette couche doit �tre visible ou non. Cette m�thode peut �tre
     * appel�e pour cacher momentan�ment une couche. Elle est aussi appel�e de
     * fa�on syst�matique lorsque cette couche est ajout�e ou retir�e d'un
     * {@link Renderer}:
     *
     * <ul>
     *   <li><code>{@link Renderer#add Renderer.add}(this)</code>
     *       appelera <code>setVisible(true)</code>. Les classes d�riv�es peuvent
     *       profiter de cette sp�cification pour s'enregistrer aupr�s de {@link
     *       org.geotools.gui.swing.MapPane} comme �tant int�ress�es � suivre les
     *       mouvements de la souris par exemple.</li>
     *   <li><code>{@link Renderer#remove Renderer.remove}(this)</code>
     *       appelera <code>setVisible(false)</code>. Les classes d�riv�es peuvent
     *       profiter de cette sp�cification pour d�clarer �
     *       {@link org.geotools.gui.swing.MapPane} qu'elles ne sont plus
     *       int�ress�es � suivre les mouvements de la souris par exemple.</li>
     * </ul>
     */
    public synchronized void setVisible(final boolean visible) {
        if (visible != this.visible) {
            this.visible = visible;
            repaint();
            final Boolean before, after;
            if (visible) {
                before = Boolean.FALSE;
                after  = Boolean.TRUE;
            } else {
                before = Boolean.TRUE;
                after  = Boolean.FALSE;
            }
            firePropertyChange("visible", before, after);
        }
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
        final Component mapPane = this.mapPane;
        if (mapPane != null) {
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
    }

    /**
     * Paint this object. This method is invoked by {@link Renderer} every time this layer needs
     * to be repainted. By default, painting is done in the {@linkplain RendereringContext#mapCS
     * rendering coordinate system} (usually "real world" metres).    This method is responsible
     * for transformations from its own  {@linkplain #getCoordinateSystem underlying CS}  to the
     * {@linkplain RendereringContext#mapCS rendering CS} if needed. The {@link RenderingContext}
     * object provides informations for such transformations:
     *
     * <ul>
     * <li><p><code>context.{@link RenderingContext#getMathTransform getMathTransform}(
     *                      {@link #getCoordinateSystem getCoordinateSystem}(),
     *                      context.{@link RenderingContext#mapCS mapCS} )</code><br>
     * Returns a transform from this layer's CS to the rendering CS.</p></li>
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
     * then this is the identity transform.</p></li>
     *
     * <p>The {@link RenderingContext} object can takes care of configuring {@link Graphics2D}
     * with the right transform for a limited set of particular CS (namely, only CS leading to
     * an {@linkplain AffineTransform affine transform}). This convenient for switching between
     * {@linkplain RenderingContext#mapCS rendering CS} (the one used for drawing map features)
     * and {@linkplain RenderingContext#textCS Java2D CS} (the one used for rendering texts and
     * labels). Example:</p>
     *
     * <blockquote><pre>
     * &nbsp;Shape paint(RenderingContext context) {
     * &nbsp;    Graphics2D graphics = context.graphics;
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
     * those hints help {@link Renderer} to speed up future rendering and events processing.
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
     * @param clipBounds The area to paint, in device coordinates
     *        ({@link RenderingContext#deviceCS}).
     */
    final synchronized void update(final RenderingContext context,
                                   final Rectangle clipBounds)
            throws TransformException
    {
        if (visible) {
            if (paintedArea==null || clipBounds==null || paintedArea.intersects(clipBounds)) {
                long time = System.currentTimeMillis();
                context.paintedArea = null;
                paint(context);
                if (context.textCS == context.deviceCS) {
                    /*
                     * Keeps the bounding shape of the rendered area  only if rendering
                     * was performed on the screen or any other device with an identity
                     * default transform. This is usually not the case during printing.
                     */
                    this.paintedArea = context.paintedArea;
                }
                /*
                 * If this layer took a long time to renderer, log a message.
                 */
                time = System.currentTimeMillis()-time;
                if (time > TIME_THRESHOLD) {
                    final LogRecord record = Resources.getResources(null).getLogRecord(Level.FINEST,
                                             ResourceKeys.PAINTING_$2, getName(null),
                                             new Double(time/1000.0));
                    record.setSourceClassName(Utilities.getShortClassName(this));
                    record.setSourceMethodName("paint");
                    Renderer.LOGGER.log(record);
                }
            }
        }
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
    }

    /**
     * Ajoute un objet int�ress� � �tre inform� chaque fois qu'une propri�t� de cette
     * couche <code>RenderedObject</code> change. Les m�thodes {@link #setVisible}
     * et {@link #setZOrder} en particulier tiendront ces objets au courant des
     * changements qu'ils font.
     */
    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        listenerList.add(PropertyChangeListener.class, listener);
    }

    /**
     * Retire un objet qui n'est plus int�ress� � �tre inform� chaque fois
     * que change une propri�t� de cette couche <code>RenderedObject</code>.
     */
    public void removePropertyChangeListener(final PropertyChangeListener listener) {
        listenerList.remove(PropertyChangeListener.class, listener);
    }

    /**
     * Pr�vient tous les objets int�ress�s que l'�tat de cette couche a chang�.
     * La m�thode {@link PropertyChangeListener#propertyChange} de tous les
     * listeners sera appel�e, sauf si <code>oldValue</code> et
     * <code>newValue</code> sont identiques.
     *
     * @param propertyName nom de la propri�t� qui change (par exemple "preferredArea"} ou "zOrder"}).
     * @param oldValue Ancienne valeur (avant le changement).
     * @param newValue Nouvelle valeur (apr�s le changement).
     */
    protected void firePropertyChange(final String propertyName,
                                      final Object oldValue, final Object newValue)
    {
        if (oldValue!=newValue && (oldValue==null || !oldValue.equals(newValue))) {
            PropertyChangeEvent event = null;
            final Object[]  listeners = listenerList.getListenerList();
            for (int i=listeners.length; (i-=2)>=0;) {
                if (listeners[i] == PropertyChangeListener.class) {
                    if (event == null) {
                        event=new PropertyChangeEvent(this, propertyName, oldValue, newValue);
                    }
                    try {
                        ((PropertyChangeListener) listeners[i+1]).propertyChange(event);
                    } catch (RuntimeException exception) {
                        Utilities.unexpectedException("fr.ird.map", "RenderedObject",
                                                      "firePropertyChange", exception);
                    }
                }
            }
        }
    }

    /**
     * Lib�re les ressources occup�es par cette couche. Cette m�thode est appel�e automatiquement
     * lorsqu'il a �t� d�termin� que cette couche sera bient�t d�truite.   Elle permet de lib�rer
     * les ressources plus rapidement que si l'on attend que le ramasse-miettes fasse son travail.
     */
    protected void dispose() {
        paintedArea = null;
        final Object[] listeners = listenerList.getListenerList();
        for (int i=listeners.length; (i-=2)>=0;) {
            listenerList.remove((Class)listeners[i-2], (EventListener)listeners[i-1]);
        }
    }

    /**
     * Invoked every time the {@link Renderer}'s zoom changed. A zoom change require two
     * updates to {@link #paintedArea}:
     * <ul>
     *   <li>Since <code>paintedArea</code> is in {@link RenderingContext#deviceCS} and since
     *       the transform between the Java2D and the rendering CS is zoom-dependent, a change
     *       of zoom requires a change of <code>paintedArea</code>.</li>
     * <li>Since the zoom change may bring some new area inside the widget bounds, this new
     *     area may need to be rendered and should be added to <code>paintedArea</code>.</li>
     * </ul>
     *
     * Note: The <code>change</code> affine transform must be a change in the <strong>device
     *       coordinate space</strong> ({@link RenderingContext#deviceCS}). But the transform
     *       given by {@link org.geotools.gui.swing.ZoomPane#fireZoomChange} is in the rendering
     *       coordinate space ({@link RenderingContext#mapCS}). Conversion can be performed
     *       as this:
     *
     *       Lets <var>C</var> by the change in rendering space, and <var>Z</var> be the
     *       {@linkplain org.geotools.gui.swing.ZoomPane#zoom zoom}.  Then the change in
     *       device space is <code>ZCZ<sup>-1</sup></code>.
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
     * @param change The zoom <strong>change</strong> in <strong>device</strong> coordinate
     *        system, or <code>null</code> if unknow. If <code>null</code>, then this layer
     *        will be fully redrawn during the next rendering.
     */
    final void zoomChanged(final AffineTransform change) {
        final Shape paintedArea = this.paintedArea;
        if (paintedArea != null) {
            if (change!=null && mapPane!=null) {
                final Area newArea = new Area(mapPane.getBounds());
                newArea.subtract(newArea.createTransformedArea(change));
                final Area area = (paintedArea instanceof Area) ?    (Area)paintedArea
                                                                : new Area(paintedArea);
                area.transform(change);
                area.add(newArea);
                this.paintedArea = area;
            } else {
                this.paintedArea = null;
            }
        }
    }

    /**
     * Tells if this layer <strong>may</strong> contains the specified point. This method
     * performs only a fast check. Subclasses will have to perform a more exhautive check
     * in their {@link #mouseClicked}, {@link #getPopupMenu} and similar methods. The
     * coordinate system is the {@link RenderingContext#textCS} used the last time this
     * layer was rendered.
     *
     * @param  x <var>x</var> coordinate.
     * @param  y <var>y</var> coordinate.
     * @return <code>true</code> if this layer is visible and may contains the specified point.
     */
    final boolean contains(final int x, final int y) {
        if (visible) {
            final Shape paintedArea = this.paintedArea;
            return (paintedArea==null) || paintedArea.contains(x,y);
        }
        return false;
    }

    /**
     * Indique si la r�gion g�ographique <code>big</code> contient enti�rement la sous-r�gion
     * <code>small</code> sp�cifi�e. Un cas particuluer survient si un ou plusieurs bords de
     * <code>small</code> co�ncide avec les bords correspondants de <code>big</code>. L'argument
     * <code>edge</code> indique si on consid�re qu'il y a inclusion ou pas dans ces circonstances.
     *
     * @param big   R�gion g�ographique dont on veut v�rifier s'il contient une sous-r�gion.
     * @param small Sous-r�gion g�ographique dont on veut v�rifier l'inclusion dans <code>big</code>.
     * @param edge <code>true</code> pour consid�rer qu'il y a inclusion si ou ou plusieurs bords
     *        de <code>big</code> et <code>small</code> con�ncide, ou <code>false</code> pour exiger
     *        que <code>small</code> ne touche pas aux bords de <code>big</code>.
     */
    static boolean contains(final Rectangle2D big, final Rectangle2D small, final boolean edge) {
        return edge ? (small.getMinX()>=big.getMinX() && small.getMaxX()<=big.getMaxX() && small.getMinY()>=big.getMinY() && small.getMaxY()<=big.getMaxY()):
                      (small.getMinX()> big.getMinX() && small.getMaxX()< big.getMaxX() && small.getMinY()> big.getMinY() && small.getMaxY()< big.getMaxY());
    }

    /**
     * Agrandi (si n�cessaire) une r�gion g�ographique en fonction de l'ajout, la supression ou
     * la modification des coordonn�es d'une sous-r�gion. Cette m�thode est appel�e lorsque les
     * coordonn�es de la sous-r�gion <code>oldSubArea</code> ont chang�es pour devenir
     * <code>newSubArea</code>. Si ce changement s'est traduit par un agrandissement de
     * <code>area</code>, alors le nouveau rectangle agrandi sera retourn�. Si le changement
     * n'a aucun impact sur <code>area</code>, alors <code>area</code> sera retourn� tel quel.
     * Si le changement PEUT avoir diminu� la dimension de <code>area</code>, alors cette m�thode
     * retourne <code>null</code> pour indiquer qu'il faut recalculer <code>area</code> � partir
     * de z�ro.
     *
     * @param  area       R�gion g�ographique qui pourrait �tre affect�e par le changement de
     *                    coordonn�es d'une sous-r�gion. En aucun cas ce rectangle <code>area</code>
     *                    ne sera directement modifi�. Si une modification est n�cessaire, elle sera
     *                    faite sur un clone qui sera retourn�. Cet argument peut �tre
     *                    <code>null</code> si aucune r�gion n'�tait pr�c�demment d�finie.
     * @param  oldSubArea Anciennes coordonn�es de la sous-r�gion, ou <code>null</code> si la
     *                    sous-r�gion n'existait pas avant l'appel de cette m�thode. Ce rectangle
     *                    ne sera jamais modifi� ni retourn�.
     * @param  newSubArea Nouvelles coordonn�es de la sous-r�gion, ou <code>null</code> si la
     *                    sous-r�gion est supprim�e. Ce rectangle ne sera jamais modifi� ni
     *                    retourn�.
     *
     * @return Un rectangle contenant les coordonn�es mises-�-jour de <code>area</code>, si cette
     *         mise-�-jour a pu se faire. Si elle n'a pas pu �tre faite faute d'informations, alors
     *         cette m�thode retourne <code>null</code>. Dans ce dernier cas, il faudra recalculer
     *         <code>area</code> � partir de z�ro.
     */
    static Rectangle2D changeArea(Rectangle2D area,
                                  final Rectangle2D oldSubArea,
                                  final Rectangle2D newSubArea)
    {
        if (area == null) {
            /*
             * Si aucune r�gion n'avait �t� d�finie auparavant. La sous-r�gion
             * "newSubArea" repr�sente donc la totalit� de la nouvelle r�gion
             * "area". On construit un nouveau rectangle plut�t que de faire un
             * clone pour �tre certain d'avoir un type d'une pr�cision suffisante.
             */
            if (newSubArea != null) {
                area = new Rectangle2D.Double();
                area.setRect(newSubArea);
            }
            return area;
        }
        if (newSubArea == null) {
            /*
             * Une sous-r�gion a �t� supprim�e ("newSubArea" est nulle). Si la sous-r�gion supprim�e ne
             * touchait pas au bord de "area",  alors sa suppression ne peut pas avoir diminu�e "area":
             * on retournera alors area. Si au contraire "oldSubArea" touchait au bord de "area", alors
             * on ne sait pas si la suppression de "oldSubArea" a diminu� "area".  Il faudra recalculer
             * "area" � partir de z�ro, ce que l'on indique en retournant NULL.
             */
            if (               oldSubArea==null  ) return area;
            if (contains(area, oldSubArea, false)) return area;
            return null;
        }
        if (oldSubArea != null) {
            /*
             * Une sous-r�gion a chang�e ("oldSubArea" est devenu "newSubArea"). Si on d�tecte que ce
             * changement PEUT diminuer la superficie totale de "area", il faudra recalculer "area" �
             * partir de z�ro pour en �tre sur. On retourne donc NULL.  Si au contraire la superficie
             * totale de "area" ne peut pas avoir diminu�e, elle peut avoir augment�e. Ce calcul sera
             * fait � la fin de cette m�thode, qui poursuit son cours.
             */
            double t;
            if (((t=oldSubArea.getMinX()) <= area.getMinX() && t < newSubArea.getMinX()) ||
                ((t=oldSubArea.getMaxX()) >= area.getMaxX() && t > newSubArea.getMaxX()) ||
                ((t=oldSubArea.getMinY()) <= area.getMinY() && t < newSubArea.getMinY()) ||
                ((t=oldSubArea.getMaxY()) >= area.getMaxY() && t > newSubArea.getMaxY()))
            {
                return null; // Le changement PEUT avoir diminu� "area".
            }
        }
        /*
         * Une nouvelle sous-r�gion est ajout�e. Si elle �tait d�j�
         * enti�rement comprise dans "area", alors son ajout n'aura
         * aucun impact sur "area" et peut �tre ignor�.
         */
        if (!contains(area, newSubArea, true)) {
            // Cloner est n�cessaire pour que "firePropertyChange"
            // puisse conna�tre l'ancienne valeur de "area".
            area = (Rectangle2D) area.clone();
            area.add(newSubArea);
        }
        return area;
    }
}
