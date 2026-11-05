## T-MODELS: mooiere mobs (R5 hybride) en de Klodderreus/Vahoegnir als showcase

Wat is er veranderd? Alle gewone wezens van zone 1-3 zijn opnieuw gemaakt met de verbeterde generator:
- **Rondere vormen:** grote blokken hebben afgeronde randen en hoeken (een paar trapjes, zoals een kussen), poten lopen iets taps toe.
- **Betere textures:** licht van boven (rug lichter, buik donkerder), een zachte schaduw onderaan, glansplekken op slijm, zachte
  vlekjes en stippen, vachtstreepjes op vacht en pluis, kristallen met lichte randjes.
- **Gezichtjes:** groter (ze lopen nu over de ronde voorkant door), zachte randjes en een extra glimpje in de ogen.
- **Gloed:** gloeiende delen (kristallen, ogen, lampjes) zijn in het midden feller en aan de rand zachter.
- **Wiebelen:** oren, staarten en manen wiebelen een beetje na. Slijmen hebben een wiebelend kapje bovenop (squash & stretch).
- **De Grauwe Klodderreus** is geen vanilla-slijmblok meer: een eigen groot model met guh-oren, boze 3D-wenkbrauwen, paarse
  gloei-ogen, druppels langs zijn lijf, een mond die opengaat, en een roze blije versie na de plof.
- **Vahoegnir:** hetzelfde goedgekeurde ontwerp (ook de grote oren), alleen met meer volume (licht van boven, buik in de schaduw)
  en zijn oren wiebelen mee als hij vliegt.
- Voor/na-plaatjes: `guhs_vadsrijk\t-models\voorbeelden\` (zone1_*.png, zone2_*.png, zone3_*.png, klodderreus.png, vahoegnir.png).

### Showroom: alle wezens op een rij (stil, onkwetsbaar)
Ga op een vlakke, open plek staan en kijk de kant op waar ze moeten komen.
```
/guhs vadsrijk models toon zone1
/guhs vadsrijk models toon zone2
/guhs vadsrijk models toon zone3
/guhs vadsrijk models toon critters
/guhs vadsrijk models plof
/guhs vadsrijk models weg
```
- `toon` zet een raster neer (6 per rij, 4 blokken uit elkaar) van alle wezens van die groep. Ze staan stil maar spelen hun
  idle-animatie: let op het wiebelen van oren, staarten en het slijmkapje.
- `plof` vrolijkt alle showroom-wezens op: zo zie je de roze, blije textures (zone 1) en de plof-doorzichtigheid.
- `weg` ruimt alles op (alles met de showroom-tag binnen 96 blokken). `toon alles` kan ook, maar dat zijn er ~55.
- Kijk vooral naar: zijn de vormen ronder, lopen de gezichtjes netjes over de voorkant, zitten er geen gaten of rare randjes
  aan de afgeronde hoeken, en lezen de ogen goed op afstand.

### Eén wezen in actie (lopen, aanvallen)
```
/guhs vadsrijk wezens spawn klodder 3
/guhs vadsrijk wezens spawn hoedjesbol 3
/guhs vadsrijk wezens spawn kristalklodder 3 true
/guhs vadsrijk z2wezens spawn sneeuwknabbelaar
/guhs vadsrijk z3wezens spawn rotzwijn
```
- Met `true` erachter (zone 1) krijg je een elite: de gouden gloed moet nog netjes over het nieuwe model liggen.

### De Grauwe Klodderreus
```
/guhs vadsrijk models reus
/guhs vadsrijk models reus anim spuug
/guhs vadsrijk models reus anim brul
/guhs vadsrijk models reus anim aanloop
/guhs vadsrijk models weg
/guhs vadsrijk models reus vecht
```
- `reus` zet hem stil neer (7 blokken voor je), om rond te lopen en te bekijken. `reus anim ...` speelt een actie bij alle
  reuzen binnen 32 blokken: spuug (mond open, lijf zwelt op), brul (bij een nieuwe fase), aanloop (hij duikt in elkaar voor een sprong).
- `reus vecht` = een echt gevecht (lv 3) zonder arena. Let op: in de lucht wordt hij lang, bij het landen plat (squash),
  bij het rollen in fase 3 rolt hij echt over de kop, en in fase 2 spuugt hij kloddertjes: die hebben nu ook het ronde
  klodder-model in plaats van het vanilla-slijmblok. Na de plof: roze, blij, doorzichtig, zonder gloei-ogen.
- Het echte gevecht bij de kampvijver (BAM-quest) gebruikt hetzelfde model.

### Vahoegnir
```
/guhs vadsrijk wezens draak
/guhs vadsrijk z3bazen vahoegnir
```
- `wezens draak` laat de grijze overvlieger langskomen: zijn grote ronde oren wiebelen nu na op de vleugelslag.
- `z3bazen vahoegnir` voor de baas (grijs -> roze per stap): zelfde ontwerp, iets meer diepte (buik en poten donkerder, rug lichter).
