/*
 * Geotools - OpenSource mapping toolkit
 * (C) 2003, Centre for Computational Geography
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
import java.awt.AWTEvent;
import javax.swing.Action;


/**
 * Actions that may be trigged by some mouse of keyboard events. Each layer
 * ({@link RenderedLayer}) can have its own tool. When a mouse event occurs,
 * the tools of the topermost layer is queried first. If it didn't {@linkplain
 * AWTEvent#consume consumed} the event, then the next layer's tools is queried,
 * etc. If no {@link RenderedLayer} consumed the event, then the {@link Renderer}'s
 * tools is queried last.
 *
 * @version $Id: Tools.java,v 1.3 2003/01/26 22:30:40 desruisseaux Exp $
 * @author Martin Desruisseaux
 *
 * @see RenderedLayer
 * @see Renderer
 */
public class Tools {
    /**
     * Default constructor.
     */
    protected Tools() {
    }

    /**
     * Format a value for the current mouse position. This method doesn't have to format the
     * mouse coordinate (this is {@link MouseCoordinateFormat#format(GeoMouseEvent)} business).
     * Instead, it is invoked for formatting a value at current mouse position. For example a
     * remote sensing image of Sea Surface Temperature (SST) can format the temperature in
     * geophysical units (e.g. "12�C"). The default implementation do nothing and returns
     * <code>false</code>.
     *
     * @param  event The mouse event.
     * @param  toAppendTo The destination buffer for formatting a value.
     * @return <code>true</code> if this method has formatted a value, or <code>false</code>
     *         otherwise. If this method returns <code>true</code>, then the next layers (with
     *         smaller {@linkplain RenderedLayer#getZOrder z-order}) will not be queried.
     *
     * @see MouseCoordinateFormat#format(GeoMouseEvent)
     */
    protected boolean formatValue(final GeoMouseEvent event, final StringBuffer toAppendTo) {
        return false;
    }

    /**
     * Retourne le texte � afficher dans une bulle lorsque la souris tra�ne sur la couche.
     * L'impl�mentation par d�faut retourne toujours <code>null</code>, ce qui signifie que
     * cette couche n'a aucun texte � afficher (les autres couches seront alors interrog�es).
     * Les classes d�riv�es peuvent red�finir cette m�thode pour retourner un texte apr�s avoir
     * v�rifi� que les coordonn�es de <code>event</code> correspondent bien � un point de la
     * couche.
     *
     * @param  event Coordonn�es du curseur de la souris.
     * @return Le texte � afficher lorsque la souris tra�ne sur la couche.
     *         Ce texte peut �tre nul pour signifier qu'il ne faut rien �crire.
     *
     * @see Renderer#getToolTipText
     */
    protected String getToolTipText(final GeoMouseEvent event) {
        return null;
    }

    /**
     * Returns a contextual menu for the specified mouse event. On Windows and Solaris platforms,
     * this method is invoked when the user press the right button. Subclasses should verify if
     * the mouse point on a feature of this layer. If so and if there is a popup menu to display,
     * then this method returns the actions for this menu. Otherwise, returns <code>null</code>.
     * The default implementation returns always <code>null</code>.
     *
     * @param  event The mouse event.
     * @return Actions for the popup menus, or <code>null</code> if none or if the mouse isn't
     *         over a feature of this layer. If this array is non-null but contains null elements,
     *         then the null elements will be understood as menu separator.
     *
     * @see Renderer#getPopupMenu
     */
    protected Action[] getPopupMenu(final GeoMouseEvent event) {
        return null;
    }

    /**
     * M�thode appell�e chaque fois que le bouton de la souris a �t� cliqu� sur une couche qui
     * pourrait �tre <code>this</code>. L'impl�mentation par d�faut ne fait rien. Les classes
     * d�riv�es qui souhaite entrepredre une action doivent d'abord v�rifier si les coordonn�es
     * de <code>event</code> correspondent bien � un point de cette couche. Si oui, alors elles
     * doivent aussi appeler {@link GeoMouseEvent#consume} apr�s leur action, pour que le clic
     * de la souris ne soit pas transmis aux autres couches en-dessous de celle-ci.
     */
    protected void mouseClicked(final GeoMouseEvent event) {
    }
}
