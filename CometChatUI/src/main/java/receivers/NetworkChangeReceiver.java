package receivers;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.inscripts.helpers.PreferenceHelper;
import com.inscripts.jsonphp.Config;
import com.inscripts.jsonphp.JsonPhp;
import com.inscripts.orm.SugarContext;
import com.inscripts.utils.Logger;
import com.inscripts.utils.SessionData;

import java.io.File;

import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import services.OfflineMessagingService;
import utils.NetworkUtil;

public class NetworkChangeReceiver extends BroadcastReceiver {
    private static final String TAG = NetworkChangeReceiver.class.getSimpleName() ;
    CometChat cometChat;
    @Override
    public void onReceive(final Context context, final Intent intent) {
        cometChat = CometChat.getInstance(context);
        int status = NetworkUtil.getConnectivityStatusString(context);
        Logger.error(TAG,"Status = "+status);
        //SugarContext.init(context);
        if( status == 1 || status == 2){
            File file = context.getDatabasePath("inscripts_cc.db");
            if(file.exists()){
                cometChat.refreshChatServices(context);
                OfflineMessagingService.enqueueWork(context, intent);
                /** resend mechanism to resend the message again after 1 min**/
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        OfflineMessagingService.enqueueWork(context, intent);
                    }
                }, 1000*60);
            }
        }
    }
}
