package app.thewalker.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.room.Database;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import app.thewalker.R;
import app.thewalker.adapters.HistoryRecyclerAdapter;
import app.thewalker.databinding.ActivityCoinsHistoryBinding;
import app.thewalker.listeners.HistoryDataItemsListener;
import app.thewalker.pojo.HistoryDataItem;
import app.thewalker.utils.EqualSpacingItemDecoration;

public class CoinsHistoryActivity extends AppCompatActivity {
    ActivityCoinsHistoryBinding binding;
    private boolean flagDecoration=false;
    FirebaseDatabase fbDb;
    DatabaseReference dbref;
    FirebaseAuth fbAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityCoinsHistoryBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());
        fbDb=FirebaseDatabase.getInstance();
        fbAuth=FirebaseAuth.getInstance();
        dbref=fbDb.getReference("users");
        if(fbAuth.getCurrentUser()!=null) {
            getHistoryData(dbref, fbAuth.getCurrentUser().getUid(), new HistoryDataItemsListener() {
                @Override
                public void historyDataItemsList(ArrayList<HistoryDataItem> historyDataItems) {
                    initRecyclerView(historyDataItems, CoinsHistoryActivity.this);
                }
            });
        }
    }
    private void initRecyclerView(ArrayList<HistoryDataItem> historyDataItems, Context context) {
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        binding.historyRv.setLayoutManager(layoutManager);

        HistoryRecyclerAdapter adapter = new HistoryRecyclerAdapter(context,historyDataItems);
        if(!flagDecoration)
        {
            binding.historyRv.addItemDecoration(new EqualSpacingItemDecoration(10, EqualSpacingItemDecoration.VERTICAL));

            flagDecoration = true;
        }

        binding.historyRv.setAdapter(adapter);
    }
    private void getHistoryData(DatabaseReference dbref, String uid, HistoryDataItemsListener listener){
        dbref.child(uid).child("WorkoutHistory").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<HistoryDataItem> historyDataItems = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String updated_on = ds.getKey();
                    int steps = (int) (long) ds.child("steps").getValue();
                    int coins = (int) (long) ds.child("coins").getValue();
                    historyDataItems.add(new HistoryDataItem(updated_on, coins, steps));
                }
                listener.historyDataItemsList(historyDataItems);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("db_error: ",error.getMessage());
            }

        });
    }
}