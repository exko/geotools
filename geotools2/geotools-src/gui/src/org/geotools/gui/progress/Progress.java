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
package org.geotools.gui.progress;

// Miscellaneous
import javax.swing.ProgressMonitor; // For JavaDoc
import org.geotools.resources.Utilities;


/**
 * Affiche l'�tat d'avancement d'une longue op�ration, ainsi que d'�ventuels avertissements.
 * La classe <code>Progress</code> ne suppose pas que l'�tat d'avancement sera report� dans
 * une fen�tre. Par exemple {@link PrintProgress} reporte l'�tat d'avancement sur le p�riph�rique
 * de sortie standard, ce qui est pratique pour les programmes ex�cut�s sur la ligne de commandes.
 * Exemple: le code suivant pourrait �tre utilis� pour informer des progr�s de la lecture d'un
 * fichier de 1000 lignes:
 *
 * <blockquote><pre>
 * &nbsp;Progress p = new {@link PrintProgress}();
 * &nbsp;p.setDecription("Loading data");
 * &nbsp;p.start();
 * &nbsp;for (int j=0; j&lt;1000; j++) {
 * &nbsp;    // ... some process...
 * &nbsp;    if ((j &amp; 255) == 0)
 * &nbsp;        p.progress(j*0.1f);
 * &nbsp;}
 * &nbsp;p.complete();
 * </pre></blockquote>
 *
 * <strong>Note:</strong>
 *       La ligne <code>if ((j&nbsp;&amp;&nbsp;255)&nbsp;==&nbsp;0)</code> est utilis�e pour
 *       n'appeller la m�thode {@link #progress} qu'une fois toutes les 256 lignes.
 *       Ce n'est pas obligatoire, mais se traduit souvent par une augmentation sensible
 *       de la vitesse de traitement.
 *
 * <p>Toutes les classes d�riv�es de {@link Progress} sont s�curitaires dans un environnement
 * multi-threads, et peuvent �tre ex�cut�es dans n'importe quel thread. Ca implique en particulier
 * qu'elles peuvent �tre ex�cut�es dans un thread autre que celui de <i>Swing</i>,  m�me si les
 * progr�s doivent �tre report�s dans une fen�tre de <i>Swing</i>.  Ex�cuter un long calcul dans
 * un thread s�par� est en g�n�ral une pratique recommand�e.</p>
 *
 * @version $Id: Progress.java,v 1.1 2003/02/03 14:51:04 desruisseaux Exp $
 * @author Martin Desruisseaux
 */
public abstract class Progress {
    /**
     * Construit un objet qui repr�sentera
     * les progr�s d'une longue op�ration.
     */
    public Progress() {
    }

    /**
     * Retourne le message d'�crivant l'op�ration
     * en cours. Si aucun message n'a �t� d�finie,
     * retourne <code>null</code>.
     */
    public abstract String getDescription();

    /**
     * Sp�cifie un message qui d�crit l'op�ration en cours.
     * Ce message est typiquement sp�cifi�e avant le d�but
     * de l'op�ration. Toutefois, cette m�thode peut aussi
     * �tre appel�e � tout moment pendant l'op�ration sans
     * que cela affecte le pourcentage accompli. La valeur
     * <code>null</code> signifie qu'on ne souhaite plus
     * afficher de description.
     */
    public abstract void setDescription(final String description);

    /**
     * Indique que l'op�ration a commenc�e.
     */
    public abstract void started();

    /**
     * Indique l'�tat d'avancement de l'op�ration. Le progr�s est repr�sent� par un
     * pourcentage variant de 0 � 100 inclusivement. Si la valeur sp�cifi�e est en
     * dehors de ces limites, elle sera automatiquement ramen�e entre 0 et 100.
     */
    public abstract void progress(final float percent);

    /**
     * Indique que l'op�ration est termin�e. L'indicateur visuel informant des
     * progr�s sera ramen� � 100% ou dispara�tra, selon l'impl�mentation de la
     * classe d�riv�e. Si des messages d'erreurs ou d'avertissements �taient
     * en attente, ils seront �crits.
     */
    public abstract void complete();

    /**
     * Lib�re les ressources utilis�es par l'�tat d'avancement. Si l'�tat
     * d'avancement �tait affich�e dans une fen�tre, cette fen�tre peut �tre
     * d�truite. L'impl�mentation par d�faut ne fait rien.
     */
    public void dispose() {
    }

    /**
     * Envoie un message d'avertissement. Ce message pourra �tre envoy� vers le
     * p�riph�rique d'erreur standard, appara�tre dans une fen�tre ou �tre tout
     * simplement ignor�.
     *
     * @param source Cha�ne de caract�re d�crivant la source de l'avertissement.
     *        Il s'agira par exemple du nom du fichier dans lequel une anomalie
     *        a �t� d�tect�e. Peut �tre nul si la source n'est pas connue.
     * @param margin Texte � placer dans la marge de l'avertissement <code>warning</code>,
     *        ou <code>null</code> s'il n'y en a pas. Il s'agira le plus souvent du num�ro
     *        de ligne o� s'est produite l'erreur dans le fichier <code>source</code>.
     * @param warning Message d'avertissement � �crire.
     */
    public abstract void warningOccurred(String source, String margin, String warning);

    /**
     * Indique qu'une exception est survenue pendant le traitement de l'op�ration.
     * Cette m�thode peut afficher la trace de l'exception dans une fen�tre ou �
     * la console, d�pendemment de la classe d�riv�e.
     */
    public abstract void exceptionOccurred(final Throwable exception);

    /**
     * Retourne la cha�ne <code>margin</code> sans les
     * �ventuelles parenth�ses qu'elle pourrait avoir
     * de part et d'autre.
     */
    static String trim(String margin) {
        margin = margin.trim();
        int lower = 0;
        int upper = margin.length();
        while (lower<upper && margin.charAt(lower+0)=='(') lower++;
        while (lower<upper && margin.charAt(upper-1)==')') upper--;
        return margin.substring(lower, upper);
    }

    /**
     * Returns a string representation for this object.
     */
    public synchronized String toString() {
        return Utilities.getShortClassName(this)+'['+getDescription()+']';
    }
}
