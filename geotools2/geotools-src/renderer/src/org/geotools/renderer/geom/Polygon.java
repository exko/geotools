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
package org.geotools.renderer.geom;

// Geometry and graphics
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.IllegalPathStateException;
import java.awt.geom.NoninvertibleTransformException;

// Collections
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;

// Input/Output
import java.io.Writer;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;

// Formatting
import java.util.Locale;
import java.text.NumberFormat;
import java.text.FieldPosition;

// Geotools dependencies
import org.geotools.units.Unit;
import org.geotools.cs.Ellipsoid;
import org.geotools.cs.Projection;
import org.geotools.cs.CoordinateSystem;
import org.geotools.cs.CoordinateSystemFactory;
import org.geotools.cs.ProjectedCoordinateSystem;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.ct.CannotCreateTransformException;
import org.geotools.ct.CoordinateTransformation;
import org.geotools.ct.TransformException;
import org.geotools.ct.MathTransform2D;
import org.geotools.ct.MathTransform;

// Miscellaneous
import org.geotools.util.WeakHashSet;
import org.geotools.math.Statistics;
import org.geotools.resources.XMath;
import org.geotools.resources.XArray;
import org.geotools.resources.Arguments;
import org.geotools.resources.Utilities;
import org.geotools.resources.XRectangle2D;
import org.geotools.resources.CTSUtilities;
import org.geotools.resources.renderer.Resources;
import org.geotools.resources.renderer.ResourceKeys;
import org.geotools.renderer.array.ArrayData;


/**
 * A single polygon. Each <code>Polygon</code> object can have its own {@link CoordinateSystem}
 * object, usually specified at construction time. A set of polygons can be built from an array
 * of (<var>x</var>,<var>y</var>) coordinates or from a geometric shape using one of
 * {@link #getInstances(float[],CoordinateSystem) getInstances(...)} factory methods.
 * <strong>Points given to factory methods should not contains map border.</strong>
 * Border points (orange points in the figure below) are treated specially and must
 * be specified using {@link #appendBorder appendBorder(...)} or
 * {@link #prependBorder prependBorder(...)} methods.
 *
 * <p align="center"><img src="doc-files/borders.png"></p>
 *
 * <TABLE WIDTH="80%" ALIGN="center" CELLPADDING="18" BORDER="4" BGCOLOR="#FFE0B0"><TR><TD>
 * <P ALIGN="justify"><STRONG>This class may change in a future version, hopefully toward
 * ISO-19107. Do not rely on it.</STRONG>
 * </TD></TR></TABLE>
 *
 * @version $Id: Polygon.java,v 1.5 2003/02/07 23:04:51 desruisseaux Exp $
 * @author Martin Desruisseaux
 *
 * @see Isoline
 */
public class Polygon extends GeoShape {
    /**
     * Num�ro de version pour compatibilit� avec des
     * bathym�tries enregistr�es sous d'anciennes versions.
     */
    private static final long serialVersionUID = 6197907210475790821L;

    /**
     * Small number for comparaisons (mostly in assertions).
     * Should be in the range of precision of <code>float</code> type.
     */
    private static final double EPS = 1E-6;

    /**
     * Projection � utiliser pour les calculs qui
     * exigent un syst�me de coordonn�es cart�sien.
     */
    private static final String CARTESIAN_PROJECTION = "Stereographic";

    /**
     * The enum value for <code>InteriorType == null</code>.
     */
    private static final int UNCLOSED = InteriorType.UNCLOSED;

    /**
     * Un des maillons de la cha�ne de polylignes, ou
     * <code>null</code> s'il n'y a aucune donn�e de
     * m�moris�e.
     */
    private Polyline data;

    /**
     * Transformation permettant de passer du syst�me de coordonn�es des points <code>data</code>
     * vers le syst�me de coordonn�es de ce polygone. {@link CoordinateTransformation#getSourceCS}
     * doit obligatoirement �tre le syst�me de coordonn�es de <code>data</code>, tandis que
     * {@link CoordinateTransformation#getTargetCS} doit �tre le syst�me de coordonn�es du polygone.
     * Lorsque ce polygone utilise le m�me syst�me de coordonn�es que <code>data</code> (ce qui
     * est le cas la plupart du temps), alors ce champ contiendra une transformation identit�.
     * Ce champ peut �tre nul si le syst�me de coordonn�es de <code>data</code> n'est pas connu.
     */
    private CoordinateTransformation coordinateTransform;

    /**
     * Rectangle englobant compl�tement tous les points de <code>data</code>. Ce
     * rectangle est une information tr�s utile pour rep�rer plus rapidement les
     * traits qui n'ont pas besoin d'�tre redessin�s (par exemple sous l'effet d'un zoom).
     * <strong>Le rectangle {@link Rectangle2D} r�f�renc� par ce champ ne doit jamais �tre
     * modifi�</strong>, car il peut �tre partag� par plusieurs objets {@link Polygon}.
     */
    private transient Rectangle2D dataBounds;

    /**
     * Rectangle englobant compl�tement les coordonn�es projet�es de ce polygone.
     * Ce champs est utilis� comme une cache pour la m�thode {@link #getBounds2D()}
     * afin de la rendre plus rapide.
     *
     * <strong>Le rectangle {@link Rectangle2D} r�f�renc� par ce champ ne doit jamais �tre
     * modifi�</strong>, car il peut �tre partag� par plusieurs objets {@link Polygon}.
     */
    private transient Rectangle2D bounds;

    /**
     * <code>true</code> if {@link #getPathIterator} will returns a flattened iterator.
     * In this case, there is no need to wrap it into a {@link FlatteningPathIterator}.
     */
    private transient boolean flattened;

    /**
     * Indique si cette forme a �t� ferm�e. Si le polygone a �t� ferm�, alors ce champ
     * aura la valeur {@link InteriorType#ELEVATION} ou {@link InteriorType#DEPRESSION}.
     */
    private byte interiorType = (byte) UNCLOSED;

    /**
     * R�solution moyenne du polygone. Ce champ contient la distance moyenne entre
     * deux points du polygone, ou {@link Float#NaN} si cette r�solution n'a pas
     * encore �t� calcul�e.
     */
    private float resolution = Float.NaN;

    /**
     * The resolution to apply at rendering time, as a multiple of
     * <code>{@link #resolution}/{@link #RESOLUTION_FACTOR}</code>.
     * The value 0 means that all data should be used.
     */
    private transient byte renderingResolution;

    /**
     * A constant for compacting {@link #renderingResolution} as a single <code>byte</code>.
     */
    private static final int RESOLUTION_FACTOR = 4;

    /**
     * R�f�rence molle vers un tableau <code>float[]</code>. Ce tableau est utilis�
     * pour conserver en m�moire des points qui ont d�j� �t� projet�s ou transform�s.
     */
    private transient PolygonCache cache;

    /**
     * Construit un polygone initialement vide.
     */
    private Polygon(final CoordinateTransformation coordinateTransform) {
        this.coordinateTransform = coordinateTransform;
        if (coordinateTransform != null) {
            CoordinateSystem cs;
            if ((cs=coordinateTransform.getSourceCS()).getDimension() != 2 ||
                (cs=coordinateTransform.getTargetCS()).getDimension() != 2)
            {
                throw new IllegalArgumentException(org.geotools.resources.cts.Resources.format(
                   org.geotools.resources.cts.ResourceKeys.ERROR_CANT_REDUCE_TO_TWO_DIMENSIONS_$1, cs));
            }
        }
        flattened = checkFlattenedShape();
    }

    /**
     * Construct an empty polygon. Use {@link #append} to add points.
     *
     * @param coordinateSystem The coordinate system to use for all
     *        points in this polygon, or <code>null</code> if unknow.
     */
    public Polygon(final CoordinateSystem coordinateSystem) {
        this(getIdentityTransform(getCoordinateSystem2D(coordinateSystem)));
    }

    /**
     * Construct a new polygon with the same data than the specified
     * polygon. The new polygon will have a copy semantic. However,
     * implementation try to share as much internal data as possible
     * in order to reduce memory footprint.
     */
    public Polygon(final Polygon polygon) {
        super(polygon);
        data                = Polyline.clone(polygon.data);
        coordinateTransform = polygon.coordinateTransform;
        dataBounds          = polygon.dataBounds;
        bounds              = polygon.bounds;
        flattened           = polygon.flattened;
        resolution          = polygon.resolution;
        interiorType        = polygon.interiorType;
    }

    /**
     * Construct a closed polygon with the specified rectangle.
     * The new polygon will be empty if the rectangle was empty
     * or contains at least one <code>NaN</code> value.
     *
     * @param rectangle Rectangle to copy in the new polygon.
     * @param coordinateSystem The rectangle's coordinate system,
     *        or <code>null</code> if unknow.
     */
    public Polygon(final Rectangle2D rectangle, final CoordinateSystem coordinateSystem) {
        this(coordinateSystem);
        if (!rectangle.isEmpty()) {
            final float xmin = (float)rectangle.getMinX();
            final float ymin = (float)rectangle.getMinY();
            final float xmax = (float)rectangle.getMaxX();
            final float ymax = (float)rectangle.getMaxY();
            final Polyline[] polylines = Polyline.getInstances(new float[] {
                xmin,ymin,
                xmax,ymin,
                xmax,ymax,
                xmin,ymax
            });
            if (polylines.length == 1) {
                // length may be 0 or 2 if some points contain NaN
                data = polylines[0];
            }
        }
    }

    /**
     * Construit des polygones � partir des coordonn�es (<var>x</var>,<var>y</var>) sp�cifi�es.
     * Les valeurs <code>NaN</code> au d�but et � la fin de <code>data</code> seront ignor�es.
     * Celles qui apparaissent au milieu auront pour effet de s�parer le trait en plusieurs
     * polygones.
     *
     * @param  data Tableau de coordonn�es (peut contenir des NaN). Ces donn�es seront copi�es,
     *         de sorte que toute modification future de <code>data</code> n'aura pas d'impact
     *         sur les polygones cr��s.
     * @param  coordinateSystem Syst�me de coordonn�es des points de <code>data</code>.
     *         Cet argument peut �tre nul si le syst�me de coordonn�es n'est pas connu.
     * @return Tableau de polygones. Peut avoir une longueur de 0, mais ne sera jamais nul.
     */
    public static Polygon[] getInstances(final float[] data, final CoordinateSystem coordinateSystem) {
        final Polyline[] polylines = Polyline.getInstances(data);
        final Polygon[]  polygons  = new Polygon[polylines.length];
        final CoordinateTransformation ct = getIdentityTransform(coordinateSystem);
        for (int i=0; i<polygons.length; i++) {
            final Polygon polygon = new Polygon(ct);
            polygon.data      = polylines[i];
            polygon.flattened = polygon.checkFlattenedShape();
            polygons[i]       = polygon;
        }
        return polygons;
    }

    /**
     * Construit des polygones � partir de la forme g�om�trique sp�cifi�e. Si <code>shape</code>
     * est d�j� de la classe <code>Polygon</code>, il sera retourn� dans un tableau de longueur 1.
     * Dans les autres cas, cette m�thode peut retourner un tableau de longueur de 0, mais ne
     * retourne jamais <code>null</code>.
     *
     * @param  shape Forme g�om�trique � copier dans un ou des polygones.
     * @param  coordinateSystem Syst�me de coordonn�es des points de <code>shape</code>.
     *         Cet argument peut �tre nul si le syst�me de coordonn�es n'est pas connu.
     * @return Tableau de polygones. Peut avoir une longueur de 0, mais ne sera jamais nul.
     */
    public static Polygon[] getInstances(final Shape shape, CoordinateSystem coordinateSystem) {
        coordinateSystem = getCoordinateSystem2D(coordinateSystem);
        if (shape instanceof Polygon) {
            return new Polygon[] {(Polygon) shape};
        }
        final CoordinateTransformation ct = getIdentityTransform(coordinateSystem);
        final List               polygons = new ArrayList();
        final PathIterator            pit = shape.getPathIterator(null, getFlatness(shape));
        final float[]              buffer = new float[6];
        float[]                     array = new float[64];
        while (!pit.isDone()) {
            if (pit.currentSegment(array) != PathIterator.SEG_MOVETO) {
                throw new IllegalPathStateException();
            }
            /*
             * Une fois entr� dans ce bloc, le tableau <code>array</code> contient
             * d�j� le premier point aux index 0 (pour x) et 1 (pour y). On ajoute
             * maintenant les autres points tant qu'ils correspondent � des
             * instructions <code>LINETO</code>.
             */
            int index = 2;
            InteriorType interiorType = null;
      loop: for (pit.next(); !pit.isDone(); pit.next()) {
                switch (pit.currentSegment(buffer)) {
                    case PathIterator.SEG_LINETO: {
                        if (index >= array.length) {
                            array = XArray.resize(array, 2*index);
                        }
                        System.arraycopy(buffer, 0, array, index, 2);
                        index += 2;
                        break;
                    }
                    case PathIterator.SEG_MOVETO: {
                        break loop;
                    }
                    case PathIterator.SEG_CLOSE: {
                        interiorType = InteriorType.FLAT;
                        pit.next();
                        break loop;
                    }
                    default: {
                        throw new IllegalPathStateException();
                    }
                }
            }
            /*
             * Construit les polygones qui correspondent �
             * la forme g�om�trique qui vient d'�tre balay�e.
             */
            final Polyline[] polylines = Polyline.getInstances(array, 0, index);
            for (int i=0; i<polylines.length; i++) {
                final Polygon polygon = new Polygon(ct);
                polygon.data = polylines[i];
                polygon.flattened = polygon.checkFlattenedShape();
                polygon.close(interiorType);
                polygons.add(polygon);
            }
        }
        return (Polygon[]) polygons.toArray(new Polygon[polygons.size()]);
    }

    /**
     * Returns a suggested value for the <code>flatness</code> argument in
     * {@link Shape#getPathIterator(AffineTransform,double)} for the specified shape.
     */
    static double getFlatness(final Shape shape) {
        final Rectangle2D bounds = shape.getBounds2D();
        return 0.025*Math.max(bounds.getHeight(), bounds.getWidth());
    }

    /**
     * Same as {@link CTSUtilities#getCoordinateSystem2D}, but wrap the {@link TransformException}
     * into an {@link IllegalArgumentException}. Used for constructors only. Other methods still
     * use the method throwing a transform exception.
     */
    private static CoordinateSystem getCoordinateSystem2D(final CoordinateSystem cs)
            throws IllegalArgumentException
    {
        try {
            return CTSUtilities.getCoordinateSystem2D(cs);
        } catch (TransformException exception) {
            throw new IllegalArgumentException(exception.getLocalizedMessage());
        }
    }

    /**
     * Retourne le syst�me de coordonn�es natif des points de
     * {@link #data}, ou <code>null</code> s'il n'est pas connu.
     */
    private CoordinateSystem getInternalCS() {
        // copy 'coordinateTransform' reference in order to avoid synchronization
        final CoordinateTransformation coordinateTransform = this.coordinateTransform;
        return (coordinateTransform!=null) ? coordinateTransform.getSourceCS() : null;
    }

    /**
     * Returns the polygon's coordinate system, or <code>null</code> if unknow.
     */
    public CoordinateSystem getCoordinateSystem() {
        // copy 'coordinateTransform' reference in order to avoid synchronization
        final CoordinateTransformation coordinateTransform = this.coordinateTransform;
        return (coordinateTransform!=null) ? coordinateTransform.getTargetCS() : null;
    }

    /**
     * Retourne la transformation qui permet de passer du syst�me de coordonn�es
     * des points {@link #data} vers le syst�me de coordonn�es sp�cifi�.   Si au
     * moins un des syst�mes de coordonn�es n'est pas connu, alors cette m�thode
     * retourne <code>null</code>.
     *
     * @throws CannotCreateTransformException Si la transformation ne peut pas �tre cr��e.
     */
    final CoordinateTransformation getTransformationFromInternalCS(final CoordinateSystem cs)
            throws CannotCreateTransformException
    {
        // copy 'coordinateTransform' reference in order to avoid synchronization
        final CoordinateTransformation coordinateTransform = this.coordinateTransform;
        if (cs!=null && coordinateTransform!=null) {
            if (cs.equals(coordinateTransform.getTargetCS(), false)) {
                return coordinateTransform;
            }
            return getCoordinateTransformation(coordinateTransform.getSourceCS(), cs);
        }
        return null;
    }

    /**
     * Returns a math transform for the specified transformations.
     * If no transformation is available, or if it is the identity
     * transform, then this method returns <code>null</code>. This
     * method accept null argument.
     */
    static MathTransform2D getMathTransform2D(final CoordinateTransformation transformation) {
        if (transformation != null) {
            final MathTransform transform = transformation.getMathTransform();
            if (!transform.isIdentity()) {
                return (MathTransform2D) transform;
            }
        }
        return null;
    }

    /**
     * Set the polygon's coordinate system. Calling this method is equivalents
     * to reproject all polygon's points from the old coordinate system to the
     * new one.
     *
     * @param  The new coordinate system. A <code>null</code> value reset the
     *         coordinate system given at construction time.
     * @throws TransformException If a transformation failed. In case of failure,
     *         the state of this object will stay unchanged (as if this method has
     *         never been invoked).
     */
    public synchronized void setCoordinateSystem(CoordinateSystem coordinateSystem)
            throws TransformException
    {
        // Do not use 'Polygon.getCoordinateSystem2D', since
        // we want a 'TransformException' in case of failure.
        coordinateSystem = CTSUtilities.getCoordinateSystem2D(coordinateSystem);
        if (coordinateSystem == null) {
            coordinateSystem = getInternalCS();
            // May still null. Its ok.
        }
        final CoordinateTransformation transformCandidate =
                getTransformationFromInternalCS(coordinateSystem);
        /*
         * Compute bounds now. The getBounds2D(...) method scan every point.
         * Concequently, if a exception must be throws, it will be thrown now.
         */
        bounds = Polyline.getBounds2D(data, (MathTransform2D)transformCandidate.getMathTransform());
        /*
         * Store the new coordinate transform
         * only after projection succeded.
         */
        this.coordinateTransform = transformCandidate;
        this.resolution = Float.NaN;
        this.cache = null;
        this.flattened = checkFlattenedShape();
    }

    /**
     * Indique si la transformation sp�cifi�e est la transformation identit�e.
     * Une transformation nulle (<code>null</code>) est consid�r�e comme �tant
     * une transformation identit�e.
     */
    private static boolean isIdentity(final CoordinateTransformation coordinateTransform) {
        return coordinateTransform==null || coordinateTransform.getMathTransform().isIdentity();
    }

    /**
     * Test if this polygon is empty. An
     * empty polygon contains no point.
     */
    public synchronized boolean isEmpty() {
        return Polyline.getPointCount(data) == 0;
    }

    /**
     * Return the bounding box of this polygon, including its possible
     * borders. This method uses a cache, such that after a first calling,
     * the following calls should be fairly quick.
     *
     * @return A bounding box of this polygons. Changes to the
     *         fields of this rectangle will not affect the cache.
     */
    public synchronized Rectangle2D getBounds2D() {
        return (Rectangle2D) getCachedBounds().clone();
    }

    /**
     * Returns the smallest bounding box containing {@link #getBounds2D}.
     *
     * @deprecated This method is required by the {@link Shape} interface,
     *             but it doesn't provides enough precision for most cases.
     *             Use {@link #getBounds2D()} instead.
     */
    public synchronized Rectangle getBounds() {
        final Rectangle bounds = new Rectangle();
        bounds.setRect(getCachedBounds()); // 'setRect' effectue l'arrondissement correct.
        return bounds;
    }

    /**
     * Retourne un rectangle englobant tous les points de {@link #data}. Parce que cette m�thode
     * retourne directement le rectangle de la cache et non une copie, le rectangle retourn� ne
     * devrait jamais �tre modifi�.
     *
     * @return Un rectangle englobant toutes les points de {@link #data}.
     *         Ce rectangle peut �tre vide, mais ne sera jamais nul.
     */
    private Rectangle2D getDataBounds() {
        // assert Thread.holdsLock(this);
        // Can't make this assertion, because this method is invoked
        // by {@link #getCachedBounds}. See the later for details.

        if (dataBounds == null) {
            dataBounds = getBounds(data, null);
            if (isIdentity(coordinateTransform)) {
                bounds = dataBounds; // Avoid computing the same rectangle two times
            }
        }
        assert equalsEps(getBounds(data, null), dataBounds) : dataBounds;
        return dataBounds;
    }

    /**
     * Return the bounding box of this isoline. This methode returns
     * a direct reference to the internally cached bounding box. DO
     * NOT MODIFY!
     */
    final Rectangle2D getCachedBounds() {
        assert Thread.holdsLock(this);
        if (bounds == null) {
            bounds = getBounds(data, coordinateTransform);
            if (isIdentity(coordinateTransform)) {
                dataBounds = bounds; // Avoid computing the same rectangle two times
            }
        }
        assert equalsEps(getBounds(data, coordinateTransform), bounds) : bounds;
        return bounds;
    }

    /**
     * Retourne un rectangle englobant tous les points projet�s dans le syst�me de coordonn�es
     * sp�cifi�. Cette m�thode tentera de retourner un des rectangles de la cache interne lorsque
     * appropri�. Parce que cette m�thode peut retourner directement le rectangle de la cache et
     * non une copie, le rectangle retourn� ne devrait jamais �tre modifi�.
     *
     * @param  Le syst�me de coordonn�es selon lequel projeter les points.
     * @return Un rectangle englobant tous les points de ce polygone.
     *         Ce rectangle peut �tre vide, mais ne sera jamais nul.
     * @throws TransformException si une projection cartographique a �chou�e.
     */
    private Rectangle2D getCachedBounds(final CoordinateSystem coordinateSystem)
            throws TransformException
    {
        // assert Thread.holdsLock(this);
        // Can't make this assertion, because {@link #intersects(Polygon,boolean)} invokes
        // this method without synchronization on this polygon. In doesn't hurt as long as
        // {@link #intersectsPolygon} and {@link #intersectsEdge} are private methods.

        if (Utilities.equals(getInternalCS(),       coordinateSystem)) return getDataBounds();
        if (Utilities.equals(getCoordinateSystem(), coordinateSystem)) return getCachedBounds();
        Rectangle2D bounds = Polyline.getBounds2D(data, getMathTransform2D(coordinateTransform));
        if (bounds == null) {
            bounds = new Rectangle2D.Float();
        }
        return bounds;
    }

    /**
     * Retourne en rectangle englobant tous les points de <code>data</code>.  Cette m�thode ne
     * devrait �tre appel�e que dans un contexte o� l'on sait que la projection cartographique
     * ne devrait jamais �chouer.
     *
     * @param  data Un des maillons de la cha�ne de tableaux de points (peut �tre nul).
     * @param  coordinateTransform Transformation � appliquer sur les points de <code>data</code>.
     * @return Un rectangle englobant toutes les points de <code>data</code>.
     *         Ce rectangle peut �tre vide, mais ne sera jamais nul.
     */
    private static Rectangle2D getBounds(final Polyline data,
                                         final CoordinateTransformation coordinateTransform)
    {
        Rectangle2D bounds;
        try {
            bounds = Polyline.getBounds2D(data, getMathTransform2D(coordinateTransform));
            if (bounds == null) {
                assert Polyline.getPointCount(data) == 0;
                bounds = new Rectangle2D.Float();
            }
        } catch (TransformException exception) {
            // Should not happen, since {@link #setCoordinateSystem}
            // has already successfully projected every points.
            unexpectedException("getBounds2D", exception);
            bounds = null;
        }
        return bounds;
    }

    /**
     * Check if two rectangles are almost equals (except for an epsilon value).    If one or
     * both argument is <code>null</code>, then this method does nothing. This method occurs
     * when one rectangle come from the cache and hasn't been computed yet.   This method is
     * used for assertions only.
     */
    private static boolean equalsEps(final Rectangle2D expected, final Rectangle2D actual) {
        if (expected==null || actual==null) {
            return true;
        }
        final double eps = EPS * XMath.hypot(expected.getCenterX(), expected.getCenterY());
        return Math.abs(expected.getMinX() - actual.getMinX()) <= eps &&
               Math.abs(expected.getMinY() - actual.getMinY()) <= eps &&
               Math.abs(expected.getMaxX() - actual.getMaxX()) <= eps &&
               Math.abs(expected.getMaxY() - actual.getMaxY()) <= eps;
    }

    /**
     * Indique si la coordonn�e (<var>x</var>,<var>y</var>) sp�cifi�e est � l'int�rieur
     * de ce polygone. Le polygone doit avoir �t� ferm� avant l'appel de cette m�thode
     * (voir {@link #close}), sans quoi cette m�thode retournera toujours <code>false</code>.
     *
     * @param  x Coordonn�e <var>x</var> du point � tester.
     * @param  y Coordonn�e <var>y</var> du point � tester.
     * @param  transformation Transformation � utiliser pour convertir les points de {@link #data},
     *         ou <code>null</code> pour ne pas faire de transformation. Si une transformation
     *         non-nulle est sp�cifi�e, elle devrait avoir �t� obtenue par un appel � la m�thode
     *         <code>getTransformationFromInternalCS(targetCS)</code>. Tous les points du polygone
     *         seront alors projet�s selon le syst�me de coordonn�es <code>targetCS</code>. Autant
     *         que possible, il est plus efficace de ne calculer que la projection inverse du point
     *         (<var>x</var>,<var>y</var>) et de sp�cifier <code>null</code> pour cet argument.
     * @return <code>true</code> si le point est � l'int�rieur de ce polygone.
     *
     * @author Andr� Gosselin (version originale en C)
     * @author Martin Desruisseaux (adaptation pour le Java)
     */
    private boolean contains(final float x, final float y,
                             final CoordinateTransformation transformation)
    {
        if (interiorType == UNCLOSED) {
            return false;
        }
        /*
         * Imaginez une ligne droite partant du point (<var>x</var>,<var>y</var>)
         * et allant jusqu'� l'infini � droite du point (c'est-�-dire vers l'axe
         * des <var>x</var> positifs). On comptera le nombre de fois que le polygone
         * intercepte cette ligne. Si ce nombre est impair, le point est � l'int�rieur
         * du polygone. La variable <code>nInt</code> fera ce comptage.
         */
        int   nInt                 = 0;
        int   intSuspended         = 0;
        int   nPointsToRecheck     = 0;
        final Point2D.Float nextPt = new Point2D.Float();
        final Polyline.Iterator it = new Polyline.Iterator(data, getMathTransform2D(transformation));
        float x1                   = Float.NaN;
        float y1                   = Float.NaN;
        /*
         * Extrait un premier point. Il y aura un probl�me dans l'algorithme qui suit
         * si le premier point est sur la m�me ligne horizontale que le point � v�rifier.
         * Pour contourner le probl�me, on recherchera le premier point qui n'est pas sur
         * la m�me ligne horizontale.
         */
        while (true) {
            final float x0=x1;
            final float y0=y1;
            nPointsToRecheck++;
            if (it.next(nextPt) == null) {
                return false;
            }
            x1 = nextPt.x;
            y1 = nextPt.y;
            if (y1 != y) break;
            /*
             * V�rifie si le point tombe exactement
             * sur le segment (x0,y0)-(x1-y1). Si oui,
             * ce n'est pas la peine d'aller plus loin.
             */
            if (x0 < x1) {
                if (x>=x0 && x<=x1) return true;
            } else {
                if (x>=x1 && x<=x0) return true;
            }
        }
        /*
         * Balaye tous les points du polygone. Lorsque le dernier point sera
         * extrait, la variable <code>count</code> sera ajust�e de fa�on � ne
         * rebalayer que les points qui doivent �tre repass�s.
         */
        for (int count=-1; count!=0; count--) {
            /*
             * Obtient le point suivant. Si on a atteint la fin du polygone,
             * alors on refermera le polygone si ce n'�tait pas d�j� fait.
             * Si le polygone avait d�j� �t� referm�, alors ce sera la fin
             * de la boucle.
             */
            final float x0=x1;
            final float y0=y1;
            if (it.next(nextPt) == null) {
                count = nPointsToRecheck+1;
                nPointsToRecheck = 0;
                it.rewind();
                continue;
            }
            x1=nextPt.x;
            y1=nextPt.y;
            /*
             * On dispose maintenant d'un segment de droite allant des coordonn�es
             * (<var>x0</var>,<var>y0</var>) jusqu'� (<var>x1</var>,<var>y1</var>).
             * Si on s'apper�oit que le segment de droite est compl�tement au dessus
             * ou compl�tement en dessous du point (<var>x</var>,<var>y</var>), alors
             * on sait qu'il n'y a pas d'intersection � droite et on continue la boucle.
             */
            if (y0 < y1) {
                if (y<y0 || y>y1) continue;
            } else {
                if (y<y1 || y>y0) continue;
            }
            /*
             * On sait maintenant que notre segment passe soit � droite, ou soit � gauche
             * de notre point. On calcule maintenant la coordonn�e <var>xi</var> � laquelle
             * � lieu l'intersection (avec la droite horizontale passant par notre point).
             */
            final float dy = y1-y0;
            final float xi = x0 + (x1-x0)*(y-y0)/dy;
            if (!Float.isInfinite(xi) && !Float.isNaN(xi)) {
                /*
                 * Si l'intersection est compl�tement � gauche du point, alors il n'y
                 * a �videmment pas d'intersection � droite et on continue la boucle.
                 * Sinon, si l'intersection se fait exactement � la coordonn�e <var>x</var>
                 * (c'est peu probable...), alors notre point est exactement sur la bordure
                 * du polygone et le traitement est termin�.
                 */
                if (x >  xi) continue;
                if (x == xi) return true;
            } else {
                /*
                 * Un traitement particulier est fait si le segment est horizontal. La valeur
                 * <var>xi</var> n'est pas valide (on peut voir �a comme si l'intersection se
                 * faisait partout sur la droite plut�t qu'en un seul point). Au lieu de faire
                 * les v�rifications avec <var>xi</var>, on les fera plut�t avec les <var>x</var>
                 * minimal et maximal du segment.
                 */
                if (x0 < x1) {
                    if (x >  x1) continue;
                    if (x >= x0) return true;
                } else {
                    if (x >  x0) continue;
                    if (x >= x1) return true;
                }
            }
            /*
             * On sait maintenant qu'il y a une intersection � droite. En principe, il
             * suffirait d'incr�menter 'nInt'. Toutefois, on doit faire attention au cas
             * cas o� <var>y</var> serait exactement � la hauteur d'une des extr�mit�s du
             * segment. Y a t'il intersection ou pas? �a d�pend si les prochains segments
             * continuent dans la m�me direction ou pas. On ajustera un drapeau, de sorte
             * que la d�cision d'incr�menter 'nInt' ou pas sera prise plus tard dans la
             * boucle, quand les autres segments auront �t� examin�s.
             */
            if (x0==x1 && y0==y1) {
                continue;
            }
            if (y==y0 || y==y1) {
                final int sgn=XMath.sgn(dy);
                if (sgn != 0) {
                    if (intSuspended!=0) {
                        if (intSuspended==sgn) nInt++;
                        intSuspended=0;
                    } else {
                        intSuspended=sgn;
                    }
                }
            }
            else nInt++;
        }
        /*
         * Si le nombre d'intersection � droite du point est impaire,
         * alors le point est � l'int�rieur du polygone. Sinon, il est
         * � l'ext�rieur.
         */
        return (nInt & 1)!=0;
    }

    /**
     * Indique si la coordonn�e (<var>x</var>,<var>y</var>) sp�cifi�e est � l'int�rieur
     * de ce polygone. Les coordonn�es du point doivent �tre exprim�es selon le syst�me
     * de coordonn�es du polygone, soit {@link #getCoordinateSystem()}. Le polygone doit
     * aussi avoir �t� ferm� avant l'appel de cette m�thode (voir {@link #close}), sans
     * quoi cette m�thode retournera toujours <code>false</code>.
     */
    public synchronized boolean contains(double x, double y) {
        // IMPLEMENTATION NOTE: The polygon's native point array ({@link #data}) and the
        // (x,y) point may use different coordinate systems. For efficiency raisons, the
        // (x,y) point is projected to the "native" polygon's coordinate system  instead
        // of projecting all polygon's points. As a result, point very close to the polygon's
        // edge may appear inside (when viewed on screen) while this method returns <code>false</code>,
        // and vis-versa. This is because some projections transform straight lines
        // into curves, but the Polygon class ignore curves and always use straight
        // lines between any two points.
        if (coordinateTransform!=null) try {
            final MathTransform transform = coordinateTransform.getMathTransform();
            if (!transform.isIdentity()) {
                Point2D point = new Point2D.Double(x,y);
                point = ((MathTransform2D) transform.inverse()).transform(point, point);
                x = point.getX();
                y = point.getY();
            }
        } catch (TransformException exception) {
            // Si la projection a �chou�e, alors le point est probablement en dehors
            // du polygone (puisque tous les points du polygone sont projetables).
            return false;
        }
        /*
         * On v�rifie d'abord si le rectangle 'dataBounds' contient
         * le point, avant d'appeler la co�teuse m�thode 'contains'.
         */
        return getDataBounds().contains(x,y) && contains((float)x, (float)y, null);
    }

    /**
     * V�rifie si un point <code>pt</code> est � l'int�rieur de ce polygone. Les coordonn�es
     * du point doivent �tre exprim�es selon le syst�me de coordonn�es du polygone, soit
     * {@link #getCoordinateSystem()}. Le polygone doit aussi avoir �t� ferm� avant l'appel
     * de cette m�thode (voir {@link #close}), sans quoi cette m�thode retournera toujours
     * <code>false</code>.
     */
    public boolean contains(final Point2D pt) {
        return contains(pt.getX(), pt.getY());
    }

    /**
     * Test if the interior of this contour entirely contains the given rectangle.
     * The rectangle's coordinates must expressed in this contour's coordinate
     * system (as returned by {@link #getCoordinateSystem}).
     */
    public synchronized boolean contains(final Rectangle2D rect) {
        return containsPolygon(new Polygon(rect, getCoordinateSystem()));
    }

    /**
     * Test if the interior of this polygon
     * entirely contains the given shape.
     */
    public synchronized boolean contains(final Shape shape) {
        if (shape instanceof Polygon) {
            return containsPolygon((Polygon) shape);
        }
        final Polygon[] polygons = getInstances(shape, getCoordinateSystem());
        for (int i=0; i<polygons.length; i++) {
            if (!containsPolygon(polygons[i])) {
                return false;
            }
        }
        return polygons.length!=0;
    }

    /**
     * Test if the interior of this polygon
     * entirely contains the given polygon.
     */
    private boolean containsPolygon(final Polygon shape) {
        /*
         * Cette m�thode retourne <code>true</code> si ce polygone contient
         * au moins un point de <code>shape</code> et qu'il n'y a aucune
         * intersection entre <code>shape</code> et <code>this</code>.
         */
        if (interiorType != UNCLOSED) try {
            final CoordinateSystem coordinateSystem = getInternalCS();
            if (getDataBounds().contains(shape.getCachedBounds(coordinateSystem))) {
                final Point2D.Float firstPt = new Point2D.Float();
                final  Line2D.Float segment = new  Line2D.Float();
                final Polyline.Iterator  it = new Polyline.Iterator(shape.data,
                                          shape.getMathTransform2D(
                                          shape.getTransformationFromInternalCS(coordinateSystem)));
                if (it.next(firstPt)!=null && contains(firstPt.x, firstPt.y, null)) {
                    segment.x2 = firstPt.x;
                    segment.y2 = firstPt.y;
                    do if (!it.next(segment)) {
                        if (shape.interiorType==UNCLOSED || isSingular(segment)) {
                            return true;
                        }
                        segment.x2 = firstPt.x;
                        segment.y2 = firstPt.y;
                    } while (!intersects(segment));
                }
            }
        } catch (TransformException exception) {
            // Conservatly return 'false' if some points from 'shape' can't be projected into
            // {@link #data}'s coordinate system.   This behavior is compliant with the Shape
            // specification. Futhermore, those points are probably outside this polygon since
            // all polygon's points are projectable.
        }
        return false;
    }

    /**
     * Indique si les points (x1,y1) et (x2,y2)
     * de la ligne sp�cifi�e sont identiques.
     */
    private static boolean isSingular(final Line2D.Float segment) {
        return Float.floatToIntBits(segment.x1)==Float.floatToIntBits(segment.x2) &&
               Float.floatToIntBits(segment.y1)==Float.floatToIntBits(segment.y2);
    }

    /**
     * Determine si la ligne <code>line</code> intercepte une des lignes de
     * ce polygone. Le polygone sera automatiquement referm� si n�cessaire;
     * il n'est donc pas n�cessaire que le dernier point r�p�te le premier.
     *
     * @param  line Ligne dont on veut d�terminer si elle intercepte ce polygone.
     *         Cette ligne doit obligatoirement �tre exprim�e selon le syst�me de
     *         coordonn�es natif de {@link #array}, c'est-�-dire {@link #getInternalCS}.
     * @return <code>true</code> si la ligne <code>line</code> intercepte ce poylgone.
     */
    private boolean intersects(final Line2D line) {
        final Point2D.Float firstPt = new Point2D.Float();
        final  Line2D.Float segment = new  Line2D.Float();
        final Polyline.Iterator  it = new Polyline.Iterator(data, null); // Ok m�me si 'data' est nul.
        if (it.next(firstPt) != null) {
            segment.x2 = firstPt.x;
            segment.y2 = firstPt.y;
            do if (!it.next(segment)) {
                if (interiorType==UNCLOSED || isSingular(segment)) {
                    return false;
                }
                segment.x2 = firstPt.x;
                segment.y2 = firstPt.y;
            } while (!segment.intersectsLine(line));
            return true;
        }
        return false;
    }

    /**
     * Tests if the interior of the contour intersects the interior of a specified rectangle.
     * The rectangle's coordinates must expressed in this contour's coordinate
     * system (as returned by {@link #getCoordinateSystem}).
     */
    public synchronized boolean intersects(final Rectangle2D rect) {
        return intersectsPolygon(new Polygon(rect, getCoordinateSystem()));
    }

    /**
     * Tests if the interior of the contour intersects the interior of a specified shape.
     * The shape's coordinates must expressed in this contour's coordinate
     * system (as returned by {@link #getCoordinateSystem}).
     */
    public synchronized boolean intersects(final Shape shape) {
        if (shape instanceof Polygon) {
            return intersectsPolygon((Polygon) shape);
        }
        final Polygon[] polygons = getInstances(shape, getCoordinateSystem());
        for (int i=0; i<polygons.length; i++) {
            if (intersectsPolygon(polygons[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test if this polygon intercepts a specified polygon.
     *
     * If this polygon is <em>closed</em> (if it is an island or a lake),
     * this method will return <code>true</code> if at least one point of
     * <code>s</code> lies inside this polygons. If this polygons is not
     * closed, then this method will return the same thing as
     * {@link #intersectsEdge}.
     */
    private boolean intersectsPolygon(final Polygon shape) {
        return intersects(shape, interiorType==UNCLOSED);
    }

    /**
     * Test if the edge of this polygons intercepts the edge of a
     * specified polygons.
     *
     * This should never happen with an error free bathymery map. However,
     * it could happen if the two polygons don't use the same units. For
     * example, this method may be use to test if an isoline of 15 degrees
     * celsius intercepts an isobath of 30 meters.
     *
     * @param s polygons to test.
     * @return <code>true</code> If an intersection is found.
     */
    final boolean intersectsEdge(final Polygon shape) {
        return intersects(shape, true);
    }

    /**
     * Impl�mentation des m�thodes <code>intersects[Polygon|Edge](Polygon)</code>.
     *
     * @param  shape polygones � v�rifier.
     * @param  checkEdgeOnly <code>true</code> pour ne v�rifier que
     *         les bordures, sans tenir compte de l'int�rieur de ce
     *         polygone.
     */
    private boolean intersects(final Polygon shape, final boolean checkEdgeOnly) {
        assert Thread.holdsLock(this);
        try {
            final CoordinateSystem coordinateSystem = getInternalCS();
            if (getDataBounds().intersects(shape.getCachedBounds(coordinateSystem))) {
                final Point2D.Float firstPt = new Point2D.Float();
                final  Line2D.Float segment = new  Line2D.Float();
                final Polyline.Iterator  it = new Polyline.Iterator(shape.data,
                                          shape.getMathTransform2D(
                                          shape.getTransformationFromInternalCS(coordinateSystem)));
                if (it.next(firstPt) != null) {
                    if (checkEdgeOnly || !contains(firstPt.x, firstPt.y)) {
                        segment.x2 = firstPt.x;
                        segment.y2 = firstPt.y;
                        do if (!it.next(segment)) {
                            if (interiorType==UNCLOSED || isSingular(segment)) {
                                return false;
                            }
                            segment.x2 = firstPt.x;
                            segment.y2 = firstPt.y;
                        } while (!intersects(segment));
                    }
                    return true;
                }
            }
            return false;
        } catch (TransformException exception) {
            // Conservatly return 'true' if some points from 'shape' can't be projected into
            // {@link #data}'s coordinate system.  This behavior is compliant with the Shape
            // specification.
            return true;
        }
    }

    /**
     * Returns a path iterator for this polygon.
     */
    public synchronized PathIterator getPathIterator(final AffineTransform transform) {
        return new PolygonPathIterator(this, transform);
        // Only polygons internal to Isoline have renderingResolution!=0.
        // Consequently, public polygons never apply decimation, while
        // Isoline's polgyons may apply a decimation for faster rendering
        // when painting through the 'Isoline.paint(...)' method.
    }

    /**
     * Returns a flattened path iterator for this polygon.
     */
    public PathIterator getPathIterator(final AffineTransform transform, final double flatness) {
        if (!isFlattenedShape()) {
            return getPathIterator(transform);
        } else {
            return super.getPathIterator(transform, flatness);
        }
    }

    /**
     * Returns <code>true</code> if {@link #getPathIterator} will returns a flattened iterator.
     * In this case, there is no need to wrap it into a {@link FlatteningPathIterator}.
     */
    final boolean isFlattenedShape() {
        assert flattened == checkFlattenedShape() : flattened;
        return flattened;
    }

    /**
     * Returns <code>true</code> if {@link #getPathIterator} will returns a flattened iterator.
     * In this case, there is no need to wrap it into a {@link FlatteningPathIterator}.
     */
    final boolean checkFlattenedShape() {
        return coordinateTransform==null ||
               coordinateTransform.getMathTransform()==null ||
               !Polyline.hasBorder(data);
    }

    /**
     * Returns the cache for rendering data. This cache
     * is used by the {@link PolygonPathIterator} only.
     */
    final PolygonCache getCache() {
        assert Thread.holdsLock(this);
        if (cache == null) {
            cache = new PolygonCache();
        }
        return cache;
    }

    /**
     * Sp�cifie la r�solution � appliquer au moment du tra�age du polygone.
     * Cette information est utilis�e par les objets {@link PolygonPathIterator}.
     *
     * @param  resolution R�solution � appliquer.
     */
    final void setRenderingResolution(final float resolution) {
        int newResolution = 0;
        if (resolution != 0) {
            // We could execute this code inconditionnaly, but 'setRenderingResolution(0)'
            // is sometime invoked from non-synchronized block, immediately after cloning.
            // The 'if' condition avoid the 'assert' in this methods and its dependencies,
            // as well as the execution of the (potentially heavy) 'getMeanResolution()'.
            assert Thread.holdsLock(this);
            newResolution = (int) ((resolution / getMeanResolution()) * RESOLUTION_FACTOR);
            newResolution = Math.max(0, Math.min(0xFF, newResolution));
        }
        if ((byte)newResolution != renderingResolution) {
            cache = null;
            renderingResolution = (byte)newResolution;
        }
    }

    /**
     * Returns the rendering resolution.
     */
    final float getRenderingResolution() {
        assert Thread.holdsLock(this);
        return ((int)renderingResolution & 0xFF) * getMeanResolution() / RESOLUTION_FACTOR;
    }

    /**
     * Returns an estimation of memory usage in bytes. This method is for information
     * purpose only. The memory really used by two polygons may be lower than the sum
     * of their  <code>getMemoryUsage()</code>  return values, since polylgons try to
     * share their data when possible. Furthermore, this method do not take in account
     * the extra bytes generated by Java Virtual Machine for each objects.
     *
     * @return An <em>estimation</em> of memory usage in bytes.
     */
    final synchronized long getMemoryUsage() {
        return Polyline.getMemoryUsage(data) + 50;
    }

    /**
     * Return the number of points in this polygon.
     */
    public synchronized int getPointCount() {
        return Polyline.getPointCount(data);
    }

    /**
     * Returns all polygon's points. Point coordinates are stored in {@link Point2D}
     * objects using this polygon's coordinate system ({@link #getCoordinateSystem}).
     * This method returns an immutable collection: changes done to <code>Polygon</code>
     * after calling this method will not affect the collection. Despite this method has
     * a copy semantic, the collection will share many internal structures in such a way
     * that memory consumption should stay low.
     *
     * @return The polygon's points as a collection of {@link Point2D} objects.
     */
    public synchronized Collection getPoints() {
        return new Polyline.Collection(Polyline.clone(data),
                                       getMathTransform2D(coordinateTransform));
    }

    /**
     * Returns an iterator for this polygon's internal points.
     * Points are projected in the specified coordinate system.
     *
     * @param  cs The destination coordinate system, or <code>null</code>
     *            for this polygon's native coordinate system.
     * @return An iterator for points in the specified coordinate system.
     * @throws CannotCreateTransformException if a transformation can't be constructed.
     */
    final Polyline.Iterator iterator(final CoordinateSystem cs)
            throws CannotCreateTransformException
    {
        assert Thread.holdsLock(this);
        return new Polyline.Iterator(data, getMathTransform2D(getTransformationFromInternalCS(cs)));
    }

    /**
     * Stores the value of the first point into the specified point object.
     *
     * @param  point Object in which to store the unprojected coordinate.
     * @return <code>point</code>, or a new {@link Point2D} if <code>point</code> was nul.
     * @throws NoSuchElementException If this polygons contains no point.
     *
     * @see #getFirstPoints(Point2D[])
     * @see #getLastPoint(Point2D)
     */
    public synchronized Point2D getFirstPoint(Point2D point) throws NoSuchElementException {
        point = Polyline.getFirstPoint(data, point);
        final MathTransform2D transform = getMathTransform2D(coordinateTransform);
        if (transform!=null) try {
            point = transform.transform(point, point);
        } catch (TransformException exception) {
            // Should not happen, since {@link #setCoordinateSystem}
            // has already successfully projected every points.
            unexpectedException("getFirstPoint", exception);
        }
        assert !Double.isNaN(point.getX()) && !Double.isNaN(point.getY());
        return point;
    }

    /**
     * Stores the value of the last point into the specified point object.
     *
     * @param  point Object in which to store the unprojected coordinate.
     * @return <code>point</code>, or a new {@link Point2D} if <code>point</code> was nul.
     * @throws NoSuchElementException If this polygon contains no point.
     *
     * @see #getLastPoints(Point2D[])
     * @see #getFirstPoint(Point2D)
     */
    public synchronized Point2D getLastPoint(Point2D point) throws NoSuchElementException {
        point = Polyline.getLastPoint(data, point);
        final MathTransform2D transform = getMathTransform2D(coordinateTransform);
        if (transform!=null) try {
            point = transform.transform(point, point);
        } catch (TransformException exception) {
            // Should not happen, since {@link #setCoordinateSystem}
            // has already successfully projected every points.
            unexpectedException("getLastPoint", exception);
        }
        assert !Double.isNaN(point.getX()) && !Double.isNaN(point.getY());
        return point;
    }

    /**
     * Stores the values of <code>points.length</code> first points into the specified array.
     *
     * @param points An array to fill with first polygon's points. <code>points[0]</code>
     *               will contains the first point, <code>points[1]</code> the second point,
     *               etc.
     *
     * @throws NoSuchElementException If this polygon doesn't contain enough points.
     */
    public synchronized void getFirstPoints(final Point2D[] points) throws NoSuchElementException {
        Polyline.getFirstPoints(data, points);
        final MathTransform2D transform = getMathTransform2D(coordinateTransform);
        if (transform!=null) try {
            for (int i=0; i<points.length; i++) {
                points[i] = transform.transform(points[i], points[i]);
                assert !Double.isNaN(points[i].getX()) && !Double.isNaN(points[i].getY());
            }
        } catch (TransformException exception) {
            // Should not happen, since {@link #setCoordinateSystem}
            // has already successfully projected every points.
            unexpectedException("getFirstPoints", exception);
        }
        assert points.length==0 || Utilities.equals(getFirstPoint(null), points[0]);
    }

    /**
     * Stores the values of <code>points.length</code> last points into the specified array.
     *
     * @param points An array to fill with last polygon's points.
     *               <code>points[points.length-1]</code> will contains the last point,
     *               <code>points[points.length-2]</code> the point before the last one, etc.
     *
     * @throws NoSuchElementException If this polygon doesn't contain enough points.
     */
    public synchronized void getLastPoints(final Point2D[] points) throws NoSuchElementException {
        Polyline.getLastPoints(data, points);
        final MathTransform2D transform = getMathTransform2D(coordinateTransform);
        if (transform!=null) try {
            for (int i=0; i<points.length; i++) {
                points[i] = transform.transform(points[i], points[i]);
                assert !Double.isNaN(points[i].getX()) && !Double.isNaN(points[i].getY());
            }
        } catch (TransformException exception) {
            // Should not happen, since {@link #setCoordinateSystem}
            // has already successfully projected every points.
            unexpectedException("getLastPoints", exception);
        }
        assert points.length==0 || Utilities.equals(getLastPoint(null), points[points.length-1]);
    }

    /**
     * Ajoute des points au d�but de ce polygone.  Ces points seront consid�r�s comme
     * faisant partie de la bordure de la carte, et non comme des points repr�sentant
     * une structure g�ographique.
     *
     * @param  border Coordonn�es � ajouter sous forme de paires de nombres (x,y).
     * @param  lower  Index du premier <var>x</var> � ajouter � la bordure.
     * @param  upper  Index suivant celui du dernier <var>y</var> � ajouter � la bordure.
     * @throws IllegalStateException si ce polygone a d�j� �t� ferm�.
     * @throws TransformException si <code>border</code> contient des points
     *         invalides pour le syst�me de coordonn�es natif de ce polygone.
     */
    public void prependBorder(final float[] border, final int lower, final int upper)
            throws TransformException
    {
        addBorder(border, lower, upper, false);
    }

    /**
     * Ajoute des points � la fin de ce polygone.  Ces points seront consid�r�s comme
     * faisant partie de la bordure de la carte, et non comme des points repr�sentant
     * une structure g�ographique.
     *
     * @param  border Coordonn�es � ajouter sous forme de paires de nombres (x,y).
     * @param  lower  Index du premier <var>x</var> � ajouter � la bordure.
     * @param  upper  Index suivant celui du dernier <var>y</var> � ajouter � la bordure.
     * @throws IllegalStateException si ce polygone a d�j� �t� ferm�.
     * @throws TransformException si <code>border</code> contient des points
     *         invalides pour le syst�me de coordonn�es natif de ce polygone.
     */
    public void appendBorder(final float[] border, final int lower, final int upper)
            throws TransformException
    {
        addBorder(border, lower, upper, true);
    }

    /**
     * Impl�mentation de <code>prependBorder(...)</code> et <code>prependBorder(...)</code>.
     *
     * @param append <code>true</code> pour effectuer l'op�ration <code>appendBorder</code>, ou
     *               <code>false</code> pour effectuer l'op�ration <code>prependBorder</code>.
     */
    private synchronized void addBorder(float[] border, int lower, int upper, final boolean append)
            throws TransformException
    {
        if (interiorType != UNCLOSED) {
            throw new IllegalStateException(Resources.format(ResourceKeys.ERROR_POLYGON_CLOSED));
        }
        final MathTransform2D transform = getMathTransform2D(coordinateTransform);
        if (transform != null) {
            final float[] oldBorder = border;
            border = new float[upper-lower];
            transform.inverse().transform(oldBorder, lower, border, 0, border.length);
            lower = 0;
            upper = border.length;
        }
        if (append) {
            data = Polyline.appendBorder(data, border, lower, upper);
        } else {
            data = Polyline.prependBorder(data, border, lower, upper);
        }
        flattened  = checkFlattenedShape();
        dataBounds = null;
        bounds     = null;
        cache      = null;
        // No change to resolution, since its doesn't take border in account.
    }

    /**
     * Ajoute � la fin de ce polygone les donn�es du polygone sp�cifi�.
     * Cette m�thode ne fait rien si <code>toAppend</code> est nul.
     *
     * @param  toAppend Polygone � ajouter � la fin de <code>this</code>.
     *         Le polygone <code>toAppend</code> ne sera pas modifi�.
     * @throws IllegalStateException    si ce polygone a d�j� �t� ferm�.
     * @throws IllegalArgumentException si le polygone <code>toAppend</code> a d�j� �t� ferm�.
     * @throws TransformException si <code>toAppend</code> contient des points
     *         invalides pour le syst�me de coordonn�es natif de ce polygone.
     */
    public synchronized void append(final Polygon toAppend) throws TransformException {
        if (toAppend == null) {
            return;
        }
        if (!Utilities.equals(getInternalCS(), toAppend.getInternalCS())) {
            throw new UnsupportedOperationException(); // TODO.
        }
        if (interiorType != UNCLOSED || toAppend.interiorType != UNCLOSED) {
            throw new IllegalStateException(Resources.format(ResourceKeys.ERROR_POLYGON_CLOSED));
        }
        data = Polyline.append(data, Polyline.clone(toAppend.data));
        if (dataBounds != null) {
            if (toAppend.dataBounds != null) {
                dataBounds.add(toAppend.dataBounds);
                assert equalsEps(dataBounds, getDataBounds()) : dataBounds;
            } else {
                dataBounds = null;
            }
        }
        bounds = null;
        cache  = null;
        if (resolution > 0) {
            if (toAppend.resolution > 0) {
                final int thisCount =          getPointCount();
                final int thatCount = toAppend.getPointCount();
                resolution = (resolution*thisCount + toAppend.resolution*thatCount) / (thisCount + thatCount);
                assert resolution > 0;
                return;
            }
        }
        resolution = Float.NaN;
        flattened = checkFlattenedShape();
    }

    /**
     * Reverse point order in this polygon.
     */
    public synchronized void reverse() {
        data = Polyline.reverse(data);
        flattened = checkFlattenedShape();
        cache = null;
    }

    /**
     * Close and freeze this polygon. After closing it,
     * no more points can be added to this polygon.
     *
     * @param type Tells if this polygon is an elevation (e.g. an island in the middle
     *        of the sea) or a depression (e.g. a lake in the middle of a continent).
     *        If this argument doesn't apply, then it can be <code>null</code>.
     *
     * @see #getInteriorType
     */
    public synchronized void close(final InteriorType type) {
        data = Polyline.freeze(data, type!=null, false);
        interiorType = (byte) InteriorType.getValue(type);
        flattened = checkFlattenedShape();
        cache = null;
    }

    /**
     * Returns wether this polygon is closed or not.
     */
    final boolean isClosed() {
        return interiorType != UNCLOSED;
    }

    /**
     * Tells if this polygon's interior is an {@linkplain InteriorType#ELEVATION elevation}
     * or a {@linkplain InteriorType#DEPRESSION depression}.
     *
     * @return This polygon's interior type, or <code>null</code> is it doesn't apply.
     *
     * @see #close
     */
    public InteriorType getInteriorType() {
        return InteriorType.getEnum(interiorType);
    }
    
    /**
     * Return a polygons with the point of this polygons from <code>lower</code>
     * inclusive to <code>upper</code> exclusive. The returned polygon may not be
     * closed, i.e. {@link #getInteriorType} may returns <code>null</code>.
     * If no data are available in the specified range, this method returns
     * <code>null</code>.
     */
    public synchronized Polygon subpoly(final int lower, final int upper) {
        final Polyline sub = Polyline.subpoly(data, lower, upper);
        if (sub == null) {
            return null;
        }
        if (Polyline.equals(sub, data)) {
            return this;
        }
        final Polygon subPoly = new Polygon(coordinateTransform);
        subPoly.data = sub;
        subPoly.flattened = subPoly.checkFlattenedShape();
        assert subPoly.getPointCount() == (upper-lower);
        return subPoly;
    }

    /**
     * Return a polygons with the point of this polygons from <code>lower</code>
     * inclusive to the end. The returned polygon may not be closed, i.e. {@link
     * #getInteriorType} may returns <code>null</code>. If no data are available
     * in the specified range, this method returns <code>null</code>.
     */
    final synchronized Polygon subpoly(final int lower) {
        return subpoly(lower, getPointCount());
    }

    /**
     * Returns the mean resolution. This method cache the result for faster processing.
     *
     * @return The mean resolution, or {@link Float#NaN} if this polygon doesn't have any point.
     */
    final float getMeanResolution() {
        assert Thread.holdsLock(this);
        if (!(resolution > 0)) { // '!' take NaN in account
            final Statistics stats = getResolution();
            resolution = (stats!=null) ? (float)stats.mean() : Float.NaN;
        }
        assert Float.isNaN(resolution) ? (getResolution()==null || getPointCount()==0) :
               Math.abs(getResolution().mean()-resolution) <= EPS*resolution : resolution;
        return resolution;
    }

    /**
     * Returns the polygon's resolution.  The mean resolution is the mean distance between
     * every pair of consecutive points in this polygon  (ignoring "extra" points used for
     * drawing a border, if there is one). This method try to express the resolution in
     * linear units (usually meters) no matter if the coordinate systems is actually a
     * {@link ProjectedCoordinateSystem} or a {@link GeographicCoordinateSystem}.
     * More specifically:
     * <ul>
     *   <li>If the coordinate system is a {@linkplain GeographicCoordinateSystem geographic}
     *       one, then the resolution is expressed in units of the underlying
     *       {@linkplain Ellipsoid#getAxisUnit ellipsoid's axis length}.</li>
     *   <li>Otherwise (especially if the coordinate system is a {@linkplain
     *       ProjectedCoordinateSystem projected} one), the resolution is expressed in
     *       {@linkplain ProjectedCoordinateSystem#getUnits units of the coordinate system}.</li>
     * </ul>
     */
    public synchronized Statistics getResolution() {
        try {
            final Statistics stats = Polyline.getResolution(data, coordinateTransform);
            assert !(stats!=null && Math.abs(resolution-stats.mean())>EPS*resolution) : resolution;
            resolution = (stats!=null) ? (float)stats.mean() : Float.NaN;
            return stats;
        } catch (TransformException exception) {
            // Should not happen, since {@link #setCoordinateSystem}
            // has already successfully projected every points.
            unexpectedException("getResolution", exception);
            return null;
        }
    }

    /**
     * Set the polygon's resolution. This method try to interpolate new points in such a way
     * that every point is spaced by exactly <code>resolution</code> units (usually meters)
     * from the previous one.
     *
     * @param  resolution Desired resolution, in the same units than {@link #getResolution}.
     * @throws TransformException If some coordinate transformations were needed and failed.
     *         There is no guaranteed on contour's state in case of failure.
     */
    public synchronized void setResolution(final double resolution) throws TransformException {
        CoordinateSystem targetCS = getCoordinateSystem();
        if (CTSUtilities.getHeadGeoEllipsoid(targetCS) != null) {
            /*
             * The 'Polyline.setResolution(...)' algorithm require a cartesian coordinate system.
             * If this polygon's coordinate system is not cartesian, check if the underlying data
             * used a cartesian CS  (this polygon may be a "view" of the data under an other CS).
             * If the underlying data are not cartesian neither, create a temporary sterographic
             * projection for computation purpose.
             */
            targetCS = getInternalCS();
            if (targetCS instanceof GeographicCoordinateSystem) {
                final GeographicCoordinateSystem geoCS = (GeographicCoordinateSystem) targetCS;
                final Ellipsoid ellipsoid = geoCS.getHorizontalDatum().getEllipsoid();
                final String         name = "Temporary cartesian";
                final Rectangle2D  bounds = getCachedBounds();
                final Point2D      center = new Point2D.Double(bounds.getCenterX(),
                                                               bounds.getCenterY());
                final Projection projection = new Projection(name, CARTESIAN_PROJECTION,
                                                             ellipsoid, center, null);
                targetCS = new ProjectedCoordinateSystem(name, geoCS, projection);
            }
        }
        Polyline.setResolution(data, getTransformationFromInternalCS(targetCS), resolution);
        clearCache(); // Clear everything in the cache.
    }

    /**
     * Compress this polygon. Compression is destructive, i.e. it may lost data. This method
     * process in two steps:
     *
     * First, it invokes <code>{@link #setResolution setResolution}(dx&nbsp;+&nbsp;factor*std)</code>
     * where <var>dx</var> is the {@linkplain #getResolution mean resolution} of this
     * polygon and <var>std</var> is the resolution's standard deviation.
     *
     * Second, it replace absolute positions (left handed image) by relative positions
     * (right handed image), i.e. distances relative to the previous point.  Since all
     * distances are of similar magnitude, distances can be coded in <code>byte</code>
     * primitive type instead of <code>float</code>.
     *
     * <table cellspacing='12'><tr>
     * <td><p align="center"><img src="doc-files/uncompressed.png"></p></td>
     * <td><p align="center"><img src="doc-files/compressed.png"></p></td>
     * </tr></table>
     *
     * @param  factor Facteur contr�lant la baisse de r�solution.  Les valeurs �lev�es
     *         d�ciment davantage de points, ce qui r�duit d'autant la consommation de
     *         m�moire. Ce facteur est g�n�ralement positif, mais il peut aussi �tre 0
     *         o� m�me l�g�rement n�gatif.
     * @return A <em>estimation</em> of the compression rate. For example a value of 0.2
     *         means that the new polygon use <em>approximatively</em> 20% less memory.
     *         Warning: this value may be inacurate, for example if the old polygon was
     *         used to shares its data with an other polygon, compressing one polygon
     *         may actually increase memory usage since the two polygons will no longer
     *         share their data.
     * @throws TransformException Si une erreur est survenue lors d'une projection cartographique.
     */
    public synchronized float compress(final float factor) throws TransformException {
        final Statistics stats = Polyline.getResolution(data, coordinateTransform);
        if (stats != null) {
            final long  memoryUsage = getMemoryUsage();
            final double resolution = stats.mean() + factor*stats.standardDeviation(false);
            if (resolution > 0) {
                setResolution(resolution);
                data = Polyline.freeze(data, false, true); // Apply the compression algorithm
                return (float) (memoryUsage - getMemoryUsage()) / (float) memoryUsage;
            }
            data = Polyline.freeze(data, false, false); // No compression
        }
        return 0;
    }

    /**
     * Returns a polygon approximatively equals to this polygon clipped to the specified bounds.
     * The clip is only approximative  in that the resulting polygon may extends outside the clip
     * area. However, it is garanted that the resulting polygon contains at least all the interior
     * of the clip area.
     *
     * If this method can't performs the clip, or if it believe that it doesn't worth to do a clip,
     * it returns <code>this</code>. If this polygon doesn't intersect the clip area, then this
     * method returns <code>null</code>. Otherwise, a new polygon is created and returned. The new
     * polygon will try to share as much internal data as possible with <code>this</code> in order
     * to keep memory footprint low.
     *
     * @param  clipper An object containing the clip area.
     * @return <code>null</code> if this polygon doesn't intersect the clip, <code>this</code>
     *         if no clip has been performed, or a new clipped polygon otherwise.
     */
    final synchronized Polygon clip(final Clipper clipper) {
        final Rectangle2D clip = clipper.getInternalClip(this);
        final Rectangle2D dataBounds = getDataBounds();
        if (clip.contains(dataBounds)) {
            return this;
        }
        if (!clip.intersects(dataBounds)) {
            return null;
        }
        /*
         * Selon toutes apparences, le polygone n'est ni compl�tement � l'int�rieur
         * ni compl�tement en dehors de <code>clip</code>. Il faudra donc se resoudre
         * � faire une v�rification plus pouss�e (et plus couteuse).
         */
        final Polygon clipped = clipper.clip(this);
        if (clipped != null) {
            if (Polyline.equals(data, clipped.data)) {
                return this;
            }
        }
        return clipped;
    }

    /**
     * Returns the string to be used as the tooltip for the given location.
     * If there is no such tooltip, returns <code>null</code>.
     *
     * @param  point Coordinates (usually mouse coordinates). Must be
     *         specified in this polygon's coordinate system
     *         (as returned by {@link #getCoordinateSystem}).
     * @param  locale The desired locale for the tool tips.
     * @return The tooltip text for the given location,
     *         or <code>null</code> if there is none.
     */
    public String getToolTipText(final Point2D point, final Locale locale) {
        if (interiorType == UNCLOSED) {
            return null;
        }
        return super.getToolTipText(point, locale);
    }

    /**
     * Return a copy of all coordinates of this polygon. Coordinates are usually
     * (<var>x</var>,<var>y</var>) or (<var>longitude</var>,<var>latitude</var>)
     * pairs, depending of the {@linkplain #getCoordinateSystem coordinate system
     * in use}.
     *
     * @param  The destination array. The coordinates will be filled in {@link ArrayData#array}
     *         from index {@link ArrayData#length}. The array will be expanded if needed, and
     *         {@link ArrayData#length} will be updated with index after the <code>array</code>'s
     *         element filled with the last <var>y</var> ordinates.
     * @param  resolution The minimum distance desired between points.
     */
    final void toArray(final ArrayData dest, float resolution) {
        assert Thread.holdsLock(this);
        try {
            /*
             * If the polygon's coordinate system is geographic, then we must translate
             * the resolution (which is in linear unit, usually meters) to angular units.
             * The formula used below is only an approximation (probably not the best one).
             * It estimate the average of latitudinal and longitudinal angles corresponding
             * to the distance 'resolution' in the middle of the polygon's bounds. The average
             * is weighted according the width/height ratio of the polygon's bounds.
             */
            final CoordinateSystem cs = getCoordinateSystem();
            final Ellipsoid ellipsoid = CTSUtilities.getHeadGeoEllipsoid(cs);
            if (ellipsoid != null) {
                final Unit          unit = cs.getUnits(1);
                final Rectangle2D bounds = getCachedBounds();
                double             width = bounds.getWidth();
                double            height = bounds.getHeight();
                double          latitude = bounds.getCenterY();
                latitude = Unit.RADIAN.convert(latitude, unit);
                final double sin = Math.sin(latitude);
                final double cos = Math.cos(latitude);
                final double normalize = width+height;
                width  /= normalize;
                height /= normalize;
                resolution *= (height + width/cos) * XMath.hypot(sin/ellipsoid.getSemiMajorAxis(),
                                                                 cos/ellipsoid.getSemiMinorAxis());
                // Assume that longitude has the same unit than latitude.
                resolution = (float) unit.convert(resolution, Unit.RADIAN);
            }
            /*
             * Transform the resolution from this polygon's CS to the underlying data CS.
             * TODO: we should use 'MathTransform.derivative' instead, but it is not yet
             *       implemented for most transforms.
             */
            if (coordinateTransform != null) {
                final MathTransform tr = coordinateTransform.getMathTransform();
                if (!tr.isIdentity()) {
                    final Rectangle2D bounds = getCachedBounds();
                    final double  centerX = bounds.getCenterX();
                    final double  centerY = bounds.getCenterY();
                    final double[] coords = new double[] {
                        centerX-resolution, centerY,
                        centerX+resolution, centerY,
                        centerX,            centerY-resolution,
                        centerX,            centerY+resolution
                    };
                    tr.inverse().transform(coords, 0, coords, 0, coords.length/2);
                    resolution = (float) (0.25*(
                                          XMath.hypot(coords[2]-coords[0], coords[3]-coords[1]) +
                                          XMath.hypot(coords[6]-coords[4], coords[7]-coords[5])));
                }
            }
            /*
             * Gets the array and transform it, if needed.
             */
            Polyline.toArray(data, dest, resolution, getMathTransform2D(coordinateTransform));
        } catch (TransformException exception) {
            // Should not happen, since {@link #setCoordinateSystem}
            // has already successfully projected every points.
            unexpectedException("toArray", exception);
        }
    }

    /**
     * Return a copy of all coordinates of this polygon. Coordinates are usually
     * (<var>x</var>,<var>y</var>) or (<var>longitude</var>,<var>latitude</var>)
     * pairs, depending of the {@linkplain #getCoordinateSystem coordinate system
     * in use}. This method never return <code>null</code>, but may return an array
     * of length 0 if no data are available.
     *
     * @param  resolution The minimum distance desired between points, in the same units
     *         than for the {@link #getResolution} method  (i.e. linear units as much as
     *         possible - usually meters - even for geographic coordinate system).
     *         If <code>resolution</code> is greater than 0, then points that are closer
     *         than <code>resolution</code> from previous points will be skiped. This method
     *         is not required to perform precise distances computation.
     * @return The coordinates expressed in this
     *         {@linkplain #getCoordinateSystem polygon's coordinate system}.
     */
    public synchronized float[] toArray(final float resolution) {
        final ArrayData array = new ArrayData(64);
        toArray(array, resolution);
        return XArray.resize(array.array(), array.length());
    }

    /**
     * Returns a hash value for this polygon.
     */
    public synchronized int hashCode() {
        return Polyline.hashCode(data);
    }

    /**
     * Compare the specified object with this polygon for equality.
     */
    public synchronized boolean equals(final Object object) {
        if (object == this) {
            // Slight optimization
            return true;
        }
        if (super.equals(object)) {
            final Polygon that = (Polygon) object;
            return                  this.interiorType    ==   that.interiorType         &&
                   Utilities.equals(this.coordinateTransform, that.coordinateTransform) &&
                    Polyline.equals(this.data,                that.data);
        }
        return false;
    }

    /**
     * Return a clone of this polygon. The clone has a deep copy semantic,
     * i.e. any change to the current polygon (including adding new points)
     * will not affect the clone,  and vis-versa   (any change to the clone
     * will not affect the current polygon). However, the two polygons will
     * share many internal structures in such a way that memory consumption
     * for polygon's clones should be kept low.
     */
    public synchronized Object clone() {
        final Polygon polygon = (Polygon) super.clone();
        polygon.data = Polyline.clone(data); // Take an immutable view of 'data'.
        return polygon;
    }

    /**
     * Efface toutes les informations qui �taient conserv�es dans une cache interne.
     * Cette m�thode peut �tre appel�e lorsque l'on sait que ce polygone ne sera plus
     * utilis� avant un certain temps. Elle ne cause la perte d'aucune information,
     * mais rendra la prochaine utilisation de ce polygone plus lente (le temps que
     * les caches internes soient reconstruites,  apr�s quoi le polygone retrouvera
     * sa vitesse normale).
     */
    final synchronized void clearCache() {
        cache      = null;
        bounds     = null;
        dataBounds = null;
        resolution = Float.NaN;
        flattened  = checkFlattenedShape();
    }

    /**
     * Invoked during deserialization.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        flattened = checkFlattenedShape(); // Reasonably fast to compute.
    }

    /**
     * M�thode appel�e lorsqu'une erreur inatendue est survenue.
     *
     * @param  method Nom de la m�thode dans laquelle est survenu l'exception.
     * @param  exception L'exception survenue.
     * @throws IllegalPathStateException syst�matiquement relanc�e.
     */
    static void unexpectedException(final String method, final TransformException exception) {
        Polyline.unexpectedException("Polygon", method, exception);
        final IllegalPathStateException e = new IllegalPathStateException(
                                                exception.getLocalizedMessage());
        e.initCause(exception);
        throw e;
    }

    /**
     * Write all point coordinates to the specified stream.
     * This method is usefull for debugging purpose.
     *
     * @param  out The destination stream, or <code>null</code> for the standard output.
     * @param  locale Desired locale, or <code>null</code> for a default one.
     * @throws IOException If an error occured while writting to the destination stream.
     */
    public void print(final Writer out, final Locale locale) throws IOException {
        print(new String[]{getName(locale)}, new Collection[]{getPoints()}, out, locale);
    }

    /**
     * Write side-by-side all point coordinates of many polygons.
     * This method is usefull for checking the result of a coordinate
     * transformation; one could write side-by-side the original and
     * transformed polygons. Note that this method may require unicode
     * support for proper output.
     *
     * @param  polygons The set of polygons. polygons may have different length.
     * @param  out The destination stream, or <code>null</code> for the standard output.
     * @param  locale Desired locale, or <code>null</code> for a default one.
     * @throws IOException If an error occured while writting to the destination stream.
     */
    public static void print(final Polygon[] polygons, final Writer out, final Locale locale)
            throws IOException
    {
        final String[]     titles = new String[polygons.length];
        final Collection[] arrays = new Collection[polygons.length];
        for (int i=0; i<polygons.length; i++) {
            final Polygon polygon = polygons[i];
            titles[i] = polygon.getName(locale);
            arrays[i] = polygon.getPoints();
        }
        print(titles, arrays, out, locale);
    }

    /**
     * Write side-by-side all points from arbitrary collections.
     * Note that this method may require unicode support for proper output.
     *
     * @param  titles The column's titles. Should have the same length than <code>points</code>.
     * @param  points Array of points collections. Collections may have different size.
     * @param  out The destination stream, or <code>null</code> for the standard output.
     * @param  locale Desired locale, or <code>null</code> for a default one.
     * @throws IOException If an error occured while writting to the destination stream.
     */
    public static void print(final String[] titles, final Collection[] points, Writer out, Locale locale)
            throws IOException
    {
        if (locale == null) locale = Locale.getDefault();
        if (out    == null)    out = Arguments.getWriter(System.out);

        final int            width = 8; // Columns width.
        final int        precision = 3; // Significant digits.
        final String     separator = "  \u2502  "; // Vertical bar.
        final String lineSeparator = System.getProperty("line.separator", "\n");
        final NumberFormat  format = NumberFormat.getNumberInstance(locale);
        final FieldPosition  dummy = new FieldPosition(0);
        final StringBuffer  buffer = new StringBuffer();
        format.setMinimumFractionDigits(precision);
        format.setMaximumFractionDigits(precision);
        format.setGroupingUsed(false);

        final Iterator[] iterators = new Iterator[points.length];
        for (int i=0; i<points.length; i++) {
            if (i != 0) {
                out.write(separator);
            }
            int length=0;
            if (titles[i] != null) {
                length=titles[i].length();
                final int spaces = Math.max(width-length/2, 0);
                out.write(Utilities.spaces(spaces));
                out.write(titles[i]);
                length += spaces;
            }
            out.write(Utilities.spaces(1+2*width-length));
            iterators[i]=points[i].iterator();
        }
        out.write(lineSeparator);
        boolean hasNext; do {
            hasNext=false;
            buffer.setLength(0);
            for (int i=0; i<iterators.length; i++) {
                if (i!=0) buffer.append(separator);
                final Iterator   it = iterators[i];
                final boolean hasPt = it.hasNext();
                final Point2D point = (hasPt) ? (Point2D) it.next() : null;
                boolean xy=true; do {
                    final int start = buffer.length();
                    if (point != null) {
                        format.format(xy ? point.getX() : point.getY(), buffer, dummy);
                    }
                    buffer.insert(start, Utilities.spaces(width-(buffer.length()-start)));
                    if (xy) {
                        buffer.append('\u00A0'); // No-break space
                    }
                } while (!(xy = !xy));
                hasNext |= hasPt;
            }
            if (!hasNext) {
                break;
            }
            buffer.append(lineSeparator);
            out.write(buffer.toString());
        } while (hasNext);
    }




    /**
     * This interface defines the method required by any object that
     * would like to be a renderer for polygons in an {@link Isoline}.
     * The {@link #paint} method is invoked by {@link Isoline#paint}.
     *
     * @version $Id: Polygon.java,v 1.5 2003/02/07 23:04:51 desruisseaux Exp $
     * @author Martin Desruisseaux
     *
     * @see Polygon
     * @see Isoline#paint
     * @see org.geotools.renderer.j2d.RenderedIsoline
     */
    public static interface Renderer {
        /**
         * Returns the clip area in units of polygon and isoline's coordinate system (both use
         * the same). This is usually "real world" metres or degrees of latitude/longitude.
         *
         * @see Polygon#getCoordinateSystem
         * @see Isoline#getCoordinateSystem
         */
        public abstract Shape getClip();

        /**
         * Returns the rendering resolution, in units of polygon and isoline's coordinate system.
         * (usually metres or degrees). A larger resolution speed up rendering, while a smaller
         * resolution draw more precise map.
         *
         * @param  current The current rendering resolution.
         * @return the <code>current</code> rendering resolution if it still good enough,
         *         or a new resolution if a change is needed.
         *
         * @see Polygon#getCoordinateSystem
         * @see Isoline#getCoordinateSystem
         */
        public abstract float getRenderingResolution(float current);

        /**
         * Draw or fill a polygon. {@link Isoline#paint} invokes this method with a decimated and/or
         * clipped polygon in argument. This polygon expose some internal state of {@link Isoline}.
         * <strong>Do not modify it, neither keep a reference to it after this method call</strong>
         * in order to avoid unexpected behaviour.
         *
         * @param polygon The polygon to draw. <strong>Do not modify.</strong>
         */
        public abstract void paint(final Polygon polygon);

        /**
         * Invoked once after a series of polygons has been painted. This method is typically
         * invoked by {@link Isoline#paint} after all isoline's polygons has been painted.
         * Some implementation may choose to release resources here. The arguments provided
         * to this method are for information purpose only.
         *
         * @param rendered The total number of <em>rendered</em> points. This number is
         *        always smaller than {@link Isoline#getPointCount}  since the renderer
         *        may have clipped or decimated data. This is the number of points keep
         *        in the cache.
         * @param recomputed The number of points that has been recomputed (i.e. decompressed,
         *        decimated, projected and transformed). They are points that was not reused
         *        from the cache. This number is always smaller than or equals to
         *        <code>rendered</code>.
         * @param resolution The mean resolution of rendered polygons.
         */
        public abstract void paintCompleted(int rendered, int recomputed, double resolution);
    }
}
