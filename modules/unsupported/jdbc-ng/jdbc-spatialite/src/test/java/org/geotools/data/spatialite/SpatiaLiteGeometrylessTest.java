package org.geotools.data.spatialite;

import org.geotools.jdbc.JDBCGeometrylessTest;
import org.geotools.jdbc.JDBCGeometrylessTestSetup;

public class SpatiaLiteGeometrylessTest extends JDBCGeometrylessTest {

    @Override
    protected JDBCGeometrylessTestSetup createTestSetup() {
        return new SpatiaLiteGeometrylessTestSetup();
    }

}