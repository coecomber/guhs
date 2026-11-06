# SPIEK U-AUDIO2: bazenmuziek met fases, fanfares, wereldgeluiden, NPC-stemmetjes

Besluit V4 (feedbackronde 4):
- eigen bazenmuziek met fases + een eigen overwinningsfanfare;
- een eigen thema voor de Grauwe Droom;
- wereldgeluiden SUBTIEL (les: de wind in de havenstadjes was te aanwezig);
- NPC-stemmetjes met de bestaande guh-geluiden, hier en daar een pieppiepmuisje of blub.

> Smaak gaat voor. Klinkt iets niet goed? Zeg welk stuk en wat eraan schort ("te druk", "ander instrument", "fanfare te lang", "bijen te hard"). Muziek = één regel in `tools/vadsrijk/geluid/stukken.py`; volumes van de wereldgeluiden = één tabel in `WereldGeluiden.java`.

## Snel alles horen
| Wat | Commando |
|---|---|
| Klikbare lijst van ALLES (klik = luisteren) | `/guhs vadsrijk geluid` |
| Baasthema + na 12 s de laatste fase erin | `/guhs vadsrijk geluid fase grauwe_droom` |
| De fanfare van een baas | `/guhs vadsrijk geluid fanfare zeeslurf` |
| Een NPC-stemmetje | `/guhs vadsrijk geluid stem groet` |
| Een wereldgeluid (4 blokken voor je) | `/guhs vadsrijk geluid wereld bijen` |
| Wat hoort er hier (wereldgeluiden) | `/guhs vadsrijk geluid wereldinfo` |
| Alles stil (ook een baas-voorbeeld) | `/guhs vadsrijk geluid stop` |

## 1. Bazenmuziek met fases
Elke baas heeft nu een **eigen thema** en een **laatste-fase-versie**:
- Het is hetzelfde stuk, noot voor noot even lang.
- Er komen drums, pauken, een hoog strijkers-ostinato, koor en de melodie een octaaf hoger op koper bij.
- Bij de overgang naar de laatste fase loopt de muziek in ~2 s naadloos over (crossfade op dezelfde plek in het stuk).

Testen zonder gevecht:
1. `/guhs vadsrijk geluid fase vahoegnir`
   → Het gewone thema start. Na **12 s** zwelt de laatste fase erin, zonder hapering of sprong.
2. `/guhs vadsrijk geluid stop` → Stil.
3. Doe hetzelfde met een paar andere bazen: `/guhs vadsrijk geluid fase gorgonzola`, `... fase mika_imker`.

Nieuwe thema's (die bazen leenden eerst een thema of kregen "algemeen"):
| Baas | Commando | Karakter |
|---|---|---|
| De Mika-imker (zone 3) | `/guhs vadsrijk geluid fase mika_imker` | zoemend strijkers-ostinato (bijen!), fagot + klarinet |
| **De Grauwe Droom** (zone 5) | `/guhs vadsrijk geluid fase grauwe_droom` | dromerig **slaapliedje-baasthema**: speeldoos-wals, celesta, glaspad. Laatste fase: donker koor, taiko, tremolo |
| Donderbuik (zone 5) | `/guhs vadsrijk geluid fase donderbuik` | knorrig en zwaar: tuba + hoorn, taiko-donder, kalimba-regendruppels |
| Opzichter Gorgonzola (zone 5) | `/guhs vadsrijk geluid fase gorgonzola` | knorrig-komische ketelkamer-polka: fagot + trombone, houtblok |
| Kootje Zilverstaart (zone 5) | `/guhs vadsrijk geluid fase zilverstaart` | sluwe tango: viool + accordeon, staccato strijkers |

De zone 1-4-bazen houden hun thema (Zeeslurf, Vliegervanger enz.) en krijgen er een laatste fase + fanfare bij.

In een echt gevecht:
1. Ga naar een baas, bijvoorbeeld de Zeeslurf of de Grauwe Droom.
2. Vecht tot de LAATSTE fase. Dat is de laatste fasekaart, bij de Grauwe Droom fase 3.
   → De muziek zwelt dan aan.
3. `/guhs vadsrijk geluid info` → Achter de baas staat dan "(laatste fase)".

## 2. Eigen overwinningsfanfare
Een verslagen baas speelt nu zijn **eigen fanfare** (~5 s), in plaats van de gedeelde stinger.
- Het is zijn eigen toonsoort in majeur (hij is opgevrolijkt!), met zijn eigen hoofdinstrument en een eigen loopje.
- De Grauwe Droom krijgt een zacht speeldoos-slotje.
- De baasmuziek maakt meteen plaats.

| Testen | Commando |
|---|---|
| Lijst, klik door de fanfares | `/guhs vadsrijk geluid` (regel "fanfare:") |
| Eén fanfare | `/guhs vadsrijk geluid fanfare grauwe_droom` |

De fanfare volgt de schuif **stingers**.

## 3. Wereldgeluiden (SUBTIEL)
Het zijn zachte geluidjes van plekken vlak bij je. Er klinken er hooguit 4 tegelijk mee, er komt hooguit één geluidje per ~2-4 s, en elk geluidje heeft zijn eigen lange pauze. Op ~12 blokken hoor je ze niet meer.

| Bron | Wanneer | Voorbeeld |
|---|---|---|
| fonteintje | een KLEIN watervalletje (1-3 blokken): fonteinen, spuwers. Grote watervallen hadden al hun eigen ruis (die is niet verdubbeld). | `/guhs vadsrijk geluid wereld fontein` |
| bijen | bijenkorf / bijennest | `... wereld bijen` |
| smidse | aambeeld / smeedtafel: 2-4 zachte hamertikjes | `... wereld smidse` |
| markt | 3+ NPC-guhs dicht bij elkaar: zacht guh-geroezemoes | `... wereld markt` |
| windgong | klok / windgong-blok | `... wereld windgong` |
| kampvuur | brandend kampvuur: een zacht extra knisperen | `... wereld kampvuur` |
| kraak | gondelstations (Kandijkom) en liften (zone 3) | `... wereld kraak` |
| grot-echo | onder de grond lopen: je voetstappen krijgen een zachte echo | ga een grot in en loop |

Testen:
1. Ga naar Honingtak (bijen, liften) of de Kandijkom-gondel. Typ `/guhs vadsrijk geluid wereldinfo`.
   → Je ziet wat er gevonden is en wat je nu hoort.
2. Luister een minuut.
   → Het moet zacht en spaarzaam zijn: af en toe een geluidje, nooit een muur van geluid.
3. Loop een grot in.
   → Je stappen krijgen een zachte echo. Bij sluipen is er geen echo.
4. Zet de schuif **wereldgeluiden** op 0.
   → Alles hierboven is weg. Het gewone omgevingsgeluid blijft.

## 4. NPC-stemmetjes
Ze gebruiken de BESTAANDE guh-geluiden van de Guhs-mod. Elke NPC heeft zijn eigen toonhoogte (op naam), dus dezelfde NPC klinkt altijd hetzelfde.
- **Klik op een NPC** of open een gesprek → een "guh!" (groet).
- Een pagina die iets **vraagt** (?) → een nieuwsgierig "oeh?".
- Een **blije** pagina (bedankje, quest ingeleverd) → een giechel.
- Mika-sprekers → Mika-geluidjes. De verteller en je eigen regels zijn stil.
- Af en toe hoor je een **pieppiepmuisje** of een **blub**. Aan de kust van zone 4 hoor je vaker blub.

Testen:
1. Klik in Zwamzoet op een paar NPC's.
   → Elke NPC zegt "guh" in zijn eigen toonhoogte, en maar één keer per klik (niet dubbel bij het openen van het gesprek).
2. Blader door een gesprek.
   → Bij een vraag een "oeh?", bij een blije pagina een giechel. Het typ-getik blijft zoals het was.
3. Losse geluidjes: `/guhs vadsrijk geluid stem groet`, `... stem vraag`, `... stem dank`, `... stem mika`, `... stem piep`, `... stem blub`.
4. Zet de schuif **stemmen** op 0.
   → Geen stemmetjes meer. Het typ-getik heeft zijn eigen schuif (tandwiel in het praatscherm).

## Schuifjes
**Mods → Guhs → Config** (`guhs-vadsrijk-geluid.toml`). De oude schuiven blijven; er zijn er twee bij:
- **wereldgeluiden** (standaard 60). Volgt ook de schuif **Omgeving**.
- **stemmen** (standaard 70). Volgt ook de schuif **Vriendelijke wezens**.

Bazen-fases volgen **bazenmuziek**, fanfares volgen **stingers**.
