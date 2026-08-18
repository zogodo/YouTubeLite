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

function SetQuality() {
    var player = document.getElementById("movie_player");
    if (player == null) { return; }
    try {
        if (typeof player.setPlaybackQuality === 'function') {
            player.setPlaybackQuality("hd720");
        }
        if (typeof player.setPlaybackQualityRange === 'function') {
            player.setPlaybackQualityRange("hd720");
        }
    } catch (e) { }
}
/*播放器未就绪时重试,直到就绪*/
var qn = 0;
function SetQualityLoop() {
    var player = document.getElementById("movie_player");
    if (player != null && typeof player.getPlayerState === 'function') {
        SetQuality();
        return;
    }
    if (qn++ > 20) { return; }
    setTimeout(SetQualityLoop, 500);
}
SetQualityLoop();
/*新视频加载或状态变化时重新设置,防止被重置*/
document.addEventListener('yt-navigate-finish', SetQuality);
window.addEventListener('onStateChange', SetQuality);


function PauseOrPlay() {
    var player = document.getElementById("movie_player");
    if (player == null) { return 0; }
    if (player.getPlayerState() == 1) {
        player.pauseVideo();
        return 0;
    } else {
        player.playVideo();
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

/*根据图片URL找到其所在链接,找不到则返回当前页URL(可能是视频页自身的缩略图)*/
function FindVideoLink(imageUrl) {
    if (imageUrl == null) { return location.href; }
    var imgs = document.querySelectorAll('img');
    for (var i = 0; i < imgs.length; i++) {
        if (imgs[i].src == imageUrl) {
            var a = imgs[i].closest ? imgs[i].closest('a') : null;
            if (a == null) {
                var n = imgs[i];
                while (n != null && n.tagName != 'A') { n = n.parentElement; }
                a = n;
            }
            if (a != null && a.href) {
                return a.href;
            }
        }
    }
    return location.href;
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


