package com.saulx.based.model

import okhttp3.RequestBody
import java.io.File

sealed class FileUploadOptions(
    var mimeType: String? = null,
    var src: String? = null, //url
    var name: String? = null,
    var id: String? = null,
    var url: String? = null //todo (() => Promise<String>)???
)

class ReferenceFileOptions(src: String, name: String?): FileUploadOptions(null, src, name, null, null)

class StringFileUploadOptions(mimeType: String? = null, src: String? = null, name: String? = null, id: String? = null, url: String? = null, var contents: String):
    FileUploadOptions(mimeType, src, name, id, url)

class FileFileUploadOptions(mimeType: String? = null, src: String? = null, name: String? = null, id: String? = null, url: String? = null, var file: File):
    FileUploadOptions(mimeType, src, name, id, url)

abstract class CustomFileUploadOptions(mimeType: String? = null, src: String? = null, name: String? = null, id: String? = null, url: String? = null): FileUploadOptions(mimeType, src, name, id, url) {

    abstract fun toRequestBody(): RequestBody
}