import { LitElement, html } from 'lit-element';

export class TagEditor extends LitElement {
    constructor(){
        super();
        this.controlBar = new TagConrolBar()
        this.tagList = new TagList()
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

export class TagConrolBar extends LitElement {
    constructor(){
        super();
        this.tagElements = [];
    }

    addTagElement(element){
        this.tagElements.push(element);
        this.requestUpdate();
    }

    openAddTagForm(){
        this.shadowRoot.getElementById("add-tag-button").style.display = "none";
        this.shadowRoot.getElementById("add-tag-form").style.display = "inline";

        this.requestUpdate();
    }

    closeAddTagForm(){
        this.shadowRoot.getElementById("add-tag-button").style.display = "inline";
        this.shadowRoot.getElementById("add-tag-form").style.display = "none";

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
        fetch(`http://${apiAddr}/tags/add_tag?tag_name=${tagName}&nsfw=${nsfw}`);

        this.closeAddTagForm();        
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

export class TagList extends LitElement {
    constructor(){
        super();
        this.tagElements = [];
        fetch(`http://${apiAddr}/tags/get_all_tags`).then((response) =>{
            if(response.ok){
                return response.text();
        } else {
            console.log("ERROR fetching page count for search\n" +
            "Tags: " + searchString)
        }}).then(this.initTagList.bind(this));
    }

    initTagList(text){        
        text = text.replaceAll("\\", "\\\\");
        let data = JSON.parse(text);

        data.forEach(tag => {
            this.tagElements.push(new TagElement(tag["tag_name"], tag["nsfw"]))
        });
        this.requestUpdate();
    }

    setTagElementList(tags){
        this.tagElements = tags;
        this.requestUpdate();
    }

    render(){
        return html`<link rel="stylesheet" href="template.css">
                    <div class="image_flex">
                        ${this.tagElements}
                    </div>`;
    }
}

export class TagElement extends LitElement {
    constructor(tagName, nsfw){
        super();
        this.name = tagName;
        this.nsfw = nsfw;
    }

    updateTag(){
        let nsfwVal = this.shadowRoot.getElementById("nsfw-check").checked;
        let cleanName = this.name.replaceAll("\\", "\\\\")

        fetch(`http://${apiAddr}/tags/update_tag?tag_name=${this.name}&nsfw=${nsfwVal}`);
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

customElements.define('tag-editor', TagEditor);
customElements.define('tag-control-bar', TagConrolBar);
customElements.define('tag-list', TagList);
customElements.define('tag-element', TagElement);