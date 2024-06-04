package me.zogodo.youtubelite;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.ValueCallback;
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

        ValueCallback cb = new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                String[] vInfs = value.split("\n");
                //MyNotification.MyMediaNotify(vInfs[0], vInfs[1], vInfs[2]);
                MyNotification.MyMediaNotify("vInfs[0]", "vInfs[1]", "vInfs[2]");
            }
        };
        webView.evaluateJavascript("javascript:GetVideoInfo();", cb);
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
