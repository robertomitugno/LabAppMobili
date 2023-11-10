package com.example.labappmobili.RoomDB.WiFi;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface WiFiDao {

    @Query("SELECT * FROM WifiDatabase")
    List<WiFi> getAllWifi();

    @Insert
    void insertWiFi(WiFi wifi);

    @Query("DELETE FROM WifiDatabase")
    void deleteAllWifi();

    @Query("UPDATE WifiDatabase SET latitudine=:latitudine , longitudine =:longitudine WHERE id =:id")
    void updateLTE(String latitudine, String longitudine, int id);
}
