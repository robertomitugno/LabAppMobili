package com.example.labappmobili;

import static com.example.labappmobili.GridTileProvider.inMetersLatCoordinate;
import static com.example.labappmobili.GridTileProvider.inMetersLngCoordinate;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;

import android.telephony.TelephonyManager;
import android.util.Log;


import androidx.room.Room;

import com.example.labappmobili.RoomDB.LTE.LTE;
import com.example.labappmobili.RoomDB.LTE.LTEDB;
import com.example.labappmobili.RoomDB.LTE.LTEDao;
import com.google.android.gms.maps.GoogleMap;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class LteSignalManager {

    private static Context context;
    private static GoogleMap googleMap;

    private static TelephonyManager telephonyManager;

    Location currentLocation;
    static LTEDB ltedb;
    static GridTileProvider gridTileProvider;

    static List<LTE> LTElist;

    public LteSignalManager(Context context) {
        this.context = context;
        initializeRoomDatabase();
    }
    public LteSignalManager(Context context, GoogleMap map) {
        this.context = context;
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE); // Initialize il TelephonyManager
        this.googleMap = map;
        initializeRoomDatabase();
    }

    public LteSignalManager(Context context, Location currentLocation, GoogleMap map) {
        this.context = context;
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE); // Initialize TelephonyManager
        this.currentLocation = currentLocation;
        this.googleMap = map;
        initializeRoomDatabase();
    }


    static List<LTE> getAllLteValue() {

        initializeRoomDatabase();

        if (ltedb == null) {
            return new ArrayList<>(); //Il database non è ancora inizializzato
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

    private static void initializeRoomDatabase() {
        ltedb = Room.databaseBuilder(context, LTEDB.class, "LteDB").build();
    }

    static int getLTELevel() {
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


    static String insertLTEMeasurement(Location currentLocation, long time) {

        initializeRoomDatabase(); // Inizializza il database Room

        Date date = new Date();

        int lteLevel = getLTELevel();

        double latitudine = inMetersLatCoordinate(currentLocation.getLatitude());
        double longitudine = inMetersLngCoordinate(currentLocation.getLongitude());

        GridTileProvider gridTileProvider = new GridTileProvider(context, getAllLteValue());

        if(gridTileProvider.checkTimeArea(currentLocation, time).startsWith(context.getResources().getString(R.string.waiting))){
            return gridTileProvider.checkTimeArea(currentLocation, time);
        }

        LTE lteMeasurement = new LTE(latitudine, longitudine, lteLevel, date.getTime());

        Log.d("Misurazione","Inserimento lte : " + latitudine + " : " + longitudine + " : " + lteLevel);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            // Esegui l'operazione di inserimento nel database Room su un thread separato
            LTEDao lteDao = ltedb.getLTEDao();
            lteDao.insertLTE(lteMeasurement);
        });

        //getLteListInBackground();

        return context.getResources().getString(R.string.measurement_complete);
    }


    static void deleteLTEMeasurement(Object lte) {

        initializeRoomDatabase(); // Inizializza il database Room

        Log.d("Misurazione","Eliminazione lte : " + lte);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            // Esegui l'operazione di inserimento nel database Room su un thread separato
            LTEDao lteDao = ltedb.getLTEDao();
            lteDao.deleteLTEById(((LTE) lte).getId());
        });
    }

    static void showLteMap(){
        gridTileProvider = new GridTileProvider(context, getAllLteValue());
        GridManager.getInstance().setGrid(googleMap, gridTileProvider);
    }


    public static int getLteColor(int valore){
        if (valore > 3) {
            return Color.GREEN;
        } else if (valore > 2) {
            return Color.YELLOW;
        } else {
            return Color.RED;
        }
    }

}

