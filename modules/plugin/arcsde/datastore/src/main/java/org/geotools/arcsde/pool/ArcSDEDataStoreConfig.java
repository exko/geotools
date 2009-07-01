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
 *
 */
package org.geotools.arcsde.pool;

import static org.geotools.arcsde.pool.ArcSDEConnectionConfig.CONNECTION_TIMEOUT_PARAM_NAME;
import static org.geotools.arcsde.pool.ArcSDEConnectionConfig.INSTANCE_NAME_PARAM_NAME;
import static org.geotools.arcsde.pool.ArcSDEConnectionConfig.MAX_CONNECTIONS_PARAM_NAME;
import static org.geotools.arcsde.pool.ArcSDEConnectionConfig.MIN_CONNECTIONS_PARAM_NAME;
import static org.geotools.arcsde.pool.ArcSDEConnectionConfig.PASSWORD_PARAM_NAME;
import static org.geotools.arcsde.pool.ArcSDEConnectionConfig.PORT_NUMBER_PARAM_NAME;
import static org.geotools.arcsde.pool.ArcSDEConnectionConfig.SERVER_NAME_PARAM_NAME;
import static org.geotools.arcsde.pool.ArcSDEConnectionConfig.USER_NAME_PARAM_NAME;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Represents a set of ArcSDE database connection parameters. Instances of this class are used to
 * validate ArcSDE connection params as in <code>DataSourceFactory.canProcess(java.util.Map)</code>
 * and serves as keys for maintaining single <code>SdeConnectionPool</code>'s by each set of
 * connection properties
 * 
 * @author Gabriel Roldan
 * @source $URL:
 *         http://svn.geotools.org/geotools/trunk/gt/modules/plugin/arcsde/datastore/src/main/java
 *         /org/geotools/arcsde/pool/ArcSDEConnectionConfig.java $
 * @version $Id$
 */
@SuppressWarnings("unchecked")
public class ArcSDEDataStoreConfig {
    /*
     * ArcSDEDataStoreConfige's logger
     */
    private static final Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger("org.geotools.arcsde.pool");

    /**
     * message of the exception thrown if a mandatory parameter is not supplied
     */
    private static final String NULL_ARGUMENTS_MSG = "Illegal arguments. At least one of them was null. Check to pass "
            + "correct values to dbtype, server, port, database, user and password parameters";

    private static final String ILLEGAL_ARGUMENT_MSG = " is not valid for parameter ";

    /** must equals to <code>"arcsde"</code> */
    public static final String DBTYPE_PARAM_NAME = "dbtype";

    /** constant to pass "arcsde" as DBTYPE_PARAM */
    public static final String DBTYPE_PARAM_VALUE = "arcsde";

    /** namespace URI assigned to datastore instance */
    public static final String NAMESPACE_PARAM_NAME = "namespace";

    public static final String VERSION_PARAM_NAME = "database.version";

    public static final String ALLOW_NON_SPATIAL_TABLES_PARAM_NAME = "datastore.allowNonSpatialTables";

    /** namespace URI assigned to datastore */
    private String namespaceUri;

    /** ArcSDE database version name, or null for DEFAULT version */
    private String version;

    /** whether to publish arcsde registered, non-spatial tables */
    private boolean allowNonSpatialTables;

    private ArcSDEConnectionConfig sessionConfig = new ArcSDEConnectionConfig();

    /**
     * Configure arcsde connection information from supplied connection parameters.
     * 
     * @param params
     *            Connection parameters
     * @throws NullPointerException
     *             if at least one mandatory parameter is null
     * @throws IllegalArgumentException
     *             if at least one mandatory parameter is present but does not have a "valid" value.
     */
    public ArcSDEDataStoreConfig(Map params) throws IllegalArgumentException {
        init(params);
    }

    public ArcSDEDataStoreConfig(ArcSDEConnectionConfig sessionConfig, final String namespace,
            final String versionName, final boolean allowNonSpatialTables) {

        this.sessionConfig = sessionConfig;
        this.namespaceUri = namespace;
        this.allowNonSpatialTables = allowNonSpatialTables;
        this.version = versionName;
    }

    /**
     * Extra connection parameters from the provided map.
     * 
     * @param params
     *            Connection parameters
     * @throws NumberFormatException
     *             If port could not be parsed into a number
     * @throws IllegalArgumentException
     *             If any of the parameters are invalid
     */
    private void init(Map params) throws NumberFormatException, IllegalArgumentException {
        String dbtype = (String) params.get(DBTYPE_PARAM_NAME);
        String server = (String) params.get(SERVER_NAME_PARAM_NAME);
        String port = String.valueOf(params.get(PORT_NUMBER_PARAM_NAME));
        String instance = (String) params.get(INSTANCE_NAME_PARAM_NAME);
        String user = (String) params.get(USER_NAME_PARAM_NAME);
        String pwd = (String) params.get(PASSWORD_PARAM_NAME);
        Integer _port = checkParams(dbtype, server, port, instance, user, pwd);
        sessionConfig.setServerName(server);
        sessionConfig.setPortNumber(_port);
        sessionConfig.setDatabaseName(instance);
        sessionConfig.setUserName(user);
        sessionConfig.setPassword(pwd);
        setUpOptionalParams(params);
    }

    /**
     * Handle optional parameters; most are focused on connection pool use.
     * 
     * @param params
     *            Connection parameters
     * @throws IllegalArgumentException
     *             If any of the optional prameters are invlaid.
     */
    private void setUpOptionalParams(Map params) throws IllegalArgumentException {
        String exceptionMsg = "";
        Object ns = params.get(NAMESPACE_PARAM_NAME);

        this.namespaceUri = ns == null ? null : String.valueOf(ns);

        Integer minConnections = getInt(params.get(MIN_CONNECTIONS_PARAM_NAME),
                SessionPool.DEFAULT_CONNECTIONS);
        Integer maxConnections = getInt(params.get(MAX_CONNECTIONS_PARAM_NAME),
                SessionPool.DEFAULT_MAX_CONNECTIONS);
        Integer connTimeOut = getInt(params.get(CONNECTION_TIMEOUT_PARAM_NAME),
                SessionPool.DEFAULT_MAX_WAIT_TIME);

        this.version = (String) params.get(VERSION_PARAM_NAME);

        Object nonSpatial = params.get(ALLOW_NON_SPATIAL_TABLES_PARAM_NAME);
        this.allowNonSpatialTables = nonSpatial == null ? false : Boolean.valueOf(String
                .valueOf(nonSpatial));

        if (minConnections.intValue() <= 0) {
            exceptionMsg += MIN_CONNECTIONS_PARAM_NAME + " must be a positive integer. ";
        }

        if (maxConnections.intValue() <= 0) {
            exceptionMsg += MAX_CONNECTIONS_PARAM_NAME + " must be a positive integer. ";
        }

        if (connTimeOut.intValue() <= 0) {
            exceptionMsg += CONNECTION_TIMEOUT_PARAM_NAME + " must be a positive integer. ";
        }

        if (minConnections.intValue() > maxConnections.intValue()) {
            exceptionMsg += MIN_CONNECTIONS_PARAM_NAME + " must be lower than "
                    + MAX_CONNECTIONS_PARAM_NAME + ".";
        }

        if (exceptionMsg.length() != 0) {
            throw new IllegalArgumentException(exceptionMsg);
        }

        sessionConfig.setMinConnections(minConnections);
        sessionConfig.setMaxConnections(maxConnections);
        sessionConfig.setConnTimeOut(connTimeOut);
    }

    /**
     * Convert value to an Integer, or use the default value
     * 
     * @param value
     *            Object to convert to int
     * @param defaultValue
     *            Default value if conversion fails
     * @return value as an interger, or default value if that is not possible
     */
    private static final Integer getInt(Object value, int defaultValue) {
        if (value == null) {
            return Integer.valueOf(defaultValue);
        }

        String sVal = String.valueOf(value);

        try {
            return Integer.valueOf(sVal);
        } catch (NumberFormatException ex) {
            return Integer.valueOf(defaultValue);
        }
    }

    private static Integer checkParams(String dbType, String serverName, String portNumber,
            String databaseName, String userName, String userPassword)
            throws IllegalArgumentException, NullPointerException {
        // check if dbtype is 'arcsde'
        if (!(DBTYPE_PARAM_VALUE.equals(dbType))) {
            throw new IllegalArgumentException("parameter dbtype must be " + DBTYPE_PARAM_VALUE);
        }

        // check for nullity
        if ((serverName == null) || (portNumber == null) || (userName == null)
                || (userPassword == null)) {
            throw new NullPointerException(NULL_ARGUMENTS_MSG);
        }

        if (serverName.length() == 0) {
            throwIllegal(SERVER_NAME_PARAM_NAME, serverName);
        }

        if (databaseName == null || databaseName.length() == 0) {
            LOGGER.fine("No database name specified");
        }

        if (userName.length() == 0) {
            throwIllegal(USER_NAME_PARAM_NAME, userName);
        }

        if (userPassword.length() == 0) {
            throwIllegal(PASSWORD_PARAM_NAME, userPassword);
        }

        Integer port = null;

        try {
            port = Integer.valueOf(portNumber);
        } catch (NumberFormatException ex) {
            throwIllegal(PORT_NUMBER_PARAM_NAME, portNumber);
        }

        return port;
    }

    /**
     * DOCUMENT ME!
     * 
     * @param paramName
     *            DOCUMENT ME!
     * @param paramValue
     *            DOCUMENT ME!
     * @throws IllegalArgumentException
     *             DOCUMENT ME!
     */
    private static void throwIllegal(String paramName, String paramValue)
            throws IllegalArgumentException {
        throw new IllegalArgumentException("'" + paramValue + "'" + ILLEGAL_ARGUMENT_MSG
                + paramName);
    }

    public String getNamespaceUri() {
        return namespaceUri;
    }

    public String getDatabaseName() {
        return sessionConfig.getDatabaseName();
    }

    public Integer getPortNumber() {
        return sessionConfig.getPortNumber();
    }

    public String getServerName() {
        return sessionConfig.getServerName();
    }

    public String getUserName() {
        return sessionConfig.getUserName();
    }

    public String getUserPassword() {
        return sessionConfig.getPassword();
    }

    @Override
    public int hashCode() {
        int hash = 37;
        hash *= getServerName().hashCode();
        hash *= getPortNumber().hashCode();
        hash *= getUserName().hashCode();
        return hash;
    }

    /**
     * Checks for equality over another <code>ArcSDEConnectionConfig</code>, taking into account the
     * values of database name, user name, and port number.
     * 
     * @param o
     *            DOCUMENT ME!
     * @return DOCUMENT ME!
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof ArcSDEDataStoreConfig)) {
            return false;
        }

        ArcSDEDataStoreConfig config = (ArcSDEDataStoreConfig) o;

        return config.getServerName().equals(getServerName())
                && config.getPortNumber().equals(getPortNumber())
                && config.getUserName().equals(getUserName());
    }

    public Integer getConnTimeOut() {
        return sessionConfig.getConnTimeOut();
    }

    public Integer getMaxConnections() {
        return sessionConfig.getMaxConnections();
    }

    public Integer getMinConnections() {
        return sessionConfig.getMinConnections();
    }

    /**
     * @return the ArcSDE version name to connect to
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return whether to publish ArcSDE registered, non-spatial tables
     */
    public boolean isAllowNonSpatialTables() {
        return allowNonSpatialTables;
    }

    /**
     * @return a human friendly description of this parameter holder contents (password is masked),
     *         mostly usefull for stack traces
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(getClass().getName() + "[");
        sb.append("dbtype=").append(ArcSDEDataStoreConfig.DBTYPE_PARAM_VALUE);
        sb.append(", server=").append(getServerName());
        sb.append(", port=").append(getPortNumber());
        sb.append(", instance=").append(getDatabaseName());
        sb.append(", user=").append(getUserName());
        // hidding password as the result of this method
        // is probably going to end up in a stack trace
        sb.append(", password=*****");
        sb.append(", version='").append(version == null ? "[DEFAULT]" : version).append("'");
        sb.append(", non-spatial:").append(allowNonSpatialTables);
        sb.append(", minConnections=").append(getMinConnections());
        sb.append(", maxConnections=").append(getMaxConnections());
        sb.append(", connTimeOut=").append(getConnTimeOut());
        sb.append("]");

        return sb.toString();
    }

}
