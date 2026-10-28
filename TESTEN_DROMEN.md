# Guhdromenland testen (1.8.0, deel A: de droomdimensie)

## Zo kom je in een droom (het echte spel)
1. Zet een **Doos met knabbels** of een **kattenhuisje** neer, zoals in het creatieve tabblad of via het recept.
2. Zorg dat er een **tamme guh van jou** in de buurt is.
3. **'s Avonds of 's nachts:** rechtsklik op de doos. Je guh kruipt erin en valt in slaap. Wat je in je hand hebt, geef je mee:
   - knuffel/guhkussen → Slaapliedjesheuvels
   - sterrenlantaarn/sterrenkoekje → Sterrenkussenveld
   - roze guh koek/ansichtkaart → Herinneringseiland
   - badeendje → Wolkenbadje
   - lampion (later droomzand) → Maanmolentje
   - niets → Droomplein
4. Ga zelf in een **bed binnen 6 blokken** liggen. Na ongeveer 2 seconden word je wakker in Guhdromenland.
5. **Wakker worden** kan op drie manieren: met het **droomkussen** (rechtsklik; je krijgt er een bij je eerste droom), door te praten met de **Wekkerguh** op het plein, of door dood te gaan in de droom (je verliest niets). Je komt terug bij je bed, je guh wordt geeuwend wakker en je krijgt je meegegeven item terug.
6. **Uitstoppen:** sluip en rechtsklik op de doos.

## In de droom
- Bouwen en slopen kan niet, in creative wel.
- Je zweeft zachtjes: slow falling en een iets hogere sprong.
- Val je naar beneden, dan vangt een Blub-bubbel je op en zet je terug op het laatste eiland.
- **Steigers**, de witte plateautjes met een zeepbel erboven: ga erop staan, dan zweef je naar het plein. Op het plein staat er één per eiland.
- De eilanden zijn nog **placeholders**: ronde wolkeneilandjes. De eiland-agents leveren de echte.

## Testcommando's (op, permissieniveau 2)
| Commando | Wat |
|---|---|
| `/guhs droom ga [eiland]` | Meteen de droom in, op dat eiland. Je thuisplek wordt onthouden. In de droom zweef je naar dat eiland. Eilanden: plein, sterren, molentje, badje, herinnering, liedjes |
| `/guhs droom wakker` | Wakker worden |
| `/guhs droom bedtijd aan` / `uit` | Instoppen kan altijd (handig overdag) / weer alleen 's avonds en 's nachts |
| `/guhs droom voortgang` | Toont Snoes' stukjes, de finale en "Snoes geholpen" |
| `/guhs droom voortgang zet <liedje\|ster\|herinnering\|bad\|droomzand\|alles\|finale\|snoes>` | Zet voortgang (finale/snoes zet ook de Blub-vlag) |
| `/guhs droom voortgang reset` | Alles vergeten, ook de Blub-vlag |
| `/guhs droom eilanden opnieuw` | Alle eilanden opnieuw stempelen |

Snel testen: `/guhs droom bedtijd aan`, zet een doos neer, rechtsklik met je guh in de buurt, en ga in een bed liggen. Of gewoon `/guhs droom ga sterren`.
