/*
 *    Geotools2 - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2002, Geotools Project Managment Committee (PMC)
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
 */

package org.geotools.filter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import java.io.IOException;
import java.util.logging.Logger;


/**
 * Encodes Geometry filters into valid oracle SDO statements.
 * 
 * <p>
 * At this stage it only supports the GEOMETRY_BBOX types.
 * </p>
 * 
 * <p>
 * Encoded filters get written to the protected Writer called <code>out</code>
 * </p>
 *
 * @author Sean Geoghegan, Defence Science and Technology Organisation
 * @author $Author: cholmesny $
 * @version $Id: SQLEncoderOracle.java,v 1.1 2003/07/11 23:46:03 cholmesny Exp $ 
 */
public class SQLEncoderOracle extends SQLEncoder {
    private static final Logger LOGGER = Logger.getLogger("org.geotools.filter.SQLEncoderOracle");
    private static FilterCapabilities capabilities = SQLEncoder.getCapabilities();

    /** The standard SQL multicharacter wild card. */
    private static String SQL_WILD_MULTI = "%";

    /** The standard SQL single character wild card. */
    private static String SQL_WILD_SINGLE = "_";

    static {
        capabilities.addType(AbstractFilter.GEOMETRY_BBOX);
        capabilities.addType(AbstractFilter.GEOMETRY_CONTAINS);
        capabilities.addType(AbstractFilter.LIKE);
    }

    /** The escaped version of the single wildcard for the REGEXP pattern. */
    private String escapedWildcardSingle = "\\.\\?";

    /** The escaped version of the multiple wildcard for the REGEXP pattern. */
    private String escapedWildcardMulti = "\\.\\*";
    private int srid;

    /**
     * Creates a new SQLEncoderOracle with a specified SRID.
     *
     * @param srid The Spatial Reference ID to use when generating SDO SQL statements.
     */
    public SQLEncoderOracle(int srid) {
        this.srid = srid;
    }

    /**
     * Handles Geometry Filter encoding. Currently only supports the encoding of GEOMETRY_BBOX
     * filters. If a GEOMETRY_BBOX filter is encounter it will be converted into an SDO_RELATE()
     * function.  If another filter is found, nothing will happen.
     *
     * @param geomFilter The geometry filter to encode.
     *
     * @see org.geotools.filter.FilterVisitor#visit(org.geotools.filter.GeometryFilter)
     */
    public void visit(GeometryFilter geomFilter) {
        LOGGER.info("Visiting a Geometry filter");

        try {
            if (geomFilter.getFilterType() == AbstractFilter.GEOMETRY_BBOX) {
                Expression left = geomFilter.getLeftGeometry();
                Expression right = geomFilter.getRightGeometry();

                // left and right have to be valid expressions
                if ((left != null) && (right != null)) {
                    out.write("SDO_RELATE(");
                    left.accept(this);
                    out.write(",");
                    right.accept(this);
                    out.write(",'mask=inside querytype=WINDOW') = 'TRUE' ");
                } else {
                    LOGGER.warning(
                        "Invalid filter. Cannot have a Geometry filter with only one expression.");
                }
            } else if (geomFilter.getFilterType() == AbstractFilter.GEOMETRY_CONTAINS) {
                Expression left = geomFilter.getLeftGeometry();
                Expression right = geomFilter.getRightGeometry();

                if ((left != null) && (right != null)) {
                    out.write("SDO_RELATE(");
                    left.accept(this);
                    out.write(",");
                    right.accept(this);
                    out.write(",'mask=contains querytype=WINDOWS') = 'TRUE' ");
                } else {
                    LOGGER.warning(
                        "Invalid filter. Cannot have a Geometry filter with only one expression.");
                    ;
                }
            } else {
                LOGGER.warning("Unknown filter type: " + geomFilter.getFilterType() +
                    " only BBOX and Contains are currently supported.");
            }
        } catch (IOException e) {
            LOGGER.warning("IO Error exporting geometry filter");
        }
    }

    /**
     * Converts a literal expression into a valid SDO object.  Only handles Literal Geometries, all
     * other literals are passed up to  the parent.
     *
     * @param literal The Literal expression to encode.
     *
     * @see org.geotools.filter.FilterVisitor#visit(org.geotools.filter.LiteralExpression)
     */
    public void visit(LiteralExpression literal) {
        if (literal.getType() == DefaultExpression.LITERAL_GEOMETRY) {
            Geometry geometry = (Geometry) literal.getLiteral();

            try {
                out.write(toSDOGeom(geometry));
            } catch (IOException e) {
                LOGGER.warning("IO Error exporting Literal Geometry");
            }
        } else {
            // can't do it, send it off to the parent
            super.visit(literal);
        }
    }

    /**
     * Writes the SQL for the Like Filter.  Assumes the current java implemented wildcards for the
     * Like Filter: . for multi and .? for single. And replaces them with the SQL % and _,
     * respectively.  Currently  does nothing, and should not be called, not included in the
     * capabilities.
     *
     * @param filter the Like Filter to be visited.
     *
     * @task TODO: LikeFilter doesn't work right...revisit this when it does. Need to think through
     *       the escape char, so it works right when Java uses one, and escapes correctly with an
     *       '_'.
     */
    public void visit(LikeFilter filter) {
        try {
            String pattern = filter.getPattern();
            pattern = pattern.replaceAll(escapedWildcardMulti, SQL_WILD_MULTI);
            pattern = pattern.replaceAll(escapedWildcardSingle, SQL_WILD_SINGLE);

            //pattern = pattern.replace('\\', ''); //get rid of java escapes.
            //TODO escape the '_' char, as it could be in our string and will
            //mess up the SQL wildcard matching
            ((Expression) filter.getValue()).accept(this);
            out.write(" LIKE ");
            out.write("'" + pattern + "'");
            String esc = filter.getEscape();

            if (pattern.indexOf(esc) != -1) { //if it uses the escape char
                out.write(" ESCAPE " + "'" + esc + "'"); //this needs testing
            }

            //TODO figure out when to add ESCAPE clause, probably just for the '_' char.
        } catch (java.io.IOException ioe) {
            LOGGER.warning("Unable to export filter" + ioe);
        }
    }

    /** Converts JTS Geometry to a String version of a SDO Geometry.
     * 
     *
     * @param geometry The JTS Geometry to convert.
     *
     * @return A String representation of the SDO Geometry.
     */
    private String toSDOGeom(Geometry geometry) {
        if (Point.class.isAssignableFrom(geometry.getClass())) {
            return toSDOGeom((Point) geometry);
        } else if (LineString.class.isAssignableFrom(geometry.getClass())) {
            return toSDOGeom((LineString) geometry);
        } else if (Polygon.class.isAssignableFrom(geometry.getClass())) {
            return toSDOGeom((Polygon) geometry);
        } else {
            LOGGER.warning("Got a literal geometry that I can't handle: " +
                geometry.getClass().getName());
            return "";
        }
    }

    /**
     * Converts a Point Geometry in an SDO SQL geometry construction statement.
     * 
     * <p>
     * 2D geometries is assumed. If higher dimensional geometries are used the query will be
     * encoded as a 2D geometry.
     * </p>
     *
     * @param point The point to encode.
     *
     * @return An SDO SQL geometry object construction statement
     */
    private String toSDOGeom(Point point) {
        if (point.getDimension() > 2) {
            LOGGER.warning("" + point.getDimension() + " dimensioned geometry provided." +
                " This encoder only supports 2D geometries. The query will be constructed as" +
                " a 2D query.");
        }

        StringBuffer buffer = new StringBuffer("mdsys.sdo_geometry(");
        buffer.append(point.getDimension());
        buffer.append("001,");

        if (srid > 0) {
            LOGGER.info("Using layer SRID: " + srid);
            buffer.append(srid);
        } else {
            LOGGER.info("Using NULL SRID: ");
            buffer.append("NULL");
        }

        buffer.append(",MDSYS.SDO_POINT_TYPE(");
        buffer.append(point.getX());
        buffer.append(",");
        buffer.append(point.getY());
        buffer.append(",NULL),NULL,NULL)");

        return buffer.toString();
    }

    /**
     * Converts a LineString Geometry in an SDO SQL geometry construction statement.
     * 
     * <p>
     * 2D geometries is assumed. If higher dimensional geometries are used the query will be
     * encoded as a 2D geometry.
     * </p>
     *
     * @param line The line to encode.
     *
     * @return An SDO SQL geometry object construction statement
     */
    private String toSDOGeom(LineString line) {
        if (line.getDimension() > 2) {
            LOGGER.warning("" + line.getDimension() + " dimensioned geometry provided." +
                " This encoder only supports 2D geometries. The query will be constructed as" +
                " a 2D query.");
        }

        StringBuffer buffer = new StringBuffer("mdsys.sdo_geometry(");
        buffer.append(line.getDimension());
        buffer.append("002,");

        if (srid > 0) {
            LOGGER.info("Using layer SRID: " + srid);
            buffer.append(srid);
        } else {
            LOGGER.info("Using NULL SRID: ");
            buffer.append("NULL");
        }

        buffer.append(",NULL,MDSYS.sdo_elem_info_array(1,2,1),");
        buffer.append("MDSYS.sdo_ordinate_array(");

        Coordinate[] coordinates = line.getCoordinates();

        for (int i = 0; i < coordinates.length; i++) {
            buffer.append(coordinates[i].x);
            buffer.append(",");
            buffer.append(coordinates[i].y);

            if (i != (coordinates.length - 1)) {
                buffer.append(",");
            }
        }

        buffer.append("))");

        return buffer.toString();
    }

    /**
     * Converts a Polygon Geometry in an SDO SQL geometry construction statement.
     * 
     * <p>
     * 2D geometries is assumed. If higher dimensional geometries are used the query will be
     * encoded as a 2D geometry.
     * </p>
     *
     * @param polygon The polygon to encode.
     *
     * @return An SDO SQL geometry object construction statement
     */
    private String toSDOGeom(Polygon polygon) {
        StringBuffer buffer = new StringBuffer();

        if (polygon.getDimension() > 2) {
            LOGGER.warning("" + polygon.getDimension() + " dimensioned geometry provided." +
                " This encoder only supports 2D geometries. The query will be constructed as" +
                " a 2D query.");
        }

        if (polygon.getExteriorRing() != null) {
            buffer.append("mdsys.sdo_geometry(");
            buffer.append(polygon.getDimension());
            buffer.append("003,");

            if (srid > 0) {
                LOGGER.info("Using layer SRID: " + srid);
                buffer.append(srid);
            } else {
                LOGGER.info("Using NULL SRID: ");
                buffer.append("NULL");
            }

            buffer.append(",NULL,MDSYS.sdo_elem_info_array(1,1003,1),");
            buffer.append("MDSYS.sdo_ordinate_array(");

            Coordinate[] coordinates = polygon.getExteriorRing().getCoordinates();

            for (int i = 0; i < coordinates.length; i++) {
                buffer.append(coordinates[i].x);
                buffer.append(",");
                buffer.append(coordinates[i].y);

                if (i != (coordinates.length - 1)) {
                    buffer.append(",");
                }
            }

            buffer.append("))");
        } else {
            LOGGER.warning(
                "No Exterior ring on polygon.  This encode only supports Polygons with exterior rings.");
        }

        if (polygon.getNumInteriorRing() > 0) {
            LOGGER.warning("Polygon contains Interior Rings. These rings will not be included" +
                " in the query.");
        }

        return buffer.toString();
    }
}
