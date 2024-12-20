package io.openim.android.ouicore.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.net.Uri;
import android.webkit.WebViewClient;

import io.openim.android.ouicore.base.BaseActivity;
import io.openim.android.ouicore.base.BaseViewModel;
import io.openim.android.ouicore.databinding.ActivityWebViewBinding;
import io.openim.android.ouicore.utils.Common;


public class WebViewActivity extends BaseActivity<BaseViewModel, ActivityWebViewBinding> {
    public final static String TITLE = "title";
    public final static String RIGHT = "right";
    public final static String LOAD_URL = "loadUrl";



    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindViewDataBinding(ActivityWebViewBinding.inflate(getLayoutInflater()));
        sink();;

        initView();
    }

    public void toBack(View v) {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void initView() {
        String title = getIntent().getStringExtra(TITLE);
        if (!TextUtils.isEmpty(title))
            view.title.setText(title);
        String right = getIntent().getStringExtra(RIGHT);
        if (null == right)
            right = "";
        view.right.setText(right);


        view.right.setOnClickListener(v -> finish());

        WebSettings webSettings = view.webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setGeolocationEnabled(true);
        String dir = this.getApplicationContext().getDir("database", Context.MODE_PRIVATE).getPath();
        webSettings.setGeolocationDatabasePath(dir);
        webSettings.setDomStorageEnabled(true);

        view.webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView v, int newProgress) {
                if (newProgress == 100) {
                    view.progressBar.setVisibility(View.GONE);
                } else {
                    view.progressBar.setVisibility(View.VISIBLE);
                    view.progressBar.setProgress(newProgress);
                }
                super.onProgressChanged(v, newProgress);
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
                super.onGeolocationPermissionsShowPrompt(origin, callback);
            }
        });

        view.webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (TextUtils.isEmpty(url)) return false;

                try {
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    }
                } catch (Exception e) {
                    return true;
                }

                view.loadUrl(url);
                return true;
            }
        });
        String url = getIntent().getStringExtra(LOAD_URL);
        if (TextUtils.isEmpty(url)) {
            toast("the url is a null value.");
            Common.UIHandler.postDelayed(WebViewActivity.this::finish, 2000);
            return;
        }
        view.webView.loadUrl(url);
    }
}
