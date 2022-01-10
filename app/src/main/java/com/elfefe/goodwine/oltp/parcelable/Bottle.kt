package com.elfefe.goodwine.oltp.parcelable

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Bottle(
    val id: Int = 0,
    val date: Long,
    val picture: String,
    val description: String,
    val rating: Float
) : Parcelable


fun Bottle.entity() =
    com.elfefe.goodwine.oltp.entities.Bottle(id, date, picture, description, rating)
fun com.elfefe.goodwine.oltp.entities.Bottle.parcel() =
    Bottle(id, date, picture, description, rating)
