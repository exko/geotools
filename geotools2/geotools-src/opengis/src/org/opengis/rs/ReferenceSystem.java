/*
 * Copyright (C) 2003 Open GIS Consortium, Inc. All Rights Reserved. http://www.opengis.org/Legal/
 */
package org.opengis.rs;


/**
 * Description of a spatial and temporal reference system used by a dataset.
 *  
 * @author <A HREF="http://www.opengis.org">OpenGIS&reg;</A>
 * @version 2.0
 */
public interface ReferenceSystem {
    /**
     * The name by which this reference system is uniquely identified.
     *
     * @return The reference system name.
     *
     * @mandatory
     * @task REVISIT: Should we ask for a (possibly null) java.util.Locale argument?
     */
//    public String getSrsName();

    /**
     * Set of alternative identifications of this reference system. The first <code>srsID</code>,
     * if any, is normally the primary identification code, and any others are aliases.
     *
     * @return Coordinate reference system identifiers,
     *         or <code>null</code> if not available.
     *
     * @optional
     * @task TODO: Uncomment
     */
//    public Identifier[] getSrsID();

    /**
     * Area for which the (coordinate) reference system is valid.
     *
     * @return Coordinate reference system valid area.
     *         or <code>null</code> if not available.
     *
     * @optional
     * @task TODO: Uncomment
     */
//    public org.opengis.crs.extent.Extent[] getValidArea();

    /**
     * Description of domain of usage, or limitations of usage, for which this
     * CRS object is valid.
     *
     * @return Coordinate reference system scope,
     *         or <code>null</code> if not available.
     *
     * @optional
     * @task REVISIT: Should we ask for a (possibly null) java.util.Locale argument?
     */
//    public String getScope();

    /**
     * Comments on or information about this (coordinate) reference system,
     * including data source information.
     *
     * @return Coordinate reference system remarks
     *         or <code>null</code> if not available.
     *
     * @optional
     * @task REVISIT: Should we ask for a (possibly null) java.util.Locale argument?
     */
//    public String getRemarks();
}
