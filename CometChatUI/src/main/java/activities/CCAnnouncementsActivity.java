package activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.CommonUtils;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;

import cometchat.inscripts.com.cometchatcore.coresdk.CCUIHelper;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;

/**
 * Created by inscripts-236 on 8/9/17.
 */

public class CCAnnouncementsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private CometChat cometChat;
    private RelativeLayout ccContainer;
    private int colorPrimary,colorPrimaryDark;
    private WebView homesite;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView noInternetText;
    private String announcementTabUrl;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cc_activity_announcements);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        cometChat = CometChat.getInstance(this);
        if((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))){
            ccContainer = (RelativeLayout) findViewById(R.id.cc_announcement_container);
            CCUIHelper.convertActivityToPopUpView(this,ccContainer,toolbar);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.setTitle((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_ANNOUNCEMENTS)));
        setCCTheme();
        setUpFields();
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(false);
                refreshWebView();
            }
        });
        homesite.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int progress) {
                super.onProgressChanged(view, progress);
            }
        });

        homesite.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    WebView webView = (WebView) v;
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_BACK:
                            if (webView.canGoBack()) {
                                webView.goBack();
                                return true;
                            }
                            break;
                    }
                }
                return false;
            }
        });

        homesite.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(getBaseContext());
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.setAcceptCookie(true);
                cookieManager.setCookie(url, "cc_platform_cod=android;");
                cookieSyncManager.sync();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (URLUtil.isValidUrl(url)) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
//                    return true means the host application handles the url.
                    return true;
                } else {
//                    return false means the current WebView handles the url.
                    return false;
                }
            }
        });
        homesite.getSettings().setAppCacheMaxSize(5 * 1024 * 1024); // 5MB
        homesite.getSettings().setAppCachePath(this.getCacheDir().getAbsolutePath());
        homesite.getSettings().setAllowFileAccess(true);

        homesite.getSettings().setDomStorageEnabled(true);
        homesite.getSettings().setJavaScriptEnabled(true);
        homesite.getSettings().setAppCacheEnabled(true);
        homesite.getSettings().setBuiltInZoomControls(false);
        announcementTabUrl = cometChat.getAnnouncementUrl();
        Logger.error("Announcement url = " + announcementTabUrl);
        homesite.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        homesite.getSettings().setUserAgentString("cc_android");
        refreshWebView();
    }

    private void refreshWebView() {
        if (!CommonUtils.isConnected()) {
            homesite.setVisibility(View.GONE);
            noInternetText.setVisibility(View.VISIBLE);
        } else {
            homesite.setVisibility(View.VISIBLE);
            noInternetText.setVisibility(View.GONE);
            homesite.loadUrl(announcementTabUrl);
        }

    }

    private void setUpFields() {
        homesite = (WebView) findViewById(R.id.webViewAnnouncement);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        noInternetText = (TextView) findViewById(R.id.textViewNoInternet);
    }

    private void setCCTheme(){
        colorPrimary = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY));
        colorPrimaryDark = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY_DARK));
        if((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))){
            toolbar.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        }else {
            toolbar.setBackgroundColor(colorPrimary);
        }
        CCUIHelper.setStatusBarColor(this,colorPrimaryDark);
    }
}
