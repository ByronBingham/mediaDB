/**
 * File to store templates used in the results page
 */

import { LitElement, html } from 'lit-element';

/**
 * Template for an individual page result
 */
export class ResultPageElement extends LitElement {

    constructor(id, imageUrl){
        super();
        this.id = id;
        this.imageUrl = imageUrl;
        this.editting = true;
        this.selected = false;
    }

    openImage(){
        console.log("Opening Image with id=" + this.id);
        window.location = `/${webapp_name}/imagePage.html?id=${this.id}`;
    }

    editImage(){
        this.editting = true;
        this.selected = false;
        this.requestUpdate();
    }

    stopEdittingImage(){
        this.editting = false;
        this.selected = false;
        this.requestUpdate();
    }

    getSelected(){
        return this.selected;
    }

    setSelected(){
        this.selected = this.shadowRoot.getElementById("selected-checkbox").checked;
    }

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
    }

    addResultElement(resEl){
        this.resultElements.push(resEl);
        this.requestUpdate();
    }

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
            </div>
            <div class="image_flex">
                ${this.resultElements}
            </div>`
        } else {
            return html`
            <link rel="stylesheet" href="template.css">
            <div class="results-edit-toggle">
                <button @click=${this.toggleEditing}>Edit Tags</button>
            </div>
            <div class="image_flex">
                ${this.resultElements}
            </div>`
        }
    }
}

// Register elements
customElements.define('result-page-element', ResultPageElement);
customElements.define('result-page', ResultsPage);