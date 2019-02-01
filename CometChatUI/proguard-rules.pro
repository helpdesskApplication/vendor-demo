# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in H:\android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# For IceLink
-keep class fm.icelink.** { *; }
-keep class fm.android.** { *; }

-dontskipnonpubliclibraryclassmembers
-target 1.7
-dontshrink
-dontoptimize
-keeppackagenames com.inscripts.heartbeats
-keepattributes SourceFile,LineNumberTable,*Annotation*,InnerClasses,Signature,Exceptions,EnclosingMethod
-renamesourcefileattribute SourceFile
-dontpreverify
-verbose
-ignorewarnings
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-dontpreverify
-verbose
-printmapping mapping.txt
-allowaccessmodification

# Gson specific classes
-keep class sun.misc.Unsafe {
    <fields>;
    <methods>;
}
-keep class com.google.gson.stream.** {
    <fields>;
    <methods>;
}


-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.licensing.ILicensingService
-dontnote com.android.vending.licensing.ILicensingService

-keep public class * extends com.inscripts.utils.SuperActivity
-keep public class * extends com.inscripts.utils.CustomActivity
-keep public class * extends android.support.v7.app.AppCompatActivity

# Preserve all native method names and the names of their classes.
-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Preserve static fields of inner classes of R classes that might be accessed
# through introspection.
-keepclassmembers class **.R$* {
  public static <fields>;
}

# Preserve the special static methods that are required in all enumeration classes.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep public class * {
    public protected *;
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keep,allowshrinking class com.inscripts.heartbeats.* {
    <methods>;
}

-keep,allowshrinking class com.google.libvpx.*

-keep class com.inscripts.** { *; }
-keep public interface * {*;}

-dontwarn org.bouncycastle.**
-dontwarn org.apache.http**
-dontwarn fm.AdobeExtension**

-ignorewarnings
-keep class * {
public private *;
}