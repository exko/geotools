package org.geotools.gce.imagemosaic.jdbc;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.net.URL;

import java.sql.SQLException;


public class OracleOnlineTest extends AbstractTest {
    static DBDialect dialect = null;

    public OracleOnlineTest(String test) {
        super(test);
    }

    @Override
    protected String getSrsId() {
        return "4326";
    }

    @Override
    public String getConfigUrl() {
        return "file:target/resources/oek.oracle.xml";
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();

        OracleOnlineTest test = new OracleOnlineTest("");

        if (test.checkPreConditions() == false) {
            return suite;
        }

        suite.addTest(new OracleOnlineTest("testScripts"));
        suite.addTest(new OracleOnlineTest("testGetConnection"));
        suite.addTest(new OracleOnlineTest("testDrop"));
        suite.addTest(new OracleOnlineTest("testCreate"));
        suite.addTest(new OracleOnlineTest("testImage1"));
        suite.addTest(new OracleOnlineTest("testFullExtent"));
        suite.addTest(new OracleOnlineTest("testNoData"));
        suite.addTest(new OracleOnlineTest("testPartial"));
        suite.addTest(new OracleOnlineTest("testVienna"));
        suite.addTest(new OracleOnlineTest("testViennaEnv"));
        suite.addTest(new OracleOnlineTest("testDrop"));
        suite.addTest(new OracleOnlineTest("testCreateJoined"));
        suite.addTest(new OracleOnlineTest("testImage1Joined"));
        suite.addTest(new OracleOnlineTest("testFullExtentJoined"));
        suite.addTest(new OracleOnlineTest("testNoDataJoined"));
        suite.addTest(new OracleOnlineTest("testPartialJoined"));
        suite.addTest(new OracleOnlineTest("testViennaJoined"));
        suite.addTest(new OracleOnlineTest("testViennaEnvJoined"));
        suite.addTest(new OracleOnlineTest("testDrop"));
        suite.addTest(new OracleOnlineTest("testCloseConnection"));

        return suite;
    }

    @Override
    protected String getSubDir() {
        return "oracle";
    }

    @Override
    protected DBDialect getDBDialect() {
        if (dialect != null) {
            return dialect;
        }

        Config config = null;

        try {
            config = Config.readFrom(new URL(getConfigUrl()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        dialect = DBDialect.getDBDialect(config);

        return dialect;
    }

    void executeRegister(String stmt) throws SQLException {
        Connection.prepareStatement(stmt).execute();
    }

    void executeUnRegister(String stmt) throws SQLException {
        Connection.prepareStatement(stmt).execute();
    }
}
