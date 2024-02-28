package org.bmedia.Processing;

import java.nio.file.StandardWatchEventKinds;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Generic class for processing various types of media. Implementations of this class should take in files, process them,
 * and add them to the DB
 *
 * @param <T>
 */
abstract public class MediaProcessor<T> extends Thread {

    // Private variables
    private final AtomicBoolean processing = new AtomicBoolean(false);
    private final ArrayList<QueueAction<T>> dataChunk = new ArrayList<>();

    // Main queue used to store pending processing actions
    protected LinkedBlockingQueue<QueueAction<T>> actionQueue;
    protected ProcessingGroup group;
    protected boolean running = true;

    /**
     * Implementation of {@link TimerTask} used to intermittently check the queue and do processing if there are actions
     * in the queue
     */
    private class TimedUpdate extends TimerTask {

        MediaProcessor<T> mediaProcessor;

        /**
         * Main constructor
         *
         * @param mediaProcessor {@link MediaProcessor}. Should just be set to the instance that creates this TimedUpdate
         */
        public TimedUpdate(MediaProcessor<T> mediaProcessor) {
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
     * Main constructor
     *
     * @param group {@link ProcessingGroup} to process for
     */
    protected MediaProcessor(ProcessingGroup group, long processingIntervalSeconds) {
        this.actionQueue = new LinkedBlockingQueue<>();
        this.group = group;
        TimedUpdate timedUpdate = new TimedUpdate(this);
        Timer timer = new Timer();
        timer.schedule(timedUpdate, processingIntervalSeconds * 1000, processingIntervalSeconds * 1000);
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
     * This contains control logic that will monitor the tasking queue of the processor and start processing
     * if there are enough tasks or enough time has passed
     */
    public final synchronized void monitorQueue() {
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
                this.doAtomicProcessing();
                timesInterrupted = 0;
            }
        }
    }

    /**
     * Process images and add them into the DB
     *
     * @param pathStrings List of images to process and add to the DB
     */
    protected abstract void addFilesToDB(ArrayList<T> pathStrings);

    /**
     * Implementation should "delete" files from the DB by setting the paths of any files with one of the provided paths
     * to null. Setting paths to null allows the DB to keep tag and any other metadata in case the file is re-added to
     * the DB (or if it was just moved, etc.)
     * <p>
     * TODO: If I ever get jfif/webp support added, generalize the implementations into here instead of making abstract
     *
     * @param pathStrings List of paths (relative to file share base dir) to remove from the DB
     */
    protected abstract void deleteFilesFromDB(ArrayList<T> pathStrings);

    /**
     * The actual processing is done here. There should only be one instance of this function running at one time
     */
    private void doAtomicProcessing() {
        if (this.getProcessing()) {
            return;
        }
        this.setProcessing(true);
        // do processing
        ArrayList<T> addFiles = new ArrayList<>();
        ArrayList<T> deleteFiles = new ArrayList<>();
        for (QueueAction<T> action : this.getDataChunk()) {
            if (action.getActionType().equals(StandardWatchEventKinds.ENTRY_CREATE.name())) {
                addFiles.add(action.getData());
            } else if (action.getActionType().equals(StandardWatchEventKinds.ENTRY_DELETE.name())) {
                deleteFiles.add(action.getData());
            } else {
                System.out.println("WARNING: Queue action type \"" + action.getActionType() + "\" is not valid " +
                        "or is not implemented. Skipping action");
            }
        }

        System.out.println("INFO: Adding " + addFiles.size() + " files to the database");
        addFilesToDB(addFiles);
        System.out.println("INFO: Deleting " + deleteFiles.size() + " files from the database");
        deleteFilesFromDB(deleteFiles);

        this.clearDataChunk();

        this.setProcessing(false);
    }

    /**
     * Add a processing action to the queue
     *
     * @param data       Media/data to process
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
        this.monitorQueue();
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
