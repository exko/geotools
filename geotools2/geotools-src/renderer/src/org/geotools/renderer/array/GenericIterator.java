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

// J2SE dependencies
import java.awt.Point;
import java.awt.geom.Point2D;


/**
 * It�rateur balayant les donn�es d'un tableau {@link GenericArray}.
 *
 * @version $Id: GenericIterator.java,v 1.1 2003/05/23 17:58:59 desruisseaux Exp $
 * @author Martin Desruisseaux
 */
final class GenericIterator extends PointIterator {
    /**
     * The <var>x</var> and <var>y</var> vectors.
     */
    private final GenericArray.Vector x,y;

    /**
     * Index suivant celui de la derni�re donn�e � balayer.
     */
    private final int upper;

    /**
     * Index de la prochaine donn�e � retourner.
     */
    private int index;

    /**
     * The data type for {@link #next}.
     */
    private final int type;

    /**
     * Construit un it�rateur qui balaiera la plage sp�cifi�e d'un tableau de donn�es.
     */
    public GenericIterator(GenericArray.Vector x, GenericArray.Vector y, int lower, int upper) {
        this.x     = x;
        this.y     = y;
        this.index = lower;
        this.upper = upper;
        this.type  = Math.max(x.type(), y.type());
    }

    /**
     * Indique si les m�thodes {@link #next} peuvent retourner d'autres donn�es.
     */
    public boolean hasNext() {
        return index < upper;
    }

    /**
     * Retourne la valeur de la longitude courante.
     */
    public float nextX() {
        return x.getAsFloat(index);
    }

    /**
     * Retourne la valeur de la latitude courante, puis avance au point suivant.
     */
    public float nextY() {
        return y.getAsFloat(index++);
    }

    /**
     * Retourne la valeur du point courant dans un objet {@link Point2D},
     * puis avance au point suivant. Cette m�thode combine un appel de
     * {@link #nextX} suivit de {@link #nextY}.
     */
    public Object next() {
        switch (type) {
            case 0:  return new Point         (x.getAsInteger(index), y.getAsInteger(index++));
            case 1:  return new Point2D.Float (x.getAsFloat  (index), y.getAsFloat  (index++));
            default: return new Point2D.Double(x.getAsDouble (index), y.getAsDouble (index++));
        }
    }
}
