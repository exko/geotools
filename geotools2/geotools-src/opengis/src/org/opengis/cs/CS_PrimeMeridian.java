package org.opengis.cs;

// JDK's classes
import java.rmi.RemoteException;


/**
 * A meridian used to take longitude measurements from.
 *
 * @version 1.01
 * @since   1.00
 * @author Martin Daly
 */
public interface CS_PrimeMeridian extends CS_Info
{
    /**
     * Returns the longitude value relative to the Greenwich Meridian.
     * The longitude is expressed in this objects angular units.
     *
     * @throws RemoteException if a remote method call failed.
     */
    double getLongitude() throws RemoteException;

    /**
     * Returns the AngularUnits.
     *
     * @throws RemoteException if a remote method call failed.
     */
    CS_AngularUnit getAngularUnit() throws RemoteException;
}
