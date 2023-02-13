package org.bmedia.Processing;

import org.bmedia.Main;

import java.util.ArrayList;

public class ImageProcessor extends MediaProcessor<String>{

    public ImageProcessor(ProcessingGroup group){
        super(group);
    }

    @Override
    public void processData() {
        int timesInterrupted = 0;

        ArrayList<String> dataChunk = new ArrayList<>();

        while(Main.isRunning()){
            String data = "";
            try {
                data = this.dataQueue.take();
                dataChunk.add(data);
            } catch (InterruptedException e){
                timesInterrupted++;
                if(timesInterrupted > 10){  // TODO: make this a variable
                    System.out.println("ERROR: Processing thread interrupted too many times. Exiting thread");
                    return;
                } else {
                    System.out.println("WARNING: processing thread interrupted");
                    continue;
                }
            }

            // do processing if there's enough data for a chunk or if a certain amount of time has passed
            if(dataChunk.size() >= group.getChunkSize()){   // TODO: implement timer
                // do processing

                timesInterrupted = 0;
            }
        }

    }


}
