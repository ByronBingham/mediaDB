package org.bmedia.Processing;

import org.bmedia.IngesterConfig;
import org.bmedia.Main;
import org.bmedia.Utils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class MusicProcessor extends MediaProcessor<String> {

    /**
     * Main constructor
     *
     * @param group {@link ProcessingGroup} to process for
     */
    protected MusicProcessor(ProcessingGroup group, long processingIntervalSeconds) {
        super(group, processingIntervalSeconds);
    }

    /**
     * Process songs and add them into the DB
     *
     * @param pathStrings List of images to process and add to the DB
     */
    @Override
    protected void addFilesToDB(ArrayList<String> pathStrings) {
        ArrayList<String> valueArr = new ArrayList<>();

        for (String path : pathStrings) {

            String md5 = Utils.getMd5(path);
            String dbPath = IngesterConfig.getPathRelativeToShare(path);

            valueArr.add("('" + md5 + "', '" + dbPath + "')");
        }

        String query = "INSERT INTO " + group.getFullTableName() + " (md5, file_path) VALUES ";

        query += String.join(",", valueArr.toArray(new String[0])) + "ON CONFLICT (file_path) DO NOTHING;";
        try (Statement statement = Main.getDbconn().createStatement()) {
            statement.executeUpdate(query);
        } catch (SQLException e) {
            System.out.println("ERROR: SQL error while adding music files to database");
            return;
        }

    }

    /**
     * "Deletes" music files from the DB by setting the paths of any files with one of the provided paths to null. Setting
     * paths to null allows the DB to keep tag and any other metadata in case the image is re-added to the DB (or if it
     * was just moved, etc.)
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
