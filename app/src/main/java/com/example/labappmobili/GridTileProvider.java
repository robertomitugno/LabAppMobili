package com.example.labappmobili;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

import java.io.ByteArrayOutputStream;

public class GridTileProvider implements TileProvider {
    private static final int TILE_SIZE_DP = 256;

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

    private Bitmap drawGridTile(int x, int y, int zoom) {
        int tileSize = (int) TILE_SIZE_DP;
        Bitmap bitmap = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        for (int i = 0; i < 4; i++) {
            int rectSize = tileSize / 4;
            int rectX = i * rectSize;
            int rectY = i * rectSize;

            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);

            canvas.drawRect(rectX, 0, rectX, tileSize, paint);
            canvas.drawRect(0, rectY, tileSize, rectY, paint);
        }
        return bitmap;
    }

}