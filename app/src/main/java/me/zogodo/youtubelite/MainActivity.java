package me.zogodo.youtubelite;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

import java.util.Stack;

public class MainActivity extends AppCompatActivity
{
    MyWebView webView = null;
    long exitTime = 0;
    public static String my_js = "window.addEventListener('scroll', function () {\n" +
                   "    var links = document.querySelectorAll('a:not([target=_blank])');\n" +
                   "    for (var i = 0; i < links.length; i++) {\n" +
                   "        links[i].target = '_blank';\n" +
                   "    }\n" +
                   "});";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        String url = "https://m.youtube.com";
        webView = new MyWebView(this, new Stack<>());
        webView.WebViewInit(url, my_js, "");
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
