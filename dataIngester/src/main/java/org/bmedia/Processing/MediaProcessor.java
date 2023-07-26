package org.bmedia.Processing;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Generic class for processing various types of media. Implementations of this class should take in files, process them,
 * and add them to the DB
 * @param <T>
 */
abstract public class MediaProcessor<T> extends Thread {

    // Private variables

    // Main queue used to store pending processing actions
    protected LinkedBlockingQueue<QueueAction<T>> actionQueue;
    protected ProcessingGroup group;
    protected boolean running = true;

    /**
     * Main constructor
     * @param group {@link ProcessingGroup} to process for
     */
    protected MediaProcessor(ProcessingGroup group) {
        this.actionQueue = new LinkedBlockingQueue<>();
        this.group = group;
    }

    /**
     * Add a processing action to the queue
     * @param data Media/data to process
     * @param actionType Action (add/delete/etc.) that should be taken with the data
     */
    public void addAction(T data, String actionType) {
        try {
            this.actionQueue.put(new QueueAction<>(data, actionType));
        } catch (InterruptedException e) {
            System.out.println("WARNING: Interrupted while adding data to add queue");
        }
    }

    /**
     * Calling this function should do/start the processing for this instance
     */
    abstract public void processData();

    /**
     * Implementation of {@link Thread}'s 'run' function
     */
    @Override
    public final void run() {
        this.processData();
    }

    /**
     * Implementation of {@link Thread}'s 'interrupt' function
     */
    @Override
    public final void interrupt() {
        this.running = false;
    }

}
