# GoodWine — diagnostic de l'état réel du build

État constaté le **2026-08-01** sur un clone neuf de `elfefe/GoodWine`
(dernier commit `278385f`, « Added upload to firebase. »).
Aucune correction n'a été appliquée à ce stade : ce document ne fait que consigner les faits.

Ticket Jira : **GEN-83**.

## 1. Versions en présence

| Élément | Version au dépôt | Publiée en |
|---|---|---|
| Wrapper Gradle | 7.3 | novembre 2021 |
| Android Gradle Plugin | **7.2.0-alpha07** | janvier 2022 |
| Kotlin | 1.6.0 | novembre 2021 |
| Compose (BOM manuel `compose_version`) | 1.1.0-rc01 | décembre 2021 |
| Compose Material3 | 1.0.0-**alpha03** | janvier 2022 |
| `compileSdk` / `targetSdk` / `minSdk` | 31 / 31 / 24 | Android 12 |
| Firebase BOM | 29.0.3 | décembre 2021 |
| CameraX | 1.0.2 (`camera-view` en 1.0.0-alpha32) | 2021 |
| Room | 2.4.1 | janvier 2022 |
| `sourceCompatibility` / `jvmTarget` | 1.8 | — |

Le build s'appuie sur une **version alpha d'AGP**, jamais stabilisée. Le wrapper Gradle date du
`Fri Jan 07 20:20:22 CET 2022` (horodatage du fichier `gradle-wrapper.properties`).

## 2. Blocages, dans l'ordre où on les rencontre

### 2.1 Gradle 7.3 est incompatible avec un JDK récent — BLOQUANT

```
$ JAVA_HOME=<corretto-21.0.4> ./gradlew assembleDebug --no-daemon

FAILURE: Build failed with an exception.

* What went wrong:
Could not compile settings file 'X:\Projects\GoodWine\settings.gradle'.
> startup failed:
  General error during conversion: Unsupported class file major version 65

  java.lang.IllegalArgumentException: Unsupported class file major version 65
```

`major version 65` = classes JDK 21. Gradle 7.3 ne va pas au-delà du JDK 17.
Le build **ne démarre pas** sur une machine dont le JDK par défaut est récent.

### 2.2 Sur JDK 17, le SDK Android doit être localisé

```
$ JAVA_HOME=<jbr-17.0.12> ./gradlew assembleDebug --no-daemon

* What went wrong:
Could not determine the dependencies of task ':app:compileDebugJavaWithJavac'.
> SDK location not found. Define location with an ANDROID_SDK_ROOT environment variable
  or by setting the sdk.dir path in your project's local properties file
  at 'X:\Projects\GoodWine\local.properties'.
```

Comportement normal (`local.properties` est ignoré par git). Non bloquant, mentionné pour
l'ordre des étapes.

Attention au format : un `sdk.dir` écrit avec des antislashs simples
(`sdk.dir=C\:\Users\…`) est lu comme une séquence d'échappement invalide et produit un
`java.io.IOException: La syntaxe du nom de fichier, de répertoire ou de volume est incorrecte`
qu'on impute facilement à autre chose. Écrire des slashs : `sdk.dir=C:/Users/…`.

### 2.3 Le `signingConfig` de debug pointe vers un chemin qui n'existe pas — BLOQUANT

```
* What went wrong:
Execution failed for task ':app:validateSigningDebug'.
> Keystore file 'X:\Projects\GoodWine\app\home\celadodc-rswl.com\felix.boulereiff\Certs\Android\goodwine\goodwine.jks'
  not found for signing config 'debug'.
```

Le chemin Linux absolu est réinterprété comme relatif au module `app/`, d'où ce chemin composite.

Origine, dans `app/build.gradle` :

```groovy
signingConfigs {
    debug {
        storeFile file('/home/celadodc-rswl.com/felix.boulereiff/Certs/Android/goodwine/goodwine.jks')
        storePassword '***SECRET-PURGE-2026-08-01***'
        keyPassword  '***SECRET-PURGE-2026-08-01***'
        keyAlias     'goodwine'
    }
}
```

Chemin absolu d'un poste Linux d'une ancienne entreprise. Le build **ne peut aboutir sur
aucune autre machine**, y compris la machine actuelle. Ce `signingConfig` est de plus appliqué au
`defaultConfig` (donc à toutes les variantes, release comprise).

**Ce bloc contient aussi un mot de passe de keystore en clair, versionné dans un dépôt public
depuis quatre ans.** Il est resté invisible pour gitleaks lors de l'audit du 2026-07-27 (aucune
règle ne couvre `storePassword` dans un `.gradle`). Voir la section 5.

### 2.4 `google-services.json` est absent — BLOQUANT une fois 2.3 corrigé

Le fichier a été **supprimé de tout l'historique le 2026-07-27** dans le cadre de la purge de
secrets (GEN-5). Les plugins `com.google.gms.google-services` et
`com.google.firebase.crashlytics` sont pourtant toujours appliqués : le build échouera à la
tâche `processDebugGoogleServices` tant qu'un fichier n'aura pas été fourni.

Trois issues possibles, à trancher dans GEN-84 :
retélécharger le fichier depuis la console Firebase et le laisser hors dépôt ; le remplacer par
un gabarit `google-services.json.example` ; ou retirer Firebase du projet.

### 2.5 Dépendances mortes ou à risque

- `com.facebook.android:facebook-android-sdk:latest.release` — une version **dynamique**. Le build
  n'est pas reproductible : il change de dépendance à chaque résolution. La version courante du SDK
  exige `minSdk 21+`, un `compileSdk` récent et une configuration différente de celle du code
  (l'API `LoginResult`/`AccessToken` a changé entre les versions 12 et 18).
- `com.google.android.gms:play-services-safetynet:18.0.1` — **API dépréciée par Google**, arrêtée au
  profit de Play Integrity. Aucune ligne du code ne s'en sert : à retirer purement et simplement.
- `com.google.android.gms.auth.api.credentials.Credential` (importé dans `FirebaseRepository.kt:25`)
  — **Smart Lock for Passwords, supprimé** des Play services. Import mort : jamais utilisé dans le
  fichier.
- `com.facebook.share.widget.LikeView` (importé dans `MainActivity.kt:91`) — composant **retiré du
  SDK Facebook**. Import mort lui aussi.
- `androidx.camera:camera-view:1.0.0-alpha32` alors que le reste de CameraX est en 1.0.2 :
  versions désaccordées.
- Compose Material3 en `1.0.0-alpha03` : API très éloignée de la version stable actuelle.
- `annotationProcessor` **et** `kapt` déclarent tous deux `room-compiler` : doublon.

Aucun dépôt `jcenter()` n'est déclaré — ni dans `build.gradle`, ni dans `settings.gradle`, qui
utilisent `google()` et `mavenCentral()`. Sur ce point le ticket GEN-84 partait d'une hypothèse
fausse : **il n'y a rien à faire**.

### 2.6 Ce qui cassera à la montée vers AGP 8

- `package="com.elfefe.goodwine"` est déclaré dans `AndroidManifest.xml`. AGP 8 exige un
  `namespace` dans `app/build.gradle` et refuse l'attribut du manifeste.
- `task clean(type: Delete)` dans `build.gradle` racine : syntaxe supprimée par Gradle 9,
  dépréciée avant.
- `jvmTarget 1.8` avec un JDK 21 : à porter à 17 au minimum.
- `kapt` est à remplacer par KSP pour Room (kapt freine et se marie mal avec Kotlin 2.x).

## 3. Ce que fait réellement l'application

1 746 lignes de Kotlin, dont **817 dans le seul `MainActivity.kt`** : toute l'interface y tient.

Architecture : un `Mediator` unique instancié par `BaseApplication`, trois dépôts
(`CameraRepository`, `FirebaseRepository`, `OltpRepository`) et quatre ViewModels qui passent tous
par ce médiateur.

Écrans, dans l'ordre des composables de `MainActivity` :

| Composable | Rôle |
|---|---|
| `Loading` | écran d'attente |
| `Content` | aiguillage tutoriel / principal |
| `Tutorial` | tutoriel au premier lancement (drapeau `FIRST_USE_TAG` en SharedPreferences) |
| `Main` | conteneur |
| `Front` | façade, gestes de glissement |
| `Options` | barre d'options : tri par note, tri par date, mode d'affichage |
| `FloatingButton` | bouton d'action |
| `Bottle` | fiche bouteille : photo, description, note (200 lignes) |
| `Camera` | prise de vue CameraX via `AndroidViewBinding` sur `camera_view.xml` |

Fonctions réellement implémentées :

- **Prise de photo** d'une étiquette avec CameraX, conversion YUV → JPEG → Bitmap
  (`Image.toBitmap()` dans `extensions.kt`), écriture dans le stockage interne.
- **Fiche bouteille** : date, chemin de la photo, description, note de 0 à 5
  (bibliothèque `compose-ratingbar`).
- **Stockage local Room** (`oltp.db`, une seule table `Bottle`, version 1, aucune migration)
  avec tri par note et par date, ascendant et descendant.
- **Dictée vocale** de la description via `SpeechRecognizer` (`RecognizerIntent`).
- **Authentification Firebase** : anonyme, e-mail/mot de passe, Google, Facebook, et par
  téléphone — cette dernière lit le numéro via `TelephonyManager.line1Number`, ce qui exige
  `READ_PHONE_NUMBERS` + `READ_PHONE_STATE` et **renvoie null sur la quasi-totalité des
  téléphones actuels**.
- **Synchronisation Firestore** : collection `Database/{email}/Bottle`.

## 4. Défauts fonctionnels repérés à la lecture

Ils relèvent de GEN-86, pas de la remise en état du build.

1. **`syncData()` plante sur une base vide.** `whereNotIn` refuse une liste vide côté Firestore, et
   `it.id.toInt()` lève une exception si un document n'a pas un identifiant numérique.
2. **Les photos ne sont pas synchronisées.** `Bottle.picture` contient un chemin de fichier local
   (`/data/data/…`) envoyé tel quel dans Firestore : il ne veut rien dire sur un autre appareil.
   `firebase-storage` est déclaré en dépendance mais **n'est jamais appelé** — le dernier commit
   s'intitule pourtant « Added upload to firebase ».
3. **`FirebaseViewmodel.syncBottles()` est vide** : `fun syncBottles() {}`.
4. **Aucune suppression de bouteille depuis l'interface**, alors que `BottleDao.delete()` existe.
5. **`connectPhone` s'appuie sur `line1Number`**, non renseigné depuis Android 10 sauf cas rares.
6. **Ordre d'initialisation fragile** : `extensions.kt` déclare `val app = BaseApplication.instance`
   au niveau fichier, alors que `instance` est un `lateinit` affecté dans `onCreate`. Cela ne tient
   que parce que `instance = this` précède `Mediator()` d'une ligne. Toute réorganisation de
   `onCreate` provoque une `UninitializedPropertyAccessException` au démarrage.
7. **`onActivityResult` est déprécié** et `FirebaseRepository.onFacebookResult` déréférence
   `facebookCallback`, un `lateinit` qui n'est affecté que si `connectFacebook` a été appelé
   auparavant — sinon plantage.
8. **Aucune gestion du refus de permission** : `askPermission` se rappelle en boucle.

## 5. Sécurité — un point qui n'était pas dans le périmètre initial

Le mot de passe du keystore **`***SECRET-PURGE-2026-08-01***`** figure en clair dans `app/build.gradle`, présent
dans un dépôt public depuis quatre ans, et **encore dans l'arbre de travail aujourd'hui**. L'audit
gitleaks du 2026-07-27 ne l'a pas remonté (aucune règle sur `storePassword`).

Le fichier `.jks` lui-même n'a jamais été versionné, ce qui limite la portée : sans le keystore, le
mot de passe seul ne permet pas de signer. Mais il ressemble aux mots de passe personnels employés
ailleurs, ce qui en fait le vrai risque.

À traiter : sortir la valeur du fichier (variables d'environnement ou `keystore.properties` hors
dépôt), purger l'historique comme cela a été fait pour les neuf autres dépôts, et **ne plus
réutiliser ce mot de passe ailleurs**.

Sont en revanche **publics par conception** et ne demandent aucune action :
`facebook_app_id` (3059236311018256) et le `client_id` OAuth Google dans `res/values/strings.xml`.

## 6. Hygiène du dépôt

- `README.md` fait **36 octets** : « # GoodWine / Save this great wine ! ».
- `.idea/` est versionné, alors que `.gitignore` n'en exclut que quelques fichiers.
- Les deux seuls tests sont les gabarits d'Android Studio : `assertEquals(4, 2 + 2)` et
  la vérification du nom de paquet. **Aucun test réel.**
- Aucun workflow GitHub Actions.
- Licence GPL-3.0 présente.
- `app/src/main/ic_goodwine-playstore.png` traîne à la racine de `src/main`, hors de `res/`.
- Le dépôt jumeau `GoodWineWebsite` est **vide** : la politique de confidentialité qu'exigeait le
  Play Store n'a jamais été écrite, ce qui explique probablement l'abandon de la publication.

## 7. Verdict

Trois blocages empêchent le moindre build : la version de Gradle, le chemin du keystore, et le
`google-services.json` absent. Aucun n'est difficile — c'est le portage vers AGP 8 et Compose
stable qui représente le vrai travail, avec les API Facebook et CameraX qui ont changé entre-temps.

Le cœur métier (Room, tri, prise de vue, notation) est en revanche complet et cohérent. Ce qui
manque est la synchronisation réelle des photos, annoncée par le dernier commit mais jamais écrite.
