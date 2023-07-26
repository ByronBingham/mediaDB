package org.bmedia;

import org.apache.commons.io.FileUtils;
import org.bmedia.Processing.GroupListener;
import org.bmedia.Processing.ProcessingGroup;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main class of the ingester. Runs/initializes all components.
 */
public class Main {

    /**
     * Class used to run timed tasks for the ingester
     */
    private static class TimedUpdate extends TimerTask {

        /**
         * Default constructor
         */
        public TimedUpdate() {
        }

        /**
         * Code to be run on a timer is put here. Currently runs the initial DB update function
         */
        @Override
        public void run() {
            Main.initialDbUpdate();
        }
    }

    // Private variables
    private static TimedUpdate timedUpdate;
    private static Timer timer = new Timer();

    private static ArrayList<ProcessingGroup> processingGroups;
    private static ArrayList<GroupListener> groupListeners;

    private static Connection dbconn = null;

    private static AtomicBoolean checkingFs = new AtomicBoolean(false);

    /**
     * Main function
     *
     * @param args There should be one CLI argument that points to the ingester config file
     */
    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Invalid or missing arguments: there should be one argument that points to the ingester config file");
            return;
        }
        try {
            IngesterConfig.init(args[0]);
        } catch (IOException | org.json.simple.parser.ParseException e) {
            System.out.println("ERROR: Could not read/parse the provided config file: ");
            e.printStackTrace();
            return;
        }

        // Initialize DB connection
        try {
            dbconn = DriverManager.getConnection("jdbc:postgresql://" + IngesterConfig.getDbHostname() + ":" + IngesterConfig.getDbHostPort() +
                    "/" + IngesterConfig.getDbName(), IngesterConfig.getDbUser(), IngesterConfig.getDbPassword());
        } catch (SQLException e) {
            System.out.println("ERROR: Unable to establish connection to database. Exiting...");
            return;
        }

        // Init other variables
        processingGroups = ProcessingGroup.createGroupsFromFile(args[0]);
        timedUpdate = new TimedUpdate();
        timer.schedule(timedUpdate, IngesterConfig.getTimedUpdateDelaySec() * 1000, IngesterConfig.getTimedUpdateIntervalSec() * 1000);
    }

    /**
     * This function is meant to be called infrequently to catch anything that the constant file monitoring does not catch.
     * It checks for things like broken file paths in the DB, or files that were added to the filesystem while the ingester
     * was not running.
     */
    private static void initialDbUpdate() {

        // This checks if the ingester is already running checks on the filesystem
        // If this function takes long enough, the timer might be called again while the first call is still running
        // This makes sure there is only instance of this function running at one time
        if (!checkingFs.get()) {
            checkingFs.set(true);
            System.out.println("INFO: Running check for missing files");
        } else {
            // Exit if another instance of this function is already running
            return;
        }

        // Run checks for all groups
        for (ProcessingGroup group : processingGroups) {
            // Get list of paths from DB
            String query = "SELECT file_path FROM " + group.getFullTableName() + ";";
            ArrayList<String> dbPathStrings = new ArrayList<>();
            try (Statement statement = Main.getDbconn().createStatement()) {
                ResultSet result = statement.executeQuery(query);

                while (result.next()) {
                    String stringResult = result.getString("file_path");
                    if (stringResult == null) {
                        continue;
                    }
                    dbPathStrings.add(stringResult);
                }
            } catch (SQLException e) {
                System.out.println("ERROR: SQL error");
            }

            // Make sure format of strings is same as file system paths
            HashSet<String> dbPaths = new HashSet<>();
            for (String dbPathString : dbPathStrings) {
                if (dbPathString == null) {
                    System.out.println("WARNING: Null path found in DB paths during initial update");
                    continue;
                }
                dbPaths.add((new File(IngesterConfig.getFullFilePath(dbPathString))).getAbsolutePath());
            }

            // Check for and remove broken paths
            // NOTE: I think I disabled this because having the API handle this on-demand was faster; this function took too long with this block enabled
            /*for(String dbPath: dbPaths){
                if(!Files.exists(Path.of(dbPath))){
                    // keep DB entry but set path to null
                    String relPath = IngesterConfig.getPathRelativeToShare(dbPath);
                    try {
                        removeBrokenPathInDB(relPath, group);
                    } catch (SQLException e){
                        System.out.println("WARNING: Could not delete path from DB: \"" + relPath + "\"");
                    }
                }
            }*/

            // Get all filesystem paths
            HashSet<String> fsPaths = new HashSet<>();
            for (String path : group.getSourceDirs()) {
                String[] tmp = group.getValidExtensions().toArray(new String[0]);
                if (tmp == null) {
                    System.out.println("ERROR: no valid extensions were specified");
                    return;
                }
                Collection<File> files = FileUtils.listFiles(new File(path), tmp, true);
                for (File file : files) {
                    try {
                        fsPaths.add(file.getAbsolutePath());
                    } catch (Exception e) {
                        System.out.println("ERROR: Issue getting the canonical path of file \"" + file + "\"");
                    }
                }
            }

            // Compare paths to filesystem
            if (fsPaths.size() < 1) {
                System.out.println("WARNING: No files found in the file system supplied in the config");
            }
            for (String fsPath : fsPaths) {
                if (!dbPaths.contains(fsPath)) {
                    // a file in the filesystem has not been added to the database yet
                    group.addImageFile(fsPath);
                }
            }
        }

        System.out.println("INFO: Finished queuing initial DB update");
        checkingFs.set(false);
    }

    /**
     * Gets the DB connection
     * @return DB connection
     */
    public static Connection getDbconn() {
        return dbconn;
    }

    // TODO: this function is also in the ingester; would be best if there could be a library for both services that had one copy of the function
    /**
     * Sets an item's path to NULL. This allows the DB to keep the tag data in case the image is re-added (or just moved).
     * This saves lots of processing time in the case of moved/re-added images
     * @param relativeDbPath Path of the image to "remove" relative to the file share base directory
     * @param group Processing group associated with image
     * @throws SQLException DB Exception
     */
    private static void removeBrokenPathInDB(String relativeDbPath, ProcessingGroup group) throws SQLException {
        String baseQuery = "UPDATE " + group.getFullTableName() + " SET file_path=NULL WHERE file_path=?;";

        // Make sure separators are consistent with DB
        if (relativeDbPath.startsWith("/") || relativeDbPath.startsWith("\\")) {
            relativeDbPath = relativeDbPath.substring(1);
        }

        // Run query
        PreparedStatement statement = Main.getDbconn().prepareStatement(baseQuery);
        statement.setString(1, relativeDbPath);
        statement.executeUpdate();
        System.out.println("INFO: Nulled broken path in DB: \"" + relativeDbPath + "\"");
    }
}