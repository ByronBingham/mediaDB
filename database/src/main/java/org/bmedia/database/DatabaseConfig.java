package org.bmedia;

public class DatabaseConfig {

    private static DatabaseConfig instance;

    // Private vars
    private final String dbHostname;
    private final long dbHostPort;
    private final String dbName;
    private final String dbUser;
    private final String dbPassword;
    private final String fileShareBaseDir;

    private DatabaseConfig(String dbHostname, long dbHostPort, String dbName, String dbUser, String dbPassword, String fileShareBaseDir){
        this.dbHostname = dbHostname;
        this.dbHostPort = dbHostPort;
        this.dbName = dbName;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.fileShareBaseDir = fileShareBaseDir;
    }

    public static boolean initialize(String configFilePath){
        // Parse file

        // Init singleton

        return false;
    }
}
