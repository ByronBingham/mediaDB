package org.bmedia.musicClientLib.playlists;

import io.github.borewit.lizzy.playlist.Playlist;
import io.github.borewit.lizzy.playlist.SpecificPlaylist;
import io.github.borewit.lizzy.playlist.SpecificPlaylistFactory;
import io.github.borewit.lizzy.playlist.m3u.M3U;
import io.github.borewit.lizzy.playlist.m3u.M3U8;
import io.github.borewit.lizzy.playlist.m3u.Resource;
import io.github.borewit.lizzy.playlist.xml.asx.Asx;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class PlaylistFunctions {

    public boolean addSongToPlayList(String playlistFilePath, String songFilePath){

        return false;
    }

    public boolean removeSongFromPlayList(String playlistFilePath, String songFilePath){

        return false;
    }

    public void updateDBWithPlaylist(String playListFilePath){

        SpecificPlaylist clientPlaylist;
        URL pathUrl;
        try {
            String fileExt = FilenameUtils.getExtension(playListFilePath);
            pathUrl = new URL(playListFilePath);
            clientPlaylist = SpecificPlaylistFactory.getInstance().readFrom(pathUrl);
            List<Resource> playlistResources;

            switch (fileExt){
                case ".m3u8":
                    M3U8 m3u8 = (M3U8) clientPlaylist;
                    playlistResources = m3u8.getResources();
                    break;
                case ".m3u":
                    M3U m3u = (M3U) clientPlaylist;
                    playlistResources = m3u.getResources();
                    break;
                case ".pls":
                    M3U8 m3U8 = (M3U8) clientPlaylist;
                    playlistResources = m3U8.getResources();
                    break;
                default:
                    System.out.println("WARNING: ");
            }



        } catch (MalformedURLException e){

        } catch (IOException e){

        }

    }

    public void updateDBWithPlaylistFolder(String playlistFolderPath){

    }

}
