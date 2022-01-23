package me.zogodo.youtubelite;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.webkit.*;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.io.IOException;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

/**
 * Created by zogod on 17/2/19.
 */
public class MyWebViewClient extends WebViewClient
{
    MyWebView mywebview;

    public MyWebViewClient(MyWebView mywebview) {
        this.mywebview = mywebview;
    }

    public static String oldUrl = "";
    @Override
    public void doUpdateVisitedHistory(WebView view, String url, boolean isReload)
    {
        // your code here
        Log.e("zzz doUpdateVisitedHistory", url);

        /*
        if (url.equals(oldUrl))
        {
            super.doUpdateVisitedHistory(view, url, isReload);
            return;
        }
        oldUrl = url;

        view.stopLoading();
        //view.setVisibility(view.INVISIBLE);

        LayoutInflater inflater = (LayoutInflater) this.mywebview.activity.getSystemService(LAYOUT_INFLATER_SERVICE);
        ConstraintLayout rel_layout = (ConstraintLayout) inflater.inflate(R.layout.activity_main, null);
        MyWebView new_mywebview = rel_layout.findViewById(R.id.webview);
        new_mywebview.activity = this.mywebview.activity;
        new_mywebview.webview_stack = this.mywebview.webview_stack;
        //new_mywebview.first_load = false;
        new_mywebview.VebViewInit(url, this.mywebview.myjs, this.mywebview.mycss);
        this.mywebview.webview_stack.push(new_mywebview);

        new_mywebview.StartView();
        */
        super.doUpdateVisitedHistory(view, url, isReload);
    }

    //@Override
    @TargetApi(24)
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req)
    {
        String url = req.getUrl().toString();
        Log.e("zzz shouldOverrideUrlLoading", url);
        // Log.e("sho__ url", url);
        WebView.HitTestResult hit = view.getHitTestResult();
        if (hit.getExtra() != null)
        {
            view.stopLoading();
            view.setVisibility(view.INVISIBLE);

            LayoutInflater inflater = (LayoutInflater) this.mywebview.activity.getSystemService(LAYOUT_INFLATER_SERVICE);
            ConstraintLayout rel_layout = (ConstraintLayout) inflater.inflate(R.layout.activity_main, null);
            MyWebView new_mywebview = rel_layout.findViewById(R.id.webview);
            new_mywebview.activity = this.mywebview.activity;
            new_mywebview.webview_stack = this.mywebview.webview_stack;
            //new_mywebview.first_load = false;
            new_mywebview.VebViewInit(url, this.mywebview.myjs, this.mywebview.mycss);
            this.mywebview.webview_stack.push(new_mywebview);

            new_mywebview.StartView();
        }
        return false;
    }

    public void onPageStarted(WebView view, String url, Bitmap favicon)
    {
        Log.e("zzz onPageStarted", url);
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onLoadResource(WebView view, String url)
    {
        //Log.e("zzz onLoadResource", url);
        super.onLoadResource(view, url);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void onPageFinished(WebView view, String url)
    {
        // Log.e("ended url", url);
        this.mywebview.loadUrl("javascript:" + this.mywebview.myjs);
        this.mywebview.injectCSS();

        this.mywebview.setVisibility(this.mywebview.VISIBLE);

        super.onPageFinished(view, url);
    }

}
