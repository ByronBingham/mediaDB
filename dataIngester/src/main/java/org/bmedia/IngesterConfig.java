package org.bmedia;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Singleton class to store ingester config information
 */
public class IngesterConfig {

    // Private variables
    private static IngesterConfig instance;

    private final String dbHostname;
    private final long dbHostPort;
    private final String dbName;
    private final String dbUser;
    private final String dbPassword;
    private final String fileShareBaseDir;
    private final long timedUpdateDelaySec;
    private final long timedUpdateIntervalSec;
    private final boolean removeBrokenPaths;
    private final String pythonExePath;
    private final String ddProjectPath;
    private final boolean deleteDuplicates;

    /**
     * Static initialization function. Call this before using the static methods of this class
     *
     * @param configPath Path to ingester config
     * @throws IOException
     * @throws ParseException
     */
    public static void init(String configPath) throws IOException, ParseException {
        instance = new IngesterConfig(configPath);
    }

    /**
     * Main constructor
     * <p>
     * TODO: validate against schema
     *
     * @param configPath Path to ingester config
     * @throws IOException
     * @throws ParseException
     */
    private IngesterConfig(String configPath) throws IOException, ParseException {
        String jsonString = "";
        jsonString = Files.readString(Path.of(configPath));

        JSONParser parser = new JSONParser();
        Object obj = parser.parse(jsonString);
        JSONObject jsonObj = (JSONObject) obj;

        dbHostname = (String) jsonObj.get("db_host_name");
        dbHostPort = (long) jsonObj.get("db_host_port");
        dbName = (String) jsonObj.get("db_name");
        dbUser = (String) jsonObj.get("db_user");
        dbPassword = (String) jsonObj.get("db_password");
        timedUpdateDelaySec = (long) jsonObj.get("update_delay_sec");
        timedUpdateIntervalSec = (long) jsonObj.get("update_interval_sec");
        removeBrokenPaths = (boolean) jsonObj.get("remove_broken_paths");
        pythonExePath = (String) jsonObj.get("python_exe");
        ddProjectPath = (String) jsonObj.get("dd_project_dir");
        fileShareBaseDir = (String) jsonObj.get("share_base_dir");
        deleteDuplicates = (boolean) jsonObj.get("delete_duplicates");
    }

    /**
     * Get the DB host address
     *
     * @return DB address
     */
    public static String getDbHostname() {
        return instance.dbHostname;
    }

    /**
     * Get the port number used to connect to the DB
     *
     * @return DB host port number
     */
    public static long getDbHostPort() {
        return instance.dbHostPort;
    }

    /**
     * Get name of the DB to use
     *
     * @return DB name
     */
    public static String getDbName() {
        return instance.dbName;
    }

    /**
     * This is used to specify after what period of time the timed update should start running
     *
     * @return Timed update delay in seconds
     */
    public static long getTimedUpdateDelaySec() {
        return instance.timedUpdateDelaySec;
    }

    /**
     * This specifies the interval at which the timed update is run
     *
     * @return Timed update interval in seconds
     */
    public static long getTimedUpdateIntervalSec() {
        return instance.timedUpdateIntervalSec;
    }

    /**
     * Not currently in use
     * <p>
     * If this is true, the ingester will remove broken paths in the DB
     *
     * @return True if the ingester should remove broken paths, otherwise False
     */
    public static boolean removeBrokenPaths() {
        return instance.removeBrokenPaths;
    }

    /**
     * Returns the full, absolute path, given a path that is relative to the file share base path
     *
     * @param subPath Path relative to the file share base path
     * @return Full, absolute path
     */
    public static String getFullFilePath(String subPath) {
        if (subPath == null) {
            System.out.println("WARNING: \"subPath\" was null");
            return null;
        }
        if (subPath.startsWith(instance.fileShareBaseDir)) {
            System.out.println("WARNING: full path passed into \"getFullPath()\"");
            return subPath;
        } else {
            if (instance.fileShareBaseDir.endsWith("/") || instance.fileShareBaseDir.endsWith("\\")) {
                return instance.fileShareBaseDir + subPath;
            } else {
                return instance.fileShareBaseDir + "/" + subPath;
            }
        }
    }

    /**
     * Path to the python executable to use for the image auto-tagging script
     *
     * @return Path to Python interpreter
     */
    public static String getPythonExePath() {
        return instance.pythonExePath;
    }

    /**
     * Get the DB password
     * <p>
     * TODO: better security
     *
     * @return DB password
     */
    public static String getDbPassword() {
        return instance.dbPassword;
    }

    /**
     * Get DB username to use
     * <p>
     * TODO: better security
     *
     * @return Username
     */
    public static String getDbUser() {
        return instance.dbUser;
    }

    /**
     * Path to DeepDanbooru project folder. Needed for image tag processing
     *
     * @return Path to DeepDanbooru project folder
     */
    public static String getDdProjectPath() {
        return instance.ddProjectPath;
    }

    /**
     * Whether the ingester should remove duplicate image entries from the DB
     *
     * @return True if duplicates should be removed
     */
    public static boolean getDeleteDuplicates() {
        return instance.deleteDuplicates;
    }

    /**
     * Given an absolute path to a file on the file share, returns the path relative to the file share's base directory.
     * This is how file paths are stored in the DB.
     *
     * @param fullPath Full file path, including the path to the file share
     * @return Path relative to the file share's base directory
     */
    public static String getPathRelativeToShare(String fullPath) {
        if (fullPath.startsWith(instance.fileShareBaseDir)) {
            return fullPath.substring(instance.fileShareBaseDir.length());
        } else {
            System.out.println("ERROR: Full path does not seem to be a subdirectory/file of the base share");
            return null;
        }
    }

}
