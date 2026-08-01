package com.elfefe.goodwine.oltp

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.elfefe.goodwine.oltp.dao.BottleDao
import com.elfefe.goodwine.oltp.entities.Bottle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Le tri de la cave est écrit en SQL : seule une vraie base peut dire s'il fait ce qu'on croit.
 * Base en mémoire, donc aucun état partagé entre deux tests et rien à nettoyer sur l'appareil.
 */
@RunWith(AndroidJUnit4::class)
class BottleDaoTest {

    private lateinit var db: OltpDatabase
    private lateinit var dao: BottleDao

    private val chinon = Bottle(1, date = 300L, picture = "a.png", description = "Chinon", rating = 2f)
    private val margaux = Bottle(2, date = 100L, picture = "b.png", description = "Margaux", rating = 5f)
    private val cahors = Bottle(3, date = 200L, picture = "c.png", description = "Cahors", rating = 4f)

    @Before
    fun ouvrir() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            OltpDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.bottleDao()
        dao.insertAll(chinon, margaux, cahors)
    }

    @After
    fun fermer() = db.close()

    @Test
    fun toutes_les_bouteilles_sont_relues() = runBlocking {
        assertEquals(3, dao.getAll().first().size)
    }

    @Test
    fun le_tri_par_note_croissante_place_la_moins_bonne_en_tete() = runBlocking {
        assertEquals(listOf(2f, 4f, 5f), dao.getByRatingAsc().first().map { it.rating })
    }

    @Test
    fun le_tri_par_note_decroissante_place_la_meilleure_en_tete() = runBlocking {
        assertEquals(listOf(5f, 4f, 2f), dao.getByRatingDesc().first().map { it.rating })
    }

    @Test
    fun le_tri_par_date_croissante_place_la_plus_ancienne_en_tete() = runBlocking {
        assertEquals(listOf(100L, 200L, 300L), dao.getByDateAsc().first().map { it.date })
    }

    @Test
    fun le_tri_par_date_decroissante_place_la_plus_recente_en_tete() = runBlocking {
        assertEquals(listOf(300L, 200L, 100L), dao.getByDateDesc().first().map { it.date })
    }

    @Test
    fun reenregistrer_une_bouteille_connue_la_remplace_au_lieu_de_lever() = runBlocking {
        // C'est ce que fait la synchronisation à chaque passage. Sans OnConflictStrategy.REPLACE,
        // l'insertion levait une SQLiteConstraintException.
        dao.insertAll(chinon.copy(description = "Chinon 2019", rating = 3f))

        val relues = dao.getAll().first()
        assertEquals(3, relues.size)
        assertEquals("Chinon 2019", relues.first { it.id == 1 }.description)
        assertEquals(3f, relues.first { it.id == 1 }.rating)
    }

    @Test
    fun une_bouteille_supprimee_disparait_de_la_cave() = runBlocking {
        dao.delete(margaux)

        assertEquals(listOf(1, 3), dao.getAll().first().map { it.id })
    }

    @Test
    fun une_bouteille_se_retrouve_par_son_identifiant() = runBlocking {
        assertEquals("Cahors", dao.getById(3).first().single().description)
    }

    @Test
    fun un_identifiant_inconnu_ne_rend_rien() = runBlocking {
        assertEquals(emptyList<Bottle>(), dao.getById(404).first())
    }
}
