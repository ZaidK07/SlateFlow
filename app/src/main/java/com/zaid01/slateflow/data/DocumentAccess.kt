package com.zaid01.slateflow.data

import android.content.ContentResolver
import android.net.Uri
import java.io.FileNotFoundException

internal fun ContentResolver.requireReadable(uri: Uri) {
    val descriptor = openFileDescriptor(uri, "r") ?: throw FileNotFoundException()
    descriptor.close()
}

data class UnavailableDocument(val uri: String, val name: String)
