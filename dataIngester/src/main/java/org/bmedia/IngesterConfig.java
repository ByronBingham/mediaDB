package org.bmedia;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class IngesterConfig {

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

    public static void init(String configPath) throws IOException, ParseException {
        instance = new IngesterConfig(configPath);
    }

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
    }

    public static String getDbHostname() {
        return instance.dbHostname;
    }

    public static long getDbHostPort() {
        return instance.dbHostPort;
    }

    public static String getDbName() {
        return instance.dbName;
    }

    public static long getTimedUpdateDelaySec() {
        return instance.timedUpdateDelaySec;
    }

    public static long getTimedUpdateIntervalSec() {
        return instance.timedUpdateIntervalSec;
    }

    public static boolean removeBrokenPaths() {
        return instance.removeBrokenPaths;
    }

    public static String getFullFilePath(String subPath) {
        if(instance.fileShareBaseDir.endsWith("/") || instance.fileShareBaseDir.endsWith("\\")){
            return instance.fileShareBaseDir + subPath;
        } else {
            return instance.fileShareBaseDir + "/" + subPath;
        }
    }

    public static String getPythonExePath() {
        return instance.pythonExePath;
    }

    public static String getDbPassword() {
        return instance.dbPassword;
    }

    public static String getDbUser() {
        return instance.dbUser;
    }

    public static String getDdProjectPath() {
        return instance.ddProjectPath;
    }

    public static String getPathRelativeToShare(String fullPath) {
        if (fullPath.startsWith(instance.fileShareBaseDir)) {
            return fullPath.substring(instance.fileShareBaseDir.length());
        } else {
            System.out.println("ERROR: Full path does not seem to be a subdirectory/file of the base share");
            return null;
        }
    }

}
