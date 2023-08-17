/**
 * File to store templates used in the tag editor page
 */

import { LitElement, html } from 'lit-element';

/**
 * Template for a tag editor page. Includes a top bar with controls and a editable list of tags
 */
export class TagEditor extends LitElement {
    constructor(){
        super();
        this.tagList = new TagList();
        this.controlBar = new TagConrolBar(this.tagList);
    }

    addTagElement(element){
        this.tagElements.push(element);
        this.requestUpdate();
    }

    render(){
        return html`<div>
                        ${this.controlBar}
                        ${this.tagList}
                    </div>`;
    }
}

/**
 * Template control bar for the tag editor page
 */
export class TagConrolBar extends LitElement {
    constructor(tagList){
        super();

        this.tagList = tagList;
    }

    /**
     * Opens the add tag form
     */
    openAddTagForm(){
        this.shadowRoot.getElementById("add-tag-button").style.display = "none";
        this.shadowRoot.getElementById("add-tag-form").style.display = "inline";

        this.requestUpdate();
    }

    /**
     * Closes the add tag form
     */
    closeAddTagForm(){
        this.shadowRoot.getElementById("add-tag-button").style.display = "inline";
        this.shadowRoot.getElementById("add-tag-form").style.display = "none";

        this.clearAddTagForm();
        this.requestUpdate();
    }

    /**
     * Clears data from the tag adding form
     */
    clearAddTagForm(){
        this.shadowRoot.getElementById("tag-name-txt").value = "";
        this.shadowRoot.getElementById("nsfw-check").checked = false;
    }

    /**
     * Adds a tag to the DB
     * 
     * @param {*} event Submit event
     * @returns 
     */
    submitAddTagForm(event){
        let tagName = this.shadowRoot.getElementById("tag-name-txt").value;
        let nsfw = this.shadowRoot.getElementById("nsfw-check").checked;
        fetch(`${apiAddr}/tags/add_tag?tag_name=${tagName}&nsfw=${nsfw}`);

        this.closeAddTagForm();
        this.tagList.addTag(tagName, nsfw);

        // One or both of these prevents the form from refreshing the page...
        event.preventDefault();
        return false;     
    }

    render(){
        return html`<div>
                        <button type="button" id="add-tag-button" @click=${this.openAddTagForm}>Add Tag</button>

                        <div id="add-tag-form" style="display: none;">
                            <button type="button" @click=${this.closeAddTagForm}>X</button>
                            <form @submit="${this.submitAddTagForm}">
                                <input type="text" id="tag-name-txt">
                                <input type="checkbox" id="nsfw-check">
                                <input type="submit" value="Submit">
                            </form>
                        </div>
                        
                    </div>`;
    }
}

/**
 * Template tag list for the tag editor page
 */
export class TagList extends LitElement {
    constructor(){
        super();
        this.tagElements = [];
        fetch(`${apiAddr}/tags/get_all_tags`).then((response) =>{
            if(response.ok){
                return response.text();
        } else {
            console.log("ERROR fetching all tags form DB");
        }}).then(this.initTagList.bind(this));
    }

    /**
     * Initializes and populates the tag list
     * 
     * @param {*} text Response string from API including list of all tags
     */
    initTagList(text){        
        text = text.replaceAll("\\", "\\\\");
        let data = JSON.parse(text);

        data.forEach(tag => {
            this.tagElements.push(new TagElement(tag["tag_name"], tag["nsfw"]))
        });
        this.requestUpdate();
    }

    /**
     * Adds a tag element to the tag list
     * 
     * @param {*} tagName Name of tag
     * @param {*} nsfw Whether nsfw or not
     */
    addTag(tagName, nsfw) {
        this.tagElements.push(new TagElement(tagName, nsfw))

        let sortFunction = function(a, b){
            return a.getTagName().localeCompare(b.getTagName());
        }

        this.tagElements.sort(sortFunction);
        this.requestUpdate();
    }

    render(){
        return html`<link rel="stylesheet" href="template.css">
                    <div class="image_flex">
                        ${this.tagElements}
                    </div>`;
    }
}

/**
 * Template for an individual element of the tag list
 */
export class TagElement extends LitElement {
    constructor(tagName, nsfw){
        super();
        this.name = tagName;
        this.nsfw = nsfw;
    }

    /**
     * Gets the tag name of this element
     * 
     * @returns Tag name
     */
    getTagName(){
        return this.name;
    }

    /**
     * Updates the tag in the DB
     */
    updateTag(){
        let nsfwVal = this.shadowRoot.getElementById("nsfw-check").checked;

        fetch(`${apiAddr}/tags/update_tag?tag_name=${this.name}&nsfw=${nsfwVal}`);
    }

    render(){
        return html`
                    <link rel="stylesheet" href="template.css">
                    <div class="tag-edit">
                        <table>
                            <tr><td><p>${this.name}</p></td>
                            <td><input type="checkbox" id="nsfw-check" @click=${this.updateTag} .checked=${this.nsfw}></td></tr>
                        </table>
                    </div>`;
    }
}

// Register elements
customElements.define('tag-editor', TagEditor);
customElements.define('tag-control-bar', TagConrolBar);
customElements.define('tag-list', TagList);
customElements.define('tag-element', TagElement);