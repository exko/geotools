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
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

// Graphics
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import javax.swing.Action;

// Geotools dependencies
import org.geotools.cs.CoordinateSystem;
import org.geotools.ct.MathTransform2D;
import org.geotools.ct.TransformException;
import org.geotools.units.Unit;
import org.geotools.resources.XMath;
import org.geotools.resources.XArray;
import org.geotools.resources.Utilities;
import org.geotools.resources.CTSUtilities;
import org.geotools.resources.XAffineTransform;


/**
 * A set of marks and/or labels to be rendered. Marks can have different sizes and orientations
 * (for example a field of wind arrows). This abstract class is not a container for marks.
 * Subclasses must override the {@link #getMarkIterator} method in order to returns informations
 * about marks.
 *
 * @version $Id: RenderedMarks.java,v 1.8 2003/03/15 12:58:15 desruisseaux Exp $
 * @author Martin Desruisseaux
 */
public abstract class RenderedMarks extends RenderedLayer {
    /**
     * Default color for marks.
     */
    static final Color DEFAULT_COLOR = new Color(102, 102, 153, 192);

    /**
     * Projection cartographique utilis�e la
     * derni�re fois pour obtenir les donn�es.
     */
    private transient MathTransform2D lastProjection;

    /**
     * Transformation affine utilis�e la derni�re fois.
     * Cette information est utilis�e pour savoir si on
     * peut r�utiliser {@link #transformedShapes}.
     */
    private transient AffineTransform lastTransform;

    /**
     * Formes g�om�triques transform�es utilis�es la derni�re fois.
     * Ces formes seront r�utilis�es autant que possible plut�t que
     * d'�tre constamment recalcul�es.
     */
    private transient Shape[] transformedShapes;

    /**
     * Bo�te englobant toutes les coordonn�es des formes apparaissant
     * dans {@link #transformedShapes}. Les coordonn�es de cette bo�te
     * seront en pixels.
     */
    private transient Rectangle shapeBoundingBox;

    /**
     * Typical amplitude of marks, or 0 or {@link Double#NaN} if it need to be recomputed.
     * This value is computed by {@link #getTypicalAmplitude} and cached here for faster
     * access. The default implementation computes the Root Mean Square (RMS) value of all
     * {@linkplain MarkIterator#amplitude marks amplitude}.
     *
     * Note: this field is read and write by {@link RenderedGridMarks}, which overrides
     * {@link #getTypicalAmplitude}.
     */
    transient double typicalAmplitude;

    /**
     * Construct a new layer of marks.
     */
    public RenderedMarks() {
        super();
    }

    /**
     * Returns the number of marks. <strong>Note: this method is a temporary hack and will
     * be removed in a future version.</strong>
     *
     * @task TODO: Make this method package-privated and rename it "guessCount".
     *             The actual count will be fetched from the MarkIterator.
     */
    protected abstract int getCount();

    /**
     * Returns an iterator for iterating through the marks.
     * This iterator doesn't need to be thread-safe.
     */
    public abstract MarkIterator getMarkIterator();

    /**
     * Returns the units for {@linkplain MarkIterator#amplitude marks amplitude}, or
     * <code>null</code> if unknow. All marks must use the same units. The default
     * implementation returns always <code>null</code>.
     */
    public Unit getAmplitudeUnit() {
        return null;
    }

    /**
     * Returns the typical amplitude of marks. The default implementation computes the <cite>Root
     * Mean Square</cite> (RMS) value of all {@linkplain MarkIterator#amplitude marks amplitude}.
     *
     * This information is used with mark's {@linkplain MarkIterator#amplitude amplitude} and
     * {@linkplain MarkIterator#markShape shape} in order to determine how big they should be
     * rendered. Marks with an {@linkplain MarkIterator#amplitude amplitude} equals to the
     * typical amplitude will be rendered with their {@linkplain MarkIterator#markShape shape}
     * unscaled. Other marks will be rendered with scaled versions of their shapes.
     */
    public double getTypicalAmplitude() {
        synchronized (getTreeLock()) {
            if (!(typicalAmplitude>0)) {
                int n=0;
                double rms=0;
                for (final MarkIterator it=getMarkIterator(); it.next();) {
                    final double v = it.amplitude();
                    if (!Double.isNaN(v)) {
                        rms += v*v;
                        n++;
                    }
                }
                typicalAmplitude = (n>0) ? Math.sqrt(rms/n) : 1;
            }
            return typicalAmplitude;
        }
    }

    /**
     * Dessine la forme g�om�trique sp�cifi�e. Cette m�thode est appell�e automatiquement par la
     * m�thode {@link #paint(RenderingContext)}. Les classes d�riv�es peuvent la red�finir si
     * elles veulent modifier la fa�on dont les marques sont dessin�es. Cette m�thode re�oit
     * en argument une forme g�om�trique <code>shape</code> � dessiner dans <code>graphics</code>.
     * Les rotations, translations et facteurs d'�chelles n�cessaires pour bien repr�senter la
     * marque auront d�j� �t� pris en compte. Le graphique <code>graphics</code> a d�ja re�u la
     * transformation affine appropri�e. L'impl�mentation par d�faut ne fait qu'utiliser le
     * pseudo-code suivant:
     *
     * <blockquote><pre>
     * graphics.setColor(<var>defaultColor</var>);
     * graphics.fill(shape);
     * </pre></blockquote>
     *
     * @param graphics Graphique � utiliser pour tracer la marque. L'espace de coordonn�es
     *                 de ce graphique sera les pixels ou les points (1/72 de pouce).
     * @param shape    Forme g�om�trique repr�sentant la marque � tracer.
     * @param iterator The iterator used for computing <code>shape</code>. This method can
     *                 query properties like the {@linkplain MarkIterator#position position},
     *                 the {@linkplain MarkIterator#amplitude amplitude}, etc. However, it
     *                 should <strong>not</strong> moves the iterator (i.e. do not invoke
     *                 any {@link MarkIterator#next} method).
     */
    protected void paint(final Graphics2D graphics, final Shape shape, final MarkIterator iterator)
    {
        graphics.setColor(DEFAULT_COLOR);
        graphics.fill(shape);
    }

    /**
     * Retourne les indices qui correspondent aux coordonn�es sp�cifi�es.
     * Ces indices seront utilis�es par {@link MarkIterator#visible(Rectangle)}
     * pour v�rifier si un point est dans la partie visible. Cette m�thode
     * sera red�finie par {@link RenderedGridMarks}.
     *
     * @param visibleArea Coordonn�es logiques de la r�gion visible � l'�cran.
     */
    Rectangle getUserClip(final Rectangle2D visibleArea) {
        return null;
    }

    /**
     * Fait en sorte que {@link #transformedShapes} soit non-nul et ait
     * exactement la longueur n�cessaire pour contenir toutes les formes
     * g�om�triques des marques. Si un nouveau tableau a d� �tre cr��,
     * cette m�thode retourne <code>true</code>. Si l'ancien tableau n'a
     * pas �t� modifi� parce qu'il convenait d�j�, alors cette m�thode
     * retourne <code>false</code>.
     */
    private boolean validateShapesArray(final int shapesCount) {
        if (transformedShapes==null || transformedShapes.length!=shapesCount) {
            transformedShapes = new Shape[shapesCount];
            return true;
        }
        return false;
    }

    /**
     * Proc�de au tra�age des marques de cette couche. Les classes d�riv�es ne
     * devraient pas avoir besoin de red�finir cette m�thode. Pour modifier la
     * fa�on de dessiner les marques, red�finissez plut�t une des m�thodes
     * �num�r�es dans la section "voir aussi" ci-dessous.
     *
     * @throws TransformException if a coordinate transformation was required and failed.
     *
     * @see MarkIterator#visible
     * @see MarkIterator#position
     * @see MarkIterator#geographicArea
     * @see MarkIterator#markShape
     * @see MarkIterator#direction
     * @see MarkIterator#amplitude
     * @see #getTypicalAmplitude
     * @see #getAmplitudeUnit
     * @see #paint(Graphics2D, Shape, MarkIterator)
     */
    protected void paint(final RenderingContext context) throws TransformException {
        assert Thread.holdsLock(getTreeLock());
        final Graphics2D        graphics = context.getGraphics();
        final AffineTransform fromWorld  = context.getAffineTransform(context.mapCS, context.textCS);
        final AffineTransform fromPoints = context.getAffineTransform(context.textCS, context.deviceCS);
        final Rectangle   zoomableBounds = context.getPaintingArea(context.textCS).getBounds();
        final int                  count = getCount();
        if (count != 0) {
            final MarkIterator iterator = getMarkIterator();
            /*
             * V�rifie si la transformation affine est la m�me que la derni�re fois. Si ce n'est
             * pas le cas, alors on va recr�er une liste de toutes les formes g�om�triques
             * transform�es. Cette liste servira � la fois � tracer les fl�ches et, plus tard,
             * � d�terminer si le curseur de la souris tra�ne sur l'une d'entre elles. Certains
             * �l�ments peuvent �tre nuls s'ils n'apparaissent pas dans la zone de tra�age.
             */
            final MathTransform2D projection = (MathTransform2D)
                    context.getMathTransform(getCoordinateSystem(), context.mapCS);
            if (validateShapesArray(count) || !Utilities.equals(projection, lastProjection) ||
                                              !Utilities.equals(fromWorld,  lastTransform))
            {
                shapeBoundingBox = null;
                lastProjection   = projection;
                lastTransform    = fromWorld;
                Rectangle userClip;
                try {
                    Rectangle2D visibleArea;
                    visibleArea = XAffineTransform.inverseTransform(fromWorld, zoomableBounds, null);
                    visibleArea = CTSUtilities.transform((MathTransform2D)projection.inverse(),
                                                          visibleArea, visibleArea);
                    userClip = getUserClip(visibleArea);
                } catch (NoninvertibleTransformException exception) {
                    userClip = null;
                } catch (TransformException exception) {
                    userClip = null;
                }
                /*
                 * On veut utiliser une transformation affine identit� (donc en utilisant
                 * une �chelle bas�e sur les pixels plut�t que les coordonn�es utilisateur),
                 * mais en utilisant la m�me rotation que celle qui a cours dans la matrice
                 * <code>fromWorld</code>. On peut y arriver en utilisant l'identit� ci-dessous:
                 *
                 *    [ m00  m01 ]     m00� + m01�  == constante sous rotation
                 *    [ m10  m11 ]     m10� + m11�  == constante sous rotation
                 */
                double scale;
                final double[] matrix = new double[6];
                fromWorld.getMatrix(matrix);
                scale = XMath.hypot(matrix[0], matrix[2]);
                matrix[0] /= scale;
                matrix[2] /= scale;
                scale = XMath.hypot(matrix[1], matrix[3]);
                matrix[1] /= scale;
                matrix[3] /= scale;
                /*
                 * Initialise quelques variables qui
                 * serviront dans le reste de ce bloc...
                 */
                final double typicalScale = getTypicalAmplitude();
                final AffineTransform tr  = new AffineTransform();
                double[] array            = new double[32];
                double[] buffer           = new double[32];
                int   [] X                = new int   [16];
                int   [] Y                = new int   [16];
                int      pointIndex       = 0;
                int      shapeIndex       = 0;
                Shape    lastShape        = null;
                boolean  shapeIsPolygon   = false;
                /*
                 * Balaie les donn�es de chaques marques. Pour chacune d'elles,
                 * on d�finira une transformation affine qui prendra en compte
                 * les translations et rotations de la marque. Cette transformation
                 * servira � transformer les coordonn�es de la marque "mod�le" en
                 * coordonn�es pixels propres � chaque marque.
                 */
                while (iterator.next()) {
                    if (!iterator.visible(userClip)) {
                        transformedShapes[shapeIndex++] = null;
                        continue;
                    }
                    final AffineTransform fromShape;
                    Shape shape = iterator.geographicArea();
                    if (shape != null) {
                        /*
                         * Si l'utilisateur a d�finit une �tendue g�ographique
                         * pour cette marque,  alors la forme de cette �tendue
                         * sera transform�e et utilis�e telle quelle.
                         */
                        shape = projection.createTransformedShape(shape);
                        fromShape = fromWorld;
                    } else {
                        /*
                         * Si l'utilisateur a d�finit la forme d'une marque en pixels,
                         * alors cette marque sera translat�e � la coordonn�es voulue,
                         * puis une rotation sera appliqu�e en fonction du zoom actuel
                         * et de l'angle sp�cifi� par {@link #getDirection}.
                         */
                        Point2D point;
                        if ((point=iterator.position ())==null ||
                            (shape=iterator.markShape())==null)
                        {
                            transformedShapes[shapeIndex++] = null;
                            continue;
                        }
                        point = projection.transform(point, point);
                        matrix[4] = point.getX();
                        matrix[5] = point.getY();
                        fromWorld.transform(matrix, 4, matrix, 4, 1);
                        tr.setTransform(matrix[0], matrix[1], matrix[2], matrix[3], matrix[4], matrix[5]);
                        scale = iterator.amplitude()/typicalScale;
                        tr.scale(scale,scale);
                        tr.rotate(iterator.direction());
                        fromShape = tr;
                    }
                    /*
                     * A ce stade, on dispose maintenant 1) De la forme g�om�trique d'une
                     * marque et 2) de la transformation affine � appliquer sur la forme.
                     * V�rifie maintenant si la forme est un polygone (c'est-�-dire si ses
                     * points sont reli�s uniquement par des lignes droites). Si c'est le cas,
                     * un traitement sp�cial sera possible. Dans tous les cas, on conservera
                     * le r�sultat dans une cache interne afin d'�viter d'avoir � refaire ces
                     * calculs lors du prochain tra�age.
                     */
                    if (shape != lastShape) {
                        lastShape      = shape;
                        shapeIsPolygon = false;
                        final PathIterator pit = shape.getPathIterator(null);
                        if (!pit.isDone() && pit.currentSegment(array)==PathIterator.SEG_MOVETO) {
                            pointIndex = 2;
testPolygon:                for (pit.next(); !pit.isDone(); pit.next()) {
                                switch (pit.currentSegment(buffer)) {
                                    case PathIterator.SEG_LINETO: {
                                        if (pointIndex >= array.length) {
                                            array = XArray.resize(array, 2*pointIndex);
                                        }
                                        System.arraycopy(buffer, 0, array, pointIndex, 2);
                                        pointIndex += 2;
                                        continue testPolygon;
                                    }
                                    case PathIterator.SEG_CLOSE: {
                                        pit.next();
                                        shapeIsPolygon = pit.isDone();
                                        break testPolygon;
                                    }
                                    default: {
                                        // The shape is not a polygon.
                                        // Break the 'for' loop now.
                                        break testPolygon;
                                    }
                                }
                            }
                        }
                    }
                    /*
                     * Les coordonn�es de la forme g�om�trique ayant �t� obtenue,
                     * cr�� une forme g�om�trique transform�e (c'est-�-dire dont
                     * les coordonn�es seront exprim�es en pixels au lieu d'�tre
                     * en m�tres).
                     */
                    final Shape transformedShape;
                    if (!shapeIsPolygon) {
                        // La m�thode 'createTransformedShape' cr�e g�n�ralement un objet
                        // 'GeneralPath', qui peut convenir mais qui est quand m�me un peu
                        // lourd. Si possible, on va plut�t utiliser le code du bloc suivant,
                        // qui cr�era un objet 'Polygon'.
                        transformedShape = fromShape.createTransformedShape(shape);
                    } else {
                        if (pointIndex > buffer.length) {
                            buffer = XArray.resize(buffer, pointIndex);
                        }
                        final int length = pointIndex/2;
                        fromShape.transform(array, 0, buffer, 0, length);
                        if (length > X.length) X=XArray.resize(X, length);
                        if (length > Y.length) Y=XArray.resize(Y, length);
                        for (int j=0; j<length; j++) {
                            final int k = (j*2);
                            X[j] = (int) Math.round(buffer[k+0]);
                            Y[j] = (int) Math.round(buffer[k+1]);
                        }
                        transformedShape = new Polygon(X,Y,length);
                    }
                    /*
                     * Construit un rectangle qui englobera toutes
                     * les marques. Ce rectangle sera utilis� par
                     * {@link MapPanel} pour d�tecter quand la souris
                     * tra�ne dans la r�gion...
                     */
                    transformedShapes[shapeIndex++] = (transformedShape.intersects(zoomableBounds))
                                                    ? transformedShape : null;
                    final Rectangle bounds = transformedShape.getBounds();
                    if (shapeBoundingBox == null) {
                        shapeBoundingBox = bounds;
                    } else {
                        shapeBoundingBox.add(bounds);
                    }
                }
            }
            /*
             * Proc�de maintenant au tra�age de
             * toutes les marques de la couche.
             */
            final AffineTransform graphicsTr = graphics.getTransform();
            final Stroke          oldStroke  = graphics.getStroke();
            final Paint           oldPaint   = graphics.getPaint();
            try {
                int shapeIndex=0;
                iterator.seek(-1);
                graphics.setTransform(fromPoints);
                graphics.setStroke(DEFAULT_STROKE);
                final Rectangle clip = graphics.getClipBounds();
                while (iterator.next()) {
                    final Shape shape = transformedShapes[shapeIndex++];
                    if (shape!=null && (clip==null || shape.intersects(clip))) {
                        paint(graphics, shape, iterator);
                    }
                }
            } finally {
                graphics.setTransform(graphicsTr);
                graphics.setStroke(oldStroke);
                graphics.setPaint(oldPaint);
            }
        }
        context.addPaintedArea(shapeBoundingBox, context.textCS);
    }

    /**
     * Indique que cette couche a besoin d'�tre red�ssin�e. Cette m�thode
     * <code>repaint()</code> peut �tre appel�e � partir de n'importe quel
     * thread (pas n�cessairement celui de <cite>Swing</cite>).
     */
    public void repaint() {
        synchronized (getTreeLock()) {
            clearCache();
        }
        super.repaint();
    }

    /**
     * D�clare que la marque sp�cifi�e a besoin d'�tre redessin�e.
     * Cette m�thode peut �tre utilis�e pour faire appara�tre ou
     * dispara�tre une marque, apr�s que sa visibilit� (telle que
     * retourn�e par {@link MarkIterator#visible}) ait chang�e.
     *
     * Si un nombre restreint de marques sont � redessiner, cette
     * m�thode sera efficace car elle provoquera le retra�age d'une
     * portion relativement petite de la carte. Si toutes les marques
     * sont � redessiner, il peut �tre plus efficace d'appeller {@link
     * #repaint()}.
     */
    public void repaint(final int index) {
        synchronized (getTreeLock()) {
            if (transformedShapes != null) {
                final Shape shape = transformedShapes[index];
                if (shape != null) {
                    repaint(shape.getBounds());
                    return;
                }
            }
            repaint();
        }
    }

    /**
     * Efface des informations qui avaient �t� conserv�es dans une m�moire cache.
     * Cette m�thode est automatiquement appel�e lorsqu'il a �t� d�termin� que cette
     * couche ne sera plus affich�e avant un certain temps.
     */
    void clearCache() {
        assert Thread.holdsLock(getTreeLock());
        lastTransform     = null;
        lastProjection    = null;
        transformedShapes = null;
        shapeBoundingBox  = null;
        typicalAmplitude  = Double.NaN;
        super.clearCache();
    }




    /////////////////////////////////////////////////////////////////////////////////////////////
    ////////////    EVENTS (note: may be moved out of this class in a future version)    ////////
    /////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Temporary point for mouse events.
     */
    private transient Point2D point;

    /**
     * Retourne le texte � afficher dans une bulle lorsque le curseur
     * de la souris tra�ne sur la carte. L'impl�mentation par d�faut
     * identifie la marque sur laquelle tra�ne le curseur et appelle
     * {@link MarkIterator#getToolTipText()}.
     *
     * @param  event Coordonn�es du curseur de la souris.
     * @return Le texte � afficher lorsque la souris tra�ne sur cet �l�ment.
     *         Ce texte peut �tre nul pour signifier qu'il ne faut pas en �crire.
     */
    final String getToolTipText(final GeoMouseEvent event) {
        synchronized (getTreeLock()) {
            final Shape[] transformedShapes = RenderedMarks.this.transformedShapes;
            if (transformedShapes != null) {
                MarkIterator iterator = null;
                final Point2D point = this.point = event.getPixelCoordinate(this.point);
                for (int i=transformedShapes.length; --i>=0;) {
                    final Shape shape = transformedShapes[i];
                    if (shape != null) {
                        if (shape.contains(point)) {
                            if (iterator == null) {
                                iterator = getMarkIterator();
                            }
                            iterator.seek(i);
                            final String text = iterator.getToolTipText(event);
                            if (text != null) {
                                return text;
                            }
                        }
                    }
                }
            }
        }
        return super.getToolTipText(event);
    }
}
