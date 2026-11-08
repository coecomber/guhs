# SPIEK U-GEROEZEMOES: guh-geroezemoes op drukke plekken

Wens: in Hoogvads en andere drukke plekken een levendig achtergrond-**geroezemoes**.
- Een bedje van gepraat, maar guh-achtig: vooral onverstaanbaar zacht guh-gekwebbel.
- Hier en daar een hoorbare "njeg", een giechel, een pieppiepmuisje of een blub.
- **Niet overdrijven** (les van de wind): het moet een prettige achtergrond blijven.

> Smaak gaat voor. Te hard, te zacht, te vaak een stemmetje? Zeg het, het zijn een paar getallen:
> - het volume van het bed: `BASIS` in `GeroezemoesSpeler.java`;
> - hoe vaak er een stemmetje klinkt: `pauze` in `Geroezemoes.java`;
> - hoe druk een plek is: de tabel `PLEKKEN` in `Geroezemoes.java`.

## Snel horen
| Wat | Commando |
|---|---|
| Klikbare lijst (onderaan: geroezemoes) | `/guhs vadsrijk geluid` |
| Wat hoort er nu: plek, aantal guhs, intensiteit, bedden | `/guhs vadsrijk geluid geroezemoes info` |
| Rustig bed vastzetten | `/guhs vadsrijk geluid geroezemoes rustig` |
| Drukke straat vastzetten | `/guhs vadsrijk geluid geroezemoes druk` |
| Volle markt vastzetten | `/guhs vadsrijk geluid geroezemoes markt` |
| Zelf een intensiteit kiezen (0-100) | `/guhs vadsrijk geluid geroezemoes kracht 40` |
| Terug naar automatisch | `/guhs vadsrijk geluid geroezemoes auto` |
| Eén stemmetje, 5 blokken voor je | `/guhs vadsrijk geluid geroezemoes njeg` (ook `lach`, `guh`, `oeh`, `piep`, `blub`) |

Vastzetten werkt overal, ook buiten het Vadsrijk, dus je kunt de drie bedden rustig na elkaar vergelijken. Daarna altijd `auto`.

## 1. De drie bedden
Ze zijn gemaakt van ALLEEN de bestaande guh-geluiden: geen nep-stemmen. Veel kleine guhs praten door elkaar, elk met een eigen toonhoogte en afstand, in zinnetjes met pauzes. De verre guhs klinken gedempt, dus je verstaat niemand.

1. `/guhs vadsrijk geluid geroezemoes rustig`
   → Een paar guhs, ver weg. Een stille straat of de avond.
2. `/guhs vadsrijk geluid geroezemoes druk`
   → Een drukke straat. Heel af en toe een "njeg" of piepje ín het bed.
3. `/guhs vadsrijk geluid geroezemoes markt`
   → Een vol marktplein. Ook een blub ergens.
4. Luister 40 s per bed.
   → Je hoort GEEN tik of sprong als het bed rondloopt (lussen van 30 s).
   → Het bed mag nooit boven de muziek of de stemmen uit komen.
5. `/guhs vadsrijk geluid geroezemoes auto`

## 2. Losse stemmetjes
- Af en toe klinkt één duidelijk stemmetje, bij een van de guhs om je heen (je hoort het van die kant komen):
  - een "njeg", een "guh!", een giechel of een "oeh?";
  - soms een pieppiepmuisje of een blub (aan de Lampionkust vaker een blub).
- Hoe vaak:
  - een rustige plek: ongeveer elke 25-40 s;
  - een volle markt: elke 8-14 s.
- Ze wachten op de pauze van de wereldgeluiden (bijen, smidse, ...), dus er stapelen nooit twee geluidjes op elkaar.

## 3. Hoe druk waar (automatisch)
Volgorde van druk naar rustig:
1. Hoogvads (de Marktlaan het drukst);
2. de bazaar van Saffraanpoort;
3. de markt van Kandijkom;
4. Zwamzoet;
5. de kade van Lampionhaven;
6. Honingtak;
7. het Rozenplein van Rozenkloof;
8. Knuffelkamp;
9. (De Haven van zone 1, nog iets rustiger.)

Hoe het werkt:
- Hoe meer guhs er om je heen staan (binnen 22 blokken: NPC's, de stadsbewoners van Hoogvads, ruiters), hoe drukker.
- Een stad wordt nooit helemaal stil.
- 's Nachts is alles veel stiller (40%).
- Buiten deze plekken: staan er 3+ guhs bij elkaar, dan hoor je een klein beetje geroezemoes (dat was vroeger het "marktgeroezemoes" van de wereldgeluiden; dat is nu hierin opgegaan).
- Het wisselen gaat geleidelijk (een paar seconden), nooit met een sprong.
- Tijdens een baasgevecht zakt het naar een kwart.

Teleporteren en luisteren (zeg overal `/guhs vadsrijk geluid geroezemoes info`):
```
/execute in guhs:vadsrijk run tp @s 2738 240 -510
```
→ De Marktlaan van Hoogvads: overdag "plek hoogvads (markt)". Het bed staat op of tegen "markt".
```
/execute in guhs:vadsrijk run tp @s 3762 72 -812
```
→ De bazaar van Saffraanpoort: druk. Het synthetische bazaar-bed van de zone loopt eronder door.
```
/guhs vadsrijk z2wereld plek plein
```
→ Kandijkom.
```
/guhs vadsrijk wereld plek plein
```
→ Zwamzoet.
```
/execute in guhs:vadsrijk run tp @s 1844 67 -460
```
→ Het havenplein van Lampionhaven.
```
/guhs vadsrijk z3wereld plek markt
```
→ Honingtak.
```
/execute in guhs:vadsrijk run tp @s 3400 71 -716
```
→ Het Rozenplein van Rozenkloof.
```
/guhs vadsrijk wereld plek kampvuur_kamp
```
→ Knuffelkamp: het rustigst van de rij.

Nacht testen:
```
/time set 18000
```
→ `info` zegt "nacht", de intensiteit is veel lager. Terug: `/time set 1000`.

## 4. De schuif
- Mods → Guhs → Config → **"Vadsrijk: geroezemoes"** (standaard 60).
- 0 = helemaal uit (ook de losse stemmetjes).
- Hij volgt ook Minecraft's schuif "Omgeving".
