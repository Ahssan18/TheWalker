package app.thewalker;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import app.thewalker.databinding.ActivityLoginBinding;
import app.thewalker.listeners.CoinsCallback;
import app.thewalker.listeners.LastLoginTimeStampCallback;
import app.thewalker.listeners.RefferalCodeValidCallback;
import app.thewalker.listeners.TotalInfoCallback;
import app.thewalker.utils.DataMainPoint;

import static app.thewalker.fragments.HomeFragment.getDeviceId;


public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 100;
    private FirebaseAuth mAuth;
    GoogleSignInClient mGoogleSignInClient;
    ActivityLoginBinding binding;
    FirebaseDatabase fbdb;
    DatabaseReference dbref;
    private static final long LOGIN_TS = System.currentTimeMillis();


    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityLoginBinding.inflate(getLayoutInflater());
        View view=binding.getRoot();
        setContentView(view);
        // Configure Google Sign In
        fbdb = FirebaseDatabase.getInstance();
        dbref=fbdb.getReference("users");

        if ((ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED)&&
                (ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.READ_PHONE_STATE)
                        != PackageManager.PERMISSION_GRANTED) ){
            // Permission is not granted
            ActivityCompat.requestPermissions(LoginActivity.this,
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION,Manifest.permission.READ_PHONE_STATE},
                    1);
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mAuth = FirebaseAuth.getInstance();


    mGoogleSignInClient=GoogleSignIn.getClient(this,gso);

    binding.gSignin.setOnClickListener(l->{
        signIn();
    });
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase

                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());

            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                // ...
            }
        }

    }
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");

                            getLastLoginTimeStamp(dbref.child(getString(R.string.didkp)).child(getDeviceId(LoginActivity.this)), new LastLoginTimeStampCallback() {
                                @Override
                                public void lastLogin(long ts,String email) {
                                    if(binding.loginRefferalEt.getText().toString().isEmpty()){
                                        Log.e(TAG, "lastLoginoopop "+ts);
                                        ;
                                        if (email.equals("0")) {
                                            addUniqueUserIdentifier(dbref);
                                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                            startActivity(intent);

                                            //Not a new user
                                        }else {
                                            boolean valid1 = checkLastLogin(ts, 432000000, email, mAuth.getCurrentUser().getEmail());
                                            if (valid1) {
                                                addUniqueUserIdentifier(dbref);
                                                Log.e(TAG, "lastLogin: "+"true");
                                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                                startActivity(intent);
                                                //Old device
                                            } else {
                                                Log.e(TAG, "lastLogin: "+"false");
                                                binding.previousEmailId.setText("Your previous email id was: "+email);
                                                Toast.makeText(LoginActivity.this, "Wait 5 days before you can login again from different id", Toast.LENGTH_SHORT).show();
                                                mGoogleSignInClient.signOut();
                                                mAuth.signOut();
                                            }
                                            Log.e(TAG, "lastLogin: "+"hello");


                                        }

                                    }else {
                                        checkRefferalCodeValidity(dbref.child("refferal_codes"), binding.loginRefferalEt.getText().toString(), new RefferalCodeValidCallback() {
                                            @Override
                                            public void refferalCodeValid(boolean valid) {
                                                if (valid) {
                                                    DataMainPoint.getTotalCoinsReward(dbref.child(mAuth.getCurrentUser().getUid()).child("TotalInfo")
                                                            , new CoinsCallback() {
                                                                @Override
                                                                public void num_coins(int coins) {
                                                                    if(coins==-1){
                                                                        dbref.child(mAuth.getCurrentUser().getUid()).child("TotalInfo").child("total_coins_reward").setValue(10);

                                                                    }else {
                                                                        final int coins_total=coins +10;
                                                                        dbref.child(mAuth.getCurrentUser().getUid()).child("TotalInfo").child("total_coins_reward").setValue(coins_total);

                                                                    }
                                                                }
                                                            });
//                                                    DataMainPoint.getTotalInfo(dbref.child(mAuth.getCurrentUser().getUid()).child("TotalInfo")
//                                                            , (level, total_coins, total_steps) -> {
//                                                                final int totalcoin=total_coins+10;
//                                                                dbref.child(mAuth.getCurrentUser().getUid()).child("TotalInfo").child("total_coins_reward").setValue(totalcoin);
//
//                                                            });
                                                    Log.e(TAG, "lastLoginoopop "+ts);
                                                    ;
                                                    if (email.equals("0")) {
                                                        addUniqueUserIdentifier(dbref);
                                                        dbref.child(mAuth.getCurrentUser().getUid()).child("userInfo").child("RefferalCodeUsed").setValue("used");

                                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                                        startActivity(intent);

                                                        //Not a new user
                                                    }else {
                                                        boolean valid1 = checkLastLogin(ts, 432000000, email, mAuth.getCurrentUser().getEmail());
                                                        if (valid1) {
                                                            addUniqueUserIdentifier(dbref);
                                                            Log.e(TAG, "lastLogin: "+"true");
                                                            dbref.child(mAuth.getCurrentUser().getUid()).child("userInfo").child("RefferalCodeUsed").setValue("used");
                                                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                                            startActivity(intent);
                                                            //Old device
                                                        } else {
                                                            Log.e(TAG, "lastLogin: "+"false");
                                                            binding.previousEmailId.setText("Your previous email id was: "+email);
                                                            Toast.makeText(LoginActivity.this, "Wait 5 days before you can login again from different id", Toast.LENGTH_SHORT).show();
                                                            mGoogleSignInClient.signOut();
                                                            mAuth.signOut();
                                                        }
                                                        Log.e(TAG, "lastLogin: "+"hello");


                                                    }
                                                } else {
                                                    Toast.makeText(LoginActivity.this,"Refferal code is either not valid or was used",Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        }, mAuth.getCurrentUser().getUid(),dbref);
                                    }
                                    //New device

                                }
                            });

                            FirebaseUser user = mAuth.getCurrentUser();


                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());


                        }

                        // ...
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mAuth.getCurrentUser()!=null){
            Intent intent = new Intent(LoginActivity.this,MainActivity.class);
            startActivity(intent);
        }
    }
    private void addUniqueUserIdentifier(DatabaseReference Usersref){
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(LoginActivity.this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    1);
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }else {
            if (telephonyManager != null) {
                Usersref.child(mAuth.getCurrentUser().getUid()).child("userInfo").child("deviceId").setValue(getDeviceId(LoginActivity.this));
                Usersref.child(getString(R.string.didkp)).child(getDeviceId(LoginActivity.this)).child("email").setValue(mAuth.getCurrentUser().getEmail());
                Usersref.child(getString(R.string.didkp)).child(getDeviceId(LoginActivity.this)).child("did").setValue(getDeviceId(LoginActivity.this));
                Usersref.child(getString(R.string.didkp)).child(getDeviceId(LoginActivity.this)).child("last_login").setValue(LOGIN_TS);

            }
        }
    }
    private boolean checkLastLogin(long last_login_timestamp,long timestamp_of_login_reset,String previous_login_email,String current_login){
        long timestamp_now=System.currentTimeMillis();
        long new_ts=last_login_timestamp+timestamp_of_login_reset;
        if(previous_login_email.equals(current_login)) {
          return true;
        }else {
            if (timestamp_now >= new_ts) {
                return true;
            } else {
                return false;

            }
        }

    }
    private void getLastLoginTimeStamp(DatabaseReference dbref, LastLoginTimeStampCallback callback){
        dbref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.child("last_login").getValue()!=null){
                    callback.lastLogin((long)snapshot.child("last_login").getValue(),snapshot.child("email").getValue().toString());
                    Log.e(TAG, "lastlogin ts"+(long)snapshot.child("last_login").getValue());

                }else {
                    callback.lastLogin(0,"0");

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("db_error: ",error.getMessage());
            }
        });
    }
    private void checkRefferalCodeValidity(DatabaseReference dbref, String code, RefferalCodeValidCallback callback,String user_id,DatabaseReference dbref1){

        dbref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.child(code).exists()){
                    dbref1.child(user_id).child("userInfo").child("RefferalCodeUsed").addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.getValue()!=null){
                                callback.refferalCodeValid(false);

                            }else {
                                callback.refferalCodeValid(true);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });


                }else {
                    callback.refferalCodeValid(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("db_error: ",error.getMessage());
            }
        });
    }



}