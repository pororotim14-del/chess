# Chess Engine Assistant - R8 / ProGuard rules (release)

# Native engine (JNI) entry points must be kept.
-keep class com.chessassistant.coreengine.jni.** { *; }

# Keep kotlinx.serialization generated serializers.
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    static *** INSTANCE;
}
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Room entities are created via generated code; keep fields.
-keep class com.chessassistant.data.db.** { *; }

# Hilt generates and injects these.
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

-dontwarn java.lang.invoke.
-dontwarn org.slf4j.**

-keepattributes SourceFile,LineNumberTable
-repackageclasses 'com.chessassistant.r8'