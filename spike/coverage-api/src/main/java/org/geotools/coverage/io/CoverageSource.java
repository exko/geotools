/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.coverage.io;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import javax.media.jai.ImageLayout;

import org.geotools.coverage.io.range.RangeType;
import org.geotools.data.ResourceInfo;
import org.geotools.referencing.CRS;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.feature.type.Name;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.TransfiniteSet;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.temporal.TemporalPrimitive;
import org.opengis.util.ProgressListener;

/**
 * Allows access to a Coverage.
 * 
 * @author Simone Giannecchini, GeoSolutions
 * @author Jody Garnett
 * @todo add a method to obtain capabilities for this {@link CoverageSource}
 */
public interface CoverageSource {

	/**
	 * Name of the Coverage (ie data product) provided by this CoverageSource.
	 * 
	 * @since 2.5
	 * @return Name of the Coverage (ie data product) provided.
	 */
	Name getName(final ProgressListener listener);

	/**
	 * Returns the list of metadata keywords associated with the input source as
	 * a whole (not associated with any particular grid coverage).
	 * 	
	 * @return
	 * @throws java.io.IOException
	 * @todo I just copied this from the old GCE spec. We might want to think about something better in the near future
	 */
	public abstract String[] getMetadataNames(final ProgressListener listener) throws java.io.IOException;

	/**
	 * Retrieve the metadata value for a given metadata name.
	 * 
	 * @param name
	 * @return
	 * @throws java.io.IOException
	 * @todo I just copied this from the old GCE spec. We might want to think about something better in the near future
	 */
	public abstract java.lang.String getMetadataValue(java.lang.String name,final ProgressListener listener)
			throws java.io.IOException;

	/**
	 * Information describing the contents of this resource.
	 * <p>
	 * Please note that for FeatureContent:
	 * <ul>
	 * <li>name - unique with in the context of a Service
	 * <li>schema - used to identify the type of resource; usually the format
	 * or data product being represented
	 * <ul>
	 * 
	 * @todo do we need this??
	 */
	ResourceInfo getInfo(final ProgressListener listener);

	/**
	 * The first {@link BoundingBox} of this {@link List} should contain the
	 * overall bounding for the underlying coverage in its native coordinate
	 * reference system. However, by setting the <code>global</code> param to
	 * true we can request additional bounding boxes in case the area covered by
	 * the mentioned coverage is poorly approximated by a single coverage, like
	 * it could happen for a mosaic which has some holes.
	 * 
	 * @param global
	 * @param listener
	 * @return
	 * @throws IOException
	 * @see {@link BoundingBox}
	 * 
	 */
	public List<BoundingBox> getHorizontalDomain(final boolean global,
			final ProgressListener listener) throws IOException;
	
	
	/**
	 * An ordered {@link SortedSet} of {@link DirectPosition} element for the undelrying coverage.
	 * Note that the {@link CRS} for such positions can be <code>null</code> in case the overall spatial {@link CRS} 
	 * is a non-separable 3D {@link CRS} like WGS84-3D. Otherwise, all the positions should share the same
	 * {@link VerticalCRS}.
	 * 
	 * @param overall
	 * @param listener
	 * @return
	 * @throws IOException
	 * @todo consider {@link TransfiniteSet} as an alternative to {@link SortedSet}
	 * @todo allow using an interval as well as a direct position
	 * @todo allow transfinite sets!
	 */
	public SortedSet<DirectPosition> getVerticalDomain(final boolean global,
			final ProgressListener listener) throws IOException;

	/**
	 * The first {@link Rectangle} should describe the overall bidimensional
	 * raster range for the underlying coverage. However, by setting the
	 * <code>overall</code> param to true we can request additional raster
	 * ranges in case the area covered by the mentioned coverage is poorly
	 * approximated by a single {@link Rectangle}, like it could happen for a
	 * mosaic which has some holes.
	 * 
	 * @param overall
	 * @param listener
	 * @return
	 * @throws IOException
	 * 
	 * @todo should we consider {@link GridEnvelope}?? or {@link ImageLayout} which also contains tiling information???
	 */
	public List<Rectangle> getRasterDomain(final boolean overall,
			final ProgressListener listener) throws IOException;
	/**
	 * Optimal size to use for each dimension when accessing grid values. These
	 * values together give the optimal block size to use when retrieving grid
	 * coverage values. For example, a client application can achieve better
	 * performance for a 2-D grid coverage by reading blocks of 128 by 128 if
	 * the grid is tiled into blocks of this size. The sequence is ordered by
	 * dimension. If the implementation does not have optimal sizes, the
	 * sequence will be <code>null</code>.
	 * 
	 * @return The optimal size to use for each dimension when accessing grid
	 *         values, or <code>null</code> if none.
	 */
	public int[] getOptimalDataBlockSizes();

	/**
	 * Transformation between the 2D raster space and the 2D model space. In
	 * case the underlying coverage is unrectified this transformation maybe a
	 * georeferencing transformation of simply the identity in case we do not
	 * have means to georeference the mentioned coverage.
	 * 
	 * @param brief
	 * @param listener
	 * @return
	 * @throws IOException
	 */
	public MathTransform2D getGridToWorldTransform(final boolean brief,
			final ProgressListener listener) throws IOException;

	/**
	 * Describes the temporal domain for the underlying {@link CoverageSource}
	 * by returning an ordered {@link Set} of {@link TemporalPrimitive}s elements for
	 * it.
	 * Note that the {@link TemporalCRS} for the listed {@link TemporalGeometricPrimitive}s
	 * can be obtained from the overall {@link CRS} for the underlying coverage.
	 * 
	 * @todo should we change to something different than
	 *       {@link TemporalPrimitive}? Should we consider GML TimeSequence
	 * @param listener
	 * @return an ordered {@link Set} of {@link TemporalPrimitive}s elements.
	 * @todo allow transfinite sets!
	 * @throws IOException
	 */
	public SortedSet<TemporalGeometricPrimitive> getTemporalDomain(
			final ProgressListener listener) throws IOException;
	
	/**
	 * Describes the {@link CRS} for the underlying coverage.
	 * It can be a {@link CompoundCRS} in case we have an xD coverage where x>2.
	 *  
	 * @param listener
	 * @return
	 * @throws IOException
	 */
	public CoordinateReferenceSystem getCoordinateReferenceSystem(
			final ProgressListener listener) throws IOException;	

	
	/**
	 * 
	 * @param request
	 * @param listener
	 * @return
	 * @throws IOException
	 */
	public CoverageResponse read(final CoverageRequest request,final ProgressListener listener) throws IOException;

	/**
	 * Retrieves a {@link RangeType} instance which can be used to describe the
	 * codomain for the underlying coverage.
	 * 
	 * @param listener
	 * @return a {@link RangeType} instance which can be used to describe the
	 * 			codomain for the underlying coverage.
	 * @throws IOException in case something bad occurs
	 */
	public RangeType getRange(final ProgressListener listener) throws IOException;

	/**
	 * Closes this {@link CoverageSource} and releases any lock or cached information it holds.
	 * 
	 */
	public void dispose();
	
//	/**
//	 * @todo TBD, I am not even sure this should leave at the general interface level!
//	 * 
//	 * @return
//	 * @throws IOException
//	 */
//	public Object getGCPManager(final ProgressListener listener)throws IOException;
//	
//	/**
//	 * @todo TBD, I am not even sure this should leave at the general interface level!
//	 * 
//	 * @return
//	 * @throws IOException
//	 */
//	public Object getStaticsManager(final ProgressListener listener)throws IOException;
//	
//	
//	
//	/**
//	 * @todo TBD, I am not even sure this should leave at the general interface level!
//	 * 
//	 * @return
//	 * @throws IOException
//	 */
//	public Object getOverviesManager(final ProgressListener listener)throws IOException;
	

}