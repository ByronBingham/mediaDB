package org.bmedia.Processing;

import java.util.ArrayList;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Generic class for processing various types of media. Implementations of this class should take in files, process them,
 * and add them to the DB
 * @param <T>
 */
abstract public class MediaProcessor<T> extends Thread {

    // Private variables
    private AtomicBoolean processing = new AtomicBoolean(false);
    private ArrayList<QueueAction<T>> dataChunk = new ArrayList<>();

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
     * Implementation of {@link TimerTask} used to intermittently check the queue and do processing if there are actions
     * in the queue
     */
    private class TimedUpdate extends TimerTask {

        MediaProcessor mediaProcessor;

        /**
         * Main constructor
         *
         * @param mediaProcessor {@link ImageProcessor}. Should just be set to the instance that creates this TimedUpdate
         */
        public TimedUpdate(MediaProcessor mediaProcessor) {
            this.mediaProcessor = mediaProcessor;
        }

        /**
         * Main timed function
         */
        @Override
        public void run() {
            if (!this.mediaProcessor.getProcessing()) {
                System.out.println("INFO: Timed update called");
                this.mediaProcessor.doAtomicProcessing();
            }
        }
    }

    /**
     * Returns true if this instance is already processing images
     *
     * @return True if already processing
     */
    public boolean getProcessing() {
        return this.processing.get();
    }

    /**
     * Set the processing state of this processor
     *
     * @param processing True if this processor is about to start processing. False if this processor is done processing
     */
    public void setProcessing(boolean processing) {
        this.processing.set(processing);
    }

    /**
     * The actual processing is done here. There should only be one instance of this function running at one time
     */
    public final synchronized void doAtomicProcessing(){
        int timesInterrupted = 0;

        // Should keep running indefinitely until explicitly stopped (interrupted)
        while (this.running) {
            try {
                // Skip if the timed function was called while this processor was already processing
                if (this.getProcessing()) {
                    Thread.sleep(1000); // TODO: make var
                    continue;
                }
                dataChunk.add(this.actionQueue.take());
            } catch (InterruptedException e) {
                timesInterrupted++;
                if (timesInterrupted > 10) {  // TODO: make this a variable
                    System.out.println("ERROR: Processing thread interrupted too many times. Exiting thread");
                    return;
                } else {
                    System.out.println("WARNING: processing thread interrupted");
                    continue;
                }
            }

            // do processing if there's enough data for a chunk or if a certain amount of time has passed
            if (dataChunk.size() >= group.getChunkSize() && !this.getProcessing()) {
                this.atomicProcessingImplementation();
                timesInterrupted = 0;
            }
        }
    }

    /**
     * The child class should put its processing here. This will be called by {@link MediaProcessor}'s
     * {@code doAtomicProcessing()}
     */
    protected abstract void atomicProcessingImplementation();

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
     * Implementation of {@link Thread}'s 'run' function
     */
    @Override
    public final void run() {
        this.doAtomicProcessing();
    }

    /**
     * Implementation of {@link Thread}'s 'interrupt' function
     */
    @Override
    public final void interrupt() {
        this.running = false;
    }

    // Getter's and Setters

    public ArrayList<QueueAction<T>> getDataChunk() {
        return dataChunk;
    }
    public void clearDataChunk() {
        this.dataChunk.clear();
    }
}
