## Kledingkast, vleugels en effecten (T-KLEDING)

**Openen:** toets **O** (overal, ook thuis), of het tabje "Kledingkast" bovenaan Rijdieren/Huisdieren/Verkenningsboek, of de kleermakers (Zwamzoet, de breister in Kandijkom, de kleermaker in zone 3): keuze **"Naar de Kledingkast"**, of:
```
/guhs vadsrijk kledingkast
```
Alles is **per account** (niet per karakter): wat je ontgrendelt, blijft voor al je ridders.

### Snel alles testen
```
/guhs vadsrijk kleding alles
/guhs vadsrijk kleding kast
```
→ alles ontgrendeld (ook de "later"-dingen) + alle Vadsrijk-harnaslooks. Links zie je jezelf draaien met de muis; klik in een categorie op **Kies**: je ziet het meteen aan je poppetje.
- Overzicht wat je hebt + bron: `/guhs vadsrijk kleding lijst`
- Alles weer weg (ook voor opnieuw testen): `/guhs vadsrijk kleding wis`
- Eén ding geven / afpakken / aandoen: `/guhs vadsrijk kleding geef vleugel:kristal`, `.../kleding neem vleugel:kristal`, `.../kleding kies kaap:sterren`

### Vleugels + zweven (alleen in het Vadsrijk)
```
/guhs vadsrijk kleding vahoegnir
```
→ zoals na Vahoegnir (roze, einde zone 3): titelkaart **"Vahoegveertjes"**, een tip, en je hebt ze meteen om.
- **Zweven:** spring van iets hoogs en **houd spatie ingedrukt** → je zakt heel langzaam (nooit omhoog). Vooruit ingedrukt = ongeveer loopsnelheid. Geen valschade bij landen.
- Kijk of het mag: `/guhs vadsrijk kleding zweef` (zegt: ontgrendeld, welke vleugels, mag hier, zweeft nu).
- **Mag NIET** in bazen-arena's en kerkers (Spiegelburcht, Zeppelinwerf, Hart van de Boom): daar val je gewoon.
- **Thuis** zie je je vleugels wél op je rug, maar zweven doet niets.

Elke vleugel één eigen beweeg-trucje (geen gevechtsstats). Test met `kleding kies <id>`:
| vleugel | id | trucje | hoe test je het |
|---|---|---|---|
| Vahoegveertjes | `vleugel:vahoegveertjes` | gewoon zweven | spatie vasthouden in de lucht |
| Kristalvleugels | `vleugel:kristal` | dubbele sprong | spring, en tik in de lucht nog eens op spatie (1x per sprong) |
| Honingbijvleugeltjes | `vleugel:honingbij` | sneller zweven | zweven + W: duidelijk vlotter vooruit |
| Goudveren Vleugels | `vleugel:goudveer` | langer zweven | je zakt ongeveer half zo snel |
| Schemervleugels | `vleugel:schemer` | luchtdash | in de lucht nog eens spatie tikken: zwiepje vooruit |
| Pluisvleugeltjes | `vleugel:pluis` | zachte landing | spring van hoog zónder zweven: geen schade + roze plofje |
| Lampionvlieger | `vleugel:lampion` | langer zweven | (zone 4, komt later) |
| Sterrenstofvleugels | `vleugel:sterrenstof` | dubbele sprong | puzzelbeloning (t-puzzels) |
| Poollichtvleugels | `vleugel:poollicht` | langer zweven | puzzelbeloning |
| Lantaarnmotvleugels | `vleugel:lantaarnmot` | zachte landing | puzzelbeloning |

**Wat je moet zien aan de vleugels:** stilstaan/lopen = schuin naar achteren op je rug, springen/vallen = verder uitgeklapt, zweven = klapperen (op en neer). Verder dan 24 blokken: stilstaand model (LOD). Verf (tab Verf) kleurt vleugels + hoofdding.

**Waar komen ze vandaan (echt spelen):** Vahoegveertjes = Vahoegnir roze; Kristal = Splinterpieken 100% (B-boek); Honingbij = Schemerwortel 100%; Goudveren = 25 Gouden Veren verzameld; Schemer = zeldzaam (3%, laatste baas 8%) van een baas in Vahoeg-vorm; Pluis = geheim (één zweefvlucht van 100+ blokken omlaag, bv. van de kruin van de Schemerwortel naar beneden); Lampion = zone 4.

### Geheimen testen
```
/guhs vadsrijk kleding geheim zweefvlucht
/guhs vadsrijk kleding geheim kleermakers
```
→ Pluisvleugeltjes / Reiscape. Echt: Reiscape = de Kledingkast openen bij alle drie de kleermakers (Zwamzoet, Kandijkom, zone 3).

### Puzzelbeloningen (van t-puzzels)
```
/guhs vadsrijk kleding puzzel pz_vleugels_sterrenstof
/guhs vadsrijk kleding puzzel pz_cape_mos
```
→ doet hetzelfde als het oplossen van die puzzel. In de Kledingkast staan ze als "???" tot je ze hebt.

### Cape (rug-vak, over de stat-cape heen)
`kleding kies kaap:knuffelorde` (ook `roze`, `sterren`, `bloesem`, `reis`, `sneeuwvlok`, `honingraat`, `mos`) → in F5 een cape op je rug. Vleugels én cape tegelijk kan niet (één rugvak). Met vleugels om wordt een gewone cape verborgen.

### Hoofd, harnas, naam, titel
- **Hoofd:** `accessoire:oortjes`, `strikje`, `paddenhoedje`, `kristaltiara`, `honingkroon`, `feestmuts` (+ `lampionspeld` later). Zit ook netjes bovenop een helm.
- **Harnas (transmog):** tab Harnas, per stuk `<` `>`: "Je eigen stuk" / "Verborgen" / elk Vadsrijk-harnas dat je ooit in je tas had. Alleen zichtbaar **in het Vadsrijk** en alleen als je in dat vak echt iets draagt. Helm verbergen = "Verborgen" bij Helm.
- **Naamkleur:** tab Naamkleur → je naam boven je hoofd, in de tablijst en in de chat in die kleur (titel erachter blijft).
- **Titel:** tab Titel = de bestaande titels (zelfde als in de Guhdex).

### Effecten (één van elk actief)
- **Opvrolijken** (bij JOUW laatste klap op een wezen; bovenop de roze plof): confetti, regenboogje, hartjesballon, bloemenregen, knuffelregen, guh-giechel, "Njeg!"-letters. Snel kijken zonder vechten:
```
/guhs vadsrijk kleding plof plof:njeg
/guhs vadsrijk kleding plof plof:regenboog
```
- **Spoor** (lopen of rijden): bloemblaadjes, sneeuwvlokjes, sterretjes, honingdruppels, muzieknootjes.
- **Aura** (pas na ~3 s stilstaan, weg zodra je beweegt): roze gloed, pluisjes, vuurvliegjes.
- **Schuifjes** links onder in de Kledingkast: **Anderen: Alle/Minder/Uit** en **Eigen: Alle/Minder/Uit** (bewaard in `config/guhs_kleding.properties`). In een bazen-arena: sporen minder, aura's uit.
- Teller-beloningen testen: `/guhs vadsrijk kleding kills 1000` en dan `/guhs vadsrijk kleding check` → guh-giechel, paarse naam, Njeg!-letters.
