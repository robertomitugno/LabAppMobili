package com.example.labappmobili.RoomDB.LTE;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity (tableName = "LteDB")
public class LTE {

    @ColumnInfo (name = "Latitudine")
    private double Latitudine;

    @ColumnInfo (name = "Id")
    @PrimaryKey (autoGenerate = true)
    private int Id;

    @ColumnInfo (name = "Longitudine")
    private double Longitudine;

    @ColumnInfo (name = "LteValue")
    private int LteValue;

    @ColumnInfo (name = "Date")
    private long Date;

    public LTE(double Latitudine, int Id, double Longitudine, int LteValue, long Date) {
        this.Latitudine = Latitudine;
        this.Id = Id;
        this.Longitudine = Longitudine;
        this.LteValue = LteValue;
        this.Date = Date;
    }

    @Ignore
    public LTE(double Latitudine, double Longitudine, int LteValue, long Date) {
        this.Latitudine = Latitudine;
        this.Longitudine = Longitudine;
        this.LteValue = LteValue;
        this.Date = Date;
    }

    public int getLteValue() {
        return LteValue;
    }

    public double getLatitudine() {
        return Latitudine;
    }

    @Override
    public String toString() {
        return "LTE{" +
                "Latitudine=" + Latitudine +
                ", Id=" + Id +
                ", Longitudine=" + Longitudine +
                ", LteValue=" + LteValue +
                '}';
    }

    public int getId() {
        return Id;
    }

    public long getDate() {
        return Date;
    }

    public double getLongitudine() {
        return Longitudine;
    }


}
