package com.example.labappmobili.RoomDB.LTE;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {LTE.class}, exportSchema = false, version = 1)
public abstract class LTEDB extends RoomDatabase {

    public abstract LTEDao getLTEDao();

}
