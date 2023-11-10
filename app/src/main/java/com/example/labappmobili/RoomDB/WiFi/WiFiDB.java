package com.example.labappmobili.RoomDB.WiFi;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {WiFi.class}, exportSchema = false, version = 1)
public abstract class WiFiDB extends RoomDatabase {

    public abstract WiFiDao getWiFiDao();

}
