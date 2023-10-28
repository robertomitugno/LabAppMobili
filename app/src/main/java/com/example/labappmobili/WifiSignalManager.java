package com.example.labappmobili;

import android.content.Context;
import android.location.Location;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.room.Room;

import com.example.labappmobili.RoomDB.WiFi.WiFi;
import com.example.labappmobili.RoomDB.WiFi.WiFiDB;
import com.example.labappmobili.RoomDB.WiFi.WiFiDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WifiSignalManager {
    private final Context context;
    private final Location currentLocation;
    WiFiDB wifidb;

    List<WiFi> WifiList;


    public WifiSignalManager(Context context, Location currentLocation) {
        this.context = context;
        this.currentLocation = currentLocation;
    }

    public void updateWifiSignalStrength() {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        TextView wifiSignalStrengthText = ((MainActivity) context).findViewById(R.id.wifiValue);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int signalStrength = wifiInfo.getRssi() + 127;
            wifiSignalStrengthText.setText("WiFi : " + signalStrength + " Mb/s");

            insertWifiMeasurement(currentLocation.getLatitude(), currentLocation.getLongitude(), signalStrength);
            //getWifiListInBackground();

        } else {
            wifiSignalStrengthText.setText(R.string.wifi_signal_strength_n_a);
        }
    }


    private void insertWifiMeasurement(double latitudine, double longitudine, double signalStrength) {
        WiFi wifiMeasurement = new WiFi(latitudine, longitudine, signalStrength);
        wifidb = Room.databaseBuilder(context, WiFiDB.class, "WifiDB").build();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            // Esegui l'operazione di inserimento nel database Room su un thread separato
            WiFiDao wifiDAo = wifidb.getWiFiDao();
            wifiDAo.insertWiFi(wifiMeasurement);
        });
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

}