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
package org.geotools.gui.swing.style.sld;

import java.awt.Component;
import javax.swing.JDialog;
import org.geotools.gui.swing.style.StyleElementEditor;
import org.geotools.map.MapLayer;
import org.geotools.styling.Fill;
import org.geotools.styling.StyleBuilder;

/**
 * Fill panel
 * 
 * @author  Johann Sorel
 */
public class JFillPane extends javax.swing.JPanel implements StyleElementEditor<Fill>{

    private MapLayer layer = null;
    private Fill fill = null;

    /** 
     * Creates new form JFillPanel 
     */
    public JFillPane() {
        initComponents();
        
        guiColor.setType(JExpressionPane.EXP_TYPE.COLOR);
        guiAlpha.setType(JExpressionPane.EXP_TYPE.OPACITY);
    }

    public void setLayer(MapLayer layer) {
        this.layer = layer;
        guiColor.setLayer(layer);
        guiAlpha.setLayer(layer);
    }

    public MapLayer getLayer(){
        return layer;
    }
    
    public void setEdited(Fill fill) {
        this.fill = fill;
        if (fill != null) {
            guiColor.setExpression(fill.getColor());
            guiAlpha.setExpression(fill.getOpacity());
            //handle by a button
            //fill.getGraphicFill();
        }
    }

    public Fill getEdited() {

        if (fill == null) {
            fill = new StyleBuilder().createFill();
        }

        apply();
        return fill;
    }

    public void apply() {
        if (fill != null) {
            fill.setColor(guiColor.getExpression());
            fill.setOpacity(guiAlpha.getExpression());
        }
    }

    public Component getComponent(){
        return this;
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        butFill = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        guiAlpha = new org.geotools.gui.swing.style.sld.JExpressionPane();
        lbl2 = new javax.swing.JLabel();
        guiColor = new org.geotools.gui.swing.style.sld.JExpressionPane();
        lbl_color1 = new javax.swing.JLabel();

        setOpaque(false);

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/geotools/gui/swing/style/sld/Bundle"); // NOI18N
        butFill.setText(bundle.getString("fill")); // NOI18N
        butFill.setBorderPainted(false);
        butFill.setPreferredSize(new java.awt.Dimension(49, 22));
        butFill.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butFillActionPerformed(evt);
            }
        });

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel3.setText(bundle.getString("graphic")); // NOI18N

        lbl2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lbl2.setText(bundle.getString("opacity")); // NOI18N

        lbl_color1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lbl_color1.setText(bundle.getString("color")); // NOI18N

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(lbl_color1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(guiColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
            .add(layout.createSequentialGroup()
                .add(lbl2)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(guiAlpha, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
            .add(layout.createSequentialGroup()
                .add(jLabel3)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(butFill, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        layout.linkSize(new java.awt.Component[] {jLabel3, lbl2, lbl_color1}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.linkSize(new java.awt.Component[] {guiAlpha, guiColor}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                    .add(lbl_color1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(guiColor, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(lbl2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(guiAlpha, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(butFill, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
        );
    }// </editor-fold>//GEN-END:initComponents
    private void butFillActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butFillActionPerformed
        JDialog dia = new JDialog();

        JGraphicPane pane = new JGraphicPane();
        pane.setLayer(layer);

        if (fill != null) {
            pane.setEdited(fill.getGraphicFill());
        }

        dia.setContentPane(pane);
        dia.pack();
        dia.setLocationRelativeTo(butFill);
        dia.setModal(true);
        dia.setVisible(true);

        if (fill == null) {
            fill = new StyleBuilder().createFill();
        }
        fill.setGraphicFill(pane.getEdited());
    }//GEN-LAST:event_butFillActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton butFill;
    private org.geotools.gui.swing.style.sld.JExpressionPane guiAlpha;
    private org.geotools.gui.swing.style.sld.JExpressionPane guiColor;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel lbl2;
    private javax.swing.JLabel lbl_color1;
    // End of variables declaration//GEN-END:variables
}
