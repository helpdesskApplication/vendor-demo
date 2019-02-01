package services;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import helpers.CCSubcribe;

public class CCSubscribeService extends Service {

    private String TAG = CCSubscribeService.class.getSimpleName();
    private IBinder myBinder = new MyBinder();
    private Activity mActivity;


    public CCSubscribeService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        CCSubcribe.SubcribeToCometChat(mActivity);
        return myBinder;
    }

    public class MyBinder extends Binder {
        public CCSubscribeService getService(Activity activity) {
            mActivity = activity;
            return CCSubscribeService.this;
        }
    }
}
