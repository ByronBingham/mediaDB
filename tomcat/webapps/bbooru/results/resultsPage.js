function onDocLoad(){
    console.log("loading stuff");
    params = (new URL(document.location)).searchParams;
    searchString = params.get("search");
    if(searchString){
        sendSearchRequest(searchString);
    }
}

function handThumbResponse(data){
    b64Thumb = data["thumb_base64"];
    //console.log(b64Thumb);

}

function handleSearchResponse(data){
    console.log("response recieved")
    //dataArr = JSON.parse(data)

    data.forEach((obj) => {
        md5 = obj["md5"];
        filename = obj["filename"];
        thumbHeight = 200;

        fetch(`http://${serverAddr}/images/get_thumbnail?md5=${md5}&filename=${filename}&thumb_height=${thumbHeight}`).then((response) =>{
            return response.json();
        }
        ).then(handThumbResponse);

    });
}

function sendSearchRequest(tagsString){

    // query API
    requestString = `http://${serverAddr}/search_images/by_tag/page?tags=${tagsString}&page_num=0&results_per_page=${default_images_per_page}` +
    `&include_thumb=false&thumb_height=${thumb_height}`;
    console.log("Request: " + requestString);

    // send request
    console.log("sending request")
    response = fetch(requestString).then((response) =>{
        return response.json();
    }
    ).then(handleSearchResponse);  
    
}