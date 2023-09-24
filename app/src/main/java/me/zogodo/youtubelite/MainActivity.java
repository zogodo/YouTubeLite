package me.zogodo.youtubelite;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity
{
    public static MainActivity me;
    public static String indexUrl = "file:///android_asset/goto.html";
    public static WebView webView = null;
    long exitTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        MainActivity.me = this;

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

        OpenRawMusicS();
    }

    private MediaPlayer mediaPlayer1;
    private void OpenRawMusicS() {
        mediaPlayer1 = MediaPlayer.create(this, R.raw.slience);
        mediaPlayer1.start();
        mediaPlayer1.setLooping(true);
        //mediaPlayer1.stop();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.e("noti", "onResume");
        MyNotification.CancelNotification(MyNotification.notifyId);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        Log.e("noti", "onPause");
        MyNotification.MyMediaNotify();
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        Log.e("a", "onNewIntent");
        super.onNewIntent(null);
    };

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
        MyNotification.CancelNotification(MyNotification.notifyId);
        super.onDestroy();
    }
}
