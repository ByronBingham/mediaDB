import { LitElement, html } from 'lit-element';

export class ImageViewer extends LitElement {

    constructor(md5, filename, imageData){
        super();
        this.md5 = md5;
        this.filename = filename;
        this.imageData = imageData;
    }

    viewImageFullScreen(){

    }

    render(){
        return html`<div>
                        <img src="data:image/jpg;base64,${this.imageData}" alt="${this.filename}" @click=${this.viewImageFullScreen}/>
                    </div>`
    }

}

export class ImageTagList extends LitElement {
    constructor(){
        super();
        this.tagElements = [];
    }

    addTagElement(element){
        this.tagElements.push(element);
        this.requestUpdate();
    }

    render(){
        return html`<div>
                        ${this.tagElements}
                    </div>`;
    }
}

export class ImageTag extends LitElement {
    constructor(tagName, nsfw){
        super();
        this.name = tagName;
        this.nsfw = nsfw;
    }

    searchTag(){
        window.location=`/bbooru/resultsPage.html?search=${this.name}`;
    }

    render(){
        return html`<p @click=${this.searchTag}>${this.name}</p>`;
    }
}

customElements.define('image-viewer', ImageViewer);
customElements.define('image-tag-list', ImageTagList);
customElements.define('image-tag', ImageTag);