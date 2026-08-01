package com.elfefe.goodwine.oltp

import com.elfefe.goodwine.oltp.parcelable.Bottle
import com.elfefe.goodwine.oltp.parcelable.entity
import com.elfefe.goodwine.oltp.parcelable.parcel
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * L'app manipule deux Bottle homonymes — l'entité Room et le parcelable de l'interface — et
 * passe de l'une à l'autre à chaque lecture comme à chaque écriture. Une inversion de champs
 * dans ces conversions serait silencieuse : les types se ressemblent trop.
 */
class BottleMappingTest {

    private val reference = Bottle(
        id = 12,
        date = 1_700_000_000_000L,
        picture = "/data/photo.png",
        description = "Saint-Emilion 2018",
        rating = 3.5f
    )

    @Test
    fun `un aller-retour entre parcelable et entite ne perd rien`() {
        assertEquals(reference, reference.entity().parcel())
    }

    @Test
    fun `chaque champ arrive bien a sa place dans l entite`() {
        val entity = reference.entity()

        assertEquals(reference.id, entity.id)
        assertEquals(reference.date, entity.date)
        assertEquals(reference.picture, entity.picture)
        assertEquals(reference.description, entity.description)
        assertEquals(reference.rating, entity.rating)
    }

    @Test
    fun `une bouteille neuve porte l identifiant zero que Room remplacera`() {
        // L'écran d'ajout construit la fiche sans identifiant : c'est ce zéro qui déclenche
        // l'auto-incrément côté Room.
        val neuve = Bottle(date = 1L, picture = "p", description = "d", rating = 1f)

        assertEquals(0, neuve.id)
        assertEquals(0, neuve.entity().id)
    }
}
