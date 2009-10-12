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
package org.geotools.gui.swing.datachooser;

import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.postgis.PostgisDataStoreFactory;
import org.geotools.gui.swing.datachooser.model.DBModel;
import org.geotools.gui.swing.icon.IconBundle;
import org.geotools.gui.swing.misc.Render.RandomStyleFactory;
import org.geotools.map.DefaultMapLayer;
import org.geotools.map.MapLayer;
import org.geotools.styling.Style;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * PostGIS databaseChooser
 * 
 * @author Johann Sorel
 *
 * @source $URL$
 */
public class JPostGISDataPanel extends javax.swing.JPanel implements DataPanel {

    private static ResourceBundle BUNDLE = ResourceBundle.getBundle("org/geotools/gui/swing/datachooser/Bundle");
    private DataStore store;
    private final Map params = new HashMap<String, Object>();

    /** Creates new form DefaultShapeTypeChooser */
    public JPostGISDataPanel() {
        initComponents();

        params.put("dbtype", "postgis");

        params.put(PostgisDataStoreFactory.HOST.key, PostgisDataStoreFactory.HOST.sample);
        params.put(PostgisDataStoreFactory.PORT.key, PostgisDataStoreFactory.PORT.sample);
        params.put(PostgisDataStoreFactory.SCHEMA.key, PostgisDataStoreFactory.SCHEMA.sample);
        params.put(PostgisDataStoreFactory.DATABASE.key, PostgisDataStoreFactory.DATABASE.sample);
        params.put(PostgisDataStoreFactory.USER.key, PostgisDataStoreFactory.USER.sample);
        params.put(PostgisDataStoreFactory.PASSWD.key, PostgisDataStoreFactory.PASSWD.sample);
        params.put(PostgisDataStoreFactory.MAXCONN.key, PostgisDataStoreFactory.MAXCONN.sample);
        params.put(PostgisDataStoreFactory.MINCONN.key, PostgisDataStoreFactory.MINCONN.sample);
        params.put(PostgisDataStoreFactory.NAMESPACE.key, PostgisDataStoreFactory.NAMESPACE.sample);
        params.put(PostgisDataStoreFactory.VALIDATECONN.key, PostgisDataStoreFactory.VALIDATECONN.sample);
        params.put(PostgisDataStoreFactory.ESTIMATEDEXTENT.key, PostgisDataStoreFactory.ESTIMATEDEXTENT.sample);
        params.put(PostgisDataStoreFactory.LOOSEBBOX.key, PostgisDataStoreFactory.LOOSEBBOX.sample);
        params.put(PostgisDataStoreFactory.WKBENABLED.key, PostgisDataStoreFactory.WKBENABLED.sample);

        setProperties(params);
        
        jtf_host.setToolTipText(PostgisDataStoreFactory.HOST.description.toString());
        jtf_port.setToolTipText(PostgisDataStoreFactory.PORT.description.toString());
        jtf_schema.setToolTipText(PostgisDataStoreFactory.SCHEMA.description.toString());
        jtf_database.setToolTipText(PostgisDataStoreFactory.DATABASE.description.toString());
        jtf_user.setToolTipText(PostgisDataStoreFactory.USER.description.toString());
        jtf_password.setToolTipText(PostgisDataStoreFactory.PASSWD.description.toString());
        jsp_max_connects.setToolTipText(PostgisDataStoreFactory.MAXCONN.description.toString());
        jsp_min_connects.setToolTipText(PostgisDataStoreFactory.MINCONN.description.toString());
        jtf_namespace.setToolTipText(PostgisDataStoreFactory.NAMESPACE.description.toString());
        chk_validate.setToolTipText(PostgisDataStoreFactory.VALIDATECONN.description.toString());
        chk_estimated.setToolTipText(PostgisDataStoreFactory.ESTIMATEDEXTENT.description.toString());
        chk_loose.setToolTipText(PostgisDataStoreFactory.LOOSEBBOX.description.toString());
        chk_wkb.setToolTipText(PostgisDataStoreFactory.WKBENABLED.description.toString());

        tab_table.setTableHeader(null);
        tab_table.setModel(new DBModel(tab_table));

    }

    public Map getProperties() {
        return params;
    }

    public void setProperties(Map map) {

        if(map == null){
            throw new NullPointerException();
        }
        
        Object val = null;
        
        val = map.get(PostgisDataStoreFactory.HOST.key);
        jtf_host.setText( (val == null) ? "" : val.toString());
        val = map.get(PostgisDataStoreFactory.PORT.key);
        jtf_port.setText((val == null) ? "" : val.toString());
        val = map.get(PostgisDataStoreFactory.SCHEMA.key);
        jtf_schema.setText((val == null) ? "" : val.toString());
        val = map.get(PostgisDataStoreFactory.DATABASE.key);
        jtf_database.setText((val == null) ? "" : val.toString());
        val = map.get(PostgisDataStoreFactory.USER.key);
        jtf_user.setText((val == null) ? "" : val.toString());
        val = map.get(PostgisDataStoreFactory.PASSWD.key);
        jtf_password.setText((val == null) ? "" : val.toString());
        val = map.get(PostgisDataStoreFactory.MAXCONN.key);
        jsp_max_connects.setValue((val == null) ? 10 : val);
        val = map.get(PostgisDataStoreFactory.MINCONN.key);
        jsp_min_connects.setValue((val == null) ? 4 : val);
        val = map.get(PostgisDataStoreFactory.NAMESPACE.key);
        jtf_namespace.setText((val == null) ? "" : val.toString());
        val = map.get(PostgisDataStoreFactory.VALIDATECONN.key);
        chk_validate.setSelected( (val == null) ? (Boolean) PostgisDataStoreFactory.VALIDATECONN.sample : (Boolean)val);
        val = map.get(PostgisDataStoreFactory.ESTIMATEDEXTENT.key);
        chk_estimated.setSelected((val == null) ? (Boolean) PostgisDataStoreFactory.ESTIMATEDEXTENT.sample : (Boolean)val);
        val = map.get(PostgisDataStoreFactory.LOOSEBBOX.key);
        chk_loose.setSelected( (val == null) ? (Boolean) PostgisDataStoreFactory.LOOSEBBOX.sample : (Boolean)val);
        val = map.get(PostgisDataStoreFactory.WKBENABLED.key);
        chk_wkb.setSelected( (val == null) ? (Boolean) PostgisDataStoreFactory.WKBENABLED.sample : (Boolean)val);
        
    }

    private void refreshTable() {

        if (store != null) {
            ((DBModel) tab_table.getModel()).clean();
            try {
                ((DBModel) tab_table.getModel()).add(store.getTypeNames());
            } catch (IOException ex) {
                System.out.println(ex);
            }
        }

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        but_refresh = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tab_table = new org.jdesktop.swingx.JXTable();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jtf_host = new javax.swing.JTextField();
        jtf_port = new javax.swing.JTextField();
        jtf_schema = new javax.swing.JTextField();
        jtf_database = new javax.swing.JTextField();
        jtf_user = new javax.swing.JTextField();
        jtf_password = new javax.swing.JPasswordField();
        jPanel2 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jtf_namespace = new javax.swing.JTextField();
        chk_validate = new javax.swing.JCheckBox();
        chk_wkb = new javax.swing.JCheckBox();
        chk_loose = new javax.swing.JCheckBox();
        chk_estimated = new javax.swing.JCheckBox();
        jsp_max_connects = new javax.swing.JSpinner();
        jsp_min_connects = new javax.swing.JSpinner();
        gui_progress = new javax.swing.JProgressBar();

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/geotools/gui/swing/datachooser/Bundle"); // NOI18N
        but_refresh.setText(bundle.getString("connect")); // NOI18N
        but_refresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                actionRefresh(evt);
            }
        });

        tab_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane1.setViewportView(tab_table);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("connection"))); // NOI18N

        jLabel1.setText(bundle.getString("host")); // NOI18N

        jLabel2.setText(bundle.getString("port")); // NOI18N

        jLabel3.setText(bundle.getString("schema")); // NOI18N

        jLabel4.setText(bundle.getString("database")); // NOI18N

        jLabel5.setText(bundle.getString("user")); // NOI18N

        jLabel6.setText(bundle.getString("password")); // NOI18N

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jtf_host, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jLabel2)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jtf_port, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 202, Short.MAX_VALUE))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jLabel3)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jtf_schema, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 183, Short.MAX_VALUE))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jLabel4)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jtf_database, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 176, Short.MAX_VALUE))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jLabel5)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jtf_user, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jLabel6)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jtf_password, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 173, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(jtf_host, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(jtf_port, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(jtf_schema, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel4)
                    .add(jtf_database, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel5)
                    .add(jtf_user, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel6)
                    .add(jtf_password, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("advanced"))); // NOI18N

        jLabel7.setText(bundle.getString("max_connects")); // NOI18N

        jLabel8.setText(bundle.getString("min_connects")); // NOI18N

        jLabel13.setText(bundle.getString("namespace")); // NOI18N

        chk_validate.setText(bundle.getString("validate_connects")); // NOI18N

        chk_wkb.setText(bundle.getString("wkb_enabled")); // NOI18N

        chk_loose.setText(bundle.getString("loose_bbox")); // NOI18N

        chk_estimated.setText(bundle.getString("estimated_extend")); // NOI18N

        jsp_max_connects.setModel(new javax.swing.SpinnerNumberModel(0, 0, 255, 1));

        jsp_min_connects.setModel(new javax.swing.SpinnerNumberModel(0, 0, 255, 1));

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(chk_estimated)
                        .addContainerGap())
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(chk_loose)
                        .addContainerGap())
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(chk_wkb)
                        .addContainerGap())
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(jPanel2Layout.createSequentialGroup()
                                .add(jLabel7)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jsp_max_connects, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 71, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(jPanel2Layout.createSequentialGroup()
                                .add(jLabel8)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jsp_min_connects)))
                        .addContainerGap(74, Short.MAX_VALUE))
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(chk_validate, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 234, Short.MAX_VALUE)
                        .add(20, 20, 20))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2Layout.createSequentialGroup()
                        .add(jLabel13)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jtf_namespace, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 163, Short.MAX_VALUE)
                        .addContainerGap())))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel7)
                    .add(jsp_max_connects, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel8)
                    .add(jsp_min_connects, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(chk_validate)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(chk_wkb)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(chk_loose)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(chk_estimated)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jLabel13)
                    .add(jtf_namespace, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        gui_progress.setString(bundle.getString("waiting")); // NOI18N
        gui_progress.setStringPainted(true);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(gui_progress, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 384, Short.MAX_VALUE)
                        .add(18, 18, 18)
                        .add(but_refresh))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 199, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                        .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                    .add(gui_progress, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(but_refresh, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    private void actionRefresh(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_actionRefresh

        params.put(PostgisDataStoreFactory.HOST.key, jtf_host.getText());
        params.put(PostgisDataStoreFactory.PORT.key, jtf_port.getText());
        params.put(PostgisDataStoreFactory.SCHEMA.key, jtf_schema.getText());
        params.put(PostgisDataStoreFactory.DATABASE.key, jtf_database.getText());
        params.put(PostgisDataStoreFactory.USER.key, jtf_user.getText());
        params.put(PostgisDataStoreFactory.PASSWD.key, new String(jtf_password.getPassword()));
        params.put(PostgisDataStoreFactory.MAXCONN.key, jsp_max_connects.getValue());
        params.put(PostgisDataStoreFactory.MINCONN.key, jsp_min_connects.getValue());
        params.put(PostgisDataStoreFactory.NAMESPACE.key, jtf_namespace.getText());
        params.put(PostgisDataStoreFactory.VALIDATECONN.key, chk_validate.isSelected());
        params.put(PostgisDataStoreFactory.ESTIMATEDEXTENT.key, chk_estimated.isSelected());
        params.put(PostgisDataStoreFactory.LOOSEBBOX.key, chk_loose.isSelected());
        params.put(PostgisDataStoreFactory.WKBENABLED.key, chk_wkb.isSelected());


        but_refresh.setEnabled(false);
        gui_progress.setString(BUNDLE.getString("connecting"));
        gui_progress.setIndeterminate(true);
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    store = DataStoreFinder.getDataStore(params);
                    refreshTable();                    
                    gui_progress.setString(BUNDLE.getString("waiting"));
                } catch (IOException ex) {
                    store = null;                    
                    gui_progress.setString(BUNDLE.getString("error"));
                }
            }
        };
        t.start();
        
        gui_progress.setIndeterminate(false);
        but_refresh.setEnabled(true);
        
    }//GEN-LAST:event_actionRefresh

    public ImageIcon getIcon() {
        return IconBundle.getResource().getIcon("16_database");
    }

    public String getTitle() {
        return BUNDLE.getString("postgis");
    }

    public Component getChooserComponent() {
        return this;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton but_refresh;
    private javax.swing.JCheckBox chk_estimated;
    private javax.swing.JCheckBox chk_loose;
    private javax.swing.JCheckBox chk_validate;
    private javax.swing.JCheckBox chk_wkb;
    private javax.swing.JProgressBar gui_progress;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSpinner jsp_max_connects;
    private javax.swing.JSpinner jsp_min_connects;
    private javax.swing.JTextField jtf_database;
    private javax.swing.JTextField jtf_host;
    private javax.swing.JTextField jtf_namespace;
    private javax.swing.JPasswordField jtf_password;
    private javax.swing.JTextField jtf_port;
    private javax.swing.JTextField jtf_schema;
    private javax.swing.JTextField jtf_user;
    private org.jdesktop.swingx.JXTable tab_table;
    // End of variables declaration//GEN-END:variables
    public MapLayer[] getLayers() {
        ArrayList<MapLayer> layers = new ArrayList<MapLayer>();
        RandomStyleFactory rsf = new RandomStyleFactory();

        if (store != null) {

            for (int i = 0; i < tab_table.getSelectedRows().length; i++) {
                try {
                    DBModel model = (DBModel) tab_table.getModel();
                    String name = (String) model.getValueAt(tab_table.getSelectedRows()[i], 0);
                    FeatureSource<SimpleFeatureType, SimpleFeature> fs = store.getFeatureSource(name);
                    Style style = rsf.createRandomVectorStyle(fs);

                    MapLayer layer = new DefaultMapLayer(fs, style);
                    layer.setTitle("postgis - " + name);
                    layers.add(layer);
                } catch (IOException ex) {
                    System.out.println(ex);
                }
            }

        }

        return layers.toArray(new MapLayer[layers.size()]);
    }
}
