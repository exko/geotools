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
package org.geotools.arcsde.gce.band;

import java.awt.image.WritableRaster;

import org.geotools.arcsde.gce.imageio.RasterCellType;
import org.geotools.data.DataSourceException;

import com.esri.sde.sdk.client.SeRasterTile;

/**
 * @deprecated leaving in test code by now until making sure we're not loosing test coverage
 */
@Deprecated
public abstract class ArcSDERasterBandCopier {

    protected int tileWidth, tileHeight;

    public static ArcSDERasterBandCopier getInstance(RasterCellType pixelType, int tileWidth,
            int tileHeight) {
        ArcSDERasterBandCopier ret;
        switch (pixelType) {
        case TYPE_8BIT_U:
            ret = new UnsignedByteBandCopier();
            break;
        case TYPE_1BIT:
            ret = new OneBitBandCopier();
            break;
        case TYPE_32BIT_REAL:
            ret = new FloatBandCopier();
            break;
        case TYPE_16BIT_U:
            ret = new UnsignedShortBandCopier();
            break;
        case TYPE_16BIT_S:
            ret = new ShortBandCopier();
            break;
        case TYPE_4BIT:
            ret = new UnsignedByteBandCopier();
            break;
        default:
            throw new IllegalArgumentException(
                    "Don't know how to create ArcSDE band reader for pixel type " + pixelType);
        }
        ret.tileWidth = tileWidth;
        ret.tileHeight = tileHeight;
        return ret;
    }

    /**
     * @param tile
     *            The actual tile you wish to copy from
     * @param raster
     *            The raster into which data should be copied
     * @param copyOffX
     *            The x-coordinate of the TILE at which the raster should start copying
     * @param copyOffY
     *            The y-coordinate of the TILE at which the raster should start copying
     * @param targetBand
     *            The band in the supplied raster into which the data from this tile should be
     *            copied
     * @throws DataSourceException
     */
    public abstract void copyPixelData(SeRasterTile tile, WritableRaster raster, int copyOffX,
            int copyOffY, int targetBand) throws DataSourceException;

}