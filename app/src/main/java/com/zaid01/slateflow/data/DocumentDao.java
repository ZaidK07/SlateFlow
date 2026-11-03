package com.zaid01.slateflow.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY openedAt DESC") List<Document> recent();
    @Query("SELECT * FROM documents WHERE uri = :uri") Document find(String uri);
    @Insert(onConflict = OnConflictStrategy.REPLACE) void put(Document document);
    @Query("UPDATE documents SET anchor = :anchor, offset = :offset, fraction = :fraction, progress = :progress WHERE uri = :uri")
    void savePosition(String uri, int anchor, int offset, float fraction, float progress);
}
