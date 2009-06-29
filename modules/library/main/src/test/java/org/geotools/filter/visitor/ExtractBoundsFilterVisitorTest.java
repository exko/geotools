package org.geotools.filter.visitor;

import static org.junit.Assert.*;

import org.geotools.factory.CommonFactoryFinder;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class ExtractBoundsFilterVisitorTest {

	FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
	ExtractBoundsFilterVisitor visitor = new ExtractBoundsFilterVisitor();
	Envelope infinity = visitor.infinity();

	@Test
	public void testInclude() {
		Envelope env = (Envelope) Filter.INCLUDE.accept(visitor, null);
		assertEquals(infinity, env);
	}
	
	@Test
	public void testExclude() {
		Envelope env = (Envelope) Filter.EXCLUDE.accept(visitor, null);
		assertTrue(env.isNull());
	}
	
	@Test
	public void testNonSpatial() {
		Filter f = ff.less(ff.property("att"), ff.literal(10));
		Envelope env = (Envelope) f.accept(visitor, null);
		assertEquals(infinity, env);
	}
	
	@Test
	public void testBbox() {
		Filter f = ff.bbox("geom", -10, -10, 10, 10, null);
		Envelope env = (Envelope) f.accept(visitor, null);
		assertEquals(new Envelope(-10, 10, -10, 10), env);
	}
	
	@Test
	public void testAnd() {
		Filter f = ff.and(ff.bbox("geom", -10, -10, 10, 10, null), 
				          ff.equals(ff.property("att"), ff.literal("10")));
		Envelope env = (Envelope) f.accept(visitor, null);
		assertEquals(new Envelope(-10, 10, -10, 10), env);
	}
	
	@Test
	public void testOr() {
		Filter f = ff.or(ff.bbox("geom", -10, -10, 10, 10, null), 
				          ff.equals(ff.property("att"), ff.literal("10")));
		Envelope env = (Envelope) f.accept(visitor, null);
		assertEquals(infinity, env);
	}
	
	@Test
	public void testTouches() {
        Coordinate[] coords = new Coordinate[]{new Coordinate(0,0), new Coordinate(10,10)};
        LineString lineString = new GeometryFactory().createLineString( coords );
        Filter filter = ff.touches( ff.property("name"), ff.literal( lineString ) );
        Envelope env = (Envelope) filter.accept(visitor, null);
        assertEquals(new Envelope(0, 10, 0, 10), env);
	}
	
	@Test
	public void testBeyond() {
        Coordinate[] coords = new Coordinate[]{new Coordinate(0,0), new Coordinate(10,10)};
        LineString lineString = new GeometryFactory().createLineString( coords );
        Filter filter = ff.beyond(ff.property("name"), ff.literal( lineString ), 100, "m");
        Envelope env = (Envelope) filter.accept(visitor, null);
        assertEquals(infinity, env);
	}
	
	@Test
	public void testNotBeyond() {
        Coordinate[] coords = new Coordinate[]{new Coordinate(0,0), new Coordinate(10,10)};
        LineString lineString = new GeometryFactory().createLineString( coords );
        Filter filter = ff.beyond(ff.property("name"), ff.literal( lineString ), 100, "m");
        Envelope env = (Envelope) filter.accept(visitor, null);
        // the thing is not so smart to assess that not(beyond) -> within, but we have to make
        // sure that at least the returned envelope contains the real one
        assertEquals(infinity, env);
	}
	
	@Test
	public void testNull() {
		Filter filter = ff.isNull(ff.property("name"));
		Envelope env = (Envelope) filter.accept(visitor, null);
        assertEquals(infinity, env);
	}
	
}