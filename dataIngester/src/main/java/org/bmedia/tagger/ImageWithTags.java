package org.bmedia.tagger;


import java.util.ArrayList;
import java.util.HashMap;

public class ImageWithTags {

    private final String pathString;
    private final ArrayList<String> tags;

    public ImageWithTags(String pathString, ArrayList<String> tags) {
        this.pathString = pathString;
        this.tags = tags;
    }

    public String getPathString() {
        return pathString;
    }

    public ArrayList<String> getTags() {
        return tags;
    }
}
