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
package org.geotools.renderer;

// Geometry
import java.awt.geom.AffineTransform;

// Collections
import java.util.Iterator;


/**
 * It�rateur balayant les points d'un polygone ou d'un isobath.
 *
 * @version $Id: PathIterator.java,v 1.1 2003/01/13 22:40:50 desruisseaux Exp $
 * @author Martin Desruisseaux
 */
final class PathIterator implements java.awt.geom.PathIterator {
    /**
     * It�rateur balayant les objets
     * {@link Polygon} � tracer.
     */
    private final Iterator polygons;

    /**
     * Le polygone en cours de tra�age.
     */
    private Polygon polygon;

    /**
     * Transformation a appliquer aux coordonn�es
     * (rotation, translation, �chelle...)
     */
    private final AffineTransform transform;

    /**
     * Tableaux des coordonn�es � tracer. Ces coordonn�es
     * seront sous forme de paires (<var>x</var>,<var>y</var>).
     */
    private float[] array;

    /**
     * Index de la prochaine valeur � retourner
     * dans le tableau {@link #array}.
     */
    private int index;

    /**
     * Construit un it�rateur qui balayera les points d'un seul polygone.
     *
     * @param polygone Polygone � tracer.
     * @param transform Transformation affine facultative (peut �tre nulle).
     */
    public PathIterator(final Polygon polygon, final AffineTransform transform) {
        this.polygon   = polygon;
        this.polygons  = null;
        this.transform = transform;
        this.array     = polygon.getDrawingArray(transform);
        if (array==null || array.length==0) {
            polygon.releaseDrawingArray(array);
            array = null;
        }
    }

    /**
     * Construit un it�rateur qui balayera les points d'un ensemble de polygones.
     *
     * @param polygons It�rateur balayant une liste d'objets {@link Polygons}.
     * @param transform Transformation affine facultative (peut �tre nulle).
     */
    public PathIterator(final Iterator polygons, final AffineTransform transform) {
        this.polygons  = polygons;
        this.transform = transform;
        while (polygons.hasNext()) {
            polygon = (Polygon) polygons.next();
            array   = polygon.getDrawingArray(transform);
            if (array!=null && array.length!=0) {
                break;
            }
            polygon.releaseDrawingArray(array);
            array = null;
        }
    }

    /**
     * Tests if there are more points to read.
     * @return true if there are no more points to read.
     */
    public boolean isDone() {
        return array == null;
    }

    /**
     * Moves the polygons to the next segment of the path forwards
     * along the primary direction of traversal as long as there are
     * more points in that direction.
     */
    public void next() {
        if (array!=null && (index+=2) >= array.length) {
            if (index!=array.length || !polygon.isClosed()) {
                polygon.releaseDrawingArray(array);
                array = null;
                index = 0;
                if (polygons != null) {
                    while (polygons.hasNext()) {
                        polygon = (Polygon) polygons.next();
                        array   = polygon.getDrawingArray(transform);
                        if (array!=null && array.length!=0) {
                            return;
                        }
                        polygon.releaseDrawingArray(array);
                        array = null;
                    }
                }
            }
        }
    }

    /**
     * Retourne la coordonn�e et le type du prochain segment. Cette m�thode retourne
     * {@link #SEG_MOVETO} au d�but de chaque polygone. Apr�s le dernier point, elle
     * retourne {@link #SEG_CLOSE} si la forme g�om�trique est une �le, un lac ou tout
     * autre forme ferm�e. Entre ces deux extr�mit�s, elle retourne toujours {@link #SEG_LINETO}.
     *
     * @param into Tableau dans lequel m�moriser les coordonn�es du prochain point.
     *             Ce tableau doit avoir une longueur d'au moins 2 (pour 1 point).
     */
    public int currentSegment(final float into[]) {
        if (index < array.length) {
            into[0] = array[index  ];
            into[1] = array[index+1];
            return (index==0) ? SEG_MOVETO : SEG_LINETO;
        }
        return SEG_CLOSE;
    }

    /**
     * Retourne la coordonn�e et le type du prochain segment. Cette m�thode retourne
     * {@link #SEG_MOVETO} au d�but de chaque polygone. Apr�s le dernier point, elle
     * retourne {@link #SEG_CLOSE} si la forme g�om�trique est une �le, un lac ou tout
     * autre forme ferm�e. Entre ces deux extr�mit�s, elle retourne toujours {@link #SEG_LINETO}.
     *
     * @param into Tableau dans lequel m�moriser les coordonn�es du prochain point.
     *             Ce tableau doit avoir une longueur d'au moins 2 (pour 1 point).
     */
    public int currentSegment(final double into[]) {
        if (index < array.length) {
            into[0] = array[index  ];
            into[1] = array[index+1];
            return (index==0) ? SEG_MOVETO : SEG_LINETO;
        }
        return SEG_CLOSE;
    }

    /**
     * Return the winding rule for determining the interior of the path.
     * @return <code>WIND_EVEN_ODD</code> by default.
     */
    public int getWindingRule() {
        return WIND_EVEN_ODD;
    }
}
