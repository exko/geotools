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

// Divers
import java.util.Arrays;
import org.geotools.resources.XArray;


/**
 * Enveloppe un tableau <code>float[]</code> dans
 * lequel des donn�es pourront �tre ajout�s.
 *
 * @version $Id: DynamicArray.java,v 1.1 2003/01/10 23:08:46 desruisseaux Exp $
 * @author Martin Desruisseaux
 */
final class DynamicArray extends SubArray {
    /**
     * Num�ro de s�rie (pour compatibilit� avec des versions ant�rieures).
     */
    private static final long serialVersionUID = 3336921710471431118L;

    /**
     * Construit un tableau qui contiendra une copie
     * des coordonn�es du tableau sp�cifi�.
     */
    public DynamicArray(final PointArray points) {
        super(points.toArray(), 0, 2*points.count());
        assert array.length == upper;
    }

    /**
     * Construit un tableau qui enveloppera les donn�es <code>float[]</code> sp�cifi�s.
     * Les donn�es seront copi�es, de sorte que les futures modifications apport�es �
     * ce tableau ne modifieront pas les donn�es originales.
     */
    public DynamicArray(final float[] array, final int lower, final int upper, final int extra) {
        super(new float[(upper-lower) + extra], lower, upper);
        final int length = upper-lower;
        this.lower = extra/2;
        this.upper = this.lower + length;
        System.arraycopy(array, lower, this.array, this.lower, length);
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
        lower = 2*lower + this.lower;
        upper = 2*upper + this.lower;
        return getInstance(array, lower, upper);
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
    public PointArray insertAt(int index, final float toMerge[],
                               final int mergeLower, final int mergeUpper, final boolean reverse)
    {
        int count = mergeUpper-mergeLower;
        if (count==0) {
            return this;
        }
        index  = 2*index + lower;
        final int     cLower = lower;
        final int     cUpper = upper;
        final int     cIndex = index;
        final float[] cArray = array;
        final boolean firstHalf = (cIndex < (cLower+cUpper)/2);
        /*
         * CAS 1: Les donn�es sont ajout�es dans la premi�re moiti�e du tableau.
         *        Ins�re de la place en d�pla�ant les derniers points vers le d�but.
         *        Toutefois, s'il s'av�re n�cessaire d'agrandir le tableau, alors on
         *        en profitera pour ajouter un peu d'espace � la fin du tableau car
         *        �a pourrait servir plus tard...
         *
         * CAS 2: Les donn�es sont ajout�es dans la derni�re moiti�e du tableau.
         *        Ins�re de la place en d�pla�ant les derniers points vers la fin,
         *        comme d'habitude. Toutefois, s'il s'av�re n�cessaire d'agrandir
         *        le tableau, alors on en profitera pour ajouter un peu d'espace
         *        au d�but du tableau car �a pourrait servir plus tard...
         */
        if (firstHalf) lower -= count;
        else           upper += count;
        if (lower<0 || upper>=cArray.length) {
            int offset = Math.max(1024, count);
            array  = new float[cArray.length + count + offset];
            offset = (offset/2) - cLower;
            lower  = cLower + offset;
            upper  = cUpper + offset + count;
            index  = cIndex + offset;
        }
        if (firstHalf) {
            if (array != cArray) {
                System.arraycopy(cArray, cIndex, array, index+count, cUpper-cIndex);
            }
            System.arraycopy(cArray, cLower, array, lower, cIndex-cLower);
            if (array == cArray) {
                index -= count;
            }
        } else {
            if (array != cArray) {
                System.arraycopy(cArray, cLower, array, lower, cIndex-cLower);
            }
            System.arraycopy(cArray, cIndex, array, index+count, cUpper-cIndex);
        }
        /*
         * Maintenant que de la place a �t� cr��e, copie les nouvelles donn�es.
         * Durant la copie, on inversera l'ordre des points si �a avait �t� demand�.
         */
        if (reverse) {
            while ((count-=2) >= 0) {
                System.arraycopy(toMerge, mergeLower+count, array, index, 2);
                index += 2;
            }
        } else {
            System.arraycopy(toMerge, mergeLower, array, index, count);
        }
        return this;
    }

    /**
     * Inverse l'ordre des points dans le tableau <code>array</code> sp�cifi�
     * entre les index <code>lower</code> et <code>upper</code>.
     */
    private static void reverse(final float[] array, int lower, int upper) {
        assert (lower & 1) == 0;
        assert (upper & 1) == 0;
        while (lower < upper) {
            final float lowX=array[lower]; array[lower++]=array[upper-2];
            final float lowY=array[lower]; array[lower++]=array[--upper];
            array[  upper] = lowY;
            array[--upper] = lowX;
        }
    }

    /**
     * Renverse l'ordre de tous les points compris dans ce tableau.
     *
     * @return <code>this</code> si l'invertion � pu �tre faite sur-place,
     *         ou un autre tableau si �a n'a pas �t� possible.
     */
    public PointArray reverse() {
        reverse(array, lower, upper);
        return this;
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
        if (compress && count()>=8) {
            return new CompressedArray(array, lower, upper);
        }
        PointArray points = getInstance(array, lower, upper);
        if (points != null) {
            points = points.getFinal(compress);
        }
        return points;
    }

    /**
     * V�rifie le bon fonctionnement de cette classe. Cette m�thode peut �tre appel�e
     * sans argument.  Les assertions doivent �tre activ�es (option <code>-ea</code>)
     * pour que la v�rification soit effective. Cette m�thode peut aussi �tre ex�cut�e
     * sans les assertions pour tester les performances.
     */
    public static void main(final String[] args) {
        float[] checkPoints = new float[2];
        PointArray   points = getInstance(checkPoints);
        for (int i=0; i<2000; i++) {
            /*
             * Construit un tableau (de longueur paire) qui sera ins�r� au milieu
             * des points d�j� existants. Ce tableau sera remplis avec des nombres
             * al�atoires pour distinguer les diff�rentes coordonn�es.
             */
            final float[] toMerge = new float[(int)(32*Math.random()) & ~1];
            for (int j=0; j<toMerge.length; j++) {
                toMerge[j] = (float) (2000*Math.random()-1000);
            }
            /*
             * S�lectionne au hasard une plage de valeurs � prendre en compte dans
             * le tableau <code>toMerge</code>. Les autres valeurs seront abandonn�es.
             * Choisit aussi au hasard un point d'insertions pour les nouveaux points.
             */
            final int lower = (int)(toMerge.length*(0.49*Math.random()    )) & ~1;
            final int upper = (int)(toMerge.length*(0.49*Math.random()+0.5)) & ~1;
            final int index = (int)(checkPoints.length  *Math.random()     ) & ~1;
            /*
             * Ins�re les nouveaux points dans le tableau 'checkPoints'. Ce tableau est
             * maintenu uniquement � des fins de comparaisons avec le "vrai" tableau de
             * points. V�rifie si les deux tableaux ont un contenu identique.
             */
            final boolean reverse = (Math.random() > 0.5);
            points = points.insertAt(index/2, toMerge, lower, upper, reverse);
            if (reverse) reverse(toMerge, lower, upper);
            assert (checkPoints=XArray.insert(toMerge, lower, checkPoints, index, upper-lower)).length == 2*points.count();
            assert Arrays.equals(points.toArray(), checkPoints);
        }
        /*
         * Pr�pare une liste d'index d�limitant des plages de points �
         * l'int�rieur du tableau. Ces plages seront extraites plusieurs
         * fois sur diff�rentes versions du tableau de points.
         */
        final int index[]=new int[8];
        for (int i=0; i<index.length; i++) {
            index[i] = (int)(Math.random()*points.count());
        }
        Arrays.sort(index);
        /*
         * Teste maintenant l'extraction d'un sous-tableau
         * ainsi que la compression des donn�es.
         */
        for (int i=0; i<=3; i++) {
            switch (i) {
                case 0: {
                    break;
                }
                case 1: {
                    points = points.getFinal(false);
                    break;
                }
                case 2: {
                    checkPoints=points.toArray();
                    Arrays.sort(checkPoints);
                    points=getInstance(checkPoints);
                    break;
                }
                case 3: {
                    points = points.getFinal(true);
                    break;
                }
            }
            System.out.println(points);
            for (int j=0; j<index.length; j++) {
                System.out.print("    [");
                System.out.print(index[j]);
                System.out.print("..");
                System.out.print(index[j+1]);
                System.out.print("]: ");
                System.out.println(points.subarray(index[j], index[++j]));
            }
            System.out.println();
        }
        /*
         * Recherche l'�cart maximal entre les
         * donn�es compress�es et les donn�es r�elles.
         */
        float dx=0, dy=0;
        final PointIterator iterator=points.iterator(0);
        for (int i=0; i<checkPoints.length;) {
            assert iterator.hasNext();
            final float dxi = Math.abs(iterator.nextX()-checkPoints[i++]);
            final float dyi = Math.abs(iterator.nextY()-checkPoints[i++]);
            if (dxi > dx) dx=dxi;
            if (dyi > dy) dy=dyi;
        }
        System.out.print(dx);
        System.out.print(", ");
        System.out.println(dy);
    }
}
