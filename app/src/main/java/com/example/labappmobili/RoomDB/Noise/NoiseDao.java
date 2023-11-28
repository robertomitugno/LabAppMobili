package com.example.labappmobili.RoomDB.Noise;

import androidx.room.Dao;
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

    @Query("DELETE FROM NoiseDB")
    void deleteAllNoise();

}
