package org.bmedia;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class ApiSettings {

    private static ApiSettings instance;

    private String dbHostName;
    private String dbHostPort;
    private String dbName;
    private String fileShareBaseDir;
    private String schemaName;
    private String adminUsername;
    private String adminPassword;
    private String queryUsername;
    private String queryPassword;

    public static void init(String dbConfigPath) {
        instance = new ApiSettings(dbConfigPath);
    }

    private ApiSettings(String dbConfigPath) {
        JSONParser parser = new JSONParser();
        String dbConfigString = "";
        try {
            dbConfigString = FileUtils.readFileToString(new File(dbConfigPath));
        } catch (IOException e) {
            System.out.println("ERROR: \"" + dbConfigPath + "\" is not a valid file");
            return;
        }
        try {
            Object obj = parser.parse(dbConfigString);
            JSONObject jsonObj = (JSONObject) obj;

            dbHostName = (String) jsonObj.get("database_host_name");
            dbHostPort = (String) jsonObj.get("database_host_port");
            dbName = (String) jsonObj.get("database_name");
            schemaName = (String) jsonObj.get("database_schema");
            adminUsername = (String) jsonObj.get("admin_username");
            adminPassword = (String) jsonObj.get("admin_password");
            queryUsername = (String) jsonObj.get("query_username");
            queryPassword = (String) jsonObj.get("query_password");
        } catch (ParseException e) {
            System.out.println("ERROR: Problem encountered parsing db config:\n" + e.getMessage());
            return;
        }
        fileShareBaseDir = System.getenv("MEDIA_SHARE");
        if (fileShareBaseDir == null) {
            System.out.println("ERROR: Did not find environment variable \"MEDIA_SHARE\". Please make sure it is defined");
            return;
        }
        if (!FileUtils.isDirectory(new File(fileShareBaseDir))) {
            System.out.println("ERROR: Environment variable \"MEDIA_SHARE\" is not a directory.");
            return;
        }
    }


    public static String getDbName() {
        return instance.dbName;
    }

    public static String getSchemaName() {
        return instance.schemaName;
    }

    public static String getAdminUsername() {
        return instance.adminUsername;
    }

    public static String getAdminPassword() {
        return instance.adminPassword;
    }

    public static String getQueryUsername() {
        return instance.queryUsername;
    }

    public static String getQueryPassword() {
        return instance.queryPassword;
    }

    public static String getDbHostName() {
        return instance.dbHostName;
    }

    public static String getDbHostPort() {
        return instance.dbHostPort;
    }

    public static String getFileShareBaseDir() {
        return instance.fileShareBaseDir;
    }

    public static String getFullFilePath(String subPath) {
        return instance.fileShareBaseDir + "/" + subPath;
    }
}
