package org.opengis.cs;
import org.opengis.pt.*;

/** Builds up complex objects from simpler objects or values.
  * CS_CoordinateSystemFactory allows applications to make coordinate
  * systems that cannot be created by a CS_CoordinateSystemAuthorityFactory.
  * This factory is very flexible, whereas the authority factory is easier to use.
  *
  * So CS_CoordinateSystemAuthorityFactory can be used to make 'standard'
  * coordinate systems, and CS_CoordinateSystemAuthorityFactory can be used to make
  * 'special' coordinate systems.
  *
  * For example, the EPSG authority has codes for USA state plane coordinate systems
  * using the NAD83 datum, but these coordinate systems always use meters.  EPSG does
  * not have codes for NAD83 state plane coordinate systems that use feet units.  This
  * factory lets an application create such a hybrid coordinate system.
  */
public interface CS_CoordinateSystemFactory extends java.rmi.Remote
{
    /** Creates a coordinate system object from an XML string.
     *  @param xml Coordinate system encoded in XML format.
     */
    CS_CoordinateSystem createFromXML(String xml);

    /** Creates a coordinate system object from a Well-Known Text string.
     *  @param wellKnownText Coordinate system encoded in Well-Known Text format.
     */
    CS_CoordinateSystem createFromWKT(String wellKnownText);

    /** Creates a compound coordinate system.
     *  @param name Name to give new object.
     *  @param head Coordinate system to use for earlier ordinates.
     *  @param tail Coordinate system to use for later ordinates.
     */
    CS_CompoundCoordinateSystem createCompoundCoordinateSystem(String name,CS_CoordinateSystem head,CS_CoordinateSystem tail);

    /** Creates a fitted coordinate system.
     *  The units of the axes in the fitted coordinate system will be inferred
     *  from the units of the base coordinate system.  If the affine map
     *  performs a rotation, then any mixed axes must have identical units.
     *  For example, a (lat_deg,lon_deg,height_feet) system can be rotated in
     *  the (lat,lon) plane, since both affected axes are in degrees.  But you
     *  should not rotate this coordinate system in any other plane.
     *  @param name Name to give new object.
     *  @param base Coordinate system to base the fitted CS on.
     *  @param toBaseWKT Well-Known Text of transform from returned CS to base CS.
     *  @param arAxes Axes for fitted coordinate system.  The number of axes must match the source dimension of the transform "toBaseWKT".
     */
    CS_FittedCoordinateSystem createFittedCoordinateSystem(String name,CS_CoordinateSystem base,String toBaseWKT,CS_AxisInfo[] arAxes);

    /** Creates a local coordinate system.
     *  The dimension of the local coordinate system is determined by the size
     *  of the axis array.  All the axes will have the same units.  If you want
     *  to make a coordinate system with mixed units, then you can make a
     *  compound coordinate system from different local coordinate systems.
     *  @param name Name to give new object.
     *  @param datum Local datum to use in created CS.
     *  @param unit Units to use for all axes in created CS.
     *  @param arAxes Axes to use in created CS.
     */
    CS_LocalCoordinateSystem createLocalCoordinateSystem(String name,CS_LocalDatum datum,CS_Unit unit,CS_AxisInfo[] arAxes);

    /** Creates an ellipsoid from radius values.
     *  @param name Name to give new object.
     *  @param semiMajorAxis Equatorial radius in supplied linear units.
     *  @param semiMinorAxis Polar radius in supplied linear units.
     *  @param linearUnit Linear units of ellipsoid axes.
     */
    CS_Ellipsoid createEllipsoid(String name,double semiMajorAxis,double semiMinorAxis,CS_LinearUnit linearUnit);

    /** Creates an ellipsoid from an major radius, and inverse flattening.
     *  @param name Name to give new object.
     *  @param semiMajorAxis Equatorial radius in supplied linear units.
     *  @param inverseFlattening Eccentricity of ellipsoid.
     *  @param linearUnit Linear units of major axis.
     */
    CS_Ellipsoid createFlattenedSphere(String name,double semiMajorAxis,double inverseFlattening,CS_LinearUnit linearUnit);

    /** Creates a projected coordinate system using a projection object.
     *  @param name Name to give new object.
     *  @param gcs Geographic coordinate system to base projection on.
     *  @param projection Projection from GCS to PCS.
     *  @param linearUnit Linear units of returned PCS.
     *  @param axis0 Details of 0th ordinates in returned PCS coordinates.
     *  @param axis1 Details of 1st ordinates in returned PCS coordinates.
     */
    CS_ProjectedCoordinateSystem createProjectedCoordinateSystem(String name,CS_GeographicCoordinateSystem gcs,CS_Projection projection,CS_LinearUnit linearUnit,CS_AxisInfo axis0,CS_AxisInfo axis1);

    /** Creates a projection.
     *  @param name Name to give new object.
     *  @param wktProjectionClass Classification string for projection (e.g. "Transverse_Mercator").
     *  @param parameters Parameters to use for projection, in units of intended PCS.
     */
    CS_Projection createProjection(String name,String wktProjectionClass,CS_ProjectionParameter[] parameters);

    /** Creates horizontal datum from ellipsoid and Bursa-Wolf parameters.
     *  Since this method contains a set of Bursa-Wolf parameters, the created datum
     *  will always have a relationship to WGS84.  If you wish to create a horizontal datum
     *  that has no relationship with WGS84, then you can either specify CS_HD_Other as the
     *  horizontalDatumType, or create it via WKT.
     *  @param name Name to give new object.
     *  @param horizontalDatumType Type of horizontal datum to create.
     *  @param ellipsoid Ellipsoid to use in new horizontal datum.
     *  @param toWGS84 Suggested approximate conversion from new datum to WGS84.
     */
    CS_HorizontalDatum createHorizontalDatum(String name,CS_DatumType horizontalDatumType,CS_Ellipsoid ellipsoid,CS_WGS84ConversionInfo toWGS84);

    /** Creates a prime meridian, relative to Greenwich.
     *  @param name Name to give new object.
     *  @param angularUnit Angular units of longitude.
     *  @param longitude Longitude of prime meridian in supplied angular units East of Greenwich.
     */
    CS_PrimeMeridian createPrimeMeridian(String name,CS_AngularUnit angularUnit,double longitude);

    /** Creates a GCS, which could be Lat/Lon or Lon/Lat.
     *  @param name Name to give new object.
     *  @param angularUnit Angular units for created GCS.
     *  @param horizontalDatum Horizontal datum for created GCS.
     *  @param primeMeridian Prime Meridian for created GCS.
     *  @param axis0 Details of 0th ordinates in returned GCS coordinates.
     *  @param axis1 Details of 1st ordinates in returned GCS coordinates.
     */
    CS_GeographicCoordinateSystem createGeographicCoordinateSystem(String name,CS_AngularUnit angularUnit,CS_HorizontalDatum horizontalDatum,CS_PrimeMeridian primeMeridian,CS_AxisInfo axis0,CS_AxisInfo axis1);

    /** Creates a local datum.
     *  @param name Name to give new object.
     *  @param localDatumType Type of local datum to create.
     */
    CS_LocalDatum createLocalDatum(String name,CS_DatumType localDatumType);

    /** Creates a vertical datum from an enumerated type value.
     *  @param name Name to give new object.
     *  @param verticalDatumType Type of vertical datum to create.
     */
    CS_VerticalDatum createVerticalDatum(String name,CS_DatumType verticalDatumType);

    /** Creates a vertical coordinate system from a datum and linear units.
     *  @param name Name to give new object.
     *  @param verticalDatum Datum to use for new coordinate system.
     *  @param verticalUnit Units to use for new coordinate system.
     *  @param axis Axis to use for new coordinate system.
     */
    CS_VerticalCoordinateSystem createVerticalCoordinateSystem(String name,CS_VerticalDatum verticalDatum,CS_LinearUnit verticalUnit,CS_AxisInfo axis);
}

