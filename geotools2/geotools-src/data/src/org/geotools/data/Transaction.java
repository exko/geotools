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

import java.io.IOException;
import java.util.Set;


/**
 * The controller for Transaction with FeatureStore.
 * 
 * <p>
 * Shapefiles, databases, etc. are safely modified with the assistance of this
 * interface. Transactions are also to provide authorization when working with
 * locked features.
 * </p>
 * 
 * <p>
 * All opperations are considered to be working against a Transaction.
 * Transaction.AUTO_COMMIT is used to represent an immidiate mode where
 * requests are immidately commited.
 * </p>
 * 
 * <p>
 * For more information please see DataStore and FeatureStore.
 * </p>
 * 
 * <p>
 * Example Use:
 * </p>
 * <pre><code>
 * Transaction t = new DefaultTransaction();
 * try {
 *     FeatureStore road = (FeatureStore) store.getFeatureSource("road");
 *     FeatureStore river = (FeatureStore) store.getFeatureSource("river");
 * 
 *     road.setTransaction( t );
 *     river.setTransaction( t );
 * 
 *     t.addAuthorization( lockID );  // proivde authoriztion
 *     road.removeFeatures( filter ); // opperate against transaction
 *     river.removeFeature( filter ); // opperate against transaction
 * 
 *     t.commit(); // commit opperations
 * }
 * catch (IOException io){
 *     t.rollback(); // cancel opperations
 * }
 * finally {
 *     t.close(); // free resources
 * }
 * </code></pre>
 *
 * @author Jody Garnett
 * @author Chris Holmes, TOPP
 * @version $Id: Transaction.java,v 1.3 2003/11/16 00:56:16 jive Exp $
 */
public interface Transaction {
    /** Represents AUTO_COMMIT Mode */
    static final Transaction AUTO_COMMIT = new AutoCommitTransaction();

    //
    // External State
    //

    /**
     * List of Authorizations IDs held by this transaction.
     * 
     * <p>
     * This list is reset by the next call to commit() or rollback().
     * </p>
     * 
     * <p>
     * Authorization IDs are used to provide FeatureLock support.
     * </p>
     *
     * @return List of Authorization IDs
     */
    Set getAuthorizations();

    /**
     * Allows FeatureSource to squirel away information( and callbacks ) for
     * later.
     * 
     * <p>
     * The most common example is a JDBC DataStore saving the required
     * connection for later opperations.
     * </p>
     * <pre><code>
     * ConnectionState implements State {
     *     public Connection conn;
     *     public addAuthorization() {}
     *     public commit(){ conn.commit(); }
     *     public rollback(){ conn.rollback(); }
     * }
     * </code></pre>
     * 
     * <p>
     * putState will call State.setTransaction( transaction ) to allow State a
     * chance to configure itself.
     * </p>
     *
     * @param key Key used to externalize State
     * @param state Externalized State
     */
    void putState(Object key, State state);

    /**
     * Allows FeatureSources to clean up information ( and callbacks ) they
     * earlier provided.
     * 
     * <p>
     * Care should be taken when using shared State to not remove State
     * required by another FeatureSources.
     * </p>
     * 
     * <p>
     * removeState will call State.setTransaction( null ) to allow State a
     * chance cleanup after itself.
     * </p>
     *
     * @param key Key that was used to externalize State
     */
    void removeState(Object key);

    /**
     * Allows DataStores to squirel away information( and callbacks ) for
     * later.
     * 
     * <p>
     * The most common example is a JDBC DataStore saving the required
     * connection for later opperations.
     * </p>
     *
     * @return Current State externalized by key, or <code>null</code> if not
     *         found
     */
    State getState(Object key);

    //
    // Flow Control
    //    

    /**
     * Makes all transactions made since the previous commit/rollback
     * permanent.
     * 
     * <p>
     * FeatureSources will need to issue any changes notifications using a
     * FeatureEvent.FEATURES_CHANGED to all FeatureSources with the same
     * typeName and a different Transaction. FeatureSources with the same
     * Transaction will of been notified of changes as the FeaureWriter made
     * them.
     * </p>
     *
     * @throws DataSourceException if there are any datasource errors.
     *
     * @see #setAutoCommit(boolean)
     */
    void commit() throws IOException;

    /**
     * Undoes all transactions made since the last commit or rollback.
     * 
     * <p>
     * FeatureSources will need to issue any changes notifications using a
     * FeatureEvent.FEATURES_CHANGED. This will need to be issued to all
     * FeatureSources with the same typeName and Transaction.
     * </p>
     *
     * @throws DataSourceException if there are problems with the datasource.
     * @throws UnsupportedOperationException if the rollback method is not
     *         supported by this datasource.
     *
     * @see #setAutoCommit(boolean)
     */
    void rollback() throws IOException;

    //
    // Locking Support
    // 

    /**
     * Provides an Authorization ID for this Transaction.
     * 
     * <p>
     * All proceeding modifyFeatures,removeFeature, unLockFeatures, refreshLock
     * and ReleaseLock operations will make use of the provided authorization.
     * </p>
     * 
     * <p>
     * Authorization is only maintained until the this Transaction is commited
     * or rolledback.
     * </p>
     * 
     * <p>
     * That is operations will only succeed if affected features either:
     * </p>
     * 
     * <ul>
     * <li>
     * not locked
     * </li>
     * <li>
     * locked with the provided authID
     * </li>
     * </ul>
     * 
     * <p>
     * Authorization ID is provided as a String, rather than a FeatureLock, to
     * account for across process lock use.
     * </p>
     *
     * @param authID
     */
    void addAuthorization(String authID) throws IOException;

    /**
     * Provides an oppertunity for a Transaction to free an State it maintains.
     * <p>
     * This method should call State.setTransaction( null ) on all State it
     * maintains.
     * </p>
     * <p>
     * It is hoped that FeatureStore implementations that have externalized
     * their State with the transaction take the oppertunity to revert to
     * Transction.AUTO_COMMIT.
     * </p>
     * @throws IOException
     */
    void close() throws IOException;
    
    /**
     * DataStore implementations can use this interface to externalize the
     * state they require to implement Transaction Support.
     * 
     * <p>
     * The commit and rollback methods will be called as required. The
     * intension is that several DataStores can share common transaction state
     * (example: Postgis DataStores sharing a connection to the same
     * database).
     * </p>
     *
     * @author jgarnett, Refractions Reasearch Inc.
     * @version CVS Version
     *
     * @see org.geotools.data
     */
    static public interface State {
        /**
         * Provides configuration information for Transaction.State
         * 
         * <p>
         * setTransaction is called with non null <code>transaction</code> when
         * Transaction.State is <code>putState</code> into a Transaction. This
         * tranasction will be used to determine correct event notification.
         * </p>
         * 
         * <p>
         * setTransaction is called with <code>null</code> when removeState is
         * called (usually during Transaction.close() ).
         * </p>
         *
         * @param transaction
         */
        void setTransaction(Transaction transaction);

        /**
         * Call back used for Transaction.setAuthorization()
         */
        void addAuthorization(String AuthID) throws IOException;

        /**
         * Call back used for Transaction.commit()
         */
        void commit() throws IOException;

        /**
         * Call back used for Transaction.rollback()
         */
        void rollback() throws IOException;
    }
}


/**
 * This is used to represent the absense of a Transaction and the use of
 * AutoCommit.
 * 
 * <p>
 * This class serves as the implementation of the constant Transaction.NONE.
 * </p>
 *
 * @author jgarnett
 */
class AutoCommitTransaction implements Transaction {
    /**
     * Authorization IDs are not stored by AutoCommit.
     * 
     * <p>
     * Authorization IDs are only stored for the duration of a Transaction.
     * </p>
     *
     * @return Set of authorizations
     *
     * @throws UnsupportedOperationException AUTO_COMMIT does not support this
     */
    public Set getAuthorizations() {
        throw new UnsupportedOperationException(
            "Authorization IDs are not valid for AutoCommit Transaction");
    }

    /**
     * AutoCommit does not save State.
     * 
     * <p>
     * While symetry would be good, state should be commited not stored for
     * later.
     * </p>
     *
     * @param key Key that is not used to Store State
     * @param state State we are not going to externalize
     *
     * @throws UnsupportedOperationException AutoCommit does not support State
     */
    public void putState(Object key, State state) {
        throw new UnsupportedOperationException(
            "AutoCommit does not support the putState opperations");
    }

    /**
     * AutoCommit does not save State.
     * 
     * <p>
     * While symetry would be good, state should be commited not stored for
     * later.
     * </p>
     *
     * @param key Key that is not used to Store State
     *
     * @throws UnsupportedOperationException AutoCommit does not support State
     */
    public void removeState(Object key) {
        throw new UnsupportedOperationException(
            "AutoCommit does not support the removeState opperations");
    }

    /**
     * I am not sure should AutoCommit be able to save sate?
     * 
     * <p>
     * While symetry would be good, state should be commited not stored for
     * later.
     * </p>
     *
     * @param key Key used to retrieve State
     *
     * @return State earlier provided with putState
     *
     * @throws UnsupportedOperationException As Autocommit does not support
     *         State
     */
    public State getState(Object key) {
        throw new UnsupportedOperationException(
            "AutoCommit does not support the getState opperations");
    }

    /**
     * Implemented as a NOP since this Transaction always commits.
     * 
     * <p>
     * This allows the following workflow:
     * </p>
     * <pre>
     * <code>
     * Transaction t = roads.getTransaction();
     * try{
     *     roads.addFeatures( features );
     *     roads.getTransaction().commit();
     * }
     * catch( IOException erp ){
     *     //something went wrong;
     *     roads.getTransaction().rollback();
     * }
     * </code>
     * </pre>
     *
     * @throws IOException If commit fails
     */
    public void commit() throws IOException {
        // implement a NOP
    }

    /**
     * Implements a NOP since AUTO_COMMIT does not maintain State.
     */
    public void close(){
        // no state to clean up after
    }
    /**
     * Auto commit mode cannot support the rollback opperation.
     *
     * @throws IOException if Rollback fails
     */
    public void rollback() throws IOException {
        throw new IOException(
            "AutoCommit cannot support the rollback opperation");
    }

    /**
     * Authorization IDs are not stored by AutoCommit.
     * 
     * <p>
     * Authorization IDs are only stored for the duration of a Transaction.
     * </p>
     *
     * @param authID Authorization ID
     *
     * @throws IOException If set authorization fails
     */
    public void addAuthorization(String authID) throws IOException {
        throw new IOException(
            "Authorization IDs are not valid for AutoCommit Transaction");
    }
}
