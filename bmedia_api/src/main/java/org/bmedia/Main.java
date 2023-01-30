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

    @RequestMapping(value = "/search_images/by_tag/all", produces = "application/json")
    public ResponseEntity<String> search_images_by_tag_all(@RequestParam("tags") String[] tags,
                                                           @RequestParam("include_thumb") boolean includeThumb) {


        return ResponseEntity.status(HttpStatus.OK).body("This is the BMedia API");
    }

    @RequestMapping(value = "/search_images/by_tag/page", produces = "application/json")
    public ResponseEntity<String> search_images_by_tag_page(@RequestParam("tags") String[] tags,
                                                            @RequestParam("page_num") int pageNum,
                                                            @RequestParam("results_per_page") int resultsPerPage,
                                                            @RequestParam("include_thumb") Optional<Boolean> includeThumb,
                                                            @RequestParam("thumb_height") Optional<Integer> thumbHeight) {

        boolean includeThumbVal = includeThumb.orElse(false);
        int thumbHeightVal = thumbHeight.orElse(400);


        String tag_string = "'" + String.join("','", tags) + "'";
        int numTags = tags.length;
        String includePatString = "";

        if (includeThumbVal) {
            includePatString = ",a.file_path";
        }

        // for reference: https://elliotchance.medium.com/handling-tags-in-a-sql-database-5597b9894049
        String query = "SELECT a.md5,a.filename,a.resolution_width,a.resolution_height,a.file_size_bytes" + includePatString +
                " FROM bmedia_schema.art a JOIN bmedia_schema.art_tags_join at ON (a.md5, a.filename) = (at.md5, at.filename) " +
                "WHERE at.tag_name IN (" + tag_string + ") " +
                "GROUP BY (a.md5, a.filename) HAVING COUNT(at.tag_name) >= " + numTags +
                " OFFSET " + pageNum * resultsPerPage + " LIMIT " + resultsPerPage + ";";

        String jsonOut = "[";
        try {
            Statement statement = dbconn.createStatement();
            ResultSet result = statement.executeQuery(query);

            ArrayList<String> jsonEntries = new ArrayList<>();
            while (result.next()) {
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
                        "\"file_size_bytes\": " + fileSizeBytes;

                if (includeThumbVal) {
                    String imagePath = result.getString("file_path");
                    String b64Thumb = getThumbnailForImage(imagePath, thumbHeightVal);
                    if(b64Thumb == null){
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("FILE IO error");
                    }
                    jsonEntry += "\n,\"thumb_base64\": \"" + b64Thumb + "\"";
                }

                jsonEntry += "}";
                jsonEntries.add(jsonEntry);
            }
            jsonOut += String.join(",", jsonEntries);
            jsonOut += "]";
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("SQL error");
        }

        return ResponseEntity.status(HttpStatus.OK).body(jsonOut);
    }

    @RequestMapping(value = "/images/get_thumbnail", produces = "application/json")
    public ResponseEntity<String> get_image_thumbnail(@RequestParam("md5") String md5,
                                                      @RequestParam("filename") String filename,
                                                      @RequestParam("thumb_height") Optional<Integer> thumbHeight){
        int thumbHeightVal = thumbHeight.orElse(400);

        String query = "SELECT file_path FROM bmedia_schema.art WHERE md5='" + md5 + "' AND filename='" + filename + "';";

        String b64Thumb = null;
        try{
            Statement statement = dbconn.createStatement();
            ResultSet result = statement.executeQuery(query);

            if(!result.next()){
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("SQL error: no results returned");
            }

            String filePath = result.getString("file_path");
            b64Thumb = getThumbnailForImage(filePath, thumbHeightVal);
            if(b64Thumb == null){
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("SQL error: no results returned from query");
            }

        } catch (SQLException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("SQL error");
        }

        String jsonOut = "{" +
                "\"md5\": \"" + md5 + "\"," +
                "\"filename\": \"" + filename + "\"," +
                "\n\"thumb_base64\": \"" + b64Thumb + "\"\n}";

        return ResponseEntity.status(HttpStatus.OK).body(jsonOut);
    }

    @RequestMapping(value = "/images/get_image_full", produces = "application/json")
    public ResponseEntity<String> get_image_full(@RequestParam("md5") String md5,
                                                      @RequestParam("filename") String filename){

        String query = "SELECT file_path FROM bmedia_schema.art WHERE md5='" + md5 + "' AND filename='" + filename + "';";

        String b64Image = null;
        try{
            Statement statement = dbconn.createStatement();
            ResultSet result = statement.executeQuery(query);

            if(!result.next()){
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("SQL error: no results returned");
            }

            String filePath = result.getString("file_path");
            b64Image = getFullImage_b64(filePath);
            if(b64Image == null){
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("SQL error: no results returned from query");
            }

        } catch (SQLException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("SQL error");
        }

        String jsonOut = "{" +
                "\"md5\": \"" + md5 + "\"," +
                "\"filename\": \"" + filename + "\"," +
                "\n\"image_base64\": \"" + b64Image + "\"\n}";

        return ResponseEntity.status(HttpStatus.OK).body(jsonOut);
    }

    @RequestMapping(value = "/images/get_tags", produces = "application/json")
    public ResponseEntity<String> get_image_tags(@RequestParam("md5") String md5,
                                                 @RequestParam("filename") String filename) {

        // for reference: https://elliotchance.medium.com/handling-tags-in-a-sql-database-5597b9894049
        String query = "SELECT at.tag_name, t.nsfw FROM bmedia_schema.tags t " +
                "JOIN bmedia_schema.art_tags_join at ON t.tag_name = at.tag_name " +
                "WHERE at.md5='" + md5 + "' AND at.filename='" + filename + "';";

        String jsonOut = "[";
        try {
            Statement statement = dbconn.createStatement();
            ResultSet result = statement.executeQuery(query);

            ArrayList<String> jsonEntries = new ArrayList<>();
            while (result.next()) {
                String tag_name = result.getString("tag_name");
                Boolean nsfw = result.getBoolean("nsfw");

                String jsonEntry = "{" +
                        "\"tag_name\": \"" + tag_name + "\"," +
                        "\"nsfw\": " + nsfw + "}";
                jsonEntries.add(jsonEntry);
            }
            jsonOut += String.join(",", jsonEntries);
            jsonOut += "]";
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("SQL error");
        }

        return ResponseEntity.status(HttpStatus.OK).body(jsonOut);
    }
    private String getThumbnailForImage(String imagePath, int thumbHeight){

        ByteArrayOutputStream boas = new ByteArrayOutputStream();
        String imgExt = FilenameUtils.getExtension(imagePath);
        try {
            BufferedImage img = ImageIO.read(new File(imagePath));
            BufferedImage imgSmall = Scalr.resize(img, Scalr.Method.BALANCED, Scalr.Mode.FIT_TO_HEIGHT,
                    thumbHeight, thumbHeight, Scalr.OP_ANTIALIAS);

            // convert image to jpg compatible format if necessary
            if (imgExt.equals("png")) {
                BufferedImage newBufferedImage = new BufferedImage(imgSmall.getWidth(), imgSmall.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
                newBufferedImage.createGraphics().drawImage(imgSmall, 0, 0, Color.WHITE, null);
                imgSmall = newBufferedImage;
            }
            if (!ImageIO.write(imgSmall, "jpg", boas)) {
                System.out.println("ERROR: Failed to write image to buffer for b64 encoding.");
                return null;
            }
        } catch (IOException e) {
            System.out.println("ERROR: IO error while trying to encode image" + imagePath + ". \n" + e.getMessage());
            return null;
        }
        return Base64.getEncoder().encodeToString(boas.toByteArray());

    }

    private String getFullImage_b64(String imagePath){
        ByteArrayOutputStream boas = new ByteArrayOutputStream();
        try {
            BufferedImage img = ImageIO.read(new File(imagePath));
            String extension = FilenameUtils.getExtension(imagePath);
            if (!ImageIO.write(img, extension, boas)) {
                System.out.println("ERROR: Failed to write image to buffer for b64 encoding.");
                return null;
            }
        } catch (IOException e){
            System.out.println("ERROR: IO error while trying to encode image " + imagePath + ". \n" + e.getMessage());
            return null;
        }
        return Base64.getEncoder().encodeToString(boas.toByteArray());
    }

}