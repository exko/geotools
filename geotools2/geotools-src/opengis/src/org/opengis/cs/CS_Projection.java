package org.opengis.cs;

// JDK's classes
import java.rmi.RemoteException;


/**
 * A projection from geographic coordinates to projected coordinates.
 *
 * @version 1.01
 * @since   1.00
 * @author Martin Daly
 */
public interface CS_Projection extends CS_Info
{
    /**
     * Gets number of parameters of the projection.
     *
     * @throws RemoteException if a remote method call failed.
     */
    int getNumParameters() throws RemoteException;

    /**
     * Gets an indexed parameter of the projection.
     *
     * @param index Zero based index of parameter to fetch.
     * @throws RemoteException if a remote method call failed.
     */
    CS_ProjectionParameter getParameter(int index) throws RemoteException;

    /**
     * Gets the projection classification name (e.g. 'Transverse_Mercator').
     *
     * @throws RemoteException if a remote method call failed.
     */
    String getClassName() throws RemoteException;
}
