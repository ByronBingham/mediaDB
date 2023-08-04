/**
 * File to store functions used in the results page
 */

import {ResultPageElement, ResultsPage} from './resultsTemplates';
import { getNswfCookie } from '../util';

var resultPage = undefined;

/**
 * Get results for the page from the API when the page loads
 */
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

/**
 * Handes the reponse from the API for a thumbnail
 * 
 * @param {*} data 
 */
const handleThumbResponse = function(id, data){
    let thumbUrl = URL.createObjectURL(data);

    resultPage.addResultElement(new ResultPageElement(id, thumbUrl));
}

/**
 * Handes the reponse from the API for page results
 * 
 * @param {*} data 
 */
const handleSearchResponse = function(data){
    document.getElementById("results-div").appendChild(resultPage);

    data.forEach((obj) => {
        let id = obj["id"];
        let thumbHeight = thumb_height;

        fetch(`${apiAddr}/images/get_thumbnail?table_name=${dbTableName}&id=${id}&thumb_height=${thumbHeight}`).then((response) =>{
            if(response.ok){
                response.blob().then(handleThumbResponse.bind(null, id));
            } else {
                console.log("ERROR fetching thumbnail for image\n" +
                "ID: " + id);
            }
        });

    });
}

/**
 * Sends an API request for page results
 * 
 * @param {*} tagsString The search string including a list (space delimited) of tags to search for
 * @param {*} pageNum Page number to get results for
 */
const sendSearchRequest = function(tagsString, pageNum){
    // query API
    let nsfw = getNswfCookie()
    let requestString = `${apiAddr}/search_images/by_tag/page?table_name=${dbTableName}&tags=${tagsString}&page_num=${pageNum}&results_per_page=${default_images_per_page}` +
    `&include_thumb=false&include_nsfw=${nsfw}`;

    // send request
    fetch(requestString).then((response) =>{
        return response.json();
    }
    ).then(handleSearchResponse);  
    
}