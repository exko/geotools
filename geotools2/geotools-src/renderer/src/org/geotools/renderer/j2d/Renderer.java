/*
 * Geotools 2 - OpenSource mapping toolkit
 * (C) 2003, Geotools Project Managment Committee (PMC)
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

// J2SE dependencies
import java.awt.Shape;
import java.awt.Image;
import java.awt.Frame;
import java.awt.Dialog;
import java.awt.Rectangle;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.GraphicsConfiguration;
import java.awt.IllegalComponentStateException;
import java.awt.image.VolatileImage;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import javax.swing.JInternalFrame;
import javax.swing.ToolTipManager;
import javax.swing.JComponent;
import java.util.Map;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.Locale;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Java Advanced Imaging
import javax.media.jai.GraphicsJAI;
import javax.media.jai.PlanarImage; // For Javadoc

// Geotools dependencies
import org.geotools.cs.Ellipsoid;
import org.geotools.cs.AxisInfo;
import org.geotools.cs.AxisOrientation;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.FittedCoordinateSystem;
import org.geotools.cs.CompoundCoordinateSystem;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.ct.MathTransform;
import org.geotools.ct.MathTransform2D;
import org.geotools.ct.MathTransformFactory;
import org.geotools.ct.CoordinateTransformation;
import org.geotools.ct.CannotCreateTransformException;
import org.geotools.ct.NoninvertibleTransformException;
import org.geotools.ct.CoordinateTransformationFactory;
import org.geotools.ct.TransformException;
import org.geotools.gp.GridCoverageProcessor;
import org.geotools.units.UnitException;
import org.geotools.units.Unit;
import org.geotools.util.RangeSet;
import org.geotools.resources.XArray;
import org.geotools.resources.Utilities;
import org.geotools.resources.CTSUtilities;
import org.geotools.resources.XDimension2D;
import org.geotools.resources.XRectangle2D;
import org.geotools.resources.XAffineTransform;
import org.geotools.resources.GraphicsUtilities;
import org.geotools.resources.renderer.Resources;
import org.geotools.resources.renderer.ResourceKeys;
import org.geotools.renderer.Renderer2D;


/**
 * A renderer for drawing map objects into a {@link Graphics2D}. A newly constructed
 * <code>Renderer</code> is initially empty. To make something appears, {@link RenderedLayer}s
 * must be added using one of <code>addLayer(...)</code> methods. The visual content depends of
 * the <code>RenderedLayer</code> subclass. It may be an isoline ({@link RenderedGeometries}),
 * a remote sensing image ({@link RenderedGridCoverage}), a set of arbitrary marks
 * ({@link RenderedMarks}), a map scale ({@link RenderedMapScale}), etc.
 *
 * @version $Id: Renderer.java,v 1.34 2003/08/10 20:27:48 desruisseaux Exp $
 * @author Martin Desruisseaux
 */
public class Renderer implements Renderer2D {
    /**
     * The logger for the Java2D renderer module.
     */
    static final Logger LOGGER = Logger.getLogger("org.geotools.renderer.j2d");

    /**
     * Small value for avoiding rounding error.
     */
    private static final double EPS = 1E-6;



    //////////////////////////////////////////////////////////////////
    ////////                                                  ////////
    ////////         RenderedLayers (a.k.a "Layers")          ////////
    ////////                                                  ////////
    //////////////////////////////////////////////////////////////////
    /**
     * Objet utilis� pour comparer deux objets {@link RenderedLayer}.
     * Ce comparateur permettra de classer les {@link RenderedLayer}
     * par ordre croissant d'ordre <var>z</var>.
     */
    private static final Comparator COMPARATOR = new Comparator() {
        public int compare(final Object layer1, final Object layer2) {
            return Float.compare(((RenderedLayer)layer1).getZOrder(),
                                 ((RenderedLayer)layer2).getZOrder());
        }
    };

    /**
     * An empty list of layers.
     */
    private static final RenderedLayer[] EMPTY = new RenderedLayer[0];

    /**
     * The set of {@link RenderedLayer} to display. Named "layers" here because each
     * {@link RenderedLayer} has its own <var>z</var> value and layer are painted in
     * increasing <var>z</var> order (i.e. layers with a hight <var>z</var> value are
     * painted on top of layers with a low <var>z</var> value).
     */
    private RenderedLayer[] layers;

    /**
     * The number of valid elements in {@link #layers}.
     */
    private int layerCount;

    /**
     * Tells if elements in {@link #layers} are sorted in increasing <var>z</var> value.
     * If <code>false</code>, then <code>Arrays.sort(layers, COMPARATOR)</code> need to
     * be invoked.
     */
    private boolean layerSorted;

    /**
     * The offscreen buffers. There is one {@link VolatileImage} for each range in the
     * {@link #offscreenZRanges} set. If non-null, this array length must be equals to
     * the size of {@link #offscreenZRanges}.
     *
     * @see #setOffscreenBuffered
     */
    private transient VolatileImage[] offscreenBuffers;

    /**
     * The ranges of {@linkplain RenderedLayer#getZOrder z-order} for which to create an
     * offscreen buffer. There is one offscreen buffer (usually a {@link VolatileImage})
     * for each range of z-orders.
     *
     * @see #setOffscreenBuffered
     */
    private RangeSet offscreenZRanges;



    //////////////////////////////////////////////////////////////////
    ////////                                                  ////////
    ////////        Coordinate systems and transforms         ////////
    ////////                                                  ////////
    //////////////////////////////////////////////////////////////////
    /**
     * Axis orientations in the Java2D space. Coordinates are in "dots" (about 1/72 of inch),
     * <var>x</var> values increasing right and <var>y</var> values increasing down. East and
     * South directions are relative to the screen (like {@link java.awt.BorderLayout}), not
     * geographic directions.
     */
    private static final AxisInfo[] TEXT_AXIS = new AxisInfo[] {
        new AxisInfo("Column", AxisOrientation.EAST),
        new AxisInfo("Line",   AxisOrientation.SOUTH)
    };

    /**
     * The coordinate systems for rendering. This object contains the following:
     * <ul>
     *   <li>The "real world" coordinate system  ({@link RenderingContext#mapCS mapCS}).
     *       Should never be <code>null</code>, since it is the viewer coordinate system
     *       as returned by {@link #getCoordinateSystem}.</li>
     *   <li>The Java2D coordinate system ({@link RenderingContext#textCS textCS}),  which
     *       is rendering-dependent.   This CS must be the one used for the last rendering
     *       ({@link GeoMouseEvent rely on it}), or <code>null</code> if the map has never
     *       been rendered yet.</li>
     *   <li>The device coordinate system ({@link RenderingContext#deviceCS deviceCS}), which
     *       is rendering-dependent.  This CS must be the one used for the last rendering, or
     *       <code>null</code> if the map has never been rendered yet.</li>
     * </ul>
     *
     * @see RenderingContext#mapCS
     * @see RenderingContext#textCS
     * @see RenderingContext#deviceCS
     */
    private RenderingContext context;

    /**
     * The scale factor from the last rendering, or <code>null</code> if unknow.
     * This scale factor is computed at rendering time after a zoom change. All
     * registered listeners will be notified of any scale change.
     */
    private Float scaleFactor;

    /**
     * The affine transform from the units used in {@link RenderingContext#mapCS mapCS} to
     * "dots" units. A dots is equals to 1/72 of inch. This transform is basically nothing
     * else than a unit conversion;  we still in "real world" CS  (not yet the screen CS).
     * This transform is used as a convenient step in the computation of a realistic scale
     * factor.
     */
    private final AffineTransform normalizeToDots = new AffineTransform();

    /**
     * The affine transform from {@link RenderingContext#mapCS mapCS} to
     * {@link RenderingContext#textCS textCS} used in the last rendering.
     */
    private final AffineTransform mapToText = new AffineTransform();

    /**
     * A set of {@link MathTransform}s from various source CS. The target CS must be
     * {@link RenderingContext#mapCS} for all entries. Keys are source CS.  This map
     * is used only in order to avoid the costly call to
     * {@link CoordinateTransformationFactory#createFromCoordinateSystems} as much as
     * possible. If a transformation is not available in this collection, then the usual
     * {@link #factory} will be used.
     */
    private final Map transforms = new WeakHashMap();

    /**
     * The factory to use for creating {@link CoordinateTransformation} objects.
     * This factory can be set by {@link #setRenderingHint}.
     */
    private CoordinateTransformationFactory factory = CoordinateTransformationFactory.getDefault();



    //////////////////////////////////////////////////////////////////
    ////////                                                  ////////
    ////////        Rendering hints (e.g. resolution)         ////////
    ////////                                                  ////////
    //////////////////////////////////////////////////////////////////
    /**
     * A set of rendering hints. Recognized hints include
     * {@link Hints#COORDINATE_TRANSFORMATION_FACTORY} and
     * any of {@link Hints#FINEST_RESOLUTION}.
     *
     * @see Hints#FINEST_RESOLUTION
     * @see Hints#REQUIRED_RESOLUTION
     * @see Hints#COORDINATE_TRANSFORMATION_FACTORY
     * @see Hints#PREFETCH
     * @see RenderingHints#KEY_RENDERING
     * @see RenderingHints#KEY_COLOR_RENDERING
     * @see RenderingHints#KEY_INTERPOLATION
     */
    protected final RenderingHints hints = new RenderingHints(null);

    /**
     * <code>true</code> if the renderer is allowed to prefetch data before to
     * paint layers.   Prefetching data may speed up rendering on machine with
     * more than one processor.
     */
    private boolean prefetch = (Runtime.getRuntime().availableProcessors() >= 2);

    /**
     * The rendering resolutions, in units of {@link RenderingContext#textCS} coordinate system
     * (usually 1/72 of inch). A larger resolution speed up rendering, while a smaller resolution
     * draw more precise map. The value can be set with {@link #setRenderingHint}. They are read
     * only by {@link RenderedGeometries}.
     */
    float minResolution, maxResolution;

    /**
     * The bounding box of all {@link RenderedLayer} in the {@link RenderingContext#mapCS}
     * coordinate system. This box is computed from {@link RenderedLayer#getPreferredArea}.
     * A <code>null</code> value means that none of them returned a non-null value.
     */
    private Rectangle2D preferredArea;

    /**
     * Statistics about rendering. Used for logging messages only.
     */
    final RenderingStatistics statistics = new RenderingStatistics();



    //////////////////////////////////////////////////////////////////
    ////////                                                  ////////
    ////////         AWT and Swing container / events         ////////
    ////////                                                  ////////
    //////////////////////////////////////////////////////////////////
    /**
     * The component owner, or <code>null</code> if none. This is used for managing
     * repaint request (see {@link RenderedLayer#repaint}) or mouse events.
     */
    final Component mapPane;

    /**
     * Listeners to be notified about any changes in this layer's properties.
     * Examples of properties that may change:
     * <code>"preferredArea"</code>.
     */
    protected final PropertyChangeSupport listeners;

    /**
     * Listener for events of interest to this renderer. Events may come
     * from any {@link RenderedLayer} or from the {@link Component}.
     */
    private final ListenerProxy listenerProxy = new ListenerProxy();

    /**
     * Classe ayant la charge de r�agir aux diff�rents �v�nements qui int�ressent cet
     * objet <code>Renderer</code>. Cette classe r�agira entre autres aux changements
     * de l'ordre <var>z</var> ainsi qu'aux changements des coordonn�es g�ographiques
     * d'une couche.
     */
    private final class ListenerProxy extends ComponentAdapter implements PropertyChangeListener {
        /** Invoked when the component's size changes. */
        public void componentResized(final ComponentEvent event) {
            synchronized (Renderer.this) {
                zoomChanged(null);
            }
        }

        /** Invoked when the component has been made invisible. */
        public void componentHidden(final ComponentEvent event) {
            synchronized (Renderer.this) {
                clearCache();
            }
            // As a symetrical approach, it would be nice to invokes 'prefetch(...)' inside
            // 'componentShown(...)' too. But we don't know for sure what the widget bounds
            // and the zoom will be. We are better to wait until 'paint(...)' is invoked.
        }

        /** Invoked when a {@link RenderedLayer}'s property is changed. */
        public void propertyChange(final PropertyChangeEvent event) {
            final String propertyName = event.getPropertyName();
            synchronized (Renderer.this) {
                if (propertyName.equalsIgnoreCase("preferredArea")) {
                    changePreferredArea((Rectangle2D)    event.getOldValue(),
                                        (Rectangle2D)    event.getNewValue(),
                                        ((RenderedLayer) event.getSource()).getCoordinateSystem(),
                                        "RenderedLayer", "setPreferredArea");
                    return;
                }
                if (propertyName.equalsIgnoreCase("zOrder")) {
                    layerSorted = false;
                    return;
                }
                if (propertyName.equalsIgnoreCase("coordinateSystem")) {
                    computePreferredArea("RenderedLayer", "setCoordinateSystem");
                    return;
                }
                if (propertyName.equalsIgnoreCase("gridCoverage")) {
                    computePreferredArea("RenderedLayer", "setGridCoverage");
                    return;
                }
            }
        }
    }



    //////////////////////////////////////////////////////////////////
    ////////                                                  ////////
    ////////      Constructors and essential properties       ////////
    ////////                                                  ////////
    //////////////////////////////////////////////////////////////////
    /**
     * Construct a new renderer for the specified component.
     *
     * @param owner The widget that own this renderer, or <code>null</code> if none.
     */
    public Renderer(final Component owner) {
        final CoordinateSystem cs = GeographicCoordinateSystem.WGS84;
        context   = new RenderingContext(this, cs, cs, cs);
        listeners = new PropertyChangeSupport(this);
        this.mapPane = owner;
        if (mapPane != null) {
            mapPane.addComponentListener(listenerProxy);
        }
        updateNormalizationFactor(cs);
    }

    /**
     * Returns this renderer's name. The default implementation returns the title
     * of the window which contains the map pane.
     *
     * @param  locale The desired locale, or <code>null</code> for a default locale.
     * @return This renderer's name, or <code>null</code> if none.
     *
     * @see #getLocale
     * @see Component#getName
     * @see RenderedLayer#getName
     */
    public synchronized String getName(final Locale locale) {
        for (Component c=mapPane; c!=null; c=c.getParent()) {
            if (c instanceof Frame) {
                return ((Frame) c).getTitle();
            }
            if (c instanceof Dialog) {
                return ((Dialog) c).getTitle();
            }
            if (c instanceof JInternalFrame) {
                return ((JInternalFrame) c).getTitle();
            }
        }
        return null;
    }

    /**
     * Returns to locale for this renderer. The renderer will inherit
     * the locale of its {@link Component}, if it have one. Otherwise,
     * a default locale will be returned.
     *
     * @see Component#getLocale
     * @see JComponent#getDefaultLocale
     * @see Locale#getDefault
     */
    public Locale getLocale() {
        if (mapPane != null) try {
            return mapPane.getLocale();
        } catch (IllegalComponentStateException exception) {
            // Not yet added to a containment hierarchy. Ignore...
            if (mapPane instanceof JComponent) {
                return JComponent.getDefaultLocale();
            }
        }
        return Locale.getDefault();
    }

    /**
     * Returns the current rendering context. Used by {@link GeoMouseEvent}
     * in order to gets a "snapshot" of current coordinate system states.
     */
    final RenderingContext getRenderingContext() {
        assert context.getGraphics() == null;
        return context;
    }

    /**
     * Returns the view coordinate system. This is the "real world" coordinate system
     * used for displaying all {@link RenderedLayer}s. Note that underlying data in
     * <code>RenderedLayer</code> doesn't need to be in this coordinate system:
     * transformations will performed on the fly as needed at rendering time.
     *
     * @return The two dimensional coordinate system used for display.
     */
    public CoordinateSystem getCoordinateSystem() {
        return context.mapCS;
    }

    /**
     * Set the view coordinate system. This is the "real world" coordinate system
     * to use for displaying all {@link RenderedLayer}s. Layers are notified of the
     * coordinate system change with {@link RenderedLayer#setCoordinateSystem}.
     *
     * If this method is never invoked, then the default is
     * {@linkplain GeographicCoordinateSystem#WGS84 WGS 1984}.
     *
     * @param cs The view coordinate system. If the specified coordinate system has more
     *           than two dimensions, then it must be a {@link CompoundCoordinateSystem}
     *           with a two dimensional {@link CompoundCoordinateSystem#getHeadCS headCS}.
     * @throws TransformException If <code>cs</code> can't be reduced to a two-dimensional
     *         coordinate system, or if data can't be transformed for some other reason.
     */
    public void setCoordinateSystem(CoordinateSystem cs) throws TransformException {
        cs = CTSUtilities.getCoordinateSystem2D(cs);
        final CoordinateSystem oldCS;
        synchronized (this) {
            int changed = 0;
            oldCS = getCoordinateSystem();
            if (!cs.equals(oldCS, false)) {
                try {
                    while (changed < layerCount) {
                        layers[changed].setCoordinateSystem(cs);
                        changed++; // Incremente only if previous call succeed.
                    }
                } catch (TransformException exception) {
                    // Roll back all CS changes.
                    while (--changed >= 0) {
                        try {
                            layers[changed].setCoordinateSystem(oldCS);
                        } catch (TransformException unexpected) {
                            // Should not happen, since we are rolling
                            // back to an old CS that previously worked.
                            handleException("Renderer", "setCoordinateSystem", unexpected);
                        }
                    }
                    clearCache();
                    throw exception;
                }
                clearCache();
                CoordinateSystem textCS = createFittedCoordinateSystem("textCS", cs, mapToText);
                context = new RenderingContext(this, cs, textCS, textCS);
                computePreferredArea("Renderer", "setCoordinateSystem");
                updateNormalizationFactor(cs);
                scaleFactor = null;
            }
        }
        listeners.firePropertyChange("coordinateSystem", oldCS, cs);
    }

    /**
     * Update {@link #normalizeToDots} for the specified coordinate system.
     */
    private void updateNormalizationFactor(final CoordinateSystem cs) {
        final Ellipsoid ellipsoid = CTSUtilities.getHeadGeoEllipsoid(cs);
        normalizeToDots.setToScale(getNormalizationFactor(cs.getUnits(0), ellipsoid),
                                   getNormalizationFactor(cs.getUnits(1), ellipsoid));
    }

    /**
     * Returns the amount of "dots" in one unit of the specified unit. There is 72 dots in one
     * inch, and 2.54/100 inchs in one metre.  The <code>unit</code> argument must be a linear
     * or an angular unit.
     *
     * @param unit The unit. If <code>null</code>, then the unit will be assumed to be metres or
     *        degrees depending of whatever <code>ellipsoid</code> is <code>null</code> or not.
     * @param ellipsoid The ellipsoid if the coordinate system is geographic, or <code>null</code>
     *        otherwise.
     */
    private double getNormalizationFactor(Unit unit, final Ellipsoid ellipsoid) {
        double m = 1;
        try {
            if (ellipsoid != null) {
                if (unit == null) {
                    unit = Unit.DEGREE;
                }
                /*
                 * Converts an angular unit to a linear one.   An ellipsoid has two axis that we
                 * could use. For the WGS84 ellipsoid, the semi-major axis results in a nautical
                 * mile of 1855.32 metres  while  the semi-minor axis results in a nautical mile
                 * of 1849.10 metres. The average of semi-major and semi-minor axis results in a
                 * nautical mile of 1852.21 metres, which is pretty close to the internationaly
                 * agreed length (1852 metres). This is consistent with the definition of nautical
                 * mile, which is the length of an angle of 1 minute along the meridian at 45� of
                 * latitude.
                 */
                m = Unit.RADIAN.convert(m, unit) * 0.5*(ellipsoid.getSemiMajorAxis() +
                                                        ellipsoid.getSemiMinorAxis());
                unit = ellipsoid.getAxisUnit();
            }
            if (unit != null) {
                m = Unit.METRE.convert(m, unit);
            }
        } catch (UnitException exception) {
            /*
             * A unit conversion failed. Since this normalizing factor is used only for computing a
             * scale, it is not crucial to the renderer working. Log a warning message and continue.
             * We keep the m value computed so far, which will be assumed to be a length in metres.
             */
            final LogRecord record = Resources.getResources(getLocale()).getLogRecord(Level.WARNING,
                                                     ResourceKeys.WARNING_UNEXPECTED_UNIT_$1, unit);
            record.setSourceClassName ("Renderer");
            record.setSourceMethodName("setCoordinateSystem");
            record.setThrown(exception);
            LOGGER.log(record);
        }
        return 7200/2.54 * m;
    }

    /**
     * Returns a bounding box that completely encloses all layer's {@linkplain
     * RenderedLayer#getPreferredArea preferred area}, visible or not. This
     * bounding box should be representative of the geographic area to drawn.
     * Coordinates are expressed in this {@linkplain #getCoordinateSystem
     * renderer's coordinate system}.
     *
     * @return The enclosing area computed from available data, or <code>null</code>
     *         if this area can't be computed.
     */
    public Rectangle2D getPreferredArea() {
        final Rectangle2D area = this.preferredArea;
        return (area!=null) ? (Rectangle2D) area.clone() : null;
    }

    /**
     * Set the geographic area. This is method is invoked as a result of
     * internal computation. User should not call this method directly.
     *
     * @param newArea The new preferred area (will not be cloned).
     */
    private void setPreferredArea(final Rectangle2D newArea) {
        final Rectangle2D oldArea;
        synchronized (this) {
            oldArea = preferredArea;
            preferredArea = newArea;
        }
        listeners.firePropertyChange("preferredArea", oldArea, newArea);
    }

    /**
     * Recompute inconditionnaly the {@link #preferredArea}. Will be computed
     * from the value returned by {@link RenderedLayer#getPreferredArea} for
     * all layers.
     *
     * @param  sourceClassName  The caller's class name, for logging purpose.
     * @param  sourceMethodName The caller's method name, for logging purpose.
     */
    private void computePreferredArea(final String sourceClassName, final String sourceMethodName) {
        assert Thread.holdsLock(this);
        Rectangle2D         newArea = null;
        CoordinateSystem lastSystem = null;
        MathTransform2D   transform = null;
        for (int i=layerCount; --i>=0;) {
            final RenderedLayer layer = layers[i];
            Rectangle2D bounds = layer.getPreferredArea();
            if (bounds != null) {
                // In theory, the RendererLayer should use the same CS than this Renderer.
                // However, as a safety, we will check for coordinate transformations anyway.
                final CoordinateSystem coordinateSystem = layer.getCoordinateSystem();
                try {
                    if (lastSystem==null || !lastSystem.equals(coordinateSystem, false)) {
                        transform  = (MathTransform2D) getMathTransform(
                                                       coordinateSystem, getCoordinateSystem(),
                                                       sourceClassName, sourceMethodName);
                        lastSystem = coordinateSystem;
                    }
                    bounds = CTSUtilities.transform(transform, bounds, null);
                    if (newArea == null) {
                        newArea = bounds;
                    } else {
                        newArea.add(bounds);
                    }
                } catch (TransformException exception) {
                    handleException(sourceClassName, sourceMethodName, exception);
                    // Continue. The preferred area for this layer will be ignored.
                }
            }
        }
        setPreferredArea(newArea);
    }

    /**
     * Remplace un rectangle par un autre dans le calcul de {@link #preferredArea}. Si �a a eu
     * pour effet de changer les coordonn�es g�ographiques couvertes, un �v�nement appropri�
     * sera lanc�. Cette m�thode est plus �conomique que {@link #computePreferredArea} du fait
     * qu'elle essaie de ne pas tout recalculer. Si on n'a pas pu faire l'�conomie d'un recalcul
     * toutefois, alors {@link #computePreferredArea} sera appel�e.
     *
     * @param  oldSubArea       The old preferred area of the layer that changed.
     * @param  newSubArea       The new preferred area of the layer that changed.
     * @param  coordinateSystem The coordinate system for <code>[old|new]SubArea</code>.
     * @param  sourceClassName  The caller's class name, for logging purpose.
     * @param  sourceMethodName The caller's method name, for logging purpose.
     */
    private void changePreferredArea(Rectangle2D oldSubArea,
                                     Rectangle2D newSubArea,
                                     final CoordinateSystem coordinateSystem,
                                     final String sourceClassName,
                                     final String sourceMethodName)
    {
        assert Thread.holdsLock(this);
        try {
            final MathTransform2D transform = (MathTransform2D) getMathTransform(
                    coordinateSystem, getCoordinateSystem(), sourceClassName, sourceMethodName);
            oldSubArea = CTSUtilities.transform(transform, oldSubArea, null);
            newSubArea = CTSUtilities.transform(transform, newSubArea, null);
        } catch (TransformException exception) {
            handleException(sourceClassName, sourceMethodName, exception);
            computePreferredArea(sourceClassName, sourceMethodName);
            return;
        }
        final Rectangle2D expandedArea = changeArea(preferredArea, oldSubArea, newSubArea);
        if (expandedArea != null) {
            setPreferredArea(expandedArea);
        } else {
            computePreferredArea(sourceClassName, sourceMethodName);
        }
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
    private static Rectangle2D changeArea(Rectangle2D area,
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

    /**
     * Indique si la r�gion g�ographique <code>big</code> contient enti�rement la sous-r�gion
     * <code>small</code> sp�cifi�e. Un cas particulier survient si un ou plusieurs bords de
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
     * Returns the preferred pixel size in "real world" coordinates. For image layers, this is
     * the size of image's pixels. For other kind of layers, "pixel size" are to be understood
     * as some dimension representative of the layer's resolution.  This method invokes {@link
     * RenderedLayer#getPreferredPixelSize} for each layers and returns the finest resolution.
     *
     * @return The preferred pixel size in "real world" coordinates (in this 
     *         {@linkplain #getCoordinateSystem renderer's coordinate system})
     *         or <code>null</code> if no layer provided a preferred pixel size.
     *
     * @task TODO: Transformations should use MathTransform.derivative(...)
     *             instead, but it is not yet implemented for projections.
     */
    public Dimension2D getPreferredPixelSize() {
        double minWidth  = Double.POSITIVE_INFINITY;
        double minHeight = Double.POSITIVE_INFINITY;
        for (int i=layerCount; --i>=0;) {
            final RenderedLayer layer = layers[i];
            final Dimension2D size = layer.getPreferredPixelSize();
            if (size!=null) try {
                double width  = size.getWidth();
                double height = size.getHeight();
                final MathTransform2D transform = (MathTransform2D) getMathTransform(
                                                        layer.getCoordinateSystem(),
                                                        this. getCoordinateSystem(),
                                                        "Renderer", "getPreferredPixelSize");
                if (!transform.isIdentity()) {
                    /*
                     * In theory, the RendererLayer should use the same CS than this Renderer.
                     * However, as a safety, we will check for coordinate transformations anyway.
                     * We create a pixel in the midle of the preferred area and transform it to
                     * this coordinate system.  TODO: we should use MathTransform.derivative
                     * instead, but is is not yet implemented for projections.
                     */
                    Rectangle2D area = layer.getPreferredArea();
                    if (area == null) {
                        area = new Rectangle2D.Double();
                    }
                    area.setRect(area.getCenterX()-0.5*width,
                                 area.getCenterY()-0.5*height,
                                 width, height);
                    area   = CTSUtilities.transform(transform, area, area);
                    width  = area.getWidth();
                    height = area.getHeight();
                }
                if (width  < minWidth ) minWidth =width;
                if (height < minHeight) minHeight=height;
            } catch (TransformException exception) {
                handleException("Renderer", "getPreferredPixelSize", exception);
                // Not a big deal. Continue...
            }
        }
        if (minWidth >0 && !Double.isInfinite(minWidth) &&
            minHeight>0 && !Double.isInfinite(minHeight))
        {
            return new XDimension2D.Double(minWidth, minHeight);
        } else {
            return null;
        }
    }

    /**
     * Returns the scale factor, or {@link Float#NaN} if the scale is not know.
     * The scale factor is usually smaller than 1. For example for a 1:1000 scale,
     * the scale factor will be 0.001. This scale factor takes in account the physical
     * size of the rendering device (e.g. the screen size) if such information is available.
     * Note that this scale can't be more accurate than the
     * {@linkplain GraphicsConfiguration#getNormalizingTransform() information supplied
     * by the underlying system}.
     */
    public float getScale() {
        final Float scaleFactor = this.scaleFactor; // Avoid the need for synchronization.
        return (scaleFactor!=null) ? scaleFactor.floatValue() : Float.NaN;
    }



    //////////////////////////////////////////////////////////////////
    ////////                                                  ////////
    ////////          add/remove/get rendered layers          ////////
    ////////                                                  ////////
    //////////////////////////////////////////////////////////////////
    /**
     * Add a new layer to this renderer. A <code>Renderer</code> do not draw anything as long
     * as at least one layer hasn't be added.    A {@link RenderedLayer} can be anything like
     * an isobath, a remote sensing image, city locations, map scale, etc.  The drawing order
     * (relative to other layers) is determined by the {@linkplain RenderedLayer#getZOrder
     * z-order} property. A {@link RenderedLayer} object can be added to only one
     * <code>Renderer</code> object.
     *
     * @param  layer Layer to add to this <code>Renderer</code>. This method call
     *         will be ignored if <code>layer</code> has already been added to this
     *         <code>Renderer</code>.
     * @throws IllegalArgumentException If <code>layer</code> has already been added
     *         to an other <code>Renderer</code>, or if the layer can't be added for
     *         some other reason.
     *
     * @see #removeLayer
     * @see #removeAllLayers
     * @see #getLayers
     * @see #getLayerCount
     */
    public synchronized void addLayer(final RenderedLayer layer) throws IllegalArgumentException {
        synchronized (layer.getTreeLock()) {
            if (layer.renderer == this) {
                return;
            }
            if (layer.renderer != null) {
                throw new IllegalArgumentException(
                            Resources.format(ResourceKeys.ERROR_RENDERER_NOT_OWNER_$1, layer));
            }
            layer.renderer = this;
            try {
                layer.setCoordinateSystem(getCoordinateSystem());
            } catch (TransformException exception) {
                layer.renderer = null;
                final IllegalArgumentException e;
                e = new IllegalArgumentException(exception.getLocalizedMessage());
                e.initCause(exception);
                throw e;
            }
            /*
             * Ajoute la nouvelle couche dans le tableau {@link #layers}. Le tableau
             * sera agrandit si n�cessaire et on d�clarera qu'il a besoin d'�tre reclass�.
             */
            if (layers == null) {
                layers = new RenderedLayer[16];
            }
            if (layerCount >= layers.length) {
                layers = (RenderedLayer[]) XArray.resize(layers, Math.max(layerCount,8) << 1);
            }
            layers[layerCount++] = layer;
            layerSorted = false;
            layer.setVisible(true);
            changePreferredArea(null, layer.getPreferredArea(), layer.getCoordinateSystem(),
                                "Renderer", "addLayer");
            layer.addPropertyChangeListener(listenerProxy);
        }
        repaint(); // Must be invoked last
        listeners.firePropertyChange("layers", (layerCount==1) ? EMPTY : null, null);
    }

    /**
     * Remove a layer from this renderer. Note that if the layer is going to
     * be added back to the same renderer later, then it is more efficient to invoke
     * <code>{@link RenderedLayer#setVisible RenderedLayer.setVisible}(false)</code>.
     *
     * @param  layer The layer to remove. This method call will be ignored
     *         if <code>layer</code> has already been removed from this
     *         <code>Renderer</code>.
     * @throws IllegalArgumentException If <code>layer</code> is owned by
     *         an other <code>Renderer</code> than <code>this</code>.
     *
     * @see #addLayer
     * @see #removeAllLayers
     * @see #getLayers
     * @see #getLayerCount
     */
    public synchronized void removeLayer(final RenderedLayer layer) throws IllegalArgumentException
    {
        assert Thread.holdsLock(layer.getTreeLock());
        if (layer.renderer == null) {
            return;
        }
        if (layer.renderer != this) {
            throw new IllegalArgumentException(
                        Resources.format(ResourceKeys.ERROR_RENDERER_NOT_OWNER_$1, layer));
        }
        repaint(); // Must be invoked first
        layer.removePropertyChangeListener(listenerProxy);
        final CoordinateSystem layerCS = layer.getCoordinateSystem();
        final Rectangle2D    layerArea = layer.getPreferredArea();
        layer.setVisible(false);
        layer.clearCache();
        layer.renderer = null;
        /*
         * Retire cette couche de la liste {@link #layers}. On recherchera
         * toutes les occurences de cette couche, m�me si en principe elle ne
         * devrait appara�tre qu'une et une seule fois.
         */
        for (int i=layerCount; --i>=0;) {
            final RenderedLayer scan = layers[i];
            if (scan == layer) {
                System.arraycopy(layers, i+1, layers, i, (--layerCount)-i);
                layers[layerCount] = null;
            }
        }
        changePreferredArea(layerArea, null, layerCS, "Renderer", "removeLayer");
        listeners.firePropertyChange("layers", null, (layerCount!=0) ? null : EMPTY);
    }

    /**
     * Remove all layers from this renderer.
     *
     * @see #addLayer
     * @see #removeLayer
     * @see #getLayers
     * @see #getLayerCount
     */
    public synchronized void removeAllLayers() {
        repaint(); // Must be invoked first
        while (--layerCount>=0) {
            final RenderedLayer layer = layers[layerCount];
            assert Thread.holdsLock(layer.getTreeLock());
            layer.removePropertyChangeListener(listenerProxy);
            layer.setVisible(false);
            layer.clearCache();
            layer.renderer = null;
            layers[layerCount] = null;
        }
        layerCount = 0;
        setPreferredArea(null);
        clearCache();
        listeners.firePropertyChange("layers", null, EMPTY);
    }

    /**
     * Returns all registered layers. The returned array is sorted in increasing
     * {@linkplain RenderedLayer#getZOrder z-order}: element at index 0 contains
     * the first layer to be drawn.
     *
     * @return The sorted array of layers. May have a 0 length, but will never
     *         be <code>null</code>. Change to this array, will not affect this
     *         <code>Renderer</code>.
     *
     * @see #addLayer
     * @see #removeLayer
     * @see #removeAllLayers
     * @see #getLayerCount
     */
    public synchronized RenderedLayer[] getLayers() {
        sortLayers();
        if (layers != null) {
            final RenderedLayer[] array = new RenderedLayer[layerCount];
            System.arraycopy(layers, 0, array, 0, layerCount);
            return array;
        } else {
            return EMPTY;
        }
    }

    /**
     * Returns the number of layers in this renderer.
     *
     * @see #getLayers
     * @see #addLayer
     * @see #removeLayer
     * @see #removeAllLayers
     */
    public int getLayerCount() {
        return layerCount;
    }

    /**
     * Proc�de au classement imm�diat des couches, si ce n'�tait pas d�j� fait.
     */
    private void sortLayers() {
        assert Thread.holdsLock(this);
        if (!layerSorted && layers!=null) {
            layers = (RenderedLayer[]) XArray.resize(layers, layerCount);
            Arrays.sort(layers, COMPARATOR);
            layerSorted = true;
        }
    }

    //////////////////////////////////////////////////////////////////
    ////////                                                  ////////
    ////////                    Rendering                     ////////
    ////////                                                  ////////
    //////////////////////////////////////////////////////////////////
    /**
     * Returns a rendering hint.
     *
     * @param  key The hint key (e.g. {@link Hints#FINEST_RESOLUTION}).
     * @return The hint value for the specified key, or <code>null</code> if there is no
     *         hint for the specified key.
     *
     * @see Hints#FINEST_RESOLUTION
     * @see Hints#REQUIRED_RESOLUTION
     * @see Hints#COORDINATE_TRANSFORMATION_FACTORY
     * @see Hints#PREFETCH
     * @see RenderingHints#KEY_RENDERING
     * @see RenderingHints#KEY_COLOR_RENDERING
     * @see RenderingHints#KEY_INTERPOLATION
     */
    public synchronized Object getRenderingHint(final RenderingHints.Key key) {
        return hints.get(key);
    }

    /**
     * Add a rendering hint. Hints provides optional information used by some rendering code.
     *
     * @param key   The hint key (e.g. {@link Hints#FINEST_RESOLUTION}).
     * @param value The hint value. A <code>null</code> value remove the hint.
     *
     * @see Hints#FINEST_RESOLUTION
     * @see Hints#REQUIRED_RESOLUTION
     * @see Hints#COORDINATE_TRANSFORMATION_FACTORY
     * @see Hints#PREFETCH
     * @see RenderingHints#KEY_RENDERING
     * @see RenderingHints#KEY_COLOR_RENDERING
     * @see RenderingHints#KEY_INTERPOLATION
     */
    public synchronized void setRenderingHint(RenderingHints.Key key, Object value) {
        if (value != null) {
            hints.put(key, value);
        } else {
            hints.remove(key);
        }
        if (Hints.FINEST_RESOLUTION.equals(key)) {
            if (value != null) {
                minResolution = ((Number) hints.get(key)).floatValue();
                if (minResolution >= 0) {
                    if (minResolution > maxResolution) {
                        maxResolution = minResolution;
                    }
                    return;
                }
            }
            minResolution = 0;
            return;
        }
        if (Hints.REQUIRED_RESOLUTION.equals(key)) {
            if (value != null) {
                maxResolution = ((Number) hints.get(key)).floatValue();
                if (maxResolution >= 0) {
                    if (maxResolution < minResolution) {
                        minResolution = maxResolution;
                    }
                    return;
                }
            }
            maxResolution = minResolution;
            return;
        }
        if (Hints.PREFETCH.equals(key)) {
            if (value != null) {
                prefetch = ((Boolean) value).booleanValue();
            } else {
                prefetch = (Runtime.getRuntime().availableProcessors() >= 2);
            }
            return;
        }
        if (Hints.GRID_COVERAGE_PROCESSOR.equals(key) &&
            !hints.containsKey(Hints.COORDINATE_TRANSFORMATION_FACTORY))
        {
            key = Hints.COORDINATE_TRANSFORMATION_FACTORY;
            value = (value!=null) ? ((GridCoverageProcessor) value).getRenderingHint(key) : null;
            // Fall through
        }
        if (Hints.COORDINATE_TRANSFORMATION_FACTORY.equals(key)) {
            factory = (value!=null) ? (CoordinateTransformationFactory) value :
                                       CoordinateTransformationFactory.getDefault();
            clearCache();
            return;
        }
    }

    /**
     * Enable or disable the use of offscreen buffer for all {@linkplain RenderedLayer layers}
     * in the given range of {@linkplain RenderedLayer#getZOrder z-orders}. When enabled, all
     * layers in the given range will be rendered once in an offscreen buffer (for example an
     * {@link VolatileImage}); the image will then been reused as much as possible. The offscreen
     * buffer may be invalidate at any time by some external event (including a call to any of
     * {@link RenderedLayer#repaint()} methods) and will be recreated as needed. Using offscreen
     * buffer for background layers that do not change often (e.g. a background map) help to make
     * the GUI more responsive to frequent change in foreground layers (e.g. a glass pane with
     * highlighted selections).
     * <br><br>
     * An arbitrary amount of ranges can be specified. Each <strong>distinct</strong> range will
     * use its own offscreen buffer. This means that if this method is invoked twice for enabling
     * buffering in overlapping range of z-values, then the union of the two ranges will shares
     * the same offscreen image.
     *
     * @param lower The lower z-order, inclusive.
     * @param upper The upper z-order, inclusive.
     * @param enable <code>true</code> for enabling offscreen buffering for the specified range,
     *        or <code>false</code> for disabling it.
     */
    public synchronized void setOffscreenBuffered(final float lower,
                                                  final float upper,
                                                  final boolean enable)
    {
        /*
         * Save the references to the old VolatileImage. We will try to reuse
         * existing VolatileImages after the range set has been updated.
         */
        final Map old;
        if (offscreenZRanges == null) {
            if (!enable) {
                return;
            }
            offscreenZRanges = new RangeSet(Float.class);
            old = Collections.EMPTY_MAP;
        } else {
            int index=0;
            old = new HashMap();
            for (final Iterator it=offscreenZRanges.iterator(); it.hasNext();) {
                if (old.put(it.next(), offscreenBuffers[index++]) != null) {
                    throw new AssertionError(); // Should not happen
                }
            }
            assert index == offscreenBuffers.length : index;
        }
        /*
         * Update the range set, and rebuild the offscreen buffers array.
         */
        if (enable) {
            offscreenZRanges.add(lower, upper);
        } else {
            offscreenZRanges.remove(lower, upper);
        }
        offscreenBuffers = new VolatileImage[offscreenZRanges.size()];
        int index=0;
        for (final Iterator it=offscreenZRanges.iterator(); it.hasNext();) {
            offscreenBuffers[index++] = (VolatileImage) old.remove(it.next());
        }
        assert index == offscreenBuffers.length : index;
        /*
         * Release resources used by remaining (now unused) images.
         */
        for (final Iterator it=old.values().iterator(); it.hasNext();) {
            ((Image) it.next()).flush();
        }
    }

    /**
     * Signal that a layer at the given {@linkplain RenderedLayer#getZOrder z-order} need
     * to be repaint.  If an offscreen buffer were allocated for this z-order, it must be
     * flushed. This method is invoked by any of {@link RenderedLayer#repaint} methods.
     *
     * @param zOrder The z-order of the offscreen buffer to flush.
     */
    final synchronized void clearOffscreenBuffer(final float zOrder) {
        if (offscreenZRanges != null) {
            final int index = offscreenZRanges.indexOfRange(new Float(zOrder));
            if (index >= 0) {
                final Image image = offscreenBuffers[index];
                if (image != null) {
                    image.flush();
                }
            }
        }
    }

    /**
     * Flush all offscreen buffers.
     */
    private void flushOffscreenBuffers() {
        assert Thread.holdsLock(this);
        if (offscreenBuffers != null) {
            for (int i=0; i<offscreenBuffers.length; i++) {
                final Image image = offscreenBuffers[i];
                if (image != null) {
                    image.flush();
                }
            }
        }
    }

    /**
     * Returns a string representation of a coordinate system. This method is
     * used for formatting a logging message in {@link #getMathTransform}.
     */
    private static String toString(final CoordinateSystem cs) {
        final StringBuffer buffer = new StringBuffer(Utilities.getShortClassName(cs));
        buffer.append('[');
        final String name = cs.getName(null);
        if (name != null) {
            buffer.append('"');
            buffer.append(name);
            buffer.append('"');
        }
        buffer.append(']');
        return buffer.toString();
    }

    /**
     * Returns the math transform factory associated to this renderer.
     */
    final synchronized MathTransformFactory getMathTransformFactory() {
        return factory.getMathTransformFactory();
    }

    /**
     * Construct a transform between two coordinate systems. If a {@link
     * Hints#COORDINATE_TRANSFORMATION_FACTORY} has been provided, the
     * specified {@link CoordinateTransformationFactory} will be used.
     *
     * @param  sourceCS The source coordinate system.
     * @param  targetCS The target coordinate system.
     * @param  sourceClassName  The caller's class name, for logging purpose.
     * @param  sourceMethodName The caller's method name, for logging purpose.
     * @return A transformation from <code>sourceCS</code> to <code>targetCS</code>.
     * @throws CannotCreateTransformException if the transformation can't be created.
     *
     * @see #getRenderingHint
     * @see #setRenderingHint
     * @see Hints#COORDINATE_TRANSFORMATION_FACTORY
     */
    final synchronized MathTransform getMathTransform(final CoordinateSystem sourceCS,
                                                      final CoordinateSystem targetCS,
                                                      final String sourceClassName,
                                                      final String sourceMethodName)
            throws CannotCreateTransformException
    {
        if (sourceCS == targetCS) {
            // Fast check for a very common case. We will use the more
            // general (but slower) 'equals(..., false)' version later.
            return MathTransform2D.IDENTITY;
        }
        MathTransform tr;
        /*
         * Check if the math transform is available in the cache. A majority of transformations
         * will be from 'layerCS' to 'mapCS' to 'textCS'.  The cache looks for the 'layerCS' to
         * to 'mapCS' transform.
         */
        final boolean cachedTransform = targetCS.equals(context.mapCS, false);
        if (cachedTransform) {
            tr = (MathTransform) transforms.get(sourceCS);
            if (tr != null) {
                return tr;
            }
        }
        /*
         * If one of the CS is a FittedCoordinateSystem, then check if we can use directly
         * its 'toBase' transform without using the costly CoordinateTransformationFactory.
         * This check is worth to be done since it is a very common situation.  A majority
         * of transformations will be from 'mapCS' to 'textCS',  which is the case we test
         * first. The converse (transformations from 'textCS' to 'mapCS') is less frequent
         * and can be catched by the 'transform' cache, which is why we test it last.
         */
        if (targetCS instanceof FittedCoordinateSystem) {
            final FittedCoordinateSystem fittedCS = (FittedCoordinateSystem) targetCS;
            if (sourceCS.equals(fittedCS.getBaseCoordinateSystem(), false)) try {
                return fittedCS.getToBase().inverse();
            } catch (NoninvertibleTransformException exception) {
                throw new CannotCreateTransformException(sourceCS, targetCS, exception);
            }
        }
        if (sourceCS instanceof FittedCoordinateSystem) {
            final FittedCoordinateSystem fittedCS = (FittedCoordinateSystem) sourceCS;
            if (targetCS.equals(fittedCS.getBaseCoordinateSystem(), false)) {
                tr = fittedCS.getToBase();
                if (cachedTransform) {
                    transforms.put(sourceCS, tr);
                }
                return tr;
            }
        }
        if (sourceCS.equals(targetCS, false)) {
            return MathTransform2D.IDENTITY;
        }
        /*
         * Now that we failed to reuse a pre-existing transform, ask to the factory
         * to create a new one. A message is logged in order to trace down the amount
         * of coordinate transformations created.
         */
        if (LOGGER.isLoggable(Level.FINER)) {
            // FINER is the default level for entering, returning, or throwing an exception.
            final LogRecord record = Resources.getResources(null).getLogRecord(Level.FINER,
                                               ResourceKeys.INITIALIZING_TRANSFORMATION_$2,
                                               toString(sourceCS), toString(targetCS));
            record.setSourceClassName (sourceClassName);
            record.setSourceMethodName(sourceMethodName);
            LOGGER.log(record);
        }
        tr = factory.createFromCoordinateSystems(sourceCS, targetCS).getMathTransform();
        if (cachedTransform) {
            transforms.put(sourceCS, tr);
        }
        return tr;
    }

    /**
     * Returns a fitted coordinate system for {@link RenderingContext#textCS} and
     * {@link RenderingContext#deviceCS}.
     *
     * @param  name     The coordinate system name (e.g. "text" or "device").
     * @param  base     The base coordinate system (e.g. {@link RenderingContext#mapCS}).
     * @param  fromBase The transform from the base CS to the fitted CS.  Note that this is the
     *                  opposite of the usual {@link FittedCoordinateSystem} constructor. We do
     *                  it that way because it is the way we usually gets affine transform from
     *                  {@link Graphics2D}.
     * @return The fitted coordinate system, or <code>base</code> if the transform is the identity
     *         transform.
     * @throws NoninvertibleTransformException if the affine transform is not invertible.
     */
    private CoordinateSystem createFittedCoordinateSystem(final String           name,
                                                          final CoordinateSystem base,
                                                          final AffineTransform fromBase)
            throws NoninvertibleTransformException
    {
        if (fromBase.isIdentity()) {
            return base;
        }
        /*
         * Inverse the MathTransform rather than the AffineTransform because MathTransform
         * keep a reference to its inverse. It avoid the need for re-inversing it again later,
         * which help to avoid rounding errors.
         */
        final MathTransform toBase;
        toBase = factory.getMathTransformFactory().createAffineTransform(fromBase).inverse();
        return new FittedCoordinateSystem(name, base, toBase, TEXT_AXIS);
    }

    /**
     * Paint this <code>Renderer</code> and all visible layers it contains.
     * This method invokes {@link RenderedLayer#paint} for each layer.
     *
     * @param graph  The graphics handler.
     * @param zoom   The zoom (usually provided by {@link org.geotools.gui.swing.ZoomPane#zoom}.
     * @param zoomableBounds The bounds of drawing area (usually provided by
     *               {@link org.geotools.gui.swing.ZoomPane#getZoomableBounds}).
     *
     * @deprecated Use {@link #paint(Graphics2D, Rectangle, AffineTransform)} instead.
     */
    public void paint(final Graphics2D         graph,
                      final AffineTransform     zoom,
                      final Rectangle zoomableBounds)
    {
        paint(graph, zoomableBounds, zoom);
    }

    /**
     * Paint this <code>Renderer</code> and all visible layers it contains.
     *
     * @deprecated Use {@link #paint(Graphics2D,Rectangle,AffineTransform,boolean)} instead.
     */
    public void paint(final Graphics2D         graph,
                      final Rectangle zoomableBounds,
                      final AffineTransform     zoom)
    {
        paint(graph, zoomableBounds, zoom, mapPane==null);
    }

    /**
     * Paint this <code>Renderer</code> and all visible layers it contains.
     * This method invokes {@link RenderedLayer#paint} for each layer.
     *
     * @param graph  The graphics handler to draw to.
     * @param zoomableBounds The bounds of the output area in output units (usually pixels).
     *               Those bounds are usually provided by
     *               {@link org.geotools.gui.swing.ZoomPane#getZoomableBounds}).
     * @param zoom   A transform which converts &quot;World coordinates&quot; to
     *               output coordinates. This transform is usually provided by
     *               {@link org.geotools.gui.swing.ZoomPane#zoom}.
     * @param isPrinting <code>true</code> if the map is printed instead of painted on screen.
     *               When printing, layers like {@link RenderedGridCoverage} should block until
     *               all data are available instead of painting only available data and invokes
     *               {@link RenderedLayer#repaint()} later.
     */
    public synchronized void paint(final Graphics2D         graph,
                                   final Rectangle zoomableBounds,
                                   final AffineTransform     zoom,
                                   final boolean       isPrinting)
    {
        statistics.init();
        sortLayers();
        RenderingContext       context = this.context;
        final RenderedLayer[]   layers = this.layers;
        final GraphicsJAI     graphics = GraphicsJAI.createGraphicsJAI(graph, mapPane);
        final Rectangle     clipBounds = graphics.getClipBounds();
        final AffineTransform toDevice = graphics.getTransform();
        final boolean         toScreen = toDevice.isIdentity();
        final boolean         sameZoom = mapToText.equals(zoom);
        try {
            /*
             * Set a flag for avoiding some 'paint' events while we are actually painting...
             */
            RenderedLayer.setDirtyArea(layers, layerCount, clipBounds.contains(zoomableBounds) ?
                                                           XRectangle2D.INFINITY : clipBounds);
            /*
             * If the zoom has changed, send a notification to all layers before to start the
             * rendering. Layers will update their cache, which is used in order to decide if
             * a layer need to be repainted or not. Note that some layers may change their state,
             * which may results in a new 'paint' event to be fired. But because of the 'dirtyArea'
             * flag above, some 'paint' event will be intercepted in order to avoid repainting the
             * same area twice.
             */
            if (!sameZoom) {
                flushOffscreenBuffers();
                /*
                 * Compute the change as an affine transform, and sent the notification.
                 */
                try {
                    final AffineTransform change = mapToText.createInverse();
                    change.preConcatenate(zoom);
                    if (true) {
                        // Scale slightly the zoom in order to avoid rounding errors in Area.
                        final double centerX = zoomableBounds.getCenterX();
                        final double centerY = zoomableBounds.getCenterY();
                        change.translate( centerX,  centerY);
                        change.scale    (   1+EPS,    1+EPS);
                        change.translate(-centerX, -centerY);
                    }
                    zoomChanged(change);
                } catch (java.awt.geom.NoninvertibleTransformException exception) {
                    // Should not happen. If it happen anyway, declare that everything must be
                    // repainted. It will be slower, but will not prevent the renderer to work.
                    Utilities.unexpectedException("org.geotools.renderer.j2d",
                                                  "Renderer", "paint", exception);
                    zoomChanged(null);
                }
                try {
                    /*
                     * Compute the new scale factor. This scale factor take in account the real
                     * size of the rendering device (e.g. the screen),  but is only as accurate
                     * as the information supplied by the underlying system.
                     */
                    final GraphicsConfiguration config = graphics.getDeviceConfiguration();
                    final AffineTransform normalize = zoom.createInverse();
                    normalize.concatenate(config.getNormalizingTransform());
                    normalize.preConcatenate(normalizeToDots);
                    scaleChanged((float)(1/XAffineTransform.getScale(normalize)));
                } catch (java.awt.geom.NoninvertibleTransformException exception) {
                    Utilities.unexpectedException("org.geotools.renderer.j2d",
                                                  "Renderer", "paint", exception);
                }
            }
            /*
             * If the zoom or the device changed, then the 'textCS' and 'deviceCS' must
             * be recreated.
             */
            if (!sameZoom || !toScreen) try {
                final CoordinateSystem mapCS, textCS, deviceCS;
                mapCS    = context.mapCS;
                textCS   = createFittedCoordinateSystem("textCS",    mapCS, zoom);
                deviceCS = createFittedCoordinateSystem("deviceCS", textCS, toDevice);
                context  = new RenderingContext(this, mapCS, textCS, deviceCS);
                if (toScreen) {
                    mapToText.setTransform(zoom);
                    this.context = context;
                }
            } catch (TransformException exception) {
                // Impossible to process to the rendering. Paint the stack
                // trace right into the component and exit from this method.
                GraphicsUtilities.paintStackTrace(graphics, zoomableBounds, exception);
                return;
            }
            /*
             * Draw all layers, starting with the one with the lowest <var>z</var> value. Before
             * to start the actual drawing,  we will notify all layers that they are about to be
             * drawn. Some layers may spend one or two threads for pre-computing data.
             */
            graphics.transform(zoom);
            graphics.addRenderingHints(hints);
            context.init(graphics, zoomableBounds, isPrinting);
            if (prefetch) {
                // Prepare data in separated threads.
                for (int i=0; i<layerCount; i++) {
                    layers[i].prefetch(context);
                }
            }
            for (int i=0; i<layerCount; i++) {
                try {
                    layers[i].update(context, clipBounds);
                } catch (TransformException exception) {
                    handleException("RenderedLayer", "paintComponent", exception);
                } catch (RuntimeException exception) {
                    Utilities.unexpectedException("org.geotools.renderer.j2d",
                                                  "RenderedLayer", "paint", exception);
                }
            }
        } finally {
            context.init(null, null, false);
            RenderedLayer.setDirtyArea(layers, layerCount, null);
            graphics.setTransform(toDevice);
        }
        /*
         * If this map took a long time to renderer, log a message.
         */
        statistics.finish(this);
    }

    /**
     * Declare that the {@link Component} need to be repainted.
     * This method can be invoked from any thread (it doesn't
     * need to be the <cite>Swing</cite> thread).
     */
    final void repaint() {
        final Component mapPane = this.mapPane;
        if (mapPane != null) {
            if (!EventQueue.isDispatchThread()) {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        repaint();
                    }
                });
                return;
            }
            mapPane.repaint();
        }
    }

    /**
     * Transform the specified rectangle from the rendering coordinate system to the "dot"
     * coordinate system used by Java2D. The transformation used is the one used the last
     * time the {@link #paint} method was invoked.
     *
     * @param  bounds The rectangle in "real world" rendering coordinates.
     * @return The rectangle in "dot" (or Java2D) coordinates.
     */
    final Rectangle mapToText(final Rectangle2D bounds) {
        return (Rectangle) XAffineTransform.transform(mapToText, bounds, new Rectangle());
    }

    /**
     * M�thode appel�e lorsqu'une exception {@link TransformException} non-g�r�e
     * s'est produite. Cette m�thode peut �tre appel�e pendant le tra�age de la carte
     * o� les mouvements de la souris. Elle ne devrait donc pas utiliser une bo�te de
     * dialogue pour reporter l'erreur, et retourner rapidement pour �viter de bloquer
     * la queue des �v�nements de <i>Swing</i>.
     *
     * @param  sourceClassName  The caller's class name, for logging purpose.
     * @param  sourceMethodName The caller's method name, for logging purpose.
     * @param  exception        The transform exception.
     */
    static void handleException(final String className,
                                final String methodName,
                                final TransformException exception)
    {
        Utilities.unexpectedException("org.geotools.renderer.j2d", className, methodName, exception);
    }



    //////////////////////////////////////////////////////////////////
    ////////                                                  ////////
    ////////                     Events                       ////////
    ////////                                                  ////////
    //////////////////////////////////////////////////////////////////
    /**
     * Returns the string to be used as the tooltip for a given mouse event.
     * This method queries registered {@linkplain RenderedLayer layers} in decreasing
     * {@linkplain RenderedLayer#getZOrder z-order} until one is found to returns a
     * non-null string.
     *
     * @param  event The mouse event.
     * @return The tool tip text, or <code>null</code> if there is no tool tip for this location.
     */
    public synchronized String getToolTipText(final GeoMouseEvent event) {
        sortLayers();
        final int x = event.getX();
        final int y = event.getY();
        final RenderedLayer[] layers = this.layers;
        for (int i=layerCount; --i>=0;) {
            final RenderedLayer layer = layers[i];
            if (layer.contains(x,y)) {
                final String tooltip = layer.getToolTipText(event);
                if (tooltip != null) {
                    return tooltip;
                }
            }
        }
        return null;
    }

    /**
     * Format a value for the current mouse position. This method invokes
     * {@link RenderedLayer#formatValue} for registered {@linkplain RenderedLayer
     * layers} in decreasing {@linkplain RenderedLayer#getZOrder z-order} until
     * one is found to returns <code>true</code>.
     *
     * @param  event The mouse event.
     * @param  toAppendTo The destination buffer for formatting a value.
     * @return <code>true</code> if this method has formatted a value,
     *         or <code>false</code> otherwise.
     *
     * @see Tools#formatValue
     * @see MouseCoordinateFormat#format(GeoMouseEvent)
     */
    final synchronized boolean formatValue(final GeoMouseEvent event,
                                           final StringBuffer toAppendTo)
    {
        sortLayers();
        final int x = event.getX();
        final int y = event.getY();
        final RenderedLayer[] layers = this.layers;
        for (int i=layerCount; --i>=0;) {
            final RenderedLayer layer = layers[i];
            if (layer.contains(x,y)) {
                if (layer.formatValue(event, toAppendTo)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * M�thode appel�e automatiquement chaque fois que le zoom a chang�.
     * Cette m�thode met � jour les coordonn�es des formes g�om�triques
     * d�clar�es dans les objets {@link RenderedLayer}.
     *
     * @param change The zoom <strong>change</strong> in <strong>device</strong> coordinate
     *        system, or <code>null</code> if unknow. If <code>null</code>, then all layers
     *        will be fully redrawn during the next rendering.
     */
    private void zoomChanged(final AffineTransform change) {
        if (change!=null && change.isIdentity()) {
            return;
        }
        assert Thread.holdsLock(this);
        for (int i=layerCount; --i>=0;) {
            layers[i].zoomChanged(change);
        }
    }

    /**
     * Invoked automatically everytime the scale changed. This method is usually (but not always)
     * invoked together with {@link #zoomChanged}. Note that some zoom changes do not imply a
     * scale change. For example the zoom change may be just a translation or a rotation.
     *
     * @param scaleFactor The new scale factor.
     */
    private void scaleChanged(final float scaleFactor) {
        final Float oldScale = this.scaleFactor;
        if (oldScale==null || oldScale.floatValue()!=scaleFactor) {
            final Float newScale = new Float(scaleFactor);
            this.scaleFactor = newScale;
            for (int i=layerCount; --i>=0;) {
                layers[i].listeners.firePropertyChange("scale", oldScale, newScale);
            }
            listeners.firePropertyChange("scale", oldScale, newScale);
        }
    }

    /**
     * Add a property change listener to the listener list. The listener is
     * registered for all properties. For example, adding or removing layers
     * may fire a <code>"preferredArea"</code> change events.
     *
     * @param listener The property change listener to be added
     */
    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        listeners.addPropertyChangeListener(listener);
    }

    /**
     * Add a <code>PropertyChangeListener</code> for a specific property.
     * The listener will be invoked only when that specific property changes.
     *
     * @param propertyName The name of the property to listen on.
     * @param listener     The PropertyChangeListener to be added.
     */
    public void addPropertyChangeListener(final String propertyName,
                                          final PropertyChangeListener listener)
    {
        listeners.addPropertyChangeListener(propertyName, listener);
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
     * Remove a PropertyChangeListener for a specific property.
     *
     * @param propertyName The name of the property that was listened on.
     * @param listener     The PropertyChangeListener to be removed.
     */
    public void removePropertyChangeListener(final String propertyName,
                                             final PropertyChangeListener listener)
    {
        listeners.removePropertyChangeListener(propertyName, listener);
    }

    /**
     * Efface les donn�es qui avaient �t� conserv�es dans une cache interne. L'appel
     * de cette m�thode permettra de lib�rer un peu de m�moire � d'autres fins. Elle
     * devrait �tre appel�e lorsque l'on sait qu'on n'affichera plus la carte avant
     * un certain temps. Par exemple la m�thode {@link java.applet.Applet#stop}
     * devrait appeller <code>clearCache()</code>. Notez que l'appel de cette m�thode
     * ne modifie aucunement le param�trage de la carte. Seulement, son prochain
     * tra�age sera plus lent, le temps que <code>Renderer</code> reconstruise les
     * caches internes.
     */
    private void clearCache() {
        assert Thread.holdsLock(this);
        for (int i=layerCount; --i>=0;) {
            layers[i].clearCache();
        }
        transforms.clear();
    }

    /**
     * Provides a hint that a renderer will no longer be accessed from a reference in user
     * space. The results are equivalent to those that occur when the program loses its
     * last reference to this renderer, the garbage collector discovers this, and finalize
     * is called. This can be used as a hint in situations where waiting for garbage
     * collection would be overly conservative.
     * <br><br>
     * <code>Renderer</code> defines this method to invoke {@link RenderedLayer#dispose} for
     * all layers. The results of referencing a renderer or any of its layers after a call to
     * <code>dispose()</code> are undefined. However, invoking this method more than once is
     * safe.
     *
     * @see RenderedLayer#dispose
     * @see PlanarImage#dispose
     */
    public synchronized void dispose() {
        flushOffscreenBuffers();
        offscreenBuffers = null;
        offscreenZRanges = null;
        final RenderedLayer[] layers = new RenderedLayer[layerCount];
        if (layerCount != 0) {
            System.arraycopy(this.layers, 0, layers, 0, layerCount);
        }
        removeAllLayers();
        for (int i=layerCount; --i>=0;) {
            layers[i].dispose();
        }
        clearCache();
        final PropertyChangeListener[] list = listeners.getPropertyChangeListeners();
        for (int i=list.length; --i>=0;) {
            listeners.removePropertyChangeListener(list[i]);
        }
    }

    /**
     * Returns a string representation of this renderer and all its {@link RenderedLayer}s.
     * This method is for debugging purpose only and may change in any future version.
     */
    public synchronized String toString() {
        sortLayers();
        final String lineSeparator = System.getProperty("line.separator", "\n");
        final StringBuffer buffer = new StringBuffer(Utilities.getShortClassName(this));
        buffer.append("[\"");
        buffer.append(getName(null));
        buffer.append("\", ");
        buffer.append(layerCount);
        buffer.append(" layers]");
        buffer.append(lineSeparator);
        for (int i=0; i<layerCount; i++) {
            buffer.append("    ");
            buffer.append(layers[i]);
            buffer.append(lineSeparator);
        }
        return buffer.toString();
    }
}
