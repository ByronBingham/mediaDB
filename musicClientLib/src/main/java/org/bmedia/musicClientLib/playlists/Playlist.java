package org.bmedia.musicClientLib.playlists;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Playlist {

    private String clientPLPath;
    private String playlistTitle;
    private ArrayList<String> songPaths;

    public Playlist() {
        clientPLPath = "";
        playlistTitle = "";
        songPaths = new ArrayList<>();
    }

    public Playlist(String clientPLPath, String playlistTitle, ArrayList<String> songPaths) {
        this.clientPLPath = clientPLPath;
        this.playlistTitle = playlistTitle;
        this.songPaths = songPaths;
    }

    public static Playlist parsePlaylist(String playlistFilePath) {
        ArrayList<String> songPaths = null;

        String fileExt = FilenameUtils.getExtension(playlistFilePath);
        String fileName = FilenameUtils.getBaseName(playlistFilePath);
        switch (fileExt) {
            case "m3u":
            case "m3u8":
                songPaths = parseM3UPlaylist(playlistFilePath, fileExt);
                break;
            default:
                System.out.println("WARNING: ." + fileExt + " is not a supported playlist type");
                return null;
        }

        return new Playlist(playlistFilePath, fileName, songPaths);
    }

    private static ArrayList<String> parseM3UPlaylist(String playlistFilePath, String fileExt) {
        String charset = "";
        ArrayList<String> songPaths = new ArrayList<>();

        switch (fileExt) {
            case "m3u":
                charset = "ASCII";
                break;
            case "m3u8":
                charset = "UTF-8";
                break;
            default:
                System.out.println("WARNING: ." + fileExt + " is not a supported playlist type");
                return null;
        }

        List<String> lines;
        try {
            lines = FileUtils.readLines(new File(playlistFilePath), charset);
        } catch (IOException e) {
            System.out.println("ERROR: Problem parsing '" + playlistFilePath + "' as a M3U/M3U8 playlist");
            return null;
        }

        boolean nextLineIsURI = false;
        for (String line : lines) {
            if (line.startsWith("#EXTINF")) {
                nextLineIsURI = true;
            } else if (nextLineIsURI) {
                // validate song path
                if (!Files.exists(Paths.get(line))) {
                    System.out.println("WARNING: Path '" + line + "' in playlist '" + playlistFilePath + "' does not exist. Skipping...");
                } else {
                    songPaths.add(line);
                }

                nextLineIsURI = false;
            }
        }

        return songPaths;
    }

    public void addSong(String songPath){
        if(!Files.exists(Paths.get(songPath))){
            System.out.println("WARNING: '" + songPath + "' is not a valid file path; it will not be added to this playlist");
            return;
        } else {
            this.songPaths.add(songPath);
        }
    }

    public String getClientPLPath() {
        return clientPLPath;
    }

    public String getPlaylistTitle() {
        return playlistTitle;
    }

    public ArrayList<String> getSongPaths() {
        return songPaths;
    }

}
