package com.example.nlnx;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<HistoryItem> historyItems;
    private final DeleteListener deleteListener;

    public interface DeleteListener {
        void onDelete(int position);
    }

    public HistoryAdapter(List<HistoryItem> historyItems, DeleteListener deleteListener) {
        this.historyItems = historyItems;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.history_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = historyItems.get(position);
        holder.tvPrompt.setText(item.getPrompt());
        holder.tvResponse.setText(item.getResponse());
        holder.tvModel.setText(item.getModel());

        holder.btnDelete.setOnClickListener(v ->
                deleteListener.onDelete(holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    public void updateData(List<HistoryItem> newItems) {
        historyItems = newItems;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPrompt, tvResponse, tvModel;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPrompt = itemView.findViewById(R.id.tv_prompt);
            tvResponse = itemView.findViewById(R.id.tv_response);
            tvModel = itemView.findViewById(R.id.tv_model);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}