package me.zogodo.youtubelite;

import android.content.Context;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.adblockplus.libadblockplus.android.AdblockEngine;
import org.adblockplus.libadblockplus.android.settings.AdblockHelper;
import org.adblockplus.libadblockplus.android.webview.AdblockWebView;

public class MainActivity extends AppCompatActivity
{
    public static MainActivity me;
    public static String indexUrl = "https://m.youtube.com";
    WebView webView = null;
    long exitTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        MainActivity.me = this;

        String basePath = getDir(AdblockEngine.BASE_PATH_DIRECTORY, Context.MODE_PRIVATE).getAbsolutePath();
        AdblockHelper
                .get()
                .init(this, basePath, AdblockHelper.PREFERENCE_NAME)
                .preloadSubscriptions(R.raw.easylist_minified, R.raw.exceptionrules_minimal);
                //.setDisabledByDefault();
        AdblockHelper.get().getProvider().retain(false);
        AdblockHelper.get().getProvider().waitForReady();
        final AdblockEngine adblockEngine = AdblockHelper.get().getProvider().getEngine();
        adblockEngine.setEnabled(true);

        /*
        AdblockSettingsStorage storage = AdblockHelper.get().getStorage();
        AdblockSettings settings = storage.load();
        if (settings == null) // not yet saved
        {
            settings = AdblockSettingsStorage.getDefaultSettings(this); // default
        }
        settings.setAdblockEnabled(true);
        storage.save(settings);
        */

        indexUrl = "https://zogodo.github.io";
        indexUrl = "https://www.openguet.cn/lab/views/login.jsp";
        indexUrl = "http://nsfwyoutube.com/";
        webView = new AdblockWebView(this);
        this.setContentView(webView);
        webView.loadUrl(indexUrl);
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
