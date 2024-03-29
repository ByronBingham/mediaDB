package org.bmedia.Processing;

import org.apache.commons.io.FileUtils;
import org.bmedia.IngesterConfig;
import org.bmedia.Main;
import org.bmedia.Utils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.security.InvalidParameterException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Processing groups process 1 or more directory(s) that contain images to be processed. This allows the end user to
 * add different groups of folders to separate DB tables. Each processing group only processes the images found in the
 * folders specified in the ingester's config.
 * <p>
 * Processing groups store the information about the group (which DB/table/folders/etc.). The associated
 * {@link MediaProcessor} Handles the queing and processing of the group's images, and the group's {@link GroupListener}
 * handles checking for new/moved/deleted images and adding tasks to the {@link MediaProcessor}'s queue.
 * <p>
 * TODO: this class will need to be generalized more when new media types are added
 */
public class ProcessingGroup {

    /**
     * Enum for the different types of media that the ingester can handle
     * <p>
     * NOTE: currently only the IMAGE type is implemented
     */
    enum MEDIA_TYPE {
        IMAGE,
        MUSIC
    }

    // Private variables
    private final String name;
    private final ArrayList<String> sourceDirs;
    private final String targetSchema;
    private final String targetTableName;
    // Full table name. e.g. "schema_name.table_name"
    private final String fullTableName;
    private final boolean autoTag;
    private final boolean jfifWebmToJpg;
    private final MEDIA_TYPE mediaType;
    private final ArrayList<String> valid_extensions;
    private final double tagProbabilityThreshold;
    private final long chunkSize;

    private final MediaProcessor mediaProcessor;

    private final GroupListener groupListener;

    /**
     * Main constructor
     *
     * @param name                    Name of the processing group
     * @param sourceDirs              List of directories to check for media
     * @param targetDbSchema          Name of the DB schema to use
     * @param targetTableName         Name of the DB table to use
     * @param autoTag                 True if media should be auto-tagged (only applies to images currently)
     * @param jfifWebmToJpg           True if oddball filetypes shoudl be converted to more standard types
     * @param validExtensions         List of file extensions to check for. All other file extensions will be ignored
     * @param mediaType               The type of media that this group will process
     * @param tagProbabilityThreshold Ignore auto-detected tags with a confidence/probability under this threshold. Should be 0.0-1.0
     * @param chunkSize               This number specifies how many files will be processed at once by the ingester. Use a lower number if you run into memory/resource issues
     * @throws IOException
     */
    public ProcessingGroup(String name, ArrayList<String> sourceDirs, String targetDbSchema, String targetTableName, boolean autoTag,
                           boolean jfifWebmToJpg, ArrayList<String> validExtensions, MEDIA_TYPE mediaType, double tagProbabilityThreshold, long chunkSize) throws IOException {
        this.name = name;
        this.sourceDirs = sourceDirs;
        this.targetSchema = targetDbSchema;
        this.targetTableName = targetTableName;
        this.autoTag = autoTag;
        this.jfifWebmToJpg = jfifWebmToJpg;
        this.mediaType = mediaType;
        this.valid_extensions = validExtensions;
        this.tagProbabilityThreshold = tagProbabilityThreshold;
        this.chunkSize = chunkSize;

        // Full table name. e.g. "schema_name.table_name"
        this.fullTableName = targetDbSchema + ((!targetDbSchema.equals("")) ? "." : "") + targetTableName;

        // Initialize a MediaProcessor for this group
        switch (this.mediaType) {
            case IMAGE:
                this.mediaProcessor = new ImageProcessor(this, 30); // TODO: make var
                break;
            case MUSIC:
                this.mediaProcessor = new MusicProcessor(this, 30); // TODO: make var
                break;
            default:
                System.out.println("ERROR: \"" + this.mediaType + "\" is not a valid media type");
                throw new InvalidParameterException("\"" + this.mediaType + "\" is not a valid media type");
        }

        // Initialize a GroupListener for this group
        groupListener = new GroupListener(this); // TODO: make var

        // Start the processor and listener
        this.mediaProcessor.start();
        this.groupListener.start();
    }

    /**
     * Creates a list of processing groups based on the ingester's JSON config file
     * <p>
     * TODO: the JSON needs validated at some point, whether here, or before it gets here
     *
     * @param jsonFile Ingester config file
     * @return List of initialized {@link ProcessingGroup}
     */
    public static ArrayList<ProcessingGroup> createGroupsFromFile(String jsonFile) {
        ArrayList<ProcessingGroup> out = new ArrayList<>();

        String jsonString = "";
        try {
            jsonString = Files.readString(Path.of(jsonFile));
        } catch (IOException e) {
            System.out.println("ERROR: Problem encountered reading group config file:\n" + e.getMessage());
            return new ArrayList<>();
        }

        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(jsonString);
            JSONObject jsonObj = (JSONObject) obj;
            JSONArray groups = (JSONArray) jsonObj.get("ingest_groups");
            Iterator<JSONObject> groupsIter = groups.iterator();
            while (groupsIter.hasNext()) {
                JSONObject groupObj = groupsIter.next();

                String groupName = (String) groupObj.get("name");
                ArrayList<String> sourceDirs = new ArrayList(Arrays.asList(((JSONArray) groupObj.get("source_dirs")).toArray()));
                String dbSchema = (String) groupObj.get("target_schema");
                String dbName = (String) groupObj.get("target_table");
                String fullDbName = dbSchema + ((!dbSchema.equals("")) ? "." : "") + dbName;
                if (!Utils.checkForDb(fullDbName)) {
                    System.out.println("ERROR: The database \"" + fullDbName + "\" does not exist.");
                    System.out.println("\tSkipping group \"" + groupName + "\"...");
                    continue;
                }
                boolean autoTag = (boolean) groupObj.get("auto_tag");
                double tagProbThres = (double) groupObj.get("tag_prob_thres");
                boolean jfifWebmFlag = (boolean) groupObj.get("jfif_webm_to_jpg");
                MEDIA_TYPE mediaType = MEDIA_TYPE.valueOf(((String) groupObj.get("media_type")).toUpperCase());
                ArrayList<String> validExtensions = new ArrayList(Arrays.asList(((JSONArray) groupObj.get("valid_extensions")).toArray()));
                long chunkSize = (long) groupObj.get("chunk_size");

                // Create new processing group and add it to the list
                ProcessingGroup newGroup;
                try {
                    newGroup = new ProcessingGroup(groupName, sourceDirs, dbSchema, dbName, autoTag, jfifWebmFlag,
                            validExtensions, mediaType, tagProbThres, chunkSize);
                } catch (IOException e) {
                    System.out.println("WARNING: Could not create Processing Group \"" + groupName + "\". Skipping:\n" + e.getMessage());
                    continue;
                }
                out.add(newGroup);
            }
        } catch (ParseException e) {
            System.out.println("ERROR: Problem encountered parsing group config:\n" + e.getMessage());
            return new ArrayList<>();
        }

        return out;
    }

    /**
     * Gets processing group name
     *
     * @return processing group name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets list of dirs this processing group is looking in
     *
     * @return list of directories
     */
    public ArrayList<String> getSourceDirs() {
        return sourceDirs;
    }

    /**
     * Gets the name of the DB schema this processing group is using
     *
     * @return DB schema name
     */
    public String getTargetSchema() {
        return targetSchema;
    }

    /**
     * Gets the name of the DB table name this processing group is using
     *
     * @return DB table name
     */
    public String getTargetTableName() {
        return targetTableName;
    }

    /**
     * Gets the full name of the table this processing group is using, including the schema name. e.g. "schema_name.table_name"
     *
     * @return DB schema + table name
     */
    public String getFullTableName() {
        return this.fullTableName;
    }

    /**
     * Gets the full name of the tag join table this processing group is using, including the schema name. e.g. "schema_name.table_name"
     *
     * @return DB schema + table name
     */
    public String getFullTagJoinTableName() {
        return this.fullTableName + "_tags_join";
    }

    /**
     * Whether or not this group is doing auto-tagging
     *
     * @return True if this processing group is auto-tagging media
     */
    public boolean isAuto_tag() {
        return autoTag;
    }

    /**
     * Whether or not this processing group will convert file's to more usable formats.
     *
     * @return True if this group will convert file formats
     */
    public boolean isJfifWebmToJpg() {
        return jfifWebmToJpg;
    }

    /**
     * Gets the list of file extensions that this processing group will look for
     *
     * @return List of file extensions
     */
    public ArrayList<String> getValidExtensions() {
        return valid_extensions;
    }

    /**
     * Gets the probability threshold this processing group uses to keep/ignore auto-generated tags. Should be between
     * 0.0 and 1.0
     *
     * @return Probability threshold
     */
    public double getTagProbabilityThreshold() {
        return tagProbabilityThreshold;
    }

    /**
     * Gets the chunk size for this processing group. The group will process at most this number of files at once
     *
     * @return chunk size
     */
    public long getChunkSize() {
        return chunkSize;
    }

    /**
     * {@link GroupListener} object for this processing group
     *
     * @return {@link GroupListener}
     */
    public GroupListener getGroupListener() {
        return groupListener;
    }

    /**
     * Adds a file to the processing queue for this group's processor. If this image already exists in the DB, the DB
     * entry is simply updated to use the provided path instead of re-processing the image
     *
     * @param filePath (Absolute/full) path of image to add
     */
    public void addFile(String filePath) {

        // check if image already exists in DB
        long fileID = checkForFileInDB(filePath);
        if (fileID == -1) {
            this.mediaProcessor.addAction(filePath, StandardWatchEventKinds.ENTRY_CREATE.name());
        } else {
            // just update image path if the file already exists in the db
            String relPath = IngesterConfig.getPathRelativeToShare(filePath);
            if (relPath == null) {
                System.out.println("WARNING: Could not get share-relative path for \"" + filePath + "\"");
                return;
            }
            String newPath = Utils.toLinuxPath(relPath);
            updateFilePath(fileID, newPath);
        }
    }

    /**
     * Stops and processing this group is doing
     */
    public void kill() {
        this.mediaProcessor.interrupt();
        this.groupListener.interrupt();
    }

    /**
     * Checks if the given file already exists in the DB. This function will do some initial checks (checksum,
     * file size, width, height) and if everything else matches, it will check if the images are the same on a per-pixel basis
     *
     * @param filePath Path (absolute/full) of image to check
     * @return -1 if no DB image is found. If a matching DB entry is found, the matching image's ID will be returned
     */
    private long checkForFileInDB(String filePath) {

        // Get MD5 for specified file
        String md5 = Utils.getMd5(filePath);

        // Do media-type-specific stuff here
        String dbPath = null;
        long id = -1;
        long dupeID = -1;
        String fullDbPath = null;
        switch (this.mediaType) {
            case IMAGE:
                String imageQuery = "SELECT id,resolution_width,resolution_height,file_path,file_size_bytes FROM " + this.fullTableName + " WHERE md5='" + md5 + "';";
                long imageW = -1;
                long imageH = -1;
                long imageSizeBytes = -1;
                try (Statement statement = Main.getDbconn().createStatement();) {

                    ResultSet result = statement.executeQuery(imageQuery);

                    // no result returned
                    if (!result.next()) {
                        break;
                    }

                    id = result.getLong("id");
                    imageW = result.getLong("resolution_width");
                    imageH = result.getLong("resolution_height");
                    dbPath = result.getString("file_path");
                    if(dbPath == null){
                        System.out.println("INFO: File exists in DB but path was null (ID: " + id + " )");
                        return id;
                    }
                    imageSizeBytes = result.getLong("file_size_bytes");
                } catch (SQLException e) {
                    System.out.println("SQL Error: \n" + e.getMessage());
                }

                // Check dimensions and file size
                long[] whs = Utils.getWHS(filePath);
                fullDbPath = IngesterConfig.getFullFilePath(dbPath);
                // If width and height are the same, the images might be the same
                if (whs[0] == imageW && whs[1] == imageH) {
                    // Check if db image was deleted
                    if (dbPath == null || !(new File(fullDbPath).exists())) {
                        // If the image was deleted, we can't verify if the images would have been the same, but if the filesize
                        // Is the same we can probably assume they were the same image
                        if (whs[2] == imageSizeBytes) {
                            dupeID = id;
                        } else {
                            break;
                        }
                    }

                    // Check if images are really the same
                    if (Utils.areFilesSame(filePath, fullDbPath)) {
                        dupeID = id;
                    } else {
                        break;
                    }

                } else {
                    break;
                }
            case MUSIC:
                // This just checks that MD5 and filename are the same
                String musicQuery = "SELECT id,file_path FROM " + this.fullTableName + " WHERE md5='" + md5 + "';";
                try (Statement statement = Main.getDbconn().createStatement();) {

                    ResultSet result = statement.executeQuery(musicQuery);

                    // no result returned
                    if (!result.next()) {
                        break;
                    }

                    id = result.getLong("id");
                    dbPath = result.getString("file_path");
                    fullDbPath = IngesterConfig.getFullFilePath(dbPath);

                    // Check filenames
                    String dbFileName = Paths.get(dbPath).getFileName().toString();
                    String inFileName = Paths.get(filePath).getFileName().toString();
                    if (inFileName.equals(dbFileName)) {
                        // Make super-duper sure the files are the same
                        if (Utils.areFilesSame(filePath, fullDbPath)) {
                            dupeID = id;
                        } else {
                            break;
                        }
                    } else {
                        break;
                    }

                } catch (SQLException e) {
                    System.out.println("SQL Error: \n" + e.getMessage());
                }
                break;
        }

        // TODO: only delete duplicate files after confirming the new file has been added to the DB
        if (dupeID != -1 && IngesterConfig.getDeleteDuplicates()) {
            try {
                System.out.println("INFO: Deleting duplicate: \"" + fullDbPath + "\"");
                Files.delete(Path.of(fullDbPath));
            } catch (IOException e) {
                System.out.println("WARNING: Could not delete duplicate \"" + fullDbPath + "\"");
            }
        }

        return id;
    }

    /**
     * Updates a DB entry with the provided path. Use this if a file has moved or a duplicate was deleted
     *
     * @param fileID    DB entry ID of the file to update
     * @param shortPath Path (relative to the file share's base directory) to update
     */
    private void updateFilePath(long fileID, String shortPath) {
        System.out.println("INFO: Updating file ID " + fileID + " to path \"" + shortPath);

        String query = "UPDATE " + this.fullTableName + " SET file_path=? WHERE id=?;";
        try (PreparedStatement statement = Main.getDbconn().prepareStatement(query);) {
            statement.setString(1, shortPath);
            statement.setLong(2, fileID);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ERROR: SQL Error: \n" + e.getMessage());
        }
    }
}
