package org.bmedia.Processing;


import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;

public class GroupListener extends Thread {

    private ProcessingGroup group;
    private MediaProcessor mediaProcessor;
    private ArrayList<Path> paths = new ArrayList<>();
    private WatchService watchService;
    private int secondsBetweenUpdates;
    private boolean running = true;

    public GroupListener(ProcessingGroup group, MediaProcessor mediaProcessor, int secondsBetweenUpdates) throws IOException {
        this.group = group;
        this.mediaProcessor = mediaProcessor;
        this.secondsBetweenUpdates = secondsBetweenUpdates;

        this.init();
    }

    private void init() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();

        for (String pathString : this.group.getSourceDirs()) {
            this.paths.add(Path.of(pathString));
        }

        for (Path path : this.paths) {
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
        }
    }

    public WatchService getWatchService() {
        return this.watchService;
    }

    private void updateFileEvents() {
        WatchKey key;
        try {
            while ((key = this.watchService.take()) != null) {
                if (!this.running) {
                    break;
                }
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path basePath = (Path)key.watchable();
                    String canonicalFile = null;
                    try{
                        canonicalFile = new File(basePath.resolve((Path) event.context()).toString()).getCanonicalPath();
                        if(group.isJfifWebmToJpg() &&
                                (FilenameUtils.getExtension(canonicalFile).equals("webp") || FilenameUtils.getExtension(canonicalFile).equals("jfif"))){
                            continue;
                        }
                    } catch (IOException e){
                        System.out.println("ERROR: Could not get the canonical path for \"" + basePath.resolve((Path) event.context()) + "\"");
                        return;
                    }
                    if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
                        this.mediaProcessor.addAction(canonicalFile, StandardWatchEventKinds.ENTRY_CREATE.name());
                    } else if (kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {
                        if(this.mediaProcessor.removeAction(canonicalFile)){
                            continue;
                        }
                        this.mediaProcessor.addAction(canonicalFile, StandardWatchEventKinds.ENTRY_DELETE.name());
                    }
                }
                key.reset();
            }
        } catch (InterruptedException e) {
            System.out.println("WARNING: Interrupted exception while getting watch service events");
        }
    }

    @Override
    public final void run() {
        this.updateFileEvents();
    }

    @Override
    public final void interrupt() {
        this.running = false;
    }

}
