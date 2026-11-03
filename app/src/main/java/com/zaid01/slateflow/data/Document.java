package com.zaid01.slateflow.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "documents")
public class Document {
    @PrimaryKey @NonNull public String uri = "";
    @NonNull public String name = "";
    @NonNull public String kind = "md";
    public long openedAt;
    public int anchor;
    public int offset;
    public float fraction;
    public float progress;
}
