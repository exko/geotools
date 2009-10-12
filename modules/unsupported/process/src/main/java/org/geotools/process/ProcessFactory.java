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
package org.geotools.process;

import java.util.Map;
import java.util.Set;

import org.geotools.data.Parameter;
import org.opengis.feature.type.Name;
import org.opengis.util.InternationalString;

/**
 * Used to describe the parameters needed for a group of Process, and for creating a Process to use.
 *
 * @author gdavis
 * @author Andrea Aime - OpenGeo
 *
 * @source $URL$
 */
public interface ProcessFactory {
    public static final String GT_NAMESPACE = "gt";
	
    /**
     * The names (non human readable) that can be used to
     * refer to the processes generated by this factory.
     * <p>
     * This name is used to advertise the availability of a Process
     * in a WPS; while the Title and Description will change depending
     * on the users locale; this name will be consistent.
     * </p>
     * It is up to the implementor to ensure this name is unique
     * @return a set of names handled by this process factory
     */
	public Set<Name> getNames();	
	
    /** Human readable title suitable for display for the specified process
     * <p>
     * Please note that this title is *not* stable across locale; if you want
     * to remember a ProcessFactory between runs please use getName (which is
     * dependent on the implementor to guarantee uniqueness) or use the classname
     * @param name the process identifier
     */
	public InternationalString getTitle(Name name);
	
	/**
	 * Human readable description of the specified process
	 * @param name the process whose description is to be returned
	 * @return
	 */
	public InternationalString getDescription(Name name);
	
	/**
	 * Description of the Map parameter to use when executing.
	 * @param name the process identifier
	 * @return Description of required parameters
	 * 
	 */
	public Map<String,Parameter<?>> getParameterInfo(Name name);
	
	/**
	 * Create a process for execution.
	 * @return Process implementation
	 * @param name the process identifier
	 */
	public Process create(Name name);
	
	/**
	 * Description of the results returned
	 * @param name the process identifier
	 * @param parameters the parameters to be used
	 * @return
	 * @throws IllegalArgumentException
	 */
	public Map<String,Parameter<?>> getResultInfo(Name name, Map<String, Object> parameters) throws IllegalArgumentException;
	
	/**
	 * It is up to the process implementors to implement progress on the task,
	 * this method is used to see if the process has progress monitoring implemented
	 * @param name the process identifier
	 * @return true if it supports progress monitoring
	 */
	public boolean supportsProgress(Name name);
	
	/**
	 * Return the version of the process
	 * @param name the process identifier
	 * @return String version
	 */
	public String getVersion(Name name);	
}
