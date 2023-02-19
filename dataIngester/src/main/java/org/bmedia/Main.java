package org.bmedia;

import org.apache.commons.io.FileUtils;
import org.bmedia.Processing.GroupListener;
import org.bmedia.Processing.ProcessingGroup;
import org.w3c.dom.ls.LSOutput;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

    private static ArrayList<ProcessingGroup> processingGroups;
    private static ArrayList<GroupListener> groupListeners;
    private static AtomicBoolean running = new AtomicBoolean(true);

    private static Connection dbconn = null;

    public static void main(String[] args) {

        try {
            // TODO: make part of config
            dbconn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/bmedia", "bmedia_admin", "changeme");
        } catch (SQLException e) {
            System.out.println("ERROR: Unable to establish connection to database. Exiting...");
            return;
        }

        // TODO: change to arg val
        processingGroups = ProcessingGroup.createGroupsFromFile(args[0]);
        initialDbUpdate();

    }

    public static boolean isRunning() {
        return running.get();
    }

    /**
     *
     */
    private static void initialDbUpdate() {
        for (ProcessingGroup group : processingGroups) {
            // Get list of paths from DB
            String query = "SELECT file_path FROM " + group.getFullTableName() + ";";
            ArrayList<String> dbPathStrings = new ArrayList<>();
            try (Statement statement = Main.getDbconn().createStatement()) {
                ResultSet result = statement.executeQuery(query);

                while (result.next()) {
                    dbPathStrings.add(result.getString("file_path"));
                }
            } catch (SQLException e) {
                System.out.println("ERROR: SQL error");
            }

            // Make sure format of strings is same as file system paths
            HashSet<String> dbPaths = new HashSet<>();
            for (String dbPathString : dbPathStrings) {
                dbPaths.add((new File(dbPathString)).getAbsolutePath());
            }

            // Get all filesystem paths
            HashSet<String> fsPaths = new HashSet<>();
            for (String path : group.getSourceDirs()) {
                String[] tmp = group.getValid_extensions().toArray(new String[0]);
                Collection<File> files = FileUtils.listFiles(new File(path), tmp, true);
                for (File file : files) {
                    try {
                        fsPaths.add(file.getCanonicalPath());
                    } catch (IOException e){
                        System.out.println("ERROR: Issue getting the canonical path of file \"" + file + "\"");
                    }
                }
            }

            // Compare paths to filesystem
            if (fsPaths.size() > 0) {
                for (String fsPath : fsPaths) {
                    if (!dbPaths.contains(fsPath)) {
                        // a file in the filesystem has not been added to the database yet
                        group.addFile(fsPath);
                    }
                }

                for (String dbPath : dbPaths) {
                    if (!fsPaths.contains(dbPath) && false) { // TODO: make config flag to clean broken file paths from DB
                        // a path in the database no longer exists in the file system
                        group.deleteFile(dbPath);
                    }
                }

            } else {
                System.out.println("WARNING: No files found in the file system supplied in the config");
            }
        }

        System.out.println("INFO: Finished queuing initial DB update");
    }

    public static Connection getDbconn() {
        return dbconn;
    }
}