package org.bmedia;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.bmedia.Processing.GroupListener;
import org.bmedia.Processing.ProcessingGroup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

    private static class TimedUpdate extends TimerTask {

        public TimedUpdate() {
        }

        @Override
        public void run() {
            Main.initialDbUpdate();
        }
    }

    private static TimedUpdate timedUpdate;
    private static Timer timer = new Timer();

    private static ArrayList<ProcessingGroup> processingGroups;
    private static ArrayList<GroupListener> groupListeners;
    private static AtomicBoolean running = new AtomicBoolean(true);

    private static Connection dbconn = null;

    private static AtomicBoolean checkingFs = new AtomicBoolean(false);

    public static void main(String[] args) {

        if(args.length != 1){
            System.out.println("Invalid or missing arguments: there should be one argument that points to the ingester config file");
            return;
        }
        try {
            IngesterConfig.init(args[0]);
        }catch (IOException | org.json.simple.parser.ParseException e){
            System.out.println("ERROR: Could not read/parse the provided config file: ");
            e.printStackTrace();
            return;
        }

        try {
            String tmp = "jdbc:postgresql://" + IngesterConfig.getDbHostname() + ":" + IngesterConfig.getDbHostPort() +
                    "/" + IngesterConfig.getDbName();
            dbconn = DriverManager.getConnection("jdbc:postgresql://" + IngesterConfig.getDbHostname() + ":" + IngesterConfig.getDbHostPort() +
                    "/" + IngesterConfig.getDbName(), IngesterConfig.getDbUser(), IngesterConfig.getDbPassword());
        } catch (SQLException e) {
            System.out.println("ERROR: Unable to establish connection to database. Exiting...");
            return;
        }

        processingGroups = ProcessingGroup.createGroupsFromFile(args[0]);
        timedUpdate = new TimedUpdate();
        timer.schedule(timedUpdate, IngesterConfig.getTimedUpdateDelaySec() * 1000, IngesterConfig.getTimedUpdateIntervalSec() * 1000);

    }

    public static boolean isRunning() {
        return running.get();
    }

    /**
     *
     */
    private static void initialDbUpdate() {
        if(!checkingFs.get()){
            checkingFs.set(true);
            System.out.println("INFO: Running check for missing files");
        }
        else {
            return;
        }
        for (ProcessingGroup group : processingGroups) {
            // Get list of paths from DB
            String query = "SELECT file_path FROM " + group.getFullTableName() + ";";
            ArrayList<String> dbPathStrings = new ArrayList<>();
            try (Statement statement = Main.getDbconn().createStatement()) {
                ResultSet result = statement.executeQuery(query);

                while (result.next()) {
                    String stringResult = result.getString("file_path");
                    if(stringResult == null){
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
                if(dbPathString == null){
                    System.out.println("WARNING: Null path found in DB paths during initial update");
                    continue;
                }
                dbPaths.add((new File(IngesterConfig.getFullFilePath(dbPathString))).getAbsolutePath());
            }

            // Check for and remove broken paths
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
                if(tmp == null){
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
                    group.addFile(fsPath);
                }
            }
        }

        System.out.println("INFO: Finished queuing initial DB update");
        checkingFs.set(false);
    }

    public static Connection getDbconn() {
        return dbconn;
    }

    private static void removeBrokenPathInDB(String relativeDbPath, ProcessingGroup group) throws SQLException{
        String baseQuery = "UPDATE " + group.getFullTableName() + " SET file_path=NULL WHERE file_path=?;";

        if(relativeDbPath.startsWith("/") || relativeDbPath.startsWith("\\")){
            relativeDbPath = relativeDbPath.substring(1);
        }

        PreparedStatement statement = Main.getDbconn().prepareStatement(baseQuery);
        statement.setString(1, relativeDbPath);
        statement.executeUpdate();
        System.out.println("INFO: Nulled broken path in DB: \"" + relativeDbPath + "\"");
    }
}