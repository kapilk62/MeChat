package com.example.mechat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStreamReader;//Add these two imports
import java.io.BufferedReader;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rilixtech.widget.countrycodepicker.CountryCodePicker;

import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {
    public static final String TAG = "TAG";
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    EditText phoneNumber,codeEnter;
    Button nextBtn;
    ProgressBar progressBar;
    TextView state;
    CountryCodePicker codePicker;
    String verificationId;
    PhoneAuthProvider.ForceResendingToken token;
    Boolean verificationOnProgress = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        phoneNumber = findViewById(R.id.phone);
        codeEnter = findViewById(R.id.codeEnter);
        progressBar = findViewById(R.id.progressBar);
        nextBtn = findViewById(R.id.nextBtn);
        state = findViewById(R.id.state);
        codePicker = findViewById(R.id.ccp);

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (!verificationOnProgress){
                if (!phoneNumber.getText().toString().isEmpty() && phoneNumber.getText().toString().length() == 10)
                {
                    String phoneNum = "+"+codePicker.getSelectedCountryCode()+phoneNumber.getText().toString();
                    Log.d(TAG, "onClick: Phone No -> " +phoneNum);
                    progressBar.setVisibility(View.VISIBLE);
                    state.setText("Sending OTP");
                    state.setVisibility(View.VISIBLE);
                    requestOTP(phoneNum);


                }
                else {
                    phoneNumber.setError("Invalid Number");
                }
            }
            else {
                String userOTP = codeEnter.getText().toString();
                if(!userOTP.isEmpty() && userOTP.length() == 6){
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId,userOTP);
                    verifyAuth(credential);

                }
                else {
                    codeEnter.setError("Enter Valid OTP");
                }

            }
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        if (fAuth.getCurrentUser() != null){
            progressBar.setVisibility(View.VISIBLE);
            state.setText("Wait");
            state.setVisibility(View.VISIBLE);
            checkUserProfile();

        }
    }


    private void verifyAuth(PhoneAuthCredential credential) {
        fAuth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){
                checkUserProfile();
                }
                else {
                    Toast.makeText(RegisterActivity.this,"Authentication Failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void checkUserProfile() {

        DocumentReference docRef = fStore.collection("users").document(fAuth.getCurrentUser().getUid());
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()){
                    startActivity(new Intent(getApplicationContext(),MainActivity.class));
                    finish();
                }
                else {
                    startActivity(new Intent(getApplicationContext(),UserDetailActivity.class));
                    finish();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(RegisterActivity.this, "Profile Do Not Exists", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void requestOTP(String phoneNum) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneNum,30L, TimeUnit.SECONDS, this, new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(s, forceResendingToken);
                progressBar.setVisibility(View.GONE);
                state.setVisibility(View.GONE);
                codeEnter.setVisibility(View.VISIBLE);
                verificationId = s;
                token = forceResendingToken;
                nextBtn.setText("Verify");
                nextBtn.setEnabled(true);
                verificationOnProgress = true;

            }

            @Override
            public void onCodeAutoRetrievalTimeOut(@NonNull String s) {
                super.onCodeAutoRetrievalTimeOut(s);
                Toast.makeText(RegisterActivity.this,"OTP Expired, Re-send OTP", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                verifyAuth(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Toast.makeText(RegisterActivity.this,"Can't Create Account"+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}