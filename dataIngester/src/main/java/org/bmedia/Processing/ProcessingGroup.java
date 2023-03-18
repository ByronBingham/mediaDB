package org.bmedia.Processing;

import org.bmedia.Utils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.nio.file.*;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class ProcessingGroup {

    enum MEDIA_TYPE {
        IMAGE
    }

    private final String name;
    private final ArrayList<String> sourceDirs;
    private final String targetTableSchema;
    private final String targetTableName;
    private final String fullTableName;
    private final boolean autoTag;
    private final boolean jfifWebmToJpg;
    private final MEDIA_TYPE mediaType;
    private final ArrayList<String> valid_extensions;
    private final double tagProbabilityThreshold;
    private final long chunkSize;

    private final MediaProcessor mediaProcessor;

    private final GroupListener groupListener;

    public ProcessingGroup(String name, ArrayList<String> sourceDirs, String targetDbSchema, String targetDbName, boolean autoTag,
                           boolean jfifWebmToJpg, ArrayList<String> valid_extensions, MEDIA_TYPE mediaType, double tagProbabilityThreshold, long chunkSize) throws IOException {
        this.name = name;
        this.sourceDirs = sourceDirs;
        this.targetTableSchema = targetDbSchema;
        this.targetTableName = targetDbName;
        this.autoTag = autoTag;
        this.jfifWebmToJpg = jfifWebmToJpg;
        this.mediaType = mediaType;
        this.valid_extensions = valid_extensions;
        this.tagProbabilityThreshold = tagProbabilityThreshold;
        this.chunkSize = chunkSize;

        this.fullTableName = targetDbSchema + ((!targetDbSchema.equals("")) ? "." : "") + targetDbName;


        switch (this.mediaType) {
            case IMAGE:
                this.mediaProcessor = new ImageProcessor(this, 30); // TODO: make var
                break;
            default:
                System.out.println("ERROR: \"" + this.mediaType + "\" is not a valid media type");
                throw new InvalidParameterException("\"" + this.mediaType + "\" is not a valid media type");
        }
        groupListener = new GroupListener(this, this.mediaProcessor, 10); // TODO: make var

        this.mediaProcessor.start();
        this.groupListener.start();
    }

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

    public String getName() {
        return name;
    }

    public ArrayList<String> getSourceDirs() {
        return sourceDirs;
    }

    public String getTargetTableSchema() {
        return targetTableSchema;
    }

    public String getTargetTableName() {
        return targetTableName;
    }

    public String getFullTableName() {
        return this.fullTableName;
    }

    public String getFullTagJoinTableName() {
        return this.fullTableName + "_tags_join";
    }

    public boolean isAuto_tag() {
        return autoTag;
    }

    public boolean isJfifWebmToJpg() {
        return jfifWebmToJpg;
    }

    public ArrayList<String> getValidExtensions() {
        return valid_extensions;
    }

    public double getTagProbabilityThreshold() {
        return tagProbabilityThreshold;
    }

    public long getChunkSize() {
        return chunkSize;
    }

    public GroupListener getGroupListener() {
        return groupListener;
    }

    public void addFile(String filePath) {
        this.mediaProcessor.addAction(filePath, StandardWatchEventKinds.ENTRY_CREATE.name());
    }

    public void deleteFile(String filePath) {
        this.mediaProcessor.addAction(filePath, StandardWatchEventKinds.ENTRY_DELETE.name());
    }

    public void kill() {
        this.mediaProcessor.interrupt();
        this.groupListener.interrupt();
    }
}
