/*
 *    GeoTools - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2003-2006, Geotools Project Managment Committee (PMC)
 *    (C) 2001, Institut de Recherche pour le Développement
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
 */
package org.geotools.coverage.processing;

import java.awt.RenderingHints;
import java.io.Serializable;
import java.util.Iterator;

import org.opengis.coverage.Coverage;
import org.opengis.coverage.processing.Operation;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.util.InternationalString;

import org.geotools.factory.Hints;
import org.geotools.resources.Classes;
import org.geotools.resources.Utilities;
import org.geotools.resources.i18n.Errors;
import org.geotools.resources.i18n.ErrorKeys;


/**
 * Provides descriptive information for a {@linkplain Coverage coverage} processing operation.
 * The descriptive information includes such information as the name of the operation, operation
 * description, and number of source grid coverages required for the operation.
 *
 * @since 2.2
 * @source $URL$
 * @version $Id$
 * @author Martin Desruisseaux
 */
public abstract class AbstractOperation implements Operation, Serializable {
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = -1441856042779942954L;;

    /**
     * The parameters descriptor.
     */
    protected final ParameterDescriptorGroup descriptor;

    /**
     * Constructs an operation. The operation name will be the same than the
     * parameter descriptor name.
     *
     * @param descriptor The parameters descriptor.
     */
    public AbstractOperation(final ParameterDescriptorGroup descriptor) {
        ensureNonNull("descriptor", descriptor);
        this.descriptor = descriptor;
    }

    /**
     * Returns the name of the processing operation. The default implementation
     * returns the {@linkplain #descriptor} code name.
     */
    public String getName() {
        return descriptor.getName().getCode();
    }

    /**
     * Returns the description of the processing operation. If there is no description,
     * returns {@code null}. The default implementation returns the {@linkplain #descriptor}
     * remarks.
     */
    public String getDescription() {
        final InternationalString remarks = descriptor.getRemarks();
        return (remarks!=null) ? remarks.toString() : null;
    }

    /**
     * Returns the URL for documentation on the processing operation. If no online documentation
     * is available the string will be null. The default implementation returns {@code null}.
     */
    public String getDocURL() {
        return null;
    }

    /**
     * Returns the version number of the implementation.
     */
    public String getVersion() {
        return descriptor.getName().getVersion();
    }

    /**
     * Returns the vendor name of the processing operation implementation.
     * The default implementation returns "Geotools 2".
     */
    public String getVendor() {
        return "Geotools 2";
    }

    /**
     * Returns the number of source coverages required for the operation.
     */
    public int getNumSources() {
        return getNumSources(descriptor);
    }

    /**
     * Returns the number of source coverages in the specified parameter group.
     */
    private static int getNumSources(final ParameterDescriptorGroup descriptor) {
        int count = 0;
        for (final Iterator it=descriptor.descriptors().iterator(); it.hasNext();) {
            final GeneralParameterDescriptor candidate = (GeneralParameterDescriptor) it.next();
            if (candidate instanceof ParameterDescriptorGroup) {
                count += getNumSources((ParameterDescriptorGroup) candidate);
                continue;
            }
            if (candidate instanceof ParameterDescriptor) {
                final Class type = ((ParameterDescriptor) candidate).getValueClass();
                if (Coverage.class.isAssignableFrom(type)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Returns an initially empty set of parameters.
     */
    public ParameterValueGroup getParameters() {
        return descriptor.createValue();
    }

    /**
     * Applies a process operation to a coverage. This method is invoked by {@link DefaultProcessor}.
     *
     * @param  parameters List of name value pairs for the parameters required for the operation.
     * @param  hints A set of rendering hints, or {@code null} if none. The {@code DefaultProcessor}
     *         may provides hints for the following keys: {@link Hints#COORDINATE_OPERATION_FACTORY}
     *         and {@link Hints#JAI_INSTANCE}.
     * @return The result as a coverage.
     *
     * @throws CoverageProcessingException if the operation can't be applied.
     */
    public abstract Coverage doOperation(final ParameterValueGroup parameters, final Hints hints)
            throws CoverageProcessingException;

    /**
     * Returns the {@link AbstractProcessor} instance used for an operation. The instance is fetch
     * from the rendering hints given to the {@link #doOperation} method. If no processor is
     * specified, then a default one is returned.
     *
     * @param  hints The rendering hints, or {@code null} if none.
     * @return The {@code AbstractProcessor} instance in use (never {@code null}).
     */
    protected static AbstractProcessor getProcessor(final RenderingHints hints) {
        if (hints != null) {
            final Object value = hints.get(Hints.GRID_COVERAGE_PROCESSOR);
            if (value instanceof AbstractProcessor) {
                return (AbstractProcessor) value;
            }
        }
        return AbstractProcessor.getInstance();
    }

    /**
     * Makes sure that an argument is non-null. This is a convenience method for
     * implementations in subclasses.
     *
     * @param  name   Argument name.
     * @param  object User argument.
     * @throws IllegalArgumentException if {@code object} is null.
     */
    protected static void ensureNonNull(final String name, final Object object)
            throws IllegalArgumentException
    {
        if (object == null) {
            throw new IllegalArgumentException(Errors.format(ErrorKeys.NULL_ARGUMENT_$1, name));
        }
    }

    /**
     * Returns a hash value for this operation. This value need not remain consistent between
     * different implementations of the same class.
     */
    @Override
    public int hashCode() {
        // Since we should have only one operation registered for each name,
        // the descriptors hash code should be enough.
        return descriptor.hashCode() ^ (int)serialVersionUID;
    }

    /**
     * Compares the specified object with this operation for equality.
     */
    @Override
    public boolean equals(final Object object) {
        if (object!=null && object.getClass().equals(getClass())) {
            final AbstractOperation that = (AbstractOperation) object;
            return Utilities.equals(this.descriptor, that.descriptor);
        }
        return false;
    }

    /**
     * Returns a string representation of this operation. The returned string is
     * implementation dependent. It is usually provided for debugging purposes only.
     */
    @Override
    public String toString() {
        return Classes.getShortClassName(this) + '[' + getName() + ']';
    }
}
