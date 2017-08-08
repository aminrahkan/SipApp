package aminrahkan7.sipapp.Activitis;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import org.abtollc.sdk.AbtoApplication;
import org.abtollc.sdk.AbtoPhone;
import org.abtollc.sdk.OnCallConnectedListener;
import org.abtollc.sdk.OnCallDisconnectedListener;
import org.abtollc.sdk.OnCallHeldListener;
import org.abtollc.sdk.OnRemoteAlertingListener;
import org.abtollc.sdk.OnToneReceivedListener;

import aminrahkan7.sipapp.R;

public class CallActivity extends AppCompatActivity implements OnCallConnectedListener, OnCallDisconnectedListener, OnCallHeldListener, OnRemoteAlertingListener, OnToneReceivedListener {

    private AbtoPhone abtoPhone;
    int accExpire;


    //Views
    private TextView call_status;
    private TextView call_timer;
    private ImageButton deny_call_button;
    private ImageButton accept_call_button;
    private ImageButton btnSpeaker;
    private TextView caller_name;

    public static final String CALL_ID = "call_id";


    private boolean speakerOn = false;


    //intents
    public static final String CALL_INTENT = "inComingCall";// false outGoingCall , true inComingCall
    public static final String SIP_NAME = "sip_name";
    private boolean callType;
    private int activeCallId = AbtoPhone.INVALID_CALL_ID;
    private String remoteContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        super.onCreate(savedInstanceState);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_call);

        Bundle bundle = getIntent().getExtras();
        callType = bundle.getBoolean(CALL_INTENT);
        activeCallId = bundle.getInt(CALL_ID);
        remoteContact = getIntent().getStringExtra(AbtoPhone.REMOTE_CONTACT);


        initViews();


        abtoPhone = ((AbtoApplication) getApplication()).getAbtoPhone();


        int accId = (int) abtoPhone.getCurrentAccountId();
        accExpire = abtoPhone.getConfig().getAccountExpire(accId);

        try {
            if (!callType) {
                if (bundle.containsKey(SIP_NAME)) {
                    String sip_name = bundle.getString(SIP_NAME);
                    abtoPhone.startCall("sip:" + sip_name + "@sip.linphone.org", abtoPhone.getCurrentAccountId());
                }
            }

        } catch (RemoteException e) {
            e.printStackTrace();
        }


        initListeners();

    }

    private void initListeners() {
        abtoPhone.setCallConnectedListener(this);
        abtoPhone.setCallDisconnectedListener(this);
        abtoPhone.setOnCallHeldListener(this);
        abtoPhone.setRemoteAlertingListener(this);
        abtoPhone.setToneReceiveListener(this);
    }

    //done
    public static Intent newInstance(Context context, boolean callType) {

        Intent intent = new Intent(context, CallActivity.class);
        intent.putExtra(CALL_INTENT, callType);
        return intent;
    }

    private void initViews() {
        call_status = findViewById(R.id.call_status);
        call_timer = findViewById(R.id.call_timer);
        deny_call_button = findViewById(R.id.deny_call_button);
        accept_call_button = findViewById(R.id.accept_call_button);
        btnSpeaker = findViewById(R.id.btnSpeaker);
        caller_name = findViewById(R.id.caller_name);

        if (callType) {//inComingCall
            accept_call_button.setVisibility(ImageButton.VISIBLE);
            deny_call_button.setVisibility(ImageButton.VISIBLE);

        } else {
            accept_call_button.setVisibility(ImageButton.GONE);
            deny_call_button.setVisibility(ImageButton.VISIBLE);
        }

        caller_name.setText(remoteContact);


    }

    public void hangUpClicked(View view) {
        try {
            mHandler.removeCallbacks(mUpdateTimeTask);
            if (abtoPhone.getBeforeConfirmedCallId() == -1) {
                abtoPhone.hangUp();
            } else {
                abtoPhone.rejectCall();
            }
        } catch (RemoteException e) {

        }
        finish();
    }

    public void pickUpClicked(View view) {

        try {
            abtoPhone.answerCall(200);
        } catch (RemoteException e) {

        }
    }


    @Override
    public void onCallConnected(String remoteContact) {
        Log.i("sip -->", "onCallConnected: ");

        this.accept_call_button.setVisibility(View.GONE);
        this.call_timer.setVisibility(View.VISIBLE);

        if (mTotalTime == 0L) {
            mPointTime = System.currentTimeMillis();
            mHandler.removeCallbacks(mUpdateTimeTask);
            mHandler.postDelayed(mUpdateTimeTask, 100);
        }
    }

    @Override
    //public void onCallDisconnected(String remoteContact, int callId)//old SDK build
    public void onCallDisconnected(String remoteContact, int callId, int statusCode) {//new SDK build (after 2017/06/15)
        if (callId == abtoPhone.getAfterEndedCallId()) {
            finish();
            mTotalTime = 0;
        }
    }

    // ==========Timer==============
    private long mPointTime = 0;
    private long mTotalTime = 0;
    private Handler mHandler = new Handler();
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            Log.i("sip -->", "CallConnected run  ");
            mTotalTime += System.currentTimeMillis() - mPointTime;
            mPointTime = System.currentTimeMillis();
            int seconds = (int) (mTotalTime / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            if (seconds < 10) {
                call_timer.setText("" + minutes + ":0" + seconds);
            } else {
                call_timer.setText("" + minutes + ":" + seconds);
            }

            mHandler.postDelayed(this, 1000);
        }
    };


    public void onDestroy() {
        super.onDestroy();

        mHandler.removeCallbacks(mUpdateTimeTask);

        abtoPhone.setCallConnectedListener(null);
        abtoPhone.setCallDisconnectedListener(null);
        abtoPhone.setOnCallHeldListener(null);
        abtoPhone.setRemoteAlertingListener(null);
        abtoPhone.setToneReceiveListener(null);
    }

    public void changeSpeakerStatus(View view) {

        if (speakerOn) {
            btnSpeaker.setImageResource(R.drawable.fm_mute);
        } else {
            btnSpeaker.setImageResource(R.drawable.fm_unmute);
        }
        speakerOn = !speakerOn;


        try {
            abtoPhone.setSpeakerphoneOn(speakerOn);
        } catch (RemoteException e) {
        }

    }

    @Override
    public void onCallHeld(HoldState holdState) {

    }

    @Override
    public void onRemoteAlerting(long l, int i) {

    }

    @Override
    public void onToneReceived(char c) {

    }
}
