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
package org.geotools.gui.swing;

// Events and action
import java.util.EventListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.ActionMap;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.geotools.gui.swing.event.ZoomChangeEvent;
import org.geotools.gui.swing.event.ZoomChangeListener;

// Geometry
import java.awt.Shape;
import java.awt.Point;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import org.geotools.resources.XAffineTransform;
import org.geotools.resources.XDimension2D;
import org.geotools.resources.Utilities;

// Graphics
import java.awt.Paint;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.Window;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;

// User interface
import java.awt.Toolkit;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.AbstractButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ScrollPaneLayout;
import javax.swing.BoundedRangeModel;
import javax.swing.plaf.ComponentUI;

// Logging
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

// Miscellaneous
import java.util.Arrays;
import java.io.Serializable;
import org.geotools.resources.gui.Resources;
import org.geotools.resources.gui.ResourceKeys;


/**
 * Base class for widget with a zoomable content. User can perform zooms using
 * keyboard, menu or mouse. <code>ZoomPane</code> is an abstract class.
 * Subclass must override at least two methods:
 *
 * <ul>
 *   <li>{@link #getArea()}, which must return a bounding box for the content
 *       to paint. This area can be expressed in arbitrary units. For example,
 *       an object wanting to display a geographic map with a content ranging
 *       from 10� to 15�E and 40� to 45�N should override this method as
 *       follows:
 *
 *       <pre>
 *       &nbsp;public Rectangle2D getArea() {
 *       &nbsp;    return new Rectangle2D.Double(10,40,5,5);
 *       &nbsp;}
 *       </pre></li>
 *
 *   <li>{@link #paintComponent(Graphics2D)}, which must paint the widget
 *       content. Implementation must invoke
 *
 *                    <code>graphics.transform({link #zoom})</code>
 *
 *       somewhere in its code in order to perform the zoom. Note that, by
 *       default, the {@linkplain #zoom} is initialized in such a way that
 *       the <var>y</var> axis points upwards, like the convention in
 *       geometry. This is as opposed to the default Java2D axis orientation,
 *       where the <var>y</var> axis points downwards. If the implementation
 *       wants to paint text, it should do this with the default transform.
 *       Example:
 *
 *       <pre>
 *       &nbsp;protected void paintComponent(final Graphics2D graphics) {
 *       &nbsp;    graphics.clip({link #getZoomableBounds getZoomableBounds}(null));
 *       &nbsp;    final AffineTransform textTr=graphics.getTransform();
 *       &nbsp;    graphics.transform({link #zoom});
 *       &nbsp;    <strong>
 *       &nbsp;    // Paint the widget here, using logical coordinates. The
 *       &nbsp;    // coordinate system is the same as {@link #getArea()}'s one.
 *       &nbsp;    </strong>
 *       &nbsp;    graphics.setTransform(textTr);
 *       &nbsp;    <strong>
 *       &nbsp;    // Paint any text here, in <em>pixel</em> coordinates.
 *       &nbsp;    </strong>
 *       &nbsp;}
 *       </pre></li>
 * </ul>
 *
 * Subclass can also override {@link #reset}, which sets up the initial
 * {@linkplain #zoom}. The default implementation sets up the initial zoom
 * in such a way that the following relations are approximately held:
 *
 * <blockquote>
 * Logical coordinates provided by {@link #getPreferredArea()},
 * after an affine transform described by {@link #zoom},
 * match pixel coordinates provided by {@link #getZoomableBounds(Rectangle)}.
 * </blockquote>
 *
 * The "preferred area" is initially the same as {@link #getArea()}. The user
 * can specify a different preferred area with {@link #setPreferredArea}. The
 * user can also reduce zoomable bounds by inserting an empty border around the
 * widget,
 * e.g.:
 *
 * <pre>
 * &nbsp;setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
 * </pre>
 *
 * <p>&nbsp;</p>
 * <h2>Zoom actions</h2>
 * Whatever action is performed by the user, all zoom commands are translated
 * by calls to {@link #transform}. Derived classes can redefine this method
 * if they want to take particular actions during zooms, for example, modifying
 * the minimum and maximum of a graph's axes.  The table below shows the 
 * keyboard presses assigned to each zoom:
 *
 * <P><TABLE ALIGN=CENTER BORDER=2>
 * <TR><TD><IMG SRC="doc-files/keyboard/up.png"></TD>        <TD>Scroll up</TD>   <TD><code>"Up"</code></TD></TR>
 * <TR><TD><IMG SRC="doc-files/keyboard/down.png"></TD>      <TD>Scroll down</TD>    <TD><code>"Down"</code></TD></TR>
 * <TR><TD><IMG SRC="doc-files/keyboard/left.png"></TD>      <TD>Scroll left</TD> <TD><code>"Left"</code></TD></TR>
 * <TR><TD><IMG SRC="doc-files/keyboard/right.png"></TD>     <TD>Scroll right</TD> <TD><code>"Right"</code></TD></TR>
 * <TR><TD><IMG SRC="doc-files/keyboard/pageDown.png"></TD>  <TD>Zoom in</TD>                <TD><code>"ZoomIn"</code></TD></TR>
 * <TR><TD><IMG SRC="doc-files/keyboard/pageUp.png"></TD>    <TD>Zoom out</TD>              <TD><code>"ZoomOut"</code></TD></TR>
 * <TR><TD><IMG SRC="doc-files/keyboard/end.png"></TD>       <TD>Zoom</TD>            <TD><code>"Zoom"</code></TD></TR>
 * <TR><TD><IMG SRC="doc-files/keyboard/home.png"></TD>      <TD>Default zoom</TD>           <TD><code>"Reset"</code></TD></TR>
 * <TR><TD>Ctrl+<IMG SRC="doc-files/keyboard/left.png"></TD> <TD>Anti-clockwise rotation</TD><TD><code>"RotateLeft"</code></TD></TR>
 * <TR><TD>Ctrl+<IMG SRC="doc-files/keyboard/right.png"></TD><TD>Clockwise rotation</TD>        <TD><code>"RotateRight"</code></TD></TR>
 * </TABLE></P>
 *
 * In this table, the last column gives the Strings by which the different
 * actions which manage the zooms. For example, to zoom in, we must write
 * <code>{@link #getActionMap() getActionMap()}.get("ZoomIn")</code>.
 *
 * <p><strong>Note: {@link JScrollPane} objects are not suitable for adding
 * scrollbars to a <code>ZoomPane</code>object.</strong>
 * Instead, use {@link #createScrollPane}. Once more, all movements performed
 * by the user through the scrollbars will be translated by calls to
 * {@link #transform}.</p>
 *
 * @version 1.0
 * @author Martin Desruisseaux
 */
public abstract class ZoomPane extends JComponent {
    /**
     * Minimum width and height of this component.
     */
    private static final int MINIMUM_SIZE=10;

    /**
     * Default width and height of this component.
     */
    private static final int DEFAULT_SIZE = 400;

    /**
     * Default width and height of the magnifying glass.
     */
    private static final int DEFAULT_MAGNIFIER_SIZE = 150;

    /**
     * Constant indicating the scale changes on the <var>x</var> axis.
     */
    public static final int SCALE_X = (1 << 0);

    /**
     * Constant indicating the scale changes on the <var>y</var> axis.
     */
    public static final int SCALE_Y = (1 << 1);

    /**
     * Constant indicating the scale changes on the <var>x</var> and
     * <var>y</var> axes, with the added condition that these changes
     * must be uniform.  This flag combines {@link #SCALE_X} and
     * {@link #SCALE_Y}. The inverse, however, 
     * (<code>{@link #SCALE_X}|{@link #SCALE_Y}</code>) doesn't involve
     * <code>UNIFORM_SCALE</code>.
     */
    public static final int UNIFORM_SCALE = SCALE_X | SCALE_Y | (1 << 2);

    /**
     * Constant indicating the translations on the <var>x</var> axis.
     */
    public static final int TRANSLATE_X = (1 << 3);

    /**
     * Constant indicating the translations on the <var>y</var> axis.
     */
    public static final int TRANSLATE_Y = (1 << 4);

    /**
     * Constant indicating a rotation.
     */
    public static final int ROTATE  = (1 << 5);

    /**
     * Constant indicating the resetting of scale, rotation and translation
     * to a default value which makes the whole graphic appear in a window.
     * This command is translated by a call to {@link #reset}.
     */
    public static final int RESET = (1 << 6);

    /**
     * Constant indicating default zoom close to the maximum permitted zoom.
     * This zoom should allow details of the graphic to be seen without being
     * overly big.
     * Note: this flag will only have any effect if at least one of the
     * {@link #SCALE_X} and {@link #SCALE_Y} flags is not also specified.
     */
    public static final int DEFAULT_ZOOM = (1 << 7);

    /**
     * Mask representing the combination of all flags.
     */
    private static final int MASK = SCALE_X | SCALE_Y | UNIFORM_SCALE | TRANSLATE_X | TRANSLATE_Y | ROTATE | RESET | DEFAULT_ZOOM;

    /**
     * Number of pixels by which to move the content of
     * <code>ZoomPane</code> during translations.
     */
    private static final double AMOUNT_TRANSLATE = 10;

    /**
     * Zoom factor.  This factor must be greater than 1.
     */
    private static final double AMOUNT_SCALE = 1.03125;

    /**
     * Rotation angle.
     */
    private static final double AMOUNT_ROTATE = Math.PI/90;

    /**
     * Factor by which to multiply the {@link #ACTION_AMOUNT} numbers
     * when the "Shift" key is kept pressed.
     */
    private static final double ENHANCEMENT_FACTOR=7.5;

    /**
     * Flag indicating that a paint is in progress.
     */
    private static final int IS_PAINTING = 0;

    /**
     * Flag indicating that a paint of the magnifying glass is in progress.
     */
    private static final int IS_PAINTING_MAGNIFIER = 1;

    /**
     * Flat indicating that a print is in progress.
     */
    private static final int IS_PRINTING = 2;

    /**
     * List of keys which will identify the zoom actions.
     * These keys also identify the resources to use in order to make the 
     * description appear in the user's language.
     */
    private static final String[] ACTION_ID = {
        /*[0] Left        */ "Left",
        /*[1] Right       */ "Right",
        /*[2] Up          */ "Up",
        /*[3] Down        */ "Down",
        /*[4] ZoomIn      */ "ZoomIn",
        /*[5] ZoomOut     */ "ZoomOut",
        /*[6] ZoomMax     */ "ZoomMax",
        /*[7] Reset       */ "Reset",
        /*[8] RotateLeft  */ "RotateLeft",
        /*[9] RotateRight */ "RotateRight"
    };

    /**
     * List of resource keys, to construct the menus
     * in the user's language.
     */
    private static final int[] RESOURCE_ID = {
        /*[0] Left        */ ResourceKeys.LEFT,
        /*[1] Right       */ ResourceKeys.RIGHT,
        /*[2] Up          */ ResourceKeys.UP,
        /*[3] Down        */ ResourceKeys.DOWN,
        /*[4] ZoomIn      */ ResourceKeys.ZOOM_IN,
        /*[5] ZoomOut     */ ResourceKeys.ZOOM_OUT,
        /*[6] ZoomMax     */ ResourceKeys.ZOOM_MAX,
        /*[7] Reset       */ ResourceKeys.RESET,
        /*[8] RotateLeft  */ ResourceKeys.ROTATE_LEFT,
        /*[9] RotateRight */ ResourceKeys.ROTATE_RIGHT
    };

    /**
     * List of default keystrokes used to perform zooms.
     * The elements of this table go in pairs.  The even indexes indicate
     * the keystroke whilst the odd indexes indicate the modifier
     * (CTRL or SHIFT for example). To obtain the {@link KeyStroke} object
     * for a numbered action <var>i</var>, we can use the following code:
     * 
     * <blockquote><pre>
     * final int key=DEFAULT_KEYBOARD[(i << 1)+0];
     * final int mdf=DEFAULT_KEYBOARD[(i << 1)+1];
     * KeyStroke stroke=KeyStroke.getKeyStroke(key, mdf);
     * </pre></blockquote>
     */
    private static final int[] ACTION_KEY = {
        /*[0] Left        */ KeyEvent.VK_LEFT,      0,
        /*[1] Right       */ KeyEvent.VK_RIGHT,     0,
        /*[2] Up          */ KeyEvent.VK_UP,        0,
        /*[3] Down        */ KeyEvent.VK_DOWN,      0,
        /*[4] ZoomIn      */ KeyEvent.VK_PAGE_UP,   0,
        /*[5] ZoomOut     */ KeyEvent.VK_PAGE_DOWN, 0,
        /*[6] ZoomMax     */ KeyEvent.VK_END,       0,
        /*[7] Reset       */ KeyEvent.VK_HOME,      0,
        /*[8] RotateLeft  */ KeyEvent.VK_LEFT,      KeyEvent.CTRL_MASK,
        /*[9] RotateRight */ KeyEvent.VK_RIGHT,     KeyEvent.CTRL_MASK
    };

    /**
     * Connstants indicating the type of action to perform:
     * translation, zoom or rotation.
     */
    private static final short[] ACTION_TYPE = {
        /*[0] Left        */ (short) TRANSLATE_X,
        /*[1] Right       */ (short) TRANSLATE_X,
        /*[2] Up          */ (short) TRANSLATE_Y,
        /*[3] Down        */ (short) TRANSLATE_Y,
        /*[4] ZoomIn      */ (short) SCALE_X|SCALE_Y,
        /*[5] ZoomOut     */ (short) SCALE_X|SCALE_Y,
        /*[6] ZoomMax     */ (short) DEFAULT_ZOOM,
        /*[7] Reset       */ (short) RESET,
        /*[8] RotateLeft  */ (short) ROTATE,
        /*[9] RotateRight */ (short) ROTATE
    };

    /**
     * Amounts by which to translate, zoom or rotate
     * the contents of the window.
     */
    private static final double[] ACTION_AMOUNT = {
        /*[0] Left        */  +AMOUNT_TRANSLATE,
        /*[1] Right       */  -AMOUNT_TRANSLATE,
        /*[2] Up          */  +AMOUNT_TRANSLATE,
        /*[3] Down        */  -AMOUNT_TRANSLATE,
        /*[4] ZoomIn      */   AMOUNT_SCALE,
        /*[5] ZoomOut     */ 1/AMOUNT_SCALE,
        /*[6] ZoomMax     */   Double.NaN,
        /*[7] Reset       */   Double.NaN,
        /*[8] RotateLeft  */  -AMOUNT_ROTATE,
        /*[9] RotateRight */  +AMOUNT_ROTATE
    };

    /**
     * List of operation types forming a group.  During creation of the
     * menus, the different groups will be separated by a menu separator.
     */
    private static final short[] GROUP = {
        (short) (TRANSLATE_X | TRANSLATE_Y),
        (short) (SCALE_X | SCALE_Y | DEFAULT_ZOOM | RESET),
        (short) (ROTATE)
    };

    /**
     * <code>ComponentUI</code> object in charge of obtaining the preferred
     * size of a <code>ZoomPane</code> object as well as drawing it.
     */
    private static final ComponentUI UI = new ComponentUI() {
        /**
         * Returns a default minimum size.
         */
        public Dimension getMinimumSize(final JComponent c) {
            return new Dimension(MINIMUM_SIZE,MINIMUM_SIZE);
        }

        /**
         * Returns the maximum size. We use the preferred
         * size as a default maximum size.
         */
        public Dimension getMaximumSize(final JComponent c) {
            return getPreferredSize(c);
        }

        /**
         * Returns the default preferred size. User can override this
         * preferred size by invoking {@link JComponent#setPreferredSize}.
         */
        public Dimension getPreferredSize(final JComponent c) {
            return ((ZoomPane) c).getDefaultSize();
        }

        /**
         * Override {@link ComponentUI#update} in order to handle painting of
         * magnifying glass, which is a special case. Since the magnifying
         * glass is painted just after the normal component, we don't want to
         * clear the background before painting it.
         */
        public void update(final Graphics g, final JComponent c) {
            switch (((ZoomPane) c).flag) {
                case IS_PAINTING_MAGNIFIER: paint(g,c); break; // Avoid background clearing
                default:             super.update(g,c); break;
            }
        }

        /**
         * Paint the component. This method basically delegates the
         * work to {@link ZoomPane#paintComponent(Graphics2D)}.
         */
        public void paint(final Graphics g, final JComponent c) {
            final ZoomPane pane = (ZoomPane)   c;
            final Graphics2D gr = (Graphics2D) g;
            switch (pane.flag) {
                case IS_PAINTING:           pane.paintComponent(gr); break;
                case IS_PAINTING_MAGNIFIER: pane.paintMagnifier(gr); break;
                case IS_PRINTING:           pane.printComponent(gr); break;
                default: throw new IllegalStateException(Integer.toString(pane.flag));
            }
        }
    };

    /**
     * Object in charge of drawing a box representing the user's selection.  We
     * retain a reference to this object in order to be able to register it and
     * extract it at will from the list of objects interested in being notified
     * of the mouse movements.
     */
    private final MouseListener mouseSelectionTracker=new MouseSelectionTracker()
    {
        /**
         * Returns the selection shape. This is usually a rectangle, but could
         * very well be an ellipse or any other kind of geometric shape. This
         * method asks {@link ZoomPane#getMouseSelectionShape} for the shape.
         */
        protected Shape getModel(final MouseEvent event) {
            final Point2D point=new Point2D.Double(event.getX(), event.getY());
            if (getZoomableBounds().contains(point)) try {
                return getMouseSelectionShape(zoom.inverseTransform(point, point));
            } catch (NoninvertibleTransformException exception) {
                unexpectedException("getModel", exception);
            }
            return null;
        }

        /**
         * Invoked when the user finishes the selection. This method will
         * delegate the action to {@link ZoomPane#mouseSelectionPerformed}.
         * Default implementation will perform a zoom.
         */
        protected void selectionPerformed(int ox, int oy, int px, int py) {
            try {
                final Shape selection=getSelectedArea(zoom);
                if (selection!=null) {
                    mouseSelectionPerformed(selection);
                }
            } catch (NoninvertibleTransformException exception) {
                unexpectedException("selectionPerformed", exception);
            }
        }
    };

    /**
     * Class responsible for listening out for the different events necessary
     * for the smooth working of {@link ZoomPane}. This class will listen out
     * for mouse clicks (in order to eventually claim the focus or make a
     * contextual menu appear).  It will listen out for changes in the size
     * of the component (to adjust the zoom), etc.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private final class Listeners extends MouseAdapter implements MouseWheelListener, ComponentListener, Serializable
    {
        public void mouseWheelMoved (final MouseWheelEvent event) {ZoomPane.this.mouseWheelMoved (event);}
        public void mousePressed    (final MouseEvent      event) {ZoomPane.this.mayShowPopupMenu(event);}
        public void mouseReleased   (final MouseEvent      event) {ZoomPane.this.mayShowPopupMenu(event);}
        public void componentResized(final ComponentEvent  event) {ZoomPane.this.processSizeEvent(event);}
        public void componentMoved  (final ComponentEvent  event) {}
        public void componentShown  (final ComponentEvent  event) {}
        public void componentHidden (final ComponentEvent  event) {}
    }

    /**
     * Affine transform containing zoom factors, translations and rotations.
     * During the painting of a component, this affine transform should be
     * combined with a call to 
     * <code>{@link Graphics2D#transform(AffineTransform) Graphics2D.transform}(zoom)</code>.
     */
    protected final AffineTransform zoom=new AffineTransform();

    /**
     * Indicates whether the zoom is the result of a {@link #reset} operation.
     */
    private boolean zoomIsReset;

    /**
     * Types of zoom permitted.  This field should be a combination of the
     * constants {@link #SCALE_X}, {@link #SCALE_Y}, {@link #TRANSLATE_X},
     * {@link #TRANSLATE_Y}, {@link #ROTATE}, {@link #RESET} and
     * {@link #DEFAULT_ZOOM}.
     */
    private final int type;

    /**
     * Strategy to follow in order to calculate the initial affine transform.
     * The value <code>true</code> indicates that the content should fill the
     * entire panel, even if it means losing some of the edges. The value
     * <code>false</code> indicates, on the contrary, that we should display
     * the entire contents, even if it means leaving blank spaces in the panel.
     */
    private boolean fillPanel=false;

    /**
     * Rectangle representing the logical coordinates of the visible region.
     * This information is used to keep the same region when the size or 
     * position of the component changes.  Initially, this rectangle is empty.
     * It will only stop being empty if {@link #reset} is called and 
     * {@link #getPreferredArea} and {@link #getZoomableBounds} have both 
     * returned valid coordinates.
     *
     * @see #getVisibleArea
     * @see #setVisibleArea
     */
    private final Rectangle2D visibleArea=new Rectangle2D.Double();

    /**
     * Rectangle representing the logical coordinates of the region to 
     * display initially, the first time that the window is displayed.
     * The value <code>null</code> indicates a call to {@link #getArea}.
     *
     * @see #getPreferredArea
     * @see #setPreferredArea
     */
    private Rectangle2D preferredArea;

    /**
     * Menu to display when the user right clicks with their mouse.  This menu
     * will contain the navigation options.
     *
     * @see #getPopupMenu
     */
    private transient PointPopupMenu navigationPopupMenu;

    /**
     * Flag indicating which part of the paint is in progress.  The permitted
     * values are {@link #IS_PAINTING}, {@link #IS_PAINTING_MAGNIFIER} and
     * {@link #IS_PRINTING}.
     */
    private transient int flag;

    /**
     * Indicates if this <code>ZoomPane</code> object should be repainted when
     * the user adjusts the scrollbars.  The default value is <code>false</code>,
     * which means that <code>ZoomPane</code> will wait until the user has 
     * released the scrollbar before repainting the component.
     *
     * @see #isPaintingWhileAdjusting
     * @see #setPaintingWhileAdjusting
     */
    private boolean paintingWhileAdjusting;

    /**
     * Rectangle in which to place the coordinates returned by
     * {@link #getZoomableBounds}. This object is defined in order to avoid
     * allocating objects too often {@link Rectangle}.
     */
    private transient Rectangle cachedBounds;

    /**
     * Object in which to record the result of {@link #getInsets}.
     * Used in order to avoid {@link #getZoomableBounds} allocating
     * {@link Insets} objects too often.
     */
    private transient Insets cachedInsets;

    /**
     * Indicates whether the user is authorised to display
     * the magnifying glass.  The default value is <code>true</code>.
     */
    private boolean magnifierEnabled=true;

    /**
     * Magnification factor inside the magnifying glass.
     * This factor must be greater than 1.
     */
    private double magnifierPower=4;

    /**
     * Geometric shape in which to magnify.  The coordinates of this
     * shape should be expressed in pixels.  The value <code>null</code>
     * means that no magnifying glass will be drawn.
     */
    private transient MouseReshapeTracker magnifier;

    /**
     * Colour with which to tint magnifying glass.
     */
    private final Color magnifierColor=new Color(197,204,221);

    /**
     * Colour of the magnifying glass's border.
     */
    private final Color magnifierBorder=new Color(102,102,153);

    /**
     * Construct a <code>ZoomPane</code>.
     *
     * @param  type Allowed zoom type. It can be a bitwise combination of the
     *         following constants:
     *             {@link #SCALE_X}, {@link #SCALE_Y}, {@link #UNIFORM_SCALE},
     *             {@link #TRANSLATE_X}, {@link #TRANSLATE_Y},
     *             {@link #ROTATE}, {@link #RESET} and {@link #DEFAULT_ZOOM}.
     * @throws IllegalArgumentException If <code>type</code> is invalid.
     */
    public ZoomPane(final int type) throws IllegalArgumentException {
        if ((type & ~MASK) != 0) {
            throw new IllegalArgumentException();
        }
        this.type=type;
        final Resources resources = Resources.getResources(null);
        final InputMap   inputMap = getInputMap();
        final ActionMap actionMap = getActionMap();
        for (int i=0; i<ACTION_ID.length; i++) {
            final short actionType=ACTION_TYPE[i];
            if ((actionType & type)!=0) {
                final String  actionID = ACTION_ID[i];
                final double    amount = ACTION_AMOUNT[i];
                final int     keyboard = ACTION_KEY[(i<<1)+0];
                final int     modifier = ACTION_KEY[(i<<1)+1];
                final KeyStroke stroke = KeyStroke.getKeyStroke(keyboard, modifier);
                final Action    action = new AbstractAction() {
                    /*
                     * Action to perform when a key has been hit
                     * or the mouse clicked.
                     */
                    public void actionPerformed(final ActionEvent event) {
                        Point          point = null;
                        final Object  source = event.getSource();
                        final boolean button = (source instanceof AbstractButton);
                        if (button) {
                            for (Container c=(Container) source; c!=null; c=c.getParent()) {
                                if (c instanceof PointPopupMenu) {
                                    point = ((PointPopupMenu) c).point;
                                    break;
                                }
                            }
                        }
                        double m=amount;
                        if (button || (event.getModifiers() & ActionEvent.SHIFT_MASK)!=0) {
                            if ((actionType & UNIFORM_SCALE)!=0) m = (m>=1) ? 2.0 : 0.5;
                            else                                 m*= ENHANCEMENT_FACTOR;
                        }
                        transform(actionType & type, m, point);
                    }
                };
                action.putValue(Action.NAME,               resources.getString(RESOURCE_ID[i]));
                action.putValue(Action.ACTION_COMMAND_KEY, actionID);
                action.putValue(Action.ACCELERATOR_KEY,    stroke);
                actionMap.put(actionID, action);
                inputMap .put(stroke, actionID);
                inputMap .put(KeyStroke.getKeyStroke(keyboard, modifier|KeyEvent.SHIFT_MASK), actionID);
            }
        }
        /*
         * Adds an object which will be in charge of listening
         * for mouse clicks in order to display a contextual
         * menu, as well as an object which will be in charge of
         * listening for mouse movements in order to perform
         * zooms.
         */
        final Listeners listeners=new Listeners();
        addComponentListener       (listeners);
        super.addMouseListener     (listeners);
        super.addMouseWheelListener(listeners);
        super.addMouseListener(mouseSelectionTracker);
        setAutoscrolls(true);
        setFocusable(true);
        setOpaque(true);
        setUI(UI);
    }

    /**
     * Reinitializes the affine transform {@link #zoom} in order to cancel
     * any zoom, rotation or translation.  The default implementation 
     * initializes the affine transform {@link #zoom} in order to make the
     * <var>y</var> axis point upwards and make the whole of the region covered
     * by the {@link #getPreferredArea} logical coordinates appear in the panel.
     *<br><br>
     * Note: for the derived classes: <code>reset()</code> is <u>the only</u>
     * method of <code>ZoomPane</code> which doesn't have to pass through
     * {@link #transform(AffineTransform)} to modify the zoom.  This exception
     * is necessary to avoid falling into an infinite loop.
     */
    public void reset() {
        reset(getZoomableBounds(), true);
    }

    /**
     * Reinitializes the affine transform {@link #zoom} in order to cancel
     * any zoom, rotation or translation. The argument <code>yAxisUpward</code>
     * indicates whether the <var>y</var> axis should point upwards.  The value
     * <code>false</code> lets it point downwards. This method is offered
     * for convenience sake for derived classes which want to redefine
     * {@link #reset()}.
     *
     * @param zoomableBounds Coordinates, in pixels, of the screen space in
     *                       which to draw.  This argument will usually be
     *                       <code>{@link #getZoomableBounds(Rectangle)
     *                                     getZoomableBounds}(null)</code>.
     * @param yAxisUpward    <code>true</code> if the <var>y</var> axis should
     *                       point upwards rather than downwards.
     */
    protected final void reset(final Rectangle zoomableBounds,
                               final boolean yAxisUpward) {
        if (!zoomableBounds.isEmpty()) {
            final Rectangle2D preferredArea=getPreferredArea();
            if (isValid(preferredArea)) {
                final AffineTransform change;
                try {
                    change=zoom.createInverse();
                } catch (NoninvertibleTransformException exception) {
                    unexpectedException("reset", exception);
                    return;
                }
                if (yAxisUpward) zoom.setToScale(+1, -1);
                else             zoom.setToIdentity();
                final AffineTransform transform=setVisibleArea(preferredArea,
                                                               zoomableBounds);
                change.concatenate(zoom);
                zoom  .concatenate(transform);
                change.concatenate(transform);
                fireZoomChanged0  (change);
                getVisibleArea(zoomableBounds); // Force update of 'visibleArea'
                /*
                 * The three private versions 'fireZoomPane0', 'getVisibleArea'
                 * and 'setVisibleArea' avoid calling other methods of ZoomPane
                 * so as not to end up in an infinite loop.
                 */
                repaint(zoomableBounds);
                zoomIsReset=true;
                log("reset", visibleArea);
            }
        }
    }

    /**
     * Set the policy for the zoom when the content is initially drawn
     * or when the user resets the zoom. Value <code>true</code> means that
     * the panel should initially be completely filled, even if the content
     * partially falls outside the panel's bounds. Value <code>false</code>
     * means that the full content should appear in the panel, even if some
     * space is not used. Default value is <code>false</code>.
     */
    protected void setResetPolicy(final boolean fill) {
        fillPanel = fill;
    }

    /**
     * Returns a bounding box that contains the logical coordinates of all
     * data that may be displayed in this <code>ZoomPane</code>. For example,
     * if this <code>ZoomPane</code> is to display a geographic map, then
     * this method should return the map's bounds in degrees of latitude
     * and longitude. This bounding box is completely independent of any
     * current zoom setting and will change only if the content changes.
     *
     * @return A bounding box for the logical coordinates of all contents
     *         that are going to be drawn in this <code>ZoomPane</code>. If
     *         this bounding box is unknown, then this method can return
     *         <code>null</code> (but this is not recommended).
     */
    public abstract Rectangle2D getArea();

    /**
     * Indicates whether the logical coordinates of a region have been defined.
     * This method returns <code>true</code> if {@link #setPreferredArea} has
     * been called with a non null argument.
     */
    public final boolean hasPreferredArea() {
        return preferredArea!=null;
    }

    /**
     * Returns the logical coordinates of the region that we want to see
     * displayed the first time that <code>ZoomPane</code> appears on the
     * screen.  This region will also be displayed each time the method
     * {link #reset} is called. The default implementation goes as follows:
     *
     * <ul>
     *   <li>If a region has already been defined by a call to
     *       {@link #setPreferredArea}, this region will be returned.</li>
     *   <li>If not, the whole region {@link #getArea} will be returned.</li>
     * </ul>
     *
     * @return The logical coordinates of the region to be initially displayed,
     *         or <code>null</code> if these coordinates are unknown.
     */
    public final Rectangle2D getPreferredArea() {
        return (preferredArea!=null) ? (Rectangle2D) preferredArea.clone() : getArea();
    }

    /**
     * Specifies the logical coordinates of the region that we want to see
     * displayed the first time that <code>ZoomPane</code> appears on the screen.
     * This region will also be displayed the first time that the method
     * {link #reset} is called.
     */
    public final void setPreferredArea(final Rectangle2D area) {
        if (area!=null) {
            if (isValid(area)) {
                final Object oldArea;
                if (preferredArea==null) {
                    oldArea=null;
                    preferredArea=new Rectangle2D.Double();
                }
                else oldArea=preferredArea.clone();
                preferredArea.setRect(area);
                firePropertyChange("preferredArea", oldArea, area);
                log("setPreferredArea", area);
            } else {
                throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_BAD_RECTANGLE_$1, area));
            }
        }
        else preferredArea=null;
    }

    /**
     * Returns the logical coordinates of the region visible on the screen.
     * In the case of a geographic map, for example, the logical coordinates
     * can be expressed in degrees of latitude/longitude or even in metres
     * if a cartographic projection has been defined.
     */
    public final Rectangle2D getVisibleArea() {
        return getVisibleArea(getZoomableBounds());
    }

    /**
     * Implementation of {@link #getVisibleArea()}.
     */
    private Rectangle2D getVisibleArea(final Rectangle zoomableBounds) {
        if (zoomableBounds.isEmpty()) {
            return (Rectangle2D) visibleArea.clone();
        }
        Rectangle2D visible;
        try {
            visible=XAffineTransform.inverseTransform(zoom, zoomableBounds, null);
        } catch (NoninvertibleTransformException exception) {
            unexpectedException("getVisibleArea", exception);
            visible=new Rectangle2D.Double(zoomableBounds.getCenterX(), zoomableBounds.getCenterY(), 0, 0);
        }
        visibleArea.setRect(visible);
        return visible;
    }

    /**
     * Defines the limits of the visible part, in logical coordinates.  This
     * method will modify the zoom and the translation in order to display the
     * specified region. If {@link #zoom} contains a rotation, this rotation
     * will not be modified.
     *
     * @param  logicalBounds Logical coordinates of the region to be displayed.
     * @throws IllegalArgumentException if <code>source</code> is empty.
     */
    public final void setVisibleArea(final Rectangle2D logicalBounds) throws IllegalArgumentException {
        log("setVisibleArea", logicalBounds);
        transform(setVisibleArea(logicalBounds, getZoomableBounds()));
    }

    /**
     * Defines the limits of the visible part, in logical coordinates.  This
     * method will modify the zoom and the translation in order to display the
     * specified region.  If {@link #zoom} contains a rotation, this rotation
     * will not be modified.
     *
     * @param  source Logical coordinates of the region to be displayed.
     * @param  dest Pixel coordinates of the region of the window in which to 
     *         draw (normally {@link #getZoomableBounds()}).
     * @return Change to apply to the affine transform {@link #zoom}.
     * @throws IllegalArgumentException if <code>source</code> is empty.
     */
    private AffineTransform setVisibleArea(Rectangle2D source, Rectangle2D dest) throws IllegalArgumentException {
        /*
         * Verifies the validity of the rectangle <code>source</code>. An
         * invalid rectangle will be rejected. However, we will be more
         * flexible for <code>dest</code> since the window could have been
         * reduced by the user.
         */
        if (!isValid(source)) {
            throw new IllegalArgumentException(Resources.format(ResourceKeys.ERROR_BAD_RECTANGLE_$1, source));
        }
        if (!isValid(dest)) {
            return new AffineTransform();
        }
        /*
         * Converts the destination into logical coordinates.  We can
         * then perform a zoom and a translation which would put
         * <code>source</code> in <code>dest</code>.
         */
        try {
            dest=XAffineTransform.inverseTransform(zoom, dest, null);
        } catch (NoninvertibleTransformException exception) {
            unexpectedException("setVisibleArea", exception);
            return new AffineTransform();
        }
        final double sourceWidth  = source.getWidth ();
        final double sourceHeight = source.getHeight();
        final double   destWidth  =   dest.getWidth ();
        final double   destHeight =   dest.getHeight();
              double           sx = destWidth/sourceWidth;
              double           sy = destHeight/sourceHeight;
        /*
         * Standardizes the horizontal and vertical scales,
         * if such a standardization has been requested.
         */
        if ((type & UNIFORM_SCALE) == UNIFORM_SCALE) {
            if (fillPanel)
            {
                     if (sy*sourceWidth  > destWidth ) sx=sy;
                else if (sx*sourceHeight > destHeight) sy=sx;
            }
            else
            {
                     if (sy*sourceWidth  < destWidth ) sx=sy;
                else if (sx*sourceHeight < destHeight) sy=sx;
            }
        }
        final AffineTransform change=AffineTransform.getTranslateInstance(
                         (type & TRANSLATE_X)!=0 ? dest.getCenterX()    : 0,
                         (type & TRANSLATE_Y)!=0 ? dest.getCenterY()    : 0);
        change.scale    ((type & SCALE_X    )!=0 ? sx                   : 1,
                         (type & SCALE_Y    )!=0 ? sy                   : 1);
        change.translate((type & TRANSLATE_X)!=0 ? -source.getCenterX() : 0,
                         (type & TRANSLATE_Y)!=0 ? -source.getCenterY() : 0);
        XAffineTransform.round(change);
        return change;
    }

    /**
     * Returns the bounding box (in pixel coordinates) of the zoomable area.
     * <strong>For performance reasons, this method reuses an internal cache.
     * Never modify the returned rectangle!</strong>. This internal method
     * is invoked by every method looking for this <code>ZoomPane</code>
     * dimension.
     *
     * @return The bounding box of the zoomable area, in pixel coordinates
     *         relative to this <code>ZoomPane</code> widget. <strong>Do not
     *         change the returned rectangle!</strong>
     */
    private final Rectangle getZoomableBounds() {
        return cachedBounds = getZoomableBounds(cachedBounds);
    }
    
    /**
     * Returns the bounding box (in pixel coordinates) of the zoomable area.
     * This method is similar to {@link #getBounds(Rectangle)}, except that
     * the zoomable area may be smaller than the whole widget area.
     * For example, a chart needs to keep some space for axes around the
     * zoomable area. Another difference is that pixel coordinates are
     * relative to the widget, i.e. the (0,0) coordinate lies on the
     * <code>ZoomPane</code> upper left corner, no matter what its location
     * on screen.<br>
     * <br>
     * <code>ZoomPane</code> invokes <code>getZoomableBounds</code> when it
     * needs to set up an initial {@link #zoom} value. Subclasses should also
     * set the clip area to this bounding box in their
     * {@link #paintComponent(Graphics2D)} method <em>before</em> setting the
     * graphics transform. For example:
     *
     * <blockquote><pre>
     * graphics.clip(getZoomableBounds(null));
     * graphics.transform({@link #zoom});
     * </pre></blockquote>
     *
     * @param  bounds An optional pre-allocated rectangle, or <code>null</code>
     *                to create a new one. This argument is useful if the caller
     *                wants to avoid allocating a new object on the heap.
     * @return The bounding box of the zoomable area, in pixel coordinates
     *         relative to this <code>ZoomPane</code> widget.
     */
    protected Rectangle getZoomableBounds(Rectangle bounds) {
        Insets insets;
        bounds=getBounds(bounds); insets=cachedInsets;
        insets=getInsets(insets); cachedInsets=insets;
        if (bounds.isEmpty()) {
            final Dimension size=getPreferredSize();
            bounds.width  = size.width;
            bounds.height = size.height;
        }
        bounds.x       =  insets.left;
        bounds.y       =  insets.top;
        bounds.width  -= (insets.left+insets.right);
        bounds.height -= (insets.top+insets.bottom);
        return bounds;
    }

    /**
     * Returns the default size for this component.  This is the size
     * returned by {@link #getPreferredSize} if no preferred size has
     * been explicitly set with {@link #setPreferredSize}.
     *
     * @return The default size for this component.
     */
    protected Dimension getDefaultSize() {
        return getViewSize();
    }

    /**
     * Returns the preferred pixel size for a close zoom. For image rendering,
     * the preferred pixel size is the image's pixel size in logical units. For
     * other kinds of rendering, this "pixel" size should be some reasonable
     * resolution. The default implementation computes a default value from
     * {@link #getArea}.
     */
    protected Dimension2D getPreferredPixelSize() {
        final Rectangle2D area = getArea();
        if (isValid(area)) {
            return new XDimension2D.Double(area.getWidth () / (10*getWidth ()),
                                           area.getHeight() / (10*getHeight()));
        }
        else {
            return new Dimension(1,1);
        }
    }

    /**
     * Returns the current {@linkplain #zoom} scale factor. A value of
     * 1/100 means that 100 metres are displayed as 1 pixel (provided
     * that the logical coordinates of {@link #getArea} are expressed
     * in metres). Scale factors for X and Y axes can be computed
     * separately using the following equations:
     *
     * <table cellspacing=3><tr>
     * <td width=50%><IMG src="doc-files/scaleX.png"></td>
     * <td width=50%><IMG src="doc-files/scaleY.png"></td>
     * </tr></table>
     *
     * This method combines scale along both axes, which is correct
     * if this <code>ZoomPane</code> has been constructed with the
     * {@link #UNIFORM_SCALE} type.
     */
    public double getScaleFactor() {
        final double m00 = zoom.getScaleX();
        final double m11 = zoom.getScaleY();
        final double m01 = zoom.getShearX();
        final double m10 = zoom.getShearY();
        return Math.sqrt(m00*m00 + m11*m11 + m01*m01 + m10*m10);
    }

    /**
     * LINDA UP TO HERE
     Change the {@linkplain #zoom} by applying and affine transform. The
     * <code>change</code> transform must express a change if logical units,
     * for example a translation in meters. This method is conceptually similar
     * to the following code:
     *
     * <pre>
     * {@link #zoom}.{@link AffineTransform#concatenate(AffineTransform) concatenate}(change);
     * {@link #fireZoomChanged(AffineTransform) fireZoomChanged}(change);
     * {@link #repaint() repaint}({@link #getZoomableBounds getZoomableBounds}(null));
     * </pre>
     *
     * @param  change The zoom change, as an affine transform in logical
     *         coordinates. If <code>change</code> is the identity transform,
     *         then this method do nothing and listeners are not notified.
     */
    public void transform(final AffineTransform change) {
        if (!change.isIdentity()) {
            zoom.concatenate(change);
            XAffineTransform.round(zoom);
            fireZoomChanged(change);
            repaint(getZoomableBounds());
            zoomIsReset=false;
        }
    }

    /**
     * Effectue un zoom, une translation ou une rotation sur le contenu de
     * <code>ZoomPane</code>. Le type d'op�ration � effectuer d�pend de
     * l'argument <code>operation</code>:
     *
     * <ul>
     *   <li>{@link #TRANSLATE_X} effectue une translation le long de l'axe des
     *       <var>x</var>. L'argument <code>amount</code> sp�cifie la
     *       transformation � effectuer en nombre de pixels. Une valeur n�gative
     *       d�place vers la gauche tandis qu'une valeur positive d�place vers
     *       la droite.</li>
     *   <li>{@link #TRANSLATE_Y} effectue une translation le long de l'axe des
     *       <var>y</var>. L'argument <code>amount</code> sp�cifie la
     *       transformation � effectuer en nombre de pixels. Une valeur n�gative
     *       d�place vers le haut tandis qu'une valeur positive d�place vers le
     *       bas.</li>
     *   <li>{@link #UNIFORM_SCALE} effectue un zoom. L'argument
     *       <code>zoom</code> sp�cifie le zoom � effectuer. Une valeur
     *       sup�rieure � 1 effectuera un zoom avant, tandis qu'une valeur
     *       comprise entre 0 et 1 effectuera un zoom arri�re.</li>
     *   <li>{@link #ROTATE} effectue une rotation. L'argument <code>zoom</code>
     *       sp�cifie l'angle de rotation en radians.</li>
     *   <li>{@link #RESET} Red�finit le zoom � une �chelle, rotation et
     *       translation par d�faut. Cette op�ration aura pour effet de faire
     *       appara�tre la totalit� ou quasi-totalit� du contenu de
     *       <code>ZoomPane</code>.</li>
     *   <li>{@link #DEFAULT_ZOOM} Effectue un zoom par d�faut, proche du zoom
     *       maximal, qui fait voir les d�tails du contenu de
     *       <code>ZoomPane</code> mais sans les grossir ex�gar�ment.</li>
     * </ul>
     *
     * @param  operation Type d'op�ration � effectuer.
     * @param  amount Translation en pixels ({@link #TRANSLATE_X} et
     *         {@link #TRANSLATE_Y}), facteur d'�chelle ({@link #SCALE_X} et
     *         {@link #SCALE_Y}) ou angle de rotation en radians
     *         ({@link #ROTATE}). Dans les autres cas, cet argument est ignor�
     *         et peut �tre {@link Double#NaN}.
     * @param  center Centre du zoom ({@link #SCALE_X} et {@link #SCALE_Y}) ou
     *         de la rotation ({@link #ROTATE}), en coordonn�es pixels. La
     *         valeur <code>null</code> d�signe une valeur par d�faut, le plus
     *         souvent le centre de la fen�tre.
     * @throws UnsupportedOperationException si l'argument
     *         <code>operation</code> n'est pas reconnu.
     */
    private void transform(final int operation,
                           final double amount,
                           final Point2D center) throws UnsupportedOperationException {
        if ((operation & (RESET))!=0) {
            /////////////////////
            ////    RESET    ////
            /////////////////////
            if ((operation & ~(RESET))!=0) {
                throw new UnsupportedOperationException();
            }
            reset();
            return;
        }
        final AffineTransform change;
        try {
            change=zoom.createInverse();
        } catch (NoninvertibleTransformException exception) {
            unexpectedException("transform", exception);
            return;
        }
        if ((operation & (TRANSLATE_X|TRANSLATE_Y))!=0) {
            /////////////////////////
            ////    TRANSLATE    ////
            /////////////////////////
            if ((operation & ~(TRANSLATE_X|TRANSLATE_Y))!=0) {
                throw new UnsupportedOperationException();
            }
            change.translate(((operation & TRANSLATE_X)!=0) ? amount : 0,
                             ((operation & TRANSLATE_Y)!=0) ? amount : 0);
        } else {
            /*
             * Obtient les coordonn�es (en pixels)
             * du centre de rotation ou du zoom.
             */
            final double centerX;
            final double centerY;
            if (center!=null) {
                centerX = center.getX();
                centerY = center.getY();
            } else {
                final Rectangle bounds=getZoomableBounds();
                if (bounds.width>=0 && bounds.height>=0) {
                    centerX = bounds.getCenterX();
                    centerY = bounds.getCenterY();
                } else {
                    return;
                }
                /*
                 * On accepte les largeurs et hauteurs de 0. Si toutefois le
                 * rectangle n'est pas valide (largeur ou hauteur n�gatif),
                 * alors on terminera cette m�thode sans rien faire. Aucun
                 * zoom n'aura �t� effectu�.
                 */
            }
            if ((operation & (ROTATE))!=0) {
                //////////////////////
                ////    ROTATE    ////
                //////////////////////
                if ((operation & ~(ROTATE))!=0) {
                    throw new UnsupportedOperationException();
                }
                change.rotate(amount, centerX, centerY);
            } else if ((operation & (SCALE_X|SCALE_Y))!=0) {
                /////////////////////
                ////    SCALE    ////
                /////////////////////
                if ((operation & ~(UNIFORM_SCALE))!=0) {
                    throw new UnsupportedOperationException();
                }
                change.translate(+centerX, +centerY);
                change.scale(((operation & SCALE_X)!=0) ? amount : 1,
                             ((operation & SCALE_Y)!=0) ? amount : 1);
                change.translate(-centerX, -centerY);
            } else if ((operation & (DEFAULT_ZOOM))!=0) {
                ////////////////////////////
                ////    DEFAULT_ZOOM    ////
                ////////////////////////////
                if ((operation & ~(DEFAULT_ZOOM))!=0) {
                    throw new UnsupportedOperationException();
                }
                final Dimension2D size=getPreferredPixelSize();
                double sx = 1/(size.getWidth()  * XAffineTransform.getScaleX0(zoom));
                double sy = 1/(size.getHeight() * XAffineTransform.getScaleY0(zoom));
                if ((type & UNIFORM_SCALE)==UNIFORM_SCALE) {
                    if (sx>sy) sx=sy;
                    if (sy>sx) sy=sx;
                }
                if ((type & SCALE_X)==0) sx=1;
                if ((type & SCALE_Y)==0) sy=1;
                change.translate(+centerX, +centerY);
                change.scale    ( sx     ,  sy     );
                change.translate(-centerX, -centerY);
            }
            else {
                throw new UnsupportedOperationException();
            }
        }
        change.concatenate(zoom);
        XAffineTransform.round(change);
        transform(change);
    }

    /**
     * Ajoute un objet � la liste des objets int�ress�s
     * � �tre inform�s des changements de zoom.
     */
    public void addZoomChangeListener(final ZoomChangeListener listener) {
        listenerList.add(ZoomChangeListener.class, listener);
    }

    /**
     * Retire un objet de la liste des objets int�ress�s
     * � �tre inform�s des changements de zoom.
     */
    public void removeZoomChangeListener(final ZoomChangeListener listener) {
        listenerList.remove(ZoomChangeListener.class, listener);
    }

    /**
     * Ajoute un objet � la liste des objets int�ress�s
     * � �tre inform�s des �v�nements de la souris.
     */
    public void addMouseListener(final MouseListener listener) {
        super.removeMouseListener(mouseSelectionTracker);
        super.addMouseListener   (listener);
        super.addMouseListener   (mouseSelectionTracker); // MUST be last!
    }

    /**
     * Signale qu'un changement du zoom vient d'�tre effectu�. Chaque objets
     * enregistr�s par la m�thode {@link #addZoomChangeListener} sera pr�venu
     * du changement aussit�t que possible.
     *
     * @param change Transformation affine qui repr�sente le changement dans le
     *               zoom. Soit <code>oldZoom</code> et <code>newZoom</code> les
     *               transformations affines de l'ancien et du nouveau zoom
     *               respectivement. Alors la relation
     *
     * <code>newZoom=oldZoom.{@link AffineTransform#concatenate concatenate}(change)</code>
     *
     *               doit �tre respect�e (aux erreurs d'arrondissements pr�s).
     *               <strong>Notez que cette m�thode peut modifier
     *               <code>change</code></strong> pour combiner en une seule
     *               transformation plusieurs appels cons�cutifs de
     *               <code>fireZoomChanged</code>.
     */
    protected void fireZoomChanged(final AffineTransform change) {
        visibleArea.setRect(getVisibleArea());
        fireZoomChanged0(change);
    }

    /**
     * Pr�viens les classes d�riv�es que le zoom a chang�. Contrairement � la
     * m�thode {@link #fireZoomChanged} prot�g�e, cette m�thode priv�e ne
     * modifie aucun champ interne et n'essaye pas d'appeller d'autres m�thodes
     * de <code>ZoomPane</code> comme {@link #getVisibleArea}. On �vite ainsi
     * une boucle sans fin lorsque cette m�thode est appel�e par {@link #reset}.
     */
    private void fireZoomChanged0(final AffineTransform change) {
        /*
         * Note: il faut lancer l'�v�nement m�me si la transformation
         *       est la matrice identit�, car certaine classe utilise
         *       ce truc pour mettre � jour les barres de d�filements.
         */
        if (change==null) {
            throw new NullPointerException();
        }
        ZoomChangeEvent event=null;
        final Object[] listeners=listenerList.getListenerList();
        for (int i=listeners.length; (i-=2)>=0;) {
            if (listeners[i]==ZoomChangeListener.class) {
                if (event==null) {
                    event=new ZoomChangeEvent(this, change);
                }
                try {
                    ((ZoomChangeListener) listeners[i+1]).zoomChanged(event);
                } catch (RuntimeException exception) {
                    unexpectedException("fireZoomChanged", exception);
                }
            }
        }
    }

    /**
     * M�thode appel�e automatiquement apr�s que l'utilisateur ait s�lectionn�e
     * une r�gion � l'aide de la souris. L'impl�mentation par d�faut zoom la
     * r�gion <code>area</code> s�lectionn�e. Les classes d�riv�es peuvent
     * red�finir cette m�thode pour entreprendre une autre action.
     *
     * @param area R�gion s�lectionn�e par l'utilisateur, en coordonn�es
     *        logiques.
     */
    protected void mouseSelectionPerformed(final Shape area) {
        final Rectangle2D rect=(area instanceof Rectangle2D) ? (Rectangle2D) area : area.getBounds2D();
        if (isValid(rect)) {
            setVisibleArea(rect);
        }
    }

    /**
     * Retourne la forme g�om�trique � utiliser pour d�limiter une r�gion.
     * Cette forme est g�n�ralement un rectangle mais pourrait aussi �tre
     * une ellipse, une fl�che ou d'autres formes encore. Les coordonn�es
     * de la forme retourn�e ne seront pas prises en compte. En fait, ces
     * coordonn�es seront r�guli�rement �cras�es.  Seule compte la classe
     * de la forme retourn�e (par exemple {@link java.awt.geom.Ellipse2D}
     * vs {@link java.awt.geom.Rectangle2D}) et ses param�tres non-reli�s
     * � sa position (par exemple l'arrondissement des coins d'un rectangle
     * {@link java.awt.geom.RoundRectangle2D}).
     *
     * La forme retourn�e sera g�n�ralement d'une classe d�riv�e de
     * {@link RectangularShape}, mais peut aussi �tre de la classe
     * {@link Line2D}. <strong>Tout autre classe risque de lancer une
     * {@link ClassCastException} au moment de l'ex�cution</strong>.
     *
     * L'impl�mentation par d�faut retourne toujours un objet
     * {@link java.awt.geom.Rectangle2D}.
     *
     * @param  event Coordonn�es logiques de la souris au moment ou le bouton a
     *         �t� enfonc�. Cette information peut �tre utilis�e par les classes
     *         d�riv�es qui voudraient tenir compte de la position de la souris
     *         avant de choisir une forme g�om�trique.
     * @return Forme de la classe {link RectangularShape} ou {link Line2D}, ou
     *         <code>null</code> pour indiquer qu'on ne veut pas faire de
     *         s�lection avec la souris.
     */
    protected Shape getMouseSelectionShape(final Point2D point) {
        return new Rectangle2D.Float();
    }

    /**
     * Indique si la loupe est visible. Par d�faut, la loupe n'est pas visible.
     * Appelez {@link #setMagnifierVisible(boolean)} pour la faire apparaitre.
     */
    public boolean isMagnifierVisible() {
        return magnifier!=null;
    }

    /**
     * Fait appara�tre ou dispara�tre la loupe. Si la loupe n'�tait pas visible
     * et que cette m�thode est appel�e avec l'argument <code>true</code>, alors
     * la loupe appara�tra au centre de la fen�tre.
     */
    public void setMagnifierVisible(final boolean visible) {
        setMagnifierVisible(visible, null);
    }

    /**
     * Indique si l'affichage de la loupe est autoris�e sur
     * cette composante. Par d�faut, elle est autoris�e.
     */
    public boolean isMagnifierEnabled() {
        return magnifierEnabled;
    }

    /**
     * Sp�cifie si l'affichage de la loupe est autoris�e sur cette composante.
     * L'appel de cette m�thode avec la valeur <code>false</code> fera
     * dispara�tre la loupe, supprimera le choix "Afficher la loupe" du menu
     * contextuel et fera ignorer tous les appels �
     * <code>{@link #setMagnifierVisible setMagnifierVisible}(true)</code>.
     */
    public void setMagnifierEnabled(final boolean enabled) {
        magnifierEnabled=enabled;
        navigationPopupMenu=null;
        if (!enabled) {
            setMagnifierVisible(false);
        }
    }

    /**
     * Corrige les coordonn�es d'un pixel pour tenir compte de la pr�sence de la
     * loupe. La point <code>point</code> doit contenir les coordonn�es d'un
     * pixel � l'�cran. Si la loupe est visible et que <code>point</code> se
     * trouve sur cette loupe, alors ses coordonn�es seront corrig�es pour faire
     * comme si elle pointait sur le m�me pixel, mais en l'absence de la loupe.
     * En effet, la pr�sence de la loupe peut d�placer la position apparante des
     * pixels.
     */
    public final void correctPointForMagnifier(final Point2D point) {
        if (magnifier!=null && magnifier.contains(point)) {
            final double centerX = magnifier.getCenterX();
            final double centerY = magnifier.getCenterY();
            /*
             * Le code suivant est �quivalent au transformations ci-dessous.
             * Ces transformations doivent �tre identiques � celles qui sont
             * appliqu�es dans {@link #paintMagnifier}.
             *
             *         translate(+centerX, +centerY);
             *         scale    (magnifierPower, magnifierPower);
             *         translate(-centerX, -centerY);
             *         inverseTransform(point, point);
             */
            point.setLocation((point.getX() - centerX)/magnifierPower + centerX,
                              (point.getY() - centerY)/magnifierPower + centerY);
        }
    }

    /**
     * Fait appara�tre ou dispara�tre la loupe. Si la loupe n'�tait pas visible
     * et que cette m�thode est appel�e avec l'argument <code>true</code>, alors
     * la loupe appara�tra centr� sur la coordonn�es sp�cifi�e.
     *
     * @param visible <code>true</code> pour faire appara�tre la loupe,
     *                ou <code>false</code> pour la faire dispara�tre.
     * @param center  Coordonn�e centrale � laquelle faire appara�tre la loupe.
     *                Si la loupe �tait initialement invisible, elle appara�tra
     *                centr�e � cette coordonn�e (ou au centre de l'�cran si
     *                <code>center</code> est nul). Si la loupe �tait d�j�
     *                visible et que <code>center</code> est non-nul, alors elle
     *                sera d�plac�e pour la centrer � la coordonn�es sp�cifi�e.
     */
    private void setMagnifierVisible(final boolean visible, final Point center) {
        if (visible && magnifierEnabled) {
            if (magnifier==null) {
                Rectangle bounds=getZoomableBounds(); // Do not modifiy the Rectangle!
                if (bounds.isEmpty()) bounds=new Rectangle(0,0,DEFAULT_SIZE,DEFAULT_SIZE);
                final int size=Math.min(Math.min(bounds.width, bounds.height), DEFAULT_MAGNIFIER_SIZE);
                final int centerX, centerY;
                if (center!=null) {
                    centerX = center.x - size/2;
                    centerY = center.y - size/2;
                } else {
                    centerX = bounds.x+(bounds.width -size)/2;
                    centerY = bounds.y+(bounds.height-size)/2;
                }
                magnifier=new MouseReshapeTracker(new Ellipse2D.Float(centerX, centerY, size, size))
                {
                    protected void stateWillChange(final boolean isAdjusting) {repaintMagnifier();}
                    protected void stateChanged   (final boolean isAdjusting) {repaintMagnifier();}
                };
                magnifier.setClip(bounds);
                magnifier.setAdjustable(SwingConstants.NORTH, true);
                magnifier.setAdjustable(SwingConstants.SOUTH, true);
                magnifier.setAdjustable(SwingConstants.EAST , true);
                magnifier.setAdjustable(SwingConstants.WEST , true);

                addMouseListener      (magnifier);
                addMouseMotionListener(magnifier);
                firePropertyChange("magnifierVisible", Boolean.FALSE, Boolean.TRUE);
                repaintMagnifier();
            }
            else if (center!=null) {
                final Rectangle2D frame=magnifier.getFrame();
                final double width  = frame.getWidth();
                final double height = frame.getHeight();
                magnifier.setFrame(center.x-0.5*width,
                                   center.y-0.5*height, width, height);
            }
        }
        else if (magnifier!=null) {
            repaintMagnifier();
            removeMouseMotionListener(magnifier);
            removeMouseListener      (magnifier);
            setCursor(null);
            magnifier=null;
            firePropertyChange("magnifierVisible", Boolean.TRUE, Boolean.FALSE);
        }
    }

    /**
     * Ajoute au menu sp�cifi� des options de navigations. Des menus
     * tels que "Zoom avant" et "Zoom arri�re" seront automatiquement
     * ajout�s au menu avec les raccourcis-clavier appropri�s.
     */
    public void buildNavigationMenu(final JMenu menu) {
        buildNavigationMenu(menu, null);
    }

    /**
     * Ajoute au menu sp�cifi� des options de navigations. Des menus
     * tels que "Zoom avant" et "Zoom arri�re" seront automatiquement
     * ajout�s au menu avec les raccourcis-clavier appropri�s.
     */
    private void buildNavigationMenu(final JMenu menu, final JPopupMenu popup) {
        int groupIndex=0;
        final ActionMap actionMap=getActionMap();
        for (int i=0; i<ACTION_ID.length; i++) {
            final Action action=actionMap.get(ACTION_ID[i]);
            if (action!=null && action.getValue(Action.NAME)!=null) {
                /*
                 * V�rifie si le prochain item fait parti d'un nouveau groupe.
                 * Si c'est la cas, il faudra ajouter un s�parateur avant le
                 * prochain menu.
                 */
                final int lastGroupIndex=groupIndex;
                while ((ACTION_TYPE[i] & GROUP[groupIndex]) == 0) {
                    groupIndex = (groupIndex+1) % GROUP.length;
                    if (groupIndex==lastGroupIndex) break;
                }
                /*
                 * Ajoute un item au menu.
                 */
                if (menu!=null) {
                    if (groupIndex!=lastGroupIndex) menu.addSeparator();
                    final JMenuItem item=new JMenuItem(action);
                    item.setAccelerator((KeyStroke) action.getValue(Action.ACCELERATOR_KEY));
                    menu.add(item);
                }
                if (popup!=null) {
                    if (groupIndex!=lastGroupIndex) popup.addSeparator();
                    final JMenuItem item=new JMenuItem(action);
                    item.setAccelerator((KeyStroke) action.getValue(Action.ACCELERATOR_KEY));
                    popup.add(item);
                }
            }
        }
    }

    /**
     * Menu avec une position. Cette classe retient les coordonn�es
     * exacte de l'endroit o� a cliqu� l'utilisateur lorsqu'il a
     * invok� ce menu.
     *
     * @author Martin Desruisseaux
     * @version 1.0
     */
    private static final class PointPopupMenu extends JPopupMenu {
        /**
         * Coordonn�es de l'endroit o�
         * avait cliqu� l'utilisateur.
         */
        public final Point point;

        /**
         * Construit un menu en retenant
         * la coordonn�e sp�cifi�e.
         */
        public PointPopupMenu(final Point point) {
            this.point=point;
        }
    }

    /**
     * M�thode appel�e automatiquement lorsque l'utilisateur a cliqu� sur le
     * bouton droit de la souris. L'impl�mentation par d�faut fait appara�tre
     * un menu contextuel dans lequel figure des options de navigations.
     *
     * @param  event Ev�nement de la souris contenant entre autre les
     *         coordonn�es point�es.
     * @return Le menu contextuel, ou <code>null</code> pour ne pas faire
     *         appara�tre de menu.
     */
    protected JPopupMenu getPopupMenu(final MouseEvent event) {
        if (getZoomableBounds().contains(event.getX(), event.getY())) {
            if (navigationPopupMenu==null) {
                navigationPopupMenu=new PointPopupMenu(event.getPoint());
                if (magnifierEnabled) {
                    final Resources resources = Resources.getResources(getLocale());
                    final JMenuItem item=new JMenuItem(resources.getString(ResourceKeys.SHOW_MAGNIFIER));
                    item.addActionListener(new ActionListener()
                    {
                        public void actionPerformed(final ActionEvent event)
                        {setMagnifierVisible(true, navigationPopupMenu.point);}
                    });
                    navigationPopupMenu.add(item);
                    navigationPopupMenu.addSeparator();
                }
                buildNavigationMenu(null, navigationPopupMenu);
            } else {
                navigationPopupMenu.point.x = event.getX();
                navigationPopupMenu.point.y = event.getY();
            }
            return navigationPopupMenu;
        }
        else return null;
    }

    /**
     * M�thode appel�e automatiquement lorsque l'utilisateur a cliqu� sur le
     * bouton droit de la souris � l'int�rieur de la loupe. L'impl�mentation
     * par d�faut fait appara�tre un menu contextuel dans lequel figure des
     * options relatives � la loupe.
     *
     * @param  event Ev�nement de la souris contenant entre autre les
     *         oordonn�es point�es.
     * @return Le menu contextuel, ou <code>null</code> pour ne pas faire
     *         appara�tre de menu.
     */
    protected JPopupMenu getMagnifierMenu(final MouseEvent event) {
        final Resources resources = Resources.getResources(getLocale());
        final JPopupMenu menu = new JPopupMenu(resources.getString(ResourceKeys.MAGNIFIER));
        final JMenuItem  item = new JMenuItem (resources.getString(ResourceKeys.HIDE));
        item.addActionListener(new ActionListener()
        {
            public void actionPerformed(final ActionEvent event)
            {setMagnifierVisible(false);}
        });
        menu.add(item);
        return menu;
    }

    /**
     * Fait appara�tre le menu contextuel de navigation, � la
     * condition que l'�v�nement de la souris soit bien celui
     * qui fait normalement appara�tre ce menu.
     */
    private void mayShowPopupMenu(final MouseEvent event) {
        if ( event.getID()       == MouseEvent.MOUSE_PRESSED &&
            (event.getModifiers() & MouseEvent.BUTTON1_MASK)!=0)
        {
            requestFocus();
        }
        if (event.isPopupTrigger()) {
            final Point point      = event.getPoint();
            final JPopupMenu popup = (magnifier!=null && magnifier.contains(point)) ? getMagnifierMenu(event) : getPopupMenu(event);
            if (popup!=null) {
                final Component source  = event.getComponent();
                final Window    window  = SwingUtilities.getWindowAncestor(source);
                if (window!=null) {
                    final Toolkit   toolkit = source.getToolkit();
                    final Insets    insets  = toolkit.getScreenInsets(window.getGraphicsConfiguration());
                    final Dimension screen  = toolkit.getScreenSize();
                    final Dimension size    = popup.getPreferredSize();
                    SwingUtilities.convertPointToScreen(point, source);
                    screen.width  -= (size.width  + insets.right);
                    screen.height -= (size.height + insets.bottom);
                    if (point.x > screen.width)  point.x = screen.width;
                    if (point.y > screen.height) point.y = screen.height;
                    if (point.x < insets.left)   point.x = insets.left;
                    if (point.y < insets.top)    point.y = insets.top;
                    SwingUtilities.convertPointFromScreen(point, source);
                    popup.show(source, point.x, point.y);
                }
            }
        }
    }

    /**
     * M�thode appel�e automatiquement lorsque l'utilisateur a fait
     * tourn� la roulette de la souris. Cette m�thode effectue un
     * zoom centr� sur la position de la souris.
     */
    private final void mouseWheelMoved(final MouseWheelEvent event)
    {
        if (event.getScrollType()==MouseWheelEvent.WHEEL_UNIT_SCROLL) {
            int rotation  = event.getUnitsToScroll();
            double scale  = 1+(AMOUNT_SCALE-1)*Math.abs(rotation);
            Point2D point = new Point2D.Double(event.getX(), event.getY());
            if (rotation > 0) scale = 1/scale;
            if (magnifier!=null && magnifier.contains(point)) {
                magnifierPower *= scale;
                repaintMagnifier();
            } else {
                correctPointForMagnifier(point);
                transform(UNIFORM_SCALE & type, scale, point);
            }
            event.consume();
        }
    }

    /**
     * M�thode appel�e chaque fois que la dimension
     * ou la position de la composante a chang�e.
     */
    private final void processSizeEvent(final ComponentEvent event)
    {
        if (!isValid(visibleArea) || zoomIsReset) {
            reset();
        }
        if (magnifier!=null) {
            magnifier.setClip(getZoomableBounds());
        }
        /*
         * On n'appelle par {@link #repaint} parce qu'il y a d�j� une commande
         * {@link #repaint} dans la queue.  Ainsi, le retra�age sera deux fois
         * plus rapide sous le JDK 1.3. On n'appele pas {@link #transform} non
         * plus car le zoom n'a pas vraiment chang�;  on a seulement d�couvert
         * une partie de la fen�tre qui �tait cach�e. Mais il faut tout de m�me
         * ajuster les barres de d�filements.
         */
        final Object[] listeners=listenerList.getListenerList();
        for (int i=listeners.length; (i-=2)>=0;) {
            if (listeners[i]==ZoomChangeListener.class) {
                if (listeners[i+1] instanceof Synchronizer) try {
                    ((ZoomChangeListener) listeners[i+1]).zoomChanged(null);
                } catch (RuntimeException exception) {
                    unexpectedException("processSizeEvent", exception);
                }
            }
        }
    }

    /**
     * Retourne un objet qui affiche ce <code>ZoomPane</code>
     * avec des barres de d�filements.
     */
    public JComponent createScrollPane() {
        return new ScrollPane();
    }

    /**
     * The scroll panel for {@link ZoomPane}. The standard {@link JScrollPane}
     * class is not used because it is difficult to get {@link JViewport} to
     * cooperate with transformation already handled by {@link ZoomPane#zoom}.
     *
     * @version 1.0
     * @author Martin Desruisseaux
     */
    private final class ScrollPane extends JComponent implements PropertyChangeListener {
        /**
         * The horizontal scrolling bar, or <code>null</code> if none.
         */
        private final JScrollBar scrollbarX;

        /**
         * The vertical scrolling bar, or <code>null</code> if none.
         */
        private final JScrollBar scrollbarY;

        /**
         * Construct a scroll pane for the enclosing {@link ZoomPane}.
         */
        public ScrollPane() {
            setOpaque(false);
            setLayout(new GridBagLayout());
            /*
             * Setup the scroll bars.
             */
            if ((type & TRANSLATE_X)!=0) {
                scrollbarX=new JScrollBar(JScrollBar.HORIZONTAL);
                scrollbarX.setUnitIncrement ((int) (AMOUNT_TRANSLATE));
                scrollbarX.setBlockIncrement((int) (AMOUNT_TRANSLATE*ENHANCEMENT_FACTOR));
            } else {
                scrollbarX  = null;
            }
            if ((type & TRANSLATE_Y)!=0) {
                scrollbarY=new JScrollBar(JScrollBar.VERTICAL);
                scrollbarY.setUnitIncrement ((int) (AMOUNT_TRANSLATE));
                scrollbarY.setBlockIncrement((int) (AMOUNT_TRANSLATE*ENHANCEMENT_FACTOR));
            } else {
                scrollbarY  = null;
            }
            /*
             * Add the scroll bars in the scroll pane.
             */
            final Rectangle bounds = getZoomableBounds(); // Cached Rectangle: do not modify.
            final GridBagConstraints c = new GridBagConstraints();
            if (scrollbarX!=null) {
                c.gridx=0; c.weightx=1;
                c.gridy=1; c.weighty=0;
                c.fill=c.HORIZONTAL;
                add(scrollbarX, c);
            }
            if (scrollbarY!=null) {
                c.gridx=1; c.weightx=0;
                c.gridy=0; c.weighty=1;
                c.fill=c.VERTICAL;
                add(scrollbarY, c);
            }
            if (scrollbarX!=null && scrollbarY!=null) {
                final JComponent corner = new JPanel();
                corner.setOpaque(true);
                c.gridx=1; c.weightx=0;
                c.gridy=1; c.weighty=0;
                c.fill=c.BOTH;
                add(corner, c);
            }
            c.fill=c.BOTH;
            c.gridx=0; c.weightx=1;
            c.gridy=0; c.weighty=1;
            add(ZoomPane.this, c);
        }

        /**
         * Convenience method fetching a scroll bar model.
         * Should be a static method, but compiler doesn't
         * allow this.
         */
        private /*static*/ BoundedRangeModel getModel(final JScrollBar bar) {
            return (bar!=null) ? bar.getModel() : null;
        }

        /**
         * Invoked when this <code>ScrollPane</code>  is added in
         * a {@link Container}. This method register all required
         * listeners.
         */
        public void addNotify() {
            super.addNotify();
            tieModels(getModel(scrollbarX), getModel(scrollbarY));
            ZoomPane.this.addPropertyChangeListener("zoom.insets", this);
        }

        /**
         * Invoked when this <code>ScrollPane</code> is removed from
         * a {@link Container}. This method unregister all listeners.
         */
        public void removeNotify() {
            ZoomPane.this.removePropertyChangeListener("zoom.insets", this);
            untieModels(getModel(scrollbarX), getModel(scrollbarY));
            super.removeNotify();
        }

        /**
         * Invoked when the zoomable area changed. This method will adjust
         * scroll bar's insets in order to keep scroll bars aligned in front
         * of the zoomable area.
         *
         * Note: in current version, this is an undocumented capability.
         *       Class {@link RangeBar} use it, but it is experimental.
         *       It may change in a future version.
         */
        public void propertyChange(final PropertyChangeEvent event) {
            final Insets old    = (Insets) event.getOldValue();
            final Insets insets = (Insets) event.getNewValue();
            final GridBagLayout layout = (GridBagLayout) getLayout();
            if (scrollbarX!=null && (old.left!=insets.left || old.right!=insets.right)) {
                final GridBagConstraints c = layout.getConstraints(scrollbarX);
                c.insets.left  = insets.left;
                c.insets.right = insets.right;
                layout.setConstraints(scrollbarX, c);
                scrollbarX.invalidate();
            }
            if (scrollbarY!=null && (old.top!=insets.top || old.bottom!=insets.bottom)) {
                final GridBagConstraints c = layout.getConstraints(scrollbarY);
                c.insets.top    = insets.top;
                c.insets.bottom = insets.bottom;
                layout.setConstraints(scrollbarY, c);
                scrollbarY.invalidate();
            }
        }
    }

    /**
     * Synchronise la position et l'�tendu des models <var>x</var> et
     * <var>y</var> avec la position du zoom. Les models <var>x</var>
     * et <var>y</var> sont g�n�ralement associ�s � des barres de defilements
     * horizontale et verticale. Lorsque la position d'une barre de d�filement
     * est ajust�e, le zomm sera ajust� en cons�quence. Inversement, lorsque le
     * zoom est modifi�, les positions et �tendus des barres de d�filements sont
     * ajust�es en cons�quence.
     *
     * @param x Mod�le de la barre de d�filement horizontale,
     *          ou <code>null</code> s'il n'y en a pas.
     * @param y Mod�le de la barre de d�filement verticale,
     *          ou <code>null</code> s'il n'y en a pas.
     */
    public void tieModels(final BoundedRangeModel x, final BoundedRangeModel y) {
        if (x!=null || y!=null) {
            final Synchronizer listener=new Synchronizer(x,y);
            addZoomChangeListener(listener);
            if (x!=null) x.addChangeListener(listener);
            if (y!=null) y.addChangeListener(listener);
        }
    }

    /**
     * Annule la synchronisation entre les models <var>x</var> et <var>y</var>
     * sp�cifi�s et le zoom de cet objet <code>ZoomPane</code>. Les objets
     * {@link ChangeListener} et {@link ZoomChangeListener} qui avait �t� cr��s
     * seront supprim�s.
     *
     * @param x Mod�le de la barre de d�filement horizontale,
     *          ou <code>null</code> s'il n'y en a pas.
     * @param y Mod�le de la barre de d�filement verticale,
     *          ou <code>null</code> s'il n'y en a pas.
     */
    public void untieModels(final BoundedRangeModel x, final BoundedRangeModel y) {
        final EventListener[] listeners=getListeners(ZoomChangeListener.class);
        for (int i=0; i<listeners.length; i++) {
            if (listeners[i] instanceof Synchronizer) {
                final Synchronizer s=(Synchronizer) listeners[i];
                if (s.xm==x && s.ym==y) {
                    removeZoomChangeListener(s);
                    if (x!=null) x.removeChangeListener(s);
                    if (y!=null) y.removeChangeListener(s);
                }
            }
        }
    }

    /**
     * Objet ayant la charge de synchronizer un objet {@link JScrollPane}
     * avec des barres de d�filements. Bien que ce ne soit g�n�ralement pas
     * utile, il serait possible de synchroniser plusieurs paires d'objets
     * {@link BoundedRangeModel} sur un  m�me objet <code>ZoomPane</code>.
     *
     * @author Martin Desruisseaux
     * @version 1.0
     */
    private final class Synchronizer implements ChangeListener, ZoomChangeListener {
        /**
         * Mod�le � synchroniser avec {@link ZoomPane}.
         */
        public final BoundedRangeModel xm,ym;

        /**
         * Indique si les barres de d�filements sont en train
         * d'�tre ajust�es en r�ponse � {@link #zoomChanged}.
         * Si c'est la cas, {@link #stateChanged} ne doit pas
         * faire d'autres ajustements.
         */
        private transient boolean isAdjusting;

        /**
         * Cached <code>ZoomPane</code> bounds. Used in order
         * to avoid two many object allocation on the heap.
         */
        private transient Rectangle bounds;

        /**
         * Construit un objet qui synchronisera une paire de
         * {@link BoundedRangeModel} avec {@link ZoomPane}.
         */
        public Synchronizer(final BoundedRangeModel xm, final BoundedRangeModel ym) {
            this.xm = xm;
            this.ym = ym;
        }

        /**
         * M�thode appel�e automatiquement chaque fois que la
         * position d'une des barres de d�filement a chang�e.
         */
        public void stateChanged(final ChangeEvent event) {
            if (!isAdjusting) {
                final boolean valueIsAdjusting=((BoundedRangeModel) event.getSource()).getValueIsAdjusting();
                if (paintingWhileAdjusting || !valueIsAdjusting) {
                    /*
                     * Scroll view coordinates are computed with the following
                     * steps:
                     *
                     *   1) Get the logical coordinates for the whole area.
                     *   2) Transform to pixel space using current zoom.
                     *   3) Clip to the scroll bars position (in pixels).
                     *   4) Transform back to the logical space.
                     *   5) Set the visible area to the resulting rectangle.
                     */
                    Rectangle2D area=getArea();
                    if (isValid(area)) {
                        area=XAffineTransform.transform(zoom, area, null);
                        double x=area.getX();
                        double y=area.getY();
                        double width, height;
                        if (xm!=null) {
                            x    += xm.getValue();
                            width = xm.getExtent();
                        } else {
                            width = area.getWidth();
                        }
                        if (ym!=null) {
                            y     += ym.getValue();
                            height = ym.getExtent();
                        } else {
                            height = area.getHeight();
                        }
                        area.setRect(x, y, width, height);
                        bounds = getBounds(bounds);
                        try {
                            area=XAffineTransform.inverseTransform(zoom, area, area);
                            try {
                                isAdjusting=true;
                                transform(setVisibleArea(area, bounds=getBounds(bounds)));
                            } finally {
                                isAdjusting=false;
                            }
                        } catch (NoninvertibleTransformException exception) {
                            unexpectedException("stateChanged", exception);
                        }
                    }
                }
                if (!valueIsAdjusting) {
                    zoomChanged(null);
                }
            }
        }

        /**
         * M�thode appel�e chaque fois que le zoom a chang�.
         *
         * @param change Ignor�. Peut �tre nul, et sera
         *               effectivement parfois nul.
         */
        public void zoomChanged(final ZoomChangeEvent change) {
            if (!isAdjusting) {
                Rectangle2D area=getArea();
                if (isValid(area)) {
                    area = XAffineTransform.transform(zoom, area, null);
                    try {
                        isAdjusting=true;
                        setRangeProperties(xm, area.getX(), getWidth(),  area.getWidth());
                        setRangeProperties(ym, area.getY(), getHeight(), area.getHeight());
                    }
                    finally {
                        isAdjusting=false;
                    }
                }
            }
        }
    }

    /**
     * Proc�de � l'ajustement des valeurs d'un model. Les minimums et maximums
     * seront ajust�s au besoin afin d'inclure la valeur et son �tendu. Cet
     * ajustement est n�cessaire pour �viter un comportement chaotique lorsque
     * l'utilisateur fait glisser l'ascensceur pendant qu'une partie du
     * graphique est en dehors de la zone qui �tait initialement pr�vue par
     * {@link #getArea}.
     */
    private static void setRangeProperties(final BoundedRangeModel model,
                                           final double value, final int extent,
                                           final double max) {
        if (model!=null) {
            final int pos = (int) Math.round(-value);
            model.setRangeProperties(pos, extent, Math.min(0, pos),
                                     Math.max((int) Math.round(max), pos+extent), false);
        }
    }

    /**
     * Modifie la position en pixels de la partie visible de
     * <code>ZoomPanel</code>. Soit <code>viewSize</code> les dimensions en
     * pixels qu'aurait <code>ZoomPane</code> si sa surface visible couvrait
     * la totalit� de la r�gion {@link #getArea} avec le zoom courant (Note:
     * cette dimension <code>viewSize</code> peut �tre obtenues par {@link
     * #getPreferredSize} si {@link #setPreferredSize} n'a pas �t� appel�e avec
     * une valeur non-nulle). Alors par d�finition la r�gion {@link #getArea}
     * convertit dans l'espace des pixels donnerait le rectangle
     *
     * <code>bounds=Rectangle(0,&nbsp;0,&nbsp;,viewSize.width,&nbsp;,viewSize.height)</code>.
     *
     * Cette m�thode <code>scrollRectToVisible</code> permet de d�finir la
     * sous-r�gion de <code>bounds</code> qui doit appara�tre dans la fen�tre
     * <code>ZoomPane</code>.
     */
    public void scrollRectToVisible(final Rectangle rect) {
        Rectangle2D area=getArea();
        if (isValid(area)) {
            area=XAffineTransform.transform(zoom, area, null);
            area.setRect(area.getX()+rect.getX(), area.getY()+rect.getY(), rect.getWidth(), rect.getHeight());
            try {
                setVisibleArea(XAffineTransform.inverseTransform(zoom, area, area));
            } catch (NoninvertibleTransformException exception) {
                unexpectedException("scrollRectToVisible", exception);
            }
        }
    }

    /**
     * Indique si cet objet <code>ZoomPane</code> doit �tre redessin� pendant
     * que l'utilisateur d�place le glissoir des barres de d�filements. Les
     * barres de d�filements (ou autres models) concern�es sont celles qui ont
     * �t� synchronis�es avec cet objet <code>ZoomPane</code> � l'aide de la
     * m�thode {@link #tieModels}. La valeur par d�faut est <code>false</code>,
     * ce qui signifie que <code>ZoomPane</code> attendra que l'utilisateur ait
     * relach� le glissoir avant de se redessiner.
     */
    public boolean isPaintingWhileAdjusting() {
        return paintingWhileAdjusting;
    }

    /**
     * D�finit si cet objet <code>ZoomPane</code> devra redessiner la carte
     * pendant que l'utilisateur d�place le glissoir des barres de d�filements.
     * Il vaut mieux avoir un ordinateur assez rapide pour donner la valeur
     * <code>true</code> � ce drapeau.
     */
    public void setPaintingWhileAdjusting(final boolean flag) {
        paintingWhileAdjusting = flag;
    }

    /**
     * D�clare qu'une partie de ce paneau a besoin d'�tre red�ssin�e. Cette
     * m�thode ne fait que red�finir la m�thode de la classe parente pour tenir
     * compte du cas o� la loupe serait affich�e.
     */
    public void repaint(final long tm, final int x, final int y,
                        final int width, final int height) {
        super.repaint(tm, x, y, width, height);
        if (magnifier!=null && magnifier.intersects(x,y,width,height)) {
            // Si la partie � dessiner est � l'int�rieur de la loupe,
            // le fait que la loupe fasse un agrandissement nous oblige
            // � redessiner un peu plus que ce qui avait �t� demand�.
            repaintMagnifier();
        }
    }

    /**
     * D�clare que la loupe a besoin d'�tre red�ssin�e. Une commande
     * {@link #repaint()} sera envoy�e avec comme coordonn�es les limites
     * de la loupe (en tenant compte de sa bordure).
     */
    private void repaintMagnifier() {
        final Rectangle bounds=magnifier.getBounds();
        bounds.x      -= 4;
        bounds.y      -= 4;
        bounds.width  += 8;
        bounds.height += 8;
        super.repaint(0, bounds.x, bounds.y, bounds.width, bounds.height);
    }

    /**
     * Paints the magnifier. This method is invoked after
     * {@link #paintComponent(Graphics2D)} if a magnifier
     * is visible.
     */
    protected void paintMagnifier(final Graphics2D graphics) {
        final double centerX = magnifier.getCenterX();
        final double centerY = magnifier.getCenterY();
        final Stroke  stroke =  graphics.getStroke();
        final Paint    paint =  graphics.getPaint();
        graphics.setStroke(new BasicStroke(6));
        graphics.setColor (magnifierBorder);
        graphics.draw     (magnifier);
        graphics.setStroke(stroke);
        graphics.clip     (magnifier); // Coordonn�es en pixels!
        graphics.setColor (magnifierColor);
        graphics.fill     (magnifier.getBounds2D());
        graphics.setPaint (paint);
        graphics.translate(+centerX, +centerY);
        graphics.scale    (magnifierPower, magnifierPower);
        graphics.translate(-centerX, -centerY);
        // Note: les transformations effectu�es ici doivent �tre identiques
        //       � celles qui sont faites dans {@link #pixelToLogical}.
        paintComponent    (graphics);
    }

    /**
     * Paints this component. Subclass must override this method in order to
     * drawn the <code>ZoomPane</code> content. For must implementations, the
     * first line in this method will be
     *
     * <code>graphics.transform({@link #zoom})</code>.
     */
    protected abstract void paintComponent(final Graphics2D graphics);

    /**
     * Prints this component. The default implementation
     * invokes {@link #paintComponent(Graphics2D)}.
     */
    protected void printComponent(final Graphics2D graphics) {
        paintComponent(graphics);
    }

    /**
     * Paints this component. This method is declared <code>final</code>
     * in order to avoir unintentional overriding. Override
     * {@link #paintComponent(Graphics2D)} instead.
     */
    protected final void paintComponent(final Graphics graphics) {
        flag=IS_PAINTING;
        super.paintComponent(graphics);
        /*
         * La m�thode <code>JComponent.paintComponent(...)</code> cr�e un objet <code>Graphics2D</code>
         * temporaire, puis appelle <code>ComponentUI.update(...)</code> avec en param�tre ce graphique.
         * Cette m�thode efface le fond de l'�cran, puis appelle <code>ComponentUI.paint(...)</code>.
         * Or, cette derni�re a �t� red�finie plus haut (notre objet {@link #UI}) de sorte qu'elle
         * appelle elle-m�me {@link #paintComponent(Graphics2D)}. Un chemin compliqu�, mais on a pas
         * tellement le choix et c'est somme toute assez efficace.
         */
        if (magnifier!=null) {
            flag=IS_PAINTING_MAGNIFIER;
            super.paintComponent(graphics);
        }
    }

    /**
     * Prints this component. This method is declared <code>final</code>
     * in order to avoir unintentional overriding. Override
     * {@link #printComponent(Graphics2D)} instead.
     */
    protected final void printComponent(final Graphics graphics) {
        flag=IS_PRINTING;
        super.paintComponent(graphics);
        /*
         * Ne pas appeller 'super.printComponent' parce qu'on ne
         * veut pas qu'il appelle notre 'paintComponent' ci-haut.
         */
    }

    /**
     * Retourne la dimension (en pixels) qu'aurait <code>ZoomPane</code> s'il
     * affichait la totalit� de la r�gion {@link #getArea} avec le zoom courant
     * ({@link #zoom}). Cette m�thode est pratique pour d�terminer les valeurs
     * maximales � affecter aux barres de d�filement. Par exemple la barre
     * horizontale pourrait couvrir la plage <code>[0..viewSize.width]</code>
     * tandis que la barre verticale pourrait couvrir la plage
     * <code>[0..viewSize.height]</code>.
     */
    private final Dimension getViewSize() {
        if (!visibleArea.isEmpty()) {
            Rectangle2D area=getArea();
            if (isValid(area)) {
                area=XAffineTransform.transform(zoom, area, null);
                return new Dimension((int) Math.rint(area.getWidth()),
                                     (int) Math.rint(area.getHeight()));
            }
            return getSize();
        }
        return new Dimension(DEFAULT_SIZE, DEFAULT_SIZE);
    }

    /**
     * Retourne les marges de cette composante. Cette m�thode fonctionne comme
     * <code>super.getInsets(insets)</code>, mais accepte un argument nul. Cette
     * m�thode peut �tre red�finie si on veut effectuer les zooms sur une
     * portion du graphique plut�t que sur l'ensemble.
     */
    public Insets getInsets(final Insets insets) {
        return super.getInsets((insets!=null) ? insets : new Insets(0,0,0,0));
    }

    /**
     * Retourne les marges de cette composante. Cette m�thode est d�clar�e final
     * afin d'�viter toute confusion. Si vous voulez retourner d'autres marges,
     * il faut red�finir {@link #getInsets(Insets)}.
     */
    public final Insets getInsets() {
        return getInsets(null);
    }

    /**
     * Informe <code>ZoomPane</code> que l'interface GUI a chang�.
     * L'utilisateur n'a pas � appeler cette m�thode directement.
     */
    public void updateUI() {
        navigationPopupMenu=null;
        super.updateUI();
        setUI(UI);
    }

    /**
     * Invoked when an affine transform that should be invertible is not.
     * Default implementation log the stack trace and reset the zoom.
     *
     * @param methodName The caller's method name.
     * @param exception  The exception.
     */
    private void unexpectedException(final String methodName,
                                     final NoninvertibleTransformException exception) {
        zoom.setToIdentity();
        Utilities.unexpectedException("org.geotools.gui",
                                      "org.geotools.swing.ZoomPane",
                                      methodName, exception);
    }

    /**
     * Invoked when an unexpected exception occured.
     * Default implementation log the stack trace.
     *
     * @param methodName The caller's method name.
     * @param exception  The exception.
     */
    private static void unexpectedException(final String methodName,
                                            final RuntimeException exception) {
        Utilities.unexpectedException("org.geotools.gui",
                                      "org.geotools.swing.ZoomPane",
                                      methodName, exception);
    }

    /**
     * Convenience method logging an area setting from the
     * <code>ZoomPane</code> class. This method is invoked
     * from {@link #setPreferredArea} and {@link #setVisibleArea}.
     *
     * @param methodName The caller's method name (e.g. <code>"setArea"</code>).
     * @param       area The coordinates to log (may be <code>null</code>).
     */
    private static void log(final String methodName, final Rectangle2D area) {
        log("org.geotools.swing.ZoomPane", methodName, area);
    }

    /**
     * Convenience method for logging events related to area setting.
     * Events are logged in the <code>"org.geotools.gui"</code> logger
     * with {@link Level#FINE}. <code>ZoomPane</code> invokes this method
     * for logging any [@link #setPreferredArea} and {@link #setVisibleArea}
     * invocations. Subclasses may invokes this method for logging some other
     * kinds of area changes.
     *
     * @param  className The fully qualified caller's class name
     *                   (e.g. <code>"org.geotools.swing.ZoomPane"</code>).
     * @param methodName The caller's method name (e.g. <code>"setArea"</code>).
     * @param       area The coordinates to log (may be <code>null</code>).
     */
    static void log(final String className,
                    final String methodName,
                    final Rectangle2D area) {
        final Double[] areaBounds;
        if (area!=null) {
            areaBounds = new Double[] {
                new Double(area.getMinX()), new Double(area.getMaxX()),
                new Double(area.getMinY()), new Double(area.getMaxY())
            };
        } else {
            areaBounds = new Double[4];
            Arrays.fill(areaBounds, new Double(Double.NaN));
        }
        final Resources resources = Resources.getResources(null);
        final LogRecord record = resources.getLogRecord(Level.FINE,
                                                        ResourceKeys.RECTANGLE_$4,
                                                        areaBounds);
        record.setSourceClassName ( className);
        record.setSourceMethodName(methodName);
        Logger.getLogger("org.geotools.gui").log(record);
    }

    /**
     * V�rifie si le rectangle <code>rect</code> est valide. Le rectangle sera
     * consid�r� invalide si sa largeur ou sa hauteur est inf�rieure ou �gale �
     * 0, ou si une de ses coordonn�es est infinie ou NaN.
     */
    private static boolean isValid(final Rectangle2D rect) {
        if (rect==null) {
            return false;
        }
        final double x=rect.getX();
        final double y=rect.getY();
        final double w=rect.getWidth();
        final double h=rect.getHeight();
        return (x>Double.NEGATIVE_INFINITY && x<Double.POSITIVE_INFINITY &&
                y>Double.NEGATIVE_INFINITY && y<Double.POSITIVE_INFINITY &&
                w>0                        && w<Double.POSITIVE_INFINITY &&
                h>0                        && h<Double.POSITIVE_INFINITY);
    }
}
