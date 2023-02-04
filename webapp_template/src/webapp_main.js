import {webapp_name} from './constants.js';

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

    window.location= `${webapp_name}/resultsPage.html?search=` + tagsString;
}