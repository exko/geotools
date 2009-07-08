/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2004-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.data.complex;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.geotools.data.DataAccess;
import org.geotools.data.DataAccessFinder;
import org.geotools.data.DataSourceException;
import org.geotools.data.FeatureSource;
import org.geotools.data.complex.config.NonFeatureTypeProxy;
import org.geotools.feature.Types;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

/**
 * This tests AppSchemaDataAccessRegistry class. When an appschema data access is created, it would
 * be registered in the registry. Once it's in the registry, its feature type mapping and feature
 * source (simple or mapped) would be accessible globally.
 * 
 * @author Rini Angreani, Curtin Universtiy of Technology
 */
public class AppSchemaDataAccessRegistryTest extends TestCase {

    public static final Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger("org.geotools.data.complex");

    static final String GSMLNS = "http://www.cgi-iugs.org/xml/GeoSciML/2";

    static final String GMLNS = "http://www.opengis.net/gml";

    static final Name MAPPED_FEATURE_TYPE = Types.typeName(GSMLNS, "MappedFeatureType");

    static final Name MAPPED_FEATURE = Types.typeName(GSMLNS, "MappedFeature");

    static final Name GEOLOGIC_UNIT_TYPE = Types.typeName(GSMLNS, "GeologicUnitType");

    static final Name GEOLOGIC_UNIT = Types.typeName(GSMLNS, "GeologicUnit");

    static final Name COMPOSITION_PART_TYPE = Types.typeName(GSMLNS, "CompositionPartType");

    static final Name COMPOSITION_PART = Types.typeName(GSMLNS, "CompositionPart");

    static final Name CGI_TERM_VALUE = Types.typeName(GSMLNS, "CGI_TermValue");

    static final Name CGI_TERM_VALUE_TYPE = Types.typeName(GSMLNS, "CGI_TermValueType");

    static final Name CONTROLLED_CONCEPT = Types.typeName(GSMLNS, "ControlledConcept");

    final String schemaBase = "/test-data/";

    /**
     * Geological unit data access
     */
    private AppSchemaDataAccess guDataAccess;

    /**
     * Compositional part data access
     */
    private AppSchemaDataAccess cpDataAccess;

    /**
     * Mapped feature data access
     */
    private AppSchemaDataAccess mfDataAccess;

    /**
     * CGI Term Value data access
     */
    private AppSchemaDataAccess cgiDataAccess;

    /**
     * Controlled Concept data access
     */
    private AppSchemaDataAccess ccDataAccess;

    /**
     * Test registering all data accesses works.
     * 
     * @throws Exception
     */
    public void testRegisterDataAccess() throws Exception {
        this.loadDataAccesses();

        this.checkRegisteredDataAccess(mfDataAccess, MAPPED_FEATURE, false);
        this.checkRegisteredDataAccess(guDataAccess, GEOLOGIC_UNIT, false);
        this.checkRegisteredDataAccess(cpDataAccess, COMPOSITION_PART, true);
        this.checkRegisteredDataAccess(cgiDataAccess, CGI_TERM_VALUE, true);
        this.checkRegisteredDataAccess(ccDataAccess, CONTROLLED_CONCEPT, true);

        DataAccessRegistry.unregisterAll();
    }

    /**
     * Test unregistering all data accesses works.
     * 
     * @throws Exception
     */
    public void testUnregisterDataAccess() throws Exception {
        this.loadDataAccesses();

        unregister(mfDataAccess, MAPPED_FEATURE);
        unregister(guDataAccess, GEOLOGIC_UNIT);
        unregister(cpDataAccess, COMPOSITION_PART);
        unregister(cgiDataAccess, CGI_TERM_VALUE);
        unregister(ccDataAccess, CONTROLLED_CONCEPT);

        DataAccessRegistry.unregisterAll();
    }

    /**
     * Test that asking for a nonexistent type causes an excception to be thrown with the correct
     * number of type names in the detail message.
     * 
     * @throws Exception
     */
    public void testThrowDataSourceException() throws Exception {
        loadDataAccesses();
        Name typeName = Types.typeName(GSMLNS, "DoesNotExist");
        boolean handledException = false;
        try {
            AppSchemaDataAccessRegistry.getMapping(typeName);
        } catch (DataSourceException e) {
            String message = e.getMessage();
            LOGGER.info(e.toString());
            assertEquals("Count number of available type names in exception message", 5, message
                    .split("Available:")[1].split(",").length);
            handledException = true;
        }
        assertTrue("Expected a DataSourceException to have been thrown and handled",
                handledException);
        DataAccessRegistry.unregisterAll();
    }

    /**
     * Load all data accesses
     * 
     * @throws Exception
     */
    private void loadDataAccesses() throws Exception {
        /**
         * Load Mapped Feature data access
         */
        Map dsParams = new HashMap();
        URL url = getClass().getResource(schemaBase + "MappedFeaturePropertyfile.xml");
        assertNotNull(url);
        dsParams.put("dbtype", "app-schema");
        dsParams.put("url", url.toExternalForm());
        mfDataAccess = (AppSchemaDataAccess) DataAccessFinder.getDataStore(dsParams);
        assertNotNull(mfDataAccess);

        /**
         * Load Geological Unit data access
         */
        url = getClass().getResource(schemaBase + "GeologicUnit.xml");
        assertNotNull(url);
        dsParams.put("url", url.toExternalForm());
        guDataAccess = (AppSchemaDataAccess) DataAccessFinder.getDataStore(dsParams);
        assertNotNull(guDataAccess);

        /**
         * Find Compositional Part data access
         */
        cpDataAccess = (AppSchemaDataAccess) DataAccessRegistry.getDataAccess(COMPOSITION_PART);
        assertNotNull(cpDataAccess);

        /**
         * Find CGI Term Value data access
         */
        cgiDataAccess = (AppSchemaDataAccess) DataAccessRegistry.getDataAccess(CGI_TERM_VALUE);
        assertNotNull(cgiDataAccess);

        /**
         * Find ControlledConcept data access
         */
        ccDataAccess = (AppSchemaDataAccess) DataAccessRegistry.getDataAccess(CONTROLLED_CONCEPT);
        assertNotNull(ccDataAccess);
    }

    /**
     * Tests that registry works.
     * 
     * @param dataAccess
     *            The app schema data access to check
     * @param typeName
     *            Feature type
     * @param isNonFeature
     *            true if the type is non feature
     * @throws IOException
     */
    private void checkRegisteredDataAccess(AppSchemaDataAccess dataAccess, Name typeName,
            boolean isNonFeature) throws IOException {
        FeatureTypeMapping mapping = AppSchemaDataAccessRegistry.getMapping(typeName);
        assertNotNull(mapping);
        // compare with the supplied data access
        assertEquals(dataAccess.getMapping(typeName).equals(mapping), true);
        if (isNonFeature) {
            assertEquals(mapping.getTargetFeature().getType() instanceof NonFeatureTypeProxy, true);
        }

        // should return a simple feature source
        FeatureSource<FeatureType, Feature> source = AppSchemaDataAccessRegistry
                .getSimpleFeatureSource(typeName);
        assertNotNull(source);
        assertEquals(mapping.getSource(), source);

        // should return a mapping feature source
        FeatureSource<FeatureType, Feature> mappedSource = DataAccessRegistry
                .getFeatureSource(typeName);
        assertNotNull(mappedSource);
        // compare with the supplied data access
        assertEquals(mappedSource.getDataStore().equals(dataAccess), true);
    }

    /**
     * Tests that unregistering data access works
     * 
     * @param dataAccess
     *            The data access
     * @param typeName
     *            The feature type name
     * @throws IOException
     */
    private void unregister(DataAccess dataAccess, Name typeName) throws IOException {
        DataAccessRegistry.unregister(dataAccess);
        boolean notFound = false;
        try {
            FeatureTypeMapping mapping = AppSchemaDataAccessRegistry.getMapping(typeName);
        } catch (DataSourceException e) {
            notFound = true;
        }
        if (!notFound) {
            fail("Expecting DataSourceException but didn't occur. Deregistering data access fails.");
        }
        notFound = false;
        try {
            FeatureSource source = AppSchemaDataAccessRegistry.getSimpleFeatureSource(typeName);
        } catch (DataSourceException e) {
            notFound = true;
        }
        if (!notFound) {
            fail("Expecting DataSourceException but didn't occur. Deregistering data access fails.");
        }
    }
}
