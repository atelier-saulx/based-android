package com.saulx.based

import com.saulx.based.model.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object FileUploader {

    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun upload(fileUploadOptions: FileUploadOptions, url: String, auth: String): String? {

        val requestBody = when(fileUploadOptions) {
            is StringFileUploadOptions -> fileUploadOptions.contents.toRequestBody()
            is FileFileUploadOptions -> fileUploadOptions.file.asRequestBody()
            is CustomFileUploadOptions -> fileUploadOptions.toRequestBody()
            is ReferenceFileOptions -> return null
        }

        val authHeader = auth.trimIndent().let {
            URLEncoder.encode(it, "UTF-8")
        }

        println("Start coroutine")
        return suspendCoroutine { continuation ->
            val httpClient = OkHttpClient.Builder()
                .build()
            val request = Request.Builder()
                .url(url)
                .addHeader("Req-Type", "blob")
                .addHeader("Content-Type", fileUploadOptions.mimeType ?: "text/plain")
                .addHeader("JSON-Authorization", authHeader)
                .addHeader("Transfer-Encoding", "chunked")
                .post(requestBody)
                .build()
            request.headers.forEach {
                println("Request header: ${it.first} -> ${it.second}")
            }
            httpClient.newCall(request).execute().use {
                println("Upload call to $url completed ${it.code}")
                val fileId = it.body?.let { body ->
                    val responseString = body.string()
                    println("Response from $url: \"$responseString\"")
                    responseString
                }
                it.close()
                continuation.resume(if (it.code == 200) fileId else null)
            }
        }
    }
}