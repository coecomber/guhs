# SPIEK y-scheiding: is het Vadsrijk echt los van survival?

Test met een **niet-op** account voor de grens-regels (ops mogen erlangs om te testen). Voor de "lijf"-wissel maakt op of niet niet uit. Stappen die op-rechten nodig hebben (`/give`, `/effect`, `/xp`) doe je van tevoren of met een tweede account.

**1. Volle tas + item aan je muis**
- Vul je survival-inventaris helemaal. Pak een stapel op met je muis en houd het scherm open, of leg iets in het 2x2-rooster.
- Typ `/guhs vadsrijk`.
- Zie: er valt niets op de grond, niet binnen en niet buiten.
- Weer buiten met een volle tas? Wat niet paste, haal je op met `/guhs vadsrijk kist`.

**2. Enderkist**
- Leg iets in je enderkist. Ga naar binnen met `/guhs vadsrijk` en typ `/enderchest`.
- Zie: "werkt niet in het Vadsrijk". Een enderkist-scherm sluit meteen.
- Buiten werkt `/enderchest` gewoon, met je spullen erin.

**3. Vanilla xp**
- Zorg buiten voor xp: `/xp add @s 30 levels`.
- Ga naar binnen. Zie: de xp-balk staat op 0.
- Ga naar buiten. Zie: weer precies 30 levels.

**4. Drankjes, hartjes, honger, vuur**
- Buiten: `/effect give @s minecraft:strength 300 1` en `/effect give @s minecraft:absorption 300 1`. Laat je wat schade doen en wat honger krijgen.
- Ga naar binnen. Zie: geen effecten, volle hartjes en voedsel (zoals altijd binnen).
- Ga naar buiten. Zie: Kracht II is terug met de resterende tijd, en je hebt weer evenveel hartjes en honger als daarvoor. Geen gratis genezing.
- Effecten die je binnen kreeg (zoals Nachtzicht in zone 3 of Waterademen in zone 4) zijn buiten weg.

**5. Teleporten met andere mods (niet-op)**
- Buiten: `/sethome test` op een plek in de Guhmension.
- Binnen: probeer `/home test`, `/back`, `/rtp`, `/warp` en `/tpa <vriend>`. Zie: "werkt niet in het Vadsrijk".
- Buiten: laat een vriend die binnen staat jou `/tpahere`-en, of doe zelf `/back` naar een plek binnen. Zie: "Zo kom je het Vadsrijk niet in".
- Naar binnen kan alleen via de ingang of `/guhs vadsrijk`.

**6. Dieren en karretjes**
- Zit op een paard of houd een dier aan een leidtouw, en typ `/guhs vadsrijk`.
- Zie: je staat binnen zonder paard, en het dier blijft buiten.

**7. Beloningen van de rest van Guhs terwijl je binnen bent**
- Laat binnen een Erfenis-gebouw klaar worden, of open `/guhs erfenis sherlock bureau`.
- Zie: het vergrootglas zit NIET in je Vadsrijk-tas.
- Ga naar buiten. Zie: daar is het, in je gewone inventaris.

**8. Premiebord (zone 3)**
- Open het premiebord binnen en ga dan naar buiten.
- Zie: buiten kun je niets meer claimen of ruilen. Er komt geen Vadsrijk-uitrusting in je survival-inventaris.

**9. Knuffelbond-reis**
- Buiten: `/bond reis plein`. Zie: "Zo kom je het Vadsrijk niet in". Binnen werkt het gewoon.

**10. Momo**
- Neem Momo op je schouder mee naar binnen. Zie: binnen jat hij niets en geeft hij niets terug. Buiten gaat hij gewoon door.
