# Testen: NPC's, quests en het verhaal van zone 1 (Vadsrijk, slice npc)

## Starten
- `/guhs vadsrijk` brengt je naar binnen. De eerste keer in Knuffelkamp start het openingsfilmpje vanzelf (luchtschip bij
  zonsopgang, het eiland, de skyline van Zwamzoet in de verte). Opnieuw kijken: `/guhs vadsrijk npc opening`.
- **J** = je questlog (alleen in het Vadsrijk): tabbladen Quests, Geheimen, Wezens en Gids (de Avonturiersgids).
- Boven guhs met iets voor jou staat een **!** (goud = verhaal, geel = zijquest) of een **?** (hier inleveren of even praten).

## Snelste route door het verhaal
1. Sergeant Pluizebol bij de steiger -> loop naar het kampvuur -> praat met Kok Knapperkorst.
2. Zuster Zachtjes bij het tempeltje: kies je klasse.
3. Meester Kussensloop op het trainingsveld: mep 6 keer op een trainingspop (hooibaal of doelwit). Je krijgt je eerste wapen (lichtstraal!).
4. Kok: steek een kampvuur aan (rechtsklik), praat met de kok, klik de dialoog uit -> **de BAM**: de draak vliegt over, grauwe pluis
   regent naar beneden, de bloemen verwelken (alleen bij jou), de Grauwe Klodderreus landt in de vijver. Vrolijk hem op.
   Opnieuw de BAM zien: `/guhs vadsrijk npc bam`.
5. Terug naar Pluizebol (de bloemen bloeien weer, de Kampklodder woont nu in de vijver) -> Kapitein Wolkewiet: "Naar Zwamzoet!".
6. Grootmeester Vadsvoet voor de ordetempel -> littekens, Sporenwoud (Boswachter Mos), Druipgrotten (Mijnwerker Mopje loopt
   achter je aan naar het grotkampvuur), Zwamtempel (Tempelwachter Steeltje), de Opperzwam -> het Mika-speldje naar de
   Grootmeester -> het slotfilmpje "Wordt vervolgd..." en `zone1_klaar` (Geheimen-tab: "Zone 1 is weer vads!").

## Handige dev-commando's (op)
- `/guhs vadsrijk npc log`: je quests met voortgang in de chat.
- `/guhs vadsrijk npc quest start <id>` / `quest klaar <id>` (alle doelen af, daarna inleveren bij de NPC) / `quest vergeet <id>`.
- `/guhs vadsrijk npc tp <npc-id | poi.<id> | knuffel.<id> | aanwijzing.<id> | schat.<id>>`: er meteen heen.
- `/guhs vadsrijk npc reset`: alle quests, geheimen en verhaalmomenten vergeten (daarna opnieuw beginnen).
- `/guhs vadsrijk npc vlieg stad|kamp`, `/guhs vadsrijk npc cliffhanger`, `/guhs vadsrijk npc npcs` (alle NPC's opnieuw neerzetten).
- Handig erbij: `/guhs vadsrijk level <n>` (de klassenproeven zijn er vanaf level 8 en 18, het rijdier vanaf level 8).

## Waar op letten
- Staan de NPC's op een vloer (niet in een muur, niet zwevend)? Zwevend betekent: op die plek is nog niks gebouwd.
- Kleding en namen van de guhs; zwaaien ze als je dichtbij komt, wandelen de burgers een beetje rond, zitten de kok en de visser?
- Het ! en ? boven de hoofden: klopt het en verdwijnt het na het inleveren?
- Dialogen: "Verder »" door de pagina's, de knoppen "Vahoeg, ik doe het!" / "Ik help je!" / "Nu even niet".
- Questitems (grauwe pluis, wolletjes, zwamhoedjes...) vallen alleen als je ze nog nodig hebt; bij inleveren gaan ze weg.
- Zijquests in Zwamzoet: 20 stuks, plus 2 in het kamp. Het questbord (Klerk Stempeltje) laat zien welke er nu open staan.
- Geheimen: knuffels (hartjes), aanwijzingen (paarse tekentjes) en schatten (goudgeel) glinsteren als je dichtbij bent.
  De eerste aanwijzing start "Wie is Akim de Kaasguh?"; na vijf kun je Akim op de markt ontmaskeren (rechtsklik).
- De klassenproeven (Grootmeester, level 10 en 20) en het rijdier (Stalmeester Hoefje, level 8).

## Bekende gaten
- De kaart (tegels) zat niet in deze worktree: de plekken komen uit POI_VADSRIJK v0; na de integratie kunnen een paar NPC's een
  paar blokken verschoven moeten worden.
- Zonder de andere slices: winkels/smid/markt/stal zeggen "nog niet", de klassenkeuze is een simpele lijst, het luchtschip is een
  eigen filmpje, de draak is een schaduw van rook (met de wezens-slice vliegt de echte Vahoegnir over).
- Questitems hebben nog vanilla-plaatjes.
