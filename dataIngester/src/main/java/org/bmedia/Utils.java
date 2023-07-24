package org.bmedia;

import org.apache.commons.codec.digest.DigestUtils;
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
     * Gets the per-pixel difference of two images as a percentage. 100.0 being %100 different, and 0.0 being all pixels
     * the same.
     * <p>
     * NOTE: This is probably not the most optimized function. Avoid using unless you really think two images are the
     * same. e.g. check that the size and dimensions of the images match before calling this function.
     *
     * @param img1_path Full path to an image (relative or absolute, not relative to share path)
     * @param img2_path Full path to an image (relative or absolute, not relative to share path)
     * @return 0.0 to 100.0 percentage difference. Returns 100.0 (all different) if there is an error
     */
    public static double getImageDiff(String img1_path, String img2_path) {
        try {
            BufferedImage img1;
            BufferedImage img2;
            try {
                img1 = ImageIO.read(new File(img1_path));
                img2 = ImageIO.read(new File(img2_path));
            } catch (IOException e) {
                System.out.println("ERROR: could not open images \"" + img1_path + " and \"" + img2_path + ":\n" + e.getMessage());
                return -1.0;
            }
            int w1 = img1.getWidth();
            int w2 = img2.getWidth();
            int h1 = img1.getHeight();
            int h2 = img2.getHeight();
            if ((w1 != w2) || (h1 != h2)) {
                System.out.println("Both images should have same dimensions");
                return 100.0;
            } else {
                long diff = 0;
                for (int j = 0; j < h1; j++) {
                    for (int i = 0; i < w1; i++) {
                        //Getting the RGB values of a pixel
                        int pixel1 = img1.getRGB(i, j);
                        Color color1 = new Color(pixel1, true);
                        int r1 = color1.getRed();
                        int g1 = color1.getGreen();
                        int b1 = color1.getBlue();
                        int pixel2 = img2.getRGB(i, j);
                        Color color2 = new Color(pixel2, true);
                        int r2 = color2.getRed();
                        int g2 = color2.getGreen();
                        int b2 = color2.getBlue();
                        //sum of differences of RGB values of the two images
                        long data = Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);
                        diff = diff + data;
                    }
                }
                double avg = diff / (w1 * h1 * 3);
                double percentage = (avg / 255) * 100;
                return percentage;
            }
        } catch (Exception e) {
            System.out.println("WARNING: Problem getting image diff for \"" + img1_path + " and \"" + img2_path + ":\n" + e.getMessage());
            return 100.0;
        }
    }

}
