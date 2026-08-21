# dmav

Werkzeugkasten für das Handling mit DMAV-Transferdateien:

- `merge`: Erzeugt aus mehreren Transferdateien eine einzelne DMAV-Transferdatei.
- `split`: Trennt eine DMAV-Transferdatei in einzelne Transferdateien pro Datenmodell.

Die Verarbeitung erfolgt ereignisbasiert mit **iox-ili**. Die aktuelle DMAV-Modellbasis ist `DMAVTYM_Alles_V1_1` (DMAV Version 1.1). Legacy-Daten auf Basis `DMAVTYM_Alles_V1_0` werden weiterhin unterstützt.

Snapshot-Versionen:
- https://s01.oss.sonatype.org/content/repositories/snapshots/io/github/sogis/dmav/ -> X.Y.Z-SNAPSHOT -> dmav-X.Y.Z-Datum.Uhrzeit-all.jar

Release-Versionen:
- https://s01.oss.sonatype.org/content/repositories/releases/io/github/sogis/dmav/
- https://github.com/edigonzales/dmav/releases

## Anforderungen

Java 11 oder grösser zur Ausführung. Für den Gradle-Build wird ein JDK 17 oder grösser benötigt; die CI verwendet JDK 21.

## Benutzung

### Dateien mergen

```bash
java -jar dmav.jar merge --config myconfig.ini --fosnr 449 --out /path/to/directory
```

| Name | Beschreibung | Required |
|-----|-----|-----|
| `--config` | Pfad zur Konfig-Datei (siehe unten). | ja |
| `--fosnr` | BFS-Nummer; `${fosnr}` in der Konfiguration wird damit ersetzt. | ja |
| `--out` | Zielverzeichnis für `DMAV.<fosnr>.xtf`. | ja |

Beispiel für DMAV 1.1:

```ini
DMAV_FixpunkteAVKategorie3_V1_1=/data/DMAV_FixpunkteAVKategorie3_V1_1.${fosnr}.xtf
KGKCGC_FPDS2_V1_1=https://example.org/fixpunkte_v1_1.zip
```

Die Keys entsprechen den direkt von `DMAVTYM_Alles_V1_1` importierten Datenmodellen. Values können lokale XTF-/ZIP-Dateien oder HTTP(S)-URLs auf XTF-/ZIP-Dateien sein. Bei ZIP-Dateien wird die erste enthaltene XTF-Datei verwendet.

Der Merger kompiliert das passende DMAV-Umbrella-Modell und leitet daraus die erwarteten Transfer-Topics ab. Vorhandene Baskets werden mit ihren BIDs, TIDs und Referenzen übernommen. Für fehlende Topics wird programmgesteuert ein leerer Basket mit deterministischer BID erzeugt; statische `*.empty.xtf`-Vorlagen werden nicht benötigt.

Für `FixpunkteLV_V1_0` wird ein normaler, vollständiger Input über den Key `FixpunkteLV_V1_0` unterstützt. Aus Kompatibilitätsgründen sind auch die bisherigen Keys

```ini
FixpunkteLV_V1_0_LFP=/data/lfp.xtf
FixpunkteLV_V1_0_HFP=/data/hfp.xtf
```

möglich. In diesem Fall werden `LFP1` und `HFP1` wie bisher in einen gemeinsamen `FixpunkteLV`-Basket geschrieben; die bisherige historische Basket-ID bleibt für diesen Spezialfall erhalten.

### Datei aufsplitten

```bash
java -jar dmav.jar split --input path/to/dmav.xtf --fosnr 449 --out /path/to/directory
```

| Name | Beschreibung | Required |
|-----|-----|-----|
| `--input` | DMAV-Transferdatei mit mehreren Themen/Modellen. | ja |
| `--fosnr` | BFS-Nummer als Suffix der Ausgabedateien. | ja |
| `--out` | Zielverzeichnis für die resultierenden Transferdateien. | ja |

Der Splitter liest den XTF-Eventstrom mit iox-ili und schreibt pro Datenmodell eine Ausgabedatei. Mehrere Baskets desselben Modells bleiben in derselben Datei. BIDs, TIDs und Referenzen werden unverändert übertragen.

## Architektur

Splitter und Merger arbeiten direkt auf IOX-Events (`StartBasketEvent`, `ObjectEvent`, `EndBasketEvent`) und laden nicht die vollständige Transferdatei in einen DOM-Baum. Die INTERLIS-Modelle werden zur Laufzeit über ili2c geladen. Für DMAV 1.1 ist `DMAVTYM_Alles_V1_1` die verbindliche Modellbasis; bei erkannten DMAV-1.0-Inputs wird `DMAVTYM_Alles_V1_0` verwendet.

Die Testbasis enthält einen reduzierten Ausschnitt des offiziellen DMAV-1.1-Testdatensatzes. Ein Roundtrip-Test prüft `DMAV 1.1 -> split -> merge` semantisch auf Basket-IDs, Objekt-IDs und Referenzen sowie auf die modellgetrieben erzeugten leeren Baskets.

## Tests

```bash
./gradlew clean test
```

## Native Image

Für das Native-Image-Build wird lokal GraalVM CE 25.2.4 verwendet. Der Gradle-Wrapper 9.1.0 kann direkt mit dieser GraalVM laufen:

```bash
export GRAALVM_HOME=/Users/stefan/.sdkman/candidates/java/25.2.4-graalce
export JAVA_HOME="$GRAALVM_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew nativeCompile
build/native/nativeCompile/dmav --help
```

Auf GraalVM Community Edition wird automatisch Serial GC verwendet. Falls der konkrete GraalVM-Build G1 anbietet, wird G1 automatisch aktiviert. Die erzeugten Release-Binaries heißen `dmav-linux-x86_64`, `dmav-windows-x86_64.exe` und `dmav-osx-aarch_64`.

## Release

Es gibt die zwei Branches `main` und `stable`. Entwickelt wird im `main`-Branch. Für einen Release müssen die Änderungen in den `stable`-Branch gemerged werden:

```bash
git checkout stable
git rebase main
```
