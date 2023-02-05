import { LitElement, html } from 'lit-element';


export class IndexPage extends LitElement {

    constructor(md5, filename, imageData){
        super();
        this.md5 = md5;
        this.filename = filename;
        this.imageData = imageData;
    }

    viewImageFullScreen(){

    }

    render(){
        return html`<link rel="stylesheet" href="template.css">
		<br><br><br>
		<div id="static-index" class="center">
			<h1 style="font-size: 4em; color: var(--accent-color-primary);">${webapp_long_name}</a></h1><br>
				<div class="space" id="links" style="margin-bottom: 10px;">
					<a href="/${webapp_name}/resultsPage.html?search=" style="color: var(--accent-color-primary);"><b>Browse All</b></a>
					<a href="/${webapp_name}/" style="color: var(--accent-color-primary);"><b>Tag Editor</b></a>
					<a href="" style="color: var(--accent-color-primary);">My Account</a>
				</div>
			<form action="javascript:goToResults()">
				<input id="tags-search" name="tags" type="text" placeholder="Ex: blue_sky cloud 1girl">
				<input name="commit" type="submit" value="Search"><br><br>
			</form>
        </div>`
    }

}

customElements.define('index-page', IndexPage);