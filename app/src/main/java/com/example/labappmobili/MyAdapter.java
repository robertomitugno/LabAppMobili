package com.example.labappmobili;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

    private List<?> objectList;
    private OnItemClickListener onItemClickListener;

    public MyAdapter(List<?> objectList, OnItemClickListener onItemClickListener) {
        this.objectList = objectList;
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(Object item);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_object, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Object object = objectList.get(position);
        holder.bind(String.valueOf(object));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClickListener.onItemClick(object);
            }
        });
    }

    @Override
    public int getItemCount() {
        return objectList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView textView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textViewObject);
        }

        void bind(String object) {
            textView.setText(object);
        }
    }
}

