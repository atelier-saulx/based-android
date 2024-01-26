package com.saulx.based.model

import okhttp3.RequestBody
import java.io.File

sealed class FileUploadOptions(
    var payload: String?,
    var mimeType: String? = null,
    var fileName: String? = null,
    var serverKey: String? = null,
    var extension: String? = null
)

class StringFileUploadOptions(
    payload: String?,
    mimeType: String? = null,
    serverKey: String? = null,
    extension: String? = null,
    var contents: String
) :
    FileUploadOptions(payload,  mimeType, null, serverKey, extension)

class FileFileUploadOptions(
    payload: String?,
    mimeType: String? = null,
    fileName: String? = null,
    serverKey: String? = null,
    extension: String? = null,
    var file: File
) :
    FileUploadOptions(payload,  mimeType, fileName, serverKey, extension)

abstract class CustomFileUploadOptions(
    payload: String?,
    mimeType: String? = null,
    fileName: String? = null,
    serverKey: String? = null,
    extension: String? = null,
) : FileUploadOptions(payload,  mimeType, fileName, serverKey, extension) {

    abstract fun toRequestBody(): RequestBody
}