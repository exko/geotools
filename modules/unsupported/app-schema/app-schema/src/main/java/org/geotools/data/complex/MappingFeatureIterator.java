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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.complex.filter.XPath;
import org.geotools.data.complex.filter.XPath.Step;
import org.geotools.data.complex.filter.XPath.StepList;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AppSchemaFeatureFactoryImpl;
import org.geotools.feature.AttributeBuilder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureImpl;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.GeometryAttributeImpl;
import org.geotools.feature.Types;
import org.geotools.feature.type.GeometryDescriptorImpl;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.FilterFactoryImplNamespaceAware;
import org.geotools.xlink.XLINK;
import org.opengis.feature.Attribute;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureFactory;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.NamespaceSupport;

import com.vividsolutions.jts.geom.Geometry;

/**
 * A Feature iterator that operates over the FeatureSource of a
 * {@linkplain org.geotools.data.complex.FeatureTypeMapping} and produces Features of the output
 * schema by applying the mapping rules to the Features of the source schema.
 * <p>
 * This iterator acts like a one-to-one mapping, producing a Feature of the target type for each
 * feature of the source type.
 * 
 * @author Gabriel Roldan, Axios Engineering
 * @author Ben Caradoc-Davies, CSIRO Exploration and Mining
 * @author Rini Angreani, Curtin University of Technology
 * @version $Id$
 * @source $URL$
 * @since 2.4
 */
public class MappingFeatureIterator implements Iterator<Feature>, FeatureIterator<Feature> {
    /**
     * Name representation of xlink:href
     */
    public static final Name XLINK_HREF_NAME = Types.toTypeName(XLINK.HREF);

    /**
     * The mappings for the source and target schemas
     */
    protected FeatureTypeMapping mapping;

    /**
     * Expression to evaluate the feature id
     */
    protected Expression featureFidMapping;

    /**
     * Factory used to create the target feature and attributes
     */
    protected FeatureFactory attf;

    protected FeatureCollection<FeatureType, Feature> sourceFeatures;
    
    private static final Logger LOGGER = org.geotools.util.logging.Logging
    .getLogger(MappingFeatureIterator.class.getPackage().getName());

    private FeatureSource<FeatureType, Feature> mappedSource;

    /**
     * Hold on to iterator to allow features to be streamed.
     */
    protected Iterator<Feature> sourceFeatureIterator;

    protected AppSchemaDataAccess store;

    final protected XPath xpathAttributeBuilder;

    protected FilterFactory namespaceAwareFilterFactory;

    private NamespaceSupport namespaces;

    /**
     * maxFeatures restriction value as provided by query
     */
    private final int maxFeatures;

    /** counter to ensure maxFeatures is not exceeded */
    private int featureCounter;

    /**
     * This is the feature that will be processed in next()
     */
    private Feature curSrcFeature;

    /**
     * True if hasNext has been called prior to calling next()
     */
    private boolean hasNextCalled = false;

    /**
     * 
     * @param store
     * @param mapping
     *                place holder for the target type, the surrogate FeatureSource and the mappings
     *                between them.
     * @param query
     *                the query over the target feature type, that is to be unpacked to its
     *                equivalent over the surrogate feature type.
     * @throws IOException
     */
    public MappingFeatureIterator(AppSchemaDataAccess store, FeatureTypeMapping mapping, Query query)
            throws IOException {
        this.store = store;
        this.attf = new AppSchemaFeatureFactoryImpl();
        Name name = mapping.getTargetFeature().getName();

        List<AttributeMapping> attributeMappings = mapping.getAttributeMappings();

        for (AttributeMapping attMapping : attributeMappings) {
            StepList targetXPath = attMapping.getTargetXPath();
            if (targetXPath.size() > 1) {
                continue;
            }
            Step step = (Step) targetXPath.get(0);
            QName stepName = step.getName();
            if (Types.equals(name, stepName)) {
                featureFidMapping = attMapping.getIdentifierExpression();
                break;
            }
        }

        this.mapping = mapping;

        if (featureFidMapping == null || Expression.NIL.equals(featureFidMapping)) {
            FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
            featureFidMapping = ff.property("@id");
        }

        Query unrolledQuery = getUnrolledQuery(query);

        mappedSource = mapping.getSource();

        sourceFeatures = mappedSource.getFeatures(unrolledQuery);

        this.sourceFeatureIterator = sourceFeatures.iterator();

        xpathAttributeBuilder = new XPath();
        xpathAttributeBuilder.setFeatureFactory(attf);
        namespaces = mapping.getNamespaces();
        namespaceAwareFilterFactory = new FilterFactoryImplNamespaceAware(namespaces);
        xpathAttributeBuilder.setFilterFactory(namespaceAwareFilterFactory);
        this.maxFeatures = query.getMaxFeatures();

    }

    /**
     * Shall not be called, just throws an UnsupportedOperationException
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Closes the underlying FeatureIterator
     */
    public void close() {
        if (sourceFeatures != null && sourceFeatureIterator != null) {
            sourceFeatures.close(sourceFeatureIterator);
            sourceFeatureIterator = null;
            sourceFeatures = null;
        }
    }

    /**
     * Based on the set of xpath expression/id extracting expression, finds the ID for the attribute
     * <code>attributeXPath</code> from the source complex attribute.
     * 
     * @param attributeXPath
     *                the location path of the attribute to be created, for which to obtain the id
     *                by evaluating the corresponding <code>org.geotools.filter.Expression</code>
     *                from <code>sourceInstance</code>.
     * @param sourceInstance
     *                a complex attribute which is the source of the mapping.
     * @return the ID to be applied to a new attribute instance addressed by
     *         <code>attributeXPath</code>, or <code>null</code> if there is no an id mapping
     *         for that attribute.
     */
    protected String extractIdForAttribute(final Expression idExpression,
            ComplexAttribute sourceInstance) {
        String value = (String) idExpression.evaluate(sourceInstance, String.class);
        return value;
    }

    protected String extractIdForFeature(ComplexAttribute sourceInstance) {
        String fid = (String) featureFidMapping.evaluate(sourceInstance, String.class);
        return fid;
    }

    protected Object getValue(Expression expression, Object sourceFeature) {
        Object value;
        value = expression.evaluate(sourceFeature);
        if (value instanceof Attribute) {
            value = ((Attribute) value).getValue();
        }
        return value;
    }

    protected Object getValues(boolean isMultiValued, Expression expression,
            ComplexAttribute sourceFeature) {
        if (isMultiValued && sourceFeature instanceof FeatureImpl
                && expression instanceof AttributeExpressionImpl) {
            // RA: Feature Chaining
            // complex features can have multiple nodes of the same attribute.. and if they are used
            // as input to an app-schema data access to be nested inside another feature type of a
            // different XML type, it has to be mapped like this:
            // <AttributeMapping>
            // <targetAttribute>
            // gsml:composition
            // </targetAttribute>
            // <sourceExpression>
            // <inputAttribute>mo:composition</inputAttribute>
            // <linkElement>gsml:CompositionPart</linkElement>
            // <linkField>gml:name</linkField>
            // </sourceExpression>
            // <isMultiple>true</isMultiple>
            // </AttributeMapping>
            // As there can be multiple nodes of mo:composition in this case, we need to retrieve
            // all of them
            AttributeExpressionImpl attribExpression = ((AttributeExpressionImpl) expression);
            String xpath = attribExpression.getPropertyName();
            StepList xpathSteps = XPath.steps(sourceFeature.getDescriptor(), xpath, namespaces);

            ArrayList<Object> values = new ArrayList<Object>();
            Collection<Property> properties = getProperties(sourceFeature, xpathSteps);
            for (Property property : properties) {
                Object value = property.getValue();
                if (value != null) {
                    if (value instanceof Collection) {
                        values.addAll((Collection) property.getValue());
                    } else {
                        values.add(property.getValue());
                    }
                }
            }
            return values;
        }
        return getValue(expression, sourceFeature);
    }

    /**
     * Sets the values of grouping attributes.
     * 
     * @param sourceFeature
     * @param groupingMappings
     * @param targetFeature
     * 
     * @return Feature. Target feature sets with simple attributes
     */
    protected void setAttributeValue(Feature target, final ComplexAttribute source,
            final AttributeMapping attMapping) throws IOException {

        final Expression sourceExpression = attMapping.getSourceExpression();
        final AttributeType targetNodeType = attMapping.getTargetNodeInstance();
        final StepList xpath = attMapping.getTargetXPath();
        Map<Name, Expression> clientPropsMappings = attMapping.getClientProperties();

        boolean isNestedFeature = attMapping.isNestedAttribute();
        Object value = getValues(attMapping.isMultiValued(), sourceExpression, source);
        boolean isHRefLink = isByReference(clientPropsMappings, isNestedFeature);
        if (isNestedFeature) {
            // get built feature based on link value
            if (value instanceof Collection) {
                ArrayList<Feature> nestedFeatures = new ArrayList<Feature>(((Collection) value)
                        .size());
                for (Object val : (Collection) value) {
                    while (val instanceof Attribute) {
                        val = ((Attribute) val).getValue();
                    }
                    if (isHRefLink) {
                        // get the input features to avoid infinite loop in case the nested
                        // feature type also have a reference back to this type
                        // eg. gsml:GeologicUnit/gsml:occurence/gsml:MappedFeature
                        // and gsml:MappedFeature/gsml:specification/gsml:GeologicUnit
                        nestedFeatures.addAll(((NestedAttributeMapping) attMapping)
                                .getInputFeatures(val));
                    } else {
                        nestedFeatures.addAll(((NestedAttributeMapping) attMapping)
                                .getFeatures(val));
                    }
                }
                value = nestedFeatures;
            } else if (isHRefLink) {
                // get the input features to avoid infinite loop in case the nested
                // feature type also have a reference back to this type
                // eg. gsml:GeologicUnit/gsml:occurence/gsml:MappedFeature
                // and gsml:MappedFeature/gsml:specification/gsml:GeologicUnit
                value = ((NestedAttributeMapping) attMapping).getInputFeatures(value);
            } else {
                value = ((NestedAttributeMapping) attMapping).getFeatures(value);
            }
            if (isHRefLink) {
                // only need to set the href link value, not the nested feature properties
                setXlinkReference(target, clientPropsMappings, value, xpath, targetNodeType);
                return;
            }
        }
        String id = null;
        if (Expression.NIL != attMapping.getIdentifierExpression()) {
            id = extractIdForAttribute(attMapping.getIdentifierExpression(), source);
        }
        if (isNestedFeature) {
            assert (value instanceof Collection);
        }
        if (value instanceof Collection) {
            // nested feature type could have multiple instances as the whole purpose
            // of feature chaining is to cater for multi-valued properties
            for (Object singleVal : (Collection) value) {
                ArrayList<Property> valueList = new ArrayList<Property>();
                valueList.add((Property) singleVal);
                Attribute instance = xpathAttributeBuilder.set(target, xpath, valueList, id,
                        targetNodeType, false);
                setClientProperties(instance, source, clientPropsMappings);
            }
        } else {
            Attribute instance = xpathAttributeBuilder.set(target, xpath, value, id,
                    targetNodeType, false);
            setClientProperties(instance, source, clientPropsMappings);
        }
    }

    /**
     * Set xlink:href client property for multi-valued chained features. This has to be specially
     * handled because we don't want to encode the nested features attributes, since it's already an
     * xLink. Also we need to eliminate duplicates.
     * 
     * @param target
     *            The target feature
     * @param clientPropsMappings
     *            Client properties mappings
     * @param value
     *            Nested features
     * @param xpath
     *            Attribute xPath where the client properties are to be set
     * @param targetNodeType
     *            Target node type
     */
    private void setXlinkReference(Feature target, Map<Name, Expression> clientPropsMappings,
            Object value, StepList xpath, AttributeType targetNodeType) {
        // Make sure the same value isn't already set
        // in case it comes from a denormalized view for many-to-many relationship.
        // (1) Get the first existing value
        Property existingAttribute = getProperty(target, xpath);

        if (existingAttribute != null) {
            Object existingValue = existingAttribute.getUserData().get(Attributes.class);
            if (existingValue != null) {
                assert existingValue instanceof HashMap;
                existingValue = ((Map) existingValue).get(XLINK_HREF_NAME);
            }
            if (existingValue != null) {
                Expression linkExpression = clientPropsMappings.get(XLINK_HREF_NAME);
                for (Object singleVal : (Collection) value) {
                    assert singleVal instanceof Feature;
                    assert linkExpression != null;
                    Object hrefValue = linkExpression.evaluate(singleVal);
                    if (hrefValue != null && hrefValue.equals(existingValue)) {
                        // (2) if one of the new values matches the first existing value, 
                        // that means this comes from a denormalized view,
                        // and this set has already been set
                        return;
                    }
                }
            }
        }

        for (Object singleVal : (Collection) value) {
            assert singleVal instanceof Feature;
            Attribute instance = xpathAttributeBuilder.set(target, xpath, null, null,
                    targetNodeType, true);
            setClientProperties(instance, singleVal, clientPropsMappings);
        }
    }

    private void setClientProperties(final Attribute target, final Object source,
            final Map<Name, Expression> clientProperties) {
        if (clientProperties.size() == 0) {
            return;
        }
        final Map<Name, Object> targetAttributes = new HashMap<Name, Object>();
        for (Map.Entry<Name, Expression> entry : clientProperties.entrySet()) {
            Name propName = entry.getKey();
            Expression propExpr = entry.getValue();
            Object propValue = getValue(propExpr, source);
            targetAttributes.put(propName, propValue);
        }
        // FIXME should set a child Property
        target.getUserData().put(Attributes.class, targetAttributes);
    }

    /**
     * Return next feature.
     * 
     * @see java.util.Iterator#next()
     */
    public Feature next() {
        if (!hasNext()) {
            throw new IllegalStateException("there are no more features in this iterator");
        }
        hasNextCalled = false;
        Feature next;
        try {
            next = computeNext();
        } catch (IOException e) {
            close();
            throw new RuntimeException(e);
        }
        ++featureCounter;
        return next;
    }

    /**
     * Return true if there are more features.
     * 
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        if (hasNextCalled) {
            return curSrcFeature != null;
        }

        boolean exists = false;

        if (sourceFeatureIterator != null && featureCounter < maxFeatures) {
            exists = sourceFeatureIterator.hasNext();
            if (exists && this.curSrcFeature == null) {
                this.curSrcFeature = sourceFeatureIterator.next();
            }
        }

        if (!exists) {
            LOGGER.finest("no more features, produced " + featureCounter);
            close();
            curSrcFeature = null;
        }
        hasNextCalled = true;
        return exists;
    }

    /**
     * Return a query appropriate to its underlying feature source.
     * 
     * @param query
     *                the original query against the output schema
     * @return a query appropriate to be executed over the underlying feature source.
     */
    protected Query getUnrolledQuery(Query query) {
        return store.unrollQuery(query, mapping);
    }

    private Feature computeNext() throws IOException {
        assert this.curSrcFeature != null : "hasNext not called?";       

        String id = extractIdForFeature(curSrcFeature);
        
        ArrayList<Feature> sources = new ArrayList<Feature>();   
        sources.add(curSrcFeature);
        while (sourceFeatureIterator.hasNext()) {
            Feature next = sourceFeatureIterator.next();
            if (extractIdForFeature(next).equals(id)) {
                sources.add(next);
                curSrcFeature = null;
//                // ensure the next in the stream is called next time
//                hasNextCalled = false;
            } else {
                curSrcFeature = next;
                // ensure curSrcFeature is returned when next() is called
                hasNextCalled = true;
                break;
            }
        }
        
        final AttributeDescriptor targetNode = mapping.getTargetFeature();
        final Name targetNodeName = targetNode.getName();
        final List<AttributeMapping> mappings = mapping.getAttributeMappings();
        
        AttributeBuilder builder = new AttributeBuilder(attf);
        builder.setDescriptor(targetNode);
        Feature target = (Feature) builder.build(id);
        
        for (AttributeMapping attMapping : mappings) {
            StepList targetXpathProperty = attMapping.getTargetXPath();
            if (targetXpathProperty.size() == 1) {
                Step rootStep = (Step) targetXpathProperty.get(0);
                QName stepName = rootStep.getName();
                if (Types.equals(targetNodeName, stepName)) {
                    // ignore the top level mapping for the Feature itself
                    // as it was already set
                    continue;
                }
            }
            // extract the values from multiple source features of the same id
            // and set them to one built feature
            for (Feature source : sources) {
                setAttributeValue(target, source, attMapping);
            }
        }
        featureCounter++;
        if (target.getDefaultGeometryProperty() == null) {
            setGeometry(target);
        }
        return target;
    }

    /**
     * Set the feature geometry to that of the first property bound to a JTS geometry
     * 
     * @param feature
     */
    private void setGeometry(Feature feature) {
        // FIXME an ugly, ugly hack to smuggle a geometry into a feature
        // FeatureImpl.getBounds and GMLSchema do not work together
        for (final Property property : feature.getProperties()) {
            if (Geometry.class.isAssignableFrom(property.getType().getBinding())) {
                // need to manufacture a GeometryDescriptor so we can make a GeometryAttribute
                // in which we can store the Geometry
                AttributeType type = (AttributeType) property.getType();
                GeometryType geometryType = new GeometryTypeImpl(type.getName(), type.getBinding(),
                        null, type.isIdentified(), type.isAbstract(), type.getRestrictions(), type
                                .getSuper(), type.getDescription());
                AttributeDescriptor descriptor = (AttributeDescriptor) property.getDescriptor();
                GeometryDescriptor geometryDescriptor = new GeometryDescriptorImpl(geometryType,
                        descriptor.getName(), descriptor.getMinOccurs(), descriptor.getMaxOccurs(),
                        property.isNillable(), null);
                GeometryAttribute geometryAttribute = new GeometryAttributeImpl(
                        property.getValue(), geometryDescriptor, null);
                List<Property> properties = new ArrayList<Property>(feature.getProperties());
                properties.remove(property);
                properties.add(geometryAttribute);
                feature.setValue(properties);
                feature.setDefaultGeometryProperty(geometryAttribute);
                break;
            }
        }
    }

    /**
     * Returns first matching attribute from provided root and xPath.
     * 
     * @param root
     *            The root attribute to start searching from
     * @param xpath
     *            The xPath matching the attribute
     * @return The first matching attribute
     */
    private Property getProperty(ComplexAttribute root, StepList xpath) {
        Property property = root;

        final StepList steps = new StepList(xpath);

        Iterator<Step> stepsIterator = steps.iterator();

        while (stepsIterator.hasNext()) {
            assert property instanceof ComplexAttribute;
            Step step = stepsIterator.next();
            property = ((ComplexAttribute) property).getProperty(Types.toTypeName(step.getName()));
            if (property == null) {
                return null;
            }
        }
        return property;
    }

    /**
     * Return all matching properties from provided root attribute and xPath.
     * 
     * @param root
     *            The root attribute to start searching from
     * @param xpath
     *            The xPath matching the attribute
     * @return The matching attributes collection
     */
    private Collection<Property> getProperties(ComplexAttribute root, StepList xpath) {

        final StepList steps = new StepList(xpath);

        Iterator<Step> stepsIterator = steps.iterator();
        Collection<Property> properties = null;
        Step step = null;
        if (stepsIterator.hasNext()) {
            step = stepsIterator.next();
            properties = ((ComplexAttribute) root).getProperties(Types.toTypeName(step.getName()));
        }

        while (stepsIterator.hasNext()) {
            step = stepsIterator.next();
            Collection<Property> nestedProperties = new ArrayList<Property>();
            for (Property property : properties) {
                assert property instanceof ComplexAttribute;
                Collection<Property> tempProperties = ((ComplexAttribute) property)
                        .getProperties(Types.toTypeName(step.getName()));
                if (!tempProperties.isEmpty()) {
                    nestedProperties.addAll(tempProperties);
                }
            }
            properties.clear();
            if (nestedProperties.isEmpty()) {
                return properties;
            }
            properties.addAll(nestedProperties);
        }
        return properties;
    }

    /**
     * Checks if client property has xlink:ref in it, if the attribute is for chained features.
     * 
     * @param clientPropsMappings
     *            the client properties mappings
     * @param isNested
     *            true if we're dealing with chained/nested features
     * @return
     */
    private boolean isByReference(Map<Name, Expression> clientPropsMappings, boolean isNested) {
        // only care for chained features
        return isNested ? (clientPropsMappings.isEmpty() ? false : (clientPropsMappings
                .get(XLINK_HREF_NAME) == null) ? false : true) : false;
    }
}
