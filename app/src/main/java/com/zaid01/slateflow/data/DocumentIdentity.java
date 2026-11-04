package com.zaid01.slateflow.data;

import android.net.Uri;
import android.provider.DocumentsContract;

/** One provider document can be opened through a single-file grant or a tree grant. */
public final class DocumentIdentity {
    private DocumentIdentity() {}
    public static String key(Uri uri) {
        try {
            return uri.getAuthority() + ":" + DocumentsContract.getDocumentId(uri);
        } catch (IllegalArgumentException exception) {
            return uri.toString();
        }
    }
}
