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
package org.geotools.gui.swing.filter;

import java.util.Collection;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.gui.swing.icon.IconBundle;
import org.geotools.gui.swing.misc.FilterToCQL;
import org.geotools.gui.swing.misc.FilterToCQLException;
import org.geotools.map.MapLayer;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;

/**
 * CQL filter panel
 * 
 * @author Johann Sorel
 *
 * @source $URL$
 */
public class JCQLFilterPanel extends javax.swing.JPanel implements FilterPanel{

    private static ResourceBundle bundle = ResourceBundle.getBundle("org/geotools/gui/swing/propertyedit/filterproperty/Bundle");
    private Filter filter = null;
    private MapLayer layer;

    /** Creates new form JCQLPropertyPanel */
    public JCQLFilterPanel() {
        initComponents();

        lst_basic.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                Object value = lst_basic.getSelectedValue();
                append(value.toString());
            }
        });

        lst_gis.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                Object value = lst_gis.getSelectedValue();
                append(value.toString());
            }
        });

        lst_field.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                Object value = lst_field.getSelectedValue();
                append(value.toString());
            }
        });

    }

    private void append(String val) {
        if (!txt_cql.getText().endsWith(val)) {

            if (!txt_cql.getText().endsWith(" ") && txt_cql.getText().length() > 0) {
                txt_cql.append(" ");
            }
            txt_cql.append(val);
        }
    }

    private Filter verifyQuery(String str) {

        try {
            Filter flt = CQL.toFilter(str);
            txt_error.setText(" ");
            return flt;
        } catch (CQLException e) {
            txt_error.setText(bundle.getString("cql_error"));
            return null;
        }
    }

    private void parse(Filter filter) {

        FilterToCQL visitor = new FilterToCQL();

        try {
            txt_cql.setText(visitor.encodeToString(layer.getQuery().getFilter()));
        } catch (FilterToCQLException e) {
            e.printStackTrace();
        }

    }

    private void parse(MapLayer layer) {
        lst_field.removeAll();

        Collection<PropertyDescriptor> col = layer.getFeatureSource().getSchema().getDescriptors();
        Iterator<PropertyDescriptor> it = col.iterator();

        PropertyDescriptor desc;
        Vector<String> vec = new Vector<String>();
        while (it.hasNext()) {
            desc = it.next();
            vec.add(desc.getName().toString());
        }


        lst_field.setListData(vec);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane4 = new javax.swing.JScrollPane();
        txt_cql = new javax.swing.JTextArea();
        jScrollPane1 = new javax.swing.JScrollPane();
        lst_field = new javax.swing.JList();
        jScrollPane2 = new javax.swing.JScrollPane();
        lst_gis = new javax.swing.JList();
        jScrollPane3 = new javax.swing.JScrollPane();
        lst_basic = new javax.swing.JList();
        jLabel2 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        txt_error = new javax.swing.JLabel();

        txt_cql.setColumns(20);
        txt_cql.setRows(5);
        jScrollPane4.setViewportView(txt_cql);

        lst_field.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(lst_field);

        lst_gis.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "CONTAINS(<ATTR1>,<POINT(1 2)>)", "CROSS(<ATTR1>,<LINESTRING(1 2, 10 15)>)", "INTERSECT(<ATTR1>,<GEOMETRYCOLLECTION (POINT (10 10),POINT (30 30),LINESTRING (15 15, 20 20))> )", "BBOX(<ATTR1>,<10>,<20>,<30>,<40>)", "DWITHIN(<ATTR1>, <POINT(1 2)>, <10>, <kilometers>)" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        lst_gis.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(lst_gis);

        lst_basic.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "AND", "OR", "=", "<", "<=", ">", ">=", "BETWEEN", "LIKE", "NOT LIKE", "IS NULL", "IS NOT NULL", "EXISTS", "DOES-NOT-EXIST", "BEFORE", "AFTER", "DURING" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        lst_basic.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane3.setViewportView(lst_basic);

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/geotools/gui/swing/propertyedit/filterproperty/Bundle"); // NOI18N
        jLabel2.setText(bundle.getString("cql_basic")); // NOI18N

        jLabel1.setText(bundle.getString("cql_advance")); // NOI18N

        jLabel4.setText(bundle.getString("cql_field")); // NOI18N

        txt_error.setText(" ");

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jScrollPane4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 424, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jLabel2)
                            .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 138, Short.MAX_VALUE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jLabel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 138, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 138, Short.MAX_VALUE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel4)
                            .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE)))
                    .add(txt_error, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 424, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(jLabel2)
                    .add(jLabel4))
                .add(4, 4, 4)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
                    .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
                    .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 106, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(txt_error)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JList lst_basic;
    private javax.swing.JList lst_field;
    private javax.swing.JList lst_gis;
    private javax.swing.JTextArea txt_cql;
    private javax.swing.JLabel txt_error;
    // End of variables declaration//GEN-END:variables
    
    public void setFilter(Filter filter) {
        if (filter == null) {
            throw new NullPointerException();
        }

        this.filter = filter;
        parse(filter);
    }

    public Filter getFilter() {
        Filter flt = verifyQuery(txt_cql.getText());

        if (flt == null) {
            flt = Filter.INCLUDE;
        }
        filter = flt;        
        return filter;
    }

    public void setLayer(MapLayer layer) {

        if (layer == null) {
            throw new NullPointerException();
        }

        this.layer = layer;
        parse(layer);
    }

    public MapLayer getLayer() {
        return layer;
    }
 
        
    public String getTitle() {
        return bundle.getString("cqlfilter");
    }

    public ImageIcon getIcon() {
        return IconBundle.getResource().getIcon("16_filter_cql");
    }

    public String getToolTip() {
        return null;
    }
       
    public JComponent getComponent() {
        return this;
    }
}
