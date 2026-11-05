## Itemisatie 2.0 (T-ITEMS): nieuwe vakken, nieuwe stats, Erfstukken, smid-vangnetten, drankjesriem

Alles onder `/guhs vadsrijk items ...` (op 2). Test in het Vadsrijk (de vakken en de riem werken alleen daar).

### 1. Nieuwe vakken: 2e ring, armband, handschoenen, cape
```
/guhs vadsrijk items nieuwe
/guhs vadsrijk items ringen
```
- Je krijgt alle 17 nieuwe stukken (Zeldzaam, jouw niveau) en 2 ringen.
- Open je inventaris (E).
- **Rechts staat een kolom van 7 vakjes:** R, R2, K, O, A, H, C (Ring, Ring 2, Ketting, Oorbel, Armband, Handschoenen, Cape).
- **Aandoen:** Shift+klik een stuk in je inventaris, of rechtsklik het in je hand.
  - De 1e ring gaat naar R, de 2e naar R2.
  - Een 3e ring wisselt met de ring in R.
- **Afdoen:** klik op het vakje.
- **Wat moet je zien:**
  - de tooltip van een armband/oorbel/handschoen/cape toont lagere getallen dan een ring van vroeger (het "vakbudget": 70 % voor sieraden, 60 % voor handschoenen en cape);
  - na het aandoen stijgen je stats.
- **Overzicht en reset:**
  - `/guhs vadsrijk items slots` laat zien wat je draagt;
  - `/guhs vadsrijk items uittrekken` doet alles af.
- **Stats bekijken:** `/guhs vadsrijk items stats` (de 4 nieuwe stats staan in het paars).
- **De inventariswissel:** loop het Vadsrijk uit en weer in. Je vakken moeten er nog zijn, en buiten het Vadsrijk tellen ze niet.
- **Sets:** de Kloddergrijpers, Sporencape, Kristalhandschoenen, Buitcape, Bloesemhandjes, Schorshandschoenen en Wortelcape tellen als setstuk. De tooltip zegt bij welke set.
  - Test: 3 stukken van de Klodderset + de Kloddergrijpers = de 4-stukbonus.
  - Voorbeeld: `/guhs vadsrijk items geef klodderkapje` (en klodderhesje, klodderbroek), plus de grijpers.
- **Losse stukken:** `/guhs vadsrijk items geef <basistype> [zeldzaamheid] [level]` (tab-aanvulling op de id).

### 2. Duidelijke stat-namen + 4 nieuwe stats
Hover over gear. Elke regel is gewoon te lezen, bijvoorbeeld:
- "+3 Kracht (schade van dichtbij)"
- "+2,5% Kans op kritieke treffer"
- "+4% Kortere wachttijd vaardigheden"

**De 4 nieuwe stats:**
- **Verdediging negeren %:** neemt dat deel terug van wat je klap kleiner maakt: een slecht element (x0,7), schilden en blokken van wezens, en hun Knuffelschild. Max 50 %.
- **Schade tegen bazen %:** meer schade op elke baas.
- **Leven per treffer:** zoveel Vads terug per raak (max 4 per tik bij een vlakaanval).
- **Kans op effect %:** je element zet zijn status:
  - vuur = Brandend
  - natuur = Gesporen
  - water = Plakkerig
  - roze = kort Verdoofd (niet op bazen)

  Max 40 %.

**Waar ze vallen:**
- alleen op wapens, ringen, handschoenen, armbanden, kettingen, oorbellen en capes;
- nooit op helm, borst, benen of voeten.

**Test:**
```
/guhs vadsrijk items geef pluisarmband legendarisch
```
Dat stuk heeft 4-5 bonussen; herrol het een paar keer bij de smid tot er een nieuwe stat op staat. Sla daarna een wezen en kijk naar de Vads/status.

### 3. Smid: tabs, geduld (pity), Knuffeltalisman, polijsten, previews
```
/guhs vadsrijk items smidspul
/guhs vadsrijk items smid
```
`smidspul` geeft steentjes, knabbels, draad, 5 talismannen, 5 groeipluis en 50 zilver.

**Bovenaan het aambeeld staan tabs:** Sterren · Bonussen · Groeien · Overvadsen.

**Sterren:**
- Zichtbaar: de kans, het risico, de prijs, en "Als het lukt:" met de echte stat/schade-verandering van de volgende ster.
- **Geduld:**
  - elke mislukte poging geeft +5 % op de volgende (tot ster 7);
  - het balkje "Smidguh wordt ongeduldig..." vult;
  - na een succes is het weer leeg.
  - Snel testen: neem een stuk in je hand, `/guhs vadsrijk items pity 4`, kies het bij de smid → "+20.0% geduld".
- **Knuffeltalisman:**
  - vink "☐ Knuffeltalisman" aan;
  - mislukt een ster die je normaal een ster kost? Dan krijg je "De Knuffeltalisman beschermde je ster!" en is er 1 talisman op;
  - "Koop 1" koopt er een voor munten;
  - verpluisd kan nog steeds (vanaf +6).
  - Sterrenvangen bestaat niet (zoals besloten).

**Bonussen:**
- Herrollen werkt als vroeger (klik een bonus = ♥ vasthouden).
- **Polijsten:** klik een bonus en je ziet "kan X tot Y worden (nu Z)". De knop Polijsten geeft een nieuwe waarde binnen dat bereik (kan ook lager!).

### 4. Erfstukken: Vertrouwd, Doorgroeien, Overvadsen
**Vertrouwd:**
- Gedragen gear (wapen in je hand, harnas, alle 7 vakken) krijgt item-xp per opgevrolijkt wezen (elite 3, baas 15).
- Tooltip: "Vertrouwd 2 (130/280 xp): +4% basisstats"; bij 5 een gouden regel.
- Bij elk niveau komt er een chatberichtje.
- Snel: `/guhs vadsrijk items vertrouwd 300` (alles wat je draagt).

**Doorgroeien:**
```
/guhs vadsrijk items legendarisch
```
- Je krijgt een Legendarisch wapen van je klasse dat al Vertrouwd 5 is.
- Smid → tab **Groeien**: je ziet "Niveau X → X+10" en de stat-verschillen, en wat het kost (Groeipluis + munten).
- "Laat groeien!" → het stuk is Gebonden, "Doorgegroeid I", Vertrouwd begint opnieuw.
- **Grenzen:**
  - Legendarisch groeit 2x (de 3e keer zegt hij dat het op is);
  - Guhddelijk groeit altijd (tot niveau 50);
  - Episch en lager groeit nooit.
- Groeipluis valt bij elke baas (1 per helper).

**Overvadsen:**
- Smid, tab **Overvadsen**:
  1. klik een ander (oud) stuk;
  2. kies links een bonus van het oude stuk, en rechts welke bonus hij vervangt (of "een nieuwe erbij");
  3. je ziet de nieuwe waarde en de kans (15 %) op een "lieve mislukking" (iets lager).
- Het oude stuk verdwijnt; het doel blijft altijd heel.
- Het mag niet als:
  - het doel die bonus al heeft;
  - de bonus niet op dat soort stuk mag;
  - het stuk verpluisd is.

### 5. Drankjesriem
```
/guhs vadsrijk items drankjes
```
- Dit vult de riem met Vads-, energie- en Vahoegdrankjes.
- **Links naast je hotbar** staan 3 vakjes met de toetsen **Y / I / O**. Druk erop om meteen te drinken.
  - Het gedeelde drankjes-wachten (10 s) telt: het vakje wordt donker.
  - Spam = "Je drankjes moeten nog even afkoelen."
- **In je inventaris:** een kolom "Riem" rechts van de sieradenkolom.
  - Klik met een drankje op je cursor = erin.
  - Klik leeg = eruit.
  - Shift+klik een drankje in je inventaris = in de riem.
- Alleen drinkbare dingen passen (geen steen e.d.).
- De toetsen kun je veranderen in Opties → Besturing → Vadsrijk.

### 6. Balans (één plek)
```
/guhs vadsrijk balans toon
/guhs vadsrijk balans items budget_sieraad 0.8
```
- `toon` laat de sectie `[items]` zien.
- Het tweede voorbeeld zet het sieradenbudget op 80 %, live.
- Alle sleutels en waarom: `NOTITIES_T_ITEMS.md` → "Balans".
