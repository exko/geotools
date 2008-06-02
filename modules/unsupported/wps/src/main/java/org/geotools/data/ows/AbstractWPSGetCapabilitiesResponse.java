/*
 *    GeoTools - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2004-2006, GeoTools Project Managment Committee (PMC)
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
package org.geotools.data.ows;

import java.io.IOException;
import java.io.InputStream;
import net.opengis.wps.WPSCapabilitiesType;

import org.geotools.ows.ServiceException;

/**
 * Base class for WPS GetCapabilities responses. They should typically attempt to
 * parse the Capabilities document in inputStream in the constructor. 
 * 
 * @author gdavis
 */
public abstract class AbstractWPSGetCapabilitiesResponse extends Response {

	protected WPSCapabilitiesType capabilities;

	public AbstractWPSGetCapabilitiesResponse(String contentType, InputStream inputStream) throws ServiceException, IOException {
		super(contentType, inputStream);
	}
 
	/**
	 * Returns the capabilities object parsed during the response
	 */
	public WPSCapabilitiesType getCapabilities() {
		return capabilities;
	}
}
