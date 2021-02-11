package app.thewalker.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

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

import app.thewalker.BuildConfig;
import app.thewalker.MainActivity;
import app.thewalker.R;
import app.thewalker.activities.CoinsHistoryActivity;
import app.thewalker.databinding.ActivityMainBinding;
import app.thewalker.databinding.FragmentHomeBinding;
import app.thewalker.listeners.CoinsCallback;
import app.thewalker.listeners.CurrentLevelCallback;
import app.thewalker.listeners.DailyDetailsCallback;
import app.thewalker.listeners.LevelCallback;
import app.thewalker.listeners.RefferalCodeCallback;
import app.thewalker.services.ForegroundServiceLauncher;
import app.thewalker.utils.DataMainPoint;

public class HomeFragment extends Fragment  implements  GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks{

    private static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 111;
    private static final String TAG = "main";
    FitnessOptions fitnessOptions;
    private FirebaseAuth mAuth;
    private FirebaseDatabase fbDB;
    DatabaseReference Usersref;
    private static final String USERS_REF = "users";

    GoogleSignInClient mGoogleSignInClient;


    @SuppressLint("HardwareIds")
    @RequiresApi(api = Build.VERSION_CODES.O)

   FragmentHomeBinding binding;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for getActivity() fragment
        fbDB = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        Usersref = fbDB.getReference(USERS_REF);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        if(mAuth.getCurrentUser()==null){
            Toast.makeText(getActivity(),"Null User token expired",Toast.LENGTH_SHORT).show();
        }





        TelephonyManager telephonyManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    1);
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

        }else {
            if (telephonyManager != null) {
                Usersref.child(mAuth.getCurrentUser().getUid()).child("userInfo").child("deviceId").setValue(getDeviceId(getActivity()));
                Usersref.child(getString(R.string.didkp)).child(getDeviceId(getActivity())).child("email").setValue(mAuth.getCurrentUser().getEmail());
                Usersref.child(getString(R.string.didkp)).child(getDeviceId(getActivity())).child("did").setValue(getDeviceId(getActivity()));
                addRefferalCode(Usersref,getDeviceId(getActivity()));
                binding.inviteAndEarnBtn.setOnClickListener(l->{
                    Toast.makeText(getActivity(),"share",Toast.LENGTH_SHORT).show();

                    getRefferalCode(Usersref, getDeviceId(getActivity()), new RefferalCodeCallback() {

                        @Override
                        public void refferalCode(String refferal_code) {
                            if(refferal_code==null||refferal_code.isEmpty()){
                                Toast.makeText(getActivity(),"Error no refferal code",Toast.LENGTH_SHORT).show();
                            }else {
                                inviteAndEarn(refferal_code);
                            }
                        }
                    });
                });

            }
        }


        binding.belowLevelRectLay.setOnClickListener(l->{
            Intent intent=new Intent(getActivity(), CoinsHistoryActivity.class);
            startActivity(intent);
        });

        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String str = sdf.format(new Date());
        binding.updatedOnTxt.setText("Updated on: "+str);
        if(mAuth.getCurrentUser()!=null) {
            Usersref.child(mAuth.getCurrentUser().getUid()).child("userInfo").child("name").setValue(mAuth.getCurrentUser().getDisplayName());
            Usersref.child(mAuth.getCurrentUser().getUid()).child("userInfo").child("email").setValue(mAuth.getCurrentUser().getEmail());

        }








        if(mAuth.getCurrentUser()!=null) {

            updateLevelAndIcons();
            setOverallInfo(Usersref,mAuth.getCurrentUser().getUid());

            fitnessOptions=FitnessOptions.builder().
                    addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                    .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                    .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED,FitnessOptions.ACCESS_WRITE)
                    .addDataType(DataType.AGGREGATE_DISTANCE_DELTA,FitnessOptions.ACCESS_WRITE)
                    .addDataType(DataType.AGGREGATE_ACTIVITY_SUMMARY,FitnessOptions.ACCESS_READ)
                    .addDataType(DataType.AGGREGATE_MOVE_MINUTES,FitnessOptions.ACCESS_READ)
                    .build();


            GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(getActivity(),fitnessOptions);
            if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
                GoogleSignIn.requestPermissions(
                        getActivity(), // your activity
                        GOOGLE_FIT_PERMISSIONS_REQUEST_CODE, // e.g. 1
                        account,
                        fitnessOptions);
            } else {
                Fitness.getRecordingClient(getActivity(), GoogleSignIn.getAccountForExtension(getActivity(), fitnessOptions)).subscribe(DataType.TYPE_STEP_COUNT_DELTA)
                        .addOnSuccessListener(aVoid -> {
                            Log.e("hello", "subscribed to steps");
                            ForegroundServiceLauncher.getInstance().startService(getActivity());


                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("exception", "" + e);
                    }
                });



                accessGoogleFit();
                Fitness.getRecordingClient(getActivity(), GoogleSignIn.getAccountForExtension(getActivity(), fitnessOptions))
                        .listSubscriptions()
                        .addOnSuccessListener(new OnSuccessListener<List<Subscription>>() {
                            @Override
                            public void onSuccess(List<Subscription> subscriptions) {
                                for(Subscription sc:subscriptions) {
                                    Log.i(TAG, "Active subscription for data type:"+sc.getDataType().getName());
                                }
                                getHistoricalFitData(fitnessOptions);

                                getSensorData(fitnessOptions,getActivity());
                            }
                        });
                Fitness.getHistoryClient(getActivity(),GoogleSignIn.getAccountForExtension(getActivity(),fitnessOptions))
                        .readDailyTotal(DataType.AGGREGATE_DISTANCE_DELTA)


                        .addOnSuccessListener(new OnSuccessListener<DataSet>() {
                            @Override
                            public void onSuccess(DataSet dataSet) {
                                for (DataPoint dp : dataSet.getDataPoints()) {
                                    for (Field field : dp.getDataType().getFields()) {
                                        binding.kmTxt.setText(String.valueOf(Math.round(Double.parseDouble(dp.getValue(field).toString())/1000)));
                                        Log.e(TAG + " DailyData distance", "\tField:" + field.getName().toString() + " Value:" + dp.getValue(field));
                                    }
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Distance failure",""+e.toString());
                    }
                });
                Fitness.getHistoryClient(getActivity(),GoogleSignIn.getAccountForExtension(getActivity(),fitnessOptions))
                        .readDailyTotal(DataType.TYPE_CALORIES_EXPENDED)


                        .addOnSuccessListener(new OnSuccessListener<DataSet>() {
                            @Override
                            public void onSuccess(DataSet dataSet) {
                                for (DataPoint dp : dataSet.getDataPoints()) {
                                    for (Field field : dp.getDataType().getFields()) {
                                        binding.calTxt.setText(""+Math.round(Float.parseFloat(dp.getValue(field).toString())));
                                        Toast.makeText(getActivity(),"Cals "+dp.getValue(field).toString(),Toast.LENGTH_SHORT).show();
                                        Log.e(TAG + " DailyData cals", "\tField:" + field.getName().toString() + " Value:" + dp.getValue(field));
                                    }
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG+"cals", "onFailure: "+e.toString() );
                    }
                });
            }
        }
        updateCircleUi();
        binding.pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                binding.pullToRefresh.setRefreshing(true);
                updateLevelAndIcons();
                updateDB(Usersref,mAuth);
                updateCircleUi();
                setOverallInfo(Usersref,mAuth.getCurrentUser().getUid());
                DataMainPoint.setTotalInfo(Usersref,mAuth.getCurrentUser().getUid());
            }
        });
        return binding.getRoot();




    }
    private void updateLevelAndIcons() {
        Usersref.child(mAuth.getCurrentUser().getUid()).child("TotalInfo").child("current_level").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() != null) {
                    getActualLevel(mAuth.getCurrentUser().getUid(), new LevelCallback() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void level(ArrayList<Integer> flags) {
                            boolean increase = false;
                            boolean constant = false;
                            setLevelIcons(flags);
                            Log.e("flags final", "" + flags);

                            int a = 0;
                            if ((Collections.frequency(flags, 0) == 3) && (int) (long) snapshot.getValue() != 3) {
                                a = 1;

                            } else if ((Collections.frequency(flags, 1) == 3) && (int) (long) snapshot.getValue() != 1) {
                                a = 2;


                            }
                            if (a == 1) {


                                Usersref.child(mAuth.getCurrentUser()
                                        .getUid()).child("TotalInfo")
                                        .child("current_level").setValue((int) (long) snapshot.getValue() + 1);
                            } else if (a == 2) {
                                Log.e("is it increase", " f");
                                Usersref.child(mAuth.getCurrentUser()
                                        .getUid()).child("TotalInfo")
                                        .child("current_level").setValue((int) (long) snapshot.getValue() - 1);
                            }

                        }
                    }, Integer.parseInt(snapshot.getValue().toString()));
                }else {
                    Usersref.child(mAuth.getCurrentUser()
                            .getUid()).child("TotalInfo")
                            .child("current_level").setValue(1);
                }
            }

            @Override
            public void onCancelled (@NonNull DatabaseError error){


            }
        });
    }

    private void updateCircleUi() {
        DataMainPoint.getDailyData(Usersref.child(mAuth.getCurrentUser().getUid()), new DailyDetailsCallback() {

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void getDailyDetails(int coins, int steps, String updated_on) {
                binding.circleStepsTxt.setText(""+steps);
                binding.updatedOnTxt.setText(updated_on);
                getActualLevel(Usersref.child(mAuth.getCurrentUser().getUid()), new CurrentLevelCallback() {
                    @Override
                    public void currentLevel(int level) {
                        binding.pullToRefresh.setRefreshing(false);

                        if(level==1){
                            binding.progress1.setProgress((((float)coins)/5)*100);
                            Log.e("progress bar", "onSuccess: "+coins);
                            binding.progress2.setProgress(((float)(steps)/5000)*100);

                            binding.goalTxt.setText("GOAL - 5000");

                        }else if(level==2){
                            binding.progress1.setProgress((((float)coins)/10)*100);

                            binding.progress2.setProgress(((float)(steps)/10000)*100);


                            binding.goalTxt.setText("GOAL - 10,000");
                        }else if(level==3){
                            binding.progress1.setProgress((((float)coins)/15)*100);
                            binding.progress2.setProgress(((float)(steps)/15000)*100);

                            binding.goalTxt.setText("GOAL - 15,000");
                        }else {
                            binding.progress1.setProgress(100);
                            binding.progress2.setProgress(100);
                        }
                        binding.circleCoinTxt.setText(""+coins);
                    }
                });




            }
        });
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void accessGoogleFit() {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusYears(1);
        long endSeconds = end.atZone(ZoneId.systemDefault()).toEpochSecond();
        long startSeconds = start.atZone(ZoneId.systemDefault()).toEpochSecond();

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.AGGREGATE_STEP_COUNT_DELTA)
                .setTimeRange(startSeconds, endSeconds, TimeUnit.SECONDS)
                .bucketByTime(1, TimeUnit.DAYS)
                .build();
        GoogleSignInAccount account = GoogleSignIn.getAccountForExtension(getActivity(), fitnessOptions);
        Fitness.getHistoryClient(getActivity(), account)
                .readData(readRequest)
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse dataReadResponse) {
                        Log.e(TAG, "onSuccess: "+"gfit");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure: "+e.toString());
                    }
                });
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void getHistoricalFitData(FitnessOptions fitnessOptions){
        ZonedDateTime endTime = LocalDateTime.now().atZone(ZoneId.systemDefault());
        ZonedDateTime startTime = endTime.minusWeeks(1);
        Log.i(TAG,"Range STart: "+startTime);
        Log.i(TAG, "Range End: "+endTime);
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.AGGREGATE_STEP_COUNT_DELTA)
                .bucketByTime(1,TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(),endTime.toEpochSecond(),TimeUnit.SECONDS)
                .build();
        Fitness.getHistoryClient(getActivity(),GoogleSignIn.getAccountForExtension(getActivity(),fitnessOptions))
                .readData(readRequest)
                .addOnSuccessListener(r->{
                    for (Bucket bucket:r.getBuckets()){
                        for (DataSet dataSet:bucket.getDataSets()){
                            HistoricaldumpDataSet(dataSet);
                        }
                    }
                });

    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    void HistoricaldumpDataSet(DataSet dataSet) {
        Log.e(TAG, "Data returned for Data type: ${dataSet.dataType.name}");
        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.e(TAG,"Data point:");
            Log.e(TAG,"\tType: "+dp.getDataType().getName());
            Log.e(TAG,"\tStart:"+ Instant.ofEpochSecond(dp.getStartTime(TimeUnit.SECONDS)).atZone(ZoneId.systemDefault()).toLocalDateTime());
            Log.e(TAG,"\tEnd: "+Instant.ofEpochSecond(dp.getEndTime(TimeUnit.SECONDS)).atZone(ZoneId.systemDefault()).toLocalDateTime());
            for (Field field : dp.getDataType().getFields()) {
                Log.e(TAG,"\tField:"+field.getName().toString()+" Value:"+dp.getValue(field));
            }
        }
    }

    private int convertStepsToCoins(int multiplier,int steps,int level){

        int final_coins=0;

        int coins = Math.round((float) (steps * 0.001)) * multiplier;
        if(level==1){
            final_coins=Math.min(5,coins);
        }else if(level==2){
            final_coins= Math.min(10,coins);
        }else if (level==3){
            final_coins=Math.min(15,coins);
        }else {
            final_coins=15;
        }
        return final_coins;
    }
    private int getLevel(int multiplier,int steps){
        int level1=0;

        if(steps<=5000 && steps>=0){
            level1=1;
        }else if(steps>5000 && steps<=10000){
            level1=2;
        }else {
            level1=3;
        }
        return level1;
    }
    public static String EncodeString(String string) {
        return string.replace(".", ",");
    }


    private void getSensorData(FitnessOptions fitnessOptions, Context context){
        OnDataPointListener listener=new OnDataPointListener() {
            @Override
            public void onDataPoint(DataPoint dataPoint) {
                for (Field field:dataPoint.getDataType().getFields()) {
                    Value value = dataPoint.getValue(field);
                    Log.e(TAG, "Detected DataPoint field: hello "+field.getName());
                    Log.e(TAG, "Detected DataPoint value: hello1 "+value.toString());
                }
            }
        };
        Fitness.getSensorsClient(getActivity(),GoogleSignIn.getAccountForExtension(getActivity(),fitnessOptions))
                .findDataSources(new DataSourcesRequest.Builder()
                        .setDataTypes(DataType.TYPE_STEP_COUNT_DELTA)
                        .setDataSourceTypes(DataSource.TYPE_RAW).build())
                .addOnSuccessListener(new OnSuccessListener<List<DataSource>>() {
                    @Override
                    public void onSuccess(List<DataSource> dataSources) {
                        for (DataSource dataSource:dataSources){
                            Log.e(TAG, "Data source found:"+dataSource.getStreamIdentifier());
                            Log.e(TAG, "Data Source type: "+dataSource.getDataType().getName());
                            if(dataSource.getDataType()==DataType.TYPE_STEP_COUNT_DELTA){
                                Fitness.getSensorsClient(context,GoogleSignIn.getAccountForExtension(context,fitnessOptions))
                                        .add(new SensorRequest.Builder()
                                                .setDataSource(dataSource)
                                                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                                                .setSamplingRate(5,TimeUnit.SECONDS)
                                                .build(),listener).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()){
                                            Log.e("success","listener registered");

                                        }else{
                                            Log.e("Unsuccess","listener registered"+task.getException());
                                        }
                                    }
                                });
                            }
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("exception sensor",e.getMessage());
            }
        });


    }
    private void getActualLevel(String uid, LevelCallback callback,int current_level){

        int level=0;
        Query lastQuery = Usersref.child(uid).child("WorkoutHistory").orderByChild("updatedOnTs").limitToLast(3);
        lastQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<Integer> flags=new ArrayList<>();
                for(DataSnapshot ds:snapshot.getChildren()) {
                    if(ds.child("level").getValue()!=null) {
                        Log.e("updatedon: ",ds.child("updatedOnTs").getValue().toString());

                        if (((int) (long) ds.child("level").getValue()) > current_level) {
                            flags.add(0);
                        } else if (((int) (long) ds.child("level").getValue()) < current_level) {
                            flags.add(1);
                        } else {
                            flags.add(-1);
                        }
                    }else {
                        flags.add(-1);
                    }
                }
                callback.level(flags);
//                for(int i=0;i>=flags.size()-1;i++){
//                    Log.e("flags are",""+flags.get(i));
//                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Error in reading db",error.getMessage());

            }
        });
//        Usersref.child(uid).child("WorkoutHistory").addValueEventListener(new ValueEventListener() {
//            @RequiresApi(api = Build.VERSION_CODES.N)
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                int count=(int)snapshot.getChildrenCount();
//                ArrayList<Integer> levels = new ArrayList<>();
//                ArrayList<Integer> three_days_level=new ArrayList<>();
//                for(DataSnapshot ds:snapshot.getChildren()){
//                    levels.add(Integer.parseInt(ds.child("level").getValue().toString()));
//
//                }
//                int flag_increase=0;
//                ArrayList<Integer> flags_increase=new ArrayList<>();
//                for (int i=levels.size()-1;i>levels.size()-4;i--){
//                    Log.e("levels",""+levels.get(i));
//                    three_days_level.add(levels.get(i));
//                    if(levels.get(i)>current_level){
//                        flag_increase=1;
//                        flags_increase.add(flag_increase);
//
//                    }else if(levels.get(i)<current_level){
//                        flag_increase=0;
//                        flags_increase.add(flag_increase);
//                    }else {
//                        callback.level(current_level);
//                    }
////                    for(int j=levels.size()-2;j>=levels.size()-3;j--){
////                        if(levels.get(i)>1) {
////                            if (current_level>levels.get(i)&&!(levels.get(i).equals(levels.get(j)))) {
////                                callback.level(current_level-1);
////                            }else if(current_level<levels.get(i)&&!(levels.get(i).equals(levels.get(j)))){
////                                callback.level(current_level-1);
////                            }
////                        }else if(levels.get(i)==1){
////
////                        }
////                    }
//
//                }
//                ;
//                for(Integer f:flags_increase){
//                    Log.e("flags",""+f);
//                }
//                boolean allEquallevel = three_days_level.isEmpty() || three_days_level.stream().allMatch(three_days_level.get(0)::equals);
//                boolean allEqualFlags = flags_increase.isEmpty() || flags_increase.stream().allMatch(flags_increase.get(0)::equals);
//                if(current_level==3){
//                    if(allEqualFlags){
//                        callback.level(3);
//                    }else {
//                        callback.level(current_level-1);
//                    }
//                }else if(current_level==1){
//                    if(allEqualFlags){
//                        callback.level(current_level+1);
//                    }else {
//                        callback.level(1);
//                    }
//                }else {
//                    if(allEqualFlags){
//                        callback.level(current_level+1);
//                    }else {
//                        callback.level(current_level-1);
//                    }
//                }
//
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//
//            }
//        });
    }
    @SuppressLint("MissingPermission")
    public static String getDeviceId(Context context) {

        String deviceId;

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            deviceId = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
        } else {
            final TelephonyManager mTelephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (mTelephony.getDeviceId() != null) {
                deviceId = mTelephony.getDeviceId();
            } else {
                deviceId = Settings.Secure.getString(
                        context.getContentResolver(),
                        Settings.Secure.ANDROID_ID);
            }
        }

        return deviceId;
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setOverallInfo(DatabaseReference ref, String uid){
        ref.child(uid).child("TotalInfo").addListenerForSingleValueEvent(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                binding.pullToRefresh.setRefreshing(false);
                if(snapshot.child("total_coins").getValue()!=null) {
                    final int total_coins=(int)(long)snapshot.child("total_coins").getValue();
                    if(snapshot.child("total_coins_reward").getValue()!=null) {
                        final int total_coins_reward = (int) (long) snapshot.child("total_coins_reward").getValue();

                        final int total_coins_final = total_coins + total_coins_reward;
                        binding.topCoinTxt.setText(""+total_coins_final);
                    }else {
                        binding.topCoinTxt.setText(""+total_coins);
                    }

                }else {
                    binding.topCoinTxt.setText("0");
                }
                if(snapshot.child("total_steps").getValue()!=null) {
                    binding.topStepsInfoTxt.setText(snapshot.child("total_steps").getValue().toString());
                }else {
                    binding.topStepsInfoTxt.setText("0");
                }if(snapshot.child("current_level").getValue()!=null) {
                    binding.levelTxt.setText("LEVEL " + snapshot.child("current_level").getValue().toString());
                }else {
                    binding.levelTxt.setText("--");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Error in reading db",error.getMessage());

            }
        });
        binding.topUserNameTxt.setText("Hi, "+mAuth.getCurrentUser().getDisplayName());
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setLevelIcons(ArrayList<Integer> flags){

        Log.e("flags",""+flags.toString());
        try {

            if (flags.get(0) != null) {
                switch (flags.get(0)) {
                    case 0:
                        binding.firstInfoLevelIc.setImageResource(R.drawable.ic_level_crossed);
                        break;
                    case 1:
                        binding.firstInfoLevelIc.setImageResource(R.drawable.ic_level_warning);
                        break;
                    case -1:
                        binding.firstInfoLevelIc.setImageResource(R.drawable.ic_level_safe_point);
                        break;

                }
            } else {
                binding.firstInfoLevelIc.setImageResource(R.drawable.level_not_decided_yet);
            }
            if (flags.get(1) != null) {
                switch (flags.get(1)) {
                    case 0:
                        binding.secondInfoLevelIc.setImageResource(R.drawable.ic_level_crossed);
                        break;
                    case 1:
                        binding.secondInfoLevelIc.setImageResource(R.drawable.ic_level_warning);
                        break;
                    case -1:
                        binding.secondInfoLevelIc.setImageResource(R.drawable.ic_level_safe_point);

                        break;

                }
            } else {
                binding.secondInfoLevelIc.setImageResource(R.drawable.level_not_decided_yet);
            }
            if (flags.get(2) != null) {
                switch (flags.get(2)) {
                    case 0:
                        binding.thirdInfoLevelIc.setImageResource(R.drawable.ic_level_crossed);
                        break;
                    case 1:
                        binding.thirdInfoLevelIc.setImageResource(R.drawable.ic_level_warning);
                        break;
                    case -1:
                        binding.thirdInfoLevelIc.setImageResource(R.drawable.ic_level_safe_point);


                        break;

                }
            } else {
                binding.thirdInfoLevelIc.setImageResource(R.drawable.level_not_decided_yet);
            }
        }catch (IndexOutOfBoundsException e){
            binding.firstInfoLevelIc.setImageResource(R.drawable.level_not_decided_yet);
            binding.secondInfoLevelIc.setImageResource(R.drawable.level_not_decided_yet);
            binding.thirdInfoLevelIc.setImageResource(R.drawable.level_not_decided_yet);
        }
    }
    private void getActualLevel(DatabaseReference dbref, CurrentLevelCallback callback){
        dbref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.child("TotalInfo").getValue()!=null) {
                    callback.currentLevel((int) (long) snapshot.child("TotalInfo").child("current_level").getValue());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Error in reading db",error.getMessage());

            }
        });
    }
    public void updateDB(DatabaseReference dbref,FirebaseAuth fbAuth){


        fitnessOptions=FitnessOptions.builder().
                addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED,FitnessOptions.ACCESS_WRITE)
                .addDataType(DataType.AGGREGATE_DISTANCE_DELTA,FitnessOptions.ACCESS_WRITE)
                .addDataType(DataType.AGGREGATE_ACTIVITY_SUMMARY,FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_MOVE_MINUTES,FitnessOptions.ACCESS_READ)
                .build();

        Fitness.getHistoryClient(getActivity(), GoogleSignIn.getAccountForExtension(getActivity(),fitnessOptions))
                .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA).addOnSuccessListener(new OnSuccessListener<DataSet>() {
            @Override
            public void onSuccess(DataSet dataSet) {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                String str = sdf.format(new Date());
                for (DataPoint dp : dataSet.getDataPoints()) {
                    for (Field field : dp.getDataType().getFields()) {
                        int steps=Integer.parseInt(dp.getValue(field).toString());

                        dbref.child(fbAuth.getCurrentUser().getUid()).child("WorkoutInfo").child("DailySteps").setValue(Integer.parseInt(dp.getValue(field).toString()));
                        dbref.child(fbAuth.getCurrentUser().getUid()).child("WorkoutInfo").child("UpdatedOn").setValue(str);
//                                int level=getLevel(1,steps);
                        getActualDbLevel(dbref.child(fbAuth.getCurrentUser().getUid()), new CurrentLevelCallback() {
                            @Override
                            public void currentLevel(int level) {
                                int coins=convertStepsToCoins(1,steps,level);
                                dbref.child(fbAuth.getCurrentUser().getUid()).child("WorkoutInfo").child("DailyCoin").setValue(coins);
                            }
                        });

                        addValueToDb(dbref.child(fbAuth.getCurrentUser().getUid()),Integer.parseInt(dp.getValue(field).toString()));
                        Log.e("hello service","success");
                    }}
            }
        });
    }
    private void getActualDbLevel(DatabaseReference dbref, CurrentLevelCallback callback){
        dbref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                callback.currentLevel((int)(long)snapshot.child("TotalInfo").child("current_level").getValue());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Error in reading db",error.getMessage());
                Toast.makeText(getActivity(),"Error in reading db "+error.getMessage(),Toast.LENGTH_SHORT).show();

            }
        });
    }
    private void addValueToDb(DatabaseReference Usersref,int steps){
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        String str = sdf.format(System.currentTimeMillis());
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf1 = new SimpleDateFormat("HH:mm");
        String str1 = sdf1.format(new Date());

        Usersref.child("WorkoutHistory").child(str).child("steps").setValue(steps);
        Usersref.child("WorkoutHistory").child(str).child("updatedOn").setValue(str1);

        int level=getLevel(1,steps);
        getActualDbLevel(Usersref, level1-> {
            int coins=convertStepsToCoins(1,steps,level1);
            Usersref.child("WorkoutHistory").child(str).child("coins").setValue(coins);
        });
//        int coins=convertStepsToCoins(1,steps,level);
//        Usersref.child("WorkoutHistory").child(str).child("coins").setValue(coins);
        Usersref.child("WorkoutHistory").child(str).child("level").setValue(level);
    }
    protected String getRandomRefferalCode(int size) {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < size) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;

    }
    private void addRefferalCode(DatabaseReference dbref,String device_id){
        dbref.child(getString(R.string.didkp)).child(device_id).child("refferal_code").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.getValue()==null){
                    String random_code=getRandomRefferalCode(5);
                    dbref.child(getString(R.string.didkp)).child(device_id).child("refferal_code").setValue(random_code);
                    dbref.child("refferal_codes").child(random_code).child("refferal_code").setValue(random_code);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Error in reading db",error.getMessage());
                Toast.makeText(getActivity(),"Error in reading db "+error.getMessage(),Toast.LENGTH_SHORT).show();

            }
        });
    }
    private void getRefferalCode(DatabaseReference dbref, String device_id, RefferalCodeCallback callback){
        dbref.child(getString(R.string.didkp)).child(device_id).child("refferal_code").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.getValue()!=null){
                    callback.refferalCode(snapshot.getValue().toString());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Error in reading db",error.getMessage());
                Toast.makeText(getActivity(),"Error in reading db "+error.getMessage(),Toast.LENGTH_SHORT).show();

            }
        });
    }
    private void inviteAndEarn(String code){
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);



        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Get rewarded for staying fit. Download the walker app get free amazon gift cards as you walk. Here are free 10 coins Refferal Code: "+code+"\n Download Now " + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID);
        startActivity(shareIntent);
    }
    private void getTotalFinalCoins(DatabaseReference dbref, CoinsCallback callback){
        dbref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                final int total_coins=(int)(long)snapshot.child("total_coins").getValue();
                final int total_coins_reward=(int)(long)snapshot.child("total_coins_reward").getValue();
                final int  total_coins_final=total_coins+total_coins_reward;
                callback.num_coins(total_coins_final);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

}