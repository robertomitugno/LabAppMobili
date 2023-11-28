package com.example.labappmobili;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.Objects;

public class TileDataInfo {
    int tileX;
    int tileY;
    int tileZoom;
    long lastDateTime;

    public TileDataInfo(int tileX, int tileY, int tileZoom, long lastDateTime) {
        this.tileX = tileX;
        this.tileY = tileY;
        this.tileZoom = tileZoom;
        this.lastDateTime = lastDateTime;
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

    public long getLastDateTime() {
        return lastDateTime;
    }


    @Override
    public String toString() {
        return "TileDataInfo{" +
                "tileX=" + tileX +
                ", tileY=" + tileY +
                ", tileZoom=" + tileZoom +
                ", lastDateTime=" + lastDateTime +
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
