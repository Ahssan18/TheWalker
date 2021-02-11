package app.thewalker.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import app.thewalker.MainActivity;
import app.thewalker.R;
import app.thewalker.listeners.CurrentLevelCallback;
import app.thewalker.listeners.LevelCallback;
import app.thewalker.utils.DataMainPoint;

public class RunningService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    FirebaseDatabase fbdb;
    FirebaseAuth fbAuth;
    DatabaseReference dbref;
    FitnessOptions fitnessOptions;
    Context context=this;
    private static final String USERS_REF="users";
    private long timerTime = TimeUnit.SECONDS.toMillis(1800);
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                //Sync data to and fro every 300 seconds
                dbref=FirebaseDatabase.getInstance().getReference(USERS_REF);
                fbAuth=FirebaseAuth.getInstance();
                fitnessOptions=FitnessOptions.builder().
                        addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                        .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                        .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED,FitnessOptions.ACCESS_WRITE)
                        .addDataType(DataType.AGGREGATE_DISTANCE_DELTA,FitnessOptions.ACCESS_WRITE)
                        .addDataType(DataType.AGGREGATE_ACTIVITY_SUMMARY,FitnessOptions.ACCESS_READ)
                        .addDataType(DataType.AGGREGATE_MOVE_MINUTES,FitnessOptions.ACCESS_READ)
                        .build();


                Fitness.getHistoryClient(context, GoogleSignIn.getAccountForExtension(context,fitnessOptions))
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
                DataMainPoint.setTotalInfo(dbref,fbAuth.getCurrentUser().getUid());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                timerHandler.postDelayed(this, timerTime);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        fbAuth=FirebaseAuth.getInstance();
        dbref=FirebaseDatabase.getInstance().getReference(USERS_REF);
        startForeground(62318, builtNotification("Recording "));

        final String[] steps = {""};


        timerHandler.postDelayed(timerRunnable, 0);
    }

    public Notification builtNotification(String text)
    {
              NotificationManager notificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;

        NotificationCompat.Builder builder = null;


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
        {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel =
                    new NotificationChannel("ID", "Name", importance);
            // Creating an Audio Attribute
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();

            notificationManager.createNotificationChannel(notificationChannel);
            builder = new NotificationCompat.Builder(this, notificationChannel.getId());
        } else {
            builder = new NotificationCompat.Builder(this);
        }

        builder.setDefaults(Notification.DEFAULT_LIGHTS);


        String message = "Forever running service";



        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(false)
                .setPriority(Notification.PRIORITY_MAX)
                .setContentText(text+" Steps")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setColor(Color.parseColor("#0f9595"))
                .setContentTitle(getString(R.string.app_name));


        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        Notification notification = builder.build();
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        return notification;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(this, RestarterReceiver.class);
        this.sendBroadcast(broadcastIntent);
    }
    private void addValueToDb(DatabaseReference Usersref,int steps){
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        long updatedOnTs = System.currentTimeMillis();
        String str = sdf.format(System.currentTimeMillis());
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf1 = new SimpleDateFormat("HH:mm");
        String str1 = sdf1.format(new Date());

        Usersref.child("WorkoutHistory").child(str).child("steps").setValue(steps);
        Usersref.child("WorkoutHistory").child(str).child("updatedOn").setValue(str1);
        Usersref.child("WorkoutHistory").child(str).child("updatedOnTs").setValue(updatedOnTs);

        int level=getLevel(1,steps);
        getActualDbLevel(Usersref, level1-> {
            int coins=convertStepsToCoins(1,steps,level1);
            Usersref.child("WorkoutHistory").child(str).child("coins").setValue(coins);
        });
//        int coins=convertStepsToCoins(1,steps,level);
//        Usersref.child("WorkoutHistory").child(str).child("coins").setValue(coins);
        Usersref.child("WorkoutHistory").child(str).child("level").setValue(level);
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
    private int convertStepsToCoins(int multiplier,int steps,int level){

        int final_coins=0;

        int coins = Math.round((float) (steps */* 0.00*/1)) * multiplier;
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
    private void getActualDbLevel(DatabaseReference dbref, CurrentLevelCallback callback){
        dbref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                callback.currentLevel((int)(long)snapshot.child("TotalInfo").child("current_level").getValue());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


}