package com.elfefe.goodwine.mvvm

import com.elfefe.goodwine.oltp.parcelable.Bottle

/**
 * Règles de synchronisation, isolées du SDK Firebase pour être testables sans appareil.
 *
 * Elles vivaient à l'intérieur des callbacks de [com.elfefe.goodwine.mvvm.repository.FirebaseRepository],
 * mêlées aux appels réseau : impossible d'écrire le moindre test dessus, alors que c'est là que
 * se trouvaient les deux plantages relevés au diagnostic.
 */
object BottleSync {

    /** Firestore refuse plus de 30 valeurs dans un `whereNotIn`. */
    const val WHERE_NOT_IN_LIMIT = 30

    /** Ce que le serveur peut renvoyer pour une fiche, sans dépendance à Firestore. */
    data class RemoteBottle(
        val id: String,
        val date: Long?,
        val picture: String?,
        val description: String?,
        val rating: Double?
    )

    /**
     * Vrai quand la requête peut être filtrée côté serveur.
     *
     * Une liste vide faisait échouer la requête — cas d'une première installation, donc de
     * chaque nouvel utilisateur. Au-delà de la limite de Firestore, on rapatrie tout et on
     * filtre côté téléphone.
     */
    fun canFilterServerSide(knownIds: List<Int>): Boolean =
        knownIds.isNotEmpty() && knownIds.size <= WHERE_NOT_IN_LIMIT

    /**
     * Convertit une fiche distante, ou rend null si elle est inexploitable.
     *
     * Un identifiant non numérique faisait lever `toInt()` et perdait toute la synchronisation,
     * pas seulement le document fautif.
     */
    fun toBottle(remote: RemoteBottle): Bottle? {
        val id = remote.id.toIntOrNull() ?: return null
        return Bottle(
            id = id,
            date = remote.date ?: 0L,
            picture = remote.picture.orEmpty(),
            description = remote.description.orEmpty(),
            rating = (remote.rating ?: 0.0).toFloat()
        )
    }

    /** Ne garde que ce que le téléphone n'a pas déjà, en ignorant les fiches illisibles. */
    fun missingBottles(remotes: List<RemoteBottle>, knownIds: List<Int>): List<Bottle> {
        val known = knownIds.toSet()
        return remotes.mapNotNull(::toBottle).filter { it.id !in known }
    }

    /** Chemin de la photo d'une bouteille dans Storage. */
    fun picturePath(userId: String, bottleId: Int) = "bottles/$userId/$bottleId.png"

    /**
     * Vrai si la photo doit être téléversée : une fiche dont l'image est déjà une URL distante
     * — cas d'une fiche rapatriée du serveur — n'a rien à renvoyer.
     */
    fun needsUpload(picture: String): Boolean =
        picture.isNotBlank() && !picture.startsWith("http://") && !picture.startsWith("https://")
}
