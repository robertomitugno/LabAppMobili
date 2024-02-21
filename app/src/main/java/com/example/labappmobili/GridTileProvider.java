package com.example.labappmobili;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.Location;

import com.example.labappmobili.RoomDB.LTE.LTE;
import com.example.labappmobili.RoomDB.Noise.Noise;
import com.example.labappmobili.RoomDB.WiFi.WiFi;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GridTileProvider implements TileProvider {

    private static final int sizeTile = 256;
    private Bitmap bitmap;

    private static final double[] MapCoordinates = {-20037508.34789244, 20037508.34789244};
    private static final double MapSize = 20037508.34789244 * 2;
    private static final double OriginShift = Math.PI * 6378137d;
    private static final int gridSize = 6;
    private float densityScaleFactor;
    private static List<?> listMeasureSelect;

    Context context;

    static int zoom = 0;

    // Per inizio misurazione
    public GridTileProvider(Context context, List<?> listMeasureSelect) {
        this.context = context;
        densityScaleFactor = context.getResources().getDisplayMetrics().density * 0.6f;
        bitmap = Bitmap.createBitmap((int) (sizeTile * densityScaleFactor),
                (int) (sizeTile * densityScaleFactor), android.graphics.Bitmap.Config.ARGB_8888);
        this.listMeasureSelect = listMeasureSelect;
    }


    @Override
    public Tile getTile(int x, int y, int zoom) {
        Bitmap coordTile;
        coordTile = drawGridTile(x, y, zoom);

        this.zoom = zoom;

        if (coordTile != null) {
            return new Tile((int) (sizeTile), (int) (sizeTile), toByteArray(coordTile));
        }

        return NO_TILE;
    }

    private byte[] toByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public static boolean checkEmptyArea(Location location, List<?> lista) {

        if (location != null) {

            double convertedLat = latitudineInMeters(location.getLatitude());
            double convertedLng = longitudineInMeters(location.getLongitude());
            TileMap locationTile = GridTileProvider.getSubtile(convertedLng, convertedLat, 10, 0);


            for (Object object : lista) {
                if (object instanceof LTE) {

                    LTE lteItem = (LTE) object; // Cast esplicito a LTE
                    TileMap actualLteTile = GridTileProvider.getSubtile(lteItem.getLongitudine(), lteItem.getLatitudine(), 10, 0);
                    if (locationTile.equals(actualLteTile)) {
                        return false;
                    }
                } else if (object instanceof WiFi) {
                    WiFi wifiItem = (WiFi) object;
                    TileMap actualWifiTile = GridTileProvider.getSubtile(wifiItem.getLongitudine(), wifiItem.getLatitudine(), 10, 0);

                    if (actualWifiTile.equals(locationTile)) {
                        return false;
                    }
                } else if (object instanceof Noise) {
                    Noise noiseItem = (Noise) object;
                    TileMap actualNoiseTile = GridTileProvider.getSubtile(noiseItem.getLongitudine(), noiseItem.getLatitudine(), 10, 0);

                    if (actualNoiseTile.equals(locationTile)) {
                        return false;
                    }
                }
            }

        }
        return true;
    }



    public Bitmap drawGridTile(int x, int y, int zoom) {

        Bitmap copy;
        synchronized (bitmap) {
            copy = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        }

        Canvas canvas = new Canvas(copy);
        int tileSize = (int) (sizeTile * densityScaleFactor);
        int cellSize = tileSize / gridSize;
        Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(18 * densityScaleFactor);

        // in every Tile, create a Grid [gridSize:gridSize]
        int zoomMin = 3;

        if (zoom > zoomMin) {
            for (int row = 0; row < gridSize; row++) {
                for (int col = 0; col < gridSize; col++) {

                    int color = Color.TRANSPARENT;
                    int xSubCell = x * gridSize + col;
                    int ySubCell = y * gridSize + row;

                    //tile da disegnare
                    TileMap actualTile = new TileMap(xSubCell, ySubCell, zoom, 0);

                    List<Object> cellValues = new ArrayList<>();

                    for (Object item : listMeasureSelect) { // Itera sulla lista
                        if (item instanceof LTE) { // Controlla se l'oggetto è di tipo LTE
                            LTE lteItem = (LTE) item; // Cast esplicito a LTE
                            TileMap tileFound = GridTileProvider.getSubtile(lteItem.getLongitudine(), lteItem.getLatitudine(), zoom, 0);
                            if (actualTile.equals(tileFound)) {
                                cellValues.add(lteItem);
                            }
                        } else if (item instanceof WiFi) { // Controlla se l'oggetto è di tipo WiFi
                            WiFi wifiItem = (WiFi) item; // Cast esplicito a WiFi
                            TileMap tileFound = GridTileProvider.getSubtile(wifiItem.getLongitudine(), wifiItem.getLatitudine(), zoom, 0);
                            if (actualTile.equals(tileFound)) {
                                cellValues.add(wifiItem);
                            }
                        } else if (item instanceof Noise) {
                            Noise noiseItem = (Noise) item;
                            TileMap tileFound = GridTileProvider.getSubtile(noiseItem.getLongitudine(), noiseItem.getLatitudine(), zoom, 0);
                            if (actualTile.equals(tileFound)) {
                                cellValues.add(noiseItem);
                            }
                        }

                    }

                    SharedPreferences preferences = context.getSharedPreferences("PrefAvg", MODE_PRIVATE);
                    int val = preferences.getInt("averageValue", 5);

                    // Limita la lista agli ultimi 5 valori
                    if (cellValues.size() > val) {
                        cellValues = cellValues.subList(cellValues.size() - val, cellValues.size());
                        color = calculateAverage(cellValues);
                    } else if(!cellValues.isEmpty()){
                        color = calculateAverage(cellValues);
                    }


                    int cellLeft = col * cellSize;
                    int cellTop = row * cellSize;
                    int cellRight = cellLeft + cellSize;
                    int cellBottom = cellTop + cellSize;
                    Rect cellRect = new Rect(cellLeft, cellTop, cellRight, cellBottom);

                    Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    if (color == Color.TRANSPARENT) {
                        cellPaint.setStyle(Paint.Style.FILL);
                        cellPaint.setARGB(40, Color.red(color), Color.green(color), Color.blue(color));
                    } else {
                        cellPaint.setStyle(Paint.Style.FILL);
                        cellPaint.setARGB(80, Color.red(color), Color.green(color), Color.blue(color));
                    }
                    cellPaint.setStrokeWidth(0.5F);
                    canvas.drawRect(cellRect, cellPaint);

                }
            }
        }
        return copy;
    }


    // Metodo per calcolare la media di una lista di valori
    private int calculateAverage(List<Object> values) {
        if (values.isEmpty()) {
            return Color.TRANSPARENT; // Ritorna un valore di default se la lista è vuota
        }

        // Calcola la media
        double sum = 0;
        for (Object value : values) {
            if(value instanceof LTE){
                sum += LteSignalManager.getLteColor(((LTE) value).getLteValue());
            } else if(value instanceof WiFi){
                sum += WifiSignalManager.getWifiColor(((WiFi) value).getWiFiValue());
            } else if(value instanceof Noise){
                sum += NoiseSignalManager.getNoiseColor(((Noise) value).getNoiseValue());
            }
        }
        double average = sum / values.size();

        // Restituisci il colore basato sulla media
        return (int) Math.round(average);
    }


    public String checkTimeArea(Location currentLocation, long time){

        Date date = new Date();

        double convertedLat = latitudineInMeters(currentLocation.getLatitude());
        double convertedLng = longitudineInMeters(currentLocation.getLongitude());
        TileMap myTile = GridTileProvider.getSubtile(convertedLng, convertedLat, zoom, 0);

        for (Object item : listMeasureSelect) { // Itera sulla lista

            if (item instanceof LTE) { // Controlla se l'oggetto è di tipo LTE
                LTE lteItem = (LTE) item; // Cast esplicito a LTE
                TileMap tileFound = GridTileProvider.getSubtile(lteItem.getLongitudine(), lteItem.getLatitudine(), zoom, 0);
                if (myTile.equals(tileFound)) {
                    if((date.getTime() - lteItem.getDate()) < time){
                        return formatRemainingTime(time, date.getTime() - ((LTE) item).getDate());
                    }
                }
            }

            else if (item instanceof WiFi) { // Controlla se l'oggetto è di tipo WiFi

                WiFi wifiItem = (WiFi) item; // Cast esplicito a WiFi
                TileMap tileFound = GridTileProvider.getSubtile(wifiItem.getLongitudine(), wifiItem.getLatitudine(), zoom, 0);

                if (myTile.equals(tileFound)) {
                    if((date.getTime() - wifiItem.getDate()) < time){
                        return formatRemainingTime(time, date.getTime() - ((WiFi) item).getDate());
                    }
                }
            }

            else if (item instanceof Noise) {
                Noise noiseItem = (Noise) item;
                TileMap tileFound = GridTileProvider.getSubtile(noiseItem.getLongitudine(), noiseItem.getLatitudine(), zoom, 0);
                if (myTile.equals(tileFound)) {
                    if((date.getTime() - noiseItem.getDate()) < time){
                        return formatRemainingTime(time, date.getTime() - ((Noise) item).getDate());
                    }
                }
            }
        }

        return String.valueOf(R.string.measurement_error);
    }

    private String formatRemainingTime(long thresholdTime, long timeDifference) {

        long remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(thresholdTime - timeDifference);
        long remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(thresholdTime - timeDifference) % 60;

        if (remainingMinutes > 0) {
            return context.getResources().getString(R.string.waiting) + remainingMinutes + context.getResources().getString(R.string.minutes) + remainingSeconds + context.getResources().getString(R.string.seconds);
        } else if (remainingMinutes <= 0){
            return context.getResources().getString(R.string.waiting) + remainingSeconds + context.getResources().getString(R.string.seconds);
        }
        return context.getResources().getString(R.string.waiting) + remainingSeconds + context.getResources().getString(R.string.seconds);

    }


    public static List<?> getListMapTouch(double latitudine, double longitudine){

        TileMap myTile = GridTileProvider.getSubtile(longitudine, latitudine, zoom, 0);

        List<Object> listMapTouch = new ArrayList<>();
        for (Object item : listMeasureSelect) { // Itera sulla lista
            if (item instanceof LTE) { // Controlla se l'oggetto è di tipo LTE

                LTE lteItem = (LTE) item; // Cast esplicito a LTE
                TileMap tileFound = GridTileProvider.getSubtile(lteItem.getLongitudine(), lteItem.getLatitudine(), zoom, 0);

                if (myTile.equals(tileFound)) {
                    listMapTouch.add(lteItem);
                }
            }

            else if (item instanceof WiFi) { // Controlla se l'oggetto è di tipo WiFi

                WiFi wifiItem = (WiFi) item; // Cast esplicito a WiFi
                TileMap tileFound = GridTileProvider.getSubtile(wifiItem.getLongitudine(), wifiItem.getLatitudine(), zoom, 0);

                if (myTile.equals(tileFound)) {
                    listMapTouch.add(wifiItem);
                }
            }

            else if (item instanceof Noise) {
                Noise noiseItem = (Noise) item;
                TileMap tileFound = GridTileProvider.getSubtile(noiseItem.getLongitudine(), noiseItem.getLatitudine(), zoom, 0);
                if (myTile.equals(tileFound)) {
                    listMapTouch.add(noiseItem);
                }
            }
        }

        return listMapTouch;
    }

    public static TileMap getSubtile(double pointX, double pointY, int zoomLevel, long date) {
        double tileDim = MapSize / Math.pow(2d, zoomLevel);
        tileDim = tileDim / gridSize;
        int tileX = (int) ((pointX - MapCoordinates[0]) / tileDim);
        int tileY = (int) ((MapCoordinates[1] - pointY) / tileDim);
        return new TileMap(tileX, tileY, zoomLevel, date);
    }

    public static double latitudineInMeters(double latitude) {
        if (latitude < 0) {
            return -latitudineInMeters(-latitude);
        }
        return (Math.log(Math.tan((90d + latitude) * Math.PI / 360d)) / (Math.PI / 180d)) * OriginShift / 180d;
    }

    public static double longitudineInMeters(double longitude) {
        return longitude * OriginShift / 180.0;
    }

}
