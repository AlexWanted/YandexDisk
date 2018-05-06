package com.ialexwantedi.yandexdisk;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Класс Activity с авторизацией пользователя.
 */
public class SignInActivity extends AppCompatActivity {

    WebView webView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        if(getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE).getString("PREF_TOKEN", "").equals("")) {
            webView = (WebView) findViewById(R.id.webView);
            webView.setWebViewClient(new MyWebViewClient());
            String appID = "8ec5bccc291746dd8f71064feb0ea1b3";
            String url = "https://oauth.yandex.ru/authorize?response_type=token&client_id=" + appID;
            webView.loadUrl(url);
        } else {
            startActivity(new Intent(this, HomePageActivity.class));
            finish();
        }
    }

    class MyWebViewClient extends WebViewClient {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if(request.getUrl().toString().contains("alexwantedsyandexdisk")) {
                startActivity(new Intent(Intent.ACTION_VIEW, request.getUrl()));
                finish();
                return true;
            }
            return false;
        }
    }
}
