package org.opengis.cs;

// JDK's classes
import java.rmi.RemoteException;


/**
 * A one-dimensional coordinate system suitable for vertical measurements.
 *
 * @version 1.01
 * @since   1.00
 * @author Martin Daly
 */
public interface CS_VerticalCoordinateSystem extends CS_CoordinateSystem
{
    /**
     * Gets the vertical datum, which indicates the measurement method.
     *
     * @throws RemoteException if a remote method call failed.
     */
    CS_VerticalDatum getVerticalDatum() throws RemoteException;

    /**
     * Gets the units used along the vertical axis.
     * The vertical units must be the same as the {@link CS_CoordinateSystem} units.
     *
     * @throws RemoteException if a remote method call failed.
     */
    CS_LinearUnit getVerticalUnit() throws RemoteException;
}
