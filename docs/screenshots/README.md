# Parcours principal, vérifié le 2026-08-01

Captures prises sur un émulateur **Android 14 (API 34)**, caméra arrière en scène virtuelle,
build de debug sans configuration Firebase (`BuildConfig.FIREBASE_ENABLED = false`).
Elles constituent la preuve de fin demandée par le ticket **GEN-86**.

| Capture | Étape |
|---|---|
| `01-demarrage.png` | Premier lancement, demande d'accès à la caméra |
| `02-apres-permission.png` | Formulaire d'ajout — la zone caméra est encore noire |
| `03-camera.png` | Même écran : l'aperçu ne démarrait pas, `startCamera()` partait avant que le `PreviewView` n'existe |
| `04-camera-active.png` | Après correction : l'aperçu s'affiche |
| `05-photo-prise.png` | Photo capturée |
| `06-saisie.png` | Description dictée au clavier, note de 3 étoiles, bouton de validation apparu |
| `07-bouteille-enregistree.png` | Fiche enregistrée et affichée dans la cave — texte et icône illisibles, le thème dynamique d'Android 12+ écrasait la palette |
| `08-cave-lisible.png` | Après correction du thème : palette bordeaux, texte lisible, corbeille visible. La fiche a survécu à un redémarrage complet de l'app |
| `09-apres-suppression.png` | Suppression effective, sans plantage |

Aucune exception au journal (`logcat -s AndroidRuntime:E`) sur l'ensemble du parcours.

Deux captures montrent l'état **avant** correction (`03` et `07`) : elles sont conservées
exprès, pour que le défaut et sa correction soient tous deux visibles.
