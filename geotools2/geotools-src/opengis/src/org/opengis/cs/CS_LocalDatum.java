package org.opengis.cs;


/**
 * Local datum.
 * If two local datum objects have the same datum type and name, then they
 * can be considered equal.  This means that coordinates can be transformed
 * between two different local coordinate systems, as long as they are based
 * on the same local datum.
 *
 * @version 1.01
 * @since   1.00
 * @author Martin Daly
 */
public interface CS_LocalDatum extends CS_Datum
{
}
