# TESTEN ronde X (Guhs 1.10.0 "Het Vadsrijk", testclient x-int)

Start: dubbelklik `guhs_vadsrijk\x-int\run-client.bat`, maak een **nieuwe** wereld (Creatief, cheats aan), `/guhs vadsrijk`.
De client staat op alle zones open (openZones 6). De commando's per punt staan in `SPIEKBRIEF.md` hoofdstuk 1 en 2.
Vink af wat klopt; schrijf erbij wat niet klopt (met de plek: F3-coördinaten).

## A. Je feedback van ronde X (SPIEKBRIEF hoofdstuk 1)
- [ ] 1.1 Walvis (zone 5): de poort aan het eind van het noordelijke zijpad valt op; het bord heeft tekst (niet leeg!);
      in survival kun je van de troonzaal naar het Staartpark lopen en terug, zonder springen.
- [ ] 1.2 Zone 6 buiten de stadjes: niets zweeft (torenvoet, bakens, karavaankamp); de wegen door water zijn droog en vlak.
- [ ] 1.3 De raid-toren: de poort heeft een naambord + Heraut Sterrenroep; naar binnen lopen = De Wachtzaal; naar buiten
      lopen = buiten op de trap, geen lus; de toren is één geheel.
- [ ] 1.4 Dungeon-naamborden: bij alle 7 deuren een leesbaar bord (ook 's nachts); de Koraalgrot-zeebodem zegt "dichtgegroeid".
- [ ] 1.5 Kwartiermeesters hebben een naam (Lamelletje, Klontje, Wasje, Lontje, Torentje); nergens `vadsrijk.iets.iets`.
- [ ] 1.6 Zone 1/2 op de grond: de 7 plekken uit de tabel + de twee stenen trappen uit de baai bij De Haven (geen sprong meer
      aan het eind).
- [ ] 1.7 Galmtop (zone 2): in survival van onder naar het platform en terug.
- [ ] 1.9 Erfenis-dingen uit 1.9.3-1.9.7 werken nog (opslag weggeven, Terughekje, Leverkist), want die zijn erin gemerged.

## B. Release met alleen zone 1 (SPIEKBRIEF hoofdstuk 2)
Zet de release-blik aan: `/guhs vadsrijk zones open 1` en `/guhs vadsrijk zones alsspeler aan`.
- [ ] `/guhs vadsrijk zones zone1klaar`: de tracker zegt "Wordt vervolgd ... Houd Discord in de gaten!".
- [ ] De luchtschipper op De Haven (`/tp @s 86 65 495 180 5`) zegt dat het schip binnenkort vaart; de reiskaart toont zone 2+
      grijs "Binnenkort!".
- [ ] Lijn A (`/tp @s 150 127 -650`) neemt je niet mee.
- [ ] De grens (`/tp @s 704 100 -1100`) zet je terug in zone 1.
- [ ] De Zwamgrot werkt (solo, wachtrij met nep-spelers).
- [ ] Level-plafond: `/guhs vadsrijk level 19`, dan wat wezens: je stopt op 20, chatregel over sparen, Karakter-menu toont
      "Hoogste level voor nu".
- [ ] Terug: `/guhs vadsrijk zones alsspeler uit`, `/guhs vadsrijk zones open 6`: alles weer open, spaarpot betaalt uit.
- [ ] De wiki-preview met alleen zone 1: https://guhs.nl/vadsrijk-preview-zone1/ (zone 2-6 nergens te zien).

## C. Het verhaal (Questlopen, hoofdstuk 0)
- [ ] `/guhs vadsrijk questlopen`: zone 1 helemaal doorklikken tot "Een grauwe schaduw".
- [ ] Desgewenst zone 2-6 doorklikken (alles staat open).

## Wat de automatische checks al zeiden (EINDRAPPORT_RONDE_X.md)
Alle hoofdquests, NPC's en geheimpjes lopend bereikbaar (bereikcheck --strikt), de questrobot speelt alles uit (0 vast), ook
met de poort op 1 (zone 1 hoofd 13/13, overige 26/26). Wat je met je ogen moet beoordelen: hoe de borden, de toren en de
trappen eruitzien.
