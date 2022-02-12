function ToBlank() {
    var links = document.querySelectorAll("a:not([target=_blank])");
    for (var i = 0; i < links.length; i++) {
        links[i].target = "_blank";
    }
};
window.addEventListener("scroll", ToBlank);
ToBlank();

function ClickBut(query) {
    var but = document.querySelector(query);
    console.log(query);
    console.log(but);
    if (but != null) {
        but.click();
    }
}
ClickBut(".ytp-unmute");  /*取消静音*/

const QUALITIES =  ['auto', 'tiny', 'small', 'medium', 'large', 'hd720', 'hd1080', 'hd1440', 'hd2160', 'hd2880', 'highres'];
var movie_player = document.getElementById("movie_player");
console.log(movie_player.getPlaybackQuality());
if (movie_player.zz_set == null) {
    movie_player.setPlaybackQualityRange("hd720");
    movie_player.zz_set = true;
}
