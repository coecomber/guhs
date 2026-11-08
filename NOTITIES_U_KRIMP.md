# NOTITIES U-KRIMP (ronde U, 2026-11-06 / 2026-11-08)

Branch `110-u-krimp` (worktree `guhs_vadsrijk/u-krimp`, vanaf 110-u-grootte 90689737b). Opdracht: de mod-jar verder verkleinen
na U-GROOTTE (structuren), met OK van de gebruiker: OGG mono ~48 kbps, textures >= 1024x1024 halveren, PNG verliesvrij
herschrijven. Spiekbrief: `SPIEK_U_KRIMP.md`.

## Resultaat

| deel (src/main/resources) | voor | na |
|---|---|---|
| PNG (7930 bestanden) | 172.0 MB | **128.1 MB** |
| - waarvan 46 textures >= 1024x1024 | 15.1 MB | 4.4 MB (gehalveerd) |
| OGG (666 bestanden) | 116.6 MB | **94.5 MB** |
| - waarvan Vadsrijk-muziek (97 stukken) | 32.9 MB | 28.1 MB |
| **mod-jar** (`build/libs`) | **334 MB** | **269.8 MB** (-64 MB) |

Verdeling in de jar (gecomprimeerd, `guhs-1.9.2.jar`, 2026-11-08):
| in de jar | voor (334 MB) | na (269.8 MB) |
|---|---|---|
| .png (7923) | 169 MB | 126.1 MB |
| .ogg (666) | 114 MB | 92.5 MB |
| .class (8209) | | 26.4 MB |
| .gpak (2 structuurpakken, U-GROOTTE) | | 9.0 MB |
| .json (21466) | | 7.7 MB |
| rest (json5, zip, txt) | | ~0.9 MB |

## Wat er gedaan is

### `tools/krimp.py` (nieuw, herhaalbaar)
Drie stappen over `src/main/resources`, in deze volgorde:
- **groot**: textures van minstens 1024x1024 worden gehalveerd (2x2 box op premultiplied alpha, dus geen donkere randjes).
  Glowmasks/gloed-lagen gaan mee met hun basis. **GeckoLib geo en json-modellen zijn NIET aangepast**: GeckoLib normaliseert UV's
  met `texture_width/height` uit de geo (niet de beeldgrootte), json-modellen gebruiken 0..16. Offline vergeleken (draak,
  luchtbizon, wolkwacht ridder, roquefort): zelfde uiterlijk.
- **png**: verliesvrij. Pillow optimize + eigen brute-force schrijver (5 vaste filters + min-sum, zlib 9 default/filtered),
  exact palet (1-8 bit, tRNS), RGB/L/LA waar alpha/kleur niet nodig is. Elke kandidaat wordt gedecodeerd en RGBA-identiek
  gecontroleerd (ook de kleur van doorzichtige pixels) voordat hij geschreven wordt; alleen als hij kleiner is.
  Steekproef 300 bestanden tegen HEAD: 0 verschillen.
- **ogg**: Vorbis boven 52 kbps wordt een keer gedecodeerd en opnieuw gecodeerd (libsndfile, niveaus 0.85/0.92/0.97, de eerste
  die op <= 50 kbps uitkomt wint). Mono blijft mono, stereo wordt mono tenzij de zijkanalen echt verschillen (geen enkel bestand
  was echt stereo). Samplerate en aantal samples worden gecontroleerd. Alleen geschreven als het >= 5% kleiner is.
  495 van de 666 bestanden kleiner; 171 al <= 52 kbps of te weinig winst (heel korte geluidjes: de header domineert, daar blijft
  de kbps hoog maar gaat het om een paar kB).

Manifest `tools/krimp_manifest.json` (gecommit): per bestand de sha1 NA behandeling + welke stappen gedaan zijn. Een bestand met
dezelfde hash wordt overgeslagen, dus **verlieslatende stappen draaien nooit twee keer op hetzelfde bestand**. Een bestand dat een
generator opnieuw schreef (nieuwe hash) wordt opnieuw behandeld.

```
python tools/krimp.py --dry --proc 10      # wat zou er veranderen
python tools/krimp.py --proc 10            # alle stappen
python tools/krimp.py --alleen ogg         # een stap (groot | png | ogg)
python tools/krimp.py --rapport            # MB per bestandstype
```
Onbekende opties (bijv. `--help`) tonen alleen de uitleg (eerder liep `--help` per ongeluk alle stappen; hersteld, de oggs zijn
daarna vanaf HEAD opnieuw een keer gecodeerd).

### Vadsrijk-muziek
`tools/vadsrijk/geluid/render.py` zet de libsndfile compression_level nu minimaal op 0.85 (`OGG_MIN_COMPRESSIE`, te overschrijven
met env `GUHS_OGG_COMPRESSIE`). Alle 97 stukken opnieuw gerenderd vanaf MIDI (`maak_muziek.py --van-midi`): zelfde lengte,
correlatie met de oude render >= 0.97. Nieuwe renders zijn dus meteen compact.

## Voor de integrator (BELANGRIJK)
- **Draai `python tools/krimp.py --proc 10` op de uiteindelijke integratieboom**, na alle merges. Andere branches voegen
  textures/geluiden toe of schrijven ze opnieuw; die hebben een nieuwe hash en worden dan (een keer) behandeld. Bestanden die al
  gedaan zijn worden overgeslagen. Daarna `--dry` moet overal "0 bekeken" geven.
- Merge-conflict op een binair asset (PNG/OGG die hier verkleind is en op een andere branch veranderd): **neem de versie van de
  andere branch** en draai daarna krimp.py; die pakt hem dan op.
- Conflict in `tools/krimp_manifest.json`: neem deze branch' versie; verouderde regels zijn onschuldig (hash klopt niet meer ->
  bestand wordt gewoon opnieuw behandeld).
- `tools/keep_unchanged.py` zet PNG's met gelijke pixels terug naar git's versie; na deze merge is dat de compacte versie, dus dat
  werkt samen. Een generator die een grote texture (>= 1024) opnieuw schrijft, maakt hem weer groot: krimp.py halveert hem opnieuw.
- Een nieuwe GeckoLib-texture moet bij halveren niets aan de geo veranderen (UV's via texture_width); json-modellen ook niet.

## Testen
- `jar runGameTestServer -Pgt=Geluid,VadsrijkGrootte` via het slot-script: BUILD SUCCESSFUL, **All 11 required tests passed** (2026-11-08).
- Geen client-check gedaan (geen luisteren/kijken in een client); zie de spiekbrief voor wat de gebruiker even moet checken.
