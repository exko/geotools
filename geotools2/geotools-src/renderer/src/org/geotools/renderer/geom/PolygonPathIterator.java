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
package org.geotools.renderer.geom;

// J2SE dependencies
import java.util.Iterator;
import java.awt.geom.PathIterator;
import java.awt.geom.AffineTransform;
import java.awt.geom.IllegalPathStateException;

// Geotools dependencies
import org.geotools.renderer.array.ArrayData;


/**
 * Iterator for iterating through a collection of {@link Polyline} objects.
 * This iterator is typically used for drawing a single {@link Polygon}, which
 * may be made of many {@link Polylines}.
 *
 * @version $Id: PolygonPathIterator.java,v 1.5 2003/05/27 18:22:43 desruisseaux Exp $
 * @author Martin Desruisseaux
 */
final class PolygonPathIterator extends ArrayData implements PathIterator {
    /**
     * It�rateur balayant les objets {@link Polyline} � tracer.
     */
    private final Iterator polylines;

    /**
     * The polylines in the process of being drawn.
     */
    private Polyline polyline;

    /**
     * Transformation a appliquer aux coordonn�es (rotation, translation, �chelle...).
     */
    private final AffineTransform transform;

    /**
     * Index de la prochaine valeur � retourner dans le tableau {@link #array}.
     */
    private int index;

    /**
     * The type of the next curve to returns.
     */
    private int curveType = SEG_MOVETO;

    /**
     * Construit un it�rateur qui balayera les points d'un ensemble de polylignes.
     *
     * @param first     The first polyline, or <code>null</code> if none.
     * @param polylines Iterator through the next {@link Polylines} objects, or <code>null</code>.
     * @param transform An optional affine transform, or <code>null</code> if none.
     */
    public PolygonPathIterator(final Polyline first,
                               final Iterator polylines,
                               final AffineTransform transform) {
        this.polyline  = first;
        this.polylines = polylines;
        this.transform = transform;
        if (polyline != null) {
            final PolylineCache cache = polyline.getCache();
            cache.getRenderingArray(polyline, this, transform);
            if (array!=null && length!=0) {
                return;
            }
            cache.releaseRenderingArray(array);
            array = null;
        }
        if (polylines != null) {
            while (polylines.hasNext()) {
                polyline = (Polyline) polylines.next();
                final PolylineCache cache = polyline.getCache();
                cache.getRenderingArray(polyline, this, transform);
                if (array!=null && length!=0) {
                    break;
                }
                cache.releaseRenderingArray(array);
                array = null;
            }
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
     * Moves the iterator to the next segment of the path forwards
     * along the primary direction of traversal as long as there are
     * more points in that direction.
     */
    public void next() {
        if (array != null) {
            switch (curveType) {
                case SEG_MOVETO:  // fall through
                case SEG_LINETO:  index+=2; break;
                case SEG_QUADTO:  index+=4; break;
                case SEG_CUBICTO: index+=6; break;
                default: throw new IllegalPathStateException();
            }
            if (index >= length) {
                if (index!=length || !polyline.isClosed()) {
                    synchronized (polyline) {
                        polyline.getCache().releaseRenderingArray(array);
                        setData(null, 0, null);
                        index = 0;
                    }
                    if (polylines != null) {
                        while (polylines.hasNext()) {
                            polyline = (Polyline) polylines.next();
                            synchronized (polyline) {
                                final PolylineCache cache = polyline.getCache();
                                cache.getRenderingArray(polyline, this, transform);
                                if (array!=null && length!=0) {
                                    break;
                                }
                                cache.releaseRenderingArray(array);
                                setData(null, 0, null);
                            }
                        }
                    }
                }
            }
            curveType = getCurveType(index);
        }
    }

    /**
     * Retourne la coordonn�e et le type du prochain segment. Cette m�thode retourne
     * {@link #SEG_MOVETO} au d�but de chaque polyligne. Apr�s le dernier point, elle
     * retourne {@link #SEG_CLOSE} si la forme g�om�trique est une �le, un lac ou tout
     * autre forme ferm�e. Entre ces deux extr�mit�s, elle retourne toujours {@link #SEG_LINETO}.
     *
     * @param into Tableau dans lequel m�moriser les coordonn�es du prochain point.
     *             Ce tableau doit avoir une longueur d'au moins 2 (pour 1 point).
     */
    public int currentSegment(final float into[]) {
        if (index < length) {
            final int n;
            switch (curveType) {
                case SEG_CUBICTO: n=6; break;
                case SEG_QUADTO:  n=4; break;
                case SEG_LINETO:  // fall through
                case SEG_MOVETO:  n=2; break;
                default: throw new IllegalPathStateException();
            }
            System.arraycopy(array, index, into, 0, n);
            return curveType;
        }
        return SEG_CLOSE;
    }

    /**
     * Retourne la coordonn�e et le type du prochain segment. Cette m�thode retourne
     * {@link #SEG_MOVETO} au d�but de chaque polyligne. Apr�s le dernier point, elle
     * retourne {@link #SEG_CLOSE} si la forme g�om�trique est une �le, un lac ou tout
     * autre forme ferm�e. Entre ces deux extr�mit�s, elle retourne toujours {@link #SEG_LINETO}.
     *
     * @param into Tableau dans lequel m�moriser les coordonn�es du prochain point.
     *             Ce tableau doit avoir une longueur d'au moins 2 (pour 1 point).
     */
    public int currentSegment(final double into[]) {
        if (index < length) {
            switch (curveType) {
                case SEG_CUBICTO: {
                    into[5] = array[index+5];
                    into[4] = array[index+4];
                    // fall through
                }
                case SEG_QUADTO: {
                    into[3] = array[index+3];
                    into[2] = array[index+2];
                    // fall through
                }
                case SEG_LINETO: {
                    // fall through
                }
                case SEG_MOVETO: {
                    into[1] = array[index+1];
                    into[0] = array[index+0];
                    break;
                }
                default: {
                    throw new IllegalPathStateException();
                }
            }
            return curveType;
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
