/*
 * Geotools - OpenSource mapping toolkit
 * (C) 2003, Institut de Recherche pour le D�veloppement
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


/**
 * R�f�rence vers un trait de c�te {@link Polygon}. Cette classe contient aussi un valeur
 * bool�enne qui sera prise en compte par la m�thode {@link #hashCode}. Cette valeur bool�enne
 * agit comme le spin d'un �lectron. Deux instances de <code>Fermion</code> peuvent r�f�rer au
 * m�me segment {@link Polygon} s'ils n'ont pas la m�me valeur bool�enne ("spin"). Cette classe
 * est r�serv�e � un usage interne par {@link PolygonAssembler}.
 *
 * @see FermionPair
 *
 * @version $Id: Fermion.java,v 1.1 2003/02/04 12:30:52 desruisseaux Exp $
 * @author Martin Desruisseaux
 */
final class Fermion {
    /**
     * R�f�rence vers le trait de c�te repr�sent� par cet objet. Dans l'analogie
     * avec la m�canique quantique, �a serait le "niveau atomique" d'un Fermion.
     */
    Polygon path;

    /**
     * Si <code>true</code>, la fin de du trait de c�te <code>path</code> devra �tre fusionn�
     * avec un autre trait (inconnu de cet objet). Si <code>false</code>, c'est le d�but du trait
     * <code>path</code> qui devra �tre fusionn�. Dans l'analogie avec la m�canique quantique,
     * c'est le "spin" d'un Fermion.
     */
    boolean mergeEnd;

    /**
     * Indique si deux cl�s sont identiques. Deux cl�s sont consid�r�s identiques si elles
     * se r�f�rent au m�me trait de c�te {@link #path} avec la m�me valeur bool�enne
     * {@link #mergeEnd}.
     */
    public boolean equals(final Object o) {
        if (o instanceof Fermion) {
            final Fermion k=(Fermion) o;
            return k.path==path && k.mergeEnd==mergeEnd;
        } else {
            return false;
        }
    }

    /**
     * Retourne une valeur � peu pr�s unique pour cet objet. Cette valeur sera b�tie �
     * partir de la r�f�rence {@link #path} et de la valeur bool�enne {@link #mergeEnd}.
     */
    public int hashCode() {
        final int code = System.identityHashCode(path);
        return mergeEnd ? code : ~code;
    }

    /**
     * Renvoie une repr�sentation sous forme de cha�ne de caract�res de cet objet.
     * Cette repr�sentation sera de la forme "Fermion[52 pts]".
     */
    public String toString() {
        final StringBuffer buffer = new StringBuffer("Fermion[");
        if (path != null) {
            buffer.append(path.getPointCount());
            buffer.append(" pts");
        }
        buffer.append(']');
        return buffer.toString();
    }
}
