/*
 * Geotools 2 - OpenSource mapping toolkit
 * (C) 2003, Geotools Project Managment Committee (PMC)
 * (C) 2001, Institut de Recherche pour le D�veloppement
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
package org.geotools.renderer.array;

// Divers
import java.awt.geom.Point2D;
import org.geotools.resources.XArray;
import org.geotools.resources.renderer.Resources;
import org.geotools.resources.renderer.ResourceKeys;


/**
 * Default implementation of {@link PointArray} wrapping an array of (<var>x</var>,<var>y</var>)
 * coordinates as a flat <code>float[]</code> array. The default implementation is immutable and
 * doesn't use any compression technic. However, subclasses may be mutable (i.e. support the
 * {@link #insertAt insertAt(...)} method) or compress data.
 *
 * @version $Id: DefaultArray.java,v 1.8 2003/05/23 17:58:59 desruisseaux Exp $
 * @author Martin Desruisseaux
 *
 * @see #getInstance
 */
public class DefaultArray extends PointArray implements RandomAccess {
    /**
     * Serial version for compatibility with previous version.
     */
    private static final long serialVersionUID = 3160219929318094867L;

    /**
     * The array of (<var>x</var>,<var>y</var>) coordinates.
     */
    protected float[] array;

    /**
     * Wrap the given (<var>x</var>,<var>y</var>) array. The constructor stores a direct
     * reference to <code>array</code> (i.e. the array is not copied). Do not modify the
     * data after construction if this <code>DefaultArray</code> should be immutable.
     *
     * @param  array The array of (<var>x</var>,<var>y</var>) coordinates.
     * @throws IllegalArgumentException if the array's length is not even.
     */
    public DefaultArray(final float[] array) throws IllegalArgumentException {
        this.array = array;
        if ((array.length & 1) != 0) {
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_ODD_ARRAY_LENGTH_$1,
                                               new Integer(array.length)));
        }
    }

    /**
     * Returns a <code>PointArray</code> object wrapping the given (<var>x</var>,<var>y</var>)
     * array between the specified bounds. If the array doesn't contains any data (i.e. if
     * <code>lower==upper</code>), then this method returns <code>null</code>.
     *
     * @param  array The array of (<var>x</var>,<var>y</var>) coordinates.
     * @param  lower Index of the first <var>x</var> ordinate in <code>array</code>.
     * @param  upper Index after the last <var>y</var> oordinate in <code>array</code>.
     *         The difference <code>upper-lower</code> must be even.
     * @param  copy <code>true</code> if this method should copy the array (in order to
     *         protect the <code>PointArray</code> from changes), or <code>false</code>
     *         for a direct reference without copying. In the later case, the caller is
     *         responsable to ensure that the array will not be modified externally.
     * @return The <code>PointArray</code> object wrapping the given <code>array</code>.
     */
    public static PointArray getInstance(final float[] array, final int lower, final int upper,
                                         final boolean copy)
    {
        checkRange(array, lower, upper);
        if (upper == lower) {
            return null;
        }
        if (copy) {
            final float[] newArray = new float[upper-lower];
            System.arraycopy(array, lower, newArray, 0, newArray.length);
            return new DefaultArray(newArray);
        } else if (lower==0 && upper==array.length) {
            return new DefaultArray(array);
        } else {
            return new SubArray(array, lower, upper);
        }
    }

    /**
     * Returns the index of the first valid ordinate (inclusive).
     */
    protected int lower() {
        return 0;
    }

    /**
     * Returns the index after the last valid ordinate.
     */
    protected int upper() {
        return array.length;
    }

    /**
     * Returns the number of points in this array.
     */
    public final int count() {
        return (upper()-lower())/2;
    }

    /**
     * Returns an estimation of memory usage in bytes. This method count 8 bytes for each
     * (x,y) points plus 4 bytes for the internal fields (the {@link #array} reference).
     */
    public long getMemoryUsage() {
        return count()*8 + 4;
    }

    /**
     * Returns the first point in this array. If <code>point</code> is null, a new
     * {@link Point2D} object is allocated and then the result is stored in this object.
     *
     * @param  point The object in which to store the first point, or <code>null</code>.
     * @return <code>point</code> or a new {@link Point2D}, which contains the first point.
     */
    public final Point2D getFirstPoint(final Point2D point) {
        final int lower=lower();
        assert lower <= upper();
        final float x = array[lower+0];
        final float y = array[lower+1];
        if (point != null) {
            point.setLocation(x,y);
            return point;
        } else {
            return new Point2D.Float(x,y);
        }
    }

    /**
     * Returns the last point in this array. If <code>point</code> is null, a new
     * {@link Point2D} object is allocated and then the result is stored in this object.
     *
     * @param  point The object in which to store the last point, or <code>null</code>.
     * @return <code>point</code> or a new {@link Point2D}, which contains the last point.
     */
    public final Point2D getLastPoint(final Point2D point) {
        final int upper=upper();
        assert(upper >= lower());
        final float x = array[upper-2];
        final float y = array[upper-1];
        if (point != null) {
            point.setLocation(x,y);
            return point;
        } else {
            return new Point2D.Float(x,y);
        }
    }

    /**
     * Returns the point at the specified index.
     *
     * @param  index The index from 0 inclusive to {@link #count} exclusive.
     * @return The point at the given index.
     * @throws IndexOutOfBoundsException if <code>index</code> is out of bounds.
     */
    public Point2D getValue(int index) throws IndexOutOfBoundsException {
        index *= 2;
        return new Point2D.Float(array[index], array[index+1]);
    }

    /**
     * Returns an iterator object that iterates along the point coordinates.
     *
     * @param  index Index of the first point to returns in the iteration.
     * @return The iterator.
     */
    public final PointIterator iterator(final int index) {
        return new DefaultIterator(array, (2*index)+lower(), upper());
    }

    /**
     * Retourne un tableau enveloppant les m�mes points que le tableau courant,
     * mais des index <code>lower</code> inclusivement jusqu'� <code>upper</code>
     * exclusivement. Si le sous-tableau ne contient aucun point (c'est-�-dire si
     * <code>lower==upper</code>), alors cette m�thode retourne <code>null</code>.
     *
     * @param lower Index du premier point � prendre en compte.
     * @param upper Index suivant celui du dernier point � prendre en compte.
     */
    public PointArray subarray(int lower, int upper) {
        final int thisLower=lower();
        final int thisUpper=upper();
        lower = lower*2 + thisLower;
        upper = upper*2 + thisLower;
        if (lower            == upper              ) return null;
        if (lower==thisLower && upper==thisUpper   ) return this;
        if (lower==0         && upper==array.length) return new DefaultArray(array);
        return new SubArray(array, lower, upper);
    }

    /**
     * Ins�re les donn�es de <code>this</code> dans le tableau sp�cifi�. Cette m�thode est
     * strictement r�serv�e � l'impl�mentation de {@link #insertAt(int,PointArray,boolean)}.
     * La classe {@link DefaultArray} remplace l'impl�mentation par d�faut par une nouvelle
     * impl�mentation qui �vite de copier les donn�es avec {@link #toArray()}.
     */
    PointArray insertTo(final PointArray dest, final int index, final boolean reverse) {
        return dest.insertAt(index, array, lower(), upper(), reverse);
    }

    /**
     * Ins�re les donn�es (<var>x</var>,<var>y</var>) du tableau <code>toMerge</code> sp�cifi�.
     * Si le drapeau <code>reverse</code> � la valeur <code>true</code>, alors les points de
     * <code>toMerge</code> seront copi�es en ordre inverse.
     *
     * @param  index Index � partir d'o� ins�rer les points dans ce tableau. Le point � cet
     *         index ainsi que tous ceux qui le suivent seront d�cal�s vers des index plus �lev�s.
     * @param  toMerge Tableau de coordonn�es (<var>x</var>,<var>y</var>) � ins�rer dans ce
     *         tableau de points. Ses valeurs seront copi�es.
     * @param  lower Index de la premi�re coordonn�e de <code>toMerge</code> � copier dans ce tableau.
     * @param  upper Index suivant celui de la derni�re coordonn�e de <code>toMerge</code> � copier.
     * @param  reverse <code>true</code> s'il faut inverser l'ordre des points de <code>toMerge</code>
     *         lors de la copie. Cette inversion ne change pas l'ordre (<var>x</var>,<var>y</var>) des
     *         coordonn�es de chaque points.
     *
     * @return <code>this</code> si l'insertion � pu �tre faite sur
     *         place, ou un autre tableau si �a n'a pas �t� possible.
     */
    public PointArray insertAt(final int index, final float toMerge[],
                               final int lower, final int upper, final boolean reverse)
    {
        int count = upper-lower;
        if (count == 0) {
            return this;
        }
        return new DynamicArray(array, lower(), upper(), 2*(count+Math.min(count, 256)))
                        .insertAt(index, toMerge, lower, upper, reverse);
    }

    /**
     * Renverse l'ordre de tous les points compris dans ce tableau.
     *
     * @return <code>this</code> si l'inversion a pu �tre faite sur-place,
     *         ou un autre tableau si �a n'a pas �t� possible.
     */
    public PointArray reverse() {
        return new DynamicArray(array, lower(), upper(), 16).reverse();
    }

    /**
     * Retourne un tableau immutable qui contient les m�mes donn�es que celui-ci.
     * Apr�s l'appel de cette m�thode, toute tentative de modification (avec les
     * m�thodes {@link #insertAt} ou {@link #reverse}) vont retourner un autre
     * tableau de fa�on � ne pas modifier le tableau immutable.
     *
     * @param  compress <code>true</code> si l'on souhaite aussi comprimer les
     *         donn�es. Cette compression peut se traduire par une plus grande
     *         lenteur lors des acc�s aux donn�es.
     * @return Tableau immutable et �ventuellement compress�, <code>this</code>
     *         si ce tableau r�pondait d�j� aux conditions ou <code>null</code>
     *         si ce tableau ne contient aucune donn�e.
     */
    public PointArray getFinal(final boolean compress) {
        if (compress && count() >= 8) {
            return new CompressedArray(array, lower(), upper());
        }
        return super.getFinal(compress);
    }

    /**
     * Append (<var>x</var>,<var>y</var>) coordinates to the specified destination array.
     * The destination array will be filled starting at index {@link ArrayData#length}.
     * If <code>resolution2</code> is greater than 0, then points that are closer than
     * <code>sqrt(resolution2)</code> from previous one will be skiped.
     *
     * @param  The destination array. The coordinates will be filled in
     *         {@link ArrayData#array}, which will be expanded if needed.
     *         After this method completed, {@link ArrayData#length} will
     *         contains the index after the <code>array</code>'s element
     *         filled with the last <var>y</var> ordinate.
     * @param  resolution2 The minimum squared distance desired between points.
     */
    public final void toArray(final ArrayData dest, final float resolution2) {
        if (!(resolution2 >= 0)) {
            throw new IllegalArgumentException(String.valueOf(resolution2));
        }
        final int offset = dest.length;
        float[]   copy   = dest.array;
        if (resolution2 == 0) {
            final int lower    = lower();
            final int length   = upper()-lower;
            final int capacity = offset + length;
            if (copy.length < capacity) {
                dest.array = copy = XArray.resize(copy, capacity);
            }
            System.arraycopy(array, lower, copy, offset, length);
            dest.length = capacity;
        } else {
            int src = lower();
            int dst = offset;
            final int upper = upper();
            if (src < upper) {
                if (copy.length <= dst) {
                    dest.array = copy = XArray.resize(copy, capacity(src, dst, offset));
                }
                float lastX = copy[dst++] = array[src++];
                float lastY = copy[dst++] = array[src++];
                while (src < upper) {
                    final float  x  = array[src++];
                    final float  y  = array[src++];
                    final double dx = (double)x - (double)lastX;
                    final double dy = (double)y - (double)lastY;
                    if ((dx*dx + dy*dy) >= resolution2) {
                        if (copy.length <= dst) {
                            dest.array = copy = XArray.resize(copy, capacity(src, dst, offset));
                        }
                        copy[dst++] = lastX = x;
                        copy[dst++] = lastY = y;
                    }
                }
            }
            dest.length = dst;
        }
        assert dest.length <= dest.array.length;
    }
}
