/*
 * Geotools - OpenSource mapping toolkit
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

// Geometry
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;

// Graphics
import java.awt.Paint;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.Graphics2D;
import javax.swing.UIManager;
import javax.media.jai.GraphicsJAI;

// Collections
import java.util.List;
import java.util.ArrayList;

// Geotools dependencies
import org.geotools.units.Unit;
import org.geotools.math.Statistics;
import org.geotools.cs.Ellipsoid;
import org.geotools.cs.CoordinateSystem;
import org.geotools.ct.TransformException;
import org.geotools.renderer.geom.InteriorType;
import org.geotools.renderer.geom.GeoShape;
import org.geotools.renderer.geom.Polygon;
import org.geotools.renderer.geom.Isoline;
import org.geotools.resources.XMath;
import org.geotools.resources.XDimension2D;
import org.geotools.resources.XAffineTransform;
import org.geotools.resources.CTSUtilities;


/**
 * A layer for an {@link Isoline} object. Instances of this class are typically
 * used for isobaths. Each isobath (e.g. sea-level, 50 meters, 100 meters...)
 * require a different instance of <code>RenderedIsoline</code>.
 *
 * @version $Id: RenderedIsoline.java,v 1.7 2003/02/04 12:30:52 desruisseaux Exp $
 * @author Martin Desruisseaux
 */
public class RenderedIsoline extends RenderedLayer {
    /**
     * Set to <code>false</code> to disable clipping acceleration.
     * May be useful if you suspect that a bug is preventing proper
     * rendering.
     */
    private static final boolean ENABLE_CLIP = false;
    // TODO: NEED TO DEBUG. NEED TO TRACE INTO RenderingContext.clip

    /**
     * Default color for fills.
     */
    private static final Color FILL_COLOR = new Color(59,107,92);

    /**
     * The "preferred line tickness" relative to the isoline's resolution.
     * A value of 1 means that isoline might be drawn with a line as tick
     * as the isoline's resolution. A value of 0.25 means that isoline might
     * be drawn with a line of tickness equals to 1/4 of the isoline's resolution.
     */
    private static final double TICKNESS = 0.25;

    /**
     * The isoline data. The {@linkplain Isoline#getCoordinateSystem isoline's coordinate
     * system} is the one used for rendering when <code>paint(...)</code> was invoked for
     * the last time.   It may not be the same than this {@linkplain #getCoordinateSystem
     * layer's coordinate system}.
     */
    private final Isoline isoline;

    /**
     * Clipped isolines or polygons. A clipped isoline may be faster to renderer
     * than the full isoline. This list contains {@link GeoShape} objects.
     */
    private final List clipped = ENABLE_CLIP ? new ArrayList(4) : null;

    /**
     * Color for contour lines. Default to panel's foreground (usually black).
     */
    private Paint contour = UIManager.getColor("Panel.foreground");

    /**
     * Paint for filling holes. Default to panel's background (usually gray).
     */
    private Paint background = UIManager.getColor("Panel.background");

    /**
     * Paint for filling elevations.
     */
    private Paint foreground = FILL_COLOR;

    /**
     * The number of points in {@link #isoline}. For statistics purpose only.
     */
    private final int numPoints;

    /**
     * The renderer to use for painting polygons.
     * Will be created only when first needed.
     */
    private transient IsolineRenderer isolineRenderer;

    /**
     * Construct a layer for the specified isoline. The layer's coordinate system will be
     * set to the current {@linkplain Isoline#getCoordinateSystem isoline's coordinate
     * system}.
     */
    public RenderedIsoline(Isoline isoline) {
        isoline = (Isoline)isoline.clone(); // Remind: underlying data are shared, not cloned.
        this.isoline   = isoline;
        this.numPoints = isoline.getPointCount();
        final CoordinateSystem isolineCS = isoline.getCoordinateSystem();
        try {
            setCoordinateSystem(isolineCS);
        } catch (TransformException exception) {
            /*
             * Should not happen, since isoline use 2D coordinate systems. Rethrow it as an
             * illegal argument exception, which is not too far from the reality: the isoline
             * is not of the usual class.
             */
            final IllegalArgumentException e;
            e = new IllegalArgumentException(exception.getLocalizedMessage());
            e.initCause(exception);
            throw e;
        }
        setZOrder(isoline.value);
        /*
         * Compute the preferred area and the preferred pixel size.
         */
        final Rectangle2D  bounds = isoline.getBounds2D();
        final Statistics resStats = isoline.getResolution();
        if (resStats != null) {
            final double dx,dy;
            final double   resolution = resStats.mean();
            final Ellipsoid ellipsoid = CTSUtilities.getHeadGeoEllipsoid(isolineCS);
            if (ellipsoid != null) {
                // Transforms the resolution into a pixel size in the middle of 'bounds'.
                // Note: 'r' is the inverse of **apparent** ellipsoid's radius at latitude 'y'.
                //       For the inverse of "real" radius, we would have to swap sin and cos.
                final double   y = Math.toRadians(bounds.getCenterY());
                final double sin = Math.sin(y);
                final double cos = Math.cos(y);
                final double   r = XMath.hypot(sin/ellipsoid.getSemiMajorAxis(),
                                               cos/ellipsoid.getSemiMinorAxis());
                dy = Math.toDegrees(resolution*r);
                dx = dy*cos;
            } else {
                dx = dy = resolution;
            }
            setPreferredPixelSize(new XDimension2D.Double(TICKNESS*dx , TICKNESS*dy));
        }
        setPreferredArea(bounds);
        if (clipped != null) {
            clipped.add(isoline);
        }
        setTools(new Tools());
    }

    /**
     * Sets the contouring color or paint.
     * This paint will be used by all polygons.
     */
    public void setContour(final Paint paint) {
        contour = paint;
    }

    /**
     * Returns the contouring color or paint.
     */
    public Paint getContour() {
        return contour;
    }

    /**
     * Sets the filling color or paint. This paint
     * will be used only for closed polygons.
     */
    public void setForeground(final Paint paint) {
        foreground = paint;
    }

    /**
     * Returns the filling color or paint.
     */
    public Paint getForeground() {
        return foreground;
    }

    /**
     * Set the background color or paint. This information is needed in
     * order to allows <code>RenderedIsoline</code> to fill holes correctly.
     */
    public void setBackground(final Paint paint) {
        background = paint;
    }

    /**
     * Returns the background color or paint.
     */
    public Paint getBackground() {
        return background;
    }

    /**
     * The renderer for polygons. An instance of this class is attached to each instance
     * of {@link RenderedIsoline} when its <code>paint(...)</code> method is invoked for
     * the first time.  The <code>paint(...)</code> must initialize the fields before to
     * renderer polygons, and reset them to <code>null</code> once the rendering is completed.
     *
     * @version $Id: RenderedIsoline.java,v 1.7 2003/02/04 12:30:52 desruisseaux Exp $
     * @author Martin Desruisseaux
     */
    private final class IsolineRenderer implements Polygon.Renderer {
        /**
         * The minimum and maximum rendering resolution
         * in units of the isoline's coordinate system.
         */
        protected float minResolution, maxResolution;

        /**
         * The {@link Graphics2D} handler where to draw polygons.
         * Should be <code>null</code> once the rendering is completed.
         */
        protected Graphics2D graphics;

        /**
         * Returns the clip area in units of the isoline's coordinate system.
         * This is usually "real world" metres or degrees of latitude/longitude.
         */
        public Shape getClip() {
            return graphics.getClip();
        }
        
        /**
         * Returns the rendering resolution, in units of the isoline's coordinate system
         * (usually metres or degrees).  A larger resolution speed up rendering, while a
         * smaller resolution draw more precise map.
         *
         * @param  current The current rendering resolution.
         * @return the <code>current</code> rendering resolution if it still good enough,
         *         or a new resolution if a change is needed.
         */
        public float getRenderingResolution(float resolution) {
            if (resolution>=minResolution && resolution<=maxResolution) {
                return resolution;
            }
            return (minResolution + maxResolution)/2;
        }
        
        /**
         * Draw or fill a polygon. This polygon expose some internal state of {@link Isoline}.
         * <strong>Do not modify it, neither keep a reference to it after this method call</strong>
         * in order to avoid unexpected behaviour.
         */
        public void paint(Polygon polygon) {
            final InteriorType type = polygon.getInteriorType();
            if (InteriorType.ELEVATION.equals(type)) {
                graphics.setPaint(foreground);
                graphics.fill(polygon);
                if (foreground.equals(contour)) {
                    return;
                }
            } else if (InteriorType.DEPRESSION.equals(type)) {
                graphics.setPaint(background);
                graphics.fill(polygon);
                if (background.equals(contour)) {
                    return;
                }
            }
            graphics.setPaint(contour);
            graphics.draw(polygon);
        }

        /**
         * Invoked once after a series of polygons has been painted.
         *
         * @param rendered The total number of <em>rendered</em> points.
         * @param recomputed The number of points that has been recomputed
         *        (i.e. decompressed, decimated, projected and transformed).
         * @param resolution The mean resolution of rendered polygons.
         */
        public void paintCompleted(final int rendered, final int recomputed, double resolution) {
            renderer.statistics.addIsoline(numPoints, rendered, recomputed, resolution);
        }
    }

    /**
     * Draw the isoline.
     *
     * @param  context The set of transformations needed for transforming geographic
     *         coordinates (<var>longitude</var>,<var>latitude</var>) into pixels coordinates.
     * @throws TransformException If a transformation failed.
     */
    protected void paint(final RenderingContext context) throws TransformException {
        assert Thread.holdsLock(getTreeLock());
        /*
         * If the rendering coordinate system changed since last time,
         * then reproject the isoline and flush the cache.
         */
        final CoordinateSystem isolineCS = isoline.getCoordinateSystem();
        if (!context.mapCS.equals(isolineCS, false)) {
            isoline.setCoordinateSystem(context.mapCS);
            if (clipped != null) {
                clipped.clear();
                clipped.add(isoline);
            }
        }
        /*
         * Rendering acceleration: First performs the clip (if enabled),
         * then compute the decimation to use.
         */
        final Rectangle2D  bounds = isoline.getBounds2D();
        final AffineTransform  tr = context.getAffineTransform(context.mapCS, context.textCS);
        final Isoline      toDraw = (clipped!=null) ? (Isoline)context.clip(clipped) : isoline;
        if (toDraw != null) {
            final Graphics2D graphics = context.getGraphics();
            final Paint      oldPaint = graphics.getPaint();
            final Stroke    oldStroke = graphics.getStroke();
            final Ellipsoid ellipsoid = CTSUtilities.getHeadGeoEllipsoid(isolineCS);
            double r; // Estimation of the "real world" length (usually in meters) of one pixel.
            if (ellipsoid != null) {
                final Unit xUnit = isolineCS.getUnits(0);
                final Unit yUnit = isolineCS.getUnits(1);
                final double  R2 = 0.70710678118654752440084436210485; // sqrt(0.5)
                final double   x = Unit.DEGREE.convert(bounds.getCenterX(), xUnit);
                final double   y = Unit.DEGREE.convert(bounds.getCenterY(), yUnit);
                final double  dx = Unit.DEGREE.convert(R2/XAffineTransform.getScaleX0(tr), xUnit);
                final double  dy = Unit.DEGREE.convert(R2/XAffineTransform.getScaleY0(tr), yUnit);
                r = ellipsoid.orthodromicDistance(x-dx, y-dy, x+dy, y+dy);
            } else {
                // Assume a cartesian coordinate system.
                final double R2 = 1.4142135623730950488016887242097; // sqrt(2)
                r = R2/Math.sqrt((r=tr.getScaleX())*r + (r=tr.getScaleY())*r +
                                 (r=tr.getShearX())*r + (r=tr.getShearY())*r);
            }
            if (isolineRenderer == null) {
                isolineRenderer = new IsolineRenderer();
            }
            isolineRenderer.minResolution = (float)(renderer.minResolution*r);
            isolineRenderer.maxResolution = (float)(renderer.maxResolution*r);
            isolineRenderer.graphics      = graphics;
            try {
                toDraw.paint(isolineRenderer);
            } finally {
                isolineRenderer.graphics = null;
            }
            graphics.setStroke(oldStroke);
            graphics.setPaint (oldPaint);
        }
        context.addPaintedArea(XAffineTransform.transform(tr, bounds, bounds), context.textCS);
    }

    /**
     * A default set of tools for {@link RenderedIsoline} layer. An instance of this
     * class is automatically registered at the {@link RenderedIsoline} construction
     * stage.
     *
     * @version $Id: RenderedIsoline.java,v 1.7 2003/02/04 12:30:52 desruisseaux Exp $
     * @author Martin Desruisseaux
     */
    protected class Tools extends org.geotools.renderer.j2d.Tools {
        /**
         * Default constructor.
         */
        protected Tools() {
        }

        /**
         * Returns a tool tip text for the specified coordinates.
         * Default implementation delegate to {@link Isoline#getToolTipText}.
         *
         * @param  event The mouve event with geographic coordin�tes.
         * @return The tool tip text, or <code>null</code> if there
         *         in no tool tips for this location.
         */
        protected String getToolTipText(final GeoMouseEvent event) {
            final Point2D point = event.getMapCoordinate(null);
            if (point != null) {
                final String toolTips = isoline.getToolTipText(point, getLocale());
                if (toolTips != null) {
                    return toolTips;
                }
            }
            return super.getToolTipText(event);
        }
    }
}
