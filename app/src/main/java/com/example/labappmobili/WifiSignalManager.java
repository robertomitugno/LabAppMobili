package com.example.labappmobili;

import static com.example.labappmobili.GridTileProvider.inMetersLatCoordinate;
import static com.example.labappmobili.GridTileProvider.inMetersLngCoordinate;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.room.Room;

import com.example.labappmobili.RoomDB.LTE.LTE;
import com.example.labappmobili.RoomDB.LTE.LTEDB;
import com.example.labappmobili.RoomDB.LTE.LTEDao;
import com.example.labappmobili.RoomDB.WiFi.WiFi;
import com.example.labappmobili.RoomDB.WiFi.WiFiDB;
import com.example.labappmobili.RoomDB.WiFi.WiFiDao;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class WifiSignalManager {
    private final Context context;
    private final Location currentLocation;
    private final GoogleMap googleMap;
    static WiFiDB wifidb;

    List<WiFi> WifiList;

    private TileOverlay gridOverlay;



    public WifiSignalManager(Context context, Location currentLocation, GoogleMap map) {
        this.context = context;
        this.currentLocation = currentLocation;

        this.googleMap = map;
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

    public void updateWifiSignalStrength() {

        initializeRoomDatabase();
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        Log.d("prova", "wifimanager : " + wifiManager);

        TextView wifiSignalStrengthText = ((MainActivity) context).findViewById(R.id.variableText);

            double latitudine = inMetersLatCoordinate(currentLocation.getLatitude());
            double longitudine = inMetersLngCoordinate(currentLocation.getLongitude());

            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int signalStrength = wifiInfo.getRssi() + 127;
            wifiSignalStrengthText.setText("WiFi : " + signalStrength + " Mb/s");
            Log.d("prova", "wifimanager");

            insertWifiMeasurement(latitudine, longitudine, signalStrength);
            //getWifiListInBackground();

            GridTileProvider gridTileProvider = new GridTileProvider(context, getAllWifiValue());
            GridManager.getInstance().setGrid(googleMap, gridTileProvider);


    }
    private void initializeRoomDatabase() {
        wifidb = Room.databaseBuilder(context, WiFiDB.class, "WifiDatabase").build();
    }

    private void insertWifiMeasurement(double latitudine, double longitudine, double signalStrength) {
        WiFi wifiMeasurement = new WiFi(latitudine, longitudine, signalStrength);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            // Esegui l'operazione di inserimento nel database Room su un thread separato
            WiFiDao wifiDAo = wifidb.getWiFiDao();
            wifiDAo.insertWiFi(wifiMeasurement);
        });
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

    public void getWifiListInBackground(){

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Handler handler = new Handler(Looper.getMainLooper());

        executorService.execute(new Runnable() {
            @Override
            public void run() {

                //background task
                WifiList = wifidb.getWiFiDao().getAllWifi();


                //on finish task
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        StringBuilder sb = new StringBuilder();
                        for(WiFi wifi : WifiList){
                            sb.append(wifi.getLatitudine() + " : " + wifi.getLongitudine() +" -> " +wifi.getWiFiValue());
                            sb.append("\n");
                        }

                        String finalData = sb.toString();
                        Toast.makeText(context, ""+finalData, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

    }

    public void setGrid(GoogleMap myMap, GridTileProvider gridTileProvider) {
        // Aggiungi la griglia alla mappa utilizzando un TileOverlay
        Log.d("prova", "setGrid");

        if (gridOverlay != null) {
            Log.d("prova", "if setgrid");
            gridOverlay.remove(); // Rimuovi la griglia se è già presente
        }
        gridOverlay = myMap.addTileOverlay(new TileOverlayOptions().tileProvider(gridTileProvider));
    }


}