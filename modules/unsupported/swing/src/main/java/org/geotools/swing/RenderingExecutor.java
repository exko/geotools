/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
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
 */

package org.geotools.swing;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.RenderListener;
import org.opengis.feature.simple.SimpleFeature;

/**
 * This class is used by {@code JMapPane} to handle the scheduling and running of
 * rendering tasks on a background thread. It functions as a single thread, non-
 * queueing executor, ie. only one rendering task can run at any given time and,
 * while it is running, any other submitted tasks will be rejected.
 * <p>
 * Whether a rendering task is accepted or rejected can be tested on submission:
 * <pre><code>
 *     ReferencedEnvelope areaToDraw = ...
 *     Graphics2D graphicsToDrawInto = ...
 *     boolean accepted = renderingExecutor.submit(areaToDraw, graphicsToDrawInto);
 * </code></pre>
 *
 * The status of the executor can also be checked at any time like this:
 * <pre><code>
 *     boolean busy = renderingExecutor.isRunning();
 * </code></pre>
 *
 * While a rendering task is running it is regularly polled to see if it has completed
 * and, if so, whether it finished normally, was cancelled or failed. The interval between
 * polling can be adjusted which might be useful to tune the executor for particular
 * applications:
 * <pre><code>
 *     RenderingExecutor re = new RenderingExecutor( mapPane );
 *     re.setPollingInterval( 150 );  // 150 milliseconds
 * </code></pre>
 *
 * @author Michael Bedward
 * @since 2.7
 * @source $URL$
 * @version $Id$
 *
 * @see JMapPane
 */
public class RenderingExecutor {

    private final JMapPane mapPane;
    private final ExecutorService taskExecutor;
    private final ScheduledExecutorService watchExecutor;

    /** The default interval (milliseconds) for polling the result of a rendering task */
    public static final long DEFAULT_POLLING_INTERVAL = 200L;

    private long pollingInterval;

    /**
     * Constants to indicate the result of a rendering task
     */
    private enum TaskResult {
        PENDING,
        COMPLETED,
        CANCELLED,
        FAILED;
    }

    /**
     * A rendering task
     */
    private class Task implements Callable<TaskResult>, RenderListener {

        private final ReferencedEnvelope envelope;
        private final Rectangle paintArea;
        private final Graphics2D graphics;
        private boolean cancelled;
        private boolean failed;

        public Task(final ReferencedEnvelope envelope, final Rectangle paintArea, final Graphics2D graphics) {
            this.envelope = envelope;
            this.paintArea = paintArea;
            this.graphics = graphics;
            cancelled = false;
            failed = false;
        }

        public TaskResult call() throws Exception {
            GTRenderer renderer = mapPane.getRenderer();
            renderer.addRenderListener(this);

            renderer.paint(graphics, mapPane.getVisibleRect(), envelope, mapPane.getWorldToScreenTransform());

            renderer.removeRenderListener(this);

            if (cancelled) {
                return TaskResult.CANCELLED;
            } else if (failed) {
                return TaskResult.FAILED;
            } else {
                return TaskResult.COMPLETED;
            }
        }

        public synchronized void cancel() {
            if (isRunning()) {
                cancelled = true;
                mapPane.getRenderer().stopRendering();
            }
        }

        public void featureRenderer(SimpleFeature feature) {
            // @todo update a progress listener
            }

        public void errorOccurred(Exception e) {
            failed = true;
        }
    }

    private AtomicBoolean taskRunning;
    private Task task;
    private Future<TaskResult> taskResult;
    private ScheduledFuture<?> watcher;

    /**
     * Constructor
     *
     * @param mapPane the map pane to be serviced by this rendering executor
     */
    public RenderingExecutor(final JMapPane mapPane) {
        taskRunning = new AtomicBoolean(false);
        this.mapPane = mapPane;
        taskExecutor = Executors.newSingleThreadExecutor();
        watchExecutor = Executors.newSingleThreadScheduledExecutor();
        pollingInterval = DEFAULT_POLLING_INTERVAL;
    }

    /**
     * Get the interval for polling the result of a rendering task
     *
     * @return polling interval in milliseconds
     */
    public long getPollingInterval() {
        return pollingInterval;
    }

    /**
     * Set the interval for polling the result of a rendering task
     *
     * @param interval interval in milliseconds (values {@code <=} 0 are ignored)
     */
    public void setPollingInterval(long interval) {
        if (interval > 0) {
            pollingInterval = interval;
        }
    }

    /**
     * Submit a new rendering task. If no rendering task is presently running
     * this new task will be accepted; otherwise it will be rejected (ie. there
     * is no task queue).
     *
     * @param envelope the map area (world coordinates) to be rendered
     * @param graphics the graphics object to draw on
     *
     * @return true if the rendering task was accepted; false if it was
     *         rejected
     */
    public synchronized boolean submit(ReferencedEnvelope envelope, Rectangle paintArea, Graphics2D graphics) {
        if (!isRunning()) {
            task = new Task(envelope, paintArea, graphics);
            taskRunning.set(true);
            taskResult = taskExecutor.submit(task);
            watcher = watchExecutor.scheduleAtFixedRate(new Runnable() {

                public void run() {
                    pollTaskResult();
                }
            }, DEFAULT_POLLING_INTERVAL, DEFAULT_POLLING_INTERVAL, TimeUnit.MILLISECONDS);

            return true;
        }

        return false;
    }

    /**
     * Cancel the current rendering task if one is running
     */
    public void cancelTask() {
        if (isRunning()) {
            task.cancel();
        }
    }

    private void pollTaskResult() {
        if (!taskResult.isDone()) {
            return;
        }

        TaskResult result = TaskResult.PENDING;

        try {
            result = taskResult.get();
        } catch (Exception ex) {
            throw new IllegalStateException("When getting rendering result", ex);
        }

        watcher.cancel(false);
        taskRunning.set(false);

        switch (result) {
            case CANCELLED:
                mapPane.onRenderingCancelled();
                break;

            case COMPLETED:
                mapPane.onRenderingCompleted();
                break;

            case FAILED:
                mapPane.onRenderingFailed();
                break;
        }
    }

    public synchronized boolean isRunning() {
        return taskRunning.get();
    }

    @Override
    protected void finalize() throws Throwable {
        if (this.isRunning()) {
            taskExecutor.shutdownNow();
            watchExecutor.shutdownNow();
        }
    }
}
