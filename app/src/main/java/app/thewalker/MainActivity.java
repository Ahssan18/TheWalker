package app.thewalker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Subscription;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import app.thewalker.activities.CoinsHistoryActivity;
import app.thewalker.databinding.ActivityMainBinding;
import app.thewalker.fragments.ActivityFragment;
import app.thewalker.fragments.ChallengesFragment;
import app.thewalker.fragments.HomeFragment;
import app.thewalker.fragments.ProfileFragment;
import app.thewalker.fragments.RewardsFragment;
import app.thewalker.listeners.CoinsCallback;
import app.thewalker.listeners.CurrentLevelCallback;
import app.thewalker.listeners.DailyDetailsCallback;
import app.thewalker.listeners.LevelCallback;
import app.thewalker.listeners.RefferalCodeCallback;
import app.thewalker.services.ForegroundServiceLauncher;
import app.thewalker.services.RunningService;
import app.thewalker.utils.DataMainPoint;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    @SuppressLint("HardwareIds")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.navBar.setItemSelected(R.id.home,true);
        switchToHomeFrag();
        binding.navBar.setOnItemSelectedListener(new ChipNavigationBar.OnItemSelectedListener() {
            @Override
            public void onItemSelected(int i) {
                switch(i){
                    case R.id.home:
                        switchToHomeFrag();
                        break;
                    case R.id.challenges_menu_item:
                        switchFrag(new ChallengesFragment());
                        break;
                    case R.id.profile_menu_item:
                        switchFrag(new ProfileFragment());
                        break;
                    case R.id.activity_menu_item:
                        switchFrag(new ActivityFragment());
                        break;
                    case R.id.rewards_menu_item:
                        switchFrag(new RewardsFragment());
                        break;

                }

            }
        });
    }
    public void switchToHomeFrag() {
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction().replace(R.id.frag_container, new HomeFragment()).commit();
    }
    public void switchFrag(Fragment fragment) {
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction().replace(R.id.frag_container, fragment).commit();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }
}