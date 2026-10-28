# De Vliegdroom testen (1.8.0 uitbreiding)

De Vliegdroom is een eiland hoog boven het Droomplein, op hoogte 200. Je komt er alleen op een Rookguh.

## De echte manier
1. Droom naar het Droomplein, of gebruik `/guhs droom ga plein`.
2. Ten noordoosten van de fontein ligt een **Rookguh-steigertje**: 3x3 roze wolkjes met twee lantaarns. Daarboven zweeft een Rookguh. Klik erop, en hij draagt je omhoog naar de Vliegdroom.
3. Op de Vliegdroom:
   - **Rookguh-stal**: het grote nest aan de noordkant. Klik op een Rookguh om op te stappen.
     - Vooruit = vliegen waar je kijkt, ook omhoog of omlaag.
     - Springen = recht omhoog.
     - Sprinten = sneller.
     - Sluipen = eraf.
   - **Wolkjesringen-parcours**: tien ringen rond het eiland. De volgende ring fonkelt, en de klok loopt van ring 1 tot ring 10.
     - De eerste keer dat je het hele parcours vliegt: het **Vlieg-diploma** + 5 droomzand.
     - Daarna: een nieuw record geeft 1x per Minecraft-dag 2 droomzand.
     - Vlieg je 45 seconden geen ring, dan is de ronde voorbij.
   - **Hoogste wolk**: een wenteltrap rond de wolkentoren naar het uitzicht.
   - **Naar beneden**: klik op de Rookguh bij het regenboog-plaatje, of stap in de zeepbel ernaast.

## Testcommando's
- `/guhs vlieg ga`: meteen naar de Vliegdroom, ook vanuit de echte wereld.
- `/guhs vlieg info`: je record, je diploma en de volgende ring.
- `/guhs vlieg reset`: record, diploma en dag-beloning wissen.
- `/guhs droom ga vlieg`: werkt ook.
- `/guhs droom eilanden opnieuw`: zet alle eilanden opnieuw neer.

## Muziek
- `inspiration\Dromen audio\muziek\vlieg`: zet hier eigen muziek neer en draai daarna `python tools/dromen_audio_import.py`. Wil je alleen deze map importeren: `python tools/dromen_audio_import.py --ids muziek.vlieg`.
