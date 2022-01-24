package me.zogodo.youtubelite;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Toast;

import java.util.Stack;

public class MainActivity extends AppCompatActivity
{
    MyWebView webView = null;
    long exitTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        String url = "https://m.youtube.com";
        url = "https://zogodo.github.io/";
        url = "https://m.baidu.com/";
        url = "https://www.w3schools.com/html/html5_video.asp";
        url = "http://yuntong.icu:8080/test_video.html";
        url = "https://m.hao123.com/";
        url = "https://m.youtube.com";
        url = "http://yuntong.icu:8080/test_video.html";
         //webView = findViewById(R.id.webview);
        webView = new MyWebView(this, new Stack<MyWebView>());
        webView.VebViewInit(url, "", "");
        webView.StartView();
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
}
