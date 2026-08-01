package com.elfefe.goodwine.mvvm.repository

import com.elfefe.goodwine.oltp.OltpDatabase
import com.elfefe.goodwine.oltp.parcelable.Bottle
import com.elfefe.goodwine.oltp.parcelable.entity
import com.elfefe.goodwine.oltp.parcelable.parcel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.elfefe.goodwine.oltp.entities.Bottle as BottleEntity

class OltpRepository {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val db = OltpDatabase.instance

    private val _bottleFlow = MutableStateFlow<List<Bottle>>(listOf())
    val bottleFlow: StateFlow<List<Bottle>>
        get() = _bottleFlow

    // Chaque changement de tri ouvrait une collecte de plus sans fermer la précédente : après
    // quelques clics, plusieurs flux écrivaient dans _bottleFlow et l'ordre affiché devenait
    // celui du dernier arrivé, pas celui demandé. On ne garde qu'une collecte à la fois.
    private var collection: Job? = null

    init {
        observe(db.bottleDao().getAll())
    }

    private fun observe(source: Flow<List<BottleEntity>>) {
        collection?.cancel()
        collection = source
            .onEach { bottles -> _bottleFlow.value = bottles.map { it.parcel() } }
            .launchIn(scope)
    }

    fun updateBottlesRatingOrder(asc: Boolean) =
        observe(if (asc) db.bottleDao().getByRatingAsc() else db.bottleDao().getByRatingDesc())

    fun updateBottlesDateOrder(asc: Boolean) =
        observe(if (asc) db.bottleDao().getByDateAsc() else db.bottleDao().getByDateDesc())

    fun saveBottle(bottle: Bottle) {
        scope.launch { db.bottleDao().insertAll(bottle.entity()) }
    }

    fun deleteBottle(bottle: Bottle) {
        scope.launch { db.bottleDao().delete(bottle.entity()) }
    }
}
