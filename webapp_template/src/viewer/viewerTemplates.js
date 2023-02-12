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

export class ImageTagConrolBar extends LitElement {
    constructor(md5, filename, imageTagList){
        super();
        this.md5 = md5;
        this.filename = filename;
        this.imageTagList = imageTagList;
    }

    openAddTagForm(){
        this.shadowRoot.getElementById("add-tag-button").style.display = "none";
        this.shadowRoot.getElementById("add-tag-form").style.display = "inline";

        this.addEventListener("keyup", function(event) {
            if (event.keyCode === 13) {
                this.submitAddTagForm();
            }
        });

        this.requestUpdate();
    }

    closeAddTagForm(){
        this.shadowRoot.getElementById("add-tag-button").style.display = "inline";
        this.shadowRoot.getElementById("add-tag-form").style.display = "none";

        this.removeEventListener("keyup", this);

        this.clearAddTagForm();
        this.requestUpdate();
    }

    clearAddTagForm(){
        this.shadowRoot.getElementById("tag-name-txt").value = "";
        this.shadowRoot.getElementById("nsfw-check").checked = false;
    }

    submitAddTagForm(){
        let tagName = this.shadowRoot.getElementById("tag-name-txt").value;
        let nsfw = this.shadowRoot.getElementById("nsfw-check").checked;
        fetch(`http://${apiAddr}/images/add_tag?table_name=${dbTableName}&md5=${this.md5}&filename=${this.filename}&tag_name=${tagName}&nsfw=${nsfw}`).then((response) => {
            if(response.ok){
                this.imageTagList.addTagElement({"tag_name": tagName, "nsfw": nsfw});
            } else {
                console.log("ERROR adding tag to image");
            }
        });

        this.clearAddTagForm();
    }

    render(){
        return html`<div>
                        <button type="button" id="add-tag-button" @click=${this.openAddTagForm}>Add Tag</button>

                        <div id="add-tag-form" style="display: none;">
                                <button type="button" @click=${this.closeAddTagForm}>X</button>
                                <input type="text" id="tag-name-txt">
                                <input type="checkbox" id="nsfw-check">
                                <button type="button" @click=${this.submitAddTagForm}>Submit</button>
                        </div>
                        
                    </div>`;
    }
}

export class ImageTagList extends LitElement {
    constructor(md5, filename){
        super();
        this.tagData = [];
        this.tagElements = [];
        this.editTagElements = [];
        this.editing = false;
        this.md5 = md5;
        this.filename = filename;
        this.tagControlBar = new ImageTagConrolBar(this.md5, this.filename, this);
    }

    addTagElement(data){
        let tagName = data["tag_name"];
        let nsfw = data["nsfw"];
        let tagObj = new ImageTag(tagName, nsfw);
        let editTagObj = new EditTagElement(tagName, nsfw, this, this.md5, this.filename);

        this.tagElements.push(tagObj);
        this.tagData.push(data);
        this.editTagElements.push(editTagObj);

        let sortFunction = function(a, b){
            return a.getTagName().localeCompare(b.getTagName());
        }

        this.tagElements.sort(sortFunction);
        this.editTagElements.sort(sortFunction);

        this.requestUpdate();
    }

    removeTag(tagName){
        for(let i = 0; i < this.tagData.length; i++){
            if(this.tagData[i]["tag_name"] === tagName){
                this.tagData.splice(i, 1);
                break;
            }
        }
        for(let i = 0; i < this.tagElements.length; i++){
            if(this.tagElements[i].getTagName() === tagName){
                this.tagElements.splice(i, 1);
                break;
            }
        }
        for(let i = 0; i < this.editTagElements.length; i++){
            if(this.editTagElements[i].getTagName() === tagName){
                this.editTagElements.splice(i, 1);
                break;
            }
        }
        this.requestUpdate();
    }

    toggleEditor(){
        if(this.editing){
            this.tagControlBar.closeAddTagForm();
            this.editing = false;
        } else {
            this.editing = true;
        }
        this.requestUpdate();
    }

    render(){
        if(this.editing){
            return html`
                    <div>
                        <button id="editor-toggle" @click=${this.toggleEditor}>Close Editor</button>
                    </div>
                    <div>${this.tagControlBar}</div>
                    <div>
                        ${this.editTagElements}
                    </div>`;
        } else {
            return html`
                    <div>
                        <button id="editor-toggle" @click=${this.toggleEditor}>Open Editor</button>
                    </div>
                    <div>
                        ${this.tagElements}
                    </div>`;
        }        
    }
}

export class ImageTag extends LitElement {
    constructor(tagName, nsfw){
        super();
        this.name = tagName;
        this.nsfw = nsfw;
    }

    getTagName(){
        return this.name;
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

export class EditTagElement extends LitElement {
    constructor(tagName, nsfw, imageTagList, md5, filename){
        super();
        this.name = tagName;
        this.nsfw = nsfw;
        this.imageTagList = imageTagList;
        this.md5 = md5;
        this.filename = filename;
    }

    getTagName(){
        return this.name;
    }

    deleteTag(){
        fetch(`http://${apiAddr}/images/delete_tag?table_name=${dbTableName}&md5=${this.md5}&filename=${this.filename}&tag_name=${this.name}`).then((response) => {
            if(response.ok){
                this.imageTagList.removeTag(this.name);
            } else {
                console.log("ERROR deleting tag from image");
            }
        });
    }

    render(){
        return html`
                    <link rel="stylesheet" href="template.css">
                    <link rel="stylesheet" href="https://www.w3schools.com/w3css/4/w3.css">
                    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css">
                    <div class="tag-edit">
                        <table>
                            <tr><td><p>${this.name}</p></td>
                            <td><button id="delete-tag-button" @click=${this.deleteTag} style="font-size: 1.5em">&#x1F5D1;</button></td></tr>
                        </table>
                    </div>`;
    }
}

customElements.define('image-viewer', ImageViewer);
customElements.define('image-tag-list', ImageTagList);
customElements.define('image-tag', ImageTag);
customElements.define('edit-tag-element', EditTagElement);
customElements.define('image-tag-control-bar', ImageTagConrolBar);