/**
 * File with utility functions
 */

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