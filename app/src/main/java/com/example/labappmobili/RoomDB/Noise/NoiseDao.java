package com.example.labappmobili.RoomDB.Noise;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.labappmobili.RoomDB.WiFi.WiFi;

import java.util.List;

@Dao
public interface NoiseDao {

    @Query("SELECT * FROM NoiseDB")
    List<Noise> getAllNoise();

    @Insert
    void insertNoise(Noise noise);

    @Delete
    void deleteNoise(Noise noise);

    @Query("UPDATE NoiseDB SET latitudine=:latitudine , longitudine =:longitudine WHERE id =:id")
    void updateNoise(String latitudine, String longitudine, int id);
}
