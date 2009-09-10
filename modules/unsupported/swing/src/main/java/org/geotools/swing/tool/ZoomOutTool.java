/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */

package org.geotools.swing.tool;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.ResourceBundle;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.swing.JMapPane;
import org.geotools.swing.event.MapMouseEvent;

/**
 * A zoom-out tool for JMapPane.
 * <p>
 * For mouse clicks, the display will be zoomed-out such that the 
 * map centre is the position of the mouse click and the map
 * width and height are calculated as:
 * <pre>   {@code len = len.old * z} </pre>
 * where {@code z} is the linear zoom increment (>= 1.0)
 * 
 * @author Michael Bedward
 * @since 2.6
 * @source $URL$
 * @version $Id$
 */
public class ZoomOutTool extends AbstractZoomTool {

    private static final ResourceBundle stringRes = ResourceBundle.getBundle("org/geotools/swing/widget");
    
    public static final String TOOL_NAME = stringRes.getString("tool_name_zoom_out");
    public static final String TOOL_TIP = stringRes.getString("tool_tip_zoom_out");
    public static final String CURSOR_IMAGE = "/org/geotools/swing/icons/mActionZoomOut.png";
    public static final Point CURSOR_HOTSPOT = new Point(14, 9);
    public static final String ICON_IMAGE = "/org/geotools/swing/icons/mActionZoomOut.png";
    
    private Cursor cursor;
    private Icon icon;
    
    /**
     * Constructor
     *
     * @param mapPane the map mapPane that this tool is to work with
     */
    public ZoomOutTool(JMapPane pane) {
        super(pane);
        icon = new ImageIcon(getClass().getResource(ICON_IMAGE));

        Toolkit tk = Toolkit.getDefaultToolkit();
        ImageIcon imgIcon = new ImageIcon(getClass().getResource(CURSOR_IMAGE));
        cursor = tk.createCustomCursor(imgIcon.getImage(), CURSOR_HOTSPOT, TOOL_NAME);
    }
    
    /**
     * Zoom out by the currently set increment, with the map
     * centred at the location (in world coords) of the mouse
     * click
     */
    @Override
    public void onMouseClicked(MapMouseEvent pme) {
        Rectangle paneArea = mapPane.getVisibleRect();
        DirectPosition2D mapPos = pme.getMapPosition();

        double scale = mapPane.getWorldToScreenTransform().getScaleX();
        double newScale = scale / zoom;

        DirectPosition2D corner = new DirectPosition2D(
                mapPos.getX() - 0.5d * paneArea.getWidth() / newScale,
                mapPos.getY() + 0.5d * paneArea.getHeight() / newScale);
        
        Envelope2D newMapArea = new Envelope2D();
        newMapArea.setFrameFromCenter(mapPos, corner);
        mapPane.setEnvelope(newMapArea);
    }

    /**
     * Get the name assigned to this tool
     * @return "Zoom out"
     */
    @Override
    public String getName() {
        return TOOL_NAME;
    }

    /**
     * Get the mouse cursor for this tool
     */
    public Cursor getCursor() {
        return cursor;
    }
    
    /**
     * Get the icon for this tool
     */
    public Icon getIcon() {
        return icon;
    }

    /**
     * Returns false to indicate that this tool does not draw a box
     * on the map display when the mouse is being dragged
     */
    @Override
    public boolean drawDragBox() {
        return false;
    }
}
