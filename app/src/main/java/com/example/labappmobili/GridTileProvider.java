package com.example.labappmobili;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.Location;
import android.util.Log;

import com.example.labappmobili.RoomDB.LTE.LTE;
import com.example.labappmobili.RoomDB.Noise.Noise;
import com.example.labappmobili.RoomDB.WiFi.WiFi;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class GridTileProvider implements TileProvider {

    private static final int TILE_SIZE_DP = 256;
    private Bitmap borderTile;

    private static final double[] TILES_ORIGIN = {-20037508.34789244, 20037508.34789244};
    // Size of square world map in meters, using WebMerc projection.
    private static final double MAP_SIZE = 20037508.34789244 * 2;
    private static final double ORIGIN_SHIFT = Math.PI * 6378137d;
    private static final int gridSize = 6;
    private float scaleFactor;
    private final List<?> lista;
    private int color = 0;
    private boolean startMeasuramentBackground = false;
    Location location;

    Context context;

    // Per inizio misurazione
    public GridTileProvider(Context context, Location location, List<?> lista) {
        this.context = context;
        this.location = location;
        this.startMeasuramentBackground = true;
        this.lista = lista;
    }

    public GridTileProvider(Context context, List<?> lista) {
        this.context = context;
        scaleFactor = context.getResources().getDisplayMetrics().density * 0.6f;
        borderTile = Bitmap.createBitmap((int) (TILE_SIZE_DP * scaleFactor),
                (int) (TILE_SIZE_DP * scaleFactor), android.graphics.Bitmap.Config.ARGB_8888);
        this.lista = lista;
    }

    @Override
    public Tile getTile(int x, int y, int zoom) {
        Bitmap coordTile = null;
            coordTile = drawGridTile(x, y, zoom);

        if (coordTile != null) {
            return new Tile((int) (TILE_SIZE_DP), (int) (TILE_SIZE_DP), toByteArray(coordTile));
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

            double convertedLat = inMetersLatCoordinate(location.getLatitude());
            double convertedLng = inMetersLngCoordinate(location.getLongitude());
            TileDataInfo locationTile = GridTileProvider.getSubTileByCoordinate(convertedLng, convertedLat, 12);


            for (Object object : lista) {

                if (object instanceof LTE) {
                    LTE lteItem = (LTE) object; // Cast esplicito a LTE
                    TileDataInfo actualLteTile = GridTileProvider.getSubTileByCoordinate(lteItem.getLongitudine(), lteItem.getLatitudine(), 12);

                    if (actualLteTile.equals(locationTile)) {
                        return false;
                    }
                } else if (object instanceof WiFi) {
                    WiFi wifiItem = (WiFi) object;
                    TileDataInfo actualWifiTile = GridTileProvider.getSubTileByCoordinate(wifiItem.getLongitudine(), wifiItem.getLatitudine(), 12);

                    if (actualWifiTile.equals(locationTile)) {
                        return false;
                    }
                } else if (object instanceof Noise) {
                    Noise noiseItem = (Noise) object;
                    TileDataInfo actualNoiseTile = GridTileProvider.getSubTileByCoordinate(noiseItem.getLongitudine(), noiseItem.getLatitudine(), 12);

                    if (actualNoiseTile.equals(locationTile)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public Bitmap drawGridTile(int x, int y, int zoom) {
        // Synchronize copying the bitmap to avoid a race condition in some devices.
        Bitmap copy;
        synchronized (borderTile) {
            copy = borderTile.copy(Bitmap.Config.ARGB_8888, true);
        }

        Canvas canvas = new Canvas(copy);
        int tileSize = (int) (TILE_SIZE_DP * scaleFactor);
        int cellSize = tileSize / gridSize;
        Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(18 * scaleFactor);

        // in every Tile, create a Grid [gridSize:gridSize]
        int zoomMin = 3;
        if (zoom > zoomMin) {
            for (int row = 0; row < gridSize; row++) {
                for (int col = 0; col < gridSize; col++) {
                    int color = Color.TRANSPARENT;
                    int xSubCell = x * gridSize + col;
                    int ySubCell = y * gridSize + row;
                    TileDataInfo actualTile = new TileDataInfo(xSubCell, ySubCell, zoom);

                    for (Object item : lista) { // Itera sulla lista
                        if (item instanceof LTE) { // Controlla se l'oggetto è di tipo LTE
                            LTE lteItem = (LTE) item; // Cast esplicito a LTE
                            TileDataInfo tileFound = GridTileProvider.getSubTileByCoordinate(lteItem.getLongitudine(), lteItem.getLatitudine(), zoom);
                            if (actualTile.equals(tileFound)) {
                                color = LteSignalManager.getLteColor(lteItem.getLteValue());
                                break;
                            }
                        } else if (item instanceof WiFi) { // Controlla se l'oggetto è di tipo WiFi
                            WiFi wifiItem = (WiFi) item; // Cast esplicito a WiFi
                            TileDataInfo tileFound = GridTileProvider.getSubTileByCoordinate(wifiItem.getLongitudine(), wifiItem.getLatitudine(), zoom);
                            if (actualTile.equals(tileFound)) {
                                color = WifiSignalManager.getWifiColor(wifiItem.getWiFiValue());
                                break;
                            }
                        } else if (item instanceof Noise) {
                            Noise noiseItem = (Noise) item;
                            TileDataInfo tileFound = GridTileProvider.getSubTileByCoordinate(noiseItem.getLongitudine(), noiseItem.getLatitudine(), zoom);
                            if (actualTile.equals(tileFound)) {
                                color = NoiseSignalManager.getNoiseColor(noiseItem.getNoiseValue());
                                break;
                            }
                        }
                    }

                    int cellLeft = col * cellSize;
                    int cellTop = row * cellSize;
                    int cellRight = cellLeft + cellSize;
                    int cellBottom = cellTop + cellSize;
                    Rect cellRect = new Rect(cellLeft, cellTop, cellRight, cellBottom);

                    Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    if (color == Color.TRANSPARENT) {
                        cellPaint.setStyle(Paint.Style.FILL);
                        cellPaint.setARGB(20, Color.red(color), Color.green(color), Color.blue(color));
                    } else {
                        cellPaint.setStyle(Paint.Style.FILL);
                        cellPaint.setARGB(50, Color.red(color), Color.green(color), Color.blue(color));
                    }
                    cellPaint.setStrokeWidth(0.5F);
                    canvas.drawRect(cellRect, cellPaint);
                }
            }
        }
        return copy;
    }

    public static TileDataInfo getSubTileByCoordinate(double pointX, double pointY, int zoomLevel) {
        double tileDim = MAP_SIZE / Math.pow(2d, zoomLevel);
        tileDim = tileDim / gridSize;
        int tileX = (int) ((pointX - TILES_ORIGIN[0]) / tileDim);
        int tileY = (int) ((TILES_ORIGIN[1] - pointY) / tileDim);
        return new TileDataInfo(tileX, tileY, zoomLevel);
    }

    public static double inMetersLatCoordinate(double latitude) {
        if (latitude < 0) {
            return -inMetersLatCoordinate(-latitude);
        }
        return (Math.log(Math.tan((90d + latitude) * Math.PI / 360d)) / (Math.PI / 180d)) * ORIGIN_SHIFT / 180d;
    }

    public static double inMetersLngCoordinate(double longitude) {
        return longitude * ORIGIN_SHIFT / 180.0;
    }
}
