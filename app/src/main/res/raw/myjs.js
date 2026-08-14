function ToBlank() {
    var links = document.querySelectorAll("a:not([target=_blank])");
    for (var i = 0; i < links.length; i++) {
        links[i].target = "_blank";
    }
    return links.length > 0
}
function ToBlankLoop() {
    if (ToBlank()) {
        window.addEventListener("scroll", ToBlank);
        return;
    }
    setTimeout("ToBlankLoop()", 100);
}
ToBlankLoop();

var n = 0;
function ClickBut(query) {
    var but = document.querySelector(query);
    console.log(query + ": " + but);
    if (but != null) {
        but.click();
        return;
    }
    if (n++ > 10) {
        return;
    }
    setTimeout('ClickBut(".ytp-unmute")', 500);
}
ClickBut(".ytp-unmute");  /*取消静音*/

var QUALITIES =  ['auto', 'tiny', 'small', 'medium', 'large', 'hd720', 'hd1080', 'hd1440', 'hd2160', 'hd2880', 'highres'];
var movie_player = document.getElementById("movie_player");
/*console.log(movie_player.getPlaybackQuality());*/
if (movie_player != null && movie_player.zz_set == null) {
    movie_player.setPlaybackQualityRange("hd720");
    movie_player.zz_set = true;
}

function PauseOrPlay() {
    if (movie_player.getPlayerState() == 1) {
        movie_player.pauseVideo();
        return 0;
    } else {
        movie_player.playVideo();
        return 1;
    }
}

function GetVideoInfo() {
    if (!window.location.href.startsWith('https://m.youtube.com/watch?v=')) {
        return "XXX☯YYY☯ZZZ";
    }
    var title = document.querySelectorAll('.slim-video-information-title .yt-core-attributed-string')[0].innerHTML;
    var content = document.querySelectorAll('.slim-owner-channel-name .yt-core-attributed-string')[0].innerHTML;
    var info = title + '☯' + content + '☯' + 'Url';
    return info;
}

function IsVideoPlaying() {
    var player = document.getElementById("movie_player");
    if (player != null && typeof player.getPlayerState === 'function') {
        var state = player.getPlayerState();
        return state == 1 || state == 3;
    }
    return false;
}

function MuteAd() {
    var player = document.getElementById("movie_player");
    if (player == null) { return; }
    var adEl = document.querySelector('.ytp-ad-player-overlay, .ytp-ad-action-interrupt, .ytp-ad-image-overlay');
    if (adEl == null) { return; }
    player.mute();
    if (typeof player.skipAd === 'function') {
        player.skipAd();
    }
}
function SkipVideoAd() {
    var btn = document.querySelector('.ytp-skip-ad-button, .ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button-modern');
    if (btn != null) { btn.click(); }
    var close = document.querySelector('.ytp-ad-overlay-close-button');
    if (close != null) { close.click(); }
    MuteAd();
}
setInterval(SkipVideoAd, 300);

var adCss = document.createElement('style');
adCss.type = 'text/css';
adCss.innerHTML = '.chip-bar,.ytp-ad-overlay-container,.ytp-ad-text-overlay,.video-ads,#player-ads,ytd-ad-slot-renderer,.ytp-ad-action-interrupt,.ytp-ad-player-overlay,.ytp-ad-module,.ytp-ad-image-overlay,.ytp-ad-skip-button-container{display:none!important}';
document.head.appendChild(adCss);


