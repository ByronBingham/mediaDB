package org.bmedia;

import org.apache.commons.io.FilenameUtils;
import org.imgscalr.Scalr;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Optional;

@RestController
@RequestMapping("/")
@CrossOrigin(origins = "*")
public class ImageController {
    @RequestMapping(value = "/search_images/by_tag/page", produces = "application/json")
    public ResponseEntity<String> search_images_by_tag_page(@RequestParam("table_name") String tbName,
                                                            @RequestParam("tags") String[] tags,
                                                            @RequestParam("page_num") int pageNum,
                                                            @RequestParam("results_per_page") int resultsPerPage,
                                                            @RequestParam("include_thumb") Optional<Boolean> includeThumb,
                                                            @RequestParam("thumb_height") Optional<Integer> thumbHeight,
                                                            @RequestParam("include_nsfw") Optional<Boolean> includeNsfw) {

        boolean includeThumbVal = includeThumb.orElse(false);
        int thumbHeightVal = thumbHeight.orElse(400);
        boolean includeNsfwVal = includeNsfw.orElse(false);
        String schemaName = ApiSettings.getSchemaName();
        String tbNameFull = schemaName + "." + tbName;
        String tagJoinTableName = schemaName + "." + tbName + "_tags_join";
        String tagTableName = schemaName + "." + "tags";

        for (int i = 0; i < tags.length; i++) {
            tags[i] = tags[i].replace("'", "''");
        }
        String tag_string = "'" + String.join("','", tags) + "'";
        int numTags = tags.length;
        String includePatString = "";

        if (includeThumbVal) {
            includePatString = ",a.file_path";
        }

        String nsfwString1 = "";
        String nsfwString2 = "";
        String nsfwJoinString = "";
        if (!includeNsfwVal) {
            nsfwJoinString += "JOIN " + tagTableName + " t ON at.tag_name = t.tag_name ";
            nsfwString1 += "t.nsfw = TRUE ";
            nsfwString2 += "MAX(CASE t.nsfw WHEN TRUE THEN 1 ELSE 0 END) = 0";
        }

        String query = "";
        if (numTags == 0) {
            if (!includeNsfwVal) {
                nsfwString1 = "WHERE " + nsfwString1;
                nsfwString2 = "HAVING " + nsfwString2;
            }
            query = "SELECT a.id,a.md5,a.filename,a.resolution_width,a.resolution_height,a.file_size_bytes" + includePatString +
                    " FROM " + tbNameFull + " a JOIN " + tagJoinTableName + " at ON (a.id) = (at.id) " +
                    nsfwJoinString +
                    "GROUP BY (a.id)" + nsfwString2 + " ORDER BY a.id DESC" +
                    " OFFSET " + pageNum * resultsPerPage + " LIMIT " + resultsPerPage + ";";
        } else {
            // for reference: https://elliotchance.medium.com/handling-tags-in-a-sql-database-5597b9894049
            if (!includeNsfwVal) {
                nsfwString1 = "OR  " + nsfwString1;
                nsfwString2 = " AND " + nsfwString2;
            }
            query = "SELECT a.id,a.md5,a.filename,a.resolution_width,a.resolution_height,a.file_size_bytes" + includePatString +
                    " FROM " + tbNameFull + " a JOIN " + tagJoinTableName + " at ON (a.id) = (at.id) " +
                    nsfwJoinString +
                    "WHERE at.tag_name IN (" + tag_string + ") " + nsfwString1 +
                    "GROUP BY (a.id) HAVING COUNT(at.tag_name) >= " + numTags + nsfwString2 + " ORDER BY a.id DESC" +
                    " OFFSET " + pageNum * resultsPerPage + " LIMIT " + resultsPerPage + ";";
        }

        String jsonOut = "[";
        try {
            Statement statement = Main.getDbconn().createStatement();
            ResultSet result = statement.executeQuery(query);

            ArrayList<String> jsonEntries = new ArrayList<>();
            while (result.next()) {
                long id = result.getLong("id");
                String md5 = result.getString("md5");
                String filename = result.getString("filename");
                int resolutionWidth = result.getInt("resolution_width");
                int resolutionHeight = result.getInt("resolution_height");
                int fileSizeBytes = result.getInt("file_size_bytes");

                String jsonEntry = "{" +
                        "\"id\": " + id + "," +
                        "\"md5\": \"" + md5 + "\"," +
                        "\"filename\": \"" + filename + "\"," +
                        "\"resolution_width\": " + resolutionWidth + "," +
                        "\"resolution_height\": " + resolutionHeight + "," +
                        "\"file_size_bytes\": " + fileSizeBytes;

                if (includeThumbVal) {
                    String imagePath = result.getString("file_path");
                    String b64Thumb = getThumbnailForImage(imagePath, thumbHeightVal);
                    if (b64Thumb == null) {
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

    @RequestMapping(value = "/search_images/by_tag/page/count", produces = "application/json")
    public ResponseEntity<String> search_images_by_tag_page_count(@RequestParam("table_name") String tbName,
                                                                  @RequestParam("tags") String[] tags,
                                                                  @RequestParam("results_per_page") int resultsPerPage,
                                                                  @RequestParam("include_nsfw") Optional<Boolean> includeNsfw) {
        boolean includeNsfwVal = includeNsfw.orElse(false);
        String schemaName = ApiSettings.getSchemaName();
        String tbNameFull = schemaName + "." + tbName;
        String tagJoinTableName = schemaName + "." + tbName + "_tags_join";
        String tagTableName = schemaName + "." + "tags";

        for (int i = 0; i < tags.length; i++) {
            tags[i] = tags[i].replace("'", "''");
        }
        String tag_string = "'" + String.join("','", tags) + "'";
        int numTags = tags.length;

        String nsfwString1 = "";
        String nsfwString2 = "";
        String nsfwJoinString = "";
        if (!includeNsfwVal) {
            nsfwJoinString += "JOIN " + tagTableName + " t ON at.tag_name = t.tag_name ";
            nsfwString1 += "t.nsfw = TRUE ";
            nsfwString2 += "MAX(CASE t.nsfw WHEN TRUE THEN 1 ELSE 0 END) = 0";
        }

        String query = "";
        if (numTags == 0) {
            if (!includeNsfwVal) {
                nsfwString1 = "WHERE " + nsfwString1;
                nsfwString2 = "HAVING " + nsfwString2;
            }
            query = "SELECT COUNT(*) AS itemCount FROM " +
                    "(SELECT a.id" +
                    " FROM " + tbNameFull + " a JOIN " + tagJoinTableName + " at ON (a.id) = (at.id) " +
                    nsfwJoinString +
                    "GROUP BY (a.id)" + nsfwString2 +
                    ") AS g;";
        } else {
            // for reference: https://elliotchance.medium.com/handling-tags-in-a-sql-database-5597b9894049
            if (!includeNsfwVal) {
                nsfwString1 = "OR  " + nsfwString1;
                nsfwString2 = " AND " + nsfwString2;
            }
            query = "SELECT COUNT(*) AS itemCount FROM " +
                    "(SELECT a.id" +
                    " FROM " + tbNameFull + " a JOIN " + tagJoinTableName + " at ON (a.id) = (at.id) " +
                    nsfwJoinString +
                    "WHERE at.tag_name IN (" + tag_string + ") " + nsfwString1 +
                    "GROUP BY (a.id) HAVING COUNT(at.tag_name) >= " + numTags + nsfwString2 +
                    ") AS g;";
        }

        String jsonOut = "";
        try {
            Statement statement = Main.getDbconn().createStatement();
            ResultSet result = statement.executeQuery(query);

            if (result.next()) {
                int totalResults = result.getInt("itemCount");
                int pages = (int) Math.ceil(totalResults / resultsPerPage);

                jsonOut += "{" +
                        "\"pages\": \"" + pages + "\"," +
                        "\"total_results\": " + totalResults;

                jsonOut += "}";
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("SQL error");
        }

        return ResponseEntity.status(HttpStatus.OK).body(jsonOut);
    }

    @RequestMapping(value = "/images/get_thumbnail", produces = "application/json")
    public ResponseEntity<String> get_image_thumbnail(@RequestParam("table_name") String tbName,
                                                      @RequestParam("id") long id,
                                                      @RequestParam("thumb_height") Optional<Integer> thumbHeight) {
        int thumbHeightVal = thumbHeight.orElse(400);
        String schemaName = ApiSettings.getSchemaName();
        String tbNameFull = schemaName + "." + tbName;

        String query = "SELECT file_path FROM " + tbNameFull + " WHERE id='" + id + "';";

        String b64Thumb = null;
        try {
            Statement statement = Main.getDbconn().createStatement();
            ResultSet result = statement.executeQuery(query);

            if (!result.next()) {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("SQL error: no results returned");
            }

            String filePath = result.getString("file_path");
            b64Thumb = getThumbnailForImage(filePath, thumbHeightVal);
            if (b64Thumb == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("SQL error: no results returned from query");
            }

        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("SQL error");
        }

        String jsonOut = "{" +
                "\"id\": \"" + id + "\"," +
                "\n\"thumb_base64\": \"" + b64Thumb + "\"\n}";

        return ResponseEntity.status(HttpStatus.OK).body(jsonOut);
    }

    @RequestMapping(value = "/images/get_image_full", produces = "application/json")
    public ResponseEntity<String> get_image_full(@RequestParam("table_name") String tbName,
                                                 @RequestParam("id") long id) {
        String schemaName = ApiSettings.getSchemaName();
        String tbNameFull = schemaName + "." + tbName;

        String query = "SELECT file_path FROM " + tbNameFull + " WHERE id=" + id + ";";

        String b64Image = null;
        try {
            Statement statement = Main.getDbconn().createStatement();
            ResultSet result = statement.executeQuery(query);

            if (!result.next()) {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("SQL error: no results returned");
            }

            String filePath = result.getString("file_path");
            b64Image = getFullImage_b64(filePath);
            if (b64Image == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("SQL error: no results returned from query");
            }

        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("SQL error");
        }

        String jsonOut = "{" +
                "\"id\": \"" + id + "\"," +
                "\n\"image_base64\": \"" + b64Image + "\"\n}";

        return ResponseEntity.status(HttpStatus.OK).body(jsonOut);
    }

    @RequestMapping(value = "/images/get_tags", produces = "application/json")
    public ResponseEntity<String> get_image_tags(@RequestParam("table_name") String tbName,
                                                 @RequestParam("id") long id) {
        String schemaName = ApiSettings.getSchemaName();
        String tagJoinTableName = schemaName + "." + tbName + "_tags_join";
        String tagTableName = schemaName + "." + "tags";

        String query = "SELECT at.tag_name, t.nsfw FROM " + tagTableName + " t " +
                "JOIN " + tagJoinTableName + " at ON t.tag_name = at.tag_name " +
                "WHERE at.id=" + id + ";";

        String jsonOut = "[";
        try {
            Statement statement = Main.getDbconn().createStatement();
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

    @RequestMapping(value = "/images/add_tag", produces = "application/json")
    public ResponseEntity<String> add_tag_to_image(@RequestParam("table_name") String tbName,
                                                   @RequestParam("id") long id,
                                                   @RequestParam("tag_name") String tagName,
                                                   @RequestParam("nsfw") Optional<Boolean> nsfw,
                                                   @RequestParam("overwrite_nsfw") Optional<Boolean> overwriteNsfw) {
        if (tagName.equals("")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cannot have empty tag name in request");
        }

        boolean nsfwVal = nsfw.orElse(false);
        boolean overwriteNsfwVal = overwriteNsfw.orElse(false);
        tagName = tagName.replace("'", "''");
        String schemaName = ApiSettings.getSchemaName();
        String tagJoinTableName = tbName + "_tags_join";

        String query1 = "INSERT INTO " + schemaName + ".tags (tag_name, nsfw) VALUES (?, ?) ON CONFLICT (tag_name) DO";
        if(overwriteNsfwVal){
            query1 += " UPDATE SET nsfw = " +
                    "EXCLUDED.nsfw;";
        } else {
            query1 += " NOTHING;";
        }

        String query2 = "INSERT INTO " + schemaName + "." + tagJoinTableName + " (id, tag_name) VALUES (?, ?)" +
                " ON CONFLICT DO NOTHING;";

        try {
            // add tag if not already in tag table
            PreparedStatement statement1 = Main.getDbconn().prepareStatement(query1);
            statement1.setString(1, tagName);
            statement1.setBoolean(2, nsfwVal);
            statement1.executeUpdate();

            // add entry into join table
            PreparedStatement statement2 = Main.getDbconn().prepareStatement(query2);
            statement2.setLong(1, id);
            statement2.setString(2, tagName);
            statement2.executeUpdate();

        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("SQL error");
        }

        return ResponseEntity.status(HttpStatus.OK).body("Successfully added tag");
    }

    @RequestMapping(value = "/images/delete_tag", produces = "application/json")
    public ResponseEntity<String> delte_tag_for_image(@RequestParam("table_name") String tbName,
                                                      @RequestParam("id") String id,
                                                      @RequestParam("tag_name") String tagName) {
        if (tagName.equals("")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cannot have empty tag name in request");
        }

        tagName = tagName.replace("'", "''");
        String schemaName = ApiSettings.getSchemaName();
        String tagJoinTableName = tbName + "_tags_join";
        tagName = tagName.replace("'", "''");

        String query = "DELETE FROM " + schemaName + "." + tagJoinTableName + " WHERE id = '" + id + "'" +
                " AND tag_name = '" + tagName + "';";

        try {
            Statement statement = Main.getDbconn().createStatement();
            statement.executeUpdate(query);
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("SQL error");
        }

        return ResponseEntity.status(HttpStatus.OK).body("Successfully added tag");
    }

    private String getThumbnailForImage(String imagePath, int thumbHeight) {

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

    private String getFullImage_b64(String imagePath) {
        ByteArrayOutputStream boas = new ByteArrayOutputStream();
        try {
            BufferedImage img = ImageIO.read(new File(imagePath));
            String extension = FilenameUtils.getExtension(imagePath);
            if (!ImageIO.write(img, extension, boas)) {
                System.out.println("ERROR: Failed to write image to buffer for b64 encoding.");
                return null;
            }
        } catch (IOException e) {
            System.out.println("ERROR: IO error while trying to encode image " + imagePath + ". \n" + e.getMessage());
            return null;
        }
        return Base64.getEncoder().encodeToString(boas.toByteArray());
    }
}
