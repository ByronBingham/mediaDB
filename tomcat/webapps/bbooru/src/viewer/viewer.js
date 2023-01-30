import { ImageViewer, ImageTagList, ImageTag } from "./viewerTemplates";
import {apiAddr} from '../constants';

var imageViewer = undefined;
var imageTagList = undefined;

window.onDocLoad = function(){
    let params = (new URL(document.location)).searchParams;
    let md5 = params.get("md5");
    let filename = params.get("filename");
    if(md5 && filename){
        sendImageRequest(md5, filename);
        sendTagsRequest(md5, filename);
    }

    imageTagList = new ImageTagList();
}

const handleImageResponse = function(data){
    let md5 = data["md5"];
    let filename = data["filename"];
    let imageData = data["image_base64"];
    imageViewer = new ImageViewer(md5, filename, imageData);

    document.getElementById("image-viewer").appendChild(imageViewer);
}

const handleTagsResponse = function(data){
    let tagList = data;
    console.log("got tags");
    document.getElementById("tags-sidebar").appendChild(imageTagList);

    tagList.forEach(tagData => {
        let tagName = tagData["tag_name"];
        let nsfw = tagData["nsfw"];
        let tagObj = new ImageTag(tagName, nsfw);
        imageTagList.addTagElement(tagObj);
    });    
}

const sendImageRequest = function(md5, filename){    
    // query API
    let requestString = `http://${apiAddr}/images/get_image_full?md5=${md5}&filename=${filename}`;
    console.log("Request: " + requestString);

    // send request
    fetch(requestString).then((response) =>{
        return response.json();
    }
    ).then(handleImageResponse);  
    
}

const sendTagsRequest = function(md5, filename){    
    // query API
    let requestString = `http://${apiAddr}/images/get_tags?md5=${md5}&filename=${filename}`;
    console.log("Request: " + requestString);

    // send request
    fetch(requestString).then((response) =>{
        return response.json();
    }
    ).then(handleTagsResponse);  
    
}