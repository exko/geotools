/*
 * Geotools 2 - OpenSource mapping toolkit
 * (C) 2003, Geotools Project Managment Committee (PMC)
 * (C) 2001, Institut de Recherche pour le D�veloppement
 * (C) 1999, Fisheries and Oceans Canada
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
package org.geotools.ct.proj;

// Geotools dependencies
import org.geotools.cs.Projection;
import org.geotools.ct.MissingParameterException;


/**
 * Classe de base des projections cartographiques coniques. Les projections
 * coniques consistent � projeter la surface de la Terre sur un c�ne tangeant ou s�cant � la
 * Terre. Les parall�les apparaissent habituellement comme des arcs de cercles et les m�ridiens
 * comme des lignes droites. Les projections coniques ne sont pas tr�s utilis�s du fait que
 * leurs distorsions augmentent rapidement � mesure que l'on s'�loigne des parall�les standards.
 * Elles sont plut�t utilis�es pour les r�gions aux latitudes moyennes qui s'�tendent sur une
 * large r�gion d'est en ouest, comme les Etats-Unis.
 *
 * <p>&nbsp;</p>
 * <p align="center"><img src="../doc-files/ConicProjection.png"></p>
 *
 * @version $Id: ConicProjection.java,v 1.3 2003/05/13 10:58:48 desruisseaux Exp $
 * @author Martin Desruisseaux
 *
 * @see <A HREF="http://mathworld.wolfram.com/ConicProjection.html">Conic projection on MathWorld</A>
 */
public abstract class ConicProjection extends MapProjection {
    /**
     * Construct a new map projection from the suplied parameters.
     *
     * @param  parameters The parameter values in standard units.
     * @throws MissingParameterException if a mandatory parameter is missing.
     */
    protected ConicProjection(final Projection parameters) throws MissingParameterException {
        super(parameters);
    }
}
