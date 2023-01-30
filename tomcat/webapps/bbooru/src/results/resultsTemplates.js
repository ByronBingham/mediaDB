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
        window.location = `/bbooru/imagePage.html?md5=${this.md5}&filename=${this.filename}`;
    }

    render(){
        return html`<div>
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
        return html`<div>
            ${this.resultElements}
        </div>`
    }
}


customElements.define('result-page-element', ResultPageElement);
customElements.define('result-page', ResultsPage);