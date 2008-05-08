/*
 *    GeoTools - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2003-2006, Geotools Project Managment Committee (PMC)
 *    (C) 2003, Institut de Recherche pour le Développement
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.coverage.grid;

import java.awt.geom.AffineTransform;

import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.metadata.spatial.PixelOrientation;

import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.operation.transform.IdentityTransform;

import org.junit.*;
import static org.junit.Assert.*;


/**
 * Test the {@link GridGeometry} implementation.
 *
 * @source $URL$
 * @version $Id$
 * @author Martin Desruisseaux
 */
public final class GridGeometryTest {
    /**
     * Tests the construction with an identity transform.
     */
    @Test
    public void testIdentity() throws FactoryException {
        final int[] lower = new int[] {0,     0, 2};
        final int[] upper = new int[] {100, 200, 4};
        final MathTransform identity = IdentityTransform.create(3);
        GridGeometry2D gg;
        try {
            gg = new GridGeometry2D(new GeneralGridRange(lower,upper), identity, null);
            fail();
        } catch (IllegalArgumentException e) {
            // This is the expected dimension.
        }
        upper[2] = 3;
        gg = new GridGeometry2D(new GeneralGridRange(lower,upper), identity, null);
        assertTrue(identity.isIdentity());
        assertTrue(gg.getGridToCRS().isIdentity());
        assertTrue(gg.getGridToCRS2D().isIdentity());
        assertEquals(3, gg.getGridToCRS().getSourceDimensions());
        assertEquals(2, gg.getGridToCRS2D().getSourceDimensions());
        assertTrue(gg.getGridToCRS2D() instanceof AffineTransform);
        /*
         * Tests with a pixel orientation.
         */
        AffineTransform tr = (AffineTransform) gg.getGridToCRS2D(PixelOrientation.CENTER);
        assertTrue(tr.isIdentity());
        tr = (AffineTransform) gg.getGridToCRS2D(PixelOrientation.UPPER_LEFT);
        assertFalse(tr.isIdentity());
        assertEquals(AffineTransform.TYPE_TRANSLATION, tr.getType());
        assertEquals(-0.5, tr.getTranslateX(), 0);
        assertEquals(-0.5, tr.getTranslateY(), 0);
        tr = (AffineTransform) gg.getGridToCRS2D(PixelOrientation.valueOf("LOWER"));
        assertEquals(AffineTransform.TYPE_TRANSLATION, tr.getType());
        assertEquals(0.0, tr.getTranslateX(), 0);
        assertEquals(0.5, tr.getTranslateY(), 0);
    }

    /**
     * Tests the construction from an envelope.
     */
    @Test
    public void testEnvelope() {
        final int[]    lower   = new int[]    {   0,   0,  4};
        final int[]    upper   = new int[]    {  90,  45,  5};
        final double[] minimum = new double[] {-180, -90,  9};
        final double[] maximum = new double[] {+180, +90, 10};
        final GridGeometry2D gg;
        gg = new GridGeometry2D(new GeneralGridRange(lower,upper),
                                new GeneralEnvelope(minimum, maximum));
        final AffineTransform tr = (AffineTransform) gg.getGridToCRS2D();
        assertEquals(AffineTransform.TYPE_UNIFORM_SCALE |
                     AffineTransform.TYPE_TRANSLATION   |
                     AffineTransform.TYPE_FLIP, tr.getType());

        assertEquals(   4, tr.getScaleX(),     0);
        assertEquals(  -4, tr.getScaleY(),     0);
        assertEquals(-178, tr.getTranslateX(), 0);
        assertEquals(  88, tr.getTranslateY(), 0);
    }

    /**
     * Tests construction with 0.5 pixel translations.
     */
    @Test
    public void testPixelInCell() {
        final MathTransform identity = IdentityTransform.create(4);
        final int[] lower = new int[] {100, 300, 3, 6};
        final int[] upper = new int[] {200, 400, 4, 7};
        final GeneralGridRange range = new GeneralGridRange(lower, upper);
        GridGeometry2D gg = new GridGeometry2D(range, PixelInCell.CELL_CORNER, identity, null, null);

        assertSame (identity, gg.getGridToCRS(PixelInCell.CELL_CORNER));
        assertFalse(identity.equals(gg.getGridToCRS(PixelInCell.CELL_CENTER)));
        assertFalse(identity.equals(gg.getGridToCRS(PixelOrientation.CENTER)));
        assertSame (gg.getGridToCRS(PixelInCell.CELL_CENTER), gg.getGridToCRS(PixelOrientation.CENTER));

        AffineTransform tr = (AffineTransform) gg.getGridToCRS2D(PixelOrientation.CENTER);
        assertFalse(tr.isIdentity());
        assertEquals(AffineTransform.TYPE_TRANSLATION, tr.getType());
        assertEquals(0.5, tr.getTranslateX(), 0);
        assertEquals(0.5, tr.getTranslateY(), 0);

        tr = (AffineTransform) gg.getGridToCRS2D(PixelOrientation.UPPER_LEFT);
        assertTrue(tr.isIdentity());
    }
}
