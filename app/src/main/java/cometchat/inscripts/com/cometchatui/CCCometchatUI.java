package cometchat.inscripts.com.cometchatui;

import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.support.multidex.MultiDexApplication;

import com.inscripts.orm.SugarContext;

import receivers.NetworkChangeReceiver;

/**
 * Created by inscripts on 23/6/17.
 */

public class CCCometchatUI extends MultiDexApplication {
    private static final java.lang.String TAG = CCCometchatUI.class.getSimpleName();
    NetworkChangeReceiver networkChangeReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        SugarContext.init(this);
        networkChangeReceiver=new NetworkChangeReceiver();
        if (Build.VERSION.SDK_INT >= 24) { registerReceiver(networkChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)); }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        SugarContext.terminate();
        unregisterReceiver(networkChangeReceiver);
    }

}
