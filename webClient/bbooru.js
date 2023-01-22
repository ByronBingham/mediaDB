getResults = async() => {
    console.log("here");
    // get tags
    tags = document.getElementById("tags-search").value.split(' ');
    filteredTags = []
    tags.forEach(tag => {
        if(tag !== ''){
            filteredTags.push(tag);
        }
    });
    tagsString = filteredTags.join(',')


    requestString = `http://${serverAddr}/search_images/by_tag/page?tags=${tagsString}&page_num=0&results_per_page=${default_images_per_page}` +
    `&include_thumb=true&thumb_height=${thumb_height}`;
    console.log(requestString);
    response = await fetch(requestString);
    
    responseJson = await response.json();

    console.log(responseJson);
    //TODO: https://stackoverflow.com/questions/17502071/transfer-data-from-one-html-file-to-another
    location.href="file:///D:/Documents/_Programming/_GitRepos/mediaDB/webClient/bbooru/resultsPage.html";
}