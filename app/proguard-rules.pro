# ProGuard rules for Mini Jarvis
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
