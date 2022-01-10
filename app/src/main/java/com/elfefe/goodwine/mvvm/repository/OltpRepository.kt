package com.elfefe.goodwine.mvvm.repository

import com.elfefe.goodwine.oltp.OltpDatabase
import com.elfefe.goodwine.oltp.parcelable.Bottle
import com.elfefe.goodwine.oltp.parcelable.entity
import com.elfefe.goodwine.oltp.parcelable.parcel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class OltpRepository {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val db = OltpDatabase.instance

    private val _bottleFlow = MutableStateFlow<List<Bottle>>(listOf())
    val bottleFlow: StateFlow<List<Bottle>>
        get() = _bottleFlow

    init {
        db.bottleDao().getAll().onEach {
            _bottleFlow.value = it.map { bottle -> bottle.parcel() }
        }.launchIn(scope)
    }

    fun updateBottlesRatingOrder(asc: Boolean) {
        if (asc) db.bottleDao().getByRatinAsc().onEach {
            _bottleFlow.value = it.map { bottle -> bottle.parcel() }
        }.launchIn(scope)
        else db.bottleDao().getByRatingDesc().onEach {
            _bottleFlow.value = it.map { bottle -> bottle.parcel() }
        }.launchIn(scope)
    }

    fun updateBottlesDateOrder(asc: Boolean) {
        if (asc) db.bottleDao().getByDateAsc().onEach {
            _bottleFlow.value = it.map { bottle -> bottle.parcel() }
        }.launchIn(scope)
        else db.bottleDao().getByDateDesc().onEach {
            _bottleFlow.value = it.map { bottle -> bottle.parcel() }
        }.launchIn(scope)
    }

    fun saveBottle(bottle: Bottle) {
        scope.launch {
            db.bottleDao().insertAll(bottle.entity())
        }
    }
}