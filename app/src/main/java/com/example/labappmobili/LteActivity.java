package com.example.labappmobili;

import static java.lang.Double.parseDouble;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.labappmobili.RoomDB.LTE.LTE;
import com.example.labappmobili.RoomDB.LTE.LTEDB;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LteActivity extends AppCompatActivity {

    Button saveButton, getDataButton;
    EditText latitudineEdit, longitudineEdit, idUpdate, idDelete;

    List<LTE> LTElist;

    LTEDB ltedb;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lte_activity);

        saveButton = findViewById(R.id.saveButton);
        getDataButton = findViewById(R.id.getDataButton);


        latitudineEdit = findViewById(R.id.latitudineEdit);
        longitudineEdit = findViewById(R.id.longitudineEdit);

        RoomDatabase.Callback myCallBack = new RoomDatabase.Callback() {
            @Override
            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                super.onCreate(db);
            }

            @Override
            public void onOpen(@NonNull SupportSQLiteDatabase db) {
                super.onOpen(db);
            }
        };

        ltedb = Room.databaseBuilder(getApplicationContext(), LTEDB.class , "ltedb").addCallback(myCallBack).build();

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double latitudine = Double.parseDouble(latitudineEdit.getText().toString());
                double longitudine = Double.parseDouble(longitudineEdit.getText().toString());

                LTE lte1 = new LTE(latitudine, longitudine,0);

                addLteInBackground(lte1, getApplicationContext());
            }
        });

        getDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                getLteListInBackground();

            }
        });

    }



    public static void addLteInBackground(LTE lte, Context context){

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Handler handler = new Handler(Looper.getMainLooper());

        RoomDatabase.Callback myCallBack = new RoomDatabase.Callback() {
            @Override
            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                super.onCreate(db);
            }

            @Override
            public void onOpen(@NonNull SupportSQLiteDatabase db) {
                super.onOpen(db);
            }
        };

        LTEDB ltedb = Room.databaseBuilder(context.getApplicationContext(), LTEDB.class , "ltedb").addCallback(myCallBack).build();


        executorService.execute(new Runnable() {
            @Override
            public void run() {

                //background task
                ltedb.getLTEDao().insertLTE(lte);


                //on finish task
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
            }
        });

    }



    public void getLteListInBackground(){

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Handler handler = new Handler(Looper.getMainLooper());

        executorService.execute(new Runnable() {
            @Override
            public void run() {

                //background task
                LTElist = ltedb.getLTEDao().getAllLte();


                //on finish task
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        StringBuilder sb = new StringBuilder();
                        for(LTE lte : LTElist){
                            sb.append(lte.getLatitudine() + " : " + lte.getLongitudine());
                            sb.append("\n");
                        }

                        String finalData = sb.toString();
                        Toast.makeText(LteActivity.this, ""+finalData, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

    }


}
