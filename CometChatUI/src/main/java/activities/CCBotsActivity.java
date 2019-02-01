package activities;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import adapters.BotListAdapter;
import cometchat.inscripts.com.cometchatcore.coresdk.CCUIHelper;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;
import models.Bot;

public class CCBotsActivity extends AppCompatActivity {

    private static final String TAG = CCBotsActivity.class.getSimpleName();
    private Toolbar toolbar;
    private RelativeLayout ccContainer;
    private RelativeLayout emptyView;
    private ListView lstViewBot;
    private List<Bot> botList;
    private BotListAdapter botListAdapter;
    private TextView tvNoBots;
    private ImageView ivNoBots;
    private int colorPrimary, colorPrimaryDark;
    private CometChat cometChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cc_activity_bots);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        cometChat = CometChat.getInstance(this);
        if((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))){
            ccContainer =  findViewById(R.id.cc_bots_container);
            CCUIHelper.convertActivityToPopUpView(this,ccContainer,toolbar);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setCCTheme();

        Logger.error(TAG, "Bot list hash "+ PreferenceHelper.get(PreferenceKeys.HashKeys.BOT_LIST_HASH));
        getSupportActionBar().setTitle((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_BOTS)));
        setupfields();
        setupFieldsListeners();
        if(PreferenceHelper.contains(PreferenceKeys.HashKeys.BOT_LIST_HASH) && PreferenceHelper.get(PreferenceKeys.HashKeys.BOT_LIST_HASH) != null && !PreferenceHelper.get(PreferenceKeys.HashKeys.BOT_LIST_HASH).isEmpty()){
            fetchBotList();
        }
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

    private void fetchBotList(){
        cometChat.getAllBots(new Callbacks() {
            @Override
            public void successCallback(JSONObject jsonObject) {
             Logger.errorLong(TAG,"getAllBots(): successCallback: "+jsonObject);
                try {
                    if(jsonObject.has(CometChatKeys.AjaxKeys.BOT_LIST) && jsonObject.get(CometChatKeys.AjaxKeys.BOT_LIST) instanceof JSONObject){
                        Bot.updateAllBots(jsonObject.getJSONObject(CometChatKeys.AjaxKeys.BOT_LIST));
                        updateBotList();
                    }else {
                        Logger.error(TAG,"delete All Bots");
                        Bot.deleteAll(Bot.class);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                Logger.error(TAG,"getAllBots(): failCallback: "+jsonObject);
            }
        });
    }

    private void setupfields() {
        lstViewBot = findViewById(R.id.list_of_bots);
        emptyView = findViewById(R.id.relativeLayoutbotsMessage);
        lstViewBot.setEmptyView(emptyView);
        ivNoBots = findViewById(R.id.ivNoBots);
        tvNoBots = findViewById(R.id.textViewNoBotMessage);

        tvNoBots.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_NO_BOTS)));
    }

    private void setupFieldsListeners() {
        lstViewBot.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Bot bot = botList.get(i);
                Intent intent = new Intent(getApplicationContext(), CCBotsDetailActivity.class);
                intent.putExtra("BOT_ID",bot.botId+"");
                startActivity(intent);
            }
        });
    }

    private void updateBotList() {
        botList = Bot.getAllbots();
        Logger.error("Bot list size "+botList.size());
        botListAdapter = new BotListAdapter(this,botList);
        lstViewBot.setAdapter(botListAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_bots,menu);

        menu.findItem(R.id.custom_refresh_bot_menu).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.custom_refresh_bot_menu) {
            if(PreferenceHelper.contains(PreferenceKeys.HashKeys.BOT_LIST_HASH) && PreferenceHelper.get(PreferenceKeys.HashKeys.BOT_LIST_HASH) != null && !PreferenceHelper.get(PreferenceKeys.HashKeys.BOT_LIST_HASH).isEmpty()){
                Logger.error("Hybrid bots refresh");
                SessionData.getInstance().setInitialHeartbeat(true);
                updateBotList();
            }else {
                Logger.error("Index bots refresh");
                fetchBotList();
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
