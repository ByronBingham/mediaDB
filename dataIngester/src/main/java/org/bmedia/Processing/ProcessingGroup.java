package org.bmedia.Processing;

import org.bmedia.Utils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class ProcessingGroup {

    enum MEDIA_TYPE {
        IMAGE ("image");
        public final String value;

        private MEDIA_TYPE(String label) {
            this.value = label;
        }
    }

    private final String name;
    private final ArrayList<String> sourceDirs;
    private final String targetDbSchema;
    private final String targetDbName;
    private final boolean auto_tag;
    private final boolean jfifWebmToJpg;
    private MEDIA_TYPE mediaType;
    private final ArrayList<String> valid_extensions;
    private final String dbHostAddress;
    private final String dbUsername;
    private final String dbPassword;
    private final double tagProbabilityThreshold;
    private final int chunkSize;


    private GroupListener groupListener;

    public ProcessingGroup(String name, ArrayList<String> sourceDirs, String targetDbSchema, String targetDbName, boolean auto_tag,
                           boolean jfifWebmToJpg, ArrayList<String> valid_extensions, MEDIA_TYPE mediaType, String dbHostAddress, String dbUsername,
                           String dbPassword, double tagProbabilityThreshold, int chunkSize) throws IOException {
        this.name = name;
        this.sourceDirs = sourceDirs;
        this.targetDbSchema = targetDbSchema;
        this.targetDbName = targetDbName;
        this.auto_tag = auto_tag;
        this.jfifWebmToJpg = jfifWebmToJpg;
        this.mediaType = mediaType;
        this.valid_extensions = valid_extensions;
        this.dbHostAddress = dbHostAddress;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
        this.tagProbabilityThreshold = tagProbabilityThreshold;
        this.chunkSize = chunkSize;

        groupListener = new GroupListener(this);
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
                ArrayList<String> sourceDirs = new ArrayList(Arrays.asList(((JSONArray) jsonObj.get("source_dirs")).toArray()));
                String dbSchema = (String) groupObj.get("target_schema");
                String dbName = (String) groupObj.get("target_db");
                String fullDbName = dbSchema + ((!dbSchema.equals("")) ? "." : "") + dbName;
                if (!Utils.checkForDb(fullDbName)) {
                    System.out.println("ERROR: The database \"" + fullDbName + "\" does not exist.");
                    System.out.println("\tSkipping group \"" + groupName + "\"...");
                    continue;
                }
                boolean autoTag = (boolean) groupObj.get("auto_tag");
                double tagProbThres = (double) groupObj.get("tag_prob_thres");
                boolean jfifWebmFlag = (boolean) groupObj.get("jfif_webm_to_jpg");
                MEDIA_TYPE mediaType = MEDIA_TYPE.valueOf((String) groupObj.get("db_host"));
                ArrayList<String> validExtensions = new ArrayList(Arrays.asList(((JSONArray) jsonObj.get("valid_extensions")).toArray()));
                int chunkSize = (int) groupObj.get("chunk_size");
                String dbHostAddress = (String) groupObj.get("db_host");
                String dbUsername = (String) groupObj.get("db_user");
                String dbPassword = (String) groupObj.get("db_password");

                ProcessingGroup newGroup;
                try {
                    newGroup = new ProcessingGroup(groupName, sourceDirs, dbSchema, dbName, autoTag, jfifWebmFlag,
                            validExtensions, mediaType, dbHostAddress, dbUsername, dbPassword, tagProbThres, chunkSize);
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

    public void updateFileEvents(){
        WatchKey key;
        try {
            while ((key = this.groupListener.getWatchService().take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind kind = event.kind();
                    if(kind.equals(StandardWatchEventKinds.ENTRY_CREATE)){
                        // add stuff to queue
                    } else if(kind.equals(StandardWatchEventKinds.ENTRY_DELETE)){
                        // add stuff to queue
                    }
                }
                key.reset();
            }
        } catch (InterruptedException e){
            System.out.println("WARNING: Interrupted exception while getting watch service events");
        }
    }

    public String getName() {
        return name;
    }

    public ArrayList<String> getSourceDirs() {
        return sourceDirs;
    }

    public String getTargetDbSchema() {
        return targetDbSchema;
    }

    public String getTargetDbName() {
        return targetDbName;
    }

    public boolean isAuto_tag() {
        return auto_tag;
    }

    public boolean isJfifWebmToJpg() {
        return jfifWebmToJpg;
    }

    public ArrayList<String> getValid_extensions() {
        return valid_extensions;
    }

    public String getDbHostAddress() {
        return dbHostAddress;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public double getTagProbabilityThreshold() {
        return tagProbabilityThreshold;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public GroupListener getGroupListener() {
        return groupListener;
    }
}
