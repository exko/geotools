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
package org.geotools.gui.swing.propertyedit;

import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import org.geotools.gui.swing.propertyedit.model.MultiTreeNode;
import org.geotools.gui.swing.propertyedit.model.MultiTreeRenderer;

/**
 * Multiproperty panel
 *
 * @author  Johann Sorel
 */
public abstract class MultiPropertyPanel extends javax.swing.JPanel implements PropertyPane {

    private List<PropertyPane> panels = new ArrayList<PropertyPane>();
    private PropertyPane active = null;
    private DefaultTreeModel model = new DefaultTreeModel(new DefaultMutableTreeNode("Styles"));

    /** Creates new form MultiPropertyPanel */
    public MultiPropertyPanel() {
        super();
        initComponents();

        pan_subprop.setLayout(new GridLayout(1, 1));

        tree.setModel(model);
        tree.setRootVisible(false);
        tree.setCellRenderer(new MultiTreeRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        tree.addTreeSelectionListener(new TreeSelectionListener() {

                    public void valueChanged(TreeSelectionEvent e) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
                        final Object obj = node.getUserObject();

                        if (obj != null) {
                            if (obj instanceof PropertyPane) {
                                setSelectedPropertyPanel((PropertyPane) obj);
                            }
                        }
                    }
                });

    }

    public void addPropertyPanel(PropertyPane panel) {
        if (panel != null) {

            if (!panels.contains(panel)) {
                DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
                MultiTreeNode node = new MultiTreeNode(panel);
                root.add(node);

                model.reload();
                tree.expandAll();

                panels.add(panel);

                if (panels.size() == 1) {
                    setSelectedPropertyPanel(panel);
                }
            }


        }
    }

    /*public boolean removePropertyPanel(PropertyPanel panel) {
    if (panel != null) {
    return panels.remove(panel);
    }
    return false;
    }*/
    public boolean setSelectedPropertyPanel(final PropertyPane panel) {

        if (panel != null) {
            if (panels.contains(panel)) {
                active = panel;
                SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                pan_subprop.removeAll();
                                pan_subprop.add(panel.getComponent());
                                pan_subprop.revalidate();
                                pan_subprop.repaint();
                            }
                        });

                return true;
            }
        }

        return false;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jXTitledPanel1 = new org.jdesktop.swingx.JXTitledPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tree = new org.jdesktop.swingx.JXTree();
        jPanel1 = new javax.swing.JPanel();
        pan_subprop = new javax.swing.JPanel();

        jSplitPane1.setBorder(null);
        jSplitPane1.setDividerSize(4);

        jXTitledPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/geotools/gui/swing/propertyedit/Bundle"); // NOI18N
        jXTitledPanel1.setTitle(bundle.getString("editor")); // NOI18N
        jXTitledPanel1.setTitleFont(jXTitledPanel1.getTitleFont().deriveFont(jXTitledPanel1.getTitleFont().getStyle() | java.awt.Font.BOLD));
        jXTitledPanel1.getContentContainer().setLayout(new java.awt.BorderLayout());

        jScrollPane1.setBorder(null);
        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane1.setViewportBorder(null);
        jScrollPane1.setMinimumSize(new java.awt.Dimension(152, 202));
        jScrollPane1.setPreferredSize(new java.awt.Dimension(152, 202));

        tree.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        tree.setMinimumSize(new java.awt.Dimension(150, 200));
        tree.setPreferredSize(new java.awt.Dimension(150, 200));
        jScrollPane1.setViewportView(tree);

        jXTitledPanel1.getContentContainer().add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jSplitPane1.setLeftComponent(jXTitledPanel1);

        jPanel1.setLayout(new java.awt.BorderLayout());

        org.jdesktop.layout.GroupLayout pan_subpropLayout = new org.jdesktop.layout.GroupLayout(pan_subprop);
        pan_subprop.setLayout(pan_subpropLayout);
        pan_subpropLayout.setHorizontalGroup(
            pan_subpropLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 317, Short.MAX_VALUE)
        );
        pan_subpropLayout.setVerticalGroup(
            pan_subpropLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 280, Short.MAX_VALUE)
        );

        jPanel1.add(pan_subprop, java.awt.BorderLayout.CENTER);

        jSplitPane1.setRightComponent(jPanel1);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 475, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 280, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    public void setTarget(Object target) {
        for (PropertyPane pan : panels) {
            pan.setTarget(target);
        }
    }

    public void apply() {
        for (PropertyPane pan : panels) {

            if (pan.equals(active)) {
                pan.apply();
            } else {
                pan.reset();
            }
        }
    }

    public void reset() {
        for (PropertyPane pan : panels) {
            pan.reset();
        }
    }

    public abstract String getTitle();

    public abstract ImageIcon getIcon();

    public abstract String getToolTip();

    public Component getComponent() {
        return this;
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane1;
    private org.jdesktop.swingx.JXTitledPanel jXTitledPanel1;
    private javax.swing.JPanel pan_subprop;
    private org.jdesktop.swingx.JXTree tree;
    // End of variables declaration//GEN-END:variables
}
