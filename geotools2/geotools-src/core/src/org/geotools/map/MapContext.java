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
package org.geotools.map;

import com.vividsolutions.jts.geom.Envelope;
import org.geotools.data.FeatureResults;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.map.event.BoundingBoxListener;
import org.geotools.map.event.LayerListListener;
import org.geotools.map.event.MapBoundsListener;
import org.geotools.map.event.MapLayerListListener;
import org.geotools.styling.Style;
import org.opengis.sc.CoordinateReferenceSystem;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.Iterator;


/**
 * Store context information about a map display.  This object is based on the OGC Web Map Context
 * Specification.
 *
 * @author Cameron Shorter
 * @version $Id: MapContext.java,v 1.1 2003/12/04 23:20:33 aaime Exp $
 */
public interface MapContext {
    /**
     * Add a new layer if not already present and trigger a {@link LayerListEvent}.
     *
     * @param layer the layer to be inserted
     *
     * @return true if the layer has been added, false otherwise
     */
    boolean addLayer(MapLayer layer);

    /**
     * Add a new layer in the specified position and trigger a {@link LayerListEvent}. Layer won't
     * be added if it's already in the list.
     *
     * @param index index at which the layer will be inserted
     * @param layer the layer to be inserted
     *
     * @return true if the layer has been added, false otherwise
     */
    boolean addLayer(int index, MapLayer layer);

    /**
     * Add a new layer and trigger a {@link LayerListEvent}.
     *
     * @param layer Then new layer that has been added.
     */
    void addLayer(FeatureSource featureSource, Style style);

    /**
     * Add a new layer and trigger a {@link LayerListEvent}.
     *
     * @param layer Then new layer that has been added.
     */
    // void addLayer(FeatureCollection collection, Style style);

    /**
     * Remove a layer, if present, and trigger a {@link LayerListEvent}.
     *
     * @param MapLayer
     *
     * @return true if the layer has been removed
     */
    boolean removeLayer(MapLayer layer);

    /**
     * Remove a layer and trigger a {@link LayerListEvent}.
     *
     * @param index The index of the layer that it's going to be removed
     *
     * @return the layer removed, if any
     */
    MapLayer removeLayer(int index);

    /**
     * Add an array of new layers and trigger a {@link LayerListEvent}.
     *
     * @param layers The new layers that are to be added.
     *
     * @return the number of layers actually added to the MapContext
     */
    int addLayers(MapLayer[] layers);

    /**
     * Remove an array of layers and trigger a {@link LayerListEvent}.
     *
     * @param layers The layers that are to be removed.
     */
    void removeLayers(MapLayer[] layers);

    /**
     * Clears the whole layer list. Will fire a LayerListChangedEvent
     */
    void clearLayerList();

    /**
     * Return this model's list of layers.  If no layers are present, then an empty array is
     * returned.
     *
     * @return This model's list of layers.
     */
    MapLayer[] getLayers();

    /**
     * Return the requested layer.
     *
     * @param index index of layer to return.
     *
     * @return the layer at the specified position
     *
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    MapLayer getLayer(int index) throws IndexOutOfBoundsException;

    /**
     * Moves a layer from a position to another. Will fire a MapLayerListEvent
     *
     * @param sourcePosition the layer current position
     * @param destPosition the layer new position
     */
    void moveLayer(int sourcePosition, int destPosition);

    /**
     * Returns an iterator over the layers in this context in proper sequence.
     *
     * @return an iterator over the layers in this context in proper sequence.
     */
    Iterator iterator();

    /**
     * Returns the index of the first occurrence of the specified layer,  or -1 if this list does
     * not contain this element.
     *
     * @param the layer to search for
     *
     * @return DOCUMENT ME!
     */
    int indexOf(MapLayer layer);

    /**
     * Returns the number of layers in this map context
     *
     * @return the number of layers in this map context
     */
    int getLayerCount();

    /**
     * Get the bounding box of all the layers in this MapContext. If all the layers cannot
     * determine the bounding box in the speed required for each layer, then null is returned. The
     * bounds will be expressed in the MapContext coordinate system.
     *
     * @return The bounding box of the features or null if unknown and too expensive for the method
     *         to calculate.
     *
     * @throws IOException if an IOException occurs while accessing the FeatureSource bounds
     */
    Envelope getLayerBounds() throws IOException;

    /**
     * Register interest in receiving a {@link LayerListEvent}.  A <code>LayerListEvent</code> is
     * sent if a layer is added or removed, but not if the data within a layer changes.
     *
     * @param listener The object to notify when Layers have changed.
     */
    void addMapLayerListListener(MapLayerListListener listener);

    /**
     * Remove interest in receiving {@link LayerListEvent}.
     *
     * @param listener The object to stop sending <code>LayerListEvent</code>s.
     */
    void removeMapLayerListListener(MapLayerListListener listener);

    /**
     * Set a new area of interest and trigger a {@link BoundingBoxEvent}. Note that this is the
     * only method to change coordinate system.  A <code>setCoordinateReferenceSystem</code>
     * method is not provided to ensure this class is not dependant on transform classes.
     *
     * @param areaOfInterest The new areaOfInterest.
     * @param CoordinateReferenceSystem The coordinate system being using by this model.
     *
     * @throws IllegalArgumentException if an argument is <code>null</code>.
     */
    void setAreaOfInterest(Envelope areaOfInterest,
        CoordinateReferenceSystem coordinateReferenceSystem)
        throws IllegalArgumentException;

    /**
     * Set a new area of interest and trigger an {@link BoundingBoxEvent}.
     *
     * @param areaOfInterest The new area of interest.
     *
     * @throws IllegalArgumentException if an argument is <code>null</code>.
     */
    void setAreaOfInterest(Envelope areaOfInterest);

    /**
     * Gets the current area of interest.
     *
     * @return Current area of interest
     */
    Envelope getAreaOfInterest();

    /**
     * Get the current coordinate system.
     *
     * @return the coordinate system of this box.
     */
    CoordinateReferenceSystem getCoordinateReferenceSystem();

    /**
     * Transform the coordinates according to the provided transform. Useful for zooming and
     * panning processes.
     *
     * @param transform The transform to change area of interest.
     */
    void transform(AffineTransform transform);

    /**
     * Register interest in receiving {@link MapBoundsEvent}s.
     *
     * @param listener The object to notify when the area of interest has changed.
     */
    void addMapBoundsListener(MapBoundsListener listener);

    /**
     * Remove interest in receiving a {@link BoundingBoxEvent}s.
     *
     * @param listener The object to stop sending change events.
     */
    void removeMapBoundsListener(MapBoundsListener listener);

    /**
     * Get the abstract which describes this interface, returns an empty string if this has not
     * been set yet.
     *
     * @return The Abstract.
     */
    String getAbstract();

    /**
     * Set an abstract which describes this context.
     *
     * @param conAbstract the Abstract.
     */
    void setAbstract(final String conAbstract);

    /**
     * Get the contact information associated with this context, returns an empty string if
     * contactInformation has not been set.
     *
     * @return the ContactInformation.
     */
    String getContactInformation();

    /**
     * Set contact inforation associated with this class.
     *
     * @param contactInformation the ContactInformation.
     */
    void setContactInformation(final String contactInformation);

    /**
     * Get an array of keywords associated with this context, returns an empty array if no keywords
     * have been set. The array returned is a copy, changes to the returned array won't influence
     * the MapContextState
     *
     * @return array of keywords
     */
    String[] getKeywords();

    /**
     * Set an array of keywords to associate with this context.
     *
     * @param keywords the Keywords.
     */
    void setKeywords(final String[] keywords);

    /**
     * Get the title, returns an empty string if it has not been set yet.
     *
     * @return the title, or an empty string if it has not been set.
     */
    String getTitle();

    /**
     * Set the title of this context.
     *
     * @param title the title.
     */
    void setTitle(final String title);

    /**
     * Registers PropertyChangeListener to receive events.
     *
     * @param listener The listener to register.
     */
    public void addPropertyChangeListener(java.beans.PropertyChangeListener listener);

    /**
     * Removes PropertyChangeListener from the list of listeners.
     *
     * @param listener The listener to remove.
     */
    public void removePropertyChangeListener(java.beans.PropertyChangeListener listener);
}
