package com.example.labappmobili;

import static com.example.labappmobili.GridTileProvider.inMetersLatCoordinate;
import static com.example.labappmobili.GridTileProvider.inMetersLngCoordinate;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;

import androidx.room.Room;

import com.example.labappmobili.RoomDB.LTE.LTE;
import com.example.labappmobili.RoomDB.LTE.LTEDB;
import com.example.labappmobili.RoomDB.LTE.LTEDao;
import com.google.android.gms.maps.GoogleMap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class LteSignalManager {

    private static Context context;
    private GoogleMap googleMap;

    private TelephonyManager telephonyManager;

    Location currentLocation;
    static LTEDB ltedb;
    static GridTileProvider gridTileProvider;

    static List<LTE> LTElist;


    public LteSignalManager(Context context, GoogleMap map) {
        this.context = context;
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE); // Inizializza il TelephonyManager
        this.googleMap = map;
    }

    public LteSignalManager(Context context, Location currentLocation, GoogleMap map) {
        this.context = context;
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE); // Inizializza il TelephonyManager
        this.currentLocation = currentLocation;
        this.googleMap = map;
    }


    int updateLTELevel() {
        initializeRoomDatabase(); // Inizializza il database Room

        TextView variableText = ((MainActivity) context).findViewById(R.id.variableText);

        int lteLevel = getLTELevel();

        if(currentLocation != null){
            double latitudine = inMetersLatCoordinate(currentLocation.getLatitude());
            double longitudine = inMetersLngCoordinate(currentLocation.getLongitude());

            // Inserisci la misurazione nel database Room
            insertLTEMeasurement(latitudine, longitudine, lteLevel);
        }

        // Passa il valore di lteValue alla classe GridTileProvider
        gridTileProvider = new GridTileProvider(context, getAllLteValue());
        GridManager.getInstance().setGrid(googleMap, gridTileProvider);

        //boolean boh = GridTileProvider.checkEmptyArea(currentLocation, getAllLteValue());
        variableText.setText("LTE : " + lteLevel + " dBm");

        return lteLevel;
    }


    static List<LTE> getAllLteValue() {

        if (ltedb == null) {
            return new ArrayList<>(); // Il database non è ancora inizializzato
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        AtomicReference<List<LTE>> lteListRef = new AtomicReference<>(new ArrayList<>());

        executorService.execute(() -> {
            LTEDao lteDao = ltedb.getLTEDao();
            lteListRef.set(lteDao.getAllLte());
        });

        executorService.shutdown();

        while (!executorService.isTerminated()) {
            // Attendi fino a quando il thread non è terminato
        }

        return lteListRef.get();
    }

    private void initializeRoomDatabase() {
        ltedb = Room.databaseBuilder(context, LTEDB.class, "LteDB").build();
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

        Log.d("Misurazione","Inserimento Lte : " + latitudine + " : " + longitudine + " : " + lteLevel);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            // Esegui l'operazione di inserimento nel database Room su un thread separato
            LTEDao lteDao = ltedb.getLTEDao();
            lteDao.insertLTE(lteMeasurement);
        });
    }

    private int getColorForAverageLTELevel() {

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                //background task
                LTElist = ltedb.getLTEDao().getLastFiveLte();
            }
        });

        if (LTElist != null && LTElist.size() >= 5) {
            int sum = 0;
            int count = 0;

            // Calcola la media dei valori delle ultime 5 misurazioni
            for (int i = LTElist.size() - 1; i >= LTElist.size() - 5; i--) {
                sum += LTElist.get(i).getLteValue();
                count++;
            }

            int average = sum / count;

            return getLteColor(average);
        } else {
            // Restituisci un colore di default se non ci sono abbastanza misurazioni
            return Color.TRANSPARENT;
        }


    }

    public static int getLteColor(int valore){
        if (valore > 3) {
            return Color.GREEN;
        } else if (valore > 2) {
            return Color.YELLOW;
        } else if (valore <= 2) {
            return Color.RED;
        } else {
            return Color.TRANSPARENT;
        }
    }

    public static void removeGrid(){
        GridManager.getInstance().removeGrid();
    }

}

