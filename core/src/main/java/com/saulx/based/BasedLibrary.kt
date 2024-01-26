package com.saulx.based

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.NativeLong

internal interface BasedLibrary: Library {

    interface ObserveCallback: Callback {
        fun invoke(data: String, checksum: NativeLong, error: String, subId: Int)
    }

    interface GetCallback: Callback {
        fun invoke(data: String, error: String)
    }

    interface AuthCallback: Callback {
        fun invoke(data: String)
    }

    interface Dummy: Callback {
        fun invoke(data: Int)
    }

    fun Based__new_client(): Int
    fun Based__delete_client(client_id: Int)

    fun Based__get_service(
        client_id: Int,
        cluster: String,
        org: String,
        project: String,
        env: String,
        name: String,
        key: String,
        optional_key: Boolean,
        html: Boolean
    ): String

    fun Based__connect_to_url(client_id: Int, url: String)
    fun Based__connect(
        client_id: Int,
        cluster: String,
        org: String,
        project: String,
        env: String,
        name: String,
        key: String,
        optional_key: Boolean
    )

    fun Based__disconnect(client_id: Int)
    fun Based__observe(client_id: Int, name: String, payload: String, cb: ObserveCallback): Int

    fun Based__get(client_id: Int, name: String, payload: String, cb: GetCallback): Int

    fun Based__unobserve(client_id: Int, sub_id: Int)
    fun Based__call(client_id: Int, name: String, payload: String, cb: GetCallback): Int

    fun Based__set_auth_state(client_id: Int, state: String, cb: AuthCallback): Int
}