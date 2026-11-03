# SlateFlow

A local PDF and Markdown reader for Android 14+, built with Kotlin and Jetpack Compose. Designed for phones and tablets, with dark grey surfaces, rounded controls, and Signature Blue or Android wallpaper accents.

## Reading

- Open PDFs, `.md`, `.markdown`, and plain-text documents through Android’s file picker. Persisted URI permissions let the app reopen files without broad storage access.
- The library lists recent documents and reading progress. Wider windows use a sidebar and card grid; expanded landscape PDF readers show two pages per row; portrait readers keep a single column.
- A gear icon opens a dedicated Settings page for color accents, Markdown margins, text size, and line spacing. Settings persist in DataStore.
- Markdown supports headings, lists, links, code blocks, tables, and strikethrough. Parsing runs off the main thread. Embedded remote and relative images are not loaded in this first version. Files larger than 12 MiB are rejected to bound memory use.
- PDF rendering runs off the main thread and uses native `PdfRenderer`. ARGB bitmaps are required by the platform. Each page is capped around 4 MiB; the cache holds at most three pages and 12 MiB. Live UI bitmaps are never manually recycled.

## Reading memory

Room stores a position for each document URI. Markdown positions use a character anchor and fraction within the line; PDF positions use a page index and fraction within the visible row. This makes restoration more stable when window sizes or typography change.

The writer saves during continuous scrolling at intervals of at least 250 ms, after 150 ms of inactivity, and on pause or navigation. Writes are serialized; switching documents waits for queued saves before reading their stored state. The last active document reopens on launch.

Abrupt process termination or power loss can discard a position that has not reached disk. No lifecycle callback can guarantee a final save after termination. A moved, deleted, edited, or inaccessible document may require reselection or yield a different restored position. Different picker URIs for the same file are tracked separately.

## Build and verification

Open the project in Android Studio using its configured SDK and JDK, or run:

```sh
./gradlew :app:assembleDebug :app:assembleRelease :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

The device tests cover database persistence, independent per-document positions, bounded PDF bitmap sizes and cache eviction, native Markdown anchor restoration through initial layout and typography reflow, and Settings navigation. Release builds enable R8 and resource shrinking; release signing is not configured.

Performance and exact restoration still need validation on the intended 4 GB tablet, including very large documents, rotation, and fast continuous scrolling. A device-generated Baseline Profile and Macrobenchmark measurements remain follow-up work; no fixed frame-rate guarantee is assumed.
