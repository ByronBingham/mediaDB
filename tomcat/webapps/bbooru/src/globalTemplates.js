import { LitElement, html } from 'lit-element';
import { getNswfCookie, setNswfCookie } from './util';

export class SearchBar extends LitElement {
    constructor(){
        super();
    }

    goToResults(){
        console.log("stuff");
        let tags = this.shadowRoot.getElementById("tags-search").value.split(' ');
        let filteredTags = [];
        tags.forEach(tag => {
            if(tag !== ''){
                filteredTags.push(tag);
            }
        });
        let tagsString = filteredTags.join(',');

        window.location=`/bbooru/resultsPage.html?search=` + tagsString;
    }

    render(){
        return html`<form">
                        <input id="tags-search" name="tags" type="text" placeholder="Ex: blue_sky cloud 1girl">
                        <input name="commit" type="submit" value="Search" @click=${this.goToResults}><br><br>
                    </form>`;
    }
}

export class TopBar extends LitElement {
    constructor(){
        super();
        this.nswf = getNswfCookie();
        this.visibility = "hidden";
        if(this.nswf){
            this.visibility = "visible";
        }
    }

    goToHome(){
        window.location=`/bbooru`;
    }

    toggle(){
        let checkbox = this.shadowRoot.getElementById("nsfw-check");
        this.nswf = checkbox.checked;
        setNswfCookie(this.nswf);
    }

    unhideNsfw(){
        this.visibility = "visible";
        this.requestUpdate();
    }

    render(){
        return html`<link rel="stylesheet" href="bbooru.css">
                    <div class="top-bar">
                        <table><tr>
                            <td><p @click=${this.goToHome}>BBooru</p></td>
                            <td><search-bar></search-bar></td>
                            <td style="width: 5vw; padding-left: 2vw;" @click=${this.unhideNsfw}>
                                <div>
                                    <link rel="stylesheet" href="bbooru.css">
                                    <input type="checkbox" id="nsfw-check" .checked=${this.nswf} @click=${this.toggle} 
                                        style="visibility: ${this.visibility}; transform: scale(2.5);">
                                </div>
                            </td>
                        </tr></table>
                    </div>`;
    }

}

customElements.define('search-bar', SearchBar);
customElements.define('top-bar', TopBar);