/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.geotools.filter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import java.util.List;
import org.geotools.data.DataUtilities;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;

/**
 * Fields and helper method for unit test classes
 * @author Michael Bedward
 */
public class SEFunctionTestBase {

    public SEFunctionTestBase() {
    }
    protected final FilterFactory2 ff2 = CommonFactoryFinder.getFilterFactory2(null);
    protected final FunctionFinder finder = new FunctionFinder(null);
    protected final GeometryFactory gf = JTSFactoryFinder.getGeometryFactory(null);
    protected List<Expression> parameters;

    protected SimpleFeature feature(Number number) throws Exception {
        String typeSpec = "geom:Point,value:" + number.getClass().getSimpleName();
        SimpleFeatureType type = DataUtilities.createType("Feature", typeSpec);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        Coordinate coord = new Coordinate(0, 0);
        builder.add(gf.createPoint(coord));
        builder.add(number);
        return builder.buildFeature(null);
    }

}
