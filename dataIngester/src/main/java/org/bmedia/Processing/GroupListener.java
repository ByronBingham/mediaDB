package org.bmedia.Processing;

import org.bmedia.Processing.ProcessingGroup;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;

public class GroupListener {

    private ProcessingGroup group;
    private ArrayList<Path> paths = new ArrayList<>();
    private WatchService watchService;

    public GroupListener(ProcessingGroup group) throws IOException {
        this.group = group;
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

    public WatchService getWatchService(){
        return this.watchService;
    }

}
