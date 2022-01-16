package com.elfefe.goodwine.utils.enums

import com.google.firebase.auth.FirebaseUser

sealed interface Connection {
    object Success: Connection
    object Connecting: Connection
    class Failure(val e: Exception): Connection
}