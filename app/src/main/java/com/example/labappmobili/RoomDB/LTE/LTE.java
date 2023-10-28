package com.example.labappmobili.RoomDB.LTE;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity (tableName = "LteDB")
public class LTE {

    @ColumnInfo (name = "Latitudine")
    private double latitudine;

    @ColumnInfo (name = "Id")
    @PrimaryKey (autoGenerate = true)
    private int id;

    @ColumnInfo (name = "Longitudine")
    private double longitudine;

    @ColumnInfo (name = "LteValue")
    private int LteValue;

    public LTE(double latitudine, int id, double longitudine, int LteValue) {
        this.latitudine = latitudine;
        this.id = id;
        this.longitudine = longitudine;
        this.LteValue = LteValue;
    }

    @Ignore
    public LTE(double latitudine, double longitudine, int LteValue) {
        this.latitudine = latitudine;
        this.longitudine = longitudine;
        this.LteValue = LteValue;
    }

    public int getLteValue() {
        return LteValue;
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

    @Override
    public String toString() {
        return "LTE{" +
                "latitudine=" + latitudine +
                ", longitudine=" + longitudine +
                ", LteValue=" + LteValue +
                '}';
    }
}
