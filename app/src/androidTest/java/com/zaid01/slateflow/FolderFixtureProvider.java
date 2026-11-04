package com.zaid01.slateflow;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.net.Uri;
import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Java keeps this test-only provider independent of the target APK's Kotlin runtime. */
public final class FolderFixtureProvider extends ContentProvider {
    private static final String[] COLUMNS = {"document_id", "_display_name", "mime_type", "last_modified", "_size", "flags"};
    private final Map<String, Object[]> entries = new LinkedHashMap<>();

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] arguments, String order) {
        try {
            String id = DocumentsContract.getDocumentId(uri);
            return "children".equals(uri.getLastPathSegment())
                ? queryChildDocuments(id, projection, order)
                : queryDocument(id, projection);
        } catch (FileNotFoundException exception) {
            throw new IllegalArgumentException(exception);
        }
    }
    @Override public String getType(Uri uri) { return "application/octet-stream"; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }

    @Override public boolean onCreate() {
        String directory = DocumentsContract.Document.MIME_TYPE_DIR;
        entries.put("root", new Object[]{"root", "Reading", directory, 0L, 0L, 0});
        entries.put("notes", new Object[]{"notes", "Notes", directory, 0L, 0L, 0});
        entries.put("empty", new Object[]{"empty", "Empty", directory, 0L, 0L, 0});
        entries.put("pdf", new Object[]{"pdf", "Guide.PDF", "application/pdf", 0L, 200L, 0});
        entries.put("md", new Object[]{"md", "Readme.md", "application/octet-stream", 0L, 120L, 0});
        entries.put("image", new Object[]{"image", "Photo.jpg", "image/jpeg", 0L, 500L, 0});
        entries.put("nested", new Object[]{"nested", "Chapter.markdown", "text/markdown", 0L, 80L, 0});
        return true;
    }

    public Cursor queryRoots(String[] projection) {
        MatrixCursor cursor = new MatrixCursor(new String[]{"root_id", "document_id", "title", "flags"});
        cursor.addRow(new Object[]{"fixture", "root", "SlateFlow test folder", 0});
        return cursor;
    }

    private Cursor rows(String[] ids, String[] projection) throws FileNotFoundException {
        String[] requested = projection == null ? COLUMNS : projection;
        MatrixCursor cursor = new MatrixCursor(requested);
        for (String id : ids) {
            Object[] entry = entries.get(id);
            if (entry == null) throw new FileNotFoundException(id);
            Object[] row = new Object[requested.length];
            for (int index = 0; index < requested.length; index++) {
                for (int column = 0; column < COLUMNS.length; column++) {
                    if (COLUMNS[column].equals(requested[index])) row[index] = entry[column];
                }
            }
            cursor.addRow(row);
        }
        return cursor;
    }

    public Cursor queryDocument(String id, String[] projection) throws FileNotFoundException {
        return rows(new String[]{id}, projection);
    }

    public Cursor queryChildDocuments(String parent, String[] projection, String order) throws FileNotFoundException {
        switch (parent) {
            case "root": return rows(new String[]{"notes", "empty", "pdf", "md", "image"}, projection);
            case "notes": return rows(new String[]{"nested"}, projection);
            case "empty": return rows(new String[]{}, projection);
            default: throw new FileNotFoundException(parent);
        }
    }

    public boolean isChildDocument(String parent, String child) {
        return parent.equals(child)
            || (parent.equals("root") && entries.containsKey(child))
            || (parent.equals("notes") && child.equals("nested"));
    }

    public ParcelFileDescriptor openDocument(String id, String mode, CancellationSignal signal) throws FileNotFoundException {
        throw new FileNotFoundException("Metadata-only fixture");
    }
}
