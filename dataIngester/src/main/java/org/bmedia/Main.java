package org.bmedia;

import org.bmedia.Processing.GroupListener;
import org.bmedia.Processing.ProcessingGroup;

import java.io.IOException;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

    private static ArrayList<ProcessingGroup> processingGroups;
    private static ArrayList<GroupListener> groupListeners;
    private static AtomicBoolean running = new AtomicBoolean(true);

    public static void main(String[] args) {

        // TODO: change to arg val
        processingGroups = ProcessingGroup.createGroupsFromFile("exampleConfig.json");
        for (ProcessingGroup group : processingGroups) {
            try {
                groupListeners.add(new GroupListener(group));
            } catch (IOException e) {
                System.out.println("ERROR: Problem occured setting up group listeners. Exiting...");
                return;
            }
        }

    }

    public static boolean isRunning(){
        return running.get();
    }

    private static void setupProcessors(){
        // start threads to process groups' queues
    }

    /**
     *
     */
    private static void initialDbUpdate(){
        // Get list of paths from DB

        // Compare paths to filesystem

        // Add/Delete entries from DB as necessary
    }

    private static void startGroupListening() {
        while (running.get()) {
            for (GroupListener listener : groupListeners) {

            }
        }
    }
}