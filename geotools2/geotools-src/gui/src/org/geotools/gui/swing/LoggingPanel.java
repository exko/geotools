/*
 * Geotools - OpenSource mapping toolkit
 * (C) 2002, Centre for Computational Geography
 * (C) 2002, Institut de Recherche pour le D�veloppement
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

// Swing dependencies
import javax.swing.JTable;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.table.TableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ChangeEvent;

// AWT
import java.awt.Color;
import java.awt.Frame;
import java.awt.Dialog;
import java.awt.Component;
import java.awt.BorderLayout;

// Logging
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

// Collections
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

// Resources
import org.geotools.resources.XArray;
import org.geotools.resources.gui.Resources;
import org.geotools.resources.gui.ResourceKeys;
import org.geotools.resources.SwingUtilities;


/**
 * A panel displaying logging messages. The windows displaying Geotools's logging messages
 * can be constructed with the following code:
 *
 * <blockquote>
 * new LoggingPanel("org.geotools").{@link #show(Component) show}(null);
 * </blockquote>
 *
 * This panel is initially set to listen to messages of level {@link Level#CONFIG} or higher.
 * This level can be changed with <code>{@link #getHandler}.setLevel(aLevel)</code>.
 *
 * @version $Id: LoggingPanel.java,v 1.4 2002/09/02 13:17:47 desruisseaux Exp $
 * @author Martin Desruisseaux
 */
public class LoggingPanel extends JPanel {
    /**
     * The model for this component.
     */
    private final LoggingTableModel model = new LoggingTableModel();

    /**
     * The table for displaying logging messages.
     */
    private final JTable table = new JTable(model);

    /**
     * The levels for colors enumerated in <code>levelColors</code>. This array
     * <strong>must</strong> be in increasing order. Logging messages of level
     * <code>levelValues[i]</code> or higher will be displayed with foreground
     * color <code>levelColors[i*2]</code> and background color <code>levelColors[i*2+1]</code>.
     *
     * @see Level#intValue
     * @see #getForeground(Level)
     * @see #getBackground(Level)
     */
    private int[] levelValues = new int[0];

    /**
     * Pairs of foreground and background colors to use for displaying logging messages.
     * Logging messages of level <code>levelValues[i]</code> or higher will be displayed
     * with foreground color <code>levelColors[i*2]</code> and background color
     * <code>levelColors[i*2+1]</code>.
     *
     * @see #getForeground(Level)
     * @see #getBackground(Level)
     */
    private final List levelColors = new ArrayList();

    /**
     * Constructs a new logging panel. This panel is not registered to any logger.
     * Registration can be done with the following code:
     *
     * <blockquote><pre>
     * logger.{@link Logger#addHandler addHandler}({@link #getHandler});
     * </pre></blockquote>
     */
    public LoggingPanel() {
        super(new BorderLayout());
        table.setShowGrid(false);
        table.setCellSelectionEnabled(false);
        table.setGridColor(Color.LIGHT_GRAY);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setDefaultRenderer(Object.class, new CellRenderer());

        if (true) {
            int width = 300;
            final TableColumnModel columns = table.getColumnModel();
            for (int i=model.getColumnCount(); --i>=0;) {
                columns.getColumn(i).setPreferredWidth(width);
                width = 80;
            }
        }

        final JScrollPane scroll = new JScrollPane(table);
        new AutoScroll(scroll.getVerticalScrollBar().getModel());
        add(scroll, BorderLayout.CENTER);

        setLevelColor(Level.CONFIG,  new Color(0, 128, 0), null);
        setLevelColor(Level.WARNING, Color.RED,            null);
        setLevelColor(Level.SEVERE,  Color.WHITE,     Color.RED);
    }

    /**
     * Constructs a new logging panel and register it to the specified logger.
     *
     * @param logger The logger to listen to, or <code>null</code> for the root logger.
     */
    public LoggingPanel(Logger logger) {
        this();
        if (logger == null) {
            logger = Logger.getLogger("");
        }
        logger.addHandler(getHandler());
    }

    /**
     * Construct a logging panel and register it to the specified logger.
     *
     * @param logger The logger name to listen to, or <code>null</code> for the root logger.
     */
    public LoggingPanel(final String logger) {
        this(Logger.getLogger(logger!=null ? logger : ""));
    }

    /**
     * Returns the logging handler.
     */
    public Handler getHandler() {
        return model;
    }

    /**
     * Returns the capacity. This is the maximum number of {@link LogRecord}s the handler
     * can memorize. If more messages are logged, then the earliest messages will be discarted.
     */
    public int getCapacity() {
        return model.getCapacity();
    }

    /**
     * Set the capacity. This is the maximum number of {@link LogRecord}s the handler can
     * memorize. If more messages are logged, then the earliest messages will be discarted.
     */
    public void setCapacity(final int capacity) {
        model.setCapacity(capacity);
    }

    /**
     * Returns the foreground color for the specified log record. This method is invoked at
     * rendering time for every cell in the table's "message" column. The default implementation
     * returns a color based on the record's level, using colors set with {@link #setLevelColor}.
     *
     * @param  record The record to get the foreground color.
     * @return The foreground color for the specified record,
     *         or <code>null</code> for the default color.
     */
    public Color getForeground(final LogRecord record) {
        return getColor(record, 0);
    }

    /**
     * Returns the background color for the specified log record. This method is invoked at
     * rendering time for every cell in the table's "message" column. The default implementation
     * returns a color based on the record's level, using colors set with {@link #setLevelColor}.
     *
     * @param  record The record to get the background color.
     * @return The background color for the specified record,
     *         or <code>null</code> for the default color.
     */
    public Color getBackground(final LogRecord record) {
        return getColor(record, 1);
    }

    /**
     * Returns the foreground or background color for the specified record.
     *
     * @param  record The record to get the color.
     * @param  offset 0 for the foreground color, or 1 for the background color.
     * @return The color for the specified record, or <code>null</code> for the default color.
     */
    private Color getColor(final LogRecord record, final int offset) {
        int i = Arrays.binarySearch(levelValues, record.getLevel().intValue());
        if (i < 0) {
            i = ~i - 1; // "~" is the tild symbol, not minus.
            if (i < 0) {
                return null;
            }
        }
        return (Color) levelColors.get(i*2 + offset);
    }

    /**
     * Set the foreground and background colors for messages of the specified level.
     * The specified colors will apply on any messages of level <code>level</code> or
     * greater, up to the next level set with an other call to <code>setLevelColor(...)</code>.
     *
     * @param level       The minimal level to set color for.
     * @param foreground  The foreground color, or <code>null</code> for the default color.
     * @param background  The background color, or <code>null</code> for the default color.
     */
    public void setLevelColor(final Level level, final Color foreground, final Color background) {
        final int value = level.intValue();
        int i = Arrays.binarySearch(levelValues, value);
        if (i >= 0) {
            i *= 2;
            levelColors.set(i+0, foreground);
            levelColors.set(i+1, background);
        } else {
            i = ~i;
            levelValues = XArray.insert(levelValues, i, 1);
            levelValues[i] = value;
            i *= 2;
            levelColors.add(i+0, foreground);
            levelColors.add(i+1, background);
        }
        assert XArray.isSorted(levelValues);
        assert levelValues.length*2 == levelColors.size();
    }

    /**
     * Layout this component. This method give all the remaining space, if any,
     * to the last table's column. This column is usually the one with logging
     * messages.
     */
    public void doLayout() {
        final TableColumnModel model = table.getColumnModel();
        final int      messageColumn = model.getColumnCount()-1;
        Component parent = table.getParent();
        int delta = parent.getWidth();
        if ((parent=parent.getParent()) instanceof JScrollPane) {
            delta -= ((JScrollPane) parent).getVerticalScrollBar().getPreferredSize().width;
        }
        for (int i=0; i<messageColumn; i++) {
            delta -= model.getColumn(i).getWidth();
        }
        final TableColumn column = model.getColumn(messageColumn);
        if (delta > Math.max(column.getWidth(), column.getPreferredWidth())) {
            column.setPreferredWidth(delta);
        }
        super.doLayout();
    }

    /**
     * Convenience method showing this logging panel into a frame.
     * Different kinds of frame can be constructed according <code>owner</code> class:
     *
     * <ul>
     *   <li>If <code>owner</code> or one of its parent is a {@link JDesktopPane},
     *       then <code>panel</code> is added into a {@link JInternalFrame}.</li>
     *   <li>If <code>owner</code> or one of its parent is a {@link Frame} or a {@link Dialog},
     *       then <code>panel</code> is added into a {@link JDialog}.</li>
     *   <li>Otherwise, <code>panel</code> is added into a {@link JFrame}.</li>
     * </ul>
     *
     * @param  owner The owner, or <code>null</code> to show
     *         this logging panel in a top-level window.
     * @return The frame. May be a {@link JInternalFrame},
     *         a {@link JDialog} or a {@link JFrame}.
     */
    public Component show(final Component owner) {
        final Component frame = SwingUtilities.toFrame(owner, this,
                        Resources.format(ResourceKeys.EVENT_LOGGER));
        frame.setSize(750, 300);
        frame.setVisible(true);
        doLayout();
        return frame;
    }

    /**
     * Display cell contents. This class is used for changing
     * the cell's color according the log record level.
     */
    private final class CellRenderer extends DefaultTableCellRenderer
                                  implements TableColumnModelListener
    {
        /**
         * Default color for the foreground.
         */
        private Color foreground;

        /**
         * Default color for the background.
         */
        private Color background;

        /**
         * The index of messages column.
         */
        private int messageColumn;

        /**
         * The last row for which the side has been computed.
         */
        private int lastRow;

        /**
         * Construct a new cell renderer.
         */
        public CellRenderer() {
            foreground = super.getForeground();
            background = super.getBackground();
            table.getColumnModel().addColumnModelListener(this);
        }

        /**
         * Set the foreground color.
         */
        public void setForeground(final Color foreground) {
            super.setForeground(this.foreground=foreground);
        }

        /**
         * Set the background colior
         */
        public void setBackground(final Color background) {
            super.setBackground(this.background=background);
        }

        /**
         * Returns the component to use for painting the cell.
         */
        public Component getTableCellRendererComponent(final JTable  table,
                                                       final Object  value,
                                                       final boolean isSelected,
                                                       final boolean hasFocus,
                                                       final int     rowIndex,
                                                       final int     columnIndex)
        {
            Color foreground = this.foreground;
            Color background = this.background;
            final boolean isMessage = (columnIndex == messageColumn);
            if (!isMessage) {
                foreground = Color.GRAY;
            }
            else if (rowIndex >= 0) {
                final TableModel candidate = table.getModel();
                if (candidate instanceof LoggingTableModel) {
                    final LoggingTableModel model = (LoggingTableModel) candidate;
                    final LogRecord record = model.getLogRecord(rowIndex);
                    Color color;
                    color=LoggingPanel.this.getForeground(record); if (color!=null) foreground=color;
                    color=LoggingPanel.this.getBackground(record); if (color!=null) background=color;
                }
            }
            super.setBackground(background);
            super.setForeground(foreground);
            final Component component = super.getTableCellRendererComponent(table, value,
                                             isSelected, hasFocus, rowIndex, columnIndex);
            /*
             * If a new record is being painted and this new record is wider
             * than previous ones, then make the message column width larger.
             */
            if (isMessage) {
                if (rowIndex > lastRow) {
                    final int width = component.getPreferredSize().width + 15;
                    final TableColumn column = table.getColumnModel().getColumn(columnIndex);
                    if (width > column.getPreferredWidth()) {
                        column.setPreferredWidth(width);
                    }
                    if (rowIndex == lastRow+1) {
                        lastRow = rowIndex;
                    }
                }
            }
            return component;
        }

        /**
         * Invoked when the message column may have moved. This method update the
         * {@link #messageColumn} field, so that the message column will continue
         * to be paint with special colors.
         */
        private final void update() {
            messageColumn = table.convertColumnIndexToView(model.getColumnCount()-1);
        }
        
        public void columnAdded        (TableColumnModelEvent e) {update();}
        public void columnMarginChanged          (ChangeEvent e) {update();}
        public void columnMoved        (TableColumnModelEvent e) {update();}
        public void columnRemoved      (TableColumnModelEvent e) {update();}
        public void columnSelectionChanged(ListSelectionEvent e) {update();}
    }
}
