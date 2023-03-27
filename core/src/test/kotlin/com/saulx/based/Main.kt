package com.saulx.based

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import com.saulx.based.model.FileFileUploadOptions
import org.junit.jupiter.api.Test
import java.io.File

@ExperimentalCoroutinesApi
class CoreTest {

    @Test
    fun compile() {
        println("run test")
        val client = BasedClient()
        client.connect(org = "airhub", project = "airhub", env = "edge")
//        runBlocking {
//            client.observe("chill", "").take(5).collect {
//                println("got chill $it")
//            }
//        }
        runBlocking {
            delay(1000)
        }

        println("Sending auth")
        runBlocking {
            client.auth("importservice")
        }

        println("file upload test")
        runBlocking {
            client.get("{ \"\$id\": \"root\", \"children\": true }")
        }
        val now = System.currentTimeMillis()
        runBlocking {
            val url = client.file(FileFileUploadOptions(mimeType = "image/png", file = File("src/test/resources/sdkgeneratieflow.png")))
            val end = System.currentTimeMillis()
            println("service url $url in ${end - now}ms")
        }
        client.dispose()
    }
}