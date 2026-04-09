# 保留 Kotlin 标准库的反射功能
-keep class kotlin.Metadata { *; }

# 保留 Kotlin 协程相关的类（避免协程被优化掉）
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# 保留 OkHttp 相关的类
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# 保留 OkHttp 序列化的类
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# 保留 OkHttp 代理相关类
-keep class okhttp3.internal.platform.** { *; }
-dontwarn okhttp3.internal.platform.**

# 保留 Gson 序列化（如果使用了 Gson）
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# 保留 JSON 序列化相关的类（适用于 Kotlinx Serialization）
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# 保留 Ktor 客户端（如果使用了 Ktor）
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# 保留 KMP 共享代码中的类（避免类被优化掉）
-keep class com.yourpackage.** { *; }

# 避免移除 R8 认为无用的代码
-dontshrink
-dontoptimize
-dontpreverify