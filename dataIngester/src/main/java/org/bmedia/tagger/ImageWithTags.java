package org.bmedia.tagger;


import java.util.ArrayList;

/**
 * Simple class to store tag data for an image
 */
public class ImageWithTags {

    // Private variables
    private final String pathString;
    private final ArrayList<String> tags;

    /**
     * Main constructor
     *
     * @param pathString Path to image
     * @param tags       List of tags associated with the image
     */
    public ImageWithTags(String pathString, ArrayList<String> tags) {
        this.pathString = pathString;
        this.tags = tags;
    }

    /**
     * Gets the path of the image
     *
     * @return Path to the imaage
     */
    public String getPathString() {
        return pathString;
    }

    /**
     * Gets the list of tags associated with the image
     *
     * @return List of tags
     */
    public ArrayList<String> getTags() {
        return tags;
    }
}
