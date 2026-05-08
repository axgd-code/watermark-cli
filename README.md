# watermark-cli

CLI Java pour appliquer un watermark texte sur des PDF.

## Inspiration

Le projet est inspire de ce repository open-source:

- https://github.com/MTES-MCT/dossierfacile-backend

Plus precisement, l'implementation reprend la logique du module `dossierfacile-pdf-generator`, notamment la classe `BOPdfDocumentTemplate` (et son filtre de distorsion associe), afin de reutiliser un algorithme deja eprouve au lieu d'en reinventer un.

Ce qui est repris dans ce CLI:

- rendu des pages PDF en images
- application d'un watermark diagonal repete avec blur gaussien
- protection des zones QR code (suppression locale du watermark)
- reconstruction d'un PDF final page par page

## Prerequis

- Java 21+
- Maven 3.9+

## Build

```bash
mvn clean package
```

Le binaire genere est:

```bash
target/watermark-cli.jar
```

## Utilisation

### 1) Un seul PDF en entree

```bash
java -jar target/watermark-cli.jar \
  --input /chemin/source/document.pdf \
  --output /chemin/sortie/document-watermarked.pdf \
  --watermark "DOSSIER DE LOCATION - NOM PRENOM"
```

Tu peux aussi donner un dossier de sortie existant avec un input fichier:

```bash
java -jar target/watermark-cli.jar \
  -i /chemin/source/document.pdf \
  -o /chemin/sortie/ \
  -w "DOSSIER DE LOCATION - NOM PRENOM"
```

### 2) Dossier en entree (recursif)

```bash
java -jar target/watermark-cli.jar \
  --input /chemin/source/dossiers \
  --output /chemin/sortie/dossiers-watermarked \
  --watermark "DOSSIER DE LOCATION - NOM PRENOM"
```

Comportement:

- parcours recursif de tous les sous-dossiers
- traitement de tous les fichiers `*.pdf`
- recreation de la meme structure de dossiers en sortie
- les fichiers non-PDF sont ignores

### 3) Écraser les fichiers de sortie

Par defaut, la commande échoue si le PDF de sortie existe deja.

Pour autoriser l'ecrasement:

```bash
java -jar target/watermark-cli.jar \
  -i /chemin/source \
  -o /chemin/sortie \
  -w "MON WATERMARK" \
  --overwrite
```

## Options

- `-i, --input` (obligatoire): chemin vers un PDF ou un dossier
- `-o, --output` (obligatoire): chemin du PDF de sortie ou dossier de sortie
- `-w, --watermark` (obligatoire): texte du watermark
- `--overwrite` (optionnel): écrase les fichiers existants
- `--use-colors` (optionnel): active la variante couleur du watermark
- `--use-distortion` (optionnel): active la distorsion du watermark

## Tests

Executer les tests:

```bash
mvn test
```

Couverture actuelle:

- test moteur: generation d'un PDF watermarke et verification du nombre de pages
- test CLI fichier unique
- test CLI dossier recursif avec conservation de la structure
- test CLI en erreur si la sortie existe sans `--overwrite`

## Notes techniques

- Le pipeline de rendu/watermark est volontairement aligne avec l'implementation DossierFacile.
- Le projet est volontairement scope aux PDF en entree (pas d'images directes).
- La sortie est un nouveau PDF rasterise et watermarke.

## Licence

Voir le fichier `LICENSE`.
