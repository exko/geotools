/*
 *    Geotools - OpenSource mapping toolkit
 *    (C) 2002, Centre for Computational Geography
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
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

/*
 * JTSUtilities.java
 *
 * Created on March 5, 2003, 11:18 AM
 */
package org.geotools.data.shapefile.shp;

import com.vividsolutions.jts.algorithm.*;
import com.vividsolutions.jts.geom.*;

/** A collection of utility methods for use with JTS and the shapefile package.
 * @author aaime
 * @author Ian Schneider
 */
public class JTSUtilities {
  
  /** Creates a new instance of JTSUtilities */
  private JTSUtilities() {
  }
  
  /** Determine the min and max "z" values in an array of Coordinates.
   * @param cs The array to search.
   * @return An array of size 2, index 0 is min, index 1 is max.
   */  
  public static final double[] zMinMax(final Coordinate[] cs) {
    double zmin;
    double zmax;
    boolean validZFound = false;
    double[] result = new double[2];
    
    zmin = Double.NaN;
    zmax = Double.NaN;
    
    double z;
    
    for (int t = cs.length - 1; t >= 0; t--) {
      z = cs[t].z;
      
      if (!(Double.isNaN(z))) {
        if (validZFound) {
          if (z < zmin) {
            zmin = z;
          }
          
          if (z > zmax) {
            zmax = z;
          }
        } else {
          validZFound = true;
          zmin = z;
          zmax = z;
        }
      }
    }
    
    result[0] = (zmin);
    result[1] = (zmax);
    
    
    return result;
  }
  
  /** Determine the best ShapeType for a given Geometry.
   * @param geom The Geometry to analyze.
   * @return The best ShapeType for the Geometry.
   */  
  public static final ShapeType findBestGeometryType(Geometry geom) {
    ShapeType type = ShapeType.UNDEFINED;
    
    if(geom instanceof Point) {
      type = ShapeType.POINT;
    } else if(geom instanceof MultiPoint) {
      type = ShapeType.MULTIPOINT;
    } else if(geom instanceof Polygon) {
      type = ShapeType.POLYGON;
    } else if(geom instanceof MultiPolygon) {
      type = ShapeType.POLYGON;
    } else if(geom instanceof LineString) {
      type = ShapeType.ARC;
    } else if(geom instanceof MultiLineString) {
      type = ShapeType.ARC;
    } 
    return type;
  }
  
  public static final Class findBestGeometryClass(ShapeType type) {
    Class best;
    if (type == null || type == ShapeType.NULL) {
      best = Geometry.class;
    } else if (type.isLineType()) {
      best = MultiLineString.class;
    } else if (type.isMultiPointType()) {
      best = MultiPoint.class;
    } else if (type.isPointType()) {
      best = Point.class;
    } else if (type.isPolygonType()) {
      best = MultiPolygon.class;
    } else {
      throw new RuntimeException("Unknown ShapeType->GeometryClass : " + type);
    }
    return best;
  }
  
  /** Does what it says, reverses the order of the Coordinates in the ring.
   * @param lr The ring to reverse.
   * @return A new ring with the reversed Coordinates.
   */  
  public static final LinearRing reverseRing(LinearRing lr) {
    int numPoints = lr.getNumPoints() - 1;
    Coordinate[] newCoords = new Coordinate[numPoints + 1];
    
    for(int t = numPoints; t >= 0; t--) {
      newCoords[t] = lr.getCoordinateN(numPoints - t);
    }
    
    return new LinearRing(newCoords, lr.getPrecisionModel(), lr.getSRID());
  }
  
  /** Create a nice Polygon from the given Polygon. Will ensure that shells are
   * clockwise and holes are counter-clockwise. Capiche?
   * @param p The Polygon to make "nice".
   * @return The "nice" Polygon.
   */  
  public static final Polygon makeGoodShapePolygon(Polygon p) {
    LinearRing outer;
    LinearRing[] holes = new LinearRing[p.getNumInteriorRing()];
    Coordinate[] coords;
    CGAlgorithms cga = new RobustCGAlgorithms();
    
    coords = p.getExteriorRing().getCoordinates();
    
    if(cga.isCCW(coords)) {
      outer = reverseRing((LinearRing) p.getExteriorRing());
    } else {
      outer = (LinearRing) p.getExteriorRing();
    }
    
    for(int t = 0,tt = p.getNumInteriorRing(); t < tt; t++) {
      coords = p.getInteriorRingN(t).getCoordinates();
      
      if(!(cga.isCCW(coords))) {
        holes[t] = reverseRing((LinearRing) p.getInteriorRingN(t));
      } else {
        holes[t] = (LinearRing) p.getInteriorRingN(t);
      }
    }
    
    return new Polygon(outer, holes, p.getPrecisionModel(), p.getSRID());
  }
  
  /** Like makeGoodShapePolygon, but applied towards a multi polygon.
   * @param mp The MultiPolygon to "niceify".
   * @return The "nicified" MultiPolygon.
   */
  public static final MultiPolygon makeGoodShapeMultiPolygon(MultiPolygon mp) {
    MultiPolygon result;
    Polygon[] ps = new Polygon[mp.getNumGeometries()];
    
    //check each sub-polygon
    for(int t = 0; t < mp.getNumGeometries(); t++) {
      ps[t] = makeGoodShapePolygon((Polygon) mp.getGeometryN(t));
    }
    
    result = new MultiPolygon(ps, new PrecisionModel(), 0);
    
    return result;
  }
  
  /** Returns: <br>
   * 2 for 2d (default) <br>
   * 4 for 3d  - one of the oordinates has a non-NaN z value <br>
   * (3 is for x,y,m but thats not supported yet) <br>
   * @param cs The array of Coordinates to search.
   * @return The dimension.
   */
  public static final int guessCoorinateDims(final Coordinate[] cs) {
    int dims = 2;
    
    for(int t = cs.length - 1; t >= 0; t--) {
      if(!(Double.isNaN(cs[t].z))) {
        dims = 4;
        break;
      }
    }
    
    return dims;
  }
  
  /** Determine the best ShapeType for a geometry with the given dimension.
   * @param geom The Geometry to examine.
   * @param shapeFileDimentions The dimension 2,3 or 4.
   * @throws ShapefileException If theres a problem, like a bogus Geometry.
   * @return The best ShapeType.
   */   
  public static final ShapeType getShapeType(Geometry geom, int shapeFileDimentions)
  throws ShapefileException {
    
    ShapeType type = null;
    
    if (geom instanceof Point) {
      switch (shapeFileDimentions) {
        case 2:
          type = ShapeType.POINT;
          break;
        case 3:
          type = ShapeType.POINTM;
          break;
        case 4:
          type = ShapeType.POINTZ;
          break;
        default:
          throw new ShapefileException("Too many dimensions for shapefile : " + shapeFileDimentions);
      }
    } else if (geom instanceof MultiPoint) {
      switch (shapeFileDimentions) {
        case 2:
          type = ShapeType.MULTIPOINT;
          break;
        case 3:
          type = ShapeType.MULTIPOINTM;
          break;
        case 4:
          type = ShapeType.MULTIPOINTZ;
          break;
        default:
          throw new ShapefileException("Too many dimensions for shapefile : " + shapeFileDimentions);
      }
    } else if ((geom instanceof Polygon) || (geom instanceof MultiPolygon)) {
      switch (shapeFileDimentions) {
        case 2:
          type = ShapeType.POLYGON;
          break;
        case 3:
          type = ShapeType.POLYGONM;
          break;
        case 4:
          type = ShapeType.POLYGONZ;
          break;
        default:
          throw new ShapefileException("Too many dimensions for shapefile : " + shapeFileDimentions);
      }
    } else if ((geom instanceof LineString) || (geom instanceof MultiLineString)) {
      switch (shapeFileDimentions) {
        case 2:
          type = ShapeType.ARC;
          break;
        case 3:
          type = ShapeType.ARCM;
          break;
        case 4:
          type = ShapeType.ARCZ;
          break;
        default:
          throw new ShapefileException("Too many dimensions for shapefile : " + shapeFileDimentions);
      }
    }
    
    if (type == null) {
      throw new ShapefileException("Cannot handle geometry type : " + (geom == null ? "null" : geom.getClass().getName()));
    }
    return type;
  }
}
