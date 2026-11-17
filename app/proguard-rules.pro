# Markwon and Prism4j
-keep class io.noties.markwon.** { *; }
-dontwarn io.noties.markwon.**
-keep class io.noties.prism4j.** { *; }
-dontwarn io.noties.prism4j.**

# JLatexMath
-keep class ru.noties.jlatexmath.** { *; }
-dontwarn ru.noties.jlatexmath.**
-keep class org.scilab.forge.jlatexmath.** { *; }
-dontwarn org.scilab.forge.jlatexmath.**

# SlateFlow models and Room DAOs
-keep class com.zaid01.slateflow.data.** { *; }
-keep class com.zaid01.slateflow.reader.** { *; }
-keepattributes *Annotation*
-keepattributes Signature
