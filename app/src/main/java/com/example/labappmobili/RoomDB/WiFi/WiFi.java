package com.example.labappmobili.RoomDB.WiFi;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity (tableName = "WifiDatabase")
public class WiFi {

    @ColumnInfo (name = "Latitudine")
    private double latitudine;

    @ColumnInfo (name = "Id")
    @PrimaryKey (autoGenerate = true)
    private int id;

    @ColumnInfo (name = "Longitudine")
    private double longitudine;

    @ColumnInfo (name = "WiFiValue")
    private double WiFiValue;


    public WiFi(double latitudine, int id, double longitudine, double WiFiValue) {
        this.latitudine = latitudine;
        this.id = id;
        this.longitudine = longitudine;
        this.WiFiValue = WiFiValue;
    }

    @Ignore
    public WiFi(double latitudine, double longitudine, double WiFiValue) {
        this.latitudine = latitudine;
        this.longitudine = longitudine;
        this.WiFiValue = WiFiValue;

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

    public double getWiFiValue() {
        return WiFiValue;
    }
}
