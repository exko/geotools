/*
 * RobustGeometryPropertiesTest.java
 * JUnit based test
 *
 * Created on 07 March 2002, 11:41
 */

package org.geotools.algorithms;

import com.vividsolutions.jts.geom.*;
import junit.framework.*;

/**
 *
 * @author andyt
 */
public class RobustGeometryPropertiesTest extends TestCase {
    
    GeometryProperties geometryProperties1 = new RobustGeometryProperties();
    GeometryCollection geometryCollection1;
    Geometry[] geometries1;
    MultiPolygon multiPolygon1;
    Polygon[] polygons1;
    Polygon polygon1;
    LinearRing[] linearRings1;
    LinearRing linearRing1;
    MultiLineString multiLineString1;
    LineString[] lineStrings1;
    LineString lineString1;
    MultiPoint multiPoint1;
    Point[] points1;
    Point point1;
    Coordinate[] coordinates1;
    Coordinate coordinate1;
    Coordinate[] coordinates2;
    
    public RobustGeometryPropertiesTest(java.lang.String testName) {
        super(testName);
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(RobustGeometryPropertiesTest.class);
        return suite;
    }
    
    public void testPoint() {
        System.out.println("Testing "+points1[0]);
        assertEquals(0.0d,geometryProperties1.getArea(points1[0]),0.0d);
        assertEquals(0.0d,geometryProperties1.getPerimeter(points1[0]),0.0d);
    }

    public void testMultiPoint() {
        System.out.println("Testing "+multiPoint1);
        assertEquals(0.0d,geometryProperties1.getArea(multiPoint1),0.0d);
        assertEquals(0.0d,geometryProperties1.getPerimeter(multiPoint1),0.0d);
    }

    public void testLineString() {
        System.out.println("Testing "+lineStrings1[0]);
        assertEquals(0.0d,geometryProperties1.getArea(lineStrings1[0]),0.0d);
        assertEquals(6.0d,geometryProperties1.getPerimeter(lineStrings1[0]),0.0d);
    }

    public void testMultiLineString() {
        System.out.println("Testing "+multiLineString1);
        assertEquals(0.0d,geometryProperties1.getArea(multiLineString1),0.0d);
        assertEquals(11.0d,geometryProperties1.getPerimeter(multiLineString1),0.0d);
    }

    public void testPolygon() {
        System.out.println("Testing "+polygons1[0]);
        assertEquals(8.0d,geometryProperties1.getArea(polygons1[0]),0.0d);
        assertEquals(16.0d,geometryProperties1.getPerimeter(polygons1[0]),0.0d);
    }

    public void testMultiPolygon() {
        System.out.println("Testing "+multiPolygon1);
        //assertEquals((8.0d + Math.PI),geometryProperties1.getArea(multiPolygon1),0.0d);
        assertEquals((16.0d + (2 * Math.PI)),geometryProperties1.getPerimeter(multiPolygon1),0.3d);
    }

    public void testGeometryCollection() {
        System.out.println("Testing "+geometryCollection1);
        assertEquals((8.0d + Math.PI),geometryProperties1.getArea(multiPolygon1),0.0d);
        assertEquals((27.0d + (2 * Math.PI)),geometryProperties1.getPerimeter(multiPolygon1),0.0d);
    }
    
    public void setUp() {
        
        //System.out.println("Setting up geometries to test");
        
        GeometryFactory geometryFactory1 = new GeometryFactory();
        PrecisionModel precisionModel1 = geometryFactory1.getPrecisionModel();
        int SRID = 0;
    
        // Generate points[] and MultiPoint
        //
        //  Y
        //       [0] [1] [2]
        //  0.0  [3] [4] [5]
        //       [6] [7] [8]
        //
        //           0.0     X
        points1 =  new Point[9];
        points1[0] = geometryFactory1.createPoint(new Coordinate(-1.0d,1.0d));
        points1[1] = geometryFactory1.createPoint(new Coordinate(0.0d,1.0d));
        points1[2] = geometryFactory1.createPoint(new Coordinate(1.0d,1.0d));
        points1[3] = geometryFactory1.createPoint(new Coordinate(-1.0d,0.0d));
        points1[4] = geometryFactory1.createPoint(new Coordinate(0.0d,0.0d));
        points1[5] = geometryFactory1.createPoint(new Coordinate(1.0d,0.0d));
        points1[6] = geometryFactory1.createPoint(new Coordinate(-1.0d,-1.0d));
        points1[7] = geometryFactory1.createPoint(new Coordinate(0.0d,-1.0d));
        points1[8] = geometryFactory1.createPoint(new Coordinate(1.0d,-1.0d));
        multiPoint1 = geometryFactory1.createMultiPoint(points1);
        
        // Generate LineString[] and MultiLineString
        lineStrings1 = new LineString[2];
        coordinates1 = new Coordinate[7];
        coordinates1[0] = new Coordinate(0.0d,0.0d);
        coordinates1[1] = new Coordinate(1.0d,0.0d);
        coordinates1[2] = new Coordinate(1.0d,1.0d);
        coordinates1[3] = new Coordinate(2.0d,1.0d);
        coordinates1[4] = new Coordinate(2.0d,2.0d);
        coordinates1[5] = new Coordinate(3.0d,2.0d);
        coordinates1[6] = new Coordinate(3.0d,3.0d);
        // LineStrings1[0]
        //
        //   Y
        //
        //  3.0              [6]
        //               [4] [5]
        //           [2] [3]
        //  0.0  [0] [1]
        //
        //       0.0         3.0    X
        lineStrings1[0] = geometryFactory1.createLineString(coordinates1);
        coordinates1 = new Coordinate[2];
        coordinates1[0] = new Coordinate(0.0d,0.0d);
        coordinates1[1] = new Coordinate(3.0d,4.0d);
        // LineStrings1[1]
        //
        //   Y
        //
        //  4.0              [1]
        //
        //
        //
        //  0.0  [0]
        //
        //       0.0         3.0    X
        lineStrings1[1] = geometryFactory1.createLineString(coordinates1);
        multiLineString1 = geometryFactory1.createMultiLineString(lineStrings1);

        // Generate Polygon[] and MultiPolygon
        polygons1 = new Polygon[2];
        coordinates1 = new Coordinate[5];
        coordinates1[0] = new Coordinate(0.0d,0.0d);
        coordinates1[1] = new Coordinate(3.0d,0.0d);
        coordinates1[2] = new Coordinate(3.0d,3.0d);
        coordinates1[3] = new Coordinate(0.0d,3.0d);
        coordinates1[4] = new Coordinate(0.0d,0.0d);
        coordinates2 = new Coordinate[5];
        coordinates2[0] = new Coordinate(1.0d,1.0d);
        coordinates2[1] = new Coordinate(1.0d,2.0d);
        coordinates2[2] = new Coordinate(2.0d,2.0d);
        coordinates2[3] = new Coordinate(2.0d,1.0d);
        coordinates2[4] = new Coordinate(1.0d,1.0d);
        linearRing1 = new LinearRing(null,precisionModel1,SRID);
        linearRings1 = new LinearRing[1];
        try {
            linearRing1 = geometryFactory1.createLinearRing(coordinates1);
            linearRings1[0] = geometryFactory1.createLinearRing(coordinates2);
        } catch (TopologyException te1) {
            System.out.println(te1+" Exiting");
            System.exit(0);
        }
        // polygons1[0]
        //
        //   Y
        //
        //  3.0   [3]               [2]
        //  2.0         [1]   [2]     
        //  1.0        [0,4]  [3]
        //  0.0  [0,4]              [1]
        //
        //        0.0   1.0   2.0   3.0    X
        polygons1[0] = geometryFactory1.createPolygon(linearRing1,linearRings1);
        polygons1[1] = (Polygon) geometryFactory1.createPoint(new Coordinate(0.0d,0.0d)).buffer(1.0d);
        multiPolygon1 = geometryFactory1.createMultiPolygon(polygons1);
        
        // Generate Geometry[] and GeometryCollection
        geometries1 = new Geometry[3];
        geometries1[0] = multiPoint1;
        geometries1[1] = multiLineString1;
        geometries1[2] = multiPolygon1;
        geometryCollection1 = geometryFactory1.createGeometryCollection(geometries1);
    }
}
