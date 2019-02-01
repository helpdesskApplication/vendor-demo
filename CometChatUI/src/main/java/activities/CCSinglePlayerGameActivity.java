/*

CometChat
Copyright (c) 2016 Inscripts
License: https://www.cometchat.com/legal/license

*/
package activities;


import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.factories.URLFactory;
import com.inscripts.pojos.CCSettingMapper;

import cometchat.inscripts.com.cometchatcore.coresdk.CCUIHelper;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;

public class CCSinglePlayerGameActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RelativeLayout ccContainer;
    private WebView webView;
    private int colorPrimary, colorPrimaryDark;
    private String mFirstUrlLoaded = "";
    private CometChat cometChat;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cc_activity_single_player_game);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        cometChat = CometChat.getInstance(this);
        if((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))){
            ccContainer = (RelativeLayout) findViewById(R.id.cc_single_player_game_container);
            CCUIHelper.convertActivityToPopUpView(this,ccContainer,toolbar);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setCCTheme();

        // Remove later
        /*if (null != mobileLangs && null != mobileLangs.get176()) {
            this.setTitle(mobileLangs.get176());
        }else{
            this.setTitle("Single Player Games");
        }*/
        setTitle((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_GAMES_TITLE)));

        setupWebView();
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

    private void setupWebView() {
        webView = (WebView) findViewById(R.id.webview);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(false);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setVerticalScrollBarEnabled(true);
        webView.setHorizontalScrollbarOverlay(true);
        webView.loadUrl(URLFactory.getSinglePlayerGameURL());
        mFirstUrlLoaded = URLFactory.getSinglePlayerGameURL();
        //webView.getSettings().setBuiltInZoomControls(true);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (!mFirstUrlLoaded.equals(url)){
                    webView.getSettings().setLoadWithOverviewMode(true);
                    getSupportActionBar().hide();
                }
                return super.shouldOverrideUrlLoading(view, url);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                super.onConsoleMessage(consoleMessage);
                return true;
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (!mFirstUrlLoaded.equals(webView.getUrl())) {
            webView.loadUrl(URLFactory.getSinglePlayerGameURL());
            webView.getSettings().setLoadWithOverviewMode(false);
            getSupportActionBar().show();
        } else {
            super.onBackPressed();
        }
    }
}