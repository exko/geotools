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

// Graphics and geometry
import java.awt.Font;
import java.awt.font.GlyphVector;
import java.awt.font.FontRenderContext;
import java.awt.Graphics2D;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

// Collections
import java.util.List;
import java.util.ArrayList;

// Input / Output
import java.io.PrintWriter;
import java.io.StringWriter;
import org.geotools.io.ExpandedTabWriter;


/**
 * A set of utilities methods for painting in a {@link Graphics2D} handle.
 * Method in this class was used to be in {@link org.geotools.gui.swing.ExceptionMonitor}.
 * We had to extract them in a separated class in order to avoid dependencies of renderer
 * module toward the GUI one, especially since the extracted methods are not Swing specific.
 *
 * @version $Id: GraphicsUtilities.java,v 1.1 2003/01/27 22:52:13 desruisseaux Exp $
 * @author Martin Desruisseaux
 */
public final class GraphicsUtilities {
    /**
     * Number of spaces to leave between each tab.
     */
    private static final int TAB_WIDTH = 4;

    /**
     * The creation of <code>GraphicsUtilities</code> class objects is forbidden.
     */
    private GraphicsUtilities() {
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
    public static void paintStackTrace(final Graphics2D graphics,
                                       final Rectangle  widgetBounds,
                                       final Throwable  exception)
    {
        /*
         * Obtains the exception trace in the form of a character chain.
         * The carriage returns in this chain can be "\r", "\n" or "r\n".
         */
        final String message = printStackTrace(exception);
        /*
         * Examines the character chain line by line.
         * "Glyphs" will be created as we go along and we will take advantage
         * of this to calculate the necessary space.
         */
        double width = 0, height = 0;
        final List glyphs = new ArrayList();
        final List bounds = new ArrayList();
        final int length = message.length();
        final Font font = graphics.getFont();
        final FontRenderContext context = graphics.getFontRenderContext();
        for (int i = 0; i < length;) {
            int ir = message.indexOf('\r', i);
            int in = message.indexOf('\n', i);
            if (ir < 0) ir = length;
            if (in < 0) in = length;
            final int irn = Math.min(ir, in);
            final GlyphVector line = font.createGlyphVector(context, message.substring(i, irn));
            final Rectangle2D rect=line.getVisualBounds();
            final double w = rect.getWidth();
            if (w > width) width = w;
            height += rect.getHeight();
            glyphs.add(line);
            bounds.add(rect);
            i = (Math.abs(ir - in) <= 1 ? Math.max(ir, in) : irn) + 1;
        }
        /*
         * Proceeds to draw all the previously calculated glyphs.
         */
        float xpos = (float) (0.5 * (widgetBounds.width - width));
        float ypos = (float) (0.5 * (widgetBounds.height - height));
        final int size = glyphs.size();
        for (int i = 0; i < size; i++) {
            final GlyphVector line = (GlyphVector) glyphs.get(i);
            final Rectangle2D rect = (Rectangle2D) bounds.get(i);
            ypos += rect.getHeight();
            graphics.drawGlyphVector(line, xpos, ypos);
        }
    }

    /**
     * Returns an exception trace. All tabs will have been replaced by 4 white spaces.
     * This method was used to be a private one in {@link org.geotools.gui.swing.ExceptionMonitor}.
     * Do not rely on it.
     */
    public static String printStackTrace(final Throwable exception) {
        final StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(new ExpandedTabWriter(writer, TAB_WIDTH)));
        return writer.toString();
    }
}
