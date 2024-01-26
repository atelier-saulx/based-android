package com.saulx.based

import com.saulx.based.model.CustomFileUploadOptions
import com.saulx.based.model.FileFileUploadOptions
import com.saulx.based.model.FileUploadOptions
import com.saulx.based.model.StringFileUploadOptions
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object FileUploader {

    suspend fun upload(fileUploadOptions: FileUploadOptions, url: String, auth: String): String? {
        var finalUrl = url
        val requestBody = when (fileUploadOptions) {
            is StringFileUploadOptions -> fileUploadOptions.contents.toRequestBody()
            is FileFileUploadOptions -> fileUploadOptions.file.asRequestBody()
            is CustomFileUploadOptions -> fileUploadOptions.toRequestBody()
        }

        val size = requestBody.contentLength()

        if(fileUploadOptions.payload?.isNotEmpty() == true){
            finalUrl += "?${fileUploadOptions.payload}"
        }

        println("Start coroutine at $finalUrl")
        return suspendCoroutine { continuation ->
            val httpClient = OkHttpClient.Builder()
                .build()
            val requestBuilder = Request.Builder().url(finalUrl)
                .addHeader("Req-Type", "blob")
                .addHeader("JSON-Authorization", auth)
                .addHeader("Transfer-Encoding", "chunked")
                .addHeader("Content-Type", fileUploadOptions.mimeType ?: "text/plain")
                .addHeader("Content-Length", size.toString())
                .post(requestBody)

            if (!fileUploadOptions.fileName.isNullOrEmpty()) {
                requestBuilder.addHeader("Content-Name", fileUploadOptions.fileName!!)
            }
            if (fileUploadOptions.mimeType.isNullOrEmpty() && !fileUploadOptions.extension.isNullOrEmpty()) {
                requestBuilder.addHeader("Content-Extension", fileUploadOptions.extension!!)
            }
            val request = requestBuilder.build()
            request.headers.forEach {
                println("Request header: ${it.first} -> ${it.second}")
            }
            httpClient.newCall(request).execute().use {
                println("Upload call to $finalUrl completed ${it.code}")
                val fileId = it.body?.let { body ->
                    val responseString = body.string()
                    println("Response from $finalUrl: \"$responseString\"")
                    responseString
                }
                it.close()

                continuation.resume(if (it.code == 200) fileId else null)
            }
        }
    }
}