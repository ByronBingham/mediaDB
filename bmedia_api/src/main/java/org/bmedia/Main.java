package org.bmedia;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;

@SpringBootApplication
@RestController
@CrossOrigin(origins = "*")
public class Main {

    private static Connection dbconn = null;

    public static void main(String[] args) {
        ApiSettings.init(args[0]);

        try {
            dbconn = DriverManager.getConnection("jdbc:postgresql://" + ApiSettings.getDbHostName() + ":" +
                            ApiSettings.getDbHostPort() + "/" + ApiSettings.getDbName(),
                    ApiSettings.getAdminUsername(), ApiSettings.getAdminPassword());
        } catch (SQLException e) {
            System.out.println("ERROR: Unable to establish connection to database. Exiting...");
            return;
        }
        args = (new ArrayList<>((Arrays.asList(args))).subList(1, args.length)).toArray(new String[0]);
        SpringApplication.run(Main.class, args);
    }

    @RequestMapping(value = "/", produces = "text/plain")
    public ResponseEntity<String> defaultResponse() {
        return ResponseEntity.status(HttpStatus.OK).body("This is the BMedia API");
    }

public synchronized static Connection getDbconn() throws SQLException{
        try{
            Statement statement = dbconn.createStatement();
            ResultSet result = statement.executeQuery("SELECT 1;");
        } catch (SQLException e){
            dbconn = DriverManager.getConnection("jdbc:postgresql://" + ApiSettings.getDbHostName() + ":" +
                            ApiSettings.getDbHostPort() + "/" + ApiSettings.getDbName(),
                    ApiSettings.getAdminUsername(), ApiSettings.getAdminPassword());
            System.out.println("INFO: Error encountered while checking DB connection:\n" + e.getMessage());
            System.out.println("INFO: Connection with database was closed. Re-connecting to database");
        }

        return dbconn;
    }
}