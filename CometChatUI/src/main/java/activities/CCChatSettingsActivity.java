/*

CometChat
Copyright (c) 2016 Inscripts
License: https://www.cometchat.com/legal/license

*/
package activities;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.factories.URLFactory;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.helpers.VolleyHelper;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.interfaces.VolleyAjaxCallbacks;
import com.inscripts.keys.CometChatKeys;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.Logger;

import org.json.JSONObject;

import cometchat.inscripts.com.cometchatcore.coresdk.CCUIHelper;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;

public class CCChatSettingsActivity extends AppCompatActivity {

    private static final java.lang.String TAG = CCChatSettingsActivity.class.getSimpleName();
    private Toolbar toolbar;
    private RelativeLayout ccContainer;

    private SwitchCompat soundSwitch, vibrateSwitch, readTickSwitch, lastseenswitch;
    private SwitchCompat notificationSwitch;

    private String flag = "true";
    private boolean isChatSetting = true;
    private int colorPrimary, colorPrimaryDark;
    private CometChat cometChat;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cc_activity_notification_settings);

        this.setTitle(this.getTitle());

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        cometChat = CometChat.getInstance(this);
        if((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))){
            ccContainer = (RelativeLayout) findViewById(R.id.cc_settings_container);
            CCUIHelper.convertActivityToPopUpView(this, ccContainer, toolbar);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        try {
            setCCTheme();
            setupFields();
            setupSwitches();
            setupFieldsListeners();
            setThemeColor();
        } catch (Exception ex) {
            ex.printStackTrace();
        }


        /*notificationContainer = (RelativeLayout) findViewById(R.id.relativeLayoutNotificationContainer);
        soundContainer = (RelativeLayout) findViewById(R.id.relativeLayoutSoundContainer);
        vibrateContainer = (RelativeLayout) findViewById(R.id.relativeLayoutVibrateContainer);*/

    }

    private void setCCTheme(){
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
            TextView notificationTitle, soundTitle, vibrateTitle, readTickText, readTitle, lastSeenText, lastSeenSettingText, notificationText;
            View readlineview, lastSeenView;
            RelativeLayout relativeLayoutReadTickContainer, lastSeenRelative;
            RelativeLayout chatSettingContainer, notificationContainer;

            notificationSwitch = (SwitchCompat) findViewById(R.id.switchNotification);
            soundSwitch = (SwitchCompat) findViewById(R.id.switchNotificationSound);
            vibrateSwitch = (SwitchCompat) findViewById(R.id.switchNotificationVibrate);
            readTickSwitch = (SwitchCompat) findViewById(R.id.switchReadTick);
            lastseenswitch = (SwitchCompat) findViewById(R.id.switchLastSeen);

            notificationTitle = (TextView) findViewById(R.id.ShowNotificationText);
            soundTitle = (TextView) findViewById(R.id.textViewNotificaionSound);
            vibrateTitle = (TextView) findViewById(R.id.textViewNotificationVibrate);
            readTickText = (TextView) findViewById(R.id.textViewReadTickToggle);
            readTitle = (TextView) findViewById(R.id.readReceiptText);
            lastSeenText = (TextView) findViewById(R.id.textViewLastSeenToggle);
            lastSeenSettingText = (TextView) findViewById(R.id.lastSeenSettingText);
            notificationText = (TextView) findViewById(R.id.textViewNotificationToggle);
            chatSettingContainer = (RelativeLayout) findViewById(R.id.chatSettingContainer);
            notificationContainer = (RelativeLayout) findViewById(R.id.relativeLayoutNotificationContainer);
            relativeLayoutReadTickContainer = (RelativeLayout) findViewById(R.id.relativeLayoutReadTickContainer);
            lastSeenRelative = (RelativeLayout) findViewById(R.id.lastSeenRelative);
            readlineview = (View) findViewById(R.id.lineViewReadTick);

        isChatSetting = getIntent().getBooleanExtra("isChatSetting", false);

        if (isChatSetting) {
            chatSettingContainer.setVisibility(View.VISIBLE);
            notificationContainer.setVisibility(View.GONE);
            this.setTitle((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_CHAT_SETTINGS)));
        } else {
            chatSettingContainer.setVisibility(View.GONE);
            notificationContainer.setVisibility(View.VISIBLE);
            this.setTitle((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_NOTIFICATION_SETTINGS)));
        }

        if ((Boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.SHOW_TICKS)) &&(Boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.RECEIPT_ENABLE))) {
            relativeLayoutReadTickContainer.setVisibility(View.VISIBLE);
            readlineview.setVisibility(View.VISIBLE);
        } else {
            relativeLayoutReadTickContainer.setVisibility(View.GONE);
            readlineview.setVisibility(View.GONE);
        }

        if ((Boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.FEATURE,SettingSubType.LAST_SEEN_ENABLED))) {
            lastSeenRelative.setVisibility(View.VISIBLE);
            // lastSeenView.setVisibility(View.VISIBLE);
        } else {
            lastSeenRelative.setVisibility(View.GONE);
            //  lastSeenView.setVisibility(View.GONE);
        }


            notificationTitle.setText((String) cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE, SettingSubType.LANG_NOTIFICATION_TITLE)));

        notificationText.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_SHOW_NOTIFICATIONS)));
        soundTitle.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_SOUND)));
        vibrateTitle.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_VIBRATE)));
        readTickText.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_READ_TICK_TEXT)));
        /*if (null != mobileLangs.get167()) {
            //readTickText.setText(mobileLangs.get167());
        }*/
        readTitle.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_READ_RECEIPT_SETTING)));
           /* if (null != mobileLangs.get169()) {
                setActionBarTitle(mobileLangs.get169());
            }*/
            lastSeenSettingText.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_LAST_SEEN_SETTING)));
       /* if (null != mobileLangs.get170()) {

        }*/
        lastSeenText.setText((String)cometChat.getCCSetting(new CCSettingMapper(SettingType.LANGUAGE,SettingSubType.LANG_LAST_SEEN_MESSAGE)));
        /*if (null != mobileLangs.get171()) {
            //lastSeenText.setText(mobileLangs.get171());
        }*/
    }


    private void setupFieldsListeners() {
            readTickSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        readTickSwitch.getThumbDrawable().setColorFilter(colorPrimary, PorterDuff.Mode.MULTIPLY);
                        readTickSwitch.getTrackDrawable().setColorFilter(Color.parseColor(getTransparentPrimaryColor("#99")), PorterDuff.Mode.MULTIPLY);
                        PreferenceHelper.save(PreferenceKeys.UserKeys.READ_TICK, "1");
                    } else {
                        readTickSwitch.getThumbDrawable().setColorFilter(Color.parseColor("#E0E0E0"), PorterDuff.Mode.MULTIPLY);
                        readTickSwitch.getTrackDrawable().setColorFilter(Color.parseColor("#BDBDBD"), PorterDuff.Mode.MULTIPLY);
                        PreferenceHelper.save(PreferenceKeys.UserKeys.READ_TICK, "0");
                    }
                }
            });


            lastseenswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {

                    Logger.error("Last seen changed listner isChecked = " + isChecked);
                    if (isChecked) {
                        lastseenswitch.getThumbDrawable().setColorFilter(colorPrimary, PorterDuff.Mode.MULTIPLY);
                        lastseenswitch.getTrackDrawable().setColorFilter(Color.parseColor(getTransparentPrimaryColor("#99")), PorterDuff.Mode.MULTIPLY);
                    } else {
                        lastseenswitch.getThumbDrawable().setColorFilter(Color.parseColor("#E0E0E0"), PorterDuff.Mode.MULTIPLY);
                        lastseenswitch.getTrackDrawable().setColorFilter(Color.parseColor("#BDBDBD"), PorterDuff.Mode.MULTIPLY);
                    }

                    cometChat.changeLastSeenSetting(isChecked, new Callbacks() {
                        @Override
                        public void successCallback(JSONObject jsonObject) {
                          Logger.error(TAG,"changeLastSeenSetting(): successCallback: "+jsonObject);
                          Logger.error(TAG,"Last seen enabled: "+isChecked);
                          if(isChecked){
                              PreferenceHelper.save(PreferenceKeys.UserKeys.LAST_SEEN_SETTING,"1");
                          }else {
                              PreferenceHelper.save(PreferenceKeys.UserKeys.LAST_SEEN_SETTING,"0");
                          }
                        }

                        @Override
                        public void failCallback(JSONObject jsonObject) {
                            Logger.error(TAG,"changeLastSeenSetting(): failCallback: "+jsonObject);
                        }
                    });
                }
            });

            notificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        notificationSwitch.getThumbDrawable().setColorFilter(colorPrimary, PorterDuff.Mode.MULTIPLY);
                        notificationSwitch.getTrackDrawable().setColorFilter(Color.parseColor(getTransparentPrimaryColor("#99")), PorterDuff.Mode.MULTIPLY);
                        PreferenceHelper.save(PreferenceKeys.UserKeys.NOTIFICATION_ON, "1");
                        activiateSwitches(true);
                    } else {
                        notificationSwitch.getThumbDrawable().setColorFilter(Color.parseColor("#E0E0E0"), PorterDuff.Mode.MULTIPLY);
                        notificationSwitch.getTrackDrawable().setColorFilter(Color.parseColor("#BDBDBD"), PorterDuff.Mode.MULTIPLY);
                        PreferenceHelper.save(PreferenceKeys.UserKeys.NOTIFICATION_ON, "0");
                        activiateSwitches(false);
                    }
                }
            });

            soundSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        soundSwitch.getThumbDrawable().setColorFilter(colorPrimary, PorterDuff.Mode.MULTIPLY);
                        soundSwitch.getTrackDrawable().setColorFilter(Color.parseColor(getTransparentPrimaryColor("#99")), PorterDuff.Mode.MULTIPLY);
                        PreferenceHelper.save(PreferenceKeys.UserKeys.NOTIFICATION_SOUND, "1");
                    } else {
                        soundSwitch.getThumbDrawable().setColorFilter(Color.parseColor("#E0E0E0"), PorterDuff.Mode.MULTIPLY);
                        soundSwitch.getTrackDrawable().setColorFilter(Color.parseColor("#BDBDBD"), PorterDuff.Mode.MULTIPLY);
                        PreferenceHelper.save(PreferenceKeys.UserKeys.NOTIFICATION_SOUND, "0");
                    }
                }
            });

            vibrateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        vibrateSwitch.getThumbDrawable().setColorFilter(colorPrimary, PorterDuff.Mode.MULTIPLY);
                        vibrateSwitch.getTrackDrawable().setColorFilter(Color.parseColor(getTransparentPrimaryColor("#99")), PorterDuff.Mode.MULTIPLY);
                        PreferenceHelper.save(PreferenceKeys.UserKeys.NOTIFICATION_VIBRATE, "1");
                    } else {
                        vibrateSwitch.getThumbDrawable().setColorFilter(Color.parseColor("#E0E0E0"), PorterDuff.Mode.MULTIPLY);
                        vibrateSwitch.getTrackDrawable().setColorFilter(Color.parseColor("#BDBDBD"), PorterDuff.Mode.MULTIPLY);
                        PreferenceHelper.save(PreferenceKeys.UserKeys.NOTIFICATION_VIBRATE, "0");
                    }
                }
            });
    }

    @SuppressLint("NewApi")
    private void setupSwitches() {
            if (PreferenceHelper.get(PreferenceKeys.UserKeys.NOTIFICATION_ON).equals("1")) {
                notificationSwitch.setChecked(true);
                activiateSwitches(true);
                soundSwitch.setChecked(PreferenceHelper.get(PreferenceKeys.UserKeys.NOTIFICATION_SOUND).equals("1"));
                vibrateSwitch.setChecked(PreferenceHelper.get(PreferenceKeys.UserKeys.NOTIFICATION_VIBRATE).equals("1"));
            } else {
                notificationSwitch.setChecked(false);
                activiateSwitches(false);
            }
            if (PreferenceHelper.get(PreferenceKeys.UserKeys.READ_TICK).equals("1")) {
                readTickSwitch.setChecked(true);
            } else {
                readTickSwitch.setChecked(false);
            }

            Logger.error("Last seen setting = " + PreferenceHelper.get(PreferenceKeys.UserKeys.LAST_SEEN_SETTING));
            if (PreferenceHelper.get(PreferenceKeys.UserKeys.LAST_SEEN_SETTING).equals("1")) {
                lastseenswitch.setChecked(true);
            } else {
                lastseenswitch.setChecked(false);
            }
    }

    private void activiateSwitches(boolean isActivated) {
            soundSwitch.setClickable(isActivated);
            soundSwitch.setEnabled(isActivated);
            soundSwitch.setChecked(isActivated);
            vibrateSwitch.setClickable(isActivated);
            vibrateSwitch.setEnabled(isActivated);
            vibrateSwitch.setChecked(isActivated);
    }

    private void setThemeColor() {
            if (soundSwitch.isChecked()) {
                soundSwitch.getThumbDrawable().setColorFilter(colorPrimary, PorterDuff.Mode.MULTIPLY);
                soundSwitch.getTrackDrawable().setColorFilter(Color.parseColor(getTransparentPrimaryColor("#99")), PorterDuff.Mode.MULTIPLY);
            } else {
                soundSwitch.getThumbDrawable().setColorFilter(Color.parseColor("#E0E0E0"), PorterDuff.Mode.MULTIPLY);
                soundSwitch.getTrackDrawable().setColorFilter(Color.parseColor("#BDBDBD"), PorterDuff.Mode.MULTIPLY);
            }

            if (vibrateSwitch.isChecked()) {
                vibrateSwitch.getThumbDrawable().setColorFilter(colorPrimary, PorterDuff.Mode.MULTIPLY);
                vibrateSwitch.getTrackDrawable().setColorFilter(Color.parseColor(getTransparentPrimaryColor("#99")), PorterDuff.Mode.MULTIPLY);
            } else {
                vibrateSwitch.getThumbDrawable().setColorFilter(Color.parseColor("#E0E0E0"), PorterDuff.Mode.MULTIPLY);
                vibrateSwitch.getTrackDrawable().setColorFilter(Color.parseColor("#BDBDBD"), PorterDuff.Mode.MULTIPLY);
            }

            if (notificationSwitch.isChecked()) {
                notificationSwitch.getThumbDrawable().setColorFilter(colorPrimary, PorterDuff.Mode.MULTIPLY);
                notificationSwitch.getTrackDrawable().setColorFilter(Color.parseColor(getTransparentPrimaryColor("#99")), PorterDuff.Mode.MULTIPLY);
            } else {
                notificationSwitch.getThumbDrawable().setColorFilter(Color.parseColor("#E0E0E0"), PorterDuff.Mode.MULTIPLY);
                notificationSwitch.getTrackDrawable().setColorFilter(Color.parseColor("#BDBDBD"), PorterDuff.Mode.MULTIPLY);
            }

            if (readTickSwitch.isChecked()) {
                readTickSwitch.getThumbDrawable().setColorFilter(colorPrimary, PorterDuff.Mode.MULTIPLY);
                readTickSwitch.getTrackDrawable().setColorFilter(Color.parseColor(getTransparentPrimaryColor("#99")), PorterDuff.Mode.MULTIPLY);
            } else {
                readTickSwitch.getThumbDrawable().setColorFilter(Color.parseColor("#E0E0E0"), PorterDuff.Mode.MULTIPLY);
                readTickSwitch.getTrackDrawable().setColorFilter(Color.parseColor("#BDBDBD"), PorterDuff.Mode.MULTIPLY);
            }

            if (lastseenswitch.isChecked()) {
                lastseenswitch.getThumbDrawable().setColorFilter(colorPrimary, PorterDuff.Mode.MULTIPLY);
                lastseenswitch.getTrackDrawable().setColorFilter(Color.parseColor(getTransparentPrimaryColor("#99")), PorterDuff.Mode.MULTIPLY);
            } else {
                lastseenswitch.getThumbDrawable().setColorFilter(Color.parseColor("#E0E0E0"), PorterDuff.Mode.MULTIPLY);
                lastseenswitch.getTrackDrawable().setColorFilter(Color.parseColor("#BDBDBD"), PorterDuff.Mode.MULTIPLY);
            }
    }

    private String getTransparentPrimaryColor(String t) {
        String p =  (String) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.COLOR_PRIMARY_STRING));
        p = t + (p.substring(1, p.length()));
        return p;
    }
}
