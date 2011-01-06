/*$************************************************************************************************
 **
 ** $Id: package-info.java 1246 2008-06-24 06:52:12Z desruisseaux $
 **
 ** $URL: https://geoapi.svn.sourceforge.net/svnroot/geoapi/tags/2.3-M2/geoapi/src/main/java/org/opengis/annotation/package-info.java $
 **
 ** Copyright (C) 2005 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/

/**
 * A set of base types from ISO 19103 which can not be mapped directly from Java, plus utilities.
 *
 *  <h3>Names and Namespaces</h3>
 * <p align="justify">The job of a "name" in the context of ISO 19103 is to associate that name
 * with an {@link java.lang.Object}.  Examples given are <cite>objects</cite>: which form namespaces
 * for their attributes, and <cite>Schema</cite>: which form namespaces for their components.
 * A straightforward and natural use of the namespace structure defined in 19103 is the translation
 * of given names into specific storage formats.  XML has different naming rules than shapefiles,
 *  and both are different than NetCDF.  This common framework can easily be harnessed to impose
 *  constraints specific to a particular application without requiring that a separate implementation
 *  of namespaces be provided for each format.</p>
 * 
 * <h3>Records and Schemas</h3>
 * <p align="justify">Records and Schemas are similar to a {@code struct} in C/C++, a table in SQL,
 * a {@code RECORD} in Pascal, or an attribute-only class in Java if it were stripped of all notions
 * of inheritance.  They are organized into named collections called Schemas. Both records and schemas
 * behave as dictionaries for their members and are similar to "packages" in Java.</p>
 *
 * @since GeoAPI 2.0
 */
package org.opengis.util;