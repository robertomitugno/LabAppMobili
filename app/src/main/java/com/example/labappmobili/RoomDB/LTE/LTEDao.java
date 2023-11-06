package com.example.labappmobili.RoomDB.LTE;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LTEDao {

    @Query("Select * from LteDB")
    List<LTE> getAllLte();

    @Query("SELECT * FROM LteDB ORDER BY Id DESC LIMIT 5")
    List<LTE> getLastFiveLte();

    @Query("SELECT * FROM LteDB ORDER BY Id DESC LIMIT 1")
    LTE getLastLte();

    @Insert
    void insertLTE(LTE lte);

    @Delete
    void deleteLTE(LTE lte);

    @Query("UPDATE LteDB SET Latitudine=:Latitudine , Longitudine =:Longitudine WHERE Id =:id")
    void updateLTE(String Latitudine, String Longitudine, int id);
}
