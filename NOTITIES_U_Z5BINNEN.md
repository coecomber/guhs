# NOTITIES U-Z5BINNEN - Hoogvads interieurs: ronde kroonluchters en lichtere zalen

Ronde U (2026-11-06). Feedback: "de kroonluchters lijken houten steigers/roosters, het is binnen te donker".
Worktree `guhs_vadsrijk/u-z5binnen`, branch `110-u-z5binnen` (van 110-u-z5 8ea405aac).
Referentie van de gebruiker: grote ronde hangende kroonluchters met kettingen, kaarsen en kristallen druppels.

## Nieuwe blokken (Java `feature/vadsrijk/zone5/binnen/Z5Licht.java`, resources via `tools/features/vadsrijk_t_z5blokken.py` sectie LICHT)
Zonder hv_-prefix, net als de andere Hoogvads-blokken. Allemaal lichtgevend, in de tab "Guhs: Bouwen", met recept.

| id | wat | licht |
|---|---|---|
| `kroonluchter` | 1 blok met een rond 3D-model van 3 blokken breed: haak, staaf, kroonring met 8 kaarsen, grote ring (r 17 px) op 8 armen met 16 kaarsen en 16 kristallen druppels, schaal met een kristalring en een druppel onderaan | 15 |
| `gouden_kaarsenrand` | dunne gouden rail met een kaars op de knoop; `north/east/south/west` verbindt met rails en kroonluchters, `kaars`, `kristal` (druppel eronder) | 13 (kaars) / 7 |
| `kristallen_hanger` | gouden kapje, draadje, 2 kralen en een grote geslepen druppel | 9 |
| `gouden_wandluchter` | wandkandelaar met 3 kaarsen en druppels, `facing` = de kant waar hij naartoe steekt | 14 |
| `gouden_kandelaar` | staande kandelaar, model 2 blokken hoog (botsing 1 dun paaltje) | 14 |
| `gouden_kroonlijst_muur` | gouden muurtje (walls-tag, verbind-pass pakt het mee) | - |
| `gouden_lichtlijst` | gouden kroonlijst die licht geeft (licht verstopt in de gewelfribben) | 15 |

- Vlammen en kristallen hebben `"light_emission": 15/10` in het model (fel in het spel), het goud is `minecraft:block/gold_block`.
- 3 textures getekend in `textures/block/hoogvads_licht/` (kaars, vlam, kristal).
- De ring in het kroonluchtermodel is een 16-hoek van gedraaide staafjes (22,5 graden stappen, dezelfde draairichting als het spel).
- Gametest `Z5LichtGameTests` (batch `vadsrijk_z5licht`): registratie, namen, licht, de rail verbindt en laat los, geen botsing.

## Kroonluchters (`tools/vadsrijk/zone5/binnen2.py`)
`gotiek.kroonluchter()` bouwt nu `binnen2.kroonluchter` (de oude plaatversie heet `kroonluchter_oud`), maat op de straal:
- **S** (r < 3,5): 1 `kroonluchter` aan een ketting (bibliotheek, zijbeuken, kanselarij);
- **M** (r < 5): ring van kaarsenrails r 3 met 4 kroonluchter-kronen erin, een kroonluchter in het midden (troonzaal, Raadszaal, academiehal, koor);
- **L** (r < 8): ring r 5 met 6 kronen en kristallen hangers, een kroonring r 2 erboven (schip);
- **XL**: ring r 8 met 8 kronen en een gordijn van hangers, een lagere ring r 5, een kroonring r 3 (de kruising).
- Geen spaken meer (een ring met een kruis erin leest van onderen als rooster); de ringen hangen aan 4 kettingen naar het plafond.
- De kathedraal-luchters hangen lager (schip Y0+16, kruising Y0+21, zijbeuken Y0+12), zodat het licht de vloer haalt.

## Lichtere interieurs (`binnen2.bouw`, aangeroepen in `bouw5.bouw` na `hoog3`)
- **Kathedraal:**
  - marmeren dambordvloer (wit/roze) met gouden rozetten (kruising, transeptarmen, onder de poort);
  - gouden bankeinden;
  - kandelaars langs het middenpad en rond het altaar;
  - wandluchters op elke pijler (twee hoogtes, schip- en zijbeukkant) en op de zijbeukmuren en transeptwanden;
  - vaandels op de pijlers;
  - lichtpoelen (tapijtmozaïek in spitsboogvorm) onder de zijbeukramen en de transeptrozen;
  - 255 gouden lichtribben in de gewelven;
  - het gouden guh-beeld: kandelaars om de sokkel, verborgen licht (`minecraft:light`) ervoor en een stralenkrans van lichtlijst erachter.
- **Bibliotheek:** dambordvloer, bleek eikenhout voor bureaus/galerijen, een goudband in de boekenwanden, wandluchters onder en boven de galerijen, lichtlijst in het plafond.
- **Raadszaal:** het hangende goudplatenrooster van U-Z5 is weg (`hoog3.raadszaal`); nu lichtlijst vlak IN het plafond, dambordvloer met rozet, wandluchters en vaandels tussen de pilasters, 4 kandelaars bij de lessenaar.
- **Troonzaal:** dambordvloer met rozet, wandluchters aan de zuilen, kandelaars, verborgen licht voor de koningin, lichtlijst in het gewelf.
- **Paleis (kanselarij + gang):** dambordvloer, 2 S-kroonluchters, wandluchters.
- **Academiehal:** dambordvloer met rozet, 2 M-kroonluchters, wandluchters, kandelaars bij de kapiteinstafel.
- Alles staat in lucht (vloeren: alleen vloerblokken worden geruild, zelfde hoogte). Staande dingen houden 2 blokken afstand van elke plek (info + plan5).

## Renderer
- `kit/opname.py` `bloklicht`: lichtgevende blokken als puntlichten, zoals in het spel (flood-fill, 1 minder per blok, niet door volle blokken, lichtcurve van het spel, warme kleur). Het werkt via `opname(..., blok_licht=True|box)`.
- `kit/prender.py`: `EMIS_TEX` (vlam/kristal-quads gloeien, het goud niet) en `belicht(..., blok=...)`.
- `kit/hoogkit.py` neemt de LICHT-blokken en `hoogvads_licht/` mee in de repo-zip.
- `render5.py` heeft de shots `b_*` (of `binnen` = allemaal): kathedraal schip/kruising, bibliotheek, Raadszaal, troonzaal, kanselarij, academiehal.
- Renders in `renders-hoogvads/ronde3/`: `binnen2_voor_*` (oud, al met bloklicht) en `binnen2_*` (nieuw).

## Gedeelde bestanden (add-only, gemarkeerd U-Z5BINNEN)
- `Z5WereldRegistry.java`: 1 regel `Z5Licht.register(modBus)`.
- `vadsrijk_t_z5blokken.py`: sectie LICHT + aanroepen in `build`/`_direct`.
- `bouw5.py`: `binnen2.bouw`-aanroep.
- `hoog3.py`: het goudrooster van de Raadszaal is weg.
- `kit/gotiek.py`: `kroonluchter` gaat naar binnen2.
- `kit/kathedraal3.py`: hoogtes van de luchters.
- `kit/opname.py`, `kit/prender.py`, `kit/hoogkit.py`: renderer.
- Tags: `walls` (blok+item) en `mineable/pickaxe`.
- Lang: 7 namen (NL+EN).

## Checks
- `compileJava` is groen.
- `runGameTestServer -Pgt=Z5LichtGameTests,Z5WereldGameTests`: **All 8 required tests passed**.
- `maak_kaart --zone 5` (met de verbind-pass) + `loopcheck5.py`:
  - precies dezelfde stand als U-Z5: LEEG 523 / TRAP 668 / TRAP- 468 / VAL 485;
  - alleen de 4 bekende onbereikbare staartvin-plekken van t-z5wezens (niet van deze slice);
  - alle andere plekken zijn bereikbaar.
- De gegenereerde tegels, `Z5WereldPlekken.java` en `POI_Z5.md` zijn teruggezet (niet gecommit). **De integratie moet `maak_kaart --zone 5` draaien.**

## Hervatpunt
- Regen: `python tools/vadsrijk/maak_kaart.py --zone 5`, daarna `python tools/vadsrijk/zone5/loopcheck5.py`.
- Renders: `python tools/vadsrijk/zone5/render5.py <map> <prefix> binnen` (of losse `b_*`-shots). Ongeveer 2-3 minuten per shot, want het bloklicht kost tijd.
- Blokken opnieuw maken: `python tools/features/vadsrijk_t_z5blokken.py`. De textures worden alleen getekend als ze ontbreken.

## Open
- Het uiterlijk is alleen in renders bekeken; de gebruiker beoordeelt het in de dev client.
  - Let vooral op het `kroonluchter`-model (3 blokken breed): de 16-hoek-ring met gedraaide staafjes moet in het spel rond lezen.
  - Let ook op de `light_emission` van de vlammen.
- De grote ringen van kaarsenrails zijn op blokniveau nog getrapt. De ronde kroontjes erop dragen het ronde beeld.
  - Als het nog te hoekig is: meer kronen per ring, of de ringen alleen uit kroonluchters maken.
- De academiehal-shot kijkt deels tegen de binnenkant van de buitenmuur met ramen. Dat is bestaande bouw en is niet aangeraakt.
- `minecraft:light`-blokken (verborgen licht bij het guh-beeld en de koningin) zijn onzichtbaar en hebben geen botsing.
