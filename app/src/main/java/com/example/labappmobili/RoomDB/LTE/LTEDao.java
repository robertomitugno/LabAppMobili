package com.example.labappmobili.RoomDB.LTE;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LTEDao {

    @Query("SELECT * FROM lteDB")
    List<LTE> getAllLte();

    @Insert
    void insertLTE(LTE lte);

    @Delete
    void deleteLTE(LTE lte);

    @Query("UPDATE lteDB SET latitudine=:latitudine , longitudine =:longitudine WHERE id =:id")
    void updateLTE(String latitudine, String longitudine, int id);
}
