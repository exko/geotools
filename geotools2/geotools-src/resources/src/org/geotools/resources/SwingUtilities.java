/*
 * Geotools - OpenSource mapping toolkit
 * (C) 2002, Centre for Computational Geography
 * (C) 2001, Institut de Recherche pour le D�veloppement
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
package org.geotools.resources;

// J2SE library
import java.awt.Color;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.EventQueue;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.LookAndFeel;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

// Geotools dependencies
import org.geotools.resources.rsc.Resources;
import org.geotools.resources.rsc.ResourceKeys;


/**
 * A collection of utility methods for Swing.  All <code>show*</code> methods delegate
 * their work to the corresponding method in {@link JOptionPane}, with two differences:
 *
 * <ul>
 *   <li><code>SwingUtilities</code>'s method may be invoked from any thread. If they
 *       are invoked from a non-Swing thread, execution will be delegate to the Swing
 *       thread and the calling thread will block until completion.</li>
 *   <li>If a parent component is a {@link JDesktopPane}, dialogs will be rendered as
 *       internal frames instead of frames.</li>
 * </ul>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class SwingUtilities {
    /**
     * Do not allow any instance
     * of this class to be created.
     */
    private SwingUtilities() {
    }

    /**
     * Insert a Swing component into a frame. The kind of frame depends on the owner:
     *
     * <ul>
     *   <li>If <code>owner</code> or one of its parent is a {@link JDesktopPane},
     *       then <code>panel</code> is added into a {@link JInternalFrame}.</li>
     *   <li>If <code>owner</code> or one of its parent is a {@link Frame} or a {@link Dialog},
     *       then <code>panel</code> is added into a {@link JDialog}.</li>
     *   <li>Otherwise, <code>panel</code> is added into a {@link JFrame}.</li>
     * </ul>
     *
     * @param  owner The frame's owner, or <code>null</code> if none.
     * @param  panel The panel to insert into a frame.
     * @param  title The frame's title.
     * @return The frame. This frame is not initially visible. The method
     *         <code>Component.setVisible(true)</code> must be invoked
     *         in order to show the frame.
     */
    public static Component toFrame(Component owner,
                                    final JComponent panel,
                                    final String     title)
    {
        while (owner != null) {
            if (owner == panel) {
                throw new IllegalArgumentException();
            }
            if (owner instanceof JDesktopPane) {
                final JInternalFrame frame = new JInternalFrame(title);
                ((JDesktopPane) owner).add(frame);
                frame.getContentPane().add(panel);
                frame.pack();
                return frame;
            }
            if (owner instanceof Frame) {
                final JDialog dialog = new JDialog((Frame) owner, title);
                dialog.getContentPane().add(panel);
                dialog.pack();
                return dialog;
            }
            if (owner instanceof Dialog) {
                final JDialog dialog = new JDialog((Dialog) owner, title);
                dialog.getContentPane().add(panel);
                dialog.pack();
                return dialog;
            }
            owner = owner.getParent();
        }
        //
        // Add the panel as a standalone window.
        // This window has its own button on the task bar.
        //
        final JFrame frame = new JFrame(title);
        frame.getContentPane().add(panel);
        frame.pack();
        return frame;
    }

    /**
     * Brings up a "Ok/Cancel" dialog with no icon. This method can be invoked
     * from any thread and blocks until the user click on "Ok" or "Cancel".
     *
     * @param  owner  The parent component. Dialog will apears on top of this owner.
     * @param  dialog The dialog content to show.
     * @param  title  The title string for the dialog.
     * @return <code>true</code> if user clicked "Ok", <code>false</code> otherwise.
     */
    public static boolean showOptionDialog(final Component owner,
                                           final Object   dialog,
                                           final String    title)
    {
        return showOptionDialog(owner, dialog, title, null);
    }

    /**
     * Brings up a "Ok/Cancel/Reset" dialog with no icon. This method can be invoked
     * from any thread and blocks until the user click on "Ok" or "Cancel".
     *
     * @param  owner  The parent component. Dialog will apears on top of this owner.
     * @param  dialog The dialog content to show.
     * @param  title  The title string for the dialog.
     * @param  reset  Action to execute when user press "Reset", or <code>null</code>
     *                if there is no "Reset" button. If <code>reset</code> is an
     *                instance of {@link Action}, the button label will be set
     *                according the action's properties.
     * @return <code>true</code> if user clicked "Ok", <code>false</code> otherwise.
     */
    public static boolean showOptionDialog(final Component      owner,
                                           final Object        dialog,
                                           final String         title,
                                           final ActionListener reset)
    {
        // Delegate to Swing thread if this method
        // is invoked from an other thread.
        if (!EventQueue.isDispatchThread()) {
            final boolean result[] = new boolean[1];
            invokeAndWait(new Runnable() {
                public void run() {
                    result[0] = showOptionDialog(owner, dialog, title, reset);
                }
            });
            return result[0];
        }
        // Construct the buttons bar.
        Object[]    options = null;
        Object initialValue = null;
        int okChoice = JOptionPane.OK_OPTION;
        if (reset != null) {
            final Resources resources = Resources.getResources(owner!=null ? owner.getLocale() : null);
            final JButton button;
            if (reset instanceof Action) {
                button = new JButton((Action)reset);
            } else {
                button = new JButton(resources.getString(ResourceKeys.RESET));
                button.addActionListener(reset);
            }
            options = new Object[] {
                resources.getString(ResourceKeys.OK),
                resources.getString(ResourceKeys.CANCEL),
                button
            };
            initialValue = options[okChoice=0];
        }

        // Bring ups the dialog box.
        final int choice;
        if (JOptionPane.getDesktopPaneForComponent(owner)!=null) {
            choice=JOptionPane.showInternalOptionDialog(
                    owner,                         // Composante parente
                    dialog,                        // Message
                    title,                         // Titre de la bo�te de dialogue
                    JOptionPane.OK_CANCEL_OPTION,  // Boutons � placer
                    JOptionPane.PLAIN_MESSAGE,     // Type du message
                    null,                          // Icone
                    options,                       // Liste des boutons
                    initialValue);                 // Bouton par d�faut
        } else {
            choice=JOptionPane.showOptionDialog(
                    owner,                         // Composante parente
                    dialog,                        // Message
                    title,                         // Titre de la bo�te de dialogue
                    JOptionPane.OK_CANCEL_OPTION,  // Boutons � placer
                    JOptionPane.PLAIN_MESSAGE,     // Type du message
                    null,                          // Icone
                    options,                       // Liste des boutons
                    initialValue);                 // Bouton par d�faut
        }
        return choice==okChoice;
    }

    /**
     * Brings up a message dialog with a "Ok" button. This method can be invoked
     * from any thread and blocks until the user click on "Ok".
     *
     * @param  owner   The parent component. Dialog will apears on top of this owner.
     * @param  message The dialog content to show.
     * @param  title   The title string for the dialog.
     * @param  type    The message type
     *                ({@link JOptionPane#ERROR_MESSAGE},
     *                 {@link JOptionPane#INFORMATION_MESSAGE},
     *                 {@link JOptionPane#WARNING_MESSAGE},
     *                 {@link JOptionPane#QUESTION_MESSAGE} or
     *                 {@link JOptionPane#PLAIN_MESSAGE}).
     */
    public static void showMessageDialog(final Component owner,
                                         final Object  message,
                                         final String    title,
                                         final int        type)
    {
        if (!EventQueue.isDispatchThread()) {
            invokeAndWait(new Runnable() {
                public void run() {
                    showMessageDialog(owner, message, title, type);
                }
            });
            return;
        }
        if (JOptionPane.getDesktopPaneForComponent(owner)!=null) {
            JOptionPane.showInternalMessageDialog(
                    owner,     // Composante parente
                    message,   // Message
                    title,     // Titre de la bo�te de dialogue
                    type);     // Type du message
        } else {
            JOptionPane.showMessageDialog(
                    owner,     // Composante parente
                    message,   // Message
                    title,     // Titre de la bo�te de dialogue
                    type);     // Type du message
        }
    }

    /**
     * Brings up a confirmation dialog with "Yes/No" buttons. This method can be
     * invoked from any thread and blocks until the user click on "Yes" or "No".
     *
     * @param  owner   The parent component. Dialog will apears on top of this owner.
     * @param  message The dialog content to show.
     * @param  title   The title string for the dialog.
     * @param  type    The message type
     *                ({@link JOptionPane#ERROR_MESSAGE},
     *                 {@link JOptionPane#INFORMATION_MESSAGE},
     *                 {@link JOptionPane#WARNING_MESSAGE},
     *                 {@link JOptionPane#QUESTION_MESSAGE} or
     *                 {@link JOptionPane#PLAIN_MESSAGE}).
     * @return <code>true</code> if user clicked on "Yes", <code>false</code> otherwise.
     */
    public static boolean showConfirmDialog(final Component owner,
                                            final Object  message,
                                            final String    title,
                                            final int        type)
    {
        if (!EventQueue.isDispatchThread()) {
            final boolean result[] = new boolean[1];
            invokeAndWait(new Runnable() {
                public void run() {
                    result[0]=showConfirmDialog(owner, message, title, type);
                }
            });
            return result[0];
        }
        final int choice;
        if (JOptionPane.getDesktopPaneForComponent(owner)!=null) {
            choice=JOptionPane.showInternalConfirmDialog(
                    owner,                     // Composante parente
                    message,                   // Message
                    title,                     // Titre de la bo�te de dialogue
                    JOptionPane.YES_NO_OPTION, // Boutons � faire appara�tre
                    type);                     // Type du message
        } else {
            choice=JOptionPane.showConfirmDialog(
                    owner,                     // Composante parente
                    message,                   // Message
                    title,                     // Titre de la bo�te de dialogue
                    JOptionPane.YES_NO_OPTION, // Boutons � faire appara�tre
                    type);                     // Type du message
        }
        return choice==JOptionPane.YES_OPTION;
    }

    /**
     * Retourne une �tiquette pour la composante sp�cifi�e.
     * Le texte de l'�tiquette pourra �ventuellement �tre
     * distribu� sur plusieurs lignes.
     *
     * @param owner Composante pour laquelle on construit une �tiquette.
     *              L'�tiquette aura la m�me largeur que <code>owner</code>.
     * @param text  Texte � placer dans l'�tiquette.
     */
    public static JComponent getMultilineLabelFor(final JComponent owner, final String text) {
        final JTextArea label=new JTextArea(text);
        final Dimension size=owner.getPreferredSize();
        size.height=label.getMaximumSize().height;
        label.setMaximumSize  (size);
        label.setWrapStyleWord(true);
        label.setLineWrap     (true);
        label.setEditable    (false);
        label.setFocusable   (false);
        label.setOpaque      (false);
        label.setBorder       (null); // Certains L&F placent une bordure.
        LookAndFeel.installColorsAndFont(label, "Label.background",
                                                "Label.foreground",
                                                "Label.font");
        return label;
    }

    /**
     * Causes runnable to have its run method called in the dispatch thread of
     * the event queue. This will happen after all pending events are processed.
     * The call blocks until this has happened.
     */
    public static void invokeAndWait(final Runnable runnable)
    {
        if (EventQueue.isDispatchThread()) {
            runnable.run();
        } else {
            try {
                EventQueue.invokeAndWait(runnable);
            } catch (InterruptedException exception) {
                // Someone don't want to let us sleep. Go back to work.
            } catch (InvocationTargetException target) {
                final Throwable exception=target.getTargetException();
                if (exception instanceof RuntimeException) {
                    throw (RuntimeException) exception;
                }
                if (exception instanceof Error) {
                    throw (Error) exception;
                }
                // Should not happen, since {@link Runnable#run} do not allow checked exception.
                throw new UndeclaredThrowableException(exception, exception.getLocalizedMessage());
            }
        }
    }
}
