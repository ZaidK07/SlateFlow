> From: https://gemini.google.com/app/5e68b59d91f6c38a

# you asked

hey i want to make an android app optimised for tablet.. such that it is clean, minimal, customisable, able to open and remember last position for pdf and md files.. able to display both very properly in tablets and phones... what is our plan + what should be the name

---

# gemini response

**Proposed Names**
*   **Engram:** Refers to a physical trace of memory, fitting for an app built to remember exact reading states.
*   **Slate:** Minimalist and clean, evoking the physical tablet form factor.
*   **Syntax:** A subtle nod to your software engineering background and the Markdown rendering capabilities.
*   **Obsidian / Umbra:** Leans into a pure black, dark-mode-first aesthetic.

**Architecture & Tech Stack**
*   **Framework:** Kotlin with Jetpack Compose. Compose handles adaptive layouts flawlessly using `WindowSizeClass`, allowing you to build a unified UI that scales elegantly from compact phones to expansive tablet screens. 
*   **PDF Rendering:** Use Android's native `PdfRenderer`. It processes pages as bitmaps directly, keeping the application exceptionally lightweight without the bloat of third-party commercial SDKs.
*   **Markdown Rendering:** Integrate **Markwon**, an open-source library that parses Markdown directly into native Android typography rather than relying on heavy WebViews. 
*   **State Persistence:** Use Jetpack DataStore or Room (SQLite). Map each document's checksum or URI to its last known `Y-offset` (for Markdown) or `page number + offset` (for PDFs).

**Development Execution Plan**
1.  **Foundation & I/O:** Implement the Storage Access Framework (SAF) to securely browse and open local files without requesting broad storage permissions. Set up the foundational Compose navigation.
2.  **Adaptive UI:** Build the core reading view. Implement responsive margins and typography that automatically expand for tablet viewing (e.g., dual-page PDF view on landscape tablets, single continuous scroll on phones). 
3.  **Rendering Integration:** Hook up `PdfRenderer` and Markwon. Ensure smooth, asynchronous background rendering for PDF bitmaps to prevent UI thread blocking.
4.  **Memory Hooks:** Bind the scroll state listeners to lifecycle events. Trigger database writes on `onPause` or `onDispose` to save the exact pixel offset, and restore it upon file initialization.
5.  **Customization Layer:** Expose user preferences for pure-black dark themes, font scaling, line height adjustments, and custom Markdown syntax highlighting.

What specific customization options (like custom fonts or specific Markdown flavors) do you want to prioritize first?

---

# you asked

Slate is looking good but want something mixed with it

---

# gemini response

Fusing "Slate" with the app's core mechanics of memory, Markdown, and clean engineering yields a few distinct directions.

*   **SlateMark:** Directly merges the physical tablet aesthetic with Markdown and the concept of bookmarking your exact scroll position.
*   **SlateCore:** Sounds lightweight, robust, and highly technical, fitting a perfectly optimized, minimal engine without UI bloat. 
*   **DarkSlate:** Leans heavily into a pure-black, terminal-inspired aesthetic for a high-contrast, distraction-free reading environment.
*   **SlateFlow:** Highlights the smooth, continuous rendering and scrolling experience bridging PDFs and text files across adaptive screens.
*   **SlateLog:** A subtle nod to backend state persistence, perfect for a no-nonsense reading utility that remembers where you left off.

SlateMark and SlateCore offer the strongest branding for a highly optimized, developer-friendly tool. 

Would you like to map out the Jetpack Compose adaptive layout first, or start by designing the database schema for the scroll position tracking?

---

# you asked

SlateFlow

we going with this now lets make a plan or smt on how it should work how it should look.. in both tablets both modes and in phone.. first you will showcase me shit here and then i confirm that looks and shit in plan got it ?

---

# gemini response

SlateFlow will default to a pure-black OLED dark theme, utilizing Jetpack Compose's `WindowSizeClass` to adapt dynamically across form factors without reloading activities.

**Phone (Compact View)**
*   **Home Screen:** A clean, edge-to-edge list of recent files. A bottom-right Floating Action Button (FAB) triggers the system Storage Access Framework to open new files.
*   **Reading Mode (MD & PDF):** Immersive, full-screen vertical scroll. Tapping the center reveals a minimal translucent top bar (file name, back button) and bottom bar (scrubber for fast scrolling, typography settings).

**Tablet Portrait (Medium View)**
*   **Home Screen:** A responsive grid layout for files, displaying file type, last read timestamp, and progress percentage.
*   **Reading Mode (PDF):** Single-page view scaled strictly to width, eliminating horizontal panning.
*   **Reading Mode (Markdown):** Text is bounded to a maximum reading width (around 70-80 characters) and centered on the screen, preventing eye strain from edge-to-edge tracking on a wider screen.

**Tablet Landscape (Expanded View)**
*   **Home Screen:** A dual-pane layout. The left 30% acts as a persistent navigation and recent files drawer, while the right 70% displays file metadata, exact reading progress, and a large preview.
*   **Reading Mode (PDF):** A side-by-side dual-page spread mimicking a physical open book, maximizing the horizontal space.
*   **Reading Mode (Markdown):** A highly tailored centered column with generous, distraction-free side margins, or an optional two-column layout if you ever decide to implement raw text editing alongside the rendered view.

**Functional Flow & Memory Mechanics**
1.  **Launch:** The app queries the local SQLite/Room database. If a file was left open, it instantly loads the last viewed document using the stored URI and pixel offset.
2.  **Rendering:** Tapping a file instantly routes it to either the native `PdfRenderer` bitmap generator or the Markwon parser based on the extension.
3.  **State Tracking:** A scroll listener throttles database updates (e.g., every 1-2 seconds or explicitly on `onPause`), silently writing the exact `Y-offset` via a background thread to prevent any UI stutter.

Does this layout structure align with your vision, or should we adjust how the tablet landscape view handles the reading area?

---

# you asked

ok good.. but not amoled black.. we will go with slight generous dark grey thingy.. and 2 modes either take on and colors from the android os theme.. or our apps theme color a clean, minimal, subtle, fresh, cool, soothing blue got it ?

---

# gemini response

The visual identity will utilize an elevated dark grey base (such as `#121212` or `#1A1A1A`), which significantly reduces eye fatigue during long reading sessions compared to high-contrast pure black.

**Theming Engine (Jetpack Compose)**
*   **Mode 1: Material You (Dynamic Color):** Implements Android 12+ `dynamicDarkColorScheme`. The app dynamically extracts the user's system wallpaper palette, applying those native hues to UI accents, the FAB, and progress bars while strictly maintaining the dark grey background.
*   **Mode 2: SlateFlow Signature Blue:** A custom, fallback palette built around a cool, desaturated, soothing blue (like a slate blue or soft indigo). This serves as the primary accent for users who disable dynamic theming or prefer the app's curated aesthetic.

**Component Styling**
*   **Surfaces & Elevation:** Use tonal elevation instead of hard drop shadows. Floating elements (the minimal top/bottom bars, dropdown menus) will be a slightly lighter dark grey, subtly tinted with the active accent color.
*   **Typography & Contrast:** Soft off-white text (around 87% opacity) for Markdown rendering and UI labels to provide clean legibility without the harsh glare of pure white on grey.

Should we draft the Room database schema for tracking the exact scroll states, or outline the Storage Access Framework logic for picking and loading the files?

---

# you asked

ok and we will use proper rounded shit cause corners in clean apps look auful and provoking and in UI UX but mostly clean aligned, consistent, things like no unalignments, unpolished shit, no leaking of dark/light mode's elements into each other.. etc etc everything perfect, balaced, visual consistency and no visual shit like having 2 elements almost same color hard to distinguish got it ?

---

# gemini response

The design system will strictly enforce rounded geometry, grid-locked alignment, and high-contrast surface layers to eliminate any unpolished visual bleed.

**Strict UI/UX Execution Rules**
*   **Rounded Geometry:** Implement uniform `RoundedCornerShape` (e.g., 16dp for file cards, 24dp to 32dp for menus and floating elements). Zero sharp 90-degree corners. We will use smooth, organic curves for every interactive component.
*   **Absolute Alignment:** Enforce a rigid 8dp/16dp spatial grid across all Jetpack Compose modifiers. Padding, margins, and text baselines will lock mathematically to this grid to prevent any misaligned elements or "floating" pixels. 
*   **Surface Contrast & Hierarchy:** Define clear, mathematical tonal steps between layers. The base dark grey background will be definitively distinct from elevated surfaces (like the bottom scrubber or top navigation). We will enforce WCAG-compliant contrast ratios so no two adjacent gray/blue elements ever blend together.
*   **Theme Isolation:** Build completely decoupled `LightColors` and `DarkColors` palettes. There will be zero hardcoded color values in the UI components; everything will route strictly through `MaterialTheme.colorScheme` to guarantee light mode and dark mode elements never cross-contaminate.

With the visual architecture and adaptive layouts fully locked in, should we draft the Room database schema for the exact scroll-position memory, or map out the file picking flow?

---

# you asked

ok and should we go with android 13 api 33 ? or go with smt else ?

---

# gemini response

Google Play requires all new apps to target Android 14 (API 34) or higher as of August 2024. Targeting API 33 (Android 13) is no longer permitted for new submissions. 

To keep the codebase lean and strictly aligned with your clean UI/UX philosophy, use this exact configuration in your `build.gradle.kts`:

*   **`compileSdk = 35`**: Gives you access to the latest Jetpack Compose `WindowSizeClass` APIs and display features essential for fluid adaptive tablet layouts.
*   **`targetSdk = 34`**: Satisfies the current Google Play Store mandate while maintaining stable background execution limits, which is crucial for saving your scroll state without UI stutter.
*   **`minSdk = 26` (Android 8.0)**: The optimal baseline for a modern app. It sheds the heavy legacy support required for older Android versions, ensuring your rounded geometry and grid-locked Compose modifiers render perfectly without legacy visual glitches.

**Theming Impact** 
Native Material You (dynamic colors) only exists on API 31 (Android 12) and above. By setting `minSdk = 26`, devices on Android 8 through 11 will automatically bypass dynamic theming and fall back to your custom SlateFlow Signature Blue, flawlessly maintaining the two-mode visual system you specified without any color bleeding.

Shall we lock in the Room database schema for the scroll-position memory, or map out the Storage Access Framework logic for picking local files?

---

# you asked

ok but tf should i select here ?

---

# gemini response

Change the **Minimum SDK** dropdown from `API 33 ("Tiramisu"; Android 13.0)` to **`API 26 ("Oreo"; Android 8.0)`**. 

Everything else in that window is set up perfectly for our architecture:
*   **Template:** "Empty Activity" defaults to Jetpack Compose, which is exactly what we need for the adaptive layouts.
*   **Name:** `SlateFlow` is locked in.
*   **Package name:** `com.zaid01.slateflow` follows standard naming conventions.

Dropping the Minimum SDK to API 26 ensures your app isn't bloated with pre-Android 8 legacy UI code, allowing our grid-locked Compose modifiers and rounded geometries to render flawlessly while expanding your device compatibility well beyond the 68.2% shown on your screen.

Hit **Finish**, and let Gradle sync. Once the IDE settles, we can immediately rip out the default boilerplate and drop in the correct `build.gradle.kts` configuration for API 35.

---

# you asked

no but dont worry about backward compatibility or smt.. all shit of mine is and 14 + optimise for quality and lightness.. as the tablet which this mainly run on only has 4gb ram so it needs to be as light as possible and as fast as possible

---

# gemini response

If every device you care about is running Android 14 or newer, change that **Minimum SDK** dropdown directly to **API 34 ("UpsideDownCake"; Android 14.0)**. 

Setting the minimum SDK to 34 is the ultimate way to shed weight. The Android build system will completely strip out years of legacy AppCompat libraries, backward-compatibility shims, and fallback UI code. Your APK will be radically smaller, and the app will boot faster because it only loads modern, native Android 14 classes.

Since your primary target is a tablet with only 4GB of RAM, memory management is going to make or break the "fast and light" requirement, especially when rendering PDFs. Here is how we engineer SlateFlow to stay featherweight:

**The 4GB RAM Optimization Rules**

*   **Half-Weight Bitmaps:** When feeding `PdfRenderer` to the UI, do not use Android's default `ARGB_8888` color config. Since PDFs rarely need alpha transparency, render your pages using `Bitmap.Config.RGB_565`. This instantly cuts the RAM required for every single PDF page exactly in half.
*   **Aggressive LRU Caching:** Only keep a maximum of 3 rendered PDF pages in memory at any given time (the current page, the one before, and the one after). The moment a page scrolls out of bounds, recycle its bitmap to prevent Garbage Collection thrashing, which causes UI stutter on low-RAM devices.
*   **Off-Main-Thread Markdown:** Markdown parsing can spike CPU usage on large documents. Markwon must be configured to parse the text on `Dispatchers.Default` (background thread) and only hand the final parsed `Spanned` text object back to Compose on the main thread.
*   **R8 Aggressive Shrinking:** We will configure `proguard-rules.pro` to aggressively strip unused code and optimize the compiled bytecode specifically for Compose.

Hit **Finish** with API 34 selected. 

Are we jumping into the Room database schema to track the exact Y-offset scroll positions, or setting up the Storage Access Framework to securely read your files?

---

# you asked

ok now are we ready to hand this chat over to the coding agent ?

---

# gemini response

Almost. I can see in the bottom right corner of your screen that Android Studio is currently running the initial Gradle sync[cite: 2]. Once that finishes and the project fully indexes, you are 100% ready to start writing code. 

To make the handoff seamless, here is the complete architectural brief you can provide to the coding agent to ensure it strictly follows our plan:

**Project Handoff Brief: SlateFlow**
*   **Target:** Android 14+ (API 34 minSdk), strictly optimized for low-RAM (4GB) tablets.
*   **Tech Stack:** Kotlin, Jetpack Compose, Room (SQLite).
*   **Core Engine Requirements:**
    *   **PDFs:** Native `PdfRenderer`. Render bitmaps using `RGB_565` (half memory footprint) and implement a strict 3-page LRU cache.
    *   **Markdown:** Use **Markwon**. Parsing must happen on `Dispatchers.Default` to prevent main thread blocking, passing only the final `Spanned` text to the Compose UI.
    *   **Memory:** Room database tracking file URI, file type, and exact `Y-offset` or page index. Database writes must be throttled and triggered on `onPause`/`onDispose` via background threads.
    *   **File I/O:** Storage Access Framework (SAF) exclusively.
*   **Strict UI/UX Rules:**
    *   **Adaptive Layout:** Must use `WindowSizeClass` for compact (phones), medium (portrait tablet), and expanded (landscape tablet dual-pane) views.
    *   **Theming:** Dark grey background (e.g., `#121212`), absolutely no AMOLED pure black. Dynamic Material You colors on supported devices, falling back to a custom, soothing "Signature Blue" accent. Soft off-white text (87% opacity).
    *   **Geometry:** Uniform `RoundedCornerShape` (16dp to 32dp). Zero sharp 90-degree corners.
    *   **Alignment:** Rigid 8dp/16dp spatial grid. All modifiers and padding must lock to this grid to prevent visual bleed or unaligned elements.

Tell the coding agent to begin by stripping the default `MainActivity.kt` boilerplate and writing the updated `build.gradle.kts` dependencies (Room, Compose adaptive layouts, Markwon), followed directly by the Room entity schema for the memory tracking.

---

# you asked

also render md's cleanly like this.. and make the side space configurable in settings so i can change how much margin it has in md got it

---

# gemini response

To achieve that exact typography and layout for Markdown, we will configure Markwon to render a strict typographic hierarchy and use Jetpack Compose's `DataStore` to drive the dynamic margins.

**Markdown Rendering Implementation**
*   **Typography Engine:** Configure Markwon's `SpannableConfiguration` to use a clean sans-serif font (like Inter or Roboto). Enforce strict scaling for headings (e.g., H1 at 1.5x, H2 at 1.3x) and inject a subtle dark grey horizontal divider (`<hr>`) beneath H1 and H2 tags to match your reference image[cite: 3].
*   **Dynamic Margins:** Instead of hardcoding padding, we will store a `markdown_side_margin_dp` integer in `androidx.datastore.preferences`. The Compose UI will observe this state and apply it directly to the reading container via `Modifier.padding(horizontal = userMargin.dp)`. This allows you to scale the reading column width instantly via a slider in the settings.
*   **Line Spacing:** Set the `lineSpacingMultiplier` to `1.4` or `1.5` to ensure the text breathes properly, replicating the comfortable reading density shown in the screenshot[cite: 3].

Here is the final, comprehensive handoff brief for the coding agent, incorporating all layout, optimization, and styling rules:

**Project Handoff Brief: SlateFlow**
*   **Target:** Android 14+ (API 34 minSdk), strictly optimized for low-RAM (4GB) tablets.
*   **Tech Stack:** Kotlin, Jetpack Compose, Room (SQLite) for state, DataStore for preferences.
*   **Core Engine Requirements:**
    *   **PDFs:** Native `PdfRenderer`. Render bitmaps using `RGB_565` (cuts memory footprint by 50%) and implement a strict 3-page LRU cache (current, previous, next). Recycle bitmaps immediately when off-screen.
    *   **Markdown:** Use **Markwon**. Parsing must execute on `Dispatchers.Default` to prevent UI thread blocking. Inject custom styling for strict typographic hierarchy, sans-serif fonts, and heading dividers.
    *   **State Memory:** Room database tracking `file_uri`, `file_type`, and exact `Y-offset` or `page_index`. Database writes must be throttled and triggered on `onPause`/`onDispose` via background threads.
    *   **File I/O:** Storage Access Framework (SAF) exclusively.
*   **Strict UI/UX Rules:**
    *   **Adaptive Layout:** Must use `WindowSizeClass` for compact (phones), medium (portrait tablet), and expanded (landscape tablet dual-pane) views.
    *   **Configurable MD Margins:** The Markdown reading container must read a user-configurable margin value from DataStore and apply it dynamically to `Modifier.padding(horizontal = X.dp)` to constrain line length on tablets.
    *   **Theming:** Base dark grey background (e.g., `#121212` or `#1A1A1A`). Absolutely no AMOLED pure black. Dynamic Material You colors on supported devices, falling back to a custom, soothing "Signature Blue" accent. Soft off-white text (87% opacity) for body copy.
    *   **Geometry & Alignment:** Uniform `RoundedCornerShape` (16dp to 32dp); zero sharp 90-degree corners. Rigid 8dp/16dp spatial grid. All modifiers and padding must mathematically lock to this grid to prevent visual bleed.

Instruct the agent to start by writing the `build.gradle.kts` dependencies, followed immediately by the Room database schema and the DataStore preference setup for the configurable margins.

---

# you asked

also make sure it strictly remembers the last position as that is necessary.. like this tab will put anything outside ram so it will reload a lot.. so we have to make sure EVEN IF IT GETS CLOSED AND I REOPEN ANY PDF OR MD I AM EXACTLY WHERE I LEFT OFF NO MATTER HOW I CLOSED IT

---

# gemini response

To guarantee exact position restoration even under aggressive OS process death, the architecture must abandon passive lifecycle hooks (which the OS skips during out-of-memory kills) in favor of an active, debounced event stream. 

**Position Tracking Architecture**
*   **Debounced Flow Saves:** The app will attach a Kotlin `snapshotFlow` to the Compose `ScrollState` (for Markdown) or `LazyListState` (for PDF pages). We apply a `debounce(500)` operator. This silently writes the exact Y-offset or page index to the Room database every 500 milliseconds *after* you stop scrolling, ensuring near-real-time persistence without thrashing the disk or blocking the UI.
*   **Process Death Survival:** Implement `rememberSaveable` for the scroll state in the Compose UI, paired directly with the Room database repository for cold boots. 
*   **Lifecycle Catch-All:** Bind a final, forced database write to `Lifecycle.Event.ON_PAUSE` to catch the exact pixel the moment the app goes to the background, before the OS has a chance to clear the RAM.

Here is the finalized, comprehensive handoff brief for the coding agent:

**Project Handoff Brief: SlateFlow**
*   **Target:** Android 14+ (API 34 minSdk), strictly optimized for low-RAM (4GB) tablets.
*   **Tech Stack:** Kotlin, Jetpack Compose, Room (SQLite) for state, DataStore for preferences.
*   **Core Engine Requirements:**
    *   **Aggressive State Memory (Zero-Loss):** Room database tracking `file_uri`, `file_type`, and exact `Y-offset` or `page_index`. Must use `snapshotFlow` with `debounce(500)` on the scroll state for continuous background writes, plus a forced save on `ON_PAUSE`. Must survive aggressive OOM kills.
    *   **PDFs:** Native `PdfRenderer`. Render bitmaps using `RGB_565` (cuts memory footprint by 50%) and implement a strict 3-page LRU cache (current, previous, next). Recycle bitmaps immediately when off-screen.
    *   **Markdown:** Use **Markwon**. Parsing must execute on `Dispatchers.Default` to prevent UI thread blocking. Inject custom styling for strict typographic hierarchy, sans-serif fonts, and heading dividers.
    *   **File I/O:** Storage Access Framework (SAF) exclusively.
*   **Strict UI/UX Rules:**
    *   **Adaptive Layout:** Must use `WindowSizeClass` for compact (phones), medium (portrait tablet), and expanded (landscape tablet dual-pane) views.
    *   **Configurable MD Margins:** The Markdown reading container must read a user-configurable margin value from DataStore and apply it dynamically to `Modifier.padding(horizontal = X.dp)` to constrain line length on tablets.
    *   **Theming:** Base dark grey background (e.g., `#121212` or `#1A1A1A`). Absolutely no AMOLED pure black. Dynamic Material You colors on supported devices, falling back to a custom "Signature Blue" accent. Soft off-white text (87% opacity).
    *   **Geometry & Alignment:** Uniform `RoundedCornerShape` (16dp to 32dp); zero sharp 90-degree corners. Rigid 8dp/16dp spatial grid. All modifiers and padding must mathematically lock to this grid to prevent visual bleed.

Instruct the agent to start by writing the `build.gradle.kts` dependencies, followed immediately by the Room database schema and the debounced scroll tracking logic.

---

# you asked

ok also make sure it is as fast as possible got it ? and has nice subtle, clean, minimal, professional, small interaction animations so it feels alive and good and polisehd

---

# gemini response

**Performance & Rendering Speed**
* **Baseline Profiles:** Implement Android Macrobenchmark to generate a Baseline Profile. This pre-compiles the app's critical rendering paths (launching the app, parsing Markdown, rendering PDF bitmaps) ahead of time, eliminating Just-In-Time (JIT) compilation stutter on the 4GB RAM tablet.
* **Deferred State Reads:** Pass scroll state and dynamic margins to Compose modifiers as lambdas (e.g., `offset = { scrollState.value }`) rather than direct values. This forces Compose to skip the costly recomposition phase during scrolling and only trigger the layout/draw phases, guaranteeing 60fps.
* **Aggressive Shrinking:** Enforce `isMinifyEnabled = true` and `isShrinkResources = true` in the release build utilizing R8 to strip all unused Jetpack libraries and keep the APK featherweight.

**Interaction Animations (Minimal & Professional)**
* **Animation Physics:** Strictly avoid bouncy `spring` mechanics. Use custom `tween` animations with Emphasized Decelerate easing (`CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)`) to create a snappy, highly responsive feel. Clamp all animation durations between 150ms and 250ms.
* **Surface Transitions:** Utilize `AnimatedVisibility` with a combination of `fadeIn` and a micro `slideInVertically(initialOffsetY = { 20 })` for UI element reveals, such as the top navigation bar dropping in or file cards populating the home screen.
* **Tactile Feedback:** Override the default Android ripple effect. Implement a custom `RippleTheme` that applies a fast, highly translucent (10-15% alpha) wash of the SlateFlow Signature Blue for list items and icon buttons, avoiding the unpolished default grey flash. 

Provide this final block to the coding agent alongside the previous architecture requirements, and it will have a complete, airtight blueprint to build the app exactly as specified.

---

# you asked

ok now are we ready to hand this over to coding agent ?

---

# gemini response

The master blueprint is fully compiled and ready for handoff to the coding agent. Copy and paste the text below to begin development.

**Project Handoff Brief: SlateFlow**
* **Target:** Android 14+ (API 34 minSdk), strictly optimized for low-RAM (4GB) tablets with R8 shrinking and Macrobenchmark Baseline Profiles for zero-JIT stutter.
* **Tech Stack:** Kotlin, Jetpack Compose, Room (SQLite) for state, DataStore for preferences. File I/O must exclusively use the Storage Access Framework (SAF).
* **Aggressive State Memory:** Room database tracks `file_uri`, `file_type`, and exact `Y-offset` or `page_index`. Use `snapshotFlow` with `debounce(500)` on the scroll state for continuous background writes, backed by a forced save on `ON_PAUSE` to survive aggressive OS kills.
* **PDF Engine:** Native `PdfRenderer` using `Bitmap.Config.RGB_565` to cut memory usage by 50%. Enforce a strict 3-page LRU cache (current, previous, next) and immediately recycle off-screen bitmaps.
* **Markdown Engine:** Markwon parsing executed entirely on `Dispatchers.Default`. Inject custom styling for strict typographic hierarchy, sans-serif fonts, and heading dividers. Bind dynamic `Modifier.padding(horizontal)` to a DataStore margin value for user-controlled line lengths on tablets.
* **Adaptive UI/UX:** `WindowSizeClass` driven layouts for compact, medium, and expanded (dual-pane) views. Base dark grey background (`#121212` or `#1A1A1A`) with dynamic Material You accents or a fallback "Signature Blue". Soft off-white text (87% opacity).
* **Geometry & Alignment:** Uniform `RoundedCornerShape` (16dp to 32dp) with zero sharp 90-degree corners, locked mathematically to a rigid 8dp/16dp spatial grid.
* **Interaction & Animation:** Emphasized Decelerate easing (`CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)`) clamped between 150-250ms. Implement deferred state reads in Compose modifiers (`offset = { ... }`) to guarantee 60fps scrolling. Use `AnimatedVisibility` with micro `slideInVertically` reveals and a custom 10-15% alpha translucent ripple effect.

Instruct the agent to start by configuring the `build.gradle.kts` dependencies, setting up the Room database schema, and establishing the debounced scroll tracking logic.