package com.example.labappmobili;

import static com.example.labappmobili.GridTileProvider.inMetersLatCoordinate;
import static com.example.labappmobili.GridTileProvider.inMetersLngCoordinate;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.room.Room;

import com.example.labappmobili.RoomDB.LTE.LTE;
import com.example.labappmobili.RoomDB.LTE.LTEDB;
import com.example.labappmobili.RoomDB.LTE.LTEDao;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class LteSignalManager {

    private static Context context;
    private GoogleMap googleMap;
    private boolean isLTERequested = true;
    private TelephonyManager telephonyManager;

    Location currentLocation;

    static LTEDB ltedb;

    private TileOverlay gridOverlay;

    private static final int TILE_SIZE_DP = 256;
    private final Bitmap borderTile;

    private static final double[] TILES_ORIGIN = {-20037508.34789244, 20037508.34789244};
    // Size of square world map in meters, using WebMerc projection.
    private static final double MAP_SIZE = 20037508.34789244 * 2;
    private static final double ORIGIN_SHIFT = Math.PI * 6378137d;
    private static final int gridSize = 6;
    private final float scaleFactor;

    static List<LTE> LTElist;


    public LteSignalManager(Context context, Location currentLocation, GoogleMap map) {
        this.context = context;
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE); // Inizializza il TelephonyManager
        this.currentLocation = currentLocation;
        this.googleMap = map;
        this.scaleFactor = context.getResources().getDisplayMetrics().density * 0.6f;
        this.borderTile = Bitmap.createBitmap((int) (TILE_SIZE_DP * scaleFactor),
                (int) (TILE_SIZE_DP * scaleFactor), Bitmap.Config.ARGB_8888);
    }


    int updateLTELevel() {
        initializeRoomDatabase(); // Inizializza il database Room

        TextView variableText = ((MainActivity) context).findViewById(R.id.variableText);


        double latitudine = inMetersLatCoordinate(currentLocation.getLatitude());
        double longitudine = inMetersLngCoordinate(currentLocation.getLongitude());

        // Inserisci la misurazione nel database Room
        int lteLevel = getLTELevel();
        insertLTEMeasurement(latitudine, longitudine, lteLevel);

        // Passa il valore di lteValue alla classe GridTileProvider
        GridTileProvider gridTileProvider = new GridTileProvider(context, getAllLteValue());
        GridManager.getInstance().setGrid(googleMap, gridTileProvider);

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
        ltedb = Room.databaseBuilder(context, LTEDB.class, "LteDB").build();

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

    public void getLteListInBackground(){

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {

                //background task
                LTElist = ltedb.getLTEDao().getAllLte();

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

}

