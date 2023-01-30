import { ImageViewer } from "./viewerTemplates";
import {apiAddr} from '../constants';

var imageViewer = undefined;

window.onDocLoad = function(){
    let params = (new URL(document.location)).searchParams;
    let md5 = params.get("md5");
    let filename = params.get("filename");
    if(md5 && filename){
        sendImageRequest(md5, filename);
    }
}

const handleImageResponse = function(data){
    let md5 = data["md5"];
    let filename = data["filename"];
    let imageData = data["image_base64"];
    imageViewer = new ImageViewer(md5, filename, imageData);

    document.getElementById("image-viewer").appendChild(imageViewer);
}

const sendImageRequest = function(md5, filename){    
    // query API
    let requestString = `http://${apiAddr}/images/get_image_full?md5=${md5}&filename=${filename}`;
    console.log("Request: " + requestString);

    // send request
    console.log("sending request")
    fetch(requestString).then((response) =>{
        return response.json();
    }
    ).then(handleImageResponse);  
    
}