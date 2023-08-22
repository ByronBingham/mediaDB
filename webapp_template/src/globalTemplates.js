/**
 * File to store templates used accross the web UI
 */

import { LitElement, html } from 'lit-element';
import { getExtraUrlParamQueries, getListOfAllTags, getNswfCookie, getUrlParam, setNswfCookie, getAppBaseUrl } from './util';

/**
 * Template search bar
 */
export class SearchBar extends LitElement {

    /**
     * SearchBar constructor
     */
    constructor(){
        super();
        this.tagInput = new TagInput(this.goToResults, this, "Search");
    }

    /**
     * Goes to the results page for this search
     * 
     * @param {*} event Submit event
     * @returns 
     */
    goToResults(tagList){
        let tagsString = tagList.join(',');

        window.location=`/${webapp_name}/resultsPage.html?tags=` + tagsString;
    }

    render(){
        return html`${this.tagInput}`;
    }
}

/**
 * Template topbar. Includes a home button, search bar, and hidden nsfw toggle
 */
export class TopBar extends LitElement {

    /**
     * TopBar contructor
     */
    constructor(){
        super();
        this.nswf = getNswfCookie();
        this.visibility = "hidden";
        if(this.nswf){
            this.visibility = "visible";
        }
    }

    /**
     * Toggles the nsfw value
     */
    toggle(){
        let checkbox = this.shadowRoot.getElementById("nsfw-check");
        if(this.nswf != checkbox.checked) {
            this.nswf = checkbox.checked;
            setNswfCookie(this.nswf);
            location.reload();
        }
    }

    /**
     * Makes nsfw toggle visible
     */
    unhideNsfw(){
        this.visibility = "visible";
        this.requestUpdate();
    }

    render(){
        let url = getAppBaseUrl();
        return html`<link rel="stylesheet" href="template.css">
                    <div class="top-bar">
                        <table><tr>
                            <td><a href="${url}" class="no-underline-link"><p>${webapp_long_name}</p></a></td>
                            <td><search-bar></search-bar></td>
                            <td style="width: 5vw; padding-left: 2vw;" @click=${this.unhideNsfw}>
                                <div>
                                    <link rel="stylesheet" href="template.css">
                                    <input type="checkbox" id="nsfw-check" .checked=${this.nswf} @click=${this.toggle} 
                                        style="visibility: ${this.visibility}; transform: scale(2.5);">
                                </div>
                            </td>
                        </tr></table>
                    </div>`;
    }

}

/**
 * Tempolate page number element. Clicking on this takes the user to the page specified in an instance of this element
 */
export class PageNumber extends LitElement {

    /**
     * PageNumber constructor
     * 
     * @param {*} value Charater to display
     * @param {*} pageNumber Page number this element represents
     * @param {*} url Base url to append page # to
     * @param {*} isCurrent (true/false) if this element represents the current page
     */
    constructor(value, pageNumber, url, isCurrent){
        super();
        this.value = value;
        this.url = url +"&page=" + pageNumber;
        this.isCurrent = isCurrent;
        this.style="display: flex;";
    }

    render(){
        let styleString = "";
        if(this.isCurrent){
            styleString = "style=\"font-weight: bold;\""
        }
        return html`
                    <link rel="stylesheet" href="template.css">
                    <a class="page-number" href="${this.url}" ${this.styleString}>${this.value}</a>`;
    }
}

/**
 * Template page selector element. Contains multiple page numbers that can be selected, as well as page-forward/back arrows
 */
export class PageSelector extends LitElement {

    /**
     * PageSelector constructor
     */
    constructor(){
        super();
        let params = (new URL(document.location)).searchParams;
        let searchString = params.get("tags");
        if(searchString === undefined || searchString === null){
            searchString = "";
        }
        this.currentPageNum = params.get("page");
        if(this.currentPageNum === undefined || this.currentPageNum === null){
            this.currentPageNum = 0;
        } else {
            this.currentPageNum = parseInt(this.currentPageNum);
        }
        this.baseUrl = `/${webapp_name}/resultsPage.html?tags=${searchString}&${getExtraUrlParamQueries()}`

        this.backPage = html``;
        this.backbackPage = html``;
        this.pageBackFive = html``;
        this.pageBackTwo = html``;
        this.pageBackOne = html``;
        this.fwPage = html``;
        this.fwfwPage = html``;
        this.pageFwFive = html``;
        this.pageFwTwo = html``;
        this.pageFwOne = html``;

        let extraQueriesString = getExtraUrlParamQueries();
        let nsfw = getNswfCookie();
        fetch(`${api_addr}/search_images/by_tag/page/count?table_name=${db_table_name}&tags=${searchString}&results_per_page=${default_images_per_page}&include_nsfw=${nsfw}&${extraQueriesString}`).then((response) =>{
            if(response.ok){
                return response.json();
            } else {
                console.log("ERROR fetching page count for search\n" +
                "Tags: " + searchString)
            }
        }
        ).then(this.pageNumCallback.bind(this));
    }

    /**
     * Create page number selector
     * 
     * @param {*} data 
     */
    pageNumCallback(data) {
        let lastPageNum = data["pages"] - 1;

        if(this.currentPageNum > 0){
            this.backPage = new PageNumber("<", (this.currentPageNum - 1), this.baseUrl, false);
        }

        if(this.currentPageNum > 1){
            this.backbackPage = new PageNumber("<<", 0, this.baseUrl, false);
        }

        if(this.currentPageNum > 4){
            this.pageBackFive = new PageNumber(`${this.currentPageNum - 5}`, (this.currentPageNum - 5), this.baseUrl, false);
            this.pageBackFive = html`${this.pageBackFive}<p class="page-number">...</p>`;
        }

        
        if(this.currentPageNum > 1){
            this.pageBackTwo = new PageNumber(`${this.currentPageNum - 2}`, (this.currentPageNum - 2), this.baseUrl, false);
        }

        
        if(this.currentPageNum > 0){
            this.pageBackOne = new PageNumber(`${this.currentPageNum - 1}`, (this.currentPageNum - 1), this.baseUrl, false);
        }

        this.currentPageElement = new PageNumber(this.currentPageNum, this.currentPageNum, this.baseUrl, true);

        
        if(this.currentPageNum < lastPageNum){
            this.fwPage = new PageNumber(">", (this.currentPageNum + 1), this.baseUrl, false);
        }

        
        if(this.currentPageNum < lastPageNum - 1){
            this.fwfwPage = new PageNumber(">>", lastPageNum, this.baseUrl, false);
        }

        
        if(this.currentPageNum < lastPageNum - 4){
            this.pageFwFive = new PageNumber(`${this.currentPageNum + 5}`, (this.currentPageNum + 5), this.baseUrl, false);
            this.pageFwFive = html`<p class="page-number">...</p>${this.pageFwFive}`;
        }

        
        if(this.currentPageNum < lastPageNum - 1){
            this.pageFwTwo = new PageNumber(`${this.currentPageNum + 2}`, this.currentPageNum + 2, `${this.baseUrl}`, false);
        }

        
        if(this.currentPageNum < lastPageNum){
            this.pageFwOne = new PageNumber(`${this.currentPageNum + 1}`, this.currentPageNum + 1, `${this.baseUrl}`, false);
        }

        console.log("Page Selector Done Loading");
        this.requestUpdate();
    }

    render(){
        return html`
                    <link rel="stylesheet" href="template.css">
                    <div class="page-selector">
                        ${this.backbackPage} ${this.backPage} ${this.pageBackFive} ${this.pageBackTwo} ${this.pageBackOne}
                        ${this.currentPageElement}
                        ${this.pageFwOne} ${this.pageFwTwo} ${this.pageFwFive} ${this.fwPage} ${this.fwfwPage}
                    </div>`;
    }


}

/**
 * Template for a tag input. Allows users to input a list of tags while getting tag suggestions
 * 
 * TODO: make less jank...
 */
export class TagInput extends LitElement {

    /**
     * TagInput constructor
     * 
     * @param {*} submitCallback Callback function that should be called when this form is submitted. Callback should expect to take a list of tag names as input
     * @param {*} parent Parent owning the instance of this class
     * @param {*} submitText Text to display on the submit button of this form
     * @param {*} prePopulate If true, takes the tags from the URL parameters of the loaded page and adds them to this form. If false, does not pre-load any tags
     */
    constructor(submitCallback, parent, submitText, prePopulate=true){
        super();
        this.submitText = submitText;
        this.submitCallback = submitCallback.bind(parent);
        this.parent = parent;
        this.searchString = "";
        this.submittedTags = [];
        if(prePopulate){
            let tags = getUrlParam("tags");
            if(tags !== undefined && tags !== null){
                let tagArr = tags.split(",");
                tagArr.forEach((tag) =>{
                    this.submittedTags.push(tag);
                });
            }
        }
        this.tagList = [];
        getListOfAllTags().then(this.updateTagList.bind(this));
    }

    /**
     * Update the list of tags in this object with an input list of tags
     * 
     * @param {*} tagsArray List of tag names
     */
    updateTagList(tagsArray){
        this.tagList = tagsArray;
        this.requestUpdate();
    }

    /**
     * Goes to the results page for this search
     * 
     * @param {*} event Submit event
     * @returns 
     */
    doCallback(event){
        let lastTags = this.shadowRoot.getElementById("tags-search").value.split(' ');
        lastTags = lastTags.concat(this.submittedTags);
        let filteredTags = [];
        let tagMap = new Map();
        lastTags.forEach(tag => {
            if(tag !== ''){
                tagMap.set(tag, 1);
            }
        });
        let tagIter = tagMap.keys();
        for(const tag of tagIter){
            filteredTags.push(tag);
        }
        this.submitCallback(filteredTags);

        this.clearTags();

        // One or both of these prevents the form from refreshing the page...
        event.preventDefault();
        return false;
    }

    /**
     * Clear all tags from this form
     */
    clearTags(){
        this.submittedTags = [];
        this.shadowRoot.getElementById("tags-search").value = "";
        this.requestUpdate();
    }

    /**
     * Called on any keypress. Checks for space and backspace input.
     * 
     * For space, pushes the current word to this form's tag list.
     * For backspace, if the input box is also empty, pops the last tag from this form's tag list and puts it into the input box
     * 
     * @param {*} event Keypress event
     */
    updateTagInput(event){
        let inputElement = this.shadowRoot.getElementById("tags-search");
        if(event.keyCode === 32) { // If space was pressed, check if we can autocomplete
            this.submittedTags.push(inputElement.value.trim());
            inputElement.value = "";
        } else if(event.keyCode === 8) {    // Update suggestions
            if(inputElement.value === ""){
                let lastElement = this.submittedTags.pop();
                if(lastElement === undefined || lastElement === null){
                    inputElement.value = "";
                } else {
                    inputElement.value = lastElement;
                }
            }
        }
        this.requestUpdate();
    }

    render(){
        let options = [];

        this.tagList.forEach(tag => {
            options.push(html`<option value="${tag}">${tag}</option>`);
        });

        return html`
                    <link rel="stylesheet" href="template.css">
                    <form class="results-bar-group" @submit="${this.doCallback}">
                        <label id="tag-input-label" for="tags-search" style="color: var(--accent-color-primary)">${this.submittedTags.join(" ")}</label>
                        <input id="tags-search" name="tags" type="text" placeholder="Ex: blue_sky cloud 1girl" value="${this.searchString}" list="tagsList" autocomplete="off"
                        @keyup="${this.updateTagInput}" @change="${this.updateTagInput}">                        
                        <datalist id="tagsList">
                            ${options}
                        </datalist>
                        <input name="clear" type="button" value="X" @click="${this.clearTags}">
                        <input name="commit" type="submit" value="${this.submitText}">
                    </form>`;
    }
}

// Register elements
customElements.define('search-bar', SearchBar);
customElements.define('top-bar', TopBar);
customElements.define('page-number', PageNumber);
customElements.define('page-selector', PageSelector);
customElements.define('tag-input', TagInput);