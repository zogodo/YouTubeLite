package me.zogodo.youtubelite;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.Toast;

public class MainActivity extends Activity
{
    public static MainActivity me;
    public static String indexUrl = "file:///android_asset/goto.html";
    public static WebView webView = null;
    public static final int REQUEST_SAVE_IMAGE = 100;
    public static String pendingImageUrl = null;
    long exitTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        MainActivity.me = this;
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Intent intent = getIntent();
        String dataString = intent.getDataString();
        if (dataString != null)
        {
            dataString = dataString.replaceAll("://z/", "://");
            indexUrl = dataString;
        }
        Log.e("zurl", indexUrl, null);

        webView = new MyWebView();
        webView.loadUrl(indexUrl);

        //一打开app就启动前台服务:常驻媒体通知(播放/暂停按钮),不关app就一直在
        try
        {
            KeepAliveService.start(this);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        //注意:不再在这里停止服务,媒体通知要常驻(只由任务关闭/退出时清理)
        //回到前台,取消后台自动续播标记(恢复用户正常控制)
        if (webView != null)
        {
            webView.evaluateJavascript("javascript:__keepPlaying = false;", null);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onPause()
    {
        super.onPause();
        Log.e("noti", "onPause");

        //读取视频标题/频道名,更新媒体通知上显示的信息
        webView.evaluateJavascript("javascript:GetVideoInfo();", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                try
                {
                    /*evaluateJavascript 返回的是JSON字符串(带引号和转义),用JSONTokener正确还原*/
                    String raw = (String) new org.json.JSONTokener(value).nextValue();
                    String[] vInfs = raw.split("☯");
                    if (vInfs.length >= 2)
                    {
                        String title = vInfs[0].trim();
                        String channel = vInfs[1].trim();
                        String artUrl = vInfs.length >= 3 ? vInfs[2].trim() : "";
                        /*GetVideoInfo 在非视频页会返回占位符 XXX/YYY,忽略*/
                        if (!"XXX".equals(title) && title.length() > 0)
                        {
                            KeepAliveService.updateMediaInfo(title, channel, artUrl);
                        }
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });

        //后台若仍在播放,启动前台服务保活,防止被系统杀掉
        webView.evaluateJavascript("javascript:IsVideoPlaying();", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                boolean playing = "true".equals(value);
                if (playing)
                {
                    try
                    {
                        KeepAliveService.start(MainActivity.me);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                //设置标记:后台播放时若被其它应用(抢占音频焦点)打断暂停,页面看门狗会自动续播
                webView.evaluateJavascript("javascript:__keepPlaying = " + playing + ";", null);
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        Log.e("a", "onNewIntent");
        super.onNewIntent(null);
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_SAVE_IMAGE && grantResults.length > 0
                && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
                && pendingImageUrl != null)
        {
            MyWebView.downloadAndSave(this, pendingImageUrl);
            pendingImageUrl = null;
        }
    }

    @Override
    public void setContentView(View view)
    {
        if (view instanceof MyWebView)
        {
            webView = (MyWebView)view;
        }
        super.setContentView(view);
    }

    public void onBackPressed()
    {
        if (webView != null && webView.canGoBack())
        {
            webView.goBack();
        }
        else
        {
            // 判断是否可后退，是则后退，否则退出程序
            if (((System.currentTimeMillis() - exitTime) > 3000))
            {
                Toast.makeText(getApplicationContext(), "再按一次返回 退出程序", Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            }
            else
            {
                finish();
                System.exit(0);
            }
        }
    }

    @Override
    public void onStop()
    {
        Log.e("noti", "onStop");
        //cancelNotification(notifyId);
        super.onStop();
    }

    @Override
    public void onDestroy()
    {
        Log.e("noti", "onDestroy");
        //真正退出app(返回键退出)时清理常驻的媒体通知;仅旋转/内存回收不清理
        if (isFinishing())
        {
            KeepAliveService.stop(this);
        }
        super.onDestroy();
    }
}
