package ai.nexconn.chatui.activity;

import ai.nexconn.chatui.R;
import ai.nexconn.chatui.config.FeatureConfig;
import ai.nexconn.chatui.config.NCChatUIConfig;
import ai.nexconn.chatui.utils.log.RLog;
import ai.nexconn.chatui.widget.ChatUIWebView;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import java.util.List;

public class NCWebviewActivity extends NCBaseActivity {
    private static final String TAG = "NCWebviewActivity";

    private String mPrevUrl;
    protected ChatUIWebView mWebView;
    private ProgressBar mProgressBar;
    private static final String FILE = "file://";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nc_ac_webview);
        initStatusBar(R.color.app_color_white);
        Intent intent = getIntent();
        mWebView = findViewById(R.id.nc_webview);
        mProgressBar = findViewById(R.id.nc_web_progressbar);
        mWebView.setVerticalScrollbarOverlay(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
            mWebView.getSettings().setDisplayZoomControls(false);
        }
        mWebView.getSettings().setSupportZoom(true);
        mWebView.setWebViewClient(new NCWebviewClient());
        mWebView.setWebChromeClient(new NCWebChromeClient());
        mWebView.setDownloadListener(new ChatUIWebViewDownLoadListener());
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setDefaultTextEncodingName("utf-8");
        mWebView.getSettings().setSavePassword(false);

        String url = intent.getStringExtra("url");
        Uri data = intent.getData();
        if (url != null) {
            if (NCChatUIConfig.featureConfig().NC_set_java_script_enabled) {
                if (url.startsWith(FILE)) {
                    mWebView.getSettings().setJavaScriptEnabled(false);
                } else {
                    mWebView.getSettings().setJavaScriptEnabled(true);
                }
            }
            mPrevUrl = url;
            mWebView.loadUrl(url);
            String title = intent.getStringExtra("title");
            if (mTitleBar != null && !TextUtils.isEmpty(title)) {
                mTitleBar.setTitle(title);
            }
        } else if (data != null) {
            mPrevUrl = data.toString();
            mWebView.loadUrl(data.toString());
        }

        if (mTitleBar != null) {
            mTitleBar.setRightVisible(false);
        }
    }

    public void setOnTitleReceivedListener(OnTitleReceivedListener onTitleReceivedListener) {
        this.onTitleReceivedListener = onTitleReceivedListener;
    }

    private OnTitleReceivedListener onTitleReceivedListener;

    public interface OnTitleReceivedListener {
        /**
         * onTitleReceived
         *
         * @param title Title
         */
        void onTitleReceived(String title);
    }

    private class NCWebviewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (mPrevUrl != null) {
                if (!mPrevUrl.equals(url)) {
                    if (!(url.toLowerCase().startsWith("http://")
                            || url.toLowerCase().startsWith("https://"))) {
                        Intent intent = new Intent("android.intent.action.VIEW");
                        Uri content_url = Uri.parse(url);
                        intent.setData(content_url);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        try {
                            startActivity(intent);
                        } catch (Exception e) {
                            RLog.e(TAG, "not apps install for this intent =" + e.toString());
                            RLog.e(TAG, "NCWebviewClient", e);
                        }
                        return true;
                    }
                    mPrevUrl = url;
                    mWebView.loadUrl(url);
                    return true;
                } else {
                    return false;
                }
            } else {
                mPrevUrl = url;
                mWebView.loadUrl(url);
                return true;
            }
        }

        @Override
        public void onReceivedSslError(
                WebView view, final SslErrorHandler handler, SslError error) {
            if (NCWebviewActivity.this.isDestroyed() || NCWebviewActivity.this.isFinishing()) {
                RLog.e(TAG, "onReceivedSslError but activity is finish");
                return;
            }
            FeatureConfig.SSLInterceptor interceptor =
                    NCChatUIConfig.featureConfig().getSSLInterceptor();
            boolean check = false;
            if (interceptor != null) {
                check = interceptor.check(error.getCertificate());
            }
            if (check) {
                handler.proceed();
            } else {
                final AlertDialog.Builder builder = new AlertDialog.Builder(NCWebviewActivity.this);
                builder.setMessage(R.string.nc_notification_error_ssl_cert_invalid);
                builder.setNegativeButton(
                        R.string.nc_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                handler.cancel();
                            }
                        });

                final AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
    }

    private class NCWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (newProgress == 100) {
                mProgressBar.setVisibility(View.GONE);
            } else {
                if (mProgressBar.getVisibility() == View.GONE) {
                    mProgressBar.setVisibility(View.VISIBLE);
                }
                mProgressBar.setProgress(newProgress);
            }
            super.onProgressChanged(view, newProgress);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            if (mTitleBar != null && TextUtils.isEmpty(mTitleBar.getTitle())) {
                mTitleBar.setTitle(title);
            }
            if (onTitleReceivedListener != null) {
                onTitleReceivedListener.onTitleReceived(title);
            }
        }

        @Override
        public void onCloseWindow(WebView window) {
            super.onCloseWindow(window);
            if (!isFinishing()) {
                finish();
            }
        }
    }

    private class ChatUIWebViewDownLoadListener implements DownloadListener {

        @Override
        public void onDownloadStart(
                String url,
                String userAgent,
                String contentDisposition,
                String mimetype,
                long contentLength) {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            if (checkIntent(NCWebviewActivity.this, intent)) {
                startActivity(intent);
                if (("file").equals(uri.getScheme()) && uri.toString().endsWith(".txt")) {
                    finish();
                }
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public boolean checkIntent(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> apps = packageManager.queryIntentActivities(intent, 0);
        return apps.size() > 0;
    }
}
