package org.bmedia.tagger;

import org.bmedia.IngesterConfig;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Utility class used to get auto-generated tags for images
 * <p>
 * Uses the DeepDanbooru project (<a href="https://github.com/KichangKim/DeepDanbooru">Link to repo here</a>)
 */
public class ImageTagger {

    /**
     * Runs DeepDanbooru to get image tags for a list of image paths
     *
     * @param imagePaths List of images to tag
     * @return Returns the full DeepDanbooru output
     */
    private static String runDeepdanbooru(ArrayList<String> imagePaths) {
        String out = "";

        String ddcmd = IngesterConfig.getPythonExePath() + " -m deepdanbooru evaluate --project-path \"" + IngesterConfig.getDdProjectPath() + "\" --allow-gpu";
        for (String path : imagePaths) {
            if (path.contains("\"")) {
                System.out.println("ERROR: Illegal character '\"' found in path");
                continue;
            }
            ddcmd += " \"" + path.replace("`", "``") + "\" ";
        }

        // Run DeepDanbooru
        try {
            Process process = Runtime.getRuntime().exec("powershell.exe");

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(process.getInputStream()));
            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(process.getErrorStream()));

            BufferedWriter cmdWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

            cmdWriter.write(ddcmd + "\n");
            cmdWriter.flush();
            cmdWriter.close();

            // Get STD out
            String line;
            while ((line = stdInput.readLine()) != null) {
                out += line + "\n";
            }

            // Get err out
            String errLine;
            String errOut = "";
            while ((errLine = stdError.readLine()) != null) {
                errOut += errLine + "\n";
            }

            // Check if there were any errors while running the command
            if (!errOut.equals("")) {
                if (errOut.contains("not exist")) {
                    System.out.println("WARNING: Filed passed into DD did not exist: ");
                    System.out.println(errOut);
                } else if (!errOut.contains("Cleanup called")) {
                    System.out.println("WARNING: DD did not exit successfully: ");
                    System.out.println(errOut);
                }
            }

            int exitVal = process.waitFor();
            if (exitVal != 0) {
                System.out.println("ERROR: DeepDanbooru did not exit successfully: \n" + out);
            }

            stdInput.close();
            stdError.close();
        } catch (IOException | InterruptedException e) {
            System.out.println("ERROR: Error encountered while running DeepDanbooru");
        }

        return out;
    }

    /**
     * Parses a DeepDanbooru input string into a list of {@link ImageWithTags}
     *
     * @param data         DeepDanbooru output
     * @param tagProbThres Tag probability threshold. Any tags with a confidence below this number will not be added to
     *                     the tag list
     * @return list of {@link ImageWithTags}
     */
    private static ArrayList<ImageWithTags> parseDDOutput(String data, double tagProbThres) {

        // Split output by image
        String[] sections = data.split("Tags of ");
        ArrayList<ImageWithTags> images = new ArrayList<>();

        // Loop through the data for each image
        for (String section : sections) {
            if (section.equals("") || section.toLowerCase().contains("windows") || section.toLowerCase().contains("bash")) {
                continue;
            }

            String[] lines = section.split("\n");
            String tmp = lines[0].substring(0, lines[0].length() - 1);
            String pathString = null;
            try {
                pathString = new File(tmp).getCanonicalPath();
            } catch (IOException e) {
                System.out.println("ERROR: could not get canonical path for \"" + tmp + "\"");
            }
            ArrayList<String> tags = new ArrayList<>();
            for (String line : lines) {
                if (!line.startsWith("(")) {
                    continue;
                }

                line = line.replace("(", "");
                line = line.replace(")", "");
                String[] splitLine = line.split(" ");
                double prob = Double.parseDouble(splitLine[0]);
                String tagName = splitLine[1];

                if (prob >= tagProbThres) {
                    tags.add(tagName);
                }
            }

            images.add(new ImageWithTags(pathString, tags));
        }

        return images;
    }

    /**
     * Runs {@code parseDDOutput()} and {@code runDeepdanbooru()} together
     *
     * @param imagePaths   List of image paths to get tags for
     * @param tagProbThres tagProbThres Tag probability threshold. Any tags with a confidence below this number will not be added to
     *                     the tag list
     * @return
     */
    public static ArrayList<ImageWithTags> getTagsForImages(ArrayList<String> imagePaths, double tagProbThres) {
        return parseDDOutput(runDeepdanbooru(imagePaths), tagProbThres);
    }

}
