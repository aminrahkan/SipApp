package aminrahkan7.sipapp.Activitis;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.abtollc.sdk.AbtoApplication;
import org.abtollc.sdk.AbtoPhone;
import org.abtollc.sdk.AbtoPhoneCfg;
import org.abtollc.sdk.OnInitializeListener;
import org.abtollc.sdk.OnRegistrationListener;
import org.abtollc.utils.codec.Codec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aminrahkan7.sipapp.R;
import aminrahkan7.sipapp.Services.IncomingCallService;

public class MainActivity extends AppCompatActivity implements OnInitializeListener {

    List<Integer> a=new ArrayList<>();

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
        boolean bCanStartPhoneInitialization = (Build.VERSION.SDK_INT >= 23) ?  askPermissions() : true;
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
        abtoPhone.setInitializeListener(this);

        //configure phone instance
        AbtoPhoneCfg config = abtoPhone.getConfig();
        config.setCodecPriority(Codec.G729, (short) 0);
        config.setCodecPriority(Codec.GSM, (short) 0);
        config.setCodecPriority(Codec.PCMU, (short) 200);
        config.setCodecPriority(Codec.PCMA, (short) 100);

        config.setCodecPriority(Codec.H264, (short) 220);
        config.setCodecPriority(Codec.H263_1998, (short) 210);

        //config.setSignallingTransport(AbtoPhoneCfg.SignalingTransportType.TCP);
        config.setSipPort(0);

        org.abtollc.utils.Log.setLogLevel(5);
        org.abtollc.utils.Log.setUseFile(true);

        // Start initializing - !app has invoke this method only once!
        abtoPhone.initialize(true);
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
        try {

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
                    }

                    public void onRegistered(long accId) {

                        Log.e("SipTest", "registerUser register");

                        ReadyToCall = true;
                        sipRegister = true;
                        //Unsubscribe reg events
                        abtoPhone.setRegistrationStateListener(null);

                        tvRegisterState.setText("Register ok ...");
                        tvRegisterState.setTextColor(Color.GREEN);

                        tvCallStatus.setText("Ready to Call ...");
                        tvCallStatus.setTextColor(Color.GREEN);
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
        } catch (Exception e) {
            Log.e("SipTest", "registerUser exeption: " + e.getMessage());
        }

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
        edSipUserName.setText("sip989012084475");
        edSipPassToRegister.setText("86412129");
        edServerAddress.setText("sip.linphone.org");
        edSipUserToCall.setText("aminrahkan");
    }

    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;

    private boolean askPermissions()
    {
        List<String> permissionsNeeded = new ArrayList<String>();

        final List<String> permissionsList = new ArrayList<String>();
        if (!addPermission(permissionsList, Manifest.permission.RECORD_AUDIO))           permissionsNeeded.add("Record audio");
        if (!addPermission(permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE)) permissionsNeeded.add("Write logs to sd card");
        if (!addPermission(permissionsList, Manifest.permission.CAMERA))                 permissionsNeeded.add("Camera");
        if (!addPermission(permissionsList, Manifest.permission.USE_SIP))                permissionsNeeded.add("Use SIP protocol");


        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {
                // Need Rationale
                //String message = "You need to grant access to " + permissionsNeeded.get(0);
                //for (int i = 1; i < permissionsNeeded.size(); i++) message = message + ", " + permissionsNeeded.get(i);


                ActivityCompat.requestPermissions(this,
                        permissionsList.toArray(new String[permissionsList.size()]),
                        REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);

                return false;
            }

            ActivityCompat.requestPermissions(this,
                    permissionsList.toArray(new String[permissionsList.size()]),
                    REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            return false;
        }

        return true;
    }


    private boolean addPermission(List<String> permissionsList, String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            // Check for Rationale Option
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission))     return false;
        }


        return true;
    }

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        switch (requestCode)
        {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS:
            {
                Map<String, Integer> perms = new HashMap<String, Integer>();
                //Initial
                perms.put(Manifest.permission.RECORD_AUDIO,           PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.CAMERA,                 PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.USE_SIP,                PackageManager.PERMISSION_GRANTED);

                //Fill with results
                for (int i = 0; i < permissions.length; i++) perms.put(permissions[i], grantResults[i]);

                //Check for ACCESS_FINE_LOCATION
                if (perms.get(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.USE_SIP) == PackageManager.PERMISSION_GRANTED) {
                    // All Permissions Granted
                    initPhone();
                } else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "Some permissions were denied", Toast.LENGTH_SHORT).show();
//                    ProgressBar bar = (ProgressBar) findViewById(R.id.pbHeaderProgress);
//                    bar.setVisibility(View.GONE);
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}