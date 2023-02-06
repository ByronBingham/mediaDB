package org.bmedia;


import org.apache.commons.io.FilenameUtils;
import org.imgscalr.Scalr;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Optional;

@SpringBootApplication
@RestController
@CrossOrigin(origins = "*")
public class Main {

    private static Connection dbconn = null;

    public static void main(String[] args) {
        try {
            dbconn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/bmedia", "bmedia_admin", "changeme");
        } catch (SQLException e) {
            System.out.println("ERROR: Unable to establish connection to database. Exiting...");
            return;
        }
        SpringApplication.run(Main.class, args);
    }

    @RequestMapping(value = "/", produces = "text/plain")
    public ResponseEntity<String> defaultResponse() {
        return ResponseEntity.status(HttpStatus.OK).body("This is the BMedia API");
    }

    public static Connection getDbconn(){
        return dbconn;
    }
}