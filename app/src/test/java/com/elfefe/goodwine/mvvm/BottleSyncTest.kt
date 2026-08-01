package com.elfefe.goodwine.mvvm

import com.elfefe.goodwine.mvvm.BottleSync.RemoteBottle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ces tests couvrent les deux plantages relevés au diagnostic, plus les règles qui décident
 * de ce qui part sur le réseau. Aucun accès à Firestore : [BottleSync] ne connaît que des
 * types du projet.
 */
class BottleSyncTest {

    // --- choix de la requête -------------------------------------------------------------

    @Test
    fun `une cave vide interdit le filtrage serveur`() {
        // Firestore rejette un whereNotIn sans valeur : c'était le cas de toute première
        // installation, donc de chaque nouvel utilisateur.
        assertFalse(BottleSync.canFilterServerSide(emptyList()))
    }

    @Test
    fun `une cave de taille courante autorise le filtrage serveur`() {
        assertTrue(BottleSync.canFilterServerSide(listOf(1, 2, 3)))
    }

    @Test
    fun `le filtrage serveur s arrete a la limite de Firestore`() {
        val pile = (1..BottleSync.WHERE_NOT_IN_LIMIT).toList()
        assertTrue(BottleSync.canFilterServerSide(pile))
        assertFalse(BottleSync.canFilterServerSide(pile + 999))
    }

    // --- lecture d'une fiche distante ----------------------------------------------------

    @Test
    fun `une fiche complete est convertie telle quelle`() {
        val bottle = BottleSync.toBottle(
            RemoteBottle("42", 1_700_000_000_000L, "photo.png", "Chateau Margaux 2015", 4.5)
        )

        assertEquals(42, bottle?.id)
        assertEquals(1_700_000_000_000L, bottle?.date)
        assertEquals("photo.png", bottle?.picture)
        assertEquals("Chateau Margaux 2015", bottle?.description)
        assertEquals(4.5f, bottle?.rating)
    }

    @Test
    fun `un identifiant non numerique est ignore sans lever`() {
        // toInt() levait ici, et l'exception remontait dans le callback Firestore : c'est
        // toute la synchronisation qui tombait, pas seulement ce document.
        assertNull(BottleSync.toBottle(RemoteBottle("brouillon", 0L, "", "", 0.0)))
    }

    @Test
    fun `les champs absents prennent des valeurs neutres`() {
        val bottle = BottleSync.toBottle(RemoteBottle("7", null, null, null, null))

        assertEquals(7, bottle?.id)
        assertEquals(0L, bottle?.date)
        assertEquals("", bottle?.picture)
        assertEquals("", bottle?.description)
        assertEquals(0f, bottle?.rating)
    }

    // --- fusion --------------------------------------------------------------------------

    @Test
    fun `seules les fiches inconnues du telephone sont rapatriees`() {
        val remotes = listOf(
            RemoteBottle("1", 0L, "", "deja la", 3.0),
            RemoteBottle("2", 0L, "", "nouvelle", 4.0)
        )

        val manquantes = BottleSync.missingBottles(remotes, knownIds = listOf(1))

        assertEquals(1, manquantes.size)
        assertEquals(2, manquantes.first().id)
    }

    @Test
    fun `une fiche illisible n empeche pas les autres de passer`() {
        val remotes = listOf(
            RemoteBottle("bancal", 0L, "", "illisible", 0.0),
            RemoteBottle("5", 0L, "", "correcte", 2.0)
        )

        val manquantes = BottleSync.missingBottles(remotes, knownIds = emptyList())

        assertEquals(listOf(5), manquantes.map { it.id })
    }

    @Test
    fun `sur une cave vide tout est rapatrie`() {
        val remotes = (1..3).map { RemoteBottle("$it", 0L, "", "", 0.0) }

        assertEquals(3, BottleSync.missingBottles(remotes, emptyList()).size)
    }

    // --- photos --------------------------------------------------------------------------

    @Test
    fun `le chemin de la photo isole chaque utilisateur`() {
        assertEquals("bottles/abc123/42.png", BottleSync.picturePath("abc123", 42))
    }

    @Test
    fun `une photo locale doit etre televersee`() {
        assertTrue(BottleSync.needsUpload("/data/data/com.elfefe.goodwine/files/1700_4.png"))
    }

    @Test
    fun `une photo deja distante n est pas renvoyee`() {
        // Sans cette garde, une fiche rapatriée repartait aussitôt vers Storage à chaque
        // synchronisation, en boucle.
        assertFalse(BottleSync.needsUpload("https://firebasestorage.googleapis.com/v0/b/x/o/y.png"))
        assertFalse(BottleSync.needsUpload("http://exemple.test/y.png"))
    }

    @Test
    fun `une fiche sans photo n a rien a televerser`() {
        assertFalse(BottleSync.needsUpload(""))
        assertFalse(BottleSync.needsUpload("   "))
    }
}
