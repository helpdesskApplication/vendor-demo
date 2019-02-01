package cometchat.inscripts.com.readyui;

import android.app.Activity;
import android.app.Application;
import android.arch.lifecycle.Lifecycle;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.support.multidex.MultiDexApplication;
import android.util.Log;
import android.widget.Toast;

import com.inscripts.enums.StatusOption;
import com.inscripts.interfaces.Callbacks;
import com.inscripts.orm.SugarContext;
import com.inscripts.utils.Logger;

import org.json.JSONObject;

import activities.CometChatActivity;
import cometchat.inscripts.com.cometchatcore.coresdk.CometChat;
import receivers.NetworkChangeReceiver;

/**
 * Created by Jitvar on 10/3/2017.
 */

public class CCCometchatUI extends MultiDexApplication implements Application.ActivityLifecycleCallbacks{
    private static final java.lang.String TAG = CCCometchatUI.class.getSimpleName();
    NetworkChangeReceiver networkChangeReceiver;
    CometChat cometChat;
    private int activityReferences = 0;
    private boolean isActivityChangingConfigurations = false;

    @Override
    public void onCreate() {
        super.onCreate();
        SugarContext.init(this);
        networkChangeReceiver=new NetworkChangeReceiver();
        if (Build.VERSION.SDK_INT >= 24) { registerReceiver(networkChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)); }
        registerActivityLifecycleCallbacks(this);
        cometChat = CometChat.getInstance(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        SugarContext.terminate();
        unregisterReceiver(networkChangeReceiver);
        Log.e(TAG,"onTerminate called");
    }


    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (++activityReferences == 1 && !isActivityChangingConfigurations) {
            cometChat.setStatus(StatusOption.AVAILABLE, new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"Status set to AVAILABLE");
                }

                @Override
                public void failCallback(JSONObject jsonObject) {

                }
            });
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {
        Logger.error(TAG, "onActivityPaused: "+activity.getLocalClassName());
    }

    @Override
    public void onActivityStopped(Activity activity) {
        Logger.error(TAG, "onActivityStopped: "+activity.getLocalClassName());
        isActivityChangingConfigurations = activity.isChangingConfigurations();
        if (--activityReferences == 0 && !isActivityChangingConfigurations) {
            cometChat.setStatus(StatusOption.OFFLINE, new Callbacks() {
                @Override
                public void successCallback(JSONObject jsonObject) {
                    Logger.error(TAG,"Status set to OFFLINE");
                }

                @Override
                public void failCallback(JSONObject jsonObject) {

                }
            });
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        Logger.error(TAG, "onActivityDestroyed: "+activity.getLocalClassName());
    }
}
