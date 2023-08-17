/**
 * File to store templates used in image viewer page
 */

import { LitElement, html } from 'lit-element';
import { TagInput } from '../globalTemplates';

/**
 * Template for the image viewer page
 */
export class ImageViewer extends LitElement {

    constructor(id, imageUrl){
        super();
        this.id = id;
        this.imageUrl = imageUrl;
    }

    viewImageFullScreen(){
        // TODO: implement
    }

    render(){
        return html`<div style="height: 100%; width: 100%;">
                        <img style="object-fit: contain; height: 100%; width: 100%;" src="${this.imageUrl}" alt="${this.filename}" @click=${this.viewImageFullScreen}/>
                    </div>`
    }

}

/**
 * Template for control bar with image viewer settings
 */
export class ImageTagConrolBar extends LitElement {
    constructor(id, imageTagList){
        super();
        this.id = id;
        this.imageTagList = imageTagList;
        this.tagInput = new TagInput(this.submitAddTagForm, this, "Add Tag", false);
    }

    submitAddTagForm(tagList){
        tagList.forEach((tagName) =>{
            fetch(`${apiAddr}/images/add_tag?table_name=${dbTableName}&id=${this.id}&tag_name=${tagName}`).then((response) => {
                if(response.ok){
                    this.imageTagList.addTagElement({"tag_name": tagName, "nsfw": false});
                } else {
                    console.log("ERROR adding tag to image");
                }
            });
        });        
    }

    render(){
        return html`${this.tagInput}`;
    }
}

/**
 * Template tag list for the image viewer page
 */
export class ImageTagList extends LitElement {
    constructor(id){
        super();
        this.tagData = [];
        this.tagElements = [];
        this.editTagElements = [];
        this.editing = false;
        this.id = id;
        this.tagControlBar = new ImageTagConrolBar(this.id, this);
    }

    addTagElement(data){
        let tagName = data["tag_name"];
        let nsfw = data["nsfw"];
        let tagObj = new ImageTag(tagName, nsfw);
        let editTagObj = new EditTagElement(tagName, nsfw, this, this.id);

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

/**
 * Template for individual tag element in image viewer tag list
 */
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

/**
 * Template for individual, editable tag element in image viewer tag list
 */
export class EditTagElement extends LitElement {
    constructor(tagName, nsfw, imageTagList, id){
        super();
        this.name = tagName;
        this.nsfw = nsfw;
        this.imageTagList = imageTagList;
        this.id = id;
    }

    getTagName(){
        return this.name;
    }

    deleteTag(){
        fetch(`${apiAddr}/images/delete_tag?table_name=${dbTableName}&id=${this.id}&tag_name=${this.name}`).then((response) => {
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

// Register elements
customElements.define('image-viewer', ImageViewer);
customElements.define('image-tag-list', ImageTagList);
customElements.define('image-tag', ImageTag);
customElements.define('edit-tag-element', EditTagElement);
customElements.define('image-tag-control-bar', ImageTagConrolBar);