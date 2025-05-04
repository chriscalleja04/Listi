package com.example.listi;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class MyYearGroupsAdapter extends RecyclerView.Adapter<MyYearGroupsAdapter.MyViewHolder> {
    private final RecyclerViewInterface recyclerViewInterface;
    private Context context;
    private ArrayList<YearGroup> yearGroupArrayList;

    private YearGroupActionListener actionListener;

    private String role;



    public MyYearGroupsAdapter(Context context, ArrayList<YearGroup> yearGroupArrayList, RecyclerViewInterface recyclerViewInterface, YearGroupActionListener actionListener, String role) {
        this.yearGroupArrayList = yearGroupArrayList;
        this.context = context;
        this.recyclerViewInterface = recyclerViewInterface;
        this.actionListener = actionListener;
        this.role = role;
    }

    @NonNull
    @Override
    public MyYearGroupsAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item, parent, false);

        return new MyViewHolder(v, recyclerViewInterface);
    }

    @Override
    public void onBindViewHolder(@NonNull MyYearGroupsAdapter.MyViewHolder holder, int position) {
        YearGroup yearGroup = yearGroupArrayList.get(position);
        holder.name.setText(yearGroup.getName());
        if(role.equals("admin")) {
            holder.edit.setVisibility(View.VISIBLE);
            holder.delete.setVisibility(View.VISIBLE);
            holder.edit.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putString("name", yearGroup.getName());
                bundle.putString("id", yearGroup.getID());
                Navigation.findNavController(v).navigate(R.id.updateYearGroup, bundle);
            });
            holder.delete.setOnClickListener(v -> {
                actionListener.onDeleteYearGroup(yearGroup.getID());
            });
        }else{
            holder.edit.setVisibility(View.GONE);
            holder.delete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return yearGroupArrayList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        FloatingActionButton edit, delete;
        public MyViewHolder(@NonNull View itemView, RecyclerViewInterface recyclerViewInterface) {
            super(itemView);
            name = itemView.findViewById(R.id.listHeader);
            edit = itemView.findViewById(R.id.editItem);
            delete = itemView.findViewById(R.id.deleteItem);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(recyclerViewInterface != null){
                        int position = getAdapterPosition();
                        if(position != RecyclerView.NO_POSITION){
                            recyclerViewInterface.onItemClick(position);
                        }
                    }
                }
            });
        }
    }

    public interface YearGroupActionListener{
        void onDeleteYearGroup(String yearGroupId);
    }
}
