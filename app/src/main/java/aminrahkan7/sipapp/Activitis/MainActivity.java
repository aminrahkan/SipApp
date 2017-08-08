package aminrahkan7.sipapp.Activitis;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.abtollc.sdk.AbtoApplication;
import org.abtollc.sdk.AbtoPhone;
import org.abtollc.sdk.AbtoPhoneCfg;
import org.abtollc.sdk.OnInitializeListener;
import org.abtollc.sdk.OnRegistrationListener;
import org.abtollc.utils.codec.Codec;

import aminrahkan7.sipapp.R;
import aminrahkan7.sipapp.Services.IncomingCallService;

public class MainActivity extends AppCompatActivity implements OnInitializeListener {


    private AbtoPhone abtoPhone;
    private TextView tvRegisterState;
    private TextView tvCallStatus;

    private EditText edServerAddress;
    private EditText edSipUserToCall;
    private EditText edSipPassToRegister;
    private EditText edSipUserName;

    private boolean ReadyToCall = false;
    private boolean sipRegister = false;
    private boolean abtoRegister = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        abtoPhone = ((AbtoApplication) getApplication()).getAbtoPhone();
        initViews();

        initPhone();


    }

    private void initViews() {
        tvRegisterState = findViewById(R.id.tvRegisterState);
        tvCallStatus = findViewById(R.id.tvCallStatus);

        edSipUserName = findViewById(R.id.edSipUserName);
        edSipPassToRegister = findViewById(R.id.edSipPassToRegister);
        edServerAddress = findViewById(R.id.edServerAddress);
        edSipUserToCall = findViewById(R.id.edSipUserToCall);
    }


    protected void initPhone() {
        Log.i("SipTest", "initPhone: start");
        abtoPhone.setInitializeListener(this);

        //configure phone instance
        AbtoPhoneCfg config = abtoPhone.getConfig();
        config.setCodecPriority(Codec.G729, (short) 0);
        config.setCodecPriority(Codec.PCMU, (short) 250);
        config.setCodecPriority(Codec.PCMA, (short) 240);
        config.setCodecPriority(Codec.GSM, (short) 0);
        //config.setCodecPriority(Codec.speex_8000, (short) 0);
        //config.setCodecPriority(Codec.speex_16000, (short) 0);
        //config.setCodecPriority(Codec.speex_32000, (short) 0);


        config.setSignallingTransport(AbtoPhoneCfg.SignalingTransportType.UDP);
//        config.setSignallingTransport(AbtoPhoneCfg.SignalingTransportType.TCP);
        //config.setSignallingTransport(AbtoPhoneCfg.SignalingTransportType.TLS);
        //config.setTLSVerifyServer(false);

        //to establish secure call set this option to true
        config.setUseSRTP(false);

        org.abtollc.utils.Log.setLogLevel(5);
        org.abtollc.utils.Log.setUseFile(true);

        // Start initializing - !has to be invoked only once, when  app started!
        abtoPhone.initialize();
        Log.i("SipTest", "initPhone: end");
    }


    @Override
    public void onInitializeState(OnInitializeListener.InitializeState state, String message) {

        Log.i("SipTest", "onInitializeState");
        switch (state) {
            case START:
                Log.i("SipTest", "Start");
            case INFO:
                Log.i("SipTest", "INFO");
            case WARNING:
                Log.i("SipTest", "WARNING");
                break;
            case FAIL:
                Log.i("SipTest", "FAIL");

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Error")
                        .setMessage(message)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dlg, int which) {
                                dlg.dismiss();

                            }
                        }).create().show();
                break;
            case SUCCESS:
                abtoRegister = true;
                Log.i("SipTest", "SUCCESS");
                //Intent intent = new Intent(this, RegisterActivity.class);
                //startActivity(intent);
                //finish();
//                registerUser();


                break;

            default:
                break;
        }
    }

    public void onDestroy() {
        abtoPhone.setInitializeListener(null);
        super.onDestroy();

    }//onDestroy

    public void registerUser() {
//        try {

            if (abtoRegister) {
                String sipServerAddress = edServerAddress.getText().toString();
                String sipUserName = edSipUserName.getText().toString();
                String sipPassToRegister = edSipPassToRegister.getText().toString();


                if (sipServerAddress.length() == 0) {
                    edServerAddress.setError("server address not valid");
                    return;
                }
                if (sipServerAddress.length() == 0) {
                    edSipPassToRegister.setError("Sip pass not valid");
                    return;
                }
                if (sipServerAddress.length() == 0) {
                    edSipUserName.setError("sip username not valid");
                    return;
                }
                Log.e("SipTest", " domin : " + sipServerAddress + " user : " + sipUserName + " pass : " + sipPassToRegister);
                long accId = abtoPhone.getConfig().addAccount(sipServerAddress, null, sipUserName, sipPassToRegister, null, "", 300, false);//


                //Register
                try {
                    Log.e("SipTest", "registerUser register");
                    abtoPhone.register();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }


                // Set registration event
                abtoPhone.setRegistrationStateListener(new OnRegistrationListener() {

                    public void onRegistrationFailed(long accId, int statusCode, String statusText) {

                        tvRegisterState.setText("Not Register ...");
                        tvRegisterState.setTextColor(Color.RED);
                        Log.e("SipTest", "registerUser fail status code " + statusCode + " " + statusText);
//                    registerUser();
                    }

                    public void onRegistered(long accId) {

                        Log.e("SipTest", "registerUser register");

                        //Unsubscribe reg events
                        abtoPhone.setRegistrationStateListener(null);

                        tvRegisterState.setText("Register ...");
                        tvRegisterState.setTextColor(Color.GREEN);
                        //Start incoming call service

                        startReceiveSipCallService();
                        //Start main activity
                    }

                    @Override
                    public void onUnRegistered(long arg0) {
                        Log.e("SipTest", "registerUser UnRegister");
                        tvRegisterState.setText("Not Register ...");
                        tvRegisterState.setTextColor(Color.RED);
                    }
                }); //registration listener
            }
//        } catch (Exception e) {
//            Log.e("SipTest", "registerUser exeption: " + e.getMessage());
//
//        }

    }

    private void startReceiveSipCallService() {
        startService(new Intent(MainActivity.this, IncomingCallService.class));
    }

    public void callClicked(View view) {

        if (ReadyToCall && sipRegister) {
            Intent i = CallActivity.newInstance(MainActivity.this, false);
            i.putExtra(CallActivity.SIP_NAME_TO_CALL, edSipUserToCall.getText().toString());
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }
    }

    public void registerClicked(View view) {
        if (abtoRegister) registerUser();
        else Log.e("SipTest", "abto not registerd: ");
    }

    public void initDefaultValues(View view) {
        edSipUserName.setText("tsit");
        edSipPassToRegister.setText("86412129");
        edServerAddress.setText("sip.linphone.org");
        edSipUserToCall.setText("aminrahkan");
    }
}