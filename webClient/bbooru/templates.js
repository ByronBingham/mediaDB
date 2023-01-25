//import { LitElement, html } from 'https://unpkg.com/lit-element/lit-element.js?module';

class ResultPageElement extends LitElement {

    constructor(){
        this.imageData = "";
    }

    render(){
        return html`<div>
            <img src="data:image/jpg;base64,${this.imageData}" alt="image" />
        </div>`
    }

}

class ResultsPage extends LitElement {
    constructor(){
        this.resultElements = [];
    }



    render(){
        return html`<div>
            ${this.resultElements}
        </div>`
    }
}

customElements.define('result-page-element', ResultPageElement);
customElements.define('result-page', ResultsPage);