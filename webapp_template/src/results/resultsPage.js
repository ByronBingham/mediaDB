import {ResultPageElement, ResultsPage} from './resultsTemplates';
import { getNswfCookie } from '../util';

var resultPage = undefined;

window.onDocLoad = function(){
    let params = (new URL(document.location)).searchParams;
    let searchString = params.get("search");
    let currentPageNum = params.get("page");
    if(currentPageNum === undefined || currentPageNum === null){
        currentPageNum = 0;
    } else {
        currentPageNum = parseInt(currentPageNum);
    }
    resultPage = new ResultsPage();
    if(searchString){
        sendSearchRequest(searchString, currentPageNum);
    } else {
        sendSearchRequest("", currentPageNum);
    }
}

const handleThumbResponse = function(data){
    let id = data["id"];
    let b64Thumb = data["thumb_base64"];

    resultPage.addResultElement(new ResultPageElement(id, b64Thumb));
}

const handleSearchResponse = function(data){
    document.getElementById("results-div").appendChild(resultPage);

    data.forEach((obj) => {
        let id = obj["id"];
        let thumbHeight = 200;

        fetch(`http://${apiAddr}/images/get_thumbnail?table_name=${dbTableName}&id=${id}&thumb_height=${thumbHeight}`).then((response) =>{
            if(response.ok){
                return response.json();
            } else {
                console.log("ERROR fetching thumbnail for image\n" +
                "ID: " + id)
            }
        }
        ).then(handleThumbResponse);

    });
}

const sendSearchRequest = function(tagsString, pageNum){
    // query API
    let nsfw = getNswfCookie()
    let requestString = `http://${apiAddr}/search_images/by_tag/page?table_name=${dbTableName}&tags=${tagsString}&page_num=${pageNum}&results_per_page=${default_images_per_page}` +
    `&include_thumb=false&include_nsfw=${nsfw}`;
    console.log(requestString);

    // send request
    fetch(requestString).then((response) =>{
        return response.json();
    }
    ).then(handleSearchResponse);  
    
}