package com.example.labappmobili.RoomDB.Noise;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity (tableName = "NoiseDB")
public class Noise {

    @ColumnInfo (name = "Latitudine")
    private double latitudine;

    @ColumnInfo (name = "Id")
    @PrimaryKey (autoGenerate = true)
    private int id;

    @ColumnInfo (name = "Longitudine")
    private double longitudine;

    @ColumnInfo (name = "NoiseValue")
    private double noiseValue;

    @ColumnInfo (name = "Date")
    private long Date;


    public Noise(double latitudine, int id, double longitudine, double noiseValue, long Date) {
        this.latitudine = latitudine;
        this.id = id;
        this.longitudine = longitudine;
        this.noiseValue = noiseValue;
        this.Date = Date;
    }

    @Ignore
    public Noise(double latitudine, double longitudine, double noiseValue, long Date) {
        this.latitudine = latitudine;
        this.longitudine = longitudine;
        this.noiseValue = noiseValue;
        this.Date = Date;
    }

    public double getLatitudine() {
        return latitudine;
    }

    public int getId() {
        return id;
    }

    public double getLongitudine() {
        return longitudine;
    }

    public double getNoiseValue() {
        return noiseValue;
    }

    public long getDate() {
        return Date;
    }

    @Override
    public String toString() {
        return "Noise{" +
                "latitudine=" + latitudine +
                ", id=" + id +
                ", longitudine=" + longitudine +
                ", noiseValue=" + noiseValue +
                ", Date=" + Date +
                '}';
    }
}
