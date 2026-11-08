# SPIEK U-Z6RAID: De Sterrendovende Toren (de eerste raid)

Branch `110-u-z6raid`. Alles onder `/guhs vadsrijk z6raid ...` (gamemaster). Speel in de Vadsrijk-dimensie.

## Hoe de raid werkt (kort)
- **2 tot 10 spelers.** Alleen kom je er niet in.
- De toren aan de Moederoase heeft een deur (die bouwt u-z6wereld). Loop je erin, dan sta je in **De Wachtzaal**: de begane grond van de binnentoren (een ruimte onder de toren).
- Ga met 2-10 spelers **3 tellen op de gouden startcirkel** staan. Partyleden die in de Wachtzaal staan, gaan mee.
- **Eén groep tegelijk.** Is de toren bezet? Dan kom je via de cirkel in de **rij**. Ben je aan de beurt, dan krijg je een bericht en ga je vanzelf naar boven als er 2+ van jullie in de Wachtzaal staan.
- **Vier verdiepingen**, elk met een trap. De trap omhoog zit dicht met paars **sterrenvlies** tot de baas van die verdieping is opgevrolijkt.
- **Flauwvallen is mild.**
  - Je wordt wakker op het trapbordes van de verdieping waar je bent, en je mag gewoon weer meedoen.
  - Eénvads-ridders gaan in de raid NIET met pensioen.
- **Wipe:** staat er niemand van de groep meer levend in de zaal terwijl het gevecht bezig is? Dan begint de verdieping na 3 tellen opnieuw. De wipeteller gaat dan +1.
- **Elke clear geeft een kist** (er is geen weeklimiet). Na een clear brengt de lichtzuil in het midden van het dak je naar buiten. De uitgang in de Wachtzaal is de paarse boog in het westen.
- Rechtsboven zie je de **raidframes**:
  - tot 10 spelers met hun Vads, flauw/weg/buiten;
  - de verdieping en de baas;
  - de gevechtsklok tot de woede (enrage);
  - de wipes, de totale tijd en het aantal spelers.

## Snel testen (alleen, met nepspelers)
```
/guhs vadsrijk z6raid bouw
/guhs vadsrijk z6raid start 4
```
- `bouw` plaatst de binnentoren onder de toren op 4140,16,-688. Dat gebeurt ook vanzelf als je er dichtbij komt.
- `start 4` start de raid als groep van 4: jij plus 3 nepguhs, die grijs in de raidframes staan.
- Je staat dan op het bordes van verdieping 1. **Loop de zaal in** (naar het westen): de Zandwachter staat in het midden.
- Wat je moet zien:
  - de intro;
  - de baasbalk;
  - de raidframes rechts;
  - bij 4 spelers 2 pilaren en 2 spiegels (de bazen schalen x2.35).

```
/guhs vadsrijk z6raid info
/guhs vadsrijk z6raid verdieping 2
/guhs vadsrijk z6raid verdieping 3
/guhs vadsrijk z6raid verdieping 4
/guhs vadsrijk z6raid wipe
/guhs vadsrijk z6raid klaar
/guhs vadsrijk z6raid stop
```
- `info`: wie er binnen is, de rij, de HP van de 4 bazen en welke trappen open zijn.
- `verdieping 2`/`3`/`4`: spring naar die verdieping (de trappen eronder gaan open).
- `wipe`: forceer een wipe. De baas komt vol terug, de teller gaat +1 en je staat weer op het bordes.
- `klaar`: meteen een clear. Je krijgt een kist, de sterren in de koepel gaan branden en je krijgt de titels.
- `stop`: de toren is weer vrij.

Een baas zonder groep (creatief vecht je mee):
```
/guhs vadsrijk z6raid baas 1
```
(Dit kan ook met 2, 3 of 4.)

Een fase overslaan (sta bij de baas):
```
/guhs vadsrijk z6raid fase 2
/guhs vadsrijk z6raid fase 3
```

Teleporteren:
```
/guhs vadsrijk z6raid tp wachtzaal
/guhs vadsrijk z6raid tp poort
/guhs vadsrijk z6raid tp 3
```
- `tp poort` zet je vlak bij de torendeur. Loop erin en je komt in de Wachtzaal.

## Verdieping 1: De Zandwachter
- De **zandhuid**: hij krijgt maar half zoveel schade.
- **De zandloper** (cast-balk, 25 s):
  - Op de twee roze platen (west en oost) moet TEGELIJK iemand staan, 2 tellen lang.
  - Dan draait hij om: hij is verdoofd en 18 s kwetsbaar. In zijn **rug** doe je x1,8 schade.
  - Lukt het niet, dan volgt **Begraven**: 45 % schade.
- **Zandpilaren:** sla ze binnen 20 s kapot, anders bouwt hij een zandmuur.
- **Zandgolven:** rode banen; zoek het gat.
- **Zandvuist:** een gele cirkel. Ga er SAMEN in staan (stack).
- **Spetters:** ga uit elkaar (spread).
- **Zandstorm:** een paarse cast-balk. Onderbreek die door schade te doen.

Mechanieken testen:
```
/guhs vadsrijk z6raid mech zandloper
/guhs vadsrijk z6raid mech omdraaien
/guhs vadsrijk z6raid mech pilaren
```

## Verdieping 2: De Spiegelvizier (Meester Glans)
- Hij verstopt zich tussen identieke spiegelbeelden. **Alleen de echte fonkelt af en toe** (kijk goed!).
- Rechtsklik een **draaispiegel**: die wijst dan naar het volgende beeld.
- Staan 2 lichtbundels op hetzelfde beeld?
  - Is het de echte: hij is 20 s kwetsbaar.
  - Is het nep: er stapt een spiegelkloon uit en iedereen krijgt 10 %.
- **Wie is wie?:** elke 30 s wisselt hij twee spelers van plek.
- Bij **20 %** breekt de zaal: er regenen scherven.

```
/guhs vadsrijk z6raid mech verstop
/guhs vadsrijk z6raid mech ontmasker
/guhs vadsrijk z6raid mech fout
```

## Verdieping 3: De Gedoofde Sterrenbeelden
- Er zijn er drie tegelijk:
  - **De Grote Knuffel** (taai):
    - Knuffelklem: een cirkel op het doelwit; de rest gaat weg.
    - Berenbrul: een donut, dus ga naar BINNEN.
  - **De Vliegende Bultguh:** stormt over een rode baan.
  - **De Kleine Pluis:** heelt de andere twee. Onderbreek haar **Sterrenlied**.
- Op 0 zijn ze niet verslagen maar **"uit"** (ze stralen). Dan loopt er een **venster van 10 s**: alle drie moeten erin uit.
- Te laat? Dan steken ze elkaar weer aan, op 40 %.
- In de raidframes zie je drie balken plus het venster.

```
/guhs vadsrijk z6raid mech uit
```
(Dit zet alle drie meteen uit.)

## Verdieping 4: Hertogin Ringeling (het dak)
- **Fase 1, Tien Tenen:**
  - Ze schopt ringen. Je ziet een roze vangcirkel in een rode cirkel.
  - **SPRING** in de roze cirkel vlak voor de ring landt: dan vang je hem en vliegt hij terug.
  - Niet gevangen? Dan krijg je het effect van de ring: vuur, ijs, lichtdief, duw of wissel.
- **Fase 2, De Lege Lens** (60 %):
  - Het wordt donker.
  - Pak een **sterrenscherf** (rechtsklik) en breng hem naar een gedoofd **sterrenbaken** (rechtsklik of erlangs lopen).
  - Branden alle bakens? Dan komt het licht terug: ze is verdoofd en krijgt x1,5 schade.
  - Zolang de bakens niet branden, zakt ze niet onder 25 %.
- **Fase 3, Ringdans** (25 %):
  - Lage ring = springen, hoge ring = sluipen.
  - Na 3 ringen barst de Lens: ze valt (x2 schade).
- **Einde:** ze wordt **opgevrolijkt** en praat. De sterren in de koepel gaan branden en ze keert zich tegen de Keizer.

```
/guhs vadsrijk z6raid mech schop
/guhs vadsrijk z6raid mech lens
/guhs vadsrijk z6raid mech bakens
/guhs vadsrijk z6raid mech val
```

## Buit
```
/guhs vadsrijk z6raid loot
/guhs vadsrijk z6raid loot baas
/guhs vadsrijk z6raid loot zeldzaam
/guhs vadsrijk z6raid items
```
- `loot`: een clear-kist.
- `loot baas`: een kleine kist (zoals bij baas 1-3).
- `loot zeldzaam`: de Sterrenring-vleugels (in je Kledingkast, toets O; luchtdash), het huisdier Sterrenpluisje, het Sterrennacht-halster (= het rijdier, zie hieronder) en Ringelings Teenring.
- `items`: de hele Sterrenzijde-set, de 3 raidwapens en de Ringreplica.
- **Rijdier (V-INT, jouw loot-regel):** de **Bultguh in Sterrennacht-vacht** valt bij elke clear met **10 % per speler**, en is
  zeker na 15 keer mis. Heb je de Bultguh nog niet, dan krijg je hem erbij. Het halster uit `loot zeldzaam` geeft precies
  hetzelfde (rechtsklik).
```
/guhs vadsrijk loot pech z6raid_sterrennacht 15
/guhs vadsrijk loot rol z6raid_sterrennacht
```
  Daarna: N (rijdieren) > Bultguh > vacht Sterrennacht staat aan.
- **Knuffelbond:** een clear geeft elk lid in de toren **200 bondsxp** (plus 40 per verslagen baas, binnen de dagcap van 600).
- **HUD:** de raid-frames rechts zijn te verschuiven en te schalen in Instellingen > HUD aanpassen ("Raid-frames"), ook buiten de raid.
- **Geluid:** in de Wachtzaal en op de verdiepingen hoor je de zachte torendrone (geen gebiedsmuziek); in een gevecht de
  bazenmuziek met fases; na Ringeling haar eigen fanfare.
- Wat je moet zien:
  - de **Sterrenzijde-set** (lv 70, legendarisch):
    - 2 stuks = +15 Verdediging en +150 Vads;
    - 4 stuks = Schade tegen bazen +10%;
  - de wapens: Ringbreker (Ridder), Komeetboog (Jager) en Sterrenstaf (Magier);
  - de titels **Ringbreker** en **Sterrenaansteker** (Sterrenaansteker als je niet flauwviel). Die kies je in de Guhdex.

## Sterren
```
/guhs vadsrijk z6raid sterren aan
/guhs vadsrijk z6raid sterren uit
```
Dit zet de sterren in de koepel van het dak aan of uit.
