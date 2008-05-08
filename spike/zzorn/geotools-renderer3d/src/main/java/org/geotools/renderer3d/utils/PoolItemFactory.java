package org.geotools.renderer3d.utils;

/**
 * A simple factory that can create items of type T.  Used by the Pool class.
 *
 * @author Hans H�ggstr�m
 */
public interface PoolItemFactory<T>
{
    /**
     * @return a new instance of T.
     */
    T create();
}
