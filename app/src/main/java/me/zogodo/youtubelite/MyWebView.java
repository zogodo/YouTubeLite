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
            return;
        }
        MyWebView.webview_stack.pop();
        MyWebView old_mywebview = MyWebView.webview_stack.peek();
        MainActivity.me.setContentView(old_mywebview);
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

    @TargetApi(Build.VERSION_CODES.ECLAIR_MR1)
    @SuppressLint("SetJavaScriptEnabled")
    public void WebViewInit()
    {
        this.getSettings().setJavaScriptEnabled(true);
        this.getSettings().setSupportMultipleWindows(true);
        this.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        this.getSettings().setAllowFileAccess(true);
        this.getSettings().setAppCacheEnabled(true);
        /*允许无手势自动播放(和浏览器一样),YouTube便可直接带声播放,
          否则只能静音播放并显示"点按取消静音"*/
        this.getSettings().setMediaPlaybackRequiresUserGesture(false);
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


