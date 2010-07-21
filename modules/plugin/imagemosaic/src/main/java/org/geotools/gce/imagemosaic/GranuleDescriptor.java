/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2007-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.gce.imagemosaic;

import it.geosolutions.imageio.utilities.Utilities;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.ROIShape;
import javax.media.jai.TileCache;
import javax.media.jai.TileScheduler;
import javax.media.jai.operator.AffineDescriptor;

import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.data.DataUtilities;
import org.geotools.factory.Hints;
import org.geotools.gce.imagemosaic.RasterLayerResponse.GranuleLoadingResult;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.LiteShape2;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.builder.GridToEnvelopeMapper;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.resources.geometry.XRectangle2D;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;

import com.sun.media.jai.util.Rational;
import com.vividsolutions.jts.geom.Geometry;

/**
 * A granuleDescriptor is a single piece of the mosaic, with its own overviews and
 * everything.
 * 
 * <p>
 * This class is responsible for caching the various size of the different
 * levels of each single granuleDescriptor since computing them each time is expensive
 * (opening a file, looking for a reader, parsing metadata,etc...).
 * 
 * <p>
 * Right now we are making the assumption that a single granuleDescriptor is made a by a
 * single file with embedded overviews, either explicit or intrinsic through wavelets like MrSID,
 * ECW or JPEG2000.
 * 
 * @author Simone Giannecchini, GeoSolutions S.A.S.
 * @author Stefan Alfons Krueger (alfonx), Wikisquare.de : Support for jar:file:foo.jar/bar.properties URLs
 * @since 2.5.5
 */
public class GranuleDescriptor {
	
	/** Logger. */
	private final static Logger LOGGER = org.geotools.util.logging.Logging.getLogger(GranuleDescriptor.class); 
	   
    // FORMULAE FOR FORWARD MAP are derived as follows
    //     Nearest
    //        Minimum:
    //            srcMin = floor ((dstMin + 0.5 - trans) / scale)
    //            srcMin <= (dstMin + 0.5 - trans) / scale < srcMin + 1
    //            srcMin*scale <= dstMin + 0.5 - trans < (srcMin + 1)*scale
    //            srcMin*scale - 0.5 + trans
    //                       <= dstMin < (srcMin + 1)*scale - 0.5 + trans
    //            Let A = srcMin*scale - 0.5 + trans,
    //            Let B = (srcMin + 1)*scale - 0.5 + trans
    //
    //            dstMin = ceil(A)
    //
    //        Maximum:
    //            Note that srcMax is defined to be srcMin + dimension - 1
    //            srcMax = floor ((dstMax + 0.5 - trans) / scale)
    //            srcMax <= (dstMax + 0.5 - trans) / scale < srcMax + 1
    //            srcMax*scale <= dstMax + 0.5 - trans < (srcMax + 1)*scale
    //            srcMax*scale - 0.5 + trans
    //                       <= dstMax < (srcMax+1) * scale - 0.5 + trans
    //            Let float A = (srcMax + 1) * scale - 0.5 + trans
    //
    //            dstMax = floor(A), if floor(A) < A, else
    //            dstMax = floor(A) - 1
    //            OR dstMax = ceil(A - 1)
    //
    //     Other interpolations
    //
    //        First the source should be shrunk by the padding that is
    //        required for the particular interpolation. Then the
    //        shrunk source should be forward mapped as follows:
    //
    //        Minimum:
    //            srcMin = floor (((dstMin + 0.5 - trans)/scale) - 0.5)
    //            srcMin <= ((dstMin + 0.5 - trans)/scale) - 0.5 < srcMin+1
    //            (srcMin+0.5)*scale <= dstMin+0.5-trans <
    //                                                  (srcMin+1.5)*scale
    //            (srcMin+0.5)*scale - 0.5 + trans
    //                       <= dstMin < (srcMin+1.5)*scale - 0.5 + trans
    //            Let A = (srcMin+0.5)*scale - 0.5 + trans,
    //            Let B = (srcMin+1.5)*scale - 0.5 + trans
    //
    //            dstMin = ceil(A)
    //
    //        Maximum:
    //            srcMax is defined as srcMin + dimension - 1
    //            srcMax = floor (((dstMax + 0.5 - trans) / scale) - 0.5)
    //            srcMax <= ((dstMax + 0.5 - trans)/scale) - 0.5 < srcMax+1
    //            (srcMax+0.5)*scale <= dstMax + 0.5 - trans <
    //                                                   (srcMax+1.5)*scale
    //            (srcMax+0.5)*scale - 0.5 + trans
    //                       <= dstMax < (srcMax+1.5)*scale - 0.5 + trans
    //            Let float A = (srcMax+1.5)*scale - 0.5 + trans
    //
    //            dstMax = floor(A), if floor(A) < A, else
    //            dstMax = floor(A) - 1
    //            OR dstMax = ceil(A - 1)
    //
    

	private static float rationalTolerance = 0.000001F;
    private static Rectangle2D layoutHelper(RenderedImage source,
                                            float scaleX,
                                            float scaleY,
                                            float transX,
                                            float transY,
                                            Interpolation interp) {

        // Represent the scale factors as Rational numbers.
		// Since a value of 1.2 is represented as 1.200001 which
		// throws the forward/backward mapping in certain situations.
		// Convert the scale and translation factors to Rational numbers
		Rational scaleXRational = Rational.approximate(scaleX,rationalTolerance);
		Rational scaleYRational = Rational.approximate(scaleY,rationalTolerance);

		long scaleXRationalNum = (long) scaleXRational.num;
		long scaleXRationalDenom = (long) scaleXRational.denom;
		long scaleYRationalNum = (long) scaleYRational.num;
		long scaleYRationalDenom = (long) scaleYRational.denom;

		Rational transXRational = Rational.approximate(transX,rationalTolerance);
		Rational transYRational = Rational.approximate(transY,rationalTolerance);

		long transXRationalNum = (long) transXRational.num;
		long transXRationalDenom = (long) transXRational.denom;
		long transYRationalNum = (long) transYRational.num;
		long transYRationalDenom = (long) transYRational.denom;

		int x0 = source.getMinX();
		int y0 = source.getMinY();
		int w = source.getWidth();
		int h = source.getHeight();

		// Variables to store the calculated destination upper left coordinate
		long dx0Num, dx0Denom, dy0Num, dy0Denom;

		// Variables to store the calculated destination bottom right
		// coordinate
		long dx1Num, dx1Denom, dy1Num, dy1Denom;

		// Start calculations for destination

		dx0Num = x0;
		dx0Denom = 1;

		dy0Num = y0;
		dy0Denom = 1;

		// Formula requires srcMaxX + 1 = (x0 + w - 1) + 1 = x0 + w
		dx1Num = x0 + w;
		dx1Denom = 1;

		// Formula requires srcMaxY + 1 = (y0 + h - 1) + 1 = y0 + h
		dy1Num = y0 + h;
		dy1Denom = 1;

		dx0Num *= scaleXRationalNum;
		dx0Denom *= scaleXRationalDenom;

		dy0Num *= scaleYRationalNum;
		dy0Denom *= scaleYRationalDenom;

		dx1Num *= scaleXRationalNum;
		dx1Denom *= scaleXRationalDenom;

		dy1Num *= scaleYRationalNum;
		dy1Denom *= scaleYRationalDenom;

		// Equivalent to subtracting 0.5
		dx0Num = 2 * dx0Num - dx0Denom;
		dx0Denom *= 2;

		dy0Num = 2 * dy0Num - dy0Denom;
		dy0Denom *= 2;

		// Equivalent to subtracting 1.5
		dx1Num = 2 * dx1Num - 3 * dx1Denom;
		dx1Denom *= 2;

		dy1Num = 2 * dy1Num - 3 * dy1Denom;
		dy1Denom *= 2;

		// Adding translation factors

		// Equivalent to float dx0 += transX
		dx0Num = dx0Num * transXRationalDenom + transXRationalNum * dx0Denom;
		dx0Denom *= transXRationalDenom;

		// Equivalent to float dy0 += transY
		dy0Num = dy0Num * transYRationalDenom + transYRationalNum * dy0Denom;
		dy0Denom *= transYRationalDenom;

		// Equivalent to float dx1 += transX
		dx1Num = dx1Num * transXRationalDenom + transXRationalNum * dx1Denom;
		dx1Denom *= transXRationalDenom;

		// Equivalent to float dy1 += transY
		dy1Num = dy1Num * transYRationalDenom + transYRationalNum * dy1Denom;
		dy1Denom *= transYRationalDenom;

		// Get the integral coordinates
		int l_x0, l_y0, l_x1, l_y1;

		l_x0 = Rational.ceil(dx0Num, dx0Denom);
		l_y0 = Rational.ceil(dy0Num, dy0Denom);

		l_x1 = Rational.ceil(dx1Num, dx1Denom);
		l_y1 = Rational.ceil(dy1Num, dy1Denom);

		// Set the top left coordinate of the destination
		final Rectangle2D retValue= new Rectangle2D.Double();
		retValue.setFrame(l_x0, l_y0, l_x1 - l_x0 + 1, l_y1 - l_y0 + 1);
		return retValue;
	}
	/**
	 * This class represent an overview level in a single granuleDescriptor.
	 * 
	 * <p> Notice that the internal transformations for the various levels are reffered to the corner, rather than to the centre.
	 * @author Simone Giannecchini, GeoSolutions S.A.S.
	 *
	 */
	class GranuleOverviewLevelDescriptor{

		final double scaleX;
		
		final double scaleY;
		
		final int width;
		
		final int height;

		final AffineTransform2D baseToLevelTransform;
		
		final AffineTransform2D gridToWorldTransformCorner;

		final Rectangle rasterDimensions;
		
		
		public AffineTransform getBaseToLevelTransform() {
			return baseToLevelTransform;
		}

		public double getScaleX() {
			return scaleX;
		}

		public double getScaleY() {
			return scaleY;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public GranuleOverviewLevelDescriptor(final double scaleX,final double scaleY,final int width,final int height) {
			this.scaleX = scaleX;
			this.scaleY = scaleY;
			this.baseToLevelTransform=new AffineTransform2D( XAffineTransform.getScaleInstance(scaleX,scaleY,0,0));
			
			final AffineTransform gridToWorldTransform_ = new AffineTransform(baseToLevelTransform);
			gridToWorldTransform_.preConcatenate(Utils.CENTER_TO_CORNER);
			gridToWorldTransform_.preConcatenate(baseGridToWorld);
			this.gridToWorldTransformCorner=new AffineTransform2D(gridToWorldTransform_);
			this.width = width;
			this.height = height;
			this.rasterDimensions= new Rectangle(0,0,width,height);
		}

		public Rectangle getBounds() {
			return (Rectangle) rasterDimensions.clone();
		}

		public AffineTransform2D getGridToWorldTransform() {
			return gridToWorldTransformCorner;
		}		
		


		@Override
		public String toString() {
			// build a decent representation for this level
			final StringBuilder buffer = new StringBuilder();
			buffer.append("Description of a granuleDescriptor level").append("\n")
			.append("width:\t\t").append(width).append("\n")
			.append("height:\t\t").append(height).append("\n")
			.append("scaleX:\t\t").append(scaleX).append("\n")
			.append("scaleY:\t\t").append(scaleY).append("\n")
			.append("baseToLevelTransform:\t\t").append(baseToLevelTransform.toString()).append("\n")
			.append("gridToWorldTransform:\t\t").append(gridToWorldTransformCorner.toString()).append("\n");
			return buffer.toString();
		}
		
	}
	
	ReferencedEnvelope granuleBBOX;
	
	ROIShape granuleROIShape;
        
        Geometry inclusionGeometry;
	
	URL granuleUrl;
	
	int maxDecimationFactor = -1;
	
	final Map<Integer,GranuleOverviewLevelDescriptor> granuleLevels= Collections.synchronizedMap(new HashMap<Integer,GranuleOverviewLevelDescriptor>());
	
	AffineTransform baseGridToWorld;
	
	ImageReaderSpi cachedReaderSPI;

	SimpleFeature originator;

	private void init(final BoundingBox granuleBBOX, final URL granuleUrl,
			final ImageReaderSpi suggestedSPI, final Geometry inclusionGeometry) {
		this.granuleBBOX = ReferencedEnvelope.reference(granuleBBOX);
		this.granuleUrl = granuleUrl;
		this.inclusionGeometry = inclusionGeometry;
		
		// create the base grid to world transformation
		ImageInputStream inStream = null;
		ImageReader reader = null;
		try {
			//
			//get info about the raster we have to read
			//
			
			// get a stream
			inStream = Utils.getInputStream(granuleUrl);
			if(inStream == null)
				throw new IllegalArgumentException("Unable to get an input stream for the provided file "+granuleUrl.toString());
			
			// get a reader and try to cache the suggested SPI first
			if(cachedReaderSPI == null){
				inStream.mark();
				if(suggestedSPI!=null && suggestedSPI.canDecodeInput(inStream))
				{
					cachedReaderSPI=suggestedSPI;
					inStream.reset();
				}
				else{
					inStream.mark();
					reader = Utils.getReader(inStream);
					if(reader != null)
						cachedReaderSPI = reader.getOriginatingProvider();
					inStream.reset();
				}
				
			}
			reader = cachedReaderSPI.createReaderInstance();
			if(reader == null)
				throw new IllegalArgumentException("Unable to get an ImageReader for the provided file "+granuleUrl.toString());
			
			//get selected level and base level dimensions
			final Rectangle originalDimension = Utils.getDimension(0,inStream, reader);
			
			// build the g2W for this tile, in principle we should get it
			// somehow from the tile itself or from the index, but at the moment
			// we do not have such info, hence we assume that it is a simple
			// scale and translate
			final GridToEnvelopeMapper geMapper= new GridToEnvelopeMapper(new GridEnvelope2D(originalDimension), granuleBBOX);
			geMapper.setPixelAnchor(PixelInCell.CELL_CENTER);//this is the default behavior but it is nice to write it down anyway
			this.baseGridToWorld = geMapper.createAffineTransform();
			
			try {
                            if (inclusionGeometry != null){
                                        LiteShape2 shape = null;
                                        geMapper.setPixelAnchor(PixelInCell.CELL_CORNER);
                                shape = new LiteShape2(inclusionGeometry, geMapper.createTransform().inverse(),null, false);
                                this.granuleROIShape = (ROIShape) new ROIShape(shape);
                            }
                            
                        } catch (TransformException e1) {
                            throw new IllegalArgumentException(e1);
                        } catch (FactoryException e1) {
                            throw new IllegalArgumentException(e1);
                        }
			
			// add the base level
			this.granuleLevels.put(Integer.valueOf(0), new GranuleOverviewLevelDescriptor(1, 1, originalDimension.width, originalDimension.height));

		} catch (IllegalStateException e) {
			throw new IllegalArgumentException(e);
			
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		} 
		finally{
			try{
				if(inStream != null)
					inStream.close();
			}
			catch (Throwable e) {
				throw new IllegalArgumentException(e);
			}
			finally{
				if(reader != null)
					reader.dispose();
			}
		}
	}
	
	public GranuleDescriptor(
                final String granuleLocation,
                final BoundingBox granuleBBox, 
                final ImageReaderSpi suggestedSPI,
                final Geometry inclusionGeometry) {
	    this (granuleLocation, granuleBBox, suggestedSPI, inclusionGeometry, -1);
	}
	
	public GranuleDescriptor(
	        final String granuleLocation,
                final BoundingBox granuleBBox, 
                final ImageReaderSpi suggestedSPI,
                final Geometry inclusionGeometry,
                final int maxDecimationFactor) {

	    this.maxDecimationFactor = maxDecimationFactor;
            final URL rasterFile = DataUtilities.fileToURL(new File(granuleLocation));
            
            if (rasterFile == null) 
                    return;
            
            if (LOGGER.isLoggable(Level.FINER))
                LOGGER.finer("File found " + granuleLocation);
    
            this.originator = null;
            init (granuleBBox, rasterFile, suggestedSPI, inclusionGeometry);
        
	}
	
//        public GranuleDescriptor(
//                final String granuleLocation,
//                final GridGeometry2D gridGeometry,
//                final ImageReaderSpi suggestedSPI,
//                final Geometry inclusionGeometry,
//                final int maxDecimationFactor) {
//
//            this.maxDecimationFactor = maxDecimationFactor;
//            final URL rasterFile = DataUtilities.fileToURL(new File(granuleLocation));
//            
//            if (rasterFile == null) {
//                    return;
//            }
//            if (LOGGER.isLoggable(Level.FINER)) {
//                    LOGGER.finer("File found "+granuleLocation);
//            }
//    
//            this.originator = null;
//            init2(gridGeometry, rasterFile, suggestedSPI, inclusionGeometry);
//        
//        }	
	
	public GranuleDescriptor(
                SimpleFeature feature, 
                ImageReaderSpi suggestedSPI,
                PathType pathType,
                final String locationAttribute,
                final String parentLocation) {
                    this(feature,suggestedSPI,pathType,locationAttribute,parentLocation,null);
                }
	
	public GranuleDescriptor(
			SimpleFeature feature, 
			ImageReaderSpi suggestedSPI,
			PathType pathType,
			final String locationAttribute,
			final String parentLocation,
			final Geometry inclusionGeometry) {
		// Get location and envelope of the image to load.
		final String granuleLocation = (String) feature.getAttribute(locationAttribute);
		final ReferencedEnvelope granuleBBox = ReferencedEnvelope.reference(feature.getBounds());
		

		// If the granuleDescriptor is not there, dump a message and continue
		final URL rasterFile = pathType.resolvePath(parentLocation, granuleLocation);
		if (rasterFile == null) {
			return;
		}
		if (LOGGER.isLoggable(Level.FINE))
			LOGGER.fine("File found "+granuleLocation);

		this.originator=feature;
		init(granuleBBox,rasterFile,suggestedSPI, inclusionGeometry);
		
		
	}

	public GranuleLoadingResult loadRaster(
			final ImageReadParam readParameters,
			final int imageIndex, 
			final ReferencedEnvelope cropBBox,
			final MathTransform2D mosaicWorldToGrid,
			final RasterLayerRequest request,
			final Hints hints) throws IOException {
		
		if (LOGGER.isLoggable(java.util.logging.Level.FINER)){
		    final String name = Thread.currentThread().getName();
			LOGGER.finer("Thread:" + name + " Loading raster data for granuleDescriptor "+this.toString());
		}

		final ReferencedEnvelope bbox = inclusionGeometry != null? new ReferencedEnvelope(granuleBBOX.intersection(inclusionGeometry.getEnvelopeInternal()), granuleBBOX.getCoordinateReferenceSystem()):granuleBBOX;
                
                // intersection of this tile bound with the current crop bbox
                final ReferencedEnvelope intersection = new ReferencedEnvelope(bbox.intersection(cropBBox), cropBBox.getCoordinateReferenceSystem());
                if (intersection.isEmpty()) {
                    if (LOGGER.isLoggable(java.util.logging.Level.FINE)){
                            LOGGER.fine(new StringBuilder("Got empty intersection for granule ").append(this.toString())
                                    .append(" with request ").append(request.toString()).append(" Resulting in no granule loaded: Empty result").toString());
                    }
                    return null;
                }

		ImageInputStream inStream=null;
		ImageReader reader=null;
		try {
			//
			//get info about the raster we have to read
			//
			
			// get a stream
			inStream = Utils.getInputStream(granuleUrl);
			if(inStream==null)
				return null;
	
			// get a reader and try to cache the relevant SPI
			if(cachedReaderSPI==null){
				reader = Utils.getReader( inStream);
				if(reader!=null)
					cachedReaderSPI=reader.getOriginatingProvider();
			}
			else
				reader=cachedReaderSPI.createReaderInstance();
			if(reader==null) {
				if (LOGGER.isLoggable(java.util.logging.Level.WARNING)){
					LOGGER.warning(new StringBuilder("Unable to get s reader for granuleDescriptor ").append(this.toString())
					        .append(" with request ").append(request.toString()).append(" Resulting in no granule loaded: Empty result").toString());
				}
				return null;
			}
			
			//get selected level and base level dimensions
			final GranuleOverviewLevelDescriptor selectedlevel= getLevel(imageIndex);
	
			
			// now create the crop grid to world which can be used to decide
			// which source area we need to crop in the selected level taking
			// into account the scale factors imposed by the selection of this
			// level together with the base level grid to world transformation
                        AffineTransform tempCropWorldToGrid = new AffineTransform(selectedlevel.gridToWorldTransformCorner);
                        AffineTransform2D cropWorldToGrid= new AffineTransform2D(tempCropWorldToGrid);
                        cropWorldToGrid=(AffineTransform2D) cropWorldToGrid.inverse();
			// computing the crop source area which lives into the
			// selected level raster space, NOTICE that at the end we need to
			// take into account the fact that we might also decimate therefore
			// we cannot just use the crop grid to world but we need to correct
			// it.
			final Rectangle sourceArea = CRS.transform(cropWorldToGrid, intersection).toRectangle2D().getBounds();
			XRectangle2D.intersect(sourceArea, selectedlevel.rasterDimensions, sourceArea);//make sure roundings don't bother us
			// is it empty??
			if (sourceArea.isEmpty()) {
				if (LOGGER.isLoggable(java.util.logging.Level.FINE)){
					LOGGER.fine("Got empty area for granuleDescriptor "+this.toString()+
					        " with request "+request.toString()+" Resulting in no granule loaded: Empty result");
					
				}
				return null;

			} else if (LOGGER.isLoggable(java.util.logging.Level.FINER)){
				LOGGER.finer((new StringBuffer("Loading level ").append(
						imageIndex).append(" with source region ").append(
						sourceArea).toString()));
			}

			// Setting subsampling 
			int newSubSamplingFactor = 0;
			final String pluginName = cachedReaderSPI.getPluginClassName();
			if (pluginName != null && pluginName.equals(Utils.DIRECT_KAKADU_PLUGIN)){
				final int ssx = readParameters.getSourceXSubsampling();
				final int ssy = readParameters.getSourceYSubsampling();
				newSubSamplingFactor = Utilities.getSubSamplingFactor2(ssx , ssy);
				if (newSubSamplingFactor != 0) {
				    if (newSubSamplingFactor > maxDecimationFactor && maxDecimationFactor != -1){
				        newSubSamplingFactor = maxDecimationFactor;
				    }
				    readParameters.setSourceSubsampling(newSubSamplingFactor, newSubSamplingFactor,0,0);
				}
			}
			
			// set the source region
			readParameters.setSourceRegion(sourceArea);
			final RenderedImage raster;
			try{
				// read
				raster= request.getReadType().read(readParameters,imageIndex, granuleUrl, selectedlevel.rasterDimensions,cachedReaderSPI, hints);
				
			}
			catch (Throwable e) {
				if (LOGGER.isLoggable(java.util.logging.Level.FINE)){
					LOGGER.log(java.util.logging.Level.FINE,
					        new StringBuilder("Unable to load raster for granuleDescriptor ").append(this.toString())
					        .append(" with request ").append(request.toString()).append(" Resulting in no granule loaded: Empty result").toString(),e);
				}
				return null;
			}

			// use fixed source area
			sourceArea.setRect(readParameters.getSourceRegion());

			//
			// setting new coefficients to define a new affineTransformation
			// to be applied to the grid to world transformation
			// -----------------------------------------------------------------------------------
			//
			// With respect to the original envelope, the obtained planarImage
			// needs to be rescaled. The scaling factors are computed as the
			// ratio between the cropped source region sizes and the read
			// image sizes.
			//
			// place it in the mosaic using the coords created above;
			double decimationScaleX =  ((1.0 * sourceArea.width) / raster.getWidth());
			double decimationScaleY =  ((1.0 * sourceArea.height) / raster.getHeight());
			final AffineTransform decimationScaleTranform = XAffineTransform.getScaleInstance(decimationScaleX, decimationScaleY);

			// keep into account translation  to work into the selected level raster space
			final AffineTransform afterDecimationTranslateTranform =XAffineTransform.getTranslateInstance(sourceArea.x, sourceArea.y);
			
			// now we need to go back to the base level raster space
			final AffineTransform backToBaseLevelScaleTransform =selectedlevel.baseToLevelTransform;
			
			// now create the overall transform
			final AffineTransform finalRaster2Model = new AffineTransform(baseGridToWorld);
			finalRaster2Model.concatenate(Utils.CENTER_TO_CORNER);
			final double x = finalRaster2Model.getTranslateX();
                        final double y = finalRaster2Model.getTranslateY();
                        
			if(!XAffineTransform.isIdentity(backToBaseLevelScaleTransform,10E-6))
				finalRaster2Model.concatenate(backToBaseLevelScaleTransform);
			if(!XAffineTransform.isIdentity(afterDecimationTranslateTranform,10E-6))
				finalRaster2Model.concatenate(afterDecimationTranslateTranform);
			if(!XAffineTransform.isIdentity(decimationScaleTranform,10E-6))
				finalRaster2Model.concatenate(decimationScaleTranform);

			// keep into account translation factors to place this tile
			finalRaster2Model.preConcatenate((AffineTransform) mosaicWorldToGrid);
			final Interpolation interpolation = request.getInterpolation();
			//paranoiac check to avoid that JAI freaks out when computing its internal layouT on images that are too small
			Rectangle2D finalLayout= layoutHelper(
					raster, 
					(float)finalRaster2Model.getScaleX(), 
					(float)finalRaster2Model.getScaleY(), 
					(float)finalRaster2Model.getTranslateX(), 
					(float)finalRaster2Model.getTranslateY(), 
					interpolation);
			if(finalLayout.isEmpty())
			{
				if(LOGGER.isLoggable(java.util.logging.Level.FINE))
					LOGGER.fine("Unable to create a granuleDescriptor "+this.toString()+ " due to jai scale bug");
				return null;
			}
			ROIShape granuleLoadingShape = null;
			if (granuleROIShape != null){
                            final Point2D translate = mosaicWorldToGrid.transform(new DirectPosition2D(x,y), (Point2D) null);
                            AffineTransform tx2 = new AffineTransform();
                            tx2.preConcatenate(AffineTransform.getScaleInstance(((AffineTransform)mosaicWorldToGrid).getScaleX(), 
                                    -((AffineTransform)mosaicWorldToGrid).getScaleY()));
                            tx2.preConcatenate(AffineTransform.getScaleInstance(((AffineTransform)baseGridToWorld).getScaleX(), 
                                    -((AffineTransform)baseGridToWorld).getScaleY()));
                            tx2.preConcatenate(AffineTransform.getTranslateInstance(translate.getX(),translate.getY()));
                            final Shape sp = granuleROIShape.getAsShape();
                            final Area area = new Area(sp);
                            area.transform(tx2);
                            granuleLoadingShape = new ROIShape(area);
                            
                        }
			// apply the affine transform  conserving indexed color model
			final RenderingHints localHints = new RenderingHints(JAI.KEY_REPLACE_INDEX_COLOR_MODEL, Boolean.FALSE);
			if(XAffineTransform.isIdentity(finalRaster2Model,10E-6))
			    return new GranuleLoadingResult(raster, granuleLoadingShape);
			else
			{
				//
				// In case we are asked to use certain tile dimensions we tile
				// also at this stage in case the read type is Direct since
				// buffered images comes up untiled and this can affect the
				// performances of the subsequent affine operation.
				//
				final Dimension tileDimensions=request.getTileDimensions();
				if(tileDimensions!=null&&request.getReadType().equals(ReadType.DIRECT_READ)) {
					final ImageLayout layout = new ImageLayout();
					layout.setTileHeight(tileDimensions.width).setTileWidth(tileDimensions.height);
					localHints.add(new RenderingHints(JAI.KEY_IMAGE_LAYOUT,layout));
				}
				if (hints != null && hints.containsKey(JAI.KEY_TILE_CACHE)){
				    final Object cache = hints.get(JAI.KEY_TILE_CACHE);
				    if (cache != null && cache instanceof TileCache)
				        localHints.add(new RenderingHints(JAI.KEY_TILE_CACHE, (TileCache) cache));
				}
				if (hints != null && hints.containsKey(JAI.KEY_TILE_SCHEDULER)){
                                    final Object scheduler = hints.get(JAI.KEY_TILE_SCHEDULER);
                                    if (scheduler != null && scheduler instanceof TileScheduler)
                                        localHints.add(new RenderingHints(JAI.KEY_TILE_SCHEDULER, (TileScheduler) scheduler));
                                }
				boolean addBorderExtender = true;
                                if (hints != null && hints.containsKey(JAI.KEY_BORDER_EXTENDER)) {
                                    final Object extender = hints.get(JAI.KEY_BORDER_EXTENDER);
                                    if (extender != null && extender instanceof BorderExtender) {
                                        localHints.add(new RenderingHints(JAI.KEY_BORDER_EXTENDER, (BorderExtender) extender));
                                        addBorderExtender = false;
                                    }
                                }
                                // border extender
                                if (addBorderExtender) {
                                    localHints.add(Utils.BORDER_EXTENDER_HINTS);
                                }
//				return WarpDescriptor.create(raster, new WarpAffine(translationTransform.createInverse()),new InterpolationNearest(), request.getBackgroundValues(),localHints);
				return new GranuleLoadingResult(AffineDescriptor.create(raster, finalRaster2Model, interpolation, request.getBackgroundValues(),localHints), granuleLoadingShape);
			}
		
		} catch (IllegalStateException e) {
			if (LOGGER.isLoggable(java.util.logging.Level.WARNING)) {
				LOGGER.log(java.util.logging.Level.WARNING, new StringBuilder("Unable to load raster for granuleDescriptor ")
				.append(this.toString()).append(" with request ").append(request.toString()).append(" Resulting in no granule loaded: Empty result").toString(), e);
			}
			return null;
		} catch (org.opengis.referencing.operation.NoninvertibleTransformException e) {
                    if (LOGGER.isLoggable(java.util.logging.Level.WARNING)) {
                        LOGGER.log(java.util.logging.Level.WARNING, new StringBuilder("Unable to load raster for granuleDescriptor ")
                        .append(this.toString()).append(" with request ").append(request.toString()).append(" Resulting in no granule loaded: Empty result").toString(), e);
                    }
			return null;
		} catch (TransformException e) {
                    if (LOGGER.isLoggable(java.util.logging.Level.WARNING)) {
                        LOGGER.log(java.util.logging.Level.WARNING, new StringBuilder("Unable to load raster for granuleDescriptor ")
                        .append(this.toString()).append(" with request ").append(request.toString()).append(" Resulting in no granule loaded: Empty result").toString(), e);
                    }
			return null;

		} 
		finally{
			try{
				if(inStream!=null)
					inStream.close();
			}
			finally{
				if(reader!=null)
					reader.dispose();
			}
		}
	}


	public GranuleOverviewLevelDescriptor getLevel(final int index) {
		synchronized (granuleLevels) {
			if(granuleLevels.containsKey(Integer.valueOf(index)))
				return granuleLevels.get(Integer.valueOf(index));
			else
			{
				//load level
				// create the base grid to world transformation
				ImageInputStream inStream=null;
				ImageReader reader=null;
				try {
					//
					//get info about the raster we have to read
					//
					
					// get a stream
					inStream = Utils.getInputStream(granuleUrl);
					if(inStream==null)
						throw new IllegalArgumentException();
			
					// get a reader and try to cache the relevant SPI
					if(cachedReaderSPI==null){
						reader = Utils.getReader( inStream);
						if(reader!=null)
							cachedReaderSPI=reader.getOriginatingProvider();
					}
					else
						reader=cachedReaderSPI.createReaderInstance();
					if(reader==null)
						throw new IllegalArgumentException("Unable to get an ImageReader for the provided file "+granuleUrl.toString());					
					
					//get selected level and base level dimensions
					final Rectangle levelDimension = Utils.getDimension(index,inStream, reader);
					
					
					final GranuleOverviewLevelDescriptor baseLevel= granuleLevels.get(0);
					final double scaleX=baseLevel.width/(1.0*levelDimension.width);
					final double scaleY=baseLevel.height/(1.0*levelDimension.height);
					
					// add the base level
					final GranuleOverviewLevelDescriptor newLevel=new GranuleOverviewLevelDescriptor(scaleX,scaleY,levelDimension.width,levelDimension.height);
					this.granuleLevels.put(Integer.valueOf(index),newLevel);
					
					return newLevel;
					

				} catch (IllegalStateException e) {
					throw new IllegalArgumentException(e);
					
				} catch (IOException e) {
					throw new IllegalArgumentException(e);
				} 
				finally{
					try{
						if(inStream!=null)
							inStream.close();
					}
					catch (Throwable e) {
						throw new IllegalArgumentException(e);
					}
					finally{
						if(reader!=null)
							reader.dispose();
					}
				}	
			}			
		}
	}

	@Override
	public String toString() {
		// build a decent representation for this level
		final StringBuilder buffer = new StringBuilder();
		buffer.append("Description of a granuleDescriptor ").append("\n");
		buffer.append("BBOX:\t\t").append(granuleBBOX.toString());
		buffer.append("file:\t\t").append(granuleUrl);
		buffer.append("gridToWorld:\t\t").append(baseGridToWorld);
		int i=1;
		for(final GranuleOverviewLevelDescriptor granuleOverviewLevelDescriptor : granuleLevels.values())
		{
			i++;
			buffer.append("Description of level ").append(i++).append("\n");
			buffer.append(granuleOverviewLevelDescriptor.toString()).append("\n");
		}
		return buffer.toString();
	}

	public BoundingBox getGranuleBBOX() {
		return granuleBBOX;
	}

	public URL getGranuleUrl() {
		return granuleUrl;
	}

	public SimpleFeature getOriginator() {
		return originator;
	}
	
}
