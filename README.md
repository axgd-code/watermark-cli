# watermark-cli

Language: Francais | [English](README.en.md)

CLI Java pour appliquer un filigrane texte sur des PDF, en fichier unique ou en traitement massif de dossiers imbriqués.

## Pourquoi ce projet ?

- Traitement d'un PDF ou d'un répertoire complet (récursif)
- Conservation de l'arborescence en sortie
- Protection des zones QR code
- Progression affichée en CLI sur les tâches longues (fichiers/pages)
- Algorithme éprouvé, issu d'un projet open source de référence

## Inspiration

Ce projet s'inspire du dépôt DossierFacile :
https://github.com/MTES-MCT/dossierfacile-backend

Plus précisément, l'implémentation reprend la logique du module `dossierfacile-pdf-generator`, notamment la classe `BOPdfDocumentTemplate` (ainsi que son filtre de distorsion), afin de réutiliser un algorithme existant et robuste plutôt que d'en créer un nouveau.

Éléments repris dans ce CLI :

- rendu des pages PDF en images
- application d'un filigrane diagonal répété avec un flou gaussien
- protection des zones QR code (suppression locale du filigrane)
- reconstruction d'un PDF final page par page

## Prérequis

- Java 21+
- Maven 3.9+

## Build

```bash
mvn clean package
```

Le binaire généré est :

```bash
target/watermark-cli.jar
```

## Démarrage rapide

```bash
java -jar target/watermark-cli.jar \
  --input /chemin/source/document.pdf \
  --output /chemin/sortie/document-watermarked.pdf \
  --watermark "DOSSIER DE LOCATION - NOM PRENOM"
```

## Utilisation détaillée

### 1. Traiter un seul PDF

```bash
java -jar target/watermark-cli.jar \
  --input /chemin/source/document.pdf \
  --output /chemin/sortie/document-watermarked.pdf \
  --watermark "DOSSIER DE LOCATION - NOM PRENOM"
```

Vous pouvez aussi fournir un dossier de sortie existant avec une entrée fichier :

```bash
java -jar target/watermark-cli.jar \
  -i /chemin/source/document.pdf \
  -o /chemin/sortie/ \
  -w "DOSSIER DE LOCATION - NOM PRENOM"
```

### 2. Traiter un dossier (récursif)

```bash
java -jar target/watermark-cli.jar \
  --input /chemin/source/dossiers \
  --output /chemin/sortie/dossiers-watermarked \
  --watermark "DOSSIER DE LOCATION - NOM PRENOM"
```

Comportement :

- parcours récursif de tous les sous-dossiers
- traitement de tous les fichiers `*.pdf`
- recréation de la même structure de dossiers en sortie
- fichiers non PDF ignorés

### 3. Écraser les fichiers existants

Par défaut, la commande échoue si le PDF de sortie existe déjà.

Pour autoriser l'écrasement :

```bash
java -jar target/watermark-cli.jar \
  -i /chemin/source \
  -o /chemin/sortie \
  -w "MON WATERMARK" \
  --overwrite
```

## Options

- `-i, --input` (obligatoire) : chemin vers un PDF ou un dossier
- `-o, --output` (obligatoire) : chemin du PDF de sortie ou dossier de sortie
- `-w, --watermark` (obligatoire) : texte du filigrane
- `--overwrite` (optionnel) : écrase les fichiers existants
- `--use-colors` (optionnel) : active la variante couleur du filigrane
- `--use-distortion` (optionnel) : active la distorsion du filigrane

## Tests

Exécuter les tests :

```bash
mvn test
```

Couverture actuelle :

- test moteur : génération d'un PDF filigrané et vérification du nombre de pages
- test CLI sur fichier unique
- test CLI sur dossier récursif avec conservation de la structure
- test CLI en erreur si la sortie existe sans `--overwrite`

## Notes techniques

- Le pipeline de rendu/filigrane est volontairement aligné avec l'implémentation DossierFacile.
- Le projet est volontairement limité aux PDF en entrée (pas d'images directes).
- La sortie est un nouveau PDF rasterisé et filigrané.

## Licence

Voir le fichier `LICENSE`.
