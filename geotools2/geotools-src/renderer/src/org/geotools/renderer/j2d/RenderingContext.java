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

// J2SE dependencies
import java.util.List;
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

// Geotools dependencies
import org.geotools.cs.CoordinateSystem;
import org.geotools.ct.MathTransform2D;
import org.geotools.ct.TransformException;
import org.geotools.ct.CoordinateTransformation;
import org.geotools.ct.CannotCreateTransformException;
import org.geotools.resources.Utilities;
import org.geotools.resources.CTSUtilities;
import org.geotools.resources.XAffineTransform;
import org.geotools.resources.renderer.Resources;
import org.geotools.resources.renderer.ResourceKeys;


/**
 * Information relatives aux tra�age d'une carte. Ces informations comprennent notamment
 * la suite des transformations n�cessaires � la conversion de coordonn�es g�ographiques
 * en coordonn�es pixels.  Soit <code>point</code> un objet {@link Point2D} repr�sentant
 * une coordonn�e.  Alors la conversion de la coordonn�e g�ographique vers la coordonn�e
 * pixel suivra le chemin suivant (l'ordre des op�rations est important):
 *
 * <blockquote><table>
 *   <tr><td><code>{@link #getMathTransform2D getMathTransform2D}(layer).transform(point, point)</code></td>
 *       <td>pour convertir la coordonn�e g�ographique de l'objet {@link RenderedObject} sp�cifi�
 *           vers le syst�me de coordonn�es de l'afficheur {@link Renderer}. Le r�sultat est
 *           encore en coordonn�es logiques, par exemple en v�ritables m�tres sur le terrain
 *           ou encore en degr�s de longitude ou de latitude.</td></tr>
 *
 *   <tr><td><code>{@link #getAffineTransform getAffineTransform}({@link #WORLD_TO_POINT}).transform(point, point)</code></td>
 *       <td>pour transformer les m�tres en des unit�s proches de 1/72 de pouce. Avec cette
 *           transformation, nous passons des "m�tres" sur le terrain en "points" sur l'�cran
 *           (ou l'imprimante).</td></tr>
 *
 *   <tr><td><code>{@link #getAffineTransform getAffineTransform}({@link #POINT_TO_PIXEL}).transform(point, point)</code></td>
 *       <td>pour passer des 1/72 de pouce vers des unit�s qui d�pendent du p�riph�rique.</td></tr>
 * </table></blockquote>
 *
 * @version $Id: RenderingContext.java,v 1.1 2003/01/20 00:06:35 desruisseaux Exp $
 * @author Martin Desruisseaux
 *
 * @see RenderedObject#paint
 * @see Renderer#paint
 */
public final class RenderingContext {
    /**
     * Expansion factor for clip. When a clip for some rectangle is requested, a bigger
     * clip will be computed in order to avoid recomputing a new one if user zoom up or
     * apply translation. A scale of 2 means that rectangle two times wider and heigher
     * will be computed.
     *
     * DEBUGGING TIPS: Set this scale to a value below 1 to <em>see</em> the clipping's
     *                 effect in the window area.
     */
    private static final double CLIP_SCALE = 0.75;

    /**
     * The originating {@link Renderer}.
     */
    private final Renderer renderer;

    /**
     * Transformation (g�n�ralement une projection cartographique) servant � convertir les
     * coordonn�es g�ographiques vers les donn�es projet�es � l'�cran. La valeur retourn�e
     * par {@link CoordinateTransformation#getTargetCS} doit obligatoirement �tre le syst�me
     * de coordonn�es utilis� pour l'affichage. En revanche, la valeur retourn�e par {@link
     * CoordinateTransformation#getSourceCS} peut �tre n'importe quel syst�me de coordonn�es,
     * mais il vaux mieux pour des raisons de performance que ce soit le syst�me de
     * coordonn�es le plus utilis� par les objets � dessiner.
     */
    private final CoordinateTransformation transformation;

    /**
     * Transformation affine convertissant les m�tres vers les unit�s de texte (1/72 de pouce).
     * Ces unit�s de textes pourront ensuite �tre converties en unit�s du p�riph�rique avec la
     * transformation {@link #fromPoints}. Cette transformation <code>fromWorld</code> peut varier
     * en fonction de l'�chelle de la carte, tandis que la transformation {@link #fromPoints}
     * ne varie g�n�ralement pas pour un p�riph�rique donn�.
     */
    private final AffineTransform fromWorld;

    /**
     * Transformation affine convertissant des unit�s de texte (1/72 de pouce) en unit�s
     * d�pendantes du p�riph�rique. Lors des sorties vers l'�cran, cette transformation est
     * g�n�ralement la matrice identit�. Pour les �critures vers l'imprimante, il s'agit d'une
     * matrice configur�e d'une fa�on telle que chaque point correspond � environ 1/72 de pouce.
     *
     * Cette transformation affine reste habituellement identique d'un tra�age � l'autre
     * de la composante. Elle varie si par exemple on passe de l'�cran vers l'imprimante.
     */
    private final AffineTransform fromPoints;

    /**
     * Position et dimension de la r�gion de la
     * fen�tre dans lequel se fait le tra�age.
     */
    private final Rectangle bounds;

    /**
     * The {@link #bounds} rectangle transformed into logical
     * coordinates (according {@link #getViewCoordinateSystem}).
     */
//    private transient Rectangle2D logicalClip;

    /**
     * Objet � utiliser pour d�couper les polygones. Cet objet
     * ne sera cr�� que la premi�re fois o� il sera demand�.
     */
//    private transient Clipper clipper;

    /**
     * Construit un objet <code>RenderingContext</code> avec les param�tres sp�cifi�s.
     * Ce constructeur ne fait pas de clones.
     *
     * @param renderer The originating {@link Renderer}.
     * @param transformation Transformation (g�n�ralement une projection cartographique) servant �
     *               convertir les coordonn�es g�ographiques vers les donn�es projet�es � l'�cran.
     *               La valeur retourn�e par {@link CoordinateTransformation#getTargetCS} doit
     *               obligatoirement �tre le syst�me de coordonn�es utilis� pour l'affichage. En
     *               revanche, la valeur retourn�e par {@link CoordinateTransformation#getSourceCS}
     *               peut �tre n'importe quel syst�me de coordonn�es, mais il vaux mieux pour des
     *               raisons de performance que ce soit le syst�me de coordonn�es le plus utilis�
     *               par les objets � dessiner.
     * @param fromWorld  Transformation affine convertissant les m�tres vers les unit�s de texte
     *                   (1/72 de pouce).
     * @param fromPoints Transformation affine convertissant des unit�s de texte (1/72 de pouce)
     *                   en unit�s d�pendantes du p�riph�rique.
     * @param bounds     Position et dimension de la r�gion de la fen�tre dans lequel se fait le
     *                   tra�age.
     */
    RenderingContext(final Renderer                 renderer,
                     final CoordinateTransformation transformation,
                     final AffineTransform          fromWorld,
                     final AffineTransform          fromPoints,
                     final Rectangle                bounds)
    {
        if (renderer!=null && transformation!=null && fromWorld!=null && fromPoints!=null) {
            this.renderer       = renderer;
            this.transformation = transformation;
            this.fromWorld      = fromWorld;
            this.fromPoints     = fromPoints;
            this.bounds         = bounds;
        } else {
            throw new NullPointerException();
        }
    }

    /**
     * Returns the view coordinate system. This is the coordinate system to be used
     * for drawing objects on the widget. User can set this coordinate system prior
     * rendering with {@link Renderer#setCoordinateSystem}.
     *
     * @see Renderer#setCoordinateSystem
     * @see Renderer#getCoordinateSystem
     * @see RenderedObject#getCoordinateSystem
     */
    public CoordinateSystem getViewCoordinateSystem() {
        return transformation.getTargetCS();
    }

    /**
     * Retourne la transformation � utiliser pour convertir les coordonn�es d'un objet vers
     * les coordonn�es projet�es � l'�cran. Cette transformation sera souvent une projection
     * cartographique.
     *
     * @param  layer Objet dont on veut convertir les coordonn�es.
     * @return Une transformation qui transformera les coordonn�es de l'objet sp�cifi�
     *         (<code>layer</code>) vers les coordonn�es affich�es � l'�cran ({@link Renderer}).
     * @throws CannotCreateTransformException Si la transformation n'a pas pu �tre cr��e.
     */
    public MathTransform2D getMathTransform2D(final RenderedObject layer)
            throws CannotCreateTransformException
    {
        CoordinateTransformation transformation = this.transformation;
        final CoordinateSystem source = layer.getCoordinateSystem();
        if (!transformation.getSourceCS().equals(source, false)) {
            transformation = renderer.getCoordinateTransformation(source,
                                        transformation.getTargetCS(),
                                        "RenderingContext", "getMathTransform2D");
        }
        return (MathTransform2D) transformation.getMathTransform();
    }

    /**
     * Retourne une transformation affine. Deux types de transformations sont d'int�ret:
     *
     * <ul>
     *   <li>{@link TransformationStep#WORLD_TO_POINT}:
     *       Transformation affine convertissant les m�tres vers les unit�s de texte (1/72 de pouce).
     *       Ces unit�s de textes pourront ensuite �tre converties en unit�s du p�riph�rique avec la
     *       transformation {@link #POINT_TO_PIXEL}. Cette transformation peut varier en fonction
     *       de l'�chelle de la carte.</li>
     *   <li>{@link TransformationStep#POINT_TO_PIXEL}:
     *       Transformation affine convertissant des unit�s de texte (1/72 de pouce) en unit�s
     *       d�pendantes du p�riph�rique. Lors des sorties vers l'�cran, cette transformation
     *       est g�n�ralement la matrice identit�. Pour les �critures vers l'imprimante, il s'agit
     *       d'une matrice configur�e d'une fa�on telle que chaque point correspond � environ 1/72
     *       de pouce. Cette transformation affine reste habituellement identique d'un tra�age �
     *       l'autre de la composante. Elle ne varie que si on change de p�riph�rique, par exemple
     *       si on dessine vers l'imprimante plut�t que l'�cran.</li>
     * </ul>
     *
     * <strong>Note: cette m�thode ne fait pas de clone. Ne modifiez pas l'objet retourn�!</strong>
     */
    public AffineTransform getAffineTransform(final TransformationStep type) {
        if (TransformationStep.WORLD_TO_POINT.equals(type)) {
            return fromWorld;
        }
        if (TransformationStep.POINT_TO_PIXEL.equals(type)) {
            return fromPoints;
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    /**
     * Returns <code>true</code> if the rendering is performed on the
     * screen or any other devices with an identity default transform.
     * This method usually returns <code>false</code> during printing.
     */
    final boolean normalDrawing() {
        return fromPoints.isIdentity();
    }

    /**
     * Returns the drawing area in point coordinates.
     * For performance reason, this method do not clone
     * the area. Do not modify it!
     */
    public Rectangle getDrawingArea() {
        return bounds;
    }

    /**
     * Returns the rendered area in point coordinates.
     *
     * @task TODO: Not yet implemented.
     */
    final Shape getRenderedArea() {
        return null; // TODO
    }

    /**
     * Clip a contour to the current widget's bounds. The clip is only approximative
     * in that the resulting contour may extends outside the widget's area. However,
     * it is garanteed that the resulting contour will contains at least the interior
     * of the widget's area (providing that the first contour in the supplied list
     * cover this area).
     *
     * This method is used internally by some layers (like {@link fr.ird.map.layer.IsolineLayer})
     * when computing and drawing a clipped contour may be faster than drawing the full contour
     * (especially if clipped contours are cached for reuse).
     * <br><br>
     * This method expect a <em>modifiable</em> list of {@link Contour} objects as argument.
     * The first element in this list must be the "master" contour (the contour to clip) and
     * will never be modified.  Elements at index greater than 0 may be added and removed at
     * this method's discression, so that the list is actually used as a cache for clipped
     * <code>Contour</code> objects.
     *
     * <br><br>
     * <strong>WARNING: This method is not yet debugged</strong>
     *
     * @param  contours A modifiable list with the contour to clip at index 0.
     * @return A possibly clipped contour. May be any element of the list or a new contour.
     *         May be <code>null</code> if the "master" contour doesn't intercept the clip.
     */
//    public Contour clip(final List<Contour> contours) {
//        if (contours.isEmpty()) {
//            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_EMPTY_LIST));
//        }
//        if (isPrinting) {
//            return contours.get(0);
//        }
//        /*
//         * Gets the clip area expressed in MapPanel's coordinate system
//         * (i.e. gets bounds in "logical visual coordinates").
//         */
//        if (logicalClip==null) try {
//            logicalClip = XAffineTransform.inverseTransform(fromWorld, bounds, logicalClip);
//        } catch (NoninvertibleTransformException exception) {
//            // (should not happen) Clip failed: conservatively returns the whole contour.
//            Utilities.unexpectedException("fr.ird.map", "RenderingContext", "clip", exception);
//            return contours.get(0);
//        }
//        final CoordinateSystem targetCS = getViewCoordinateSystem();
//        /*
//         * Iterate through the list (starting from the last element)
//         * until we found a contour capable to handle the clip area.
//         */
//        Contour contour;
//        Rectangle2D clip;
//        Rectangle2D bounds;
//        Rectangle2D temporary=null;
//        int index=contours.size();
//        do {
//            contour = contours.get(--index);
//            clip    = logicalClip;
//            /*
//             * First, we need to know the clip in contour's coordinates.
//             * The {@link fr.ird.map.layer.IsolineLayer} usually keeps
//             * isoline in the same coordinate system than the MapPanel's
//             * one. But a user could invoke this method in a more unusual
//             * way, so we are better to check...
//             */
//            final CoordinateSystem sourceCS;
//            synchronized (contour) {
//                bounds   = contour.getCachedBounds();
//                sourceCS = contour.getCoordinateSystem();
//            }
//            if (!targetCS.equals(sourceCS, false)) try {
//                CoordinateTransformation transformation = this.transformation;
//                if (!transformation.getSourceCS().equals(sourceCS, false)) {
//                    transformation = Contour.getCoordinateTransformation(sourceCS, targetCS, "RenderingContext", "clip");
//                }
//                clip = temporary = CTSUtilities.transform((MathTransform2D)transformation.getMathTransform(), clip, temporary);
//            } catch (TransformException exception) {
//                Utilities.unexpectedException("fr.ird.map", "RenderingContext", "clip", exception);
//                continue; // A contour seems invalid. It will be ignored (and probably garbage collected soon).
//            }
//            /*
//             * Now that both rectangles are using the same coordinate system,
//             * test if the clip fall completly inside the contour. If yes,
//             * then we should use this contour for clipping.
//             */
//            if (Layer.contains(bounds, clip, true)) {
//                break;
//            }
//        } while (index!=0);
//        /*
//         * A clipped contour has been found (or we reached the begining
//         * of the list). Check if the requested clip is small enough to
//         * worth a clipping.
//         */
//        final double ratio2 = (bounds.getWidth()*bounds.getHeight()) / (clip.getWidth()*clip.getHeight());
//        if (ratio2 >= CLIP_SCALE*CLIP_SCALE) {
//            if (clipper == null) {
//                clipper = new Clipper(scale(logicalClip, CLIP_SCALE), targetCS);
//            }
//            // Remove the last part of the list, which is likely to be invalide.
//            contours.subList(index+1, contours.size()).clear();
//            contour = contour.getClipped(clipper);
//            if (contour != null) {
//                contours.add(contour);
//                Contour.LOGGER.finer("Clip performed"); // TODO: give more precision
//            }
//        }
//        return contour;
//    }

    /**
     * Expand or shrunk a rectangle by some factor. A scale of 1 lets the rectangle
     * unchanged. A scale of 2 make the rectangle two times wider and heigher. In
     * any case, the rectangle's center doesn't move.
     */
    private static Rectangle2D scale(final Rectangle2D rect, final double scale) {
        final double trans  = 0.5*(scale-1);
        final double width  = rect.getWidth();
        final double height = rect.getHeight();
        return new Rectangle2D.Double(rect.getX()-trans*width,
                                      rect.getY()-trans*height,
                                      scale*width, scale*height);
    }
}
