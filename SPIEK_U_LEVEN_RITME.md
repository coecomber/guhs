# SPIEK U-LEVEN-RITME: dag en nacht in de steden, weer met charme, emote-reacties, het Pluizenblaadje

Alle commando's werken als op (permissieniveau 2), in het Vadsrijk. Handig om eerst naar een stad te gaan, bijvoorbeeld naar
Bakker Bram in Zwamzoet:
```
/guhs vadsrijk npc tp bakker_bram
```

Overzicht van wat er in de stad gebeurt waar je staat:
```
/guhs vadsrijk ritme status
```
Dit toont de stad, dag of nacht, hoeveel winkeliers in bed liggen, nachtbellen, bekende bedden, slapers, uilen, nachtmarkten en prikborden.

## 1. Dag- en nachtritme in de steden

Je hoeft niet op de avond te wachten. Zet het stadsritme op nacht, zonder de tijd te veranderen:
```
/guhs vadsrijk ritme nacht
```
Terug naar dag of naar de gewone klok:
```
/guhs vadsrijk ritme dag
/guhs vadsrijk ritme auto
```
Echt 's nachts gaat het ook vanzelf: tussen `/time set 13000` en `/time set 23000`.

**Winkels gaan dicht.** Welke winkeliers gaan slapen en welke blijven open (en waarom):
```
/guhs vadsrijk ritme winkels
```
Wat je ziet:
- Een winkelier wandelt naar het dichtstbijzijnde vrije bed (binnen 40 blokken) en gaat daar opgekruld slapen, met af en toe een klein "z"-vlokje.
- Op zijn plek staat een bordje **"Gesloten, morgen weer open!"**, met een belletje en "Klik voor de nachtbel".
- Klik op de bel: je hoort een belletje, er is een slaperig zinnetje ("Komt eraan... *gaap*") en de winkel gaat gewoon open.
- Klik je op de slapende winkelier zelf, dan wordt hij een beetje wakker ("Vooruit, omdat jij het bent...") en gaat de winkel ook open.
- Je zit dus nooit vast.

**Quests lopen altijd door.** Elke guh die in een quest voorkomt, blijft 24/7 op zijn plek. Dat geldt ook voor reisguhs en winkeliers
met extra keuzes. 's Nachts gapen ze af en toe. In `/guhs vadsrijk ritme winkels` staat bij hen "blijft 24/7".

**Slapende guhs in de huizen.** Loop 's nachts door een stad en kijk in de huizen: in een deel van de bedden ligt een guh te slapen,
soms met slaapmutsje of pyjama, met "z Z z" boven zijn hoofdje. Klik erop: hij snurkt zachtjes en slaapt verder. Bij de ochtend zijn
ze weg.

**Lichtjes bij schemering.** Bij zonsondergang gaan in alle steden de lichtjes aan, in een golf vanaf het plein: eerst een flitsje, de
hele nacht een zachte gloed en een hoornsignaal. Zwamzoet deed dit al; nu doen De Haven, Knuffelkamp, Kandijkom, Lampionhaven,
Hoogvads, Rozenkloof en Saffraanpoort ook mee. Honingtak houdt zijn eigen avondgloed. Dit volgt de echte klok:
```
/time set 12400
```
Of forceer de golf direct:
```
/guhs vadsrijk sfeer golf
```

**Nachtmarkt met lampionnen** in Zwamzoet, Kandijkom, Lampionhaven en Saffraanpoort. Zet hem meteen neer in de stad waar je staat:
```
/guhs vadsrijk ritme markt
```
Wat je ziet:
- Een ring van gloeiende lampionslingers boven het plein, roze, geel en mint.
- Een bord "✦ Nachtmarkt ✦".
- Twee kraamguhs:
  - **Lotje Lampion** verkoopt een lampionsnoepje (10 brons) voor een minuut Snelpootjes;
  - **Opa Sterrenkoek** verkoopt een sterrenkoekje (5 brons). Daar zit een grappige spreuk in en je krijgt er wat Vads van terug.

**Uilen en vuurvliegjes.** 's Nachts zitten in elke stad met een speler erin 3 guh-uiltjes op de daken. Ze vliegen rond en roepen
"Oehoe-njeg!". Rond je voeten dwarrelen vuurvliegjes. Vuurvliegjes uitzetten kan in `config/guhs-vadsrijk-ritme.toml`
(`vuurvliegjes = false`).

## 2. Weer met charme

Elke zone heeft zijn eigen weer, en elke speler ziet hetzelfde op hetzelfde moment:

| zone | wat |
|---|---|
| 1 | Zwamzoet: het roze buitje met regenboog dat er al was (`/guhs vadsrijk sfeer bui`) |
| 2 | Splinterpieken: een sneeuwbuitje met dikke vlokken, daarna een regenboog |
| 3 | Schemerwortel: bloesemregen, daarna een regenboog |
| 4 | Lampionkust: een lief regenbuitje met zacht regengeluid, daarna een regenboog; elke avond een warme lampiongloed in de mist |
| 5 | Hoogvads: altijd een paar wolkenslierten, en soms een hele optocht |
| 6 | Zijdeland: altijd een zacht stoffig briesje, en soms warme zandwervels |
| alle | om de dag ochtendmist rond zonsopgang |

Bekijk het meteen. Dit is alleen voor jou en gaat over de zone waar je nu staat:
```
/guhs vadsrijk ritme weer bui
/guhs vadsrijk ritme weer regenboog
/guhs vadsrijk ritme weer gloed
/guhs vadsrijk ritme weer mist
/guhs vadsrijk ritme weer stop
```
- `gloed` werkt alleen in zone 4.
- `bui` doet in zone 2 sneeuw, in zone 3 bloesem, in zone 4 regen, in zone 5 wolken en in zone 6 zand.

**Schuif:** Vadsrijk-menu (H) > Instellingen > **Weer met charme** (0 = uit, 100 = vol, standaard 70). Het weer volgt ook de
sfeer-stand: UIT zet alles uit, ZACHT maakt het minder.

## 3. Reacties op je emotes + de knuffelmeter

Ga bij een paar guhs staan in een stad en gebruik het emotewiel (`;` ingedrukt houden) of typ:
```
/emote zwaai
/emote knuffel
/emote dans
/emote juich
/emote lach
/emote hartje
/emote slaap
/emote wijs
```
Wat er moet gebeuren (bij maximaal 4 guhs binnen 7 blokken, een voor een):

| emote | reactie |
|---|---|
| zwaai | ze zwaaien terug |
| knuffel | de dichtstbijzijnde guh (binnen 3,5 blok) knuffelt terug, met hartjes en "X knuffelt je terug!"; de rest krijgt hartjes |
| dans | ze dansen mee |
| juich | ze juichen mee |
| lach | eentje rolt om van het lachen, de rest juicht |
| hartje | ze worden verlegen en er komen hartjes |
| slaap | ze gapen mee |
| wijs | ze kijken waar jij naar wijst |

**Knuffelmeter:**
- Elke stadsguh die je blij maakt telt een keer per dag, met een klein hartje boven zijn hoofd.
- Je ziet de stand in het Vadsrijk-menu (H) > **Karakter** (onderaan links: "Knuffelmeter: ... guhs blij gemaakt" plus de stand per stad), en
  op het Pluizenblaadje.
- Beloningen per stad:
  - 5 = 25 brons;
  - 15 = 100 brons;
  - 25 = 200 brons, en de eerste keer de titel **Knuffelvriend**.
- Beloningen voor alle steden samen:
  - 50 = een hartjesbril voor je guh, met de post (thuis af te halen);
  - 120 = de titel **Grote Knuffelaar** en 500 brons.

Handig bij het testen:
```
/guhs vadsrijk ritme knuffel
/guhs vadsrijk ritme knuffel zet zwamzoet 24
/guhs vadsrijk ritme knuffel reset
```
Zet je hem op 24, dan geeft de volgende blije guh de drempel van 25. Een `zet` zelf geeft geen beloningen.

## 4. Het Pluizenblaadje (krant + prikbord per stad)

Naast het questbord van elke stad staat een berkenhouten bord met daarboven bijvoorbeeld **"Het Zwamzoetse Pluizenblaadje"** en
"Nieuws & klusjes (klik!)". Dat staat in Zwamzoet, De Haven, Kandijkom, Honingtak, Lampionhaven, Hoogvads, Rozenkloof en
Saffraanpoort. Klik erop. Openen kan ook van overal in de stad:
```
/guhs vadsrijk ritme krant
```
Wat je leest, met "Verder »":
- de kop met de dag;
- een nieuwtje van de stad zelf;
- een algemeen nieuwtje met een echte burger erin;
- iets over gisteren: de knuffelkampioen van gisteren, of hoeveel klodders er zijn opgevrolijkt, of "er gebeurde niets";
- het weerbericht van vandaag. Dat klopt echt met het weer van de zone, inclusief de tijd en de ochtendmist van morgen;
- jouw knuffelmeter in deze stad;
- twee klusjes.

**Klusjes** (2 per stad per dag, voor iedereen dezelfde, de voortgang is per speler):
- **ZOEK**: "Oma Truus zoekt haar breinaald! Het ligt ergens bij ...".
  - Het verloren ding glinstert alleen voor jou, en dichtbij krijg je "Warm..." en "Heel warm!" te zien.
  - Ga erop staan, dan heb je het gevonden.
  - Lever het in bij de vrager of bij het bord.
- **BEZORG**: breng een pakketje naar een andere burger. Klik op die burger en je krijgt meteen je beloning.
- **HAAL**: haal iets op bij een burger en breng het terug naar de vrager of naar het bord.

Wat je krijgt per klusje: 20-50 brons en een knuffelpunt voor de stad.

Alleen burgers zonder questrol doen mee, dus een questklik wordt nooit overgenomen. De klusjes en het nieuws wisselen elke dag. Doe
alsof het morgen is:
```
/guhs vadsrijk ritme krant morgen
/guhs vadsrijk ritme krant vandaag
```

## Waar je op moet letten (voor jouw oordeel)
- Hoe het staat en ligt:
  - slapende guhs op het bed: ligt hij mooi op de matras?
  - de nachtbel en het bordje op de winkelplek;
  - de lampionring van de nachtmarkt: hangt hij niet in een dak?
  - het bord van het Pluizenblaadje naast het questbord.
- De hoeveelheid weer met de schuif op 70. Is het lief genoeg en niet te druk?
- Of de lichtjesgolf in de andere steden mooi aangaat. Daarvoor is `/time set 12400` en dan even kijken het beste.

## Gametests
```
runGameTestServer -Pgt=VadsrijkURitme
```
Er zijn 4 tests:
- `roosterBlokkeertNooitQuests`: geen enkele quest-guh gaat ooit naar bed, de nachtbel kan altijd iets openen, en een slapende winkelier opent toch zijn winkel;
- `knuffelmeter`: een keer per guh per dag, de beloningen en de knuffelreactie;
- `krantReset`: alle teksten bestaan in het Nederlands en het Engels, de klusjes zijn voor iedereen gelijk en wisselen per dag, en de voortgang reset;
- `stedenEnWeer`: de stadsvakken kloppen met de gebieden, de zones, en de weerklok.
