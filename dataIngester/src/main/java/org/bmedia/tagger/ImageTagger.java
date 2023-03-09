package org.bmedia.tagger;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class ImageTagger {

    private static String runDeepdanbooru(ArrayList<String> imagePaths){
        ArrayList<String> fullCommand = new ArrayList<>();
        String out = "";

        String ddcmd = "../venv/Scripts/python.exe -m deepdanbooru evaluate --project-path \"../DeepDanbooru\" --allow-gpu";
        for(String path: imagePaths){
            if(path.contains("\"")){
                System.out.println("ERROR: Illegal character '\"' found in path");
                continue;
            }
            ddcmd += " '" + path.replace("'", "''") + "' ";
        }

        if(System.getProperty("os.name").toLowerCase().contains("windows")){
            fullCommand = new ArrayList<>(Arrays.asList(ddcmd));
        } else if(System.getProperty("os.name").toLowerCase().contains("linux")) {
            fullCommand = null;
        } else {
            System.out.println("ERROR: unsupported OS detected");
            return null;
        }

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

            String line;
            while ((line = stdInput.readLine()) != null) {
                out += line + "\n";
            }

            String errLine;
            String errOut = "";
            while ((errLine = stdError.readLine()) != null) {
                errOut += errLine + "\n";
            }
            if(!errOut.equals("")){
                if(errOut.contains("not exist")){
                    System.out.println("WARNING: Filed passed into DD did not exist: ");
                    System.out.println(errOut);
                }else if(!errOut.contains("Cleanup called")) {
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
        } catch (IOException | InterruptedException e){
            System.out.println("ERROR: Error encountered while running DeepDanbooru");
        }

        return out;
    }

    private static ArrayList<ImageWithTags> parseDDOutput(String data, double tagProbThres){
        String[] sections = data.split("Tags of ");
        ArrayList<ImageWithTags> images = new ArrayList<>();

        for(String section: sections){
            if (section.equals("") || section.toLowerCase().contains("windows") || section.toLowerCase().contains("bash")){
                continue;
            }

            String[] lines = section.split("\n");
            String tmp = lines[0].substring(0, lines[0].length() - 1);
            String pathString = null;
            try{
                pathString = new File(tmp).getCanonicalPath();
            } catch (IOException e){
                System.out.println("ERROR: could not get canonical path for \"" + tmp + "\"");
            }
            ArrayList<String> tags = new ArrayList<>();
            for(String line: lines){
                if(!line.startsWith("(")){
                    continue;
                }

                line = line.replace("(", "");
                line = line.replace(")", "");
                String[] splitLine = line.split(" ");
                double prob = Double.parseDouble(splitLine[0]);
                String tagName = splitLine[1];

                if(prob >= tagProbThres) {
                    tags.add(tagName);
                }
            }

            images.add(new ImageWithTags(pathString, tags));
        }

        return images;
    }

    public static ArrayList<ImageWithTags> getTagsForImages(ArrayList<String> imagePaths, double tagProbThres){
        return parseDDOutput(runDeepdanbooru(imagePaths), tagProbThres);
    }

}
