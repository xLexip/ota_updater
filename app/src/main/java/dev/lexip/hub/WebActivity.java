package dev.lexip.hub;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebActivity extends AppCompatActivity {

    public String url = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Bundle b = getIntent().getExtras();
        if(b != null)
            url = b.getString("url");

        WebView webView = (WebView) findViewById(R.id.wvWeb);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(url);

        webView.getSettings().setForceDark(webView.getSettings().FORCE_DARK_ON);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
    }
}