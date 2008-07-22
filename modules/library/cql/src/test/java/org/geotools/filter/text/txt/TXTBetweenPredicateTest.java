/*
 *    GeoTools - The Open Source Java GIS Tookit
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

package org.geotools.filter.text.txt;

import org.geotools.filter.text.cql2.CQLBetweenPredicateTest;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.cql2.CompilerFactory.Language;
import org.junit.Test;
import org.opengis.filter.Filter;


/**
 * Test case for between predicate with expressions
 * <p>
 * <pre>
 * <between predicate> ::= <expression> [ "NOT" ] "BETWEEN" <expression> "AND" <expression>
 * </pre>
 * </p>
 *
 * @author Mauricio Pazos (Axios Engineering)
 * @since 2.5
 */
public class TXTBetweenPredicateTest extends CQLBetweenPredicateTest{

    public TXTBetweenPredicateTest() {
        // sets the language used to execute this test case
        super(Language.TXT);
    }
    
    /**
     * sample: 2 BETWEEN 1 AND 3 
     * @throws CQLException 
     */
    @Test
    public void literalBetweenLiterals() throws Exception{

        String txtPredicate = FilterTXTSample.LITERAL_BETWEEN_TWO_LITERALS;
        Filter expected = FilterTXTSample.getSample(txtPredicate);
        
        testBetweenPredicate(txtPredicate, expected);
    }
    
    /**
     * sample: 2 BETWEEN (2-1) AND (2+1)
     * 
     * @throws CQLException 
     */
    @Test
    public void literalBetweenExpressions() throws Exception{
        

        String txtPredicate = FilterTXTSample.LITERAL_BETWEEN_TWO_EXPRESSIONS;
        Filter expected = FilterTXTSample.getSample(txtPredicate);
        
        testBetweenPredicate(txtPredicate, expected);

    }

    /**
     * sample: area( the_geom ) BETWEEN 10000 AND 30000
     * @throws Exception 
     */
    @Test
    public void functionBetweenLiterals() throws Exception{

        String txtPredicate = FilterTXTSample.FUNCTION_BETWEEN_LITERALS;
        Filter expected = FilterTXTSample.getSample(txtPredicate);
        
        testBetweenPredicate(txtPredicate, expected);
    }
    
    /**
     * sample: area( the_geom ) BETWEEN abs(10000) AND abs(30000)
     * @throws Exception 
     */
    @Test
    public void functionBetweenFunctions()throws Exception{
        
        final String txtPredicate= FilterTXTSample.FUNCTION_BETWEEN_FUNCTIONS;
        Filter expected = FilterTXTSample.getSample(txtPredicate);
        
        testBetweenPredicate(txtPredicate, expected);
    }
    
}