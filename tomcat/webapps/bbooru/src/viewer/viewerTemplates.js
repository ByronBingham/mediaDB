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

customElements.define('image-viewer', ImageViewer);