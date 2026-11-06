# SPIEK U-GILDEN: de Knuffelbonden (guilds), de Bondenvallei en de kastelen

**Naam:** het heet **Knuffelbond** (kort: "bond"). "Gilde" botst met de Ambachtsgilde (met de Gildemeester) die al in Hoogvads staat.
Het dal heet de **Bondenvallei**. De dev-commando's heten wel `/guhs vadsrijk gilde ...`.

Voor spelers is alles via **`/bond`**: een klikbaar paneel in de chat (alle knoppen zijn commando's).
In het Vadsrijk staat er ook een **⚑-knopje** links van je inventaris (onder de twee opslag-knopjes), dat opent hetzelfde paneel.

## 0. Snel alles klaarzetten (dev)
```
/guhs vadsrijk gilde maak ROZE Roze Ridders
/bond
```
- Dit maakt de bond gratis aan, zonder level-eis en zonder dat je bij de klerk hoeft te staan.
- Je krijgt een kavel in de Bondenvallei met een **Wachttoren** (kasteel niveau 1).
- In de chat zie je: `[ROZE] Roze Ridders  lv 1  ||||....  0/1000`, plus de knoppen.
- Beweeg over de bondsnaam: je ziet de **banier** (roze met een witte bloem en een magenta rand).

## 1. Oprichten (alleen in Hoogvads, zone 5)
```
/guhs vadsrijk gilde klerk
```
- Je staat nu bij **Bondsklerk Zegeltje** in de kanselarij van het paleis. Praat met hem.
- Kies **"Ik wil een bond oprichten"**:
  - zit je al in een bond: "één bond per ridder";
  - is zone 4 nog niet klaar én ben je onder lv 56: "kom later terug";
  - anders krijg je in de chat een klikbare **[Bond oprichten]**: het commando `/bond maak TAG Naam` staat dan klaar.
- Het kost **1 goud**. De naam is 3-20 letters/cijfers/spaties/streepjes, de tag 2-4 letters/cijfers. Beide zijn uniek.
- Overal anders zegt `/bond maak` dat het alleen bij de Bondsklerk kan.

## 2. Lid worden (vanaf zone 1)
- **Uitnodigen:** `/bond uitnodig <speler>`, of in `/bond leden` de knop [Iemand uitnodigen].
  - De ander krijgt een melding met [Ja] [Nee].
  - Uitnodigen mag een leider, officier of lid. Een rekruut mag het niet.
- **Bondenbord:** in elke stad staat een **Bondenbord-guh** (Knuffelkamp, Zwamzoet, Kandijkom, Honingtak, Lampionhaven, Hoogvads), plus één op het valleiplein.
  - De guh verschijnt rond het plein zodra je in de buurt komt. Praat met hem en kies "Welke bonden zoeken leden?".
  - Je krijgt een lijst met [Word lid] (open bond) of [Aanmelden] (een officier keurt goed).
  - Testen met 1 speler kan zo: `/bond werving open`, dan met een tweede account `/bond bord`.
- **Aanmeldingen goedkeuren:** in `/bond leden` staan ze onderaan met [Ja] [Nee]. Officieren krijgen ook meteen een melding.
- Nieuwe leden zijn **rekruut**. Na 200 bijgedragen bondsxp worden ze vanzelf **lid**.
- Een bond heeft max 50 leden.

## 3. Rangen en rechten
| rang | wat mag het |
|---|---|
| leider | alles: banier, leiderschap overdragen (♛), opheffen |
| officier | uitnodigen, rekruten/leden eruit zetten, rekruut→lid, werving, kasteel bouwen, de hele kluis |
| lid | uitnodigen, uit kluisvak 1 pakken |
| rekruut | chatten, dingen IN de kluis leggen, bouwmateriaal geven |

- `/bond leden` toont iedereen (● online, ○ offline) met knoppen ▲ ▼ ✖ ♛, alleen als jouw rang dat mag.
- `/bond verlaat`: een leider moet eerst het leiderschap overdragen. De laatste die vertrekt heft de bond op.
- `/bond ophef`: alleen de leider, en alleen als de kluis leeg is (er raakt nooit iets kwijt). De kas en het bouwdepot gaan naar de leider.

## 4. Lidmaatschap is per KARAKTER
- Wissel van ridder (`/guhs vadsrijk karakter`): je andere ridder zit niet in de bond.
- Het ⟨TAG⟩ bij je naam verdwijnt dan, en `/bond` zegt "Je zit (met dit karakter) niet in een Knuffelbond".
- Zo kan een ander karakter in een andere bond.

## 5. Bondschat, naamplaatje, banier
- `/bc hallo allemaal` stuurt één regel naar de bond.
- `/bond chat` zet de bondschat-modus aan/uit: wat je typt gaat naar de bond. Begin een regel met `!` om één keer naar iedereen te praten.
- In het Vadsrijk staat **⟨ROZE⟩** achter je naam: boven je hoofd, in de chat en in de spelerslijst (Tab). Het staat in de kleur van de banier.
- **Banier ontwerpen:** `/bond banier`.
  - Grondkleur: ◀ ▶. Per laag (max 3): patroon ◀ ▶, kleur ◀ ▶, ✖ weg, [+ laag].
  - Alle banieren in jullie kasteel kleuren meteen mee, en het naamplaatje krijgt de grondkleur.

## 6. Bondsxp, levels, perks, weektaken
- Bondsxp komt vanzelf van leden:
  - wezen opgevrolijkt: +1 (elite +3);
  - baas: +40 (plus een trofee);
  - quest: +15;
  - level omhoog: +10;
  - ambachts-level: +10;
  - 1 per 50 ambachts-xp.
  - Per lid max 600 per dag.
- **Snel testen:**
```
/guhs vadsrijk gilde level 6
/bond perks
```
  - Je ziet de perks met ✔ tot level 6. Het dev-commando zegt ook: bank-tabs 3, reiskorting 10%, teleport elke 15 min.
- **Perks** (gemak, GEEN stats):
  - Bondsrust (lv 2/6);
  - kluisvakken 1 t/m 5 (lv 1/3/5/7/9);
  - reiskorting op de luxe reiskaart-vlucht 5/10/15% (lv 4/6/8);
  - teleport naar het kasteel elke 30/15/10/5 min (lv 1/5/7/10).
- **Bondsrust testen:**
```
/guhs vadsrijk gilde rust
```
  - De rustpot vult zich alsof je 24 uur offline was.
  - Versla wat wezens: je krijgt +50% gevechts-xp. In `/bond` staat "Bondsrust: nog X xp".
- **Weektaken:** `/bond taken` toont er 3, met een balkje. Ze zijn nieuw elke maandag om 06:00.
  - `/guhs vadsrijk gilde week klaar`: alle drie klaar. Je ziet 3x "Weektaak klaar: +400 bondsxp en 10 zilver in de kas" en "Alle drie de weektaken klaar!".
  - `/guhs vadsrijk gilde week nieuw`: andere taken.
- **Reiskorting:** op bondslevel 4+ kost een luxe vlucht met de reiskaart (niet de eerste keer naar een plek) minder dan 4 zilver.

## 7. De Bondenvallei en het kasteel
```
/guhs vadsrijk gilde poort
```
- Je staat bij de **Bondenpoort** in het Staartpark van Hoogvads (het prieel), met een ring van lichtjes.
- Praat met **Poortwachter Wolkenpoort** en kies "Naar de Bondenvallei!".
- **Het valleiplein** is een zwevend roze eiland met een fontein, bankjes en lantaarns.
  - Noord staat de **poortboog** (paarse lichtjes): loop erdoorheen en je bent terug in Hoogvads.
  - **Pleinwachter Pluim** brengt je terug, of naar je kasteel. Er staat ook een Bondenbord.
- Ten zuiden van het plein liggen de **kavels** van de bonden: zwevende eilanden met kersenhouten vlonderwegen en lantaarnpalen.
  - Elk kavel heeft een bloeiende azalea-haag met een poortje.
  - Aan de open randen staan hekjes (verbonden), zodat je niet van het eiland valt.
  - Val je toch: een wolkje brengt je terug naar het plein.
- **Naar je kasteel:**
  - `/bond thuis` of [Naar ons kasteel] werkt vanuit **elke stad** en overal in de vallei. Wachttijd 30 min (korter met bondslevel).
  - Dev zonder wachttijd: `/guhs vadsrijk gilde tp`. Wachttijd wissen: `/guhs vadsrijk gilde tpklaar`.
- **In de Wachttoren (niveau 1):**
  - **teleportsteen** (lodestone) in het midden: rechtsklik → [Terug naar waar ik was] (gratis), [Valleiplein], [Knuffelkamp] [Zwamzoet] ... (alleen steden die je al mag bereizen; gebruikt de wachttijd);
  - **bondskluis** (de kisten tegen de westmuur);
  - een trap naar boven (elke verdieping heeft een trap met een reling) en een dakterras met kantelen, hoektorentjes en een vlaggenmast met jullie banier.
- **Uitbouwen:** `/bond kasteel` toont wat het volgende niveau nodig heeft: bondslevel, kas en bouwmaterialen van de Ambachten.
  - [Bouwmaterialen geven] neemt wat nodig is uit je inventaris en je materiaaltas.
  - [+1z] [+10z] [+1g] stopt munten in de kas.
  - [Bouwen!] mag een officier of de leider.
  - Snel testen:
```
/guhs vadsrijk gilde kasteel vol
/bond kasteel
```
  - Klik [Bouwen!]: het kasteel wordt opnieuw opgebouwd op het volgende niveau.
  - Of direct naar een niveau: `/guhs vadsrijk gilde kasteel 5`.
- **De niveaus:**
  1. **Wachttoren**: toren, teleportsteen, kluis.
  2. **Torenhof**: ringmuur met kantelen en steunpilaren, poortgebouw met twee torentjes met spitsen, en de **Trofeeënzaal** (lange hal met puntdak, 3 kroonluchters en 12 sokkels).
  3. **Burcht**: + twee hoektorens met spitsen, + westvleugel met **smidse** (smeedtafel, aambeeld) en **keuken** (rookoven). Rechtsklik opent de werkplaats van dat Ambacht.
  4. **Vesting**: + vier hoektorens, + oostvleugel met **naaiatelier** (weefgetouw) en **brouwerij** (brouwstandaard), bloembedden.
  5. **Knuffelslot**: een grote donjon (3 verdiepingen) met een troon, kroonluchters, banieren, een dakterras met een hoge torenspits en vier torentjes.
- **Trofeeën:** versla met een bondslid een baas, dan komt er een trofee bij (gouden beeldje + naam) in de Trofeeënzaal (niveau 2+).
  - Testen:
```
/guhs vadsrijk gilde kasteel 2
/guhs vadsrijk gilde trofee vadsrijk.bazen.koning_klodder
```
  - Je ziet de trofee in de hal, en in de chat "Nieuwe trofee ...".

## 8. De bondskluis (rechtsklik een kist in je kasteel)
- Je ziet een gewone kist van 6 rijen. De onderste rij zijn knoppen:
  - roze ruitjes = vakken, groen = het vak waar je nu in kijkt, grijs = op slot (met het bondslevel dat nodig is);
  - papiertje = wat jij mag;
  - trechter = sorteren (als je eruit mag pakken).
- Rekruut: alleen erin leggen. Lid: ook uit vak 1 pakken. Officier/leider: overal.
- Bundels, shulkers en gebonden spullen mogen er niet in.
- Kijken twee leden tegelijk in hetzelfde vak, dan zien ze elkaars zetten meteen.
- Wordt iemand uit de bond gezet, dan sluit zijn kluisscherm.
- Dev: `/guhs vadsrijk gilde bank` opent de kluis overal.

## 9. Ranglijst + website
- `/bond ranglijst` toont de top 10 (level, xp, leden, kasteel).
- Voor de site: `/guhs vadsrijk gilde export` schrijft `hiscores/bonden.json`, in het formaat van de andere hiscores.
  - Alleen als de live export aanstaat (`/guhs vadsrijk web nu`).

## 10. Overig dev
- `/guhs vadsrijk gilde info`: alle bonden met level, kavel, kasteel, kas en depot.
- `/guhs vadsrijk gilde xp <n>`: bondsxp erbij.
- `/guhs vadsrijk gilde uitnodig <speler>`: zet die speler direct in je bond.
- `/guhs vadsrijk gilde vallei`: naar het plein.
- `/guhs vadsrijk gilde kasteel bouw`: het huidige niveau opnieuw neerzetten.
- `/guhs vadsrijk gilde wis`: alle bonden vergeten (de blokken blijven staan).
