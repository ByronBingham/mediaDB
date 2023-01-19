package org.bmedia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.*;
import java.util.ArrayList;

@SpringBootApplication
@RestController
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

    @RequestMapping(value = "/search_images/by_tag/all", produces = "application/json")
    public ResponseEntity<String> search_images_by_tag_all(@RequestParam("tags") String[] tags,
                                                           @RequestParam("include_thumb") boolean includeThumb) {


        return ResponseEntity.status(HttpStatus.OK).body("This is the BMedia API");
    }

    @RequestMapping(value = "/search_images/by_tag/page", produces = "application/json")
    public ResponseEntity<String> search_images_by_tag_page(@RequestParam("tags") String[] tags,
                                                            @RequestParam("page_num") int pageNum,
                                                            @RequestParam("results_per_page") int resultsPerPage,
                                                            @RequestParam("include_thumb") boolean includeThumb) {

        // TODO: add base64 encoded thumbnail to return JSON
        String tag_string = "'" + String.join("','", tags) + "'";
        int numTags = tags.length;

        // for reference: https://elliotchance.medium.com/handling-tags-in-a-sql-database-5597b9894049
        String query = "SELECT a.md5,a.filename,a.resolution_width,a.resolution_height,a.file_size_bytes FROM " +
                "bmedia_schema.art a JOIN bmedia_schema.art_tags_join at ON (a.md5, a.filename) = (at.md5, at.filename) " +
                "WHERE at.tag_name IN (" + tag_string + ") " +
                "GROUP BY (a.md5, a.filename) HAVING COUNT(at.tag_name) >= " + numTags +
                " OFFSET " + pageNum * resultsPerPage + " LIMIT " + resultsPerPage + ";";

        String jsonOut = "[";
        try {
            Statement statement = dbconn.createStatement();
            ResultSet result = statement.executeQuery(query);

            ArrayList<String> jsonEntries = new ArrayList<>();
            while(result.next()){
                String md5 = result.getString("md5");
                String filename = result.getString("filename");
                int resolutionWidth = result.getInt("resolution_width");
                int resolutionHeight = result.getInt("resolution_height");
                int fileSizeBytes = result.getInt("file_size_bytes");

                String jsonEntry = "{" +
                        "\"md5\": \"" + md5 + "\"," +
                        "\"filename\": \"" + filename + "\"," +
                        "\"resolution_width\": " + resolutionWidth + "," +
                        "\"resolution_height\": " + resolutionHeight + "," +
                        "\"file_size_bytes\": " +fileSizeBytes +
                        "}";
                jsonEntries.add(jsonEntry);
            }
            jsonOut += String.join(",", jsonEntries);
            jsonOut += "]";
        } catch (SQLException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("SQL error");
        }

        return ResponseEntity.status(HttpStatus.OK).body(jsonOut);
    }
}