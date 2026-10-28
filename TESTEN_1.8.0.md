# Testbuild 1.8.0 "Guhdromenland": snel beginnen

Start: `C:\Users\Juiced\Documents\MineScapeStory\guhs_work\w-int\run-client.bat`. Het is dezelfde testwereld als bij de Blub-tests.
Per onderdeel staan alle commando's in de losse `TESTEN_*.md` in deze map.

## 1. Het verhaal op de echte manier
1. Zorg dat je een tamme guh in de buurt hebt en dat het avond is (`/time set 13000`).
2. Klik op een bed: **Snoes** verschijnt. Praat een paar keer met hem en stop hem in. Ga daarna zelf slapen.
3. Je wordt wakker op het **Droomplein**, waar **Bubbels**, de droomgids, je opwacht.
4. Haal de 5 stukjes, in elke volgorde:
   - **Slaapliedjesheuvels**: sluip naar de verlegen Blubs en zet ze op hun eigen klokjesheuvel (do-re-mi-fa-sol).
   - **Sterrenkussenveld**: stuiter over de kussens naar de grote ster. Vallende sterretjes geven droomzand.
   - **Herinneringseiland**: raak de herinneringsbelletjes aan. Het belletje van Snoes gaat als laatste open.
   - **Wolkenbadje**: stap op het zeepbelpad, pak de badeend, het zeepje en het schuim, en praat met Snoes.
   - **Maanmolentje**: klik op de blaaskussentjes voor de Blubs, draai alle tandwieltjes omhoog en praat met de Zandguh.
5. Ga met alle 5 stukjes terug naar het plein: de **finale**.
6. Wakker worden kan met het droomkussen of via de Wekkerguh.

## 2. Snel springen (testcommando's)
- `/guhs snoes start | hoofdstuk <1-5> | finale | reset | status`: de stappen van het verhaal.
- `/guhs droom ga [plein|liedjes|sterren|herinnering|badje|molentje]` en `/guhs droom wakker`.
- `/guhs droom eilanden opnieuw`: alle eilanden opnieuw neerzetten (na een update van een eiland).
- `/guhs droom voortgang [zet …|reset]`: stukjes zetten of resetten.
- `/guhs molentje los|schud|info`.
- `/guhs droomwezens spawn <naam>`: Snoes, de Zandguh, de Wekkerguh, Rommelguh, de Oma-guh, kussengevecht-guhs en sterrenvisjes.
- `/guhs slaapzorg moe|slaap|liedje|wakker|plekjes|spullen`.
- Blub: `/guhs blub snoes aan`, `/guhs blub raster`, `/guhs blub groepje`. Weer: `/guhs weer zone <weertje>` en `/guhs weer potjes`.

## 3. Nog niet in deze build
- **De Zingende Weide**: wordt nog gebouwd. Tot die er is, spawnen wilde Blubs nog tijdelijk in de Wolkenweide en de Bloemenvallei.
- **Nooit in een echte client bekeken**: de droomdimensie zelf (lucht, zweven, eilanden stempelen) en alle filmpjes. De testserver kan geen eigen dimensies laden.
