import { LitElement, html } from 'lit-element';

export class ImageViewer extends LitElement {

    constructor(md5, filename, imageData){
        super();
        this.md5 = md5;
        this.filename = filename;
        this.imageData = imageData;
    }

    viewImageFullScreen(){
        // TODO: implement
    }

    render(){
        return html`<div style="height: 100%; width: 100%;">
                        <img style="object-fit: contain; height: 100%; width: 100%;" src="data:image/jpg;base64,${this.imageData}" alt="${this.filename}" @click=${this.viewImageFullScreen}/>
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
        window.location=`/${webapp_name}/resultsPage.html?search=${this.name}`;
    }

    render(){
        return html`
        <link rel="stylesheet" href="template.css">
        <p class="tag" @click=${this.searchTag}>${this.name}</p>`;
    }
}

customElements.define('image-viewer', ImageViewer);
customElements.define('image-tag-list', ImageTagList);
customElements.define('image-tag', ImageTag);