import { LitElement, html } from 'lit-element';

/**
 * Template for a home/index page
 */
export class IndexPage extends LitElement {

    constructor(){
        super();
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
					<a href="/${webapp_name}/tagEditor.html" style="color: var(--accent-color-primary);"><b>Tag Editor</b></a>
					<a href="" style="color: var(--accent-color-primary);">My Account</a>
				</div>
				<search-bar></search-bar>
        </div>`
    }

}

// Register elements
customElements.define('index-page', IndexPage);