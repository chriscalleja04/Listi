package com.example.listi;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MyStudentAdapter extends RecyclerView.Adapter<MyStudentAdapter.MyViewHolder> {
    private final RecyclerViewInterface recyclerViewInterface;
    private Context context;
    private ArrayList<Student> studentArrayList;

    public MyStudentAdapter(Context context,ArrayList<Student> studentArrayList, RecyclerViewInterface recyclerViewInterface) {
        this.studentArrayList = studentArrayList;
        this.context = context;
        this.recyclerViewInterface = recyclerViewInterface;
    }

    @NonNull
    @Override
    public MyStudentAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item, parent, false);

        return new MyViewHolder(v, recyclerViewInterface);
    }

    @Override
    public void onBindViewHolder(@NonNull MyStudentAdapter.MyViewHolder holder, int position) {
        Student student = studentArrayList.get(position);
        holder.name.setText(student.getName());
    }

    @Override
    public int getItemCount() {
        return studentArrayList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView name, email;
        public MyViewHolder(@NonNull View itemView, RecyclerViewInterface recyclerViewInterface) {
            super(itemView);
            name = itemView.findViewById(R.id.listHeader);
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
}
