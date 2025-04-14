package com.saulx.based.client.gson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.saulx.based.BasedClient
import com.saulx.based.exceptions.BasedClientParseException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private var gson = GsonBuilder().create()

@ExperimentalCoroutinesApi
fun BasedClient.useGson(gsonInstance: Gson) {
    gson = gsonInstance
}

@ExperimentalCoroutinesApi
suspend fun BasedClient.get(name: String, payload: JsonElement?): JsonElement {
    return gson.fromJson(this.get(name, payload?.let { gson.toJson(payload) } ?: ""),
        JsonElement::class.java)
}

@ExperimentalCoroutinesApi
suspend fun <T> BasedClient.get(name: String, payload: Any?, returnType: Class<T>): T {
    return gson.fromJson(this.get(name, payload?.let { gson.toJson(payload) } ?: ""), returnType)
}

@ExperimentalCoroutinesApi
suspend fun BasedClient.function(name: String, payload: JsonElement?): JsonElement {
    return gson.fromJson(this.function(name, payload?.let { gson.toJson(payload) } ?: ""),
        JsonElement::class.java)
}

@ExperimentalCoroutinesApi
suspend fun <T> BasedClient.function(name: String, payload: Any?, returnType: Class<T>): T {
    return gson.fromJson(this.function(name, payload?.let { gson.toJson(payload) } ?: ""),
        returnType)
}

@ExperimentalCoroutinesApi
fun BasedClient.observe(name: String, payload: JsonElement?): Flow<Result<JsonElement>> {
    return this.observe(name, payload?.let { gson.toJson(payload) } ?: "")
        .map { json ->
            try {
                Result.success(gson.fromJson(json, JsonElement::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(BasedClientParseException(json, e))
            }
        }
}

@ExperimentalCoroutinesApi
fun <T> BasedClient.observe(
    name: String,
    payload: Any?,
    returnType: Class<T>
): Flow<Result<T>> {
    return this.observe(name, payload?.let {
        gson.toJson(payload)
    } ?: "")
        .map { json ->
            try {
                Result.success(gson.fromJson(json, returnType))
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(BasedClientParseException(json, e))
            }
        }
}