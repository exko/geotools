/*
 * Geotools - OpenSource mapping toolkit
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


/**
 * Classe enveloppant une portion seulement d'un tableau <code>float[]</code>.
 * Des instances de cette classes sont retourn�es par {@link DefaultArray#subarray}.
 * L'impl�mentation par d�faut de cette classe est imutable. Toutefois, certaines
 * classes d�riv�es (notamment {@link DynamicArray}) ne le seront pas forc�ment.
 *
 * @version $Id: SubArray.java,v 1.2 2003/01/20 00:06:34 desruisseaux Exp $
 * @author Martin Desruisseaux
 */
class SubArray extends DefaultArray {
    /**
     * Num�ro de s�rie (pour compatibilit� avec des versions ant�rieures).
     */
    private static final long serialVersionUID = 5172936367826790633L;

    /**
     * Plage des donn�es valides
     * du tableau {@link #array}.
     */
    protected int lower, upper;

    /**
     * Enveloppe une partie d'un tableau de <code>float[]</code>.
     *
     * @param  array Tableau de coordonn�es (<var>x</var>,<var>y</var>).
     * @param  lower Index de la premi�re coordonn�es <var>x</var> �
     *         prendre en compte dans le tableau <code>array</code>.
     * @param  upper Index suivant celui de la derni�re coordonn�e <var>y</var> �
     *         prendre en compte dans le tableau <code>array</code>. La diff�rence
     *         <code>upper-lower</code> doit obligatoirement �tre paire.
     */
    public SubArray(final float[] array, final int lower, final int upper) {
        super(array);
        this.lower = lower;
        this.upper = upper;
        checkRange(array, lower, upper);
    }

    /**
     * Retourne l'index de la
     * premi�re coordonn�e valide.
     */
    protected final int lower() {
        return lower;
    }

    /**
     * Retourne l'index suivant celui
     * de la derni�re coordonn�e valide.
     */
    protected final int upper() {
        return upper;
    }

    /**
     * Returns an estimation of memory usage in bytes. This method returns the same value
     * than {@link DefaultArray#getMemoryUsage} plus 8 bytes for the internal fields (the
     * {@link #lower} and {@link #upper} fields).
     */
    public long getMemoryUsage() {
        return super.getMemoryUsage() + 8;
    }
}
