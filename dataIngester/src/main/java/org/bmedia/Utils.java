package org.bmedia;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Utils {

    public static boolean checkForDb(String dbName){
        // TODO: implement
        return true;
    }

    public static String getMd5(String pathString){
        try (InputStream is = Files.newInputStream(Paths.get(pathString))) {
            return DigestUtils.md5Hex(is);
        } catch (IOException e){
            System.out.println("WARNING: Issue getting md5 for file \"" + pathString + "\"");
        }
        return null;
    }

    public static void imageToJpg(String pathString){
        try {
            BufferedImage image = ImageIO.read(new File(pathString));
            String filename = FilenameUtils.getName(pathString);
            String newPath = pathString.replaceAll(filename, FilenameUtils.getBaseName(pathString) + ".jpg");
            ImageIO.write(image, "JPG", new File(newPath));
            if(!new File(pathString).delete()){
                System.out.println("WARNING: File \"" + pathString + "\" not deleted");
            }
        } catch (IOException | IllegalArgumentException e){
            System.out.println("ERROR: could not convert \"" + pathString + "\" to JPG format");
        }
    }

}
