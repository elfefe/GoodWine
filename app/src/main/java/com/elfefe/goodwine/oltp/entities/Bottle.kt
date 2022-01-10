package com.elfefe.goodwine.oltp.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Bottle(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: Long,
    val picture: String,
    val description: String,
    val rating: Float
)
