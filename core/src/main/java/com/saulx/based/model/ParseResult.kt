package com.saulx.based.model

sealed class ParseResult<out T> {
    data class Success<T>(val data: T) : ParseResult<T>()
    data class Failure(val error: Throwable, val payload: String) : ParseResult<Nothing>()
}