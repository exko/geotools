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
package org.geotools.renderer.j2d;

// J2SE dependencies
import java.awt.geom.Point2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

// Geotools dependencies
import org.geotools.cs.CoordinateSystem;
import org.geotools.ct.MathTransform2D;
import org.geotools.ct.TransformException;
import org.geotools.ct.CoordinateTransformation;
import org.geotools.ct.CannotCreateTransformException;
import org.geotools.ct.NoninvertibleTransformException;
import org.geotools.resources.CTSUtilities;


/**
 * An event which indicates that a mouse action occurred in a map component. This event can
 * convert mouse position in geographic coordinates. All {@link MouseListener}s registered
 * to {@link MapPanel} will automatically receive events of this class.
 * Listeners implementations can implements their code as below:
 *
 * <blockquote><pre>
 * &nbsp;public void mouseClicked(MouseEvent e) {
 * &nbsp;    GeoMouseEvent event = (GeoMouseEvent) e;
 * &nbsp;    // Process event here...
 * &nbsp;}
 * </pre></blockquote>
 *
 * @version $Id: GeoMouseEvent.java,v 1.1 2003/01/20 00:06:34 desruisseaux Exp $
 * @author Martin Desruisseaux
 */
public class GeoMouseEvent extends MouseEvent {
    /**
     * Num�ro de version pour compatibilit� avec des versions pr�c�dentes.
     */
//    private static final long serialVersionUID = -6195580751022572130L;

    /**
     * Carte qui a construit cet �v�nement.
     */
//    private final MapPanel mapPanel;

    /**
     * The transform most likely to be used by this event. This transform must be equals
     * to <code>MapPanel.commonestTransform.inverse()</code>, or be <code>null</code> if
     * not yet computed. The <code>getSourceCS()</code> MUST be the map panel's coordinate
     * system (i.e. the coordinate system used for rendering). The <code>getTargetCS()</code>
     * is arbitrary, but performance will be better if it is set to the commonest coordinate
     * system used by layers.
     */
    private CoordinateTransformation inverseTransform;

    /**
     * Coordonn�es <var>x</var> et <var>y</var> projet�es dans le syst�me
     * <code>inverseTransform.getTargetCS()</code>.  Ces champs ne sont
     * valides que si {@link #projected} est <code>true</code>.
     */
    private transient double px, py;

    /**
     * Indique si les coordonn�es {@link #px} et {@link #py}
     * sont valides. Ces coordonn�es ne seront calcul�es que
     * la premi�re fois o� elle seront demand�es.
     */
    private transient boolean projected;

    /**
     * Construit un �v�nements qui utilisera les m�mes param�tres que <code>event</code>.
     * Les coordonn�es en pixels pourront �tre converties en coordonn�es g�ographiques en
     * utilisant les param�tres de l'objet {@link MapPanel} sp�cifi�.
     *
     * @param event Ev�nement original.
     * @param mapPanel Carte ayant produit cet �v�nement.
     */
    public GeoMouseEvent(final MouseEvent event /*, final MapPanel mapPanel*/) {
        super(event.getComponent(),    // the Component that originated the event
              event.getID(),           // the integer that identifies the event
              event.getWhen(),         // a long int that gives the time the event occurred
              event.getModifiers(),    // the modifier keys down during event (shift, ctrl, alt, meta)
              event.getX(),            // the horizontal x coordinate for the mouse location
              event.getY(),            // the vertical y coordinate for the mouse location
              event.getClickCount(),   // the number of mouse clicks associated with event
              event.isPopupTrigger(),  // a boolean, true if this event is a trigger for a popup-menu
              event.getButton());      // which of the mouse buttons has changed state (JDK 1.4 only).
//        this.mapPanel = mapPanel;
    }

    /**
     * Retourne les coordonn�es pixel de cet �v�nement. Cette m�thode est � peu
     * pr�s �quivalente � {@link #getPoint}, mais tiendra compte de la pr�sence
     * de la loupe si elle est affich�e.
     *
     * @param  dest Un point pr�-alou� dans lequel m�moriser le r�sultat,
     *         ou <code>null</code> s'il n'y en a pas.
     * @return La coordonn�e pixel. Si <code>dest</code> �tait non-nul,
     *         alors il sera utilis� et retourn�.
     */
//    public Point2D getPoint2D(Point2D dest) {
//        if (dest!=null) dest.setLocation(getX(), getY());
//        else dest=new Point2D.Double(getX(), getY());
//        mapPanel.correctPointForMagnifier(dest);
//        return dest;
//    }

    /**
     * Retourne les coordonn�es logiques de l'endroit ou s'est produit l'�v�nement.
     * Les coordonn�es seront exprim�es selon le syst�me de coordonn�es de l'afficheur
     * {@link MapPanel}. Si la coordonn�e n'a pas pu �tre d�termin�e, alors
     * cette m�thode retourne <code>null</code>.
     *
     * @param  dest Un point pr�-alou� dans lequel m�moriser le r�sultat, ou <code>null</code>
     *         s'il n'y en a pas.
     * @return La coordonn�e logique, ou <code>null</code> si elle n'a pas pu �tre calcul�e.
     *         Si la coordonn�e a pu �tre obtenue et que <code>dest</code> est non-nul, alors
     *         une nouveau point sera automatiquement cr�� et retourn�.
     */
//    public Point2D getVisualCoordinate(final Point2D dest) {
//        return mapPanel.inverseTransform(getPoint2D(dest));
//    }

    /**
     * Retourne les coordonn�es logiques de l'endroit o� s'est produit l'�v�nement.
     * Les coordonn�es seront exprim�es selon le syst�me de coordonn�es sp�cifi�.
     * Si la coordonn�e n'a pas pu �tre d�termin�e, alors cette m�thode retourne
     * <code>null</code>.
     *
     * @param  system Le syst�me de coordonn�es selon lequel on veut exprimer la coordonn�e.
     * @param  dest Un point pr�-alou� dans lequel m�moriser le r�sultat, ou <code>null</code>
     *         s'il n'y en a pas.
     * @return La coordonn�e logique, ou <code>null</code> si elle n'a pas pu �tre calcul�e.
     *         Si la coordonn�e a pu �tre obtenue et que <code>dest</code> est non-nul, alors
     *         une nouveau point sera automatiquement cr�� et retourn�.
     */
//    public Point2D getCoordinate(CoordinateSystem system, Point2D dest) {
//        system = CTSUtilities.getCoordinateSystem2D(system);
//        try {
//            if (inverseTransform==null) {
//                inverseTransform = mapPanel.getCommonestTransformation("GeoMouseEvent", "getCoordinate").inverse();
//            }
//            /*
//             * Si le syst�me de coordonn�es sp�cifi� n'est pas
//             * celui que l'on attendait, calcule � la vol� les
//             * coordonn�es.
//             */
//            if (!system.equals(inverseTransform.getTargetCS(), false)) {
//                dest=getVisualCoordinate(dest);
//                if (dest == null) {
//                    return null;
//                }
//                final CoordinateTransformation tr;
//                tr = Contour.getCoordinateTransformation(mapPanel.getCoordinateSystem(), system);
//                return ((MathTransform2D) tr.getMathTransform()).transform(dest, dest);
//            }
//            /*
//             * Si le syst�me de coordonn�es est bien celui que l'on attendait
//             * et qu'on avait d�j� calcul� les coordonn�es pr�c�demment, utilise
//             * le r�sultat qui avait �t� conserv� dans la cache.
//             */
//            if (projected) {
//                if (dest != null) {
//                    dest.setLocation(px,py);
//                    return dest;
//                }
//                return new Point2D.Double(px,py);
//            }
//            /*
//             * Calcule les coordonn�es et conserve le r�sultat dans
//             * une cache interne pour les interogations subs�quentes.
//             */
//            dest=getVisualCoordinate(dest);
//            if (dest != null) {
//                dest = ((MathTransform2D) inverseTransform.getMathTransform()).transform(dest, dest);
//                px=dest.getX();
//                py=dest.getY();
//                projected=true;
//            }
//            return dest;
//        } catch (TransformException exception) {
//            /*
//             * Si une projection cartographique a �chou�e, reporte
//             * l'erreur � l'objet {@link MapPanel} qui avait lanc�
//             * cet �v�nement.
//             */
//            mapPanel.handleException("GeoMouseEvent", "getCoordinate", exception);
//        }
//        return null;
//    }

    /**
     * Retourne une transformation qui convertira les coordonn�es du syst�me de l'affichage
     * vers le syst�me <code>cached.{@link #getTargetCS() getTargetCS()}</code>. Si la
     * transformation <code>cached</code> convient d�j�, elle sera retourn�e plut�t que
     * de cr�er une nouvelle transformation.
     */
//    final CoordinateTransformation getTransformToTarget(final CoordinateTransformation cached) throws CannotCreateTransformException
//    {
//        final CoordinateSystem sourceCS = mapPanel.getCoordinateSystem();
//        if (sourceCS.equals(cached.getSourceCS(), false)) {
//            return cached;
//        }
//        final CoordinateSystem targetCS = cached.getTargetCS();
//        try {
//            if (inverseTransform == null) {
//                inverseTransform = mapPanel.getCommonestTransformation("MouseCoordinateFormat", "format").inverse();
//            }
//            assert sourceCS.equals(inverseTransform.getSourceCS(), false);
//            if (targetCS.equals(inverseTransform.getTargetCS(), false)) {
//                return inverseTransform;
//            }
//        } catch (NoninvertibleTransformException exception) {
//            // This method is actually invoked by MouseCoordinateFormat only.
//            mapPanel.handleException("MouseCoordinateFormat", "format", exception);
//        }
//        return Contour.getCoordinateTransformation(sourceCS, targetCS, "MouseCoordinateFormat", "format");
//    }
}
