package com.example.labappmobili;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.Objects;

public class GridTileProvider implements TileProvider {

    private static final int TILE_SIZE_DP = 256;
    private final Bitmap borderTile;

    private static final double[] TILES_ORIGIN = {-20037508.34789244, 20037508.34789244};
    // Size of square world map in meters, using WebMerc projection.
    private static final double MAP_SIZE = 20037508.34789244 * 2;
    private static final double ORIGIN_SHIFT = Math.PI * 6378137d;
    private static final int gridSize = 6;
    private final float scaleFactor;

    Location location;

    public GridTileProvider(Context context, Location location) {
        scaleFactor = context.getResources().getDisplayMetrics().density * 0.6f;
        borderTile = Bitmap.createBitmap((int) (TILE_SIZE_DP * scaleFactor),
                (int) (TILE_SIZE_DP * scaleFactor), android.graphics.Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(borderTile);
        this.location = location;
    }

    @Override
    public Tile getTile(int x, int y, int zoom) {
        Bitmap coordTile = drawGridTile(x, y, zoom);

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

    public Bitmap drawGridTile(int x, int y, int zoom) {
        // Synchronize copying the bitmap to avoid a race condition in some devices.
        Bitmap copy;
        synchronized (borderTile) {
            copy = borderTile.copy(Bitmap.Config.ARGB_8888, true);
        }
        int alpha = 120; // transparency
        Canvas canvas = new Canvas(copy);
        int color;

        int tileSize = (int) (TILE_SIZE_DP * scaleFactor);
        int cellSize = tileSize / gridSize;
        Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(18 * scaleFactor);

        // in every Tile, create a Grid [gridSize:gridSize]
        int zoomMin = 5;
        if (zoom > zoomMin) {
            for (int row = 0; row < gridSize; row++) {
                for (int col = 0; col < gridSize; col++) {
                    // (x, y) of the subCell
                    int xSubCell = x * gridSize + col;
                    int ySubCell = y * gridSize + row;

                    TileDataInfo actualTile = new TileDataInfo(xSubCell, ySubCell, zoom);

                    Log.d("actualTile" , "actual : "+ actualTile);
                    Log.d("actualTile", "xsub : " + xSubCell + " ysub  : " + ySubCell);
                    // Check if the current Tile corresponds to the user's current location
                    if (location != null) {
                        double convertedLat = inMetersLatCoordinate(location.getLatitude());
                        double convertedLng = inMetersLngCoordinate(location.getLongitude());


                        TileDataInfo tileFound = GridTileProvider.getSubTileByCoordinate(convertedLng, convertedLat, zoom);

                            if(actualTile.equals(tileFound)) {
                                //Log.d("LAST DATE TIME", String.valueOf(tile.getLastDateTime()));
                                color = Color.GREEN;
                            }else {
                                color = Color.TRANSPARENT;
                            }


                        Log.d("actualTile" , "userLatLong : "+ tileFound + " : " + convertedLng  + "   \n subCeLLLAtLong : ");



                    } else {
                        color = Color.TRANSPARENT;
                    }

                    // draw the Sub-Rectangle
                    int cellLeft = col * cellSize;
                    int cellTop = row * cellSize;
                    int cellRight = cellLeft + cellSize;
                    int cellBottom = cellTop + cellSize;
                    Rect cellRect = new Rect(cellLeft, cellTop, cellRight, cellBottom);

                    Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    if (color == Color.TRANSPARENT) {
                        cellPaint.setStyle(Paint.Style.STROKE);
                        cellPaint.setColor(Color.GRAY);
                    } else {
                        cellPaint.setStyle(Paint.Style.FILL);
                        cellPaint.setARGB(alpha, Color.red(color), Color.green(color), Color.blue(color));
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
        Log.d("TILE DIM", tileDim + " metri");

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


    public static class TileDataInfo {
        int tileX;
        int tileY;
        int tileZoom;
        Date lastDateTime;

        public TileDataInfo(int tileX, int tileY, int tileZoom) {
            this.tileX = tileX;
            this.tileY = tileY;
            this.tileZoom = tileZoom;

        }

        public int getTileX() {
            return tileX;
        }

        public int getTileY() {
            return tileY;
        }

        public int getTileZoom() {
            return tileZoom;
        }

        public Date getLastDateTime() {
            return lastDateTime;
        }

        @NonNull
        @Override
        public String toString() {
            return "TileDataInfo{" +
                    "tileX=" + tileX +
                    ", tileY=" + tileY +
                    ", tileZoom=" + tileZoom +
                    '}';
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TileDataInfo that = (TileDataInfo) o;
            return tileX == that.tileX && tileY == that.tileY && tileZoom == that.tileZoom;
        }


        @Override
        public int hashCode() {
            return Objects.hash(tileX, tileY, tileZoom);
        }
    }

}
