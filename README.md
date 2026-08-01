# GoodWine

[![Android CI](https://github.com/elfefe/GoodWine/actions/workflows/android.yml/badge.svg)](https://github.com/elfefe/GoodWine/actions/workflows/android.yml)

Application Android de cave à vin : on photographie l'étiquette d'une bouteille, on la note,
on la commente — à la voix si l'on veut — et on retrouve sa cave triée par note ou par date.

Écrite en 2021-2022 en Jetpack Compose, remise en état de marche en 2026.

## Ce que fait l'application

- **Prise de vue de l'étiquette** avec CameraX, enregistrée dans le stockage interne.
- **Fiche bouteille** : photo, description, note de 0 à 5 (par demi-étoiles), date d'ajout.
- **Dictée de la description** via la reconnaissance vocale d'Android.
- **Cave locale** en base Room, triable par note ou par date, dans les deux sens.
- **Suppression** d'une fiche, localement et dans le cloud.
- **Comptes Firebase** : anonyme, e-mail, Google, Facebook.
- **Synchronisation Firestore** des fiches, **photo comprise** : l'image part dans Firebase
  Storage et c'est son URL qui est enregistrée.

## Compiler

Il faut un **JDK 21** et le SDK Android (`compileSdk 35`).

```bash
git clone https://github.com/elfefe/GoodWine.git
cd GoodWine
echo "sdk.dir=/chemin/vers/Android/Sdk" > local.properties   # slashs, pas d'antislashs
./gradlew assembleDebug
```

L'APK sort dans `app/build/outputs/apk/debug/`.

Le projet compile **sans configuration Firebase** : dans ce cas les plugins Google Services et
Crashlytics ne sont pas appliqués et `BuildConfig.FIREBASE_ENABLED` vaut `false`. L'application se
construit et s'installe, mais tout ce qui touche aux comptes et à la synchronisation reste
inopérant.

### Activer Firebase

`app/google-services.json` n'est pas versionné (il l'a été par le passé, et a été purgé de
l'historique en juillet 2026). Pour brancher Firebase :

1. créer un projet dans la [console Firebase](https://console.firebase.google.com/) ;
2. y ajouter une application Android de paquet `com.elfefe.goodwine` ;
3. télécharger `google-services.json` et le déposer dans `app/`.

Un gabarit de la structure attendue est fourni : `app/google-services.json.example`.

Les fournisseurs d'authentification (e-mail, Google, Facebook) doivent être activés dans la console,
et l'empreinte SHA-1 de votre clé de signature déclarée pour que la connexion Google fonctionne.

### Signature

Le dépôt ne contient **aucun keystore ni mot de passe**. Les builds de debug utilisent la clé de
debug par défaut d'Android. Pour un build de release, passez par un `keystore.properties` hors
dépôt ou par des variables d'environnement — les deux sont ignorés par `.gitignore`.

## Structure

```
app/src/main/java/com/elfefe/goodwine/
├── BaseApplication.kt          point d'entrée, instancie le Mediator
├── mvvm/
│   ├── Mediator.kt             façade unique entre ViewModels et dépôts
│   ├── repository/             Camera (CameraX), Firebase (auth + Firestore), Oltp (Room)
│   └── viewmodel/              Ui, Camera, Oltp, Firebase
├── oltp/                       base Room : entité Bottle, DAO, base
├── ui/
│   ├── MainActivity.kt         toute l'interface Compose
│   └── theme/
└── utils/
```

### Connexion Facebook

Elle reste inactive tant que `facebook_client_token` n'est pas renseigné dans
`res/values/strings.xml` (console Facebook → Paramètres → Avancé → Jeton client). Depuis sa
version 13, le SDK **lève** au démarrage si ce jeton manque : l'app le détecte et se passe de
Facebook plutôt que de refuser de démarrer.

## Tests

```bash
./gradlew test                        # 16 tests unitaires, sans appareil
./gradlew connectedDebugAndroidTest   # 9 tests Room sur émulateur ou téléphone
```

Les tests unitaires portent sur `BottleSync` — les règles de synchronisation, extraites du SDK
Firebase pour être vérifiables — et sur les conversions entre l'entité Room et le parcelable.
Les tests instrumentés vérifient les tris SQL de la cave, l'écrasement d'une fiche connue et la
suppression, sur une base Room en mémoire.

## Limites connues

Suivies dans Jira (epic GEN-82) :

- L'authentification **par téléphone** attend désormais que l'appelant fournisse le numéro,
  mais **aucun écran ne l'expose** : la fonction existe sans point d'entrée.
- La synchronisation est **manuelle et à sens unique par écran** : elle part à l'ouverture de la
  liste, sans résolution de conflit si la même fiche a changé des deux côtés.
- `MainActivity` fait 900 lignes : toute l'interface y tient, et plusieurs composables posent
  leurs observateurs `LiveData` dans le corps de la composition.
- La reconnaissance vocale n'est **pas testée automatiquement**.

Le détail, avec les messages d'erreur d'origine, est dans [DIAGNOSTIC.md](DIAGNOSTIC.md).

## Licence

GPL-3.0 — voir [LICENSE](LICENSE).
