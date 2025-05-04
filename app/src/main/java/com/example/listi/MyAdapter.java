package com.example.listi;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    private Context context;
    private List<WordList> listArrayWordList;

    public MyAdapter(Context context, List<WordList> listArrayWordList) {
        this.context = context;
        this.listArrayWordList = listArrayWordList;

    }

    @NonNull
    @Override
    public MyAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_selection, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyAdapter.MyViewHolder holder, int position) {
        WordList wordList = listArrayWordList.get(position);
        holder.name.setText(wordList.name);

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(wordList.isChecked());

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            wordList.setChecked(isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return listArrayWordList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        CheckBox checkBox;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.listHeader);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }

    public List<WordList> getCheckedItems() {
        List<WordList> checkedItems = new ArrayList<>();
        for (WordList list : listArrayWordList) {
            if (list.isChecked()) {
                checkedItems.add(list);
            }
        }
        return checkedItems;
    }
}

