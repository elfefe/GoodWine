package com.elfefe.goodwine.utils.enums

sealed interface Connection {
    object Success: Connection
    object Connecting: Connection
    class Failure(val e: Exception) : Connection
}