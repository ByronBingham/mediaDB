import { LitElement, html } from 'lit-element';

export class ResultPageElement extends LitElement {

    constructor(md5, filename, b64image){
        super();
        this.md5 = md5;
        this.filename = filename;
        this.imageData = b64image;
    }

    openImage(){
        console.log("Opening Image with md5=" + this.md5 + " and filename=" + this.filename);
        window.location = `/${webapp_name}/imagePage.html?md5=${this.md5}&filename=${this.filename}`;
    }

    render(){
        return html`
        <link rel="stylesheet" href="template.css">
        <div class="image_flex_item">
            <img src="data:image/jpg;base64,${this.imageData}" alt="image" @click=${this.openImage}/>
        </div>`
    }

}

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


customElements.define('result-page-element', ResultPageElement);
customElements.define('result-page', ResultsPage);