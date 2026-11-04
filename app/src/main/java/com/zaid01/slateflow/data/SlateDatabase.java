package com.zaid01.slateflow.data;

import android.database.Cursor;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Document.class}, version = 2, exportSchema = false)
public abstract class SlateDatabase extends RoomDatabase {
    public abstract DocumentDao documents();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE documents ADD COLUMN documentKey TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE documents ADD COLUMN sourceUri TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE documents ADD COLUMN folderName TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE documents ADD COLUMN folderUri TEXT NOT NULL DEFAULT ''");
            try (Cursor cursor = db.query("SELECT uri FROM documents")) {
                while (cursor.moveToNext()) {
                    String uri = cursor.getString(0);
                    db.execSQL("UPDATE documents SET documentKey = ?, sourceUri = ? WHERE uri = ?",
                        new Object[] {DocumentIdentity.key(Uri.parse(uri)), uri, uri});
                }
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS index_documents_documentKey ON documents(documentKey)");
        }
    };
}
