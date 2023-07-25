package org.bmedia.Processing;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;

/**
 * Class to handle listening for filesystem changes. This class is used to detect new files as they are added/removed
 * from the filesystem
 */
public class GroupListener extends Thread {

    // Private variables

    private ProcessingGroup group;
    private ArrayList<Path> paths = new ArrayList<>();
    private WatchService watchService;
    private boolean running = true;

    /**
     * Main constructor
     *
     * @param group {@link ProcessingGroup} that this instance is listening for
     * @throws IOException
     */
    public GroupListener(ProcessingGroup group) throws IOException {
        this.group = group;

        this.init();
    }

    /**
     * Initializes this {@link GroupListener} and sets up file listeners
     *
     * @throws IOException
     */
    private void init() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();

        for (String pathString : this.group.getSourceDirs()) {
            this.paths.add(Path.of(pathString));
        }

        for (Path path : this.paths) {
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
        }
    }

    /**
     * Handles all new filesystem events
     */
    private void updateFileEvents() {
        WatchKey key;
        try {
            // Loop through all outstanding events
            while ((key = this.watchService.take()) != null) {
                if (!this.running) {
                    break;
                }
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path basePath = (Path) key.watchable();
                    String canonicalFile = null;
                    try {
                        canonicalFile = new File(basePath.resolve((Path) event.context()).toString()).getCanonicalPath();
                        boolean validExtension = false;

                        // Check if file has a valid extension
                        for (String extension : group.getValidExtensions()) {
                            if (canonicalFile.contains(extension)) {
                                validExtension = true;
                            }
                        }
                        if (!validExtension) {
                            System.out.println("INFO: Invalid file type found. File: \"" + canonicalFile + "\"");
                            continue;
                        }
                    } catch (IOException e) {
                        System.out.println("ERROR: Could not get the canonical path for \"" + basePath.resolve((Path) event.context()) + "\"");
                        return;
                    }
                    if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
                        this.group.addImageFile(canonicalFile);
                    }
                }
                key.reset();
            }
        } catch (InterruptedException e) {
            System.out.println("WARNING: Interrupted exception while getting watch service events");
        }
    }

    /**
     * Override of {@code Thread.run()}
     */
    @Override
    public final void run() {
        this.updateFileEvents();
    }

    /**
     * Override of {@code Thread.interrupt()}
     */
    @Override
    public final void interrupt() {
        this.running = false;
    }

}
