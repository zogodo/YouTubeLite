package me.zogodo.youtubelite;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Stack;

/**
 * Created by zogod on 17/2/19.
 */
public class MyWebView extends WebView
{
    //region 共有变量
    public static Stack<MyWebView> webview_stack = null;
    public static String myJs = null;
    public static String myCss = "";
    //endregion

    //region 构造器
    public MyWebView()
    {
        super(MainActivity.me);
        if (MyWebView.webview_stack == null)
        {
            MyWebView.webview_stack = new Stack<>();
            MyWebView.myJs = CookieTool.RawFileToString(MainActivity.me, R.raw.myjs);
        }
        MyWebView.webview_stack.push(this);
        MainActivity.me.setContentView(this);
        //MainActivity.webView = this;
        this.WebViewInit();
    }
    public MyWebView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }
    //endregion

    //region goBack
    public boolean canGoBack()
    {
        Log.e("zzz " + this.hashCode(), "canGoBack " + MyWebView.webview_stack.size() + super.canGoBack());
        return MyWebView.webview_stack.size() > 1 || super.canGoBack();
    }
    public void goBack()
    {
        Log.e("zzz " + this.hashCode(), "goBack " + MyWebView.webview_stack.size() + super.canGoBack());
        if (super.canGoBack()) {
            super.goBack();
            /*历史回退后页面可能还是视频页,更新媒体通知的信息*/
            UpdateMediaInfoFromPage(3);
            return;
        }
        MyWebView.webview_stack.pop();
        MyWebView old_mywebview = MyWebView.webview_stack.peek();
        MainActivity.me.setContentView(old_mywebview);
        /*恢复的旧页面不会重新加载,onPageFinished不会触发,这里主动更新媒体通知*/
        old_mywebview.UpdateMediaInfoFromPage(3);
    }

    /*从本页面读取视频标题/频道名/封面,更新媒体通知(数据未就绪时重试)*/
    public void UpdateMediaInfoFromPage(final int retries)
    {
        try
        {
            this.evaluateJavascript("javascript:GetVideoInfo();", new ValueCallback<String>()
            {
                @Override
                public void onReceiveValue(String value)
                {
                    boolean ok = false;
                    try
                    {
                        String raw = (String) new org.json.JSONTokener(value).nextValue();
                        String[] vInfs = raw.split("☯");
                        if (vInfs.length >= 2)
                        {
                            String title = vInfs[0].trim();
                            String channel = vInfs[1].trim();
                            String artUrl = vInfs.length >= 3 ? vInfs[2].trim() : "";
                            if (!"XXX".equals(title) && title.length() > 0)
                            {
                                Log.e("media", "updateMediaInfo title=" + title + " channel=" + channel);
                                KeepAliveService.updateMediaInfo(title, channel, artUrl);
                                ok = true;
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    if (!ok && retries > 0)
                    {
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                UpdateMediaInfoFromPage(retries - 1);
                            }
                        }, 1200);
                    }
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    //endregion

    @Override
    public void loadUrl(String url)
    {
        super.loadUrl(url);
    }

    //https://stackoverflow.com/questions/52028940
    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        if (visibility != View.GONE) super.onWindowVisibilityChanged(View.VISIBLE);
    }

    public void injectCSS()
    {
        this.loadUrl("javascript:(function() {" +
                "var parent = document.getElementsByTagName('head').item(0);" +
                "var style = document.createElement('style');" +
                "style.type = 'text/css';" +
                "style.textContent = '" + MyWebView.myCss + "';" +
                "parent.appendChild(style)" +
                "})()");
    }

    //region 自动解除静音(真实触摸)
    /*新 WebView 冷打开视频页没有用户手势,YouTube 会静音自动播放并显示
      "点按取消静音"提示(.ytp-unmute).JS 只负责"看"(轮询提示是否出现并取坐标),
      解除静音的动作是往该坐标注入一次真实 MotionEvent 触摸,
      与用户亲手点击完全等效,不存在 JS 操纵播放器的兼容问题*/
    private int unmutePollCount = 0;
    private boolean unmutePolling = false;
    private final android.os.Handler uiHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    public void StartUnmuteTapPolling()
    {
        if (unmutePolling)
        {
            return;
        }
        unmutePolling = true;
        unmutePollCount = 0;
        PollUnmuteChip();
    }

    private void PollUnmuteChip()
    {
        if (!unmutePolling || unmutePollCount > 30)
        {
            unmutePolling = false;
            return;
        }
        unmutePollCount++;
        this.evaluateJavascript(
                "javascript:(function(){var c=document.querySelector('.ytp-unmute');" +
                        "if(!c){return '';}" +
                        "var r=c.getBoundingClientRect();" +
                        "return (r.x+r.width/2)+','+(r.y+r.height/2);})()",
                new ValueCallback<String>()
                {
                    @Override
                    public void onReceiveValue(String value)
                    {
                        if (!unmutePolling)
                        {
                            return;
                        }
                        String pos = null;
                        if (value != null && value.length() > 4)
                        {
                            pos = value.substring(1, value.length() - 1);   /*去掉首尾引号*/
                        }
                        if (pos == null || pos.isEmpty() || pos.indexOf(',') < 0)
                        {
                            uiHandler.postDelayed(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    PollUnmuteChip();
                                }
                            }, 200);
                            return;
                        }
                        try
                        {
                            String[] parts = pos.split(",");
                            /*getBoundingClientRect 是CSS像素,触摸事件用物理像素*/
                            float density = getResources().getDisplayMetrics().density;
                            float x = Float.parseFloat(parts[0]) * density;
                            float y = Float.parseFloat(parts[1]) * density;
                            DispatchRealTap(x, y);
                        }
                        catch (NumberFormatException e)
                        {
                            uiHandler.postDelayed(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    PollUnmuteChip();
                                }
                            }, 200);
                        }
                    }
                });
    }

    /*注入一次真实触摸(DOWN+UP),Chromium当作可信用户输入处理*/
    private void DispatchRealTap(float x, float y)
    {
        long now = android.os.SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0);
        this.dispatchTouchEvent(down);
        down.recycle();
        uiHandler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                long t = android.os.SystemClock.uptimeMillis();
                MotionEvent up = MotionEvent.obtain(t, t, MotionEvent.ACTION_UP, x, y, 0);
                MyWebView.this.dispatchTouchEvent(up);
                up.recycle();
                unmutePolling = false;   /*点到即止*/
                Log.e("unmute", "real tap at " + x + "," + y);
            }
        }, 80);
    }
    //endregion

    @TargetApi(Build.VERSION_CODES.ECLAIR_MR1)
    @SuppressLint("SetJavaScriptEnabled")
    public void WebViewInit()
    {
        this.getSettings().setJavaScriptEnabled(true);
        this.getSettings().setSupportMultipleWindows(true);
        this.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        this.getSettings().setAllowFileAccess(true);
        this.getSettings().setAppCacheEnabled(true);
        /*DOM storage(localStorage)默认关闭,必须打开,否则YouTube播放器的
          用户偏好持久化(画质/字幕选择等 yt-player-* 键)全部失效,
          每个新WebView都"失忆",像浏览器那样记住选择靠的就是它*/
        this.getSettings().setDomStorageEnabled(true);
        /*允许无手势自动播放(和浏览器一样),YouTube便可直接带声播放,
          否则只能静音播放并显示"点按取消静音"*/
        this.getSettings().setMediaPlaybackRequiresUserGesture(false);
        /*把 Java 对象暴露给网页JS: JS里用 window.java 调用(里带@JavascriptInterface的方法).
          必须在 loadUrl 之前注册, 且只对可信页面使用(接口对所有frame可见) */
        this.addJavascriptInterface(MainActivity.me, "java");
        this.setWebViewClient(new MyWebViewClient(this));
        //这两个是要在 Chrome inspect 调试时用的
        this.setWebChromeClient(new MyWebChromeClient());
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            this.setWebContentsDebuggingEnabled(true);
        }
        this.setOnLongClickListener(longClickListener);
    }

    //region 长按保存图片
    private final View.OnLongClickListener longClickListener = new View.OnLongClickListener()
    {
        @Override
        public boolean onLongClick(View v)
        {
            HitTestResult hr = MyWebView.this.getHitTestResult();
            if (hr != null)
            {
                String extra = hr.getExtra();
                int type = hr.getType();
                if (extra != null &&
                        (type == HitTestResult.IMAGE_TYPE
                                || type == HitTestResult.SRC_IMAGE_ANCHOR_TYPE))
                {
                    showImageMenu(extra);
                    return true;
                }
                else if (extra != null && type == HitTestResult.ANCHOR_TYPE)
                {
                    //文本链接(如视频标题):extra 就是链接地址
                    showLinkMenu(extra);
                    return true;
                }
            }
            //文本等其他区域:交还系统默认长按行为
            return false;
        }
    };

    private void showLinkMenu(final String linkUrl)
    {
        String[] items = {"复制链接"};
        new AlertDialog.Builder(MainActivity.me)
                .setTitle("操作")
                .setItems(items, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        copyToClipboard(linkUrl);
                    }
                })
                .show();
    }

    private void showImageMenu(final String imageUrl)
    {
        String[] items = {"保存图片", "复制视频链接"};
        new AlertDialog.Builder(MainActivity.me)
                .setTitle("操作")
                .setItems(items, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        if (which == 0)
                        {
                            saveImage(MainActivity.me, imageUrl);
                        }
                        else
                        {
                            //长按的是缩略图(位于视频链接内),通过JS找到其所在链接复制
                            MyWebView.this.evaluateJavascript(
                                    "javascript:FindVideoLink('" + imageUrl + "');",
                                    new ValueCallback<String>()
                                    {
                                        @Override
                                        public void onReceiveValue(String value)
                                        {
                                            if (value == null || value.length() < 3) return;
                                            String link = value.substring(1, value.length() - 1);
                                            copyToClipboard(link);
                                        }
                                    });
                        }
                    }
                })
                .show();
    }

    private void copyToClipboard(String link)
    {
        ClipboardManager cm = (ClipboardManager) MainActivity.me.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("videoUrl", link));
        Toast.makeText(MainActivity.me, "视频链接已复制", Toast.LENGTH_SHORT).show();
    }

    public static void saveImage(Context ctx, final String imageUrl)
    {
        //Android 10 以下需要 WRITE_EXTERNAL_STORAGE 运行时权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
        {
            if (ctx.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            {
                MainActivity.pendingImageUrl = imageUrl;
                ((Activity) ctx).requestPermissions(
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MainActivity.REQUEST_SAVE_IMAGE);
                return;
            }
        }
        downloadAndSave(ctx, imageUrl);
    }

    public static void downloadAndSave(final Context ctx, final String imageUrl)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    URL url = new URL(imageUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("Referer", "https://www.youtube.com/");
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    InputStream is = conn.getInputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = is.read(buf)) != -1)
                    {
                        baos.write(buf, 0, n);
                    }
                    is.close();
                    byte[] data = baos.toByteArray();
                    String ext = "jpg";
                    String contentType = conn.getContentType();
                    if (contentType != null)
                    {
                        if (contentType.contains("png")) ext = "png";
                        else if (contentType.contains("gif")) ext = "gif";
                        else if (contentType.contains("webp")) ext = "webp";
                    }
                    final boolean ok = saveToGallery(ctx, data, ext);
                    MainActivity.me.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Toast.makeText(ctx, ok ? "图片已保存到相册" : "保存图片失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                catch (final Exception e)
                {
                    e.printStackTrace();
                    MainActivity.me.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Toast.makeText(ctx, "保存图片失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private static boolean saveToGallery(Context ctx, byte[] data, String ext)
    {
        String fileName = "YoutubeLite_" + System.currentTimeMillis() + "." + ext;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/" + ext);
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/YoutubeLite");
            Uri uri = ctx.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) return false;
            try
            {
                OutputStream os = ctx.getContentResolver().openOutputStream(uri);
                if (os == null) return false;
                os.write(data);
                os.close();
                return true;
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return false;
            }
        }
        else
        {
            try
            {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "YoutubeLite");
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, fileName);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data);
                fos.close();
                MediaScannerConnection.scanFile(ctx, new String[]{file.getAbsolutePath()}, null, null);
                return true;
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return false;
            }
        }
    }
    //endregion
}


