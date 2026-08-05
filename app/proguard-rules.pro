# SQLCipher loads its native layer via JNI; R8 must not rename the bridge.
-keep class net.zetetic.database.** { *; }
-keep class net.sqlcipher.** { *; }

# kotlinx.serialization keeps generated serializers by reflection on the
# companion; the standard rules from the plugin cover most of it, these
# cover the @Serializable classes we own.
-keepclassmembers class com.nijika21.yourmoney.** {
    *** Companion;
}
-keepclasseswithmembers class com.nijika21.yourmoney.** {
    kotlinx.serialization.KSerializer serializer(...);
}
