package org.bmedia;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public class ProcessingGroup {

    private final String name;
    private final ArrayList<String> sourceDirs;
    private final String targetDbSchema;
    private final String targetDbName;
    private final boolean auto_tag;
    private final boolean jfifWebmToJpg;
    private final ArrayList<String> valid_extensions;
    private final String dbHostAddress;
    private final String dbUsername;
    private final String dbPassword;
    private final double tagProbabilityThreshold;
    private final int chunkSize;

    public ProcessingGroup(String name, ArrayList<String> sourceDirs, String targetDbSchema, String targetDbName, boolean auto_tag,
                           boolean jfifWebmToJpg, ArrayList<String> valid_extensions, String dbHostAddress, String dbUsername,
                           String dbPassword, double tagProbabilityThreshold, int chunkSize) {
        this.name = name;
        this.sourceDirs = sourceDirs;
        this.targetDbSchema = targetDbSchema;
        this.targetDbName = targetDbName;
        this.auto_tag = auto_tag;
        this.jfifWebmToJpg = jfifWebmToJpg;
        this.valid_extensions = valid_extensions;
        this.dbHostAddress = dbHostAddress;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
        this.tagProbabilityThreshold = tagProbabilityThreshold;
        this.chunkSize = chunkSize;
    }

    public static ArrayList<ProcessingGroup> createGroupsFromFile(String jsonFile){
        ArrayList<ProcessingGroup> out = new ArrayList<>();

        String jsonString = "";
        try {
            jsonString = Files.readString(Path.of(jsonFile));
        }catch (IOException e){
            System.out.println("ERROR: Problem encountered reading group config file:\n" + e.getMessage());
            return new ArrayList<>();
        }

        JSONParser parser = new JSONParser();
        try{
            Object obj = parser.parse(jsonString);
            JSONObject jsonObj = (JSONObject)obj;
            JSONArray groups = (JSONArray)jsonObj.get("ingest_groups");
            Iterator<JSONObject> groupsIter = groups.iterator();
            while(groupsIter.hasNext()){
                JSONObject groupObj = groupsIter.next();

                String groupName = (String)groupObj.get("name");
                ArrayList<String> sourceDirs = new ArrayList(Arrays.asList(((JSONArray)jsonObj.get("source_dirs")).toArray()));
                String dbSchema = (String)groupObj.get("target_schema");
                String dbName = (String)groupObj.get("target_db");
                String fullDbName = dbSchema + ((!dbSchema.equals(""))?".":"") + dbName;
                if(!Utils.checkForDb(fullDbName)){
                    System.out.println("ERROR: The database \"" + fullDbName + "\" does not exist.");
                    System.out.println("\tSkipping group \"" + groupName + "\"...");
                    continue;
                }
                boolean autoTag = (boolean)groupObj.get("auto_tag");
                double tagProbThres = (double)groupObj.get("tag_prob_thres");
                boolean jfifWebmFlag = (boolean)groupObj.get("jfif_webm_to_jpg");
                ArrayList<String> validExtensions = new ArrayList(Arrays.asList(((JSONArray)jsonObj.get("valid_extensions")).toArray()));
                int chunkSize = (int)groupObj.get("chunk_size");
                String dbHostAddress = (String)groupObj.get("db_host");
                String dbUsername = (String)groupObj.get("db_user");
                String dbPassword = (String)groupObj.get("db_password");

                ProcessingGroup newGroup = new ProcessingGroup(groupName, sourceDirs, dbSchema, dbName, autoTag, jfifWebmFlag,
                        validExtensions, dbHostAddress, dbUsername, dbPassword, tagProbThres, chunkSize);
                out.add(newGroup);
            }
        } catch (ParseException e){
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
}
