/*
 * RobustGeometryProperties.java
 * This file is covered by the LGPL
 * Created on 05 March 2002, 17:59
 */

package org.geotools.algorithms;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Coordinate;

/** 
 * Robust implementation of the GeometryProperties interface
 * TODO: Update comments
 * TODO: Add in calculation for area of a Polygon
 *
 *
 * @author andyt
 * @version $Revision: 1.4 $ $Date: 2002/05/25 17:51:42 $
 */
public class RobustGeometryProperties implements org.geotools.algorithms.GeometryProperties {

    /**
     * Returns the area of a GeometryCollection
     * @param geometryCollection1 The GeometryCollection for which the area is
     * calulated
     * @return The total area of all geometries in the collection
     */
    protected double getArea(GeometryCollection geometryCollection1) {
        double area = 0.0d;
        double perimeter = 0.0d;
        int numberOfGeometries1 = geometryCollection1.getNumGeometries();
        // Go through geometryCollection1 and sum areas of component geometries
        for (int i = 0; i < numberOfGeometries1; i++) {
            area += getArea(geometryCollection1.getGeometryN(i));
        }
        return area;
    }

    /**
     * Returns
     * @param geometryCollection The GeometryCollection for which the perimeter is
     * calulated
     * @return the perimeter of a GeometryCollection
     */
    protected double getPerimeter(GeometryCollection geometryCollection) {
        double perimeter = 0.0d;
        int numberOfGeometries = geometryCollection.getNumGeometries();
        // Go through geometryCollection and sum perimeters of component 
        // geometries
        for (int i = 0; i < numberOfGeometries; i++) {
            perimeter += getPerimeter(geometryCollection.getGeometryN(i));
        }
        return perimeter;
    }

   /**
     * Cacluclate and return the area of the specified geometry.<br>
     * For Polygons this is the total area inside the external ring less
     * the total of any contained by interior rings.  GeometryCollections
     * (including MultiPolygons) are ittereted through so the result is the
     * sum of all polygons anywhere within the collection.
     * Any geometry other than Polgyon or a collection returns 0;
     *
     * @param geometry The Geometry to calculate the area of.
     * @return The total area of the Geometry 
     */
    public double getArea(Geometry geometry) {
        double area = 0.0d;
        if (geometry instanceof GeometryCollection) {
            area += getArea((GeometryCollection) geometry);
        } else if (geometry instanceof MultiPolygon) {
            area += getArea((MultiPolygon) geometry);
        } else if (geometry instanceof Polygon) {
            area += getArea((Polygon) geometry);
        } else {
            area += 0.0d;
        }
        return area;
    }

   /**
     * Cacluclate and return the perimeter of the specified geometry.<br>
     * For Polygons this is the total length of the exterior ring and all
     * internal rings.  For LineStrings the total line length is returned.
     * GeometryCollections are ittereted through so the result is the
     * sum of all Polygon and Line geometries anywhere within the collection.
     * Any point geometries return a value of 0;
     *
     * @param geometry The Geometry to calculate the area of.
     * @return The total area of the Geometry 
     */
    public double getPerimeter(Geometry geometry) {
        double perimeter = 0.0d;
        if (geometry instanceof GeometryCollection) {
            perimeter += getPerimeter((GeometryCollection) geometry);
        } else if (geometry instanceof MultiPolygon) {
            perimeter += getPerimeter((MultiPolygon) geometry);
        } else if (geometry instanceof Polygon) {
            perimeter += getPerimeter((Polygon) geometry);
        } else if (geometry instanceof MultiLineString) {
            perimeter += getPerimeter((MultiLineString) geometry);
        } else if (geometry instanceof LineString) {
            perimeter += getPerimeter((LineString) geometry);
        } else {
            perimeter += 0.0d;
        }
        return perimeter;
    }
    
    /**
     * Returns the area of a MultiPolygon
     * @param multiPolygon - the MultiPolygon for which the area is calculated
     * @return Total area of all polygons in multiPolgyon
     */
    protected double getArea(MultiPolygon multiPolygon) {
        double area = 0.0d;
        int numberOfGeometries = multiPolygon.getNumGeometries();
        for (int i = 0; i < numberOfGeometries; i++) {
            area += getArea(multiPolygon.getGeometryN(i));
        }
        return area;
    }
    
    /**
     * Returns the perimeter of a MultiPolygon
     * @param multiPolygon - the MultiPolygon for which the perimeter is 
     * calculated
     * @return Total perimeter of all polygons in the multiPolygon
     */
    protected double getperimeter(MultiPolygon multiPolygon) {
        double perimeter = 0.0d;
        int numberOfGeometries = multiPolygon.getNumGeometries();
        for (int i = 0; i < numberOfGeometries; i++) {
            perimeter += getPerimeter(multiPolygon.getGeometryN(i));
        }
        return perimeter;
    }

    /**
     * Returns the area of a Polygon
     * @param polygon - the Polygon for which the area is calculated
     * @return The area of the polgyon
     */
    protected double getArea(Polygon polygon) {
        double area = 0.0d;
        double interiorArea = 0.0d;
        Coordinate[] exteriorRingCoordinates = polygon.getExteriorRing().getCoordinates();
        int numberOfExteriorRingCoordinates = exteriorRingCoordinates.length;
        // Calculate the boundingBox of polygon1 aligned with the axes x and y
        double minx = Double.POSITIVE_INFINITY;
        double maxx = Double.NEGATIVE_INFINITY;
        double miny = Double.POSITIVE_INFINITY;
        double maxy = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < numberOfExteriorRingCoordinates; i++) {
            minx = Math.min(minx, exteriorRingCoordinates[i].x);
            maxx = Math.max(maxx, exteriorRingCoordinates[i].x);
            miny = Math.min(miny, exteriorRingCoordinates[i].y);
            maxy = Math.max(maxy, exteriorRingCoordinates[i].y);
        }
        //Calculate area of each trapezoid formed by droping lines from 
        //each pair of coordinates in exteriorRingCoorinates to the x-axis.
        //x[i]<x[i-1] will contribute a negative area
        for (int i = 0; i < (numberOfExteriorRingCoordinates - 1); i++) {
            area += (((exteriorRingCoordinates[i + 1].x - minx) - 
                    (exteriorRingCoordinates[i].x - minx)) * 
                    (((exteriorRingCoordinates[i + 1].y - miny) + 
                    (exteriorRingCoordinates[i].y - miny)) / 2d));
        }
        area = Math.abs(area);
        //Calculate area of each trapezoid formed by droping lines
        //from each pair of coordinates in interiorRingCoorinates to the x-axis.
        int numberOfInteriorRings = polygon.getNumInteriorRing();
        int numberOfInteriorRingCoordinates;
        Coordinate[] interiorRingCoordinates;
        for (int i = 0; i < numberOfInteriorRings; i++) {
            interiorArea = 0.0d;
            interiorRingCoordinates = polygon.getInteriorRingN(i).getCoordinates();
            numberOfInteriorRingCoordinates = interiorRingCoordinates.length;
            minx = Double.POSITIVE_INFINITY;
            maxx = Double.NEGATIVE_INFINITY;
            miny = Double.POSITIVE_INFINITY;
            maxy = Double.NEGATIVE_INFINITY;
            for (int j = 0; j < numberOfInteriorRingCoordinates; j++) {
                minx = Math.min(minx, interiorRingCoordinates[j].x);
                maxx = Math.max(maxx, interiorRingCoordinates[j].x);
                miny = Math.min(miny, interiorRingCoordinates[j].y);
                maxy = Math.max(maxy, interiorRingCoordinates[j].y);
            }
            for (int j = 0; j < (numberOfInteriorRingCoordinates - 1); j++) {
                interiorArea += (((interiorRingCoordinates[j + 1].x - minx) - 
                                (interiorRingCoordinates[j].x - minx)) * 
                                (((interiorRingCoordinates[j + 1].y - miny) + 
                                (interiorRingCoordinates[j].y - miny)) / 2d));
            }
            area -= Math.abs(interiorArea);
        }
        return area;
    }

    /**
     * Returns the perimeter of a Polygon
     * @param polygon - the Polygon for which the perimeter is calculated
     * @return The perimeter of the polygon
     */
    protected double getPerimeter(Polygon polygon) {
        double perimeter = 0.0d;
        LineString lineString = polygon.getExteriorRing();
        perimeter += getPerimeter(lineString);
        int numberOfHoles = polygon.getNumInteriorRing();
        for (int i = 0; i < numberOfHoles; i++) {
            perimeter += getPerimeter(polygon.getInteriorRingN(i));
        }
        return perimeter;
    }

    /**
     * Returns the perimeter of a MultiLineString
     * @param multiLineString - the MultiLineString for which the perimeter is
     * calculated
     * @return the total perimter (length) of the lines in multiLineString
     */
    protected double getPerimeter(MultiLineString multiLineString) {
        double perimeter = 0.0d;
        int numberOfGeometries = multiLineString.getNumGeometries();
        for (int i = 0; i < numberOfGeometries; i++) {
            perimeter += getPerimeter(multiLineString.getGeometryN(i));
        }
        return perimeter;
    }

    /**
     * Returns the perimeter of a LineString
     * @param lineString - the LineString for which the perimeter is calculated
     * @return the perimeter (length) of hte lineString
     */
    protected double getPerimeter(LineString lineString) {
        double perimeter = 0.0d;
        int numberOfPoints = lineString.getNumPoints();
        Coordinate[] coordinates = lineString.getCoordinates();
        for (int i = 0; i < (numberOfPoints - 1); i++) {
            perimeter += coordinates[i].distance(coordinates[i + 1]);
        }
        return perimeter;
    }
}
