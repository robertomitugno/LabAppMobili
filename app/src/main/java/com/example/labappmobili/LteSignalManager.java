package com.example.labappmobili;

import static com.example.labappmobili.GridTileProvider.inMetersLatCoordinate;
import static com.example.labappmobili.GridTileProvider.inMetersLngCoordinate;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.room.Room;

import com.example.labappmobili.RoomDB.LTE.LTE;
import com.example.labappmobili.RoomDB.LTE.LTEDB;
import com.example.labappmobili.RoomDB.LTE.LTEDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LteSignalManager {

    private final Context context;
    private boolean isLTERequested = true;
    private TelephonyManager telephonyManager;

    Location currentLocation;

    LTEDB ltedb;

    List<LTE> LTElist;


    public LteSignalManager(Context context, Location currentLocation) {
        this.context = context;
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE); // Inizializza il TelephonyManager
        this.currentLocation = currentLocation;
    }

    int updateLTELevel() {
        TextView lteSignal = ((MainActivity) context).findViewById(R.id.lteValue);
        if (isLTERequested) {
            int lteLevel = getLTELevel();
            lteSignal.setText("LTE : " + lteLevel + " dBm");

            double latitudine = inMetersLatCoordinate(currentLocation.getLatitude());
            double longitudine = inMetersLngCoordinate(currentLocation.getLongitude());


            // Inserisci la misurazione nel database Room
            insertLTEMeasurement(latitudine, longitudine, lteLevel);
            //getLteListInBackground();

            return lteLevel;
        } else {
            lteSignal.setText(R.string.lte_level_n_a);
            return 0;
        }
    }

    private int getLTELevel() {
        try {
            // Ottieni la forza del segnale LTE
            int lteSignalStrength = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                lteSignalStrength = telephonyManager.getSignalStrength().getLevel();
            }
            return lteSignalStrength;
        } catch (Exception e) {
            return 0; // Ritorna un valore di default se si verifica un'eccezione
        }
    }


    private void insertLTEMeasurement(double latitudine, double longitudine, int lteLevel) {
        LTE lteMeasurement = new LTE(latitudine, longitudine, lteLevel);
        ltedb = Room.databaseBuilder(context, LTEDB.class, "LteDB").build();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            // Esegui l'operazione di inserimento nel database Room su un thread separato
            LTEDao lteDao = ltedb.getLTEDao();
            lteDao.insertLTE(lteMeasurement);
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
                            sb.append(lte.getLatitudine() + " : " + lte.getLongitudine() +" -> " +lte.getLteValue());
                            sb.append("\n");
                        }

                        String finalData = sb.toString();
                        Toast.makeText(context, ""+finalData, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

    }

}

