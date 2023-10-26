package com.example.labappmobili;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.widget.TextView;

public class LteSignalManager {

    private final Context context;
    private boolean isLTERequested = true;
    private TelephonyManager telephonyManager;

    public LteSignalManager(Context context) {
        this.context = context;
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE); // Inizializza il TelephonyManager
    }

    void updateLTELevel() {
        TextView lteSignal = ((MainActivity) context).findViewById(R.id.lteValue);
        if (isLTERequested) {
            int lteLevel = getLTELevel();
            lteSignal.setText("LTE : " + lteLevel + " dBm");
        } else {
            lteSignal.setText(R.string.lte_level_n_a);
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
}

