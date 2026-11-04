# SlateFlow 1.1

A local PDF and Markdown reader for Android 14+, built with Kotlin and Jetpack Compose for phones and tablets.

## Appearance and navigation

Nexus brings cool neutral surfaces, blue details, green actions, rounded controls, and bundled Inter typography. Choose Light, Dark, or System in Settings, with optional Android wallpaper accents. The launcher icon uses the same blue, white, and green palette. Inter's license is bundled in `assets/licenses/Inter-OFL.txt`.

Phones use Dashboard and Explore bottom tabs. Wider windows use a compact navigation rail. Readers use one PDF page per row in portrait and two in expanded landscape windows.

## Dashboard

A compact welcome card and one-row Continue reading card precede the reading history. The customize control saves grouping (last read, type, folder, progress, or none), sorting (recent, name, or progress), cards/list layout, and welcome/continue visibility. File-type filtering also persists. Search matches document names and known folder paths. Identically named folders retain separate groups when their URIs differ.

## Explore

Add one or more folders using Android's folder picker. The app remembers those tree permissions and browses their PDFs, Markdown/plain-text files, and subfolders. Folder cards show counts of their immediate children, not recursive totals. Breadcrumbs and Android Back support nested navigation; search and file-type filters apply to the current directory.

Directory queries run off the main thread. Only current/visible folder summaries are loaded, with at most three summary queries at a time and a bounded cache. Refresh clears those summaries. A directory with over 20,000 items reports a limit rather than silently hiding files. Android restricts which storage roots can be selected; grant each accessible folder tree you want to browse.

Removing a root from Explore hides it without deleting files. Its URI permission is retained for documents already opened from that tree, so history can still reopen them. Revoking access through Android or moving/deleting a document may require choosing it again.

## Reading and memory

Both readers place their fading scroll indicator at the outer right edge, clear of the reading content. Markdown margins are applied to the text inside the full-width scroll view. PDF has a separate indicator gutter and a page scrubber.

Markdown supports headings, lists, links, code blocks, tables, and strikethrough. Parsing runs off the main thread. Embedded remote/relative images are not loaded. Files larger than 12 MiB are rejected to bound memory use. Native `PdfRenderer` requires ARGB bitmaps; pages are capped around 4 MiB, and the cache holds at most three pages and 12 MiB. Live UI bitmaps are never manually recycled.

Room saves character/line anchors for Markdown and page/row fractions for PDF. Saves run during continuous scrolling at intervals of at least 250 ms, after 150 ms of inactivity, and on pause/navigation. Switching documents waits for queued writes. The last active document reopens on launch.

The 1.0-to-1.1 database migration preserves positions and adds source/folder metadata. Provider document IDs match individual and tree URIs, allowing a document opened through Explore to reuse its earlier history and position. Older individually opened documents show under “Opened individually” until opened through Explore.

Abrupt termination or power loss can discard a position not yet committed to disk. Changed document content can affect anchor restoration. No lifecycle callback guarantees a final save after termination.

## Build and verification

Use Android Studio's configured SDK/JDK, or run:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

Tests cover grouping/filtering/sorting, same-name folder grouping, schema migration without position loss, tree/single URI identity, nested folder metadata, independent saved positions, bounded PDF rendering, Markdown reflow, navigation, and theme controls. Test documents are synthetic and installed only with the instrumentation APK.

Release builds enable R8/resource shrinking; release signing is not configured. The installable development APK is `app/build/outputs/apk/debug/app-debug.apk`. Version 1.1 uses version code 2 and can update the previous debug-signed 1.0 installation without clearing its data. A device-generated Baseline Profile and Macrobenchmark remain future performance work.
