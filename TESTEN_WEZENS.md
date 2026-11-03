# Testen: de wezens van zone 1 (Het Vadsrijk)

Alle commando's werken als op (creative of survival). In creative vallen de wezens je niet aan; zet survival aan om te vechten.

## Snel alles zien
1. `/guhs vadsrijk proeftuin` (de testplek van kern), daarna `/guhs vadsrijk level 10` en `/guhs vadsrijk vol`.
2. Een wezen maken: `/guhs vadsrijk wezens spawn <soort> [level] [elite]`, bijvoorbeeld `/guhs vadsrijk wezens spawn hoedjesbol 8`.
   Soorten: klodder, kloddertje, bubbelklodder, stuiterklodder, moederklodder, hoedjesbol, sporenpuffer, zwamwachter, mosgeest,
   druipslijm, kristalklodder, knaag, glimzwam, tempelridder, sporenpriester.
3. Een elite met een vaste eigenschap: `/guhs vadsrijk wezens affix klodder splitsend 4` (gloeiend / snel / splitsend / schildend).
4. Opruimen: `/guhs vadsrijk wezens opruimen`.

## Waar op letten
- **Uiterlijk**: ontvadst = grauw-paars met een chagrijnig guh-gezichtje (zware oogleden, wenkbrauwen). Slijmpjes en zwammen
  hebben guh-oortjes. De Grauwe Knaag is expres géén guh (knaagoortjes, kraaloogjes, tandjes).
- **De plof**: vrolijk ze op. Ze worden fel van kleur met een blij gezichtje en blosjes, roze stofje en hartjes, huppelen weg en
  verdwijnen. Geen doodsanimatie.
- **Elites**: 25 % groter, gouden gloed, deeltjes per eigenschap, betere loot. Splitsend laat 2 kleintjes achter.
- **Elk wezen zijn trucje**:
  - Bubbelklodder: houdt afstand en spuugt belletjes (Plakkerig).
  - Stuiterklodder: een roze cirkel op de grond, dan springt hij er hoog op. Stap opzij!
  - Moederklodder: spuugt kloddertjes op 66 % en 33 % en spat bij de plof uiteen in 3 kloddertjes.
  - Hoedjesbol: een rode streep, dan rent hij. Tegen een muur wordt hij duizelig.
  - Sporenpuffer: groene wolk = Gesporen.
  - Zwamwachter: van voren "Geblokt!". Loop om hem heen, of sla net na zijn klap.
  - Mosgeest: zweeft rond je en zwiept soms opzij ("Zwiep!").
  - Druipslijm: hangt ondersteboven aan een plafond en valt op je (test: maak een overkapping van 4 hoog en spawn hem eronder).
  - Kristalklodder: weerkaatst soms spreuken ("Weerkaatst!"). Klappen werken altijd.
  - Grauwe Knaag: komt met 2-3 tegelijk (via een spawner), bijt en springt terug.
  - Glimzwam: rent weg en heelt de anderen (groene lijntjes). Eerst hem!
  - Tempelzwam-ridder: rode strook, dan een zware zwaai.
  - Sporenpriester: blijft op afstand, schiet sporen en geeft de anderen Vahoeg en een schildje.
- **Kampklodder**: `/guhs vadsrijk wezens kampklodder` zet hem bij je voeten. Rechtsklik = aaien (hartjes, wiebel, "Blub!").
- **Vahoegnir**: `/guhs vadsrijk wezens overvlucht` (laag langs, brult boven je hoofd) of `/guhs vadsrijk wezens draak 60` (een
  rondje boven je). Let op: flappende veervleugels, guh-oren als hoorns, grijze pluis die naar beneden dwarrelt.
- **Loot en sets**: `/guhs vadsrijk wezens set klodder 5` en `/guhs vadsrijk wezens set zwam 5` geven een hele set. Trek ze aan
  en kijk in de tooltip: "Klodderset (4/4)" met twee groene bonusregels. `/guhs vadsrijk wezens loot moederklodder 6 true` rolt de
  elite-loot van een wezen.
- **Spawners**: in de echte wereld (na de wereld-slice) lopen ze vanzelf in de Klodderweide, het Sporenwoud, de Druipgrotten en de
  Zwamtempel. `/guhs vadsrijk wezens spawners` telt ze.

## Bekende gaten
- Set-bonus +4 Behendigheid/Wijsheid telt pas mee als kern één regel toevoegt (de rest van de bonussen werkt al).
- Geen sieraden (kern heeft nog geen ring/ketting/oorbel-slots).
- Harnas is (nog) niet zichtbaar op je poppetje.
- De grot-plekken (hoogte) zijn een gok tot de wereld-slice ze vastlegt.
