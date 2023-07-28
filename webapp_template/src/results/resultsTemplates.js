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
    }

    openImage(){
        console.log("Opening Image with id=" + this.id);
        window.location = `/${webapp_name}/imagePage.html?id=${this.id}`;
    }

    render(){
        return html`
        <link rel="stylesheet" href="template.css">
        <div class="image_flex_item">
            <img src="${this.imageUrl}" alt="image" @click=${this.openImage}/>
        </div>`
    }

}

/**
 * Template for the results page
 */
export class ResultsPage extends LitElement {
    constructor(){
        super();
        this.resultElements = [];
    }

    addResultElement(resEl){
        this.resultElements.push(resEl);
        this.requestUpdate();
    }

    render(){
        return html`
        <link rel="stylesheet" href="template.css">
        <div class="image_flex">
            ${this.resultElements}
        </div>`
    }
}

// Register elements
customElements.define('result-page-element', ResultPageElement);
customElements.define('result-page', ResultsPage);