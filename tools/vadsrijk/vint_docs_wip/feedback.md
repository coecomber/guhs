# 8. FEEDBACK RONDE 4: ELK PUNT, WAT ER GEDAAN IS EN HOE JE HET CONTROLEERT

Per punt uit je feedback: wat er veranderd is, de commando's en waar je op let. Alles is getest vanuit deze testclient
(v-int). De kaart is opnieuw gegenereerd, dus begin in een **nieuwe wereld** (zie Starten).

## 8.1 Zone-overgangen natuurlijker (u-overgang)
- **Gedaan:** één gedeelde open zee tussen alle zones: de zeebodem sluit aan, de waterkleur, lucht en mist verlopen in 8 stapjes,
  de wolkenzee van zone 3 rafelt uit, en er staan losse rotsjes in de zee.
- **Controleren:**
```
/guhs vadsrijk indeling rand wolkenkloof_noord
/guhs vadsrijk indeling plek haven_z2_z3_vertrek
```
- **Let op:** geen rechte wolkenrand, geen ineens andere waterkleur, geen onderwaterklif op een rechte lijn. F3: open zee =
  "Het Vadsrijk - De Zee". Meer plekken staan in §1 bij U-OVERGANG.

## 8.2 Zone 5: gebouwen episch, interieurs, figuranten (u-z5, later u-z5binnen)
- **Gedaan:**
  - de kathedraal is gotisch met diepte: steunberen, luchtbogen, pinakels, een roosvenster, diepe portalen, ribgewelven en
    grote kroonluchters;
  - de universiteit kreeg hoge gotische ramen, dakkapellen en een bibliotheekgalerij;
  - het paleis, de raadszaal, de academie en het station kregen reliëf;
  - de hele stad kreeg vensterkappen;
  - de figuranten zijn gefixt: geen stapel kleren meer en het goede model per soort.
- **Controleren:**
```
/guhs vadsrijk z5wereld plek kathedraal_plein
/guhs vadsrijk z5wereld plek kathedraal_schip
/guhs vadsrijk z5wereld plek universiteit_bibliotheek_galerij
/guhs vadsrijk z5wereld plek troonzaal_audientie
/guhs vadsrijk z5wereld plek markt_fontein
/guhs vadsrijk z5wereld bewoners
```
- **Let op:** de diepte van de gevels, de kroonluchters binnen en of de figuranten er als normale guhs uitzien.

## 8.3 Knuffelkamp: alle paden verlicht (u-deco)
- **Gedaan:** ~86 guh-lantaarnpalen langs alle kamppaden, om de ~14 blokken.
- **Controleren:** `/time set night`, dan `/guhs vadsrijk wereld plek kampvuur_kamp` en loop alle paden af.
- **Let op:** je ziet steeds de volgende paal, en er staat nooit een paal op het pad.

## 8.4 Luchtschip: hele schip klikbaar, vrij lopen, uitleg als het nog niet mag (u-luchtschip)
- **Gedaan:**
  - 12 klikvakken over het hele schip (romp, ballon, oortjes, staart);
  - lopen met WASD over het dek (de reling houdt je tegen);
  - bij een dichte lijn: "Volg eerst de hoofdquest: na ... kun je hier vliegen" + het level.
- **Controleren:**
```
/gamemode survival
/guhs vadsrijk luchtschip haven haven_z1_z2_vertrek
```
- **Let op:** klik op de ballon of de staart: je stapt in. Met een nieuw karakter (lijn dicht) zie je de uitleg in de chat en
  in de actiebalk.

## 8.5 W3-uitkijkpunten op epische plekken (u-overgang)
- **Gedaan:** nieuw is De Reuzenhoed bovenop de tempelzwam. De uitkijkpunten bij de ballonhavens zijn weg. Alle 21 namen zijn
  vertaald.
- **Controleren:**
```
/guhs vadsrijk indeling plek uitkijk_hoedtop
/guhs vadsrijk indeling plek uitkijk_nest
/guhs vadsrijk indeling plek uitkijk_galmtop
```

## 8.6 Zichtbaarheid van cape en uitrusting (u-zichtbaar)
- **Gedaan:** de stat-cape gaat via de echte cape-laag. Handschoenen, armband, ringen, ketting en oorbellen staan op het
  spelermodel en worden gesynct naar anderen.
- **Controleren:**
```
/guhs vadsrijk z3npc klaarzetten
/guhs vadsrijk zichtbaar aankleden
```
- **Let op:** druk F5 en kijk of je de cape ziet en de handschoenen. In de eerste persoon zie je de handschoenen NIET; dat is
  bekend.

## 8.7 Gondel zone 2: geen mobs bij aankomst (u-deco)
- **Gedaan:** binnen 24 blokken van elke aankomstplek spawnt niets meer. Dat geldt voor de gondels, havens, steigers, de
  luchthaven, terugportalen, kampvuren en stadsranden.
- **Controleren:**
```
/guhs vadsrijk z2wereld plek gondel_dal
/guhs vadsrijk udeco vrij
```

## 8.8 Dubbele spawns (u-dubbel)
- **Oorzaak:** niet-opgeslagen entities bleven als "spook" in het geheugen hangen, en een manager keek in het laadvenster
  (de chunk is er al, de entities nog niet).
- **Gedaan:** uniek-sleutels per baas/NPC, een binnenkomst-bewaker en een spookveger.
- **Controleren:**
```
/guhs vadsrijk npc tp z2_kapitein
/guhs vadsrijk dubbel scan
/guhs vadsrijk z3bazen tp kruinroofvogel
/guhs vadsrijk dubbel scan
```
- **Let op:** 1 Kristalwacht Splinter en 1 Kruinroofvogel, ook na een paar keer weg- en terugvliegen.

## 8.9 Hekken, kettingen, vlaggetjes en slingers verbonden (u-hekken)
- **Gedaan:** een centrale verbindingspas over alle tegels (24.136 open verbindingen → 0). Hij zit in het bouwproces
  (`maak_kaart`), en door code geplaatste stukken gebruiken `Verbind.java`.
- **Controleren:** de teleports in §1 bij U-HEKKEN, plus de kastelen van de Knuffelbond en de huizenwijk.
- **Let op:** overal doorlopende lijnen, ook op de tegelgrenzen.

## 8.10 Uitkijkpunt `uitkijk_nest`: naam, losse hekken, zwevende lantaarns (u-overgang + u-deco + u-hekken)
- **Gedaan:**
  - de naam is nu **Het Kruinnest** (NL/EN);
  - de reling is één ring;
  - de lantaarns staan op de vloer.
- **Controleren:** `/guhs vadsrijk indeling plek uitkijk_nest`

## 8.11 Wenteltrap in de boom: verbonden hekken + verdiepingen (u-deco + u-hekken)
- **Gedaan:**
  - een gekleurde band per verdieping;
  - banieren en een lantaarntje op elk bordes;
  - een naambord: "Honingtak, Verdieping 2 van 6".
- **Controleren:** `/guhs vadsrijk z3wereld plek trap_bordes_1` t/m `_6`; als een bord ontbreekt: `/guhs vadsrijk udeco borden`.

## 8.12 Kruinroofvogel-arena: onderkant (u-deco)
- **Gedaan:** een organische nestkom met takken, hangend mos, wortels en honingdruppels.
- **Controleren:** `/guhs vadsrijk z3wereld plek honinglift_kruin_onder` en kijk omhoog.

## 8.13 Honingtak: licht, bankjes, prullenbakjes, bijen (u-deco)
- **Gedaan:**
  - om de ~10 blokken een paal;
  - om en om een bankje of een prullenbak + bloembak;
  - een kring rond de fontein;
  - overdag max 8 bijen (vanilla + guhbijtjes) plus de honingbijtjes. Ze steken nooit.
- **Controleren:**
```
/time set day
/guhs vadsrijk z3wereld plek plein
/guhs vadsrijk udeco bijen
```

## 8.14 Karakters (u-karakter)
- **Gedaan:**
  - drie VASTE slots: links Normaal, midden Knuffelmodus, rechts Eénvads. De modus is niet aan te passen.
  - De eerste keer maken werkt nu: een welkomstscherm na het openingsfilmpje, met een rode reden als iets niet mag.
  - Vindbaar zonder commando's:
    - knop ⚔ bij de inventaris;
    - tab **Ridders** in het Vadsrijk-menu (H);
    - shift + rechtsklik op een kampvuur;
    - de NPC's (Grootmeester, kampwachten, Rekruteerder);
    - Werver Wimpel.
  - De Rustende Ridders-zaal staat nu in de wereldgeneratie (Zwamzoet, tussen de Kruidenier en de boulevard).
- **Controleren:**
```
/guhs vadsrijk karakter welkom vergeet
/guhs vadsrijk karakter welkom
/guhs vadsrijk karakter zaal tp
```
- **Let op:** wisselen mag alleen in een stad, bij een kampvuur of in de eerste minuut.

## 8.15 Spiekbrief zonder zone 4 en 5
- **Gedaan:** deze spiekbrief heeft hoofdstuk 4 (De Lampionkust) en 5 (Hoogvads) volledig, plus alle ronde-U-punten per zone.

## 8.16 Schildpadeiland: lampion en bloemetjes te hoog, knuffel op de NPC (u-deco)
- **Gedaan:** alles staat nu op de echte schildhoogte, en geheime knuffel 6 heeft een eigen plekje.
- **Controleren:**
```
/guhs vadsrijk z4wereld plek schildpad_rug
/guhs vadsrijk z4wereld plek geheim_knuffel_6
```

## 8.17 Audio (u-audio2)
- **Gedaan:**
  - bazenmuziek met een laatste fase (crossfade);
  - een eigen overwinningsfanfare per baas;
  - 5 nieuwe thema's: imker, Grauwe Droom, Donderbuik, Gorgonzola, Zilverstaart;
  - zachte wereldgeluiden (korven, aambeelden, fonteintjes, kampvuren, marktgeroezemoes);
  - een grot-echo;
  - NPC-stemmetjes uit de bestaande guh/piep/blub-geluiden;
  - schuifjes "wereldgeluiden" en "stemmen".
- **Controleren:**
```
/guhs vadsrijk geluid fase grauwe_droom
/guhs vadsrijk geluid fanfare klodderreus
/guhs vadsrijk geluid stem groet
```
- **Let op:** of de wereldgeluiden nu subtiel genoeg zijn.

## 8.18 Website: volledig testen (u-site)
- **Status:** zie §7 (u-site). Lokaal te bekijken, en publiceren op een eigen sub-url. Staat het er nog niet, dan
  meldt het EINDRAPPORT de status.

## 8.19 Besluiten V1-V5 die nu gebouwd zijn
- **Knuffelbond (guilds):**
  - oprichten in Hoogvads, lid worden vanaf zone 1;
  - eigen kasteel (niveau 1-5) in de Bondenvallei;
  - weektaken, perks, bondschat `/bc`, ranglijst.
  - Waar: tab **Knuffelbond** in H of `/bond`.
- **Huizen:**
  - Het Knuffelkwartier, met kavels in straatjes en 1 huis per account;
  - te bereiken via de Kwartiermeester op elk hoofdplein; tab **Mijn huis** in H.
- **Grotten:** in elke zone 1 sfeergrot: Zwamgrot, Kristalgrot, Wortelgrot, Koraalgrot (duiken!) en de Wolkengrotten (Blaasgat).
  Elk met een ambacht, een elite, een puzzel en een schat.
- **Job-advancements nieuw model:**
  - lv 17: de specialisatie kiezen;
  - lv 35: zone 3;
  - lv 58: na de zone-5-finale;
  - respec is gratis bij de klasse-NPC;
  - de spec staat in de Karakter-tab en in de party-frames.
- **Gear:** geen iNv; een beter/slechter-pijl in de tooltip.
- **Rijdende guhs:** 22 ruiters op Pluimguhs (20 in zone 3 op de trappen, Honingtak en stal; 2 in zone 1).
- **Party-frames, emotes (;) en de HUD-bewerker:** de HUD-bewerker zit in Instellingen › HUD aanpassen, of linksboven in het
  pauzemenu.
- **Ambachten groeien mee:** T4/T5, cap 60, koraalduiken, Hoogvads als ambachtshoofdstad.
- **Collectiescherm:** alles zit in één Vadsrijk-menu (H) met even grote hoofdtabs, en de overflow is gefixt.
- **Pauzemenu:** rechts staat een overzicht van alle Vadsrijk-toetsen.
