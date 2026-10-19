-keepclassmembers class * {
    @org.junit.Test *;
    @org.junit.Before *;
    @org.junit.After *;
    @org.junit.BeforeClass *;
    @org.junit.AfterClass *;
}
-keep class androidx.test.** { *; }
-dontwarn androidx.test.**
-dontwarn androidx.test.espresso.**
-keep @androidx.annotation.VisibleForTesting class *
-keepclassmembers class * {
    @androidx.annotation.VisibleForTesting *;
}
-dontwarn org.mockito.**
-dontwarn io.mockk.**
-keep class kotlin.jvm.internal.** { *; }
-keep class kotlin.Metadata { *; }
-keep class androidx.tracing.Trace { *; }
-keep class com.maptec.applied.demo.** { *; }