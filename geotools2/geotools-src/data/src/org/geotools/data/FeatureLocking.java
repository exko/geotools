/*
 *    Geotools2 - OpenSource mapping toolkit
 *    http://geotools.org
 *    (C) 2002, Geotools Project Managment Committee (PMC)
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
 *
 */
package org.geotools.data;

import org.geotools.filter.Filter;
import java.io.IOException;
import java.util.Set;


/**
 * Provides Feature based locking.
 * 
 * <p>
 * Features from individual shapefiles, database tables, etc. can be protected
 * or reserved from modification through this interface.
 * </p>
 * 
 * <p>
 * This is a prototype DataSource replacement please see FeatureSource form
 * more information
 * </p>
 *
 * @author Jody Garnett
 * @author Ray Gallagher
 * @author Rob Hranac, TOPP
 * @author Chris Holmes, TOPP
 * @version $Id: FeatureLocking.java,v 1.3 2003/11/22 00:51:04 jive Exp $
 */
public interface FeatureLocking extends FeatureStore {
    /**
     * All locking operations will operate against the provided
     * <code>lock</code>.
     * 
     * <p>
     * This in in keeping with the stateful spirit of DataSource in which
     * operations are against the "current" transaction. If a FeatureLock is
     * not provided lock operations will only be applicable for the current
     * transaction (they will expire on the next commit or rollback).
     * </p>
     * 
     * <p>
     * That is lockFeatures() operations will:
     * </p>
     * 
     * <ul>
     * <li>
     * Be recorded against the provided FeatureLock.
     * </li>
     * <li>
     * Be recorded against the current transaction if no FeatureLock is
     * provided.
     * </li>
     * </ul>
     * 
     * <p>
     * Calling this method with <code>setFeatureLock( FeatureLock.TRANSACTION
     * )</code> will revert to per transaction operation.
     * </p>
     * 
     * <p>
     * This design allows for the following:
     * </p>
     * 
     * <ul>
     * <li>
     * cross DataSource FeatureLock usage
     * </li>
     * <li>
     * not having pass in the same FeatureLock object multiple times
     * </li>
     * </ul>
     */
    void setFeatureLock(FeatureLock lock);

    /**
     * FeatureLock features described by Query.
     * 
     * <p>
     * To implement WFS parcial Locking retrieve your features with a query
     * operation first before trying to lock them individually. If you are not
     * into WFS please don't ask what parcial locking is.
     * </p>
     *
     * @param query Query describing the features to lock
     *
     * @return Number of features locked
     *
     * @throws DataSourceException Thrown if anything goes wrong
     */
    int lockFeatures(Query query) throws IOException;

    /**
     * FeatureLock features described by Filter.
     * 
     * <p>
     * To implement WFS parcial Locking retrieve your features with a query
     * operation first before trying to lock them individually. If you are not
     * into WFS please don't ask what parcial locking is.
     * </p>
     *
     * @param filter Filter describing the features to lock
     *
     * @return Number of features locked
     *
     * @throws DataSourceException Thrown if anything goes wrong
     */
    int lockFeatures(Filter filter) throws IOException;

    /**
     * FeatureLock all Features.
     * 
     * <p>
     * The method does not prevent addFeatures() from being used (we could add
     * a lockDataSource() method if this functionality is required.
     * </p>
     *
     * @return Number of Features locked by this opperation
     *
     * @throws DataSourceException
     */
    int lockFeatures() throws IOException;

    /**
     * Unlocks all Features.
     * 
     * <p>
     * Authorization must be provided prior before calling this method.
     * </p>
     * <pre><code>
     * <b>void</b> releaseLock( String lockId, LockingDataSource ds ){
     *    ds.setAuthorization( "LOCK534" );
     *    ds.unLockFeatures(); 
     * }
     * </code></pre>
     *
     * @throws DataSourceException
     */
    void unLockFeatures() throws IOException;

    /**
     * Unlock Features denoted by provided filter.
     * 
     * <p>
     * Authorization must be provided prior before calling this method.
     * </p>
     *
     * @param filter
     *
     * @throws DataSourceException
     */
    void unLockFeatures(Filter filter) throws IOException;

    /**
     * Unlock Features denoted by provided query.
     * 
     * <p>
     * Authorization must be provided prior before calling this method.
     * </p>
     *
     * @param query Specifies fatures to unlock
     *
     * @throws DataSourceException
     */
    void unLockFeatures(Query query) throws IOException;

    /**
     * Used to complete release a lock.
     * 
     * <p>
     * Authorization must be provided prior before calling this method.
     * </p>
     * 
     * <p>
     * All featurs locked with the provided authID will be unlocked.
     * </p>
     * 
     * <p>
     * This method is probably in the wrong spot as it has nothing to do with
     * FeatureSource, consider moving to Transaction?
     * </p>
     *
     * @param authID Idetification of Lock to release
     */
    void releaseLock(String authID) throws IOException;

    /**
     * Used to refresh a lock.
     * 
     * <p>
     * Authorization must be provided prior before calling this method.
     * </p>
     * 
     * <p>
     * All features locked with the provied authID will be locked for
     * additional time (the origional duration request).
     * </p>
     * 
     * <p>
     * This method is probably in the wrong spot as it has nothing to do with
     * FeatureSource, consider moving to Transaction?
     * </p>
     *
     * @param authID Idetification of Lock to refresh
     */
    void refreshLock(String authID) throws IOException;
    
    /**
     * Idea for a response from a high-level lock( Query ) function.
     * 
     * @author jgarnett
     */
    public static class Response {
        String authID;
        Set locked;
        Set notLocked;
        public Response( FeatureLock lock, Set lockedFids, Set notLockedFids ){
            authID = lock.getAuthorization();
            locked = lockedFids;
            notLocked = notLockedFids;
        }
        public String getAuthorizationID(){
            return authID;
        }
        public Set getLockedFids(){
            return locked;
        }
        public Set getNotLockedFids(){
            return notLocked;
        }
    }
}
