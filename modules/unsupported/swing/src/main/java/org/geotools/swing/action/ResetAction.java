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

package org.geotools.swing.action;

import java.awt.event.ActionEvent;
import org.geotools.swing.JMapPane;


/**
 * An action for connect a control (probably a JButton) to
 * the JMapPane.reset() method which sets the bounds of the
 * map area to include the full extent of all map layers
 * 
 * @author Michael Bedward
 * @since 2.6
 * @source $URL$
 * @version $Id$
 */
public class ResetAction extends MapAction {
    public static final String TOOL_NAME = java.util.ResourceBundle.getBundle("org/geotools/swing/widget").getString("tool_name_reset");
    public static final String TOOL_TIP = java.util.ResourceBundle.getBundle("org/geotools/swing/widget").getString("tool_tip_reset");
    public static final String ICON_IMAGE = "/org/geotools/swing/icons/mActionZoomFullExtent.png";
    
    /**
     * Constructor - when used with a JButton the button will
     * display a small icon only
     * 
     * @param pane the map pane being serviced by this action
     */
    public ResetAction(JMapPane pane) {
        this(pane, false);
    }

    /**
     * Constructor
     * 
     * @param pane the map pane being serviced by this action
     * @param showToolName set to true for the control to display the tool name
     */
    public ResetAction(JMapPane pane, boolean showToolName) {
        String toolName = showToolName ? TOOL_NAME : null;
        
        String iconImagePath = null;
        super.init(pane, toolName, TOOL_TIP, ICON_IMAGE);
    }
    
    /**
     * Called when the control is activated. Calls the map pane to reset the 
     * display 
     */
    public void actionPerformed(ActionEvent e) {
        pane.reset();
    }

}