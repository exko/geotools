/*
 * Geotools 2 - OpenSource mapping toolkit
 * (C) 2003, Geotools Project Managment Committee (PMC)
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
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

// Geotools dependencies
import org.geotools.util.ProgressListener;
import org.geotools.resources.Utilities;


/**
 * Classe priv�e utilis�e pour identifier les lacs � l'int�rieur d'une �le ou d'un continent.
 * Chaque noeud contient une r�f�rence vers un objet {@link Polygon} et une liste de r�f�rences
 * vers d'autres objets <code>PolygonInclusion</code> dont les polygones sont enti�rement compris
 * � l'int�rieur de celui de cet objet <code>PolygonInclusion</code>.
 *
 * @task TODO: Avec un peu plus de code, il serait possible de faire appara�tre ces objet comme
 *             un noeud dans une composante {@link javax.swing.JTree}. �a pourrait �tre tr�s
 *             int�ressant, car l'utilisateur pourrait voir quels lacs sont contenus sur un
 *             continent et quelles �les sont contenues dans un lac donn�e, de la m�me fa�on
 *             qu'on explore les dossiers d'un ordinateur.
 *
 * @version $Id: PolygonInclusion.java,v 1.2 2003/05/13 11:00:46 desruisseaux Exp $
 * @author Martin Desruisseaux
 */
final class PolygonInclusion {
    /**
     * Polygone associ� � cet objet.
     */
    private final Polygon polygon;

    /**
     * Liste des objets <code>PolygonInclusion</code> fils. Les polygons de chacun de
     * ces fils sera enti�rement compris dans le polygon {@link #polygon} de cet objet.
     */
    private List childs;

    /**
     * Construit un noeud qui enveloppera le polygone
     * sp�cifi�. Ce noeud n'aura aucune branche pour
     * l'instant.
     */
    private PolygonInclusion(Polygon polygon) {
        this.polygon = polygon;
    }

    /**
     * V�rifie si deux noeuds sont identiques.
     * Cette m�thode ne doit pas �tre red�finie.
     */
    public final boolean equals(final Object other) {
        return this == other;
    }

    /**
     * Retourne un code repr�sentant ce noeud.
     * Cette m�thode ne doit pas �tre red�finie.
     */
    public final int hashCode() {
        return System.identityHashCode(this);
    }

    /**
     * Ajoute � la liste des polygons-fils ({@link #childs}) tous les polygones de la liste
     * sp�cifi�e (<code>polygons</code>) qui sont enti�rement compris dans {@link #polygon}.
     * Chaque polygone ajout� � {@link #childs} sera retir� de <code>polygons</code>, de
     * sorte qu'apr�s l'appel de cette m�thode, <code>polygons</code> ne contiendra plus
     * que les polygones qui ne sont pas compris dans {@link #polygon}.
     */
    private void addChilds(final List polygons) {
        for (final Iterator it=polygons.iterator(); it.hasNext();) {
            final PolygonInclusion node = (PolygonInclusion) it.next();
            if (node!=this && polygon.contains(node.polygon))  {
                if (childs == null) {
                    childs = new LinkedList();
                }
                childs.add(node);
                it.remove();
            }
        }
        buildTree(childs, null);
    }

    /**
     * Apr�s avoir ajout� des polygones � la liste interne, on appel {@link #addChilds}
     * de fa�on r�cursive pour chacun des polygones de {@link #childs}. On obtiendra
     * ainsi une arborescence des polygones, chaque parent contenant enti�rement 0, 1
     * ou plusieurs enfants. Par exemple appellons "Continent" le polygone r�f�r� par
     * {@link #polygon}. Supposons que "Continent" contient enti�rement deux autres
     * polygones, "Lac" et "�le". Le code pr�c�dent avait ajout� "Lac" et "�le" � la
     * liste {@link #childs}. Maintenant on demandera � "Lac" d'examiner cette liste. Il
     * trouvera qu'il contient enti�rement "�le" et l'ajoutera � sa propre liste interne
     * apr�s l'avoir retir� de la liste {@link #child} de "Continent".
     */
    private static void buildTree(final List childs, final ProgressListener progress) {
        if (childs != null) {
            int count = 0;
            final Set alreadyProcessed = new HashSet(childs.size() + 64);
            for (Iterator it=childs.iterator(); it.hasNext();) {
                final PolygonInclusion node = (PolygonInclusion) it.next();
                if (alreadyProcessed.add(node)) {
                    if (progress != null) {
                        progress.progress(100f * (count++ / childs.size()));
                    }
                    node.addChilds(childs);
                    it = childs.iterator();
                }
            }
        }
    }

    /**
     * Examine tous les polygones sp�cifi�s et tente de
     * diff�rencier les �les des lacs. Les polygones de
     * la liste sp�cifi�e seront automatiquement class�s.
     */
    static void process(final Polygon[] polygons, final ProgressListener progress) {
        if (progress != null) {
            // TODO: localize...
            progress.setDescription("Searching lakes");
            progress.started();
        }
        final List childs = new LinkedList();
        for (int i=0; i<polygons.length; i++) {
            childs.add(new PolygonInclusion(polygons[i]));
        }
        buildTree(childs, progress);
        assert setType(childs, true, polygons, 0) == polygons.length;
    }

    /**
     * D�finie comme �tant une �le ou un lac tous les noeuds
     * apparaissant dans la liste sp�cifi�e. Les polygones-fils
     * des noeuds seront aussi d�finis avec le type oppos� (une
     * �le si le parent est un lac, et vis-versa). Au passage,
     * cette m�thode recopiera les r�f�rences dans le tableau
     * sp�cifi� en argument. Elles seront ainsi class�.
     *
     * @param childs Liste des noeuds � d�finir.
     * @param land <code>true</code> s'il faut les d�finir comme des �les,
     *        land <code>false</code> s'il faut les d�finir comme des lacs.
     * @param polygons Tableau dans lequel recopier les r�f�rences.
     * @param index Index � partir d'o� copier les r�f�rences.
     * @return Index suivant celui de la derni�re r�f�rence copi�e.
     */
    private static int setType(final List childs, final boolean land,
                               final Polygon[] polygons, int index)
    {
        if (childs != null) {
            /*
             * Commence par traiter tous les polygones-fils. On fait ceux-ci
             * en premier afin qu'ils apparaissent au d�but de la liste, avant
             * les polygones parents qui les contiennent.
             */
            for (final Iterator it=childs.iterator(); it.hasNext();) {
                final PolygonInclusion node = (PolygonInclusion) it.next();
                index = setType(node.childs, !land, polygons, index);
            }
            /*
             * Proc�de maintenant au traitement
             * des polygones de <code>childs</code>.
             */
            final InteriorType type = land ? InteriorType.ELEVATION : InteriorType.DEPRESSION;
            for (final Iterator it=childs.iterator(); it.hasNext();) {
                final PolygonInclusion node = (PolygonInclusion) it.next();
                if (node.polygon.getInteriorType() != null) {
                    node.polygon.close(type);
                }
                polygons[index++] = node.polygon;
            }
        }
        return index;
    }

    /**
     * Retourne une cha�ne de caract�res contenant le polygone
     * {@link #polygon} de ce noeud ainsi que de tous les noeuds-fils.
     */
    public String toString() {
        final StringBuffer buffer = new StringBuffer();
        toString(buffer, 0);
        return buffer.toString();
    }

    /**
     * Impl�mentation de la m�thode {@link #toString()}.
     * Cette m�thode s'appellera elle-m�me de fa�on recursive.
     */
    private void toString(StringBuffer buffer, int indentation) {
        buffer.append(Utilities.spaces(indentation));
        buffer.append(polygon);
        buffer.append('\n');
        if (childs != null) {
            for (final Iterator it=childs.iterator(); it.hasNext();) {
                ((PolygonInclusion) it.next()).toString(buffer, indentation+2);
            }
        }
    }
}

