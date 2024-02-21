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

    @Insert
    void insertLTE(LTE lte);

    @Query("DELETE FROM LteDB")
    void deleteAllLTE();

    @Query("DELETE FROM LteDB WHERE Id = :id")
    void deleteLTEById(int id);

    @Query("UPDATE LteDB SET Latitudine=:Latitudine , Longitudine =:Longitudine WHERE Id =:id")
    void updateLTE(String Latitudine, String Longitudine, int id);
}
