## Opslag-uitbreidingen + de VIP-grap (U-OPSLAGVIP)

Alles werkt alleen **in het Vadsrijk** (met je Vadsrijk-inventaris). Munten nodig? `/guhs vadsrijk munten 100000` (= 10 goud, in brons).

### 1. Alleen de EERSTE gekochte uitbreiding is te koop
- `/guhs vadsrijk opslag knapzak reset` en `/guhs vadsrijk opslag bank tabs 0` → begin opnieuw (je spullen blijven bewaard).
- `/guhs vadsrijk opslag knapzak` (of toets **I**)
  - **Kijk naar** de flapjes boven het scherm:
    - **1** = open;
    - **"?"** = nog te halen (verhaal zone 1 / zone 2);
    - **"+"** (geel) = te koop;
    - **grijs flapje met een slotje** = "Binnenkort!".
  - Bij een nieuw karakter is **flapje 5** grijs met een slotje. Hover erover: **"Binnenkort!"** + "Deze uitbreiding komt in een latere update."
  - Koop het "+"-vak (25 zilver). Daarna is er niks meer te koop. Flapjes die je niet via het verhaal krijgt, blijven "Binnenkort!".
- `/guhs vadsrijk opslag bank` (alsof je bij de bankier staat)
  - Vak 2 = **"+"** voor **10 zilver**. Vak 3, 4 en 5 = grijs met een slotje, "Binnenkort!".
  - Koop vak 2: daarna staan vak 3-5 nog steeds op "Binnenkort!".
  - Heb je vroeger al meer bankvakken gekocht? Die hou je gewoon.

### 2. De prijscurve (later zelf de getallen kiezen)
- `/guhs vadsrijk opslag prijzen` → in de chat staat de curve: uitbreiding 1-5 met de prijs voor knapzak en bank, en "te koop" of "Binnenkort!". Daaronder staat jouw stand.
- Prijs van de n-de uitbreiding = **basis × factor^n** (n = 0 voor de eerste). Standaard is dat knapzak 2500 × 4^n en bank 1000 × 4^n brons. De factor 4 is een **plaatshouder**: jij kiest later de echte getallen.
- Een tweede uitbreiding openzetten, zonder code:
  - `/guhs vadsrijk opslag prijzen max_koopbaar 2`
  - Open het scherm opnieuw: het volgende vak is nu "+" voor 4× de prijs (knapzak 1 goud, bank 40 zilver).
- De andere knoppen:
  - `/guhs vadsrijk opslag prijzen knapzak_factor 10`
  - `/guhs vadsrijk opslag prijzen bank_basis 2000`
  - De sleutels: `max_koopbaar`, `knapzak_basis`, `knapzak_factor`, `bank_basis`, `bank_factor`.
- Alles staat ook in `config/guhs-vadsrijk-balans.toml`, onder **[opslag]**. Een aangepast bestand wordt binnen 5 seconden opnieuw gelezen.
- Terugzetten: `/guhs vadsrijk opslag prijzen max_koopbaar 1`
- Snel testen hoeveel je gekocht hebt:
  - `/guhs vadsrijk opslag knapzak gekocht <0-4>`
  - `/guhs vadsrijk opslag bank tabs <0-4>`

### 3. De grap: "WORD GUH VIP++" (een parodie, er wordt nergens echt betaald)
- Meteen zien: `/guhs vadsrijk opslag vipgrap`. Daarmee wis je de vlag en gaat de knapzak open met de grap erin.
- Of op de echte manier:
  - `/guhs vadsrijk opslag vipgrap reset`
  - Klik in de knapzak of de bank op een geel **"+"**-vak.
- **Wat moet er gebeuren** (de eerste ~10 seconden):
  - Het hele scherm wordt een schreeuwerige deal:
    - **WORD GUH VIP++** in grote letters die knipperen en pulseren;
    - **VAN ~~50 EURO~~ VOOR MAAR 34,99!!!** (de 50 euro is doorgestreept);
    - een gouden rand met knipperende lampjes, confetti en twinkelende sterretjes;
    - "🔥 BESTE DEAL 🔥"-badges in de hoeken;
    - **⏰ NOG MAAR 00:09!**, dat aftelt;
    - een grote groene **KOOP NU!!!**-knop met een scheve "97% KORTING"-sticker;
    - een dikke **rode pijl** die ernaar wijst en heen en weer wipt.
  - Je hoort een kort belletjes-deuntje en dan een "ka-ching".
  - **Klik op KOOP NU!!!** of op "Nee bedankt, ik betaal liever de volle prijs": het scherm **schudt** en er gebeurt niks ("Er gebeurt niks. Dat is de grap.").
  - Onderaan staat klein: "(Grapje! Guhs heeft geen echte betalingen. Nergens. Nooit.)"
  - **Esc** slaat de grap over.
- **Daarna** verschijnt een gewoon praatvenster: **Winkelguh Snoes** (een guh met een strikje en een strohoed). Die zegt: "Hihi, nee is cap. Je kan het gewoon met in-game munten kopen: de eerste uitbreiding kost 25 zilver." (Bij de bank staat er 10 zilver.)
  - **Kopen** (of toets 1/Enter) koopt het vak meteen met munten.
  - **Nee, dank je** (of toets 2/Esc) sluit het venster.
- **Maar één keer per account:**
  - Klik daarna nog eens op een "+"-vak, ook bij een ander karakter of bij de bank: geen grap meer, maar de gewone "Klik nog een keer"-bevestiging.
  - `/guhs vadsrijk opslag vipgrap stand` → in de chat staat "al gezien: ja/nee".
- De grap bij de bank testen: `/guhs vadsrijk opslag vipgrap reset`, dan `/guhs vadsrijk opslag bank` en klik op "+".

### Gametests
- `runGameTestServer -Pgt=VadsrijkUOpslagVip` test twee dingen:
  - `alleenEersteKoopbaar`: alleen de eerste is te koop, de curve klopt, en een oude save telt als 1 gekocht;
  - `vipVlagEenKeerPerAccount`: de vlag gaat via de knop aan, geldt voor het hele account, en blijft na opslaan en laden.
