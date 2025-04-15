package com.saulx.based.exceptions

class CallbackException(message: String) : RuntimeException(message)

data class BasedClientParseException(val payload: String, val error:Throwable) : Exception() {
    override val message: String?
        get() = "Failed to parse payload $payload"
}