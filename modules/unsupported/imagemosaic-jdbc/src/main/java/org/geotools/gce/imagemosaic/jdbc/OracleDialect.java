package org.geotools.gce.imagemosaic.jdbc;

public class OracleDialect extends DBDialect {
    public OracleDialect(Config config) {
        super(config);
    }

    protected String getDriverClassName() {
        return "oracle.jdbc.OracleDriver";
    }

    protected String getJDBCUrl(String host, Integer port, String dbName) {
        return "jdbc:oracle:thin:@" + host + ":" + port + ":" + dbName;
    }

    @Override
    protected String getDoubleSQLType() {
        return "DOUBLE PRECISION";
    }

    @Override
    protected String getRegisterSpatialStatement(String tn, String srs) {
        return "INSERT INTO user_sdo_geom_metadata (TABLE_NAME, COLUMN_NAME, DIMINFO, SRID )" +
        "VALUES('" + tn + "','" + config.getGeomAttributeNameInSpatialTable() +
        "'," +
        "MDSYS.SDO_DIM_ARRAY(MDSYS.SDO_DIM_ELEMENT('X',0,1000000,0.1),MDSYS.SDO_DIM_ELEMENT('Y',0,1000000,0.1))," +
        srs + ")";
    }

    @Override
    protected String getUnregisterSpatialStatement(String tn) {
        return "DELETE FROM user_sdo_geom_metadata WHERE TABLE_NAME='" + tn +
        "' AND COLUMN_NAME='" + config.getGeomAttributeNameInSpatialTable() +
        "'";
    }

    @Override
    protected String getBLOBSQLType() {
        return "BLOB";
    }

    @Override
    protected String getMulitPolygonSQLType() {
        return "MDSYS.SDO_GEOMETRY";
    }

    @Override
    protected String getCreateIndexStatement(String tn)
        throws Exception {
        return "CREATE INDEX IX_" + tn + " ON " + tn + "(" +
        getConfig().getGeomAttributeNameInSpatialTable() +
        ") INDEXTYPE IS MDSYS.SPATIAL_INDEX";
    }

    protected String getXMLConnectFragmentName() {
        return "connect.oracle.xml.inc";
    }
}