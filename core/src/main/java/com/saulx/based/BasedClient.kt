package com.saulx.based

import com.saulx.based.exceptions.CallbackException
import com.saulx.based.extensions.logDataResponse
import com.saulx.based.extensions.logPayload
import com.saulx.based.model.AuthState
import com.saulx.based.model.FileUploadOptions
import com.sun.jna.CallbackThreadInitializer
import com.sun.jna.Native
import com.sun.jna.NativeLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class BasedClient(private val enableTLS: Boolean = false) : DisposableHandle {

    private val getMap: ConcurrentHashMap<Int, BasedLibrary.GetCallback> = ConcurrentHashMap()
    private val functionMap: ConcurrentHashMap<Int, BasedLibrary.GetCallback> = ConcurrentHashMap()

    private val clientId = libraryInstance.Based__new_client(enableTLS)
    private val clusterUrl = "production"
    private val logger = LoggerFactory.getLogger(this::class.java)
    private var connectionInfo: ConnectionInfo? = null
    private var authState: String? = null

    fun connect(url: String) {
        libraryInstance.Based__connect_to_url(clientId, url)
    }

    suspend fun auth(state: AuthState): String {
        return auth(state.token)
    }

    suspend fun auth(state: String): String {
        val objState = """{ "token": "$state" }"""
        return withContext(Dispatchers.IO) {
            suspendCoroutine {
                logger.logPayload("auth", objState)
                val callback =
                    object : BasedLibrary.AuthCallback {
                        override fun invoke(data: String) {
                            logger.logDataResponse("auth", data)
                            authState = state
                            it.resume(data)
                        }
                    }

                Native.setCallbackThreadInitializer(callback, callbackInitializer)
                (Native.synchronizedLibrary(libraryInstance) as BasedLibrary).Based__set_auth_state(
                    clientId,
                    objState,
                    callback
                )
            }
        }
    }

    suspend fun get(name: String, payload: String): String {
        return suspendCoroutine {
            var index = 0
            val callback = object : BasedLibrary.GetCallback {
                override fun invoke(data: String, error: String) {
                    getMap.remove(index)
                    if (error.isEmpty()) {
                        it.resume(data)
                    } else {
                        it.resumeWithException(CallbackException("Error occurred in $name callback: $error"))
                    }
                    logger.logDataResponse(name, data)
                }
            }
            Native.setCallbackThreadInitializer(callback, callbackInitializer)
            logger.logPayload(name, payload)
            index = (Native.synchronizedLibrary(libraryInstance) as BasedLibrary).Based__get(
                clientId,
                name,
                payload,
                callback
            )
            getMap[index] = callback
        }
    }

    suspend fun function(name: String, payload: String): String {
        return suspendCoroutine {
            var index = 0
            val callback = object :
                BasedLibrary.GetCallback {
                override fun invoke(data: String, error: String) {
                    functionMap.remove(index)
                    if (error.isEmpty()) {
                        it.resume(data)
                    } else {
                        it.resumeWithException(CallbackException("Error occurred in $name callback: $error"))
                    }
                    logger.logDataResponse(name, data)
                }
            }
            Native.setCallbackThreadInitializer(callback, callbackInitializer)
            logger.logPayload(name, payload)
            index = (Native.synchronizedLibrary(libraryInstance) as BasedLibrary).Based__call(
                clientId,
                name,
                payload,
                callback
            )
            functionMap[index] = callback
        }
    }

    fun unobserve(id: Int) {
        libraryInstance.Based__unobserve(clientId, id)
    }

    fun disconnect() {
        libraryInstance.Based__disconnect(clientId)
    }

    fun connect(
        cluster: String = clusterUrl,
        org: String,
        project: String,
        env: String,
        name: String = "@based/env-hub",
        key: String = "",
        optional_key: Boolean = false,
        host: String = "",
        discoverUrl: String = ""
    ) {
        logger.debug("Based__connect with: org:$org, project:$project, env:$env, name:$name, discoverUrl:$discoverUrl, TLS:$enableTLS")
        this.connectionInfo = ConnectionInfo(org, project, env)
        libraryInstance.Based__connect(
            clientId,
            cluster,
            org,
            project,
            env,
            name,
            key,
            optional_key,
            host,
            discoverUrl
        )
    }

    fun observe(name: String, payload: String): Flow<String> {
        var callback: BasedLibrary.ObserveCallback
        return callbackFlow {
            logger.logPayload(name, payload)
            callback = object :
                BasedLibrary.ObserveCallback {
                override fun invoke(
                    data: String,
                    checksum: NativeLong,
                    error: String,
                    subId: Int
                ) {
                    if (error.isEmpty()) {
                        trySend(data)
                    } else {
                        logger.error("got exception: $error")
                    }

                }
            }

            val observerId: Int = libraryInstance.Based__observe(clientId, name, payload, callback)

            awaitClose {
                unobserve(observerId)
            }
        }
    }

    suspend fun file(fileOptions: FileUploadOptions): String {
        logger.debug("Start creating the file: {}, {}", fileOptions.fileName, connectionInfo)
        return connectionInfo?.let {
            val serverUrl = libraryInstance.Based__get_service(
                clientId,
                clusterUrl,
                it.org,
                it.project,
                it.env,
                "@based/env-hub",
                "",
                false,
                html = true
            )
            val targetUrl = "${serverUrl}/upload-file"
            logger.debug("Try to upload the file: $targetUrl")
            FileUploader.upload(fileOptions, targetUrl, authState ?: "")
        } ?: run {
            throw IllegalStateException("Connection info is null, cannot upload file.")
        }
    }

    override fun dispose() {
        libraryInstance.Based__delete_client(clientId)
    }

    private data class ConnectionInfo(
        val org: String,
        val project: String,
        val env: String
    )

    companion object {
        private val libraryInstance: BasedLibrary = Native.load("based", BasedLibrary::class.java)
        val callbackInitializer: CallbackThreadInitializer =
            CallbackThreadInitializer(true, true, "basedCallbacks")
    }
}