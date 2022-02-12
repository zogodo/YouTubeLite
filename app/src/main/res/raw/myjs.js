function ToBlank() {
    var links = document.querySelectorAll("a:not([target=_blank])");
    for (var i = 0; i < links.length; i++) {
        links[i].target = "_blank";
    }
};
window.addEventListener("scroll", ToBlank);
ToBlank();
var but = document.getElementsByClassName("ytp-unmute");
console.log("but:");
console.log(but);
if (but.length > 0) {
    but[0].click();
}
