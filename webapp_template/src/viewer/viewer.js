import { ImageViewer, ImageTagList, ImageTag } from "./viewerTemplates";

var imageViewer = undefined;
var imageTagList = undefined;

window.onDocLoad = function(){
    let params = (new URL(document.location)).searchParams;
    let id = params.get("id");
    if(id){
        sendImageRequest(id);
        sendTagsRequest(id);
    }

    imageTagList = new ImageTagList(id);
}

const handleImageResponse = function(data){
    let id = data["id"];
    let imageData = data["image_base64"];
    imageViewer = new ImageViewer(id, imageData);

    document.getElementById("image-viewer").appendChild(imageViewer);
}

const handleTagsResponse = function(data){
    let tagList = data;
    console.log("got tags");
    document.getElementById("tags-sidebar").appendChild(imageTagList);

    tagList.forEach(tagData => {
        imageTagList.addTagElement(tagData);
    });    
}

const sendImageRequest = function(id){    
    // query API
    let requestString = `http://${apiAddr}/images/get_image_full?table_name=${dbTableName}&id=${id}`;
    //console.log("Request: " + requestString);

    // send request
    fetch(requestString).then((response) =>{
        return response.json();
    }
    ).then(handleImageResponse);  
    
}

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