package com.example.labappmobili;

import java.util.Objects;

public class TileMap {
    int tileX;
    int tileY;
    int tileZoom;
    long lastDateTime;

    public TileMap(int tileX, int tileY, int tileZoom, long lastDateTime) {
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
        TileMap that = (TileMap) o;
        return tileX == that.tileX && tileY == that.tileY && tileZoom == that.tileZoom;
    }


    @Override
    public int hashCode() {
        return Objects.hash(tileX, tileY, tileZoom);
    }
}
