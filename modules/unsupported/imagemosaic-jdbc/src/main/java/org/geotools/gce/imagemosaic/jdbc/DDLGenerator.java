package org.geotools.gce.imagemosaic.jdbc;

import java.io.IOException;
import java.io.PrintWriter;

import java.net.URL;

import java.util.logging.Logger;


class DDLGenerator {
    private final static int DefaultPyramids = 0;
    private final static String FN_CREATEMETA = "createmeta.sql";
    private final static String FN_CREATETABLES = "createtables.sql";
    private final static String FN_CREATEINDEXES = "createindexes.sql";
    private final static String FN_FILLMETA = "fillmeta.sql";
    private final static String FN_REGISTER = "register.sql";
    private final static String FN_UNREGISTER = "unregister.sql";
    private final static String FN_DROPMETA = "dropmeta.sql";
    private final static String FN_DROPTABLES = "droptables.sql";
    private final static String FN_DROPINIDEXES = "dropindexes.sql";
    private final static String UsageInfo = "Generating DDL scripts\n" +
        "-configUrl url -spatialTNPrefix spatialTNPrefix [-tileTNPrefix tileTNPrefix]\n" +
        "  [-pyramids pyramids] -statementDelim statementDelim -srs srs";
    private Config config;
    private String spatialTNPrefix;
    private String tileTNPrefix;
    private int pyramids = DefaultPyramids;
    private String statementDelim;
    private Logger logger;
    private DBDialect dbDialect;
    private String srs;

    DDLGenerator(Config config, String spatialTNPrefix, String tileTNPrefix,
        int pyramids, String statementDelim, String srs) {
        this.config = config;
        this.pyramids = pyramids;
        this.spatialTNPrefix = spatialTNPrefix;
        this.tileTNPrefix = tileTNPrefix;
        this.statementDelim = statementDelim;
        this.srs = srs;

        this.logger = Logger.getLogger(this.getClass().getName());
        this.dbDialect = DBDialect.getDBDialect(config);
    }

    public static void start(String[] args) {
        Config config = null;
        String spatialTNPrefix = null;
        String tileTNPrefix = null;
        String statementDelim = null;
        String srs = null;
        int pyramids = DefaultPyramids;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-configUrl")) {
                try {
                    config = Config.readFrom(new URL(args[i + 1]));
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }

                i++;
            } else if (args[i].equals("-spatialTNPrefix")) {
                spatialTNPrefix = args[i + 1];
                i++;
            } else if (args[i].equals("-tileTNPrefix")) {
                tileTNPrefix = args[i + 1];
                i++;
            } else if (args[i].equals("-statementDelim")) {
                statementDelim = args[i + 1];
                i++;
            } else if (args[i].equals("-srs")) {
                srs = args[i + 1];
                i++;
            } else if (args[i].equals("-pyramids")) {
                pyramids = new Integer(args[i + 1]);
                i++;
            } else {
                System.out.println("Unkwnown option: " + args[i]);
                System.exit(1);
            }
        }

        if ((config == null) || (spatialTNPrefix == null) ||
                (statementDelim == null)) {
            System.out.println(UsageInfo);
            System.exit(1);
        }

        if (needsSpatialRegistry(config) && (srs == null)) {
            System.out.println("Must specify -srs ");
            System.exit(1);
        }

        DDLGenerator gen = new DDLGenerator(config, spatialTNPrefix,
                tileTNPrefix, pyramids, statementDelim, srs);

        try {
            gen.generate();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.exit(0);
    }

    void generate() throws Exception {
        writeCreateMeta();
        writeFillMeta();
        writeCreateTables();
        writeCreateIndexes();
        writeRegister();
        writeUnRegister();
        writeDropIndexes();
        writeDropTables();
        writeDropMeta();
    }

    void writeFillMeta() throws IOException {
        PrintWriter w = new PrintWriter(FN_FILLMETA);

        String statmentString = "INSERT INTO " + config.getMasterTable() + "(" +
            config.getCoverageNameAttribute() + "," +
            config.getTileTableNameAtribute() + "," +
            config.getSpatialTableNameAtribute() + ") VALUES ('%s','%s','%s')";

        for (int i = 0; i <= pyramids; i++) {
            String stn = getTabelName(spatialTNPrefix, i);
            String ttn = (tileTNPrefix == null) ? stn
                                                : getTabelName(tileTNPrefix, i);
            Object[] args = new Object[] { config.getCoverageName(), stn, ttn };
            w.printf(statmentString, args);
            w.println(statementDelim);
        }

        w.close();
        logger.info(FN_FILLMETA + " generated");
    }

    void writeCreateMeta() throws Exception {
        PrintWriter w = new PrintWriter(FN_CREATEMETA);
        w.print(dbDialect.getCreateMasterStatement());
        w.println(statementDelim);
        w.close();
        logger.info(FN_CREATEMETA + " generated");
    }

    void writeCreateTables() throws Exception {
        PrintWriter w = new PrintWriter(FN_CREATETABLES);

        for (int i = 0; i <= pyramids; i++) {
            if (tileTNPrefix == null) {
                w.print(dbDialect.getCreateSpatialTableStatementJoined(
                        getTabelName(spatialTNPrefix, i)));
                w.println(statementDelim);
            } else {
                w.print(dbDialect.getCreateSpatialTableStatement(getTabelName(
                            spatialTNPrefix, i)));
                w.println(statementDelim);
                w.print(dbDialect.getCreateTileTableStatement(getTabelName(
                            tileTNPrefix, i)));
                w.println(statementDelim);
            }
        }

        w.close();
        logger.info(FN_CREATETABLES + " generated");
    }

    void writeCreateIndexes() throws Exception {
        PrintWriter w = new PrintWriter(FN_CREATEINDEXES);

        for (int i = 0; i <= pyramids; i++) {
            w.print(dbDialect.getCreateIndexStatement(getTabelName(
                        spatialTNPrefix, i)));
            w.println(statementDelim);
        }

        w.close();
        logger.info(FN_CREATEINDEXES + " generated");
    }

    void writeDropMeta() throws IOException {
        PrintWriter w = new PrintWriter(FN_DROPMETA);
        w.print(dbDialect.getDropTableStatement(config.getMasterTable()));
        w.println(statementDelim);
        w.close();
        logger.info(FN_DROPMETA + " generated");
    }

    void writeDropTables() throws IOException {
        PrintWriter w = new PrintWriter(FN_DROPTABLES);

        for (int i = 0; i <= pyramids; i++) {
            w.print(dbDialect.getDropTableStatement(getTabelName(
                        spatialTNPrefix, i)));
            w.println(statementDelim);
        }

        if (tileTNPrefix != null) {
            w.println();

            for (int i = 0; i <= pyramids; i++) {
                w.print(dbDialect.getDropTableStatement(getTabelName(
                            tileTNPrefix, i)));
                w.println(statementDelim);
            }
        }

        w.close();
        logger.info(FN_DROPTABLES + " generated");
    }

    String getTabelName(String prefix, int level) {
        return prefix + "_" + level;
    }

    void writeDropIndexes() throws IOException {
        PrintWriter w = new PrintWriter(FN_DROPINIDEXES);

        for (int i = 0; i <= pyramids; i++) {
            w.print(dbDialect.getDropIndexStatment(getTabelName(
                        spatialTNPrefix, i)));
            w.println(statementDelim);
        }

        w.close();
        logger.info(FN_DROPINIDEXES + " generated");
    }

    static boolean needsSpatialRegistry(Config config) {
        SpatialExtension type = config.getSpatialExtension();

        if ((type == SpatialExtension.DB2) ||
                (type == SpatialExtension.POSTGIS) ||
                (type == SpatialExtension.ORACLE)) {
            return true;
        }

        return false;
    }

    void writeRegister() throws IOException {
        if (needsSpatialRegistry(config) == false) {
            return;
        }

        PrintWriter w = new PrintWriter(FN_REGISTER);

        for (int i = 0; i <= pyramids; i++) {
            w.print(dbDialect.getRegisterSpatialStatement(getTabelName(
                        spatialTNPrefix, i), srs));
            w.println(statementDelim);
        }

        w.close();
        logger.info(FN_REGISTER + " generated");
    }

    void writeUnRegister() throws IOException {
        if (needsSpatialRegistry(config) == false) {
            return;
        }

        PrintWriter w = new PrintWriter(FN_UNREGISTER);

        for (int i = 0; i <= pyramids; i++) {
            w.print(dbDialect.getUnregisterSpatialStatement(getTabelName(
                        spatialTNPrefix, i)));
            w.println(statementDelim);
        }

        w.close();
        logger.info(FN_UNREGISTER + " generated");
    }
}