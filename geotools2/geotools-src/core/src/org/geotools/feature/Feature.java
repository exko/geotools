/*
 * Feature.java
 *
 * Created on April 29, 2002, 3:46 PM
 */
package org.geotools.feature;

import com.vividsolutions.jts.geom.Geometry;

/** 
 * <p>Represents a feature of arbitrary complexity.
 *
 * This interface answers the question: How do we access feature attributes?
 * The most generic approach would be to pass all feature attributes as objects
 * and use Java variable and method references to access them.  However, this
 * is also the most useless approach because it establishes no unified methods
 * for getting attribute information (since it is totally Object dependent),
 * without elaborate reflection/introspection, which is inconvenient to use.
 * Unlike, its <code>FeatureType</code> counterpart, this interface does not attempt
 * to serve as a typing framework.  Rather, multiple implementations of
 * this interface should generally be for performance reasons.</p>
 *
 * <p>This interface serves two important purposes.  Most obviously, it
 * gives users of features a unified, consistent framework for
 * accessing and manipulating feature data.  Perhaps more importantly, 
 * the <code>FeatureType</code> and <code>Feature</code> interfaces also
 * work together to give implementers a framework for constraining
 * and enforcing constraints (respectively) on allowed feature types.
 * As such, this interface is as general as possible in terms of the
 * types of objects to which it provides access.
 * Keep in mind that creating new features is relatively difficult and should
 * only be done to optimize performance for highly constrained schema types.
 * For the vast majority of schemas, the generic feature implementation will 
 * work fine.</p>
 * 
 * <p><b>Notes for Feature Clients:</b><br>
 * Clients should always use feature accessor methods (getAttribute and
 * setAttribute) to modify the state of internal attribute objects.  It is
 * possible that some feature implementations will allow object state changes
 * by clients outside of the class, but this is strongly discouraged.
 * In general, feature implementations will make defensive copies of objects
 * passed to clients and it is therefore not guaranteed that client state
 * changes that take place outside of the feature will be reflected in the 
 * internal state of the feature object!  <i>For this reason, clients should
 * always use the set methods to change feature attribute object states!</i></p>
 * 
 * <p><b>Notes for Feature Implementers:</b><br>
 * It is the responsibility of the implementing class to ensure that the
 * <code>Feature</code> attributes stay synchronized with its FeatureType definition.
 * <i>Features should never get out of synch with thier declared schemas and
 * should never alter thier schemas!</i>  There are a four conventions to which
 * implementers of this interface must be aware in order to successfully manage
 * a <code>Feature</code>:</p><ol>
 *
 * <li><b>FeatureType Reference</b><br>
 * Features must always hold a single (immutable: see <code>FeatureType</code>) schema
 * reference and this reference should not be altered after a feature
 * has been created.  To ensure this, is is strongly recommended that features
 * take a valid reference to an existing immutable schema in its constructor and
 * declaure that reference final.</li>
 *
 * <li><b>Default Geometry</b><br>
 * Each feature must have a default geometry, but this primary geometry may
 * be null.  This means that a feature may contain no geometries, but it must
 * always have a method for accessing a geometry object (even if it is null).
 * It also means that a feature with multiple geometries must pick one as its
 * default geometry.  Note that the designation of the default geometry is 
 * stored as part of the <code>FeatureType</code> and is therefore immmutable.
 *
 * <li><b>Attributes</b><br>
 * All features contain zero or more attributes, which can have one or more
 * occurences inside the feature.  Attributes may be any valid Java object.
 * If attributes are instances of <code>Feature</code>, they are handled
 * specially by the <code>Feature</code> methods, in that their attributes
 * may be accessed directly by thier containing feature.  All other object
 * variables and methods must be accessed through the objects themselves.
 * It is up to implementers of <code>Feature</code> to make sure that each
 * attribute value conforms to its internal schema.  A feature should never
 * reach a state where its attributes (or sub-attributes) do not conform to
 * thier <code>FeatureType</code> definitions.  There are three ways to implement
 * this.  The first is to simply make features immutable; however, given the
 * ubiquity and required flexibility of features, this is likely not possible.
 * The second (and second easiest), is to make all feature attributes immutable.
 * For most cases, this is probably the best way to handle this issue.  The
 * third way, is to never give out a reference that would allow a client to 
 * change an attribute object's class (most obviously, an array reference).
 * Generally speaking features should attempt to minimize external object
 * references by attempting to clone incoming attributes before
 * adding them and outgoing attributes before sending them.  For features with
 * non-cloneable attributes, of course, this is not possible, so this is left
 * to the discretion of the implementor.</li>
 * 
 * <li><b>Constructors</b><br>
 * Constructors should take arguments with enough information to create a
 * valid representation of the feature.  They should also always include a 
 * valid schema that can be used to check the proposed attributes.  This
 * is necessary to ensure that the feature is always in a valid state,
 * relative to its schema.</ol>

 * @author James MacGill, CCG<br>
 * @author Rob Hranac, VFNY
 * @see org.geotools.datasource.FeatureType 
 * @see org.geotools.datasource.FeatureFlat
 */
public interface Feature {

    /** 
     * Gets a reference to the schema for this feature.
     *
     * @return A copy of this feature's schema.
     */
    public FeatureType getSchema();


    /* ************************************************************************
     * Attribute extraction methods.
     * ************************************************************************/
    /** 
     * Gets all attributes from this feature, returned as a complex object
     * array.  This array comes with no metadata, so to interpret this 
     * collection the caller class should ask for the schema as well.
     *
     * @return A copy of all feature attributes.
     */
    public Object[] getAttributes();

    /** 
     * Gets an attribute for this feature at the location specified by xPath.
     *
     * @param xPath XPath representation of attribute location.
     * @return A copy of the requested attribute.
     * @throws IllegalFeatureException Requested attribute not found.
     */
    public Object getAttribute(String xPath)
        throws IllegalFeatureException;

    /** 
     * Gets an attribute for this feature at the location specified by xPath
     * and assumes that the attribute has multiple occurances.  If it does
     * not, will throw an exception.
     *
     * @param xPath XPath representation of attribute location.
     * @return A copy of all requested feature attributes.
     * @throws IllegalFeatureException Requested attribute does not have 
     * multiple instance or does not exist.
     */
    public Object[] getAttributes(String xPath)
        throws IllegalFeatureException;


    /* ************************************************************************
     * Attribute setting methods.
     * ************************************************************************/
    /** 
     * Sets all attributes for this feature, passed as a complex object
     * array.  Note that this array must conform to the internal schema
     * for this feature, or it will throw an exception.  Checking this is, of
     * course, left to the feature to do internally.  Well behaved features
     * should always fully check the passed attributes against thier schema
     * before adding them.
     *
     * @param attributes All feature attributes.
     * @throws IllegalFeatureException Passed attributes do not match schema.
     */
    public void setAttributes(Object[] attributes)
        throws IllegalFeatureException;

    /** 
     * Sets a single attribute for this feature, passed as a complex object.
     * If the attribute does not exist or the object does not conform to the
     * internal schema, an exception is thrown.  Checking this is, of
     * course, left to the feature to do internally.  Well behaved features
     * should always fully check the passed attributes against thier schema
     * before adding them.
     *
     * @param xPath XPath representation of attribute location.
     * @param attribute Feature attribute to set.
     * @throws IllegalFeatureException Passed attribute does not match schema
     */
    public void setAttribute(String xPath, Object attribute)
        throws IllegalFeatureException;


    /* ************************************************************************
     * Geometry handling methods - for convenience only.                      *
     * ************************************************************************/
    /** 
     * Gets the default geometry for this feature.
     *
     * @return Default geometry for this feature.
     */
    public Geometry getDefaultGeometry();

    /** 
     * Sets the default geometry for this feature.
     *
     * @param geometry The geometry to set.
     */
    public void setDefaultGeometry(Geometry geometry)
        throws IllegalFeatureException;


}

