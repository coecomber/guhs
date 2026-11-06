## U-Z5TUIN: tuinen in Hoogvads en meer licht in alle gebouwen

**Welke renders zijn oud en welke nieuw?**
- `renders-hoogvads/ronde3/voor_*` = de OUDE stand.
- `binnen2_*` = de NIEUWE interieurs (de vorige slice); `binnen2_voor_*` is daarvan de oude stand.
- Deze ronde: `tuin_voor_*` = oud, `tuin_*` = nieuw (paleistuin, een woning, de herberg).

**Wat is er veranderd?**
- Het roze/groene lappendeken-gras is overal in de stad weg: één rustig groen gazon.
- Elk groter grasvlak is nu een **ontworpen tuin**:
  - paden met guh-lantaarnpalen, roze guh-bankjes en prullenbakjes;
  - moestuinvakken (tarwe, wortels, bieten, aardappels) met watergeulen;
  - pompoenen en meloenen, zoetbessen;
  - bloemenperken met een vogelhuisje, rozentuinen;
  - vijvers met waterlelies;
  - een fontein met een gouden kandelaar in de grote tuinen;
  - lage azaleahaagjes en pergola's over de lange paden.
- Smalle voortuintjes hebben een bloemenrij langs de muur.
- **Binnen** hangen in elk huis, elke winkel, de herberg, de bank, het station, de academie, de universiteit, de gildewerkplaatsen en de soepkeuken nu gouden wandluchters en kleine ronde kroonluchters.

Test in de dev client. Overdag zie je de tuinen het best, 's nachts het licht:
```
/time set noon
```

### De paleistuin (naast de Raadszaal)
```
/guhs vadsrijk z5wereld plek paleis_hof
```
- Kijk naar het plateau naast de Raadszaal (de kant van de kanselarij).
- Er is **geen ruis meer**: het gras is overal hetzelfde groen.
- Je ziet:
  - paden (lichte aarde) in een raster;
  - een fontein met een zuil en een gouden kandelaar;
  - moestuinvakken met watergeultjes;
  - pompoenen en meloenen;
  - bessen achter een azaleahaagje;
  - een bloemenperk met een vogelhuisje;
  - rozen;
  - lantaarnpalen, bankjes en prullenbakjes langs de paden.
- **Loop over alle paden:** niets mag je de weg versperren, en geen bankje of paal mag op een pad staan.
- **Spring in de fontein en weer eruit:** dat moet in één sprong lukken.
- Het plateau aan de andere kant van de Raadszaal (de noordkant) is ook een tuin.

### Andere tuinen
```
/guhs vadsrijk z5wereld plek kathedraal_roos_zuid
```
- Naast de transeptgevel van de kathedraal liggen ook tuinen. Loop er even doorheen.
```
/guhs vadsrijk z5wereld plek ooggaarde_tuin
/guhs vadsrijk z5wereld plek staartpark_prieel
```
- De parken: een vijver, perken, pergola's. Loop over een paar paden, onder een pergola door en langs een vijver.
- Nergens mag je vast komen te zitten. Water is hooguit 1 diep.

### Meer licht binnen (doe dit 's nachts)
```
/time set midnight
/guhs vadsrijk z5wereld plek huis_woning_1
```
- In de woning hangen gouden wandluchters aan de muren, naast de lamp die er al was. Het is nergens echt donker meer.
- Niets hangt in de weg: je loopt nergens met je hoofd tegen een lamp, ook niet bij de deur.
```
/guhs vadsrijk z5wereld plek huis_herberg_hemelbed
/guhs vadsrijk z5wereld plek herberg_hemelbed_boven
```
- In de herberg zie je beneden en boven wandluchters en (waar het plafond hoog genoeg is) een klein rond kroonluchtertje.
- **Loop de trap op:** daar mag geen lamp in de weg hangen.
```
/guhs vadsrijk z5wereld plek bank_hoogvads_balie
/guhs vadsrijk z5wereld plek luchthaven_station
/guhs vadsrijk z5wereld plek academie_kazerne
/guhs vadsrijk z5wereld plek universiteit_collegezaal
/guhs vadsrijk z5wereld plek gilde_smidse
/guhs vadsrijk z5wereld plek soepkeuken_tafel
```
- Overal is het lichter: wandluchters langs de muren en kroonluchtertjes in grote ruimtes.
- Lampen staan nooit boven een trap, deur of bed.
