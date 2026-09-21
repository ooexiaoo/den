# Keep Kotlinx Serialization
-keepattributes *Annotation*
-keepclassmembers class **$serializer {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.den.app.**$$serializer { *; }
-keepclassmembers class com.den.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.den.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}