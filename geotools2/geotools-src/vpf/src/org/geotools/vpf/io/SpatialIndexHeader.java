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

package org.geotools.vpf.io;

import org.geotools.vpf.ifc.VPFHeader;


/**
 * SpatialIndexHeader.java Created: Tue Mar 11 23:42:48 2003
 *
 * @author <a href="mailto:kobit@users.sourceforge.net">Artur Hefczyc</a>
 * @version $Id: SpatialIndexHeader.java,v 1.5 2003/04/04 09:15:48 kobit Exp $
 */
public class SpatialIndexHeader implements VPFHeader {
    /**
     * Variable constant <code>SPATIAL_INDEX_HEADER_LENGTH</code> keeps value
     * of
     */
    public static final int SPATIAL_INDEX_HEADER_LENGTH = 24;
    protected int numPrims = 0;
    protected float xmin = 0;
    protected float ymin = 0;
    protected float xmax = 0;
    protected float ymax = 0;
    protected int numNodes = 0;

    /**
     * Creates a new SpatialIndexHeader object.
     *
     * @param numPrims DOCUMENT ME!
     * @param xmin DOCUMENT ME!
     * @param ymin DOCUMENT ME!
     * @param xmax DOCUMENT ME!
     * @param ymax DOCUMENT ME!
     * @param numNodes DOCUMENT ME!
     */
    public SpatialIndexHeader(
        int numPrims,
        float xmin,
        float ymin,
        float xmax,
        float ymax,
        int numNodes
    ) {
        this.numPrims = numPrims;
        this.xmin = xmin;
        this.ymin = ymin;
        this.xmax = xmax;
        this.ymax = ymax;
    }

    // SpatialIndexHeader constructor

    /**
     * Returns particular <code>VPFHeader</code> length.
     *
     * @return an <code>int</code> value of header length.
     */
    public int getLength() {
        return SPATIAL_INDEX_HEADER_LENGTH;
    }

    /**
     * Method <code><code>getRecordSize</code></code> is used to return size in
     * bytes of records stored in this table. If table keeps variable length
     * records <code>-1</code> should be returned.
     *
     * @return an <code><code>int</code></code> value
     */
    public int getRecordSize() {
        return -1;
    }

    /**
     * Gets the value of numPrims
     *
     * @return the value of numPrims
     */
    public int getNumPrims() {
        return this.numPrims;
    }

    //   /**
    //    * Sets the value of numPrims
    //    *
    //    * @param argNumPrims Value to assign to this.numPrims
    //    */
    //   public void setNumPrims(int argNumPrims)
    //   {
    // 	this.numPrims = argNumPrims;
    //   }

    /**
     * Gets the value of xmin
     *
     * @return the value of xmin
     */
    public float getXmin() {
        return this.xmin;
    }

    //   /**
    //    * Sets the value of xmin
    //    *
    //    * @param argXmin Value to assign to this.xmin
    //    */
    //   public void setXmin(float argXmin)
    //   {
    // 	this.xmin = argXmin;
    //   }

    /**
     * Gets the value of ymin
     *
     * @return the value of ymin
     */
    public float getYmin() {
        return this.ymin;
    }

    //   /**
    //    * Sets the value of ymin
    //    *
    //    * @param argYmin Value to assign to this.ymin
    //    */
    //   public void setYmin(float argYmin)
    //   {
    // 	this.ymin = argYmin;
    //   }

    /**
     * Gets the value of xmax
     *
     * @return the value of xmax
     */
    public float getXmax() {
        return this.xmax;
    }

    //   /**
    //    * Sets the value of xmax
    //    *
    //    * @param argXmax Value to assign to this.xmax
    //    */
    //   public void setXmax(float argXmax)
    //   {
    // 	this.xmax = argXmax;
    //   }

    /**
     * Gets the value of ymax
     *
     * @return the value of ymax
     */
    public float getYmax() {
        return this.ymax;
    }

    //   /**
    //    * Sets the value of ymax
    //    *
    //    * @param argYmax Value to assign to this.ymax
    //    */
    //   public void setYmax(float argYmax)
    //   {
    // 	this.ymax = argYmax;
    //   }

    /**
     * Gets the value of numNodes
     *
     * @return the value of numNodes
     */
    public int getNumNodes() {
        return this.numNodes;
    }

    //   /**
    //    * Sets the value of numNodes
    //    *
    //    * @param argNumNodes Value to assign to this.numNodes
    //    */
    //   public void setNumNodes(int argNumNodes)
    //   {
    // 	this.numNodes = argNumNodes;
    //   }
}


// SpatialIndexHeader
