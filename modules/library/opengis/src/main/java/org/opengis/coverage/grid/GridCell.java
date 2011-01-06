/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2011, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2005 Open Geospatial Consortium Inc.
 *    
 *    All Rights Reserved. http://www.opengis.org/legal/
 */
package org.opengis.coverage.grid;

import java.util.Set;
import org.opengis.annotation.UML;

import static org.opengis.annotation.Obligation.*;
import static org.opengis.annotation.Specification.*;


/**
 * A grid cell delineated by the grid lines of a {@linkplain Grid grid}. Its corners
 * are associated with the {@linkplain GridPoint grid points} at the intersections of
 * the grid lines that bound it
 *
 * @version ISO 19123:2004
 * @author  Martin Schouwenburg
 * @author  Wim Koolhoven
 * @author  Martin Desruisseaux (IRD)
 * @since   GeoAPI 2.1
 */
@UML(identifier="CV_GridCell", specification=ISO_19123)
public interface GridCell {
    /**
     * Returns the collection of {@linkplain GridPoint grid points} at the corners of the grid cell.
     * The size of this collection has no upper bound, to allow for grids of any dimension.
     * In a quadrilateral grid, the multiplicity of corner equals 2&times;<var>d</var>, where
     * <var>d</var> is the value of {@link Grid#getDimension}.
     *
     * @return The corners of the grid cell.
     *
     * @see GridPoint#getCells
     */
    @UML(identifier="corner", obligation=MANDATORY, specification=ISO_19123)
    Set<GridPoint> getCorners();

    /**
     * Returns the {@linkplain Grid grid} of which this cell is a component.
     *
     * @return The grid of which this cell is a component.
     *
     * @see Grid#getCells
     */
    @UML(identifier="framework", obligation=MANDATORY, specification=ISO_19123)
    Grid getFramework();
}
