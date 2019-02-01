package cometchat.inscripts.com.cometchatui;

import android.app.Application;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;

import com.inscripts.orm.SugarContext;

import receivers.NetworkChangeReceiver;

/**
 * Created by Inscripts on 21/06/17.
 */

public class CCApplication extends Application {
    private static final java.lang.String TAG = CCApplication.class.getSimpleName();
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
