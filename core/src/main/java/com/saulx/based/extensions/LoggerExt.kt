package com.saulx.based.extensions

import org.slf4j.Logger

fun Logger.logPayload(funcName:String, payload:String) {
    debug("$funcName :: sending $payload")
}

fun Logger.logDataResponse(funcName: String, data:String) {
    debug("$funcName :: callback data $data")
}