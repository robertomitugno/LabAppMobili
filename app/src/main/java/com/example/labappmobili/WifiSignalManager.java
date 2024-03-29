package com.example.labappmobili;

import static com.example.labappmobili.GridTileProvider.latitudineInMeters;
import static com.example.labappmobili.GridTileProvider.longitudineInMeters;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.room.Room;

import com.example.labappmobili.RoomDB.WiFi.WiFi;
import com.example.labappmobili.RoomDB.WiFi.WiFiDB;
import com.example.labappmobili.RoomDB.WiFi.WiFiDao;
import com.google.android.gms.maps.GoogleMap;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class WifiSignalManager {
    private static Context context;
    private static GoogleMap googleMap;
    static WiFiDB wifidb;
    Location currentLocation;

    static GridTileProvider gridTileProvider;

    public WifiSignalManager(Context context) {
        this.context = context;
        initializeRoomDatabase();
    }

    public WifiSignalManager(Context context, GoogleMap map) {
        this.context = context;
        this.googleMap = map;
        initializeRoomDatabase();
    }

    public WifiSignalManager(Context context, Location currentLocation, GoogleMap map) {
        this.context = context;
        this.currentLocation = currentLocation;
        this.googleMap = map;
        initializeRoomDatabase();
    }

    private static void initializeRoomDatabase() {
        wifidb = Room.databaseBuilder(context, WiFiDB.class, "WifiDatabase").build();
    }

    static void showWifiMap(){
        gridTileProvider = new GridTileProvider(context, getAllWifiValue());
        GridManager.getInstance().setGrid(googleMap, gridTileProvider);
    }


    static List<WiFi> getAllWifiValue() {

        if (wifidb == null) {
            return new ArrayList<>(); // Il database non è ancora inizializzato
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        AtomicReference<List<WiFi>> wifiListRef = new AtomicReference<>(new ArrayList<>());

        executorService.execute(() -> {
            WiFiDao wiFiDao = wifidb.getWiFiDao();
            wifiListRef.set(wiFiDao.getAllWifi());
        });

        executorService.shutdown();

        while (!executorService.isTerminated()) {
            // Attendi fino a quando il thread non è terminato
        }

        return wifiListRef.get();
    }

    static int getWifiLevel(){
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo.getRssi() + 127 > 0)
            return wifiInfo.getRssi() + 127;
        else
            return 0;
    }



    static String insertWifiMeasurement(Location currentLocation, long time) {

        initializeRoomDatabase(); // Inizializza il database Room

        Date date = new Date();

        int signalStrength = getWifiLevel();

        double latitudine = latitudineInMeters(currentLocation.getLatitude());
        double longitudine = longitudineInMeters(currentLocation.getLongitude());


        GridTileProvider gridTileProvider = new GridTileProvider(context, getAllWifiValue());

        if(gridTileProvider.checkTimeArea(currentLocation, time).startsWith(context.getResources().getString(R.string.waiting))){
            return gridTileProvider.checkTimeArea(currentLocation, time);
        }

        WiFi wifiMeasurement = new WiFi(latitudine, longitudine, signalStrength, date.getTime());

        Log.d("Misurazione","Inserimento wifi : " + latitudine + " : " + longitudine + " : " + signalStrength);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            // Esegui l'operazione di inserimento nel database Room su un thread separato
            WiFiDao wifiDAo = wifidb.getWiFiDao();
            wifiDAo.insertWiFi(wifiMeasurement);
        });

        //getWifiListInBackground();

        return context.getResources().getString(R.string.measurement_complete);
    }


    static void deleteWifiMeasurement(Object wifi) {

        initializeRoomDatabase(); // Inizializza il database Room

        Log.d("Misurazione","Eliminazione wifi : " + wifi);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            // Esegui l'operazione di inserimento nel database Room su un thread separato
            WiFiDao wiFiDao = wifidb.getWiFiDao();
            wiFiDao.deleteWifiById(((WiFi) wifi).getId());
        });

    }



    public static int getWifiColor(double wiFiValue) {
        if (wiFiValue > 50) {
            return Color.GREEN;
        } else if (wiFiValue > 30) {
            return Color.YELLOW;
        } else if (wiFiValue <= 30) {
            return Color.RED;
        } else {
            return Color.TRANSPARENT;
        }
    }

}