package aminrahkan7.sipapp.Activitis;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import org.abtollc.sdk.AbtoApplication;
import org.abtollc.sdk.AbtoPhone;
import org.abtollc.sdk.AbtoPhoneCfg;
import org.abtollc.sdk.OnInitializeListener;
import org.abtollc.utils.codec.Codec;

import aminrahkan7.sipapp.R;

public class MainActivity extends AppCompatActivity implements OnInitializeListener {


    private AbtoPhone abtoPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        abtoPhone = ((AbtoApplication) getApplication()).getAbtoPhone();
        initPhone();


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

    int registerOk = 0;

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
                registerOk = 1;
                Log.i("SipTest", "SUCCESS");
//                Intent intent = new Intent(this, RegisterActivity.class);
//                startActivity(intent);
//                finish();

                registerUser();


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

    }

    public void callClicked(View view) {
        Intent i = CallActivity.newInstance(MainActivity.this, false);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

}