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
import org.geotools.swing.tool.PanTool;

/**
 * An action for connect a control (probably a JButton) to
 * the PanTool for panning the map with mouse drags.
 * 
 * @author Michael Bedward
 * @since 2.6
 * @source $URL$
 * @version $Id$
 */
public class PanAction extends MapAction {

    /**
     * Constructor - when used with a JButton the button will
     * display a small icon only
     * 
     * @param pane the map pane being serviced by this action
     */
    public PanAction(JMapPane pane) {
        this(pane, false);
    }

    /**
     * Constructor
     * 
     * @param pane the map pane being serviced by this action
     * @param showToolName set to true for the control to display the tool name
     */
    public PanAction(JMapPane pane, boolean showToolName) {
        String toolName = showToolName ? PanTool.TOOL_NAME : null;
        super.init(pane, toolName, PanTool.TOOL_TIP, PanTool.ICON_IMAGE);
    }
    
    /**
     * Called when the associated control is activated. Leads to the
     * map pane's cursor tool being set to a PanTool object
     */
    public void actionPerformed(ActionEvent e) {
        pane.setCursorTool(new PanTool(pane));
    }

}
