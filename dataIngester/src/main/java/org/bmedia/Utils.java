package org.bmedia;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utilities class for the ingester
 */
public class Utils {

    /**
     * **NYI**
     * Make sure DB with the specified name exists
     *
     * @param dbName DB name to check
     * @return True if the DB exists. Otherwise False
     */
    public static boolean checkForDb(String dbName) {
        // TODO: implement
        return true;
    }

    /**
     * Gets the MD5 string for a file
     *
     * @param pathString Full path to file (relative or absolute, not relative to share path)
     * @return MD5 string of the specified file. Null if there is an error
     */
    public static String getMd5(String pathString) {
        try (InputStream is = Files.newInputStream(Paths.get(pathString))) {
            return DigestUtils.md5Hex(is);
        } catch (IOException e) {
            System.out.println("WARNING: Issue getting md5 for file \"" + pathString + "\"");
        }
        return null;
    }

    /**
     * Converts an image into a Jpg. This is meant to reformat images like .webp into a more usable format
     *
     * @param pathString Full path to file (relative or absolute, not relative to share path)
     */
    public static void imageToJpg(String pathString) {
        try {
            BufferedImage image = ImageIO.read(new File(pathString));
            String filename = FilenameUtils.getName(pathString);
            String newPath = pathString.replaceAll(filename, FilenameUtils.getBaseName(pathString) + ".jpg");
            ImageIO.write(image, "JPG", new File(newPath));
            if (!new File(pathString).delete()) {
                System.out.println("WARNING: File \"" + pathString + "\" not deleted");
            }
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("ERROR: could not convert \"" + pathString + "\" to JPG format");
        }
    }

    /**
     * Change Windows style ( \\ ) file separators with Linux style ( / )
     *
     * @param path Path
     * @return Path with Linux style file separators
     */
    public static String toLinuxPath(String path) {
        return path.replace("\\", "/");
    }

    /**
     * Gets the width, height, and Size of an image
     *
     * @param imagePath Full path to an image (relative or absolute, not relative to share path)
     * @return Array with 3 elements [ width, height, size (in bytes) ]
     */
    public static long[] getWHS(String imagePath) {
        BufferedImage bimg = null;
        long fileSizeBytes = 0;
        long width = 0;
        long height = 0;
        try {
            fileSizeBytes = Files.size(Path.of(imagePath));
            bimg = ImageIO.read(new File(imagePath));
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("ERROR: error getting size of \"" + imagePath + "\". Attempting to convert to a different format");
            e.printStackTrace();
            Utils.imageToJpg(imagePath);
            return null;
        }
        if (bimg != null) {
            width = bimg.getWidth();
            height = bimg.getHeight();
            return new long[]{width, height, fileSizeBytes};
        } else {
            return null;
        }
    }

    /**
     * Checks if the contents of the two files are the same using {@link FileUtils#contentEquals(File, File)}. Provided
     * paths should be the full, absolute path
     *
     * @param path1 Abs path to a file
     * @param path2 Abs path to a file
     * @return
     */
    public static boolean areFilesSame(String path1, String path2){
        File dbFile = new File(path1);
        File inFile = new File(path2);
        try {
            return FileUtils.contentEquals(inFile, dbFile);
        } catch (IOException e){
            System.out.println("WARNING: error occured checking contents of files. Assuming their contents are not the " +
                    "same: `" + path1 + "` `" + path2 + "`");
            return false;
        }
    }

}
