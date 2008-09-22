/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.gce.imagemosaic.jdbc;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.geotools.coverage.grid.GeneralGridRange;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;

import org.geotools.factory.Hints;

import org.geotools.geometry.GeneralEnvelope;

import org.geotools.referencing.CRS;

import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Color;
import java.awt.Rectangle;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import java.net.URL;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;


public abstract class AbstractTest extends TestCase {
	
	protected final static Logger LOGGER = Logger.getLogger(AbstractTest.class.getPackage()
            .getName());
	
    protected static String CRSNAME = "EPSG:4326";
    protected static Connection Connection = null;

    protected static double DELTA = 1;

    protected static GeneralEnvelope ENV_1 = new GeneralEnvelope(new double[] {
                14, 47
            }, new double[] { 16, 48 });

    protected static GeneralEnvelope ENV_VIENNA = new GeneralEnvelope(new double[] {
                16.2533, 48.1371
            }, new double[] { 16.4909, 48.2798 });

    protected static GeneralEnvelope ENV_VIENNA2 = new GeneralEnvelope(new double[] {
                15.7533, 47.6371
            }, new double[] { 15.9909, 47.7298 });
    protected static String OUTPUTDIR_BASE = "target";
    protected static String OUTPUTDIR_RESOURCES = OUTPUTDIR_BASE +
        File.separator + "resources" + File.separator;
    protected static String RESOURCE_ZIP = "src/test/resources/resources.zip";
    protected Properties fixture;

    public AbstractTest(String test) {
        super(test);
    }

    boolean checkPreConditions() {
        try {
            initOutputDir();
        } catch (Exception e) {
            e.printStackTrace();
            
            LOGGER.severe("Cannot init " + OUTPUTDIR_RESOURCES +
                ", skipping test");

            return false;
        }

        String driverClassName = getDriverClassName();

        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException ex) {
        	if (LOGGER.isLoggable(Level.WARNING))
        		LOGGER.log(Level.WARNING,driverClassName + " not found, skipping test");

            return false;
        }

        File file = getFixtureFile();

        if ((file != null) && (file.exists() == false)) {
        	if (LOGGER.isLoggable(Level.CONFIG))
        		LOGGER.log(Level.CONFIG,file.getAbsolutePath() +
                	" not found, skipping test");

            return false;
        }

        return true;
    }

    public abstract String getConfigUrl();

    protected String getSrsId() {
        return null;
    }

    protected String getOutPutDir() {
        return OUTPUTDIR_BASE + File.separator + getSubDir();
    }

    protected String[] getTileTableNames() {
        return new String[] { "TILES1", "TILES2", "TILES3" };
    }

    protected String[] getSpatialTableNames() {
        return new String[] { "SPATIAL1", "SPATIAL2", "SPATIAL3" };
    }

    protected void createTargetResourceDir(File targetResourcedir)
        throws Exception {
        targetResourcedir.mkdir();

        ZipFile zipFile = new ZipFile(RESOURCE_ZIP);
        Enumeration<?extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            if (entry.isDirectory()) {
                File dir = new File(OUTPUTDIR_RESOURCES + entry.getName());
                dir.mkdir();
            } else {
                InputStream in = zipFile.getInputStream(entry);
                FileOutputStream out = new FileOutputStream(OUTPUTDIR_RESOURCES +
                        entry.getName());
                byte[] buff = new byte[4096];
                int count;

                while ((count = in.read(buff)) > 0)
                    out.write(buff, 0, count);

                in.close();
                out.close();
            }
        }
    }

    protected void initOutputDir() throws Exception {
        File targetResourcedir = new File(OUTPUTDIR_RESOURCES);

        if (targetResourcedir.exists() == false) {
            createTargetResourceDir(targetResourcedir);
        }

        // delete previous results
        File dir = new File(getOutPutDir());

        if (dir.exists()) {
            File[] files = dir.listFiles();

            if (files != null) {
                for (File f : files)
                    f.delete();
            }
        } else {
            dir.mkdir();
        }
    }

    public void testDrop() {
        try {
            Connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            Connection.prepareStatement(getDBDialect()
                                            .getDropTableStatement(getDBDialect()
                                                                       .getConfig()
                                                                       .getMasterTable()))
                      .execute();
            Connection.commit();
        } catch (SQLException e) {
            try {
                Connection.rollback();
            } catch (SQLException ex) {
            }

            ;
        }

        ;

        for (String tn : getTileTableNames()) {
            try {
                Connection.prepareStatement((getDBDialect()
                                                 .getDropTableStatement(tn)))
                          .execute();
                Connection.commit();
            } catch (SQLException e) {
                try {
                    Connection.rollback();
                } catch (SQLException ex) {
                }

                ;
            }
        }

        for (String tn : getSpatialTableNames()) {
            try {
                Connection.prepareStatement((getDBDialect()
                                                 .getDropIndexStatment(tn)))
                          .execute();

                try {
                    Connection.rollback();
                } catch (SQLException ex) {
                }

                ;
            } catch (SQLException e) {
                try {
                    Connection.rollback();
                } catch (SQLException ex) {
                }

                ;
            }

            ;
        }

        for (String tn : getSpatialTableNames()) {
            try {

                String stmt = getDBDialect().getUnregisterSpatialStatement((tn));

                if (stmt != null) {
                    executeUnRegister(stmt);
                }

                Connection.prepareStatement((getDBDialect()
                                                 .getDropTableStatement(tn)))
                          .execute();
                Connection.commit();
            } catch (SQLException e) {
                try {
                    Connection.rollback();
                } catch (SQLException ex) {
                }

                ;
            }

            ;
        }

        try {
            Connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void testScripts() {
        DDLGenerator gen = new DDLGenerator(getDBDialect().getConfig(),
        		"SPAT","TILE",2,";",getSrsId(),OUTPUTDIR_BASE+File.separator+getSubDir());
        try {
        	gen.generate();
        } catch (Exception e) {
        	Assert.fail(e.getMessage());
        	e.printStackTrace();
        }

    	
    }
    public void testCreate() {
        try {
            createXMLConnectFragment();

            Connection.prepareStatement(getDBDialect().getCreateMasterStatement())
                      .execute();

            for (String tn : getTileTableNames()) {
                Connection.prepareStatement(getDBDialect()
                                                .getCreateTileTableStatement(tn))
                          .execute();
            }

            for (String tn : getSpatialTableNames()) {
                Connection.prepareStatement(getDBDialect()
                                                .getCreateSpatialTableStatement(tn))
                          .execute();

                String stmt = getDBDialect()
                                  .getRegisterSpatialStatement(tn, getSrsId());

                if (stmt != null) {
                    executeRegister(stmt);
                }
            }

            Connection.commit();

            for (int i = 0; i < getTileTableNames().length; i++) {
            	            	              
              Import imp = null;
              if (i==0) {
                  URL dirFileUrl = new URL("file:" + OUTPUTDIR_RESOURCES + i);
            	  imp = new Import(getDBDialect().getConfig(),
            			  getSpatialTableNames()[i], getTileTableNames()[i],
            			  dirFileUrl, "tif", 2, Connection, Import.ImportTyp.DIR);
              }
              if (i==1) {              	
                URL shapeFileUrl = new URL("file:" + OUTPUTDIR_RESOURCES + i +
                        File.separator + "index.shp");
                imp = new Import(getDBDialect().getConfig(),
                        getSpatialTableNames()[i], getTileTableNames()[i],
                        shapeFileUrl, "LOCATION", 2, Connection, Import.ImportTyp.SHAPE);
              }
              if (i==2) {
                  URL csvFileUrl = new URL("file:" + OUTPUTDIR_RESOURCES + i +
                          File.separator + "index.csv");
                  imp = new Import(getDBDialect().getConfig(),
                          getSpatialTableNames()[i], getTileTableNames()[i],
                          csvFileUrl, ";", 2, Connection, Import.ImportTyp.CSV);

              }
              imp.fillSpatialTable();
            }

            for (String tn : getSpatialTableNames()) {
                Connection.prepareStatement(getDBDialect()
                                                .getCreateIndexStatement(tn))
                          .execute();
            }

            Connection.commit();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    protected JDBCAccess getJDBCAccess() {
        return JDBCAccessFactory.JDBCAccessMap.get(getConfigUrl().toString());
    }

    public void testCreateJoined() {
        JDBCAccess access = getJDBCAccess();

        for (int i = 0; i <= access.getNumOverviews(); i++) {
            ImageLevelInfo li = access.getLevelInfo(i);
            li.setTileTableName(li.getSpatialTableName());
        }

        try {
            Connection.prepareStatement(getDBDialect().getCreateMasterStatement())
                      .execute();

            for (String tn : getSpatialTableNames()) {
                Connection.prepareStatement(getDBDialect()
                                                .getCreateSpatialTableStatementJoined(tn))
                          .execute();

                String stmt = getDBDialect()
                                  .getRegisterSpatialStatement(tn, getSrsId());

                if (stmt != null) {
                    executeRegister(stmt);
                }
            }

            Connection.commit();

            for (int i = 0; i < getTileTableNames().length; i++) {
                Import imp = null;
                if (i==0) {
                    URL dirFileUrl = new URL("file:" + OUTPUTDIR_RESOURCES + i);
              	  imp = new Import(getDBDialect().getConfig(),
              			  getSpatialTableNames()[i], getSpatialTableNames()[i],
              			  dirFileUrl, "tif", 2, Connection, Import.ImportTyp.DIR);
                }
                if (i==1) {              	
                  URL shapeFileUrl = new URL("file:" + OUTPUTDIR_RESOURCES + i +
                          File.separator + "index.shp");
                  imp = new Import(getDBDialect().getConfig(),
                          getSpatialTableNames()[i], getSpatialTableNames()[i],
                          shapeFileUrl, "LOCATION", 2, Connection, Import.ImportTyp.SHAPE);
                }
                if (i==2) {
                    URL csvFileUrl = new URL("file:" + OUTPUTDIR_RESOURCES + i +
                            File.separator + "index.csv");
                    imp = new Import(getDBDialect().getConfig(),
                            getSpatialTableNames()[i], getSpatialTableNames()[i],
                            csvFileUrl, ";", 2, Connection, Import.ImportTyp.CSV);

                }
                imp.fillSpatialTable();
            }

            for (String tn : getSpatialTableNames()) {
                Connection.prepareStatement(getDBDialect()
                                                .getCreateIndexStatement(tn))
                          .execute();
            }

            Connection.commit();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    protected void imageMosaic(String name, String configUrl,
        GeneralEnvelope envelope, int width, int heigth)
        throws IOException {
        imageMosaic(name, configUrl, envelope, width, heigth, null, null);
    }

    protected void imageMosaic(String name, String configUrl,
        GeneralEnvelope envelope, int width, int heigth, Color bColor,
        CoordinateReferenceSystem crs) throws IOException {
        // Hints hints = new
        // Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM,CRS.parseWKT(M34_PRJ));
        Hints hints = null;

        if (crs != null) {
            hints = new Hints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, crs);
        }

        AbstractGridFormat format = (AbstractGridFormat) GridFormatFinder.findFormat(configUrl);
        ImageMosaicJDBCReader reader = (ImageMosaicJDBCReader) format.getReader(configUrl,
                hints);

        ParameterValue gg = AbstractGridFormat.READ_GRIDGEOMETRY2D.createValue();

        if (envelope == null) {
            envelope = reader.getOriginalEnvelope();
        } else if (envelope.getCoordinateReferenceSystem() == null) {
            envelope.setCoordinateReferenceSystem(reader.getOriginalEnvelope()
                                                        .getCoordinateReferenceSystem());
        }

        gg.setValue(new GridGeometry2D(
                new GeneralGridRange(new Rectangle(0, 0, width, heigth)),
                envelope));

        final ParameterValue outTransp = ImageMosaicJDBCFormat.OUTPUT_TRANSPARENT_COLOR.createValue();
        outTransp.setValue((bColor == null)
            ? ImageMosaicJDBCFormat.OUTPUT_TRANSPARENT_COLOR.getDefaultValue()
            : bColor);

        GridCoverage2D coverage = (GridCoverage2D) reader.read(new GeneralParameterValue[] {
                    gg, outTransp
                });

        ImageIO.write(coverage.getRenderedImage(), "tif",
            new File(getOutPutDir() + File.separator + name + ".tif"));
    }

    protected abstract String getSubDir();

    protected abstract DBDialect getDBDialect();

    public void testImage1() {
        doTestImage1("image1");
    }

    public void testImage1Joined() {
        doTestImage1("image1_joined");
    }

    public void testFullExtent() {
        doFullExtent("fullExtent");
    }

    public void testFullExtentJoined() {
        doFullExtent("fullExtentJoined");
    }

    public void testNoData() {
        doNoData("nodData");
    }

    public void testNoDataJoined() {
        doNoData("noDataJoined");
    }

    public void testPartial() {
        doPartial("partial");
    }

    public void testPartialJoined() {
        doPartial("partialJoined");
    }

    public void testVienna() {
        doVienna("vienna");
    }

    public void testViennaJoined() {
        doVienna("viennaJoined");
    }

    public void testViennaEnv() {
        doViennaEnv("viennaEnv");
    }

    public void testViennaEnvJoined() {
        doViennaEnv("viennaEnvJoined");
    }

    private void doVienna(String name) {
        try {
            ENV_VIENNA.setCoordinateReferenceSystem(CRS.decode(CRSNAME));
            imageMosaic(name, getConfigUrl(), ENV_VIENNA, 500, 500);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    private void doViennaEnv(String name) {
        try {
            ENV_VIENNA2.setCoordinateReferenceSystem(CRS.decode(CRSNAME));
            imageMosaic(name, getConfigUrl(), ENV_VIENNA2, 500, 500);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    private void doTestImage1(String name) {
        try {
            ENV_1.setCoordinateReferenceSystem(CRS.decode(CRSNAME));
            imageMosaic(name, getConfigUrl(), ENV_1, 500, 250);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    private void doFullExtent(String name) {
        JDBCAccess access = getJDBCAccess();
        ImageLevelInfo li = access.getLevelInfo(access.getNumOverviews());

        double scale = li.getEnvelope().getWidth() / 400;
        GeneralEnvelope env = new GeneralEnvelope(new double[] {
                    li.getExtentMinX(), li.getExtentMinY()
                }, new double[] { li.getExtentMaxX(), li.getExtentMaxY() });

        try {
            env.setCoordinateReferenceSystem(CRS.decode(CRSNAME));
            imageMosaic(name, getConfigUrl(), env, 400,
                (int) (li.getEnvelope().getHeight() / scale));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    private void doNoData(String name) {
        JDBCAccess access = getJDBCAccess();
        ImageLevelInfo li = access.getLevelInfo(access.getNumOverviews());

        GeneralEnvelope env = new GeneralEnvelope(new double[] {
                    li.getExtentMaxX() + DELTA, li.getExtentMaxY() + DELTA
                },
                new double[] {
                    li.getExtentMaxX() + (DELTA * 2),
                    li.getExtentMaxY() + (DELTA * 2)
                });

        try {
            env.setCoordinateReferenceSystem(CRS.decode(CRSNAME));
            imageMosaic(name, getConfigUrl(), env, 400, 400);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    private void doPartial(String name) {
        JDBCAccess access = getJDBCAccess();
        ImageLevelInfo li = access.getLevelInfo(access.getNumOverviews());

        GeneralEnvelope env = new GeneralEnvelope(new double[] {
                    li.getExtentMaxX() - DELTA, li.getExtentMaxY() - DELTA
                },
                new double[] {
                    li.getExtentMaxX() + DELTA, li.getExtentMaxY() + DELTA
                });

        try {
            env.setCoordinateReferenceSystem(CRS.decode(CRSNAME));
            imageMosaic(name, getConfigUrl(), env, 400, 400);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    public void setUp() throws Exception {
        super.setUp();

        if (fixture == null) {
            initFixture();
        }

        if (fixture == null) {
            String propertiesName = getSubDir() + ".properties";
        	if (LOGGER.isLoggable(Level.CONFIG)) {
        		LOGGER.log(Level.CONFIG,"Fixture not found - make sure " +
        				propertiesName + " copied from build tree resources directory");
        		LOGGER.log(Level.CONFIG,
        			"to Documents and Settings/userid/.geotools/imagemosaic  (Windows) ");
        		LOGGER.log(Level.CONFIG,"or   ~/.geotools/imagemosiac (Unix) ");
        	}
            throw new IOException("Fixture not found");
        }
    }

    String getFixtureId() {
        return "imagemosaic." + getSubDir();
    }

    protected File getFixtureFile() {
        File base = new File(System.getProperty("user.home") + File.separator +
                ".geotools");
        String fixtureId = getFixtureId();

        if (fixtureId == null) {
            return null;
        }

        File fixtureFile = new File(base,
                fixtureId.replace('.', File.separatorChar).concat(".properties"));

        return fixtureFile;
    }

    protected void initFixture() throws Exception {
        File fixtureFile = getFixtureFile();

        if ((fixtureFile != null) && fixtureFile.exists()) {
            InputStream input = new BufferedInputStream(new FileInputStream(
                        fixtureFile));

            try {
                fixture = new Properties();
                fixture.load(input);
            } finally {
                input.close();
            }
        }
    }

    void createXMLConnectFragment() throws Exception {
        if (fixture == null) {
            return;
        }

        String host = fixture.getProperty("host");

        if (host != null) {
            host = host.trim();
        }

        String portString = fixture.getProperty("portnum");
        Integer port = null;

        if (portString != null) {
            port = new Integer(portString);
        }

        String dbName = fixture.getProperty("dbname");

        if (dbName != null) {
            dbName = dbName.trim();
        }

        String user = fixture.getProperty("user");

        if (user != null) {
            user = user.trim();
        }

        String password = fixture.getProperty("password");

        if (password != null) {
            password = password.trim();
        }

        PrintWriter w = new PrintWriter(new FileOutputStream(OUTPUTDIR_RESOURCES +getXMLConnectFragmentName()));
        w.println("<connect>");
        w.println("     <dstype value=\"DBCP\"/>");
        w.println("     <username value=\"" + user + "\"/>");
        w.println("     <password value=\"" + password + "\"/>");
        w.println("     <jdbcUrl value=\"" +
            getJDBCUrl(host, port, dbName) + "\"/>");
        w.println("     <driverClassName value=\"" +
            getDriverClassName() + "\"/>");
        w.println("     <maxActive value=\"10\"/>");
        w.println("     <maxIdle value=\"0\"/>");
        w.println("</connect>");
        w.close();
    }

    public void testGetConnection() {
        Connection = null;

        try {
            Connection = getDBDialect().getConnection();
        } catch (Exception e) {
            Assert.fail("Error getting connection");
            e.printStackTrace();
        }
    }

    public void testCloseConnection() {
        if (Connection != null) {
            try {
                Connection.close();
            } catch (SQLException e) {
                Assert.fail("Error closing connection");
                e.printStackTrace();
            } finally {
                Connection = null;
            }
        }
    }

    void executeRegister(String stmt) throws SQLException {
    }

    void executeUnRegister(String stmt) throws SQLException {
    }
    
    protected abstract String getXMLConnectFragmentName();
    
    /**
     * @return der full qulified java class name for the jdbc driver
     */
    protected abstract String getDriverClassName();

    /**
     * @param host	 	the host name/ip address
     * @param port 		the tcpip port where the db is listening
     * @param dbName	the name of the database
     * @return			the db specific connect url
     */
    protected abstract String getJDBCUrl(String host, Integer port,
        String dbName);


}