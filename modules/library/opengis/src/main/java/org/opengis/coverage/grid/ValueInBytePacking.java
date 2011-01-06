/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2011, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2005 Open Geospatial Consortium Inc.
 *    
 *    All Rights Reserved. http://www.opengis.org/legal/
 */
package org.opengis.coverage.grid;

import java.util.List;
import java.util.ArrayList;

import org.opengis.util.CodeList;
import org.opengis.coverage.SampleDimensionType;
import org.opengis.annotation.UML;

import static org.opengis.annotation.Obligation.*;
import static org.opengis.annotation.Specification.*;


/**
 * Order of values packed in a byte for sample dimensions with less than 8 bits.
 * This include
 * {@link SampleDimensionType#UNSIGNED_1BIT UNSIGNED_1BIT},
 * {@link SampleDimensionType#UNSIGNED_2BITS UNSIGNED_2BITS} and
 * {@link SampleDimensionType#UNSIGNED_4BITS UNSIGNED_4BITS} data types.
 *
 * <P>&nbsp;</P>
 * <TABLE WIDTH="80%" ALIGN="center" CELLPADDING="18" BORDER="4" BGCOLOR="#FFE0B0">
 *   <TR><TD>
 *     <P align="justify"><STRONG>WARNING: THIS CLASS WILL CHANGE.</STRONG> Current API is derived from OGC
 *     <A HREF="http://www.opengis.org/docs/01-004.pdf">Grid Coverages Implementation specification 1.0</A>.
 *     We plan to replace it by new interfaces derived from ISO 19123 (<CITE>Schema for coverage geometry
 *     and functions</CITE>). Current interfaces should be considered as legacy and are included in this
 *     distribution only because they were part of GeoAPI 1.0 release. We will try to preserve as much
 *     compatibility as possible, but no migration plan has been determined yet.</P>
 *   </TD></TR>
 * </TABLE>
 *
 * @version <A HREF="http://www.opengis.org/docs/01-004.pdf">Grid Coverage specification 1.0</A>
 * @author  Martin Desruisseaux (IRD)
 * @since   GeoAPI 1.0
 *
 * @see GridPacking
 * @see ByteInValuePacking
 *
 * @deprecated In favor of migrating to ISO 19123 definition for Coverage.
 */
@UML(identifier="GC_ValueInBytePacking", specification=OGC_01004)
public final class ValueInBytePacking extends CodeList<ValueInBytePacking> {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 6895036289489868770L;

    /**
     * List of all enumerations of this type.
     * Must be declared before any enum declaration.
     */
    private static final List<ValueInBytePacking> VALUES = new ArrayList<ValueInBytePacking>(2);

    /**
     * Low bit firts (little endian order).
     */
    @UML(identifier="GC_LoBitFirst", obligation=CONDITIONAL, specification=OGC_01004)
    public static final ValueInBytePacking LO_BIT_FIRST = new ValueInBytePacking("LO_BIT_FIRST");

    /**
     * High bit first (big endian order).
     */
    @UML(identifier="GC_HiBitFirst", obligation=CONDITIONAL, specification=OGC_01004)
    public static final ValueInBytePacking HI_BIT_FIRST = new ValueInBytePacking("HI_BIT_FIRST");

    /**
     * Constructs an enum with the given name. The new enum is
     * automatically added to the list returned by {@link #values}.
     *
     * @param name The enum name. This name must not be in use by an other enum of this type.
     */
    private ValueInBytePacking(final String name) {
        super(name, VALUES);
    }

    /**
     * Returns the list of {@code ValueInBytePacking}s.
     *
     * @return The list of codes declared in the current JVM.
     */
    public static ValueInBytePacking[] values() {
        synchronized (VALUES) {
            return VALUES.toArray(new ValueInBytePacking[VALUES.size()]);
        }
    }

    /**
     * Returns the list of enumerations of the same kind than this enum.
     */
    public ValueInBytePacking[] family() {
        return values();
    }

    /**
     * Returns the value in byte packing that matches the given string, or returns a
     * new one if none match it.
     *
     * @param code The name of the code to fetch or to create.
     * @return A code matching the given name.
     */
    public static ValueInBytePacking valueOf(String code) {
        return valueOf(ValueInBytePacking.class, code);
    }
}
