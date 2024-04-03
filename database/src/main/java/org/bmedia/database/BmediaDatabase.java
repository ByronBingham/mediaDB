package org.bmedia;

import java.sql.Connection;

public class BmediaDatabase {

    private static BmediaDatabase instance;

    // Instance vars
    private Connection dbConnection;

    private BmediaDatabase(String dbConfigFile){
        connectToDB(dbConfigFile);
    }

    public static boolean initialize(String dbConfigFile){
        instance = new BmediaDatabase(dbConfigFile);
        return false;
    }

    private boolean connectToDB(String dbConfigFile) {
        return false;
    }

}
