package activities;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.inscripts.enums.SettingSubType;
import com.inscripts.enums.SettingType;
import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.keys.PreferenceKeys;
import com.inscripts.pojos.CCSettingMapper;
import com.inscripts.utils.Logger;

import cometchat.inscripts.com.cometchatcore.coresdk.CCUIHelper;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import cometchat.inscripts.com.readyui.R;

public class CCSelectLanguageActivity extends AppCompatActivity {

    private static final java.lang.String TAG = CCSettingsActivity.class.getSimpleName();
    private Toolbar toolbar;
    private RelativeLayout ccContainer;

    private ListView lstLanguage;

    private String[] langOptions, langCodes;
    private int selectedPos;
    private int colorPrimary, colorPrimaryDark;
    private CometChat cometChat;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cc_activity_select_language);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        cometChat = CometChat.getInstance(this);
        if((boolean) cometChat.getCCSetting(new CCSettingMapper(SettingType.UI_SETTINGS, SettingSubType.IS_POPUPVIEW))){
            ccContainer = (RelativeLayout) findViewById(R.id.cc_settings_container);
            CCUIHelper.convertActivityToPopUpView(this,ccContainer,toolbar);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setCCTheme();
        setupFieldsAndVariables();

        setupLanguageList();
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

    private void setupFieldsAndVariables() {
        lstLanguage = (ListView) findViewById(R.id.listViewLanguage);

        langOptions = new String[]{"Default", "Afrikaans", "Albanian", "Arabic", "Belarusian", "Bulgarian",
                "Catalan", "Chinese (Simpl)", "Chinese (Trad)", "Croatian", "Czech", "Danish", "Dutch", "English",
                "Estonian", "Filipino", "Finnish", "French", "Galician", "German", "Greek", "Haitian Creole", "Hebrew",
                "Hindi", "Hungarian", "Icelandic", "Indonesian", "Irish", "Italian", "Japanese", "Korean", "Latvian",
                "Lithuanian", "Macedonian", "Malay", "Maltese", "Norwegian", "Persian", "Polish", "Portuguese",
                "Romanian", "Russian", "Serbian", "Slovak", "Slovenian", "Spanish", "Swahili", "Swedish", "Thai",
                "Turkish", "Ukrainian", "Vietnamese", "Welsh", "Yiddish"};

        langCodes = new String[]{"", "af", "sq", "ar", "be", "bg", "ca", "zh-CN", "zh-TW", "hr", "cs", "da", "nl",
                "en", "et", "tl", "fi", "fr", "gl", "de", "el", "ht", "iw", "hi", "hu", "is", "id", "ga", "it", "ja",
                "ko", "lv", "lt", "mk", "ms", "mt", "no", "fa", "pl", "pt", "ro", "ru", "sr", "sk", "sl", "es", "sw",
                "sv", "th", "tr", "uk", "vi", "cy", "yi"};
    }

    private void setupLanguageList() {
        LanguageAdapter adapter = new LanguageAdapter(this,langOptions);
        lstLanguage.setAdapter(adapter);
        String langCode = PreferenceHelper.get(PreferenceKeys.DataKeys.SELECTED_LANGUAGE);

        for(int i=0;i<langCodes.length;i++){
            if(langCodes[i].equals(langCode)){
                selectedPos = i;
                adapter.notifyDataSetChanged();
                break;
            }else{
                selectedPos = 0;
            }
        }
    }

    private class LanguageAdapter extends BaseAdapter {

        private String[] lang;
        private Context context;

        public LanguageAdapter(Context context , String[] lang ) {
            this.lang = lang;
            this.context = context;
        }

        @Override
        public int getCount() {
            return lang.length;
        }

        @Override
        public String getItem(int i) {
            return lang[i];
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(final int pos, View view, ViewGroup viewGroup) {

            final ViewHolder holder;

            if (null == view) {
                view = LayoutInflater.from(context)
                        .inflate(R.layout.cc_custom_list_item_select_language,viewGroup, false);

                holder = new ViewHolder();
                holder.langName = (TextView) view.findViewById(R.id.txt_Lang);
                holder.radioButton = (RadioButton) view.findViewById(R.id.radio_btn_lang);
                holder.container = (LinearLayout) view.findViewById(R.id.lang_container);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            holder.langName.setText(lang[pos]);

            ColorStateList colorStateList = new ColorStateList(
                    new int[][]{

                            new int[]{-android.R.attr.state_enabled},
                            new int[]{-android.R.attr.state_checked},
                            new int[]{android.R.attr.state_checked}
                    },
                    new int[] {
                            Color.BLACK //disabled
                            , Color.parseColor("#8e8e92")
                            , colorPrimary
                    }
            );

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            holder.radioButton.setButtonTintList(colorStateList);

            if(selectedPos == pos){
                holder.radioButton.setChecked(true);
            }else{
                holder.radioButton.setChecked(false);
            }

            holder.container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selectedPos = pos;
                    notifyDataSetChanged();
                    PreferenceHelper.save(PreferenceKeys.DataKeys.SELECTED_LANGUAGE, langCodes[pos]);
                    PreferenceHelper.save(PreferenceKeys.DataKeys.SELECTED_LANGUAGE_FULL, lang[pos]);
                    finish();
                }
            });
            return view;
        }

        class ViewHolder {
            public TextView langName;
            public RadioButton radioButton;
            public LinearLayout container;
        }
    }
}
