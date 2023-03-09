package org.bmedia;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class IngesterConfig {

    private final String dbHostname;
    private final long dbHostPort;
    private final String dbName;
    private final long timedUpdateDelaySec;
    private final long timedUpdateIntervalSec;

    public IngesterConfig(String configPath) throws IOException, ParseException {
        String jsonString = "";
        jsonString = Files.readString(Path.of(configPath));

        JSONParser parser = new JSONParser();
        Object obj = parser.parse(jsonString);
        JSONObject jsonObj = (JSONObject) obj;

        dbHostname = (String) jsonObj.get("db_host_name");
        dbHostPort = (long) jsonObj.get("db_host_port");
        dbName = (String) jsonObj.get("db_name");
        timedUpdateDelaySec = (long) jsonObj.get("update_delay_sec");
        timedUpdateIntervalSec = (long) jsonObj.get("update_interval_sec");
        removeBrokenPaths = (boolean) jsonObj.get("remove_broken_paths");
    }

    private final boolean removeBrokenPaths;

    public String getDbHostname() {
        return dbHostname;
    }

    public long getDbHostPort() {
        return dbHostPort;
    }

    public String getDbName() {
        return dbName;
    }

    public long getTimedUpdateDelaySec() {
        return timedUpdateDelaySec;
    }

    public long getTimedUpdateIntervalSec() {
        return timedUpdateIntervalSec;
    }

    public boolean removeBrokenPaths() {
        return removeBrokenPaths;
    }

}
