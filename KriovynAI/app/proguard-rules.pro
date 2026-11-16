-keepattributes *Annotation*
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep class com.orbita.personalagent.ui.navigation.** { *; }
-keep class com.orbita.personalagent.ai.** { *; }
