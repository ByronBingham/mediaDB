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

export function setNswfCookie(bool){
    document.cookie = "nsfw=" + bool.toString() + ";";
}

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