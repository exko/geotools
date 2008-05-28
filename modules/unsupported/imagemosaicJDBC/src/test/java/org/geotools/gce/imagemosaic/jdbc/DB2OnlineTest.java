package org.geotools.gce.imagemosaic.jdbc;

import junit.framework.Test;
import junit.framework.TestSuite;


public class DB2OnlineTest extends AbstractTest {
    public DB2OnlineTest(String test) {
        super(test);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new DB2OnlineTest("testDrop"));
        suite.addTest(new DB2OnlineTest("testCreate"));
        suite.addTest(new DB2OnlineTest("testImage1"));
        suite.addTest(new DB2OnlineTest("testFullExtent"));
        suite.addTest(new DB2OnlineTest("testNoData"));
        suite.addTest(new DB2OnlineTest("testPartial"));
        suite.addTest(new DB2OnlineTest("testVienna"));
        suite.addTest(new DB2OnlineTest("testViennaEnv"));
        suite.addTest(new DB2OnlineTest("testDrop"));
        suite.addTest(new DB2OnlineTest("testCreateJoined"));
        suite.addTest(new DB2OnlineTest("testImage1Joined"));
        suite.addTest(new DB2OnlineTest("testFullExtentJoined"));
        suite.addTest(new DB2OnlineTest("testNoDataJoined"));
        suite.addTest(new DB2OnlineTest("testPartialJoined"));
        suite.addTest(new DB2OnlineTest("testViennaJoined"));
        suite.addTest(new DB2OnlineTest("testViennaEnvJoined"));
        suite.addTest(new DB2OnlineTest("testDrop"));

        return suite;
    }

    @Override
    protected String getSubDir() {
        return "db2";
    }

    @Override
    protected JDBCSetup getJDBCSetup() {
        return DB2Setup.Singleton;
    }
}
