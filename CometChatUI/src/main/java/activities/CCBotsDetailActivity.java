package activities;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.factories.LocalStorageFactory;
import com.inscripts.pojos.CCSettingMapper;

import cometchat.inscripts.com.cometchatcore.coresdk.CCUIHelper;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import models.Bot;

public class CCBotsDetailActivity extends AppCompatActivity {

    private static final String TAG = CCBotsActivity.class.getSimpleName();
    private Toolbar toolbar;
    private RelativeLayout ccContainer;

    private ImageView botAvtarView;
    private TextView botDescription, botName;

    private int colorPrimary, colorPrimaryDark;
    private CometChat cometChat;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cc_activity_bot_detail);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        cometChat = CometChat.getInstance(this);
        if ((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))) {
            ccContainer = (RelativeLayout) findViewById(R.id.cc_bots_container);
            CCUIHelper.convertActivityToPopUpView(this, ccContainer, toolbar);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setCCTheme();
        setupFields();
        setBotDetails();
    }

    private void setCCTheme() {
        colorPrimary = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY));
        colorPrimaryDark = (int) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY_DARK));
        if((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))){
            toolbar.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
        }else {
            toolbar.setBackgroundColor(colorPrimary);
        }
        CCUIHelper.setStatusBarColor(this, colorPrimaryDark);
    }

    private void setupFields() {
        botDescription = (TextView) findViewById(R.id.txt_bot_description);
        botName = (TextView) findViewById(R.id.txt_bot_name);

        botAvtarView = (ImageView) findViewById(R.id.img_bot_avtar);
        botAvtarView.getBackground().setColorFilter(colorPrimary, PorterDuff.Mode.SRC_ATOP);
    }

    private void setBotDetails() {
        Intent intent = getIntent();
        String botId = intent.getStringExtra("BOT_ID");
        Bot bot = Bot.getBotDetails(botId);
        this.setTitle(bot.botName);
        if (bot.botDescription != null && bot.botDescription.isEmpty())
            botDescription.setText(R.string.default_bot_description);
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                botDescription.setText(Html.fromHtml(bot.botDescription, Html.FROM_HTML_MODE_LEGACY));
            } else {
                botDescription.setText(Html.fromHtml(bot.botDescription));
            }

        }
        botName.setText(bot.botName);
        LocalStorageFactory.loadImageUsingURL(this, bot.botAvatar, botAvtarView, R.drawable.cc_ic_robot);
    }
}
