package com.saulx.based

import com.sun.jna.Native
import com.sun.jna.NativeLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import com.saulx.based.model.AuthState
import com.saulx.based.model.FileUploadOptions
import org.slf4j.LoggerFactory
import java.util.logging.Logger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@ExperimentalCoroutinesApi
class BasedClient : DisposableHandle {

    companion object {
        private val libraryInstance: BasedLibrary = Native.load("based", BasedLibrary::class.java)
    }

    private val clientId = libraryInstance.Based__new_client()
    private val clusterUrl = "production"
    private val logger = LoggerFactory.getLogger(this::class.java)
    private var connectionInfo: ConnectionInfo? = null
    private var authState: String? = null

    private data class ConnectionInfo(
        val org: String,
        val project: String,
        val env: String
    )

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
                println("auth: sending $objState")
                libraryInstance.Based__set_auth_state(clientId, objState, object : BasedLibrary.AuthCallback {
                    override fun invoke(data: String) {
                        println("auth :: callback data '$data'")
                        authState = state
                        it.resume(data)
                    }
                })
            }
        }
    }

    suspend fun get(name: String, payload: String): String {
        return suspendCoroutine {
            println("$name :: sending '$payload'")
            libraryInstance.Based__get(clientId, name, payload, object : BasedLibrary.GetCallback {

                override fun invoke(data: String, error: String) {
                    println("$name :: callback data '$data'. Error '$error'")
                    if (error.isEmpty()) {
                        it.resume(data)
                    } else {
                        it.resumeWithException(RuntimeException(error))
                    }
                }
            })
        }
    }

    suspend fun function(name: String, payload: String): String {
        return suspendCoroutine {
            println("$name :: sending '$payload'")
            libraryInstance.Based__call(clientId, name, payload, object :
                BasedLibrary.GetCallback {
                override fun invoke(data: String, error: String) {
                    println("$name :: callback data '$data'. Error '$error'")
                    if (error.isEmpty()) {
                        it.resume(data)
                    } else {
                        it.resumeWithException(RuntimeException(error))
                    }
                }
            })
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
        optional_key: Boolean = false
    ) {
        this.connectionInfo = ConnectionInfo(org, project, env)
        libraryInstance.Based__connect(clientId, cluster, org, project, env, name, key, optional_key)
    }

    fun observe(name: String, payload: String): Flow<String> {
        return callbackFlow {
            println("$name :: sending '$payload'")
            val observerId =
                libraryInstance.Based__observe(clientId, name, payload, object :
                    BasedLibrary.ObserveCallback {
                    override fun invoke(data: String, checksum: NativeLong, error: String) {
                        if (error.isEmpty()) {
                            trySendBlocking(data)
                        } else {
                            throw RuntimeException(error)
                        }

                    }
                })
            awaitClose { unobserve(observerId) }
        }
    }

    fun observe(payload: String): Flow<String> {
        return callbackFlow {
            val observerId =
                libraryInstance.Based__observe(clientId, "based-db-observe", payload, object :
                    BasedLibrary.ObserveCallback {
                    override fun invoke(data: String, checksum: NativeLong, error: String) {
                        if (error.isEmpty()) {
                            trySendBlocking(data)
                        } else {
                            throw RuntimeException(error)
                        }

                    }
                })
            awaitClose { unobserve(observerId) }
        }
    }

    suspend fun file(fileOptions: FileUploadOptions): String? {
        println("Start creating the file: ${fileOptions.name}, $connectionInfo")
        return connectionInfo?.let {
            val serverUrl = libraryInstance.Based__get_service(clientId, clusterUrl, it.org, it.project, it.env, "@based/env-hub", "", false)
            val targetUrl = "${serverUrl}/db:file-upload"
            println("Try to upload the file: $targetUrl")
            FileUploader.upload(fileOptions, targetUrl, authState ?: "")
        }
    }

    suspend fun set(payload: String): String {
        return suspendCoroutine {
            libraryInstance.Based__call(clientId, "based-db-set", payload, object:
                BasedLibrary.GetCallback {
                override fun invoke(data: String, error: String) {
                    if (error.isEmpty()) {
                        it.resume(data)
                    } else {
                        it.resumeWithException(RuntimeException(error))
                    }
                }
            })
        }
    }

    suspend fun get(payload: String): String {
        return withContext(Dispatchers.IO) {
            logger.info("get: $payload")
            suspendCoroutine {
                libraryInstance.Based__call(clientId, "based-db-get", payload, object :
                    BasedLibrary.GetCallback {
                    override fun invoke(data: String, error: String) {
                        if (error.isEmpty()) {
                            logger.info("get success: $data")
                            it.resume(data)
                        } else {
                            logger.warn("get failed: $error")
                            it.resumeWithException(RuntimeException(error))
                        }
                    }
                })
            }
        }
    }

    override fun dispose() {
        libraryInstance.Based__delete_client(clientId)
    }
}