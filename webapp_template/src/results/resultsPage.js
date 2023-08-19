/**
 * File to store functions used in the results page
 */

import {ResultPageElement, ResultsPage} from './resultsTemplates';
import { getNswfCookie, getAscDescCookie, getExtraUrlParamQueries } from '../util';

var resultPage = undefined;

/**
 * Get results for the page from the API when the page loads
 */
window.onload = function(){
    resultPage = new ResultsPage();
    document.getElementById("results-div").appendChild(resultPage);
    doSearch(0);
}

/**
 * Loads a page of results
 * 
 * @param {*} pageOffset This can be used to load subsequent pages of results while doomscrolling
 */
export const doSearch = function(pageOffset){
    let params = (new URL(document.location)).searchParams;
    let searchString = params.get("tags");
    let currentPageNum = params.get("page");
    if(currentPageNum === undefined || currentPageNum === null){
        currentPageNum = 0;
    } else {
        currentPageNum = parseInt(currentPageNum);
    }
    if(searchString){
        sendSearchRequest(searchString, currentPageNum + pageOffset);
    } else {
        sendSearchRequest("", currentPageNum + pageOffset);
    }
}

/**
 * Handes the reponse from the API for a thumbnail
 * 
 * @param {*} data 
 */
const handleThumbResponse = function(id, data){
    let thumbUrl = URL.createObjectURL(data);

    resultPage.addResultElement(new ResultPageElement(id, thumbUrl, resultPage));
}

/**
 * Handes the reponse from the API for page results
 * 
 * @param {*} data 
 */
const handleSearchResponse = function(data){

    data.forEach((obj) => {
        let id = obj["id"];
        let thumbHeight = thumb_height;

        fetch(`${api_addr}/images/get_thumbnail?table_name=${db_table_name}&id=${id}&thumb_height=${thumbHeight}`).then((response) =>{
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
    let nsfw = getNswfCookie();
    
    let extraQueriesString = getExtraUrlParamQueries();

    let requestString = `${api_addr}/search_images/by_tag/page?table_name=${db_table_name}&tags=${tagsString}&page_num=${pageNum}&results_per_page=${default_images_per_page}` +
    `&include_thumb=false&include_nsfw=${nsfw}&asc_desc=${getAscDescCookie()}&${extraQueriesString}`;

    // send request
    fetch(requestString).then((response) =>{
        return response.json();
    }
    ).then(handleSearchResponse);
    
}