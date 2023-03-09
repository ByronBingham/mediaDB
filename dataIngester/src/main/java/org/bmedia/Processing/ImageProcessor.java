package org.bmedia.Processing;

import org.apache.commons.io.FilenameUtils;
import org.bmedia.Main;
import org.bmedia.Utils;
import org.bmedia.tagger.ImageTagger;
import org.bmedia.tagger.ImageWithTags;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class ImageProcessor extends MediaProcessor<String> {

    ArrayList<QueueAction<String>> dataChunk = new ArrayList<>();
    private AtomicBoolean processing = new AtomicBoolean(false);

    private class TimedUpdate extends TimerTask {

        ImageProcessor imageProcessor;

        public TimedUpdate(ImageProcessor imageProcessor){
            this.imageProcessor = imageProcessor;
        }
        @Override
        public void run() {
            if(!this.imageProcessor.getProcessing()) {
                System.out.println("INFO: Timed update called");
                this.imageProcessor.doAtomicProcessing();
            }
        }
    }

    private TimedUpdate timedUpdate;
    private Timer timer = new Timer();

    private long processingIntervalSeconds;

    public ImageProcessor(ProcessingGroup group, long processingIntervalSeconds) {
        super(group);
        this.processingIntervalSeconds = processingIntervalSeconds;
        timedUpdate = new TimedUpdate(this);
        timer.schedule(timedUpdate, processingIntervalSeconds * 1000, processingIntervalSeconds * 1000);
    }

    @Override
    public boolean removeAction(String data) {
        if(!this.actionQueue.removeIf(stringQueueAction -> stringQueueAction.getData().equals(data)
                && stringQueueAction.getActionType().equals(StandardWatchEventKinds.ENTRY_CREATE.name()))){
            return this.dataChunk.removeIf(stringQueueAction -> stringQueueAction.getData().equals(data)
                    && stringQueueAction.getActionType().equals(StandardWatchEventKinds.ENTRY_CREATE.name()));
        } else {
            return true;
        }
    }

    @Override
    public void processData() {
        int timesInterrupted = 0;

        while (this.running) {
            try {
                if(this.getProcessing()){
                    Thread.sleep(1000); // TODO: make var
                    continue;
                }
                dataChunk.add(this.actionQueue.take());
            } catch (InterruptedException e) {
                timesInterrupted++;
                if (timesInterrupted > 10) {  // TODO: make this a variable
                    System.out.println("ERROR: Processing thread interrupted too many times. Exiting thread");
                    return;
                } else {
                    System.out.println("WARNING: processing thread interrupted");
                    continue;
                }
            }

            // do processing if there's enough data for a chunk or if a certain amount of time has passed
            if (dataChunk.size() >= group.getChunkSize() && !this.getProcessing()) {
                doAtomicProcessing();
                timesInterrupted = 0;
            }
        }
    }

    public synchronized void doAtomicProcessing(){
        if(this.processing.get()){
            return;
        }
        this.setProcessing(true);
        // do processing
        ArrayList<String> addFiles = new ArrayList<>();
        ArrayList<String> deleteFiles = new ArrayList<>();
        for (QueueAction<String> action : dataChunk) {
            if (action.getActionType().equals(StandardWatchEventKinds.ENTRY_CREATE.name())) {
                addFiles.add(action.getData());
            } else if (action.getActionType().equals(StandardWatchEventKinds.ENTRY_DELETE.name())) {
                deleteFiles.add(action.getData());
            } else {
                System.out.println("WARNING: Queue action type \"" + action.getActionType() + "\" is not valid " +
                        "or is not implemented. Skipping action");
            }
        }

        System.out.println("INFO: Adding " + addFiles.size() + " files to the database");
        addImagesToDB(addFiles);
        System.out.println("INFO: Deleting " + deleteFiles.size() + " files from the database");
        deleteImagesFromDB(deleteFiles);

        dataChunk.clear();
        this.setProcessing(false);
    }

    public boolean getProcessing() {
        return processing.get();
    }

    public void setProcessing(boolean processing) {
        this.processing.set(processing);
    }

    private void addImagesToDB(ArrayList<String> pathStrings) {
        if(pathStrings.size() == 0){
            System.out.println("INFO: No paths provided to add");
            return;
        }
        ArrayList<ImageWithTags> imagesWithAutoTags = null;
        if (group.isAuto_tag()) {
            imagesWithAutoTags = ImageTagger.getTagsForImages(pathStrings, group.getTagProbabilityThreshold());
            if(imagesWithAutoTags.size() == 0){
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
            int width = 0;
            int height = 0;
            BufferedImage bimg = null;
            try {
                fileSizeBytes = Files.size(Path.of(pathString));
                bimg = ImageIO.read(new File(pathString));
            } catch (IOException e) {
                System.out.println("ERROR: error getting size of \"" + pathString + "\". Attempting to convert to a different format");
                Utils.imageToJpg(pathString);
                continue;
            }
            if(bimg != null) {
                width = bimg.getWidth();
                height = bimg.getHeight();
            }

            valueArr.add("('" + md5 + "', '" + filename + "', '" + fullPath + "', '" + width + "', '" + height + "', '" + fileSizeBytes + "')");
        }
        pathStrings = newPathStrings;


        if(valueArr.size() == 0){
            System.out.println("ERROR: No sql values produced for adding images to database");
            return;
        }

        query += String.join(",", valueArr.toArray(new String[0])) + "ON CONFLICT (file_path) DO NOTHING;";

        try {
            Statement statement = Main.getDbconn().createStatement();
            statement.executeUpdate(query);
        } catch (SQLException e) {
            System.out.println("ERROR: SQL error while adding images to database");
            return;
        }

        // Add at least one tag to every image so all images show up in searches
        ArrayList<ImageWithTags> imagesWithTags = new ArrayList<>();
        for(String path: pathStrings){
            imagesWithTags.add(new ImageWithTags(path, new ArrayList<>(Arrays.asList(new String[]{"placeholder"}))));
        }
        addTagsToImageInDb(imagesWithTags);


        if (group.isAuto_tag()) {
            addTagsToImageInDb(imagesWithAutoTags);
        }
    }

    private void addTagsToImageInDb(ArrayList<ImageWithTags> imagesWithTags){
        if(imagesWithTags.size() < 1){
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

        String tagQuery = "INSERT INTO " + group.getTargetTableSchema() + ".tags (tag_name, nsfw) VALUES " +
                String.join(",", tagValues) + "ON CONFLICT (tag_name) DO NOTHING;";

        try (Statement statement = Main.getDbconn().createStatement()) {
            statement.executeUpdate(tagQuery);
        } catch (SQLException e) {
            System.out.println("ERROR: SQL error while adding tags to database");
            return;
        }

        // Add tags to images in DB
        ArrayList<String> joinValues = new ArrayList<>();
        for (ImageWithTags img : imagesWithTags) {
            String filename = FilenameUtils.getName(img.getPathString());
            String md5 = Utils.getMd5(img.getPathString());
            long img_id = getIdOfImage(md5, filename);
            if(img_id == -1){
                System.out.println("WARNING: ID not found for image, skipping...");
                continue;
            }
            if(md5 == null){
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

    private long getIdOfImage(String md5, String filename){
        long idIndex = -1;

        String idQuery = "SELECT (id) FROM " + group.getFullTableName() + " WHERE md5='" + md5 + "' AND filename='" + filename + "'";
        try {
            Statement statement = Main.getDbconn().createStatement();
            ResultSet result = statement.executeQuery(idQuery);

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

    private void deleteImagesFromDB(ArrayList<String> pathStrings) {
        if(pathStrings.size() == 0){
            System.out.println("INFO: No paths provided to delete");
            return;
        }
        String baseQuery = "DELETE FROM " + group.getFullTableName() + " WHERE file_path=";

        try (Statement statement = Main.getDbconn().createStatement()) {
            for (String pathString : pathStrings) {
                if (group.isJfifWebmToJpg() &&
                        (FilenameUtils.getExtension(pathString).equals("jfif") ||
                                FilenameUtils.getExtension(pathString).equals("webp"))) {
                    continue;
                }
                String query = baseQuery + "'" + pathString + "'";
                statement.executeUpdate(query);
            }
        } catch (SQLException e) {
            System.out.println("ERROR: SQL error");
        }
    }


}
