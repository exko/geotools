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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.opengis.coverage.Coverage;

/**
 * @author   Simone Giannecchini, GeoSolutions
 * @todo revisit and improve when feedback starts to flow in
 */
public class CoverageUpdateRequest extends CoverageRequest {

	private Collection<? extends Coverage> data;
	
	/**
	 * @uml.property  name="metadata"
	 */
	private Map<String, String> metadata;
	
	

	public java.lang.String[] getMetadataNames() throws java.io.IOException {
		return null;
	}

	public java.lang.String getMetadataValue(java.lang.String arg0)
			throws java.io.IOException {
		return arg0;
	}

	/**
	 * @param metadata
	 * @throws java.io.IOException
	 * @uml.property  name="metadata"
	 */
	public void setMetadata(Map<String, String> metadata)
			throws java.io.IOException {
		this.metadata=new HashMap<String, String>(metadata);
	}

	/**
	 * @return
	 * @throws java.io.IOException
	 * @uml.property  name="metadata"
	 */
	public Map<String, String> getMetadata() throws java.io.IOException {
		return new HashMap<String, String>(this.metadata);
	}
	
	/**
	 * @param  metadata
	 * @uml.property  name="data"
	 */
	public void setData(Collection<? extends Coverage> data) {
		this.data = data;
	}

	/**
	 * @return
	 * @uml.property  name="data"
	 */
	public Collection<? extends Coverage> getData() {
		return data;
	}

}
