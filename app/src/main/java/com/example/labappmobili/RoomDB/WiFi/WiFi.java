package com.example.labappmobili.RoomDB.WiFi;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

    @ColumnInfo (name = "Date")
    private long Date;

    public WiFi(double latitudine, int id, double longitudine, double WiFiValue, long Date) {
        this.latitudine = latitudine;
        this.id = id;
        this.longitudine = longitudine;
        this.WiFiValue = WiFiValue;
        this.Date = Date;
    }

    @Ignore
    public WiFi(double latitudine, double longitudine, double WiFiValue, long Date) {
        this.latitudine = latitudine;
        this.longitudine = longitudine;
        this.WiFiValue = WiFiValue;
        this.Date = Date;
    }

    public long getDate() {
        return Date;
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

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
        String formattedDate = sdf.format(new Date(Date));

        return "Date: " + formattedDate + "\n" +
                "WiFi Value: " + WiFiValue;
    }

}
