package org.bmedia.Processing;

import java.util.concurrent.LinkedBlockingQueue;

abstract public class MediaProcessor <T>{

    protected LinkedBlockingQueue<T> dataQueue;
    protected ProcessingGroup group;

    protected MediaProcessor(ProcessingGroup group){
        this.dataQueue = new LinkedBlockingQueue<>();
        this.group = group;
    }

    public void addData(T data){
        try {
            this.dataQueue.put(data);
        } catch (InterruptedException e){
            System.out.println("WARNING: Interrupted while adding data to queue");
        }
    }

    abstract public void processData();

}
