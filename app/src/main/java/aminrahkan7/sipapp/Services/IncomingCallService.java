package aminrahkan7.sipapp.Services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import org.abtollc.sdk.AbtoApplication;
import org.abtollc.sdk.AbtoPhone;
import org.abtollc.sdk.OnIncomingCallListener;
import org.abtollc.sdk.OnInitializeListener;

import aminrahkan7.sipapp.Activitis.CallActivity;

public class IncomingCallService extends Service implements OnIncomingCallListener , OnInitializeListener {

    private AbtoPhone abtoPhone;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        abtoPhone = ((AbtoApplication) getApplication()).getAbtoPhone();

        abtoPhone.setIncomingCallListener(this);
        Log.i("sipApp", "onStartCommand:2 ");

        return START_STICKY;
    }

    @Override
    public void OnIncomingCall(String remoteContact, long arg1) {
        Log.i("sipApp", "onStartCommand:2 ");
        Intent i = CallActivity.newInstance(getApplicationContext(), true);
        i.putExtra(AbtoPhone.REMOTE_CONTACT, remoteContact);
        i.putExtra(CallActivity.CALL_ID, abtoPhone.getActiveCallId());
        i.putExtra(AbtoPhone.REMOTE_CONTACT, remoteContact);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    @Override
    public void onInitializeState(OnInitializeListener.InitializeState state, String message) {
        if (state != InitializeState.SUCCESS) return;

//        long accId = abtoPhone.getConfig().addAccount(RegisterActivity.RegDomain, null, RegisterActivity.RegUser, RegisterActivity.RegPassword, null, "", 300, false);
//
//        //Register
//        try
//        {
//            abtoPhone.register();
//        }
//        catch (RemoteException ex)
//        {
//            ex.printStackTrace();
//        }
    }
}
