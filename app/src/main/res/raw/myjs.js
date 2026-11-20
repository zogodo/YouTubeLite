/*把所有链接改为 target=_blank 新窗口打开(onCreateWindow 只有 target=_blank 才触发),
  每个页面一个 WebView 入栈,返回键直接弹栈,实现"返回不重载".
  代价:新 WebView 冷打开视频页没有用户手势,YouTube 会静音自动播放;
  由 Java 层(MyWebView.StartUnmuteTapPolling)注入真实触摸点"点按取消静音"解除*/
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
    if (window.location.pathname != "/watch") {
        clearInterval(timer1);
        return;
    }
    /*var uperName = document.getElementsByClassName('reel-player-header-channel-title sd-redesign-experiment')[0].textContent;*/
    var spans = document.getElementsByClassName('slim-video-information-channel-name');
    if (spans.length <= 0) {
        /*window.location.href*/
        alert("FixUperName: spans.length=" + spans.length);
        return;
    }
    spans = spans[0].getElementsByTagName('span');
    var uperId = spans[2];
    uperId.textContent = decodeURIComponent(uperId.textContent);
    /*uperId.textContent = uperName;*/
    clearInterval(timer1);
}
var timer1 = setInterval(FixUperName, 3000);

/*=== 在首页写入默认偏好到 localStorage(YouTube 自己的持久化格式) ===
  原理:播放器每次打开视频时会读取这些键当作"用户上次的选择",原生应用画质和字幕偏好。
  格式来自播放器源码与实测:
    yt-player-quality            = {"data":"{\"quality\":720,\"previousQuality\":720}","expiration":..,"creation":..}
    yt-player-caption-persistence= {"data":"false","expiration":..,"creation":..}
  只在首页(路径为 /)写入,视频页不写,避免覆盖用户在播放页里的手动选择。
  注意:必须先开启 WebView 的 DOM storage(setDomStorageEnabled(true)),否则 localStorage 无效*/
var DEFAULT_QUALITY_ORDINAL = 720;    /*720p 在播放器里的画质序号*/
function SeedDefaultPrefs() {
    try {
        if (!/(^|\.)youtube\.com$/.test(location.hostname)) { return; }
        if (location.pathname != "/") { return; }   /*只在首页写入*/
        var now = Date.now();
        /*画质:data 是 JSON 字符串化的 {quality,previousQuality},quality 用序号;TTL 36天(与播放器一致)*/
        localStorage.setItem("yt-player-quality", JSON.stringify({
            data: JSON.stringify({ quality: DEFAULT_QUALITY_ORDINAL, previousQuality: DEFAULT_QUALITY_ORDINAL }),
            expiration: now + 3110400000,
            creation: now
        }));
        /*字幕:data 是 JSON 字符串化的布尔值,"false" 表示不启用字幕*/
        localStorage.setItem("yt-player-caption-persistence", JSON.stringify({
            data: "false",
            expiration: now + 3122064000000,
            creation: now
        }));
    } catch (e) { }
}
SeedDefaultPrefs();

var QUALITIES =  ['auto', 'tiny', 'small', 'medium', 'large', 'hd720', 'hd1080', 'hd1440', 'hd2160', 'hd2880', 'highres'];
var m = 0;
function SetQuality() {
    var movie_player = document.getElementById("movie_player");
    /*console.log(movie_player.getPlaybackQuality());*/
    if (movie_player != null && movie_player.zz_set == null) {
        movie_player.setPlaybackQualityRange("hd720");
        movie_player.zz_set = true;
        return;
    }
    if (m++ > 60) {
        return;
    }
    setTimeout(SetQuality, 500);
}
SetQuality();  /*默认720P*/
/*var timer2 = setInterval(SetQuality, 1000);*/


/*暂停播放接口*/
function PauseOrPlay() {
    if (movie_player.getPlayerState() == 1) {
        movie_player.pauseVideo();
        return 0;
    } else {
        movie_player.playVideo();
        return 1;
    }
}

/*获取视频信息接口*/
function GetVideoInfo() {
    /*优先从播放器数据里取标题/频道名:不依赖会变的DOM类名,
      普通视频页和Shorts都适用. 格式: 标题☯频道名☯Url */
    try {
        var pr = window.ytInitialPlayerResponse;
        if (pr != null && pr.videoDetails != null && pr.videoDetails.title) {
            return pr.videoDetails.title + '☯' + (pr.videoDetails.author || '') + '☯' + 'Url';
        }
    } catch (e) { }
    /*兜底:从页面元素取(类名可能随版本变化)*/
    try {
        var t = document.querySelector('h1 .yt-core-attributed-string, .slim-video-information-title .yt-core-attributed-string');
        if (t != null) {
            return t.textContent + '☯' + '' + '☯' + 'Url';
        }
    } catch (e) { }
    return "XXX☯YYY☯ZZZ";
}

/*判断视频是否正在播放接口*/
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
    if (imageUrl == null) { return window.location.href; }
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
    return window.location.href;
}

/*快速跳过广告,没用?*/
/*
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
*/

/*=== 屏蔽一些广告元素 ===
  注意:不能隐藏 .video-ads/.ytp-ad-module/.ytp-ad-player-overlay/.ytp-ad-skip-button-container,
  它们是"跳过广告"按钮的祖先/本身,祖先display:none会连带藏掉跳过按钮.
  这里只隐藏纯浮层/广告卡片*/
var adCss = document.createElement('style');
adCss.type = 'text/css';
adCss.textContent = '.ytp-ad-overlay-container,.ytp-ad-text-overlay,#player-ads,ytd-ad-slot-renderer,.ytp-ad-image-overlay,.ytp-ad-player-overlay-flyout-cta{display:none!important}';
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
    if (window.location.pathname != "/watch") { return; }
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


