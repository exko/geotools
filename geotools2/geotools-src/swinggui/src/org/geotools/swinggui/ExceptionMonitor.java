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
package org.geotools.swinggui;

// Graphics and geometry
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Graphics2D;

// User interface
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Window;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JDesktopPane;
import javax.swing.BorderFactory;
import javax.swing.JInternalFrame;
import javax.swing.AbstractButton;

// Events
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.EventQueue;

// Logging
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Input / Output
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.geotools.io.ExpandedTabWriter;

// Collections
import java.util.List;
import java.util.ArrayList;

// Miscellaneous
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

// Resources
import org.geotools.resources.Utilities;
import org.geotools.resources.gui.Resources;
import org.geotools.resources.gui.ResourceKeys;


/**
 * Utility which enables exception messages to be displayed in a 
 * <i>Swing</i> component. The standard {@link java.lang.Exception} class
 * contains methods which write the exception to the error console.
 * This <code>ExceptionMonitor</code> class adds static methods which make
 * the message, and eventually the exception trace, appear in a
 * viewer component.
 *
 * <p>&nbsp;</p>
 * <p align="center"><img src="doc-files/ExceptionMonitor.png"></p>
 * <p>&nbsp;</p>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public final class ExceptionMonitor {
    /**
     * Number of spaces to leave between each tab.
     */
    private static final int TAB_WIDTH=4;

    /**
     * The creation of <code>ExceptionMonitor</code> class objects is
     * forbidden.
     */
    private ExceptionMonitor() {
    }

    /**
     * Displays an error message for the specified <code>exception</code>.
     * Note that this method can be called from any thread (not necessarily
     * the <i>Swing</i>) thread.
     *
     * @param owner Component in which the exception is produced, or
     *        <code>null</code> if component unknown.
     * @param exception Exception which has been thrown and is to be
     *        reported to the user.
     */
    public static void show(final Component owner, final Throwable exception) {
        show(owner, exception, null);
    }

    /**
     * Displays an error message for the specified <code>exception</code>.
     * Note that this method can be called from any thread (not necessarily
     * the <i>Swing</i>) thread.
     *
     * @param owner Component in which the exception is produced, or
     *        <code>null</code> if component unknown.
     * @param exception Exception which has been thrown and is to be
     *        reported to the user.
     * @param message Message to display.  If this parameter is null, 
     *        {@link Exception#getLocalizedMessage} will be called to obtain
     *        the message.
     */
    public static void show(final Component owner, final Throwable exception, final String message) {
        if (EventQueue.isDispatchThread()) {
            Pane.show(owner, exception, message);
        }
        else {
            final Runnable monitor=new Runnable()
            {
                public void run() {
                    Pane.show(owner, exception, message);
                }
            };
            try {
                EventQueue.invokeAndWait(monitor);
            } catch (InterruptedException error) {
                // We don't want to be allowed to sleep.  Back to work.
            } catch (InvocationTargetException error) {
                final Throwable e = error.getTargetException();
                if (e instanceof RuntimeException) throw (RuntimeException)     e;
                if (e instanceof Error)            throw (Error)                e;
                Utilities.unexpectedException("org.geotools.gui", "ExceptionMonitor", "show", e);
            }
        }
    }

    /**
     * Writes the specified exception trace in the specified graphics
     * context.  This method is useful when an exception has occurred
     * inside a {@link Component#paint} method and we want to write it
     * rather than leaving an empty window.
     *
     * @param exception Exception whose trace we want to write.
     * @param graphics Graphics context in which to write exception.  The
     *        graphics context should be in its initial state (default affine
     *        transform, default colour, etc...)
     * @param widgetBounds Size of the trace which was being drawn.
     */
    public static void paintStackTrace(final Graphics2D graphics, final Rectangle widgetBounds, final Throwable exception) {
        /*
         * Obtains the exception trace in the form of a character chain.
         * The carriage returns in this chain can be "\r", "\n" or "r\n".
         */
        final String message=printStackTrace(exception);
        /*
         * Examines the character chain line by line.
         * "Glyphs" will be created as we go along and we will take advantage
         * of this to calculate the necessary space.
         */
        double width=0, height=0;
        final List glyphs=new ArrayList();
        final List bounds=new ArrayList();
        final int length=message.length();
        final Font font=graphics.getFont();
        final FontRenderContext context=graphics.getFontRenderContext();
        for (int i=0; i<length;) {
            int ir=message.indexOf('\r', i);
            int in=message.indexOf('\n', i);
            if (ir<0) ir=length;
            if (in<0) in=length;
            final int irn=Math.min(ir,in);
            final GlyphVector line=font.createGlyphVector(context, message.substring(i, irn));
            final Rectangle2D rect=line.getVisualBounds();
            final double w=rect.getWidth();
            if (w>width) width=w;
            height += rect.getHeight();
            glyphs.add(line);
            bounds.add(rect);
            i = (Math.abs(ir-in)<=1 ? Math.max(ir,in) : irn)+1;
        }
        /*
         * Proceeds to draw all the previously calculated glyphs.
         */
        float xpos = (float) (0.5*(widgetBounds.width -width));
        float ypos = (float) (0.5*(widgetBounds.height-height));
        final int size=glyphs.size();
        for (int i=0; i<size; i++) {
            final GlyphVector line = (GlyphVector) glyphs.get(i);
            final Rectangle2D rect = (Rectangle2D) bounds.get(i);
            ypos += rect.getHeight();
            graphics.drawGlyphVector(line, xpos, ypos);
        }
    }

    /**
     * Returns an exception trace. All tabs will have been 
     * systematically replaced by 4 white spaces.
     */
    private static String printStackTrace(final Throwable exception) {
        final StringWriter writer=new StringWriter();
        exception.printStackTrace(new PrintWriter(new ExpandedTabWriter(writer, TAB_WIDTH)));
        return writer.toString();
    }

    /**
     * Class in charge of displaying any exception messages and eventually
     * their traces. The message will appear in a dialog box or in an
     * internal window, depending on the parent.
     * <strong>Note:</strong> All methods in this class must be called in the same
     * thread as the <i>Swing</i> thread.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private static final class Pane extends JOptionPane implements ActionListener {
        /**
         * Default width (in number of columns) of the component which displays
         * the exception message or trace.
         */
        private static final int WIDTH=40;

        /**
         * Minimum height (in pixels) of the dialog box when it also displays
         * the trace.
         */
        private static final int HEIGHT=300;
    
        /**
         * Displayed dialog box.  It will be a {@link JDialog} object or a
         * {@link JInternalFrame} object.
         */
        private final Component dialog;

        /**
         * Exception to display in the dialog box.
         * The method {@link Throwable#getLocalizedMessage}
         * will be called to obtain the message to display.
         */
        private final Throwable exception;
    
        /**
         * Box which will contain the "message" part of the constructed dialog
         * box.  This box will be expanded if the user asks to see the
         * exception trace.  It will arrange the components using
         * {@link BorderLayout}.
         */
        private final Container message;

        /**
         * Component displaying the exception trace. Initially, this
         * component will be null.  It will only be created if the trace
         * is requested by the user.
         */
        private Component trace;

        /**
         * Indicates whether the trace is currently visible. This field's value
         * will be inverted each time the user presses the button "trace".
         */
        private boolean traceVisible;

        /**
         * Button which makes the trace appear or disappear.
         */
        private final AbstractButton traceButton;

        /**
         * Initial size of the dialog box {@link #dialog}.
         * This information will be used to return the box to its initial size
         * when the trace disappears.
         */
        private final Dimension initialSize;

        /**
         * Resources in the user's language.
         */
        private final Resources resources;

        /**
         * Constructs a pane which will display the specified error message.
         *
         * @param owner     Parent Component of the dialog box to be
         *                  created.
         * @param exception Exception we want to report.
         * @param message   Message to display.
         * @param buttons   Buttons to place under the message.  These buttons
         *                  should be in the order "Debug", "Close".
         * @param resources Resources in the user's language.
         */
        private Pane(final Component owner,   final Throwable exception,
                     final Container message, final AbstractButton[] buttons,
                     final Resources resources) {
            super(message, ERROR_MESSAGE, OK_CANCEL_OPTION, null, buttons);
            this.exception   = exception;
            this.message     = message;
            this.resources   = resources;
            this.traceButton = buttons[0];
            buttons[0].addActionListener(this);
            buttons[1].addActionListener(this);
            /*
             * Constructs the dialog box.  Automatically detects if we can use
             * {@link InternalFrame} or if we should be happy with
             * {@link JDialog}. The exception trace will not be written
             * immediately.
             */
            final String classname=Utilities.getShortClassName(exception);
            final String title=resources.getString(ResourceKeys.ERROR_$1, classname);
            final JDesktopPane desktop=getDesktopPaneForComponent(owner);
            if (desktop!=null) {
                final JInternalFrame dialog=createInternalFrame(desktop, title);
                desktop.setLayer(dialog, JDesktopPane.MODAL_LAYER.intValue());
                dialog.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
                dialog.setResizable(false);
                dialog.pack();
                this.dialog=dialog;
            } else {
                final JDialog dialog=createDialog(owner, title);
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setResizable(false);
                dialog.pack();
                this.dialog=dialog;
            }
            initialSize=dialog.getSize();
        }

        /**
         * Constructs and displays a dialog box which informs the user that an
         * exception has been produced.  This method should be called in the
         * same thread as the <i>Swing</i> thread.
         */
        public static void show(final Component owner, final Throwable exception, String message) {
            final Resources resources = Resources.getResources((owner!=null) ? owner.getLocale() : null);
            if (message==null) {
                message=exception.getLocalizedMessage();
                if (message==null) {
                    final String classname = Utilities.getShortClassName(exception);
                    message=resources.getString(ResourceKeys.NO_DETAILS_$1, classname);
                }
            }
            final JTextArea textArea=new JTextArea(message, 1, WIDTH);
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setBackground(null);
            textArea.setBorder(null); // Certain L&Fs have a border.
            /**
             * Constructs the rest of the dialog box.  The title bar will
             * contain the name of the exception class.
             */
            final JComponent messageBox=new JPanel(new BorderLayout());
            messageBox.add(textArea, BorderLayout.NORTH);
            final Pane pane=new Pane(owner, exception, messageBox, new AbstractButton[]
            {
                new JButton(resources.getString(ResourceKeys.DEBUG)),
                new JButton(resources.getString(ResourceKeys.CLOSE))
            }, resources);
            pane.dialog.setVisible(true);
        }

        /**
         * Displays the exception trace below the message. This method is
         * called automatically when the dialog box's "Debug" button is
         * pressed.  If the exception trace still hasn't been written, this
         * method will construct the necessary components once and for all.
         */
        public void actionPerformed(final ActionEvent event) {
            if (event.getSource()!=traceButton) {
                dispose();
                return;
            }
            /*
             * Constructs the exception trace once and for all if it hasn't
             * already been constructed.
             */
            if (trace==null) {
                JComponent traceComponent=null;
                for (Throwable cause  = exception;
                               cause != null;
                               cause  = cause.getCause())
                {
                    final JTextArea text=new JTextArea(1, WIDTH);
                    text.setTabSize(4);
                    text.setText(printStackTrace(cause));
                    text.setEditable(false);
                    final JScrollPane scroll=new JScrollPane(text);
                    if (traceComponent!=null) {
                        if (!(traceComponent instanceof JTabbedPane)) {
                            String classname = Utilities.getShortClassName(exception);
                            JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
                            tabs.addTab(classname, traceComponent);
                            traceComponent = tabs;
                        }
                        String classname = Utilities.getShortClassName(cause);
                        ((JTabbedPane) traceComponent).addTab(classname, scroll);
                    }
                    else {
                        traceComponent = scroll;
                    }
                }
                if (traceComponent==null) {
                    // Should not happen
                    return;
                }
                traceComponent.setBorder(BorderFactory.createCompoundBorder(
                                         BorderFactory.createEmptyBorder(12,0,0,0),
                                                      traceComponent.getBorder()));
                trace=traceComponent;
            }
            /*
             * Inserts or hides the exception trace.  Even if the trace is 
             * hidden, it will not be destroyed if the user would like to
             * redisplay it.
             */
            final Resources resources = Resources.getResources(getLocale());
            traceButton.setText(resources.format(traceVisible ? ResourceKeys.DEBUG
                                                              : ResourceKeys.HIDE));
            traceVisible = !traceVisible;
            if (dialog instanceof Dialog) {
                ((Dialog) dialog).setResizable(traceVisible);
            } else {
                ((JInternalFrame) dialog).setResizable(traceVisible);
            }
            if (traceVisible) {
                message.add(trace, BorderLayout.CENTER);
                dialog.setSize(initialSize.width, HEIGHT);
            } else {
                message.remove(trace);
                dialog.setSize(initialSize);
            }
            dialog.validate();
        }

        /**
         * Frees up the resources used by this dialog box.  This method is
         * called when the user closes the dialog box which reported the
         * exception.
         */
        private void dispose() {
            if (dialog instanceof Window) {
                ((Window) dialog).dispose();
            } else {
                ((JInternalFrame) dialog).dispose();
            }
        }
    }
}
