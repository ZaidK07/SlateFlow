# SlateFlow 1.2

A local PDF and Markdown reader for Android 14+, built with Kotlin and Jetpack Compose for phones and tablets.

## Appearance and navigation

Nexus brings cool neutral surfaces, blue details, green actions, rounded controls, and bundled Inter typography. Choose Light, Dark, or System in Settings, with optional Android wallpaper accents. The launcher icon uses the same blue, white, and green palette. Inter's license is bundled in `assets/licenses/Inter-OFL.txt`.

Phones use Dashboard and Explore bottom tabs. Wider windows use a compact navigation rail. Readers use one PDF page per row in portrait and two in expanded landscape windows.

## Dashboard

A compact welcome card and one-row Continue reading card precede the reading history. The customize control saves grouping (last read, type, folder, progress, or none), sorting (recent, name, or progress), cards/list layout, and welcome/continue visibility. File-type filtering also persists. Search matches document names and known folder paths. Identically named folders retain separate groups when their URIs differ.

## Explore

Add one or more scan locations using Android's folder picker. Explore scans every nested directory within those grants once when added and presents a flat list of folders that directly contain PDFs or Markdown/plain-text documents. Empty/intermediate-only folders are excluded. A folder containing documents appears even when some of its descendants also contain documents. Provider document IDs prevent duplicate results from overlapping grants and cycles.

Folder cards can show full paths, PDF/Markdown counts, and document sizes. Display options persist, along with sorting by path, name, size, or document count. Size totals only the supported documents directly inside that folder; unknown provider sizes are marked incomplete. Search matches full paths; PDF/Markdown filters select folders containing those file types. Selecting a folder opens its direct documents.

Scanning runs off the main thread, one metadata query per unique directory, without opening or rendering documents. It uses an iterative queue, counts files in one pass, and skips sorting intermediate directory contents. UI progress is throttled to roughly ten updates per second. A metadata-only index is atomically saved after completed scans and reused on launch without an automatic rescan. Only adding/changing locations or pressing Refresh scans directories again; adding a location scans just the new tree. Opening a discovered folder loads its current document list on demand, with a loading indicator. Explore indexing adds no libraries or background services; it stores metadata only, with no document copies, thumbnails, or rendered pages.

Unreadable folders are skipped and reported; valid siblings remain visible. Refresh rescans and retries location access. Traversal is bounded to 20,000 directories and 20,000 entries per directory for low-memory devices; limits are reported rather than claiming a complete scan. The index is scoped to the exact granted locations. Interrupted cache writes preserve the previous snapshot, and removed/inaccessible grants are filtered out.

Manage grants under **Scan locations**. Removing one hides its indexed folders without deleting files; permission is retained for previously opened reading-history documents. Android restricts which storage roots can be selected, so add each accessible location you want to scan. Missing, moved, or inaccessible documents show a “File not found” dialog with Lucide check/minus icons, matching PracticeFlow’s icon set. OK preserves the history entry; Remove deletes only that entry, never the original file. Lucide’s license is bundled under `assets/licenses/`.

## Reading and memory

Both readers place their fading scroll indicator at the outer right edge, clear of the reading content. Markdown margins are applied to the text inside the full-width scroll view. PDF has a separate indicator gutter and a page scrubber.

Markdown supports headings, lists, links, code blocks, tables, and strikethrough. Parsing runs off the main thread. Embedded remote/relative images are not loaded. Files larger than 12 MiB are rejected to bound memory use. PDFium renders the original PDF content and exposes its real text geometry on Android 14+, without requiring newer system PDF extensions. Long-press text to select a word, drag the selection or its handles, and use Copy or Select page. PDF tools also provide on-demand text search. Mixed text/image layouts remain intact. Image-only scans have no selectable text unless their PDF already contains an OCR layer; OCR is not bundled.

Pages are capped around 4 MiB, and the cache holds at most three bitmaps and 12 MiB. Text is opened only on selection/search, with at most one native text page retained. Search cancels on query changes and checks pages sequentially. Selections over 100,000 characters report a limit to protect clipboard and memory usage. Live UI bitmaps are never manually recycled. Native binaries are compressed in the universal APK, and Android extracts only the device's ABI. PDFium/PdfiumAndroid license notices are bundled under `assets/licenses/`.

Room saves character/line anchors for Markdown and page/row fractions for PDF. Saves run during continuous scrolling at intervals of at least 250 ms, after 150 ms of inactivity, and on pause/navigation. Switching documents waits for queued writes. The last active document reopens on launch.

The 1.0-to-1.1 database migration preserves positions and adds source/folder metadata. Provider document IDs match individual and tree URIs, allowing a document opened through Explore to reuse its earlier history and position. Older individually opened documents show under “Opened individually” until opened through Explore.

Abrupt termination or power loss can discard a position not yet committed to disk. Changed document content can affect anchor restoration. No lifecycle callback guarantees a final save after termination.

## Clear app data

Settings contains a **Clear app data** action guarded by a confirmation dialog. Confirming invokes Android's own reset for this app and closes SlateFlow. It clears indexes, saved scan locations/access grants, reading history and positions, preferences, and app cache together. Original PDFs and Markdown files selected from external locations are not deleted. Cancel dismisses the dialog without invoking the reset.

## Build and verification

Use Android Studio's configured SDK/JDK, or run:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

Tests cover recursive discovery through 1,500 nested levels, empty/intermediate folders, overlapping roots, cycles, cancellation, unreadable branches, scan limits, size totals, filtering/sorting, persistent and interrupted cache writes, history migration, reading positions, bounded PDF rendering, navigation, themes, Explore controls, mixed-content PDF selection/search, actual long-press-to-copy interaction, missing-file recovery without deleting original files, and reset confirmation/cancellation. Test documents are synthetic and installed only with the instrumentation APK.

Release builds enable R8/resource shrinking; release signing is not configured. The installable development APK is `app/build/outputs/apk/debug/app-debug.apk`. For local delivery, the optimized release APK can be signed with the same development key; the current packaged build is `app/build/outputs/apk/release/SlateFlow-1.2.apk`. Version 1.2 uses version code 3 and can update the previous debug-signed installations without clearing its data. A device-generated Baseline Profile and Macrobenchmark remain future performance work.
