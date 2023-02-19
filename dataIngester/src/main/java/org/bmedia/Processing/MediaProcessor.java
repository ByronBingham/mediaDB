package org.bmedia.Processing;

import org.postgresql.core.Tuple;

import java.nio.file.StandardWatchEventKinds;
import java.util.concurrent.LinkedBlockingQueue;

abstract public class MediaProcessor <T> extends Thread {

    protected LinkedBlockingQueue<QueueAction<T>> actionQueue;
    protected ProcessingGroup group;
    protected boolean running = true;

    protected MediaProcessor(ProcessingGroup group){
        this.actionQueue = new LinkedBlockingQueue<>();
        this.group = group;
    }

    public void addAction(T data, String actionType){
        try {
            this.actionQueue.put(new QueueAction<>(data, actionType));
        } catch (InterruptedException e){
            System.out.println("WARNING: Interrupted while adding data to add queue");
        }
    }

    public abstract boolean removeAction(T data);

    abstract public void processData();

    @Override
    public final void run(){
        this.processData();
    }

    @Override
    public final void interrupt(){
        this.running = false;
    }

}
