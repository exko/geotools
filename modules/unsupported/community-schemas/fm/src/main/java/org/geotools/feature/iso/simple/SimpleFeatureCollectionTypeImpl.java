/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2006-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.feature.iso.simple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.geotools.feature.iso.Types;
import org.geotools.feature.iso.type.AssociationDescriptorImpl;
import org.geotools.feature.iso.type.AssociationTypeImpl;
import org.geotools.feature.iso.type.FeatureCollectionTypeImpl;
import org.opengis.feature.simple.SimpleFeatureCollectionType;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AssociationDescriptor;
import org.opengis.feature.type.AssociationType;
import org.opengis.feature.type.TypeName;
import org.opengis.util.InternationalString;

public class SimpleFeatureCollectionTypeImpl extends FeatureCollectionTypeImpl
		implements SimpleFeatureCollectionType {

	public SimpleFeatureCollectionTypeImpl(TypeName name,
			AssociationDescriptor member, Set restrictions,
			InternationalString description) {
		super(name, new ArrayList(), Collections.singleton(member), null, null,
				false, restrictions, null, description);
	}

	public SimpleFeatureCollectionTypeImpl(TypeName name,
			SimpleFeatureType member, InternationalString description) {
		super( name, Collections.EMPTY_LIST, members(member), null, member.getCRS(), false,
				Collections.EMPTY_SET, null, description);
	}

	private static final List members(SimpleFeatureType member) {
		AssociationType aggregation = new AssociationTypeImpl(Types
				.typeName("contained"), member, false, false,
				Collections.EMPTY_SET, null, null);

		AssociationDescriptor memberOf = new AssociationDescriptorImpl(
				aggregation, Types.typeName("memberOf"), 0, Integer.MAX_VALUE);

		return Collections.singletonList(memberOf);
	}

	public SimpleFeatureType getMemberType() {
		if (MEMBERS.isEmpty())
			return null;

		AssociationDescriptor ad = (AssociationDescriptor) MEMBERS.iterator()
				.next();

		if (ad != null) {
			return (SimpleFeatureType) ad.getType().getReferenceType();
		}
		return null;
	}
}
