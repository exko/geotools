package org.opengis.cs;
import org.opengis.pt.*;

/** A base interface for metadata applicable to coordinate system objects.
 *  The metadata items 'Abbreviation', 'Alias', 'Authority', 'AuthorityCode',
 *  'Name' and 'Remarks' were specified in the Simple Features interfaces,
 *  so they have been kept here.
 *
 *  This specification does not dictate what the contents of these items should
 *  be.  However, the following guidelines are suggested:
 *
 *  When CS_CoordinateSystemAuthorityFactory is used to create an object, the
 *  'Authority' and 'AuthorityCode' values should be set to the authority name
 *  of the factory object, and the authority code supplied by the client,
 *  respectively.  The other values may or may not be set.  (If the authority is
 *  EPSG, the implementer may consider using the corresponding metadata values
 *  in the EPSG tables.)
 *
 *  When CS_CoordinateSystemFactory creates an object, the 'Name' should be set
 *  to the value supplied by the client.  All of the other metadata items should
 *  be left empty.
 */
public interface CS_Info extends java.rmi.Remote
{
  /** Gets the name. */
  String getName();

  /** Gets the authority name.
   *  An Authority is an organization that maintains definitions of Authority
   *  Codes.  For example the European Petroleum Survey Group (EPSG) maintains
   *  a database of coordinate systems, and other spatial referencing objects,
   *  where each object has a code number ID.  For example, the EPSG code for a
   *  WGS84 Lat/Lon coordinate system is '4326'.
   */
  String getAuthority();

  /** Gets the authority-specific identification code.
   *  The AuthorityCode is a compact string defined by an Authority to reference
   *  a particular spatial reference object.  For example, the European Survey
   *  Group (EPSG) authority uses 32 bit integers to reference coordinate systems,
   *  so all their code strings will consist of a few digits.  The EPSG code for
   *  WGS84 Lat/Lon is '4326'.
   *
   *  An empty string is used for no code.
   */
  String getAuthorityCode();

  /** Gets the alias.*/
  String getAlias();

  /** Gets the abbreviation.*/
  String getAbbreviation();

  /** Gets the provider-supplied remarks.*/
  String getRemarks();

  /** Gets a Well-Known text representation of this object.*/
  String getWKT();

  /** Gets an XML representation of this object.*/
  String getXML();
}

