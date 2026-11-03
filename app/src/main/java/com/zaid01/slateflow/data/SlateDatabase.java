package com.zaid01.slateflow.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Document.class}, version = 1, exportSchema = false)
public abstract class SlateDatabase extends RoomDatabase {
    public abstract DocumentDao documents();
}
