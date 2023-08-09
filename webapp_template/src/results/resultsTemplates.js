/**
 * File to store templates used in the results page
 */

import { LitElement, html } from 'lit-element';
import { getDoomScrollCookie, setDoomScrollCookie } from '../util';
import { doSearch } from './resultsPage.js';

/**
 * Template for an individual page result
 */
export class ResultPageElement extends LitElement {

    constructor(id, imageUrl, resultPage){
        super();
        this.id = id;
        this.imageUrl = imageUrl;
        this.editting = resultPage.getEditing();
        this.selected = false;

        resultPage.decrementLoading();
    }

    /**
     * Open image veiwer for this image
     */
    openImage(){
        window.location = `/${webapp_name}/imagePage.html?id=${this.id}`;
    }

    /**
     * Show edting elements
     */
    editImage(){
        this.editting = true;
        this.selected = false;
        this.requestUpdate();
    }

    /**
     * Hide and reset edting elements
     */
    stopEdittingImage(){
        this.editting = false;
        this.selected = false;
        this.requestUpdate();
    }

    /**
     * Get whether this image is selected for mass tag edting
     */
    getSelected(){
        return this.selected;
    }

    /**
     * Update whether this image is selected for mass tag edting
     */
    setSelected(){
        this.selected = this.shadowRoot.getElementById("selected-checkbox").checked;
    }

    /**
     * Get the ID of this result element
     * 
     * @returns ID
     */
    getId(){
        return this.id;
    }

    render(){
        if(this.editting){
            return html`
            <link rel="stylesheet" href="template.css">
            <div class="image_flex_item">
                <div style="position:relative;">
                    <img src="${this.imageUrl}" alt="image"/>
                    <input type="checkbox" class="result-image-checkbox" id="selected-checkbox" @change=${this.setSelected}>
                </div>
            </div>`;
        } else {
            return html`
            <link rel="stylesheet" href="template.css">
            <div class="image_flex_item">
                <img src="${this.imageUrl}" alt="image"  @click=${this.openImage}/>
            </div>`;
        }
    }

}

/**
 * Template for the results page
 */
export class ResultsPage extends LitElement {
    
    constructor(){
        super();
        this.resultElements = [];
        this.editing = false;
        this.pageOffset = 1;
        this.loadingElement = default_images_per_page;
        this.doomScrollButtonText = (getDoomScrollCookie())?"Mode: Doomscroll":"Mode: Page";
    }

    /**
     * Decrement the hold/lock variable for auto-loading image results
     */
    decrementLoading(){
        this.loadingElement -= 1;
        if(this.loadingElement < 0){
            this.loadingElement = 0;
        }
        if(!this.loadingElement){
            this.doScroll();
        }
    }

    /**
     * Adds a result element to the results page list
     * 
     * @param {*} resEl Result element to add
     */
    addResultElement(resEl){
        this.resultElements.push(resEl);
        this.requestUpdate();
    }

    /**
     * Returns the edting state
     * 
     * @returns True if edting
     */
    getEditing(){
        return this.editing;
    }

    /**
     * Toggle tag mass editing
     */
    toggleEditing(){
        this.editing = !this.editing;
        this.resultElements.forEach(element => {
            if(this.editing){
                element.editImage();
            } else {
                element.stopEdittingImage();
            }
        });
        this.requestUpdate();
    }

    /**
     * Submit mass tags to the API
     * 
     * @param {*} event Submit event from tags form
     * @returns 
     */
    submitTags(event){
        // Get selected ids
        let ids = [];
        this.resultElements.forEach(element => {
            if(element.getSelected()){
                ids.push(element.getId());
            }
        });
        let idsString = ids.join(",");

        // Get list of tags
        let tagString = this.shadowRoot.getElementById("tag-list").value;
        let tagList = tagString.replaceAll(" ", ",");

        fetch(`${apiAddr}/images/add_tags?table_name=${dbTableName}&id=${idsString}&tag_names=${tagList}`).then((response) => {
            if(response.ok){
                console.log("Successfully mass-added tags to images");
                this.shadowRoot.getElementById("tag-list").value = "";
                this.resultElements.forEach(element => {
                    element.stopEdittingImage();
                });
            } else {
                console.log("ERROR adding tags to images");
            }
        });

        this.requestUpdate();
        
        // One or both of these prevents the form from refreshing the page...
        event.preventDefault();
        return false;
    }

    /**
     * Toggle doomscroll
     */
    toggleDoomScroll(){
        setDoomScrollCookie(!getDoomScrollCookie());
        this.doomScrollButtonText = (getDoomScrollCookie())?"Mode: Doomscroll":"Mode: Page";
        this.doScroll();
        this.requestUpdate();
    }

    /**
     * Handle auto-loading for doomscrolling based on the user's scroll position
     */
    doScroll(){
        // Only handle scrolling if doomscrolling
        if(getDoomScrollCookie() && !this.loadingElement){
            let scrollPosition = this.shadowRoot.getElementById("result-list").scrollTop;
            let scrollHeight = this.shadowRoot.getElementById("result-list").scrollHeight;

            // If close enough to bottom, load more images
            // TODO: make the threshold number below a variable
            if(scrollPosition > scrollHeight - this.shadowRoot.getElementById("result-list").offsetHeight - 500 && !this.loadingElement){
                this.loadingElement += default_images_per_page;
                doSearch(this.pageOffset);
                this.pageOffset += 1;
            }
        }
    }

    render(){
        if(this.editing){
            return html`
            <link rel="stylesheet" href="template.css">
            <div class="results-edit-toggle">
                <button @click=${this.toggleEditing}>Close Editor</button>
                <form @submit="${this.submitTags}">
                    <input type="text" id="tag-list">
                    <input name="commit" type="submit" value="Submit">
                </form>
                <button @click=${this.toggleDoomScroll} style="align: right;">${this.doomScrollButtonText}</button>
            </div>
            <div class="image_flex" id="result-list" @scroll=${this.doScroll}>
                ${this.resultElements}
            </div>`
        } else {
            return html`
            <link rel="stylesheet" href="template.css">
            <div class="results-edit-toggle">
                <button @click=${this.toggleEditing}>Edit Tags</button>
                <button @click=${this.toggleDoomScroll}>${this.doomScrollButtonText}</button>
            </div>
            <div class="image_flex" id="result-list" @scroll=${this.doScroll}>
                ${this.resultElements}
            </div>`
        }
    }
}

// Register elements
customElements.define('result-page-element', ResultPageElement);
customElements.define('result-page', ResultsPage);