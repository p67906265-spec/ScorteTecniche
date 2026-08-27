package it.paolo.scortetecniche;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView webView;
    private static final String DEFAULT_HOME = "https://scortetecniche.rf.gd/";
    private static final String PREFS = "scorte_tecniche_settings";
    private static final String PREF_SITE_URL = "site_url";
    private static final int FILE_REQUEST = 701;
    private ValueCallback<Uri[]> fileCallback;
    private String siteUrl;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.web_view);
        siteUrl = getSharedPreferences(PREFS, MODE_PRIVATE).getString(PREF_SITE_URL, DEFAULT_HOME);
        findViewById(R.id.settings_button).setOnClickListener(view -> showSiteSettings());

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
                String configuredHost = Uri.parse(siteUrl).getHost();
                if (configuredHost != null && host.equals(configuredHost)) return false;
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
        if(savedInstanceState==null) {
            // Un nuovo avvio dell'app deve sempre richiedere il login.
            WebStorage.getInstance().deleteAllData();
            cm.removeAllCookies(removed -> {
                cm.flush();
                webView.clearHistory();
                webView.loadUrl(siteUrl);
            });
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private void showSiteSettings() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("https://esempio.it/");
        input.setText(siteUrl);
        input.setSelectAllOnFocus(true);
        int padding=(int)(20*getResources().getDisplayMetrics().density);
        input.setPadding(padding,padding/2,padding,padding/2);
        AlertDialog dialog=new AlertDialog.Builder(this)
            .setTitle("Indirizzo del sito")
            .setMessage("Inserisci l'indirizzo completo al quale deve collegarsi l'app.")
            .setView(input).setNegativeButton("Annulla",null)
            .setNeutralButton("Ripristina",null).setPositiveButton("Salva",null).create();
        dialog.setOnShowListener(ignored->{
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view->{
                String value=normalizeSiteUrl(input.getText().toString());
                Uri uri=Uri.parse(value);
                if(!("https".equalsIgnoreCase(uri.getScheme())||"http".equalsIgnoreCase(uri.getScheme()))||uri.getHost()==null){input.setError("Inserisci un indirizzo valido");return;}
                saveAndOpenSite(value);dialog.dismiss();
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view->{saveAndOpenSite(DEFAULT_HOME);dialog.dismiss();});
        });
        dialog.show();
    }

    private String normalizeSiteUrl(String value){value=value.trim();if(!value.contains("://"))value="https://"+value;return value.endsWith("/")?value:value+"/";}
    private void saveAndOpenSite(String value){siteUrl=value;getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString(PREF_SITE_URL,value).apply();webView.clearHistory();webView.loadUrl(value);Toast.makeText(this,"Indirizzo del sito salvato.",Toast.LENGTH_SHORT).show();}

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
