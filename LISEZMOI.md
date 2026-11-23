# Alerte Courses (inDrive + Yassir Driver)

Cette application Android lit les demandes de course affichées par **inDrive** et **Yassir Driver**. Quand une course correspond à vos critères, elle vous alerte par un son fort, une vibration, une notification et une annonce vocale.
Elle **n'accepte jamais** de course à votre place et n'envoie aucune donnée hors du téléphone.

Critères par défaut :
- prix d'au moins **1000 DA** ;
- client à **15 km** maximum ;
- destination à Alger Centre, Hydra ou dans l'ouest d'Alger (liste modifiable dans l'application).

---

## 1. Obtenir le fichier APK

### Option A : sans rien installer, avec GitHub (gratuit)
1. Créez un compte sur github.com, puis un **nouveau dépôt** (« New repository »), en privé.
2. Sur la page du dépôt, cliquez sur « uploading an existing file » et glissez **tout le contenu** de ce dossier, y compris le dossier `.github`. Validez avec « Commit changes ».
   - Si le dossier `.github` n'est pas envoyé (dossier masqué), créez le fichier `.github/workflows/build-apk.yml` via « Add file → Create new file » et copiez-y son contenu.
3. Ouvrez l'onglet **Actions**. La construction démarre automatiquement et dure environ 3 à 5 minutes.
4. Cliquez sur la construction terminée (coche verte), puis téléchargez **AlerteCourses-apk** en bas de la page. Le fichier `app-debug.apk` se trouve dans le zip.

### Option B : avec Android Studio sur PC
1. Installez Android Studio, puis « File → Open » et choisissez ce dossier.
2. Attendez la fin de la synchronisation, puis « Build → Build App Bundle(s) / APK(s) → Build APK(s) ».
3. L'APK se trouve dans `app/build/outputs/apk/debug/app-debug.apk`.

## 2. Installer et activer sur le téléphone
1. Copiez l'APK sur le téléphone et ouvrez-le. Autorisez l'installation depuis cette source si Android le demande.
2. Ouvrez **Alerte Courses** et acceptez les notifications.
3. Appuyez sur « 1. Activer le service d'accessibilité ». Choisissez **Alerte Courses**, puis activez-le.
   - **Si l'option est grisée** (Android 13 et plus) : revenez dans l'application et appuyez sur « autoriser les paramètres restreints ». Dans les infos de l'application, ouvrez le menu **⋮** en haut à droite, puis « Autoriser les paramètres restreints ». Recommencez ensuite l'étape 3.
4. Appuyez sur « 🔔 Tester l'alerte » pour vérifier le son et la voix.
5. Sur un téléphone Samsung, désactivez l'optimisation de batterie pour Alerte Courses : Paramètres → Applications → Alerte Courses → Batterie → **Non restreinte**.

## 3. Utilisation
- Laissez inDrive (liste « Commandes de course ») ou Yassir (« Détecteur de courses ») **ouvert et affiché**, écran allumé.
- Chaque course n'est signalée qu'une fois, pendant 10 minutes.
- La section **Journal** affiche toutes les courses lues avec le motif de chacune : ✅ alerte, ou ❌ suivi de la raison (prix, distance, hors zones).

## 4. Si une course n'est pas reconnue
Dans la section **Diagnostic**, copiez le « Dernier texte lu à l'écran » et envoyez-le à Claude, qui ajustera la lecture. C'est aussi ce qu'il faut faire après une mise à jour d'inDrive ou de Yassir qui change l'affichage.

## Remarques
- Avec inDrive, les adresses portent souvent seulement « (Alger) », sans la commune. Par défaut, ces courses ne déclenchent pas d'alerte, car elles peuvent être n'importe où. L'option « Accepter (Alger) sans commune » permet de les inclure.
- Lire l'écran d'une autre application peut aller contre les conditions d'utilisation d'inDrive et de Yassir. Ne transformez jamais cet outil en accepteur automatique : c'est ce type d'outil qui entraîne des blocages de compte.
