package services;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import com.inscripts.interfaces.Callbacks;
import com.inscripts.utils.Logger;

import org.json.JSONObject;

import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;


/**
 * Created by Akash on 4/17/2018.
 */

public class CreditDeductionService extends IntentService {
    private static final String TAG = CreditDeductionService.class.getSimpleName();
    private CometChat cometChat = CometChat.getInstance(this);
    public CreditDeductionService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String baseData = intent.getStringExtra("baseData");
        String toId = intent.getStringExtra("toId");
        String featureType = intent.getStringExtra("featureType");
        String featureName = intent.getStringExtra("featureName");
        int isGroup = intent.getIntExtra("isGroup",0);
        Log.d(TAG, "creditsToDeduct: ");
        cometChat.deductCredits(baseData, toId, featureType, featureName, isGroup, new Callbacks() {
            @Override
            public void successCallback(JSONObject jsonObject) {
                Logger.error(TAG,"deductCredits successCallback: "+jsonObject);
            }

            @Override
            public void failCallback(JSONObject jsonObject) {
                Logger.error(TAG,"deductCredits failCallback: "+jsonObject);
            }
        });
    }
}
