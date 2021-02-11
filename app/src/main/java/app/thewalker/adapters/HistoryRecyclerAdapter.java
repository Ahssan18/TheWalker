package app.thewalker.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import app.thewalker.R;
import app.thewalker.pojo.HistoryDataItem;

public class HistoryRecyclerAdapter extends RecyclerView.Adapter<HistoryRecyclerAdapter.MyRecyclerAdapter> {
    Context context;
    ArrayList<HistoryDataItem> historyDataItems = new ArrayList<>();
    LayoutInflater inflater;
    public HistoryRecyclerAdapter(Context context,ArrayList<HistoryDataItem> historyDataItems){
        this.context=context;
        this.historyDataItems=historyDataItems;
        this.inflater=LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public MyRecyclerAdapter onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyRecyclerAdapter(inflater.inflate(R.layout.history_item,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyRecyclerAdapter holder, int position) {
        holder.steps_txt.setText(historyDataItems.get(position).getSteps() + " steps");
        holder.coins_txt.setText(""+historyDataItems.get(position).getNum_coins());
        holder.updated_on.setText(historyDataItems.get(position).getUpdated_on());
    }

    @Override
    public int getItemCount() {
        Log.e("SIze",""+historyDataItems.size());
        return historyDataItems.size();
    }

    public static class MyRecyclerAdapter extends RecyclerView.ViewHolder {
        TextView steps_txt;
        TextView coins_txt;
        TextView updated_on;
        public MyRecyclerAdapter(@NonNull View itemView) {
            super(itemView);
            steps_txt=itemView.findViewById(R.id.history_steps_txt);
            coins_txt=itemView.findViewById(R.id.history_coin_txt);
            updated_on=itemView.findViewById(R.id.history_updated_on_txt);
        }
    }
}
