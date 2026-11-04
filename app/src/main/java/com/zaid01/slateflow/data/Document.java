package com.zaid01.slateflow.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Index;
import androidx.room.ColumnInfo;

@Entity(tableName = "documents", indices = {@Index("documentKey")})
public class Document {
    @PrimaryKey @NonNull public String uri = "";
    @NonNull public String name = "";
    @NonNull public String kind = "md";
    @NonNull @ColumnInfo(defaultValue = "''") public String documentKey = "";
    @NonNull @ColumnInfo(defaultValue = "''") public String sourceUri = "";
    @NonNull @ColumnInfo(defaultValue = "''") public String folderName = "";
    @NonNull @ColumnInfo(defaultValue = "''") public String folderUri = "";
    public long openedAt;
    public int anchor;
    public int offset;
    public float fraction;
    public float progress;
}
