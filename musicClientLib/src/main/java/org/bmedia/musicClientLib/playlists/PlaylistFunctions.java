package org.bmedia.musicClientLib.playlists;

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

        Playlist clientPlaylist;

        URL pathUrl;
        try {
            // Get client playlist
            String fileExt = FilenameUtils.getExtension(playListFilePath);
            pathUrl = new URL(playListFilePath);
            clientPlaylist = Playlist.parsePlaylist(playListFilePath);

            // Get DB



        } catch (MalformedURLException e){

        } catch (IOException e){

        }

    }

    public void updateDBWithPlaylistFolder(String playlistFolderPath){

    }

}
