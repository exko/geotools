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
package org.geotools.renderer.lite;

import java.awt.Canvas;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;

// J2SE dependencies
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * $Id: ImageLoader.java,v 1.3 2003/07/27 15:29:47 aaime Exp $
 *
 * @author Ian Turton
 */
class ImageLoader implements Runnable {
    /** The logger for the rendering module. */
    private static final Logger LOGGER = Logger.getLogger(
            "org.geotools.rendering");

    /** The images managed by the loader */
    private static Map images = new HashMap();

    /** A canvas used as the image observer on the tracker */
    private static Canvas obs = new Canvas();

    /** Used to track the images loading status */
    private static MediaTracker tracker = new MediaTracker(obs);

    /** Currently loading image */
    private static int imageID = 1;

    /** Location of the loading image */
    private URL location;

    /** Still waiting for the image? */
    private boolean waiting = true;

    /**
     * Add an image to be loaded by the ImageLoader
     *
     * @param location the image location
     * @param interactive if true the methods returns immediatly, otherwise
     *        waits for the image to be loaded
     */
    private void add(URL location, boolean interactive) {
        int imgId = imageID;
        this.location = location;
        LOGGER.finest("adding image, interactive? " + interactive);

        Thread t = new Thread(this);
        t.start();

        if (interactive) {
            LOGGER.finest("fast return");

            return;
        } else {
            waiting = true;

            while (waiting) {
                LOGGER.finest("waiting..." + waiting);

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    LOGGER.warning(e.toString());
                }
            }

            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest(
                    imgId + " complete?: "
                    + (isFlagUp(imgId, tracker.COMPLETE)));
                LOGGER.finest(
                    imgId + " abort?: " + (isFlagUp(imgId, tracker.ABORTED)));
                LOGGER.finest(
                    imgId + " error?: " + (isFlagUp(imgId, tracker.ERRORED)));
                LOGGER.finest(
                    imgId + " loading?: " + (isFlagUp(imgId, tracker.LOADING)));
                LOGGER.finest(imgId + "slow return " + waiting);
            }

            return;
        }
    }

    /**
     * Checks the state of the current tracker against a flag
     *
     * @param id the image id
     * @param flag the flag to be checked
     *
     * @return true if the flag is up
     */
    private boolean isFlagUp(int id, int flag) {
        return (tracker.statusID(id, true) & flag) == flag;
    }

    /**
     * Fetch a buffered image from the loader, if interactive is false then the
     * loader will wait for  the image to be available before returning, used
     * by printers and file output renderers. If interactive is true and the
     * image is ready then return, if image is not ready start loading it  and
     * return null. The renderer is responsible for finding an alternative to
     * use.
     *
     * @param location the url of the image to be fetched
     * @param interactive boolean to signal if the loader should wait for the
     *        image to be ready.
     *
     * @return the buffered image or null
     */
    public BufferedImage get(URL location, boolean interactive) {
        if (images.containsKey(location)) {
            LOGGER.finest("found it");

            return (BufferedImage) images.get(location);
        } else {
            if (!interactive) {
                images.put(location, null);
            }

            LOGGER.finest("adding " + location);
            add(location, interactive);

            return (BufferedImage) images.get(location);
        }
    }

    /**
     * Runs the loading thread
     */
    public void run() {
        int myID = 0;
        Image img = null;

        try {
            img = Toolkit.getDefaultToolkit().createImage(location);
            myID = imageID++;
            tracker.addImage(img, myID);
        } catch (Exception e) {
            LOGGER.warning(
                "Exception fetching image from " + location + "\n" + e);
            images.remove(location);
            waiting = false;

            return;
        }

        try {
            while ((tracker.statusID(myID, true) & tracker.LOADING) != 0) {
                tracker.waitForID(myID, 500);
                LOGGER.finest(myID + "loading - waiting....");
            }
        } catch (InterruptedException ie) {
            LOGGER.warning(ie.toString());
        }

        int state = tracker.statusID(myID, true);

        if (state == tracker.ERRORED) {
            LOGGER.finer("" + myID + " Error loading");
            images.remove(location);
            waiting = false;

            return;
        }

        if ((state & tracker.COMPLETE) == tracker.COMPLETE) {
            LOGGER.finest("" + myID + "completed load");

            int iw = img.getWidth(obs);
            int ih = img.getHeight(obs);
            BufferedImage bi = new BufferedImage(
                    iw, ih, BufferedImage.TYPE_INT_ARGB);
            Graphics2D big = bi.createGraphics();
            big.drawImage(img, 0, 0, obs);
            images.put(location, bi);

            waiting = false;

            return;
        }

        LOGGER.finer("" + myID + " whoops - some other outcome " + state);
        waiting = false;

        return;
    }
}
