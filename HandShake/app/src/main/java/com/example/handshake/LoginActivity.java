package com.example.handshake;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import in.aabhasjindal.otptextview.OtpTextView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hbb20.CountryCodePicker;

import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {
    EditText mobNumberEditText;
    CountryCodePicker ccp;
    Button generateOtpButton,validateOtpButton;
    TextView resendNotificationTextView;
    OtpTextView otpTextView;
    public int counter = 30;

    Vibrator v;

    String phoneNumber;
    String ls = "";
    String otpid;
    FirebaseAuth mAuth;

    SharedPreferences sharedPreferences = null;
    Boolean nightModeFlag;

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            startActivity(new Intent(LoginActivity.this,MainActivity.class));
            finish();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        getSupportActionBar().hide();

        mAuth = FirebaseAuth.getInstance();

        v = (Vibrator)getSystemService(VIBRATOR_SERVICE);

        sharedPreferences = getSharedPreferences("night",0);
        nightModeFlag = sharedPreferences.getBoolean("night_mode",true);
        if (nightModeFlag) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }

        mobNumberEditText = (EditText)findViewById(R.id.mobNumberEditText);
        otpTextView = (OtpTextView)findViewById(R.id.otp_view);
        ccp = (CountryCodePicker)findViewById(R.id.ccp);
        ccp.registerCarrierNumberEditText(mobNumberEditText);
        generateOtpButton = (Button)findViewById(R.id.generateOtpButton);
        validateOtpButton = (Button)findViewById(R.id.validateOtpButton);
        resendNotificationTextView = (TextView) findViewById(R.id.resendNotificationTextView);


        otpTextView.setVisibility(View.GONE);
        validateOtpButton.setVisibility(View.GONE);

        generateOtpButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {

                generateOtpButton.setEnabled(false);
                otpTextView.setVisibility(View.VISIBLE);
                validateOtpButton.setVisibility(View.VISIBLE);
                otpTextView.requestFocusOTP();

                phoneNumber = ccp.getFullNumberWithPlus().replace(" ","").toString();

                initiateOtp();

                new CountDownTimer(30000,1000){

                    @Override
                    public void onTick(long l) {
                        counter--;
                        resendNotificationTextView.setText("resend in "+counter+" sec");
                    }

                    @Override
                    public void onFinish() {
                        resendNotificationTextView.setText("");
                        generateOtpButton.setEnabled(true);
                        counter = 30;
                            
                    }
                }.start();
            }
        });

        validateOtpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               if (otpTextView.getOTP().toString().isEmpty()) {
//                   Toast.makeText(LoginActivity.this, "Please fill the OTP section", Toast.LENGTH_SHORT).show();
                   otpTextView.showError();
                   v.vibrate(500);
               } else if (otpTextView.getOTP().toString().length() != 6) {
//                   Toast.makeText(LoginActivity.this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                   otpTextView.showError();
                   v.vibrate(500);
               } else {
                   PhoneAuthCredential credential= PhoneAuthProvider.getCredential(otpid,otpTextView.getOTP().toString());
                   signInWithPhoneAuthCredential(credential);
               }

            }
        });


    }

    private void initiateOtp() {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        @Override
                        public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                            otpid = s;
                        }

                        @Override
                        public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                            signInWithPhoneAuthCredential(phoneAuthCredential);
                        }

                        @Override
                        public void onVerificationFailed(@NonNull FirebaseException e) {
//                            Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            otpTextView.showError();
                            v.vibrate(500);
                        }
                    }).build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Intent intent = new Intent(LoginActivity.this,UserDetailActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
//                            Toast.makeText(LoginActivity.this, "Sign In code error", Toast.LENGTH_SHORT).show();
                            otpTextView.showError();
                            v.vibrate(500);
                        }
                    }
                });
    }
}