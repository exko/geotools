/*
 * Geotools - OpenSource mapping toolkit
 * (C) 2002, Centre for Computational Geography
 * (C) 2001, Institut de Recherche pour le D�veloppement
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
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 * Contacts:
 *     UNITED KINGDOM: James Macgill
 *             mailto:j.macgill@geog.leeds.ac.uk
 *
 *     FRANCE: Surveillance de l'Environnement Assist�e par Satellite
 *             Institut de Recherche pour le D�veloppement / US-Espace
 *             mailto:seasnet@teledetection.fr
 *
 *     CANADA: Observatoire du Saint-Laurent
 *             Institut Maurice-Lamontagne
 *             mailto:osl@osl.gc.ca
 *
 *    This package contains documentation from OpenGIS specifications.
 *    OpenGIS consortium's work is fully acknowledged here.
 */
package org.geotools.gc;

// J2SE dependencies
import java.awt.image.RenderedImage;

// JAI dependencies
import javax.media.jai.Histogram;

// Geotools dependencies
import org.geotools.cv.SampleDimension;
import org.geotools.cv.SampleDimensionType;
import org.geotools.cv.ColorInterpretation;


/**
 * Describes the band values for a grid coverage.
 *
 * @version $Id: GridSampleDimension.java,v 1.4 2002/10/17 21:11:04 desruisseaux Exp $
 * @author <A HREF="www.opengis.org">OpenGIS</A>
 * @author Martin Desruisseaux
 */
final class GridSampleDimension extends SampleDimension {
    /**
     * Band number for this sample dimension.
     */
    private final int band;

    /**
     * The grid value data type.
     */
    private final SampleDimensionType type;
    
    /**
     * Construct a sample dimension with a set of categories from an other sample dimension.
     *
     * @param band  The originating sample dimension.
     * @param image The image to be wrapped by {@link GridCoverage}.
     * @param bandNumber The band number.
     */
    public GridSampleDimension(final SampleDimension band,
                               final RenderedImage   image,
                               final int             bandNumber)
    {
        super(band);
        this.band = bandNumber;
        this.type = SampleDimensionType.getEnum(image.getSampleModel(), bandNumber);
    }

    /**
     * Returns a code value indicating grid value data type.
     * This will also indicate the number of bits for the data type.
     *
     * @return a code value indicating grid value data type.
     */
    public SampleDimensionType getSampleDimensionType() {
        return type;
    }

    /**
     * Returns the color interpretation of the sample dimension.
     * Since {@link CategoryList} are designed for indexed color
     * models, current implementation returns {@link ColorInterpretation#PALETTE_INDEX}.
     * We need to find a more general way in some future version.
     */
    public ColorInterpretation getColorInterpretation() {
        return ColorInterpretation.PALETTE_INDEX;
    }
    
    /**
     * Returns the minimum value occurring in this sample dimension.
     */
    //  public double getMinimumValue()
    //  {return getHistogram().getLowValue(band);}
    
    /**
     * Returns the maximum value occurring in this sample dimension.
     */
    //  public double getMaximumValue()
    //  {return getHistogram().getHighValue(band);}
    
    /**
     * Determine the mode grid value in this sample dimension.
     */
    //  public double getModeValue()
    //  {throw new UnsupportedOperationException("Not implemented");}
    
    /**
     * Determine the median grid value in this sample dimension.
     */
    //  public double getMedianValue()
    //  {throw new UnsupportedOperationException("Not implemented");}
    
    /**
     * Determine the mean grid value in this sample dimension.
     */
    //  public double getMeanValue()
    //  {return getHistogram().getMean()[band];}
    
    /**
     * Determine the standard deviation from the mean
     * of the grid values in a sample dimension.
     */
    //  public double getStandardDeviation()
    //  {return getHistogram().getStandardDeviation()[band];}
    
    /**
     * Gets the histogram for the underlying grid coverage.
     */
    //  private Histogram getHistogram()
    //  {throw new UnsupportedOperationException("Not implemented");}
}
