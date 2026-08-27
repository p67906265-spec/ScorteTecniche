package it.paolo.scortetecniche;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView webView;
    private static final String HOME = "https://scortetecniche.rf.gd/";
    private static final int FILE_REQUEST = 701;
    private ValueCallback<Uri[]> fileCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.web_view);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true);

        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost() == null ? "" : uri.getHost();
                if (host.equals("scortetecniche.rf.gd")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
                catch (ActivityNotFoundException error) { Toast.makeText(MainActivity.this, "Collegamento non disponibile.", Toast.LENGTH_SHORT).show(); }
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try { startActivityForResult(params.createIntent(), FILE_REQUEST); }
                catch (ActivityNotFoundException error) { fileCallback=null; return false; }
                return true;
            }
        });
        webView.setDownloadListener((url,userAgent,disposition,mime,length)->{
            DownloadManager.Request request=new DownloadManager.Request(Uri.parse(url));
            request.addRequestHeader("Cookie",CookieManager.getInstance().getCookie(url));
            request.addRequestHeader("User-Agent",userAgent);
            request.setMimeType(mime);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,android.webkit.URLUtil.guessFileName(url,disposition,mime));
            ((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(request);
            Toast.makeText(this,"Download avviato.",Toast.LENGTH_SHORT).show();
        });
        if(savedInstanceState==null) webView.loadUrl(HOME); else webView.restoreState(savedInstanceState);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==FILE_REQUEST&&fileCallback!=null){fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode,data));fileCallback=null;}
    }

    @Override protected void onSaveInstanceState(Bundle state){webView.saveState(state);super.onSaveInstanceState(state);}

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
