package org.geotools.gce.imagemosaic.jdbc;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;

import org.geotools.geometry.GeneralEnvelope;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.text.MessageFormat;

import java.util.logging.Level;


class JDBCAccessMySql extends JDBCAccessBase {
    private String extentSelect = null;
    private String allSelect = null;
    private String allSelectJoined = null;
    private String gridSelect = null;
    private String gridSelectJoined = null;

    JDBCAccessMySql(Config config) throws IOException {
        super(config);
        initStatementStrings(config);
    }

    private void initStatementStrings(Config config) {
        String geomAttr = config.getGeomAttributeNameInSpatialTable();
        extentSelect = "select asbinary(envelope(" + geomAttr + ")) from {0}";

        String spatialSelectClause = "select s." +
            config.getKeyAttributeNameInSpatialTable() + "," +
            "asbinary(envelope(s." + geomAttr + "))";

        allSelect = spatialSelectClause + ",s." +
            config.getBlobAttributeNameInTileTable() + " from {0} s";
        allSelectJoined = spatialSelectClause + ",t." +
            config.getBlobAttributeNameInTileTable() +
            " from {0} s, {1} t  WHERE ";
        allSelectJoined += (" s." + config.getKeyAttributeNameInSpatialTable() +
        " = t." + config.getKeyAttributeNameInTileTable());

        String whereClause = "mbrIntersects(" + geomAttr + "," +
            "GeomFromWKB(?)) = 1";

        gridSelect = allSelect + " WHERE " + whereClause;
        gridSelectJoined = allSelectJoined + " AND " + whereClause;
    }

    @Override
    protected void setGridSelectParams(PreparedStatement s,
        GeneralEnvelope envelope, ImageLevelInfo li) throws SQLException {
        WKBWriter w = new WKBWriter();
        byte[] bytes = w.write(polyFromEnvelope(envelope));
        s.setBytes(1, bytes);

        //s.setInt(2, li.getSrsId()); not supported
    }

    @Override
    protected String getRandomTileStatement(ImageLevelInfo li) {
        if (li.isImplementedAsTableSplit()) {
            return MessageFormat.format(allSelectJoined,
                new Object[] { li.getSpatialTableName(), li.getTileTableName() });
        } else {
            return MessageFormat.format(allSelect,
                new Object[] { li.getSpatialTableName() });
        }
    }

    @Override
    protected String getGridSelectStatement(ImageLevelInfo li) {
        if (li.isImplementedAsTableSplit()) {
            return MessageFormat.format(gridSelectJoined,
                new Object[] { li.getSpatialTableName(), li.getTileTableName() });
        } else {
            return MessageFormat.format(gridSelect,
                new Object[] { li.getSpatialTableName() });
        }
    }

    @Override
    protected String getExtentSelectStatment(ImageLevelInfo li) {
        return MessageFormat.format(extentSelect,
            new Object[] { li.getSpatialTableName() });
    }

    @Override
    protected Envelope getExtent(ImageLevelInfo li, Connection con)
        throws SQLException, IOException {
        String extentSelect = getExtentSelectStatment(li);

        String statementString = MessageFormat.format(extentSelect,
                new Object[] { li.getSpatialTableName() });
        Envelope extent = null;
        PreparedStatement s = con.prepareStatement(statementString);
        ResultSet r = s.executeQuery();

        WKBReader reader = new WKBReader();

        while (r.next()) {
            byte[] bytes = r.getBytes(1);
            Geometry g;

            try {
                g = reader.read(bytes);
            } catch (ParseException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                throw new IOException(e.getMessage());
            }

            if (extent == null) {
                extent = g.getEnvelopeInternal();
            } else {
                extent.expandToInclude(g.getEnvelopeInternal());
            }
        }

        r.close();
        s.close();

        return extent;
    }

    @Override
    protected Envelope getEnvelopeFromResultSet(ResultSet r)
        throws SQLException {
        byte[] bytes = r.getBytes(2);
        WKBReader reader = new WKBReader();
        Geometry bbox = null;

        try {
            bbox = reader.read(bytes);
        } catch (ParseException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new SQLException(e.getMessage());
        }

        return bbox.getEnvelopeInternal();
    }

    @Override
    protected CoordinateReferenceSystem getCRS(ImageLevelInfo li, Connection con)
        throws IOException {
        return null;
    }
}
