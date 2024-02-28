package org.bmedia.Processing;

import org.apache.commons.io.FilenameUtils;
import org.bmedia.IngesterConfig;
import org.bmedia.Main;
import org.bmedia.Utils;
import org.bmedia.tagger.ImageTagger;
import org.bmedia.tagger.ImageWithTags;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Implementation of {@link MediaProcessor} for processing images
 */
public class ImageProcessor extends MediaProcessor<String> {

    /**
     * Main constructor
     *
     * @param group                     Parent project group
     * @param processingIntervalSeconds How often to check for tasking (in seconds)
     */
    public ImageProcessor(ProcessingGroup group, long processingIntervalSeconds) {
        super(group, processingIntervalSeconds);
    }

    /**
     * The actual image processing is done here. There should only be one instance of this function running at one time
     */
    public void atomicProcessingImplementation() {

    }

    /**
     * Process images and add them into the DB
     *
     * @param pathStrings List of images to process and add to the DB
     */
    @Override
    protected void addFilesToDB(ArrayList<String> pathStrings) {
        if (pathStrings.size() == 0) {
            System.out.println("INFO: No paths provided to add");
            return;
        }

        // Get tags of images
        ArrayList<ImageWithTags> imagesWithAutoTags = null;
        if (group.isAuto_tag()) {
            imagesWithAutoTags = ImageTagger.getTagsForImages(pathStrings, group.getTagProbabilityThreshold());
            if (imagesWithAutoTags.size() == 0) {
                System.out.println("ERROR: Something went wrong running DeepDanbooru: no tags were returned");
                return;
            }
        }
        String query = "INSERT INTO " + group.getFullTableName() + " (md5, filename, file_path, resolution_width, resolution_height, file_size_bytes) VALUES ";

        // Add image data to query
        ArrayList<String> valueArr = new ArrayList<>();

        ArrayList<String> newPathStrings = new ArrayList<>();
        for (String pathString : pathStrings) {
            if (group.isJfifWebmToJpg() && (FilenameUtils.getExtension(pathString).equals("jfif") ||
                    FilenameUtils.getExtension(pathString).equals("webp"))) {
                Utils.imageToJpg(pathString);
                continue;   // grouplistener should pick up the change to the file, so we don't need to do anything here
            } else {
                newPathStrings.add(pathString);
            }

            String md5 = Utils.getMd5(pathString);
            String filename = FilenameUtils.getName(pathString).replace("'", "''");
            String fullPath = Path.of(pathString).toAbsolutePath().toString().replace("'", "''");
            long fileSizeBytes = 0;
            long width = 0;
            long height = 0;
            long[] whs = Utils.getWHS(pathString);
            if (whs == null) {
                continue;
            } else {
                if (whs.length != 3) {
                    System.out.println("ERROR: Error getting width/height/size of image " + pathString);
                    return;
                }
                width = whs[0];
                height = whs[1];
                fileSizeBytes = whs[2];
            }

            valueArr.add("('" + md5 + "', '" + filename + "', '" + Utils.toLinuxPath(IngesterConfig.getPathRelativeToShare(fullPath))
                    + "', '" + width + "', '" + height + "', '" + fileSizeBytes + "')");
        }
        pathStrings = newPathStrings;

        if (valueArr.size() == 0) {
            System.out.println("ERROR: No sql values produced for adding images to database");
            return;
        }

        // Add image data to DB
        query += String.join(",", valueArr.toArray(new String[0])) + "ON CONFLICT (file_path) DO NOTHING;";
        try (Statement statement = Main.getDbconn().createStatement()) {
            statement.executeUpdate(query);
        } catch (SQLException e) {
            System.out.println("ERROR: SQL error while adding images to database");
            return;
        }

        // Add at least one tag to every image so all images show up in searches
        ArrayList<ImageWithTags> imagesWithTags = new ArrayList<>();
        for (String path : pathStrings) {
            imagesWithTags.add(new ImageWithTags(path, new ArrayList<>(Arrays.asList(new String[]{"placeholder"}))));
        }
        addTagsToImagesInDb(imagesWithTags);

        if (group.isAuto_tag()) {
            addTagsToImagesInDb(imagesWithAutoTags);
        }
    }

    /**
     * Adds tags to existing images in the DB
     *
     * @param imagesWithTags List of {@link ImageWithTags}
     */
    private void addTagsToImagesInDb(ArrayList<ImageWithTags> imagesWithTags) {
        if (imagesWithTags.size() < 1) {
            System.out.println("WARNING: No tags returned from auto-tagging");
            return;
        }

        // Add tags to DB
        ArrayList<String> tagValues = new ArrayList<>();
        for (ImageWithTags img : imagesWithTags) {
            for (String tagName : img.getTags()) {
                tagName = tagName.replace("'", "''");
                tagValues.add("('" + tagName + "', false)");
            }
        }

        // Insert new tags into DB
        String tagQuery = "INSERT INTO " + group.getTargetSchema() + ".tags (tag_name, nsfw) VALUES " +
                String.join(",", tagValues) + "ON CONFLICT (tag_name) DO NOTHING;";
        try (Statement statement = Main.getDbconn().createStatement()) {
            statement.executeUpdate(tagQuery);
        } catch (SQLException e) {
            System.out.println("ERROR: SQL error while adding tags to database");
            return;
        }

        // Add/join tags to images in DB
        ArrayList<String> joinValues = new ArrayList<>();
        for (ImageWithTags img : imagesWithTags) {
            String filename = FilenameUtils.getName(img.getPathString());
            String md5 = Utils.getMd5(img.getPathString());
            long img_id = getIdOfImage(md5, filename);
            if (img_id == -1) {
                System.out.println("WARNING: ID not found for image, skipping...");
                continue;
            }
            if (md5 == null) {
                System.out.println("ERROR: could not get md5 for \"" + img.getPathString() + "\"");
                continue;
            }
            for (String tagName : img.getTags()) {
                tagName = tagName.replace("'", "''");
                joinValues.add("(" + img_id + ",'" + tagName + "')");
            }
        }
        String joinQuery = "INSERT INTO " + group.getFullTagJoinTableName() + " (id, tag_name) VALUES " +
                String.join(",", joinValues) + "ON CONFLICT (id, tag_name) DO NOTHING;";
        try (Statement statement = Main.getDbconn().createStatement()) {
            statement.executeUpdate(joinQuery);
        } catch (SQLException e) {
            System.out.println("ERROR: SQL error while adding tags to images in database");
        }
    }

    /**
     * Gets the DB ID for an image given the checksum and filename
     *
     * @param md5      MD5 string
     * @param filename filename (not path)
     * @return
     */
    private long getIdOfImage(String md5, String filename) {
        long idIndex = -1;

        String idQuery = "SELECT (id) FROM " + group.getFullTableName() + " WHERE md5='" + md5 + "' AND filename='" + filename + "'";
        try (Statement statement = Main.getDbconn().createStatement()){
            ResultSet result;
                result = statement.executeQuery(idQuery);
                if (!result.next()) {
                    System.out.println("WARNING: ID index not found");
                    return -1;
                }
                idIndex = result.getInt("id");
        } catch (SQLException e) {
            System.out.println("WARNING: SQL ERROR when searching for id index");
            return -1;
        }

        return idIndex;
    }

    /**
     * "Deletes" images from the DB by setting the paths of any images with one of the provided paths to null. Setting
     * paths to null allows the DB to keep tag and any other metadata in case the image is re-added to the DB (or if it
     * was just moved, etc.)
     * <p>
     * TODO: If I ever get jfif/webp support added, generalize this functionality into the abstract class
     *
     * @param pathStrings List of paths (relative to fileshare base dir) to remove from the DB
     */
    @Override
    protected void deleteFilesFromDB(ArrayList<String> pathStrings) {
        if (pathStrings.size() == 0) {
            System.out.println("INFO: No paths provided to delete");
            return;
        }

        String baseQuery = "UPDATE " + group.getFullTableName() + " SET file_path=NULL WHERE file_path=?;";
        try {
            try (PreparedStatement statement = Main.getDbconn().prepareStatement(baseQuery)) {
                for (String pathString : pathStrings) {

                    // These file types shouldn't be in the DB to begin with, so just ignore
                    if (group.isJfifWebmToJpg() &&
                            (FilenameUtils.getExtension(pathString).equals("jfif") ||
                                    FilenameUtils.getExtension(pathString).equals("webp"))) {
                        continue;
                    }
                    String relPath = IngesterConfig.getPathRelativeToShare(pathString);
                    if (relPath == null) {
                        System.out.println("WARNING: Could not get share-relative path for \"" + pathString + "\"");
                        continue;
                    }
                    String path = Utils.toLinuxPath(relPath);
                    if (path == null) {
                        System.out.println("WARNING: Could not get Linux path for \"" + IngesterConfig.getPathRelativeToShare(pathString) + "\"");
                        continue;
                    }
                    statement.setString(1, path);
                    statement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.out.println("ERROR: SQL error");
        }
    }


}
