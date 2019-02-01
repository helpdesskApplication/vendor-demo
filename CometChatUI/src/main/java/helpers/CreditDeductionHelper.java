package helpers;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import rolebase.RolebaseFeatures;
import services.CreditDeductionService;

/**
 * Created by Akash on 4/17/2018.
 */

public class CreditDeductionHelper {
    private static final String TAG = CreditDeductionHelper.class.getSimpleName();
    private Context context;
    private IntentFilter intentFilter = new IntentFilter();
    private static Timer timer;


    public static void deductCredit(final Context context,final String baseData, final String toId, final String featureType, final String featureName, final int isGroup,final int interval ) {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
           synchronized public void run() {
                Log.e(TAG, "initializeTimerTask() : parameters for credit deduction: \n"+"baseData: "+baseData+"\n"+"toId: "
                        +toId+"\n"+"featureType: "+featureType+"\n"+"featureName: "+featureName+"\n"+"isGroup: "+isGroup);

                Intent creditDeductionIntent = new Intent(context, CreditDeductionService.class);
                creditDeductionIntent.putExtra("baseData", baseData);
                creditDeductionIntent.putExtra("toId", toId);
                creditDeductionIntent.putExtra("featureType", featureType);
                creditDeductionIntent.putExtra("featureName", featureName);
                creditDeductionIntent.putExtra("isGroup", isGroup);
                context.startService(creditDeductionIntent);
            }
        },0,interval);
    }



    private static void stoptimertask() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public static void stopCreditDeduction(){
        Log.d(TAG, "credit deduction stopped");
        stoptimertask();
    }

    public static int getAVchatDeductiomIntervalinMillis(){
        return RolebaseFeatures.getDeductionIntervalForAVChat() * 60 * 1000;
    }

    public static int getAudiochatDeductiomIntervalinMillis(){
        return RolebaseFeatures.getDeductionIntervalForAudioChat() * 60 * 1000;
    }
}
