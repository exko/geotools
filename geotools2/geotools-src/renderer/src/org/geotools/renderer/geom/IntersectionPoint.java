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

// J2SE dependencies
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

// Geotools dependencies
import org.geotools.units.Unit;
import org.geotools.pt.CoordinatePoint;
import org.geotools.cs.CoordinateSystem;
import org.geotools.ct.TransformException;
import org.geotools.cs.GeographicCoordinateSystem;
import org.geotools.ct.CoordinateTransformationFactory;


/**
 * Coordonn�e associ�e � une intersection entre deux lignes. Cette classe est r�serv�e � un usage
 * interne afin de d�terminer de quelle fa�on on doit refermer les formes g�om�triques des �les et
 * des continents. Le point m�moris� par cette classe proviendra de l'intersection de deux lignes:
 * un des bords de la carte (g�n�ralement un des 4 c�t�s d'un rectangle, mais �a pourrait �tre une
 * autre forme g�om�trique) avec une ligne passant par les deux premiers ou les deux derniers points
 * du tra�t de c�te. Appellons la premi�re ligne (celle du bord de la carte) "<code>line</code>".
 * Cette classe m�morisera au passage le produit scalaire entre un vecteur passant le premier point
 * de <code>line</code> et le point d'intersection avec un vecteur passant par le premier et dernier
 * point de <code>line</code>. Ce produit scalaire peut �tre vu comme une sorte de mesure de la
 * distance entre le d�but de <code>line</code> et le point d'intersection.
 *
 * @version $Id: IntersectionPoint.java,v 1.1 2003/02/04 12:30:52 desruisseaux Exp $
 * @author Martin Desruisseaux
 */
final class IntersectionPoint extends Point2D.Double implements Comparable {
    /**
     * Num�ro du bord sur lequel a �t� trouv�e ce point. Cette information est laiss�
     * � la discr�tion du programmeur, qui y mettra les informations qu'il souhaite.
     * Ce num�ro peut �tre utile pour aider � retrouver plus tard la ligne sur laquelle
     * fut trouv�e le point d'intersection.
     */
    int border;

    /**
     * Distance au carr� entre le point d'intersection et le point qui en �tait le plus
     * proche. Cette information n'est pas utilis�e par cette classe, sauf dans la m�thode
     * {@link #toString}. Elle utile � des fins de d�boguage, mais aussi pour choisir quel
     * point d'intersection supprim� s'il y en a trop. On supprimera le point qui se trouve
     * le plus loin de sa bordure.
     */
    double minDistanceSq = java.lang.Double.NaN;

    /**
     * Produit scalaire entre la ligne sur laquelle fut trouv�e le point d'intersection
     * et un vecteur allant du d�but de cette ligne jusqu'au point d'intersection.
     */
    double scalarProduct;

    /**
     * Segment de tra�t de c�te auquel appartient la ligne avec
     * laquelle on a calcul� un point d'intersection.
     */
    Polygon path;

    /**
     * Indique si le point d'intersection fut calcul� � partir des deux premiers ou deux derniers
     * points du tra�t de c�te. Si <code>append</code> a la valeur <code>true</code>, cela
     * signifiera que l'intersection fut calcul�e � partir des deux derniers points du trait
     * de c�te. Refermer la forme g�om�trique de l'�le ou du continent impliquera donc que l'on
     * ajoute des points � la fin du trait de c�te ("append"), en opposition au fait d'ajouter
     * des points au d�but du trait de c�te ("prepend").
     */
    boolean append;

    /**
     * Syst�me de coordonn�es de ce point. Cette information n'est utilis�e que par la m�thode
     * {@link #toString}, afin de pouvoir �crire une coordonn�es en latitudes et longitudes.
     */
    CoordinateSystem coordinateSystem;

    /**
     * Construit un point initialis�
     * � la position (0,0).
     */
    public IntersectionPoint() {
    }

    /**
     * Construit un point initialis�
     * � la position sp�cifi�e.
     */
    public IntersectionPoint(final Point2D point) {
        super(point.getX(), point.getY());
    }

    /**
     * M�morise dans cet objet la position du point sp�cifi�. Le produit scalaire
     * de ce point avec la ligne <code>line</code> sera aussi calcul� et plac� dans
     * le champs {@link #scalarProduct}.
     *
     * @param point  Coordonn�es de l'intersection.
     * @param line   Coordonn�es de la ligne sur laquelle l'intersection <code>point</code> fut
     *               trouv�e.
     * @param border Num�ro de la ligne <code>line</code>. Cette information sera m�moris�e dans
     *               le champs {@link #border} et est laiss�e � la discretion du programmeur.
     *               Il est sugg�r� d'utiliser un num�ro unique pour chaque ligne <code>line</code>,
     *               et qui croissent dans le m�me ordre que les lignes <code>line</code> sont
     *               balay�es.
     */
    final void setLocation(final Point2D point, final Line2D.Double line, final int border) {
        super.setLocation(point);
        final double dx = line.x2-line.x1;
        final double dy = line.y2-line.y1;
        scalarProduct = ((x-line.x1)*dx+(y-line.y1)*dy) / Math.sqrt(dx*dx + dy*dy);
        this.border = border;
    }

    /**
     * Compare ce point avec un autre. Cette comparaison n'implique que
     * la position de ces points sur un certain segment. Elle permettra
     * de classer les points dans l'ordre des aiguilles d'une montre, ou
     * dans l'ordre inverse selon la fa�on dont {@link PathIterator} est
     * impl�ment�e.
     *
     * @param o Autre point d'intersection avec lequel comparer celui-ci.
     * @return -1, 0 ou +1 selon que ce point pr�c�de, �gale ou suit le
     *         point <code>o</code> dans un certain sens (g�n�ralement le
     *         sens des aiguilles d'une montre).
     */
    public int compareTo(final IntersectionPoint pt) {
        if (border < pt.border) return -1;
        if (border > pt.border) return +1;
        if (scalarProduct < pt.scalarProduct) return -1;
        if (scalarProduct > pt.scalarProduct) return +1;
        return 0;
    }

    /**
     * Compare ce point avec un autre. Cette comparaison n'implique que
     * la position de ces points sur un certain segment. Elle permettra
     * de classer les points dans l'ordre des aiguilles d'une montre, ou
     * dans l'ordre inverse selon la fa�on dont {@link PathIterator} est
     * impl�ment�e.
     *
     * @param o Autre point d'intersection avec lequel comparer celui-ci.
     * @return -1, 0 ou +1 selon que ce point pr�c�de, �gale ou suit le
     *         point <code>o</code> dans un certain sens (g�n�ralement le
     *         sens des aiguilles d'une montre).
     */
    public int compareTo(Object o) {
        return compareTo((IntersectionPoint) o);
    }

    /**
     * Indique si ce point d'intersection est identique au point <code>o</code>.
     * Cette m�thode est d�finie pour �tre coh�rente avec {@link #compareTo}, mais
     * n'est pas utilis�e.
     *
     * @return <code>true</code> si ce point d'intersection est le m�me que <code>o</code>.
     */
    public boolean equals(final Object o) {
        if (o instanceof IntersectionPoint) {
            return compareTo((IntersectionPoint) o) == 0;
        } else {
            return false;
        }
    }

    /**
     * Retourne un code � peu pr�s unique pour ce point d'intersection,
     * bas� sur le produit scalaire et le num�ro de la ligne. Ce code
     * sera coh�rent avec la m�thode {@link #equals}.
     *
     * @return Un num�ro � peu pr�s unique pour ce point d'intersection.
     */
    public int hashCode() {
        final long bits = java.lang.Double.doubleToLongBits(scalarProduct);
        return border ^ (int)bits ^ (int)(bits >>> 32);
    }

    /**
     * Renvoie une repr�sentation sous forme de cha�ne de caract�res
     * de ce point d'intersection (� des fins de d�boguage seulement).
     *
     * @return Cha�ne de caract�res repr�sentant ce point d'intersection.
     */
    public String toString() {
        final CoordinateSystem WGS84 = GeographicCoordinateSystem.WGS84;
        final StringBuffer buffer = new StringBuffer("IntersectionPoint[");
        if (coordinateSystem != null) {
            try {
                CoordinatePoint coord = new CoordinatePoint(this);
                coord = CoordinateTransformationFactory.getDefault()
                                            .createFromCoordinateSystems(coordinateSystem, WGS84)
                                            .getMathTransform().transform(coord, coord);
                buffer.append(coord);
            } catch (TransformException exception) {
                buffer.append("error");
            }
        } else {
            buffer.append((float) x);
            buffer.append(' ');
            buffer.append((float) y);
        }
        buffer.append(']');
        if (!java.lang.Double.isNaN(minDistanceSq)) {
            buffer.append(" at ");
            buffer.append((float) Math.sqrt(minDistanceSq));
        }
        if (coordinateSystem != null) {
            buffer.append(' ');
            buffer.append(coordinateSystem.getUnits(0));
        }
        buffer.append(" from #");
        buffer.append(border);
        buffer.append(" (");
        buffer.append((float) scalarProduct);
        buffer.append(')');
        return buffer.toString();
    }
}
