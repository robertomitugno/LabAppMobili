package com.example.labappmobili;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.Objects;

public class TileDataInfo {
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
