# Blub-guh testen (1.8.0 testbuild, ronde 2)

Start de client: dubbelklik `C:\Users\Juiced\Documents\MineScapeStory\guhs_work\w-blub\run-client.bat`
(eigen `run/`-map met een nieuwe testwereld; maak een creatieve wereld met cheats aan).

**Eerst:** `/guhs blub snoes aan`. Wilde Blubs zie je pas als je Snoes geholpen hebt (dat verhaal bestaat nog niet, dus zet je
het zo aan). Zonder die vlag zijn wilde Blubs onzichtbaar en niet aan te klikken; tamme Blubs zie je altijd.

## Testcommando's (op, cheats aan)
| Commando | Wat |
|---|---|
| `/guhs blub snoes aan` / `uit` | Jouw vlag "Snoes geholpen" (wilde Blubs zien en temmen) |
| `/guhs blub spawn <meter> [soort] [tint] [grootte] [baby]` | Eén Blub-guh voor je neus. soort = blauw / weide / schild / ravijn / zwam, tint 0-2, grootte in hele procenten 75-125, baby true/false. Bijv. `/guhs blub spawn 100 zwam 1 110 false` |
| `/guhs blub raster` | Het m06-raster: 5 soorten × meter 0 / 25 / 50 / 75 / 100 (tam, zittend: altijd zichtbaar) |
| `/guhs blub groepje [aantal]` | Random wilde Blubs met de echte spawn-trekking (standaard 12) |
| `/guhs blub tam` | Alle Blub-guhs binnen 16 blokken worden van jou |
| `/guhs blub info` | Meter, soort, tint, grootte, strikje enz. van de Blub waar je naar kijkt |

`spawn`, `raster` en `groepje` werken altijd, maar wilde Blubs zie je alleen met `snoes aan`.
Ook: het **Blub-guh-spawnei** (creatief tabblad Guhs) en de echte spawn (TIJDELIJK) in de **Wolkenweide** en de **Bloemenvallei**
van de Guhmension (groepjes van 2-4; `/locate biome guhs:wolkenweide`).

## Wat je kunt proberen
- **Naamkaartje**: wilde Blubs heten "Blub" ("Pure Blub" op meter 0, "Volle Guh" op 100). Geef je hem een naam, dan zie je die naam,
  en een Pure Blub / Volle Guh houdt zijn titel erachter ("Bubbels ✦ Volle Guh").
- **Temmen** met een **kaasknabbel** (1 op 3), alleen met de Snoes-vlag. Zonder vlag krijg je een grapje over Snoes.
- **Klikken met lege hand**: squish-knuffel; eigenaar: het menu. **Sluipen + klikken**: oppakken (alles blijft bewaard).
- **Menu**: naam, Volgen, Rondvadsen, **Kledingkast** (strikjes in 16 kleuren of niets; ook te zien op de Blub),
  **Trucjes** (zitten, rondje, dood spelen, hoge sprong, hupsen, slaapliedje, verstoppertje, squish, oortjes blij/verlegen/nieuwsgierig),
  Op je schouder (eraf: sluip + rechtsklik op een blok), Oppakken.
- **Grootte**: een volwassen Blub is nu 80% van het vorige model (de grootte 75-125%, in hele procenten met een klokvorm rond 100, en baby's komen daarbovenop).
- **Babyblubjes**: geef twee tamme Blubs een kaasknabbel.
- **Geluiden**: elke actie heeft nu een eigen geluid (placeholders). Zie `C:\Users\Juiced\Documents\MineScapeStory\inspiration\Blub audio\LEESMIJ.md`.
- **Uit zichzelf**: oortjes-emoties, tikkertje en knabbel delen met guhs, 's nachts slapen bij een guh of in een guhnestje,
  bellen blazen, glinsteren (Pure Blub / Volle Guh), de blauwe gloeit zachtjes.
- **Regen/plassen**: werkt nu niet in de Guhmension (daar regent het niet; er komt een apart "Weer vadser"-blok).

## Ronde 3
- **Zelf trucjes**: elke Blub (wild of tam) doet ongeveer eens per minuut zelf een trucje of oortjes-emote (niet als hij slaapt,
  verstopt zit, op je schouder zit of onderweg is; hupsen alleen met ruimte; nooit verstoppertje of slaapliedje).
- **Knuffelen met een guh**: tamme Blubs en tamme guhs knuffelen elkaar zo eens per paar minuten (huisgenootjes vaker);
  ook als trucje in het Trucjes-scherm (geen tamme guh binnen 8 blokken: een grapje).
- **Blub-klusjes in het guhhuisje** (alleen Blubs; bij andere bewoners staat er "alleen Blubs", net als "alleen Sjokkel"):
  Bellenbesproeier (gewassen groeien een tikje sneller), Knabbelbezorger (kaasknabbels uit de kist naar tamme guhs/diertjes),
  Slaapliedjeszanger ('s avonds; de guhs van het huisje zijn de volgende dag uitgerust), Bubbelbadmeester
  (zet het **Blub-bubbelbadje** klaar met schuim: guhs worden "fris en blij"). Het badje: craften `T T / TST`
  (T = witte terracotta, S = slijmbal); een Blub in de buurt gaat er ook zelf in badderen.
