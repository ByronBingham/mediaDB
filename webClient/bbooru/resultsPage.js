function onDocLoad(){
    console.log("loading stuff");
    params = (new URL(document.location)).searchParams;
    searchString = params.get("search");
    if(searchString){
        sendSearchRequest(searchString);
    }
}

function handleSearchResponse(response){
    console.log("response recieved")
    responseJson = response.json();

    console.log("Response: " + responseJson);
    document.getElementById("test").innerHTML = responseJson;
}

function sendSearchRequest(tagsString){

    // query API
    requestString = `http://${serverAddr}/search_images/by_tag/page?tags=${tagsString}&page_num=0&results_per_page=${default_images_per_page}` +
    `&include_thumb=true&thumb_height=${thumb_height}`;
    console.log("Request: " + requestString);

    // send request
    console.log("sending request")
    response = fetch(requestString).then(handleSearchResponse);  
    
}