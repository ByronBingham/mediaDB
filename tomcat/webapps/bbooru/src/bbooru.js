goToResults = function() {
    // get tags
    tags = document.getElementById("tags-search").value.split(' ');
    filteredTags = [];
    tags.forEach(tag => {
        if(tag !== ''){
            filteredTags.push(tag);
        }
    });
    tagsString = filteredTags.join(',');

    window.location=`/bbooru/resultsPage.html?search=` + tagsString;
}