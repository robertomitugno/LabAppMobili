package com.example.labappmobili.RoomDB.Noise;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.labappmobili.RoomDB.WiFi.WiFi;
import com.example.labappmobili.RoomDB.WiFi.WiFiDao;

@Database(entities = {Noise.class}, exportSchema = false, version = 1)
public abstract class NoiseDB extends RoomDatabase {

    public abstract NoiseDao getNoiseDao();

}
