/**
 * File with utility functions
 */

export function getUrlParam(paramName){
    let params = (new URL(document.location)).searchParams;
    return params.get(paramName);
}

/**
 * Gets the optional search queries from the URL.
 * 
 * @returns "min_width=[...]&min_height=[...]&aspect_ratio=[...]"
 */
export function getExtraUrlParamQueries(){
    let params = (new URL(document.location)).searchParams;
    let extraQueriesList = [];
    if(params.has("min_width")){
        extraQueriesList.push("min_width=" + params.get("min_width"));
    }
    if(params.has("min_height")){
        extraQueriesList.push("min_height=" + params.get("min_height"));
    }
    if(params.has("aspect_ratio")){
        extraQueriesList.push("aspect_ratio=" + params.get("aspect_ratio"));
    }
    return extraQueriesList.join("&");
}

/**
 * Gets the search tags from the URL.
 * 
 * @returns "tags=[tag1],[tag2],..."
 */
export function getTagsQueryString(){
    let params = (new URL(document.location)).searchParams;
    if(params.has("tags")){
        return "tags=" + params.getAll("tags");
    } else {
        return "tags=";
    }
  }

/**
 * Gets the NSFW cookie
 * @returns true/false
 */
export function getNswfCookie(){
    let val = getCookie("nsfw");
    if(val === ""){
        document.cookie = "nsfw=false;";
        return false;
    }
    if(val === 'true'){
        return true;
    } else {
        return false;
    }
}

/**
 * Sets the NSFW cookie
 */
export function setNswfCookie(bool){
    document.cookie = "nsfw=" + bool.toString() + ";";
}

/**
 * Gets the doomScroll cookie
 * @returns true/false
 */
export function getDoomScrollCookie(){
  let val = getCookie("doomScroll");
  if(val === ""){
      document.cookie = "doomScroll=false;";
      return false;
  }
  if(val === 'true'){
      return true;
  } else {
      return false;
  }
}

/**
* Sets the doomScroll cookie
*/
export function setDoomScrollCookie(bool){
  document.cookie = "doomScroll=" + bool.toString() + ";";
}

/**
 * Gets the ascending/descending cookie
 * @returns true/false
 */
export function getAscDescCookie(){
  let val = getCookie("ascDesc");
  if(val === ""){
      document.cookie = "ascDesc=false;";
      return false;
  }
  if(val === 'true'){
      return true;
  } else {
      return false;
  }
}

/**
* Sets the ascending/descending cookie
*/
export function setAscDescCookie(bool){
  document.cookie = "ascDesc=" + bool.toString() + ";";
}

/**
 * Gets a specific browser cookie for this site
 * 
 * @param {*} cname Cookie name
 * @returns The value of the cookie. Returns an empty string if the cookie is not found
 */
export function getCookie(cname) {
    let name = cname + "=";
    let decodedCookie = decodeURIComponent(document.cookie);
    let ca = decodedCookie.split(';');
    for(let i = 0; i <ca.length; i++) {
      let c = ca[i];
      while (c.charAt(0) == ' ') {
        c = c.substring(1);
      }
      if (c.indexOf(name) == 0) {
        return c.substring(name.length, c.length);
      }
    }
    return "";
}