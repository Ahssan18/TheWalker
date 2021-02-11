package app.thewalker.utils;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import app.thewalker.listeners.CoinsCallback;
import app.thewalker.listeners.CurrentLevelCallback;
import app.thewalker.listeners.DailyDetailsCallback;
import app.thewalker.listeners.StepsCallback;
import app.thewalker.listeners.TotalInfoCallback;

public class DataMainPoint {
    public static void getActualLevel(DatabaseReference dbref, CurrentLevelCallback callback){
        dbref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.child("TotalInfo").getValue()!=null) {
                    callback.currentLevel((int) (long) snapshot.child("TotalInfo").child("current_level").getValue());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    public static void getDailyData(DatabaseReference dbref, DailyDetailsCallback callback){
        dbref.child("WorkoutInfo").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
               if(snapshot.getValue()!=null&&snapshot.child("DailyCoin").getValue()!=null) {
                   callback.getDailyDetails((int) (long) snapshot.child("DailyCoin").getValue()
                           , (int) (long) snapshot.child("DailySteps").getValue(), snapshot.child("UpdatedOn").getValue().toString());
               }else {
                   Log.e("DailyData", "onDataChange error" );
               }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    public static void getTotalInfo(DatabaseReference dbref, TotalInfoCallback callback){
        dbref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                callback.totalInfo((int)(long)snapshot.child("current_level").getValue(),(int)(long)snapshot.child("total_coins").getValue(),(int)(long)snapshot.child("total_steps").getValue());

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    public static void setTotalInfo(DatabaseReference dbref,String auth){
     getSumCoins(dbref, new CoinsCallback() {
         @Override
         public void num_coins(int coins) {
             dbref.child(auth).child("TotalInfo").child("total_coins").setValue(coins);
         }
     },auth);
     getSumSteps(dbref, new StepsCallback() {
         @Override
         public void steps(int steps) {
             dbref.child(auth).child("TotalInfo").child("total_steps").setValue(steps);
         }
     },auth);

    }
    public static void getSumCoins(DatabaseReference dbref,CoinsCallback callback,String auth){
        dbref.child(auth).child("WorkoutHistory").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int sum_coins=0;
                for(DataSnapshot ds:snapshot.getChildren()){
                    if(ds.child("coins").getValue()!=null) {
                        int coins = (int) (long) ds.child("coins").getValue();
                        sum_coins = sum_coins + coins;
                    }
                }
                callback.num_coins(sum_coins);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    public static void getSumSteps(DatabaseReference dbref, StepsCallback callback, String auth){
        dbref.child(auth).child("WorkoutHistory").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int sum_steps=0;
                for(DataSnapshot ds:snapshot.getChildren()){
                    int steps = (int)(long)ds.child("steps").getValue();
                    sum_steps=sum_steps+steps;
                }
                callback.steps(sum_steps);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    public static void getTotalCoinsReward(DatabaseReference dbref,CoinsCallback callback){
        dbref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.child("total_coins_reward").getValue()!=null){
                    callback.num_coins((int)(long)snapshot.child("total_coins_reward").getValue());
                }else{
                    callback.num_coins(-1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }
}
