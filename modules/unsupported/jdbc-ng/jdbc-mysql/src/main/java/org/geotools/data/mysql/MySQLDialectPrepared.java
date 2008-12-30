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
package org.geotools.data.mysql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.PreparedStatementSQLDialect;
import org.opengis.feature.type.GeometryDescriptor;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.WKBWriter;

/**
 * MySQL database dialect based on prepared statements.
 * 
 * @author Justin Deoliveira, OpenGEO
 *
 */
public class MySQLDialectPrepared extends PreparedStatementSQLDialect {

    MySQLDialect delegate;
    
    public MySQLDialectPrepared(JDBCDataStore dataStore) {
        super( dataStore );
        delegate = new MySQLDialect( dataStore );
    }
    
    @Override
    public String getNameEscape() {
        return delegate.getNameEscape();
    }

    @Override
    public String getGeometryTypeName(Integer type) {
        return delegate.getGeometryTypeName(type);
    }

    @Override
    public Integer getGeometrySRID(String schemaName, String tableName, String columnName,
        Connection cx) throws SQLException {
        return delegate.getGeometrySRID(schemaName, tableName, columnName, cx); 
    }
    
    @Override
    public void encodeColumnName(String raw, StringBuffer sql) {
        delegate.encodeColumnName(raw, sql);
    }

    @Override
    public void encodeColumnType(String sqlTypeName, StringBuffer sql) {
        delegate.encodeColumnType(sqlTypeName, sql);
    }

    @Override
    public void encodeGeometryColumn(GeometryDescriptor gatt, int srid,
            StringBuffer sql) {
        delegate.encodeGeometryColumn(gatt, srid, sql);
    }

    @Override
    public void registerClassToSqlMappings(Map<Class<?>, Integer> mappings) {
        delegate.registerClassToSqlMappings(mappings);
    }

    @Override
    public void registerSqlTypeToClassMappings(Map<Integer, Class<?>> mappings) {
        delegate.registerSqlTypeToClassMappings(mappings);
    }

    @Override
    public void registerSqlTypeNameToClassMappings(Map<String, Class<?>> mappings) {
        delegate.registerSqlTypeNameToClassMappings(mappings);
    }
    
    @Override
    public void registerSqlTypeToSqlTypeNameOverrides(
            Map<Integer, String> overrides) {
        delegate.registerSqlTypeToSqlTypeNameOverrides(overrides);
    }

    @Override
    public void encodePostCreateTable(String tableName, StringBuffer sql) {
        delegate.encodePostCreateTable(tableName, sql);
    }

    @Override
    public void encodePrimaryKey(String column, StringBuffer sql) {
        delegate.encodePrimaryKey(column, sql);
    }

    @Override
    public Object getNextAutoGeneratedValue(String schemaName,
            String tableName, String columnName, Connection cx)
            throws SQLException {
        return delegate.getNextAutoGeneratedValue(schemaName, tableName, columnName, cx);
    }
    
    @Override
    public void encodeGeometryEnvelope(String tableName, String geometryColumn,
            StringBuffer sql) {
        delegate.encodeGeometryEnvelope(tableName, geometryColumn, sql);
    }
    
    @Override
    public Envelope decodeGeometryEnvelope(ResultSet rs, int column,
            Connection cx) throws SQLException, IOException {
        return delegate.decodeGeometryEnvelope(rs, column, cx);
    }
    
    @Override
    public Geometry decodeGeometryValue(GeometryDescriptor descriptor,
            ResultSet rs, String column, GeometryFactory factory, Connection cx)
            throws IOException, SQLException {
        return delegate.decodeGeometryValue(descriptor, rs, column, factory, cx);
    }
    
    //
    // prepared statement api
    //
    @Override
    public void prepareGeometryValue(Geometry g, int srid, Class binding,
            StringBuffer sql) {
        if ( g != null ) {
            sql.append( "GeomFromWKB(?)");
            //sql.append( "GeomFromText(?)");            
        }
        else {
            super.prepareGeometryValue(g, srid, binding, sql);
        }
    }
    
    @Override
    public void setGeometryValue(Geometry g, int srid, Class binding,
            PreparedStatement ps, int column) throws SQLException {
        if ( g != null ) {
            ps.setBytes( column, new WKBWriter().write( g ) );
            //ps.setString( column, g.toText() );
        }
        else {
            ps.setNull(column, Types.OTHER);
        }
        
    }

   
}
