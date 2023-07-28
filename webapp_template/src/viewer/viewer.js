/**
 * This file defines functions used by the image viewer page
 */

import { ImageViewer, ImageTagList, ImageTag } from "./viewerTemplates";

var imageViewer = undefined;
var imageTagList = undefined;

/**
 * When the page loads, get the image and tags from the API
 */
window.onDocLoad = function(){
    let params = (new URL(document.location)).searchParams;
    let id = params.get("id");
    if(id){
        sendImageRequest(id);
        sendTagsRequest(id);
    }

    imageTagList = new ImageTagList(id);
}

/**
 * Handles the API's response for an image
 * 
 * @param {*} data 
 */
const handleImageResponse = function(id, data){
    let imageUrl = URL.createObjectURL(data);
    imageViewer = new ImageViewer(id, imageUrl);

    document.getElementById("image-viewer").appendChild(imageViewer);
}

/**
 * Handles the API's response for an image's tags
 * 
 * @param {*} data 
 */
const handleTagsResponse = function(data){
    let tagList = data;
    console.log("got tags");
    document.getElementById("tags-sidebar").appendChild(imageTagList);

    tagList.forEach(tagData => {
        imageTagList.addTagElement(tagData);
    });    
}

/**
 * Request an image from the API
 * 
 * @param {*} id Image ID to request
 */
const sendImageRequest = function(id){    
    // query API
    let requestString = `http://${apiAddr}/images/get_image_full?table_name=${dbTableName}&id=${id}`;
    //console.log("Request: " + requestString);

    // send request
    fetch(requestString).then((response) =>{
        return response.blob().then(handleImageResponse.bind(null, id));
    }
    );  
    
}

/**
 * Request an image's tags from the API
 * 
 * @param {*} id Image ID to request tags for
 */
const sendTagsRequest = function(id){
    // query API
    let requestString = `http://${apiAddr}/images/get_tags?table_name=${dbTableName}&id=${id}`;
    //console.log("Request: " + requestString);

    // send request
    fetch(requestString).then((response) =>{
        return response.json();
    }
    ).then(handleTagsResponse);  
    
}