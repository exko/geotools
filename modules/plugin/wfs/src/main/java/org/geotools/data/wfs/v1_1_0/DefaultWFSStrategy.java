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
 */
package org.geotools.data.wfs.v1_1_0;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.QueryType;
import net.opengis.wfs.ResultTypeType;
import net.opengis.wfs.WfsFactory;

import org.geotools.data.DataSourceException;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultQuery;
import org.geotools.data.Query;
import org.geotools.data.wfs.protocol.wfs.WFSProtocol;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.SchemaException;
import org.geotools.filter.Capabilities;
import org.geotools.filter.v1_1.OGC;
import org.geotools.filter.v1_1.OGCConfiguration;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.geotools.xml.Configuration;
import org.geotools.xml.Encoder;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Id;
import org.opengis.filter.capability.FilterCapabilities;
import org.opengis.filter.identity.Identifier;
import org.opengis.filter.sort.SortBy;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A default strategy for a WFS 1.1.0 implementation that assumes the server sticks to the standard.
 * 
 * @author Gabriel Roldan (OpenGeo)
 * @version $Id$
 * @since 2.6
 * @source $URL:
 *         http://gtsvn.refractions.net/trunk/modules/plugin/wfs/src/main/java/org/geotools/data
 *         /wfs/v1_1_0/DefaultWFSStrategy.java $
 */
@SuppressWarnings("nls")
public class DefaultWFSStrategy implements WFSStrategy {

    private static final Logger LOGGER = Logging.getLogger("org.geotools.data.wfs");

    protected static final String DEFAULT_OUTPUT_FORMAT = "text/xml; subtype=gml/3.1.1";

    private Configuration filter_1_1_0_Configuration;

    public DefaultWFSStrategy() {
        filter_1_1_0_Configuration = new OGCConfiguration();
    }

    /**
     * @return {@code "text/xml; subtype=gml/3.1.1"}
     * @see WFSProtocol#getDefaultOutputFormat()
     */
    public String getDefaultOutputFormat(WFSProtocol wfs) {
        Set<String> supportedOutputFormats = wfs.getSupportedGetFeatureOutputFormats();
        if (supportedOutputFormats.contains(DEFAULT_OUTPUT_FORMAT)) {
            return DEFAULT_OUTPUT_FORMAT;
        }
        throw new IllegalArgumentException("Server does not support '" + DEFAULT_OUTPUT_FORMAT
                + "' output format: " + supportedOutputFormats);
    }

    /**
     * @see WFSStrategy#getQueryType(WFS_1_1_0_DataStore, Query)
     */
    public SimpleFeatureType getQueryType(final WFS_1_1_0_DataStore ds, final Query query)
            throws IOException {
        final String typeName = query.getTypeName();
        final SimpleFeatureType featureType = ds.getSchema(typeName);
        final CoordinateReferenceSystem coordinateSystemReproject = query
                .getCoordinateSystemReproject();

        String[] propertyNames = query.getPropertyNames();

        SimpleFeatureType queryType = featureType;
        if (propertyNames != null && propertyNames.length > 0) {
            try {
                queryType = DataUtilities.createSubType(queryType, propertyNames);
            } catch (SchemaException e) {
                throw new DataSourceException(e);
            }
        } else {
            propertyNames = DataUtilities.attributeNames(featureType);
        }

        if (coordinateSystemReproject != null) {
            try {
                queryType = DataUtilities.createSubType(queryType, propertyNames,
                        coordinateSystemReproject);
            } catch (SchemaException e) {
                throw new DataSourceException(e);
            }
        }

        return queryType;
    }

    /**
     * Creates the mapping {@link GetFeatureType GetFeature} request for the given {@link Query} and
     * {@code outputFormat}, and post-processing filter based on the server's stated filter
     * capabilities.
     * 
     * @see WFSStrategy#createGetFeatureRequest(WFS_1_1_0_DataStore, WFSProtocol, Query, String)
     */
    @SuppressWarnings("unchecked")
    public RequestComponents createGetFeatureRequest(WFS_1_1_0_DataStore ds, WFSProtocol wfs,
            Query query, String outputFormat) throws IOException {
        final WfsFactory factory = WfsFactory.eINSTANCE;

        query = new DefaultQuery(query);
        final String srsName = adaptQueryForSupportedCrs(ds, wfs, (DefaultQuery) query);
        final Filter serverFilter;
        final Filter postFilter;
        {
            final Filter queryFilter = query.getFilter();
            final Filter[] serverAndPostFilters = splitFilters(ds, wfs, queryFilter);
            serverFilter = serverAndPostFilters[0];
            postFilter = serverAndPostFilters[1];
        }

        GetFeatureType getFeature = factory.createGetFeatureType();
        getFeature.setService("WFS");
        getFeature.setVersion(wfs.getServiceVersion().toString());
        getFeature.setOutputFormat(outputFormat);

        getFeature.setHandle("GeoTools WFS DataStore");
        Integer maxFeatures = getMaxFeatures(ds, query);
        if (maxFeatures.intValue() > 0) {
            getFeature.setMaxFeatures(BigInteger.valueOf(maxFeatures));
        }
        getFeature.setResultType(ResultTypeType.RESULTS_LITERAL);

        QueryType wfsQuery = factory.createQueryType();
        wfsQuery.setTypeName(Collections.singletonList(query.getTypeName()));

        wfsQuery.setFilter(serverFilter);
        try {
            wfsQuery.setSrsName(new URI(srsName));
        } catch (URISyntaxException e) {
            throw new RuntimeException("Can't create a URI from the query CRS: " + srsName, e);
        }
        if (!query.retrieveAllProperties()) {
            String[] propertyNames = query.getPropertyNames();
            List propertyName = wfsQuery.getPropertyName();
            for (String propName : propertyNames) {
                propertyName.add(propName);
            }
        }
        SortBy[] sortByList = query.getSortBy();
        if (sortByList != null && sortByList.length > 0) {
            for (SortBy sortBy : sortByList) {
                wfsQuery.getSortBy().add(sortBy);
            }
        }

        getFeature.getQuery().add(wfsQuery);

        RequestComponents reqParts = new RequestComponents();
        // @TODO filter splitting!
        reqParts.setPostFilter(postFilter);
        reqParts.setServerRequest(getFeature);

        Map<String, String> parametersForGet = buildGetFeatureParametersForGet(getFeature);
        reqParts.setKvpParameters(parametersForGet);

        return reqParts;
    }

    protected Map<String, String> buildGetFeatureParametersForGet(GetFeatureType request)
            throws IOException {
        Map<String, String> map = new HashMap<String, String>();
        map.put("SERVICE", "WFS");
        map.put("VERSION", "1.1.0");
        map.put("REQUEST", "GetFeature");
        map.put("OUTPUTFORMAT", request.getOutputFormat());

        if (request.getMaxFeatures() != null) {
            map.put("MAXFEATURES", String.valueOf(request.getMaxFeatures()));
        }

        final QueryType query = (QueryType) request.getQuery().get(0);
        final String typeName = (String) query.getTypeName().get(0);
        map.put("TYPENAME", typeName);

        if (query.getPropertyName().size() > 0) {
            List<String> propertyNames = query.getPropertyName();
            StringBuilder pnames = new StringBuilder();
            for (Iterator<String> it = propertyNames.iterator(); it.hasNext();) {
                pnames.append(it.next());
                if (it.hasNext()) {
                    pnames.append(',');
                }
            }
            map.put("PROPERTYNAME", pnames.toString());
        }

        // SRSNAME parameter. Let the server reproject.
        // TODO: should check if the server supports the required crs
        URI srsName = query.getSrsName();
        if (srsName != null) {
            map.put("SRSNAME", srsName.toString());
        }
        final Filter filter = query.getFilter();

        if (filter != null && Filter.INCLUDE != filter) {
            if (filter instanceof Id) {
                final Set<Identifier> identifiers = ((Id) filter).getIdentifiers();
                StringBuffer idValues = new StringBuffer();
                for (Iterator<Identifier> it = identifiers.iterator(); it.hasNext();) {
                    Object id = it.next().getID();
                    // REVISIT: should URL encode the id?
                    idValues.append(String.valueOf(id));
                    if (it.hasNext()) {
                        idValues.append(",");
                    }
                }
                map.put("FEATUREID", idValues.toString());
            } else {
                String xmlEncodedFilter = encodeGetFeatureGetFilter(filter);
                map.put("FILTER", xmlEncodedFilter);
            }
        }

        return map;
    }

    /**
     * Returns a single-line string containing the xml representation of the given filter, as
     * appropriate for the {@code FILTER} parameter in a GetFeature request.
     */
    protected String encodeGetFeatureGetFilter(final Filter filter) throws IOException {
        Configuration filterConfig = getFilterConfiguration();
        Encoder encoder = new Encoder(filterConfig);
        // do not write the xml declaration
        encoder.setOmitXMLDeclaration(true);
        encoder.setEncoding(Charset.forName("UTF-8"));

        OutputStream out = new ByteArrayOutputStream();
        encoder.encode(filter, OGC.Filter, out);
        String encoded = out.toString();
        encoded = encoded.replaceAll("\n", "");
        return encoded;
    }

    protected Configuration getFilterConfiguration() {
        return filter_1_1_0_Configuration;
    }

    /**
     * Splits the filter provided by the geotools query into the server supported and unsupported
     * ones.
     * 
     * @param ds
     * @param wfs
     * @param queryFilter
     * @return a two-element array where the first element is the supported filter and the second
     *         the one to post-process
     */
    protected Filter[] splitFilters(WFS_1_1_0_DataStore ds, WFSProtocol wfs, Filter queryFilter) {
        FilterCapabilities filterCapabilities = wfs.getFilterCapabilities();
        Capabilities caps = new Capabilities(filterCapabilities);
        PostPreProcessFilterSplittingVisitor splitter = new PostPreProcessFilterSplittingVisitor(
                caps, null, null);

        queryFilter.accept(splitter, null);

        Filter server = splitter.getFilterPre();
        Filter post = splitter.getFilterPost();

        return new Filter[] { server, post };
    }

    /**
     * Checks if the query requested CRS is supported by the query feature type and if not, adapts
     * the query to the feature type default CRS, returning the CRS identifier to use for the WFS
     * query.
     * <p>
     * If the query CRS is not advertised as supported in the WFS capabilities for the requested
     * feature type, the query filter is modified so that any geometry literal is reprojected to the
     * default CRS for the feature type, otherwise the query is not modified at all. In any case,
     * the crs identifier to actually use in the WFS GetFeature operation is returned.
     * </p>
     * 
     * @param query
     * @return
     * @throws IOException
     */
    protected String adaptQueryForSupportedCrs(WFS_1_1_0_DataStore ds, WFSProtocol wfs,
            DefaultQuery query) throws IOException {
        // The CRS the query is performed in
        final String typeName = query.getTypeName();
        final CoordinateReferenceSystem queryCrs = query.getCoordinateSystem();
        final String defaultCrs = wfs.getDefaultCRS(typeName);

        if (queryCrs == null) {
            LOGGER.warning("Query does not provides a CRS, using default: " + query);
            return defaultCrs;
        }

        String epsgCode;

        final CoordinateReferenceSystem crsNative = ds.getFeatureTypeCRS(typeName);

        if (CRS.equalsIgnoreMetadata(queryCrs, crsNative)) {
            epsgCode = defaultCrs;
            LOGGER.fine("request and native crs for " + typeName + " are the same: " + epsgCode);
        } else {
            boolean transform = false;
            epsgCode = GML2EncodingUtils.epsgCode(queryCrs);
            if (epsgCode == null) {
                LOGGER.fine("Can't find the identifier for the request CRS, "
                        + "query will be performed in native CRS");
                transform = true;
            } else {
                epsgCode = "EPSG:" + epsgCode;
                LOGGER.fine("Request CRS is " + epsgCode + ", checking if its supported for "
                        + typeName);

                Set<String> supportedCRSIdentifiers = wfs.getSupportedCRSIdentifiers(typeName);
                if (supportedCRSIdentifiers.contains(epsgCode)) {
                    LOGGER.fine(epsgCode + " is supported, request will be performed asking "
                            + "for reprojection over it");
                } else {
                    LOGGER.fine(epsgCode + " is not supported for " + typeName
                            + ". Query will be adapted to default CRS " + defaultCrs);
                    transform = true;
                }
                if (transform) {
                    epsgCode = defaultCrs;
                    FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
                    SimpleFeatureType ftype = ds.getSchema(typeName);
                    ReprojectingFilterVisitor visitor = new ReprojectingFilterVisitor(ff, ftype);
                    Filter filter = query.getFilter();
                    Filter reprojectedFilter = (Filter) filter.accept(visitor, null);
                    if (LOGGER.isLoggable(Level.FINER)) {
                        LOGGER.finer("Original Filter: " + filter + "\nReprojected filter: "
                                + reprojectedFilter);
                    }
                    LOGGER.fine("Query filter reprojected to native CRS for " + typeName);
                    query.setFilter(reprojectedFilter);
                }
            }
        }
        return epsgCode;
    }

    protected int getMaxFeatures(WFS_1_1_0_DataStore ds, Query query) {
        int maxFeaturesDataStoreLimit = ds.getMaxFeatures().intValue();
        int queryMaxFeatures = query.getMaxFeatures();
        int maxFeatures = -1;
        if (Query.DEFAULT_MAX != queryMaxFeatures) {
            maxFeatures = queryMaxFeatures;
        }
        if (maxFeaturesDataStoreLimit > 0) {
            if (maxFeatures == -1) {
                maxFeatures = maxFeaturesDataStoreLimit;
            } else {
                maxFeatures = Math.min(maxFeaturesDataStoreLimit, maxFeatures);
            }
        }
        return maxFeatures;
    }

}