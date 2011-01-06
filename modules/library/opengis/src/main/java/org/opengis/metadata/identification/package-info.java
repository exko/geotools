/*$************************************************************************************************
 **
 ** $Id: package-info.java 1417 2009-06-03 14:58:00Z desruisseaux $
 **
 ** $URL: https://geoapi.svn.sourceforge.net/svnroot/geoapi/tags/2.3-M2/geoapi/src/main/java/org/opengis/metadata/identification/package-info.java $
 **
 ** Copyright (C) 2004-2005 Open GIS Consortium, Inc.
 ** All Rights Reserved. http://www.opengis.org/legal/
 **
 *************************************************************************************************/

/**
 * {@linkplain org.opengis.metadata.identification.Identification} information
 * (includes data and service identification).
 * The following is adapted from
 * <A HREF="http://www.opengis.org/docs/01-111.pdf">OpenGIS&reg; Metadata (Topic 11)</A> specification.
 *
 * <P ALIGN="justify">Identification information contains information to uniquely identify the data.
 * Identification information includes information about the citation for the resource, an abstract,
 * the purpose, credit, the status and points of contact.
 * The {@linkplain org.opengis.metadata.identification.Identification identification}
 * entity is mandatory. It may be specified (subclassed) as
 * {@linkplain org.opengis.metadata.identification.DataIdentification data identification}
 * when used to identify data and as
 * {@linkplain org.opengis.metadata.identification.ServiceIdentification service identification}
 * when used to identify a service.</p>
 *
 * <P ALIGN="justify">{@linkplain org.opengis.metadata.identification.Identification}
 * is an aggregate of the following entities:</P>
 * <UL>
 *   <LI>{@link org.opengis.metadata.distribution.Format}, format of the data</LI>
 *   <LI>{@link org.opengis.metadata.identification.BrowseGraphic}, graphic overview of the data</LI>
 *   <LI>{@link org.opengis.metadata.identification.Usage}, specific uses of the data</LI>
 *   <LI>{@link org.opengis.metadata.constraint.Constraints}, constraints placed on the resource</LI>
 *   <LI>{@link org.opengis.metadata.identification.Keywords}, keywords describing the resource</LI>
 *   <LI>{@link org.opengis.metadata.maintenance.MaintenanceInformation}, how often the data is scheduled
 *       to be updated and the scope of the update</LI>
 * </UL>
 *
 * @version <A HREF="http://www.opengeospatial.org/standards/as#01-111">ISO 19115</A>
 * @since   GeoAPI 2.0
 */
package org.opengis.metadata.identification;
