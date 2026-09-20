-ignorewarnings
# Facebook Audience Network issues
-dontwarn com.facebook.infer.annotation.**
-keep class com.facebook.ads.** { *; }

# General ProGuard Rules
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Gson, Models & Callbacks
# Keep all models, callbacks, and database entities so Gson reflection works in Release mode
-keep class com.app.webdroid.model.** { *; }
-keep class com.app.webdroid.callback.** { *; }
-keep class com.app.webdroid.database.** { *; }
-keep class com.app.webdroid.util.** { *; }
-keep class com.app.webdroid.Config { *; }
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class com.app.webdroid.model.** { <fields>; <methods>; }
-keepclassmembers class com.app.webdroid.callback.** { <fields>; <methods>; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# Retrofit & OkHttp
-dontwarn okio.**
-dontwarn javax.annotation.**

# Solodroid Ads SDK
-keep class com.solodroid.ads.sdk.** { *; }
-keep class com.solodroidx.ads.** { *; }


# Solodroid Push SDK
-keep class com.solodroid.push.sdk.** { *; }

# OneSignal
-keep class com.onesignal.** { *; }
-dontwarn com.onesignal.**

# QRCode Scanner & ZXing
-keep class com.blikoon.qrcodescanner.** { *; }
-keep class com.google.zxing.** { *; }

# Ignore common missing annotation classes affecting R8
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# WorkManager
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# Google Play In-App Review & Update
-keep class com.google.android.play.core.** { *; }

# Google Play Services Ads
-keep public class com.google.android.gms.ads.** { public *; }
-keep interface com.google.android.gms.ads.** { *; }

# Retrofit 2
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}
