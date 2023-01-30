import {ResultPageElement, ResultsPage} from './resultsTemplates';
import {apiAddr, default_images_per_page} from '../constants';

var resultPage = undefined;

window.onDocLoad = function(){
    let params = (new URL(document.location)).searchParams;
    let searchString = params.get("search");
    resultPage = new ResultsPage();
    if(searchString){
        sendSearchRequest(searchString);
    }
}

const handThumbResponse = function(data){
    let md5 = data["md5"];
    let filename = data["filename"];
    let b64Thumb = data["thumb_base64"];

    resultPage.addResultElement(new ResultPageElement(md5, filename, b64Thumb));
}

const handleSearchResponse = function(data){
    document.getElementById("results-div").appendChild(resultPage);

    data.forEach((obj) => {
        let md5 = obj["md5"];
        let filename = obj["filename"];
        let thumbHeight = 200;

        fetch(`http://${apiAddr}/images/get_thumbnail?md5=${md5}&filename=${filename}&thumb_height=${thumbHeight}`).then((response) =>{
            return response.json();
        }
        ).then(handThumbResponse);

    });
}

const sendSearchRequest = function(tagsString){    
    // query API
    let requestString = `http://${apiAddr}/search_images/by_tag/page?tags=${tagsString}&page_num=0&results_per_page=${default_images_per_page}` +
    `&include_thumb=false`;
    console.log("Request: " + requestString);

    // send request
    console.log("sending request")
    fetch(requestString).then((response) =>{
        return response.json();
    }
    ).then(handleSearchResponse);  
    
}