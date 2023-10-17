package com.example.labappmobili;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.widget.TextView;

public class WifiSignalManager {
    private Context context;

    public WifiSignalManager(Context context) {
        this.context = context;
    }

    public void updateWifiSignalStrength() {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        TextView wifiSignalStrengthText = ((MainActivity) context).findViewById(R.id.wifiValue);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int signalStrength = wifiInfo.getRssi() + 127;
            wifiSignalStrengthText.setText("WiFi Signal Strength: " + signalStrength + " dBm");
        } else {
            wifiSignalStrengthText.setText("WiFi Signal Strength: N/A");
        }
    }
}