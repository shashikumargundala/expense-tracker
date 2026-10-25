# R8 keep rules for the release build. Libraries ship their own consumer rules
# (Hilt, Compose, Retrofit, Moshi); these cover the app's own models.

# Moshi: JSON models are @JsonClass(generateAdapter = true). Keep their constructors
# and fields so the generated adapters still match after R8 full-mode optimization.
-keep @com.squareup.moshi.JsonClass class * {
    <init>(...);
    <fields>;
}

# Retrofit: service methods' generic return types and annotations are read by reflection.
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# Keep line numbers for readable release stack traces, but hide source file names.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
