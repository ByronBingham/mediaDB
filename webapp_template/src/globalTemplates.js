/**
 * File to store templates used accross the web UI
 */

import { LitElement, html } from 'lit-element';
import { getExtraUrlParamQueries, getNswfCookie, getUrlParam, setNswfCookie } from './util';

/**
 * Template search bar
 */
export class SearchBar extends LitElement {
    constructor(){
        super();
        this.searchString = "";
        let tags = getUrlParam("tags");
        if(tags !== undefined && tags !== null){
            let tagArr = tags.split(",");
            this.searchString = tagArr.join(" ");
        }
    }

    goToResults(event){
        let tags = this.shadowRoot.getElementById("tags-search").value.split(' ');
        let filteredTags = [];
        tags.forEach(tag => {
            if(tag !== ''){
                filteredTags.push(tag);
            }
        });
        let tagsString = filteredTags.join(',');

        window.location=`/${webapp_name}/resultsPage.html?tags=` + tagsString;

        // One or both of these prevents the form from refreshing the page...
        event.preventDefault();
        return false;
    }

    render(){
        return html`<form @submit="${this.goToResults}">
                        <input id="tags-search" name="tags" type="text" placeholder="Ex: blue_sky cloud 1girl" value="${this.searchString}">
                        <input name="commit" type="submit" value="Search">
                        <br><br>
                    </form>`;
    }
}

/**
 * Template topbar. Includes a home button, search bar, and hidden nsfw toggle
 */
export class TopBar extends LitElement {
    constructor(){
        super();
        this.nswf = getNswfCookie();
        this.visibility = "hidden";
        if(this.nswf){
            this.visibility = "visible";
        }
    }

    goToHome(){
        window.location=`/${webapp_name}`;
    }

    toggle(){
        let checkbox = this.shadowRoot.getElementById("nsfw-check");
        this.nswf = checkbox.checked;
        setNswfCookie(this.nswf);
    }

    unhideNsfw(){
        this.visibility = "visible";
        this.requestUpdate();
    }

    render(){
        return html`<link rel="stylesheet" href="template.css">
                    <div class="top-bar">
                        <table><tr>
                            <td><p @click=${this.goToHome}>${webapp_long_name}</p></td>
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
        fetch(`${apiAddr}/search_images/by_tag/page/count?table_name=${dbTableName}&tags=${searchString}&results_per_page=${default_images_per_page}&include_nsfw=${nsfw}&${extraQueriesString}`).then((response) =>{
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

// Register elements
customElements.define('search-bar', SearchBar);
customElements.define('top-bar', TopBar);
customElements.define('page-number', PageNumber);
customElements.define('page-selector', PageSelector);