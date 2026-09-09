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

/*up主名字乱码*/
function FixUperName() {
    /*var uperName = document.getElementsByClassName('reel-player-header-channel-title sd-redesign-experiment')[0].textContent;*/
    var spans = document.getElementsByClassName('slim-video-information-channel-name');
    if (spans.length <= 0) return;
    spans = spans[0].getElementsByTagName('span');
    var uperId = spans[2];
    uperId.textContent = decodeURIComponent(uperId.textContent);
    /*uperId.textContent = uperName;*/
    clearInterval(timer);
}
var timer = setInterval(FixUperName, 1000);

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

var DEFAULT_QUALITY = "hd720";  /*默认720P*/

/*把偏好写入 localStorage,YouTube 播放器在加载视频和打开设置菜单时会读取它*/
function SaveQualityPref() {
    try {
        localStorage.setItem("yt-player-quality", JSON.stringify({
            data: { quality: DEFAULT_QUALITY, previousQuality: DEFAULT_QUALITY }
        }));
    } catch (e) { }
}
SaveQualityPref();

/*定时强制画质:只有视频真正开始播放(state==1/3)后设置画质才生效,
  所以不能像以前那样在 movie_player 刚出现时一次性设置*/
function ApplyQuality() {
    var player = document.getElementById("movie_player");
    if (player == null) { return; }

    SaveQualityPref();

    if (typeof player.getPlayerState !== "function") { return; }
    var state = player.getPlayerState();
    if (state != 1 && state != 3) { return; }

    try {
        if (player.getPlaybackQuality() === DEFAULT_QUALITY) { return; }
    } catch (e) { }

    try {
        if (typeof player.setPlaybackQualityRange === "function") {
            player.setPlaybackQualityRange(DEFAULT_QUALITY);
        }
    } catch (e) { }
    try {
        if (typeof player.setPlaybackQuality === "function") {
            player.setPlaybackQuality(DEFAULT_QUALITY);
        }
    } catch (e) { }
}
setInterval(ApplyQuality, 1000);

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
adCss.textContent = '.ytp-ad-overlay-container,.ytp-ad-text-overlay,.video-ads,#player-ads,ytd-ad-slot-renderer,.ytp-ad-action-interrupt,.ytp-ad-player-overlay,.ytp-ad-module,.ytp-ad-image-overlay,.ytp-ad-skip-button-container{display:none!important}';
document.head.appendChild(adCss);

/*=== 修复:播放器始终固定在上方(全屏退出后会被取消固定) ===*/
function ForceStickyPlayer() {
    var container = document.querySelector('#player-container-id');
    if (container == null) { return; }
    document.documentElement.classList.add('sticky-player');
    var app = document.querySelector('ytm-app');
    if (app) { app.classList.add('sticky-player'); }
    var header = document.querySelector('ytm-header');
    if (header) { header.classList.add('sticky-player'); }
    container.classList.add('sticky-player');
}
ForceStickyPlayer();
setInterval(ForceStickyPlayer, 300);

/*=== 后台播放不被其它应用打断:视频被外部暂停时自动续播 ===*/
var __keepPlaying = false;
function ResumeVideoIfInterrupted() {
    if (__keepPlaying !== true) { return; }
    var player = document.getElementById('movie_player');
    if (player == null || typeof player.getPlayerState !== 'function') { return; }
    /*state==2 表示被暂停(多为其它应用抢占音频焦点导致),自动继续播放*/
    if (player.getPlayerState() == 2) {
        try { player.playVideo(); } catch (e) { }
    }
}
setInterval(ResumeVideoIfInterrupted, 1000);

/*=== 修复:视频页下方"相关视频"不显示发布时间,这里补上 ===*/

/*videoId -> 发布时间文本(如 "3天前")*/
var __videoTimeMap = {};

/*递归遍历对象,收集所有 videoId 与 publishedTimeText 的对应关系*/
function CollectVideoTimes(obj) {
    if (obj == null || typeof obj != "object") { return; }
    if (Array.isArray(obj)) {
        for (var i = 0; i < obj.length; i++) { CollectVideoTimes(obj[i]); }
        return;
    }
    var id = obj.videoId;
    var timeText = null;
    var pt = obj.publishedTimeText;
    if (pt != null) {
        if (typeof pt.simpleText == "string") { timeText = pt.simpleText; }
        else if (Array.isArray(pt.runs)) {
            timeText = "";
            for (var j = 0; j < pt.runs.length; j++) { timeText += pt.runs[j].text; }
        }
    }
    if (typeof id == "string" && timeText != null && timeText != "") {
        __videoTimeMap[id] = timeText;
    }
    for (var k in obj) {
        if (Object.prototype.hasOwnProperty.call(obj, k)) { CollectVideoTimes(obj[k]); }
    }
}

/*移动端 watch 页是服务端渲染,初始数据里已带相关视频*/
if (window.ytInitialData != null) { CollectVideoTimes(window.ytInitialData); }
if (window.ytInitialPlayerResponse != null) { CollectVideoTimes(window.ytInitialPlayerResponse); }

/*拦截 /youtubei/v1/next、/browse 响应:滚动加载更多相关视频时也能拿到发布时间*/
(function () {
    var realFetch = window.fetch;
    if (typeof realFetch == "function") {
        window.fetch = function () {
            var p = realFetch.apply(this, arguments);
            var url = "";
            try {
                url = typeof arguments[0] == "string" ? arguments[0] : (arguments[0] && arguments[0].url) || "";
            } catch (e) { }
            if (url.indexOf("/youtubei/v1/") != -1) {
                p.then(function (resp) {
                    try {
                        resp.clone().json().then(function (json) { CollectVideoTimes(json); }).catch(function () { });
                    } catch (e) { }
                }).catch(function () { });
            }
            return p;
        };
    }
    var realOpen = XMLHttpRequest.prototype.open;
    var realSend = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.open = function (method, url) {
        this.__zlUrl = url;
        return realOpen.apply(this, arguments);
    };
    XMLHttpRequest.prototype.send = function () {
        var self = this;
        if (self.__zlUrl && self.__zlUrl.indexOf("/youtubei/v1/") != -1) {
            self.addEventListener("load", function () {
                try { CollectVideoTimes(JSON.parse(self.responseText)); } catch (e) { }
            });
        }
        return realSend.apply(this, arguments);
    };
})();

/*从链接里取 videoId*/
function GetVideoIdFromHref(href) {
    var m = (href || "").match(/[?&]v=([^&]+)/);
    return m ? m[1] : null;
}

/*给相关视频补上发布时间(只在 /watch 页生效,首页本身已显示时间)*/
function AddPublishTimeToRelated() {
    if (location.pathname != "/watch") { return; }
    var items = document.querySelectorAll("ytm-video-with-context-renderer");
    for (var i = 0; i < items.length; i++) {
        var item = items[i];
        if (item.querySelector('[data-zl-time]')) { continue; }   /*已注入过*/
        var a = item.querySelector('a[href*="/watch?v="]');
        if (a == null) { continue; }
        var vid = GetVideoIdFromHref(a.getAttribute("href"));
        if (vid == null) { continue; }
        var time = __videoTimeMap[vid];
        if (time == null || time == "") { continue; }

        var bylines = item.querySelectorAll("ytm-badge-and-byline-renderer");
        if (bylines.length == 0) { continue; }
        var byline = bylines[bylines.length - 1];   /*追加到最后一个(通常是观看次数所在行)*/

        var sep = document.createElement("span");
        sep.className = "ytmBadgeAndBylineRendererSeparator";
        sep.setAttribute("aria-hidden", "true");
        sep.textContent = "•";

        var ts = document.createElement("span");
        ts.className = "ytmBadgeAndBylineRendererItemByline";
        ts.setAttribute("dir", "auto");
        ts.setAttribute("aria-hidden", "true");

        var inner = document.createElement("span");
        inner.className = "ytAttributedStringHost";
        inner.setAttribute("data-zl-time", "1");
        inner.textContent = time;
        ts.appendChild(inner);

        byline.appendChild(sep);
        byline.appendChild(ts);
    }
}
setInterval(AddPublishTimeToRelated, 1000);


