package com.lechenmusic.data.api

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

/**
 * A safe JSON converter that handles non-JSON responses gracefully.
 * When the server returns HTML or plain text instead of JSON,
 * it throws a meaningful exception instead of crashing.
 */
class SafeJsonConverterFactory(
    private val delegate: GsonConverterFactory
) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *> {
        // In release builds, ProGuard may strip generic type info causing
        // ClassCastException when GsonConverterFactory tries to use the type.
        // Wrap in try-catch to handle gracefully.
        val delegateConverter = try {
            delegate.responseBodyConverter(type, annotations, retrofit)
        } catch (e: ClassCastException) {
            // If the delegate fails due to type erasure issues, create a fallback
            // that parses the response as a raw JsonElement and then converts
            null
        }

        return Converter<ResponseBody, Any?> { body ->
            val contentType = body.contentType()
            val mediaType = contentType?.toString() ?: ""

            // If response is not JSON, throw a meaningful error
            if (mediaType.contains("text/html") || mediaType.contains("text/plain")) {
                val text = body.string().take(200)
                throw Exception("服务器返回了非JSON响应: $text")
            }

            try {
                if (delegateConverter != null) {
                    delegateConverter.convert(body)
                } else {
                    // Fallback: parse as generic JsonObject
                    val jsonString = body.string()
                    val gson = com.google.gson.GsonBuilder().setLenient().create()
                    gson.fromJson(jsonString, com.google.gson.JsonElement::class.java)
                }
            } catch (e: ClassCastException) {
                // Type casting error in release build - try raw JSON parsing
                try {
                    val jsonString = body.string()
                    val gson = com.google.gson.GsonBuilder().setLenient().create()
                    gson.fromJson(jsonString, com.google.gson.JsonElement::class.java)
                } catch (e2: Exception) {
                    throw Exception("服务器响应格式错误，请检查服务器地址是否正确")
                }
            } catch (e: com.google.gson.JsonSyntaxException) {
                throw Exception("服务器响应格式错误，请检查服务器地址是否正确")
            } catch (e: IllegalStateException) {
                throw Exception("服务器响应格式错误，请检查服务器地址是否正确")
            }
        }
    }

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody> {
        return delegate.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)!!
    }
}

object ApiClient {
    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String? = null

    fun getApi(baseUrl: String): SubsonicApi {
        val normalizedUrl = normalizeUrl(baseUrl)
        if (retrofit == null || currentBaseUrl != normalizedUrl) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            val gson = com.google.gson.GsonBuilder()
                .setLenient()
                .create()

            retrofit = Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(client)
                .addConverterFactory(SafeJsonConverterFactory(GsonConverterFactory.create(gson)))
                .build()
            currentBaseUrl = normalizedUrl
        }
        return retrofit!!.create(SubsonicApi::class.java)
    }

    private fun normalizeUrl(url: String): String {
        var normalized = url.trim()
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "http://$normalized"
        }
        if (!normalized.endsWith("/")) {
            normalized = "$normalized/"
        }
        return normalized
    }

    fun getCoverArtUrl(baseUrl: String, username: String, password: String, coverArtId: String?): String? {
        if (coverArtId.isNullOrBlank()) return null
        val normalizedUrl = normalizeUrl(baseUrl)
        val encodedPass = if (password.startsWith("enc:")) password else "enc:${password.toByteArray().joinToString("") { "%02x".format(it) }}"
        return "${normalizedUrl}rest/getCoverArt?u=$username&p=$encodedPass&id=$coverArtId&v=1.16.1&c=lechenmusic"
    }

    fun getStreamUrl(baseUrl: String, username: String, password: String, songId: String): String {
        val normalizedUrl = normalizeUrl(baseUrl)
        val encodedPass = if (password.startsWith("enc:")) password else "enc:${password.toByteArray().joinToString("") { "%02x".format(it) }}"
        return "${normalizedUrl}rest/stream?u=$username&p=$encodedPass&id=$songId&v=1.16.1&c=lechenmusic"
    }
}
