import { LitElement, html } from 'lit-element';

export class ResultPageElement extends LitElement {

    constructor(md5, filename, b64image){
        super();
        this.md5 = md5;
        this.filename = filename;
        this.imageData = b64image;
    }

    render(){
        return html`<div>
            <img src="data:image/jpg;base64,${this.imageData}" alt="image" />
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