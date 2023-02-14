package org.bmedia.Processing;

import org.apache.commons.lang3.time.StopWatch;
import org.bmedia.Main;

import java.nio.file.StandardWatchEventKinds;
import java.util.ArrayList;

public class ImageProcessor extends MediaProcessor<String> {

    public ImageProcessor(ProcessingGroup group) {
        super(group);
    }

    @Override
    public void processData() {
        int timesInterrupted = 0;
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        ArrayList<QueueAction<String>> dataChunk = new ArrayList<>();

        while (Main.isRunning()) {
            try {
                QueueAction<String> action = this.actionQueue.take();
                dataChunk.add(action);
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
            if (dataChunk.size() >= group.getChunkSize() || stopWatch.getTime() >= 30000) {   // TODO: make var
                // do processing
                for (QueueAction<String> action : dataChunk) {
                    if (action.getActionType().equals(StandardWatchEventKinds.ENTRY_CREATE.name())) {
                        addImageToDB(action.getData());
                    } else if (action.getActionType().equals(StandardWatchEventKinds.ENTRY_DELETE.name())) {
                        deleteImageFromDB(action.getData());
                    } else {
                        System.out.println("WARNING: Queue action type \"" + action.getActionType() + "\" is not valid " +
                                "or is not implemented. Skipping action");
                    }

                }

                timesInterrupted = 0;
                stopWatch.reset();
            }
        }
    }

    private void addImageToDB(String pathString) {

    }

    private void deleteImageFromDB(String pathString) {

    }


}
